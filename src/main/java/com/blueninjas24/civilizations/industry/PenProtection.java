package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Confirmed immutable initial pen footprint. Geometry alone does not certify land authority. */
public record PenProtection(UUID id, UUID world, UUID owner, int standingY,
                            int signX, int signY, int signZ, PenScan.Column seed,
                            PenScan.Evidence evidence, Set<UUID> animals, long confirmedMillis) {
    public PenProtection {
        Objects.requireNonNull(id); Objects.requireNonNull(world); Objects.requireNonNull(owner);
        Objects.requireNonNull(seed); Objects.requireNonNull(evidence); animals = Set.copyOf(animals);
        if (confirmedMillis < 0 || evidence.state() != PenScan.State.ENCLOSED
                || !evidence.interior().contains(seed) || animals.size() > 4096
                || Math.abs((long)signY - standingY) > 2 || standingY == Integer.MIN_VALUE || standingY == Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid confirmed pen");
        var scan = new PenScan(p -> evidence.interior().contains(p) ? PenScan.Cell.INTERIOR
                : evidence.boundary().contains(p) ? PenScan.Cell.BARRIER : PenScan.Cell.OPEN,
                seed, new PenScan.Limits(64, 4096));
        while (!scan.complete()) scan.advance(4096);
        if (!scan.evidence().equals(evidence)) throw new IllegalArgumentException("Disconnected or incomplete pen footprint");
        // The sign's supporting boundary is adjacent; accepting a sign anywhere in the world is unsafe.
        if (evidence.boundary().stream().noneMatch(p -> Math.abs((long)p.x() - signX) + Math.abs((long)p.z() - signZ) == 1))
            throw new IllegalArgumentException("Protection sign must adjoin the saved boundary");
    }
}
