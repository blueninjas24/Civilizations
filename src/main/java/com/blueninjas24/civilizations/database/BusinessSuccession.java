package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.Business;
import com.blueninjas24.civilizations.population.*;
import java.sql.*;
import java.util.*;

/** Atomic qualified succession on the SQL owner. No invented skills, funds or family relationships. */
public final class BusinessSuccession {
    public enum Kind { FAMILY, EMPLOYEE }
    public record Candidate(UUID resident, Kind kind) {}
    public record Transfer(UUID operation, UUID business, UUID previousOwner, UUID nextOwner, long step, Kind kind) {}
    private final Connection connection;
    private final BusinessRepository businesses;
    private final PopulationRepository population;
    public BusinessSuccession(Connection connection) throws SQLException {
        this.connection=Objects.requireNonNull(connection);
        businesses=new BusinessRepository(connection);population=new PopulationRepository(connection);
    }
    /** Family first; deterministic UUID tie-breaking within each eligible group. No successor leaves ownership unchanged. */
    public Optional<Transfer> succeed(UUID operation, UUID business, long step) throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(business);
        if(step<0)throw new IllegalArgumentException("Negative succession step");
        if(!connection.getAutoCommit())throw new SQLException("Succession requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var replay=find(operation);
            Optional<Transfer> result;
            if(replay.isPresent()) {
                var old=replay.orElseThrow();
                if(!old.business().equals(business)||old.step()!=step)throw new SQLException("Succession operation reused with different terms");
                result=replay;
            } else result=transferRows(operation,business,step);
            connection.commit();connection.setAutoCommit(true);return result;
        }catch(SQLException|RuntimeException failure) {
            try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}
            throw failure;
        }
    }
    private Optional<Transfer> transferRows(UUID operation,UUID id,long step)throws SQLException {
        var business=businesses.findBusiness(id).orElseThrow(()->new SQLException("Unknown business"));
        if(step<business.foundedStep())throw new SQLException("Succession predates founding");
        var owner=population.findResident(business.ownerId()).orElseThrow(()->new SQLException("Missing business owner"));
        if(owner.status()!=Resident.Status.DECEASED)return Optional.empty();
        var selected=candidate(id);
        if(selected.isEmpty())return Optional.empty();
        UUID successor=selected.orElseThrow().resident();Kind kind=selected.orElseThrow().kind();
        try(var s=connection.prepareStatement("UPDATE businesses SET owner_id=? WHERE id=? AND owner_id=?")) {
            s.setString(1,successor.toString());s.setString(2,id.toString());s.setString(3,owner.id().toString());
            if(s.executeUpdate()!=1)throw new SQLException("Stale business owner");
        }
        var result=new Transfer(operation,id,owner.id(),successor,step,kind);
        try(var s=connection.prepareStatement("INSERT INTO business_ownership VALUES(?,?,?,?,?,?)")) {
            s.setString(1,operation.toString());s.setString(2,id.toString());s.setString(3,owner.id().toString());
            s.setString(4,successor.toString());s.setLong(5,step);s.setString(6,kind.name());s.executeUpdate();
        }
        return Optional.of(result);
    }
    /** Read-only selection also prevents resale from bypassing an available successor. */
    public Optional<Candidate> candidate(UUID id)throws SQLException {
        var business=businesses.findBusiness(id).orElseThrow(()->new SQLException("Unknown business"));
        var owner=population.findResident(business.ownerId()).orElseThrow();
        if(owner.status()!=Resident.Status.DECEASED)return Optional.empty();
        var family=new TreeSet<UUID>(Comparator.comparing(UUID::toString));
        for(var tie:population.findFamilyTies(owner.id()))family.add(tie.first().equals(owner.id())?tie.second():tie.first());
        family.addAll(population.findSiblings(owner.id()));
        if(family.size()>256)throw new SQLException("Family succession review budget exceeded");
        var requirements=businesses.findFounding(id).orElseThrow().founderRequirements();
        for(var relative:family)if(qualified(relative,business,requirements))return Optional.of(new Candidate(relative,Kind.FAMILY));
        for(var employee:businesses.currentWorkers(id))if(qualified(employee.residentId(),business,requirements))return Optional.of(new Candidate(employee.residentId(),Kind.EMPLOYEE));
        return Optional.empty();
    }
    boolean qualifiedBuyer(UUID resident,UUID business)throws SQLException {
        var b=businesses.findBusiness(business).orElseThrow(()->new SQLException("Unknown business"));
        return qualified(resident,b,businesses.findFounding(business).orElseThrow().founderRequirements());
    }
    private boolean qualified(UUID candidate,Business business,JobRequirements requirements)throws SQLException {
        var person=population.findResident(candidate).orElseThrow(()->new SQLException("Missing succession family member"));
        if(!person.settlementId().equals(business.settlementId())
                ||!WorkEligibility.evaluate(person,population.findWorkProfile(candidate).orElseThrow(),requirements).eligible())return false;
        // Inheritance does not silently abandon an existing occupation in another industry.
        try(var s=connection.prepareStatement("SELECT 1 FROM business_employment e JOIN businesses b ON b.id=e.business_id WHERE e.resident_id=? AND e.ended_step IS NULL AND b.industry<>? LIMIT 1")) {
            s.setString(1,candidate.toString());s.setString(2,business.industry());try(var r=s.executeQuery()){return !r.next();}
        }
    }
    public Optional<Transfer> find(UUID operation)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM business_ownership WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                return r.next()?Optional.of(new Transfer(operation,UUID.fromString(r.getString("business_id")),
                        UUID.fromString(r.getString("previous_owner")),UUID.fromString(r.getString("next_owner")),
                        r.getLong("observed_step"),Kind.valueOf(r.getString("kind")))):Optional.empty();
            }
        }
    }
}
