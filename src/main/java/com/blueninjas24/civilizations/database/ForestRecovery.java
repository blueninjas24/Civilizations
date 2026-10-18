package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.ForestStock;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Admin-scoped recovery of saved habitat. It never discovers trees or replenishes standing timber. */
public final class ForestRecovery {
    public record Plan(ForestryRepository.Resource resource,UUID generation,long originalCapacity,
                       List<Position> supports,Set<Position> excluded,Set<Position> retired) {
        public Plan {supports=List.copyOf(supports);excluded=Set.copyOf(excluded);retired=Set.copyOf(retired);}
    }
    public record Verdict(boolean logPresent,boolean suitable) {
        public Verdict {if(suitable&&!logPresent)throw new IllegalArgumentException("Missing tree cannot be suitable");}
    }
    public record Receipt(UUID site,long capacity,long available,long lost) {}
    private final Connection c;
    public ForestRecovery(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){s.execute("""
            CREATE TABLE forest_recoveries (
                operation_id TEXT PRIMARY KEY NOT NULL,site_id TEXT NOT NULL REFERENCES forestry_resources(site_id),
                generation TEXT NOT NULL,actor TEXT NOT NULL CHECK(length(trim(actor)) BETWEEN 1 AND 80),
                observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                before_capacity INTEGER NOT NULL CHECK(before_capacity>=0),after_capacity INTEGER NOT NULL CHECK(after_capacity BETWEEN 0 AND before_capacity),
                opening_units INTEGER NOT NULL CHECK(opening_units>=0),closing_units INTEGER NOT NULL CHECK(closing_units BETWEEN 0 AND opening_units),
                evidence TEXT NOT NULL CHECK(length(evidence)>0)
            )
            """);s.execute("CREATE INDEX forest_recovery_scope ON forest_recoveries(site_id,generation)");}
    }
    public List<UUID> sites()throws SQLException {
        var result=new ArrayList<UUID>();try(var s=c.createStatement();var r=s.executeQuery("SELECT site_id FROM forestry_resources ORDER BY site_id LIMIT 33")){
            while(r.next())result.add(UUID.fromString(r.getString(1)));
        }return List.copyOf(result);
    }
    public Plan prepare(UUID site)throws SQLException {
        var epoch=new ResourceRecoveryEpochs(c).load("forest").filter(ResourceRecoveryEpochs.State::healthy)
                .orElseThrow(()->new SQLException("Current forest capture is unavailable"));
        var resource=new ForestryRepository(c).find(site).orElseThrow(()->new SQLException("Saved forest not found"));
        var supports=new ArrayList<Position>();
        try(var s=c.prepareStatement("SELECT world_id,x,y,z FROM forestry_supports WHERE site_id=? ORDER BY x,y,z LIMIT 513")) {
            s.setString(1,site.toString());try(var r=s.executeQuery()){while(r.next()){
                if(supports.size()==512)throw new SQLException("Forest exceeds complete review budget");
                if(!resource.world().toString().equals(r.getString(1)))throw new SQLException("Forest support world mismatch");
                supports.add(new Position(r.getInt(2),r.getInt(3),r.getInt(4)));
            }}
        }
        if(supports.isEmpty())throw new SQLException("No saved forest supports; cannot invent habitat");
        int minX=supports.stream().mapToInt(Position::x).min().orElseThrow(),maxX=supports.stream().mapToInt(Position::x).max().orElseThrow();
        int minZ=supports.stream().mapToInt(Position::z).min().orElseThrow(),maxZ=supports.stream().mapToInt(Position::z).max().orElseThrow();
        var excluded=new HashSet<Position>();var retired=new HashSet<Position>();long original;
        // Include nearby canopy exclusions, not just the saved trunks; margins cover every allowed geometry radius.
        try(var s=c.prepareStatement("SELECT x,y,z FROM forestry_exclusions WHERE world_id=? AND x BETWEEN ? AND ? AND z BETWEEN ? AND ? ORDER BY x,y,z LIMIT 16385")){
            s.setString(1,resource.world().toString());s.setLong(2,(long)minX-8);s.setLong(3,(long)maxX+8);s.setLong(4,(long)minZ-8);s.setLong(5,(long)maxZ+8);
            try(var r=s.executeQuery()){while(r.next()){if(excluded.size()==16384)throw new SQLException("Canopy protection review exceeds budget");excluded.add(new Position(r.getInt(1),r.getInt(2),r.getInt(3)));}}
        }
        try(var s=c.prepareStatement("SELECT l.x,l.y,l.z FROM forestry_source_losses l JOIN forestry_supports p USING(world_id,x,y,z) WHERE p.site_id=?")){
            s.setString(1,site.toString());try(var r=s.executeQuery()){while(r.next())retired.add(new Position(r.getInt(1),r.getInt(2),r.getInt(3)));}
        }
        try(var s=c.prepareStatement("SELECT capacity FROM forestry_resources WHERE site_id=?")){s.setString(1,site.toString());try(var r=s.executeQuery()){r.next();original=r.getLong(1);}}
        return new Plan(resource,epoch.generation(),original,supports,excluded,retired);
    }
    public boolean permitted(UUID site)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM forest_recoveries r JOIN resource_recovery_epochs e ON e.kind='forest' AND e.generation=r.generation AND e.healthy=1 WHERE r.site_id=? LIMIT 1")){
            s.setString(1,site.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
    public static ForestStock preview(Plan plan,Map<Position,Verdict> current) {
        if(!Set.copyOf(plan.supports()).equals(current.keySet()))throw new IllegalArgumentException("Every original forest support needs a current verdict");
        long suitable=plan.supports().stream().filter(p->current.get(p).suitable()&&!plan.excluded().contains(p)&&!plan.retired().contains(p)).count();
        var before=plan.resource().stock();long capacity=Math.min(before.capacity(),Math.multiplyExact(plan.originalCapacity(),suitable)/plan.supports().size());
        long available=Math.min(before.available(),capacity);
        return new ForestStock(capacity,available,available==capacity?0:before.recoveryRemainder());
    }
    public Receipt approve(UUID operation,String actor,Plan plan,Map<Position,Verdict> evidence,long captured,long now,BooleanSupplier healthy)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(healthy);var current=Map.copyOf(evidence);var after=preview(plan,current);
        if(actor==null||actor.isBlank()||actor.length()>80||captured<0||now<captured||now-captured>30_000)
            throw new IllegalArgumentException("Invalid or expired forest approval");
        if(!c.getAutoCommit())throw new SQLException("Forest recovery requires idle SQL owner");c.setAutoCommit(false);
        try {
            if(!healthy.getAsBoolean()||!prepare(plan.resource().site()).equals(plan))throw new SQLException("Forest/capture changed; review again");
            var before=plan.resource().stock();long observed=Math.max(now,plan.resource().observedMillis());
            // Confirmed missing/replaced trunks are permanent losses. Canopy-only loss reduces habitat without inventing a death.
            for(var p:plan.supports())if(!current.get(p).logPresent()||plan.excluded().contains(p)) {
                try(var s=c.prepareStatement("INSERT INTO forestry_exclusions VALUES(?,?,?,?,?,?) ON CONFLICT(world_id,x,y,z) DO NOTHING")) {
                    coordinate(s,plan.resource().world(),p);s.setLong(5,observed);s.setString(6,"Explicit admin forest recovery source loss");s.executeUpdate();
                }
                try(var s=c.prepareStatement("INSERT INTO forestry_source_losses VALUES(?,?,?,?,?,?) ON CONFLICT(world_id,x,y,z) DO NOTHING")) {
                    coordinate(s,plan.resource().world(),p);s.setLong(5,observed);s.setString(6,"Explicit admin forest recovery source loss");s.executeUpdate();
                }
            }
            try(var s=c.prepareStatement("UPDATE forestry_resources SET habitat_capacity=?,available=?,remainder=?,observed_ms=? WHERE site_id=?")){
                s.setLong(1,after.capacity());s.setLong(2,after.available());s.setInt(3,after.recoveryRemainder());s.setLong(4,observed);s.setString(5,plan.resource().site().toString());s.executeUpdate();
            }
            var ordered=new TreeMap<Position,Verdict>(Position.ORDER);ordered.putAll(current);String terms=plan+"; current="+ordered;
            try(var s=c.prepareStatement("INSERT INTO forestry_habitat_changes VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,operation.toString());s.setString(2,plan.resource().site().toString());s.setString(3,terms);s.setLong(4,observed);
                s.setLong(5,before.capacity());s.setLong(6,after.capacity());s.setLong(7,before.available());s.setLong(8,before.available()-after.available());s.setLong(9,after.available());
                s.setInt(10,before.recoveryRemainder());s.setInt(11,after.recoveryRemainder());s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO forest_recoveries VALUES(?,?,?,?,?,?,?,?,?,?)")){
                s.setString(1,operation.toString());s.setString(2,plan.resource().site().toString());s.setString(3,plan.generation().toString());s.setString(4,actor);s.setLong(5,observed);
                s.setLong(6,before.capacity());s.setLong(7,after.capacity());s.setLong(8,before.available());s.setLong(9,after.available());s.setString(10,terms);s.executeUpdate();
            }
            if(!healthy.getAsBoolean())throw new SQLException("Forest capture failed before commit");
            c.commit();c.setAutoCommit(true);return new Receipt(plan.resource().site(),after.capacity(),after.available(),before.available()-after.available());
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private static void coordinate(PreparedStatement s,UUID world,Position p)throws SQLException {
        s.setString(1,world.toString());s.setInt(2,p.x());s.setInt(3,p.y());s.setInt(4,p.z());
    }
}
