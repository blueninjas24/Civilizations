package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.ResidentProjectionRepository.*;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Pure reconciliation decisions. Loaded absence is evidence only for a frozen pending spawn. */
public final class ResidentProjectionPlan {
    private ResidentProjectionPlan() {}
    public enum Type { UPDATE, REMOVE, RETIRE, SPAWN, ACTIVATE }
    public record Action(Type type, UUID residentId, UUID entityId, String reason) {}
    public record LocationKey(UUID worldId, Position position) {}
    public record Context(Set<EntityKey> loadedEntities, Set<LocationKey> loadedClaimLocations,
                          Set<UUID> nearbySafeHomes, int spawnBudget) {
        public Context {
            loadedEntities = Set.copyOf(loadedEntities); loadedClaimLocations = Set.copyOf(loadedClaimLocations);
            nearbySafeHomes = Set.copyOf(nearbySafeHomes);
            if (spawnBudget < 0 || spawnBudget > 256) throw new IllegalArgumentException("Invalid projection spawn budget");
        }
    }

    public static List<Action> plan(PopulationSession.ProjectionView view, Context context) {
        var people = new HashMap<UUID, Resident>();
        view.population().residents().forEach(person -> people.put(person.id(), person));
        var identities = view.identities();
        var aliasedPeople = new HashSet<>(identities.aliases().values());
        var actions = new ArrayList<Action>();
        // Old aliases remain known forever; an obsolete loaded incarnation must never be imported anew.
        context.loadedEntities().stream().sorted(Comparator.comparing((EntityKey key) -> key.worldId().toString())
                .thenComparing(key -> key.entityId().toString())).forEach(entity -> {
            UUID id = identities.aliases().get(entity);
            if (id == null) return;
            Resident person = people.get(id); Projection projection = identities.projections().get(id);
            if (person == null || projection == null) return; // Broken/ambiguous identity requires diagnostics, not removal.
            boolean current = projection.worldId().equals(entity.worldId()) && projection.entityId().equals(entity.entityId());
            if (!current || projection.state() == State.RETIRED || person.status() == Resident.Status.DECEASED)
                actions.add(new Action(Type.REMOVE, id, entity.entityId(), "Obsolete or deceased incarnation; saved history remains"));
            else if (projection.state() == State.ACTIVE)
                actions.add(new Action(Type.UPDATE, id, entity.entityId(), "Apply saved name and age"));
            // CLAIMED entities remain frozen until their durable activation is acknowledged.
        });
        int births = 0;
        for (var person : view.population().residents().stream().sorted(Comparator.comparing(p -> p.id().toString())).toList()) {
            Projection projection = identities.projections().get(person.id());
            if (person.status() == Resident.Status.DECEASED) {
                if (projection != null && projection.state() != State.RETIRED)
                    actions.add(new Action(Type.RETIRE, person.id(), projection.entityId(), "Saved person is deceased"));
                continue;
            }
            if (projection != null && projection.state() != State.RETIRED) {
                if (!Objects.equals(person.householdId(), projection.householdId())) {
                    actions.add(new Action(Type.RETIRE, person.id(), projection.entityId(), "Saved household moved; retire old incarnation first"));
                } else if (projection.state() == State.CLAIMED) {
                    if (context.loadedEntities().contains(new EntityKey(projection.worldId(), projection.entityId())))
                        actions.add(new Action(Type.ACTIVATE, person.id(), projection.entityId(), "Claimed entity found after pending spawn or restart"));
                    else if (context.loadedClaimLocations().contains(new LocationKey(projection.worldId(), projection.position())))
                        actions.add(new Action(Type.RETIRE, person.id(), projection.entityId(), "Frozen claim absent from its fully loaded spawn location"));
                }
                // ACTIVE disappearance, including fully loaded absence, is never a death or respawn request.
                continue;
            }
            boolean unresolvedAlias = projection == null && aliasedPeople.contains(person.id());
            if (!unresolvedAlias && person.householdId() != null && identities.homes().containsKey(person.householdId())
                    && context.nearbySafeHomes().contains(person.householdId()) && births < context.spawnBudget()) {
                actions.add(new Action(Type.SPAWN, person.id(), null, "Living resident has a nearby loaded safe home and no current incarnation"));
                births++;
            }
        }
        return List.copyOf(actions);
    }
}
