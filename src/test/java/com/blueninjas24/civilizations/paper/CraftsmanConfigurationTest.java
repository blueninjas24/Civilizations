package com.blueninjas24.civilizations.paper;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CraftsmanConfigurationTest {
    YamlConfiguration config(){return YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8));}
    @Test void explicitEditablePrototypeAndMissingInstalledSectionsRemainUnchanged() {
        var c=config();EconomyConfiguration.read(c);assertEquals(WorkshopFoundingPolicy.prototype(),EconomyConfiguration.readCraftsmanFounding(c));assertEquals(WorkshopOpportunityPolicy.prototype(),EconomyConfiguration.readCraftsmanOpportunities(c));
        c.set("craftsman-founding.founder-crafting-experience",20);c.set("craftsman-opportunities.capital-hundredths",5000);
        assertEquals(20,EconomyConfiguration.readCraftsmanFounding(c).founderExperience());assertEquals(5000,EconomyConfiguration.readCraftsmanOpportunities(c).capitalHundredths());
        c.set("craftsman-founding",null);c.set("craftsman-opportunities",null);var before=c.saveToString();EconomyConfiguration.read(c);
        assertFalse(EconomyConfiguration.readCraftsmanFounding(c).enabled());assertFalse(EconomyConfiguration.readCraftsmanOpportunities(c).enabled());assertEquals(before,c.saveToString());
    }
    @Test void invalidExperienceOrUnboundedMarkerReviewIsRejectedBeforeEnable() {
        var c=config();c.set("craftsman-founding.founder-crafting-experience",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(c));
        var other=config();other.set("craftsman-opportunities.markers-per-review",1000);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(other));
    }
}
