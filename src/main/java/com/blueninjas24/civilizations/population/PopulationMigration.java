package com.blueninjas24.civilizations.population;

import com.blueninjas24.civilizations.simulation.SimulationRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Data-only movement. Whole living households move together; unknown worlds/capacity block moves. */
public final class PopulationMigration {
    private PopulationMigration() {}

    public static PopulationStep.Outcome advance(PopulationState state, SimulationRunner.Context context,
                                                long adulthoodSteps, MigrationPolicy policy,
                                                Map<UUID, String> settlementWorlds) {
        if (context.stepNumber() <= 0 || adulthoodSteps < 0) throw new IllegalArgumentException("Invalid migration step/age");
        var people = new HashMap<UUID, Resident>();
        var occupants = new HashMap<UUID, Integer>();
        var processed = new HashSet<UUID>();
        var events = new ArrayList<PopulationEvent>();
        for (Resident person : state.residents()) {
            people.put(person.id(), person);
            if (person.status() == Resident.Status.ALIVE && person.householdId() != null)
                occupants.merge(person.householdId(), 1, Math::addExact);
        }
        if (policy.moveChance() > 0) for (Resident original : state.residents()) {
            Resident person = people.get(original.id());
            if (processed.contains(person.id()) || person.status() != Resident.Status.ALIVE || person.ageSteps() < adulthoodSteps) continue;
            UUID oldHome = person.householdId();
            Integer capacity = oldHome == null ? null : state.householdCapacity().get(oldHome);
            if (oldHome != null && (capacity == null || occupants.getOrDefault(oldHome, 0) <= capacity)) continue;
            List<Resident> group = oldHome == null ? List.of(person) : state.residents().stream()
                    .map(p -> people.get(p.id())).filter(p -> p.status() == Resident.Status.ALIVE && oldHome.equals(p.householdId())).toList();
            // One chance for this pressured household; never split a family because the first member failed a roll.
            group.forEach(p -> processed.add(p.id()));
            String world = settlementWorlds.get(person.settlementId());
            if (world == null || world.isBlank()) continue;
            var destinations = state.households().stream().filter(h -> !h.settlementId().equals(person.settlementId())
                    && world.equals(settlementWorlds.get(h.settlementId()))
                    && occupants.getOrDefault(h.id(), 0) == 0
                    && state.householdCapacity().getOrDefault(h.id(), 0) >= group.size()).toList();
            if (destinations.isEmpty() || context.random().nextDouble() >= policy.moveChance()) continue;
            Household destination = destinations.get(context.random().nextInt(destinations.size()));
            for (Resident member : group) {
                Resident moved = new Resident(member.id(), member.name(), destination.settlementId(), destination.id(),
                        member.ageSteps(), member.status());
                people.put(moved.id(), moved);
                events.add(new PopulationEvent(context.stepNumber(), PopulationEvent.Type.MIGRATION, member.id(),
                        destination.settlementId(), List.of(), "Housing pressure; whole living household moved into observed vacant housing",
                        member.settlementId()));
            }
            if (oldHome != null) occupants.put(oldHome, 0);
            occupants.put(destination.id(), group.size());
        }
        if (policy.externalArrivalChance() > 0) {
            var settlements = state.households().stream().map(Household::settlementId).distinct()
                    .sorted(java.util.Comparator.comparing(UUID::toString)).toList();
            for (UUID settlement : settlements) {
                String world = settlementWorlds.get(settlement);
                if (world == null || world.isBlank()) continue;
                var available = state.households().stream().filter(h -> h.settlementId().equals(settlement)
                        && state.householdCapacity().getOrDefault(h.id(), 0) > occupants.getOrDefault(h.id(), 0)).toList();
                if (available.isEmpty() || context.random().nextDouble() >= policy.externalArrivalChance()) continue;
                Household destination = available.get(context.random().nextInt(available.size()));
                UUID id;
                do { id = new UUID(context.random().nextLong(), context.random().nextLong()); } while (people.containsKey(id));
                Resident arrival = new Resident(id, "Resident " + id.toString().substring(0, 8), settlement,
                        destination.id(), adulthoodSteps, Resident.Status.ALIVE);
                people.put(id, arrival);
                occupants.merge(destination.id(), 1, Math::addExact);
                events.add(new PopulationEvent(context.stepNumber(), PopulationEvent.Type.ARRIVAL, id, settlement,
                        List.of(), "Occasional external adult arrival with observed household space"));
            }
        }
        return new PopulationStep.Outcome(new PopulationState(new ArrayList<>(people.values()), state.households(),
                state.familyTies(), state.householdCapacity()), events);
    }
}
