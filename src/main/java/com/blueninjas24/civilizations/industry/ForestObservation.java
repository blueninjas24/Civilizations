package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Bounded, canopy-backed standing-log evidence; it conveys no land or block rights. */
public record ForestObservation(UUID world, long observedMillis, boolean complete, List<Position> trunks) {
    public static final int MAX_TRUNK_COORDINATES = 512;

    public ForestObservation {
        Objects.requireNonNull(world);
        trunks = List.copyOf(trunks);
        if (observedMillis < 0 || trunks.size() > MAX_TRUNK_COORDINATES
                || new HashSet<>(trunks).size() != trunks.size()) {
            throw new IllegalArgumentException("Invalid bounded forest observation");
        }
        trunks = trunks.stream().sorted(Position.ORDER).toList();
    }
}
