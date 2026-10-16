package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.Objects;
import java.util.UUID;

/** Exact accepted player purchase terms. No Paper inventory or implicit payment. */
public final class LivestockProductSale {
    private LivestockProductSale() {}
    public enum Product {
        MILK("milk", "MILK_BUCKET", 1), EGGS("eggs", "EGG", 64);
        private final Commodity commodity;
        private final String item;
        private final int maximumCount;
        Product(String commodity, String item, int maximumCount) {
            this.commodity = new Commodity(commodity); this.item = item; this.maximumCount = maximumCount;
        }
        public Commodity commodity() { return commodity; }
        public String item() { return item; }
    }
    public record Terms(UUID operation, UUID player, UUID settlement, UUID world, UUID seller,
                        Product product, int count, int emeralds, long hundredthsPerEmerald) {
        public Terms {
            Objects.requireNonNull(operation); Objects.requireNonNull(player); Objects.requireNonNull(settlement);
            Objects.requireNonNull(world); Objects.requireNonNull(seller); Objects.requireNonNull(product);
            if (count < 1 || count > product.maximumCount || emeralds < 1 || emeralds > 64
                    || hundredthsPerEmerald < 1 || hundredthsPerEmerald > 100_000_000)
                throw new IllegalArgumentException("Invalid livestock product sale terms");
        }
        public long cost() { return Math.multiplyExact(emeralds, hundredthsPerEmerald); }
        public int emptyBuckets() { return product == Product.MILK ? count : 0; }
    }
}
