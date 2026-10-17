package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;

/** Read-only choice of real, affordable bakery ingredients. Pending goods are never production inputs. */
final class BakeryRecipeChoice {
    private BakeryRecipeChoice() {}

    static Optional<ProductionRecipe> supply(Connection c, UUID business, IndustryRuntimePolicy policy,
            BusinessOperationPolicy operations, long budget) throws SQLException {
        var repository = new IndustryRepository(c);
        var firm = new BusinessRepository(c).findBusiness(business).orElseThrow();
        if (!firm.industry().equals("bakery")) return policy.recipes().values().stream()
                .filter(r -> r.industry().equals(firm.industry())).sorted(Comparator.comparing(ProductionRecipe::id)).findFirst();
        var store = repository.store(business).orElseThrow();
        var recipes = BakeryRecipes.ordered(policy.recipes().values(), store.spacePerUnit().keySet());
        if (recipes.size() < 2) return recipes.stream().findFirst();
        var pending = new TreeMap<Commodity,Long>();
        int count = 0;
        try (var s = c.prepareStatement("SELECT commodity,units FROM local_shipments WHERE buyer_id=? AND delivered_period IS NULL ORDER BY id LIMIT 129")) {
            s.setString(1,business.toString());
            try (var r = s.executeQuery()) { while (r.next()) { count++; pending.merge(new Commodity(r.getString(1)),r.getLong(2),Math::addExact); } }
        }
        new WorkshopStarterSupplies(c).pending(business).forEach((g,u) -> pending.merge(g,u,Math::addExact));
        if (count > 128) return BakeryRecipes.baseline(recipes);
        var sources = sources(c, firm.settlementId(), business, operations);
        for (var recipe : recipes) {
            long cost = 0, space = 0; boolean feasible = true;
            for (var e : recipe.inputs().entrySet()) {
                var good = e.getKey();
                long missing = Math.max(0,e.getValue() - Math.addExact(store.units().getOrDefault(good,0L),pending.getOrDefault(good,0L)));
                if (missing == 0) continue;
                Long price = policy.purchasingSupply().unitHundredths().get(good);
                if (price == null || price <= 0 || missing > sources.getOrDefault(good,0L)) { feasible = false; break; }
                cost = Math.addExact(cost,Math.multiplyExact(missing,price));
                space = Math.addExact(space,Math.multiplyExact(missing,store.spacePerUnit().get(good)));
            }
            if (feasible && cost <= budget && space <= store.freeSpace()) return Optional.of(recipe);
        }
        return BakeryRecipes.baseline(recipes);
    }

    private static Map<Commodity,Long> sources(Connection c, UUID town, UUID buyer, BusinessOperationPolicy operations) throws SQLException {
        var ids = new ArrayList<UUID>();
        try (var s = c.prepareStatement("""
            SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id
            JOIN business_sites p ON p.id=b.site_id JOIN settlement_worlds w ON w.settlement_id=b.settlement_id AND w.world_id=p.world_id
            WHERE b.settlement_id=? AND b.id<>? AND b.industry IN('warehouse','livestock','farming') ORDER BY b.id LIMIT 65
            """)) {
            s.setString(1,town.toString());s.setString(2,buyer.toString());
            try (var r = s.executeQuery()) { while (r.next()) ids.add(UUID.fromString(r.getString(1))); }
        }
        if (ids.size() > 64) return Map.of(); // No incomplete survey can justify optional purchasing.
        var result = new TreeMap<Commodity,Long>();var industry = new IndustryRepository(c);
        var firms = new BusinessRepository(c);var availability = new BusinessOperations(c,operations);
        for (var id : ids) {
            if (!availability.evaluate(id).permitted()) continue;
            var kind = firms.findBusiness(id).orElseThrow().industry();var store = industry.store(id).orElseThrow();
            Set<Commodity> allowed;
            if (kind.equals("warehouse")) allowed = store.units().keySet();
            else if (kind.equals("farming") && FarmMarketStore.supported(c,id,store)) allowed = Set.of(FarmMarketStore.GRAIN);
            else if (kind.equals("livestock")) {
                var herd = new LivestockRepository(c).load(id);
                if (herd.isEmpty()) continue;
                allowed = Set.of(new Commodity(herd.orElseThrow().species() == LivestockHerd.Species.COW ? "milk" : "eggs"));
            } else continue;
            for (var good : allowed) result.merge(good,store.units().getOrDefault(good,0L),Math::addExact);
        }
        return result;
    }
}
