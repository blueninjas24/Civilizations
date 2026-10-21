package com.blueninjas24.civilizations.paper;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Conservative prototype survey/scheduling limits; existing configs explicitly opt in. */
public record AutomaticHomePlanningSettings(boolean enabled,int cellsPerTick,int reviewSeconds,int maximumActiveHomes,int searchRadius) {
    public AutomaticHomePlanningSettings{
        if(cellsPerTick<64||cellsPerTick>256||reviewSeconds<10||reviewSeconds>600||maximumActiveHomes<1||maximumActiveHomes>16||searchRadius<8||searchRadius>12)
            throw new IllegalArgumentException("Automatic home planning limits: cells64..256, seconds10..600, active homes1..16, radius8..12");
    }
    public static AutomaticHomePlanningSettings disabled(){return new AutomaticHomePlanningSettings(false,128,30,1,12);}
    public static AutomaticHomePlanningSettings read(ConfigurationSection root){
        Objects.requireNonNull(root);if(!root.contains("automatic-home-planning"))return disabled();
        var section=root.getConfigurationSection("automatic-home-planning");
        if(section==null||!section.getKeys(false).equals(Set.of("enabled","cells-per-tick","review-seconds","maximum-active-homes","search-radius")))
            throw new IllegalArgumentException("Missing or unknown automatic-home-planning settings");
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("Automatic home planning enabled must be true/false");
        return new AutomaticHomePlanningSettings(enabled,integer(section,"cells-per-tick"),integer(section,"review-seconds"),integer(section,"maximum-active-homes"),integer(section,"search-radius"));
    }
    private static int integer(ConfigurationSection section,String key){if(!(section.get(key) instanceof Integer n))throw new IllegalArgumentException("Automatic home planning "+key+" must be a whole number");return n;}
}
