package com.blueninjas24.civilizations.economy;

import java.util.Objects;
import java.util.UUID;

/** Exact accepted food/emerald terms. Inventory exchange still requires a durable player adapter. */
public final class FoodPurchases {
    private FoodPurchases() {}
    public record Policy(boolean enabled, long hundredthsPerEmerald, int reservePeriods, int maximumEmeralds) {
        public Policy {
            if (hundredthsPerEmerald < 1 || hundredthsPerEmerald > 100_000_000 || reservePeriods < 1 || reservePeriods > 100
                    || maximumEmeralds < 1 || maximumEmeralds > 64) throw new IllegalArgumentException("Invalid food purchase policy");
        }
        public static Policy prototype() { return new Policy(true, 800, 3, 64); }
        public static Policy disabled() { return new Policy(false, 800, 3, 64); }
    }
    public record Terms(UUID operation, UUID player, UUID settlement, long units, String item, int count,
                        int emeralds, long hundredthsPerEmerald) {
        public Terms {
            Objects.requireNonNull(operation); Objects.requireNonNull(player); Objects.requireNonNull(settlement);
            if (units < 1 || units > 64_000_000 || count < 1 || count > 64 || item == null || !item.matches("[A-Z_]{1,32}")
                    || emeralds < 1 || emeralds > 64 || hundredthsPerEmerald < 1 || hundredthsPerEmerald > 100_000_000)
                throw new IllegalArgumentException("Invalid food purchase terms");
        }
        public long cost() { return Math.multiplyExact(emeralds, hundredthsPerEmerald); }
        public String source() { return "player-food-purchase:" + operation; }
    }
    /** Floor once to whole emeralds; exact ledger cost is only the actual currency promised. */
    public static int maximumPayment(long fairValue, long spendableBudget, Policy policy) {
        if (fairValue < 0 || spendableBudget < 0) throw new IllegalArgumentException("Negative value or budget");
        return (int)Math.min(policy.maximumEmeralds(), Math.min(fairValue, spendableBudget) / policy.hundredthsPerEmerald());
    }
}
