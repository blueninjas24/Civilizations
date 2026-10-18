package com.blueninjas24.civilizations.industry;

/** Editable automatic forestry startup limits; observations grant no tree, land, or block rights. */
public record ForestryFoundingPolicy(boolean enabled, int settlementsPerPulse, int patchesPerReview,
        int foundersPerReview, long surveyLifetimeMillis, int horizontalRange, int verticalRange,
        long minimumAgeSteps, long founderExperience, int workerCapacity,
        long startupHundredths, long capitalHundredths) {
    public ForestryFoundingPolicy {
        if (settlementsPerPulse < 1 || settlementsPerPulse > 16
                || patchesPerReview < 1 || patchesPerReview > 64
                || foundersPerReview < 1 || foundersPerReview > 512
                || surveyLifetimeMillis < 1_000 || surveyLifetimeMillis > 3_600_000
                || horizontalRange < 1 || horizontalRange > 256
                || verticalRange < 1 || verticalRange > 128
                || minimumAgeSteps < 1 || minimumAgeSteps > 1_000_000
                || founderExperience < 1 || founderExperience > 1_000_000_000
                || workerCapacity < 1 || workerCapacity > 32
                || startupHundredths < 0 || capitalHundredths < 0
                || startupHundredths > 1_000_000_000L || capitalHundredths > 1_000_000_000L
                || startupHundredths + capitalHundredths == 0) {
            throw new IllegalArgumentException("Invalid automatic forestry founding settings");
        }
    }

    public static ForestryFoundingPolicy prototype() {
        return new ForestryFoundingPolicy(true, 2, 8, 64, 120_000, 96, 48, 216, 10, 2, 500, 2_000);
    }

    public static ForestryFoundingPolicy disabled() {
        var p = prototype();
        return new ForestryFoundingPolicy(false, p.settlementsPerPulse, p.patchesPerReview, p.foundersPerReview,
                p.surveyLifetimeMillis, p.horizontalRange, p.verticalRange, p.minimumAgeSteps,
                p.founderExperience, p.workerCapacity, p.startupHundredths, p.capitalHundredths);
    }
}
