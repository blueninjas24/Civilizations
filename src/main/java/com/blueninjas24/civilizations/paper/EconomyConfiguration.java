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
        exactKeys(config, Set.of("format-version", "pace", "food", "shortages", "starting-units-per-resident", "prices"));
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
