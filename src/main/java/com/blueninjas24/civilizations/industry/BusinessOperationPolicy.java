package com.blueninjas24.civilizations.industry;

/** Opt-in prototype: death does not halt a business with real qualified staff awaiting succession/resale. */
public record BusinessOperationPolicy(boolean enabled,boolean continueStaffedAfterOwnerDeath) {
    public static BusinessOperationPolicy prototype(){return new BusinessOperationPolicy(true,true);}
    public static BusinessOperationPolicy disabled(){return new BusinessOperationPolicy(false,false);}
}
