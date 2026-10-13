package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.sql.*;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FarmMarketRuntimeTest {
    @TempDir Path directory;DatabaseManager db;EconomyRuntimeRepository runtime;EconomySettings settings;
    final UUID town=new UUID(0,1),world=new UUID(0,2),farm=FarmEnterprises.businessId(town);
    final VillageSupplyPolicy buying=new VillageSupplyPolicy(true,2000,1000,0,1,1,32,Map.of(Commodity.FOOD,100L),2);
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Farm market","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(int n=3;n<=4;n++)person(new UUID(0,n));new TreasuryRepository(db.getConnection()).initialize(new UUID(0,5),town,100000,"Explicit fixture money");
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var c:CommodityAccounting.Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        runtime=create(FarmMarketPolicy.prototype());observe(1000,32,32L);
    }
    void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("FarmMarketRuntimeTest"));db.initialize();}
    @AfterEach void close(){db.close();}
    void person(UUID id)throws Exception{db.getPopulationRepository().insertResident(new Resident(id,"Adult",town,null,300,Resident.Status.ALIVE));}
    EconomyRuntimeRepository create(FarmMarketPolicy p)throws Exception {
        var r=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        r.configureFarmBusinesses(new FarmBusinessPolicy(true,4,2500,216,0));r.configureGrainHarvest(GrainHarvestPolicy.prototype());r.configureFarmMarket(p);
        r.configureWorkLearning(WorkLearningPolicy.prototype());r.configureVillageSupplies(buying);return r;
    }
    void observe(long now,long crops,Long wheat)throws Exception {runtime.observe(new FarmObservation(town,world,now,true,crops,crops==0?null:new VillageObservation.Position(3,64,5),wheat),now);}
    long scalar(String q)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery(q)){assertTrue(r.next());return r.getLong(1);}}
    void sql(String q)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(q);}}
    long food()throws Exception{return runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units();}
    IndustryRepository.Store store()throws Exception{return db.getIndustryRepository().store(farm).orElseThrow();}
    @Test void firstFutureHarvestHasOneOwnerAndOldPublicFoodGrainAndHistoryRemainUntouched()throws Exception {
        sql("INSERT INTO grain_stocks VALUES('"+town+"',7)");runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);
        assertEquals(20,food());assertEquals(6,store().units().get(Commodity.FOOD));assertEquals(2,store().units().get(FarmMarketStore.GRAIN));assertEquals(0,store().hundredths());
        assertEquals(7,new GrainHarvestRepository(db.getConnection()).stock(town));assertEquals(0,scalar("SELECT COUNT(*) FROM grain_harvests"));
        assertEquals(8,scalar("SELECT actual_work FROM farm_market_harvests"));assertEquals(0,scalar("SELECT public_food FROM farm_market_harvests"));assertEquals(2,scalar("SELECT COUNT(*) FROM farm_learning_entries"));
        assertEquals(97500,scalar("SELECT SUM(hundredths) FROM treasuries"));runtime.pulse(2000,false);assertEquals(1,scalar("SELECT COUNT(*) FROM farm_market_harvests"));
    }
    @Test void villageActuallyBuysFarmerSurplusAndMoneyGoodsAndDelayedCargoConserveFor200Periods()throws Exception {
        runtime.startOnline(1000,Set.of(world));
        for(int n=1;n<=200;n++) {
            runtime.pulse(1000L+n*1000,false);
            long paid=scalar("SELECT COALESCE(SUM(price),0) FROM village_supply_orders");assertEquals(paid,store().hundredths());
            assertEquals(97500,scalar("SELECT SUM(hundredths) FROM treasuries")+store().hundredths());
            long pending=scalar("SELECT COALESCE(SUM(units),0) FROM village_supply_orders WHERE applied_period IS NULL");
            long work=scalar("SELECT SUM(actual_work) FROM farm_market_harvests"),eaten=scalar("SELECT SUM(households) FROM economy_periods WHERE commodity='food'");
            assertEquals(24+work-eaten,food()+store().units().get(Commodity.FOOD)+store().units().get(FarmMarketStore.GRAIN)+pending);
            assertTrue(store().usedSpace()<=256);assertTrue(store().units().get(FarmMarketStore.GRAIN)<=16);assertEquals(0,scalar("SELECT COUNT(*) FROM farm_market_harvests WHERE public_food*100>(public_food+business_food)*50"));
        }
        assertTrue(store().hundredths()>0);assertTrue(food()>=4);assertEquals(0,scalar("SELECT SUM(CASE WHEN opening+production+imports<households THEN 1 ELSE 0 END) FROM economy_periods"));
        assertTrue(runtime.views().get(town).farmBusiness().contains("Farm market:"));
    }
    @Test void populationOutpacingProductionDoesNotConfiscateAllHarvestOrInventIncome()throws Exception {
        runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);for(int n=10;n<16;n++)person(new UUID(0,n));
        for(int n=2;n<=30;n++)runtime.pulse(1000L+n*1000,false);
        assertEquals(0,scalar("SELECT COUNT(*) FROM farm_market_harvests WHERE public_food>4"));assertTrue(store().hundredths()>0);
        assertTrue(runtime.views().get(town).latest().reports().getFirst().unmet()>0,"Real capacity shortage remains after stocks are exhausted");
        assertEquals(97500,scalar("SELECT SUM(hundredths) FROM treasuries")+store().hundredths());
    }
    @Test void insufficientVillageMoneyLeavesOwnedFoodAndRealShortageRatherThanFreePurchases()throws Exception {
        sql("UPDATE treasuries SET hundredths=2500");sql("UPDATE economy_stocks SET units=0");runtime.startOnline(1000,Set.of(world));
        runtime.pulse(2000,false);assertEquals(4,store().units().get(Commodity.FOOD));assertEquals(0,store().hundredths());assertEquals(0,scalar("SELECT COUNT(*) FROM village_supply_orders"));
        for(int n=10;n<14;n++)person(new UUID(0,n));runtime.pulse(3000,false);assertTrue(runtime.views().get(town).latest().reports().getFirst().unmet()>0);assertTrue(store().units().get(Commodity.FOOD)>0);
    }
    @Test void lateHarvestFailureRollsBackStoreAdoptionPrivateGoodsPublicFoodAndSkills()throws Exception {
        sql("CREATE TRIGGER reject_market BEFORE INSERT ON farm_market_harvests BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));assertEquals(24,food());assertEquals(0,scalar("SELECT COUNT(*) FROM farm_market_stores"));assertEquals(0,scalar("SELECT COUNT(*) FROM industry_stores"));assertEquals(0,scalar("SELECT COUNT(*) FROM farm_learning_entries"));
        assertEquals(97500,scalar("SELECT SUM(hundredths) FROM treasuries"),"Actual successful founding precedes the failed food transaction and is not paid twice");
        sql("DROP TRIGGER reject_market");runtime.pulse(2000,false);assertEquals(6,store().units().get(Commodity.FOOD));assertEquals(97500,scalar("SELECT SUM(hundredths) FROM treasuries"));
    }
    @Test void legacyToolStoreAdoptsOncePreservingTermsToolsAndAllPublicGoods()throws Exception {
        runtime=create(FarmMarketPolicy.disabled());runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);db.getIndustryRepository().openStore(farm,1,Map.of(ToolUpkeepPolicy.TOOLS,1L),0);
        sql("UPDATE industry_stock SET units=1 WHERE commodity='tools'");String terms;try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT terms FROM industry_stores")){assertTrue(r.next());terms=r.getString(1);}
        long publicFood=food(),publicGrain=new GrainHarvestRepository(db.getConnection()).stock(town);var oldHistory=db.getCommodityRepository().history(town,1).orElseThrow();
        runtime=create(FarmMarketPolicy.prototype());runtime.startOnline(2000,Set.of(world));runtime.pulse(3000,false);
        assertEquals(1,store().units().get(ToolUpkeepPolicy.TOOLS));assertEquals(257,store().capacity());assertEquals(publicGrain,new GrainHarvestRepository(db.getConnection()).stock(town));assertEquals(publicFood-4,food());
        assertEquals(oldHistory,db.getCommodityRepository().history(town,1).orElseThrow());try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT prior_terms FROM farm_market_stores")){assertTrue(r.next());assertEquals(terms,r.getString(1));}
        assertEquals(1,scalar("SELECT COUNT(*) FROM farm_market_stores"));
    }
    @Test void absenceAndRestartRetainPrivateGoodsAndMoneyWithoutRepeatingHarvest()throws Exception {
        runtime.startOnline(1000,Set.of(world));for(int n=1;n<=6;n++)runtime.pulse(1000L+n*1000,false);var saved=store();long publicFood=food();
        runtime.updateLoadedWorlds(7000,Set.of());runtime.pulse(50000,false);assertEquals(saved,store());assertEquals(publicFood,food());
        db.close();open();runtime=create(FarmMarketPolicy.prototype());runtime.startOnline(100000,Set.of(world));assertEquals(saved,store());assertEquals(6,scalar("SELECT COUNT(*) FROM farm_market_harvests"));
    }
    @Test void migration73FailurePreservesLegacyBalancesAndRetriesWithoutAdoptingStores()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),72);sql("PRAGMA user_version=72");sql("CREATE VIEW farm_market_harvests AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertEquals(72,scalar("PRAGMA user_version"));assertEquals(24,scalar("SELECT units FROM economy_stocks WHERE commodity='food'"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='farm_market_stores'"));sql("DROP VIEW farm_market_harvests");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,scalar("SELECT COUNT(*) FROM farm_market_stores"));
    }
    @Test void protectedPublicToolInvoicePreservesActualFarmSalesIncomeAndPreventsDuplicateKit()throws Exception {
        var smith=new UUID(0,8);var seller=new UUID(0,9);person(smith);observe(1001,32,32L);
        var site=new BusinessSite(new UUID(0,20),town,world,10,64,0,1000,"Explicit fixture smith");var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(seller,town,"Smith",smith,smith,"blacksmith",site.id(),1,0),site,requirements,requirements,0,"Fixture"));db.getBusinessRepository().hire(new UUID(0,21),seller,smith,0);
        db.getIndustryRepository().openStore(seller,10,Map.of(Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L),0);sql("UPDATE industry_stock SET units=1 WHERE commodity='iron'");
        db.getIndustryRepository().process(new UUID(0,22),seller,new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),1,1),1,0);
        runtime.startOnline(1001,Set.of(world));for(int n=1;n<=6;n++)runtime.pulse(1001L+n*1000,false);long earned=store().hundredths();assertTrue(earned>0);
        long total=scalar("SELECT SUM(hundredths) FROM treasuries")+earned;var request=new FarmToolPurchases.Request(new UUID(0,23),farm,seller,6,1,32);
        var receipts=new FarmToolPurchases(db.getConnection(),BusinessOperationPolicy.disabled());receipts.purchase(request,FarmToolBudgetPolicy.prototype(),Set.of(world));
        assertEquals(earned,store().hundredths());assertEquals(total,scalar("SELECT SUM(hundredths) FROM treasuries")+store().hundredths()+db.getIndustryRepository().store(seller).orElseThrow().hundredths());
        receipts.purchase(request,FarmToolBudgetPolicy.disabled(),Set.of());assertEquals(earned,store().hundredths());
        assertThrows(FarmToolPurchases.Declined.class,()->receipts.purchase(new FarmToolPurchases.Request(new UUID(0,24),farm,seller,6,1,32),FarmToolBudgetPolicy.prototype(),Set.of(world)));
        db.getIndustryRepository().deliver(request.operation(),7);assertEquals(1,store().units().get(ToolUpkeepPolicy.TOOLS));
    }
    @Test void actualPaidGrainDeliveryAndBreadProcessingCanServeARealVillageFoodOrder()throws Exception {
        var baker=new UUID(0,30);var bakery=new UUID(0,31);person(baker);db.getPopulationRepository().recordSkillExperience(baker,"baking",10);
        var site=new BusinessSite(new UUID(0,32),town,world,12,64,0,1000,"Explicit qualified bakery fixture, not automatic founding");var requirements=new JobRequirements(216,Map.of("baking",10L));
        db.getBusinessRepository().found(new BusinessFounding(new Business(bakery,town,"Bakery",baker,baker,"bakery",site.id(),1,0),site,requirements,requirements,0,"Fixture"));db.getBusinessRepository().hire(new UUID(0,33),bakery,baker,0);
        db.getIndustryRepository().openStore(bakery,32,Map.of(FarmMarketStore.GRAIN,1L,Commodity.FOOD,1L),1000);
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        runtime.configureFarmBusinesses(new FarmBusinessPolicy(true,4,2500,216,0));runtime.configureGrainHarvest(GrainHarvestPolicy.prototype());runtime.configureFarmMarket(FarmMarketPolicy.prototype());
        runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);runtime.pulse(3000,false);
        var order=new UUID(0,34);db.getIndustryRepository().purchase(order,farm,bakery,FarmMarketStore.GRAIN,2,200,1,1,32);
        assertEquals(0,db.getIndustryRepository().store(bakery).orElseThrow().units().get(FarmMarketStore.GRAIN));db.getIndustryRepository().deliver(order,2);db.getIndustryRepository().deliver(order,2);
        db.getIndustryRepository().process(new UUID(0,35),bakery,new ProductionRecipe("fixture_bread","bakery",Map.of(FarmMarketStore.GRAIN,2L),Map.of(Commodity.FOOD,3L),1,1),1,2);
        assertEquals(0,db.getIndustryRepository().store(bakery).orElseThrow().units().get(FarmMarketStore.GRAIN));assertEquals(3,db.getIndustryRepository().store(bakery).orElseThrow().units().get(Commodity.FOOD));
        runtime.pulse(4000,false);var request=new VillageSupplyPurchases.Request(new UUID(0,36),town,bakery,Commodity.FOOD,3,3);
        var customers=new VillageSupplyPurchases(db.getConnection(),buying,StudyPolicy.disabled(),BusinessOperationPolicy.disabled());customers.purchase(request,Set.of(world));customers.purchase(request,Set.of(world));
        assertEquals(0,db.getIndustryRepository().store(bakery).orElseThrow().units().get(Commodity.FOOD));assertEquals(1100,db.getIndustryRepository().store(bakery).orElseThrow().hundredths());assertEquals(200,store().hundredths());
        assertEquals(97500,scalar("SELECT SUM(hundredths) FROM treasuries")+store().hundredths()+db.getIndustryRepository().store(bakery).orElseThrow().hundredths());
        runtime.pulse(5000,false);assertEquals(3,scalar("SELECT imports FROM economy_periods WHERE commodity='food' AND period=4"));runtime.pulse(5000,false);assertEquals(1,scalar("SELECT COUNT(*) FROM village_supply_orders"));
    }
    @Test void unrecognizedPrivateStoreCannotBeSilentlyRewrittenOrHaveCashTaken()throws Exception {
        runtime=create(FarmMarketPolicy.disabled());runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);
        db.getIndustryRepository().openStore(farm,10,Map.of(Commodity.FOOD,1L),500);var saved=store();long publicFood=food();
        runtime=create(FarmMarketPolicy.prototype());runtime.startOnline(2000,Set.of(world));assertThrows(SQLException.class,()->runtime.pulse(3000,false));
        assertEquals(saved,store());assertEquals(publicFood,food());assertEquals(0,scalar("SELECT COUNT(*) FROM farm_market_stores"));
    }
}
