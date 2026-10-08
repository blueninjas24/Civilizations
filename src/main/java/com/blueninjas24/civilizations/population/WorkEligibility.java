package com.blueninjas24.civilizations.population;

import java.util.ArrayList;
import java.util.List;

/** Explainable qualifications only. No salaries, employer selection or automatic hiring. */
public final class WorkEligibility {
    private WorkEligibility() {}
    public record Result(List<String> unmetRequirements) {
        public Result { unmetRequirements = List.copyOf(unmetRequirements); }
        public boolean eligible() { return unmetRequirements.isEmpty(); }
    }

    public static Result evaluate(Resident person, WorkProfile profile, JobRequirements requirements) {
        if (!person.id().equals(profile.residentId())) throw new IllegalArgumentException("Work profile belongs to another resident");
        var reasons = new ArrayList<String>();
        if (person.status() != Resident.Status.ALIVE) reasons.add("Resident is deceased");
        if (person.ageSteps() < requirements.minimumAgeSteps()) reasons.add("Minimum working age: " + requirements.minimumAgeSteps() + " logical steps");
        requirements.minimumExperience().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(skill -> {
            long actual = profile.experience().getOrDefault(skill.getKey(), 0L);
            if (actual < skill.getValue()) reasons.add(skill.getKey() + ": requires " + skill.getValue() + ", has " + actual);
        });
        return new Result(reasons);
    }
}
