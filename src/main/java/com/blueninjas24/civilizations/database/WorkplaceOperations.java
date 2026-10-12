package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Saved fixed anchors and explicit point confirmations. Chunk scans cannot prove absence at unscanned heights. */
public final class WorkplaceOperations {
    public record Anchor(UUID business,UUID world,Position position,WorkplaceObservation.Kind kind) {
        public Anchor {Objects.requireNonNull(business);Objects.requireNonNull(world);Objects.requireNonNull(position);Objects.requireNonNull(kind);}
    }
    /** Trusted main-thread adapter supplies only actually loaded block reads, never unknown observations. */
    public record Confirmation(Anchor anchor,boolean present,long capturedMillis) {
        public Confirmation {Objects.requireNonNull(anchor);if(capturedMillis<0)throw new IllegalArgumentException("Negative marker capture time");}
    }
    private final Connection connection;
    public WorkplaceOperations(Connection connection){this.connection=Objects.requireNonNull(connection);}

    public void configure(WorkplaceOperationPolicy policy)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Marker configuration requires idle SQL owner");
        try(var s=connection.prepareStatement("UPDATE workplace_operation_settings SET pause_on_missing=? WHERE singleton=1")) {
            s.setBoolean(1,Objects.requireNonNull(policy).pauseOnMissing());if(s.executeUpdate()!=1)throw new SQLException("Missing workplace operation settings");
        }
    }
    /** Compose only with NEW marker-founded acceptance, in its founding transaction. No legacy rebinding API. */
    void attachRows(Anchor anchor,long capturedMillis)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Marker attachment requires founding transaction");
        if(capturedMillis<0)throw new IllegalArgumentException("Negative marker capture time");
        var founding=new BusinessRepository(connection).findFounding(anchor.business()).orElseThrow(()->new SQLException("Missing marker founding"));
        var site=founding.site();var p=anchor.position();
        String industry=anchor.kind()==WorkplaceObservation.Kind.BARREL?"warehouse":"blacksmith";
        if(!site.worldId().equals(anchor.world())||site.x()!=p.x()||site.y()!=p.y()||site.z()!=p.z()||!founding.business().industry().equals(industry))
            throw new SQLException("Marker must match founding world, fixed site and industry");
        try(var s=connection.prepareStatement("INSERT INTO business_workplace_anchors VALUES(?,?,?,?,?,?,?,1)")) {
            s.setString(1,anchor.business().toString());s.setString(2,anchor.world().toString());s.setInt(3,p.x());s.setInt(4,p.y());s.setInt(5,p.z());s.setString(6,anchor.kind().name());s.setLong(7,capturedMillis);s.executeUpdate();
        }
        audit(anchor.business(),capturedMillis,true);
    }
    boolean matchesSavedAnchor(Anchor anchor)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM business_workplace_anchors WHERE business_id=?")) {
            s.setString(1,anchor.business().toString());try(var r=s.executeQuery()) {
                if(!r.next())return false;
                if(!read(r).equals(anchor))throw new SQLException("Startup replay cannot move or change workplace marker");
                return true;
            }
        }
    }
    public List<Anchor> anchors(UUID after,int limit)throws SQLException {
        if(limit<1||limit>64)throw new IllegalArgumentException("Marker page must be 1..64");
        var result=new ArrayList<Anchor>();
        try(var s=connection.prepareStatement("SELECT * FROM business_workplace_anchors WHERE business_id>? ORDER BY business_id LIMIT ?")) {
            s.setString(1,after==null?"":after.toString());s.setInt(2,limit);
            try(var r=s.executeQuery()){while(r.next())result.add(read(r));}
        }
        return List.copyOf(result);
    }
    /** Every page is atomic; stale/future/absent-world evidence cannot change availability. */
    public void confirm(List<Confirmation> evidence,long now,Set<UUID> activeWorlds)throws SQLException {
        evidence=List.copyOf(evidence);
        if(evidence.size()>64||now<0)throw new IllegalArgumentException("Invalid marker confirmation page");
        if(!connection.getAutoCommit())throw new SQLException("Marker confirmation requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            for(var item:evidence) {
                if(activeWorlds==null||!activeWorlds.contains(item.anchor().world())||item.capturedMillis()>now||now-item.capturedMillis()>60000)continue;
                try(var s=connection.prepareStatement("SELECT * FROM business_workplace_anchors WHERE business_id=?")) {
                    s.setString(1,item.anchor().business().toString());
                    try(var r=s.executeQuery()) {
                        if(!r.next()||!read(r).equals(item.anchor()))throw new SQLException("Marker confirmation does not match saved fixed anchor");
                        long old=r.getLong("captured_millis");
                        if(item.capturedMillis()<old)continue;
                        if(item.capturedMillis()==old) {
                            if(item.present()!=r.getBoolean("present"))throw new SQLException("Conflicting marker confirmation timestamp");
                            continue;
                        }
                    }
                }
                try(var s=connection.prepareStatement("UPDATE business_workplace_anchors SET captured_millis=?,present=? WHERE business_id=?")) {
                    s.setLong(1,item.capturedMillis());s.setBoolean(2,item.present());s.setString(3,item.anchor().business().toString());s.executeUpdate();
                }
                // Keep transition history; repeated unchanged point reads update freshness without unbounded audit growth.
                try(var s=connection.prepareStatement("SELECT present FROM workplace_operation_history WHERE business_id=? ORDER BY captured_millis DESC LIMIT 1")) {
                    s.setString(1,item.anchor().business().toString());try(var r=s.executeQuery()){if(!r.next()||r.getBoolean(1)!=item.present())audit(item.anchor().business(),item.capturedMillis(),item.present());}
                }
            }
            connection.commit();
        }catch(SQLException|RuntimeException failure){try{connection.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
        finally{connection.setAutoCommit(true);}
    }
    static boolean paused(Connection connection,UUID business)throws SQLException {
        // Preserve inspection of older schemas after a failed upgrade; do not hide missing tables on current saves.
        try(var s=connection.createStatement();var r=s.executeQuery("PRAGMA user_version")){if(r.getInt(1)<61)return false;}
        try(var s=connection.prepareStatement("SELECT 1 FROM business_workplace_anchors a JOIN workplace_operation_settings p ON p.singleton=1 WHERE a.business_id=? AND a.present=0 AND p.pause_on_missing=1")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
    static void requirePermitted(Connection connection,UUID business)throws SQLException {
        if(paused(connection,business))throw new SQLException("Paused: workplace marker missing; restore the same saved anchor");
    }
    private static Anchor read(ResultSet r)throws SQLException {
        return new Anchor(UUID.fromString(r.getString("business_id")),UUID.fromString(r.getString("world_id")),new Position(r.getInt("x"),r.getInt("y"),r.getInt("z")),WorkplaceObservation.Kind.valueOf(r.getString("kind")));
    }
    private void audit(UUID business,long time,boolean present)throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO workplace_operation_history VALUES(?,?,?)")){s.setString(1,business.toString());s.setLong(2,time);s.setBoolean(3,present);s.executeUpdate();}
    }
}
