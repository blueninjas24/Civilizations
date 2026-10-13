package com.blueninjas24.civilizations.industry;

/** Editable bounded bank observation; opening terms persist with each new habitat. */
public record FisheryShorelinePolicy(boolean enabled,int maximumRise,int maximumDistance,int cellsPerTick,int intervalTicks) {
    public FisheryShorelinePolicy {
        if(maximumRise<0||maximumRise>4||maximumDistance<0||maximumDistance>256||cellsPerTick<1||cellsPerTick>4096||intervalTicks<20||intervalTicks>72000)
            throw new IllegalArgumentException("Invalid fishery shoreline settings");
    }
    public static FisheryShorelinePolicy prototype(){return new FisheryShorelinePolicy(true,2,16,128,200);}
    public static FisheryShorelinePolicy disabled(){return new FisheryShorelinePolicy(false,2,16,128,200);}
}
