package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.economy.CommodityAccounting.Condition;
import java.util.*;

/** Nonbinding, evidence-based priorities. Selection cannot buy goods, create jobs or authorize building. */
public final class VillagePriorities {
    private VillagePriorities() {}
    public enum Action { MONITOR, SEEK_FOOD, INVESTIGATE_HOUSING }
    public record Policy(int reviewAfterPeriods, int foodWeight, int crisisMultiplier, int housingWeight,
                         int monitorWeight, int minimumRelativePercent, int learningPeriods, int maximumFoodBonusPercent) {
        public Policy(int reviewAfterPeriods, int foodWeight, int crisisMultiplier, int housingWeight,
                      int monitorWeight, int minimumRelativePercent) {
            this(reviewAfterPeriods, foodWeight, crisisMultiplier, housingWeight, monitorWeight, minimumRelativePercent, 0, 0);
        }
        public Policy {
            if (reviewAfterPeriods < 1 || reviewAfterPeriods > 1000 || foodWeight < 1 || foodWeight > 1000
                    || crisisMultiplier < 1 || crisisMultiplier > 100 || housingWeight < 1 || housingWeight > 1000
                    || monitorWeight < 1 || monitorWeight > 1000 || minimumRelativePercent < 0 || minimumRelativePercent > 100)
                throw new IllegalArgumentException("Invalid priority settings");
            if (learningPeriods < 0 || learningPeriods > 100 || maximumFoodBonusPercent < 0 || maximumFoodBonusPercent > 100
                    || (learningPeriods == 0) != (maximumFoodBonusPercent == 0))
                throw new IllegalArgumentException("Learning requires a window 1..100 and bonus 1..100, or both zero to disable");
        }
        public static Policy prototype() { return new Policy(5,10,3,6,1,25); }
        public Policy withLearning(int periods, int cap) {
            return new Policy(reviewAfterPeriods, foodWeight, crisisMultiplier, housingWeight, monitorWeight, minimumRelativePercent, periods, cap);
        }
    }
    /** Only verified applied imports from earlier periods; aggregate benefit, never player attribution. */
    public record Evidence(long imported, long avoidedUnmet) {
        public Evidence {
            if (imported < 0 || avoidedUnmet < 0 || avoidedUnmet > imported) throw new IllegalArgumentException("Invalid learning evidence");
        }
        public static Evidence none() { return new Evidence(0,0); }
        public int bonus(Policy policy) {
            return imported == 0 ? 0 : java.math.BigInteger.valueOf(avoidedUnmet)
                    .multiply(java.math.BigInteger.valueOf(policy.maximumFoodBonusPercent()))
                    .divide(java.math.BigInteger.valueOf(imported)).intValueExact();
        }
    }
    public record Candidate(Action action, boolean feasible, boolean eligible, long weight, String explanation) {}
    public record Facts(UUID settlement, long period, Condition sharedFood, long foodUnmet, long crowdedHomes) {
        public Facts {
            Objects.requireNonNull(settlement); Objects.requireNonNull(sharedFood);
            if (period < 1 || foodUnmet < 0 || crowdedHomes < 0) throw new IllegalArgumentException("Invalid priority evidence");
        }
    }
    public record Decision(UUID settlement, long period, long seed, Policy policy, Condition foodCondition,
                           long foodUnmet, long crowdedHomes, Action selected, List<Candidate> candidates, Evidence evidence) {
        public Decision(UUID settlement, long period, long seed, Policy policy, Condition foodCondition,
                        long foodUnmet, long crowdedHomes, Action selected, List<Candidate> candidates) {
            this(settlement,period,seed,policy,foodCondition,foodUnmet,crowdedHomes,selected,candidates,Evidence.none());
        }
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
        return review(seed,needs,policy,Evidence.none());
    }
    public static Decision review(long seed, Facts needs, Policy policy, Evidence evidence) {
        Objects.requireNonNull(evidence);
        long unmet = needs.foodUnmet(), crowded = needs.crowdedHomes();
        boolean food = unmet > 0;
        long foodWeight = (long)policy.foodWeight() * (needs.sharedFood() == Condition.SEVERE_SHORTAGE ? policy.crisisMultiplier() : 1);
        int bonus = evidence.bonus(policy);
        long scale = policy.learningPeriods()==0 ? 1 : 100;
        foodWeight = foodWeight * (scale + (scale==1 ? 0 : bonus));
        var raw = List.of(
                new Candidate(Action.MONITOR,true,false,policy.monitorWeight()*scale,"Monitor known conditions; spend nothing."),
                new Candidate(Action.SEEK_FOOD,food,false,foodWeight,(food ? "Actual food demand is unmet; seek a feasible source before buying." : "Blocked: current food demand is fully supplied.")
                        + (policy.learningPeriods()==0 ? "" : " Prior verified imports prevented " + evidence.avoidedUnmet() + " unmet units out of " + evidence.imported()
                        + " imported; bounded food weight bonus " + bonus + "%, window " + policy.learningPeriods() + " earlier periods. No appeal-causation or player reward inferred.")),
                new Candidate(Action.INVESTIGATE_HOUSING,crowded>0,false,policy.housingWeight()*scale,crowded>0 ? "Observed homes are crowded; investigate safe options before building." : "Blocked: no observed crowded household; unknown housing is not proof."));
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
        return new Decision(needs.settlement(),needs.period(),seed,policy,needs.sharedFood(),unmet,crowded,Objects.requireNonNull(selected),candidates,evidence);
    }
}
