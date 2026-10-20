package com.blueninjas24.civilizations.paper;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Editable bounded player-declared exclusions; cannot enable automatic construction or donation. */
public record BuildingProtectionSettings(boolean enabled,int maximumEdge,int maximumArea,int maximumRequests,
                                         int previewSeconds,int selectionSeconds) {
    public BuildingProtectionSettings {
        if(maximumEdge<1||maximumEdge>512||maximumArea<1||maximumArea>262144||maximumRequests<1||maximumRequests>32
                ||previewSeconds<10||previewSeconds>300||selectionSeconds<30||selectionSeconds>1800)
            throw new IllegalArgumentException("Invalid building protection limits");
    }
    public static BuildingProtectionSettings prototype(){return new BuildingProtectionSettings(true,128,4096,16,60,300);}
    public static BuildingProtectionSettings read(ConfigurationSection root) {
        var section=root.getConfigurationSection("building-protection");var d=prototype();
        if(section==null){if(root.contains("building-protection"))throw new IllegalArgumentException("building-protection must be a section");return d;}
        if(!Set.of("enabled","maximum-edge","maximum-area","maximum-requests","preview-seconds","selection-seconds").containsAll(section.getKeys(false)))
            throw new IllegalArgumentException("Unknown building-protection setting");
        Object enabled=section.get("enabled",d.enabled());if(!(enabled instanceof Boolean flag))throw new IllegalArgumentException("building-protection.enabled must be true/false");
        return new BuildingProtectionSettings(flag,integer(section,"maximum-edge",d.maximumEdge()),integer(section,"maximum-area",d.maximumArea()),
                integer(section,"maximum-requests",d.maximumRequests()),integer(section,"preview-seconds",d.previewSeconds()),integer(section,"selection-seconds",d.selectionSeconds()));
    }
    private static int integer(ConfigurationSection section,String key,int fallback) {
        var value=section.get(key,fallback);
        if(!(value instanceof Integer||value instanceof Long))throw new IllegalArgumentException("building-protection."+key+" must be a whole number");
        try{return Math.toIntExact(((Number)value).longValue());}catch(ArithmeticException failure){throw new IllegalArgumentException("Building protection number too large",failure);}
    }
}
