package com.blueninjas24.civilizations.industry;

import java.math.BigInteger;

/** Cash-surplus contributions, not an inferred profit calculation or household tax. */
public record UpkeepContributionPolicy(boolean enabled, long operatingReserve, int spareCashPercent,
                                       long perBusinessPeriodLimit, long perVillagePeriodLimit) {
    public UpkeepContributionPolicy {
        if(operatingReserve<0||operatingReserve>100_000_000_000L||spareCashPercent<1||spareCashPercent>100
                ||perBusinessPeriodLimit<1||perBusinessPeriodLimit>100_000_000_000L
                ||perVillagePeriodLimit<1||perVillagePeriodLimit>100_000_000_000L)
            throw new IllegalArgumentException("Invalid business upkeep contribution limits");
    }
    public long available(long cash,long openingCapital,long earnedSales,long contributed,
                          long businessPeriodSpent,long villagePeriodSpent) {
        if(cash<0||openingCapital<0||earnedSales<0||contributed<0||businessPeriodSpent<0||villagePeriodSpent<0)
            throw new IllegalArgumentException("Negative contribution evidence");
        if(!enabled)return 0;
        long keep;
        try{keep=Math.addExact(openingCapital,operatingReserve);}catch(ArithmeticException overflow){return 0;}
        long spare=Math.max(0,cash-keep);
        long fraction=BigInteger.valueOf(spare).multiply(BigInteger.valueOf(spareCashPercent)).divide(BigInteger.valueOf(100)).longValueExact();
        return Math.min(Math.min(fraction,Math.max(0,earnedSales-contributed)),
                Math.min(Math.max(0,perBusinessPeriodLimit-businessPeriodSpent),Math.max(0,perVillagePeriodLimit-villagePeriodSpent)));
    }
    public static UpkeepContributionPolicy prototype(){return new UpkeepContributionPolicy(true,8000,50,4000,4000);}
    public static UpkeepContributionPolicy disabled(){return new UpkeepContributionPolicy(false,8000,50,4000,4000);}
}
