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
    private FarmToolUpkeepPolicy farmUpkeep=FarmToolUpkeepPolicy.disabled();
    private FarmToolBudgetPolicy farmBudget=FarmToolBudgetPolicy.disabled();
    private UUID excludedStorageBuyer;
    private LivestockPolicy livestock=LivestockPolicy.disabled();
    private LivestockRuntimePolicy herdWork=LivestockRuntimePolicy.disabled();
    private LivestockWorkingArea herdArea=LivestockWorkingArea.prototype();
    private java.util.function.BooleanSupplier herdHealthy=()->false;
    public LocalIndustryDemand withLivestock(LivestockPolicy p,LivestockRuntimePolicy work,LivestockWorkingArea area,java.util.function.BooleanSupplier healthy){
        livestock=Objects.requireNonNull(p);herdWork=Objects.requireNonNull(work);herdArea=Objects.requireNonNull(area);herdHealthy=Objects.requireNonNull(healthy);return this;
    }
    LocalIndustryDemand excludingStorageBuyer(UUID business){excludedStorageBuyer=Objects.requireNonNull(business);return this;}
    public LocalIndustryDemand withFarmTools(FarmToolUpkeepPolicy upkeep,FarmToolBudgetPolicy budget){farmUpkeep=Objects.requireNonNull(upkeep);farmBudget=Objects.requireNonNull(budget);return this;}
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
                int reviewed=0;
                while(r.next()) {
                    if(++reviewed>businessLimit+policy.supply().warehousesPerReview())return deferred("Stored business review budget exceeded; no partial shortage inferred");
                    var kind=r.getString(2);var id=UUID.fromString(r.getString(1));
                    if(kind.equals("warehouse"))warehouses.add(id);
                    else if(kind.equals("farming")&&FarmMarketStore.supported(connection,id,new IndustryRepository(connection).store(id).orElseThrow()))continue;
                    else if(kind.equals("mining")&&policy.tools().applies(kind))workshops.add(id);
                    else if(kind.equals("livestock")&&new LivestockRepository(connection).load(id).isPresent())workshops.add(id);
                    else if(kind.equals("fishing")&&registeredFishery(id))workshops.add(id);
                    else if(Set.of("quarry","sandpit").contains(kind)&&registeredExtraction(id,kind))workshops.add(id);
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
            var starter=new WorkshopStarterSupplies(connection).pending(id);
            if(!starter.isEmpty())incoming++;
            starter.forEach((good,units)->pending.merge(good,units,Math::addExact));
            if(incoming>128)return deferred("Pending cargo review budget exceeded; no partial shortage inferred");
            var store=industry.store(id).orElseThrow();
            if(founding.business().industry().equals("livestock")&&new LivestockRepository(connection).load(id).isPresent()
                    &&policy.recipes().values().stream().noneMatch(r->r.industry().equals("livestock"))) {
                long missing=0,cost=0;
                var herd=new LivestockRepository(connection).load(id).orElseThrow();
                Long price=policy.purchasingSupply().unitHundredths().get(herd.feed());
                if(livestock.enabled()&&herdWork.enabled()&&herdHealthy.getAsBoolean()&&price!=null&&price>0&&incoming<128
                        &&store.spacePerUnit().containsKey(herd.feed())) {
                    long work=Math.min(herdWork.siteWorkLimit(),Math.multiplyExact(workers,herdWork.workPerWorker()));
                    long away=new LivestockPresence(connection).unavailableAdultsRows(id,herd.state().period()+1,herdArea);
                    var forecast=LivestockHerd.plan(herd.state(),1_000_000,work,store.freeSpace(),0,livestock,herd.species(),true,away);
                    long target=Math.multiplyExact(forecast.feedUsed(),policy.purchasingSupply().inputPeriods());
                    long need=Math.max(0,target-store.units().getOrDefault(herd.feed(),0L)-pending.getOrDefault(herd.feed(),0L));
                    missing=Math.min(need,Math.min(store.hundredths()/price,store.freeSpace()/store.spacePerUnit().get(herd.feed())));cost=missing*price;
                }
                var gaps=new TreeMap<Commodity,Long>();var needs=new TreeMap<Commodity,Long>();
                if(missing>0){needs.put(herd.feed(),missing);long covered=Math.min(localStock.getOrDefault(herd.feed(),0L),missing);
                    localStock.computeIfPresent(herd.feed(),(good,units)->units-covered);long gap=missing-covered;
                    if(gap>0){gaps.put(herd.feed(),gap);total.merge(herd.feed(),gap,Math::addExact);}}
                customers.add(new Customer(id,0,needs,gaps,cost,"Actual staffed herd feed; real cash, held/incoming stock, radius, capture and storage limits"));continue;
            }
            boolean extraction=Set.of("mining","quarry","sandpit").contains(founding.business().industry());
            long toolNeed=extraction&&!mutationHealthy.getAsBoolean()?0
                    :new BusinessTools(connection,policy).missingFunded(id,founding.business().industry(),workers,store,pending);
            long toolCost=toolNeed*policy.tools().toolValueHundredths();
            var recipe=BakeryRecipeChoice.supply(connection,id,policy,operations,store.hundredths()-toolCost);
            var planned=recipe.isPresent()?WorkshopInputDemand.review(recipe.orElseThrow(),workers,store.hundredths()-toolCost,
                    store.freeSpace()-toolNeed*store.spacePerUnit().getOrDefault(ToolUpkeepPolicy.TOOLS,0L),store.units(),pending,store.spacePerUnit(),policy.purchasingSupply())
                    :new WorkshopInputDemand.Review(0,Map.of(),0,"Actual productive-work maintenance need only; no speculative raw input demand");
            var missing=new TreeMap<>(planned.missing());if(toolNeed>0)missing.merge(ToolUpkeepPolicy.TOOLS,toolNeed,Math::addExact);
            var uncovered=new TreeMap<Commodity,Long>();
            for(var e:missing.entrySet()) {
                long stocked=localStock.getOrDefault(e.getKey(),0L);long covered=Math.min(stocked,e.getValue());
                localStock.put(e.getKey(),stocked-covered);long gap=e.getValue()-covered;
                if(gap>0){uncovered.put(e.getKey(),gap);total.merge(e.getKey(),gap,Math::addExact);}
            }
            customers.add(new Customer(id,planned.batches(),missing,uncovered,Math.addExact(planned.estimatedCost(),toolCost),planned.explanation()+"; funded replacement tools "+toolNeed));
        }
        var farmNeed=new FarmEquipmentDemand(connection,farmUpkeep,farmBudget,operations).review(town,worlds);
        if(farmNeed.units()>0) {
            if(customers.size()==businessLimit)return deferred("Local customer review budget exceeded; no partial farm need inferred");
            long covered=Math.min(localStock.getOrDefault(ToolUpkeepPolicy.TOOLS,0L),farmNeed.units());
            long gap=farmNeed.units()-covered;localStock.computeIfPresent(ToolUpkeepPolicy.TOOLS,(good,units)->units-covered);
            var uncovered=gap==0?Map.<Commodity,Long>of():Map.of(ToolUpkeepPolicy.TOOLS,gap);
            if(gap>0)total.merge(ToolUpkeepPolicy.TOOLS,gap,Math::addExact);
            customers.add(new Customer(farmNeed.farm(),0,Map.of(ToolUpkeepPolicy.TOOLS,farmNeed.units()),uncovered,farmNeed.invoice(),farmNeed.explanation()));
        }
        var spending=new HashMap<UUID,Long>();for(var customer:customers)spending.put(customer.business(),customer.estimatedCost());
        var storage=new StorageUpgradeDemand(connection,policy.storageUpgrades(),operations).review(town,worlds,businessLimit+warehouses.size(),spending);
        if(!storage.complete())return deferred(storage.explanation());
        for(var need:storage.needs()) {
            // A warehouse installs a fitting through the upgrade order, rather than stocking
            // a second fitting to supply its own identical installation need.
            if(need.buyer().equals(excludedStorageBuyer))continue;
            var good=StorageUpgrades.FITTINGS;long covered=Math.min(localStock.getOrDefault(good,0L),need.units()),gap=need.units()-covered;
            localStock.computeIfPresent(good,(k,v)->v-covered);if(gap>0)total.merge(good,gap,Math::addExact);
            int index=-1;for(int n=0;n<customers.size();n++)if(customers.get(n).business().equals(need.buyer())){index=n;break;}
            var old=index<0?new Customer(need.buyer(),0,Map.of(),Map.of(),0,""):customers.get(index);
            var missing=new TreeMap<>(old.missing());missing.merge(good,need.units(),Math::addExact);var uncovered=new TreeMap<>(old.uncovered());if(gap>0)uncovered.merge(good,gap,Math::addExact);
            var updated=new Customer(need.buyer(),old.batches(),missing,uncovered,Math.addExact(old.estimatedCost(),need.price()),old.explanation()+"; funded bounded storage fittings");
            if(index<0){if(customers.size()>=businessLimit+warehouses.size())return deferred("Combined storage/input customer budget exceeded");customers.add(updated);}else customers.set(index,updated);
        }
        return new Review(true,customers,total,"Fundable workshop inputs and bounded storage fittings after owned/pending goods, shared available warehouse stock and planned spending; planning only, not orders; actual public farm tools "+farmNeed.units());
    }
    private boolean registeredFishery(UUID business)throws SQLException {
        try(var s=connection.prepareStatement("SELECT 1 FROM businesses b JOIN fishery_resources f ON f.site_id=b.site_id WHERE b.id=? AND b.industry='fishing'")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
    private boolean registeredExtraction(UUID business,String industry)throws SQLException {
        String mineral=industry.equals("quarry")?"stone":"sand";
        try(var s=connection.prepareStatement("SELECT 1 FROM businesses b JOIN mineral_deposits d ON d.site_id=b.site_id JOIN mineral_discovery m ON m.deposit_id=d.id WHERE b.id=? AND b.industry=? AND m.mineral=? LIMIT 1")) {
            s.setString(1,business.toString());s.setString(2,industry);s.setString(3,mineral);try(var r=s.executeQuery()){return r.next();}
        }
    }
    private Review deferred(String reason){return new Review(false,List.of(),Map.of(),reason);}
}
