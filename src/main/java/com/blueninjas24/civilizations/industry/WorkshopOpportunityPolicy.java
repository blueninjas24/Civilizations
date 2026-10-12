package com.blueninjas24.civilizations.industry;

/** Bounded prototype investment review; these terms do not authorize founding. */
public record WorkshopOpportunityPolicy(boolean enabled,int markersPerReview,long evidenceLifetimeMillis,
        int horizontalRange,int verticalRange,long startupHundredths,long capitalHundredths,long capacity) {
    public WorkshopOpportunityPolicy {
        if(markersPerReview<1||markersPerReview>64||evidenceLifetimeMillis<1000||evidenceLifetimeMillis>3_600_000
                ||horizontalRange<1||horizontalRange>256||verticalRange<1||verticalRange>128
                ||startupHundredths<0||startupHundredths>1_000_000_000L||capitalHundredths<1||capitalHundredths>1_000_000_000L
                ||capacity<1||capacity>1_000_000_000L)throw new IllegalArgumentException("Invalid workshop opportunity bounds");
    }
    public static WorkshopOpportunityPolicy prototype(){return new WorkshopOpportunityPolicy(true,8,60000,96,48,500,4000,1024);}
    public static WorkshopOpportunityPolicy disabled(){return new WorkshopOpportunityPolicy(false,8,60000,96,48,500,4000,1024);}
}
