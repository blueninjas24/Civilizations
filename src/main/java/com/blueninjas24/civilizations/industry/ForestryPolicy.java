package com.blueninjas24.civilizations.industry;

/** Prototype renewable timber terms; live configuration and resource ownership are separate integration steps. */
public record ForestryPolicy(boolean enabled, int minimumTrunkCoordinates, long stockPerTrunk,
                             long maximumStock, int initialStockPercent, int recoveryBasisPoints,
                             long unitsPerWorker, long siteWorkCapacity) {
    public ForestryPolicy {
        if (minimumTrunkCoordinates < 1 || minimumTrunkCoordinates > ForestObservation.MAX_TRUNK_COORDINATES
                || stockPerTrunk < 1 || stockPerTrunk > 1_000_000
                || maximumStock < Math.multiplyExact(minimumTrunkCoordinates, stockPerTrunk)
                || maximumStock > 1_000_000_000L || initialStockPercent < 0 || initialStockPercent > 100
                || recoveryBasisPoints < 0 || recoveryBasisPoints > 10_000
                || unitsPerWorker < 1 || unitsPerWorker > 1_000_000
                || siteWorkCapacity < 1 || siteWorkCapacity > 1_000_000)
            throw new IllegalArgumentException("Invalid forestry resource terms");
    }

    /** Positive, complete environmental evidence only; never call this to refill an existing saved resource. */
    public ForestStock opening(ForestObservation evidence) {
        if (!enabled || !evidence.complete() || evidence.trunks().size() < minimumTrunkCoordinates)
            throw new IllegalArgumentException("Forestry needs enabled complete canopy-backed trunk evidence");
        long capacity = Math.min(maximumStock, Math.multiplyExact(evidence.trunks().size(), stockPerTrunk));
        return new ForestStock(capacity, Math.multiplyExact(capacity, initialStockPercent) / 100, 0);
    }

    public static ForestryPolicy prototype() {
        return new ForestryPolicy(true, 6, 16, 8192, 100, 100, 4, 16);
    }

    /** Existing installations without forestry resource terms remain opted out. */
    public static ForestryPolicy disabled() {
        var p = prototype();
        return new ForestryPolicy(false, p.minimumTrunkCoordinates(), p.stockPerTrunk(), p.maximumStock(),
                p.initialStockPercent(), p.recoveryBasisPoints(), p.unitsPerWorker(), p.siteWorkCapacity());
    }
}
