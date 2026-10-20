package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import com.blueninjas24.civilizations.industry.LivestockMarketPolicy.Position;
import java.sql.*;
import java.util.*;

/** Bounded SQL-owner retail quotes. Paper supplies immutable player positions; no world/chunk access. */
public final class LivestockMarket {
    public record Offer(Terms terms, String businessName, int x, int y, int z, long availableUnits,
                        long quotedMillis, long expiresMillis) {
        public Offer { Objects.requireNonNull(terms); Objects.requireNonNull(businessName); }
    }
    /** Saved site accompanies owner acceptance so a later main-thread exchange can recheck proximity. */
    public record Reservation(LivestockProductSales.Receipt receipt, UUID world, int x, int y, int z) {
        public LivestockProductSales.Status status() { return receipt.status(); }
    }
    private final Connection c;
    private final LivestockMarketPolicy policy;
    private final LivestockProductSales sales;
    public LivestockMarket(Connection c, LivestockMarketPolicy policy, BusinessOperationPolicy operations) {
        this.c = Objects.requireNonNull(c); this.policy = Objects.requireNonNull(policy);
        sales = new LivestockProductSales(c, operations);
    }
    public List<Offer> offers(UUID player, Position position, long now, Set<UUID> worlds) throws SQLException {
        if (now < 0) throw new IllegalArgumentException("Negative quote time");
        if (!policy.enabled() || !worlds.contains(position.world())) return List.of();
        long expires = Math.addExact(now, Math.multiplyExact(1000L, policy.offerSeconds()));
        var result = new ArrayList<Offer>();
        try (var s = c.prepareStatement("""
                SELECT b.id,b.settlement_id,b.name,h.species,p.x,p.y,p.z FROM livestock_herds h
                JOIN businesses b ON b.id=h.business_id JOIN business_sites p ON p.id=b.site_id
                WHERE h.species IN('COW','CHICKEN') AND p.world_id=? AND abs(p.y-?)<=? AND abs(p.x-?)<=? AND abs(p.z-?)<=?
                ORDER BY ((p.x-?)*(p.x-?)+(p.z-?)*(p.z-?)),b.id LIMIT ?
                """)) {
            s.setString(1, position.world().toString()); s.setDouble(2, position.y()); s.setInt(3, policy.verticalRadius());
            s.setDouble(4, position.x()); s.setInt(5, policy.horizontalRadius()); s.setDouble(6, position.z()); s.setInt(7, policy.horizontalRadius());
            s.setDouble(8, position.x()); s.setDouble(9, position.x()); s.setDouble(10, position.z()); s.setDouble(11, position.z());
            s.setInt(12, policy.maximumSellers());
            try (var r = s.executeQuery()) { while (r.next()) {
                int x = r.getInt("x"), y = r.getInt("y"), z = r.getInt("z");
                if (!policy.nearby(position, position.world(), x, y, z)) continue;
                var product = r.getString("species").equals("COW") ? Product.MILK : Product.EGGS;
                var terms = policy.terms(UUID.randomUUID(), player, UUID.fromString(r.getString("settlement_id")),
                        position.world(), UUID.fromString(r.getString("id")), product);
                try {
                    var supplier = sales.supplier(terms, worlds);
                    result.add(new Offer(terms, r.getString("name"), x, y, z,
                            supplier.store().units().get(product.commodity()), now, expires));
                } catch (LivestockProductSales.Declined unavailable) { /* No eligible full order from this saved supplier. */ }
            } }
        }
        return List.copyOf(result);
    }
    /** Replays accepted reservations even after settings/availability change; caller still checks before inventory exchange. */
    public Reservation reserve(Terms terms, Position player, Set<UUID> worlds) throws SQLException {
        boolean replay = sales.find(terms.operation()).isPresent();
        if (!replay && !policy.accepts(terms)) throw new LivestockProductSales.Declined("Market disabled or price/order changed; reopen the market");
        var founding = new BusinessRepository(c).findFounding(terms.seller()).orElseThrow(() -> new LivestockProductSales.Declined("Saved supplier required"));
        var site = founding.site();
        if (!replay && !policy.nearby(player, site.worldId(), site.x(), site.y(), site.z()))
            throw new LivestockProductSales.Declined("Supplier is no longer nearby");
        return new Reservation(sales.reserve(terms, worlds), site.worldId(), site.x(), site.y(), site.z());
    }
    /** Owner-ordered recovery of a failed/unsubmitted reservation; never refunds an exchanged player inventory. */
    public Optional<LivestockProductSales.Receipt> cancel(Terms terms) throws SQLException {
        return sales.find(terms.operation()).isEmpty() ? Optional.empty() : Optional.of(sales.cancel(terms));
    }
}
