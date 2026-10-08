package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.simulation.CivilizationTime;
import java.util.List;
import java.util.UUID;

/** Read-only text from immutable worker snapshots; no SQL or world access. */
public final class PopulationDiagnostics {
    private PopulationDiagnostics() {}

    public static List<String> status(PopulationSession.Status status, PopulationSession.Settings settings) {
        var date = CivilizationTime.date(status.completedMonths(), settings.population().stepsPerYear());
        return List.of("Civilizations population: " + (status.ready() ? "running" : "not ready"),
                "Calendar: year " + date.year() + ", month " + date.month()
                        + "; completed " + status.completedMonths() + ", queued " + status.remainingMonths() + " months",
                "Saved living residents: " + status.livingPeople(),
                "Last batch: " + String.format(java.util.Locale.ROOT, "%.3f", status.lastBatchNanos() / 1_000_000.0) + " ms",
                "Prototype pace: " + settings.millisPerMonth() + " real ms/month; batch cap " + settings.monthsPerBatch(),
                "Last failure: " + (status.failure() == null ? "none" : status.failure()));
    }

    public static List<String> settlement(PopulationState state, UUID id) {
        var people = state.residents().stream().filter(p -> p.settlementId().equals(id)).toList();
        var homes = state.households().stream().filter(h -> h.settlementId().equals(id)).toList();
        if (people.isEmpty() && homes.isEmpty()) return List.of("No population records for settlement " + id);
        long living = people.stream().filter(p -> p.status() == Resident.Status.ALIVE).count();
        long observed = homes.stream().filter(h -> state.householdCapacity().containsKey(h.id())).count();
        long capacity = homes.stream().mapToLong(h -> state.householdCapacity().getOrDefault(h.id(), 0)).sum();
        return List.of("Settlement " + id,
                "Saved living residents: " + living + "; retained deceased: " + (people.size() - living),
                "Households: " + homes.size() + "; observed housing: " + observed + "/" + homes.size(),
                "Known housing capacity: " + capacity + " (unobserved homes are unknown)");
    }
}
