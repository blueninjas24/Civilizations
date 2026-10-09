package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static com.blueninjas24.civilizations.industry.OreScan.Kind.*;
import static org.junit.jupiter.api.Assertions.*;

class OreScanTest {
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static List<MineralObservation> finish(OreScan scan, int budget) {
        while (!scan.complete()) assertTrue(scan.advance(budget) <= budget);
        assertEquals(0, scan.advance(budget));
        return scan.observations(WORLD, 10);
    }

    @Test void exposedExistingOreCountsWithoutInventingOriginAndBuriedOreDoesNot() {
        OreScan.Source source = (x,y,z) -> {
            if (x == 4 && y == 0 && z == 4) return COAL;
            if (x == 10 && y == 0 && z == 10) return IRON;
            if (x == 5 && y == 0 && z == 4) return AIR;
            return SOLID;
        };
        var result = finish(new OreScan(source, 0, 0, 0, 1, Set.of()), 7);
        assertTrue(result.stream().allMatch(MineralObservation::complete));
        assertEquals(List.of(new Position(4,0,4)), result.getFirst().ore());
        assertEquals(Commodity.IRON, result.get(1).mineral());
        assertTrue(result.get(1).ore().isEmpty());
    }

    @Test void incrementalBudgetsBoundSourceReadsAndResultsIgnoreBatchSize() {
        var reads = new AtomicInteger();
        OreScan.Source source = (x,y,z) -> {
            reads.incrementAndGet(); return y == 0 && (x + z) % 2 == 0 ? COAL : AIR;
        };
        var scan = new OreScan(source, -2, 3, -1, 2, Set.of());
        while (!scan.complete()) {
            int before = reads.get(); int cells = scan.advance(11);
            assertTrue(cells <= 11); assertTrue(reads.get() - before <= cells * 7);
        }
        var result = scan.observations(WORLD,10);
        assertEquals(result, finish(new OreScan(source,-2,3,-1,2,Set.of()),10000));
        assertEquals(128,result.getFirst().ore().size());
    }

    @Test void exclusionCoordinatesArePermanentAbsoluteAndCopied() {
        var excluded = new HashSet<>(Set.of(new Position(-28,-64,52)));
        OreScan.Source source = (x,y,z) -> y == -64 && z == 4 && (x == 4 || x == 5) ? IRON : AIR;
        var scan = new OreScan(source,-2,3,-64,-63,excluded);
        excluded.clear();
        var result = finish(scan,64);
        assertEquals(List.of(new Position(-27,-64,52)),result.get(1).ore());
        assertThrows(UnsupportedOperationException.class,()->result.get(1).ore().clear());
        assertThrows(UnsupportedOperationException.class,()->result.clear());
    }

    @Test void unknownCellsAndUncertainExposureInvalidateEvidence() {
        for (boolean unknownCell : List.of(true,false)) {
            OreScan.Source source = (x,y,z) -> {
                if (x == 4 && y == 0 && z == 4) return unknownCell ? UNKNOWN : COAL;
                if (!unknownCell && x == 5 && y == 0 && z == 4) return UNKNOWN;
                return SOLID;
            };
            var result = finish(new OreScan(source,0,0,0,1,Set.of()),256);
            assertTrue(result.stream().noneMatch(MineralObservation::complete));
            assertTrue(result.getFirst().ore().isEmpty());
        }
    }

    @Test void knownAirProvesExposureEvenWithUnavailableBorderNeighbor() {
        OreScan.Source source = (x,y,z) -> {
            if (x < 0 || x > 15 || z < 0 || z > 15) return UNKNOWN;
            if (x == 0 && y == 0 && z == 4) return COAL;
            if (x == 1 && y == 0 && z == 4) return AIR;
            return SOLID;
        };
        var result = finish(new OreScan(source,0,0,0,1,Set.of()),256);
        assertTrue(result.getFirst().complete());
        assertEquals(List.of(new Position(0,0,4)),result.getFirst().ore());
    }

    @Test void overlargeMixedSurveysArePartialRatherThanSilentlyMintingTruncatedReserves() {
        // Every cell has an exposed upper/lower face; alternate layers supply air.
        OreScan.Source source = (x,y,z) -> y % 2 != 0 ? AIR : (y == 0 ? IRON : COAL);
        var result = finish(new OreScan(source,0,0,0,5,Set.of()),73);
        assertEquals(512,result.stream().mapToInt(o->o.ore().size()).sum());
        assertTrue(result.stream().noneMatch(MineralObservation::complete));
    }

    @Test void exactly512PositionsRemainCompleteAndEmptySurveyDoesNotMeanDepletion() {
        OreScan.Source source = (x,y,z) -> y % 2 != 0 ? AIR : COAL;
        var result = finish(new OreScan(source,0,0,0,3,Set.of()),256);
        assertEquals(512,result.getFirst().ore().size()); assertTrue(result.getFirst().complete());
        var empty = finish(new OreScan((x,y,z)->SOLID,0,0,0,1,Set.of()),256);
        assertTrue(empty.stream().allMatch(o->o.complete() && o.ore().isEmpty()));
    }

    @Test void failedSourceReadCanRetrySameCellWithoutDuplicateEvidence() {
        var attempts = new AtomicInteger();
        OreScan.Source source = (x,y,z) -> {
            if (x == 1 && y == 0 && z == 0 && attempts.getAndIncrement() == 0)
                throw new IllegalStateException("snapshot read failed");
            return x == 0 && y == 0 && z == 0 ? COAL : AIR;
        };
        var scan = new OreScan(source,0,0,0,1,Set.of());
        assertThrows(IllegalStateException.class,()->scan.advance(1));
        var result = finish(scan,1);
        assertEquals(List.of(new Position(0,0,0)),result.getFirst().ore());
    }

    @Test void invalidBoundsBudgetsAndUnfinishedPublicationFailExplicitly() {
        OreScan.Source source = (x,y,z)->AIR;
        assertThrows(IllegalArgumentException.class,()->new OreScan(source,0,0,1,1,Set.of()));
        assertThrows(IllegalArgumentException.class,()->new OreScan(source,0,0,Integer.MIN_VALUE,1,Set.of()));
        assertThrows(ArithmeticException.class,()->new OreScan(source,Integer.MAX_VALUE,0,0,1,Set.of()));
        var scan = new OreScan(source,0,0,0,1,Set.of());
        assertThrows(IllegalArgumentException.class,()->scan.advance(0));
        assertThrows(IllegalStateException.class,()->scan.observations(WORLD,10));
    }
}
