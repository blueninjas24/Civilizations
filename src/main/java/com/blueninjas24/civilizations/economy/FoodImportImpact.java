package com.blueninjas24.civilizations.economy;

import java.util.Objects;
import com.blueninjas24.civilizations.economy.CommodityAccounting.Report;

/** Period-local accounting counterfactual, not proof that a priority attracted a supplier. */
public final class FoodImportImpact {
    private FoodImportImpact() {}

    public record Outcome(long imported, long unmetWithoutImports, long actualUnmet,
                          long avoidedUnmet, long additionalReserve) {
        public Outcome {
            if (imported < 0 || unmetWithoutImports < 0 || actualUnmet < 0 || avoidedUnmet < 0 || additionalReserve < 0
                    || actualUnmet > unmetWithoutImports || unmetWithoutImports - actualUnmet != avoidedUnmet
                    || avoidedUnmet > imported || imported - avoidedUnmet != additionalReserve)
                throw new IllegalArgumentException("Inconsistent food import outcome");
        }
        public String explanation() {
            return "Food import impact: " + imported + " units imported; " + avoidedUnmet
                    + " units of unmet demand prevented; " + additionalReserve
                    + " additional closing reserve. Without this period's imports, unmet demand would be "
                    + unmetWithoutImports + " (actual " + actualUnmet
                    + "), holding opening stock, production and demand fixed. This does not prove an appeal caused delivery"
                    + " or attribute benefit to a particular player; no adaptive weight or reputation reward applied.";
        }
    }

    /** Exact marginal contribution of ALL imports together, independent of source ordering. */
    public static Outcome assess(Report food) {
        Objects.requireNonNull(food);
        if (!food.commodity().equals(Commodity.FOOD)) throw new IllegalArgumentException("Food report required");
        long without = Math.addExact(food.before().units(), food.flow().production());
        long available = Math.addExact(without, food.flow().imports());
        long demand = food.flow().demand();
        long fulfilled = Math.min(available, demand);
        long unmet = demand - fulfilled;
        if (food.available() != available || food.demand() != demand || food.fulfilled() != fulfilled
                || food.unmet() != unmet || food.after().units() != available - fulfilled)
            throw new IllegalArgumentException("Food report contradicts retained accounting inputs");
        long baselineUnmet = demand - Math.min(without, demand);
        long avoided = baselineUnmet - unmet;
        return new Outcome(food.flow().imports(), baselineUnmet, unmet, avoided, food.flow().imports() - avoided);
    }
}
