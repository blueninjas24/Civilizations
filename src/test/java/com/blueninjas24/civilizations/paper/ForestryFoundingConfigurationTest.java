package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.ForestryFoundingPolicy;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForestryFoundingConfigurationTest {
    @Test void shippedDefaultsEnableBoundedForestryAndRealSupplyBackedStudy() throws Exception {
        var config=defaults();
        assertEquals(ForestryFoundingPolicy.prototype(),EconomyConfiguration.readForestryFounding(config));
        var policy=EconomyConfiguration.readStudy(config);
        assertEquals(1,policy.gainPerPeriod());
        assertEquals(10,policy.skills().get("forestry").targetExperience());
        assertEquals(Map.of(Commodity.TIMBER,2L,Commodity.STONE,1L),policy.skills().get("forestry").materials());
        assertEquals(1,policy.foodReservePeriods());
        assertEquals(2,policy.materialReserveUnits());
        assertTrue(EconomyConfiguration.readCareerStudy(config).industries().contains("logging"));
        EconomyConfiguration.read(config);
    }

    @Test void missingFoundingSectionKeepsExistingInstallationsOptedOut() throws Exception {
        var config=defaults();config.set("forestry-founding",null);
        assertFalse(EconomyConfiguration.readForestryFounding(config).enabled());
        EconomyConfiguration.read(config);
    }

    @Test void unknownKeysAndInvalidFoundingLimitsFailStrictly() throws Exception {
        var unknown=defaults();unknown.set("forestry-founding.free-experience",true);
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(unknown));
        var wrongType=defaults();wrongType.set("forestry-founding.enabled","yes");
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(wrongType));
        for(var entry:Map.of("settlements-per-pulse",List.of(0,17),"survey-lifetime-seconds",List.of(0,3601),
                "worker-capacity",List.of(0,33),"founder-forestry-experience",List.of(0)).entrySet()) {
            for(var value:entry.getValue()) {
                var invalid=defaults();invalid.set("forestry-founding."+entry.getKey(),value);
                assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(invalid),entry.getKey()+"="+value);
            }
        }
    }

    private static YamlConfiguration defaults() throws Exception {
        var config=new YamlConfiguration();
        try(var input=Objects.requireNonNull(ForestryFoundingConfigurationTest.class.getResourceAsStream("/economy.yml"));
            var reader=new InputStreamReader(input,StandardCharsets.UTF_8)) {config.load(reader);}
        return config;
    }
}
