package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.math.BigInteger;
import java.util.*;

/** Fundable complete recipe-input gaps, without inventing sellers, purchases or finished-goods customers. */
public final class WorkshopInputDemand {
    public record Review(long batches,Map<Commodity,Long> missing,long estimatedCost,String explanation) {
        public Review {missing=Collections.unmodifiableMap(new TreeMap<>(missing));}
    }
    private WorkshopInputDemand() {}
    public static Review review(ProductionRecipe recipe,long workers,long funds,long freeSpace,
                               Map<Commodity,Long> held,Map<Commodity,Long> pending,Map<Commodity,Long> sizes,
                               WorkshopSupplyPolicy policy) {
        Objects.requireNonNull(recipe);Objects.requireNonNull(policy);
        held=Map.copyOf(held);pending=Map.copyOf(pending);sizes=Map.copyOf(sizes);
        if(workers<0||funds<0||freeSpace<0||held.values().stream().anyMatch(n->n<0)||pending.values().stream().anyMatch(n->n<0))
            throw new IllegalArgumentException("Nonnegative workshop resources required");
        var goods=new HashSet<>(recipe.inputs().keySet());goods.addAll(recipe.outputs().keySet());
        for(var g:goods)if(!held.containsKey(g)||!sizes.containsKey(g)||sizes.get(g)<1)
            throw new IllegalArgumentException("Store must support all recipe inputs and outputs");
        if(!policy.enabled())return new Review(0,Map.of(),0,"Automatic input demand disabled");
        long maximum=Math.multiplyExact(Math.min(recipe.siteCapacity(),Math.multiplyExact(workers,recipe.batchesPerWorker())),policy.inputPeriods());
        long lo=0,hi=maximum;
        while(lo<hi) {
            long mid=lo+(hi-lo+1)/2;
            if(estimate(recipe,mid,funds,freeSpace,held,pending,sizes,policy)!=null)lo=mid;else hi=mid-1;
        }
        var estimate=estimate(recipe,lo,funds,freeSpace,held,pending,sizes,policy);
        return new Review(lo,estimate.missing(),estimate.estimatedCost(),
            "Fundable input buffer: "+lo+" batches; missing inputs cost "+estimate.estimatedCost()+" hundredths; not a purchase or sales forecast");
    }
    private static Review estimate(ProductionRecipe recipe,long batches,long funds,long space,
                                   Map<Commodity,Long> held,Map<Commodity,Long> pending,Map<Commodity,Long> sizes,WorkshopSupplyPolicy policy) {
        var missing=new TreeMap<Commodity,Long>();var cost=BigInteger.ZERO;var incoming=BigInteger.ZERO;
        for(var e:recipe.inputs().entrySet()) {
            long covered=Math.addExact(held.get(e.getKey()),pending.getOrDefault(e.getKey(),0L));
            long units=Math.max(0,Math.multiplyExact(e.getValue(),batches)-covered);
            if(units==0)continue;var price=policy.unitHundredths().get(e.getKey());if(price==null)return null;
            missing.put(e.getKey(),units);cost=cost.add(BigInteger.valueOf(units).multiply(BigInteger.valueOf(price)));
            incoming=incoming.add(BigInteger.valueOf(units).multiply(BigInteger.valueOf(sizes.get(e.getKey()))));
        }
        var growth=weight(recipe.outputs(),sizes).subtract(weight(recipe.inputs(),sizes)).max(BigInteger.ZERO).multiply(BigInteger.valueOf(batches));
        if(cost.compareTo(BigInteger.valueOf(funds))>0||incoming.add(growth).compareTo(BigInteger.valueOf(space))>0)return null;
        return new Review(batches,missing,cost.longValueExact(),"Planning estimate only");
    }
    private static BigInteger weight(Map<Commodity,Long> goods,Map<Commodity,Long> sizes) {
        var total=BigInteger.ZERO;for(var e:goods.entrySet())total=total.add(BigInteger.valueOf(e.getValue()).multiply(BigInteger.valueOf(sizes.get(e.getKey()))));return total;
    }
}
