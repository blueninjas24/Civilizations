package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import org.bukkit.configuration.ConfigurationSection;
import java.math.BigDecimal;
import java.util.*;

/** Strict, human-readable economy.yml adapter. Invalid edits fail before database startup. */
public final class EconomyConfiguration {
    private EconomyConfiguration() {}
    public static EconomySettings read(ConfigurationSection config) {
        var rootKeys = new HashSet<>(Set.of("format-version", "pace", "food", "shortages", "starting-units-per-resident", "prices"));
        if (config.contains("farms")) rootKeys.add("farms");
        if (config.contains("priorities")) rootKeys.add("priorities");
        if (config.contains("aid-preview")) rootKeys.add("aid-preview");
        exactKeys(config, rootKeys);
        readFarms(config);
        readPriorities(config);
        readAidPreview(config);
        if (integer(config, "format-version") != 1) throw new IllegalArgumentException("Unsupported economy.yml format-version; expected 1");
        exactKeys(section(config, "pace"), Set.of("seconds-per-period", "maximum-offline-periods", "periods-per-batch"));
        exactKeys(section(config, "food"), Set.of("units-per-adult", "units-per-child", "units-per-worker", "output-percent"));
        exactKeys(section(config, "shortages"), Set.of("severe-coverage-percent", "severe-after-periods", "surplus-coverage-percent", "abundant-coverage-percent"));
        exactKeys(section(config, "prices"), Set.of("base-trade-value", "condition-percent"));
        var stocks = new TreeMap<Commodity, Long>(); var values = new TreeMap<Commodity, BigDecimal>();
        var inventory = section(config, "starting-units-per-resident");
        for (var id : inventory.getKeys(false)) stocks.put(new Commodity(id), integer(inventory, id));
        var prices = section(config, "prices.base-trade-value");
        for (var id : prices.getKeys(false)) {
            Object value = prices.get(id);
            if (!(value instanceof Number)) throw new IllegalArgumentException("prices.base-trade-value." + id + " must be a number");
            try { values.put(new Commodity(id), new BigDecimal(value.toString())); }
            catch (NumberFormatException invalid) { throw new IllegalArgumentException("prices.base-trade-value." + id + " must be finite", invalid); }
        }
        var bands = section(config, "prices.condition-percent"); var percentages = new EnumMap<Condition, Integer>(Condition.class);
        var keys = new HashSet<String>();
        for (var condition : Condition.values()) {
            var key = condition.name().toLowerCase(Locale.ROOT).replace('_', '-'); keys.add(key);
            percentages.put(condition, smallInteger(bands, key));
        }
        exactKeys(bands, keys);
        return new EconomySettings(integer(config, "pace.seconds-per-period"), smallInteger(config, "pace.maximum-offline-periods"),
                smallInteger(config, "pace.periods-per-batch"), integer(config, "food.units-per-adult"), integer(config, "food.units-per-child"),
                integer(config, "food.units-per-worker"), smallInteger(config, "food.output-percent"),
                new Policy(basisPoints(config, "shortages.severe-coverage-percent"), basisPoints(config, "shortages.surplus-coverage-percent"),
                        basisPoints(config, "shortages.abundant-coverage-percent"), integer(config, "shortages.severe-after-periods")), stocks, values, percentages);
    }
    /** Preserve existing commented format-1 files: an entirely absent new section gets documented defaults. */
    public static FarmObservation.Policy readFarms(ConfigurationSection config) {
        if (!config.contains("farms")) return FarmObservation.Policy.prototype();
        exactKeys(section(config, "farms"), Set.of("radius-blocks", "vertical-radius-blocks", "crops-per-worker"));
        return new FarmObservation.Policy(smallInteger(config, "farms.radius-blocks"), smallInteger(config, "farms.vertical-radius-blocks"),
                smallInteger(config, "farms.crops-per-worker"));
    }
    public static VillagePriorities.Policy readPriorities(ConfigurationSection config) {
        if (!config.contains("priorities")) return VillagePriorities.Policy.prototype();
        exactKeys(section(config,"priorities"),Set.of("review-after-periods","food-weight","crisis-multiplier","housing-weight","monitor-weight","minimum-relative-percent"));
        return new VillagePriorities.Policy(smallInteger(config,"priorities.review-after-periods"),smallInteger(config,"priorities.food-weight"),
                smallInteger(config,"priorities.crisis-multiplier"),smallInteger(config,"priorities.housing-weight"),smallInteger(config,"priorities.monitor-weight"),
                smallInteger(config,"priorities.minimum-relative-percent"));
    }
    public static FoodAidPreview.Policy readAidPreview(ConfigurationSection config) {
        if (!config.contains("aid-preview")) return FoodAidPreview.Policy.prototype();
        var section = section(config, "aid-preview");
        exactKeys(section, Set.of("enabled", "radius-blocks", "units-per-item"));
        if (!(section.get("enabled") instanceof Boolean enabled)) throw new IllegalArgumentException("aid-preview.enabled must be true or false");
        var items = section(section, "units-per-item");
        var units = new TreeMap<String, Long>();
        var allowed = Set.of("WHEAT", "BREAD", "CARROT", "POTATO", "BAKED_POTATO", "BEETROOT");
        for (var id : items.getKeys(false)) {
            if (!allowed.contains(id)) throw new IllegalArgumentException("Unsupported aid-preview food: " + id);
            units.put(id, integer(items, id));
        }
        return new FoodAidPreview.Policy(enabled, smallInteger(section, "radius-blocks"), units);
    }
    private static ConfigurationSection section(ConfigurationSection config, String path) {
        var section = config.getConfigurationSection(path);
        if (section == null) throw new IllegalArgumentException(path + " must be a YAML section"); return section;
    }
    private static void exactKeys(ConfigurationSection section, Set<String> expected) {
        if (!section.getKeys(false).equals(expected)) throw new IllegalArgumentException("Economy keys at "
                + (section.getCurrentPath().isEmpty() ? "root" : section.getCurrentPath()) + " must be " + new TreeSet<>(expected));
    }
    private static long integer(ConfigurationSection config, String path) {
        Object value = config.get(path);
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) return ((Number)value).longValue();
        throw new IllegalArgumentException(path + " must be a whole number");
    }
    private static int smallInteger(ConfigurationSection config, String path) {
        try { return Math.toIntExact(integer(config, path)); }
        catch (ArithmeticException invalid) { throw new IllegalArgumentException(path + " is too large", invalid); }
    }
    private static int basisPoints(ConfigurationSection config, String path) {
        try { return Math.multiplyExact(smallInteger(config, path), 100); }
        catch (ArithmeticException invalid) { throw new IllegalArgumentException(path + " is too large", invalid); }
    }
}
