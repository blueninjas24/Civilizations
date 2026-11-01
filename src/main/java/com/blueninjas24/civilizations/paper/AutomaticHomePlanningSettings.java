package com.blueninjas24.civilizations.paper;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Conservative prototype survey/scheduling limits; existing configs explicitly opt in. */
public record AutomaticHomePlanningSettings(boolean enabled,int cellsPerTick,int reviewSeconds,int maximumActiveHomes,int searchRadius,int verticalSearch) {
    public AutomaticHomePlanningSettings{
        if(cellsPerTick<64||cellsPerTick>256||reviewSeconds<10||reviewSeconds>600||maximumActiveHomes<1||maximumActiveHomes>16||searchRadius<8||searchRadius>12||verticalSearch<0||verticalSearch>4)
            throw new IllegalArgumentException("Automatic home planning limits: cells64..256, seconds10..600, active homes1..16, radius8..12, vertical search0..4");
    }
    public AutomaticHomePlanningSettings(boolean enabled,int cellsPerTick,int reviewSeconds,int maximumActiveHomes,int searchRadius){this(enabled,cellsPerTick,reviewSeconds,maximumActiveHomes,searchRadius,0);}
    public static AutomaticHomePlanningSettings disabled(){return new AutomaticHomePlanningSettings(false,128,30,1,12);}
    public static AutomaticHomePlanningSettings read(ConfigurationSection root){
        Objects.requireNonNull(root);if(!root.contains("automatic-home-planning"))return disabled();
        var section=root.getConfigurationSection("automatic-home-planning");
        if(section==null||!section.getKeys(false).containsAll(Set.of("enabled","cells-per-tick","review-seconds","maximum-active-homes","search-radius"))||!Set.of("enabled","cells-per-tick","review-seconds","maximum-active-homes","search-radius","vertical-search").containsAll(section.getKeys(false)))
            throw new IllegalArgumentException("Missing or unknown automatic-home-planning settings");
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("Automatic home planning enabled must be true/false");
        int vertical=section.contains("vertical-search")?integer(section,"vertical-search"):0;
        return new AutomaticHomePlanningSettings(enabled,integer(section,"cells-per-tick"),integer(section,"review-seconds"),integer(section,"maximum-active-homes"),integer(section,"search-radius"),vertical);
    }
    private static int integer(ConfigurationSection section,String key){if(!(section.get(key) instanceof Integer n))throw new IllegalArgumentException("Automatic home planning "+key+" must be a whole number");return n;}
}
