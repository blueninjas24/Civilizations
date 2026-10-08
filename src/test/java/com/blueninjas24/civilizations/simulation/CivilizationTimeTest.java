package com.blueninjas24.civilizations.simulation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CivilizationTimeTest {
    @Test void fractionalMonthsAndDifferentObservationIntervalsAgree() {
        var initial = new CivilizationTime.Anchor(1000, 0, 0);
        var partial = CivilizationTime.accrue(initial, 1019, 30, Long.MAX_VALUE);
        assertEquals(new CivilizationTime.Anchor(1019, 19, 0), partial.anchor());
        var split = CivilizationTime.accrue(partial.anchor(), 1100, 30, Long.MAX_VALUE);
        assertEquals(CivilizationTime.accrue(initial, 1100, 30, Long.MAX_VALUE),
                new CivilizationTime.Accrual(split.anchor(), split.addedSteps(), split.discardedMillis()));
        assertEquals(new CivilizationTime.Anchor(1100, 10, 3), split.anchor());
    }

    @Test void backwardsClockDoesNotDoubleCountRecoveredTime() {
        var initial = new CivilizationTime.Anchor(1000, 5, 10);
        assertEquals(initial, CivilizationTime.accrue(initial, 900, 30, Long.MAX_VALUE).anchor());
        assertEquals(initial, CivilizationTime.accrue(initial, 1000, 30, Long.MAX_VALUE).anchor());
        assertEquals(new CivilizationTime.Anchor(1025, 0, 11),
                CivilizationTime.accrue(initial, 1025, 30, Long.MAX_VALUE).anchor());
    }

    @Test void explicitElapsedCapReportsDiscardAndRetainsExistingBacklog() {
        var outcome = CivilizationTime.accrue(new CivilizationTime.Anchor(1000, 5, 80), 2000, 30, 90);
        assertEquals(910, outcome.discardedMillis());
        assertEquals(new CivilizationTime.Anchor(2000, 5, 83), outcome.anchor());
        assertEquals(0, CivilizationTime.accrue(outcome.anchor(), 2000, 30, 90).addedSteps());
    }

    @Test void arithmeticIsSafeAtLongBoundaryAndOverflowFails() {
        var first = new CivilizationTime.Anchor(0, Long.MAX_VALUE - 1, 0);
        assertEquals(new CivilizationTime.Anchor(Long.MAX_VALUE, Long.MAX_VALUE - 1, 1),
                CivilizationTime.accrue(first, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE).anchor());
        assertThrows(ArithmeticException.class, () -> CivilizationTime.accrue(
                new CivilizationTime.Anchor(0, 0, Long.MAX_VALUE), 1, 1, Long.MAX_VALUE));
    }

    @Test void calendarAndInvalidInputsAreExplicit() {
        assertEquals(new CivilizationTime.Date(0, 1), CivilizationTime.date(0, 12));
        assertEquals(new CivilizationTime.Date(99, 12), CivilizationTime.date(1199, 12));
        assertEquals(new CivilizationTime.Date(100, 1), CivilizationTime.date(1200, 12));
        var anchor = new CivilizationTime.Anchor(0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> CivilizationTime.date(-1, 12));
        assertThrows(IllegalArgumentException.class, () -> CivilizationTime.date(0, 0));
        assertThrows(IllegalArgumentException.class, () -> CivilizationTime.accrue(anchor, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> CivilizationTime.accrue(anchor, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> CivilizationTime.accrue(anchor, 1, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> CivilizationTime.accrue(
                new CivilizationTime.Anchor(0, 1, 0), 1, 1, 1));
    }
}
