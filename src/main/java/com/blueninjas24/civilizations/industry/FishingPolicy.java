package com.blueninjas24.civilizations.industry;

/** Editable prototype terms for a new habitat-backed fishery; not a live observer. */
public record FishingPolicy(boolean enabled, int minimumWaterColumns, long stockPerWaterColumn,
                            long maximumStock, int initialStockPercent, int recoveryBasisPoints,
                            long catchPerWorker, long siteCatchCapacity) {
    public FishingPolicy {
        if (minimumWaterColumns < 1 || minimumWaterColumns > 4096
                || stockPerWaterColumn < 1 || stockPerWaterColumn > 1_000_000
                || maximumStock < 1 || maximumStock > 1_000_000_000L
                || maximumStock < Math.multiplyExact(minimumWaterColumns, stockPerWaterColumn)
                || initialStockPercent < 0 || initialStockPercent > 100
                || recoveryBasisPoints < 0 || recoveryBasisPoints > 10_000
                || catchPerWorker < 1 || catchPerWorker > 1_000_000
                || siteCatchCapacity < 1 || siteCatchCapacity > 1_000_000)
            throw new IllegalArgumentException("Invalid fishing resource terms");
    }

    /** Caller must supply complete, suitable, distinct open-water column evidence. */
    public FishStock opening(int suitableWaterColumns, boolean complete) {
        if (!enabled || !complete || suitableWaterColumns < minimumWaterColumns || suitableWaterColumns > 4096)
            throw new IllegalArgumentException("Fishery requires enabled, complete suitable water evidence");
        long capacity = Math.min(maximumStock, Math.multiplyExact(suitableWaterColumns, stockPerWaterColumn));
        return new FishStock(capacity, Math.multiplyExact(capacity, initialStockPercent) / 100, 0);
    }

    public static FishingPolicy prototype() {
        return new FishingPolicy(true, 16, 8, 4096, 75, 200, 4, 16);
    }

    public static FishingPolicy disabled() {
        var p = prototype();
        return new FishingPolicy(false, p.minimumWaterColumns(), p.stockPerWaterColumn(), p.maximumStock(),
                p.initialStockPercent(), p.recoveryBasisPoints(), p.catchPerWorker(), p.siteCatchCapacity());
    }
}
