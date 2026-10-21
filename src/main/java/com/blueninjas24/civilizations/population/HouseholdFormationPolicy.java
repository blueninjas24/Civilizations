package com.blueninjas24.civilizations.population;

/** Administrative pacing, not permission to split existing households or invent relatives. */
public record HouseholdFormationPolicy(boolean enabled,int maximumPerMonth,int maximumCandidates) {
    public HouseholdFormationPolicy {
        if(maximumPerMonth<1||maximumPerMonth>64||maximumCandidates<1||maximumCandidates>1024)
            throw new IllegalArgumentException("Invalid household formation limits");
    }
    public static HouseholdFormationPolicy prototype(){return new HouseholdFormationPolicy(true,2,128);}
    public static HouseholdFormationPolicy disabled(){return new HouseholdFormationPolicy(false,2,128);}
}
