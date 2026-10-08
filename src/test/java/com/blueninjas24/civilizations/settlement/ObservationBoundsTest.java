package com.blueninjas24.civilizations.settlement;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ObservationBoundsTest {
    @Test void clearBorderExcludesPossibleConnectionsBeyondObservationWindow() {
        var bounds = new VillageObservation.Bounds(-64, 79, -64, 79);
        assertTrue(bounds.encloses(List.of(new VillageObservation.Position(0, 64, 0)), 32));
        for (var bed : List.of(new VillageObservation.Position(-32, 64, 0), new VillageObservation.Position(47, 64, 0),
                new VillageObservation.Position(0, 64, -32), new VillageObservation.Position(0, 64, 47),
                new VillageObservation.Position(80, 64, 0))) {
            assertFalse(bounds.encloses(List.of(bed), 32));
        }
    }
    @Test void distanceArithmeticCannotOverflowAndInvalidMarginsFail() {
        var bounds = new VillageObservation.Bounds(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertTrue(bounds.encloses(List.of(new VillageObservation.Position(0, 64, 0)), 512));
        assertThrows(IllegalArgumentException.class, () -> bounds.encloses(List.of(), -1));
        assertThrows(IllegalArgumentException.class, () -> new VillageObservation.Bounds(10, 0, 0, 10));
    }
}
