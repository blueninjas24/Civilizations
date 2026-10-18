package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Sticky protective evidence and confirmed source retirement; neither creates ownership nor refills forests. */
public final class ForestryProvenance {
    public record Result(int exclusions,int retired,long lostUnits) {}
    private final Connection c;
    public ForestryProvenance(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE forestry_tracking (
                    id INTEGER PRIMARY KEY CHECK(id=1),clean_stop INTEGER NOT NULL CHECK(clean_stop IN(0,1)),
                    trusted INTEGER NOT NULL CHECK(trusted IN(0,1)),
                    observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0)
                )
                """);
            s.execute("""
                CREATE TABLE forestry_exclusions (
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                    observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                    reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 1024),PRIMARY KEY(world_id,x,y,z)
                )
                """);
            s.execute("""
                CREATE TABLE forestry_source_losses (
                    world_id TEXT NOT NULL,x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                    observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                    reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 1024),PRIMARY KEY(world_id,x,y,z),
                    FOREIGN KEY(world_id,x,y,z) REFERENCES forestry_supports(world_id,x,y,z)
                )
                """);
        }
    }
    public boolean excludes(UUID world,Position p)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM forestry_exclusions WHERE world_id=? AND x=? AND y=? AND z=?")) {
            bind(s,world,p);try(var r=s.executeQuery()){return r.next();}
        }
    }
    public Set<Position> aroundChunk(UUID world,int x,int z,int margin,int maximum)throws SQLException {
        if(margin<0||margin>8||maximum<1||maximum>65536)throw new IllegalArgumentException("Invalid forest exclusion budget");
        int ox=Math.multiplyExact(x,16),oz=Math.multiplyExact(z,16);var result=new HashSet<Position>();
        try(var s=c.prepareStatement("SELECT x,y,z FROM forestry_exclusions WHERE world_id=? AND x BETWEEN ? AND ? AND z BETWEEN ? AND ? ORDER BY x,y,z LIMIT ?")) {
            s.setString(1,world.toString());s.setInt(2,Math.subtractExact(ox,margin));s.setInt(3,Math.addExact(ox,15+margin));
            s.setInt(4,Math.subtractExact(oz,margin));s.setInt(5,Math.addExact(oz,15+margin));s.setInt(6,maximum+1);
            try(var r=s.executeQuery()){while(r.next()){if(result.size()==maximum)throw new SQLException("Forest exclusion read budget exceeded; no truncated evidence");result.add(new Position(r.getInt(1),r.getInt(2),r.getInt(3)));}}
        }return Set.copyOf(result);
    }
    public Result record(UUID world,Collection<Position> placements,Collection<Position> removals,long millis,String reason)throws SQLException {
        Objects.requireNonNull(world);var placed=List.copyOf(placements);var removed=List.copyOf(removals);Business.text(reason,1024);
        if(millis<0||placed.size()>512||removed.size()>512||placed.isEmpty()&&removed.isEmpty()
                ||new HashSet<>(placed).size()!=placed.size()||new HashSet<>(removed).size()!=removed.size())throw new IllegalArgumentException("Invalid bounded forest mutation");
        if(!c.getAutoCommit())throw new SQLException("Forest mutation requires idle SQL owner");c.setAutoCommit(false);
        try {
            int excluded=0,retired=0;long lost=0;var all=new LinkedHashSet<>(placed);all.addAll(removed);
            try(var s=c.prepareStatement("INSERT INTO forestry_exclusions VALUES(?,?,?,?,?,?) ON CONFLICT(world_id,x,y,z) DO NOTHING")) {
                for(var p:all){bind(s,world,p);s.setLong(5,millis);s.setString(6,reason);excluded+=s.executeUpdate();}
            }
            var affected=new TreeMap<UUID,List<Position>>();
            for(var p:removed) {
                UUID site=null;
                try(var s=c.prepareStatement("SELECT site_id FROM forestry_supports WHERE world_id=? AND x=? AND y=? AND z=?")) {
                    bind(s,world,p);try(var r=s.executeQuery()){if(r.next())site=UUID.fromString(r.getString(1));}
                }
                if(site==null)continue;
                try(var s=c.prepareStatement("INSERT INTO forestry_source_losses VALUES(?,?,?,?,?,?) ON CONFLICT(world_id,x,y,z) DO NOTHING")) {
                    bind(s,world,p);s.setLong(5,millis);s.setString(6,reason);
                    if(s.executeUpdate()==1){retired++;affected.computeIfAbsent(site,key->new ArrayList<>()).add(p);}
                }
            }
            var forests=new ForestryRepository(c);
            for(var entry:affected.entrySet()) {
                UUID site=entry.getKey();var before=forests.find(site).orElseThrow();long original,total,count;
                try(var s=c.prepareStatement("SELECT capacity,(SELECT COUNT(*) FROM forestry_supports WHERE site_id=?),(SELECT COUNT(*) FROM forestry_supports p JOIN forestry_source_losses l USING(world_id,x,y,z) WHERE p.site_id=?) FROM forestry_resources WHERE site_id=?")) {
                    for(int i=1;i<=3;i++)s.setString(i,site.toString());try(var r=s.executeQuery()){r.next();original=r.getLong(1);total=r.getLong(2);count=r.getLong(3);}
                }
                if(total<1||count>total)throw new SQLException("Invalid retained forestry support history");
                long capacity=Math.min(before.stock().capacity(),Math.multiplyExact(original,total-count)/total);
                long available=Math.min(before.stock().available(),capacity),delta=before.stock().available()-available;
                var after=new ForestStock(capacity,available,available==capacity?0:before.stock().recoveryRemainder());lost=Math.addExact(lost,delta);
                long observed=Math.max(millis,before.observedMillis());
                try(var s=c.prepareStatement("UPDATE forestry_resources SET habitat_capacity=?,available=?,remainder=?,observed_ms=? WHERE site_id=?")) {
                    s.setLong(1,after.capacity());s.setLong(2,after.available());s.setInt(3,after.recoveryRemainder());s.setLong(4,observed);s.setString(5,site.toString());s.executeUpdate();
                }
                var positions=entry.getValue().stream().sorted(Position.ORDER).toList();String evidence="Confirmed source loss: "+world+":"+positions+":"+millis+":"+reason;
                UUID operation=UUID.nameUUIDFromBytes(("civilizations:forest-source-loss-v1:"+site+":"+positions).getBytes(StandardCharsets.UTF_8));
                try(var s=c.prepareStatement("INSERT INTO forestry_habitat_changes VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                    s.setString(1,operation.toString());s.setString(2,site.toString());s.setString(3,evidence);s.setLong(4,observed);
                    s.setLong(5,before.stock().capacity());s.setLong(6,after.capacity());s.setLong(7,before.stock().available());s.setLong(8,delta);s.setLong(9,after.available());
                    s.setInt(10,before.stock().recoveryRemainder());s.setInt(11,after.recoveryRemainder());s.executeUpdate();
                }
            }
            c.commit();c.setAutoCommit(true);return new Result(excluded,retired,lost);
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private static void bind(PreparedStatement s,UUID world,Position p)throws SQLException {
        s.setString(1,world.toString());s.setInt(2,p.x());s.setInt(3,p.y());s.setInt(4,p.z());
    }
}
