package com.blueninjas24.civilizations.industry;

import java.util.Objects;

/** Bounded read-only tree observation settings; they do not certify ownership or authorize adoption. */
public record ForestryObserverPolicy(boolean enabled, int horizontalRadiusChunks, int verticalRadius,
                                      int cellsPerTick, int intervalTicks, ForestScan.Geometry geometry) {
    public ForestryObserverPolicy {
        Objects.requireNonNull(geometry);
        if (horizontalRadiusChunks < 0 || horizontalRadiusChunks > 2
                || verticalRadius < 4 || verticalRadius > 64
                || cellsPerTick < 1 || cellsPerTick > 4096 || intervalTicks < 20)
            throw new IllegalArgumentException("Invalid forestry observation budgets");
    }

    public static ForestryObserverPolicy prototype() {
        return new ForestryObserverPolicy(true, 1, 24, 512, 200, ForestScan.Geometry.prototype());
    }

    public static ForestryObserverPolicy disabled() {
        var p = prototype();
        return new ForestryObserverPolicy(false, p.horizontalRadiusChunks(), p.verticalRadius(),
                p.cellsPerTick(), p.intervalTicks(), p.geometry());
    }
}
