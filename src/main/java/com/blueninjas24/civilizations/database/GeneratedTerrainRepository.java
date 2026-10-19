package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.VillageObservation;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Records newly generated terrain observed during an uninterrupted resource capture. */
public final class GeneratedTerrainRepository {
    public static final int MAX_COVERAGE_POSITIONS = 65_536;
    private static final int MIN_CHUNK = Math.floorDiv(Integer.MIN_VALUE, 16);
    private static final int MAX_CHUNK = Math.floorDiv(Integer.MAX_VALUE, 16);

    private record Chunk(int x, int z) {}

    private final Connection connection;

    public GeneratedTerrainRepository(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    public static void createSchema(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE generated_resource_chunks(
                        kind TEXT NOT NULL CHECK(kind IN ('ore','forest')),
                        world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                        chunk_x INTEGER NOT NULL CHECK(typeof(chunk_x)='integer' AND chunk_x BETWEEN -134217728 AND 134217727),
                        chunk_z INTEGER NOT NULL CHECK(typeof(chunk_z)='integer' AND chunk_z BETWEEN -134217728 AND 134217727),
                        generation TEXT NOT NULL,
                        observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                        PRIMARY KEY(kind,world_id,chunk_x,chunk_z)
                    )
                    """);
        }
    }

    /**
     * Saves caller-confirmed generated terrain only when its capture epoch is still current.
     * An existing certificate is immutable: an identical epoch is idempotent, while an older
     * or otherwise different certificate cannot be replaced. The observation time is supplied
     * by the caller; this repository does not infer when the capture epoch began.
     */
    public boolean recordGenerated(String kind, UUID worldId, int chunkX, int chunkZ,
                                   UUID expectedGeneration, long observedMillis) throws SQLException {
        requireKind(kind);
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(expectedGeneration, "expectedGeneration");
        requireChunk(chunkX, chunkZ);
        if (observedMillis < 0) throw new IllegalArgumentException("Observation time must be nonnegative");
        requireIdleOwner();

        connection.setAutoCommit(false);
        boolean completed = false;
        Throwable failure = null;
        try {
            var current = new ResourceRecoveryEpochs(connection).load(kind);
            if (current.isEmpty() || !current.orElseThrow().healthy()
                    || !current.orElseThrow().generation().equals(expectedGeneration)) {
                connection.rollback();
                completed = true;
                return false;
            }

            int inserted;
            try (var statement = connection.prepareStatement("""
                    INSERT INTO generated_resource_chunks(kind,world_id,chunk_x,chunk_z,generation,observed_ms)
                    VALUES(?,?,?,?,?,?) ON CONFLICT(kind,world_id,chunk_x,chunk_z) DO NOTHING
                    """)) {
                statement.setString(1, kind);
                statement.setString(2, worldId.toString());
                statement.setInt(3, chunkX);
                statement.setInt(4, chunkZ);
                statement.setString(5, expectedGeneration.toString());
                statement.setLong(6, observedMillis);
                inserted = statement.executeUpdate();
            }

            boolean sameCertificate = inserted == 1 || hasCertificate(kind, worldId, chunkX, chunkZ, expectedGeneration);
            if (sameCertificate) connection.commit();
            else connection.rollback();
            completed = true;
            return sameCertificate;
        } catch (SQLException | RuntimeException problem) {
            failure = problem;
            if (!completed) {
                try { connection.rollback(); }
                catch (SQLException rollbackFailure) { problem.addSuppressed(rollbackFailure); }
            }
            throw problem;
        } finally {
            try { connection.setAutoCommit(true); }
            catch (SQLException restoreFailure) {
                if (failure != null) failure.addSuppressed(restoreFailure);
                else throw restoreFailure;
            }
        }
    }

    /** Checks that every distinct chunk touched by the supplied block positions has a current certificate. */
    public boolean covers(String kind, UUID worldId, Collection<VillageObservation.Position> positions) throws SQLException {
        requireKind(kind);
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(positions, "positions");
        if (positions.isEmpty() || positions.size() > MAX_COVERAGE_POSITIONS)
            throw new IllegalArgumentException("Coverage positions must contain between 1 and "
                    + MAX_COVERAGE_POSITIONS + " entries");

        Set<Chunk> chunks = new HashSet<>();
        for (VillageObservation.Position position : positions) {
            Objects.requireNonNull(position, "position");
            chunks.add(new Chunk(Math.floorDiv(position.x(), 16), Math.floorDiv(position.z(), 16)));
        }

        var current = new ResourceRecoveryEpochs(connection).load(kind);
        if (current.isEmpty() || !current.orElseThrow().healthy()) return false;
        UUID generation = current.orElseThrow().generation();
        try (var statement = connection.prepareStatement("""
                SELECT 1 FROM generated_resource_chunks
                WHERE kind=? AND world_id=? AND chunk_x=? AND chunk_z=? AND generation=?
                """)) {
            for (Chunk chunk : chunks) {
                statement.setString(1, kind);
                statement.setString(2, worldId.toString());
                statement.setInt(3, chunk.x());
                statement.setInt(4, chunk.z());
                statement.setString(5, generation.toString());
                try (var result = statement.executeQuery()) {
                    if (!result.next()) return false;
                }
            }
        }
        return true;
    }

    private boolean hasCertificate(String kind, UUID worldId, int chunkX, int chunkZ,
                                   UUID generation) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT generation FROM generated_resource_chunks
                WHERE kind=? AND world_id=? AND chunk_x=? AND chunk_z=?
                """)) {
            statement.setString(1, kind);
            statement.setString(2, worldId.toString());
            statement.setInt(3, chunkX);
            statement.setInt(4, chunkZ);
            try (var result = statement.executeQuery()) {
                return result.next() && generation.toString().equals(result.getString(1));
            }
        }
    }

    private void requireIdleOwner() throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Generated terrain capture requires idle SQL owner");
    }

    private static void requireChunk(int x, int z) {
        if (x < MIN_CHUNK || x > MAX_CHUNK || z < MIN_CHUNK || z > MAX_CHUNK)
            throw new IllegalArgumentException("Chunk coordinate outside Minecraft block-coordinate bounds");
    }

    private static void requireKind(String kind) {
        if (!"ore".equals(kind) && !"forest".equals(kind))
            throw new IllegalArgumentException("Unknown generated resource kind: " + kind);
    }
}
