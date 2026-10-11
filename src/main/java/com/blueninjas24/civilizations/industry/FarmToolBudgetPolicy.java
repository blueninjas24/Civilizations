package com.blueninjas24.civilizations.industry;

/** Public recurring equipment spending; independent of private capital and startup grants. */
public record FarmToolBudgetPolicy(boolean enabled, long toolHundredths, long perPeriodLimit,
                                   long minimumTreasury, int foodReservePeriods, long hundredthsPerFoodUnit) {
    public FarmToolBudgetPolicy {
        if(toolHundredths<1 || toolHundredths>100_000_000L || perPeriodLimit<1 || perPeriodLimit>100_000_000_000L
                || minimumTreasury<0 || minimumTreasury>100_000_000_000L || foodReservePeriods<1
                || foodReservePeriods>100 || hundredthsPerFoodUnit<1 || hundredthsPerFoodUnit>100_000_000L)
            throw new IllegalArgumentException("Invalid public farm tool budget");
    }
    public record Review(boolean eligible,long protectedHundredths,String reason) {}
    public Review review(long cash,long foodDemand,long spent) {
        if(cash<0 || foodDemand<0 || spent<0)throw new IllegalArgumentException("Invalid farm budget evidence");
        if(!enabled)return new Review(false,0,"Public farm tool purchases disabled");
        long keep;
        try {keep=Math.max(minimumTreasury,Math.multiplyExact(Math.multiplyExact(foodDemand,foodReservePeriods),hundredthsPerFoodUnit));}
        catch(ArithmeticException overflow){return new Review(false,Long.MAX_VALUE,"Food reserve exceeds supported money range");}
        if(spent>perPeriodLimit || toolHundredths>perPeriodLimit-spent)return new Review(false,keep,"Village farm upkeep period budget exhausted");
        if(cash<keep || toolHundredths>cash-keep)return new Review(false,keep,"Farm upkeep would spend protected food funds");
        return new Review(true,keep,"Actual village funds cover one needed farm toolkit after food protection");
    }
    public static FarmToolBudgetPolicy prototype(){return new FarmToolBudgetPolicy(true,4000,4000,2500,2,200);}
    public static FarmToolBudgetPolicy disabled(){var p=prototype();return new FarmToolBudgetPolicy(false,p.toolHundredths,p.perPeriodLimit,p.minimumTreasury,p.foodReservePeriods,p.hundredthsPerFoodUnit);}
}
