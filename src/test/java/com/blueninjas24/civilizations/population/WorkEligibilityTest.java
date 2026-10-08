package com.blueninjas24.civilizations.population;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WorkEligibilityTest {
    private final UUID personId = UUID.randomUUID();
    private Resident person(long age, Resident.Status status) {
        return new Resident(personId, "Worker", UUID.randomUUID(), null, age, status);
    }

    @Test
    void livingResidentAtExactAgeAndSkillThresholdQualifies() {
        var profile = new WorkProfile(personId, Map.of("farming", 10L));
        assertTrue(WorkEligibility.evaluate(person(216, Resident.Status.ALIVE), profile,
                new JobRequirements(216, Map.of("farming", 10L))).eligible());
        assertTrue(WorkEligibility.evaluate(person(1000, Resident.Status.ALIVE), profile,
                new JobRequirements(216, Map.of())).eligible()); // No guessed retirement rule.
    }

    @Test
    void underageAndMissingSkillsHaveStableSpecificReasons() {
        var result = WorkEligibility.evaluate(person(215, Resident.Status.ALIVE), new WorkProfile(personId, Map.of()),
                new JobRequirements(216, Map.of("timber", 2L, "farming", 10L)));
        assertFalse(result.eligible());
        assertEquals(java.util.List.of("Minimum working age: 216 logical steps", "farming: requires 10, has 0", "timber: requires 2, has 0"), result.unmetRequirements());
        assertThrows(UnsupportedOperationException.class, () -> result.unmetRequirements().clear());
    }

    @Test
    void deceasedResidentNeverQualifiesEvenWithSkills() {
        var result = WorkEligibility.evaluate(person(500, Resident.Status.DECEASED),
                new WorkProfile(personId, Map.of("farming", Long.MAX_VALUE)), new JobRequirements(216, Map.of("farming", 1L)));
        assertEquals(java.util.List.of("Resident is deceased"), result.unmetRequirements());
    }

    @Test
    void rejectsAnotherPersonsSkillsAndInvalidRequirements() {
        assertThrows(IllegalArgumentException.class, () -> WorkEligibility.evaluate(person(216, Resident.Status.ALIVE),
                new WorkProfile(UUID.randomUUID(), Map.of()), new JobRequirements(216, Map.of())));
        assertThrows(IllegalArgumentException.class, () -> new WorkProfile(personId, Map.of(" ", 1L)));
        assertThrows(IllegalArgumentException.class, () -> new WorkProfile(personId, Map.of("farming", -1L)));
        assertThrows(IllegalArgumentException.class, () -> new JobRequirements(-1, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new JobRequirements(0, Map.of("farming", -1L)));
    }

    @Test
    void inputMapsRemainImmutableSnapshotsAndAbsentSkillsStartAtZero() {
        var input = new HashMap<String, Long>();
        input.put("farming", 10L);
        var profile = new WorkProfile(personId, input);
        var requirements = new JobRequirements(216, input);
        input.put("farming", 999L);
        assertEquals(10, profile.experience().get("farming"));
        assertEquals(10, requirements.minimumExperience().get("farming"));
        assertThrows(UnsupportedOperationException.class, () -> profile.experience().clear());
        assertTrue(WorkEligibility.evaluate(person(216, Resident.Status.ALIVE), new WorkProfile(personId, Map.of()),
                new JobRequirements(216, Map.of("farming", 0L))).eligible());
    }
}
