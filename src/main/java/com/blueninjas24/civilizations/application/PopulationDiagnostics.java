package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.simulation.CivilizationTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

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

    public static List<String> explain(PopulationSession.ProjectionView view, UUID id) {
        var lines = new ArrayList<>(settlement(view.population(), id));
        if (lines.getFirst().startsWith("No population records")) return List.copyOf(lines);
        var people = new java.util.HashMap<UUID, Resident>();
        view.population().residents().forEach(person -> people.put(person.id(), person));
        lines.add("Housing observations can become unknown after a partial scan; unknown capacity blocks births and moves.");
        lines.add("Recent saved outcomes (bounded global cache; not a complete history):");
        int shown = 0;
        for (var stored : view.recentEvents().reversed()) {
            var event = stored.event();
            if (!event.settlementId().equals(id) && !id.equals(event.sourceSettlementId())) continue;
            var person = people.get(event.residentId());
            lines.add("Month " + event.step() + ": " + event.type() + " "
                    + (person == null ? event.residentId() : person.name()) + " — " + event.reason()
                    + (event.sourceSettlementId() == null ? "" : "; from " + event.sourceSettlementId() + " to " + event.settlementId()));
            if (++shown == 10) break;
        }
        for (var event : view.recentDeaths()) {
            var person = people.get(event.residentId());
            if (person == null || !person.settlementId().equals(id)) continue;
            lines.add("Month " + event.completedMonth() + ": physical death of " + person.name() + " — " + event.reason());
            if (++shown >= 20) break;
        }
        if (shown == 0) lines.add("No outcomes for this settlement in the recent cache. Random chances do not promise an event every month.");
        long ambiguous = people.values().stream().filter(person -> person.settlementId().equals(id))
                .filter(person -> !view.identities().projections().containsKey(person.id()))
                .filter(person -> view.identities().aliases().containsValue(person.id())).count();
        lines.add("Unresolved physical identities: " + ambiguous + "; these cannot spawn replacements automatically.");
        return List.copyOf(lines);
    }
}
