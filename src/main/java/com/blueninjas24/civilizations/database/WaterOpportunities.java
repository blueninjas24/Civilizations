package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Current water evidence, not saved fish stock. Partial captures invalidate eligibility without deleting last complete columns. */
public final class WaterOpportunities {
    public record Candidate(UUID world,FisheryObservation.Column position,int surfaceY,long capturedMillis,int minimumDepth) {}
    public record Review(List<Candidate> columns,String explanation){public Review{columns=List.copyOf(columns);}}
    private final Connection connection;
    public WaterOpportunities(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public void observe(UUID town,WaterScan.Observation observation)throws SQLException {
        Objects.requireNonNull(observation);
        if(!connection.getAutoCommit())throw new SQLException("Water observation requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            if(!world(town).equals(observation.world()))throw new SQLException("Water evidence must match permanent settlement world");
            String hash=fingerprint(observation);
            try(var s=connection.prepareStatement("SELECT captured_millis,fingerprint FROM water_surveys WHERE settlement_id=? AND world_id=? AND chunk_x=? AND chunk_z=?")) {
                key(s,town,observation);try(var r=s.executeQuery()) {
                    if(r.next()) {
                        if(r.getLong(1)>observation.capturedMillis()){connection.commit();connection.setAutoCommit(true);return;}
                        if(r.getLong(1)==observation.capturedMillis()) {
                            if(!hash.equals(r.getString(2)))throw new SQLException("Conflicting water capture at same timestamp");
                            connection.commit();connection.setAutoCommit(true);return;
                        }
                    }
                }
            }
            try(var s=connection.prepareStatement("""
                INSERT INTO water_surveys VALUES(?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(settlement_id,world_id,chunk_x,chunk_z) DO UPDATE SET
                  captured_millis=excluded.captured_millis,complete=excluded.complete,minimum_y=excluded.minimum_y,
                  maximum_y=excluded.maximum_y,minimum_depth=excluded.minimum_depth,fingerprint=excluded.fingerprint
                """)) {
                key(s,town,observation);s.setLong(5,observation.capturedMillis());s.setBoolean(6,observation.complete());
                s.setInt(7,observation.minimumY());s.setInt(8,observation.maximumY());s.setInt(9,observation.minimumDepth());s.setString(10,hash);s.executeUpdate();
            }
            if(observation.complete()) {
                try(var s=connection.prepareStatement("DELETE FROM water_columns WHERE settlement_id=? AND world_id=? AND chunk_x=? AND chunk_z=?")){key(s,town,observation);s.executeUpdate();}
                for(var c:observation.columns())if(c.state()==FisheryHabitatObservation.State.SUITABLE) {
                    try(var s=connection.prepareStatement("INSERT INTO water_columns VALUES(?,?,?,?,?,?,?)")) {
                        key(s,town,observation);s.setInt(5,c.position().x());s.setInt(6,c.position().z());s.setInt(7,c.surfaceY());s.executeUpdate();
                    }
                }
            }
            connection.commit();connection.setAutoCommit(true);
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    public Review review(UUID town,long now,long maximumAgeMillis,Set<UUID> activeWorlds,int minimumDepth,int limit)throws SQLException {
        if(now<0||maximumAgeMillis<0||minimumDepth<1||minimumDepth>64||limit<1||limit>4096)throw new IllegalArgumentException("Invalid water review bounds");
        var world=world(town);
        if(activeWorlds==null||!activeWorlds.contains(world))return new Review(List.of(),"Water opportunities paused: world absent or availability unknown");
        long earliest=Math.max(0,now-maximumAgeMillis);var columns=new ArrayList<Candidate>();
        try(var s=connection.prepareStatement("""
            SELECT c.x,c.z,c.surface_y,v.captured_millis,v.minimum_depth FROM water_columns c JOIN water_surveys v
              USING(settlement_id,world_id,chunk_x,chunk_z)
            WHERE c.settlement_id=? AND c.world_id=? AND v.complete=1 AND v.minimum_depth=?
              AND v.captured_millis BETWEEN ? AND ?
              AND NOT EXISTS(SELECT 1 FROM fishery_columns f WHERE f.world_id=c.world_id AND f.x=c.x AND f.z=c.z)
              AND NOT EXISTS(SELECT 1 FROM water_columns o JOIN water_surveys w USING(settlement_id,world_id,chunk_x,chunk_z)
                WHERE o.settlement_id<>c.settlement_id AND o.world_id=c.world_id AND o.x=c.x AND o.z=c.z
                  AND w.complete=1 AND w.captured_millis BETWEEN ? AND ?)
            ORDER BY c.x,c.z LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setString(2,world.toString());s.setInt(3,minimumDepth);s.setLong(4,earliest);s.setLong(5,now);
            s.setLong(6,earliest);s.setLong(7,now);s.setInt(8,limit+1);
            try(var r=s.executeQuery()){while(r.next())columns.add(new Candidate(world,new FisheryObservation.Column(r.getInt(1),r.getInt(2)),r.getInt(3),r.getLong(4),r.getInt(5)));}
        }
        if(columns.size()>limit)return new Review(List.of(),"Water opportunity budget exceeded; no partial selection");
        return new Review(columns,"Current complete water columns: "+columns.size()+"; stale/future/partial/depth-mismatched/conflicting/claimed sources excluded; no resource stock, ownership or funding granted");
    }
    public List<WaterPatch.Patch> patches(UUID town,long now,long maximumAgeMillis,Set<UUID> activeWorlds,
            int minimumDepth,int minimumColumns,int maximumColumns,int maximumSites)throws SQLException {
        var review=review(town,now,maximumAgeMillis,activeWorlds,minimumDepth,4096);
        try(var s=connection.prepareStatement("SELECT center_x,center_z FROM settlements WHERE id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()) {
                if(!r.next())throw new SQLException("Water patch settlement missing");
                return WaterPatch.choose(world(town),review.columns().stream().map(c->new WaterPatch.Sample(c.position(),c.surfaceY(),c.capturedMillis())).toList(),
                        r.getInt(1),r.getInt(2),now,maximumAgeMillis,minimumColumns,maximumColumns,maximumSites);
            }
        }
    }
    private UUID world(UUID town)throws SQLException {
        try(var s=connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")) {
            s.setString(1,Objects.requireNonNull(town).toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Water settlement lacks permanent world identity");return UUID.fromString(r.getString(1));}
        }
    }
    private static void key(PreparedStatement s,UUID town,WaterScan.Observation o)throws SQLException {s.setString(1,town.toString());s.setString(2,o.world().toString());s.setInt(3,o.chunkX());s.setInt(4,o.chunkZ());}
    private static String fingerprint(WaterScan.Observation o) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(o.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
