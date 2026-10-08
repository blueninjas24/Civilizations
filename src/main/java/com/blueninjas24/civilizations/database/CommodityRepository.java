package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.economy.CommodityAccounting;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import java.sql.*;
import java.util.*;

/** SQL-owner-thread API. Explicit inventories only; no automatic village seeding or live scheduling. */
public final class CommodityRepository {
    public static final int REPLAY_VERSION = 1;
    public record Account(Policy policy, State state) {}
    private final Connection connection;
    CommodityRepository(Connection connection) { this.connection = connection; }

    /** Initialization is deliberately not an upsert: existing reserves and tuning cannot be overwritten. */
    public void initialize(UUID settlement, Map<Commodity, Long> reserves, Policy policy) throws SQLException {
        Objects.requireNonNull(settlement); Objects.requireNonNull(policy);
        transaction(() -> { initializeRows(settlement, reserves, policy); return null; });
    }
    void initializeRows(UUID settlement, Map<Commodity, Long> reserves, Policy policy) throws SQLException {
        var stocks = new TreeMap<Commodity, Stock>();
        reserves.forEach((commodity, units) -> stocks.put(commodity, new Stock(units, 0)));
        var state = new State(0, stocks);
            try (var s = connection.prepareStatement("INSERT INTO economy_accounts VALUES (?, 0, ?, ?, ?, ?, ?)")) {
                s.setString(1, settlement.toString()); s.setInt(2, REPLAY_VERSION); s.setInt(3, policy.severeCoverageBps());
                s.setInt(4, policy.surplusCoverageBps()); s.setInt(5, policy.abundantCoverageBps()); s.setLong(6, policy.severeAfterPeriods());
                s.executeUpdate();
            }
            try (var s = connection.prepareStatement("INSERT INTO economy_stocks VALUES (?, ?, ?, 0)")) {
                for (var entry : state.stocks().entrySet()) {
                    s.setString(1, settlement.toString()); s.setString(2, entry.getKey().id()); s.setLong(3, entry.getValue().units()); s.executeUpdate();
                }
            }
    }
    public Optional<Account> load(UUID settlement) throws SQLException { return transaction(() -> read(settlement)); }

    /** Caller supplies the next period, every commodity's inputs and the saved policy. Stale/repeated work fails. */
    public Result advance(UUID settlement, long nextPeriod, Map<Commodity, Flow> flows, Policy policy) throws SQLException {
        var inputs = Map.copyOf(flows);
        return transaction(() -> advanceRows(settlement, nextPeriod, inputs, policy));
    }
    Result advanceRows(UUID settlement, long nextPeriod, Map<Commodity, Flow> inputs, Policy policy) throws SQLException {
            var before = read(settlement).orElseThrow(() -> new SQLException("Economy account not initialized"));
            if (!before.policy().equals(policy)) throw new SQLException("Economy policy changed; explicit transition required");
            if (nextPeriod != Math.incrementExact(before.state().completedPeriods())) throw new SQLException("Economy period is stale or out of order");
            var result = CommodityAccounting.step(before.state(), inputs, policy);
            try (var s = connection.prepareStatement("""
                    INSERT INTO economy_periods VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                for (var report : result.reports()) {
                    var flow = report.flow();
                    s.setString(1, settlement.toString()); s.setString(2, report.commodity().id()); s.setLong(3, nextPeriod);
                    s.setLong(4, report.before().units()); s.setLong(5, report.before().shortagePeriods());
                    s.setLong(6, flow.production()); s.setLong(7, flow.imports()); s.setLong(8, flow.households());
                    s.setLong(9, flow.industry()); s.setLong(10, flow.construction()); s.setLong(11, flow.exports()); s.setLong(12, flow.events());
                    s.setInt(13, policy.severeCoverageBps()); s.setInt(14, policy.surplusCoverageBps());
                    s.setInt(15, policy.abundantCoverageBps()); s.setLong(16, policy.severeAfterPeriods());
                    s.executeUpdate();
                }
            }
            try (var s = connection.prepareStatement("UPDATE economy_stocks SET units = ?, shortage_periods = ? WHERE settlement_id = ? AND commodity = ?")) {
                for (var entry : result.state().stocks().entrySet()) {
                    s.setLong(1, entry.getValue().units()); s.setLong(2, entry.getValue().shortagePeriods());
                    s.setString(3, settlement.toString()); s.setString(4, entry.getKey().id());
                    if (s.executeUpdate() != 1) throw new SQLException("Economy stock disappeared");
                }
            }
            try (var s = connection.prepareStatement("UPDATE economy_accounts SET completed_periods = ? WHERE settlement_id = ? AND completed_periods = ?")) {
                s.setLong(1, nextPeriod); s.setString(2, settlement.toString()); s.setLong(3, before.state().completedPeriods());
                if (s.executeUpdate() != 1) throw new SQLException("Economy checkpoint changed");
            }
            return result;
    }

    /** Recompute exact explanations from retained period inputs and the protected policy. */
    public Optional<Result> history(UUID settlement, long period) throws SQLException {
        if (period < 1) throw new IllegalArgumentException("Invalid economy period");
        return transaction(() -> {
            var account = read(settlement); if (account.isEmpty()) return Optional.empty();
            var stocks = new TreeMap<Commodity, Stock>(); var flows = new TreeMap<Commodity, Flow>();
            Policy savedPolicy = null;
            try (var s = connection.prepareStatement("SELECT * FROM economy_periods WHERE settlement_id = ? AND period = ? ORDER BY commodity")) {
                s.setString(1, settlement.toString()); s.setLong(2, period);
                try (var r = s.executeQuery()) {
                    while (r.next()) {
                        var commodity = new Commodity(r.getString("commodity"));
                        var rowPolicy = new Policy(r.getInt("severe_bps"), r.getInt("surplus_bps"), r.getInt("abundant_bps"), r.getLong("severe_after"));
                        if (savedPolicy != null && !savedPolicy.equals(rowPolicy)) throw new SQLException("Inconsistent economy history policy");
                        savedPolicy = rowPolicy;
                        stocks.put(commodity, new Stock(r.getLong("opening"), r.getLong("prior_shortage")));
                        flows.put(commodity, new Flow(r.getLong("production"), r.getLong("imports"), r.getLong("households"),
                                r.getLong("industry"), r.getLong("construction"), r.getLong("exports"), r.getLong("events")));
                    }
                }
            }
            if (stocks.isEmpty()) return Optional.empty();
            if (!stocks.keySet().equals(account.orElseThrow().state().stocks().keySet())) throw new SQLException("Incomplete economy history");
            return Optional.of(CommodityAccounting.step(new State(period - 1, stocks), flows, savedPolicy));
        });
    }
    Optional<Account> read(UUID settlement) throws SQLException {
        Objects.requireNonNull(settlement);
        Policy policy; long completed;
        try (var s = connection.prepareStatement("SELECT * FROM economy_accounts WHERE settlement_id = ?")) {
            s.setString(1, settlement.toString());
            try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                if (r.getInt("replay_version") != REPLAY_VERSION) throw new SQLException("Unsupported economy replay version");
                policy = new Policy(r.getInt("severe_bps"), r.getInt("surplus_bps"), r.getInt("abundant_bps"), r.getLong("severe_after"));
                completed = r.getLong("completed_periods");
            }
        }
        var stocks = new TreeMap<Commodity, Stock>();
        try (var s = connection.prepareStatement("SELECT * FROM economy_stocks WHERE settlement_id = ? ORDER BY commodity")) {
            s.setString(1, settlement.toString());
            try (var r = s.executeQuery()) {
                while (r.next()) stocks.put(new Commodity(r.getString("commodity")), new Stock(r.getLong("units"), r.getLong("shortage_periods")));
            }
        }
        return Optional.of(new Account(policy, new State(completed, stocks)));
    }
    @FunctionalInterface private interface Work<T> { T run() throws SQLException; }
    private <T> T transaction(Work<T> work) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Commodity accounting requires no active transaction");
        connection.setAutoCommit(false);
        try {
            var result = work.run(); connection.commit(); connection.setAutoCommit(true); return result;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
}
