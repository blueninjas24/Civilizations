package com.blueninjas24.civilizations.economy;

import java.math.BigInteger;
import java.util.Objects;

/** Splits only actual future farm output. Grain replaces part of that work, never adds free work. */
public final class GrainHarvest {
    private GrainHarvest(){}
    public record Plan(long harvest,long foodOutput,long diverted,long grainUnits,long openingGrain,long closingGrain,
                       long protectedFood,long availableFood,Long wheat,long crops,GrainHarvestPolicy policy,String explanation){}
    public static Plan plan(long harvest,long openingFood,long incomingFood,long householdDemand,long openingGrain,
                            FarmObservation farm,boolean staffedFarm,GrainHarvestPolicy policy) {
        Objects.requireNonNull(farm);Objects.requireNonNull(policy);
        for(long n:new long[]{harvest,openingFood,incomingFood,householdDemand,openingGrain})if(n<0)throw new IllegalArgumentException("Nonnegative harvest accounting required");
        long available=Math.addExact(Math.addExact(openingFood,incomingFood),harvest);
        long protectedFood=Math.multiplyExact(householdDemand,Math.incrementExact((long)policy.protectedFoodPeriods()));
        long diverted=0;String reason;
        if(!policy.enabled())reason="Grain harvest disabled";
        else if(!staffedFarm)reason="Grain requires actual staffed farm work";
        else if(!farm.complete()||farm.grainCrops()==null)reason="Complete typed wheat evidence required";
        else if(farm.plantedCrops()==0||farm.grainCrops()==0||harvest==0)reason="No productive wheat capacity";
        else {
            long wheatShare=BigInteger.valueOf(harvest).multiply(BigInteger.valueOf(farm.grainCrops())).multiply(BigInteger.valueOf(policy.grainSharePercent()))
                    .divide(BigInteger.valueOf(farm.plantedCrops()).multiply(BigInteger.valueOf(100))).longValueExact();
            long foodSurplus=Math.max(0,available-protectedFood);
            long storageWork=Math.max(0,policy.capacity()-openingGrain)/policy.grainUnitsPerHarvest();
            diverted=Math.min(wheatShare,Math.min(foodSurplus,storageWork));
            reason=diverted==0?"No wheat allocation, protected food surplus or grain space":"Future wheat work allocated after protecting actual meals/reserves";
        }
        long grain=Math.multiplyExact(diverted,policy.grainUnitsPerHarvest());
        return new Plan(harvest,harvest-diverted,diverted,grain,openingGrain,Math.addExact(openingGrain,grain),protectedFood,available,
                farm.grainCrops(),farm.plantedCrops(),policy,reason);
    }
}
