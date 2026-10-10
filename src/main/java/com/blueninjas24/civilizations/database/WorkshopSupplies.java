package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Composition within one timed business-period transaction, never a second purchasing thread. */
final class WorkshopSupplies {
    private final Connection connection;
    private final BusinessOperationPolicy operations;
    WorkshopSupplies(Connection connection){this(connection,BusinessOperationPolicy.disabled());}
    WorkshopSupplies(Connection connection,BusinessOperationPolicy operations){this.connection=Objects.requireNonNull(connection);this.operations=Objects.requireNonNull(operations);}
    int deliverRows(UUID buyer,long period)throws SQLException {
        requireTransaction();
        var arrivals=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("SELECT t.id FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id WHERE a.clock_business_id=? AND t.delivered_period IS NULL AND t.due_period<=? ORDER BY t.due_period,t.id LIMIT 128")) {
            s.setString(1,buyer.toString());s.setLong(2,period);try(var r=s.executeQuery()){while(r.next())arrivals.add(UUID.fromString(r.getString(1)));}
        }
        var industry=new IndustryRepository(connection);for(var id:arrivals)industry.deliverRows(id,period);
        return arrivals.size();
    }
    String acquireRows(UUID buyer,ProductionRecipe recipe,long period,long haulWindow,long haulCadence,WorkshopSupplyPolicy policy,IndustryPolicy transport)throws SQLException {
        requireTransaction();
        if(!policy.enabled())return "New automatic input purchases disabled";
        var businesses=new BusinessRepository(connection);var founding=businesses.findFounding(buyer).orElseThrow(()->new SQLException("Unknown workshop"));
        if(!founding.business().industry().equals(recipe.industry()))throw new SQLException("Supply recipe does not match actual industry");
        var industry=new IndustryRepository(connection);var store=industry.store(buyer).orElseThrow(()->new SQLException("Workshop store missing"));
        var pending=new TreeMap<Commodity,Long>();int incoming=0;
        try(var s=connection.prepareStatement("SELECT commodity,units FROM local_shipments WHERE buyer_id=? AND delivered_period IS NULL ORDER BY due_period,id LIMIT 129")) {
            s.setString(1,buyer.toString());try(var r=s.executeQuery()) {while(r.next()) {
                incoming++;pending.merge(new Commodity(r.getString(1)),r.getLong(2),Math::addExact);
            }}
        }
        if(incoming>128)return "Supply deferred: pending cargo review budget exceeded; legacy deliveries require their original caller";
        var warehouses=new ArrayList<WorkshopInputPlan.Warehouse>();
        var candidates=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("SELECT b.id FROM businesses b JOIN residents r ON r.id=b.owner_id JOIN industry_stores k ON k.business_id=b.id WHERE b.settlement_id=? AND b.industry='warehouse' AND ((r.status='ALIVE' AND r.settlement_id=b.settlement_id) OR (r.status='DECEASED' AND ?=1)) ORDER BY b.id LIMIT ?")) {
            s.setString(1,founding.business().settlementId().toString());s.setInt(2,operations.enabled()&&operations.continueStaffedAfterOwnerDeath()?1:0);s.setInt(3,policy.warehousesPerReview());
            try(var r=s.executeQuery()){while(r.next())candidates.add(UUID.fromString(r.getString(1)));}
        }
        var availability=new BusinessOperations(connection,operations);
        for(var id:candidates)if(availability.evaluate(id).permitted())warehouses.add(new WorkshopInputPlan.Warehouse(id,industry.store(id).orElseThrow().units()));
        long hauled=0;
        try(var s=connection.prepareStatement("SELECT t.units FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND a.haul_window=? AND a.haul_cadence=?")) {
            s.setString(1,founding.business().settlementId().toString());s.setLong(2,haulWindow);s.setLong(3,haulCadence);try(var r=s.executeQuery()){while(r.next())hauled=Math.addExact(hauled,r.getLong(1));}
        }
        var plan=WorkshopInputPlan.choose(recipe,businesses.currentWorkers(buyer).size(),store.hundredths(),store.freeSpace(),
                Math.max(0,transport.localHaulUnitsPerPeriod()-hauled),128-incoming,store.units(),pending,store.spacePerUnit(),warehouses,policy);
        for(var order:plan.orders()) {
            var operation=UUID.nameUUIDFromBytes(("civilizations:workshop-input-v1:"+buyer+":"+period+":"+order.seller()+":"+order.commodity().id()).getBytes(StandardCharsets.UTF_8));
            industry.purchaseRows(operation,order.seller(),buyer,order.commodity(),order.units(),order.price(),period,
                    transport.localDeliveryPeriods(),transport.localHaulUnitsPerPeriod(),haulWindow,haulCadence);
        }
        return plan.explanation()+"; reviewed "+warehouses.size()+" local warehouses";
    }
    private void requireTransaction()throws SQLException {if(connection.getAutoCommit())throw new SQLException("Workshop supply rows require active SQL owner transaction");}
}
