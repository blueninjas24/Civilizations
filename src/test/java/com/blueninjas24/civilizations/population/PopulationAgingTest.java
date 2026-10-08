package com.blueninjas24.civilizations.population;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PopulationAgingTest {
    @Test
    void preservesEveryFieldAndInputWhileAgingOnlyLivingPeople() {
        Resident living = person(20, Resident.Status.ALIVE);
        Resident deceased = person(30, Resident.Status.DECEASED);
        List<Resident> input = new ArrayList<>(List.of(living, deceased));
        List<Resident> result = PopulationAging.step(input);
        assertEquals(new Resident(living.id(), living.name(), living.settlementId(), living.householdId(),
                21, living.status()), result.getFirst());
        assertEquals(deceased, result.get(1));
        assertEquals(List.of(living, deceased), input);
        assertThrows(UnsupportedOperationException.class, result::clear);
    }

    @Test
    void failedOverflowDoesNotPartiallyMutateInput() {
        List<Resident> input = List.of(person(5, Resident.Status.ALIVE), person(Long.MAX_VALUE, Resident.Status.ALIVE));
        assertThrows(ArithmeticException.class, () -> PopulationAging.step(input));
        assertEquals(5, input.getFirst().ageSteps());
        assertEquals(Long.MAX_VALUE, input.get(1).ageSteps());
        assertEquals(List.of(), PopulationAging.step(List.of()));
    }

    @Test
    void fiveHundredResidentsAdvanceOneThousandLogicalStepsRepeatably() {
        List<Resident> initial = java.util.stream.IntStream.range(0, 500)
                .mapToObj(i -> person(i, Resident.Status.ALIVE)).toList();
        List<Resident> first = initial;
        List<Resident> replay = initial;
        for (int step = 0; step < 1000; step++) {
            first = PopulationAging.step(first);
            replay = PopulationAging.step(replay);
        }
        assertEquals(first, replay);
        for (int i = 0; i < 500; i++) {
            assertEquals(initial.get(i).id(), first.get(i).id());
            assertEquals(i + 1000, first.get(i).ageSteps());
        }
    }

    private Resident person(long age, Resident.Status status) {
        return new Resident(UUID.randomUUID(), "Test", UUID.randomUUID(), UUID.randomUUID(), age, status);
    }
}
