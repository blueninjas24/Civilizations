package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import java.util.*;
import java.math.BigInteger;

/** Evidence-based needs, not personal rations, bank accounts or a complete prosperity score. */
public final class HouseholdNeeds {
    private HouseholdNeeds() {}
    public enum Housing { UNKNOWN, SUFFICIENT, CROWDED }
    public record Home(UUID household, long adults, long children, long foodDemand, Integer capacity, Long supplied, Long unmetPeriods) {
        public Home(UUID household, long adults, long children, long foodDemand, Integer capacity) {
            this(household, adults, children, foodDemand, capacity, null, null);
        }
        public Home {
            Objects.requireNonNull(household);
            if (adults < 0 || children < 0 || foodDemand < 0 || (capacity != null && capacity < 0))
                throw new IllegalArgumentException("Invalid household needs");
            if ((supplied == null) != (unmetPeriods == null) || (supplied != null &&
                    (supplied < 0 || supplied > foodDemand || unmetPeriods < 0 || (supplied == foodDemand && unmetPeriods != 0))))
                throw new IllegalArgumentException("Invalid household food allocation");
        }
        public Housing housing() { return capacity == null ? Housing.UNKNOWN : Math.addExact(adults, children) > capacity ? Housing.CROWDED : Housing.SUFFICIENT; }
        public String foodCoverage() { return supplied == null ? "UNKNOWN" : supplied == foodDemand ? "SUPPLIED" : "UNDERSUPPLIED"; }
        public String explanation() {
            return "Household " + household + ": " + adults + " adults, " + children + " children; food demand " + foodDemand
                    + "; simulated food " + foodCoverage() + (supplied == null ? " (historical allocation unknown)" : " (" + supplied + "/" + foodDemand + ", unmet streak " + unmetPeriods + ")")
                    + "; housing " + housing() + (capacity == null ? " (unobserved)" : " (capacity " + capacity + ")");
        }
    }
    public record Assessment(UUID settlement, long period, Condition sharedFood, long shortagePeriods,
                             long fulfilled, long demand, long unassignedResidents, List<Home> homes,
                             Long unassignedDemand, Long unassignedSupplied) {
        public Assessment(UUID settlement, long period, Condition sharedFood, long shortagePeriods,
                          long fulfilled, long demand, long unassignedResidents, List<Home> homes) {
            this(settlement, period, sharedFood, shortagePeriods, fulfilled, demand, unassignedResidents, homes, null, null);
        }
        public Assessment {
            Objects.requireNonNull(settlement); Objects.requireNonNull(sharedFood); homes = List.copyOf(homes);
            if (period < 1 || shortagePeriods < 0 || fulfilled < 0 || demand < fulfilled || unassignedResidents < 0)
                throw new IllegalArgumentException("Invalid needs assessment");
            if ((unassignedDemand == null) != (unassignedSupplied == null)) throw new IllegalArgumentException("Incomplete allocation");
            if (unassignedDemand != null) {
                long allocatedDemand = unassignedDemand, allocatedSupply = unassignedSupplied;
                if (allocatedDemand < 0 || allocatedSupply < 0 || allocatedSupply > allocatedDemand) throw new IllegalArgumentException("Invalid unassigned allocation");
                for (var home : homes) {
                    if (home.supplied() == null) throw new IllegalArgumentException("Missing household allocation");
                    allocatedDemand = Math.addExact(allocatedDemand, home.foodDemand());
                    allocatedSupply = Math.addExact(allocatedSupply, home.supplied());
                }
                if (allocatedDemand != demand || allocatedSupply != fulfilled) throw new IllegalArgumentException("Allocation does not conserve food");
            }
        }
        public String explanation() {
            return "Needs at food period " + period + ": shared village food " + sharedFood + ", supplied " + fulfilled + "/" + demand
                    + ", shortage streak " + shortagePeriods + "; " + unassignedResidents + " living residents without a household."
                    + (unassignedDemand == null ? " Historical allocation unknown." : " Unassigned residents supplied " + unassignedSupplied + "/" + unassignedDemand + ".")
                    + " Simulated coverage does not prove physical meals or complete prosperity.";
        }
    }
    public static Assessment assess(UUID town, PopulationState population, Result foodPeriod, long adulthood, EconomySettings settings) {
        return assess(town, population, foodPeriod, adulthood, settings, null);
    }
    public static Assessment assess(UUID town, PopulationState population, Result foodPeriod, long adulthood, EconomySettings settings, Assessment previous) {
        if (previous != null && (!previous.settlement().equals(town) || previous.period() != foodPeriod.state().completedPeriods() - 1))
            throw new IllegalArgumentException("Previous assessment must be the immediately preceding town period");
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
        long assignedDemand = 0;
        for (var home : homes) assignedDemand = Math.addExact(assignedDemand, home.foodDemand());
        long unassignedDemand = Math.subtractExact(food.demand(), assignedDemand);
        var demands = new ArrayList<Long>();
        homes.forEach(home -> demands.add(home.foodDemand())); demands.add(unassignedDemand);
        var allocations = allocate(demands, food.fulfilled());
        var prior = new HashMap<UUID, Home>();
        if (previous != null) previous.homes().forEach(home -> prior.put(home.household(), home));
        var allocatedHomes = new ArrayList<Home>();
        for (int index = 0; index < homes.size(); index++) {
            var home = homes.get(index); var last = prior.get(home.household());
            long streak = allocations[index] == home.foodDemand() ? 0 : Math.incrementExact(last == null || last.unmetPeriods() == null ? 0 : last.unmetPeriods());
            allocatedHomes.add(new Home(home.household(), home.adults(), home.children(), home.foodDemand(), home.capacity(), allocations[index], streak));
        }
        return new Assessment(town, foodPeriod.state().completedPeriods(), food.condition(), food.after().shortagePeriods(),
                food.fulfilled(), food.demand(), unassigned, allocatedHomes, unassignedDemand, allocations[homes.size()]);
    }
    /** Largest remainders conserve whole food units. Caller orders homes by permanent ID, then the unassigned pool. */
    static long[] allocate(List<Long> demands, long supplied) {
        long total = 0;
        for (long demand : demands) { if (demand < 0) throw new IllegalArgumentException("Negative demand"); total = Math.addExact(total, demand); }
        if (supplied < 0 || supplied > total) throw new IllegalArgumentException("Supply exceeds demand");
        long[] shares = new long[demands.size()];
        if (total == 0) return shares;
        var remainders = new ArrayList<BigInteger>(); long allocated = 0;
        for (int index = 0; index < demands.size(); index++) {
            var parts = BigInteger.valueOf(demands.get(index)).multiply(BigInteger.valueOf(supplied)).divideAndRemainder(BigInteger.valueOf(total));
            shares[index] = parts[0].longValueExact(); allocated = Math.addExact(allocated, shares[index]); remainders.add(parts[1]);
        }
        var order = new ArrayList<Integer>(); for (int index = 0; index < demands.size(); index++) order.add(index);
        order.sort(Comparator.<Integer, BigInteger>comparing(remainders::get).reversed().thenComparingInt(Integer::intValue));
        long leftover = supplied - allocated;
        for (int index = 0; index < leftover; index++) shares[order.get(index)]++;
        return shares;
    }
}
