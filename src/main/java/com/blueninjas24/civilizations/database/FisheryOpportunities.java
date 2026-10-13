package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** SQL-owner read-only first-firm evidence. Shoreline/skills and actual acceptance remain separate. */
public final class FisheryOpportunities {
    public record Opportunity(UUID town,WaterPatch.Patch water,long fundedFoodNeed) {}
    public record Review(List<Opportunity> candidates,String explanation){public Review{candidates=List.copyOf(candidates);}}
    private final Connection connection;
    private final FisheryOpportunityPolicy policy;
    private final WaterObserverPolicy water;
    private final FishingPolicy fishing;
    private final IndustryRuntimePolicy industry;
    private final StartupGrantPolicy grants;
    private final VillageSupplyPolicy customers;
    private final StudyPolicy study;
    private final BusinessOperationPolicy operations;
    public FisheryOpportunities(Connection connection,FisheryOpportunityPolicy policy,WaterObserverPolicy water,FishingPolicy fishing,
            IndustryRuntimePolicy industry,StartupGrantPolicy grants,VillageSupplyPolicy customers,StudyPolicy study,BusinessOperationPolicy operations) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.water=Objects.requireNonNull(water);
        this.fishing=Objects.requireNonNull(fishing);this.industry=Objects.requireNonNull(industry);this.grants=Objects.requireNonNull(grants);
        this.customers=Objects.requireNonNull(customers);this.study=Objects.requireNonNull(study);this.operations=Objects.requireNonNull(operations);
    }
    public Review review(UUID town,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(town);if(now<0)throw new IllegalArgumentException("Invalid fishery review time");
        if(!connection.getAutoCommit())throw new SQLException("Fishery review requires idle SQL owner");
        connection.setAutoCommit(false);
        try{var result=reviewRows(town,now,worlds==null?Set.of():Set.copyOf(worlds));connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    Review reviewRows(UUID town,long now,Set<UUID> worlds)throws SQLException {return reviewRows(town,now,worlds,false);}
    Review reviewForTrainingRows(UUID town,long now,Set<UUID> worlds)throws SQLException {return reviewRows(town,now,worlds,true);}
    private Review reviewRows(UUID town,long now,Set<UUID> worlds,boolean nextPeriodPractice)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Fishery review rows require owner transaction");
        if(!policy.enabled()||!water.enabled()||!fishing.enabled()||!industry.enabled())return no("First-fishery review, water recognition or timed fishing disabled");
        if(policy.maximumColumns()<fishing.minimumWaterColumns())return no("Configured fishery footprint cannot meet habitat minimum");
        var foodWeight=industry.mining().storageSpacePerUnit().get(Commodity.FOOD);
        if(foodWeight==null||foodWeight>policy.capacity())return no("Configured storage cannot hold caught food");
        try(var s=connection.prepareStatement("SELECT 1 FROM businesses WHERE settlement_id=? AND industry='fishing' LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())return no("Saved fishery identity retained; first-firm review cannot bypass operation, vacancy or competition");}
        }
        var funding=new BusinessStartupGrants(connection).previewRows(town,"fishing",policy.startupHundredths(),policy.capitalHundredths(),grants);
        if(!funding.eligible())return no("Fishery funding unavailable: "+funding.reason());
        var purchases=new VillageSupplyPurchases(connection,customers,study,operations);long debit=Math.addExact(policy.startupHundredths(),policy.capitalHundredths());
        var customer=nextPeriodPractice?purchases.demandForTrainingAfterSpending(town,worlds,debit):purchases.demandAfterSpending(town,worlds,debit);
        long need=customer.fundedUnits().getOrDefault(Commodity.FOOD,0L);
        if(need==0)return no("No real funded food customer after proposed startup spending: "+customer.explanation());
        UUID townWorld;
        try(var s=connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Fishery settlement lacks permanent world");townWorld=UUID.fromString(r.getString(1));}
        }
        // Count goods already owned by other eligible local suppliers before proposing new capacity.
        var suppliers=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT b.id FROM businesses b JOIN business_sites p ON p.id=b.site_id
              JOIN industry_stock k ON k.business_id=b.id AND k.commodity='food' AND k.units>0
            WHERE b.settlement_id=? AND NOT EXISTS(SELECT 1 FROM business_closures x WHERE x.business_id=b.id)
            ORDER BY b.id LIMIT 65
            """)) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next())suppliers.add(UUID.fromString(r.getString(1)));}
        }
        if(suppliers.size()>64)return no("Local food supplier budget exceeded; no incomplete investment decision");
        var firms=new BusinessRepository(connection);var people=new PopulationRepository(connection);var operating=new BusinessOperations(connection,operations);
        for(var id:suppliers) {
            var firm=firms.findFounding(id).orElseThrow();
            if(!firm.site().worldId().equals(townWorld)||!worlds.contains(firm.site().worldId())||!operating.evaluate(id).permitted()||WorkplaceOperations.paused(connection,id))continue;
            boolean staffed=false;
            for(var job:firms.currentWorkers(id)) {
                var person=people.findResident(job.residentId()).orElseThrow();
                if(person.settlementId().equals(town)&&WorkEligibility.evaluate(person,people.findWorkProfile(person.id()).orElseThrow(),firm.workerRequirements()).eligible()){staffed=true;break;}
            }
            if(staffed)need-=Math.min(need,new IndustryRepository(connection).store(id).orElseThrow().units().getOrDefault(Commodity.FOOD,0L));
        }
        if(need==0)return no("Existing eligible local owned food covers funded need; no new fishery justified");
        var patches=new WaterOpportunities(connection).patches(town,now,policy.evidenceLifetimeMillis(),worlds,water.minimumDepth(),fishing.minimumWaterColumns(),policy.maximumColumns(),policy.sitesPerReview());
        int homeX,homeY,homeZ;
        try(var s=connection.prepareStatement("SELECT center_x,center_y,center_z FROM settlements WHERE id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Unknown fishery settlement");homeX=r.getInt(1);homeY=r.getInt(2);homeZ=r.getInt(3);}
        }
        var candidates=new ArrayList<Opportunity>();
        for(var patch:patches) {
            if(Math.abs((long)patch.surfaceY()-homeY)>policy.verticalRange())continue;
            boolean nearby=true;
            for(var p:patch.evidence().columns()) {
                long dx=(long)p.x()-homeX,dz=(long)p.z()-homeZ;
                if(Math.abs(dx)>policy.horizontalRange()||Math.abs(dz)>policy.horizontalRange()||dx*dx+dz*dz>(long)policy.horizontalRange()*policy.horizontalRange()){nearby=false;break;}
            }
            if(nearby)candidates.add(new Opportunity(town,patch,need));
        }
        return new Review(candidates,candidates.isEmpty()?"No fresh connected unclaimed nearby water in bounded review"
                :"Fundable first-fishery water candidates with remaining real food customer after startup and existing suppliers; qualified founder and fresh shoreline/water acceptance still required; nothing reserved");
    }
    private static Review no(String reason){return new Review(List.of(),reason);}
}
