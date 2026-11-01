package com.blueninjas24.civilizations.paper;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Bounded home/local-road scheduler. Missing settings never silently authorize writes on an old installation. */
public record AutomaticConstructionSettings(boolean enabled,int cellsPerTick,int reviewSeconds,int blockIntervalMillis,boolean localRoadsEnabled) {
    public AutomaticConstructionSettings {
        if(cellsPerTick<16||cellsPerTick>256||reviewSeconds<1||reviewSeconds>600||blockIntervalMillis<0||blockIntervalMillis>60_000)
            throw new IllegalArgumentException("Automatic construction limits: cells16..256, review seconds1..600, block interval0..60000ms");
    }
    public AutomaticConstructionSettings(boolean enabled,int cellsPerTick,int reviewSeconds,int blockIntervalMillis){this(enabled,cellsPerTick,reviewSeconds,blockIntervalMillis,false);}
    public static AutomaticConstructionSettings disabled(){return new AutomaticConstructionSettings(false,128,10,500);}
    public static AutomaticConstructionSettings read(ConfigurationSection root){
        Objects.requireNonNull(root);if(!root.contains("automatic-construction"))return disabled();
        var section=root.getConfigurationSection("automatic-construction");
        if(section==null||!section.getKeys(false).containsAll(Set.of("enabled","cells-per-tick","review-seconds","block-interval-ms"))||!Set.of("enabled","cells-per-tick","review-seconds","block-interval-ms","local-roads-enabled").containsAll(section.getKeys(false)))
            throw new IllegalArgumentException("Missing or unknown automatic-construction settings");
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("Automatic construction enabled must be true/false");
        if(section.contains("local-roads-enabled")&&!(section.get("local-roads-enabled") instanceof Boolean))throw new IllegalArgumentException("Local roads enabled must be true/false");
        return new AutomaticConstructionSettings(enabled,integer(section,"cells-per-tick"),integer(section,"review-seconds"),integer(section,"block-interval-ms"),section.getBoolean("local-roads-enabled",false));
    }
    private static int integer(ConfigurationSection section,String key){if(!(section.get(key) instanceof Integer value))throw new IllegalArgumentException("Automatic construction "+key+" must be a whole number");return value;}
}
