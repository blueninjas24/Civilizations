package com.blueninjas24.civilizations.economy;

import java.util.*;

/** Exact food-for-stock terms. Quotes never reserve or prove a physical item exchange. */
public final class FoodBarters {
    private FoodBarters() {}
    public record Goods(String item, long unitsPerItem, long keepUnits) {
        public Goods {
            if (item == null || !Set.of("OAK_LOG", "COBBLESTONE").contains(item)
                    || unitsPerItem < 1 || unitsPerItem > 1_000_000 || keepUnits < 0 || keepUnits > 1_000_000_000)
                throw new IllegalArgumentException("Invalid prototype barter goods");
        }
    }
    public record Policy(boolean enabled, int foodReservePeriods, int maximumItems, Map<Commodity, Goods> goods) {
        public Policy {
            if (foodReservePeriods < 1 || foodReservePeriods > 100 || maximumItems < 1 || maximumItems > 64)
                throw new IllegalArgumentException("Invalid barter limits");
            goods = Collections.unmodifiableMap(new TreeMap<>(goods));
            goods.forEach((commodity, value) -> {
                Objects.requireNonNull(value);
                if (!(commodity.equals(Commodity.TIMBER) && value.item().equals("OAK_LOG"))
                        && !(commodity.equals(Commodity.STONE) && value.item().equals("COBBLESTONE")))
                    throw new IllegalArgumentException("Unsupported prototype barter commodity/item");
            });
        }
        public static Policy prototype() { return new Policy(true, 3, 64, Map.of(
                Commodity.TIMBER, new Goods("OAK_LOG", 1, 2), Commodity.STONE, new Goods("COBBLESTONE", 1, 2))); }
        public static Policy disabled() { var p=prototype();return new Policy(false,p.foodReservePeriods(),p.maximumItems(),p.goods()); }
    }
    public record Terms(UUID operation, UUID player, UUID settlement, long foodUnits, String foodItem, int foodCount,
                        Commodity commodity, String paymentItem, int paymentCount, long unitsPerItem) {
        public Terms {
            Objects.requireNonNull(operation);Objects.requireNonNull(player);Objects.requireNonNull(settlement);Objects.requireNonNull(commodity);
            if (foodUnits < 1 || foodUnits > 64_000_000 || foodCount < 1 || foodCount > 64
                    || foodItem == null || !foodItem.matches("[A-Z_]{1,32}") || paymentCount < 1 || paymentCount > 64)
                throw new IllegalArgumentException("Invalid barter terms");
            new Policy(true,1,64,Map.of(commodity,new Goods(paymentItem,unitsPerItem,0)));
        }
        public long paymentUnits() { return Math.multiplyExact(paymentCount,unitsPerItem); }
        public String source() { return "player-food-barter:"+operation; }
    }
    public static int maximumPayment(long foodValue, long outgoingUnitValue, long availableUnits, Goods goods, int cap) {
        if (foodValue < 0 || outgoingUnitValue < 1 || availableUnits < 0 || cap < 1 || cap > 64)
            throw new IllegalArgumentException("Invalid barter value/stock");
        long itemValue=Math.multiplyExact(outgoingUnitValue,goods.unitsPerItem());
        long disposable=availableUnits>goods.keepUnits() ? availableUnits-goods.keepUnits() : 0;
        return (int)Math.min(cap,Math.min(foodValue/itemValue,disposable/goods.unitsPerItem()));
    }
}
