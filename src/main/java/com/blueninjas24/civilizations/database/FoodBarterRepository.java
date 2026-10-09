package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import java.sql.*;
import java.util.*;

/** Owner transaction only. Reserved goods leave spendable stock immediately; cancellation restores them once. */
final class FoodBarterRepository {
    enum Status { RESERVED, COMPLETED, CANCELLED }
    record Barter(FoodBarters.Terms terms,Status status,long acceptedPeriod) {}
    private final Connection connection;
    FoodBarterRepository(Connection connection) { this.connection=connection; }
    Optional<Barter> find(UUID operation) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM food_barters WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                return Optional.of(new Barter(new FoodBarters.Terms(operation,UUID.fromString(r.getString("player_id")),
                        UUID.fromString(r.getString("settlement_id")),r.getLong("food_units"),r.getString("food_item"),r.getInt("food_count"),
                        new Commodity(r.getString("commodity")),r.getString("payment_item"),r.getInt("payment_count"),r.getLong("units_per_item")),
                        Status.valueOf(r.getString("status")),r.getLong("accepted_period")));
            }
        }
    }
    Barter matching(FoodBarters.Terms terms) throws SQLException {
        var saved=find(terms.operation()).orElseThrow(()->new SQLException("Barter not reserved"));
        if(!saved.terms().equals(terms))throw new SQLException("Barter operation reused with changed terms");return saved;
    }
    long reservedFood(UUID town) throws SQLException {
        long total=0;
        try(var s=connection.prepareStatement("SELECT food_units FROM food_barters WHERE settlement_id=? AND status='RESERVED'")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next())total=Math.addExact(total,r.getLong(1));}
        }return total;
    }
    boolean hasReservedPlayer(UUID player) throws SQLException {
        try(var s=connection.prepareStatement("SELECT 1 FROM food_barters WHERE player_id=? AND status='RESERVED'")) {
            s.setString(1,player.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
    String reserve(FoodBarters.Terms terms,long period) throws SQLException {
        owner();
        try(var s=connection.prepareStatement("INSERT INTO food_barters VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,terms.operation().toString());s.setString(2,terms.player().toString());s.setString(3,terms.settlement().toString());
            s.setLong(4,terms.foodUnits());s.setString(5,terms.foodItem());s.setInt(6,terms.foodCount());s.setString(7,terms.commodity().id());
            s.setString(8,terms.paymentItem());s.setInt(9,terms.paymentCount());s.setLong(10,terms.unitsPerItem());s.setString(11,"RESERVED");s.setLong(12,period);s.executeUpdate();
        }
        move(terms,-terms.paymentUnits(),"RESERVE",period);return "RESERVED";
    }
    void finish(FoodBarters.Terms terms,Status status,long period) throws SQLException {
        owner();if(status==Status.RESERVED)throw new IllegalArgumentException("Final barter status required");
        try(var s=connection.prepareStatement("UPDATE food_barters SET status=? WHERE operation_id=? AND status='RESERVED'")) {
            s.setString(1,status.name());s.setString(2,terms.operation().toString());if(s.executeUpdate()!=1)throw new SQLException("Barter no longer reserved");
        }
        if(status==Status.CANCELLED)move(terms,terms.paymentUnits(),"CANCEL",period);
    }
    private void move(FoodBarters.Terms terms,long delta,String phase,long period) throws SQLException {
        long before;
        try(var s=connection.prepareStatement("SELECT units FROM economy_stocks WHERE settlement_id=? AND commodity=?")) {
            s.setString(1,terms.settlement().toString());s.setString(2,terms.commodity().id());
            try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Barter stock missing");before=r.getLong(1);}
        }
        long after=Math.addExact(before,delta);if(after<0)throw new EconomyRuntimeRepository.PurchaseDeclined("Insufficient barter stock");
        try(var s=connection.prepareStatement("INSERT INTO barter_stock_moves(operation_id,phase,after_period,before_units,after_units) VALUES (?,?,?,?,?)")) {
            s.setString(1,terms.operation().toString());s.setString(2,phase);s.setLong(3,period);s.setLong(4,before);s.setLong(5,after);s.executeUpdate();
        }
        try(var s=connection.prepareStatement("UPDATE economy_stocks SET units=? WHERE settlement_id=? AND commodity=? AND units=?")) {
            s.setLong(1,after);s.setString(2,terms.settlement().toString());s.setString(3,terms.commodity().id());s.setLong(4,before);
            if(s.executeUpdate()!=1)throw new SQLException("Barter stock changed");
        }
    }
    private void owner() throws SQLException {if(connection.getAutoCommit())throw new SQLException("Barter requires owner transaction");}
}
