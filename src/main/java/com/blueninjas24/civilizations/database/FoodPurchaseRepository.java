package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.FoodPurchases.Terms;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Owner-transaction rows. RESERVED removes spendable funds, never credits unverified food. */
final class FoodPurchaseRepository {
    enum Status { RESERVED, COMPLETED, CANCELLED }
    record Purchase(Terms terms, Status status, long acceptedPeriod) {}
    private final Connection connection;
    FoodPurchaseRepository(Connection connection) { this.connection = connection; }
    Optional<Purchase> find(UUID operation) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM food_purchases WHERE operation_id=?")) {
            s.setString(1, operation.toString());
            try (var r = s.executeQuery()) {
                return r.next() ? Optional.of(new Purchase(new Terms(operation, UUID.fromString(r.getString("player_id")),
                        UUID.fromString(r.getString("settlement_id")), r.getLong("units"), r.getString("item"), r.getInt("item_count"),
                        r.getInt("emeralds"), r.getLong("rate")), Status.valueOf(r.getString("status")), r.getLong("accepted_period"))) : Optional.empty();
            }
        }
    }
    Purchase matching(Terms terms) throws SQLException {
        var purchase = find(terms.operation()).orElseThrow(() -> new SQLException("Purchase not reserved"));
        if (!purchase.terms().equals(terms)) throw new SQLException("Purchase operation ID reused with different terms");
        return purchase;
    }
    long reservedUnits(UUID town) throws SQLException {
        long units = 0;
        try (var s = connection.prepareStatement("SELECT units FROM food_purchases WHERE settlement_id=? AND status='RESERVED'")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) { while (r.next()) units = Math.addExact(units, r.getLong(1)); }
        }
        return units;
    }
    Purchase reserve(Terms terms, long period) throws SQLException {
        owner();
        new TreasuryRepository(connection).postRows(ledgerId(terms.operation(), "reserve"), terms.settlement(), -terms.cost(),
                "Reserved food purchase " + terms.operation());
        try (var s = connection.prepareStatement("INSERT INTO food_purchases VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1, terms.operation().toString()); s.setString(2, terms.player().toString()); s.setString(3, terms.settlement().toString());
            s.setLong(4, terms.units()); s.setString(5, terms.item()); s.setInt(6, terms.count()); s.setInt(7, terms.emeralds());
            s.setLong(8, terms.hundredthsPerEmerald()); s.setString(9, Status.RESERVED.name()); s.setLong(10, period); s.executeUpdate();
        }
        return new Purchase(terms, Status.RESERVED, period);
    }
    void finish(Terms terms, Status status) throws SQLException {
        owner();
        if (status == Status.RESERVED) throw new IllegalArgumentException("Final purchase state required");
        try (var s = connection.prepareStatement("UPDATE food_purchases SET status=? WHERE operation_id=? AND status='RESERVED'")) {
            s.setString(1, status.name()); s.setString(2, terms.operation().toString());
            if (s.executeUpdate() != 1) throw new SQLException("Purchase is no longer reserved");
        }
        if (status == Status.CANCELLED) new TreasuryRepository(connection).postRows(ledgerId(terms.operation(), "cancel"),
                terms.settlement(), terms.cost(), "Cancelled unexchanged food purchase " + terms.operation());
    }
    private void owner() throws SQLException { if (connection.getAutoCommit()) throw new SQLException("Purchase rows require owner transaction"); }
    private static UUID ledgerId(UUID operation, String phase) {
        return UUID.nameUUIDFromBytes(("civilizations-food-purchase-1|" + operation + "|" + phase).getBytes(StandardCharsets.UTF_8));
    }
}
