package com.blueninjas24.civilizations.paper;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Bounded physical-home scheduler. Missing settings never silently authorize writes on an old installation. */
public record AutomaticConstructionSettings(boolean enabled,int cellsPerTick,int reviewSeconds,int blockIntervalMillis) {
    public AutomaticConstructionSettings {
        if(cellsPerTick<16||cellsPerTick>256||reviewSeconds<1||reviewSeconds>600||blockIntervalMillis<0||blockIntervalMillis>60_000)
            throw new IllegalArgumentException("Automatic construction limits: cells16..256, review seconds1..600, block interval0..60000ms");
    }
    public static AutomaticConstructionSettings disabled(){return new AutomaticConstructionSettings(false,128,10,500);}
    public static AutomaticConstructionSettings read(ConfigurationSection root){
        Objects.requireNonNull(root);if(!root.contains("automatic-construction"))return disabled();
        var section=root.getConfigurationSection("automatic-construction");
        if(section==null||!section.getKeys(false).equals(Set.of("enabled","cells-per-tick","review-seconds","block-interval-ms")))
            throw new IllegalArgumentException("Missing or unknown automatic-construction settings");
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("Automatic construction enabled must be true/false");
        return new AutomaticConstructionSettings(enabled,integer(section,"cells-per-tick"),integer(section,"review-seconds"),integer(section,"block-interval-ms"));
    }
    private static int integer(ConfigurationSection section,String key){if(!(section.get(key) instanceof Integer value))throw new IllegalArgumentException("Automatic construction "+key+" must be a whole number");return value;}
}
