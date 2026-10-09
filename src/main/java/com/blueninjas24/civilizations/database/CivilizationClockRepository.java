package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.simulation.CivilizationTime;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.*;
import java.util.function.LongFunction;

/** Owner-thread saved time queue. Queue commits separately before demographic catch-up. */
public final class CivilizationClockRepository {
    private final Connection connection;
    CivilizationClockRepository(Connection connection) { this.connection = connection; }

    /** Explicit opt-in; existing completed demographic months become the initial queued target. */
    public void initialize(long nowMillis, long millisPerStep) throws SQLException {
        if (nowMillis < 0 || millisPerStep < 1) throw new IllegalArgumentException("Invalid clock settings");
        requireIdle();
        try (var statement = connection.prepareStatement("""
                INSERT INTO civilization_time(id, millis_per_step, last_observed_millis, remainder_millis, target_steps)
                SELECT id, ?, ?, 0, completed_steps FROM demographic_clock WHERE id = 1
                """)) {
            statement.setLong(1, millisPerStep);
            statement.setLong(2, nowMillis);
            if (statement.executeUpdate() != 1) throw new SQLException("Demographic clock must be initialized first");
        }
    }

    public Optional<CivilizationTime.Anchor> load(long millisPerStep) throws SQLException {
        if (millisPerStep < 1) throw new IllegalArgumentException("Invalid clock cadence");
        requireIdle();
        return read(millisPerStep);
    }

    /** Persist elapsed time before doing bounded simulation; failed catch-up keeps its queued target. */
    public CivilizationTime.Accrual accrue(long nowMillis, long millisPerStep, long maximumElapsedMillis) throws SQLException {
        return accrue(nowMillis, millisPerStep, maximumElapsedMillis, null);
    }

    /** Start a live session without crediting server downtime. First adoption audits legacy pending time. */
    public CivilizationTime.Anchor resumeOnline(long now, long cadence) throws SQLException {
        requireIdle();
        connection.setAutoCommit(false);
        try {
            var before = read(cadence).orElseThrow(() -> new SQLException("Calendar not initialized"));
            boolean adopted;
            long completed;
            try (var s = connection.createStatement(); var r = s.executeQuery("SELECT completed_steps, EXISTS(SELECT 1 FROM online_calendar) FROM demographic_clock WHERE id=1")) {
                if (!r.next()) throw new SQLException("Demographic clock missing");
                completed = r.getLong(1); adopted = r.getBoolean(2);
            }
            if (!adopted) try (var s = connection.prepareStatement("INSERT INTO online_calendar VALUES (1,1,?,?,?)")) {
                s.setLong(1, now); s.setLong(2, before.targetSteps()); s.setLong(3, completed); s.executeUpdate();
            }
            var resumed = new CivilizationTime.Anchor(Math.max(now, before.lastObservedMillis()), before.remainderMillis(),
                    adopted ? before.targetSteps() : completed);
            try (var s = connection.prepareStatement("UPDATE civilization_time SET last_observed_millis=?, target_steps=? WHERE id=1")) {
                s.setLong(1, resumed.lastObservedMillis()); s.setLong(2, resumed.targetSteps()); s.executeUpdate();
            }
            connection.commit(); connection.setAutoCommit(true); return resumed;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }

    /** World masks and earned time commit together, so bounded replay survives restarts and world changes. */
    public CivilizationTime.Accrual accrueOnline(long now, long cadence, Set<UUID> worlds) throws SQLException {
        return accrue(now, cadence, worlds.isEmpty() ? 0 : Long.MAX_VALUE, Set.copyOf(worlds));
    }

    public LongFunction<Set<UUID>> activeWorlds() throws SQLException {
        requireIdle();
        record Range(long from, long through, Set<UUID> worlds) {}
        var ranges = new ArrayList<Range>();
        try (var s = connection.createStatement(); var r = s.executeQuery("SELECT * FROM population_active_worlds WHERE through_step > (SELECT completed_steps FROM demographic_clock WHERE id=1) ORDER BY from_step")) {
            while (r.next()) {
                var ids = new HashSet<UUID>();
                for (var id : r.getString("worlds").split(",")) if (!id.isEmpty()) ids.add(UUID.fromString(id));
                ranges.add(new Range(r.getLong("from_step"), r.getLong("through_step"), Set.copyOf(ids)));
            }
        }
        return step -> ranges.stream().filter(r -> r.from() <= step && r.through() >= step).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing saved active-world mask for month " + step)).worlds();
    }

    private CivilizationTime.Accrual accrue(long nowMillis, long millisPerStep, long maximumElapsedMillis, Set<UUID> worlds) throws SQLException {
        requireIdle();
        try (var s = connection.createStatement(); var r = s.executeQuery("SELECT EXISTS(SELECT 1 FROM online_calendar)")) {
            boolean online = r.getBoolean(1);
            if (online != (worlds != null)) throw new SQLException("Calendar mode differs; live calendar requires saved world masks");
        }
        connection.setAutoCommit(false);
        try {
            var before = read(millisPerStep).orElseThrow(() -> new SQLException("Civilization clock not initialized"));
            var result = CivilizationTime.accrue(before, nowMillis, millisPerStep, maximumElapsedMillis);
            if (worlds != null && result.addedSteps() > 0) {
                String encoded = worlds.stream().map(UUID::toString).sorted().collect(java.util.stream.Collectors.joining(","));
                int extended;
                try (var s = connection.prepareStatement("UPDATE population_active_worlds SET through_step=? WHERE through_step=? AND worlds=?")) {
                    s.setLong(1, result.anchor().targetSteps()); s.setLong(2, before.targetSteps()); s.setString(3, encoded);
                    extended = s.executeUpdate();
                }
                if (extended == 0) try (var s = connection.prepareStatement("INSERT INTO population_active_worlds VALUES (?,?,?)")) {
                    s.setLong(1, Math.incrementExact(before.targetSteps())); s.setLong(2, result.anchor().targetSteps()); s.setString(3, encoded); s.executeUpdate();
                }
            }
            try (var statement = connection.prepareStatement("""
                    UPDATE civilization_time SET last_observed_millis = ?, remainder_millis = ?, target_steps = ?
                    WHERE id = 1 AND last_observed_millis = ? AND remainder_millis = ? AND target_steps = ?
                    """)) {
                statement.setLong(1, result.anchor().lastObservedMillis());
                statement.setLong(2, result.anchor().remainderMillis());
                statement.setLong(3, result.anchor().targetSteps());
                statement.setLong(4, before.lastObservedMillis());
                statement.setLong(5, before.remainderMillis());
                statement.setLong(6, before.targetSteps());
                if (statement.executeUpdate() != 1) throw new SQLException("Civilization time changed during accrual");
            }
            connection.commit();
            connection.setAutoCommit(true);
            return result;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            throw failure;
        }
    }

    private Optional<CivilizationTime.Anchor> read(long millisPerStep) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                SELECT t.*, d.completed_steps FROM civilization_time t
                JOIN demographic_clock d ON d.id = t.id WHERE t.id = 1
                """)) {
            if (!result.next()) return Optional.empty();
            if (result.getLong("millis_per_step") != millisPerStep)
                throw new SQLException("Civilization cadence changed; explicit transition required");
            if (result.getLong("completed_steps") > result.getLong("target_steps"))
                throw new SQLException("Demographic progress exceeds the queued calendar target");
            return Optional.of(new CivilizationTime.Anchor(result.getLong("last_observed_millis"),
                    result.getLong("remainder_millis"), result.getLong("target_steps")));
        }
    }

    private void requireIdle() throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Civilization time requires no active transaction");
    }
}
