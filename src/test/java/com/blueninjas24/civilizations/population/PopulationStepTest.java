package com.blueninjas24.civilizations.population;

import com.blueninjas24.civilizations.simulation.SimulationCheckpoint;
import com.blueninjas24.civilizations.simulation.SimulationRunner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.blueninjas24.civilizations.population.FamilyTie.Type.*;
import static org.junit.jupiter.api.Assertions.*;

class PopulationStepTest {
    private final UUID settlement = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final Household home = new Household(UUID.fromString("00000000-0000-0000-0000-000000000002"), settlement, "Home");
    private final Resident first = person(3, 240, home.id(), Resident.Status.ALIVE);
    private final Resident second = person(4, 240, home.id(), Resident.Status.ALIVE);

    @Test void pausedSettlementRetainsAgesFamiliesAndLifeEvenUnderCertainBirthAndDeathRules() {
        var before=couple(4);
        var result=PopulationStep.advance(before,new SimulationRunner.Context(1,new Random(42)),policy(1,1),java.util.Set.of());
        assertEquals(before,result.state());assertTrue(result.events().isEmpty());
        var migration=PopulationMigration.advance(before,new SimulationRunner.Context(1,new Random(42)),216,
                new MigrationPolicy(1,1),Map.of(settlement,"world"),java.util.Set.of());
        assertEquals(before,migration.state());assertTrue(migration.events().isEmpty());
    }

    @Test
    void eligibleBirthHasPermanentIdentityTwoParentsAndStructuredReason() {
        PopulationState before = couple(4);
        var result = step(before, policy(1, 0));
        assertEquals(3, result.state().livingCount());
        assertEquals(1, result.events().size());
        PopulationEvent event = result.events().getFirst();
        assertEquals(PopulationEvent.Type.BIRTH, event.type());
        assertEquals(List.of(first.id(), second.id()), event.relatedResidents());
        Resident child = result.state().residents().stream().filter(p -> p.id().equals(event.residentId())).findFirst().orElseThrow();
        assertEquals(0, child.ageSteps());
        assertEquals(home.id(), child.householdId());
        assertEquals(settlement, event.settlementId());
        assertEquals(2, result.state().familyTies().stream().filter(t -> t.second().equals(child.id())).count());
        assertEquals(2, before.livingCount());
        assertEquals(step(before, policy(1, 0)), result);
    }

    @Test
    void noBirthForFullUnknownOrUnassignedHousing() {
        assertEquals(2, step(couple(2), policy(1, 0)).state().livingCount());
        var unknown = new PopulationState(List.of(first, second), List.of(home), ties(), Map.of());
        assertEquals(2, step(unknown, policy(1, 0)).state().livingCount());
        var unassigned = new PopulationState(List.of(person(3, 240, null, Resident.Status.ALIVE),
                person(4, 240, null, Resident.Status.ALIVE)), List.of(home), ties(), Map.of(home.id(), 4));
        assertEquals(2, step(unassigned, policy(1, 0)).state().livingCount());
    }

    @Test
    void immatureOldDeceasedOrSeparatedPartnersCannotHaveChildren() {
        for (Resident candidate : List.of(person(3, 0, home.id(), Resident.Status.ALIVE),
                person(3, 600, home.id(), Resident.Status.ALIVE),
                person(3, 240, home.id(), Resident.Status.DECEASED),
                person(3, 240, null, Resident.Status.ALIVE))) {
            var state = new PopulationState(List.of(candidate, second), List.of(home), ties(), Map.of(home.id(), 4));
            assertTrue(step(state, policy(1, 0)).events().isEmpty());
        }
    }

    @Test
    void deathsRetainIdentityAgeFamilyAndAreNeverRepeated() {
        var result = step(couple(4), policy(1, 1));
        assertEquals(0, result.state().livingCount());
        assertEquals(2, result.state().residents().size());
        assertEquals(2, result.events().size());
        assertTrue(result.events().stream().allMatch(e -> e.type() == PopulationEvent.Type.DEATH));
        assertEquals(ties(), result.state().familyTies());
        var later = step(result.state(), policy(1, 1));
        assertEquals(result.state(), later.state());
        assertTrue(later.events().isEmpty());
    }

    @Test
    void backgroundMortalityDoesNotKillEveryoneWhenDisabledAndIncreasesWithAge() {
        assertTrue(step(couple(4), policy(0, 0)).events().isEmpty());
        PopulationPolicy prototype = PopulationPolicy.prototype();
        assertEquals(0.0001, prototype.deathChance(20 * 12));
        assertTrue(prototype.deathChance(80 * 12) > prototype.deathChance(60 * 12));
        assertEquals(1, prototype.deathChance(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> prototype.deathChance(-1));
    }

    @Test
    void relatedPartnersAndResidentsWithoutPartnerLinksNeverProduceChildren() {
        List<FamilyTie> parentPartner = new ArrayList<>(ties());
        parentPartner.add(new FamilyTie(first.id(), second.id(), PARENT_CHILD));
        assertTrue(step(new PopulationState(List.of(first, second), List.of(home), parentPartner,
                Map.of(home.id(), 4)), policy(1, 0)).events().isEmpty());
        Resident parent = person(5, 700, home.id(), Resident.Status.DECEASED);
        var siblingTies = new ArrayList<>(ties());
        siblingTies.add(new FamilyTie(parent.id(), first.id(), PARENT_CHILD));
        siblingTies.add(new FamilyTie(parent.id(), second.id(), PARENT_CHILD));
        assertTrue(step(new PopulationState(List.of(first, second, parent), List.of(home), siblingTies,
                Map.of(home.id(), 4)), policy(1, 0)).events().isEmpty());
        assertTrue(step(new PopulationState(List.of(first, second), List.of(home), List.of(),
                Map.of(home.id(), 4)), policy(1, 0)).events().isEmpty());
    }

    @Test
    void stableOrderingAndBatchingReplayTheSameDemographicState() {
        var reversed = new PopulationState(List.of(second, first), List.of(home), ties(), Map.of(home.id(), 10));
        var checkpoint = new SimulationCheckpoint<>(42, 0, reversed);
        SimulationRunner.Step<PopulationState> step = (state, context) -> PopulationStep.advance(state, context, policy(0.25, 0.01)).state();
        var all = SimulationRunner.advance(checkpoint, 1200, 1200, step);
        var partial = SimulationRunner.advance(new SimulationCheckpoint<>(42, 0, couple(10)), 1200, 7, step);
        while (!partial.complete()) partial = SimulationRunner.advance(partial.checkpoint(), 1200, 13, step);
        assertEquals(all.checkpoint(), partial.checkpoint());
        assertEquals(all.checkpoint(), SimulationRunner.advance(checkpoint, 1200, 1200, step).checkpoint());
        assertTrue(all.checkpoint().state().residents().size() >= 2);
    }

    @Test
    void populationAccountingEqualsBirthsMinusDeathsAndNeverExceedsHousingOnBirth() {
        PopulationState state = couple(4);
        long births = 0;
        long deaths = 0;
        for (int step = 1; step <= 1200; step++) {
            var outcome = PopulationStep.advance(state, new SimulationRunner.Context(step, new Random(step)), policy(0.4, 0.01));
            births += outcome.events().stream().filter(e -> e.type() == PopulationEvent.Type.BIRTH).count();
            deaths += outcome.events().stream().filter(e -> e.type() == PopulationEvent.Type.DEATH).count();
            state = outcome.state();
            assertEquals(2 + births - deaths, state.livingCount());
            assertTrue(state.livingCount() <= 4);
        }
        assertTrue(births > 0);
        assertTrue(deaths > 0);
        assertEquals(2 + births, state.residents().size()); // No deletion of the deceased.
    }

    @Test
    void validatesPolicyAndSnapshotReferencesIncludingCycles() {
        assertThrows(IllegalArgumentException.class, () -> policy(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> policy(0, 1.1));
        assertThrows(IllegalArgumentException.class, () -> new PopulationPolicy(0, 1, 2, 3, 4, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PopulationState(List.of(first, first), List.of(home), List.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new PopulationState(List.of(first), List.of(), List.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new PopulationState(List.of(first), List.of(home), ties(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new PopulationState(List.of(first, second), List.of(home),
                List.of(new FamilyTie(first.id(), second.id(), PARENT_CHILD), new FamilyTie(second.id(), first.id(), PARENT_CHILD)), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new PopulationState(List.of(first), List.of(home), List.of(), Map.of(home.id(), -1)));
        assertThrows(IllegalArgumentException.class, () -> step(couple(4), policy(-0.1, 0)));
        assertEquals(0, step(new PopulationState(List.of(), List.of(), List.of(), Map.of()), policy(1, 1)).state().livingCount());
    }

    private PopulationStep.Outcome step(PopulationState state, PopulationPolicy policy) {
        return PopulationStep.advance(state, new SimulationRunner.Context(1, new Random(42)), policy);
    }

    @Test
    void structuredEventsRejectMissingDuplicatedOrSelfParentIdentities() {
        assertThrows(IllegalArgumentException.class, () -> new PopulationEvent(1, PopulationEvent.Type.BIRTH,
                first.id(), settlement, List.of(second.id()), "Birth"));
        assertThrows(IllegalArgumentException.class, () -> new PopulationEvent(1, PopulationEvent.Type.BIRTH,
                first.id(), settlement, List.of(second.id(), second.id()), "Birth"));
        assertThrows(IllegalArgumentException.class, () -> new PopulationEvent(1, PopulationEvent.Type.BIRTH,
                first.id(), settlement, List.of(first.id(), second.id()), "Birth"));
        assertThrows(IllegalArgumentException.class, () -> new PopulationEvent(1, PopulationEvent.Type.DEATH,
                first.id(), settlement, List.of(second.id()), "Death"));
        assertThrows(IllegalArgumentException.class, () -> new PopulationEvent(1, PopulationEvent.Type.DEATH,
                first.id(), settlement, List.of(), " "));
    }

    private PopulationPolicy policy(double birthChance, double deathChance) {
        return new PopulationPolicy(12, 216, 240, 480, 720, birthChance, deathChance, 0);
    }

    private PopulationState couple(int capacity) {
        return new PopulationState(List.of(first, second), List.of(home), ties(), Map.of(home.id(), capacity));
    }

    private List<FamilyTie> ties() { return List.of(new FamilyTie(first.id(), second.id(), PARTNER)); }

    private Resident person(long id, long age, UUID household, Resident.Status status) {
        return new Resident(new UUID(0, id), "Test", settlement, household, age, status);
    }
}
