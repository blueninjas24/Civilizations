package com.blueninjas24.civilizations.settlement;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class BedScanTest {
    @Test void budgetedReadsVisitEachBlockOnceAndKeepNegativeWorldCoordinates() {
        var reads = new AtomicInteger();
        var scanner = new BedScan(new BedScan.Source() {
            public boolean emptySection(int sectionY) { return sectionY != -1; }
            public boolean headBed(int x, int y, int z) { reads.incrementAndGet(); return x == 15 && y == -1 && z == 7; }
        }, -2, -3, -32, 32, 2);
        assertThrows(IllegalStateException.class, scanner::beds);
        while (!scanner.complete()) assertTrue(scanner.advance(73) <= 73);
        assertEquals(4096, reads.get());
        assertEquals(List.of(new VillageObservation.Position(-17, -1, -41)), scanner.beds());
        assertEquals(0, scanner.advance(1)); assertEquals(4096, reads.get());
    }
    @Test void allEmptySectionsFinishWithoutReadingBlocks() {
        var scanner = new BedScan(new BedScan.Source() {
            public boolean emptySection(int sectionY) { return true; }
            public boolean headBed(int x, int y, int z) { fail("Empty section read"); return false; }
        }, 0, 0, -64, 320, 2);
        assertEquals(0, scanner.advance(1)); assertTrue(scanner.complete()); assertTrue(scanner.beds().isEmpty());
    }
    @Test void overflowAndInvalidBoundsFailInsteadOfTruncatingObservation() {
        var scanner = new BedScan(new BedScan.Source() {
            public boolean emptySection(int sectionY) { return false; }
            public boolean headBed(int x, int y, int z) { return true; }
        }, 0, 0, 0, 16, 2);
        assertThrows(IllegalArgumentException.class, () -> scanner.advance(3));
        assertThrows(IllegalArgumentException.class, () -> new BedScan(null, 0, 0, -63, 320, 2));
        assertThrows(IllegalArgumentException.class, () -> scanner.advance(0));
    }
}
