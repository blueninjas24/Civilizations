package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.MineralObservation;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Durable positive evidence, not a reserve, a land claim or permission to found a mine. */
public final class MineralOpportunities {
    public record Evidence(UUID world, Position position, Commodity mineral, long firstSeenMillis,
                           long lastSeenMillis, boolean excluded, UUID savedDeposit) {}
    private final Connection connection;
    public MineralOpportunities(Connection connection) { this.connection = Objects.requireNonNull(connection); }

    /** Validate and save one bounded survey atomically. Missing observations never erase saved evidence. */
    public List<MineralSurveyReader.Patch> observe(UUID town, List<MineralObservation> observations) throws SQLException {
        observations = List.copyOf(observations);
        if (!connection.getAutoCommit()) throw new SQLException("Opportunity observation requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var patches = new MineralSurveyReader(connection).read(town, observations);
            try (var insert = connection.prepareStatement("""
                    INSERT INTO mineral_opportunities(settlement_id,world_id,x,y,z,mineral,first_seen_millis,last_seen_millis)
                    VALUES(?,?,?,?,?,?,?,?)
                    ON CONFLICT(settlement_id,world_id,x,y,z) DO UPDATE SET
                    last_seen_millis=MAX(last_seen_millis,excluded.last_seen_millis)
                    WHERE mineral=excluded.mineral
                    """)) {
                for (var patch : patches) for (var p : patch.evidence().ore()) {
                    var evidence = patch.evidence();
                    insert.setString(1, town.toString()); insert.setString(2, evidence.world().toString());
                    insert.setInt(3, p.x()); insert.setInt(4, p.y()); insert.setInt(5, p.z());
                    insert.setString(6, evidence.mineral().id());
                    insert.setLong(7, evidence.observedMillis()); insert.setLong(8, evidence.observedMillis());
                    if (insert.executeUpdate() != 1) throw new SQLException("Observed mineral conflicts with retained opportunity evidence");
                }
            }
            connection.commit(); connection.setAutoCommit(true); return patches;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }

    /** Bounded diagnostic page, ordered by coordinate; exclusions/claims are derived from current saved truth. */
    public List<Evidence> read(UUID town, int limit) throws SQLException {
        Objects.requireNonNull(town);
        if (limit < 1 || limit > 512) throw new IllegalArgumentException("Opportunity page limit must be 1..512");
        var result = new ArrayList<Evidence>();
        try (var query = connection.prepareStatement("""
                SELECT o.*,
                  EXISTS(SELECT 1 FROM player_ore_placements p WHERE p.world_id=o.world_id AND p.x=o.x AND p.y=o.y AND p.z=o.z)
                  OR EXISTS(SELECT 1 FROM ore_source_removals r WHERE r.world_id=o.world_id AND r.x=o.x AND r.y=o.y AND r.z=o.z) AS excluded,
                  (SELECT e.deposit_id FROM mineral_evidence e WHERE e.world_id=o.world_id AND e.x=o.x AND e.y=o.y AND e.z=o.z) AS deposit
                FROM mineral_opportunities o WHERE settlement_id=? ORDER BY world_id,x,y,z LIMIT ?
                """)) {
            query.setString(1, town.toString()); query.setInt(2, limit);
            try (var row = query.executeQuery()) {
                while (row.next()) result.add(new Evidence(UUID.fromString(row.getString("world_id")),
                        new Position(row.getInt("x"), row.getInt("y"), row.getInt("z")),
                        new Commodity(row.getString("mineral")),
                        row.getLong("first_seen_millis"), row.getLong("last_seen_millis"), row.getBoolean("excluded"),
                        row.getString("deposit") == null ? null : UUID.fromString(row.getString("deposit"))));
            }
        }
        return List.copyOf(result);
    }
}
