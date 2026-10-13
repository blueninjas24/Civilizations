package com.blueninjas24.civilizations.industry;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PenScanTest {
    PenScan.Cell square(PenScan.Column p) {
        return Math.abs(p.x()) == 3 || Math.abs(p.z()) == 3 ? PenScan.Cell.BARRIER : PenScan.Cell.INTERIOR;
    }
    PenScan.Evidence scan(PenScan.Source source, PenScan.Limits limits, int budget) {
        var scan = new PenScan(source, new PenScan.Column(0, 0), limits);
        int turns = 0;
        while (!scan.complete()) { assertTrue(scan.advance(budget) <= budget); assertTrue(++turns < 10000); }
        assertEquals(0, scan.advance(budget));
        return scan.evidence();
    }
    @Test void completePenHasExactFiniteInteriorAndBoundary() {
        var result = scan(this::square, PenScan.Limits.prototype(), 7);
        assertEquals(PenScan.State.ENCLOSED, result.state());
        assertEquals(25, result.interior().size()); assertEquals(20, result.boundary().size());
        assertThrows(UnsupportedOperationException.class, () -> result.interior().clear());
    }
    @Test void straightWalkwayBetweenBuildingsNeverClaimsTheSurroundingWorld() {
        var result = scan(p -> p.z() == 2 && Math.abs(p.x()) <= 4
                || Math.abs(p.x()) == 4 && p.z() >= 2 && p.z() <= 4
                ? PenScan.Cell.BARRIER : PenScan.Cell.INTERIOR, new PenScan.Limits(8, 256), 3);
        assertEquals(PenScan.State.LIMIT_EXCEEDED, result.state()); assertTrue(result.interior().isEmpty());
    }
    @Test void buildingCourtyardIsPreviewableButNotDistinguishableFromPenByShape() {
        // Solid building edges and fences may truly close a courtyard. Shape alone grants no claim.
        assertEquals(PenScan.State.ENCLOSED, scan(this::square, PenScan.Limits.prototype(), 1).state());
    }
    @Test void missingFenceAllowsEscapeAndRejectsPartialFootprint() {
        var result = scan(p -> p.equals(new PenScan.Column(3, 0)) ? PenScan.Cell.INTERIOR : square(p),
                new PenScan.Limits(8, 256), 8);
        assertNotEquals(PenScan.State.ENCLOSED, result.state()); assertTrue(result.boundary().isEmpty());
    }
    @Test void unknownChunkEdgeCannotBeUsedAsFence() {
        var result = scan(p -> p.equals(new PenScan.Column(3, 0)) ? PenScan.Cell.UNKNOWN : square(p),
                PenScan.Limits.prototype(), 5);
        assertEquals(PenScan.State.UNKNOWN, result.state()); assertTrue(result.interior().isEmpty());
    }
    @Test void knownVerticalEscapeRejectsHorizontalEnclosure() {
        assertEquals(PenScan.State.OPEN, scan(p -> p.equals(new PenScan.Column(1, 0))
                ? PenScan.Cell.OPEN : square(p), PenScan.Limits.prototype(), 2).state());
    }
    @Test void areaAndDistanceCapsRefuseOversizedClaims() {
        assertEquals(PenScan.State.LIMIT_EXCEEDED, scan(this::square, new PenScan.Limits(8, 24), 8).state());
        assertEquals(PenScan.State.ENCLOSED, scan(this::square, new PenScan.Limits(3, 25), 8).state());
        assertEquals(PenScan.State.LIMIT_EXCEEDED, scan(this::square, new PenScan.Limits(2, 25), 8).state());
    }
    @Test void coordinateOverflowDoesNotWrapToAnotherPlace() {
        var scanner = new PenScan(p -> PenScan.Cell.INTERIOR,
                new PenScan.Column(Integer.MAX_VALUE, Integer.MIN_VALUE), PenScan.Limits.prototype());
        assertEquals(1, scanner.advance(1));
        assertEquals(PenScan.State.LIMIT_EXCEEDED, scanner.evidence().state());
    }
    @Test void tickBudgetAndScanOrderingAreDeterministic() {
        var reads = new AtomicInteger();
        var scanner = new PenScan(p -> { reads.incrementAndGet(); return square(p); },
                new PenScan.Column(0, 0), PenScan.Limits.prototype());
        assertThrows(IllegalStateException.class, scanner::evidence);
        while (!scanner.complete()) { int before = reads.get(); int used = scanner.advance(2); assertEquals(used, reads.get() - before); }
        assertEquals(45, reads.get());
        assertEquals(scanner.evidence(), scan(this::square, PenScan.Limits.prototype(), 4096));
    }
    @Test void aSeedOnFenceCannotProtectLand() {
        assertEquals(PenScan.State.OPEN, scan(p -> PenScan.Cell.BARRIER, PenScan.Limits.prototype(), 1).state());
    }
}
