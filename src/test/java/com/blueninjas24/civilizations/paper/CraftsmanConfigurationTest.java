package com.blueninjas24.civilizations.paper;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class CraftsmanConfigurationTest {
    YamlConfiguration config(){return YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8));}
    @Test void multipleCraftRecipesAreReadWithoutRewritingSettingsOrRelaxingOtherIndustryLimits() {
        var c=config();
        var baseline=c.getConfigurationSection("recipes.craft_storage_fittings");var stone=c.getConfigurationSection("recipes.craft_storage_fittings_stone");
        assertNotNull(baseline);assertNotNull(stone);
        assertEquals("craftsman",stone.getString("industry"));assertEquals(3,stone.getInt("inputs.stone"));
        assertEquals(1,stone.getInt("outputs.storage_fittings"));assertEquals(1,stone.getInt("batches-per-worker"));assertEquals(4,stone.getInt("site-batch-capacity"));
        var before=c.saveToString();var policy=EconomyConfiguration.readIndustryRuntime(c);
        assertEquals(2,policy.recipes().values().stream().filter(r->r.industry().equals("craftsman")).count());
        assertEquals(Set.of("craft_storage_fittings","craft_storage_fittings_stone"),policy.recipes().values().stream().filter(r->r.industry().equals("craftsman")).map(ProductionRecipe::id).collect(java.util.stream.Collectors.toSet()));
        assertEquals(before,c.saveToString());
        c.set("recipes.craft_storage_fittings_stone.inputs.stone",4);
        assertEquals(4,EconomyConfiguration.readIndustryRuntime(c).recipes().get("craft_storage_fittings_stone").inputs().get(new com.blueninjas24.civilizations.economy.Commodity("stone")));
        c.set("recipes.craft_storage_fittings_stone.industry","blacksmith");
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readIndustryRuntime(c));
    }
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
    @Test void trainingAndWorkLearningAreExplicitEditableAndOldListsStayUnchanged() {
        var c=config();var study=EconomyConfiguration.readStudy(c);assertEquals(10,study.skills().get("crafting").targetExperience());assertEquals(java.util.Map.of(com.blueninjas24.civilizations.economy.Commodity.TIMBER,1L),study.skills().get("crafting").materials());
        assertTrue(EconomyConfiguration.readCareerStudy(c).industries().contains("craftsman"));assertEquals("crafting",EconomyConfiguration.readWorkLearning(c).industrySkills().get("craftsman"));
        c.set("resident-study.skills.crafting",null);c.set("work-learning.industry-skills.craftsman",null);c.set("automatic-career-study.industries",java.util.List.of("mining","blacksmith"));var before=c.saveToString();
        assertFalse(EconomyConfiguration.readStudy(c).skills().containsKey("crafting"));assertFalse(EconomyConfiguration.readCareerStudy(c).industries().contains("craftsman"));assertFalse(EconomyConfiguration.readWorkLearning(c).industrySkills().containsKey("craftsman"));assertEquals(before,c.saveToString());
    }

}
