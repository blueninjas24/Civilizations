package com.blueninjas24.civilizations.paper;

import java.util.*;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryRuntimeConfigurationTest {
    @Test void contributionSettingsAreEditableStrictAndLegacyOptIn()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readUpkeepContributions(config).enabled());
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(UpkeepContributionPolicy.prototype(),EconomyConfiguration.readUpkeepContributions(config));
        config.set("upkeep-contributions.spare-cash-percent",101);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readUpkeepContributions(config));
        config.set("upkeep-contributions.spare-cash-percent",50);config.set("upkeep-contributions.extra",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readUpkeepContributions(config));
    }
    @Test void farmWorkSettingsAreStrictAndLegacyFilesStayOptedOut()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readFarmTools(config).enabled());
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(FarmToolUpkeepPolicy.prototype(),EconomyConfiguration.readFarmTools(config));
        config.set("farm-tools.missing-tools-output-percent",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readFarmTools(config));
        config.set("farm-tools.missing-tools-output-percent",50);config.set("farm-tools.suppliers-per-review",65);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readFarmTools(config));
    }
    @Test void farmEquipmentBudgetIsStrictAndMissingLegacySectionIsDisabled()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readFarmToolBudget(config).enabled());
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(FarmToolBudgetPolicy.prototype(),EconomyConfiguration.readFarmToolBudget(config));
        config.set("farm-tool-budget.food-reserve-periods",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readFarmToolBudget(config));
        config.set("farm-tool-budget.food-reserve-periods",2);config.set("farm-tool-budget.extra",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readFarmToolBudget(config));
    }
    @Test void staffingIsOptInForOldFilesAndBudgetsAreStrict()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readBusinessStaffing(config).enabled());
        config.loadFromString("automatic-business-staffing:\n  enabled: true\n  candidates-per-review: 64\n  hires-per-review: 2\n  industries: [mining, blacksmith, warehouse]\n");
        assertTrue(EconomyConfiguration.readBusinessStaffing(config).enabled());
        config.set("automatic-business-staffing.candidates-per-review",513);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readBusinessStaffing(config));
        config.set("automatic-business-staffing.candidates-per-review",64);config.set("automatic-business-staffing.industries",List.of("farming"));assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readBusinessStaffing(config));
    }
    @Test void warehouseGrantsShareBundledLimitsAndLegacyIndustryListsStayExplicit()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readStartupGrants(config).enabled());
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        var bundled=EconomyConfiguration.readStartupGrants(config);
        assertEquals(StartupGrantPolicy.prototype(),bundled);assertTrue(bundled.industries().contains("warehouse"));
        config.set("startup-grants.industries",List.of("mining","blacksmith"));
        var old=EconomyConfiguration.readStartupGrants(config);assertFalse(old.industries().contains("warehouse"));
        assertEquals(bundled.maximumGrant(),old.maximumGrant());assertEquals(bundled.perPeriodLimit(),old.perPeriodLimit());assertEquals(bundled.lifetimeLimit(),old.lifetimeLimit());
    }
    @Test void toolsAreExplicitEditableAndDoNotRequireFreeVillageStocks()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readToolUpkeep(config).enabled());
        config.loadFromString("business-tools:\n  enabled: true\n  startup-grace-periods: 3\n  work-periods-per-tool: 8\n  missing-tools-output-percent: 50\n  tool-value-hundredths: 4000\n  industries: [mining, blacksmith]\n");
        var policy=EconomyConfiguration.readToolUpkeep(config);assertTrue(policy.enabled());assertEquals(4000,policy.toolValueHundredths());
        config.set("business-tools.work-periods-per-tool",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readToolUpkeep(config));
        config.set("business-tools.work-periods-per-tool",8);config.set("business-tools.industries",List.of("mining","mining"));assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readToolUpkeep(config));
        config.set("business-tools.industries",List.of("mining"));config.set("business-tools.extra",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readToolUpkeep(config));
    }
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
    @Test void bundledCommerceSettingsMatchEditablePrototypes()throws Exception {
        var config=new YamlConfiguration();
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(StudyPolicy.prototype(),EconomyConfiguration.readStudy(config));
        assertEquals(WorkLearningPolicy.prototype(),EconomyConfiguration.readWorkLearning(config));
        config.set("resident-study.skills.commerce.target-experience",6);assertEquals(6,EconomyConfiguration.readStudy(config).skills().get("commerce").targetExperience());
        config.set("work-learning.industry-skills.warehouse",null);assertFalse(EconomyConfiguration.readWorkLearning(config).industrySkills().containsKey("warehouse"));
    }

    @Test void automaticCareerSettingsAreStrictEditableAndLegacyOptIn()throws Exception {
        var config=new YamlConfiguration();assertEquals(CareerStudyPolicy.disabled(),EconomyConfiguration.readCareerStudy(config));
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(CareerStudyPolicy.prototype(),EconomyConfiguration.readCareerStudy(config));assertDoesNotThrow(()->EconomyConfiguration.read(config));
        config.set("automatic-career-study.candidates-per-review",513);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readCareerStudy(config));
        config.set("automatic-career-study.candidates-per-review",1);config.set("automatic-career-study.industries",List.of("mining","mining"));assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readCareerStudy(config));
        config.set("automatic-career-study.industries",List.of("mining"));config.set("automatic-career-study.extra",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readCareerStudy(config));
    }

}
