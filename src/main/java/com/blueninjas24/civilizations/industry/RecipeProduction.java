package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Deterministic complete batches. Pending cargo is not an input and keeps its storage reservation. */
public final class RecipeProduction {
    public record Plan(long batches, Map<Commodity,Long> consumed, Map<Commodity,Long> produced, String explanation) {
        public Plan { consumed=Map.copyOf(consumed); produced=Map.copyOf(produced); }
    }
    private RecipeProduction() {}
    public static Plan calculate(ProductionRecipe recipe,long requested,long workers,long freeSpace,
                                 Map<Commodity,Long> available,Map<Commodity,Long> space) {
        if(requested<0 || workers<0 || freeSpace<0 || available.values().stream().anyMatch(v->v<0))
            throw new IllegalArgumentException("Invalid production resources");
        var goods=new TreeSet<>(recipe.inputs().keySet()); goods.addAll(recipe.outputs().keySet());
        if(goods.stream().anyMatch(g->!available.containsKey(g) || !space.containsKey(g) || space.get(g)<1))
            throw new IllegalArgumentException("Store must support every recipe input and output");
        long labor=Math.multiplyExact(workers,recipe.batchesPerWorker());
        long limit=Math.min(requested,Math.min(labor,recipe.siteCapacity()));
        long inputLimit=Long.MAX_VALUE;
        for(var e:recipe.inputs().entrySet())inputLimit=Math.min(inputLimit,available.get(e.getKey())/e.getValue());
        long consumedSpace=weight(recipe.inputs(),space),producedSpace=weight(recipe.outputs(),space);
        long net=Math.subtractExact(producedSpace,consumedSpace);
        long storageLimit=net>0?freeSpace/net:Long.MAX_VALUE;
        long batches=Math.min(limit,Math.min(inputLimit,storageLimit));
        var consumed=scale(recipe.inputs(),batches); var produced=scale(recipe.outputs(),batches);
        String explanation="Recipe "+recipe.id()+": "+batches+" batches; limits requested="+requested+", labor="+labor+
                ", site="+recipe.siteCapacity()+", inputs="+inputLimit+", storage="+(net>0?storageLimit:"no net growth")+
                "; consumed="+display(consumed)+"; produced="+display(produced);
        return new Plan(batches,consumed,produced,explanation);
    }
    private static long weight(Map<Commodity,Long> amounts,Map<Commodity,Long> space) {
        long sum=0;for(var e:amounts.entrySet())sum=Math.addExact(sum,Math.multiplyExact(e.getValue(),space.get(e.getKey())));return sum;
    }
    private static Map<Commodity,Long> scale(Map<Commodity,Long> amounts,long batches) {
        var result=new TreeMap<Commodity,Long>();amounts.forEach((g,u)->result.put(g,Math.multiplyExact(u,batches)));return result;
    }
    private static Map<String,Long> display(Map<Commodity,Long> amounts) {
        var result=new TreeMap<String,Long>();amounts.forEach((g,u)->result.put(g.id(),u));return result;
    }
}
