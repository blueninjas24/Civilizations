package com.blueninjas24.civilizations.paper;
import com.blueninjas24.civilizations.industry.*;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LivestockStartupConfigurationTest {
    @Test void livestockSharesExistingLimitsAndOldEligibilityListsArePreserved() {
        var c=YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8));
        EconomyConfiguration.read(c);var p=EconomyConfiguration.readStartupGrants(c);assertEquals(StartupGrantPolicy.prototype(),p);assertTrue(p.industries().contains("livestock"));
        assertEquals(5000,p.maximumGrant());assertEquals(5000,p.perPeriodLimit());assertEquals(20000,p.lifetimeLimit());assertEquals(2500,p.minimumTreasury());assertEquals(2,p.foodReservePeriods());
        c.set("startup-grants.industries",List.of("mining","blacksmith"));var before=c.saveToString();assertFalse(EconomyConfiguration.readStartupGrants(c).industries().contains("livestock"));assertEquals(before,c.saveToString());
    }
}
