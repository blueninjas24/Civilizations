package com.blueninjas24.civilizations.industry;

import java.util.Objects;
import java.util.UUID;

/** Trusted adapter evidence of an existing workplace, not a construction/land ownership permission. */
public record BusinessSite(UUID id, UUID settlementId, UUID worldId, int x, int y, int z,
                           long observedMillis, String evidence) {
    public BusinessSite {
        Objects.requireNonNull(id); Objects.requireNonNull(settlementId); Objects.requireNonNull(worldId);
        Business.text(evidence, 2048);
        if (observedMillis < 0) throw new IllegalArgumentException("Negative observation time");
    }
}
