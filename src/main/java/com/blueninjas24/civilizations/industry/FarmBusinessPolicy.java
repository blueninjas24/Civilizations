package com.blueninjas24.civilizations.industry;

/** Approved editable economy prototype; one initial field enterprise per village, not permanent zoning. */
public record FarmBusinessPolicy(boolean enabled, int workerCapacity, long startupHundredths,
                                 long minimumAgeMonths, long minimumFarmingExperience) {
    public FarmBusinessPolicy {
        if(workerCapacity<1 || workerCapacity>64 || startupHundredths<0 || startupHundredths>1_000_000_000
                || minimumAgeMonths<0 || minimumAgeMonths>10000 || minimumFarmingExperience<0 || minimumFarmingExperience>1_000_000)
            throw new IllegalArgumentException("Invalid farm business settings");
    }
    public static FarmBusinessPolicy prototype(){return new FarmBusinessPolicy(true,4,2500,216,0);}
    public static FarmBusinessPolicy disabled(){return new FarmBusinessPolicy(false,4,2500,216,0);}
}
