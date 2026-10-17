package com.blueninjas24.civilizations.paper;

import java.util.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryRuntimeConfigurationTest {
    @Test void waterSettingsAreStrictEditableAndLegacyOptIn()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readWaterObserver(config).enabled());
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(WaterObserverPolicy.prototype(),EconomyConfiguration.readWaterObserver(config));
        config.set("water-observer.minimum-depth",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWaterObserver(config));
        config.set("water-observer.minimum-depth",3);assertEquals(3,EconomyConfiguration.readWaterObserver(config).minimumDepth());
        config.set("water-observer.cells-per-tick",4097);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWaterObserver(config));
        config.set("water-observer.cells-per-tick",512);config.set("water-observer.extra",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWaterObserver(config));
    }

    @Test void workplaceMarkersAreEditableStrictAndLegacyOptIn()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readWorkplaceObserver(config).enabled());
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(WorkplaceObserverPolicy.prototype(),EconomyConfiguration.readWorkplaceObserver(config));config.set("workplace-observer.markers",List.of("barrel"));assertEquals(1,EconomyConfiguration.readWorkplaceObserver(config).kinds().size());
        config.set("workplace-observer.markers",List.of("chest"));assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWorkplaceObserver(config));config.set("workplace-observer.markers",List.of("barrel","barrel"));assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWorkplaceObserver(config));
        config.set("workplace-observer.markers",List.of("barrel"));config.set("workplace-observer.extra",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWorkplaceObserver(config));
    }
    @Test void automaticMineSettingsAreEditableStrictAndLegacyOptIn()throws Exception {
        var config=new YamlConfiguration();assertFalse(EconomyConfiguration.readMineFounding(config).enabled());
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        assertEquals(MineFoundingPolicy.prototype(),EconomyConfiguration.readMineFounding(config));
        config.set("automatic-mines.mineral-yield-per-batch.coal",4);assertEquals(4,EconomyConfiguration.readMineFounding(config).mineralYield().get(com.blueninjas24.civilizations.economy.Commodity.COAL));
        config.set("automatic-mines.mineral-yield-per-batch.diamond",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readMineFounding(config));
        config.set("automatic-mines.mineral-yield-per-batch.diamond",null);config.set("automatic-mines.founder-mining-experience",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readMineFounding(config));
        config.set("automatic-mines.founder-mining-experience",10);config.set("automatic-mines.extra",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readMineFounding(config));
    }
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
    @Test void multipleBakeryRecipesAreAllowedButOtherDuplicateIndustriesAreRejected() {
        var map=new HashMap<String,ProductionRecipe>();map.put("smith_tools",ProductionRecipe.smithTools());
        var policy=new IndustryRuntimePolicy(true,1000,1,IndustryPolicy.prototype(),map);map.clear();assertEquals(1,policy.recipes().size());
        var a=ProductionRecipe.smithTools();var b=new ProductionRecipe("alternate",a.industry(),a.inputs(),a.outputs(),1,1);
        assertThrows(IllegalArgumentException.class,()->new IndustryRuntimePolicy(true,1000,1,IndustryPolicy.prototype(),Map.of(a.id(),a,b.id(),b)));
        var bread=ProductionRecipe.bakeryBread();var cake=ProductionRecipe.bakeryCake();
        var bakeries=Map.of(bread.id(),bread,cake.id(),cake);
        assertEquals(2,new IndustryRuntimePolicy(true,1000,1,IndustryPolicy.prototype(),bakeries).recipes().size());
        assertThrows(IllegalArgumentException.class,()->new IndustryRuntimePolicy(true,1000,1,IndustryPolicy.prototype(),Map.of("wrong_key",a)));
    }
    @Test void bakeryRecipesHaveStableRuntimeOrderAndSimpleStartupFallback() {
        var bread=ProductionRecipe.bakeryBread();var cake=ProductionRecipe.bakeryCake();
        var recipes=Map.of(bread.id(),bread,cake.id(),cake);
        var all=Set.of(new Commodity("grain"),new Commodity("milk"),new Commodity("eggs"),Commodity.FOOD);
        assertEquals(List.of(cake,bread),BakeryRecipes.ordered(recipes.values(),all));
        assertEquals(Optional.of(bread),BakeryRecipes.baseline(recipes.values(),all));
        var oldCatalog=Set.of(new Commodity("grain"),Commodity.FOOD);
        assertEquals(List.of(bread),BakeryRecipes.ordered(recipes.values(),oldCatalog));
        var weights=Map.of(new Commodity("grain"),1L,new Commodity("milk"),1L,new Commodity("eggs"),1L,Commodity.FOOD,1L);
        var stock=Map.of(new Commodity("grain"),3L,new Commodity("milk"),1L,new Commodity("eggs"),1L,Commodity.FOOD,0L);
        assertEquals(Optional.of(cake),BakeryRecipes.produced(BakeryRecipes.ordered(recipes.values(),all),stock,weights,100,1));
        var breadOnly=Map.of(new Commodity("grain"),3L,new Commodity("food"),0L);
        assertEquals(Optional.of(bread),BakeryRecipes.produced(BakeryRecipes.ordered(recipes.values(),oldCatalog),breadOnly,
                Map.of(new Commodity("grain"),1L,Commodity.FOOD,1L),100,1));
    }
    @Test void bundledCakeRecipeHasActualMilkEggInputsAndAbstractMealOutput()throws Exception {
        var config=new YamlConfiguration();
        try(var input=getClass().getResourceAsStream("/economy.yml");var reader=new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
        var cake=EconomyConfiguration.readRecipes(config).get("bakery_cake");
        assertEquals(ProductionRecipe.bakeryCake(),cake);
        var industry=EconomyConfiguration.readIndustry(config);
        assertEquals(1L,industry.storageSpacePerUnit().get(new Commodity("milk")));
        assertEquals(1L,industry.storageSpacePerUnit().get(new Commodity("eggs")));
        var prices=EconomyConfiguration.readWorkshopSupply(config).unitHundredths();
        assertEquals(150L,prices.get(new Commodity("milk")));
        assertEquals(75L,prices.get(new Commodity("eggs")));
        var before=EconomyConfiguration.read(config).startingPerResident();
        assertFalse(before.containsKey(new Commodity("milk")));assertFalse(before.containsKey(new Commodity("eggs")));
        config.set("industry-supply.input-unit-hundredths.milk",225L);
        assertEquals(225L,EconomyConfiguration.readWorkshopSupply(config).unitHundredths().get(new Commodity("milk")));
        assertEquals(before,EconomyConfiguration.read(config).startingPerResident());
        for(Object invalid:List.of(0,1.5,"many",true)) {
            config.set("industry-supply.input-unit-hundredths.milk",invalid);
            assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWorkshopSupply(config));
        }
        config.set("industry-supply.input-unit-hundredths",null);
        assertFalse(EconomyConfiguration.readWorkshopSupply(config).unitHundredths().containsKey(new Commodity("milk")));
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
