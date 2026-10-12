package com.blueninjas24.civilizations.industry;

import java.util.Objects;

/** Immutable renewable habitat stock. Capacity survives retuning; earned periods are supplied by the owner clock. */
public record FishStock(long capacity, long available, int recoveryRemainder) {
    public FishStock {
        if (capacity < 1 || capacity > 1_000_000_000L || available < 0 || available > capacity
                || recoveryRemainder < 0 || recoveryRemainder >= 10_000
                || (available == capacity && recoveryRemainder != 0))
            throw new IllegalArgumentException("Invalid fish stock");
    }

    public record Period(FishStock stock, long recovered, long caught, long storageSpace, String explanation) {}

    /** One earned online/present-world period; recovery precedes catch. No elapsed wall-clock catch-up. */
    public Period advance(FishingPolicy policy, boolean activeWorld, long requestedCatch, int qualifiedWorkers,
                          long freeStorage, long foodSpacePerUnit) {
        Objects.requireNonNull(policy);
        if (requestedCatch < 0 || qualifiedWorkers < 0 || freeStorage < 0 || foodSpacePerUnit < 1)
            throw new IllegalArgumentException("Invalid fishing work limits");
        if (!policy.enabled() || !activeWorld)
            return new Period(this, 0, 0, 0, "Fishing paused: " + (!activeWorld ? "world absent/no earned period" : "resource policy disabled"));

        long fractionalRecovery = Math.addExact(Math.multiplyExact(capacity, policy.recoveryBasisPoints()), recoveryRemainder);
        long recovered = Math.min(capacity - available, fractionalRecovery / 10_000);
        long beforeCatch = available + recovered;
        // A full habitat cannot bank renewal for later catches.
        int remainder = beforeCatch == capacity ? 0 : (int) (fractionalRecovery % 10_000);
        long labor = Math.multiplyExact((long) qualifiedWorkers, policy.catchPerWorker());
        long storageLimit = freeStorage / foodSpacePerUnit;
        long caught = Math.min(beforeCatch, Math.min(requestedCatch,
                Math.min(labor, Math.min(policy.siteCatchCapacity(), storageLimit))));
        var after = new FishStock(capacity, beforeCatch - caught, remainder);
        return new Period(after, recovered, caught, Math.multiplyExact(caught, foodSpacePerUnit),
                "Fishing: " + caught + " food units; stock " + available + "+" + recovered + "-" + caught
                        + "=" + after.available() + "/" + capacity + "; requested " + requestedCatch
                        + ", qualified labor " + labor + ", site " + policy.siteCatchCapacity() + ", storage " + storageLimit);
    }
}
