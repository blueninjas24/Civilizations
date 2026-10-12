package com.blueninjas24.civilizations.industry;

/** Editable bounded first mineral-chain warehouse review; no permission to found or build. */
public record WarehouseOpportunityPolicy(boolean enabled,int markersPerReview,long evidenceLifetimeMillis,
        int horizontalRange,int verticalRange,long startupHundredths,long capitalHundredths,long capacity) {
    public WarehouseOpportunityPolicy {
        if(markersPerReview<1||markersPerReview>64||evidenceLifetimeMillis<1000||evidenceLifetimeMillis>3_600_000
                ||horizontalRange<1||horizontalRange>256||verticalRange<1||verticalRange>128
                ||startupHundredths<0||startupHundredths>1_000_000_000L||capitalHundredths<1||capitalHundredths>1_000_000_000L
                ||capacity<1||capacity>1_000_000_000L)throw new IllegalArgumentException("Invalid warehouse opportunity bounds");
    }
    public static WarehouseOpportunityPolicy prototype(){return new WarehouseOpportunityPolicy(true,8,60000,96,48,500,4000,1024);}
    public static WarehouseOpportunityPolicy disabled(){return new WarehouseOpportunityPolicy(false,8,60000,96,48,500,4000,1024);}
}
