package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.ResidentProjectionRepository.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.blueninjas24.civilizations.application.ResidentProjectionPlan.*;
import static org.junit.jupiter.api.Assertions.*;

class ResidentProjectionPlanTest {
    private final UUID person = new UUID(0, 1), child = new UUID(0, 2), world = new UUID(0, 3),
            home = new UUID(0, 4), town = new UUID(0, 5), entity = new UUID(0, 6), old = new UUID(0, 7);
    private final Position position = new Position(0, 64, 0);
    private Resident resident(UUID id, Resident.Status status) { return new Resident(id, "Name", town, home, 240, status); }
    private Projection projection(State state) { return new Projection(person, world, entity, state, home, position); }
    private PopulationSession.ProjectionView view(List<Resident> residents, Map<UUID, Projection> projections, Map<EntityKey, UUID> aliases) {
        return new PopulationSession.ProjectionView(new PopulationState(residents, List.of(new Household(home, town, "Home")), List.of(), Map.of(home, 4)),
                new Snapshot(projections, aliases, Map.of(home, new Home(world, position))));
    }
    private Context context(boolean loaded, boolean checked, boolean nearby, int budget) {
        return new Context(loaded ? Set.of(new EntityKey(world, entity)) : Set.of(),
                checked ? Set.of(new LocationKey(world, position)) : Set.of(), nearby ? Set.of(home) : Set.of(), budget);
    }
    @Test void activeAbsenceNeverCreatesAnotherVillagerOrImpliesDeath() {
        var saved = view(List.of(resident(person, Resident.Status.ALIVE)), Map.of(person, projection(State.ACTIVE)), Map.of(new EntityKey(world, entity), person));
        assertTrue(plan(saved, context(false, false, false, 4)).isEmpty());
        assertTrue(plan(saved, context(false, true, true, 4)).isEmpty());
        assertEquals(Type.UPDATE, plan(saved, context(true, true, true, 4)).getFirst().type());
    }
    @Test void newbornRequiresNearbySafeLoadedHomeAndBoundedDeterministicBudget() {
        var saved = view(List.of(resident(child, Resident.Status.ALIVE), resident(person, Resident.Status.ALIVE)), Map.of(), Map.of());
        assertTrue(plan(saved, context(false, false, false, 4)).isEmpty());
        assertTrue(plan(saved, context(false, false, true, 0)).isEmpty());
        var actions = plan(saved, context(false, false, true, 1));
        assertEquals(1, actions.size()); assertEquals(Type.SPAWN, actions.getFirst().type()); assertEquals(person, actions.getFirst().residentId());
    }
    @Test void frozenPendingClaimCanRecoverOnlyWhenItsEntityOrFullyLoadedLocationIsObserved() {
        var saved = view(List.of(resident(person, Resident.Status.ALIVE)), Map.of(person, projection(State.CLAIMED)), Map.of(new EntityKey(world, entity), person));
        assertTrue(plan(saved, context(false, false, true, 4)).isEmpty());
        assertEquals(Type.RETIRE, plan(saved, context(false, true, true, 4)).getFirst().type());
        assertEquals(Type.ACTIVATE, plan(saved, context(true, true, true, 4)).getFirst().type());
    }
    @Test void deadResidentsAreRemovedAndRetiredWithoutResurrectionOrLostHistory() {
        var saved = view(List.of(resident(person, Resident.Status.DECEASED)), Map.of(person, projection(State.ACTIVE)), Map.of(new EntityKey(world, entity), person));
        assertEquals(List.of(Type.REMOVE, Type.RETIRE), plan(saved, context(true, true, true, 4)).stream().map(Action::type).toList());
        assertEquals(Type.RETIRE, plan(saved, context(false, false, false, 4)).getFirst().type());
        assertEquals(Resident.Status.DECEASED, saved.population().residents().getFirst().status());
    }
    @Test void migrationRetiresUnloadedIncarnationBeforePermittingDestinationSpawn() {
        var oldProjection = new Projection(person, world, entity, State.ACTIVE, UUID.randomUUID(), position);
        var saved = view(List.of(resident(person, Resident.Status.ALIVE)), Map.of(person, oldProjection), Map.of(new EntityKey(world, entity), person));
        assertEquals(List.of(Type.RETIRE), plan(saved, context(false, false, true, 4)).stream().map(Action::type).toList());
        var retired = view(saved.population().residents(), Map.of(person, new Projection(person, world, entity, State.RETIRED, oldProjection.householdId(), position)), saved.identities().aliases());
        assertEquals(Type.SPAWN, plan(retired, context(false, false, true, 4)).getFirst().type());
    }
    @Test void historicAliasesAreRemovedWhileAmbiguousAliasesAreQuarantined() {
        var aliases = Map.of(new EntityKey(world, entity), person, new EntityKey(world, old), person);
        var saved = view(List.of(resident(person, Resident.Status.ALIVE)), Map.of(person, projection(State.ACTIVE)), aliases);
        var oldLoaded = new Context(Set.of(new EntityKey(world, old)), Set.of(), Set.of(home), 4);
        assertEquals(Type.REMOVE, plan(saved, oldLoaded).getFirst().type());
        assertTrue(plan(view(saved.population().residents(), Map.of(), aliases), oldLoaded).isEmpty());
    }
}
