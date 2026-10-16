package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** SQL-owner player sale ledger. Not a live trade command. The future Paper adapter must independently
 * prove saved player intent before reserve, and saved payment/container exchange before complete.
 * Reservations remove sellable goods but retain their storage space until completion/cancellation. */
public final class LivestockProductSales {
    public enum Status { RESERVED, COMPLETED, CANCELLED }
    public record Receipt(Terms terms, Status status, long acceptedPeriod) {}
    public static final class Declined extends SQLException { Declined(String message) { super(message); } }
    private final Connection connection;
    private final BusinessOperationPolicy operations;
    public LivestockProductSales(Connection connection, BusinessOperationPolicy operations) {
        this.connection = Objects.requireNonNull(connection); this.operations = Objects.requireNonNull(operations);
    }
    static void createSchema(Connection c) throws SQLException {
        try (var s = c.createStatement()) {
            s.execute("""
                CREATE TABLE livestock_product_sales (
                    operation_id TEXT PRIMARY KEY NOT NULL,
                    player_id TEXT NOT NULL,settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    seller_id TEXT NOT NULL REFERENCES livestock_herds(business_id),
                    product TEXT NOT NULL CHECK(product IN('MILK','EGGS')),
                    commodity TEXT NOT NULL CHECK((product='MILK' AND commodity='milk') OR (product='EGGS' AND commodity='eggs')),
                    item_count INTEGER NOT NULL CHECK(typeof(item_count)='integer' AND item_count BETWEEN 1 AND 64),
                    emeralds INTEGER NOT NULL CHECK(typeof(emeralds)='integer' AND emeralds BETWEEN 1 AND 64),
                    rate INTEGER NOT NULL CHECK(typeof(rate)='integer' AND rate BETWEEN 1 AND 100000000),
                    space_per_unit INTEGER NOT NULL CHECK(typeof(space_per_unit)='integer' AND space_per_unit>0),
                    accepted_period INTEGER NOT NULL CHECK(typeof(accepted_period)='integer' AND accepted_period>=0),
                    status TEXT NOT NULL CHECK(status IN('RESERVED','COMPLETED','CANCELLED')),
                    CHECK(product<>'MILK' OR item_count=1),
                    FOREIGN KEY(seller_id,commodity) REFERENCES industry_stock(business_id,commodity),
                    FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
                )
                """);
            s.execute("CREATE INDEX pending_livestock_product_sales ON livestock_product_sales(seller_id,status)");
        }
    }
    /** Trusted accepted terms, not a quote/permission API. No player inventory or money is changed here. */
    public Receipt reserve(Terms terms, Set<UUID> activeWorlds) throws SQLException {
        var worlds = Set.copyOf(activeWorlds);
        return transaction(() -> {
            var old = find(terms.operation());
            if (old.isPresent()) return matching(terms);
            if (!worlds.contains(terms.world())) throw new Declined("Supplier world is absent");
            var businesses = new BusinessRepository(connection);
            var founding = businesses.findFounding(terms.seller()).orElseThrow(() -> new Declined("Saved livestock supplier required"));
            if (!founding.business().settlementId().equals(terms.settlement()) || !founding.site().worldId().equals(terms.world()))
                throw new Declined("Supplier must belong to the requested village and world");
            if (!new BusinessOperations(connection, operations).evaluate(terms.seller()).permitted())
                throw new Declined("Operating livestock supplier required");
            var herd = new LivestockRepository(connection).load(terms.seller()).orElseThrow(() -> new Declined("Saved herd required"));
            var species = terms.product() == Product.MILK ? LivestockHerd.Species.COW : LivestockHerd.Species.CHICKEN;
            if (herd.species() != species) throw new Declined("Product does not match the supplier herd");
            var population = new PopulationRepository(connection); boolean staffed = false;
            for (var job : businesses.currentWorkers(terms.seller())) {
                var person = population.findResident(job.residentId()).orElseThrow();
                if (person.settlementId().equals(terms.settlement()) && WorkEligibility.evaluate(person,
                        population.findWorkProfile(person.id()).orElseThrow(), founding.workerRequirements()).eligible()) {
                    staffed = true; break;
                }
            }
            if (!staffed) throw new Declined("Qualified supplier staff required");
            var store = new IndustryRepository(connection).store(terms.seller()).orElseThrow();
            if (store.units().getOrDefault(terms.product().commodity(), 0L) < terms.count())
                throw new Declined("Insufficient actual supplier product stock");
            long period = herd.state().period(), space = store.spacePerUnit().get(terms.product().commodity());
            changeStock(terms, -terms.count());
            try (var s = connection.prepareStatement("INSERT INTO livestock_product_sales(operation_id,player_id,settlement_id,world_id,seller_id,product,commodity,item_count,emeralds,rate,space_per_unit,accepted_period,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'RESERVED')")) {
                s.setString(1, terms.operation().toString()); s.setString(2, terms.player().toString());
                s.setString(3, terms.settlement().toString()); s.setString(4, terms.world().toString());
                s.setString(5, terms.seller().toString()); s.setString(6, terms.product().name());
                s.setString(7, terms.product().commodity().id()); s.setInt(8, terms.count()); s.setInt(9, terms.emeralds());
                s.setLong(10, terms.hundredthsPerEmerald()); s.setLong(11, space); s.setLong(12, period); s.executeUpdate();
            }
            return matching(terms);
        });
    }
    /** Caller must prove the player's durable EXCHANGED receipt first. Never infers payment from intent. */
    public Receipt complete(Terms terms) throws SQLException { return finish(terms, Status.COMPLETED); }
    /** Caller must prove that the player exchange was cancelled, never used after delivery. */
    public Receipt cancel(Terms terms) throws SQLException { return finish(terms, Status.CANCELLED); }
    private Receipt finish(Terms terms, Status target) throws SQLException {
        return transaction(() -> {
            var before = matching(terms);
            if (before.status() == target) return before;
            if (before.status() != Status.RESERVED) throw new SQLException("Livestock sale already ended with a different outcome");
            if (target == Status.CANCELLED) changeStock(terms, terms.count());
            else {
                var store = new IndustryRepository(connection).store(terms.seller()).orElseThrow();
                long balance = Math.addExact(store.hundredths(), terms.cost());
                try (var s = connection.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=?")) {
                    s.setLong(1, balance); s.setString(2, terms.seller().toString());
                    if (s.executeUpdate() != 1) throw new SQLException("Supplier capital account missing");
                }
            }
            try (var s = connection.prepareStatement("UPDATE livestock_product_sales SET status=? WHERE operation_id=? AND status='RESERVED'")) {
                s.setString(1, target.name()); s.setString(2, terms.operation().toString());
                if (s.executeUpdate() != 1) throw new SQLException("Livestock sale reservation changed");
            }
            new IndustryRepository(connection).store(terms.seller()).orElseThrow(); // Check retained capacity, including other holds.
            return new Receipt(terms, target, before.acceptedPeriod());
        });
    }
    public Optional<Receipt> find(UUID operation) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM livestock_product_sales WHERE operation_id=?")) {
            s.setString(1, operation.toString()); try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                return Optional.of(new Receipt(new Terms(operation, UUID.fromString(r.getString("player_id")),
                        UUID.fromString(r.getString("settlement_id")), UUID.fromString(r.getString("world_id")),
                        UUID.fromString(r.getString("seller_id")), Product.valueOf(r.getString("product")),
                        r.getInt("item_count"), r.getInt("emeralds"), r.getLong("rate")),
                        Status.valueOf(r.getString("status")), r.getLong("accepted_period")));
            }
        }
    }
    private Receipt matching(Terms terms) throws SQLException {
        var receipt = find(terms.operation()).orElseThrow(() -> new SQLException("Livestock sale not reserved"));
        if (!receipt.terms().equals(terms)) throw new SQLException("Livestock sale identity reused with different terms");
        return receipt;
    }
    private void changeStock(Terms terms, long delta) throws SQLException {
        try (var s = connection.prepareStatement("UPDATE industry_stock SET units=units+? WHERE business_id=? AND commodity=? AND units>=?")) {
            s.setLong(1, delta); s.setString(2, terms.seller().toString()); s.setString(3, terms.product().commodity().id());
            s.setLong(4, Math.max(0, -delta));
            if (s.executeUpdate() != 1) throw new SQLException("Supplier product stock changed");
        }
    }
    @FunctionalInterface private interface Work<T> { T run() throws SQLException; }
    private <T> T transaction(Work<T> work) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Livestock sale requires idle SQL owner");
        connection.setAutoCommit(false);
        try { var result = work.run(); connection.commit(); connection.setAutoCommit(true); return result; }
        catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
}
