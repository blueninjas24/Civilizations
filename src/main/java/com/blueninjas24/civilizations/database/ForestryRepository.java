package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Trusted SQL-owner forestry resources. No automatic adoption, block rights or live-origin certification. */
public final class ForestryRepository {
    public record Resource(UUID site,UUID world,ForestStock stock,long observedMillis,String openingTerms) {}
    private final Connection c;
    public ForestryRepository(Connection c){this.c=Objects.requireNonNull(c);}
    public Resource open(UUID site,ForestObservation evidence,ForestryPolicy policy)throws SQLException {
        return transaction(()->openRows(site,evidence,policy));
    }
    Resource openRows(UUID site,ForestObservation evidence,ForestryPolicy policy)throws SQLException {
        requireRows();Objects.requireNonNull(site);Objects.requireNonNull(evidence);Objects.requireNonNull(policy);
        if(!evidence.complete())throw new SQLException("Forestry requires complete positive evidence and caller-certified provenance");
        try(var s=c.prepareStatement("SELECT world_id FROM business_sites WHERE id=?")) {
            s.setString(1,site.toString());try(var r=s.executeQuery()) {
                if(!r.next()||!evidence.world().toString().equals(r.getString(1)))throw new SQLException("Forest must match saved workplace world");
            }
        }
        var old=find(site);
        if(old.isPresent()) {
            if(!supports(site).equals(evidence.trunks()))throw new SQLException("Forest footprint changed; explicit transition required");
            return old.orElseThrow(); // A revisit, retune or restart cannot refill or rescale standing timber.
        }
        var stock=policy.opening(evidence);
        try(var s=c.prepareStatement("INSERT INTO forestry_resources VALUES(?,?,?,?,?,?,?,?)")) {
            s.setString(1,site.toString());s.setString(2,evidence.world().toString());s.setLong(3,stock.capacity());s.setLong(4,stock.capacity());
            s.setLong(5,stock.available());s.setInt(6,0);s.setLong(7,evidence.observedMillis());s.setString(8,policy.toString());s.executeUpdate();
        }
        try(var s=c.prepareStatement("INSERT INTO forestry_supports VALUES(?,?,?,?,?)")) {
            for(var p:evidence.trunks()){s.setString(1,evidence.world().toString());s.setInt(2,p.x());s.setInt(3,p.y());s.setInt(4,p.z());s.setString(5,site.toString());s.executeUpdate();}
        }
        return find(site).orElseThrow();
    }
    public Optional<Resource> find(UUID site)throws SQLException {
        try(var s=c.prepareStatement("SELECT * FROM forestry_resources WHERE site_id=?")) {
            s.setString(1,site.toString());try(var r=s.executeQuery()) {
                return r.next()?Optional.of(new Resource(site,UUID.fromString(r.getString("world_id")),
                        new ForestStock(r.getLong("habitat_capacity"),r.getLong("available"),r.getInt("remainder")),
                        r.getLong("observed_ms"),r.getString("opening_terms"))):Optional.empty();
            }
        }
    }
    public List<Position> supports(UUID site)throws SQLException {
        var result=new ArrayList<Position>();try(var s=c.prepareStatement("SELECT x,y,z FROM forestry_supports WHERE site_id=? ORDER BY x,y,z")) {
            s.setString(1,site.toString());try(var r=s.executeQuery()){while(r.next())result.add(new Position(r.getInt(1),r.getInt(2),r.getInt(3)));}
        }return List.copyOf(result);
    }
    int qualifiedWorkers(UUID business,BusinessOperationPolicy policy)throws SQLException {
        if(!new BusinessOperations(c,policy).evaluate(business).permitted())return 0;
        var firms=new BusinessRepository(c);var people=new PopulationRepository(c);
        var requirements=firms.findFounding(business).orElseThrow().workerRequirements();int count=0;
        for(var job:firms.currentWorkers(business)) {
            var person=people.findResident(job.residentId()).orElseThrow();
            if(com.blueninjas24.civilizations.population.WorkEligibility.evaluate(person,people.findWorkProfile(person.id()).orElseThrow(),requirements).eligible())count++;
        }return count;
    }
    void saveWorkRows(UUID operation,UUID business,Resource before,long period,ForestStock.Period work,ForestryPolicy policy)throws SQLException {
        requireRows();
        try(var s=c.prepareStatement("UPDATE forestry_resources SET available=?,remainder=? WHERE site_id=? AND available=? AND remainder=? AND habitat_capacity=?")) {
            s.setLong(1,work.stock().available());s.setInt(2,work.stock().recoveryRemainder());s.setString(3,before.site().toString());
            s.setLong(4,before.stock().available());s.setInt(5,before.stock().recoveryRemainder());s.setLong(6,before.stock().capacity());
            if(s.executeUpdate()!=1)throw new SQLException("Forest changed during owner work");
        }
        try(var s=c.prepareStatement("INSERT INTO forestry_work VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,operation.toString());s.setString(2,before.site().toString());s.setString(3,business.toString());s.setLong(4,period);
            s.setLong(5,before.stock().available());s.setLong(6,work.recovered());s.setLong(7,work.harvested());s.setLong(8,work.stock().available());
            s.setInt(9,before.stock().recoveryRemainder());s.setInt(10,work.stock().recoveryRemainder());s.setString(11,policy.toString());s.executeUpdate();
        }
    }
    /** Every original coordinate needs a fresh authoritative verdict. Positive-only ForestScan results are insufficient. */
    public ForestStock.HabitatChange habitat(UUID operation,UUID site,UUID world,long captured,Map<Position,Boolean> evidence)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(world);
        var sorted=new TreeMap<Position,Boolean>(Position.ORDER);sorted.putAll(Map.copyOf(evidence));
        if(captured<0)throw new IllegalArgumentException("Invalid forest capture time");
        String terms=world+":"+captured+":"+sorted;
        return transaction(()-> {
            try(var s=c.prepareStatement("SELECT * FROM forestry_habitat_changes WHERE operation_id=?")) {
                s.setString(1,operation.toString());try(var r=s.executeQuery()) {if(r.next()) {
                    if(!site.toString().equals(r.getString("site_id"))||!terms.equals(r.getString("evidence")))throw new SQLException("Forest habitat operation reused with different evidence");
                    return new ForestStock.HabitatChange(new ForestStock(r.getLong("before_capacity"),r.getLong("opening_units"),r.getInt("before_remainder")),
                            new ForestStock(r.getLong("after_capacity"),r.getLong("closing_units"),r.getInt("after_remainder")),r.getLong("lost"));
                }}
            }
            var before=find(site).orElseThrow(()->new SQLException("Unknown forest"));
            if(!world.equals(before.world())||captured<=before.observedMillis()||!supports(site).equals(List.copyOf(sorted.keySet())))
                throw new SQLException("Forest habitat requires fresh authoritative evidence for every original support");
            long original;try(var s=c.prepareStatement("SELECT capacity FROM forestry_resources WHERE site_id=?")) {
                s.setString(1,site.toString());try(var r=s.executeQuery()){r.next();original=r.getLong(1);}
            }
            var change=before.stock().habitat(original,sorted.size(),(int)sorted.values().stream().filter(Boolean::booleanValue).count());
            try(var s=c.prepareStatement("UPDATE forestry_resources SET habitat_capacity=?,available=?,remainder=?,observed_ms=? WHERE site_id=?")) {
                s.setLong(1,change.after().capacity());s.setLong(2,change.after().available());s.setInt(3,change.after().recoveryRemainder());s.setLong(4,captured);s.setString(5,site.toString());s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO forestry_habitat_changes VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,operation.toString());s.setString(2,site.toString());s.setString(3,terms);s.setLong(4,captured);
                s.setLong(5,change.before().capacity());s.setLong(6,change.after().capacity());s.setLong(7,change.before().available());s.setLong(8,change.lost());
                s.setLong(9,change.after().available());s.setInt(10,change.before().recoveryRemainder());s.setInt(11,change.after().recoveryRemainder());s.executeUpdate();
            }return change;
        });
    }
    private void requireRows()throws SQLException {if(c.getAutoCommit())throw new SQLException("Forestry rows require owner transaction");}
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!c.getAutoCommit())throw new SQLException("Forestry requires idle SQL owner");c.setAutoCommit(false);
        try{var result=work.run();c.commit();c.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
