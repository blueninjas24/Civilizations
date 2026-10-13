package com.blueninjas24.civilizations.industry;

/** Editable evidence-only recognition and budgets; no resource, land or business grants. */
public record WaterObserverPolicy(boolean enabled,int minimumDepth,int cellsPerTick,int intervalTicks,int horizontalRadiusChunks) {
    public WaterObserverPolicy {
        if(minimumDepth<1||minimumDepth>64||cellsPerTick<1||cellsPerTick>4096
                ||intervalTicks<20||intervalTicks>72000||horizontalRadiusChunks<0||horizontalRadiusChunks>2)
            throw new IllegalArgumentException("Invalid water observation settings");
    }
    public static WaterObserverPolicy prototype(){return new WaterObserverPolicy(true,2,4096,400,1);}
    public static WaterObserverPolicy disabled(){var p=prototype();return new WaterObserverPolicy(false,p.minimumDepth,p.cellsPerTick,p.intervalTicks,p.horizontalRadiusChunks);}
}
