package com.blueninjas24.civilizations.economy;

import java.util.*;

/** Read-only aid calculations. Saved identity selects a recipient; proximity never creates a town. */
public final class FoodAidPreview {
    private FoodAidPreview() {}
    public record Policy(boolean enabled, int radius, Map<String, Long> unitsPerItem) {
        public Policy {
            if (radius < 1 || radius > 128 || unitsPerItem.isEmpty()) throw new IllegalArgumentException("Aid radius must be 1..128 and food mappings nonempty");
            unitsPerItem = Map.copyOf(unitsPerItem);
            unitsPerItem.forEach((id, units) -> {
                if (!id.matches("[A-Z_]+") || units < 1 || units > 1_000_000) throw new IllegalArgumentException("Invalid aid item conversion");
            });
        }
        public static Policy prototype() {
            return new Policy(true, 32, Map.of("WHEAT", 1L, "BREAD", 3L, "CARROT", 1L,
                    "POTATO", 1L, "BAKED_POTATO", 2L, "BEETROOT", 1L));
        }
        public long units(String item, int count) {
            if (count < 1 || count > 64 || !unitsPerItem.containsKey(item)) throw new IllegalArgumentException("Unsupported food or stack quantity");
            return Math.multiplyExact(unitsPerItem.get(item), count);
        }
    }
    public record Home(UUID town, UUID world, double x, double y, double z) {
        public Home { Objects.requireNonNull(town); Objects.requireNonNull(world); }
    }
    public enum Match { FOUND, NONE, AMBIGUOUS }
    public record Recipient(Match match, UUID town) {}
    /** Closest evidenced home, within spherical range, in this permanent world. Exact ties require an explicit ID. */
    public static Recipient recipient(UUID world, double x, double y, double z, Collection<Home> homes, int radius, UUID requested) {
        Objects.requireNonNull(world);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || radius < 1 || radius > 128)
            throw new IllegalArgumentException("Invalid aid position/range");
        var distances = new HashMap<UUID, Double>();
        for (var home : homes) {
            if (!home.world().equals(world) || requested != null && !home.town().equals(requested)) continue;
            double dx = home.x() - x, dy = home.y() - y, dz = home.z() - z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= (double) radius * radius) distances.merge(home.town(), distance, Math::min);
        }
        if (distances.isEmpty()) return new Recipient(Match.NONE, null);
        double nearest = Collections.min(distances.values());
        var candidates = distances.entrySet().stream().filter(e -> e.getValue() == nearest).map(Map.Entry::getKey).toList();
        return candidates.size() == 1 ? new Recipient(Match.FOUND, candidates.getFirst()) : new Recipient(Match.AMBIGUOUS, null);
    }
}
