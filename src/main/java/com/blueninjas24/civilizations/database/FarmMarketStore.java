package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;

/** Explicit zero-money adoption of an empty/legacy equipment store; original terms and assets survive. */
final class FarmMarketStore {
    static final Commodity GRAIN=new Commodity("grain");
    static final Map<Commodity,Long> CATALOG=Map.of(Commodity.FOOD,1L,GRAIN,1L,ToolUpkeepPolicy.TOOLS,1L);
    private FarmMarketStore(){}
    static boolean registered(Connection c,UUID farm)throws SQLException {
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA user_version")){if(!r.next()||r.getInt(1)<73)return false;}
        try(var s=c.prepareStatement("SELECT 1 FROM farm_market_stores WHERE business_id=?")){s.setString(1,farm.toString());try(var r=s.executeQuery()){return r.next();}}
    }
    static long incomingTools(Connection c,UUID farm)throws SQLException {
        try(var s=c.prepareStatement("SELECT COALESCE(SUM(units),0) FROM local_shipments WHERE buyer_id=? AND commodity='tools' AND delivered_period IS NULL")) {
            s.setString(1,farm.toString());try(var r=s.executeQuery()){r.next();return r.getLong(1);}
        }
    }
    static boolean supported(Connection c,UUID farm,IndustryRepository.Store store)throws SQLException {
        if(!registered(c,farm))return false;
        long capacity;
        try(var s=c.prepareStatement("SELECT capacity FROM farm_market_stores WHERE business_id=?")){s.setString(1,farm.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Farm market registration disappeared");capacity=r.getLong(1);}}
        if(store.capacity()!=capacity||!store.spacePerUnit().equals(CATALOG)||Math.addExact(store.units().get(ToolUpkeepPolicy.TOOLS),incomingTools(c,farm))>1)
            throw new SQLException("Registered farm market store conflicts with saved capacity/catalog/equipment quota");
        return true;
    }
    static IndustryRepository.Store ensureRows(Connection c,UUID farm,FarmMarketPolicy policy,long now)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Farm store adoption requires harvest transaction");
        var industry=new IndustryRepository(c);var old=industry.store(farm);
        if(old.isPresent()&&supported(c,farm,old.orElseThrow()))return old.orElseThrow();
        long capacity=Math.incrementExact(policy.storageCapacity());String prior="No prior store; zero owned stock and capital";
        if(old.isEmpty())industry.openStoreRows(farm,capacity,CATALOG,0);
        else {
            var store=old.orElseThrow();
            if(store.capacity()!=1||store.hundredths()!=0||!store.spacePerUnit().equals(Map.of(ToolUpkeepPolicy.TOOLS,1L)))throw new SQLException("Unrecognized farm store requires reviewed adoption");
            try(var s=c.prepareStatement("SELECT terms FROM industry_stores WHERE business_id=?")){s.setString(1,farm.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Farm store missing");prior=r.getString(1);}}
            try(var s=c.prepareStatement("UPDATE industry_stores SET capacity=? WHERE business_id=? AND capacity=1")){s.setLong(1,capacity);s.setString(2,farm.toString());if(s.executeUpdate()!=1)throw new SQLException("Farm capacity changed during adoption");}
            for(var good:List.of(Commodity.FOOD,GRAIN))try(var s=c.prepareStatement("INSERT INTO industry_stock VALUES(?,?,1,0)")){s.setString(1,farm.toString());s.setString(2,good.id());s.executeUpdate();}
        }
        try(var s=c.prepareStatement("INSERT INTO farm_market_stores VALUES(?,?,?,?,?)")){s.setString(1,farm.toString());s.setLong(2,capacity);s.setLong(3,now);s.setString(4,prior);s.setString(5,policy.toString());s.executeUpdate();}
        return industry.store(farm).orElseThrow();
    }
    static long freeGoodsSpace(Connection c,IndustryRepository.Store store)throws SQLException {
        long heldTools=Math.addExact(store.units().get(ToolUpkeepPolicy.TOOLS),incomingTools(c,store.businessId()));
        return Math.max(0,store.freeSpace()-Math.max(0,1-heldTools));
    }
}
