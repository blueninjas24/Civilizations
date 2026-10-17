package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Stable selection helpers for recipes that turn actual bakery inputs into abstract meals. */
public final class BakeryRecipes {
    private BakeryRecipes() {}

    /** Runtime preference: consider recipes with more ingredients first, then stable recipe ID. */
    public static List<ProductionRecipe> ordered(Collection<ProductionRecipe> recipes,
                                                   Set<Commodity> supportedGoods) {
        Objects.requireNonNull(recipes);Objects.requireNonNull(supportedGoods);
        return recipes.stream().filter(r->r.industry().equals("bakery"))
                .filter(r->supportedGoods.containsAll(r.inputs().keySet())&&supportedGoods.containsAll(r.outputs().keySet()))
                .sorted(Comparator.comparingInt((ProductionRecipe r)->r.inputs().size()).reversed()
                        .thenComparing(ProductionRecipe::id)).toList();
    }

    /** Startup preference: the simplest available input recipe, preserving bread as the fallback. */
    public static Optional<ProductionRecipe> baseline(Collection<ProductionRecipe> recipes) {
        Objects.requireNonNull(recipes);
        return recipes.stream().filter(r->r.industry().equals("bakery"))
                .sorted(Comparator.comparingInt((ProductionRecipe r)->r.inputs().size()).thenComparing(ProductionRecipe::id))
                .findFirst();
    }

    /** Startup fallback restricted to a saved store catalogue. */
    public static Optional<ProductionRecipe> baseline(Collection<ProductionRecipe> recipes,Set<Commodity> supportedGoods) {
        Objects.requireNonNull(recipes);Objects.requireNonNull(supportedGoods);
        return recipes.stream().filter(r->r.industry().equals("bakery"))
                .filter(r->supportedGoods.containsAll(r.inputs().keySet())&&supportedGoods.containsAll(r.outputs().keySet()))
                .sorted(Comparator.comparingInt((ProductionRecipe r)->r.inputs().size()).thenComparing(ProductionRecipe::id))
                .findFirst();
    }

    /** First feasible recipe in the supplied deterministic order, using current stock and storage. */
    public static Optional<ProductionRecipe> produced(List<ProductionRecipe> ordered,Map<Commodity,Long> stock,
            Map<Commodity,Long> space,long freeSpace,long workers) {
        Objects.requireNonNull(stock);Objects.requireNonNull(space);Objects.requireNonNull(ordered);
        for(var recipe:ordered) {
            if(!recipe.industry().equals("bakery")||!stock.keySet().containsAll(recipe.inputs().keySet())
                    ||!stock.keySet().containsAll(recipe.outputs().keySet())
                    ||!space.keySet().containsAll(recipe.inputs().keySet())||!space.keySet().containsAll(recipe.outputs().keySet()))continue;
            if(RecipeProduction.calculate(recipe,recipe.siteCapacity(),workers,freeSpace,stock,space).batches()>0)
                return Optional.of(recipe);
        }
        return Optional.empty();
    }
}
