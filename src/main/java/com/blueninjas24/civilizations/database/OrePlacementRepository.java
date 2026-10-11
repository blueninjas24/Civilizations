package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Sticky placement exclusions. Only immutable observations cross into the SQL owner's connection. */
public final class OrePlacementRepository {
    private final Connection connection;
    public OrePlacementRepository(Connection connection) { this.connection = Objects.requireNonNull(connection); }

    /** World must already be observed. Replay cannot erase or replace the original placement timestamp. */
    public int record(UUID world, Collection<Position> positions, long observedMillis) throws SQLException {
        Objects.requireNonNull(world);
        var evidence = List.copyOf(positions);
        if (observedMillis < 0 || evidence.isEmpty() || evidence.size() > 512
                || new HashSet<>(evidence).size() != evidence.size())
            throw new IllegalArgumentException("Invalid bounded placement observation");
        if (!connection.getAutoCommit()) throw new SQLException("Placement recording requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            int added = recordRows(world,evidence,observedMillis);
            connection.commit(); connection.setAutoCommit(true); return added;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }

    int recordRows(UUID world,Collection<Position> positions,long observedMillis)throws SQLException {
        Objects.requireNonNull(world);var evidence=List.copyOf(positions);
        if(observedMillis<0||evidence.isEmpty()||evidence.size()>512||new HashSet<>(evidence).size()!=evidence.size())throw new IllegalArgumentException("Invalid bounded placement observation");
        if(connection.getAutoCommit())throw new SQLException("Placement rows require owner transaction");
        int added = 0;
        try (var statement = connection.prepareStatement("""
                INSERT INTO player_ore_placements(world_id,x,y,z,observed_millis) VALUES(?,?,?,?,?)
                ON CONFLICT(world_id,x,y,z) DO NOTHING
                """)) {
            for (var position : evidence) {
                bind(statement,world,position); statement.setLong(5,observedMillis);
                added += statement.executeUpdate();
            }
        }
        return added;
    }

    public boolean excludes(UUID world, Position position) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT 1 FROM (SELECT world_id,x,y,z FROM player_ore_placements UNION ALL SELECT world_id,x,y,z FROM ore_source_removals) WHERE world_id=? AND x=? AND y=? AND z=? LIMIT 1")) {
            bind(statement,world,position);
            try (var result = statement.executeQuery()) { return result.next(); }
        }
    }

    /** Bounded immutable chunk evidence. Overflow fails closed; no truncated exclusion set is returned. */
    public Set<Position> inChunk(UUID world, int chunkX, int chunkZ, int maximumPositions) throws SQLException {
        Objects.requireNonNull(world);
        if (maximumPositions < 1 || maximumPositions > 65_536) throw new IllegalArgumentException("Invalid exclusion read budget");
        int x = Math.multiplyExact(chunkX,16), z = Math.multiplyExact(chunkZ,16);
        int endX = Math.addExact(x,15), endZ = Math.addExact(z,15);
        try (var statement = connection.prepareStatement("""
                SELECT x,y,z FROM (SELECT world_id,x,y,z FROM player_ore_placements UNION SELECT world_id,x,y,z FROM ore_source_removals)
                WHERE world_id=? AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?
                ORDER BY x,y,z LIMIT ?
                """)) {
            statement.setString(1,world.toString()); statement.setInt(2,x); statement.setInt(3,endX);
            statement.setInt(4,z); statement.setInt(5,endZ); statement.setInt(6,maximumPositions+1);
            var positions = new HashSet<Position>();
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    if (positions.size() == maximumPositions) throw new SQLException("Chunk placement evidence exceeds read budget; discovery blocked");
                    positions.add(new Position(result.getInt(1),result.getInt(2),result.getInt(3)));
                }
            }
            return Set.copyOf(positions);
        }
    }

    private static void bind(PreparedStatement statement, UUID world, Position position) throws SQLException {
        Objects.requireNonNull(world); Objects.requireNonNull(position);
        statement.setString(1,world.toString()); statement.setInt(2,position.x());
        statement.setInt(3,position.y()); statement.setInt(4,position.z());
    }
}
