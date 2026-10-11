package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.WorkplaceObservation;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Current marker candidates plus retained sightings; no business, stock, money or land writes. */
public final class WorkplaceOpportunities {
    public record Candidate(UUID world,Position position,WorkplaceObservation.Kind kind,long capturedMillis) {
        public String industry(){return kind==WorkplaceObservation.Kind.BARREL?"warehouse":"blacksmith";}
    }
    public record Review(List<Candidate> candidates,String explanation) {public Review{candidates=List.copyOf(candidates);}}
    private final Connection connection;
    public WorkplaceOpportunities(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public void observe(UUID town,WorkplaceObservation observation)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Workplace observation requires idle SQL owner");connection.setAutoCommit(false);
        try {
            if(!observation.world().equals(world(town)))throw new SQLException("Workplace evidence does not match permanent settlement world");
            String hash=fingerprint(observation);
            try(var s=connection.prepareStatement("SELECT captured_millis,fingerprint FROM workplace_surveys WHERE settlement_id=? AND world_id=? AND chunk_x=? AND chunk_z=?")) {
                key(s,town,observation);try(var r=s.executeQuery()) {
                    if(r.next()) {
                        long old=r.getLong(1);if(old>observation.capturedMillis()){connection.commit();connection.setAutoCommit(true);return;}
                        if(old==observation.capturedMillis()&&!hash.equals(r.getString(2)))throw new SQLException("Same workplace capture time has conflicting evidence");
                    }
                }
            }
            try(var s=connection.prepareStatement("INSERT INTO workplace_surveys VALUES(?,?,?,?,?,?,?) ON CONFLICT(settlement_id,world_id,chunk_x,chunk_z) DO UPDATE SET captured_millis=excluded.captured_millis,complete=excluded.complete,fingerprint=excluded.fingerprint")) {
                key(s,town,observation);s.setLong(5,observation.capturedMillis());s.setBoolean(6,observation.complete());s.setString(7,hash);s.executeUpdate();
            }
            if(observation.complete()) {
                try(var s=connection.prepareStatement("DELETE FROM workplace_markers WHERE settlement_id=? AND world_id=? AND chunk_x=? AND chunk_z=?")){key(s,town,observation);s.executeUpdate();}
                for(var marker:observation.markers()) {
                    var p=marker.position();
                    try(var s=connection.prepareStatement("INSERT INTO workplace_sightings VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(settlement_id,world_id,x,y,z,kind) DO UPDATE SET last_seen_millis=MAX(last_seen_millis,excluded.last_seen_millis)")) {
                        s.setString(1,town.toString());s.setString(2,observation.world().toString());s.setInt(3,p.x());s.setInt(4,p.y());s.setInt(5,p.z());s.setString(6,marker.kind().name());s.setLong(7,observation.capturedMillis());s.setLong(8,observation.capturedMillis());s.executeUpdate();
                    }
                    try(var s=connection.prepareStatement("INSERT INTO workplace_markers VALUES(?,?,?,?,?,?,?,?)")) {
                        key(s,town,observation);s.setInt(5,p.x());s.setInt(6,p.y());s.setInt(7,p.z());s.setString(8,marker.kind().name());s.executeUpdate();
                    }
                }
            }
            connection.commit();connection.setAutoCommit(true);
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    public Review review(UUID town,long now,long maximumAgeMillis,Set<UUID> activeWorlds,int limit)throws SQLException {
        if(now<0||maximumAgeMillis<0||limit<1||limit>512)throw new IllegalArgumentException("Invalid workplace review bounds");
        UUID world=world(town);if(activeWorlds==null||!activeWorlds.contains(world))return new Review(List.of(),"Workplace candidates paused: world absent or availability unknown");
        long earliest=Math.max(0,now-maximumAgeMillis);var rows=new ArrayList<Candidate>();
        try(var s=connection.prepareStatement("""
            SELECT m.x,m.y,m.z,m.kind,v.captured_millis FROM workplace_markers m JOIN workplace_surveys v
              USING(settlement_id,world_id,chunk_x,chunk_z)
            WHERE m.settlement_id=? AND m.world_id=? AND v.complete=1 AND v.captured_millis BETWEEN ? AND ?
              AND NOT EXISTS(SELECT 1 FROM business_sites b WHERE b.world_id=m.world_id AND b.x=m.x AND b.y=m.y AND b.z=m.z)
              AND NOT EXISTS(SELECT 1 FROM village_beds b WHERE b.world_id=m.world_id AND b.x=m.x AND b.y=m.y AND b.z=m.z)
              AND NOT EXISTS(SELECT 1 FROM workplace_markers o JOIN workplace_surveys w USING(settlement_id,world_id,chunk_x,chunk_z)
                WHERE o.settlement_id<>m.settlement_id AND o.world_id=m.world_id AND o.x=m.x AND o.y=m.y AND o.z=m.z
                  AND w.complete=1 AND w.captured_millis BETWEEN ? AND ?)
            ORDER BY m.x,m.y,m.z,m.kind LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setString(2,world.toString());s.setLong(3,earliest);s.setLong(4,now);s.setLong(5,earliest);s.setLong(6,now);s.setInt(7,limit+1);
            try(var r=s.executeQuery()){while(r.next())rows.add(new Candidate(world,new Position(r.getInt(1),r.getInt(2),r.getInt(3)),WorkplaceObservation.Kind.valueOf(r.getString(4)),r.getLong(5)));}
        }
        if(rows.size()>limit)return new Review(List.of(),"Workplace candidate budget exceeded; no partial selection");
        return new Review(rows,"Current complete workplace markers: "+rows.size()+"; stale/future/partial/conflicting/occupied anchors excluded; skills, real demand/funding and current-block revalidation still required; no ownership rights");
    }
    private UUID world(UUID town)throws SQLException {
        try(var s=connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")){s.setString(1,Objects.requireNonNull(town).toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Workplace settlement lacks permanent world identity");return UUID.fromString(r.getString(1));}}
    }
    private static void key(PreparedStatement s,UUID town,WorkplaceObservation o)throws SQLException {s.setString(1,town.toString());s.setString(2,o.world().toString());s.setInt(3,o.chunkX());s.setInt(4,o.chunkZ());}
    private static String fingerprint(WorkplaceObservation observation) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(observation.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
