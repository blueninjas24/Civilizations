package com.blueninjas24.civilizations.population;

import com.blueninjas24.civilizations.simulation.SimulationRunner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PopulationMigrationTest {
    private final UUID source = new UUID(0, 1);
    private final UUID target = new UUID(0, 2);
    private final Household oldHome = new Household(new UUID(0, 3), source, "Old home");
    private final Household newHome = new Household(new UUID(0, 4), target, "New home");
    private final Resident adult = new Resident(new UUID(0, 5), "Parent", source, oldHome.id(), 240, Resident.Status.ALIVE);
    private final Resident child = new Resident(new UUID(0, 6), "Child", source, oldHome.id(), 12, Resident.Status.ALIVE);
    private final FamilyTie tie = new FamilyTie(adult.id(), child.id(), FamilyTie.Type.PARENT_CHILD);

    @Test
    void wholeLivingHouseholdMovesWithIdentitiesAgesAndFamilyLinksIntact() {
        var state = family(1, 3);
        var result = step(state, new MigrationPolicy(1, 0), worlds());
        assertEquals(2, result.state().livingCount());
        assertEquals(state.familyTies(), result.state().familyTies());
        assertEquals(List.of(oldHome, newHome), result.state().households());
        for (Resident person : result.state().residents()) {
            assertEquals(target, person.settlementId());
            assertEquals(newHome.id(), person.householdId());
        }
        assertEquals(adult.ageSteps(), result.state().residents().getFirst().ageSteps());
        assertEquals(2, result.events().size());
        assertTrue(result.events().stream().allMatch(e -> e.type() == PopulationEvent.Type.MIGRATION
                && e.sourceSettlementId().equals(source) && e.settlementId().equals(target)));
        assertEquals(state, family(1, 3)); // Input untouched.
        assertEquals(result, step(state, new MigrationPolicy(1, 0), worlds()));
    }

    @Test
    void comfortableHouseholdsUnknownHousingAndCrossWorldDestinationsDoNotCauseMoves() {
        assertTrue(step(family(2, 3), new MigrationPolicy(1, 0), worlds()).events().isEmpty());
        assertTrue(step(family(1, 3), new MigrationPolicy(1, 0), Map.of(source, "world", target, "nether")).events().isEmpty());
        assertTrue(step(family(1, 3), new MigrationPolicy(1, 0), Map.of(source, "world")).events().isEmpty());
        var unknown = new PopulationState(List.of(adult, child), List.of(oldHome, newHome), List.of(tie), Map.of(newHome.id(), 3));
        assertTrue(step(unknown, new MigrationPolicy(1, 0), worlds()).events().isEmpty());
        assertTrue(step(family(1, 3), MigrationPolicy.disabled(), worlds()).events().isEmpty());
    }

    @Test
    void undersizedOrOccupiedDestinationNeverSplitsOrOverfillsFamily() {
        assertTrue(step(family(1, 1), new MigrationPolicy(1, 0), worlds()).events().isEmpty());
        Resident occupant = new Resident(new UUID(0, 7), "Occupant", target, newHome.id(), 240, Resident.Status.ALIVE);
        var occupied = new PopulationState(List.of(adult, child, occupant), List.of(oldHome, newHome), List.of(tie),
                Map.of(oldHome.id(), 1, newHome.id(), 3));
        assertTrue(step(occupied, new MigrationPolicy(1, 0), worlds()).events().isEmpty());
    }

    @Test
    void unassignedAdultCanMoveButUnaccompaniedMinorAndDeceasedCannot() {
        for (Resident person : List.of(new Resident(adult.id(), adult.name(), source, null, 240, Resident.Status.ALIVE),
                new Resident(child.id(), child.name(), source, null, 12, Resident.Status.ALIVE),
                new Resident(adult.id(), adult.name(), source, null, 240, Resident.Status.DECEASED))) {
            var state = new PopulationState(List.of(person), List.of(newHome), List.of(), Map.of(newHome.id(), 1));
            var result = step(state, new MigrationPolicy(1, 0), worlds());
            assertEquals(person.status() == Resident.Status.ALIVE && person.ageSteps() >= 216 ? 1 : 0, result.events().size());
        }
    }

    @Test
    void externalArrivalsHaveNewStableIdentitiesAndNeverExceedObservedCapacity() {
        var state = new PopulationState(List.of(), List.of(newHome), List.of(), Map.of(newHome.id(), 1));
        var result = step(state, new MigrationPolicy(0, 1), worlds());
        assertEquals(1, result.state().livingCount());
        assertEquals(PopulationEvent.Type.ARRIVAL, result.events().getFirst().type());
        assertNull(result.events().getFirst().sourceSettlementId());
        assertEquals(216, result.state().residents().getFirst().ageSteps());
        assertEquals(result, step(state, new MigrationPolicy(0, 1), worlds()));
        assertTrue(step(result.state(), new MigrationPolicy(0, 1), worlds()).events().isEmpty());
        assertTrue(step(state, new MigrationPolicy(0, 1), Map.of()).events().isEmpty());
    }

    @Test
    void accountingConservesInternalMovesAndExternalArrivalsIncreaseTotalOnce() {
        var before = family(1, 4);
        var result = step(before, new MigrationPolicy(1, 1), worlds());
        long arrivals = result.events().stream().filter(e -> e.type() == PopulationEvent.Type.ARRIVAL).count();
        assertEquals(before.livingCount() + arrivals, result.state().livingCount());
        assertEquals(2, result.events().stream().filter(e -> e.type() == PopulationEvent.Type.MIGRATION).count());
        assertEquals(2, arrivals); // One receiving opportunity in each known settlement.
        assertEquals(4, result.state().residents().stream().map(Resident::id).distinct().count());
    }

    @Test
    void validatesPolicyAndMigrationEventOrigins() {
        assertThrows(IllegalArgumentException.class, () -> new MigrationPolicy(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new MigrationPolicy(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new PopulationEvent(1, PopulationEvent.Type.MIGRATION,
                adult.id(), target, List.of(), "Move"));
        assertThrows(IllegalArgumentException.class, () -> new PopulationEvent(1, PopulationEvent.Type.MIGRATION,
                adult.id(), target, List.of(), "Move", target));
    }

    private PopulationStep.Outcome step(PopulationState state, MigrationPolicy policy, Map<UUID, String> worlds) {
        return PopulationMigration.advance(state, new SimulationRunner.Context(1, new Random(42)), 216, policy, worlds);
    }
    private Map<UUID, String> worlds() { return Map.of(source, "world", target, "world"); }
    private PopulationState family(int oldCapacity, int newCapacity) {
        return new PopulationState(List.of(adult, child), List.of(oldHome, newHome), List.of(tie),
                Map.of(oldHome.id(), oldCapacity, newHome.id(), newCapacity));
    }
}
