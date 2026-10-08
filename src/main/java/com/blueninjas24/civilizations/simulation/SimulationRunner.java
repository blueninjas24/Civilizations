package com.blueninjas24.civilizations.simulation;

import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;

/** Stateless, seeded stepping with an explicit work budget for each call. */
public final class SimulationRunner {
    private SimulationRunner() {}

    @FunctionalInterface
    public interface Step<S> {
        /**
         * Return new immutable state; do not mutate the input, retain the random generator,
         * access world state or use wall-clock/global randomness. Iterate collections stably.
         * Steps must be bounded in cost; the runner limits step count, not elapsed CPU time.
         */
        S apply(S state, Context context);
    }

    /** Step numbers start at one; randomness is derived independently for each logical step. */
    public record Context(long stepNumber, RandomGenerator random) {}

    public record Batch<S>(SimulationCheckpoint<S> checkpoint, int processedSteps, long remainingSteps) {
        public boolean complete() {
            return remainingSteps == 0;
        }
    }

    public static <S> Batch<S> advance(SimulationCheckpoint<S> checkpoint, long targetSteps,
                                     int maximumSteps, Step<S> step) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        Objects.requireNonNull(step, "step");
        if (targetSteps < checkpoint.completedSteps()) {
            throw new IllegalArgumentException("Target cannot precede the checkpoint");
        }
        if (maximumSteps <= 0) {
            throw new IllegalArgumentException("Step budget must be positive");
        }
        int count = (int) Math.min(targetSteps - checkpoint.completedSteps(), maximumSteps);
        S state = checkpoint.state();
        long completed = checkpoint.completedSteps();
        for (int index = 0; index < count; index++) {
            long stepNumber = ++completed;
            // Java Random's specified algorithm plus fixed mixing gives repeatable per-step draws.
            // No hidden PRNG cursor needs to survive between batches or reconstructed checkpoints.
            long stepSeed = mix(checkpoint.seed() + 0x9E3779B97F4A7C15L * stepNumber);
            state = Objects.requireNonNull(step.apply(state,
                    new Context(stepNumber, new Random(stepSeed))), "Step returned null state");
        }
        return new Batch<>(new SimulationCheckpoint<>(checkpoint.seed(), completed, state),
                count, targetSteps - completed);
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
