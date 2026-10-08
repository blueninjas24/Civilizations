package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.population.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PopulationDiagnosticsTest {
    @Test void explanationShowsSavedReasonsPhysicalDeathsAndBothSidesOfMigration() {
        UUID town = UUID.randomUUID(), origin = UUID.randomUUID(), person = UUID.randomUUID(), home = UUID.randomUUID();
        var resident = new Resident(person, "Alice", town, home, 300, Resident.Status.DECEASED);
        var state = new PopulationState(List.of(resident), List.of(new Household(home, town, "Home"), new Household(UUID.randomUUID(), origin, "Origin")), List.of(), Map.of());
        var event = new PopulationEvent(5, PopulationEvent.Type.MIGRATION, person, town, List.of(), "Housing pressure and space", origin);
        var identity = new com.blueninjas24.civilizations.database.ResidentProjectionRepository.Snapshot(Map.of(),
                Map.of(new com.blueninjas24.civilizations.database.ResidentProjectionRepository.EntityKey(UUID.randomUUID(), UUID.randomUUID()), person), Map.of());
        var view = new PopulationSession.ProjectionView(state, identity,
                List.of(new com.blueninjas24.civilizations.database.DemographicRepository.StoredEvent(1, event)),
                List.of(new com.blueninjas24.civilizations.database.ResidentProjectionRepository.PhysicalDeath(1, person, 6, 1000, "FIRE")));
        assertTrue(PopulationDiagnostics.explain(view, town).stream().anyMatch(line -> line.contains("Alice") && line.contains("Housing pressure")));
        assertTrue(PopulationDiagnostics.explain(view, origin).stream().anyMatch(line -> line.contains("MIGRATION")));
        assertTrue(PopulationDiagnostics.explain(view, town).stream().anyMatch(line -> line.contains("physical death of Alice") && line.contains("FIRE")));
        assertTrue(PopulationDiagnostics.explain(view, town).contains("Unresolved physical identities: 1; these cannot spawn replacements automatically."));
        assertEquals(Resident.Status.DECEASED, state.residents().getFirst().status());
    }
    @Test void statusExplainsCalendarBacklogAndFailure() {
        var lines = PopulationDiagnostics.status(new PopulationSession.Status(true, 14, 17, 5, 2_500_000, "retryable SQL failure"),
                PopulationSession.Settings.prototype());
        assertTrue(lines.stream().anyMatch(line -> line.contains("year 1, month 3") && line.contains("queued 3")));
        assertTrue(lines.contains("Last batch: 2.500 ms"));
        assertTrue(lines.contains("Last failure: retryable SQL failure"));
    }
    @Test void settlementCountsRetainedDeadPeopleAndDistinguishesUnknownHousing() {
        UUID town = UUID.randomUUID(), otherTown = UUID.randomUUID(), known = UUID.randomUUID(), unknown = UUID.randomUUID();
        var state = new PopulationState(List.of(
                new Resident(UUID.randomUUID(), "Alive", town, known, 240, Resident.Status.ALIVE),
                new Resident(UUID.randomUUID(), "Dead", town, known, 240, Resident.Status.DECEASED),
                new Resident(UUID.randomUUID(), "Elsewhere", otherTown, null, 240, Resident.Status.ALIVE)),
                List.of(new Household(known, town, "Known"), new Household(unknown, town, "Unknown")), List.of(), Map.of(known, 4));
        var before = state;
        var lines = PopulationDiagnostics.settlement(state, town);
        assertTrue(lines.contains("Saved living residents: 1; retained deceased: 1"));
        assertTrue(lines.contains("Households: 2; observed housing: 1/2"));
        assertTrue(lines.contains("Known housing capacity: 4 (unobserved homes are unknown)"));
        assertSame(before, state);
        assertEquals(List.of("No population records for settlement " + new UUID(0, 1)),
                PopulationDiagnostics.settlement(state, new UUID(0, 1)));
    }
}
