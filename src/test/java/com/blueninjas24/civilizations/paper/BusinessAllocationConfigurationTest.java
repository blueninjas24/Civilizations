package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.BusinessAllocationPolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class BusinessAllocationConfigurationTest {
    @Test void newDefaultsOptInOldFilesOptOutAndInvalidEditsFailBeforeStartup() {
        var config=YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/economy.yml"),StandardCharsets.UTF_8));
        assertEquals(BusinessAllocationPolicy.prototype(),EconomyConfiguration.readBusinessAllocation(config));EconomyConfiguration.read(config);
        config.set("business-allocation.review-millis",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readBusinessAllocation(config));
        config.set("business-allocation.review-millis",5000);config.set("business-allocation.typo",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readBusinessAllocation(config));
        config.set("business-allocation",null);assertFalse(EconomyConfiguration.readBusinessAllocation(config).enabled());
    }
}
