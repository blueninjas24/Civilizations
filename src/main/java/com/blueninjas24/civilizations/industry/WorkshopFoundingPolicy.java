package com.blueninjas24.civilizations.industry;

/** Editable prototype qualification; an opportunity or a table never grants experience. */
public record WorkshopFoundingPolicy(boolean enabled,int foundersPerReview,long minimumAgeSteps,
        long founderExperience,long workerExperience,int workerCapacity) {
    public WorkshopFoundingPolicy {
        if(foundersPerReview<1||foundersPerReview>512||minimumAgeSteps<1||minimumAgeSteps>1_000_000
                ||founderExperience<1||founderExperience>1_000_000_000||workerExperience<1||workerExperience>founderExperience
                ||workerCapacity<1||workerCapacity>32)throw new IllegalArgumentException("Invalid workshop founding qualification bounds");
    }
    public static WorkshopFoundingPolicy prototype(){return new WorkshopFoundingPolicy(true,64,216,10,1,2);}
    public static WorkshopFoundingPolicy disabled(){return new WorkshopFoundingPolicy(false,64,216,10,1,2);}
}
