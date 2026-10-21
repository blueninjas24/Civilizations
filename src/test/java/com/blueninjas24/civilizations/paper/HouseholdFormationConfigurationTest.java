package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.population.HouseholdFormationPolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HouseholdFormationConfigurationTest {
    @Test void defaultsAndEditableLimitsAreStrict(){
        var config=new YamlConfiguration();assertEquals(HouseholdFormationPolicy.prototype(),HouseholdFormationConfiguration.read(config));
        config.set("household-formation.enabled",false);config.set("household-formation.maximum-per-month",4);config.set("household-formation.maximum-candidates",32);
        assertEquals(new HouseholdFormationPolicy(false,4,32),HouseholdFormationConfiguration.read(config));
        config.set("household-formation.maximum-per-month",0);assertThrows(IllegalArgumentException.class,()->HouseholdFormationConfiguration.read(config));
        config.set("household-formation.maximum-per-month",2.5);assertThrows(IllegalArgumentException.class,()->HouseholdFormationConfiguration.read(config));
        config.set("household-formation.maximum-per-month",2);config.set("household-formation.unknown",true);assertThrows(IllegalArgumentException.class,()->HouseholdFormationConfiguration.read(config));
    }
}
