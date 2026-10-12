package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** Read-only actual public farm equipment need; no reservation, contributions or simulated customers. */
public final class FarmEquipmentDemand {
    public record Review(UUID farm,long units,long invoice,String explanation) {}
    private final Connection connection;
    private final FarmToolUpkeepPolicy upkeep;
    private final FarmToolBudgetPolicy budget;
    private final BusinessOperationPolicy operations;
    public FarmEquipmentDemand(Connection connection,FarmToolUpkeepPolicy upkeep,FarmToolBudgetPolicy budget,BusinessOperationPolicy operations) {
        this.connection=Objects.requireNonNull(connection);this.upkeep=Objects.requireNonNull(upkeep);this.budget=Objects.requireNonNull(budget);this.operations=Objects.requireNonNull(operations);
    }
    public Review review(UUID town,Set<UUID> worlds)throws SQLException {
        UUID farm=FarmEnterprises.businessId(town);
        if(!upkeep.enabled()||!budget.enabled())return none(farm,"Public farm equipment demand disabled");
        var businesses=new BusinessRepository(connection);var founding=businesses.findFounding(farm);
        if(founding.isEmpty())return none(farm,"No canonical saved farm");
        var f=founding.orElseThrow();
        if(worlds==null||!worlds.contains(f.site().worldId()))return none(farm,"Farm world absent or availability unknown");
        if(!f.business().industry().equals("farming")||!new BusinessOperations(connection,operations).evaluate(farm).permitted())return none(farm,"Operating public farm required");
        boolean qualified=false;var people=new PopulationRepository(connection);
        for(var job:businesses.currentWorkers(farm)) {
            var person=people.findResident(job.residentId()).orElseThrow();
            if(WorkEligibility.evaluate(person,people.findWorkProfile(person.id()).orElseThrow(),f.workerRequirements()).eligible()){qualified=true;break;}
        }
        if(!qualified)return none(farm,"Qualified current farm staff required");
        var saved=new IndustryRepository(connection).store(farm);
        if(saved.isPresent()) {
            var store=saved.orElseThrow();
            if(store.capacity()!=1||store.hundredths()!=0||!store.spacePerUnit().equals(Map.of(ToolUpkeepPolicy.TOOLS,1L)))return none(farm,"Legacy farm catalog requires explicit transition");
            if(store.freeSpace()<1)return none(farm,"Farm owns a toolkit or paid incoming toolkit");
        }
        var state=ToolUpkeep.State.initial();
        try(var s=connection.prepareStatement("SELECT productive_periods,service_periods,remainder_percent FROM business_tools WHERE business_id=?")) {
            s.setString(1,farm.toString());try(var r=s.executeQuery()){if(r.next())state=new ToolUpkeep.State(r.getLong(1),r.getLong(2),r.getInt(3));}
        }
        if(state.productivePeriods()<upkeep.startupGracePeriods()||state.servicePeriods()>1)return none(farm,"No productive replacement due");
        var account=new CommodityRepository(connection).read(town);if(account.isEmpty())return none(farm,"Food account missing");
        long period=account.orElseThrow().state().completedPeriods(),demand;
        try(var s=connection.prepareStatement("""
            SELECT n.demand,n.fulfilled,n.unassigned_residents+COALESCE((SELECT SUM(h.adults+h.children)
              FROM household_needs h WHERE h.settlement_id=n.settlement_id AND h.period=n.period),0) AS assessed,
              (SELECT COUNT(*) FROM residents r WHERE r.settlement_id=n.settlement_id AND r.status='ALIVE') AS living
            FROM needs_periods n JOIN farm_evidence f ON f.settlement_id=n.settlement_id
              JOIN economy_periods e ON e.settlement_id=n.settlement_id AND e.period=n.period AND e.commodity='food'
              JOIN farm_learning_reviews l ON l.settlement_id=n.settlement_id AND l.period=n.period
            WHERE n.settlement_id=? AND n.period=? AND f.world_id=? AND f.complete=1 AND f.planted_crops>0
              AND e.production>0 AND l.business_id=? AND l.production=e.production
            """)) {
            s.setString(1,town.toString());s.setLong(2,period);s.setString(3,f.site().worldId().toString());s.setString(4,farm.toString());
            try(var r=s.executeQuery()) {
                if(!r.next())return none(farm,"Complete working fields and actual current food output required");
                if(r.getLong("assessed")!=r.getLong("living")||r.getLong("fulfilled")<r.getLong("demand"))return none(farm,"Current fully supplied population assessment required");
                demand=r.getLong("demand");
            }
        }
        long spent;
        try(var s=connection.prepareStatement("SELECT COALESCE(SUM(price),0) FROM farm_tool_purchases WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){spent=r.getLong(1);}
        }
        var cash=new TreasuryRepository(connection).load(town);if(cash.isEmpty())return none(farm,"Village treasury unknown");
        var affordable=budget.review(cash.orElseThrow().hundredths(),demand,spent);
        return affordable.eligible()?new Review(farm,1,budget.toolHundredths(),"One actual funded public farm replacement toolkit; purchasing still rechecks all facts")
                :none(farm,affordable.reason());
    }
    private static Review none(UUID farm,String reason){return new Review(farm,0,0,reason);}
}
