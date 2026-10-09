package com.blueninjas24.civilizations.industry;

import java.util.Objects;
import java.util.UUID;

/** Persistent founding identity. Ownership is not employment; registering a site grants no block rights. */
public record Business(UUID id, UUID settlementId, String name, UUID founderId, UUID ownerId,
                       String industry, UUID siteId, int workerCapacity, long foundedStep) {
    public Business {
        Objects.requireNonNull(id); Objects.requireNonNull(settlementId);
        Objects.requireNonNull(founderId); Objects.requireNonNull(ownerId); Objects.requireNonNull(siteId);
        text(name, 128); text(industry, 64);
        if (workerCapacity < 1 || workerCapacity > 1000 || foundedStep < 0)
            throw new IllegalArgumentException("Invalid business capacity or founding step");
    }

    public static void text(String value, int limit) {
        Objects.requireNonNull(value);
        if (value.isBlank() || value.length() > limit)
            throw new IllegalArgumentException("Required text must contain 1.." + limit + " characters");
    }
}
