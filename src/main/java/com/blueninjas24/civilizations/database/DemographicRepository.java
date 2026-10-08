package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.simulation.SimulationCheckpoint;
import com.blueninjas24.civilizations.simulation.SimulationRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Single owner-thread global monthly replay: people, links, events and clock commit together. */
public final class DemographicRepository {
    public static final int REPLAY_VERSION = 3;
    private final Connection connection;
    private final PopulationRepository people;

    DemographicRepository(Connection connection) throws SQLException {
        people = new PopulationRepository(connection);
        this.connection = connection;
    }

    public void initializeClock(long seed, PopulationPolicy policy) throws SQLException {
        initializeClock(seed, policy, MigrationPolicy.disabled());
    }

    public void initializeClock(long seed, PopulationPolicy policy, MigrationPolicy migration) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO demographic_clock(id, seed, completed_steps, replay_version, policy)
                VALUES(1, ?, 0, ?, ?)
                """)) {
            statement.setLong(1, seed);
            statement.setInt(2, REPLAY_VERSION);
            statement.setString(3, settingsKey(policy, migration));
            statement.executeUpdate();
        }
    }

    /** Explicit housing observation. Absence remains unknown; zero is a measured lack of capacity. */
    public void recordCapacity(UUID householdId, int capacity) throws SQLException {
        if (capacity < 0) throw new IllegalArgumentException("Capacity must be nonnegative");
        try (var statement = connection.prepareStatement("""
                INSERT INTO household_capacity(household_id, capacity) VALUES(?, ?)
                ON CONFLICT(household_id) DO UPDATE SET capacity = excluded.capacity
                """)) {
            statement.setString(1, householdId.toString());
            statement.setInt(2, capacity);
            statement.executeUpdate();
        }
    }

    public Optional<SimulationCheckpoint<PopulationState>> load(PopulationPolicy policy) throws SQLException {
        return load(policy, MigrationPolicy.disabled());
    }

    public Optional<SimulationCheckpoint<PopulationState>> load(PopulationPolicy policy, MigrationPolicy migration) throws SQLException {
        begin();
        try {
            var result = read(policy, migration);
            connection.commit();
            connection.setAutoCommit(true);
            return result;
        } catch (SQLException | RuntimeException failure) {
            rollback(failure);
            throw failure;
        }
    }

    private record Pending(PopulationState state, List<PopulationEvent> events) {
        Pending { events = List.copyOf(events); }
    }

    public SimulationRunner.Batch<PopulationState> advance(long targetSteps, int maximumSteps,
                                                           PopulationPolicy policy) throws SQLException {
        return advance(targetSteps, maximumSteps, policy, MigrationPolicy.disabled());
    }

    public SimulationRunner.Batch<PopulationState> advance(long targetSteps, int maximumSteps,
                                                           PopulationPolicy policy, MigrationPolicy migration) throws SQLException {
        begin();
        try {
            var before = read(policy, migration).orElseThrow(() -> new SQLException("Demographic clock not initialized"));
            var worlds = new HashMap<UUID, String>();
            try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                    SELECT s.id, COALESCE(w.world_id, 'legacy:' || s.world) AS world
                    FROM settlements s LEFT JOIN settlement_worlds w ON w.settlement_id = s.id
                    """)) {
                while (result.next()) worlds.put(UUID.fromString(result.getString("id")), result.getString("world"));
            }
            var pending = new SimulationCheckpoint<>(before.seed(), before.completedSteps(), new Pending(before.state(), List.of()));
            var batch = SimulationRunner.advance(pending, targetSteps, maximumSteps, (state, context) -> {
                var outcome = PopulationStep.advance(state.state(), context, policy);
                var moved = PopulationMigration.advance(outcome.state(), context, policy.adulthoodSteps(), migration, worlds);
                var events = new ArrayList<>(state.events());
                events.addAll(outcome.events());
                events.addAll(moved.events());
                return new Pending(moved.state(), events);
            });
            PopulationState after = batch.checkpoint().state().state();
            if (batch.processedSteps() > 0) {
                var originals = new HashMap<UUID, Resident>();
                before.state().residents().forEach(p -> originals.put(p.id(), p));
                for (Resident person : after.residents()) {
                    Resident original = originals.get(person.id());
                    if (original == null) people.insertResident(person);
                    else if (!original.settlementId().equals(person.settlementId())) people.updateMigratedResident(person, original.settlementId());
                    else if (!original.equals(person)) people.updateResident(person);
                }
                var originalTies = new HashSet<>(before.state().familyTies());
                for (FamilyTie tie : after.familyTies()) {
                    if (!originalTies.contains(tie)) people.insertFamilyTie(tie);
                }
                for (PopulationEvent event : batch.checkpoint().state().events()) insertEvent(event);
                synchronizeSettlementCounts(connection);
                try (var statement = connection.prepareStatement("""
                        UPDATE demographic_clock SET completed_steps = ?
                        WHERE id = 1 AND completed_steps = ? AND seed = ? AND replay_version = ? AND policy = ?
                        """)) {
                    statement.setLong(1, batch.checkpoint().completedSteps());
                    statement.setLong(2, before.completedSteps());
                    statement.setLong(3, before.seed());
                    statement.setInt(4, REPLAY_VERSION);
                    statement.setString(5, settingsKey(policy, migration));
                    if (statement.executeUpdate() != 1) throw new SQLException("Demographic clock changed during simulation");
                }
            }
            connection.commit();
            connection.setAutoCommit(true);
            return new SimulationRunner.Batch<>(new SimulationCheckpoint<>(before.seed(),
                    batch.checkpoint().completedSteps(), after), batch.processedSteps(), batch.remainingSteps());
        } catch (SQLException | RuntimeException failure) {
            rollback(failure);
            throw failure;
        }
    }

    public record StoredEvent(long sequence, PopulationEvent event) {}

    public List<StoredEvent> recentEvents(int limit) throws SQLException {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid event page");
        long cursor = 0;
        try (var statement = connection.prepareStatement("SELECT MIN(sequence) FROM (SELECT sequence FROM population_events ORDER BY sequence DESC LIMIT ?)")) {
            statement.setInt(1, limit);
            try (var result = statement.executeQuery()) { if (result.next()) cursor = Math.max(0, result.getLong(1) - 1); }
        }
        return findEvents(cursor, limit);
    }

    /** Preserve legacy census rows until actual persistent residents have been enrolled. */
    static void synchronizeSettlementCounts(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE settlements SET population = (
                        SELECT COUNT(*) FROM residents WHERE settlement_id = settlements.id AND status = 'ALIVE'
                    ) WHERE EXISTS (SELECT 1 FROM residents WHERE settlement_id = settlements.id)
                        OR EXISTS (SELECT 1 FROM settlement_worlds WHERE settlement_id = settlements.id)
                    """);
        }
    }

    /** Sequence cursor avoids losing events when a page splits one monthly step. */
    public List<StoredEvent> findEvents(long afterSequence, int limit) throws SQLException {
        if (afterSequence < 0 || limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid event page");
        try (var statement = connection.prepareStatement("SELECT * FROM population_events WHERE sequence > ? ORDER BY sequence LIMIT ?")) {
            statement.setLong(1, afterSequence);
            statement.setInt(2, limit);
            var events = new ArrayList<StoredEvent>();
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    String first = result.getString("first_relative");
                    List<UUID> related = first == null ? List.of() : List.of(UUID.fromString(first),
                            UUID.fromString(result.getString("second_relative")));
                    events.add(new StoredEvent(result.getLong("sequence"), new PopulationEvent(result.getLong("step"),
                            PopulationEvent.Type.valueOf(result.getString("type")), UUID.fromString(result.getString("resident_id")),
                            UUID.fromString(result.getString("settlement_id")), related, result.getString("reason"),
                            result.getString("source_settlement_id") == null ? null : UUID.fromString(result.getString("source_settlement_id")))));
                }
            }
            return List.copyOf(events);
        }
    }

    private Optional<SimulationCheckpoint<PopulationState>> read(PopulationPolicy policy, MigrationPolicy migration) throws SQLException {
        long seed;
        long steps;
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT * FROM demographic_clock WHERE id = 1")) {
            if (!result.next()) return Optional.empty();
            if (result.getInt("replay_version") != REPLAY_VERSION || !settingsKey(policy, migration).equals(result.getString("policy"))) {
                throw new SQLException("Demographic replay version/settings mismatch; explicit transition required");
            }
            seed = result.getLong("seed");
            steps = result.getLong("completed_steps");
        }
        return Optional.of(new SimulationCheckpoint<>(seed, steps, readPopulationState()));
    }

    /** Caller holds the owning connection/transaction; also used by atomic village enrollment. */
    PopulationState readPopulationState() throws SQLException {
        var residents = new ArrayList<Resident>();
        var households = new ArrayList<Household>();
        var ties = new ArrayList<FamilyTie>();
        var capacities = new HashMap<UUID, Integer>();
        try (var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT * FROM residents ORDER BY id")) {
                while (result.next()) residents.add(PopulationRepository.readResident(result));
            }
            try (var result = statement.executeQuery("SELECT * FROM households ORDER BY id")) {
                while (result.next()) households.add(new Household(UUID.fromString(result.getString("id")),
                        UUID.fromString(result.getString("settlement_id")), result.getString("name")));
            }
            try (var result = statement.executeQuery("SELECT * FROM family_ties ORDER BY type, first_id, second_id")) {
                while (result.next()) ties.add(new FamilyTie(UUID.fromString(result.getString("first_id")),
                        UUID.fromString(result.getString("second_id")), FamilyTie.Type.valueOf(result.getString("type"))));
            }
            try (var result = statement.executeQuery("SELECT * FROM household_capacity")) {
                while (result.next()) capacities.put(UUID.fromString(result.getString("household_id")), result.getInt("capacity"));
            }
        }
        return new PopulationState(residents, households, ties, capacities);
    }

    private void insertEvent(PopulationEvent event) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO population_events(step, type, resident_id, settlement_id, first_relative, second_relative, reason, source_settlement_id)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, event.step());
            statement.setString(2, event.type().name());
            statement.setString(3, event.residentId().toString());
            statement.setString(4, event.settlementId().toString());
            if (event.relatedResidents().isEmpty()) {
                statement.setNull(5, Types.VARCHAR);
                statement.setNull(6, Types.VARCHAR);
            } else {
                statement.setString(5, event.relatedResidents().get(0).toString());
                statement.setString(6, event.relatedResidents().get(1).toString());
            }
            statement.setString(7, event.reason());
            if (event.sourceSettlementId() == null) statement.setNull(8, Types.VARCHAR);
            else statement.setString(8, event.sourceSettlementId().toString());
            statement.executeUpdate();
        }
    }

    private static String settingsKey(PopulationPolicy policy, MigrationPolicy migration) {
        return policy.replayKey() + ";" + migration.replayKey();
    }

    private void begin() throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Demographic simulation requires no active transaction");
        connection.setAutoCommit(false);
    }

    private void rollback(Exception failure) {
        try {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
    }
}
