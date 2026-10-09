package com.blueninjas24.civilizations.paper;

import java.util.*;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryRuntimeConfigurationTest {
    @Test void oldFilesStayDisabledAndNewSettingsAreStrictAndEditable() throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readIndustryRuntime(config).enabled());
        config.loadFromString("industry-runtime:\n  enabled: true\n  real-millis-per-period: 2000\n  businesses-per-pulse: 3\n");
        var policy=EconomyConfiguration.readIndustryRuntime(config);assertTrue(policy.enabled());assertEquals(2000,policy.millisPerPeriod());assertEquals(3,policy.businessesPerPulse());
        assertEquals(ProductionRecipe.smithTools(),policy.recipes().get("smith_tools"));
        config.set("industry-runtime.real-millis-per-period",999);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readIndustryRuntime(config));
        config.set("industry-runtime.real-millis-per-period",2000);config.set("industry-runtime.businesses-per-pulse",1.5);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readIndustryRuntime(config));
        config.set("industry-runtime.businesses-per-pulse",3);config.set("industry-runtime.invent-money",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readIndustryRuntime(config));
    }
    @Test void ambiguousRuntimeRecipesAreRejectedAndPolicyOwnsItsCatalog() {
        var map=new HashMap<String,ProductionRecipe>();map.put("smith_tools",ProductionRecipe.smithTools());
        var policy=new IndustryRuntimePolicy(true,1000,1,IndustryPolicy.prototype(),map);map.clear();assertEquals(1,policy.recipes().size());
        var a=ProductionRecipe.smithTools();var b=new ProductionRecipe("alternate",a.industry(),a.inputs(),a.outputs(),1,1);
        assertThrows(IllegalArgumentException.class,()->new IndustryRuntimePolicy(true,1000,1,IndustryPolicy.prototype(),Map.of(a.id(),a,b.id(),b)));
        assertThrows(IllegalArgumentException.class,()->new IndustryRuntimePolicy(true,1000,1,IndustryPolicy.prototype(),Map.of("wrong_key",a)));
    }
}
