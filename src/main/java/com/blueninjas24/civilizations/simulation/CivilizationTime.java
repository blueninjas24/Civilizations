package com.blueninjas24.civilizations.simulation;

/** Pure conversion from elapsed real milliseconds to queued civilization months. */
public final class CivilizationTime {
    private CivilizationTime() {}

    public record Anchor(long lastObservedMillis, long remainderMillis, long targetSteps) {
        public Anchor {
            if (lastObservedMillis < 0 || remainderMillis < 0 || targetSteps < 0)
                throw new IllegalArgumentException("Clock values must be nonnegative");
        }
    }
    public record Accrual(Anchor anchor, long addedSteps, long discardedMillis) {}
    public record Date(long year, int month) {}

    /** Month 1 of year 0 is the initial state, without inventing an historical epoch. */
    public static Date date(long completedSteps, int stepsPerYear) {
        if (completedSteps < 0 || stepsPerYear < 1) throw new IllegalArgumentException("Invalid calendar");
        return new Date(completedSteps / stepsPerYear, (int) (completedSteps % stepsPerYear) + 1);
    }

    /** Caller explicitly chooses an elapsed-time cap; discarded time is reported, never hidden. */
    public static Accrual accrue(Anchor before, long nowMillis, long millisPerStep, long maximumElapsedMillis) {
        if (nowMillis < 0 || millisPerStep < 1 || maximumElapsedMillis < 0
                || before.remainderMillis() >= millisPerStep)
            throw new IllegalArgumentException("Invalid clock settings/anchor");
        // A backwards wall clock stays pinned until it catches up, avoiding duplicate aging.
        long observed = Math.max(nowMillis, before.lastObservedMillis());
        long elapsed = observed - before.lastObservedMillis();
        long accepted = Math.min(elapsed, maximumElapsedMillis);
        long steps = accepted / millisPerStep;
        long remainder = accepted % millisPerStep;
        long space = millisPerStep - before.remainderMillis();
        if (remainder >= space) {
            steps = Math.incrementExact(steps);
            remainder -= space;
        } else {
            remainder += before.remainderMillis();
        }
        return new Accrual(new Anchor(observed, remainder, Math.addExact(before.targetSteps(), steps)),
                steps, elapsed - accepted);
    }
}
