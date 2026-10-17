package com.blueninjas24.civilizations.industry;

/** Bounded cadence and evidence freshness for deliberate surplus herd slaughter. */
public record LivestockSlaughterPolicy(boolean enabled, int intervalTicks, int candidatesPerReview,
                                       long evidenceMaxAgeMillis) {
    public LivestockSlaughterPolicy {
        if (intervalTicks < 20 || intervalTicks > 1200 || candidatesPerReview < 1 || candidatesPerReview > 256
                || evidenceMaxAgeMillis < 100 || evidenceMaxAgeMillis > 30_000)
            throw new IllegalArgumentException("Invalid livestock slaughter policy bounds");
    }

    public static LivestockSlaughterPolicy prototype() {
        return new LivestockSlaughterPolicy(true, 100, 16, 5000);
    }

    public static LivestockSlaughterPolicy disabled() {
        var p = prototype();
        return new LivestockSlaughterPolicy(false, p.intervalTicks, p.candidatesPerReview, p.evidenceMaxAgeMillis);
    }
}
