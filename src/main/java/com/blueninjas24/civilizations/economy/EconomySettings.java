package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.economy.CommodityAccounting.Condition;
import com.blueninjas24.civilizations.economy.CommodityAccounting.Policy;
import java.math.*;
import java.util.*;

/** Approved experimental tuning, independent of YAML, Paper, treasury balances and demographic time. */
public record EconomySettings(long secondsPerPeriod, int maximumOfflinePeriods, int periodsPerBatch,
                              long adultFoodUnits, long childFoodUnits, long foodUnitsPerWorker,
                              int foodOutputPercent, Policy shortages, Map<Commodity, Long> startingPerResident,
                              Map<Commodity, BigDecimal> baseTradeValues, Map<Condition, Integer> pricePercent) {
    public EconomySettings {
        if (secondsPerPeriod < 1 || secondsPerPeriod > 86400 || maximumOfflinePeriods < 0 || maximumOfflinePeriods > 10000
                || periodsPerBatch < 1 || periodsPerBatch > 32) throw new IllegalArgumentException("Invalid economy pacing/budget");
        bounded(adultFoodUnits, 1000, "Adult food units"); bounded(childFoodUnits, 1000, "Child food units");
        bounded(foodUnitsPerWorker, 10000, "Food units per worker"); bounded(foodOutputPercent, 1000, "Food output percent");
        Objects.requireNonNull(shortages);
        startingPerResident = Collections.unmodifiableMap(new TreeMap<>(startingPerResident));
        var prices = new TreeMap<Commodity, BigDecimal>();
        baseTradeValues.forEach((commodity, value) -> {
            Objects.requireNonNull(value);
            if (value.signum() <= 0 || value.compareTo(BigDecimal.valueOf(1_000_000)) > 0)
                throw new IllegalArgumentException("Base trade values must be positive and at most 1000000");
            try { prices.put(commodity, value.setScale(2, RoundingMode.UNNECESSARY)); }
            catch (ArithmeticException invalid) { throw new IllegalArgumentException("Base trade values allow at most two decimal places", invalid); }
        });
        baseTradeValues = Collections.unmodifiableMap(prices);
        if (!startingPerResident.containsKey(Commodity.FOOD) || !startingPerResident.keySet().equals(baseTradeValues.keySet()))
            throw new IllegalArgumentException("Starting stocks and prices must cover the same commodities, including food");
        startingPerResident.values().forEach(value -> bounded(value, 1_000_000_000, "Starting units per resident"));
        var percentages = new EnumMap<Condition, Integer>(Condition.class); percentages.putAll(pricePercent);
        if (percentages.size() != Condition.values().length) throw new IllegalArgumentException("Every scarcity price band is required");
        percentages.values().forEach(value -> {
            if (value == null || value < 1 || value > 10000) throw new IllegalArgumentException("Price percent must be 1..10000");
        });
        pricePercent = Collections.unmodifiableMap(percentages);
    }
    /** Only used for explicit first enrollment; never replenish existing reserves on configuration change. */
    public Map<Commodity, Long> startingReserves(long livingResidents) {
        nonnegative(livingResidents); var result = new TreeMap<Commodity, Long>();
        startingPerResident.forEach((commodity, units) -> result.put(commodity, Math.multiplyExact(units, livingResidents)));
        return Collections.unmodifiableMap(result);
    }
    public long foodDemand(long adults, long children) {
        nonnegative(adults); nonnegative(children);
        return Math.addExact(Math.multiplyExact(adults, adultFoodUnits), Math.multiplyExact(children, childFoodUnits));
    }
    /** Caller must substantiate facility slots and working adults; unknown capacity cannot generate output. */
    public long foodProduction(long availableAdultWorkers, OptionalLong evidencedWorkerCapacity) {
        nonnegative(availableAdultWorkers); Objects.requireNonNull(evidencedWorkerCapacity);
        if (evidencedWorkerCapacity.isEmpty()) return 0;
        long capacity = evidencedWorkerCapacity.getAsLong(); nonnegative(capacity);
        return BigInteger.valueOf(Math.min(availableAdultWorkers, capacity)).multiply(BigInteger.valueOf(foodUnitsPerWorker))
                .multiply(BigInteger.valueOf(foodOutputPercent)).divide(BigInteger.valueOf(100)).longValueExact();
    }
    /** Exact abstract Trade Value per unit, rounded to hundredths; not an emerald/item offer. */
    public BigDecimal price(Commodity commodity, Condition condition) {
        var base = baseTradeValues.get(commodity);
        if (base == null) throw new IllegalArgumentException("No price for commodity " + commodity.id());
        Objects.requireNonNull(condition);
        return base.multiply(BigDecimal.valueOf(pricePercent.get(condition))).movePointLeft(2)
                .setScale(2, RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
    }
    private static void bounded(long value, long maximum, String name) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException(name + " must be 0.." + maximum);
    }
    private static void nonnegative(long value) { if (value < 0) throw new IllegalArgumentException("Counts cannot be negative"); }
}
