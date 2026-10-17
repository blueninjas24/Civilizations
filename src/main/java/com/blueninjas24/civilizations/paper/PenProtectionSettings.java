package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.PenScan;
import org.bukkit.configuration.ConfigurationSection;

/** Editable bounded preview work; does not enable adoption or grant land authority. */
public record PenProtectionSettings(boolean enabled,int radius,int maximumArea,int chunksPerTick,
                                    int cellsPerTick,int maximumAnimals,int maximumRequests,int previewSeconds) {
    public PenProtectionSettings {
        new PenScan.Limits(radius,maximumArea);
        if(chunksPerTick<1||chunksPerTick>4||cellsPerTick<1||cellsPerTick>4096||maximumAnimals<1||maximumAnimals>256
                ||maximumRequests<1||maximumRequests>16||previewSeconds<10||previewSeconds>300)
            throw new IllegalArgumentException("Invalid pen protection limits");
    }
    public static PenProtectionSettings prototype(){return new PenProtectionSettings(true,32,1024,2,64,128,8,60);}
    public static PenProtectionSettings read(ConfigurationSection config){
        var defaults=prototype();var section=config.getConfigurationSection("pen-protection");
        if(section==null){if(config.contains("pen-protection"))throw new IllegalArgumentException("pen-protection must be a section");return defaults;}
        Object enabled=section.get("enabled",defaults.enabled());
        if(!(enabled instanceof Boolean flag))throw new IllegalArgumentException("pen-protection.enabled must be true or false");
        return new PenProtectionSettings(flag,integer(section,"radius",defaults.radius()),integer(section,"maximum-area",defaults.maximumArea()),
                integer(section,"chunks-per-tick",defaults.chunksPerTick()),integer(section,"cells-per-tick",defaults.cellsPerTick()),
                integer(section,"maximum-animals",defaults.maximumAnimals()),integer(section,"maximum-requests",defaults.maximumRequests()),integer(section,"preview-seconds",defaults.previewSeconds()));
    }
    private static int integer(ConfigurationSection section,String path,int fallback){
        Object value=section.get(path,fallback);
        if(!(value instanceof Byte||value instanceof Short||value instanceof Integer||value instanceof Long))throw new IllegalArgumentException("pen-protection."+path+" must be an integer");
        try{return Math.toIntExact(((Number)value).longValue());}catch(ArithmeticException failure){throw new IllegalArgumentException("pen-protection."+path+" is too large",failure);}
    }
}
