package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;

/** Deterministic multi-recipe craftsman work; saved catalogues and actual inputs remain authoritative. */
final class CraftRecipeChoice {
    private CraftRecipeChoice() {}

    static Optional<ProductionRecipe> select(Connection c, UUID business, IndustryRuntimePolicy policy,
            BusinessOperationPolicy operations, long budget) throws SQLException {
        var store = new IndustryRepository(c).store(business).orElseThrow();
        var recipes = policy.recipes().values().stream().filter(r -> r.industry().equals("craftsman"))
                .filter(r -> store.spacePerUnit().keySet().containsAll(r.inputs().keySet())
                        && store.spacePerUnit().keySet().containsAll(r.outputs().keySet()))
                .sorted(Comparator.comparing(ProductionRecipe::id)).toList();
        if (recipes.size() < 2) return recipes.stream().findFirst();
        long workers = new BusinessRepository(c).currentWorkers(business).size();
        for (var recipe : recipes)
            if (RecipeProduction.calculate(recipe, recipe.siteCapacity(), workers, store.freeSpace(),
                    store.units(), store.spacePerUnit()).batches() > 0) return Optional.of(recipe);
        // Incoming goods may guide future ordering, but are never supplied to production.
        var incoming = new TreeMap<Commodity, Long>();
        int count = 0;
        try (var s = c.prepareStatement("SELECT commodity,units FROM local_shipments WHERE buyer_id=? AND delivered_period IS NULL ORDER BY id LIMIT 129")) {
            s.setString(1, business.toString());
            try (var rows = s.executeQuery()) { while (rows.next()) {
                count++;
                incoming.merge(new Commodity(rows.getString(1)), rows.getLong(2), Math::addExact);
            } }
        }
        if (count <= 128) {
            new WorkshopStarterSupplies(c).pending(business).forEach((g, n) -> incoming.merge(g, n, Math::addExact));
            for (var recipe : recipes)
                if (recipe.inputs().entrySet().stream().allMatch(e ->
                        Math.addExact(store.units().getOrDefault(e.getKey(), 0L), incoming.getOrDefault(e.getKey(), 0L)) >= e.getValue()))
                    return Optional.of(recipe);
            if (policy.supply().enabled()) {
                var town = new BusinessRepository(c).findBusiness(business).orElseThrow().settlementId();
                var warehouses = new ArrayList<UUID>();
                try (var s = c.prepareStatement("SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id WHERE b.settlement_id=? AND b.industry='warehouse' ORDER BY b.id LIMIT ?")) {
                    s.setString(1, town.toString());s.setInt(2, policy.supply().warehousesPerReview() + 1);
                    try (var rows = s.executeQuery()) { while (rows.next()) warehouses.add(UUID.fromString(rows.getString(1))); }
                }
                if (warehouses.size() <= policy.supply().warehousesPerReview()) {
                    var sources = new TreeMap<Commodity, Long>();
                    for (var id : warehouses) if (new BusinessOperations(c, operations).evaluate(id).permitted())
                        new IndustryRepository(c).store(id).orElseThrow().units().forEach((g, n) -> sources.merge(g, n, Math::addExact));
                    for (var recipe : recipes) {
                        long cost = 0, space = 0;boolean feasible = true;
                        for (var e : recipe.inputs().entrySet()) {
                            long missing = Math.max(0, e.getValue() - Math.addExact(store.units().getOrDefault(e.getKey(), 0L), incoming.getOrDefault(e.getKey(), 0L)));
                            if (missing == 0) continue;
                            var price = policy.purchasingSupply().unitHundredths().get(e.getKey());
                            if (price == null || missing > sources.getOrDefault(e.getKey(), 0L)) { feasible = false;break; }
                            cost = Math.addExact(cost, Math.multiplyExact(missing, price));
                            space = Math.addExact(space, Math.multiplyExact(missing, store.spacePerUnit().get(e.getKey())));
                        }
                        if (feasible && cost <= budget && space <= store.freeSpace()) return Optional.of(recipe);
                    }
                }
            }
        }
        return Optional.of(recipes.getFirst()); // Stable baseline for genuine input demand, not guaranteed sales.
    }
}
