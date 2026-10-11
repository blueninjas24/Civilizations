package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** Read-only SQL-owner input pressure from existing funded workshops. Never reserves or spends assets. */
public final class LocalIndustryDemand {
    public record Customer(UUID business,long batches,Map<Commodity,Long> missing,Map<Commodity,Long> uncovered,long estimatedCost,String explanation) {
        public Customer {missing=Map.copyOf(missing);uncovered=Map.copyOf(uncovered);}
    }
    public record Review(boolean complete,List<Customer> customers,Map<Commodity,Long> uncovered,String explanation) {
        public Review {customers=List.copyOf(customers);uncovered=Collections.unmodifiableMap(new TreeMap<>(uncovered));}
    }
    public record MiningReview(Review demand,MineralOpportunities.Review survey,List<MineralOpportunities.Evidence> candidates) {
        public MiningReview {candidates=List.copyOf(candidates);}
    }
    private final Connection connection;
    private final IndustryRuntimePolicy policy;
    private final BusinessOperationPolicy operations;
    private final java.util.function.BooleanSupplier mutationHealthy;
    public LocalIndustryDemand(Connection connection,IndustryRuntimePolicy policy,BusinessOperationPolicy operations) {
        this(connection,policy,operations,()->true);
    }
    public LocalIndustryDemand(Connection connection,IndustryRuntimePolicy policy,BusinessOperationPolicy operations,java.util.function.BooleanSupplier mutationHealthy) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.operations=Objects.requireNonNull(operations);
        this.mutationHealthy=Objects.requireNonNull(mutationHealthy);
    }
    public MiningReview reviewMining(UUID town,long now,long maximumAgeMillis,Set<UUID> worlds,int businessLimit)throws SQLException {
        var demand=review(town,worlds,businessLimit);var survey=new MineralOpportunities(connection).review(town,now,maximumAgeMillis,worlds);
        var candidates=demand.complete()&&survey.status()==MineralOpportunities.Status.CURRENT
            ?survey.candidates().stream().filter(e->demand.uncovered().getOrDefault(e.mineral(),0L)>0).toList():List.<MineralOpportunities.Evidence>of();
        return new MiningReview(demand,survey,candidates);
    }
    public Review review(UUID town,Set<UUID> worlds,int businessLimit)throws SQLException {
        Objects.requireNonNull(town);worlds=worlds==null?Set.of():Set.copyOf(worlds);
        if(businessLimit<1||businessLimit>64)throw new IllegalArgumentException("Business review budget must be 1..64");
        if(!policy.enabled()||!policy.supply().enabled())return deferred("Timed industry or automatic input supply disabled");
        try(var s=connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()) {
                if(!r.next()||!worlds.contains(UUID.fromString(r.getString(1))))return deferred("Saved world absent or availability unknown");
            }
        }
        var workshops=new ArrayList<UUID>();var warehouses=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT b.id,b.industry FROM businesses b JOIN industry_stores k ON k.business_id=b.id
            WHERE b.settlement_id=? AND (b.industry<>'mining' OR ?=1)
              AND NOT(b.industry='farming' AND k.capacity=1 AND k.hundredths=0
                AND EXISTS(SELECT 1 FROM farm_tool_purchases f WHERE f.farm_id=b.id)
                AND (SELECT COUNT(*) FROM industry_stock c WHERE c.business_id=b.id)=1
                AND EXISTS(SELECT 1 FROM industry_stock c WHERE c.business_id=b.id AND c.commodity='tools' AND c.space_per_unit=1))
            ORDER BY b.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setInt(2,policy.tools().applies("mining")?1:0);s.setInt(3,businessLimit+policy.supply().warehousesPerReview()+1);
            try(var r=s.executeQuery()) {
                while(r.next()) {
                    var kind=r.getString(2);var id=UUID.fromString(r.getString(1));
                    if(kind.equals("warehouse"))warehouses.add(id);
                    else if(kind.equals("mining")&&policy.tools().applies(kind))workshops.add(id);
                    else if(policy.recipes().values().stream().anyMatch(recipe->recipe.industry().equals(kind)))workshops.add(id);
                    else return deferred("Unsupported stored industry requires explicit demand review");
                }
            }
        }
        if(workshops.size()>businessLimit||warehouses.size()>policy.supply().warehousesPerReview())
            return deferred("Workshop/warehouse review budget exceeded; no partial shortage inferred");
        var availability=new BusinessOperations(connection,operations);var industry=new IndustryRepository(connection);
        var businesses=new BusinessRepository(connection);var population=new PopulationRepository(connection);
        var localStock=new TreeMap<Commodity,Long>();
        for(var id:warehouses)if(availability.evaluate(id).permitted())
            industry.store(id).orElseThrow().units().forEach((good,units)->localStock.merge(good,units,Math::addExact));
        var customers=new ArrayList<Customer>();var total=new TreeMap<Commodity,Long>();
        for(var id:workshops) {
            var available=availability.evaluate(id);
            if(!available.permitted()){customers.add(new Customer(id,0,Map.of(),Map.of(),0,available.explanation()));continue;}
            var founding=businesses.findFounding(id).orElseThrow();var requirements=founding.workerRequirements();long workers=0;
            for(var job:businesses.currentWorkers(id)) {
                var person=population.findResident(job.residentId()).orElseThrow();
                if(WorkEligibility.evaluate(person,population.findWorkProfile(person.id()).orElseThrow(),requirements).eligible())workers++;
            }
            var pending=new TreeMap<Commodity,Long>();int incoming=0;
            try(var s=connection.prepareStatement("SELECT commodity,units FROM local_shipments WHERE buyer_id=? AND delivered_period IS NULL ORDER BY id LIMIT 129")) {
                s.setString(1,id.toString());try(var r=s.executeQuery()) {
                    while(r.next()){incoming++;pending.merge(new Commodity(r.getString(1)),r.getLong(2),Math::addExact);}
                }
            }
            if(incoming>128)return deferred("Pending cargo review budget exceeded; no partial shortage inferred");
            var store=industry.store(id).orElseThrow();
            long toolNeed=founding.business().industry().equals("mining")&&!mutationHealthy.getAsBoolean()?0
                    :new BusinessTools(connection,policy).missingFunded(id,founding.business().industry(),workers,store,pending);
            long toolCost=toolNeed*policy.tools().toolValueHundredths();
            var recipe=policy.recipes().values().stream().filter(r->r.industry().equals(founding.business().industry())).findFirst();
            var planned=recipe.isPresent()?WorkshopInputDemand.review(recipe.orElseThrow(),workers,store.hundredths()-toolCost,
                    store.freeSpace()-toolNeed*store.spacePerUnit().getOrDefault(ToolUpkeepPolicy.TOOLS,0L),store.units(),pending,store.spacePerUnit(),policy.purchasingSupply())
                    :new WorkshopInputDemand.Review(0,Map.of(),0,"Actual mining maintenance need only; no speculative raw input demand");
            var missing=new TreeMap<>(planned.missing());if(toolNeed>0)missing.merge(ToolUpkeepPolicy.TOOLS,toolNeed,Math::addExact);
            var uncovered=new TreeMap<Commodity,Long>();
            for(var e:missing.entrySet()) {
                long stocked=localStock.getOrDefault(e.getKey(),0L);long covered=Math.min(stocked,e.getValue());
                localStock.put(e.getKey(),stocked-covered);long gap=e.getValue()-covered;
                if(gap>0){uncovered.put(e.getKey(),gap);total.merge(e.getKey(),gap,Math::addExact);}
            }
            customers.add(new Customer(id,planned.batches(),missing,uncovered,Math.addExact(planned.estimatedCost(),toolCost),planned.explanation()+"; funded replacement tools "+toolNeed));
        }
        return new Review(true,customers,total,"Fundable workshop input gaps after owned/pending goods and shared available warehouse stock; planning only, not orders or finished-goods demand");
    }
    private Review deferred(String reason){return new Review(false,List.of(),Map.of(),reason);}
}
