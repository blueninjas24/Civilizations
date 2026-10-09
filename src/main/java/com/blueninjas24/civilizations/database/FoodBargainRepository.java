package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import java.sql.*;
import java.util.*;

/** Owner-transaction discussion history. A quote holds no funds and cannot prove a player exchange. */
final class FoodBargainRepository {
    private final Connection connection;
    FoodBargainRepository(Connection connection) { this.connection=connection; }
    Optional<FoodBargaining.Deal> find(UUID operation) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM food_bargains WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()){return r.next() ? Optional.of(read(r)) : Optional.empty();}
        }
    }
    Optional<FoodBargaining.Deal> latest(UUID player,UUID town) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM food_bargains WHERE player_id=? AND settlement_id=? ORDER BY sequence DESC LIMIT 1")) {
            s.setString(1,player.toString());s.setString(2,town.toString());try(var r=s.executeQuery()){return r.next() ? Optional.of(read(r)) : Optional.empty();}
        }
    }
    void save(FoodBargaining.Deal deal) throws SQLException {
        owner();var t=deal.requested();var r=deal.result();
        try(var s=connection.prepareStatement("""
                INSERT INTO food_bargains(operation_id,player_id,settlement_id,units,item,item_count,requested,offered,rate,
                status,failed_attempts,observed_millis,expires_millis,cooldown_until,maximum_attempts,cooldown_seconds,offer_seconds)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            s.setString(1,t.operation().toString());s.setString(2,t.player().toString());s.setString(3,t.settlement().toString());
            s.setLong(4,t.units());s.setString(5,t.item());s.setInt(6,t.count());s.setInt(7,t.emeralds());s.setInt(8,r.offer());s.setLong(9,t.hundredthsPerEmerald());
            s.setString(10,r.status().name());s.setInt(11,r.failedAttempts());s.setLong(12,r.observedMillis());s.setLong(13,r.expiresMillis());
            s.setLong(14,r.cooldownUntil());s.setInt(15,r.policy().maximumAttempts());s.setInt(16,r.policy().cooldownSeconds());s.setInt(17,r.policy().offerSeconds());s.executeUpdate();
        }
    }
    void reserve(FoodPurchases.Terms terms,FoodBargaining.Policy policy,long now) throws SQLException {
        owner();var latest=latest(terms.player(),terms.settlement()).orElse(null);
        long observed=Math.max(now,latest==null ? 0 : latest.result().observedMillis());
        if(latest!=null && latest.result().status()==FoodBargaining.Status.CLOSED && observed<latest.result().cooldownUntil())
            throw new EconomyRuntimeRepository.PurchaseDeclined("Village bargaining cooldown is still active");
        var proposal=find(terms.operation()).orElse(null);
        if(proposal!=null) {
            if(latest==null || !latest.requested().operation().equals(terms.operation())
                    || (proposal.result().status()!=FoodBargaining.Status.AGREED && proposal.result().status()!=FoodBargaining.Status.COUNTER)
                    || !proposal.paymentTerms().equals(terms)
                    || observed>=proposal.result().expiresMillis()) throw new EconomyRuntimeRepository.PurchaseDeclined("Bargain expired, changed or superseded; make a new offer");
            try(var s=connection.prepareStatement("UPDATE food_bargains SET status='RESERVED' WHERE operation_id=? AND status IN ('AGREED','COUNTER')")) {
                s.setString(1,terms.operation().toString());if(s.executeUpdate()!=1)throw new SQLException("Bargain no longer available");
            }
        } else {
            // Quick menu acceptance is a successful alignment, but never bypasses an active cooldown.
            save(new FoodBargaining.Deal(terms,new FoodBargaining.Result(FoodBargaining.Status.RESERVED,terms.emeralds(),0,observed,observed,0,policy)));
        }
    }
    private void owner() throws SQLException {if(connection.getAutoCommit())throw new SQLException("Bargaining requires owner transaction");}
    private static FoodBargaining.Deal read(ResultSet r) throws SQLException {
        return new FoodBargaining.Deal(new FoodPurchases.Terms(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("player_id")),
                UUID.fromString(r.getString("settlement_id")),r.getLong("units"),r.getString("item"),r.getInt("item_count"),r.getInt("requested"),r.getLong("rate")),
                new FoodBargaining.Result(FoodBargaining.Status.valueOf(r.getString("status")),r.getInt("offered"),r.getInt("failed_attempts"),
                        r.getLong("observed_millis"),r.getLong("expires_millis"),r.getLong("cooldown_until"),
                        new FoodBargaining.Policy(true,r.getInt("maximum_attempts"),r.getInt("cooldown_seconds"),r.getInt("offer_seconds"))));
    }
}
