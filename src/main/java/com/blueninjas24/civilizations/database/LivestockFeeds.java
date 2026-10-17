package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Paid warehouse feed under the existing local delivery, haul, cash and storage constraints. */
final class LivestockFeeds {
    private final Connection connection;
    LivestockFeeds(Connection connection){this.connection=Objects.requireNonNull(connection);}
    String acquireRows(UUID buyer,LivestockRepository.Herd herd,long demand,long period,long window,
                       IndustryRuntimePolicy policy,BusinessOperationPolicy operations)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Feed purchases require owner transaction");
        if(demand<0||demand>1_000_000)throw new IllegalArgumentException("Invalid bounded feed demand");
        var terms=policy.purchasingSupply();var price=terms.unitHundredths().get(herd.feed());
        if(!terms.enabled()||price==null||demand==0)return "Feed purchases disabled, unpriced or no qualified care demand";
        var businesses=new BusinessRepository(connection);var firm=businesses.findBusiness(buyer).orElseThrow();
        var industry=new IndustryRepository(connection);var store=industry.store(buyer).orElseThrow();
        long incoming=0,pendingFeed=0;int shipments=0;
        try(var s=connection.prepareStatement("SELECT commodity,units FROM local_shipments WHERE buyer_id=? AND delivered_period IS NULL LIMIT 129")){
            s.setString(1,buyer.toString());try(var r=s.executeQuery()){while(r.next()){
                shipments++;var good=new com.blueninjas24.civilizations.economy.Commodity(r.getString(1));long units=r.getLong(2);
                Long weight=store.spacePerUnit().get(good);if(weight==null)throw new SQLException("Pending cargo outside herd catalogue");
                incoming=Math.addExact(incoming,Math.multiplyExact(units,weight));if(good.equals(herd.feed()))pendingFeed=Math.addExact(pendingFeed,units);
            }}
        }
        if(shipments>=128)return "Feed waits for existing cargo";
        long hauled=StorageUpgrades.haulUsed(connection,firm.settlementId(),window,policy.millisPerPeriod());
        try(var s=connection.prepareStatement("SELECT t.units FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND a.haul_window=? AND a.haul_cadence=?")){
            s.setString(1,firm.settlementId().toString());s.setLong(2,window);s.setLong(3,policy.millisPerPeriod());
            try(var r=s.executeQuery()){while(r.next())hauled=Math.addExact(hauled,r.getLong(1));}
        }
        long target=Math.multiplyExact(demand,terms.inputPeriods());
        long wanted=Math.max(0,target-Math.addExact(store.units().get(herd.feed()),pendingFeed));
        long room=store.freeSpace(),cash=store.hundredths(); // Store already reserves incoming cargo space.
        long haul=Math.max(0,policy.mining().localHaulUnitsPerPeriod()-hauled),bought=0;
        var sellers=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("SELECT id FROM businesses WHERE settlement_id=? AND industry='warehouse' AND id<>? ORDER BY id LIMIT ?")){
            s.setString(1,firm.settlementId().toString());s.setString(2,buyer.toString());s.setInt(3,terms.warehousesPerReview());
            try(var r=s.executeQuery()){while(r.next())sellers.add(UUID.fromString(r.getString(1)));}
        }
        var availability=new BusinessOperations(connection,operations);
        for(var seller:sellers){
            if(wanted==0||room==0||haul==0||shipments>=128||cash<price)break;
            if(!availability.evaluate(seller).permitted())continue;
            var stock=industry.store(seller);if(stock.isEmpty())continue;
            long units=Math.min(Math.min(wanted,stock.orElseThrow().units().getOrDefault(herd.feed(),0L)),Math.min(room,Math.min(haul,cash/price)));
            if(units==0)continue;
            long payment=Math.multiplyExact(units,price);
            var operation=UUID.nameUUIDFromBytes(("civilizations:herd-feed-v1:"+buyer+":"+period+":"+seller+":"+herd.feed().id()).getBytes(StandardCharsets.UTF_8));
            industry.purchaseRows(operation,seller,buyer,herd.feed(),units,payment,period,policy.mining().localDeliveryPeriods(),policy.mining().localHaulUnitsPerPeriod(),window,policy.millisPerPeriod());
            wanted-=units;room-=units;haul-=units;cash-=payment;bought+=units;shipments++;
        }
        return "Paid feed dispatched="+bought+"; existing feed in transit="+pendingFeed+"; remaining target="+wanted;
    }
}
