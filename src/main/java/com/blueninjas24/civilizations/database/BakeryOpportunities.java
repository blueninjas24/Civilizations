package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Read-only first-bakery evidence. Does not reserve money, grant skills or activate automatic founding. */
public final class BakeryOpportunities {
    public record Opportunity(UUID town,WorkplaceOpportunities.Candidate workplace,ProductionRecipe recipe,
                              WorkshopStarterPlan.Review starter,long fundedFoodUnits,long foodPeriod,BakeryInvestment.PrivateInputs privateInputs) {}
    public record Review(List<Opportunity> candidates,String explanation) {public Review{candidates=List.copyOf(candidates);}}
    private final Connection connection;
    private final WorkshopOpportunityPolicy policy;
    private final IndustryRuntimePolicy industry;
    private final StartupGrantPolicy grants;
    private final WorkshopStarterPolicy starter;
    private final VillageSupplyPolicy customers;
    private final StudyPolicy study;
    private final BusinessOperationPolicy operations;
    private final BakeryMarketPolicy market;
    public BakeryOpportunities(Connection connection,WorkshopOpportunityPolicy policy,IndustryRuntimePolicy industry,
            StartupGrantPolicy grants,WorkshopStarterPolicy starter,VillageSupplyPolicy customers,
            StudyPolicy study,BusinessOperationPolicy operations) {
        this(connection,policy,industry,grants,starter,customers,study,operations,BakeryMarketPolicy.disabled());
    }
    public BakeryOpportunities(Connection connection,WorkshopOpportunityPolicy policy,IndustryRuntimePolicy industry,
            StartupGrantPolicy grants,WorkshopStarterPolicy starter,VillageSupplyPolicy customers,
            StudyPolicy study,BusinessOperationPolicy operations,BakeryMarketPolicy market) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.industry=Objects.requireNonNull(industry);
        this.grants=Objects.requireNonNull(grants);this.starter=Objects.requireNonNull(starter);this.customers=Objects.requireNonNull(customers);
        this.study=Objects.requireNonNull(study);this.operations=Objects.requireNonNull(operations);
        this.market=Objects.requireNonNull(market);
    }
    public Review review(UUID town,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(town);if(now<0)throw new IllegalArgumentException("Negative bakery evidence time");
        if(!connection.getAutoCommit())throw new SQLException("Bakery review requires idle SQL owner");
        connection.setAutoCommit(false);
        try{var result=reviewRows(town,now,worlds==null?Set.of():Set.copyOf(worlds));connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    /** Acceptance must repeat this review inside its complete founding transaction and recheck the loaded marker. */
    Review reviewRows(UUID town,long now,Set<UUID> worlds)throws SQLException {
        return reviewRows(town,now,worlds,false);
    }
    Review reviewForTrainingRows(UUID town,long now,Set<UUID> worlds)throws SQLException {return reviewRows(town,now,worlds,true);}
    private Review reviewRows(UUID town,long now,Set<UUID> worlds,boolean training)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Bakery evidence rows require owner transaction");
        if(!policy.enabled()||!industry.enabled()||!industry.supply().enabled()||!starter.enabled()||!customers.enabled())return no("Bakery investment, processing, input supply, starter or public customers disabled");
        var markers=new WorkplaceOpportunities(connection).review(town,now,policy.evidenceLifetimeMillis(),worlds,policy.markersPerReview(),Set.of(WorkplaceObservation.Kind.SMOKER));
        if(markers.candidates().isEmpty())return no(markers.explanation());
        try(var s=connection.prepareStatement("SELECT 1 FROM businesses WHERE settlement_id=? AND industry='bakery' LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())return no("Existing bakery identity retained; first-firm review cannot bypass closure, vacancy or competition");}
        }
        // A new bakery starts with its simplest meal recipe; optional dairy never blocks bread.
        var recipe=BakeryRecipes.baseline(industry.recipes().values().stream()
                .filter(r->r.industry().equals("bakery")&&r.outputs().keySet().equals(Set.of(Commodity.FOOD))).toList());
        if(recipe.isEmpty())return no("Configured bakery meal recipe required; ingredients are not meals");
        var funding=new BusinessStartupGrants(connection).previewRows(town,"bakery",policy.startupHundredths(),policy.capitalHundredths(),grants);
        if(!funding.eligible())return no("Bakery startup funding unavailable: "+funding.reason());
        long debit=Math.addExact(policy.startupHundredths(),policy.capitalHundredths());
        var buyers=new VillageSupplyPurchases(connection,customers,study,operations);
        var demand=training?buyers.demandForTrainingAfterSpending(town,worlds,debit):buyers.demandAfterSpending(town,worlds,debit);
        long wanted=demand.fundedUnits().getOrDefault(Commodity.FOOD,0L);
        long competing=buyers.competingFoodUnits(town,worlds,debit,market,training);
        if(wanted==0&&competing==0)return no("No real funded village meal/reserve customer or recurring paid food orders after startup spending: "+demand.explanation());
        var supply=availableFood(town,markers.candidates().getFirst().world(),worlds);if(supply.isEmpty())return no("Local meal supplier review cap exceeded; no partial shortage inferred");
        long uncovered=Math.max(Math.max(0,wanted-supply.orElseThrow()),competing);var selected=recipe.orElseThrow();
        if(uncovered<selected.outputs().get(Commodity.FOOD))return no("Existing available meals cover demand or remaining funded need is smaller than one bakery batch");
        var stocks=WorkshopPublicStock.read(connection,town);var weights=industry.mining().storageSpacePerUnit();
        long outputSpace;
        try{var weight=weights.get(Commodity.FOOD);if(weight==null)return no("Configured bakery storage lacks meals");outputSpace=Math.multiplyExact(weight,selected.outputs().get(Commodity.FOOD));}
        catch(ArithmeticException overflow){return no("Bakery output storage exceeds supported range");}
        if(outputSpace>policy.capacity())return no("One bakery output batch cannot fit configured storage");
        var bundle=WorkshopStarterPlan.review(selected,1,starter,policy.capitalHundredths(),policy.capacity(),industry.mining().localHaulUnitsPerPeriod(),stocks,weights);
        BakeryInvestment.PrivateInputs privateInputs=null;
        if(!bundle.eligible()) {
            var privateReview=new BakeryPrivateInputs(connection,starter,operations).review(town,markers.candidates().getFirst().world(),selected,policy.capitalHundredths(),policy.capacity(),industry.mining().localHaulUnitsPerPeriod(),weights);
            if(privateReview.quote()==null)return no("Bakery paid starter inputs unavailable: "+bundle.explanation()+"; "+privateReview.explanation());
            bundle=privateReview.bundle();privateInputs=privateReview.quote();
        }
        Position center;
        try(var s=connection.prepareStatement("SELECT center_x,center_y,center_z FROM settlements WHERE id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Missing bakery settlement");center=new Position(r.getInt(1),r.getInt(2),r.getInt(3));}
        }
        var found=new ArrayList<Opportunity>();
        for(var marker:markers.candidates()) {
            var p=marker.position();long dx=(long)p.x()-center.x(),dz=(long)p.z()-center.z();
            if(Math.abs(dx)>policy.horizontalRange()||Math.abs(dz)>policy.horizontalRange()
                    ||dx*dx+dz*dz>(long)policy.horizontalRange()*policy.horizontalRange()||Math.abs((long)p.y()-center.y())>policy.verticalRange())continue;
            found.add(new Opportunity(town,marker,selected,bundle,uncovered,demand.period(),privateInputs));
        }
        return new Review(found,found.isEmpty()?"No nearby available smoker in bounded bakery review"
                :"First-bakery prospects have real funded meals demand and paid surplus inputs; qualified founder and fresh loaded-marker acceptance remain required; no assets changed");
    }
    /** Existing actual stock can satisfy demand first. Unknown/over-budget evidence never proves a gap. */
    private OptionalLong availableFood(UUID town,UUID world,Set<UUID> worlds)throws SQLException {
        var ids=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
                SELECT b.id FROM businesses b JOIN industry_stock k ON k.business_id=b.id
                WHERE b.settlement_id=? AND k.commodity='food' AND k.units>0 ORDER BY b.id LIMIT 65
                """)) {s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}}
        if(ids.size()>64)return OptionalLong.empty();
        long food=0;var businesses=new BusinessRepository(connection);var population=new PopulationRepository(connection);
        for(var id:ids) {
            var founding=businesses.findFounding(id).orElseThrow();
            if(!world.equals(founding.site().worldId())||!worlds.contains(world)||!new BusinessOperations(connection,operations).evaluate(id).permitted())continue;
            boolean qualified=false;
            for(var job:businesses.currentWorkers(id)) {
                var resident=population.findResident(job.residentId()).orElseThrow();
                if(resident.settlementId().equals(town)&&WorkEligibility.evaluate(resident,population.findWorkProfile(resident.id()).orElseThrow(),founding.workerRequirements()).eligible()){qualified=true;break;}
            }
            if(qualified)food=Math.addExact(food,new IndustryRepository(connection).store(id).orElseThrow().units().get(Commodity.FOOD));
        }
        return OptionalLong.of(food);
    }
    private static Review no(String explanation){return new Review(List.of(),explanation);}
}
