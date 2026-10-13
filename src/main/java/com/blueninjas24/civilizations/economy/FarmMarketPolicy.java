package com.blueninjas24.civilizations.economy;

/** Capped contributions and finite commercial stock; prototype terms, not compulsory sale or free money. */
public record FarmMarketPolicy(boolean enabled,int villageSharePercent,int foodReservePeriods,long storageCapacity,long grainStockTarget) {
    public FarmMarketPolicy(boolean enabled,int share,int reserves,long capacity){this(enabled,share,reserves,capacity,Math.min(16,capacity));}
    public FarmMarketPolicy {
        if(villageSharePercent<0||villageSharePercent>100||foodReservePeriods<0||foodReservePeriods>100||storageCapacity<1||storageCapacity>1_000_000||grainStockTarget<0||grainStockTarget>storageCapacity)
            throw new IllegalArgumentException("Invalid farm contribution, reserve or commercial storage terms");
    }
    public static FarmMarketPolicy prototype(){return new FarmMarketPolicy(true,50,2,256,16);}
    public static FarmMarketPolicy disabled(){return new FarmMarketPolicy(false,50,2,256,16);}
}
