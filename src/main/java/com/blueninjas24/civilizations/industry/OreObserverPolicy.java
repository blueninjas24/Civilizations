package com.blueninjas24.civilizations.industry;

/** Editable read-only observation budgets; these do not grant extraction or founding rights. */
public record OreObserverPolicy(boolean enabled,int verticalRadius,int cellsPerTick,int intervalTicks,int horizontalRadiusChunks) {
    public OreObserverPolicy {
        if(verticalRadius<1 || verticalRadius>128 || cellsPerTick<1 || cellsPerTick>4096 || intervalTicks<20 || intervalTicks>72000 || horizontalRadiusChunks<0 || horizontalRadiusChunks>2)
            throw new IllegalArgumentException("Invalid ore observation budgets");
    }
    /** Existing callers keep their single-home-chunk scope. */
    public OreObserverPolicy(boolean enabled,int verticalRadius,int cellsPerTick,int intervalTicks){this(enabled,verticalRadius,cellsPerTick,intervalTicks,0);}
    public static OreObserverPolicy prototype(){return new OreObserverPolicy(true,24,512,400,1);}
    public static OreObserverPolicy disabled(){return new OreObserverPolicy(false,24,512,400);}
}
