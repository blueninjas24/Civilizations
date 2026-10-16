package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BakeryMarketConfigurationTest {
    @Test void newFileDefaultsAreEditableAndOldFilesStayOptedOut() {
        var c=YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8));
        EconomyConfiguration.read(c);assertEquals(BakeryMarketPolicy.prototype(),EconomyConfiguration.readBakeryMarket(c));
        assertEquals(StudyPolicy.prototype(),EconomyConfiguration.readStudy(c));assertEquals(CareerStudyPolicy.prototype(),EconomyConfiguration.readCareerStudy(c));
        assertEquals("baking",EconomyConfiguration.readWorkLearning(c).industrySkills().get("bakery"));
        c.set("bakery-market.maximum-share-basis-points",2500);assertEquals(2500,EconomyConfiguration.readBakeryMarket(c).maximumShareBasisPoints());
        c.set("bakery-market.maximum-share-basis-points",10001);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(c));
        c.set("bakery-market",null);c.set("resident-study.skills.baking",null);c.set("automatic-career-study.industries",java.util.List.of("mining"));var before=c.saveToString();
        assertFalse(EconomyConfiguration.readBakeryMarket(c).enabled());assertFalse(EconomyConfiguration.readStudy(c).skills().containsKey("baking"));
        assertEquals(java.util.Set.of("mining"),EconomyConfiguration.readCareerStudy(c).industries());assertEquals(before,c.saveToString());
        assertEquals(32,BakeryMarketPolicy.prototype().share(Long.MAX_VALUE));
    }
}
