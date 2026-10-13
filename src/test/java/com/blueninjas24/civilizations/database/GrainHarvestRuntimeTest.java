package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.FarmBusinessPolicy;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.*;
import java.sql.*;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GrainHarvestRuntimeTest {
    @TempDir Path directory;DatabaseManager db;EconomyRuntimeRepository runtime;EconomySettings settings;
    final UUID town=new UUID(0,1),world=new UUID(0,2);
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Grain","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(int n=3;n<=4;n++)db.getPopulationRepository().insertResident(new Resident(new UUID(0,n),"Adult",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(new UUID(0,5),town,10000,"Fixture startup funds");
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var c:CommodityAccounting.Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        runtime=create(GrainHarvestPolicy.prototype(),true);
    }
    void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("GrainHarvestRuntimeTest"));db.initialize();}
    @AfterEach void close(){db.close();}
    EconomyRuntimeRepository create(GrainHarvestPolicy policy,boolean staffed)throws Exception {
        var value=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        if(staffed)value.configureFarmBusinesses(new FarmBusinessPolicy(true,4,2500,216,0));
        value.configureGrainHarvest(policy);return value;
    }
    void observe(long now,boolean complete,long crops,Long wheat)throws Exception {runtime.observe(new FarmObservation(town,world,now,complete,crops,crops==0?null:new VillageObservation.Position(3,64,5),wheat),now);}
    long scalar(String query)throws Exception {try(var s=db.getConnection().createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getLong(1);}}
    void sql(String query)throws Exception {try(var s=db.getConnection().createStatement()){s.execute(query);}}
    long grain()throws Exception {return new GrainHarvestRepository(db.getConnection()).stock(town);}
    long food()throws Exception {return runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units();}
    @Test void realStaffedFutureWorkSplitsOnceAndSurvivesAbsentWorldAndRestart()throws Exception {
        observe(1000,true,32,32L);runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);
        assertEquals(2,grain());assertEquals(26,food());assertEquals(7500,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
        assertEquals(8,scalar("SELECT harvest FROM grain_harvests"));assertEquals(6,scalar("SELECT food_output FROM grain_harvests"));
        assertEquals(Set.of(Commodity.FOOD),runtime.views().get(town).account().state().stocks().keySet());
        var history=db.getCommodityRepository().history(town,1).orElseThrow();runtime.pulse(2000,false);assertEquals(2,grain());
        runtime.updateLoadedWorlds(2000,Set.of());runtime.pulse(50000,false);assertEquals(2,grain());assertEquals(26,food());
        db.close();open();runtime=create(GrainHarvestPolicy.prototype(),true);runtime.startOnline(100000,Set.of(world));
        assertEquals(2,grain());assertEquals(history,db.getCommodityRepository().history(town,1).orElseThrow());runtime.pulse(101000,false);assertEquals(4,grain());assertEquals(28,food());
    }
    @Test void actualMealReserveStopsGrainDiversionWhileFoodRecovers()throws Exception {
        observe(1000,true,32,32L);sql("UPDATE economy_stocks SET units=0");runtime.startOnline(1000,Set.of(world));
        runtime.pulse(2000,false);assertEquals(0,grain());assertEquals(4,food());runtime.pulse(3000,false);assertEquals(0,grain());assertEquals(8,food());
        runtime.pulse(4000,false);assertEquals(2,grain());assertEquals(10,food());
        assertEquals(0,scalar("SELECT SUM(MAX(0,households-(opening+production+imports))) FROM economy_periods WHERE commodity='food'"));
    }
    @Test void unknownCompositionAndUnstaffedLegacyWorkCannotMintIngredients()throws Exception {
        observe(1000,true,32,null);runtime.pulse(2000,false);assertEquals(0,grain());assertEquals(8,scalar("SELECT food_output FROM grain_harvests"));
        observe(2001,false,32,32L);runtime.pulse(3001,false);assertEquals(0,grain());assertEquals(1,scalar("SELECT COUNT(*) FROM grain_harvests"));
        // Separate legacy runtime uses the same saved fields, but no private or assigned farm work.
        runtime=create(GrainHarvestPolicy.prototype(),false);observe(3002,true,32,32L);runtime.startOnline(3002,Set.of(world));runtime.pulse(4002,false);assertEquals(0,grain());
    }
    @Test void finiteSpaceAndRetunedCapacityPreserveExistingGrainAndHistoricalYield()throws Exception {
        runtime=create(new GrainHarvestPolicy(true,25,2,7,3),true);observe(1000,true,32,32L);runtime.pulse(2000,false);assertEquals(6,grain());
        runtime.pulse(3000,false);assertEquals(6,grain());assertEquals(8,scalar("SELECT food_output FROM grain_harvests WHERE period=2"));
        runtime=create(new GrainHarvestPolicy(true,100,0,1,1),true);runtime.startOnline(3000,Set.of(world));runtime.pulse(4000,false);
        assertEquals(6,grain());assertEquals(3,scalar("SELECT units_per_harvest FROM grain_harvests WHERE period=1"));assertEquals(1,scalar("SELECT COUNT(*) FROM economy_tuning"));
    }
    @Test void lateGrainHistoryFailureRollsBackFoodHarvestStockAndCheckpointTogether()throws Exception {
        observe(1000,true,32,32L);runtime.pulse(2000,false);var before=runtime.views().get(town).account();
        sql("CREATE TRIGGER reject_grain BEFORE INSERT ON grain_harvests BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(3000,false));assertEquals(2,grain());assertEquals(before,runtime.views().get(town).account());assertEquals(1,scalar("SELECT COUNT(*) FROM grain_harvests"));
        assertTrue(db.getConnection().getAutoCommit());sql("DROP TRIGGER reject_grain");runtime.pulse(3000,false);assertEquals(4,grain());
    }
    @Test void settingsCannotReinterpretEarnedBacklogAndDisabledHarvestRetainsStock()throws Exception {
        observe(1000,true,32,32L);runtime.pulse(5000,false);var old=runtime;var disabled=create(GrainHarvestPolicy.disabled(),true);
        assertThrows(SQLException.class,()->disabled.startOnline(5000,Set.of(world)));
        for(int n=0;n<3;n++)old.pulse(5000,false);long held=grain();runtime=disabled;runtime.startOnline(5000,Set.of(world));runtime.pulse(6000,false);
        assertEquals(held,grain());assertEquals(4,scalar("SELECT COUNT(*) FROM grain_harvests"));assertEquals(8,scalar("SELECT production FROM economy_periods WHERE commodity='food' AND period=5"));
    }
    @Test void migration72FailureKeepsVersionAndLegacyFoodThenRetriesWithEmptyGrainStocks()throws Exception {
        runtime=create(GrainHarvestPolicy.disabled(),false);observe(1000,true,32,32L);runtime.pulse(2000,false);var before=runtime.views().get(town).account();
        SchemaFixtures.rewind(db.getConnection(),71);sql("PRAGMA user_version=71");sql("CREATE VIEW grain_harvests AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertEquals(71,scalar("PRAGMA user_version"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='grain_stocks'"));
        sql("DROP VIEW grain_harvests");new SchemaMigrator().migrate(db.getConnection());assertEquals(before,runtime.views().get(town).account());assertEquals(0,grain());assertEquals(0,scalar("SELECT COUNT(*) FROM grain_harvests"));
    }
    @Test void shrinkingWheatCapacityUsesOnlyRemainingWorkAndZeroWheatKeepsBasicFood()throws Exception {
        observe(1000,true,32,32L);runtime.pulse(2000,false);observe(2001,true,16,16L);runtime.pulse(3001,false);
        assertEquals(3,grain());assertEquals(4,scalar("SELECT harvest FROM grain_harvests WHERE period=2"));
        observe(3002,true,16,0L);runtime.pulse(4002,false);assertEquals(3,grain());assertEquals(4,scalar("SELECT food_output FROM grain_harvests WHERE period=3"));
    }
    @Test void grainOnlyProductiveWorkStillUsesToolGraceAndEarnsRealFarmingExperienceOnce()throws Exception {
        runtime=create(new GrainHarvestPolicy(true,100,0,256,1),true);
        runtime.configureFarmTools(com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy.prototype(),com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy.disabled(),com.blueninjas24.civilizations.industry.IndustryPolicy.prototype());
        runtime.configureWorkLearning(com.blueninjas24.civilizations.industry.WorkLearningPolicy.prototype());
        observe(1000,true,32,32L);runtime.pulse(2000,false);assertEquals(8,grain());assertEquals(20,food());
        assertEquals(0,scalar("SELECT production FROM economy_periods WHERE commodity='food'"));
        assertEquals(8,scalar("SELECT output FROM farm_tool_work"));assertEquals(1,scalar("SELECT productive_periods FROM business_tools"));
        assertEquals(2,scalar("SELECT COUNT(*) FROM farm_learning_entries"));assertEquals(8,scalar("SELECT production FROM farm_learning_reviews"));
        assertTrue(runtime.views().get(town).farmBusiness().contains("Grain: saved 8"));runtime.pulse(2000,false);assertEquals(2,scalar("SELECT COUNT(*) FROM farm_learning_entries"));
    }
}
