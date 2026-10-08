package com.blueninjas24.civilizations.population;

import com.blueninjas24.civilizations.simulation.SimulationRunner;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PartnershipFormationTest {
    private static UUID id(int n) { return new UUID(0, n); }
    private static final UUID SETTLEMENT = id(100), HOME = id(200);
    private static Resident adult(int n) { return new Resident(id(n), "Resident " + n, SETTLEMENT, HOME, 240, Resident.Status.ALIVE); }
    private static PopulationState state(List<Resident> people, List<FamilyTie> ties) {
        return new PopulationState(people, List.of(new Household(HOME, SETTLEMENT, "Home")), ties, Map.of(HOME, 10));
    }
    private static PartnershipFormation.Outcome form(PopulationState state) {
        return PartnershipFormation.form(state, new PartnershipFormation.Policy(true, 216));
    }

    @Test void deterministicPairsAreExclusiveAndRepeatingDoesNotAddMore() {
        var source = state(List.of(adult(4), adult(2), adult(5), adult(3), adult(1)), List.of());
        var paired = form(source);
        assertEquals(List.of(new FamilyTie(id(1), id(2), FamilyTie.Type.PARTNER),
                new FamilyTie(id(3), id(4), FamilyTie.Type.PARTNER)), paired.addedPartnerships());
        assertTrue(form(paired.state()).addedPartnerships().isEmpty());
        assertTrue(source.familyTies().isEmpty());
        assertEquals(source.residents(), paired.state().residents());
        assertThrows(UnsupportedOperationException.class, () -> paired.addedPartnerships().clear());
    }

    @Test void disabledAgeUnassignedAndDeceasedRulesDoNotInventFamilies() {
        var child = new Resident(id(2), "Child", SETTLEMENT, HOME, 20, Resident.Status.ALIVE);
        var homeless = new Resident(id(3), "Unassigned", SETTLEMENT, null, 240, Resident.Status.ALIVE);
        var dead = new Resident(id(4), "Deceased", SETTLEMENT, HOME, 240, Resident.Status.DECEASED);
        var source = state(List.of(adult(1), child, homeless, dead), List.of());
        assertTrue(form(source).addedPartnerships().isEmpty());
        assertEquals(source, PartnershipFormation.form(source, new PartnershipFormation.Policy(false, 216)).state());
        assertThrows(IllegalArgumentException.class, () -> new PartnershipFormation.Policy(true, -1));
    }

    @Test void knownAncestryAndSiblingsBlockPairingWithoutAssigningChildParents() {
        var ancestry = List.of(new FamilyTie(id(1), id(2), FamilyTie.Type.PARENT_CHILD),
                new FamilyTie(id(1), id(3), FamilyTie.Type.PARENT_CHILD));
        assertTrue(form(state(List.of(adult(1), adult(2), adult(3)), ancestry)).addedPartnerships().isEmpty());
        var unknownChild = new Resident(id(4), "Unknown ancestry", SETTLEMENT, HOME, 20, Resident.Status.ALIVE);
        var result = form(state(List.of(adult(2), adult(3), unknownChild), List.of()));
        assertEquals(1, result.addedPartnerships().size());
        assertTrue(result.state().familyTies().stream().noneMatch(t -> t.type() == FamilyTie.Type.PARENT_CHILD));
    }

    @Test void sharedGrandparentsAlsoBlockCousinPairing() {
        var ancestors = List.of(new FamilyTie(id(1), id(2), FamilyTie.Type.PARENT_CHILD),
                new FamilyTie(id(1), id(3), FamilyTie.Type.PARENT_CHILD),
                new FamilyTie(id(2), id(4), FamilyTie.Type.PARENT_CHILD),
                new FamilyTie(id(3), id(5), FamilyTie.Type.PARENT_CHILD));
        var people = List.of(1, 2, 3).stream().map(n -> new Resident(id(n), "Ancestor", SETTLEMENT, HOME, 240, Resident.Status.DECEASED))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        people.add(adult(4)); people.add(adult(5));
        assertTrue(form(state(people, ancestors)).addedPartnerships().isEmpty());
    }

    @Test void existingLivingPartnersStayExclusiveAcrossSeparateHomes() {
        UUID other = id(201);
        var relocated = new Resident(id(2), "Partner", SETTLEMENT, other, 240, Resident.Status.ALIVE);
        var source = new PopulationState(List.of(adult(1), relocated, adult(3)),
                List.of(new Household(HOME, SETTLEMENT, "Home"), new Household(other, SETTLEMENT, "Other")),
                List.of(new FamilyTie(id(1), id(2), FamilyTie.Type.PARTNER)), Map.of());
        assertTrue(form(source).addedPartnerships().isEmpty());
    }

    @Test void deceasedPartnersRemainHistoryWhileSurvivorCanPairAgain() {
        var dead = new Resident(id(2), "Former partner", SETTLEMENT, HOME, 240, Resident.Status.DECEASED);
        var old = new FamilyTie(id(1), id(2), FamilyTie.Type.PARTNER);
        var result = form(state(List.of(adult(1), dead, adult(3)), List.of(old)));
        assertEquals(List.of(new FamilyTie(id(1), id(3), FamilyTie.Type.PARTNER)), result.addedPartnerships());
        assertTrue(result.state().familyTies().contains(old));
    }

    @Test void approvedPairsEnableBirthsThroughExistingDemographicRules() {
        var paired = form(state(List.of(adult(1), adult(2)), List.of())).state();
        var policy = new PopulationPolicy(12, 216, 240, 480, 720, 1, 0, 0);
        var outcome = PopulationStep.advance(paired, new SimulationRunner.Context(1, new Random(42)), policy);
        assertEquals(3, outcome.state().livingCount());
        assertEquals(PopulationEvent.Type.BIRTH, outcome.events().getFirst().type());
        assertEquals(2, outcome.state().familyTies().stream().filter(t -> t.type() == FamilyTie.Type.PARENT_CHILD).count());
    }
}
