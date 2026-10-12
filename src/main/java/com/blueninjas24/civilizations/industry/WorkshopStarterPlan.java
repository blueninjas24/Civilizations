package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Shared read-only feasibility for prospective investment and actual paid acceptance. */
public final class WorkshopStarterPlan {
    private WorkshopStarterPlan() {}
    public record Review(boolean eligible,Map<Commodity,Long> goods,long price,long space,String explanation) {
        public Review {goods=Collections.unmodifiableMap(new TreeMap<>(goods));}
    }
    public static Review review(ProductionRecipe recipe,int batches,WorkshopStarterPolicy policy,long cash,
            long space,long haul,Map<Commodity,Long> stocks,Map<Commodity,Long> weights) {
        Objects.requireNonNull(recipe);Objects.requireNonNull(policy);Objects.requireNonNull(stocks);Objects.requireNonNull(weights);
        if(batches<1||cash<0||space<0||haul<0)throw new IllegalArgumentException("Invalid prospective starter budget");
        if(!policy.enabled()||batches>policy.maximumBatches())return no("Starter supplies disabled or batch cap exceeded");
        var goods=new TreeMap<Commodity,Long>();long price=0,used=0,hauled=0;
        try {
            for(var input:new TreeMap<>(recipe.inputs()).entrySet()) {
                var good=input.getKey();var unitPrice=policy.prices().get(good);var weight=weights.get(good);
                if(good.equals(Commodity.FOOD)||unitPrice==null||weight==null||weight<1)return no("Starter material unpriced, forbidden or absent from store catalog");
                long units=Math.multiplyExact(input.getValue(),batches);
                if(stocks.getOrDefault(good,0L)<Math.addExact(units,policy.reserveUnits()))return no("Insufficient real surplus after protected material reserve");
                goods.put(good,units);price=Math.addExact(price,Math.multiplyExact(units,unitPrice));
                used=Math.addExact(used,Math.multiplyExact(units,weight));hauled=Math.addExact(hauled,units);
            }
            if(price>policy.maximumHundredths()||price>cash||used>space||hauled>haul)return no("Starter money, storage or haul limit exceeded");
            return new Review(true,goods,price,used,"One complete affordable starter bundle after protected material reserves; no assets reserved");
        }catch(ArithmeticException overflow){return no("Starter bundle exceeds supported quantity range");}
    }
    private static Review no(String reason){return new Review(false,Map.of(),0,0,reason);}
}
