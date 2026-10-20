package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Purpose;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Strict admin-facing construction cash policy. Values use the existing hundredths-of-an-emerald ledger. */
public final class ConstructionBudgetConfiguration {
    private ConstructionBudgetConfiguration(){}
    public static ConstructionBudgetPolicy read(ConfigurationSection root) {
        Objects.requireNonNull(root);keys(root,Set.of("format","business","village"));
        if(number(root,"format")!=1)throw new IllegalArgumentException("Unsupported construction funding format");
        return new ConstructionBudgetPolicy(account(root.getConfigurationSection("business")),account(root.getConfigurationSection("village")));
    }
    private static ConstructionBudgetPolicy.Account account(ConfigurationSection section) {
        if(section==null)throw new IllegalArgumentException("Missing construction funding account");
        keys(section,Set.of("enabled","minimum-cash","food-reserve-periods","cash-per-food-unit","essential-upkeep-reserve","maximum-purchase","per-period-limit","purposes"));
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("Construction enabled must be true/false");
        if(!(section.get("purposes") instanceof List<?> values)||values.isEmpty())throw new IllegalArgumentException("Construction purposes must be a nonempty list");
        var purposes=EnumSet.noneOf(Purpose.class);for(var item:values) {
            if(!(item instanceof String value)||!purposes.add(Purpose.valueOf(value.toUpperCase(Locale.ROOT))))throw new IllegalArgumentException("Invalid or duplicate construction purpose");
        }
        long periods=number(section,"food-reserve-periods");if(periods<0||periods>100)throw new IllegalArgumentException("Construction food reserve periods must be 0..100");
        return new ConstructionBudgetPolicy.Account(enabled,number(section,"minimum-cash"),(int)periods,
                number(section,"cash-per-food-unit"),number(section,"essential-upkeep-reserve"),number(section,"maximum-purchase"),number(section,"per-period-limit"),purposes);
    }
    private static long number(ConfigurationSection section,String key) {
        var value=section.get(key);if(!(value instanceof Integer||value instanceof Long))throw new IllegalArgumentException("Construction "+key+" must be a whole number");return ((Number)value).longValue();
    }
    private static void keys(ConfigurationSection section,Set<String> allowed){if(!allowed.equals(section.getKeys(false)))throw new IllegalArgumentException("Missing or unknown construction funding settings in "+section.getCurrentPath());}
}
