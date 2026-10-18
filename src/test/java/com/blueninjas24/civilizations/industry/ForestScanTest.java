package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForestScanTest {
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final ForestScan.Bounds BOUNDS = new ForestScan.Bounds(0, 0, 0, 10, 10, 10);
    private static final Position[] TRUNK = {new Position(5, 1, 5), new Position(5, 2, 5), new Position(5, 3, 5)};

    @Test
    void findsCanopyBackedVerticalTreeWithExactCoordinates() {
        var scan = new ForestScan(ForestScanTest::treeCell, BOUNDS, Set.of(), ForestScan.Geometry.prototype());
        finish(scan, 37);

        var observation = scan.observation(WORLD, 1234);
        assertTrue(observation.complete());
        assertEquals(List.of(TRUNK), observation.trunks());
    }

    @Test
    void knownPlacementsDoNotQualifyAsTrunkOrCanopyAndUnknownMakesEvidenceIncomplete() {
        Position placedLeaf = new Position(6, 3, 5);
        var placed = new ForestScan(ForestScanTest::treeCell, BOUNDS, Set.of(placedLeaf), ForestScan.Geometry.prototype());
        finish(placed, 100);
        assertTrue(placed.observation(WORLD, 1234).trunks().isEmpty());

        Position placedLog = TRUNK[0];
        var planted = new ForestScan(ForestScanTest::treeCell, BOUNDS, Set.of(placedLog), ForestScan.Geometry.prototype());
        finish(planted, 100);
        assertTrue(planted.observation(WORLD, 1234).trunks().isEmpty());

        var uncertain = new ForestScan((x, y, z) -> x == 0 && y == 0 && z == 0
                ? ForestScan.Kind.UNKNOWN : treeCell(x, y, z), BOUNDS, Set.of(), ForestScan.Geometry.prototype());
        finish(uncertain, 100);
        var observation = uncertain.observation(WORLD, 1234);
        assertFalse(observation.complete());
        assertEquals(List.of(TRUNK), observation.trunks());
    }

    @Test
    void incrementalBudgetAndTraversalOrderDoNotChangeBoundedEvidence() {
        int[] reads = {0};
        var source = (ForestScan.Source) (x, y, z) -> { reads[0]++; return treeCell(x, y, z); };
        var oneAtATime = new ForestScan(source, BOUNDS, Set.of(), ForestScan.Geometry.prototype());
        var firstBudget = oneAtATime.advance(7);
        assertEquals(7, firstBudget);
        assertEquals(7, reads[0]);
        finish(oneAtATime, 7);
        var observation = oneAtATime.observation(WORLD, 1234);

        var largerBatches = new ForestScan(ForestScanTest::treeCell, BOUNDS, Set.of(), ForestScan.Geometry.prototype());
        finish(largerBatches, 113);
        assertEquals(observation, largerBatches.observation(WORLD, 1234));

        var capBounds = new ForestScan.Bounds(0, 0, 0, 14, 10, 14);
        var capped = new ForestScan((x, y, z) -> (x == 3 || x == 7) && y >= 1 && y <= 3 && z == 5
                ? ForestScan.Kind.LOG : (x == 4 || x == 6) && y == 3 && z == 5
                ? ForestScan.Kind.LEAF : ForestScan.Kind.AIR,
                capBounds, Set.of(), new ForestScan.Geometry(3, 3, 2, 1));
        finish(capped, 1000);
        var cappedObservation = capped.observation(WORLD, 1234);
        assertFalse(cappedObservation.complete());
        assertTrue(cappedObservation.trunks().size() <= 1);
    }

    @Test
    void edgeTreeDoesNotPoisonInteriorEvidenceAndObservationIsIdempotent() {
        var scan = new ForestScan((x, y, z) -> {
            if ((x == 1 && z == 1 || x == 5 && z == 5) && y >= 1 && y <= 3) return ForestScan.Kind.LOG;
            if ((x == 2 && z == 1 || x == 6 && z == 5) && y == 3) return ForestScan.Kind.LEAF;
            return ForestScan.Kind.AIR;
        }, BOUNDS, Set.of(), ForestScan.Geometry.prototype());
        finish(scan, 23);

        var first = scan.observation(WORLD, 1234);
        var second = scan.observation(WORLD, 1234);
        assertTrue(first.complete());
        assertEquals(List.of(TRUNK), first.trunks());
        assertEquals(first, second);
    }

    private static ForestScan.Kind treeCell(int x, int y, int z) {
        if (x == 5 && z == 5 && y >= 1 && y <= 3) return ForestScan.Kind.LOG;
        if (x == 6 && z == 5 && y == 3) return ForestScan.Kind.LEAF;
        return ForestScan.Kind.AIR;
    }

    private static void finish(ForestScan scan, int budget) {
        while (!scan.complete()) scan.advance(budget);
    }
}
