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
        return covers(kind,worldId,positions,Long.MAX_VALUE);
    }

    /** Resource evidence must not predate the generation that establishes its natural origin. */
    public boolean covers(String kind, UUID worldId, Collection<VillageObservation.Position> positions,long evidenceMillis) throws SQLException {
        requireKind(kind);
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(positions, "positions");
        if(evidenceMillis<0)throw new IllegalArgumentException("Negative resource evidence time");
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
                WHERE kind=? AND world_id=? AND chunk_x=? AND chunk_z=? AND generation=? AND observed_ms<=?
                """)) {
            for (Chunk chunk : chunks) {
                statement.setString(1, kind);
                statement.setString(2, worldId.toString());
                statement.setInt(3, chunk.x());
                statement.setInt(4, chunk.z());
                statement.setString(5, generation.toString());
                statement.setLong(6,evidenceMillis);
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

    /** Filter current candidates without allowing a covered neighbor to authorize older terrain. */
    public Set<VillageObservation.Position> coveredPositions(String kind,UUID world,
            Collection<VillageObservation.Position> positions,long evidenceMillis)throws SQLException {
        requireKind(kind);Objects.requireNonNull(world);Objects.requireNonNull(positions);
        if(positions.isEmpty()||positions.size()>MAX_COVERAGE_POSITIONS)
            throw new IllegalArgumentException("Invalid bounded coverage positions");
        var chunks=new java.util.HashMap<Chunk,Boolean>();var result=new HashSet<VillageObservation.Position>();
        for(var position:positions) {
            Objects.requireNonNull(position);
            var chunk=new Chunk(Math.floorDiv(position.x(),16),Math.floorDiv(position.z(),16));
            Boolean covered=chunks.get(chunk);
            if(covered==null){covered=covers(kind,world,java.util.List.of(position),evidenceMillis);chunks.put(chunk,covered);}
            if(covered)result.add(position);
        }
        return Set.copyOf(result);
    }

    /** All saved coordinates must be covered; a certificate for one block cannot unlock a vein. */
    public boolean coversMineral(UUID deposit) throws SQLException {
        return coversSaved("ore",deposit,"""
                SELECT e.world_id,e.x,e.y,e.z,d.world_id,d.observed_millis
                FROM mineral_evidence e JOIN mineral_discovery d ON d.deposit_id=e.deposit_id
                WHERE e.deposit_id=? ORDER BY e.world_id,e.x,e.y,e.z LIMIT 65537
                """);
    }

    /** A retained forest requires its entire original support footprint, including lost supports. */
    public boolean coversForest(UUID site) throws SQLException {
        return coversSaved("forest",site,"""
                SELECT s.world_id,s.x,s.y,s.z,r.world_id,b.observed_millis
                FROM forestry_supports s JOIN forestry_resources r ON r.site_id=s.site_id
                JOIN business_sites b ON b.id=s.site_id
                WHERE s.site_id=? ORDER BY s.world_id,s.x,s.y,s.z LIMIT 65537
                """);
    }

    private boolean coversSaved(String kind,UUID resource,String sql)throws SQLException {
        Objects.requireNonNull(resource,"resource");
        UUID world=null;long observed=Long.MAX_VALUE;var positions=new java.util.ArrayList<VillageObservation.Position>();
        try(var statement=connection.prepareStatement(sql)) {
            statement.setString(1,resource.toString());
            try(var rows=statement.executeQuery()) {
                while(rows.next()) {
                    if(positions.size()==MAX_COVERAGE_POSITIONS)throw new SQLException("Saved resource coverage exceeds complete read budget");
                    var rowWorld=UUID.fromString(rows.getString(1));
                    if(!rows.getString(1).equals(rows.getString(5))||(world!=null&&!world.equals(rowWorld)))
                        throw new SQLException("Saved resource coverage disagrees on world identity");
                    world=rowWorld;positions.add(new VillageObservation.Position(rows.getInt(2),rows.getInt(3),rows.getInt(4)));
                    observed=Math.min(observed,rows.getLong(6));
                }
            }
        }
        return world!=null&&!positions.isEmpty()&&covers(kind,world,positions,observed);
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
