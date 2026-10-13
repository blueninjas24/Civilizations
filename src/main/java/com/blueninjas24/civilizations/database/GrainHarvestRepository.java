package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import java.sql.*;
import java.util.*;

/** SQL-owner public grain stock and retained actual harvest allocation, inside the food period transaction. */
public final class GrainHarvestRepository {
    private final Connection connection;
    public GrainHarvestRepository(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public long stock(UUID town)throws SQLException {
        try(var s=connection.prepareStatement("SELECT units FROM grain_stocks WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){return r.next()?r.getLong(1):0;}
        }
    }
    public String summary(UUID town,long period)throws SQLException {
        try(var s=connection.prepareStatement("SELECT grain_units,food_output,explanation FROM grain_harvests WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                return r.next()?"; Grain: saved "+stock(town)+", harvested "+r.getLong(1)+", basic food "+r.getLong(2)+"; "+r.getString(3):"";
            }
        }
    }
    void finishRows(UUID town,long period,GrainHarvest.Plan plan)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Grain allocation requires the food transaction");
        if(stock(town)!=plan.openingGrain())throw new SQLException("Grain stock changed during harvest");
        try(var s=connection.prepareStatement("SELECT production FROM economy_periods WHERE settlement_id=? AND commodity='food' AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                if(!r.next()||r.getLong(1)!=plan.foodOutput())throw new SQLException("Grain allocation must match the actual food period");
            }
        }
        try(var s=connection.prepareStatement("""
                INSERT INTO grain_harvests VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            s.setString(1,town.toString());s.setLong(2,period);s.setLong(3,plan.harvest());s.setLong(4,plan.foodOutput());s.setLong(5,plan.diverted());
            s.setLong(6,plan.grainUnits());s.setLong(7,plan.openingGrain());s.setLong(8,plan.closingGrain());s.setLong(9,plan.protectedFood());s.setLong(10,plan.availableFood());
            if(plan.wheat()==null)s.setNull(11,Types.INTEGER);else s.setLong(11,plan.wheat());
            s.setLong(12,plan.crops());s.setLong(13,plan.policy().grainUnitsPerHarvest());s.setLong(14,plan.policy().capacity());s.setString(15,plan.policy().toString());s.setString(16,plan.explanation());s.executeUpdate();
        }
        try(var s=connection.prepareStatement("INSERT INTO grain_stocks VALUES(?,?) ON CONFLICT(settlement_id) DO UPDATE SET units=excluded.units")) {
            s.setString(1,town.toString());s.setLong(2,plan.closingGrain());s.executeUpdate();
        }
    }
}
