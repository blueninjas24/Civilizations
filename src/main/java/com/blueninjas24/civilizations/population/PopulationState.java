package com.blueninjas24.civilizations.population;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable, explicitly observed housing capacity; missing observations are not zero capacity. */
public record PopulationState(List<Resident> residents, List<Household> households,
                              List<FamilyTie> familyTies, Map<UUID, Integer> householdCapacity) {
    public PopulationState {
        residents = residents.stream().sorted(Comparator.comparing(p -> p.id().toString())).toList();
        households = households.stream().sorted(Comparator.comparing(h -> h.id().toString())).toList();
        familyTies = familyTies.stream().sorted(Comparator.comparing((FamilyTie t) -> t.type().name())
                .thenComparing(t -> t.first().toString()).thenComparing(t -> t.second().toString())).toList();
        householdCapacity = Map.copyOf(householdCapacity);
        Map<UUID, Household> homes = new HashMap<>();
        for (Household home : households) {
            if (homes.put(home.id(), home) != null) {
                throw new IllegalArgumentException("Duplicate household identity");
            }
        }
        Map<UUID, Resident> people = new HashMap<>();
        for (Resident person : residents) {
            if (people.put(person.id(), person) != null) {
                throw new IllegalArgumentException("Duplicate resident identity");
            }
            if (person.householdId() != null && (!homes.containsKey(person.householdId())
                    || !homes.get(person.householdId()).settlementId().equals(person.settlementId()))) {
                throw new IllegalArgumentException("Invalid resident household assignment");
            }
        }
        if (new HashSet<>(familyTies).size() != familyTies.size()) {
            throw new IllegalArgumentException("Duplicate family relationship");
        }
        for (FamilyTie tie : familyTies) {
            if (!people.containsKey(tie.first()) || !people.containsKey(tie.second())) {
                throw new IllegalArgumentException("Unknown family member");
            }
        }
        // Topological validation rejects ancestry cycles even in caller-created headless input.
        Map<UUID, Integer> parentCounts = new HashMap<>();
        Map<UUID, List<UUID>> children = new HashMap<>();
        for (FamilyTie tie : familyTies) {
            if (tie.type() == FamilyTie.Type.PARENT_CHILD) {
                parentCounts.merge(tie.second(), 1, Math::addExact);
                children.computeIfAbsent(tie.first(), ignored -> new java.util.ArrayList<>()).add(tie.second());
            }
        }
        var roots = new java.util.ArrayDeque<UUID>();
        people.keySet().stream().filter(id -> !parentCounts.containsKey(id)).forEach(roots::add);
        int visited = 0;
        while (!roots.isEmpty()) {
            UUID parent = roots.removeFirst();
            visited++;
            for (UUID child : children.getOrDefault(parent, List.of())) {
                if (parentCounts.merge(child, -1, Integer::sum) == 0) roots.add(child);
            }
        }
        if (visited != people.size()) throw new IllegalArgumentException("Ancestry cycle");
        for (var capacity : householdCapacity.entrySet()) {
            if (!homes.containsKey(capacity.getKey()) || capacity.getValue() < 0) {
                throw new IllegalArgumentException("Invalid observed housing capacity");
            }
        }
    }

    public long livingCount() {
        return residents.stream().filter(p -> p.status() == Resident.Status.ALIVE).count();
    }
}
