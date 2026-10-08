package com.blueninjas24.civilizations.population;

/** Editable prototype inputs, not permanent balance or implied real-world statistics. */
public record PopulationPolicy(int stepsPerYear, long adulthoodSteps, long parentMinimumSteps,
                               long parentMaximumSteps, long seniorSteps,
                               double birthChance, double backgroundDeathChance,
                               double seniorDeathIncreasePerYear) {
    public PopulationPolicy {
        if (stepsPerYear <= 0 || adulthoodSteps < 0 || parentMinimumSteps < adulthoodSteps
                || parentMaximumSteps < parentMinimumSteps || seniorSteps < adulthoodSteps) {
            throw new IllegalArgumentException("Invalid population age/calendar settings");
        }
        probability(birthChance);
        probability(backgroundDeathChance);
        probability(seniorDeathIncreasePerYear);
    }

    public static PopulationPolicy prototype() {
        return new PopulationPolicy(12, 18 * 12, 20 * 12, 40 * 12, 60 * 12,
                0.015, 0.0001, 0.0005);
    }

    public double deathChance(long ageSteps) {
        if (ageSteps < 0) {
            throw new IllegalArgumentException("Age must be nonnegative");
        }
        double seniorYears = ageSteps >= seniorSteps ? (ageSteps - seniorSteps) / (double) stepsPerYear : 0;
        return Math.min(1, backgroundDeathChance + seniorYears * seniorDeathIncreasePerYear);
    }

    public boolean eligibleParent(Resident person) {
        return person.status() == Resident.Status.ALIVE && person.ageSteps() >= parentMinimumSteps
                && person.ageSteps() <= parentMaximumSteps;
    }

    private static void probability(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Population probabilities must be finite values in [0, 1]");
        }
    }
}
