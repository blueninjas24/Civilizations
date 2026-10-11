package com.blueninjas24.civilizations.industry;

/** Public farm work shares the productive-period tool math, without private food sales. */
public record FarmToolUpkeepPolicy(boolean enabled,int startupGracePeriods,int workPeriodsPerTool,
                                   int missingToolsOutputPercent,int suppliersPerReview) {
    public FarmToolUpkeepPolicy {
        if(startupGracePeriods<0||startupGracePeriods>100000||workPeriodsPerTool<1||workPeriodsPerTool>100000
                ||missingToolsOutputPercent<1||missingToolsOutputPercent>100||suppliersPerReview<1||suppliersPerReview>64)
            throw new IllegalArgumentException("Invalid public farm tool upkeep settings");
    }
    public ToolUpkeepPolicy workPolicy(long toolPrice){return new ToolUpkeepPolicy(enabled,startupGracePeriods,workPeriodsPerTool,missingToolsOutputPercent,toolPrice,java.util.Set.of());}
    public static FarmToolUpkeepPolicy prototype(){return new FarmToolUpkeepPolicy(true,3,8,50,8);}
    public static FarmToolUpkeepPolicy disabled(){var p=prototype();return new FarmToolUpkeepPolicy(false,p.startupGracePeriods,p.workPeriodsPerTool,p.missingToolsOutputPercent,p.suppliersPerReview);}
}
