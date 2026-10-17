package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import java.util.Objects;
import java.util.UUID;

/** Editable introductory retail terms; not a personal account or a promise of automatic herd output. */
public record LivestockMarketPolicy(boolean enabled, long hundredthsPerEmerald, int milkEmeralds,
                                    int eggsPerOrder, int eggEmeralds, int horizontalRadius,
                                    int verticalRadius, int maximumSellers, int offerSeconds) {
    public LivestockMarketPolicy {
        if (hundredthsPerEmerald < 1 || hundredthsPerEmerald > 100_000_000 || milkEmeralds < 1 || milkEmeralds > 64
                || eggsPerOrder < 1 || eggsPerOrder > 64 || eggEmeralds < 1 || eggEmeralds > 64
                || horizontalRadius < 1 || horizontalRadius > 128 || verticalRadius < 0 || verticalRadius > 64
                || maximumSellers < 1 || maximumSellers > 32 || offerSeconds < 1 || offerSeconds > 300)
            throw new IllegalArgumentException("Invalid livestock market settings");
    }
    public static LivestockMarketPolicy prototype() { return new LivestockMarketPolicy(true, 800, 2, 4, 1, 64, 24, 32, 30); }
    public static LivestockMarketPolicy disabled() { return new LivestockMarketPolicy(false, 800, 2, 4, 1, 64, 24, 32, 30); }
    public record Position(UUID world, double x, double y, double z) {
        public Position {
            Objects.requireNonNull(world);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("Finite market position required");
        }
    }
    public boolean nearby(Position player, UUID world, int x, int y, int z) {
        return player.world().equals(world) && Math.abs(player.y() - y) <= verticalRadius
                && Math.hypot(player.x() - x, player.z() - z) <= horizontalRadius;
    }
    public Terms terms(UUID operation, UUID player, UUID town, UUID world, UUID seller, Product product) {
        return new Terms(operation, player, town, world, seller, product,
                product == Product.MILK ? 1 : eggsPerOrder, product == Product.MILK ? milkEmeralds : eggEmeralds, hundredthsPerEmerald);
    }
    public boolean accepts(Terms terms) {
        return enabled && terms.equals(terms(terms.operation(), terms.player(), terms.settlement(), terms.world(), terms.seller(), terms.product()));
    }
}
