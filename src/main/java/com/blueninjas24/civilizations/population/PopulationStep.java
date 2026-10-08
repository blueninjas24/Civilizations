package com.blueninjas24.civilizations.population;

import com.blueninjas24.civilizations.simulation.SimulationRunner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Pure prototype: age, resolve deaths, then eligible births against remaining housing. */
public final class PopulationStep {
    private PopulationStep() {}

    public record Outcome(PopulationState state, List<PopulationEvent> events) {
        public Outcome { events = List.copyOf(events); }
    }

    public static Outcome advance(PopulationState state, SimulationRunner.Context context,
                                  PopulationPolicy policy) {
        if (context.stepNumber() <= 0) throw new IllegalArgumentException("Step must be positive");
        List<Resident> aged = PopulationAging.step(state.residents());
        List<Resident> people = new ArrayList<>();
        List<PopulationEvent> events = new ArrayList<>();
        Map<UUID, Resident> byId = new HashMap<>();
        Map<UUID, Integer> occupants = new HashMap<>();
        for (Resident person : aged) {
            if (person.status() == Resident.Status.ALIVE
                    && context.random().nextDouble() < policy.deathChance(person.ageSteps())) {
                person = new Resident(person.id(), person.name(), person.settlementId(), person.householdId(),
                        person.ageSteps(), Resident.Status.DECEASED);
                events.add(new PopulationEvent(context.stepNumber(), PopulationEvent.Type.DEATH,
                        person.id(), person.settlementId(), List.of(), "Age-dependent prototype mortality"));
            }
            people.add(person);
            byId.put(person.id(), person);
            if (person.status() == Resident.Status.ALIVE && person.householdId() != null) {
                occupants.merge(person.householdId(), 1, Math::addExact);
            }
        }
        List<FamilyTie> ties = new ArrayList<>(state.familyTies());
        Set<UUID> parentsThisStep = new HashSet<>();
        for (FamilyTie tie : state.familyTies()) {
            if (tie.type() != FamilyTie.Type.PARTNER) continue;
            Resident first = byId.get(tie.first());
            Resident second = byId.get(tie.second());
            UUID home = first.householdId();
            Integer capacity = home == null ? null : state.householdCapacity().get(home);
            if (!policy.eligibleParent(first) || !policy.eligibleParent(second) || home == null
                    || !home.equals(second.householdId()) || capacity == null
                    || occupants.getOrDefault(home, 0) >= capacity
                    || parentsThisStep.contains(first.id()) || parentsThisStep.contains(second.id())
                    || closeRelatives(first.id(), second.id(), state.familyTies())) continue;
            if (context.random().nextDouble() >= policy.birthChance()) continue;
            UUID id;
            do { id = new UUID(context.random().nextLong(), context.random().nextLong()); }
            while (byId.containsKey(id));
            Resident child = new Resident(id, "Resident " + id.toString().substring(0, 8),
                    first.settlementId(), home, 0, Resident.Status.ALIVE);
            people.add(child);
            byId.put(id, child);
            occupants.merge(home, 1, Math::addExact);
            parentsThisStep.add(first.id());
            parentsThisStep.add(second.id());
            ties.add(new FamilyTie(first.id(), id, FamilyTie.Type.PARENT_CHILD));
            ties.add(new FamilyTie(second.id(), id, FamilyTie.Type.PARENT_CHILD));
            events.add(new PopulationEvent(context.stepNumber(), PopulationEvent.Type.BIRTH, id,
                    first.settlementId(), List.of(first.id(), second.id()), "Eligible partners with available household capacity"));
        }
        return new Outcome(new PopulationState(people, state.households(), ties, state.householdCapacity()), events);
    }

    private static boolean closeRelatives(UUID first, UUID second, List<FamilyTie> ties) {
        Map<UUID, Set<UUID>> parents = new HashMap<>();
        for (FamilyTie tie : ties) {
            if (tie.type() == FamilyTie.Type.PARENT_CHILD) {
                parents.computeIfAbsent(tie.second(), ignored -> new HashSet<>()).add(tie.first());
            }
        }
        Set<UUID> firstParents = parents.getOrDefault(first, Set.of());
        Set<UUID> secondParents = parents.getOrDefault(second, Set.of());
        if (firstParents.stream().anyMatch(secondParents::contains)) return true;
        return ancestors(first, parents).contains(second) || ancestors(second, parents).contains(first);
    }

    private static Set<UUID> ancestors(UUID person, Map<UUID, Set<UUID>> parents) {
        Set<UUID> result = new HashSet<>();
        var queue = new ArrayDeque<>(parents.getOrDefault(person, Set.of()));
        while (!queue.isEmpty()) {
            UUID ancestor = queue.removeFirst();
            if (result.add(ancestor)) queue.addAll(parents.getOrDefault(ancestor, Set.of()));
        }
        return result;
    }
}
