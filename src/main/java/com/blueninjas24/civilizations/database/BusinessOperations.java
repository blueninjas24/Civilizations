package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.BusinessOperationPolicy;
import com.blueninjas24.civilizations.population.*;
import java.sql.*;
import java.util.*;

/** Owner availability and actual existing staff; no hiring, promotion, money or world access. */
public final class BusinessOperations {
    public record Availability(boolean permitted,String explanation) {}
    private final BusinessRepository businesses;
    private final Connection connection;
    private final PopulationRepository population;
    private final BusinessOperationPolicy policy;
    public BusinessOperations(Connection connection,BusinessOperationPolicy policy)throws SQLException {
        this.connection=connection;
        businesses=new BusinessRepository(connection);population=new PopulationRepository(connection);this.policy=Objects.requireNonNull(policy);
    }
    public Availability evaluate(UUID id)throws SQLException {
        if(BusinessClosure.blocked(connection,id))return new Availability(false,"Closed or unsold deadline expired; assets/history retained");
        if(WorkplaceOperations.paused(connection,id))return new Availability(false,"Paused: workplace marker missing; restore the same saved anchor; assets/history retained");
        var b=businesses.findBusiness(id).orElseThrow(()->new SQLException("Unknown operating business"));
        var owner=population.findResident(b.ownerId()).orElseThrow(()->new SQLException("Missing business owner"));
        if(BusinessOwnerScope.available(connection,owner,b))
            return new Availability(true,"Living owner in the same world; workplace and employees remain local");
        if(owner.status()!=Resident.Status.DECEASED||!policy.enabled()||!policy.continueStaffedAfterOwnerDeath())
            return new Availability(false,"Paused: owner unavailable; inheritance not assumed");
        var requirements=businesses.findFounding(id).orElseThrow().workerRequirements();
        for(var job:businesses.currentWorkers(id)) {
            var person=population.findResident(job.residentId()).orElseThrow();
            if(WorkEligibility.evaluate(person,population.findWorkProfile(person.id()).orElseThrow(),requirements).eligible())
                return new Availability(true,"Qualified existing staff operating after owner death; succession/resale pending");
        }
        return new Availability(false,"Paused: deceased owner and no qualified existing staff");
    }
}
