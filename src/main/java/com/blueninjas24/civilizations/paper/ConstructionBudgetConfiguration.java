package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Purpose;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Strict admin-facing construction cash policy. Values use the existing hundredths-of-an-emerald ledger. */
public final class ConstructionBudgetConfiguration {
    private ConstructionBudgetConfiguration(){}
    public static ConstructionBudgetPolicy read(ConfigurationSection root) {
        Objects.requireNonNull(root);var rootKeys=root.getKeys(false);
        if(!rootKeys.containsAll(Set.of("format","business","village"))||!Set.of("format","business","village","land","labor","housing").containsAll(rootKeys))throw new IllegalArgumentException("Missing or unknown construction settings");
        if(number(root,"format")!=1)throw new IllegalArgumentException("Unsupported construction funding format");
        return new ConstructionBudgetPolicy(account(root.getConfigurationSection("business")),account(root.getConfigurationSection("village")));
    }
    public static com.blueninjas24.civilizations.construction.ConstructionLandPolicy land(ConfigurationSection root) {
        if(!root.contains("land"))return new com.blueninjas24.civilizations.construction.ConstructionLandPolicy(4);
        var section=root.getConfigurationSection("land");if(section==null)throw new IllegalArgumentException("Invalid construction land settings");keys(section,Set.of("building-buffer"));
        long buffer=number(section,"building-buffer");if(buffer<0||buffer>16)throw new IllegalArgumentException("Construction building buffer must be 0..16");
        return new com.blueninjas24.civilizations.construction.ConstructionLandPolicy((int)buffer);
    }
    public static com.blueninjas24.civilizations.construction.HousingProjectPlanner housing(ConfigurationSection root) {
        if(!root.contains("housing"))return com.blueninjas24.civilizations.construction.HousingProjectPlanner.prototype();
        var section=root.getConfigurationSection("housing");if(section==null)throw new IllegalArgumentException("Invalid construction housing settings");
        keys(section,Set.of("maximum-new-proposals"));long cap=number(section,"maximum-new-proposals");
        if(cap<1||cap>64)throw new IllegalArgumentException("Housing proposal cap must be1..64");
        return new com.blueninjas24.civilizations.construction.HousingProjectPlanner((int)cap);
    }
    public static com.blueninjas24.civilizations.construction.ConstructionLaborPolicy labor(ConfigurationSection root) {
        if(!root.contains("labor"))return com.blueninjas24.civilizations.construction.ConstructionLaborPolicy.prototype();
        var section=root.getConfigurationSection("labor");if(section==null)throw new IllegalArgumentException("Invalid construction labor settings");
        keys(section,Set.of("enabled","village-workers","business-workers","effort-per-worker","business-share-percent"));
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("Construction labor enabled must be true/false");
        long village=number(section,"village-workers"),business=number(section,"business-workers"),effort=number(section,"effort-per-worker"),share=number(section,"business-share-percent");
        if(village<1||village>64||business<1||business>64||effort<1||effort>10_000||share<1||share>100)throw new IllegalArgumentException("Invalid construction labor bounds");
        return new com.blueninjas24.civilizations.construction.ConstructionLaborPolicy(enabled,(int)village,(int)business,(int)effort,(int)share);
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
