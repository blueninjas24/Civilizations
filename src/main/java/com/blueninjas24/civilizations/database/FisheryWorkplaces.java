package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;

/** SQL-owner saved shoreline terms/current bank and immutable recovery audit. No Paper access or block repairs. */
public final class FisheryWorkplaces {
    public record Binding(UUID site,UUID world,int surfaceY,int minimumDepth,int maximumRise,int maximumDistance,
            ShorelineScan.Position point,boolean unsafe,long observedMillis,String openingHash,String latestFingerprint) {}
    public record Receipt(UUID site,long observedMillis,ShorelineScan.Position before,boolean beforeUnsafe,boolean afterUnsafe,ShorelineRecovery.Decision decision,String fingerprint) {}
    private final Connection connection;
    public FisheryWorkplaces(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public Optional<Binding> find(UUID site)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM fishery_workplaces WHERE site_id=?")) {
            s.setString(1,site.toString());try(var r=s.executeQuery()) {
                return r.next()?Optional.of(new Binding(site,UUID.fromString(r.getString("world_id")),r.getInt("surface_y"),r.getInt("minimum_depth"),r.getInt("maximum_rise"),r.getInt("maximum_distance"),
                        new ShorelineScan.Position(r.getInt("x"),r.getInt("y"),r.getInt("z")),r.getBoolean("unsafe"),r.getLong("observed_ms"),r.getString("opening_hash"),r.getString("latest_fingerprint"))):Optional.empty();
            }
        }
    }
    public List<Binding> page(UUID after,int limit)throws SQLException {
        if(limit<1||limit>64)throw new IllegalArgumentException("Bank page must be 1..64");var result=new ArrayList<Binding>();
        try(var s=connection.prepareStatement("SELECT site_id FROM fishery_workplaces WHERE site_id>? ORDER BY site_id LIMIT ?")) {
            s.setString(1,after==null?"":after.toString());s.setInt(2,limit);try(var r=s.executeQuery()){while(r.next())result.add(find(UUID.fromString(r.getString(1))).orElseThrow());}
        }return List.copyOf(result);
    }
    /** Legacy resources without explicit shoreline terms keep their existing behavior. */
    static boolean paused(Connection connection,UUID site)throws SQLException {
        return new FisheryWorkplaces(connection).find(site).map(Binding::unsafe).orElse(false);
    }
    Binding attachRows(UUID site,WaterPatch.Patch original,int depth,int rise,int distance,ShorelineEvidence evidence,long now,long maximumAge)throws SQLException {
        owner();Objects.requireNonNull(original);Objects.requireNonNull(evidence);
        if(depth<1||depth>64||rise<0||rise>4||distance<0||distance>256||!original.evidence().complete())throw new IllegalArgumentException("Invalid opening shoreline terms");
        String opening=hash(original+":"+depth+":"+rise+":"+distance+":"+evidence);
        var old=find(site);if(old.isPresent()) {
            if(!old.orElseThrow().openingHash().equals(opening))throw new SQLException("Opening shoreline identity/terms changed");return old.orElseThrow();
        }
        fresh(evidence,now,maximumAge);
        if(original.evidence().observedMillis()>evidence.capturedMillis()||now-original.evidence().observedMillis()>maximumAge)throw new SQLException("Fresh original water evidence required");
        var fisheries=new FisheryRepository(connection);var resource=fisheries.find(site).orElseThrow(()->new SQLException("Saved fishery habitat required"));
        if(!resource.world().equals(evidence.world())||!original.evidence().world().equals(evidence.world())||!fisheries.columns(site).equals(original.evidence().columns()))throw new SQLException("Opening bank does not match original saved habitat");
        validate(original,rise,evidence);
        var decision=ShorelineRecovery.decide(evidence.origin(),distance,evidence.assessments());
        if(decision.action()!=ShorelineRecovery.Action.KEEP)throw new SQLException("First bank must be actually safe; no guessed relocation or repair");
        try(var s=connection.prepareStatement("SELECT x,y,z FROM business_sites WHERE id=?")) {
            s.setString(1,site.toString());try(var r=s.executeQuery()){if(!r.next()||r.getInt(1)!=evidence.origin().x()||r.getInt(2)!=evidence.origin().y()||r.getInt(3)!=evidence.origin().z())throw new SQLException("Opening bank must match founding site");}
        }
        try(var s=connection.prepareStatement("INSERT INTO fishery_workplaces VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,site.toString());s.setString(2,evidence.world().toString());s.setInt(3,original.surfaceY());s.setInt(4,depth);s.setInt(5,rise);s.setInt(6,distance);
            point(s,7,evidence.origin());s.setBoolean(10,false);s.setLong(11,evidence.capturedMillis());s.setString(12,opening);s.setString(13,hash(evidence.toString()));s.executeUpdate();
        }
        save(new Receipt(site,evidence.capturedMillis(),evidence.origin(),false,false,decision,hash(evidence.toString())));
        return find(site).orElseThrow();
    }
    public Receipt reconcile(UUID site,ShorelineEvidence evidence,long now,long maximumAge)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Bank reconciliation requires idle SQL owner");connection.setAutoCommit(false);
        try{var result=reconcileRows(site,evidence,now,maximumAge);connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    Receipt reconcileRows(UUID site,ShorelineEvidence evidence,long now,long maximumAge)throws SQLException {
        owner();Objects.requireNonNull(evidence);var fingerprint=hash(evidence.toString());var replay=history(site,evidence.capturedMillis());
        if(replay.isPresent()){if(!replay.orElseThrow().fingerprint().equals(fingerprint))throw new SQLException("Bank frame timestamp reused with different evidence");return replay.orElseThrow();}
        var old=find(site).orElseThrow(()->new SQLException("Legacy fishery has no saved bank terms; explicit confirmed opening required"));
        if(evidence.capturedMillis()==old.observedMillis()) {
            if(!old.latestFingerprint().equals(fingerprint))throw new SQLException("Conflicting latest bank/support frame");
            var decision=ShorelineRecovery.decide(old.point(),old.maximumDistance(),evidence.assessments());return new Receipt(site,evidence.capturedMillis(),old.point(),old.unsafe(),old.unsafe(),decision,fingerprint);
        }
        fresh(evidence,now,maximumAge);
        if(!old.world().equals(evidence.world())||!old.point().equals(evidence.origin())||evidence.capturedMillis()<=old.observedMillis())throw new SQLException("Bank evidence world, current origin or ordering changed");
        var fisheries=new FisheryRepository(connection);var original=new WaterPatch.Patch(new FisheryObservation(old.world(),0,true,fisheries.columns(site)),old.surfaceY());
        validate(original,old.maximumRise(),evidence);
        var decision=ShorelineRecovery.decide(old.point(),old.maximumDistance(),evidence.assessments());
        boolean unsafe=decision.confirmedSafe()?false:decision.currentConfirmedUnsafe()?true:old.unsafe();
        try(var s=connection.prepareStatement("UPDATE fishery_workplaces SET x=?,y=?,z=?,unsafe=?,observed_ms=?,latest_fingerprint=? WHERE site_id=? AND observed_ms=?")) {
            point(s,1,decision.position());s.setBoolean(4,unsafe);s.setLong(5,evidence.capturedMillis());s.setString(6,fingerprint);s.setString(7,site.toString());s.setLong(8,old.observedMillis());if(s.executeUpdate()!=1)throw new SQLException("Bank changed during owner reconciliation");
        }
        var receipt=new Receipt(site,evidence.capturedMillis(),old.point(),old.unsafe(),unsafe,decision,fingerprint);if(!old.point().equals(decision.position())||old.unsafe()!=unsafe)save(receipt);return receipt;
    }
    public Optional<Receipt> history(UUID site,long time)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM fishery_workplace_history WHERE site_id=? AND observed_ms=?")) {
            s.setString(1,site.toString());s.setLong(2,time);try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();var before=new ShorelineScan.Position(r.getInt("before_x"),r.getInt("before_y"),r.getInt("before_z"));var after=new ShorelineScan.Position(r.getInt("after_x"),r.getInt("after_y"),r.getInt("after_z"));
                var decision=new ShorelineRecovery.Decision(ShorelineRecovery.Action.valueOf(r.getString("action")),after,r.getBoolean("confirmed_safe"),r.getBoolean("confirmed_damage"),r.getString("explanation"));
                return Optional.of(new Receipt(site,time,before,r.getBoolean("before_unsafe"),r.getBoolean("after_unsafe"),decision,r.getString("fingerprint")));
            }
        }
    }
    private void save(Receipt receipt)throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO fishery_workplace_history VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,receipt.site().toString());s.setLong(2,receipt.observedMillis());point(s,3,receipt.before());point(s,6,receipt.decision().position());
            s.setBoolean(9,receipt.beforeUnsafe());s.setBoolean(10,receipt.afterUnsafe());s.setString(11,receipt.decision().action().name());s.setBoolean(12,receipt.decision().confirmedSafe());s.setBoolean(13,receipt.decision().currentConfirmedUnsafe());s.setString(14,receipt.fingerprint());s.setString(15,receipt.decision().explanation());s.executeUpdate();
        }
    }
    private static void validate(WaterPatch.Patch original,int rise,ShorelineEvidence evidence)throws SQLException {
        var expected=new ShorelineScan(p->ShorelineScan.Cell.UNKNOWN,original,rise).candidates();
        if(!expected.equals(evidence.assessments().stream().map(ShorelineScan.Assessment::position).toList()))throw new SQLException("Complete exact original bank candidate frame required");
    }
    private static void fresh(ShorelineEvidence evidence,long now,long maximumAge)throws SQLException {
        if(now<0||maximumAge<0||maximumAge>3_600_000)throw new IllegalArgumentException("Invalid bank freshness bounds");
        if(evidence.capturedMillis()>now||now-evidence.capturedMillis()>maximumAge)throw new SQLException("Fresh nonfuture bank evidence required");
    }
    private static void point(PreparedStatement s,int offset,ShorelineScan.Position point)throws SQLException{s.setInt(offset,point.x());s.setInt(offset+1,point.y());s.setInt(offset+2,point.z());}
    private void owner()throws SQLException{if(connection.getAutoCommit())throw new SQLException("Fishery bank rows require owner transaction");}
    static String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}
