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
    WarehouseSupplies(Connection connection,IndustryRuntimePolicy policy,BusinessOperationPolicy operations) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.operations=Objects.requireNonNull(operations);
    }
    String acquireRows(UUID buyer,UUID world,long period,long haulWindow)throws SQLException {
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
        var demand=new LocalIndustryDemand(connection,policy,operations).review(town,Set.of(world),policy.warehouseSupply().customersPerReview());
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
        var producers=ids(town,"mining",policy.warehouseSupply().producersPerReview());
        if(producers.size()>policy.warehouseSupply().producersPerReview())return "Restocking deferred: producer review budget exceeded";
        var sources=new ArrayList<WorkshopInputPlan.Warehouse>();
        for(var id:producers)if(availability.evaluate(id).permitted())sources.add(new WorkshopInputPlan.Warehouse(id,industry.store(id).orElseThrow().units()));
        long hauled=0;
        try(var s=connection.prepareStatement("SELECT t.units FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND a.haul_window=? AND a.haul_cadence=?")) {
            s.setString(1,town.toString());s.setLong(2,haulWindow);s.setLong(3,policy.millisPerPeriod());
            try(var r=s.executeQuery()){while(r.next())hauled=Math.addExact(hauled,r.getLong(1));}
        }
        var store=industry.store(buyer).orElseThrow();
        var orders=WarehouseInputPlan.choose(demand.uncovered(),pending,store.hundredths(),store.freeSpace(),
                Math.max(0,policy.mining().localHaulUnitsPerPeriod()-hauled),128-incoming,store.spacePerUnit(),sources,policy.supply().unitHundredths());
        for(var order:orders) {
            var operation=UUID.nameUUIDFromBytes(("civilizations:warehouse-input-v1:"+buyer+":"+period+":"+order.seller()+":"+order.commodity().id()).getBytes(StandardCharsets.UTF_8));
            industry.purchaseRows(operation,order.seller(),buyer,order.commodity(),order.units(),order.price(),period,
                    policy.mining().localDeliveryPeriods(),policy.mining().localHaulUnitsPerPeriod(),haulWindow,policy.millisPerPeriod());
        }
        return "Warehouse restocking: "+orders.size()+" paid purchases from actual local mine stock; shared pending cargo covers future orders only";
    }
    private List<UUID> ids(UUID town,String kind,int limit)throws SQLException {
        var result=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id WHERE b.settlement_id=? AND b.industry=? ORDER BY b.id LIMIT ?")) {
            s.setString(1,town.toString());s.setString(2,kind);s.setInt(3,limit+1);try(var r=s.executeQuery()){while(r.next())result.add(UUID.fromString(r.getString(1)));}
        }
        return result;
    }
}
