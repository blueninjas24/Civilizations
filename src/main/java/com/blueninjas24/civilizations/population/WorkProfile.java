package com.blueninjas24.civilizations.population;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Stable resident skill counters. Units/learning rates and commodity catalogs are not chosen here. */
public record WorkProfile(UUID residentId, Map<String, Long> experience) {
    public WorkProfile {
        Objects.requireNonNull(residentId);
        experience = Map.copyOf(experience);
        for (var skill : experience.entrySet()) {
            if (skill.getKey().isBlank() || skill.getValue() < 0) throw new IllegalArgumentException("Invalid skill experience");
        }
    }
}
