package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.IndustryPolicy;
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
        if (config.contains("responses")) rootKeys.add("responses");
        if (config.contains("industry")) rootKeys.add("industry");
        if (config.contains("farm-businesses")) rootKeys.add("farm-businesses");
        if (config.contains("recipes")) rootKeys.add("recipes");
        if (config.contains("ore-observer")) rootKeys.add("ore-observer");
        if (config.contains("aid-preview")) rootKeys.add("aid-preview");
        if (config.contains("donations")) rootKeys.add("donations");
        if (config.contains("finance")) rootKeys.add("finance");
        if (config.contains("purchases")) rootKeys.add("purchases");
        if (config.contains("wellbeing")) rootKeys.add("wellbeing");
        if (config.contains("bargaining")) rootKeys.add("bargaining");
        if (config.contains("barter")) rootKeys.add("barter");
        exactKeys(config, rootKeys);
        readFarms(config);
        readPriorities(config);
        readAidPreview(config);
        readDonationsEnabled(config);
        readOpeningFunds(config);
        readPurchases(config);
        readWellbeing(config);
        readBargaining(config);
        readBarter(config);
        readResponses(config);
        readIndustry(config);
        readFarmBusinesses(config);
        readRecipes(config);
        readOreObserver(config);
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
    public static com.blueninjas24.civilizations.industry.OreObserverPolicy readOreObserver(ConfigurationSection config) {
        if(!config.contains("ore-observer"))return com.blueninjas24.civilizations.industry.OreObserverPolicy.disabled();
        var s=section(config,"ore-observer");exactKeys(s,Set.of("enabled","vertical-radius","cells-per-tick","interval-ticks"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("ore-observer.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.OreObserverPolicy(enabled,smallInteger(s,"vertical-radius"),smallInteger(s,"cells-per-tick"),smallInteger(s,"interval-ticks"));
    }
    public static com.blueninjas24.civilizations.industry.FarmBusinessPolicy readFarmBusinesses(ConfigurationSection config) {
        if(!config.contains("farm-businesses"))return com.blueninjas24.civilizations.industry.FarmBusinessPolicy.disabled();
        var settings=section(config,"farm-businesses");
        exactKeys(settings,Set.of("enabled","worker-capacity","startup-trade-value-hundredths","minimum-age-months","minimum-farming-experience"));
        if(!(settings.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("farm-businesses.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.FarmBusinessPolicy(enabled,smallInteger(settings,"worker-capacity"),
                integer(settings,"startup-trade-value-hundredths"),integer(settings,"minimum-age-months"),integer(settings,"minimum-farming-experience"));
    }
    /** Backend recipe catalog. Parsing does not establish a workshop, provide stock or run production. */
    public static Map<String,com.blueninjas24.civilizations.industry.ProductionRecipe> readRecipes(ConfigurationSection config) {
        if(!config.contains("recipes"))return Map.of("smith_tools",com.blueninjas24.civilizations.industry.ProductionRecipe.smithTools());
        var catalog=section(config,"recipes");
        if(catalog.getKeys(false).size()>32)throw new IllegalArgumentException("At most 32 prototype recipes supported");
        var result=new TreeMap<String,com.blueninjas24.civilizations.industry.ProductionRecipe>();
        for(var key:catalog.getKeys(false)) {
            var entry=section(catalog,key);exactKeys(entry,Set.of("industry","inputs","outputs","batches-per-worker","site-batch-capacity"));
            if(!(entry.get("industry") instanceof String industry))throw new IllegalArgumentException("Recipe industry must be text");
            var inputs=new TreeMap<Commodity,Long>();var outputs=new TreeMap<Commodity,Long>();
            var in=section(entry,"inputs");var out=section(entry,"outputs");
            for(var good:in.getKeys(false))inputs.put(new Commodity(good),integer(in,good));
            for(var good:out.getKeys(false))outputs.put(new Commodity(good),integer(out,good));
            result.put(key,new com.blueninjas24.civilizations.industry.ProductionRecipe(key,industry,inputs,outputs,
                    integer(entry,"batches-per-worker"),integer(entry,"site-batch-capacity")));
        }
        return Collections.unmodifiableMap(result);
    }
    /** Backend settings only until reliable discovery/founding/production adapters are implemented. */
    public static IndustryPolicy readIndustry(ConfigurationSection config) {
        if(!config.contains("industry"))return IndustryPolicy.prototype();
        var settings=section(config,"industry");
        exactKeys(settings,Set.of("deposit-batches-per-observed-ore","batches-per-worker","site-batch-capacity","storage-capacity",
                "local-delivery-periods","local-haul-units-per-period","mining-bundle","storage-space-per-unit"));
        Map<Commodity,Long> bundle=new TreeMap<>(),space=new TreeMap<>();
        var yields=section(settings,"mining-bundle");
        for(var key:yields.getKeys(false))bundle.put(new Commodity(key),integer(yields,key));
        var sizes=section(settings,"storage-space-per-unit");
        for(var key:sizes.getKeys(false))space.put(new Commodity(key),integer(sizes,key));
        return new IndustryPolicy(integer(settings,"deposit-batches-per-observed-ore"),integer(settings,"batches-per-worker"),
                integer(settings,"site-batch-capacity"),integer(settings,"storage-capacity"),integer(settings,"local-delivery-periods"),
                integer(settings,"local-haul-units-per-period"),bundle,space);
    }

    /** Preserve existing commented format-1 files: an entirely absent new section gets documented defaults. */
    public static FarmObservation.Policy readFarms(ConfigurationSection config) {
        if (!config.contains("farms")) return FarmObservation.Policy.prototype();
        exactKeys(section(config, "farms"), Set.of("radius-blocks", "vertical-radius-blocks", "crops-per-worker"));
        return new FarmObservation.Policy(smallInteger(config, "farms.radius-blocks"), smallInteger(config, "farms.vertical-radius-blocks"),
                smallInteger(config, "farms.crops-per-worker"));
    }
    public static HouseholdWellbeing.Policy readWellbeing(ConfigurationSection config) {
        if (!config.contains("wellbeing")) return HouseholdWellbeing.Policy.disabled();
        var section = section(config, "wellbeing");
        exactKeys(section, Set.of("enabled", "starting-score", "recovery-per-period", "food-shortage-penalty", "food-crisis-penalty", "crowding-penalty"));
        if (!(section.get("enabled") instanceof Boolean enabled)) throw new IllegalArgumentException("wellbeing.enabled must be true or false");
        return new HouseholdWellbeing.Policy(enabled, smallInteger(section,"starting-score"), smallInteger(section,"recovery-per-period"),
                smallInteger(section,"food-shortage-penalty"), smallInteger(section,"food-crisis-penalty"), smallInteger(section,"crowding-penalty"));
    }
    public static FoodBargaining.Policy readBargaining(ConfigurationSection config) {
        if(!config.contains("bargaining"))return FoodBargaining.Policy.disabled();
        var section=section(config,"bargaining");exactKeys(section,Set.of("enabled","maximum-failed-offers","cooldown-seconds","offer-seconds"));
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("bargaining.enabled must be true or false");
        return new FoodBargaining.Policy(enabled,smallInteger(section,"maximum-failed-offers"),smallInteger(section,"cooldown-seconds"),smallInteger(section,"offer-seconds"));
    }
    public static FoodBarters.Policy readBarter(ConfigurationSection config) {
        if(!config.contains("barter"))return FoodBarters.Policy.disabled();
        var section=section(config,"barter");exactKeys(section,Set.of("enabled","food-reserve-periods","maximum-items","goods"));
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("barter.enabled must be true or false");
        var goods=new TreeMap<Commodity,FoodBarters.Goods>();var entries=section(section,"goods");
        for(var id:entries.getKeys(false)) {
            var entry=section(entries,id);exactKeys(entry,Set.of("item","units-per-item","keep-units"));
            if(!(entry.get("item") instanceof String item))throw new IllegalArgumentException("barter item must be a material name");
            goods.put(new Commodity(id),new FoodBarters.Goods(item,integer(entry,"units-per-item"),integer(entry,"keep-units")));
        }
        return new FoodBarters.Policy(enabled,smallInteger(section,"food-reserve-periods"),smallInteger(section,"maximum-items"),goods);
    }
    public static VillagePriorities.Policy readPriorities(ConfigurationSection config) {
        if (!config.contains("priorities")) return VillagePriorities.Policy.prototype();
        var keys=new java.util.HashSet<>(Set.of("review-after-periods","food-weight","crisis-multiplier","housing-weight","monitor-weight","minimum-relative-percent"));
        if(config.contains("priorities.learning"))keys.add("learning");
        exactKeys(section(config,"priorities"),keys);
        var policy=new VillagePriorities.Policy(smallInteger(config,"priorities.review-after-periods"),smallInteger(config,"priorities.food-weight"),
                smallInteger(config,"priorities.crisis-multiplier"),smallInteger(config,"priorities.housing-weight"),smallInteger(config,"priorities.monitor-weight"),
                smallInteger(config,"priorities.minimum-relative-percent"));
        if(!config.contains("priorities.learning"))return policy;
        exactKeys(section(config,"priorities.learning"),Set.of("window-periods","maximum-food-bonus-percent"));
        return policy.withLearning(smallInteger(config,"priorities.learning.window-periods"),smallInteger(config,"priorities.learning.maximum-food-bonus-percent"));
    }
    public static FoodResponses.Settings readResponses(ConfigurationSection config) {
        var policy=FoodResponses.Policy.disabled();
        if(config.contains("responses")) {
            var section=section(config,"responses");exactKeys(section,Set.of("enabled","donation-weight","emerald-weight","barter-weight","wait-weight","minimum-relative-percent"));
            if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("responses.enabled must be true or false");
            policy=new FoodResponses.Policy(enabled,smallInteger(section,"donation-weight"),smallInteger(section,"emerald-weight"),smallInteger(section,"barter-weight"),smallInteger(section,"wait-weight"),smallInteger(section,"minimum-relative-percent"));
        }
        return new FoodResponses.Settings(policy,readDonationsEnabled(config),readPurchases(config),readBarter(config),readAidPreview(config));
    }
    /** Old owner files retain their read-only behavior until donations are explicitly enabled. */
    public static boolean readDonationsEnabled(ConfigurationSection config) {
        if(!config.contains("donations"))return false;
        var section=section(config,"donations");exactKeys(section,Set.of("enabled"));
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("donations.enabled must be true or false");
        return enabled;
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
    /** Existing owner files require explicit opt-in; never silently mint opening capital. */
    public static OpeningFunds readOpeningFunds(ConfigurationSection config) {
        if (!config.contains("finance")) return OpeningFunds.disabled();
        var finance = section(config, "finance");
        exactKeys(finance, Set.of("opening-funds-enabled", "opening-trade-value-per-resident"));
        if (!(finance.get("opening-funds-enabled") instanceof Boolean enabled))
            throw new IllegalArgumentException("finance.opening-funds-enabled must be true or false");
        Object value = finance.get("opening-trade-value-per-resident");
        if (!(value instanceof Number)) throw new IllegalArgumentException("finance.opening-trade-value-per-resident must be a number");
        try {
            long cents = new BigDecimal(value.toString()).movePointRight(2).longValueExact();
            return new OpeningFunds(enabled, cents);
        } catch (NumberFormatException | ArithmeticException invalid) {
            throw new IllegalArgumentException("finance.opening-trade-value-per-resident must be finite with at most two decimal places", invalid);
        }
    }
    private static ConfigurationSection section(ConfigurationSection config, String path) {
        var section = config.getConfigurationSection(path);
        if (section == null) throw new IllegalArgumentException(path + " must be a YAML section"); return section;
    }
    public static FoodPurchases.Policy readPurchases(ConfigurationSection config) {
        if (!config.contains("purchases")) return FoodPurchases.Policy.disabled();
        var purchases = section(config, "purchases");
        exactKeys(purchases, Set.of("enabled", "trade-value-per-emerald", "food-reserve-periods", "maximum-emeralds"));
        if (!(purchases.get("enabled") instanceof Boolean enabled)) throw new IllegalArgumentException("purchases.enabled must be true or false");
        Object rate = purchases.get("trade-value-per-emerald");
        if (!(rate instanceof Number)) throw new IllegalArgumentException("purchases.trade-value-per-emerald must be a number");
        try {
            return new FoodPurchases.Policy(enabled, new BigDecimal(rate.toString()).movePointRight(2).longValueExact(),
                    smallInteger(purchases, "food-reserve-periods"), smallInteger(purchases, "maximum-emeralds"));
        } catch (NumberFormatException | ArithmeticException invalid) {
            throw new IllegalArgumentException("purchases.trade-value-per-emerald must be finite with at most two decimal places", invalid);
        }
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
