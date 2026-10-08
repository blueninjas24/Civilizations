package com.blueninjas24.civilizations.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SimulationRunnerTest {
    private static final SimulationRunner.Step<Long> CHECKSUM = (state, context) ->
            Long.rotateLeft(state, 7) ^ context.random().nextLong() ^ context.stepNumber();

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 100, 10000})
    void batchingDoesNotChangeSeededOutcome(int budget) {
        var start = new SimulationCheckpoint<>(1234, 0, 0L);
        var expected = SimulationRunner.advance(start, 10000, 10000, CHECKSUM).checkpoint();
        var checkpoint = start;
        while (checkpoint.completedSteps() < 10000) {
            var batch = SimulationRunner.advance(checkpoint, 10000, budget, CHECKSUM);
            assertTrue(batch.processedSteps() <= budget);
            checkpoint = batch.checkpoint();
        }
        assertEquals(expected, checkpoint);
    }

    @Test
    void boundedCatchUpLeavesExplicitRemainingWork() {
        AtomicInteger calls = new AtomicInteger();
        var batch = SimulationRunner.advance(new SimulationCheckpoint<>(5, 12, 0),
                1000, 3, (state, context) -> {
                    calls.incrementAndGet();
                    return state + 1;
                });
        assertEquals(3, calls.get());
        assertEquals(3, batch.processedSteps());
        assertEquals(15, batch.checkpoint().completedSteps());
        assertEquals(985, batch.remainingSteps());
        assertFalse(batch.complete());
    }

    @Test
    void reconstructedCheckpointResumesSameRandomSequence() {
        var initial = new SimulationCheckpoint<>(-9, 0, 0L);
        var partial = SimulationRunner.advance(initial, 100, 37, CHECKSUM).checkpoint();
        var restored = new SimulationCheckpoint<>(partial.seed(), partial.completedSteps(), partial.state());
        var resumed = SimulationRunner.advance(restored, 100, 100, CHECKSUM).checkpoint();
        assertEquals(SimulationRunner.advance(initial, 100, 100, CHECKSUM).checkpoint(), resumed);
    }

    @Test
    void stepsAreOrderedAndFinishedTargetsDoNotRunAgain() {
        List<Long> visited = new ArrayList<>();
        var batch = SimulationRunner.advance(new SimulationCheckpoint<>(0, 7, 0), 10, 20,
                (state, context) -> {
                    visited.add(context.stepNumber());
                    return state + 1;
                });
        assertEquals(List.of(8L, 9L, 10L), visited);
        var repeated = SimulationRunner.advance(batch.checkpoint(), 10, 20,
                (state, context) -> fail("Already completed steps must not run"));
        assertTrue(repeated.complete());
        assertEquals(0, repeated.processedSteps());
        assertEquals(batch.checkpoint(), repeated.checkpoint());
    }

    @Test
    void preservesEstablishedReplayFixture() {
        // Changes to the random/step convention must explicitly account for replay compatibility.
        assertEquals(-6_876_560_382_178_007_112L, SimulationHarness.run(42, 10000, 17).state());
    }

    @Test
    void distinctSeedsGiveDifferentProbeOutcomes() {
        assertNotEquals(SimulationHarness.run(1, 1000, 17).state(),
                SimulationHarness.run(2, 1000, 17).state());
    }

    @Test
    void failedPureStepLeavesInputCheckpointReplayable() {
        var original = new SimulationCheckpoint<>(99, 0, 0L);
        assertThrows(IllegalStateException.class, () -> SimulationRunner.advance(original, 10, 10,
                (state, context) -> {
                    if (context.stepNumber() == 4) {
                        throw new IllegalStateException("Synthetic failure");
                    }
                    return CHECKSUM.apply(state, context);
                }));
        assertEquals(0, original.completedSteps());
        assertEquals(0L, original.state());
        assertEquals(SimulationHarness.run(99, 10, 10),
                SimulationRunner.advance(original, 10, 10, CHECKSUM).checkpoint());
    }

    @Test
    void handlesFinalStepAtLongMaximumWithoutWrapping() {
        var batch = SimulationRunner.advance(new SimulationCheckpoint<>(0, Long.MAX_VALUE - 1, 0),
                Long.MAX_VALUE, 10, (state, context) -> {
                    assertEquals(Long.MAX_VALUE, context.stepNumber());
                    return state + 1;
                });
        assertEquals(Long.MAX_VALUE, batch.checkpoint().completedSteps());
        assertEquals(1, batch.processedSteps());
        assertTrue(batch.complete());
    }

    @Test
    void refusesTimeReversalAndInvalidBudgetsBeforeRunningAnyStep() {
        var checkpoint = new SimulationCheckpoint<>(0, 10, 0L);
        SimulationRunner.Step<Long> neverRun = (state, context) -> fail("Invalid inputs must not run");
        assertThrows(IllegalArgumentException.class,
                () -> SimulationRunner.advance(checkpoint, 9, 1, neverRun));
        assertThrows(IllegalArgumentException.class,
                () -> SimulationRunner.advance(checkpoint, 11, 0, neverRun));
        assertThrows(IllegalArgumentException.class,
                () -> SimulationRunner.advance(checkpoint, 11, -1, neverRun));
        assertThrows(IllegalArgumentException.class, () -> new SimulationCheckpoint<>(0, -1, 0));
        assertThrows(NullPointerException.class, () -> new SimulationCheckpoint<>(0, 0, null));
    }

    @Test
    void rejectsNullStepOutputRatherThanReturningInvalidCheckpoint() {
        assertThrows(NullPointerException.class, () -> SimulationRunner.advance(
                new SimulationCheckpoint<>(0, 0, 0), 1, 1, (state, context) -> null));
    }

    @Test
    void acceleratesOneMillionSyntheticStepsWithoutWorldOrClock() {
        var result = SimulationHarness.run(42, 1_000_000, 1000);
        assertEquals(1_000_000, result.completedSteps());
        assertEquals(result, SimulationHarness.run(42, 1_000_000, 997));
    }
}
