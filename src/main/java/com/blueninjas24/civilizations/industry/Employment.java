package com.blueninjas24.civilizations.industry;

import java.util.Objects;
import java.util.UUID;

/** One retained staffing episode; an ended job remains historical evidence. */
public record Employment(UUID id, UUID businessId, UUID residentId, long startedStep,
                         Long endedStep, String endReason) {
    public Employment {
        Objects.requireNonNull(id); Objects.requireNonNull(businessId); Objects.requireNonNull(residentId);
        if (startedStep < 0 || (endedStep != null && endedStep < startedStep)
                || ((endedStep == null) != (endReason == null)))
            throw new IllegalArgumentException("Invalid employment interval");
        if (endReason != null) Business.text(endReason, 2048);
    }
}
