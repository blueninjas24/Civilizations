package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.economy.CommodityAccounting.Condition;
import java.util.*;

/** Nonbinding, evidence-based priorities. Selection cannot buy goods, create jobs or authorize building. */
public final class VillagePriorities {
    private VillagePriorities() {}
    public enum Action { MONITOR, SEEK_FOOD, INVESTIGATE_HOUSING }
    public record Policy(int reviewAfterPeriods, int foodWeight, int crisisMultiplier, int housingWeight,
                         int monitorWeight, int minimumRelativePercent) {
        public Policy {
            if (reviewAfterPeriods < 1 || reviewAfterPeriods > 1000 || foodWeight < 1 || foodWeight > 1000
                    || crisisMultiplier < 1 || crisisMultiplier > 100 || housingWeight < 1 || housingWeight > 1000
                    || monitorWeight < 1 || monitorWeight > 1000 || minimumRelativePercent < 0 || minimumRelativePercent > 100)
                throw new IllegalArgumentException("Invalid priority settings");
        }
        public static Policy prototype() { return new Policy(5,10,3,6,1,25); }
    }
    public record Candidate(Action action, boolean feasible, boolean eligible, long weight, String explanation) {}
    public record Facts(UUID settlement, long period, Condition sharedFood, long foodUnmet, long crowdedHomes) {
        public Facts {
            Objects.requireNonNull(settlement); Objects.requireNonNull(sharedFood);
            if (period < 1 || foodUnmet < 0 || crowdedHomes < 0) throw new IllegalArgumentException("Invalid priority evidence");
        }
    }
    public record Decision(UUID settlement, long period, long seed, Policy policy, Condition foodCondition,
                           long foodUnmet, long crowdedHomes, Action selected, List<Candidate> candidates) {
        public Decision { candidates = List.copyOf(candidates); }
        public String explanation() {
            return "Priority reviewed at period " + period + ": " + selected + "; unmet food " + foodUnmet + ", crowded homes " + crowdedHomes
                    + ". This is a planning priority, not an executed purchase or building project.";
        }
    }
    public static long crowded(HouseholdNeeds.Assessment needs) {
        return needs.homes().stream().filter(home -> home.housing() == HouseholdNeeds.Housing.CROWDED).count();
    }
    public static boolean shouldReview(HouseholdNeeds.Assessment needs, Policy policy, Decision previous) {
        if (previous == null) return true;
        if (!needs.settlement().equals(previous.settlement()) || needs.period() <= previous.period())
            throw new IllegalArgumentException("Priority evidence out of order or wrong settlement");
        return !policy.equals(previous.policy()) || needs.period() - previous.period() >= policy.reviewAfterPeriods()
                || (needs.demand() > needs.fulfilled()) != (previous.foodUnmet() > 0)
                || (needs.sharedFood() == Condition.SEVERE_SHORTAGE) != (previous.foodCondition() == Condition.SEVERE_SHORTAGE)
                || (crowded(needs) > 0) != (previous.crowdedHomes() > 0);
    }
    public static Decision review(long seed, HouseholdNeeds.Assessment needs, Policy policy) {
        return review(seed,new Facts(needs.settlement(),needs.period(),needs.sharedFood(),needs.demand()-needs.fulfilled(),crowded(needs)),policy);
    }
    public static Decision review(long seed, Facts needs, Policy policy) {
        long unmet = needs.foodUnmet(), crowded = needs.crowdedHomes();
        boolean food = unmet > 0;
        long foodWeight = (long)policy.foodWeight() * (needs.sharedFood() == Condition.SEVERE_SHORTAGE ? policy.crisisMultiplier() : 1);
        var raw = List.of(
                new Candidate(Action.MONITOR,true,false,policy.monitorWeight(),"Monitor known conditions; spend nothing."),
                new Candidate(Action.SEEK_FOOD,food,false,foodWeight,food ? "Actual food demand is unmet; seek a feasible source before buying." : "Blocked: current food demand is fully supplied."),
                new Candidate(Action.INVESTIGATE_HOUSING,crowded>0,false,policy.housingWeight(),crowded>0 ? "Observed homes are crowded; investigate safe options before building." : "Blocked: no observed crowded household; unknown housing is not proof."));
        long best = raw.stream().filter(Candidate::feasible).mapToLong(Candidate::weight).max().orElseThrow();
        var candidates = raw.stream().map(c -> new Candidate(c.action(),c.feasible(),c.feasible() && c.weight()*100 >= best*policy.minimumRelativePercent(),
                c.weight(),c.explanation() + (c.feasible() && c.weight()*100 < best*policy.minimumRelativePercent() ? " Below the reasonable-priority weight threshold." : ""))).toList();
        long total = candidates.stream().filter(Candidate::eligible).mapToLong(Candidate::weight).sum();
        var random = new SplittableRandom(seed ^ needs.settlement().getMostSignificantBits() ^ Long.rotateLeft(needs.settlement().getLeastSignificantBits(),17)
                ^ (needs.period()*0x9E3779B97F4A7C15L));
        long pick = random.nextLong(total); Action selected = null;
        for (var candidate : candidates) if (candidate.eligible()) {
            if (pick < candidate.weight()) { selected = candidate.action(); break; }
            pick -= candidate.weight();
        }
        return new Decision(needs.settlement(),needs.period(),seed,policy,needs.sharedFood(),unmet,crowded,Objects.requireNonNull(selected),candidates);
    }
}
