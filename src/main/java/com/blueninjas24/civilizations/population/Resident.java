package com.blueninjas24.civilizations.population;

import java.util.Objects;
import java.util.UUID;

/** Persistent person; age is logical simulation steps, with no calendar ratio implied. */
public record Resident(UUID id, String name, UUID settlementId, UUID householdId,
                       long ageSteps, Status status) {
    public enum Status { ALIVE, DECEASED }

    public Resident {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(settlementId, "settlementId");
        Objects.requireNonNull(status, "status");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Resident name must not be blank");
        }
        if (ageSteps < 0) {
            throw new IllegalArgumentException("Resident age must be nonnegative");
        }
        // A null household represents unassigned housing, not an invented household.
    }
}
