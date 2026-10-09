package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.IndustryPolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class IndustryConfigurationTest {
    YamlConfiguration config() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/economy.yml"),StandardCharsets.UTF_8));
    }
    @Test void defaultFileAndMissingOldSectionHaveCompleteDefaults() {
        var config=config();EconomyConfiguration.read(config);
        assertEquals(IndustryPolicy.prototype(),EconomyConfiguration.readIndustry(config));
        config.set("industry",null);EconomyConfiguration.read(config);
        assertEquals(IndustryPolicy.prototype(),EconomyConfiguration.readIndustry(config));
    }
    @Test void adminsCanTuneAbundanceCapacitySpeedAndRockTypes() {
        var config=config();config.set("industry.deposit-batches-per-observed-ore",25);
        config.set("industry.batches-per-worker",4);config.set("industry.storage-capacity",512);
        config.set("industry.local-delivery-periods",3);config.set("industry.local-haul-units-per-period",64);
        config.set("industry.mining-bundle.andesite",2);config.set("industry.storage-space-per-unit.andesite",1);
        var settings=EconomyConfiguration.readIndustry(config);EconomyConfiguration.read(config);
        assertEquals(200,settings.initialDepositBatches(8));assertEquals(512,settings.storageCapacity());
        assertEquals(2L,settings.miningBundle().get(new Commodity("andesite")));assertEquals(3,settings.localDeliveryPeriods());
    }
    @Test void missingFieldsInvalidYieldsAndRegenerationSettingsFailExplicitly() {
        var config=config();config.set("industry.regeneration",true);var regeneration=config;
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(regeneration));
        config=config();config.set("industry.storage-space-per-unit.coal",null);var absent=config;
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readIndustry(absent));
        config=config();config.set("industry.batches-per-worker",0);var zero=config;
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readIndustry(zero));
        config=config();config.set("industry.site-batch-capacity",null);var missing=config;
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readIndustry(missing));
    }
}
