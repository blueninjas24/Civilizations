package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Owner-thread durable physical incarnations. Missing/unloaded entities never imply death. */
public final class ResidentProjectionRepository {
    public enum State { CLAIMED, ACTIVE, RETIRED }
    public record Projection(UUID residentId, UUID worldId, UUID entityId, State state, UUID householdId, Position position) {}
    public record EntityKey(UUID worldId, UUID entityId) {}
    public record Home(UUID worldId, Position position) {}
    public record Snapshot(Map<UUID, Projection> projections, Map<EntityKey, UUID> aliases, Map<UUID, Home> homes) {
        public Snapshot { projections = Map.copyOf(projections); aliases = Map.copyOf(aliases); homes = Map.copyOf(homes); }
        public static Snapshot empty() { return new Snapshot(Map.of(), Map.of(), Map.of()); }
    }
    public record PhysicalDeath(long sequence, UUID residentId, long completedMonth, long observedMillis, String reason) {}
    private final Connection connection;
    ResidentProjectionRepository(Connection connection) { this.connection = connection; }

    public Snapshot snapshot() throws SQLException {
        var projections = new HashMap<UUID, Projection>();
        var aliases = new HashMap<EntityKey, UUID>();
        var homes = new HashMap<UUID, Home>();
        try (var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT * FROM resident_projection ORDER BY resident_id")) {
                while (result.next()) {
                    UUID person = uuid(result.getString("resident_id"));
                    var position = result.getObject("x") == null ? null : new Position(result.getInt("x"), result.getInt("y"), result.getInt("z"));
                    projections.put(person, new Projection(person, uuid(result.getString("world_id")), uuid(result.getString("entity_id")),
                            State.valueOf(result.getString("state")), uuid(result.getString("household_id")), position));
                }
            }
            try (var result = statement.executeQuery("SELECT * FROM village_entities")) {
                while (result.next()) aliases.put(new EntityKey(uuid(result.getString("world_id")), uuid(result.getString("entity_id"))), uuid(result.getString("resident_id")));
            }
            try (var result = statement.executeQuery("SELECT * FROM village_beds WHERE household_id IS NOT NULL ORDER BY world_id, x, y, z")) {
                while (result.next()) homes.putIfAbsent(uuid(result.getString("household_id")),
                        new Home(uuid(result.getString("world_id")), new Position(result.getInt("x"), result.getInt("y"), result.getInt("z"))));
            }
        }
        return new Snapshot(projections, aliases, homes);
    }

    /** Called inside enrollment's transaction; never replaces an existing or ambiguous identity. */
    void adoptUnambiguousImports() throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO resident_projection(resident_id, world_id, entity_id, state, household_id)
                    SELECT e.resident_id, e.world_id, e.entity_id, 'ACTIVE', r.household_id
                    FROM village_entities e JOIN residents r ON r.id = e.resident_id
                    WHERE NOT EXISTS (SELECT 1 FROM resident_projection p WHERE p.resident_id = r.id)
                    AND (SELECT COUNT(*) FROM village_entities other WHERE other.resident_id = r.id) = 1
                    """);
        }
    }

    /** Commit a claim before adding the unspawned Paper entity to its world. */
    public boolean claim(UUID person, UUID world, UUID entity, UUID household, Position position) throws SQLException {
        Objects.requireNonNull(person); Objects.requireNonNull(world); Objects.requireNonNull(entity);
        Objects.requireNonNull(household); Objects.requireNonNull(position);
        return transaction(() -> {
            try (var statement = connection.prepareStatement("""
                    SELECT 1 FROM residents r JOIN settlement_worlds w ON w.settlement_id = r.settlement_id
                    WHERE r.id = ? AND r.status = 'ALIVE' AND r.household_id = ? AND w.world_id = ?
                    AND EXISTS (SELECT 1 FROM village_beds WHERE household_id = r.household_id AND world_id = w.world_id)
                    """)) {
                statement.setString(1, person.toString()); statement.setString(2, household.toString()); statement.setString(3, world.toString());
                try (var result = statement.executeQuery()) { if (!result.next()) return false; }
            }
            try (var statement = connection.prepareStatement("""
                    SELECT state FROM resident_projection WHERE resident_id = ?
                    """)) {
                statement.setString(1, person.toString());
                try (var result = statement.executeQuery()) {
                    if (result.next()) { if (!result.getString(1).equals("RETIRED")) return false; }
                    else {
                        // Unresolved historic aliases are a conflict, not permission to invent a copy.
                        try (var count = connection.prepareStatement("SELECT 1 FROM village_entities WHERE resident_id = ? LIMIT 1")) {
                            count.setString(1, person.toString());
                            try (var aliases = count.executeQuery()) { if (aliases.next()) return false; }
                        }
                    }
                }
            }
            if (new VillageIdentityRepository(connection).findResident(world, entity).isPresent()) return false;
            new VillageIdentityRepository(connection).bindEntity(world, entity, person);
            try (var statement = connection.prepareStatement("""
                    INSERT INTO resident_projection(resident_id, world_id, entity_id, state, household_id, x, y, z)
                    VALUES(?, ?, ?, 'CLAIMED', ?, ?, ?, ?)
                    ON CONFLICT(resident_id) DO UPDATE SET world_id = excluded.world_id, entity_id = excluded.entity_id,
                        state = excluded.state, household_id = excluded.household_id, x = excluded.x, y = excluded.y, z = excluded.z
                    """)) {
                statement.setString(1, person.toString()); statement.setString(2, world.toString()); statement.setString(3, entity.toString());
                statement.setString(4, household.toString()); statement.setInt(5, position.x()); statement.setInt(6, position.y()); statement.setInt(7, position.z());
                statement.executeUpdate();
            }
            return true;
        });
    }

    /** Stale callbacks cannot activate/retire a newer incarnation. Activation also checks life/home. */
    public boolean activate(UUID person, UUID entity) throws SQLException {
        try (var statement = connection.prepareStatement("""
                UPDATE resident_projection SET state = 'ACTIVE' WHERE resident_id = ? AND entity_id = ? AND state = 'CLAIMED'
                AND EXISTS (SELECT 1 FROM residents r JOIN settlement_worlds w ON w.settlement_id = r.settlement_id
                    WHERE r.id = resident_id AND r.status = 'ALIVE' AND r.household_id = resident_projection.household_id
                    AND w.world_id = resident_projection.world_id)
                """)) {
            statement.setString(1, person.toString()); statement.setString(2, entity.toString()); return statement.executeUpdate() == 1;
        }
    }
    public boolean retire(UUID person, UUID entity) throws SQLException {
        try (var statement = connection.prepareStatement("UPDATE resident_projection SET state = 'RETIRED' WHERE resident_id = ? AND entity_id = ? AND state <> 'RETIRED'")) {
            statement.setString(1, person.toString()); statement.setString(2, entity.toString()); return statement.executeUpdate() == 1;
        }
    }

    /** Actual death, never chunk disappearance. Identity, age and family history are retained. */
    public boolean recordPhysicalDeath(UUID world, UUID entity, long observedMillis, String reason) throws SQLException {
        if (observedMillis < 0 || reason == null || reason.isBlank() || reason.length() > 120)
            throw new IllegalArgumentException("Invalid physical death observation");
        return transaction(() -> {
            UUID person;
            try (var statement = connection.prepareStatement("""
                    SELECT p.resident_id FROM resident_projection p JOIN residents r ON r.id = p.resident_id
                    WHERE p.world_id = ? AND p.entity_id = ? AND p.state IN ('ACTIVE', 'CLAIMED') AND r.status = 'ALIVE'
                    """)) {
                statement.setString(1, world.toString()); statement.setString(2, entity.toString());
                try (var result = statement.executeQuery()) { if (!result.next()) return false; person = uuid(result.getString(1)); }
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO resident_lifecycle_events(resident_id, completed_month, observed_millis, reason)
                    SELECT ?, completed_steps, ?, ? FROM demographic_clock WHERE id = 1
                    """)) {
                statement.setString(1, person.toString()); statement.setLong(2, observedMillis); statement.setString(3, reason);
                if (statement.executeUpdate() != 1) throw new SQLException("Physical death requires an initialized population clock");
            }
            try (var statement = connection.prepareStatement("UPDATE residents SET status = 'DECEASED' WHERE id = ?")) {
                statement.setString(1, person.toString()); statement.executeUpdate();
            }
            retire(person, entity);
            DemographicRepository.synchronizeSettlementCounts(connection);
            return true;
        });
    }
    public List<PhysicalDeath> recentDeaths(int limit) throws SQLException {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid death page");
        var events = new ArrayList<PhysicalDeath>();
        try (var statement = connection.prepareStatement("SELECT * FROM resident_lifecycle_events ORDER BY sequence DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (var result = statement.executeQuery()) {
                while (result.next()) events.add(new PhysicalDeath(result.getLong("sequence"), uuid(result.getString("resident_id")),
                        result.getLong("completed_month"), result.getLong("observed_millis"), result.getString("reason")));
            }
        }
        return List.copyOf(events);
    }
    @FunctionalInterface private interface Operation<T> { T run() throws SQLException; }
    private <T> T transaction(Operation<T> operation) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Projection work requires no active transaction");
        connection.setAutoCommit(false);
        try {
            T result = operation.run(); connection.commit(); connection.setAutoCommit(true); return result;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
    private static UUID uuid(String value) { return value == null ? null : UUID.fromString(value); }
}
