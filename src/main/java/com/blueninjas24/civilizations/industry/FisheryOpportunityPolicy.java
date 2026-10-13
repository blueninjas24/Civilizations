package com.blueninjas24.civilizations.industry;

/** Editable bounded first-fishery investment review; no automatic founding or property rights. */
public record FisheryOpportunityPolicy(boolean enabled,int sitesPerReview,int maximumColumns,long evidenceLifetimeMillis,
        int horizontalRange,int verticalRange,long startupHundredths,long capitalHundredths,long capacity) {
    public FisheryOpportunityPolicy {
        if(sitesPerReview<1||sitesPerReview>64||maximumColumns<1||maximumColumns>256
                ||evidenceLifetimeMillis<1000||evidenceLifetimeMillis>3_600_000
                ||horizontalRange<1||horizontalRange>256||verticalRange<1||verticalRange>128
                ||startupHundredths<1||startupHundredths>100_000_000_000L
                ||capitalHundredths<1||capitalHundredths>100_000_000_000L||capacity<1||capacity>1_000_000_000)
            throw new IllegalArgumentException("Invalid fishery opportunity terms");
    }
    public static FisheryOpportunityPolicy prototype(){return new FisheryOpportunityPolicy(true,8,64,60000,96,48,500,1000,256);}
    public static FisheryOpportunityPolicy disabled(){var p=prototype();return new FisheryOpportunityPolicy(false,p.sitesPerReview,p.maximumColumns,p.evidenceLifetimeMillis,p.horizontalRange,p.verticalRange,p.startupHundredths,p.capitalHundredths,p.capacity);}
}
