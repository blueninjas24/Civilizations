package com.blueninjas24.civilizations.population;

import java.util.Map;

/** Qualification inputs for a future real business/job, not an invented employer or assignment. */
public record JobRequirements(long minimumAgeSteps, Map<String, Long> minimumExperience) {
    public JobRequirements {
        if (minimumAgeSteps < 0) throw new IllegalArgumentException("Working age must be nonnegative");
        minimumExperience = Map.copyOf(minimumExperience);
        for (var skill : minimumExperience.entrySet()) {
            if (skill.getKey().isBlank() || skill.getValue() < 0) throw new IllegalArgumentException("Invalid skill requirement");
        }
    }
}
