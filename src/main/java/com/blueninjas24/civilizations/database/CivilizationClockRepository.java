package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.simulation.CivilizationTime;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

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
        requireIdle();
        connection.setAutoCommit(false);
        try {
            var before = read(millisPerStep).orElseThrow(() -> new SQLException("Civilization clock not initialized"));
            var result = CivilizationTime.accrue(before, nowMillis, millisPerStep, maximumElapsedMillis);
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
