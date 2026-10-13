package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import java.sql.*;
import java.util.*;

/** Atomic future harvest ownership, composed with public food accounting; no money or shoppers created. */
public final class FarmMarketRepository {
    private final Connection connection;
    public FarmMarketRepository(Connection connection){this.connection=Objects.requireNonNull(connection);}
    void finishRows(UUID town,long period,FarmMarketHarvest.Plan plan)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Farm market requires food transaction");
        UUID farm=FarmEnterprises.businessId(town);
        try(var s=connection.prepareStatement("SELECT production FROM economy_periods WHERE settlement_id=? AND period=? AND commodity='food'")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){if(!r.next()||r.getLong(1)!=plan.publicFood())throw new SQLException("Farm contribution must match actual food accounting");}
        }
        for(var entry:Map.of(Commodity.FOOD,plan.businessFood(),FarmMarketStore.GRAIN,plan.grainUnits()).entrySet())try(var s=connection.prepareStatement("UPDATE industry_stock SET units=units+? WHERE business_id=? AND commodity=?")) {
            s.setLong(1,entry.getValue());s.setString(2,farm.toString());s.setString(3,entry.getKey().id());if(s.executeUpdate()!=1)throw new SQLException("Farm commodity missing");
        }
        new IndustryRepository(connection).store(farm).orElseThrow();
        try(var s=connection.prepareStatement("INSERT INTO farm_market_harvests VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,town.toString());s.setLong(2,period);s.setString(3,farm.toString());s.setLong(4,plan.potentialWork());s.setLong(5,plan.actualWork());
            s.setLong(6,plan.publicFood());s.setLong(7,plan.businessFood());s.setLong(8,plan.grainWork());s.setLong(9,plan.grainUnits());s.setString(10,plan.policy().toString());s.setString(11,plan.explanation());
            s.setString(12,plan.grainPolicy().toString());if(plan.wheat()==null)s.setNull(13,Types.INTEGER);else s.setLong(13,plan.wheat());
            s.setLong(14,plan.crops());s.setLong(15,plan.observedMillis());s.setLong(16,plan.grainPolicy().grainUnitsPerHarvest());s.setInt(17,plan.policy().villageSharePercent());s.executeUpdate();
        }
    }
    public String summary(UUID town,long period)throws SQLException {
        try(var s=connection.prepareStatement("SELECT business_id,public_food,business_food,grain_units,potential_work,actual_work FROM farm_market_harvests WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                if(!r.next())return "";var store=new IndustryRepository(connection).store(UUID.fromString(r.getString(1))).orElseThrow();
                return "; Farm market: contribution "+r.getLong(2)+", owned food "+store.units().get(Commodity.FOOD)+", grain "+store.units().get(FarmMarketStore.GRAIN)+", business cash "+store.hundredths()+" hundredths; actual work "+r.getLong(6)+"/"+r.getLong(5);
            }
        }
    }
}
