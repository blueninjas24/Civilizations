package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import java.util.*;

/** Evidence-based needs, not personal rations, bank accounts or a complete prosperity score. */
public final class HouseholdNeeds {
    private HouseholdNeeds() {}
    public enum Housing { UNKNOWN, SUFFICIENT, CROWDED }
    public record Home(UUID household, long adults, long children, long foodDemand, Integer capacity) {
        public Home {
            Objects.requireNonNull(household);
            if (adults < 0 || children < 0 || foodDemand < 0 || (capacity != null && capacity < 0))
                throw new IllegalArgumentException("Invalid household needs");
        }
        public Housing housing() { return capacity == null ? Housing.UNKNOWN : Math.addExact(adults, children) > capacity ? Housing.CROWDED : Housing.SUFFICIENT; }
        public String explanation() {
            return "Household " + household + ": " + adults + " adults, " + children + " children; food demand " + foodDemand
                    + "; housing " + housing() + (capacity == null ? " (unobserved)" : " (capacity " + capacity + ")");
        }
    }
    public record Assessment(UUID settlement, long period, Condition sharedFood, long shortagePeriods,
                             long fulfilled, long demand, long unassignedResidents, List<Home> homes) {
        public Assessment {
            Objects.requireNonNull(settlement); Objects.requireNonNull(sharedFood); homes = List.copyOf(homes);
            if (period < 1 || shortagePeriods < 0 || fulfilled < 0 || demand < fulfilled || unassignedResidents < 0)
                throw new IllegalArgumentException("Invalid needs assessment");
        }
        public String explanation() {
            return "Needs at food period " + period + ": shared village food " + sharedFood + ", supplied " + fulfilled + "/" + demand
                    + ", shortage streak " + shortagePeriods + "; " + unassignedResidents + " living residents without a household."
                    + " Shared food coverage does not prove any family's individual ration or prosperity.";
        }
    }
    public static Assessment assess(UUID town, PopulationState population, Result foodPeriod, long adulthood, EconomySettings settings) {
        if (adulthood < 1) throw new IllegalArgumentException("Invalid adulthood");
        var food = foodPeriod.reports().stream().filter(r -> r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow();
        // This prototype has household food only; do not guess future industry/export ration priorities.
        if (food.demand() != food.flow().households()) throw new IllegalArgumentException("Mixed food uses require an explicit allocation rule");
        var counts = new HashMap<UUID, long[]>(); long unassigned = 0, adults = 0, children = 0;
        for (var person : population.residents()) {
            if (!person.settlementId().equals(town) || person.status() != Resident.Status.ALIVE) continue;
            boolean child = person.ageSteps() < adulthood;
            if (child) children++; else adults++;
            if (person.householdId() == null) unassigned++;
            else counts.computeIfAbsent(person.householdId(), ignored -> new long[2])[child ? 1 : 0]++;
        }
        if (settings.foodDemand(adults, children) != food.demand()) throw new IllegalArgumentException("Food period and saved residents disagree");
        var homes = new ArrayList<Home>();
        for (var household : population.households()) {
            var people = counts.get(household.id());
            if (!household.settlementId().equals(town) || people == null) continue;
            homes.add(new Home(household.id(), people[0], people[1], settings.foodDemand(people[0], people[1]), population.householdCapacity().get(household.id())));
        }
        homes.sort(Comparator.comparing(home -> home.household().toString()));
        return new Assessment(town, foodPeriod.state().completedPeriods(), food.condition(), food.after().shortagePeriods(),
                food.fulfilled(), food.demand(), unassigned, homes);
    }
}
