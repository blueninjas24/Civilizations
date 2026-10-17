package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.LivestockPolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockConfigurationTest {
    @Test void workingRadiusIsEditableAndLegacyFilesKeepAValidatedPrototype()throws Exception {
        var config=new YamlConfiguration();try(var reader=new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(32,EconomyConfiguration.readLivestockWorkingArea(config).radiusBlocks());config.set("livestock-working-area.radius-blocks",12);EconomyConfiguration.read(config);assertEquals(12,EconomyConfiguration.readLivestockWorkingArea(config).radiusBlocks());
        config.set("livestock-working-area",null);assertEquals(32,EconomyConfiguration.readLivestockWorkingArea(config).radiusBlocks());assertFalse(config.contains("livestock-working-area"));EconomyConfiguration.read(config);
    }
    @Test void invalidAndMisspelledWorkingRadiusFailsBeforeDatabaseStartup()throws Exception {
        for(Object value:java.util.List.of(0,257,"32",1.5,true)) {
            var config=new YamlConfiguration();config.set("livestock-working-area.radius-blocks",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readLivestockWorkingArea(config));
        }
        var config=new YamlConfiguration();config.set("livestock-working-area.raduis-blocks",32);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readLivestockWorkingArea(config));
    }
    @Test void defaultsAreStrictEditableAndOldFilesOptOut()throws Exception {
        var config=new YamlConfiguration();try(var reader=new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(LivestockPolicy.prototype(),EconomyConfiguration.readLivestock(config));EconomyConfiguration.read(config);
        config.set("livestock.cow.feed-per-adult",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readLivestock(config));
        config.set("livestock.cow.feed-per-adult",1);config.set("livestock.maximum-births-per-period",0);assertEquals(0,EconomyConfiguration.readLivestock(config).maximumBirthsPerPeriod());
        config.set("livestock",null);assertFalse(EconomyConfiguration.readLivestock(config).enabled());EconomyConfiguration.read(config);
    }
}
