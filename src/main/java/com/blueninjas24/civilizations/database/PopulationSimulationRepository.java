package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.PopulationAging;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.simulation.SimulationCheckpoint;
import com.blueninjas24.civilizations.simulation.SimulationRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Manager-owned, single-thread persistence for the explicit age-only replay convention. */
public final class PopulationSimulationRepository {
    public static final int AGING_REPLAY_VERSION = 1;
    private final Connection connection;
    private final PopulationRepository people;

    PopulationSimulationRepository(Connection connection) throws SQLException {
        this.people = new PopulationRepository(connection);
        this.connection = connection;
    }

    /** Explicit opt-in; never creates people, overwrites a seed or starts a scheduler. */
    public void initializeClock(UUID settlementId, long seed) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO population_clocks (settlement_id, seed, completed_steps, replay_version)
                VALUES (?, ?, 0, ?)
                """)) {
            statement.setString(1, settlementId.toString());
            statement.setLong(2, seed);
            statement.setInt(3, AGING_REPLAY_VERSION);
            statement.executeUpdate();
        }
    }

    /** Snapshot under one read transaction, so another connection cannot mix clock and people. */
    public Optional<SimulationCheckpoint<List<Resident>>> load(UUID settlementId) throws SQLException {
        begin();
        try {
            var checkpoint = read(settlementId);
            connection.commit();
            connection.setAutoCommit(true);
            return checkpoint;
        } catch (SQLException | RuntimeException failure) {
            rollback(failure);
            throw failure;
        }
    }

    /** Ages and checkpoint commit together; failure rolls back both. Budget counts steps, not CPU. */
    public SimulationRunner.Batch<List<Resident>> advanceAging(UUID settlementId, long targetSteps,
                                                              int maximumSteps) throws SQLException {
        begin();
        try {
            var before = read(settlementId).orElseThrow(
                    () -> new SQLException("Population clock not initialized: " + settlementId));
            var batch = SimulationRunner.advance(before, targetSteps, maximumSteps,
                    (state, context) -> PopulationAging.step(state));
            if (batch.processedSteps() > 0) {
                for (Resident resident : batch.checkpoint().state()) {
                    people.updateResident(resident);
                }
                try (var statement = connection.prepareStatement("""
                        UPDATE population_clocks SET completed_steps = ?
                        WHERE settlement_id = ? AND completed_steps = ? AND seed = ? AND replay_version = ?
                        """)) {
                    statement.setLong(1, batch.checkpoint().completedSteps());
                    statement.setString(2, settlementId.toString());
                    statement.setLong(3, before.completedSteps());
                    statement.setLong(4, before.seed());
                    statement.setInt(5, AGING_REPLAY_VERSION);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Population checkpoint changed during aging");
                    }
                }
            }
            connection.commit();
            connection.setAutoCommit(true);
            return batch;
        } catch (SQLException | RuntimeException failure) {
            rollback(failure);
            throw failure;
        }
    }

    private Optional<SimulationCheckpoint<List<Resident>>> read(UUID settlementId) throws SQLException {
        long seed;
        long completed;
        try (var statement = connection.prepareStatement("SELECT * FROM population_clocks WHERE settlement_id = ?")) {
            statement.setString(1, settlementId.toString());
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                if (result.getInt("replay_version") != AGING_REPLAY_VERSION) {
                    throw new SQLException("Unsupported population replay version: " + result.getInt("replay_version"));
                }
                seed = result.getLong("seed");
                completed = result.getLong("completed_steps");
            }
        }
        return Optional.of(new SimulationCheckpoint<>(seed, completed, people.findResidents(settlementId)));
    }

    private void begin() throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("Population simulation requires no active transaction");
        }
        connection.setAutoCommit(false);
    }

    private void rollback(Exception failure) {
        try {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
