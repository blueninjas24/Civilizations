package com.blueninjas24.civilizations.industry;

/** Eligibility forecast only; never creates or reserves food orders. */
public record BakeryMarketPolicy(boolean enabled,int paidOrderLookbackPeriods,int maximumShareBasisPoints,long maximumUnitsPerPeriod) {
    public BakeryMarketPolicy {
        if(paidOrderLookbackPeriods<1||paidOrderLookbackPeriods>100||maximumShareBasisPoints<1||maximumShareBasisPoints>10000
                ||maximumUnitsPerPeriod<1||maximumUnitsPerPeriod>1_000_000)throw new IllegalArgumentException("Invalid bakery market limits");
    }
    public long share(long units) {
        if(units<0)throw new IllegalArgumentException("Negative food market");
        // Quotient/remainder avoids overflowing even for large saved order totals.
        return Math.min(maximumUnitsPerPeriod,Math.addExact(Math.multiplyExact(units/10000,maximumShareBasisPoints),units%10000*maximumShareBasisPoints/10000));
    }
    public static BakeryMarketPolicy prototype(){return new BakeryMarketPolicy(true,12,10000,32);}
    public static BakeryMarketPolicy disabled(){return new BakeryMarketPolicy(false,12,10000,32);}
}
