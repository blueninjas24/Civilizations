package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.LivestockMarketPolicy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockMarketConfigurationTest {
    YamlConfiguration config()throws Exception {
        var config=new YamlConfiguration();try(var reader=new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/economy.yml")),StandardCharsets.UTF_8)){config.load(reader);}return config;
    }
    @Test void packagedDefaultsAreEditableAndUseTheExistingCurrencyScale()throws Exception {
        var c=config();EconomyConfiguration.read(c);assertEquals(LivestockMarketPolicy.prototype(),EconomyConfiguration.readLivestockMarket(c));
        c.set("livestock-market.milk-emeralds",5);c.set("livestock-market.eggs-per-order",16);c.set("livestock-market.egg-order-emeralds",3);c.set("purchases.trade-value-per-emerald",9);
        var policy=EconomyConfiguration.readLivestockMarket(c);assertEquals(5,policy.milkEmeralds());assertEquals(16,policy.eggsPerOrder());assertEquals(3,policy.eggEmeralds());assertEquals(900,policy.hundredthsPerEmerald());EconomyConfiguration.read(c);
    }
    @Test void oldFilesStayDisabledAndAreNotSilentlyRewritten()throws Exception {
        var c=config();c.set("livestock-market",null);assertFalse(EconomyConfiguration.readLivestockMarket(c).enabled());EconomyConfiguration.read(c);assertFalse(c.contains("livestock-market"));
    }
    @Test void malformedUnknownAndUnboundedMarketSettingsFailBeforeStartup()throws Exception {
        for(var entry:Map.<String,Object>of("enabled","yes","milk-emeralds",0,"eggs-per-order",65,"egg-order-emeralds",65,"horizontal-radius",129,"vertical-radius",-1,"maximum-sellers",33,"offer-seconds",301).entrySet()) {
            var c=config();c.set("livestock-market."+entry.getKey(),entry.getValue());assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(c),entry.getKey());
        }
        var c=config();c.set("livestock-market.eggs-per-order",4.5);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(c));
        var unknown=config();unknown.set("livestock-market.free-buckets",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(unknown));
    }
}
