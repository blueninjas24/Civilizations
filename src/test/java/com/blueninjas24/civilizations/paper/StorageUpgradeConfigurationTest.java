package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageUpgradeConfigurationTest {
    YamlConfiguration config(){return YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8));}
    @Test void editableNewDefaultsAreExplicitAndOldFilesAreNotRewritten() {
        var c=config();EconomyConfiguration.read(c);assertEquals(StorageUpgradePolicy.prototype(),EconomyConfiguration.readStorageUpgrades(c));
        assertEquals(StorageUpgradePolicy.prototype(),EconomyConfiguration.readIndustryRuntime(c).storageUpgrades());
        assertEquals(ProductionRecipe.storageFittings(),EconomyConfiguration.readRecipes(c).get("craft_storage_fittings"));
        var economy=EconomyConfiguration.read(c);long inputs=0;
        for(var e:ProductionRecipe.storageFittings().inputs().entrySet())inputs+=economy.baseTradeValues().get(e.getKey()).movePointRight(2).longValueExact()*e.getValue();
        assertTrue(EconomyConfiguration.readStorageUpgrades(c).invoice()>inputs,"Default sale covers actual recipe ingredient prices; not a net-profit guarantee");
        var fitting=new Commodity("storage_fittings");assertEquals(0,EconomyConfiguration.read(c).startingReserves(20).get(fitting));assertEquals(1,EconomyConfiguration.readIndustry(c).storageSpacePerUnit().get(fitting));
        c.set("storage-upgrades.maximum-upgrades",2);assertEquals(2,EconomyConfiguration.readStorageUpgrades(c).maximumUpgrades());
        c.set("storage-upgrades",null);c.set("recipes.craft_storage_fittings",null);c.set("industry.storage-space-per-unit.storage_fittings",null);var before=c.saveToString();
        assertFalse(EconomyConfiguration.readStorageUpgrades(c).enabled());assertFalse(EconomyConfiguration.readIndustryRuntime(c).storageUpgrades().enabled());
        assertFalse(EconomyConfiguration.readRecipes(c).containsKey("craft_storage_fittings"));assertFalse(EconomyConfiguration.readIndustry(c).storageSpacePerUnit().containsKey(fitting));assertEquals(before,c.saveToString());
    }
    @Test void invalidLimitsAndUnsupportedSpecialStoreAdaptersFailBeforeStartup() {
        for(var key:java.util.List.of("capacity-per-upgrade","maximum-upgrades","maximum-growth-percent","maximum-capacity","fittings-per-upgrade","price-per-fitting","delivery-periods","suppliers-per-review")) {
            var c=config();c.set("storage-upgrades."+key,0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(c));
        }
        var c=config();c.set("storage-upgrades.industries",java.util.List.of("farming"));assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(c));
        assertEquals(200,StorageUpgradePolicy.prototype().nextCapacity(100,100,0,80,6000));
        assertEquals(100,StorageUpgradePolicy.prototype().nextCapacity(100,100,0,79,6000));
        assertEquals(100,StorageUpgradePolicy.prototype().nextCapacity(100,100,4,100,6000));
    }
}
