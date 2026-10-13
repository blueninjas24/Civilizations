package com.blueninjas24.civilizations.industry;

/** Editable qualification for first fisheries; water or profession never grants fishing experience. */
public record FisheryFoundingPolicy(boolean enabled,int foundersPerReview,long minimumAgeSteps,
        long founderExperience,long workerExperience,int workerCapacity) {
    public FisheryFoundingPolicy {
        if(foundersPerReview<1||foundersPerReview>512||minimumAgeSteps<1||minimumAgeSteps>1_000_000
                ||founderExperience<1||founderExperience>1_000_000_000||workerExperience<1||workerExperience>founderExperience
                ||workerCapacity<1||workerCapacity>32)throw new IllegalArgumentException("Invalid fishery founding qualification bounds");
    }
    public static FisheryFoundingPolicy prototype(){return new FisheryFoundingPolicy(true,64,216,10,1,2);}
    public static FisheryFoundingPolicy disabled(){return new FisheryFoundingPolicy(false,64,216,10,1,2);}
}
