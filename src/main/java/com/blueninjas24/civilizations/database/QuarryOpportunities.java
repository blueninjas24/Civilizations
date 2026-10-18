package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Current complete per-chunk frames. Missing a different chunk is never evidence of resource loss. */
final class QuarryOpportunities {
    private record Frame(UUID world,int x,int z,long captured,String fingerprint) {}
    private final Connection c;
    QuarryOpportunities(Connection c){this.c=Objects.requireNonNull(c);}
    List<MineralSurveyReader.Patch> observe(UUID town,List<MineralObservation> observations,int x,int z)throws SQLException {
        observations=List.copyOf(observations);
        boolean manual=x==Integer.MIN_VALUE&&z==Integer.MIN_VALUE;
        if(!manual&&observations.stream().flatMap(o->o.ore().stream()).anyMatch(p->Math.floorDiv(p.x(),16)!=x||Math.floorDiv(p.z(),16)!=z))
            throw new IllegalArgumentException("Quarry frame coordinates must belong to its captured chunk");
        if(!c.getAutoCommit())throw new SQLException("Quarry observation requires idle SQL owner");c.setAutoCommit(false);
        try {
            var patches=MineralSurveyReader.quarries(c).read(town,observations);var first=observations.getFirst();
            String fingerprint=MineralOpportunities.fingerprint(observations);Frame old=null;
            try(var s=c.prepareStatement("SELECT world_id,captured_millis,fingerprint FROM quarry_surveys WHERE settlement_id=? AND scope_x=? AND scope_z=?")) {
                key(s,town,x,z);try(var r=s.executeQuery()){if(r.next())old=new Frame(UUID.fromString(r.getString(1)),x,z,r.getLong(2),r.getString(3));}
            }
            if(old!=null&&old.captured()==first.observedMillis()&&!old.fingerprint().equals(fingerprint))throw new SQLException("Same quarry capture time has conflicting evidence");
            try(var s=c.prepareStatement("""
                INSERT INTO quarry_opportunities VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(settlement_id,world_id,x,y,z)
                DO UPDATE SET last_seen_millis=MAX(last_seen_millis,excluded.last_seen_millis) WHERE mineral=excluded.mineral
                """)) {
                for(var patch:patches)for(var p:patch.evidence().ore()) {
                    s.setString(1,town.toString());s.setString(2,first.world().toString());s.setInt(3,p.x());s.setInt(4,p.y());s.setInt(5,p.z());
                    s.setString(6,patch.evidence().mineral().id());s.setLong(7,first.observedMillis());s.setLong(8,first.observedMillis());
                    if(s.executeUpdate()!=1)throw new SQLException("Quarry mineral conflicts with retained coordinate identity");
                }
            }
            if(old==null||first.observedMillis()>old.captured()) {
                try(var s=c.prepareStatement("DELETE FROM quarry_survey_members WHERE settlement_id=? AND scope_x=? AND scope_z=?")){key(s,town,x,z);s.executeUpdate();}
                try(var s=c.prepareStatement("""
                    INSERT INTO quarry_surveys(settlement_id,scope_x,scope_z,world_id,captured_millis,fingerprint) VALUES(?,?,?,?,?,?)
                    ON CONFLICT(settlement_id,scope_x,scope_z) DO UPDATE SET world_id=excluded.world_id,captured_millis=excluded.captured_millis,fingerprint=excluded.fingerprint
                    """)) {
                    key(s,town,x,z);s.setString(4,first.world().toString());s.setLong(5,first.observedMillis());s.setString(6,fingerprint);s.executeUpdate();
                }
                try(var s=c.prepareStatement("INSERT INTO quarry_survey_members(settlement_id,scope_x,scope_z,world_id,x,y,z) VALUES(?,?,?,?,?,?,?)")) {
                    for(var patch:patches)for(var p:patch.evidence().ore()) {
                        key(s,town,x,z);s.setString(4,first.world().toString());s.setInt(5,p.x());s.setInt(6,p.y());s.setInt(7,p.z());s.executeUpdate();
                    }
                }
            }
            c.commit();c.setAutoCommit(true);return patches;
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    MineralOpportunities.Review review(UUID town,long now,long lifetime,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(town);worlds=worlds==null?Set.of():Set.copyOf(worlds);
        if(now<0||lifetime<1)throw new IllegalArgumentException("Nonnegative clock and positive quarry lifetime required");
        var frames=new ArrayList<Frame>();
        try(var s=c.prepareStatement("SELECT * FROM quarry_surveys WHERE settlement_id=? AND captured_millis BETWEEN ? AND ? ORDER BY captured_millis DESC,scope_x,scope_z LIMIT 65")) {
            s.setString(1,town.toString());s.setLong(2,Math.max(0,now-lifetime));s.setLong(3,now);
            try(var r=s.executeQuery()){while(r.next())frames.add(new Frame(UUID.fromString(r.getString("world_id")),r.getInt("scope_x"),r.getInt("scope_z"),r.getLong("captured_millis"),r.getString("fingerprint")));}
        }
        // Retain historical frames without letting old scopes exhaust today's review budget.
        if(frames.isEmpty())try(var s=c.prepareStatement("SELECT * FROM quarry_surveys WHERE settlement_id=? ORDER BY captured_millis DESC,scope_x,scope_z LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())frames.add(new Frame(UUID.fromString(r.getString("world_id")),r.getInt("scope_x"),r.getInt("scope_z"),r.getLong("captured_millis"),r.getString("fingerprint")));}
        }
        if(frames.size()>64)throw new SQLException("Current quarry frame review budget exceeded");
        if(frames.isEmpty())return view(MineralOpportunities.Status.MISSING,null,-1,List.of(),"No complete current quarry survey");
        var first=frames.getFirst();var world=first.world();
        if(frames.stream().anyMatch(f->!f.world().equals(world)))throw new SQLException("Quarry frames disagree on permanent world identity");
        if(!worlds.contains(world))return view(MineralOpportunities.Status.INACTIVE_WORLD,world,first.captured(),List.of(),"Quarry world absent or availability unknown");
        if(new OreTrackingRepository(c).load().map(s->!s.trusted()).orElse(true))return view(MineralOpportunities.Status.TRACKING_GAP,world,first.captured(),List.of(),"Finite resource mutation history requires reviewed recovery");
        var current=frames.stream().filter(f->f.captured()<=now&&now-f.captured()<=lifetime).toList();
        if(current.isEmpty())return view(first.captured()>now?MineralOpportunities.Status.FUTURE:MineralOpportunities.Status.STALE,world,first.captured(),List.of(),"Quarry evidence is future or stale; fresh loaded-chunk evidence required");
        var rows=new LinkedHashMap<Position,MineralOpportunities.Evidence>();
        try(var s=c.prepareStatement("""
            SELECT o.*,f.captured_millis AS captured,
              EXISTS(SELECT 1 FROM player_ore_placements p WHERE p.world_id=o.world_id AND p.x=o.x AND p.y=o.y AND p.z=o.z)
              OR EXISTS(SELECT 1 FROM ore_source_removals q WHERE q.world_id=o.world_id AND q.x=o.x AND q.y=o.y AND q.z=o.z) AS excluded,
              EXISTS(SELECT 1 FROM mineral_evidence e WHERE e.world_id=o.world_id AND e.x=o.x AND e.y=o.y AND e.z=o.z) AS claimed
            FROM quarry_survey_members m JOIN quarry_surveys f USING(settlement_id,world_id,scope_x,scope_z)
              JOIN quarry_opportunities o USING(settlement_id,world_id,x,y,z)
            WHERE m.settlement_id=? AND f.captured_millis<=? AND f.captured_millis>=?
            ORDER BY o.x,o.y,o.z,f.captured_millis LIMIT 16385
            """)) {
            s.setString(1,town.toString());s.setLong(2,now);s.setLong(3,Math.max(0,now-lifetime));int count=0;
            try(var r=s.executeQuery()){while(r.next()) {
                if(++count>16384)throw new SQLException("Current quarry coordinate review budget exceeded");
                if(r.getBoolean("excluded")||r.getBoolean("claimed"))continue;
                var p=new Position(r.getInt("x"),r.getInt("y"),r.getInt("z"));
                rows.put(p,new MineralOpportunities.Evidence(world,p,new Commodity(r.getString("mineral")),r.getLong("first_seen_millis"),r.getLong("captured"),false,null));
            }}
        }
        long captured=current.stream().mapToLong(Frame::captured).min().orElseThrow();
        return view(MineralOpportunities.Status.CURRENT,world,captured,List.copyOf(rows.values()),"Current complete quarry chunk frames: "+current.size()+"; "+rows.size()+" visible unclaimed coordinates; no historical union");
    }
    private static void key(PreparedStatement s,UUID town,int x,int z)throws SQLException {s.setString(1,town.toString());s.setInt(2,x);s.setInt(3,z);}
    private static MineralOpportunities.Review view(MineralOpportunities.Status status,UUID world,long captured,List<MineralOpportunities.Evidence> rows,String reason){return new MineralOpportunities.Review(status,world,captured,rows,reason);}
}
