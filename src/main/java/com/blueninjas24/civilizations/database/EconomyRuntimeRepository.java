package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.simulation.CivilizationTime;
import java.sql.*;
import java.util.*;

/** Single SQL-owner food runtime. Saved clocks, evidence and period effects; no Bukkit access. */
public final class EconomyRuntimeRepository {
    public record View(CommodityRepository.Account account, FarmObservation farm, Result latest, long queuedPeriods,
                       HouseholdNeeds.Assessment needs, TreasuryRepository.Account treasury) {}
    private record Clock(long cadence, CivilizationTime.Anchor anchor, String settings, long completed) {}
    private final Connection connection;
    private final CommodityRepository commodities;
    private final EconomySettings settings;
    private final FarmObservation.Policy farms;
    private final long adulthood;
    private final String signature;
    private int cursor;
    public EconomyRuntimeRepository(Connection connection, EconomySettings settings, FarmObservation.Policy farms, long adulthood) {
        this.connection = connection; this.commodities = new CommodityRepository(connection);
        this.settings = settings; this.farms = farms; this.adulthood = adulthood;
        signature = "food-runtime-1|" + settings + "|" + farms + "|adult=" + adulthood;
        if (adulthood < 1) throw new IllegalArgumentException("Invalid adult threshold");
    }
    public void start(long now) throws SQLException {
        for (var town : towns()) transaction(() -> {
            var clock = clock(town);
            if (!commodities.read(town).orElseThrow().state().stocks().keySet().equals(settings.startingPerResident().keySet()))
                throw new SQLException("Commodity catalog changed; explicit inventory transition required");
            if (!clock.settings().equals(signature)) {
                if (clock.anchor().targetSteps() != clock.completed()) throw new SQLException("Finish queued economic catch-up under old settings before retuning");
                long discarded = Math.addExact(Math.max(0, now - clock.anchor().lastObservedMillis()), clock.anchor().remainderMillis());
                try (var s = connection.prepareStatement("INSERT INTO economy_tuning(settlement_id,after_period,observed_millis,discarded_millis,old_settings,new_settings) VALUES (?,?,?,?,?,?)")) {
                    s.setString(1, town.toString()); s.setLong(2, clock.completed()); s.setLong(3, now); s.setLong(4, discarded);
                    s.setString(5, clock.settings()); s.setString(6, signature); s.executeUpdate();
                }
                try (var s = connection.prepareStatement("UPDATE economy_accounts SET severe_bps=?,surplus_bps=?,abundant_bps=?,severe_after=? WHERE settlement_id=?")) {
                    var policy = settings.shortages(); s.setInt(1, policy.severeCoverageBps()); s.setInt(2, policy.surplusCoverageBps());
                    s.setInt(3, policy.abundantCoverageBps()); s.setLong(4, policy.severeAfterPeriods()); s.setString(5, town.toString()); s.executeUpdate();
                }
                saveClock(town, new CivilizationTime.Anchor(Math.max(now, clock.anchor().lastObservedMillis()), 0, clock.completed()));
                try (var s = connection.prepareStatement("UPDATE economy_runtime SET millis_per_period=?, settings=? WHERE settlement_id=?")) {
                    s.setLong(1, Math.multiplyExact(settings.secondsPerPeriod(), 1000)); s.setString(2, signature); s.setString(3, town.toString()); s.executeUpdate();
                }
            }
            return null;
        });
        pulse(now, true);
    }
    /** Older/equal evidence is idempotent. Partial observations pause; never seed a zero-food village. */
    public void observe(FarmObservation evidence, long now) throws SQLException {
        transaction(() -> {
            var old = farm(evidence.settlement());
            if (old != null && old.observedMillis() >= evidence.observedMillis()) return null;
            try (var s = connection.prepareStatement("""
                    INSERT INTO farm_evidence VALUES (?,?,?,?,?)
                    ON CONFLICT(settlement_id) DO UPDATE SET world_id=excluded.world_id, observed_millis=excluded.observed_millis,
                    complete=excluded.complete, planted_crops=excluded.planted_crops
                    """)) {
                s.setString(1, evidence.settlement().toString()); s.setString(2, evidence.world().toString()); s.setLong(3, evidence.observedMillis());
                s.setInt(4, evidence.complete() ? 1 : 0); s.setLong(5, evidence.plantedCrops()); s.executeUpdate();
            }
            if (evidence.complete() && living(evidence.settlement(), false) + living(evidence.settlement(), true) > 0 && !hasRuntime(evidence.settlement())) {
                long people = living(evidence.settlement(), false) + living(evidence.settlement(), true);
                var account = commodities.read(evidence.settlement());
                if (account.isPresent() && !account.orElseThrow().state().stocks().keySet().equals(settings.startingPerResident().keySet()))
                    throw new SQLException("Commodity catalog changed; explicit inventory transition required");
                if (account.isEmpty()) commodities.initializeRows(evidence.settlement(), settings.startingReserves(people), settings.shortages());
                else if (!account.orElseThrow().policy().equals(settings.shortages())) throw new SQLException("Explicit legacy-account policy transition required");
                long completed = account.map(a -> a.state().completedPeriods()).orElse(0L);
                try (var s = connection.prepareStatement("INSERT INTO economy_runtime VALUES (?,?,?,?,?,?)")) {
                    s.setString(1, evidence.settlement().toString()); s.setLong(2, Math.multiplyExact(settings.secondsPerPeriod(), 1000));
                    s.setLong(3, now); s.setLong(4, 0); s.setLong(5, completed); s.setString(6, signature); s.executeUpdate();
                }
            }
            if (!evidence.complete() && hasRuntime(evidence.settlement())) {
                var clock = clock(evidence.settlement());
                saveClock(evidence.settlement(), new CivilizationTime.Anchor(Math.max(now, clock.anchor().lastObservedMillis()), 0, clock.completed()));
            }
            return null;
        });
    }
    public int pulse(long now, boolean offline) throws SQLException {
        var towns = towns();
        for (var town : towns) transaction(() -> {
            var clock = clock(town); requireSettings(clock);
            var farm = farm(town);
            if (farm == null || !farm.complete()) saveClock(town, new CivilizationTime.Anchor(Math.max(now, clock.anchor().lastObservedMillis()), 0, clock.completed()));
            else saveClock(town, CivilizationTime.accrue(clock.anchor(), now, clock.cadence(), offline
                    ? Math.multiplyExact(clock.cadence(), settings.maximumOfflinePeriods()) : Long.MAX_VALUE).anchor());
            return null;
        });
        int processed = 0;
        if (towns.isEmpty()) return processed;
        for (int visited = 0; visited < towns.size() && processed < settings.periodsPerBatch(); visited++) {
            var town = towns.get(Math.floorMod(cursor++, towns.size()));
            if (transaction(() -> step(town))) processed++;
        }
        return processed;
    }
    private boolean step(UUID town) throws SQLException {
        var clock = clock(town); requireSettings(clock); var farm = farm(town);
        if (farm == null || !farm.complete() || clock.completed() == clock.anchor().targetSteps()) return false;
        var account = commodities.read(town).orElseThrow();
        long adults = living(town, false), children = living(town, true);
        long output = settings.foodProduction(adults, OptionalLong.of(farm.plantedCrops() / farms.cropsPerWorker()));
        var flows = new TreeMap<Commodity, Flow>(); account.state().stocks().keySet().forEach(commodity -> flows.put(commodity, Flow.idle()));
        flows.put(Commodity.FOOD, new Flow(output, 0, settings.foodDemand(adults, children), 0, 0, 0, 0));
        var period = commodities.advanceRows(town, Math.incrementExact(clock.completed()), flows, settings.shortages());
        var needs = new HouseholdNeedsRepository(connection);
        needs.save(HouseholdNeeds.assess(town, needs.population(town), period, adulthood, settings));
        return true;
    }
    public Map<UUID, View> views() throws SQLException {
        var result = new TreeMap<UUID, View>();
        for (var town : towns()) {
            var account = commodities.load(town).orElseThrow();
            var latest = account.state().completedPeriods() == 0 ? null : commodities.history(town, account.state().completedPeriods()).orElseThrow();
            result.put(town, new View(account, farm(town), latest, clock(town).anchor().targetSteps() - account.state().completedPeriods(),
                    new HouseholdNeedsRepository(connection).load(town, account.state().completedPeriods()).orElse(null),
                    new TreasuryRepository(connection).load(town).orElse(null)));
        }
        return Collections.unmodifiableMap(result);
    }
    private long living(UUID town, boolean children) throws SQLException {
        try (var s = connection.prepareStatement("SELECT COUNT(*) FROM residents WHERE settlement_id=? AND status='ALIVE' AND age_steps " + (children ? "<" : ">=") + " ?")) {
            s.setString(1, town.toString()); s.setLong(2, adulthood); try (var r = s.executeQuery()) { return r.getLong(1); }
        }
    }
    private List<UUID> towns() throws SQLException {
        var result = new ArrayList<UUID>();
        try (var s = connection.createStatement(); var r = s.executeQuery("SELECT settlement_id FROM economy_runtime ORDER BY settlement_id")) {
            while (r.next()) result.add(UUID.fromString(r.getString(1)));
        }
        return result;
    }
    private boolean hasRuntime(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT 1 FROM economy_runtime WHERE settlement_id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) { return r.next(); }
        }
    }
    private FarmObservation farm(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM farm_evidence WHERE settlement_id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) {
                return r.next() ? new FarmObservation(town, UUID.fromString(r.getString("world_id")), r.getLong("observed_millis"),
                        r.getInt("complete") == 1, r.getLong("planted_crops")) : null;
            }
        }
    }
    private Clock clock(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT r.*,a.completed_periods FROM economy_runtime r JOIN economy_accounts a USING(settlement_id) WHERE settlement_id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("Economy runtime not initialized");
                return new Clock(r.getLong("millis_per_period"), new CivilizationTime.Anchor(r.getLong("last_observed_millis"), r.getLong("remainder_millis"),
                        r.getLong("target_periods")), r.getString("settings"), r.getLong("completed_periods"));
            }
        }
    }
    private void saveClock(UUID town, CivilizationTime.Anchor anchor) throws SQLException {
        try (var s = connection.prepareStatement("UPDATE economy_runtime SET last_observed_millis=?,remainder_millis=?,target_periods=? WHERE settlement_id=?")) {
            s.setLong(1, anchor.lastObservedMillis()); s.setLong(2, anchor.remainderMillis()); s.setLong(3, anchor.targetSteps()); s.setString(4, town.toString());
            if (s.executeUpdate() != 1) throw new SQLException("Economy clock disappeared");
        }
    }
    private void requireSettings(Clock clock) throws SQLException {
        if (!clock.settings().equals(signature)) throw new SQLException("Economy settings require startup transition");
    }
    @FunctionalInterface private interface Work<T> { T run() throws SQLException; }
    private <T> T transaction(Work<T> work) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Food runtime requires no active transaction");
        connection.setAutoCommit(false);
        try { var result = work.run(); connection.commit(); connection.setAutoCommit(true); return result; }
        catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
}
