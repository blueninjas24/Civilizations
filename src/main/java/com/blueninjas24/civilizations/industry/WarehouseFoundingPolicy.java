package com.blueninjas24.civilizations.industry;

/** Editable first-warehouse qualification. Observed barrels never grant commerce skill. */
public record WarehouseFoundingPolicy(boolean enabled,int foundersPerReview,long minimumAgeSteps,
        long founderExperience,long workerExperience,int workerCapacity) {
    public WarehouseFoundingPolicy {
        if(foundersPerReview<1||foundersPerReview>512||minimumAgeSteps<1||minimumAgeSteps>1_000_000
                ||founderExperience<1||founderExperience>1_000_000_000||workerExperience<1||workerExperience>founderExperience
                ||workerCapacity<1||workerCapacity>32)throw new IllegalArgumentException("Invalid warehouse founding qualification bounds");
    }
    public static WarehouseFoundingPolicy prototype(){return new WarehouseFoundingPolicy(true,64,216,10,1,2);}
    public static WarehouseFoundingPolicy disabled(){return new WarehouseFoundingPolicy(false,64,216,10,1,2);}
}
