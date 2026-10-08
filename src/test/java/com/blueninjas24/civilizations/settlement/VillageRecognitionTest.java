package com.blueninjas24.civilizations.settlement;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class VillageRecognitionTest {
    private static final UUID WORLD = new UUID(0, 1);
    private static VillageObservation.Position bed(int x) { return new VillageObservation.Position(x, 64, 0); }
    private static VillageObservation.Person person(int id, int x) {
        return new VillageObservation.Person(new UUID(0, id), bed(x), true, null);
    }
    private static VillageObservation observation(boolean complete, List<VillageObservation.Position> beds,
                                                  List<VillageObservation.Person> people) {
        return new VillageObservation(WORLD, "test-world", 1000, complete, beds, people);
    }

    @Test void populatedBedsQualifyAcrossOldRegionBoundaryRegardlessOfScanOrder() {
        var first = observation(true, List.of(bed(130), bed(126)), List.of(person(2, 130), person(1, 126)));
        var reordered = observation(true, List.of(bed(126), bed(130)), List.of(person(1, 126), person(2, 130)));
        var result = VillageRecognition.recognize(first, VillageRecognition.Policy.prototype());
        assertEquals(result, VillageRecognition.recognize(reordered, VillageRecognition.Policy.prototype()));
        assertEquals(1, result.candidates().size());
        assertEquals(List.of(bed(126), bed(130)), result.candidates().getFirst().beds());
        assertEquals(2, result.candidates().getFirst().villagers().size());
    }

    @Test void bedsAloneOrVillagersAloneDoNotEnrollPlayerBase() {
        assertTrue(VillageRecognition.recognize(observation(true, List.of(bed(0), bed(1)), List.of()),
                VillageRecognition.Policy.prototype()).candidates().isEmpty());
        var empty = VillageRecognition.recognize(observation(true, List.of(), List.of(person(1, 0), person(2, 0))),
                VillageRecognition.Policy.prototype());
        assertTrue(empty.candidates().isEmpty());
        assertEquals(2, empty.unassignedVillagers());
    }

    @Test void separatedClustersStaySeparateAndFarAwayVillagersRemainUnassigned() {
        var result = VillageRecognition.recognize(observation(true,
                List.of(bed(0), bed(1), bed(100), bed(101)),
                List.of(person(1, 0), person(2, 1), person(3, 100), person(4, 101), person(5, 1000))),
                VillageRecognition.Policy.prototype());
        assertEquals(2, result.candidates().size());
        assertEquals(1, result.unassignedVillagers());
        assertEquals(0, result.rejectedClusters());
    }

    @Test void distanceTiesAndVerticalDistanceAreDeterministic() {
        var settings = new VillageRecognition.Policy(1, 32, 1, 1, 10, 10);
        var result = VillageRecognition.recognize(observation(true, List.of(bed(20), bed(-20)),
                List.of(person(1, 0), new VillageObservation.Person(new UUID(0, 2),
                        new VillageObservation.Position(20, 200, 0), true, null))), settings);
        assertEquals(List.of(bed(-20)), result.candidates().getFirst().beds());
        assertEquals(1, result.unassignedVillagers());
        assertEquals(1, result.rejectedClusters());
        assertTrue(bed(Integer.MIN_VALUE).squaredDistance(bed(Integer.MAX_VALUE)) > 0);
    }

    @Test void partialSnapshotsRemainPartialAndCannotMasqueradeAsCompleteCensus() {
        var result = VillageRecognition.recognize(observation(false, List.of(bed(0), bed(1)),
                List.of(person(1, 0), person(2, 1))), VillageRecognition.Policy.prototype());
        assertFalse(result.candidates().getFirst().complete());
        assertThrows(UnsupportedOperationException.class, () -> result.candidates().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.candidates().getFirst().beds().clear());
    }

    @Test void duplicatesInvalidSettingsAndOverBudgetObservationsFailExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> observation(true, List.of(bed(0), bed(0)), List.of()));
        assertThrows(IllegalArgumentException.class, () -> observation(true, List.of(), List.of(person(1, 0), person(1, 1))));
        assertThrows(IllegalArgumentException.class, () -> new VillageRecognition.Policy(0, 32, 1, 1, 10, 10));
        var beds = new ArrayList<VillageObservation.Position>();
        for (int i = 0; i < 513; i++) beds.add(bed(i));
        assertThrows(IllegalArgumentException.class, () -> VillageRecognition.recognize(
                observation(true, beds, List.of()), VillageRecognition.Policy.prototype()));
    }

    @Test void thresholdsAreCallerEditableAndNamesDoNotDefineWorldIdentity() {
        var first = observation(true, List.of(bed(0)), List.of(person(1, 0)));
        assertTrue(VillageRecognition.recognize(first, VillageRecognition.Policy.prototype()).candidates().isEmpty());
        assertEquals(1, VillageRecognition.recognize(first, new VillageRecognition.Policy(32, 32, 1, 1, 10, 10)).candidates().size());
        var renamed = new VillageObservation(WORLD, "renamed-world", 1000, true, first.beds(), first.villagers());
        assertEquals(first.worldId(), renamed.worldId());
        var different = new VillageObservation(new UUID(0, 2), "test-world", 1000, true, first.beds(), first.villagers());
        assertNotEquals(first.worldId(), different.worldId());
    }
}
