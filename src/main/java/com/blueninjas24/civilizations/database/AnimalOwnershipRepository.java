package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;
import com.blueninjas24.civilizations.industry.LivestockAdoption;

/** Immutable initial ownership provenance. An observation is never an ownership transfer.
 * Trusted callers must establish animal identity, protection/authority and adoption eligibility.
 * No server command/listener currently exposes registration. Use on the SQL owner thread. */
public final class AnimalOwnershipRepository {
    public record Claim(UUID animal, UUID originWorld, LivestockAdoption.Ownership kind,
                        UUID player, UUID settlement, long recordedMillis, String source) {
        public Claim {
            Objects.requireNonNull(animal); Objects.requireNonNull(originWorld);
            Objects.requireNonNull(kind); Objects.requireNonNull(source);
            if (recordedMillis < 0 || source.isBlank() || source.length() > 256
                    || kind == LivestockAdoption.Ownership.UNCLAIMED
                    || kind == LivestockAdoption.Ownership.PLAYER && (player == null || settlement != null)
                    || kind == LivestockAdoption.Ownership.VILLAGE && (player != null || settlement == null))
                throw new IllegalArgumentException("Invalid initial animal ownership");
        }
    }
    private final Connection connection;
    public AnimalOwnershipRepository(Connection connection) { this.connection = Objects.requireNonNull(connection); }
    public Set<UUID> villageAnimals() throws SQLException {
        var animals=new HashSet<UUID>();
        try(var s=connection.createStatement();var r=s.executeQuery("SELECT animal_id FROM animal_ownership WHERE kind='VILLAGE' ORDER BY animal_id")){
            while(r.next())animals.add(UUID.fromString(r.getString(1)));
        }
        return Set.copyOf(animals);
    }
    public Optional<Claim> load(UUID animal) throws SQLException {
        Objects.requireNonNull(animal);
        try (var s = connection.prepareStatement("SELECT * FROM animal_ownership WHERE animal_id=?")) {
            s.setString(1, animal.toString());
            try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                String player = r.getString("player_id"), town = r.getString("settlement_id");
                return Optional.of(new Claim(animal, UUID.fromString(r.getString("world_id")),
                        LivestockAdoption.Ownership.valueOf(r.getString("kind")),
                        player == null ? null : UUID.fromString(player), town == null ? null : UUID.fromString(town),
                        r.getLong("recorded_millis"), r.getString("source")));
            }
        }
    }
    /** Exact retries replay; any changed provenance/owner refuses rather than silently stealing.
     * Future authorized donations need a distinct audited transfer API, not this method. */
    public Claim record(Claim claim) throws SQLException {
        Objects.requireNonNull(claim);
        if (!connection.getAutoCommit()) throw new SQLException("Animal ownership requires no active transaction");
        connection.setAutoCommit(false);
        try {
            var result = recordRows(claim);
            connection.commit(); connection.setAutoCommit(true);
            return result;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
    /** Composable with a future pen/animal observation transaction. Caller owns commit/rollback. */
    Claim recordRows(Claim claim) throws SQLException {
        Objects.requireNonNull(claim);
        if (connection.getAutoCommit()) throw new SQLException("Animal ownership rows require an owner transaction");
        var existing = load(claim.animal());
        if (existing.isPresent()) {
            if (!existing.orElseThrow().equals(claim)) throw new SQLException("Animal already has different recorded ownership/provenance");
            return existing.orElseThrow();
        }
        try (var s = connection.prepareStatement("""
                INSERT INTO animal_ownership(animal_id,world_id,kind,player_id,settlement_id,recorded_millis,source)
                VALUES(?,?,?,?,?,?,?)
                """)) {
            s.setString(1, claim.animal().toString()); s.setString(2, claim.originWorld().toString());
            s.setString(3, claim.kind().name());
            if (claim.player() == null) s.setNull(4, Types.VARCHAR); else s.setString(4, claim.player().toString());
            if (claim.settlement() == null) s.setNull(5, Types.VARCHAR); else s.setString(5, claim.settlement().toString());
            s.setLong(6, claim.recordedMillis()); s.setString(7, claim.source()); s.executeUpdate();
        }
        return claim;
    }
}
