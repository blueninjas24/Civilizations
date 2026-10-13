package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Immutable proposed footprint, bound to the actor/world/level. Confirmation still requires authority. */
public record PenClaimPreview(UUID actor, UUID world, int standingY, long expiresMillis,
                              PenScan.Evidence evidence, Set<UUID> animals) {
    public PenClaimPreview {
        Objects.requireNonNull(actor);
        Objects.requireNonNull(world);
        Objects.requireNonNull(evidence);
        animals = Set.copyOf(animals);
        if (expiresMillis < 0 || evidence.state() != PenScan.State.ENCLOSED)
            throw new IllegalArgumentException("Only a complete enclosure may be previewed");
    }
    public enum Result { READY, WRONG_ACTOR, WRONG_WORLD_OR_LEVEL, EXPIRED, NOT_AUTHORIZED, EVIDENCE_CHANGED }
    /** Caller must capture fresh geometry/animal membership and recheck claims-plugin authority.
     * READY is permission to attempt a transactional claim, never a saved ownership record. */
    public Result confirm(UUID confirmingActor, UUID currentWorld, int currentStandingY, long nowMillis,
                          boolean authorized, PenScan.Evidence freshEvidence, Set<UUID> freshAnimals) {
        if (nowMillis < 0) throw new IllegalArgumentException("Invalid confirmation time");
        if (!actor.equals(confirmingActor)) return Result.WRONG_ACTOR;
        if (!world.equals(currentWorld) || standingY != currentStandingY) return Result.WRONG_WORLD_OR_LEVEL;
        if (nowMillis >= expiresMillis) return Result.EXPIRED;
        if (!authorized) return Result.NOT_AUTHORIZED;
        if (!evidence.equals(freshEvidence) || !animals.equals(freshAnimals)) return Result.EVIDENCE_CHANGED;
        return Result.READY;
    }
}
