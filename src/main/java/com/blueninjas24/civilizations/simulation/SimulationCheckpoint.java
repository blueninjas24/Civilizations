package com.blueninjas24.civilizations.simulation;

import java.util.Objects;

/** Completed logical steps, seed and immutable domain state; no Minecraft time conversion. */
public record SimulationCheckpoint<S>(long seed, long completedSteps, S state) {
    public SimulationCheckpoint {
        if (completedSteps < 0) {
            throw new IllegalArgumentException("Completed steps must be nonnegative");
        }
        Objects.requireNonNull(state, "state");
    }
}
