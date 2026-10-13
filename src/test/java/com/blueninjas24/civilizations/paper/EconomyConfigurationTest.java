package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EconomyConfigurationTest {
    @Test void farmMarketSettingsAreStrictEditableAndMissingOldSectionOptsOut()throws Exception {
        var c=defaults();assertEquals(FarmMarketPolicy.prototype(),EconomyConfiguration.readFarmMarket(c));EconomyConfiguration.read(c);
        c.set("farm-market.village-share-percent",0);EconomyConfiguration.read(c);assertEquals(0,EconomyConfiguration.readFarmMarket(c).villageSharePercent());
        c.set("farm-market",null);EconomyConfiguration.read(c);assertFalse(EconomyConfiguration.readFarmMarket(c).enabled());
        for(Object value:List.of(-1,101,1.5,"all",true)){var bad=defaults();bad.set("farm-market.village-share-percent",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(bad));}
        var bad=defaults();bad.set("farm-market.free-money",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(bad));
    }
    @Test void grainHarvestTermsAreEditableStrictAndOldFilesOptOut()throws Exception {
        var c=defaults();assertEquals(GrainHarvestPolicy.prototype(),EconomyConfiguration.readGrainHarvest(c));EconomyConfiguration.read(c);
        c.set("grain-harvest.grain-share-percent",0);EconomyConfiguration.read(c);assertEquals(0,EconomyConfiguration.readGrainHarvest(c).grainSharePercent());
        c.set("grain-harvest",null);EconomyConfiguration.read(c);assertFalse(EconomyConfiguration.readGrainHarvest(c).enabled());
        for(Object value:List.of(-1,101,1.5,"many",true)){var bad=defaults();bad.set("grain-harvest.grain-share-percent",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(bad));}
        var bad=defaults();bad.set("grain-harvest.free-food",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(bad));
    }
    @Test void fisheryShorelineBoundsAreEditableAndOldFilesOptOut()throws Exception {
        var c=defaults();assertEquals(com.blueninjas24.civilizations.industry.FisheryShorelinePolicy.prototype(),EconomyConfiguration.readFisheryShoreline(c));
        c.set("fishery-shoreline.maximum-distance",0);EconomyConfiguration.read(c);assertEquals(0,EconomyConfiguration.readFisheryShoreline(c).maximumDistance());
        c.set("fishery-shoreline",null);EconomyConfiguration.read(c);assertFalse(EconomyConfiguration.readFisheryShoreline(c).enabled());
        for(Object value:List.of(-1,257,1.5,"far",true)){var bad=defaults();bad.set("fishery-shoreline.maximum-distance",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(bad));}
    }
    @Test void fisheryFounderQualificationIsStrictEditableAndMissingSectionOptsOut()throws Exception {
        var c=defaults();assertEquals(com.blueninjas24.civilizations.industry.FisheryFoundingPolicy.prototype(),EconomyConfiguration.readFisheryFounding(c));
        c.set("fishery-founding.founder-fishing-experience",20);EconomyConfiguration.read(c);assertEquals(20,EconomyConfiguration.readFisheryFounding(c).founderExperience());
        c.set("fishery-founding",null);EconomyConfiguration.read(c);assertFalse(EconomyConfiguration.readFisheryFounding(c).enabled());
        for(Object value:List.of(0,11,1.5,"expert",true)){var bad=defaults();bad.set("fishery-founding.worker-fishing-experience",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(bad));}
        var extra=defaults();extra.set("fishery-founding.free-experience",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(extra));
    }
    @Test void fisheryOpportunityTermsAreStrictEditableAndMissingSectionOptsOut()throws Exception {
        var c=defaults();assertEquals(com.blueninjas24.civilizations.industry.FisheryOpportunityPolicy.prototype(),EconomyConfiguration.readFisheryOpportunities(c));
        c.set("fishery-opportunities.maximum-columns",32);EconomyConfiguration.read(c);assertEquals(32,EconomyConfiguration.readFisheryOpportunities(c).maximumColumns());
        c.set("fishery-opportunities",null);EconomyConfiguration.read(c);assertFalse(EconomyConfiguration.readFisheryOpportunities(c).enabled());
        for(Object value:List.of(0,257,1.5,"many",true)){var bad=defaults();bad.set("fishery-opportunities.maximum-columns",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(bad));}
        var extra=defaults();extra.set("fishery-opportunities.free-fish",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(extra));
    }
    @Test void fishingTermsAreEditableAndMissingOldSectionOptsOut() throws Exception {
        var config = defaults();
        assertEquals(com.blueninjas24.civilizations.industry.FishingPolicy.prototype(), EconomyConfiguration.readFishingResources(config));
        config.set("fishing-resources.recovery-basis-points", 0);
        config.set("fishing-resources.initial-stock-percent", 0);
        EconomyConfiguration.read(config);
        assertEquals(0, EconomyConfiguration.readFishingResources(config).recoveryBasisPoints());
        assertEquals(0, EconomyConfiguration.readFishingResources(config).initialStockPercent());
        config.set("fishing-resources", null);
        EconomyConfiguration.read(config);
        assertFalse(EconomyConfiguration.readFishingResources(config).enabled());
    }
    @Test void invalidFishingEditsFailBeforeDatabaseStartup() throws Exception {
        for (Object value : List.of(-1, 10001, 1.5, "fast")) {
            var config = defaults(); config.set("fishing-resources.recovery-basis-points", value);
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        }
        var config = defaults(); config.set("fishing-resources.maximum-stock", 100);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        var extra = defaults(); extra.set("fishing-resources.offline-growth", true);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(extra));
        var wrong = defaults(); wrong.set("fishing-resources.enabled", "yes");
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(wrong));
    }
    @Test void villageSupplyBudgetsAreStrictEditableAndOldFilesOptOut()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.VillageSupplyPolicy.prototype(),EconomyConfiguration.readVillageSupplies(config));
        config.set("village-supply-purchases.prices.timber",175);EconomyConfiguration.read(config);assertEquals(175,EconomyConfiguration.readVillageSupplies(config).prices().get(Commodity.TIMBER));
        config.set("village-supply-purchases",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readVillageSupplies(config).enabled());
    }
    @Test void emergencyFoodStockTargetIsEditableAndMissingLegacyKeyPreservesOldRules()throws Exception {
        var config=defaults();config.set("village-supply-purchases.food-stock-reserve-periods",3);EconomyConfiguration.read(config);
        assertEquals(3,EconomyConfiguration.readVillageSupplies(config).foodStockReservePeriods());
        config.set("village-supply-purchases.food-stock-reserve-periods",null);EconomyConfiguration.read(config);
        assertEquals(0,EconomyConfiguration.readVillageSupplies(config).foodStockReservePeriods());
        for(Object value:List.of(-1,101,1.5,"many",true)){config.set("village-supply-purchases.food-stock-reserve-periods",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));}
    }
    @Test void malformedVillageSpendingRulesFailBeforeDatabaseStartup()throws Exception {
        for(Object value:List.of(0,-1,1.5,"free")){var config=defaults();config.set("village-supply-purchases.prices.food",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));}
        var config=defaults();config.set("village-supply-purchases.food-budget-per-period",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var invalid=defaults();invalid.set("village-supply-purchases.enabled","yes");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(invalid));
    }
    @Test void finiteQuarryTermsAreEditableAndLegacySettingsOptOut()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.QuarryPolicy.prototype(),EconomyConfiguration.readQuarryResources(config));
        config.set("quarry-resources.sand.units-per-batch",7);EconomyConfiguration.read(config);assertEquals(7,EconomyConfiguration.readQuarryResources(config).sand().unitsPerBatch());
        config.set("quarry-resources",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readQuarryResources(config).enabled());
    }
    @Test void malformedQuarryTermsFailBeforeOpeningDatabase()throws Exception {
        for(Object value:List.of(0,-1,1.5,"lots")){var config=defaults();config.set("quarry-resources.stone.reserve-batches-per-block",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));}
        var config=defaults();config.set("quarry-resources.enabled","yes");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var extra=defaults();extra.set("quarry-resources.sand.free-respawn",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(extra));
    }
    @Test void existingIndustryOptInsArePreservedWithoutAddingBroaderGrants()throws Exception {
        var config=defaults();config.set("startup-grants.industries",List.of("mining","blacksmith","warehouse"));
        EconomyConfiguration.read(config);
        assertEquals(Set.of("mining","blacksmith","warehouse"),EconomyConfiguration.readStartupGrants(config).industries());
        assertFalse(EconomyConfiguration.readStartupGrants(config).review("logging",10000,2,1000,0,0).eligible());
    }
    @Test void warehouseFoundingQualificationIsEditableAndLegacyDisabled()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy.prototype(),EconomyConfiguration.readWarehouseFounding(config));
        config.set("warehouse-founding.founder-commerce-experience",20);EconomyConfiguration.read(config);assertEquals(20,EconomyConfiguration.readWarehouseFounding(config).founderExperience());
        config.set("warehouse-founding",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readWarehouseFounding(config).enabled());
    }
    @Test void malformedWarehouseFoundingQualificationFailsBeforeDatabaseStartup()throws Exception {
        for(Object value:List.of(0,513,1.5,"many")){var config=defaults();config.set("warehouse-founding.founders-per-review",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));}
        var config=defaults();config.set("warehouse-founding.enabled","yes");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var invalid=defaults();invalid.set("warehouse-founding.extra",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var mismatch=defaults();mismatch.set("warehouse-founding.worker-commerce-experience",11);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(mismatch));
    }
    @Test void warehouseOpportunityPrototypeIsEditableAndLegacyDisabled()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.WarehouseOpportunityPolicy.prototype(),EconomyConfiguration.readWarehouseOpportunities(config));
        config.set("warehouse-opportunities.capital-hundredths",3500);EconomyConfiguration.read(config);assertEquals(3500,EconomyConfiguration.readWarehouseOpportunities(config).capitalHundredths());
        config.set("warehouse-opportunities",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readWarehouseOpportunities(config).enabled());
    }
    @Test void malformedWarehouseOpportunitySettingsFailBeforeDatabaseStartup()throws Exception {
        for(Object value:List.of(0,65,1.5,"many")){var config=defaults();config.set("warehouse-opportunities.markers-per-review",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));}
        var config=defaults();config.set("warehouse-opportunities.enabled","yes");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var invalid=defaults();invalid.set("warehouse-opportunities.extra",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(invalid));
    }
    @Test void workshopOpportunityPrototypeIsEditableAndLegacyDisabled()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy.prototype(),EconomyConfiguration.readWorkshopOpportunities(config));
        config.set("workshop-opportunities.capital-hundredths",3500);EconomyConfiguration.read(config);assertEquals(3500,EconomyConfiguration.readWorkshopOpportunities(config).capitalHundredths());
        config.set("workshop-opportunities",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readWorkshopOpportunities(config).enabled());
    }
    @Test void malformedWorkshopOpportunityBudgetsFailBeforeDatabaseStartup()throws Exception {
        for(Object value:List.of(0,65,1.5,"many")) {var config=defaults();config.set("workshop-opportunities.markers-per-review",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));}
        var config=defaults();config.set("workshop-opportunities.extra",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    @Test void starterSupplyDefaultsAreEditablePricesReusedAndLegacyDisabled()throws Exception {
        var config=defaults();var policy=EconomyConfiguration.readWorkshopStarterSupplies(config);
        assertTrue(policy.enabled());assertEquals(1,policy.maximumBatches());assertEquals(4000,policy.maximumHundredths());assertEquals(2,policy.reserveUnits());assertFalse(policy.prices().containsKey(Commodity.FOOD));
        config.set("prices.base-trade-value.iron",17.25);assertEquals(1725,EconomyConfiguration.readWorkshopStarterSupplies(config).prices().get(Commodity.IRON));
        config.set("workshop-starter-supplies",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readWorkshopStarterSupplies(config).enabled());
    }
    @Test void malformedStarterSupplySettingsFailBeforeDatabaseStartup()throws Exception {
        for(Object value:List.of(0,17,1.5,"one")) {var config=defaults();config.set("workshop-starter-supplies.maximum-recipe-batches",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));}
        var config=defaults();config.set("workshop-starter-supplies.protected-units-per-material",-1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var malformed=defaults();malformed.set("workshop-starter-supplies.extra",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(malformed));
    }
    @Test void startupGrantLimitsAreEditableAndAbsentLegacySectionIsDisabled()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.StartupGrantPolicy.prototype(),EconomyConfiguration.readStartupGrants(config));
        config.set("startup-grants.maximum-grant-hundredths",1234);EconomyConfiguration.read(config);assertEquals(1234,EconomyConfiguration.readStartupGrants(config).maximumGrant());
        config.set("startup-grants",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readStartupGrants(config).enabled());
    }
    @Test void invalidGrantBudgetsFoodProtectionKindsAndUnknownKeysFailBeforeDatabaseStartup()throws Exception {
        for(Object bad:List.of(0,-1,1.5,"lots")) {
            var config=defaults();config.set("startup-grants.food-reserve-periods",bad);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("startup-grants.industries",List.of("mining","mining"));assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var invalid=defaults();invalid.set("startup-grants.unlimited",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(invalid));
    }
    @Test void automaticResaleDefaultsAreEditableAndOldFilesRemainDisabled()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.BusinessResalePolicy.prototype(),EconomyConfiguration.readAutomaticResale(config));
        config.set("automatic-business-resale.sale-deadline-seconds",1200);EconomyConfiguration.read(config);assertEquals(1200000,EconomyConfiguration.readAutomaticResale(config).deadlineMillis());
        config.set("automatic-business-resale",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readAutomaticResale(config).enabled());
    }
    @Test void invalidResaleRatesValuesAndBudgetsFailBeforeDatabaseStartup()throws Exception {
        for(Object value:List.of(0,129,1.5,"many")) {
            var config=defaults();config.set("automatic-business-resale.buyers-per-review",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("automatic-business-resale.stock-value-hundredths-per-unit.coal",0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    @Test void studyCostsAndBudgetsAreEditableAndLegacyFilesRemainDisabled()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.StudyPolicy.prototype(),EconomyConfiguration.readStudy(config));
        config.set("resident-study.skills.mining.practice-supplies.stone",3);EconomyConfiguration.read(config);
        assertEquals(3,EconomyConfiguration.readStudy(config).skills().get("mining").materials().get(Commodity.STONE));
        config.set("resident-study",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readStudy(config).enabled());
    }
    @Test void invalidStudyFoodCostsRatesAndBudgetsFailBeforeDatabaseStartup()throws Exception {
        for(Object invalid:List.of(0,1001,1.5,"fast")) {
            var config=defaults();config.set("resident-study.gain-per-period",invalid);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("resident-study.skills.mining.practice-supplies.food",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        config.set("resident-study.skills.mining.practice-supplies.food",null);config.set("resident-study.students-per-period",9);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    @Test void workLearningDefaultsAreEditableAndAbsentOldSectionsRemainDisabled()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.WorkLearningPolicy.prototype(),EconomyConfiguration.readWorkLearning(config));
        config.set("work-learning.experience-per-productive-period",2);EconomyConfiguration.read(config);
        assertEquals(2,EconomyConfiguration.readWorkLearning(config).experiencePerProductivePeriod());
        config.set("work-learning",null);EconomyConfiguration.read(config);assertEquals(com.blueninjas24.civilizations.industry.WorkLearningPolicy.disabled(),EconomyConfiguration.readWorkLearning(config));
    }
    @Test void malformedLearningSettingsFailBeforeDatabaseStartup()throws Exception {
        for(Object value:List.of(0,1001,1.5,"fast")) {
            var config=defaults();config.set("work-learning.experience-per-productive-period",value);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("work-learning.industry-skills.mining",42);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var missing=defaults();missing.set("work-learning.enabled",null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(missing));
    }
    @Test void staffedOperationRuleIsEditableAndOldFilesKeepLegacyBehavior()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.BusinessOperationPolicy.prototype(),EconomyConfiguration.readBusinessOperations(config));
        config.set("business-operations.continue-staffed-after-owner-death",false);EconomyConfiguration.read(config);
        assertFalse(EconomyConfiguration.readBusinessOperations(config).continueStaffedAfterOwnerDeath());
        config.set("business-operations",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readBusinessOperations(config).enabled());
    }
    @Test void invalidOperatingRuleFailsBeforeDatabaseStartup()throws Exception {
        for(Object value:List.of("yes",1)) {
            var config=defaults();config.set("business-operations.continue-staffed-after-owner-death",value);
            assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("business-operations.continue-staffed-after-owner-death",null);
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    @Test void resaleFundingDefaultsAreEditableAndMissingOldSectionDisablesPurchases()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.BusinessPurchasePolicy.prototype(),EconomyConfiguration.readBusinessPurchasePolicy(config));
        config.set("business-resale-funding.maximum-spare-business-percent",50);EconomyConfiguration.read(config);
        assertEquals(2500,EconomyConfiguration.readBusinessPurchasePolicy(config).businessAvailable(6000));
        config.set("business-resale-funding",null);EconomyConfiguration.read(config);
        assertEquals(com.blueninjas24.civilizations.industry.BusinessPurchasePolicy.disabled(),EconomyConfiguration.readBusinessPurchasePolicy(config));
    }
    @Test void malformedResaleLimitsFailBeforeDatabaseStartup()throws Exception {
        for(Object value:List.of(-1,101,1.5,"all")) {
            var config=defaults();config.set("business-resale-funding.maximum-estate-percent",value);
            assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var missing=defaults();missing.set("business-resale-funding.minimum-retained-business-cash-hundredths",null);
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(missing));
        var extra=defaults();extra.set("business-resale-funding.free-money",true);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(extra));
    }
    @Test void nearbyOreRadiusIsEditableAndOldSectionsKeepTheirPreviousScope() throws Exception {
        var config=defaults();assertEquals(1,EconomyConfiguration.readOreObserver(config).horizontalRadiusChunks());
        config.set("ore-observer.horizontal-radius-chunks",2);assertEquals(2,EconomyConfiguration.readOreObserver(config).horizontalRadiusChunks());
        config.set("ore-observer.horizontal-radius-chunks",null);EconomyConfiguration.read(config);
        assertEquals(0,EconomyConfiguration.readOreObserver(config).horizontalRadiusChunks());
    }
    @Test void invalidNearbyOreRadiusFailsBeforeOpeningDatabase() throws Exception {
        for(Object value:List.of(-1,3,1.5,"one")) {
            var config=defaults();config.set("ore-observer.horizontal-radius-chunks",value);
            assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
    }
    @Test void readOnlyOreObservationDefaultsAreEditableAndMissingOldSectionStaysDisabled() throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.OreObserverPolicy.prototype(),EconomyConfiguration.readOreObserver(config));
        config.set("ore-observer.cells-per-tick",64);assertEquals(64,EconomyConfiguration.readOreObserver(config).cellsPerTick());
        config.set("ore-observer",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readOreObserver(config).enabled());
    }
    @Test void malformedOreObserverSettingsFailBeforeDatabaseStartup() throws Exception {
        for(var key:List.of("vertical-radius","cells-per-tick","interval-ticks")) {
            var config=defaults();config.set("ore-observer."+key,0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
            config.set("ore-observer."+key,null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("ore-observer.enabled","yes");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    @Test void automaticFarmDefaultsAreEditableAndOldFilesRemainDisabled() throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.FarmBusinessPolicy.prototype(),EconomyConfiguration.readFarmBusinesses(config));
        config.set("farm-businesses.worker-capacity",8);assertEquals(8,EconomyConfiguration.readFarmBusinesses(config).workerCapacity());
        config.set("farm-businesses",null);EconomyConfiguration.read(config);assertFalse(EconomyConfiguration.readFarmBusinesses(config).enabled());
    }
    @Test void partialOrInvalidAutomaticFarmSettingsFailBeforeDatabaseStartup() throws Exception {
        var config=defaults();config.set("farm-businesses.startup-trade-value-hundredths",-1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var bad=defaults();bad.set("farm-businesses.enabled","yes");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(bad));
        var partial=defaults();partial.set("farm-businesses.minimum-age-months",null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(partial));
    }
    @Test void barterSettingsAreEditableDisabledInitiallyAndOldFilesStayUntouched() throws Exception {
        var config=defaults();assertEquals(FoodBarters.Policy.prototype(),EconomyConfiguration.readBarter(config));
        config.set("barter.enabled",true);config.set("barter.goods.timber.units-per-item",2);config.set("barter.goods.timber.keep-units",10);
        config.set("barter.goods.stone",null);config.set("barter.maximum-items",12);
        assertEquals(new FoodBarters.Policy(true,3,12,Map.of(Commodity.TIMBER,new FoodBarters.Goods("OAK_LOG",2,10))),EconomyConfiguration.readBarter(config));
        config.set("barter",null);EconomyConfiguration.read(config);assertEquals(FoodBarters.Policy.disabled(),EconomyConfiguration.readBarter(config));
    }
    @Test void invalidPartialAndUnsubstantiatedBarterGoodsFailBeforeStartup() throws Exception {
        for(var key:List.of("food-reserve-periods","maximum-items")) {
            var config=defaults();config.set("barter."+key,0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        for(var key:List.of("item","units-per-item","keep-units")) {
            var config=defaults();config.set("barter.goods.timber."+key,null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("barter.goods.timber.item","DIAMOND");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        var extra=defaults();extra.set("barter.extra",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(extra));
    }
    @Test void bargainingIsEditableAndOldOwnerFilesDoNotOptInAutomatically() throws Exception {
        var config=defaults();assertEquals(FoodBargaining.Policy.prototype(),EconomyConfiguration.readBargaining(config));
        config.set("bargaining.maximum-failed-offers",5);config.set("bargaining.cooldown-seconds",120);config.set("bargaining.offer-seconds",30);
        assertEquals(new FoodBargaining.Policy(true,5,120,30),EconomyConfiguration.readBargaining(config));
        config.set("bargaining",null);EconomyConfiguration.read(config);assertEquals(FoodBargaining.Policy.disabled(),EconomyConfiguration.readBargaining(config));
    }
    @Test void unsafeAndPartialBargainingSettingsFailBeforeDatabaseStartup() throws Exception {
        for(var key:List.of("maximum-failed-offers","cooldown-seconds","offer-seconds")) {
            var config=defaults();config.set("bargaining."+key,0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
            config.set("bargaining."+key,null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("bargaining.extra",1);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    @Test void wellbeingIsEditableAndOldOwnerFilesStayDisabled() throws Exception {
        var config=defaults();assertEquals(HouseholdWellbeing.Policy.prototype(),EconomyConfiguration.readWellbeing(config));
        config.set("wellbeing.starting-score",60);config.set("wellbeing.recovery-per-period",2);
        assertEquals(new HouseholdWellbeing.Policy(true,60,2,3,5,1),EconomyConfiguration.readWellbeing(config));
        config.set("wellbeing",null);EconomyConfiguration.read(config);assertEquals(HouseholdWellbeing.Policy.disabled(),EconomyConfiguration.readWellbeing(config));
    }
    @Test void incompleteUnsafeAndUnknownWellbeingSettingsFailBeforeDatabaseStartup() throws Exception {
        for (var path : List.of("wellbeing.enabled","wellbeing.starting-score","wellbeing.food-crisis-penalty")) {
            var config=defaults();config.set(path,null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var high=defaults();high.set("wellbeing.starting-score",101);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(high));
        var lowCrisis=defaults();lowCrisis.set("wellbeing.food-crisis-penalty",2);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(lowCrisis));
        var unknown=defaults();unknown.set("wellbeing.salary",100);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(unknown));
    }
    @Test void adminsControlPurchasesAndOlderFilesStayDisabled() throws Exception {
        var config=defaults();assertEquals(FoodPurchases.Policy.prototype(),EconomyConfiguration.readPurchases(config));
        config.set("purchases.trade-value-per-emerald",2.25);config.set("purchases.food-reserve-periods",5);config.set("purchases.maximum-emeralds",16);
        assertEquals(new FoodPurchases.Policy(true,225,5,16),EconomyConfiguration.readPurchases(config));
        config.set("purchases",null);assertEquals(FoodPurchases.Policy.disabled(),EconomyConfiguration.readPurchases(config));EconomyConfiguration.read(config);
    }
    @Test void partialMalformedAndUnsafePurchaseEditsFailBeforeStartup() throws Exception {
        for(var entry:Map.of("purchases.enabled","yes","purchases.trade-value-per-emerald",0,"purchases.food-reserve-periods",101,
                "purchases.maximum-emeralds",65,"purchases.unknown",true).entrySet()) {
            var config=defaults();config.set(entry.getKey(),entry.getValue());assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        for(Object rate:new Object[]{"8",8.001,Double.NaN,Double.POSITIVE_INFINITY,-1,1000001}) {
            var config=defaults();config.set("purchases.trade-value-per-emerald",rate);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("purchases.maximum-emeralds",null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    @Test void adminsCanTuneOpeningFundsWithoutOverwritingOlderFiles() throws Exception {
        var config = defaults();
        assertEquals(OpeningFunds.prototype(), EconomyConfiguration.readOpeningFunds(config));
        config.set("finance.opening-trade-value-per-resident", 12.25);
        assertEquals(3675, EconomyConfiguration.readOpeningFunds(config).amount(3));
        config.set("finance.opening-funds-enabled", false);
        assertFalse(EconomyConfiguration.readOpeningFunds(config).enabled());
        config.set("finance", null);
        assertEquals(OpeningFunds.disabled(), EconomyConfiguration.readOpeningFunds(config));
        EconomyConfiguration.read(config);
    }
    @Test void invalidFinanceEditsFailBeforeStartupRatherThanGuessingMoney() throws Exception {
        for (Object value : new Object[]{-1, 1000001, 1.001, "24", Double.NaN, Double.POSITIVE_INFINITY}) {
            var config = defaults(); config.set("finance.opening-trade-value-per-resident", value);
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        }
        for (String key : List.of("finance.opening-funds-enabled", "finance.opening-trade-value-per-resident")) {
            var config = defaults(); config.set(key, null);
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        }
        var config = defaults(); config.set("finance.opening-funds-enabled", "yes"); var invalidBoolean = config;
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(invalidBoolean));
        config = defaults(); config.set("finance.extra", 1); var typo = config;
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(typo));
    }
    @Test void existingFilesDoNotSilentlyEnableDonationsAndInvalidEditsFail() throws Exception {
        var config=defaults();assertTrue(EconomyConfiguration.readDonationsEnabled(config));config.set("donations",null);assertFalse(EconomyConfiguration.readDonationsEnabled(config));
        config=defaults();config.set("donations.enabled","yes");var invalid=config;assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(invalid));
    }
    @Test void oldFilesKeepAidPreviewDefaultsAndOwnerCanTuneOrDisableThem() throws Exception {
        var config=defaults();config.set("aid-preview",null);
        assertEquals(FoodAidPreview.Policy.prototype(),EconomyConfiguration.readAidPreview(config));EconomyConfiguration.read(config);
        config=defaults();config.set("aid-preview.enabled",false);config.set("aid-preview.units-per-item.BREAD",9);
        assertFalse(EconomyConfiguration.readAidPreview(config).enabled());assertEquals(18,EconomyConfiguration.readAidPreview(config).units("BREAD",2));
    }
    @ParameterizedTest @ValueSource(strings={"aid-preview.enabled", "aid-preview.radius-blocks", "aid-preview.units-per-item"})
    void partialAidSectionsFailRatherThanGuessing(String key) throws Exception {
        var config=defaults();config.set(key,null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    @Test void unsafeAidItemsAmountsOrTypographicalOptionsFail() throws Exception {
        for (var entry : Map.of("aid-preview.units-per-item.DIAMOND",1,"aid-preview.units-per-item.BREAD",0,
                "aid-preview.radius-blocks",129,"aid-preview.enable",true,"aid-preview.enabled","yes").entrySet()) {
            var config=defaults();config.set(entry.getKey(),entry.getValue());assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
    }
    @Test void olderOwnerFilesKeepPriorityDefaultsWithoutReplacingTheirEdits() throws Exception {
        var config=defaults();config.set("priorities",null);config.set("food.output-percent",25);
        assertEquals(VillagePriorities.Policy.prototype(),EconomyConfiguration.readPriorities(config));assertEquals(25,EconomyConfiguration.read(config).foodOutputPercent());
    }
    @Test void learningIsEditableAndAbsentOldSettingsRemainDisabled() throws Exception {
        var config=defaults();assertEquals(12,EconomyConfiguration.readPriorities(config).learningPeriods());
        assertEquals(25,EconomyConfiguration.readPriorities(config).maximumFoodBonusPercent());
        config.set("priorities.learning",null);assertEquals(VillagePriorities.Policy.prototype(),EconomyConfiguration.readPriorities(config));
        config.set("priorities.learning.window-periods",0);config.set("priorities.learning.maximum-food-bonus-percent",0);
        assertEquals(0,EconomyConfiguration.readPriorities(config).learningPeriods());
    }
    @Test void responseWeightsAreEditableAndOldFilesStayDisabled() throws Exception {
        var config=defaults();assertTrue(EconomyConfiguration.readResponses(config).policy().enabled());
        config.set("responses.emerald-weight",12);assertEquals(12,EconomyConfiguration.readResponses(config).policy().emeraldWeight());
        config.set("responses",null);assertFalse(EconomyConfiguration.readResponses(config).policy().enabled());
    }
    @Test void unsafePartialAndUnknownResponseSettingsFail() throws Exception {
        for(var entry:java.util.Map.of("responses.enabled","yes","responses.barter-weight",0,"responses.minimum-relative-percent",101,"responses.typo",1).entrySet()) {
            var config=defaults();config.set(entry.getKey(),entry.getValue());assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("responses.donation-weight",null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    @Test void partialUnknownAndUnsafeLearningSettingsFail() throws Exception {
        var config=defaults();config.set("priorities.learning.window-periods",null);
        assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        for(var entry:java.util.Map.of("priorities.learning.maximum-food-bonus-percent",101,"priorities.learning.window-periods",0,"priorities.learning.typo",1).entrySet()) {
            var invalid=defaults();invalid.set(entry.getKey(),entry.getValue());assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(invalid));
        }
    }
    @Test void partialOrUnsafePrioritySettingsFailClearly() throws Exception {
        var config=defaults();config.set("priorities.food-weight",null);var missing=config;assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(missing));
        config=defaults();config.set("priorities.minimum-relative-percent",101);var invalid=config;assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(invalid));
    }
    @Test void existingOwnerFilesWithoutFarmSectionKeepApprovedDefaults() throws Exception {
        var config = defaults(); config.set("farms", null); config.set("food.output-percent", 25);
        assertEquals(FarmObservation.Policy.prototype(), EconomyConfiguration.readFarms(config));
        assertEquals(25, EconomyConfiguration.read(config).foodOutputPercent());
    }
    @Test void partialFarmSettingsFailInsteadOfGuessingCapacity() throws Exception {
        var config = defaults(); config.set("farms.crops-per-worker", null);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
    }
    private YamlConfiguration defaults() throws Exception {
        var config = new YamlConfiguration();
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/economy.yml"));
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) { config.load(reader); }
        return config;
    }
    @Test void shippedCommentedFileDefinesApprovedPrototypeAndPlainPercentages() throws Exception {
        var settings = EconomyConfiguration.read(defaults());
        assertEquals(60, settings.secondsPerPeriod()); assertEquals(120, settings.maximumOfflinePeriods()); assertEquals(2, settings.periodsPerBatch());
        assertEquals(new Policy(5000, 12500, 20000, 3), settings.shortages());
        assertEquals(5, settings.foodDemand(2, 1)); assertEquals(24, settings.startingReserves(2).get(Commodity.FOOD));
        assertEquals(new BigDecimal("5.00"), settings.price(Commodity.FOOD, Condition.SEVERE_SHORTAGE));
    }
    @ParameterizedTest @ValueSource(strings = {"pace.seconds-per-period", "food.units-per-adult", "shortages.severe-after-periods", "prices.condition-percent.balanced"})
    void fractionalAndQuotedWholeNumbersFailRatherThanTruncateOrFallBack(String path) throws Exception {
        var config = defaults(); config.set(path, 1.5);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        config.set(path, "2"); assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
    }
    @Test void missingKeysTyposUnknownVersionsAndPartialSectionsFailClearly() throws Exception {
        var config = defaults(); config.set("food.output-percent", null); var missing = config;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(missing)).getMessage().contains("food"));
        config = defaults(); config.set("food.output-percnt", 50); var typo = config;
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(typo));
        config = defaults(); config.set("format-version", 2); var version = config;
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(version));
    }
    @ParameterizedTest @ValueSource(strings = {"pace.seconds-per-period", "pace.maximum-offline-periods", "food.units-per-adult", "food.output-percent", "starting-units-per-resident.food", "prices.condition-percent.balanced"})
    void negativeUnsafeSettingsFail(String path) throws Exception {
        var config = defaults(); config.set(path, -1);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
    }
    @Test void decimalPricesAreExactAndUnsupportedPrecisionOrNonfiniteValuesFail() throws Exception {
        var config = defaults(); config.set("prices.base-trade-value.food", 2.25);
        assertEquals(new BigDecimal("3.38"), EconomyConfiguration.read(config).price(Commodity.FOOD, Condition.SHORTAGE));
        config.set("prices.base-trade-value.food", 2.001); assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        config.set("prices.base-trade-value.food", Double.NaN); assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        config.set("prices.base-trade-value.food", "2.25"); assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
    }
    @Test void extendingCommodityKeysRequiresMatchingInventoryAndPrice() throws Exception {
        var config = defaults(); config.set("starting-units-per-resident.wool", 2);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        config.set("prices.base-trade-value.wool", 3);
        assertEquals(6, EconomyConfiguration.read(config).startingReserves(3).get(new Commodity("wool")));
    }
    @Test void normalFamineAndFeastEditsProduceDistinctExplainableScenariosWithoutChangingCalendar() throws Exception {
        var normal = EconomyConfiguration.read(defaults());
        var famineFile = defaults(); famineFile.set("food.output-percent", 25); var famine = EconomyConfiguration.read(famineFile);
        var feastFile = defaults(); feastFile.set("food.output-percent", 200); var feast = EconomyConfiguration.read(feastFile);
        assertEquals(normal.secondsPerPeriod(), famine.secondsPerPeriod()); assertEquals(normal.secondsPerPeriod(), feast.secondsPerPeriod());
        var normalResult = scenario(normal); var famineResult = scenario(famine); var feastResult = scenario(feast);
        assertEquals(48, normalResult.state().stocks().get(Commodity.FOOD).units());
        assertEquals(0, famineResult.state().stocks().get(Commodity.FOOD).units());
        assertEquals(208, feastResult.state().stocks().get(Commodity.FOOD).units());
        assertEquals(Condition.SEVERE_SHORTAGE, famineResult.reports().stream().filter(r -> r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().condition());
        assertEquals(Condition.ABUNDANT, feastResult.reports().stream().filter(r -> r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().condition());
    }
    @Test void workshopRecipesHaveEditableDefaultsAndOldFilesDoNotRequireASection() throws Exception {
        var config=defaults();
        assertEquals(com.blueninjas24.civilizations.industry.ProductionRecipe.smithTools(),EconomyConfiguration.readRecipes(config).get("smith_tools"));
        config.set("recipes.smith_tools.inputs.iron",3);config.set("recipes.smith_tools.batches-per-worker",4);
        assertEquals(3,EconomyConfiguration.readRecipes(config).get("smith_tools").inputs().get(Commodity.IRON));
        assertEquals(4,EconomyConfiguration.readRecipes(config).get("smith_tools").batchesPerWorker());
        config.set("recipes",null);assertEquals(1,EconomyConfiguration.readRecipes(config).size());EconomyConfiguration.read(config);
    }
    @Test void incompleteMisspelledAndUnsafeRecipesFailBeforeDatabaseStartup() throws Exception {
        for(var path:java.util.List.of("recipes.smith_tools.inputs.iron","recipes.smith_tools.outputs.tools","recipes.smith_tools.site-batch-capacity")) {
            var config=defaults();config.set(path,0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var missing=defaults();missing.set("recipes.smith_tools.industry",null);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(missing));
        var typo=defaults();typo.set("recipes.smith_tools.batch-per-worker",2);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(typo));
        var quoted=defaults();quoted.set("recipes.smith_tools.inputs.iron","2");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(quoted));
    }
    @Test void RecipeCatalogCanBeEmptyAndCannotBeModifiedAfterReading() throws Exception {
        var config=defaults();var recipes=EconomyConfiguration.readRecipes(config);
        assertThrows(UnsupportedOperationException.class,()->recipes.clear());
        config.set("recipes",null);config.createSection("recipes");assertTrue(EconomyConfiguration.readRecipes(config).isEmpty());
    }
    @Test void markerPauseIsEditableStrictAndDoesNotRequireLegacyConfigChanges()throws Exception {
        var config=defaults();assertTrue(EconomyConfiguration.readWorkplaceOperations(config).pauseOnMissing());
        config.set("workplace-operations.pause-on-missing",false);EconomyConfiguration.read(config);
        assertFalse(EconomyConfiguration.readWorkplaceOperations(config).pauseOnMissing());
        config.set("workplace-operations.pause-on-missing","true");assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        config.set("workplace-operations",null);assertTrue(EconomyConfiguration.readWorkplaceOperations(config).pauseOnMissing());EconomyConfiguration.read(config);
    }
    @Test void workshopFounderQualificationsAreEditableAndLegacyFilesOptOut()throws Exception {
        var config=defaults();assertEquals(com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy.prototype(),EconomyConfiguration.readWorkshopFounding(config));
        config.set("workshop-founding.founder-smithing-experience",20);assertEquals(20,EconomyConfiguration.readWorkshopFounding(config).founderExperience());EconomyConfiguration.read(config);
        config.set("workshop-founding",null);assertFalse(EconomyConfiguration.readWorkshopFounding(config).enabled());EconomyConfiguration.read(config);
    }
    @Test void invalidWorkshopFounderQualificationsFailBeforeStartup()throws Exception {
        for(var key:List.of("founders-per-review","minimum-age-months","founder-smithing-experience","worker-smithing-experience","worker-capacity")) {
            var config=defaults();config.set("workshop-founding."+key,0);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
        }
        var config=defaults();config.set("workshop-founding.worker-smithing-experience",11);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.read(config));
    }
    private Result scenario(EconomySettings settings) {
        var stocks = new TreeMap<Commodity, Stock>(); settings.startingReserves(4).forEach((commodity, units) -> stocks.put(commodity, new Stock(units, 0)));
        var state = new State(0, stocks); Result result = null;
        for (int period = 0; period < 20; period++) {
            var flows = new TreeMap<Commodity, Flow>(); state.stocks().keySet().forEach(commodity -> flows.put(commodity, Flow.idle()));
            flows.put(Commodity.FOOD, new Flow(settings.foodProduction(2, OptionalLong.of(2)), 0, settings.foodDemand(4, 0), 0, 0, 0, 0));
            result = CommodityAccounting.step(state, flows, settings.shortages()); state = result.state();
        }
        return result;
    }
}
