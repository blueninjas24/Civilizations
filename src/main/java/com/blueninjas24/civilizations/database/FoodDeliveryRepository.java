package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** Explicit already-delivered food. Caller must establish its real source; this ledger does not take player items. */
final class FoodDeliveryRepository {
    static final int MAXIMUM_PER_PERIOD = 64;
    record Receipt(UUID operation, UUID settlement, long units, String source, long eligibleAfterPeriod, Long appliedPeriod) {}
    record Pending(long deliveries, long eligibleNextPeriodUnits) {}
    private final Connection connection;
    FoodDeliveryRepository(Connection connection) { this.connection = connection; }
    static void validate(UUID operation, UUID town, long units, String source) {
        Objects.requireNonNull(operation); Objects.requireNonNull(town); Objects.requireNonNull(source);
        if (units < 1 || source.isBlank() || source.length() > 256) throw new IllegalArgumentException("Positive delivered units and a source reference are required");
    }
    Optional<Receipt> find(UUID operation) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM food_deliveries WHERE operation_id=?")) {
            s.setString(1,operation.toString()); try (var r = s.executeQuery()) { return r.next() ? Optional.of(read(r)) : Optional.empty(); }
        }
    }
    Receipt record(UUID operation, UUID town, long units, String source, long eligibleAfter) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Delivery requires its owner transaction");
        try (var s = connection.prepareStatement("INSERT INTO food_deliveries(operation_id,settlement_id,units,source,eligible_after,applied_period) VALUES (?,?,?,?,?,NULL)")) {
            s.setString(1,operation.toString()); s.setString(2,town.toString()); s.setLong(3,units); s.setString(4,source); s.setLong(5,eligibleAfter); s.executeUpdate();
        }
        return new Receipt(operation,town,units,source,eligibleAfter,null);
    }
    List<Receipt> eligible(UUID town, long period) throws SQLException {
        var result = new ArrayList<Receipt>();
        try (var s = connection.prepareStatement("SELECT * FROM food_deliveries WHERE settlement_id=? AND applied_period IS NULL AND eligible_after<? ORDER BY sequence LIMIT ?")) {
            s.setString(1,town.toString()); s.setLong(2,period); s.setInt(3,MAXIMUM_PER_PERIOD);
            try (var r = s.executeQuery()) { while(r.next()) result.add(read(r)); }
        }
        return List.copyOf(result);
    }
    static long units(List<Receipt> receipts) {
        long sum=0; for(var receipt:receipts) sum=Math.addExact(sum,receipt.units()); return sum;
    }
    void apply(List<Receipt> receipts, long period) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Delivery effects require food transaction");
        try (var s=connection.prepareStatement("UPDATE food_deliveries SET applied_period=? WHERE operation_id=? AND applied_period IS NULL AND eligible_after<?")) {
            for(var receipt:receipts) {
                s.setLong(1,period);s.setString(2,receipt.operation().toString());s.setLong(3,period);
                if(s.executeUpdate()!=1) throw new SQLException("Food delivery changed or is not eligible");
            }
        }
    }
    Pending pending(UUID town, long nextPeriod) throws SQLException {
        long count;
        try(var s=connection.prepareStatement("SELECT COUNT(*) FROM food_deliveries WHERE settlement_id=? AND applied_period IS NULL")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){count=r.getLong(1);}
        }
        return new Pending(count,units(eligible(town,nextPeriod)));
    }
    private static Receipt read(ResultSet r) throws SQLException {
        long applied=r.getLong("applied_period");boolean pending=r.wasNull();
        return new Receipt(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("settlement_id")),r.getLong("units"),
                r.getString("source"),r.getLong("eligible_after"),pending?null:applied);
    }
}
