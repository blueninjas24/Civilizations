package com.blueninjas24.civilizations.population;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Explicit prototype pairing of unpartnered, unrelated adults sharing a household. */
public final class PartnershipFormation {
    private PartnershipFormation() {}
    public record Policy(boolean enabled, long minimumAgeSteps) {
        public Policy {
            if (minimumAgeSteps < 0) throw new IllegalArgumentException("Minimum partner age must be nonnegative");
        }
    }
    public record Outcome(PopulationState state, List<FamilyTie> addedPartnerships) {
        public Outcome { addedPartnerships = List.copyOf(addedPartnerships); }
    }

    public static Outcome form(PopulationState state, Policy policy) {
        if (!policy.enabled()) return new Outcome(state, List.of());
        var byId = new HashMap<UUID, Resident>();
        state.residents().forEach(person -> byId.put(person.id(), person));
        var partnered = new HashSet<UUID>();
        Map<UUID, Set<UUID>> parents = new HashMap<>();
        for (var tie : state.familyTies()) {
            if (tie.type() == FamilyTie.Type.PARENT_CHILD) {
                parents.computeIfAbsent(tie.second(), ignored -> new HashSet<>()).add(tie.first());
            } else if (byId.get(tie.first()).status() == Resident.Status.ALIVE
                    && byId.get(tie.second()).status() == Resident.Status.ALIVE) {
                // Even partners currently living apart must not be paired again.
                partnered.add(tie.first()); partnered.add(tie.second());
            }
        }
        var ancestry = new HashMap<UUID, Set<UUID>>();
        var eligible = state.residents().stream().filter(person -> person.status() == Resident.Status.ALIVE
                && person.ageSteps() >= policy.minimumAgeSteps() && person.householdId() != null
                && !partnered.contains(person.id())).toList();
        for (var person : eligible) {
            var ancestors = new HashSet<UUID>();
            var pending = new ArrayDeque<>(parents.getOrDefault(person.id(), Set.of()));
            while (!pending.isEmpty()) {
                UUID ancestor = pending.removeFirst();
                if (ancestors.add(ancestor)) pending.addAll(parents.getOrDefault(ancestor, Set.of()));
            }
            ancestry.put(person.id(), ancestors);
        }
        var additions = new ArrayList<FamilyTie>();
        for (int first = 0; first < eligible.size(); first++) {
            var a = eligible.get(first);
            if (partnered.contains(a.id())) continue;
            for (int second = first + 1; second < eligible.size(); second++) {
                var b = eligible.get(second);
                if (partnered.contains(b.id()) || !a.householdId().equals(b.householdId())) continue;
                Set<UUID> aAncestors = ancestry.get(a.id()), bAncestors = ancestry.get(b.id());
                if (aAncestors.contains(b.id()) || bAncestors.contains(a.id())
                        || aAncestors.stream().anyMatch(bAncestors::contains)) continue;
                additions.add(new FamilyTie(a.id(), b.id(), FamilyTie.Type.PARTNER));
                partnered.add(a.id()); partnered.add(b.id());
                break;
            }
        }
        var ties = new ArrayList<>(state.familyTies());
        ties.addAll(additions);
        return new Outcome(new PopulationState(state.residents(), state.households(), ties, state.householdCapacity()), additions);
    }
}
