package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Upstream purchasing within the warehouse's period/checkpoint transaction. */
final class WarehouseSupplies {
    private final Connection connection;
    private final IndustryRuntimePolicy policy;
    private final BusinessOperationPolicy operations;
    private final java.util.function.BooleanSupplier mutationHealthy;
    WarehouseSupplies(Connection connection,IndustryRuntimePolicy policy,BusinessOperationPolicy operations) {
        this(connection,policy,operations,()->true);
    }
    WarehouseSupplies(Connection connection,IndustryRuntimePolicy policy,BusinessOperationPolicy operations,java.util.function.BooleanSupplier mutationHealthy) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.operations=Objects.requireNonNull(operations);
        this.mutationHealthy=Objects.requireNonNull(mutationHealthy);
    }
    private FarmToolUpkeepPolicy farmUpkeep=FarmToolUpkeepPolicy.disabled();
    private FarmToolBudgetPolicy farmBudget=FarmToolBudgetPolicy.disabled();
    private LivestockPolicy livestock=LivestockPolicy.disabled();private LivestockRuntimePolicy herdWork=LivestockRuntimePolicy.disabled();
    private LivestockWorkingArea herdArea=LivestockWorkingArea.prototype();private java.util.function.BooleanSupplier herdHealthy=()->false;
    WarehouseSupplies withLivestock(LivestockPolicy p,LivestockRuntimePolicy work,LivestockWorkingArea area,java.util.function.BooleanSupplier healthy){livestock=p;herdWork=work;herdArea=area;herdHealthy=healthy;return this;}
    WarehouseSupplies withFarmTools(FarmToolUpkeepPolicy upkeep,FarmToolBudgetPolicy budget){farmUpkeep=Objects.requireNonNull(upkeep);farmBudget=Objects.requireNonNull(budget);return this;}
    String acquireRows(UUID buyer,UUID world,long period,long haulWindow)throws SQLException {
        return acquireRows(buyer,world,period,haulWindow,null);
    }
    String acquireRows(UUID buyer,UUID world,long period,long haulWindow,WorkLearningPolicy learning)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Warehouse purchases require active SQL owner transaction");
        if(!policy.warehouseSupply().enabled())return "New warehouse restocking disabled";
        var businesses=new BusinessRepository(connection);var founding=businesses.findFounding(buyer).orElseThrow();
        if(!founding.business().industry().equals("warehouse"))throw new SQLException("Restocking requires an actual warehouse");
        var people=new PopulationRepository(connection);boolean staffed=false;
        for(var job:businesses.currentWorkers(buyer)) {
            var resident=people.findResident(job.residentId()).orElseThrow();
            if(WorkEligibility.evaluate(resident,people.findWorkProfile(resident.id()).orElseThrow(),founding.workerRequirements()).eligible()){staffed=true;break;}
        }
        if(!staffed)return "Restocking deferred: qualified warehouse staff required";
        var town=founding.business().settlementId();
        var demand=new LocalIndustryDemand(connection,policy,operations,mutationHealthy).withFarmTools(farmUpkeep,farmBudget).withLivestock(livestock,herdWork,herdArea,herdHealthy).excludingStorageBuyer(buyer).review(town,Set.of(world),policy.warehouseSupply().customersPerReview());
        if(!demand.complete())return "Restocking deferred: "+demand.explanation();
        var availability=new BusinessOperations(connection,operations);var industry=new IndustryRepository(connection);
        var warehouses=ids(town,"warehouse",policy.supply().warehousesPerReview());
        if(warehouses.size()>policy.supply().warehousesPerReview())return "Restocking deferred: warehouse review budget exceeded";
        var pending=new TreeMap<Commodity,Long>();int incoming=0;
        for(var id:warehouses) {
            if(!availability.evaluate(id).permitted())continue;
            int count=0;
            try(var s=connection.prepareStatement("SELECT commodity,units FROM local_shipments WHERE buyer_id=? AND delivered_period IS NULL ORDER BY id LIMIT 129")) {
                s.setString(1,id.toString());try(var r=s.executeQuery()){while(r.next()){count++;pending.merge(new Commodity(r.getString(1)),r.getLong(2),Math::addExact);}}
            }
            if(count>128)return "Restocking deferred: warehouse pending cargo review budget exceeded";
            if(id.equals(buyer))incoming=count;
        }
        var producers=producerIds(town,policy.warehouseSupply().producersPerReview());
        if(producers.size()>policy.warehouseSupply().producersPerReview())return "Restocking deferred: producer review budget exceeded";
        var sources=new ArrayList<WorkshopInputPlan.Warehouse>();
        for(var id:producers)if(availability.evaluate(id).permitted()) {
            var b=businesses.findBusiness(id).orElseThrow();var allowed=new HashSet<Commodity>();
            if(b.industry().equals("mining")) {
                try(var s=connection.prepareStatement("SELECT y.commodity FROM mineral_yields y JOIN mineral_deposits d ON d.id=y.deposit_id WHERE d.site_id=?")) {
                    s.setString(1,b.siteId().toString());try(var r=s.executeQuery()){while(r.next())allowed.add(new Commodity(r.getString(1)));}
                }
            }else if(b.industry().equals("farming")&&FarmMarketStore.supported(connection,id,industry.store(id).orElseThrow())) {
                allowed.add(Commodity.FOOD);allowed.add(FarmMarketStore.GRAIN);
            }else if(b.industry().equals("livestock")&&new LivestockRepository(connection).load(id).isPresent()) {
                var herd=new LivestockRepository(connection).load(id).orElseThrow();
                allowed.add(new Commodity(herd.species()==LivestockHerd.Species.COW?"milk":"eggs"));
                allowed.add(Commodity.FOOD); // Confirmed deliberate slaughter; feed remains unavailable for resale.
            }else if(b.industry().equals("fishing")&&registeredFishery(b.siteId())) {
                allowed.add(Commodity.FOOD);
            }else if(b.industry().equals("logging")&&new ForestryRepository(connection).find(b.siteId()).isPresent()) {
                allowed.add(Commodity.TIMBER);
            }else if(Set.of("quarry","sandpit").contains(b.industry())&&registeredExtraction(b.siteId(),b.industry())) {
                try(var s=connection.prepareStatement("SELECT y.commodity FROM mineral_yields y JOIN mineral_deposits d ON d.id=y.deposit_id JOIN mineral_discovery m ON m.deposit_id=d.id WHERE d.site_id=? AND m.mineral=?")) {
                    s.setString(1,b.siteId().toString());s.setString(2,b.industry().equals("quarry")?"stone":"sand");try(var r=s.executeQuery()){while(r.next())allowed.add(new Commodity(r.getString(1)));}
                }
            }else policy.recipes().values().stream().filter(r->r.industry().equals(b.industry())).forEach(r->allowed.addAll(r.outputs().keySet()));
            var stock=new TreeMap<Commodity,Long>();industry.store(id).orElseThrow().units().forEach((good,units)->{if(allowed.contains(good))stock.put(good,units);});
            var store=industry.store(id).orElseThrow();
            if(!businesses.currentWorkers(id).isEmpty()&&new BusinessTools(connection,policy).replacementDue(id,b.industry(),store))
                stock.computeIfPresent(ToolUpkeepPolicy.TOOLS,(good,units)->Math.max(0,units-1));
            sources.add(new WorkshopInputPlan.Warehouse(id,stock));
        }
        long hauled=0;
        try(var s=connection.prepareStatement("SELECT t.units FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND a.haul_window=? AND a.haul_cadence=?")) {
            s.setString(1,town.toString());s.setLong(2,haulWindow);s.setLong(3,policy.millisPerPeriod());
            try(var r=s.executeQuery()){while(r.next())hauled=Math.addExact(hauled,r.getLong(1));}
        }
        hauled=Math.addExact(hauled,StorageUpgrades.haulUsed(connection,town,haulWindow,policy.millisPerPeriod()));
        var store=industry.store(buyer).orElseThrow();
        var orders=WarehouseInputPlan.choose(demand.uncovered(),pending,store.hundredths(),store.freeSpace(),
                Math.max(0,policy.mining().localHaulUnitsPerPeriod()-hauled),128-incoming,store.spacePerUnit(),sources,policy.purchasingSupply().unitHundredths());
        var fresh=new ArrayList<UUID>();
        for(var order:orders) {
            var operation=UUID.nameUUIDFromBytes(("civilizations:warehouse-input-v1:"+buyer+":"+period+":"+order.seller()+":"+order.commodity().id()).getBytes(StandardCharsets.UTF_8));
            boolean exists;
            try(var s=connection.prepareStatement("SELECT 1 FROM local_shipments WHERE id=?")) {
                s.setString(1,operation.toString());try(var r=s.executeQuery()){exists=r.next();}
            }
            industry.purchaseRows(operation,order.seller(),buyer,order.commodity(),order.units(),order.price(),period,
                    policy.mining().localDeliveryPeriods(),policy.mining().localHaulUnitsPerPeriod(),haulWindow,policy.millisPerPeriod());
            if(!exists)fresh.add(operation);
        }
        String experience=learning==null?"Commerce learning not requested":new CommerceLearning(connection,learning).reviewRows(buyer,period,fresh);
        return experience+"; Warehouse restocking: "+orders.size()+" paid purchases from actual local producer output; shared pending cargo covers future orders only";
    }
    private List<UUID> ids(UUID town,String kind,int limit)throws SQLException {
        var result=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id WHERE b.settlement_id=? AND b.industry=? ORDER BY b.id LIMIT ?")) {
            s.setString(1,town.toString());s.setString(2,kind);s.setInt(3,limit+1);try(var r=s.executeQuery()){while(r.next())result.add(UUID.fromString(r.getString(1)));}
        }
        return result;
    }
    private List<UUID> producerIds(UUID town,int limit)throws SQLException {
        var result=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT b.id,b.industry FROM businesses b JOIN industry_stores k ON k.business_id=b.id
            WHERE b.settlement_id=? AND b.industry<>'warehouse'
              AND NOT(b.industry='farming' AND k.capacity=1 AND k.hundredths=0
                AND EXISTS(SELECT 1 FROM farm_tool_purchases f WHERE f.farm_id=b.id)
                AND (SELECT COUNT(*) FROM industry_stock c WHERE c.business_id=b.id)=1
                AND EXISTS(SELECT 1 FROM industry_stock c WHERE c.business_id=b.id AND c.commodity='tools' AND c.space_per_unit=1))
            ORDER BY b.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setInt(2,limit+1);try(var r=s.executeQuery()){while(r.next()) {
                String kind=r.getString(2);
                var id=UUID.fromString(r.getString(1));
                boolean farm=kind.equals("farming")&&FarmMarketStore.supported(connection,id,new IndustryRepository(connection).store(id).orElseThrow());
                boolean herd=kind.equals("livestock")&&new LivestockRepository(connection).load(id).isPresent();
                var business=new BusinessRepository(connection).findBusiness(id).orElseThrow();
                boolean fishery=kind.equals("fishing")&&registeredFishery(business.siteId());
                boolean forestry=kind.equals("logging")&&new ForestryRepository(connection).find(business.siteId()).isPresent();
                boolean extraction=Set.of("quarry","sandpit").contains(kind)&&registeredExtraction(business.siteId(),kind);
                if(!kind.equals("mining")&&!farm&&!herd&&!fishery&&!extraction&&!forestry&&policy.recipes().values().stream().noneMatch(recipe->recipe.industry().equals(kind)))throw new SQLException("Unsupported stored producer industry requires explicit review");
                result.add(id);
            }}
        }
        return result;
    }
    private boolean registeredFishery(UUID site)throws SQLException {
        try(var s=connection.prepareStatement("SELECT 1 FROM fishery_resources f JOIN business_sites b ON b.id=f.site_id WHERE f.site_id=? AND b.id=?")) {
            s.setString(1,site.toString());s.setString(2,site.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
    private boolean registeredExtraction(UUID site,String industry)throws SQLException {
        String mineral=industry.equals("quarry")?"stone":"sand";
        try(var s=connection.prepareStatement("SELECT 1 FROM mineral_deposits d JOIN mineral_discovery m ON m.deposit_id=d.id WHERE d.site_id=? AND m.mineral=? LIMIT 1")) {
            s.setString(1,site.toString());s.setString(2,mineral);try(var r=s.executeQuery()){return r.next();}
        }
    }
}
