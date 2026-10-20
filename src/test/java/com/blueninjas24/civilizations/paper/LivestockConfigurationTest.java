package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.LivestockPolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockConfigurationTest {
    @Test void bundledResaleValuesCoverNewProducerGoodsAndRemainEditable()throws Exception{
        var config=new YamlConfiguration();try(var reader=new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(com.blueninjas24.civilizations.industry.BusinessResalePolicy.prototype(),EconomyConfiguration.readAutomaticResale(config));
        config.set("automatic-business-resale.stock-value-hundredths-per-unit.milk",450);
        assertEquals(450,EconomyConfiguration.readAutomaticResale(config).stockValues().get(new com.blueninjas24.civilizations.economy.Commodity("milk")));
    }
    @Test void adoptionIsEditableStrictAndMissingInstalledSectionOptsOut()throws Exception{
        var config=new YamlConfiguration();try(var reader=new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(com.blueninjas24.civilizations.industry.LivestockAdoptionPolicy.prototype(),EconomyConfiguration.readLivestockAdoption(config));EconomyConfiguration.read(config);
        config.set("livestock-adoption.clear-radius",4);assertEquals(4,EconomyConfiguration.readLivestockAdoption(config).clearRadius());
        config.set("livestock-adoption.minimum-adults",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        config.set("livestock-adoption",null);assertFalse(EconomyConfiguration.readLivestockAdoption(config).enabled());EconomyConfiguration.read(config);
    }
    @Test void liveWorkHasExplicitEditableLimitsAndInstalledFilesOptOut()throws Exception {
        var config=new YamlConfiguration();try(var reader=new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(com.blueninjas24.civilizations.industry.LivestockRuntimePolicy.prototype(),EconomyConfiguration.readLivestockRuntime(config));EconomyConfiguration.read(config);
        config.set("livestock-runtime.site-work-limit",20);assertEquals(20,EconomyConfiguration.readLivestockRuntime(config).siteWorkLimit());
        config.set("livestock-runtime.work-per-worker",1.5);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readLivestockRuntime(config));
        config.set("livestock-runtime",null);assertFalse(EconomyConfiguration.readLivestockRuntime(config).enabled());EconomyConfiguration.read(config);
    }
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
    @Test void sheepTermsAreEditableAndMissingOlderSectionKeepsLegacyFingerprint()throws Exception {
        var c=new YamlConfiguration();try(var r=new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8)){c.load(r);}
        var defaults=EconomyConfiguration.readLivestock(c);assertEquals(1,defaults.sheep().renewableUnitsPerAdult());
        c.set("livestock.sheep.renewable-units-per-adult",3);assertEquals(3,EconomyConfiguration.readLivestock(c).sheep().renewableUnitsPerAdult());
        c.set("livestock.sheep.feed-per-adult",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readLivestock(c));
        c.set("livestock.sheep",null);assertEquals(defaults,EconomyConfiguration.readLivestock(c));assertFalse(c.contains("livestock.sheep"));
        assertFalse(defaults.toString().contains("sheep"),"Default terms preserve existing operation receipt fingerprints");
    }

}
