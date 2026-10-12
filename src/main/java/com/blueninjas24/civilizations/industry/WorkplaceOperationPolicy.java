package com.blueninjas24.civilizations.industry;

/** Only explicitly marker-founded firms participate; old firms acquire no new requirement. */
public record WorkplaceOperationPolicy(boolean pauseOnMissing) {
    public static WorkplaceOperationPolicy prototype(){return new WorkplaceOperationPolicy(true);}
}
