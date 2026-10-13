package com.blueninjas24.civilizations.economy;

import java.math.BigInteger;

/** One physical harvest budget: capped public food, owned surplus/ingredients, or unused work. */
public final class FarmMarketHarvest {
    private FarmMarketHarvest(){}
    public record Plan(long potentialWork,long actualWork,long publicFood,long businessFood,long grainWork,long grainUnits,
                       long publicNeed,FarmMarketPolicy policy,GrainHarvestPolicy grainPolicy,Long wheat,long crops,long observedMillis,String explanation){}
    public static Plan plan(long potential,long openingFood,long incomingFood,long demand,long heldGrain,long freeSpace,
                            FarmObservation farm,GrainHarvestPolicy grain,FarmMarketPolicy market) {
        if(!market.enabled())throw new IllegalArgumentException("Explicit market opt-in required");
        for(long n:new long[]{potential,openingFood,incomingFood,demand,heldGrain,freeSpace})if(n<0)throw new IllegalArgumentException("Nonnegative farm accounting required");
        if(!farm.complete())throw new IllegalArgumentException("Complete farm evidence required");
        var ingredients=GrainHarvest.plan(potential,openingFood,incomingFood,demand,heldGrain,farm,true,grain);
        long grainWork=Math.min(ingredients.diverted(),Math.min(freeSpace,Math.max(0,market.grainStockTarget()-heldGrain))/grain.grainUnitsPerHarvest());
        long grainUnits=Math.multiplyExact(grainWork,grain.grainUnitsPerHarvest());
        long room=freeSpace-grainUnits;
        long target=Math.multiplyExact(demand,Math.incrementExact((long)market.foodReservePeriods()));
        long need=Math.max(0,target-Math.addExact(openingFood,incomingFood));
        long low=0,high=potential-grainWork;
        // Commercial food rises monotonically with accepted output. Storage cannot silently
        // turn the whole harvest into a compulsory donation when the contribution cap is reached.
        while(low<high) {
            long mid=low+(high-low)/2+1;
            long contribution=Math.min(need,share(mid,market.villageSharePercent()));
            if(mid-contribution<=room)low=mid;else high=mid-1;
        }
        long publicFood=Math.min(need,share(low,market.villageSharePercent()));
        return new Plan(potential,Math.addExact(low,grainWork),publicFood,low-publicFood,grainWork,grainUnits,need,market,grain,farm.grainCrops(),farm.plantedCrops(),farm.observedMillis(),
                "Capped village contribution; owned surplus uses finite storage; unaccepted work stays idle, not extra village food");
    }
    private static long share(long units,int percent) {
        return BigInteger.valueOf(units).multiply(BigInteger.valueOf(percent)).divide(BigInteger.valueOf(100)).longValueExact();
    }
}
