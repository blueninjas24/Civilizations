package com.blueninjas24.civilizations.simulation;

/** Synthetic reproducibility probe only; does not simulate residents or access a database/world. */
public final class SimulationHarness {
    private SimulationHarness() {}

    static SimulationCheckpoint<Long> run(long seed, long targetSteps, int batchSize) {
        SimulationCheckpoint<Long> checkpoint = new SimulationCheckpoint<>(seed, 0, 0L);
        SimulationRunner.Batch<Long> batch;
        do {
            batch = SimulationRunner.advance(checkpoint, targetSteps, batchSize,
                    (checksum, context) -> Long.rotateLeft(checksum, 7)
                            ^ context.random().nextLong() ^ context.stepNumber());
            checkpoint = batch.checkpoint();
        } while (!batch.complete());
        return checkpoint;
    }

    public static void main(String[] arguments) {
        if (arguments.length != 3) {
            throw new IllegalArgumentException("Usage: SimulationHarness <seed> <steps> <batch-size>");
        }
        SimulationCheckpoint<Long> result = run(Long.parseLong(arguments[0]),
                Long.parseLong(arguments[1]), Integer.parseInt(arguments[2]));
        System.out.println("mode=synthetic-probe seed=" + result.seed()
                + " completedSteps=" + result.completedSteps() + " checksum=" + result.state());
    }
}
