package com.blueninjas24.civilizations.industry;

/** Existing herd work only; this does not authorize animal adoption or founding. */
public record LivestockRuntimePolicy(boolean enabled,long workPerWorker,long siteWorkLimit) {
    public LivestockRuntimePolicy {
        if(workPerWorker<1||workPerWorker>1_000_000||siteWorkLimit<1||siteWorkLimit>1_000_000)
            throw new IllegalArgumentException("Livestock work limits must be1..1000000");
    }
    public static LivestockRuntimePolicy prototype(){return new LivestockRuntimePolicy(true,10,40);}
    public static LivestockRuntimePolicy disabled(){return new LivestockRuntimePolicy(false,10,40);}
}
