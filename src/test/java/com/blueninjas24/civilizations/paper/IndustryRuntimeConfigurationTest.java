package com.blueninjas24.civilizations.paper;

import java.util.*;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryRuntimeConfigurationTest {
    @Test void warehouseRestockingIsExplicitEditableAndStrict()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readWarehouseSupply(config).enabled());
        config.loadFromString("warehouse-supply:\n  enabled: true\n  customers-per-review: 8\n  producers-per-review: 12\n");
        assertEquals(new WarehouseSupplyPolicy(true,8,12),EconomyConfiguration.readWarehouseSupply(config));
        config.set("warehouse-supply.customers-per-review",65);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWarehouseSupply(config));
        config.set("warehouse-supply.customers-per-review",8);config.set("warehouse-supply.enabled","yes");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWarehouseSupply(config));
        config.set("warehouse-supply.enabled",true);config.set("warehouse-supply.unknown",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWarehouseSupply(config));
    }
    @Test void supplyReusesEditablePricesAndAbsentOldSectionDoesNotOptIn() throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readWorkshopSupply(config).enabled());
        config.loadFromString("industry-supply:\n  enabled: true\n  input-periods: 2\n  warehouses-per-review: 16\nprices:\n  base-trade-value:\n    iron: 12.25\n    coal: 6\n");
        var policy=EconomyConfiguration.readWorkshopSupply(config);assertTrue(policy.enabled());
        assertEquals(1225,policy.unitHundredths().get(com.blueninjas24.civilizations.economy.Commodity.IRON));
        config.set("prices.base-trade-value.iron",12.001);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWorkshopSupply(config));
        config.set("prices.base-trade-value.iron",12);config.set("industry-supply.input-periods",9);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWorkshopSupply(config));
        config.set("industry-supply.input-periods",2);config.set("industry-supply.enabled","yes");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWorkshopSupply(config));
        config.set("industry-supply.enabled",true);config.set("industry-supply.unknown",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWorkshopSupply(config));
    }
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
