package com.blueninjas24.civilizations.population;

import java.util.Objects;
import java.util.UUID;

/** Shared household identity; occupancy, home mapping and family rules remain separate concerns. */
public record Household(UUID id, UUID settlementId, String name) {
    public Household {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(settlementId, "settlementId");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Household name must not be blank");
        }
    }
}
