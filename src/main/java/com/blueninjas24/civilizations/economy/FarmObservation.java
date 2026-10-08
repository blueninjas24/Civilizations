package com.blueninjas24.civilizations.economy;

import java.util.UUID;
import java.util.Objects;

/** Read-only planted-field evidence; not block ownership or permission to harvest/edit player work. */
public record FarmObservation(UUID settlement, UUID world, long observedMillis, boolean complete, long plantedCrops) {
    public FarmObservation {
        Objects.requireNonNull(settlement); Objects.requireNonNull(world);
        if (observedMillis < 0 || plantedCrops < 0 || plantedCrops > 100000) throw new IllegalArgumentException("Invalid farm evidence");
    }
    public record Policy(int radius, int verticalRadius, int cropsPerWorker) {
        public Policy {
            if (radius < 8 || radius > 64 || verticalRadius < 1 || verticalRadius > 32 || cropsPerWorker < 1 || cropsPerWorker > 256)
                throw new IllegalArgumentException("Invalid farm observation policy");
        }
        public static Policy prototype() { return new Policy(32, 16, 16); }
    }
}
