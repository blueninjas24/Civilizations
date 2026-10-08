package com.blueninjas24.civilizations.population;

import java.util.List;

/** Pure logical aging only. No fertility, mortality, calendar or migration policy. */
public final class PopulationAging {
    private PopulationAging() {}

    public static List<Resident> step(List<Resident> residents) {
        return residents.stream().map(person -> person.status() == Resident.Status.DECEASED ? person
                : new Resident(person.id(), person.name(), person.settlementId(), person.householdId(),
                Math.incrementExact(person.ageSteps()), person.status())).toList();
    }
}
