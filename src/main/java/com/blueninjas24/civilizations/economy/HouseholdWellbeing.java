package com.blueninjas24.civilizations.economy;

import java.util.Objects;
import java.util.UUID;

/** Basic food/housing welfare, independent of money, employment and demographic effects. */
public final class HouseholdWellbeing {
    private HouseholdWellbeing() {}

    public record Policy(boolean enabled, int startingScore, int recovery, int foodPenalty,
                         int crisisPenalty, int crowdingPenalty) {
        public Policy {
            for (int value : new int[]{startingScore, recovery, foodPenalty, crisisPenalty, crowdingPenalty})
                if (value < 0 || value > 100) throw new IllegalArgumentException("Wellbeing values must be 0..100");
            if (crisisPenalty < foodPenalty) throw new IllegalArgumentException("Crisis penalty must not be smaller than food penalty");
        }
        public static Policy prototype() { return new Policy(true, 50, 1, 3, 5, 1); }
        public static Policy disabled() { return new Policy(false, 50, 1, 3, 5, 1); }
    }
    public enum Condition { UNKNOWN, SECURE, PRESSURED, FOOD_CRISIS }
    public record Snapshot(UUID household, int before, int after, Condition condition,
                           String food, HouseholdNeeds.Housing housing, long foodStreak, long crisisAfterPeriods, Policy policy) {
        public Snapshot {
            Objects.requireNonNull(household); Objects.requireNonNull(condition);
            Objects.requireNonNull(food); Objects.requireNonNull(housing); Objects.requireNonNull(policy);
            if (before < 0 || before > 100 || after < 0 || after > 100 || foodStreak < 0 || crisisAfterPeriods < 1)
                throw new IllegalArgumentException("Wellbeing score must be 0..100");
        }
        public String explanation() {
            return "Household " + household + " wellbeing: " + condition + "; basic food/housing score " + before + " -> " + after
                    + "/100; food " + food + " (longest resident shortage streak " + foodStreak + ", crisis at " + crisisAfterPeriods + "), housing " + housing
                    + ". This is welfare evidence, not money, employment or a population penalty.";
        }
    }
    public static Snapshot assess(HouseholdNeeds.Home home, Integer previousScore, Policy policy, long crisisAfterPeriods) {
        Objects.requireNonNull(home); Objects.requireNonNull(policy);
        if (!policy.enabled() || crisisAfterPeriods < 1) throw new IllegalArgumentException("Enabled wellbeing and valid crisis duration required");
        int before = previousScore == null ? policy.startingScore() : previousScore;
        boolean unknownFood = home.supplied() == null;
        boolean hungry = !unknownFood && home.supplied() < home.foodDemand();
        boolean crisis = hungry && home.unmetPeriods() >= crisisAfterPeriods;
        boolean crowded = home.housing() == HouseholdNeeds.Housing.CROWDED;
        int delta = hungry ? -(crisis ? policy.crisisPenalty() : policy.foodPenalty()) : 0;
        if (crowded) delta -= policy.crowdingPenalty();
        // Unknown capacity is not homelessness or evidence of comfort. Recovery requires both known needs met.
        if (!unknownFood && !hungry && home.housing() == HouseholdNeeds.Housing.SUFFICIENT) delta += policy.recovery();
        var condition = crisis ? Condition.FOOD_CRISIS : hungry || crowded ? Condition.PRESSURED
                : unknownFood || home.housing() == HouseholdNeeds.Housing.UNKNOWN ? Condition.UNKNOWN : Condition.SECURE;
        return new Snapshot(home.household(), before, Math.clamp(before + delta, 0, 100), condition,
                home.foodCoverage(), home.housing(), home.unmetPeriods()==null ? 0 : home.unmetPeriods(), crisisAfterPeriods, policy);
    }
}
