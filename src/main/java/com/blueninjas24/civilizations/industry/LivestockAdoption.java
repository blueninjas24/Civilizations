package com.blueninjas24.civilizations.industry;

import java.util.Objects;

/** Read-only eligibility; no animal is transferred, bred, spawned or killed here. */
public final class LivestockAdoption {
    private LivestockAdoption() {}
    public enum Ownership { UNCLAIMED, PLAYER, VILLAGE }
    public enum Habitat { FREE_RANGE, UNKNOWN_PEN, PLAYER_PEN, VILLAGE_PEN, UNKNOWN }
    public enum Decision { ADOPT, ALREADY_VILLAGE, PROTECTED, WAIT }
    public record Evidence(Ownership ownership, Habitat habitat, boolean named, boolean leashed,
                           boolean recordedPlayerBreeding, boolean complete, long observedMillis) {
        public Evidence {
            Objects.requireNonNull(ownership);
            Objects.requireNonNull(habitat);
            if (observedMillis < 0) throw new IllegalArgumentException("Invalid livestock observation duration");
        }
    }
    /** Persistent individual ownership takes precedence over current location or missing signs. */
    public static Decision assess(Evidence evidence, long requiredObservationMillis) {
        Objects.requireNonNull(evidence);
        if (requiredObservationMillis < 0) throw new IllegalArgumentException("Invalid adoption delay");
        if (evidence.ownership() == Ownership.PLAYER) return Decision.PROTECTED;
        if (evidence.ownership() == Ownership.VILLAGE) return Decision.ALREADY_VILLAGE;
        if (evidence.named() || evidence.leashed() || evidence.recordedPlayerBreeding()
                || evidence.habitat() == Habitat.PLAYER_PEN || evidence.habitat() == Habitat.UNKNOWN_PEN)
            return Decision.PROTECTED;
        if (!evidence.complete() || evidence.habitat() == Habitat.UNKNOWN) return Decision.WAIT;
        if (evidence.habitat() == Habitat.VILLAGE_PEN) return Decision.ADOPT;
        return evidence.observedMillis() >= requiredObservationMillis ? Decision.ADOPT : Decision.WAIT;
    }
}
