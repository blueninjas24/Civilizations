package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.VillageObservation;
import com.blueninjas24.civilizations.settlement.VillageRecognition;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

/** Saved identity evidence; no proximity-based adoption, census overwrite or implicit town merge. */
public final class VillageIdentityRepository {
    private final Connection connection;
    VillageIdentityRepository(Connection connection) { this.connection = connection; }
    public enum MatchType { NEW, EXISTING, CONFLICT }
    public record Match(MatchType type, List<UUID> settlementIds) {
        public Match { settlementIds = List.copyOf(settlementIds); }
    }

    public void observeWorld(UUID worldId, String currentName) throws SQLException {
        if (currentName == null || currentName.isBlank()) throw new IllegalArgumentException("World name required");
        try (var statement = connection.prepareStatement("""
                INSERT INTO observed_worlds(id, name) VALUES(?, ?)
                ON CONFLICT(id) DO UPDATE SET name = excluded.name
                """)) {
            statement.setString(1, worldId.toString()); statement.setString(2, currentName);
            statement.executeUpdate();
        }
    }

    public Optional<UUID> findWorld(UUID settlementId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id = ?")) {
            statement.setString(1, settlementId.toString());
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(UUID.fromString(result.getString(1))) : Optional.empty();
            }
        }
    }

    /** Idempotent binding; a different world cannot replace saved identity. */
    public void bindSettlement(UUID settlementId, UUID worldId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO settlement_worlds(settlement_id, world_id) VALUES(?, ?)
                ON CONFLICT(settlement_id) DO UPDATE SET world_id = excluded.world_id
                WHERE settlement_worlds.world_id = excluded.world_id
                """)) {
            statement.setString(1, settlementId.toString()); statement.setString(2, worldId.toString());
            if (statement.executeUpdate() != 1) throw new SQLException("Settlement belongs to another world");
        }
    }

    public void bindBed(UUID worldId, VillageObservation.Position bed, UUID settlementId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO village_beds(world_id, x, y, z, settlement_id) VALUES(?, ?, ?, ?, ?)
                ON CONFLICT(world_id, x, y, z) DO UPDATE SET settlement_id = excluded.settlement_id
                WHERE village_beds.settlement_id = excluded.settlement_id
                """)) {
            statement.setString(1, worldId.toString()); statement.setInt(2, bed.x());
            statement.setInt(3, bed.y()); statement.setInt(4, bed.z()); statement.setString(5, settlementId.toString());
            if (statement.executeUpdate() != 1) throw new SQLException("Bed is already linked to another settlement");
        }
    }

    /** New projection evidence must reference a resident belonging to this bound world. */
    public void bindEntity(UUID worldId, UUID entityId, UUID residentId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO village_entities(world_id, entity_id, resident_id)
                SELECT ?, ?, r.id FROM residents r JOIN settlement_worlds w ON w.settlement_id = r.settlement_id
                WHERE r.id = ? AND w.world_id = ?
                ON CONFLICT(world_id, entity_id) DO UPDATE SET resident_id = excluded.resident_id
                WHERE village_entities.resident_id = excluded.resident_id
                """)) {
            statement.setString(1, worldId.toString()); statement.setString(2, entityId.toString());
            statement.setString(3, residentId.toString()); statement.setString(4, worldId.toString());
            if (statement.executeUpdate() != 1) throw new SQLException("Unknown/wrong-world resident or entity identity conflict");
        }
    }

    public Optional<UUID> findResident(UUID worldId, UUID entityId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT resident_id FROM village_entities WHERE world_id = ? AND entity_id = ?")) {
            statement.setString(1, worldId.toString()); statement.setString(2, entityId.toString());
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(UUID.fromString(result.getString(1))) : Optional.empty();
            }
        }
    }

    /** Read a coherent identity snapshot, joining a caller-owned import transaction if present. */
    public Match match(UUID worldId, VillageRecognition.Candidate candidate) throws SQLException {
        if (candidate.beds().size() > 512 || candidate.villagers().size() > 1024)
            throw new IllegalArgumentException("Candidate exceeds identity matching budget");
        boolean owned = connection.getAutoCommit();
        if (owned) connection.setAutoCommit(false);
        try {
            var identities = new TreeSet<String>();
            try (var statement = connection.prepareStatement("SELECT settlement_id FROM village_beds WHERE world_id = ? AND x = ? AND y = ? AND z = ?")) {
                for (var bed : candidate.beds()) {
                    statement.setString(1, worldId.toString()); statement.setInt(2, bed.x());
                    statement.setInt(3, bed.y()); statement.setInt(4, bed.z());
                    try (var result = statement.executeQuery()) { if (result.next()) identities.add(result.getString(1)); }
                }
            }
            try (var statement = connection.prepareStatement("""
                    SELECT r.settlement_id FROM village_entities e
                    JOIN residents r ON r.id = e.resident_id
                    WHERE e.world_id = ? AND e.entity_id = ?
                    """)) {
                for (var person : candidate.villagers()) {
                    statement.setString(1, worldId.toString()); statement.setString(2, person.entityId().toString());
                    try (var result = statement.executeQuery()) { if (result.next()) identities.add(result.getString(1)); }
                }
            }
            var ids = identities.stream().map(UUID::fromString).toList();
            var result = new Match(ids.isEmpty() ? MatchType.NEW : ids.size() == 1 ? MatchType.EXISTING : MatchType.CONFLICT, ids);
            if (owned) { connection.commit(); connection.setAutoCommit(true); }
            return result;
        } catch (SQLException | RuntimeException failure) {
            if (owned) {
                try { connection.rollback(); connection.setAutoCommit(true); }
                catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            }
            throw failure;
        }
    }
}
