package com.blueninjas24.civilizations.economy;

/** Editable future wheat harvest allocation; never converts existing food into ingredients. */
public record GrainHarvestPolicy(boolean enabled,int grainSharePercent,int protectedFoodPeriods,long capacity,long grainUnitsPerHarvest) {
    public GrainHarvestPolicy {
        if(grainSharePercent<0||grainSharePercent>100||protectedFoodPeriods<0||protectedFoodPeriods>100||capacity<1||capacity>1_000_000||grainUnitsPerHarvest<1||grainUnitsPerHarvest>1000)
            throw new IllegalArgumentException("Invalid grain harvest allocation, food reserve or storage terms");
    }
    public static GrainHarvestPolicy prototype(){return new GrainHarvestPolicy(true,25,2,256,1);}
    public static GrainHarvestPolicy disabled(){return new GrainHarvestPolicy(false,25,2,256,1);}
}
