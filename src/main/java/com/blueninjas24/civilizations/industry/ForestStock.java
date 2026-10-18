package com.blueninjas24.civilizations.industry;

import java.util.Objects;

/** Pure renewable timber calculation. A SQL owner must supply each earned period once; this is not a live business. */
public record ForestStock(long capacity, long available, int recoveryRemainder) {
    public ForestStock {
        if (capacity < 0 || capacity > 1_000_000_000L || available < 0 || available > capacity
                || recoveryRemainder < 0 || recoveryRemainder >= 10_000
                || available == capacity && recoveryRemainder != 0)
            throw new IllegalArgumentException("Invalid standing timber stock");
    }

    public record Period(ForestStock stock, long recovered, long harvested, long storageSpace,
                         String explanation) {}
    public record HabitatChange(ForestStock before, ForestStock after, long lost) {}

    /** No wall-clock catch-up: exactly one earned active-world period, bounded by actual demand, work and storage. */
    public Period advance(ForestryPolicy policy, boolean activeWorld, long requestedUnits,
                          int qualifiedWorkers, long freeStorage, long timberSpacePerUnit) {
        Objects.requireNonNull(policy);
        if (requestedUnits < 0 || qualifiedWorkers < 0 || freeStorage < 0 || timberSpacePerUnit < 1)
            throw new IllegalArgumentException("Invalid timber work limits");
        if (!policy.enabled() || !activeWorld || capacity == 0)
            return new Period(this, 0, 0, 0, "Forestry paused: disabled, absent world or no suitable habitat");
        long fraction = Math.addExact(Math.multiplyExact(capacity, policy.recoveryBasisPoints()), recoveryRemainder);
        long recovered = Math.min(capacity - available, fraction / 10_000);
        long standing = available + recovered;
        int remainder = standing == capacity ? 0 : (int) (fraction % 10_000);
        long labor = Math.multiplyExact((long) qualifiedWorkers, policy.unitsPerWorker());
        long harvested = Math.min(standing, Math.min(requestedUnits,
                Math.min(labor, Math.min(policy.siteWorkCapacity(), freeStorage / timberSpacePerUnit))));
        var closing = new ForestStock(capacity, standing - harvested, remainder);
        return new Period(closing, recovered, harvested, Math.multiplyExact(harvested, timberSpacePerUnit),
                "Timber " + available + "+" + recovered + "-" + harvested + "=" + closing.available()
                        + "/" + capacity + "; demand " + requestedUnits + ", qualified work " + labor
                        + ", storage " + freeStorage / timberSpacePerUnit);
    }

    /** Explicit authoritative support transition only; a positive-only forest scan cannot authorize this call. */
    public HabitatChange habitat(long originalCapacity, int originalSupports, int suitableSupports) {
        if (originalCapacity < 1 || originalCapacity > 1_000_000_000L || capacity > originalCapacity
                || originalSupports < 1 || originalSupports > ForestObservation.MAX_TRUNK_COORDINATES
                || suitableSupports < 0 || suitableSupports > originalSupports)
            throw new IllegalArgumentException("Invalid confirmed forestry habitat");
        long limit = Math.multiplyExact(originalCapacity, suitableSupports) / originalSupports;
        long retained = Math.min(available, limit);
        var closing = new ForestStock(limit, retained, retained == limit ? 0 : recoveryRemainder);
        return new HabitatChange(this, closing, available - retained);
    }
}
