package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class EconomyRuntimeRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager db;
    private final UUID town = UUID.randomUUID(), world = UUID.randomUUID();
    private EconomySettings settings;
    private EconomyRuntimeRepository runtime;
    private EconomySettings settings(int output, Policy policy) {
        var bands = new EnumMap<Condition,Integer>(Condition.class); for (var band : Condition.values()) bands.put(band,100);
        return new EconomySettings(1,3,2,2,1,4,output,policy,Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
    }
    @BeforeEach void open() throws SQLException {
        db = new DatabaseManager(directory.toFile(), Logger.getLogger("FoodRuntimeTest")); db.initialize();
        db.saveSettlement(new Settlement(town,"Food test","world",0,64,0,1,999,2));
        var identities = db.getVillageIdentityRepository(); identities.observeWorld(world,"world"); identities.bindSettlement(town,world);
        for (long age : new long[]{300,300,10}) db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Person",town,null,age,Resident.Status.ALIVE));
        db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Deceased",town,null,300,Resident.Status.DECEASED));
        settings = settings(100,new Policy(5000,12500,20000,3)); runtime = runtime(settings);
    }
    @AfterEach void close() { db.close(); }
    private EconomyRuntimeRepository runtime(EconomySettings settings) { return new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216); }
    private void observe(long time, boolean complete, long crops) throws SQLException { runtime.observe(new FarmObservation(town,world,time,complete,crops),time); }
    private void sql(String sql) throws SQLException { try (var s = db.getConnection().createStatement()) { s.execute(sql); } }
    @Test void explicitFoodReliefEndsCrisisWithoutFabricatedProductionOrDuplicateImports() throws SQLException {
        observe(1000,true,0);for(int period=1;period<=10;period++)runtime.pulse(1000L+1000L*period,false);
        assertEquals(Condition.SEVERE_SHORTAGE,runtime.views().get(town).needs().sharedFood());
        var operation=UUID.randomUUID();assertTrue(runtime.recordDeliveredFood(operation,town,20,"Test relief shipment",11000));
        assertFalse(runtime.recordDeliveredFood(operation,town,20,"Test relief shipment",11000));
        assertEquals(1,runtime.views().get(town).foodAid().pendingDeliveries());
        runtime.pulse(12000,false);var view=runtime.views().get(town);var food=view.latest().reports().getFirst();
        assertEquals(0,food.flow().production());assertEquals(20,food.flow().imports());assertEquals(15,food.after().units());assertEquals(0,view.needs().shortagePeriods());
        assertEquals(VillagePriorities.Action.MONITOR,view.priority().selected());assertEquals(0,view.foodAid().pendingDeliveries());
        assertEquals(11,new FoodDeliveryRepository(db.getConnection()).find(operation).orElseThrow().appliedPeriod());
        runtime.pulse(13000,false);assertEquals(0,runtime.views().get(town).latest().reports().getFirst().flow().imports());
    }
    @Test void NewDeliveryCannotRewriteQueuedPastPeriods() throws SQLException {
        observe(1000,true,0);var operation=UUID.randomUUID();runtime.recordDeliveredFood(operation,town,20,"Late relief",11000);
        assertEquals(10,new FoodDeliveryRepository(db.getConnection()).find(operation).orElseThrow().eligibleAfterPeriod());
        for(int period=1;period<=10;period++) {runtime.pulse(11000,false);assertEquals(0,runtime.views().get(town).latest().reports().getFirst().flow().imports());}
        assertEquals(1,runtime.views().get(town).foodAid().pendingDeliveries());
        runtime.pulse(12000,false);assertEquals(20,runtime.views().get(town).latest().reports().getFirst().flow().imports());
    }
    @Test void receiptWriteFailureRollsBackPeriodEffectsAndRetriesSameGoodsExactlyOnce() throws SQLException {
        observe(1000,true,0);var id=UUID.randomUUID();runtime.recordDeliveredFood(id,town,20,"Relief",1000);
        sql("CREATE TRIGGER reject_delivery BEFORE UPDATE ON food_deliveries BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));var before=runtime.views().get(town);
        assertEquals(0,before.account().state().completedPeriods());assertNull(before.needs());assertNull(before.priority());assertEquals(36,before.account().state().stocks().get(Commodity.FOOD).units());
        assertNull(new FoodDeliveryRepository(db.getConnection()).find(id).orElseThrow().appliedPeriod());
        sql("DROP TRIGGER reject_delivery");runtime.pulse(2000,false);assertEquals(51,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
        assertEquals(1,new FoodDeliveryRepository(db.getConnection()).find(id).orElseThrow().appliedPeriod());
    }
    @Test void pendingFoodSurvivesPartialEvidenceAndRestartUntilARealFuturePeriod() throws SQLException {
        observe(1000,true,0);observe(1001,false,0);var id=UUID.randomUUID();runtime.recordDeliveredFood(id,town,20,"Relief",1001);
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest"));db.initialize();runtime=runtime(settings);
        runtime.start(100000);assertEquals(0,runtime.views().get(town).account().state().completedPeriods());assertEquals(1,runtime.views().get(town).foodAid().pendingDeliveries());
        observe(100001,true,0);runtime.pulse(101001,false);assertEquals(51,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
    }
    @Test void invalidUnknownAndConflictingDeliveryInputsFailWithoutCreatingInventory() throws SQLException {
        var id=UUID.randomUUID();assertThrows(SQLException.class,()->runtime.recordDeliveredFood(id,town,10,"No enrollment",1000));
        assertTrue(runtime.views().isEmpty());observe(1000,true,0);
        assertThrows(IllegalArgumentException.class,()->runtime.recordDeliveredFood(id,town,0,"Invalid",1000));
        assertThrows(IllegalArgumentException.class,()->runtime.recordDeliveredFood(id,town,10," ",1000));
        runtime.recordDeliveredFood(id,town,10,"Relief",1000);
        assertThrows(SQLException.class,()->runtime.recordDeliveredFood(id,town,11,"Relief",1000));
        assertThrows(SQLException.class,()->runtime.recordDeliveredFood(id,town,10,"Different source",1000));
        assertEquals(1,runtime.views().get(town).foodAid().pendingDeliveries());assertEquals(36,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
    }
    @Test void foodDeliveryBudgetProcessesAtMost64ReceiptsPerPeriodWithoutDroppingTheRemainder() throws SQLException {
        observe(1000,true,0);for(int i=0;i<65;i++)runtime.recordDeliveredFood(UUID.randomUUID(),town,1,"Test source",1000);
        runtime.pulse(2000,false);assertEquals(64,runtime.views().get(town).latest().reports().getFirst().flow().imports());assertEquals(1,runtime.views().get(town).foodAid().pendingDeliveries());
        runtime.pulse(3000,false);assertEquals(1,runtime.views().get(town).latest().reports().getFirst().flow().imports());assertEquals(0,runtime.views().get(town).foodAid().pendingDeliveries());
    }
    @Test void schema17DeliveryMigrationFailureAndRetryPreserveHistoryAndDoNotInventRelief() throws SQLException {
        observe(1000,true,0);runtime.pulse(2000,false);var original=runtime.views().get(town).latest();var priority=runtime.views().get(town).priority();
        SchemaFixtures.rewind(db.getConnection(),17);sql("CREATE TABLE food_deliveries(blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(17,r.getInt(1));}
        sql("DROP TABLE food_deliveries");new SchemaMigrator().migrate(db.getConnection());
        assertEquals(original,runtime.views().get(town).latest());assertEquals(priority,runtime.views().get(town).priority());assertEquals(0,runtime.views().get(town).foodAid().pendingDeliveries());
    }
    @Test void priorityFailureRollsBackNeedsAndFoodAndRetriesWithoutDuplicateChoice() throws SQLException {
        observe(1000,true,32);sql("CREATE TRIGGER reject_priority BEFORE INSERT ON village_priorities BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));var before=runtime.views().get(town);
        assertEquals(0,before.account().state().completedPeriods());assertNull(before.needs());assertNull(before.priority());assertEquals(1,before.queuedPeriods());
        sql("DROP TRIGGER reject_priority");runtime.pulse(2000,false);var original=runtime.views().get(town).priority();
        assertEquals(VillagePriorities.Action.MONITOR,original.selected());runtime.pulse(2000,false);assertEquals(original,runtime.views().get(town).priority());
    }
    @Test void priorityTuningAppliesForwardWithoutRewritingHistoryOrDiscardingFoodTime() throws SQLException {
        observe(1000,true,0);runtime.pulse(2000,false);var original=runtime.views().get(town).priority();
        var policy=new VillagePriorities.Policy(100,5,2,3,1,25);
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,policy,0);
        assertEquals(original,runtime.views().get(town).priority());runtime.pulse(3000,false);
        assertEquals(2,runtime.views().get(town).priority().period());assertEquals(policy,runtime.views().get(town).priority().policy());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM village_priorities")){assertEquals(2,r.getInt(1));}
    }
    @Test void schema16UpgradeDoesNotFabricatePastDecisionsAndRestartKeepsOriginalChoice() throws SQLException {
        observe(1000,true,32);runtime.pulse(2000,false);var original=runtime.views().get(town).latest();
        SchemaFixtures.rewind(db.getConnection(),16);new SchemaMigrator().migrate(db.getConnection());
        assertEquals(original,runtime.views().get(town).latest());assertNull(runtime.views().get(town).priority());
        runtime.pulse(3000,false);var decision=runtime.views().get(town).priority();
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest"));db.initialize();runtime=runtime(settings);
        assertEquals(decision,runtime.views().get(town).priority());
    }
    @Test void householdSnapshotsRetainHousingAndDemandAcrossChangingEvidenceAndRestart() throws SQLException {
        var id = UUID.randomUUID(); db.getPopulationRepository().insertHousehold(new com.blueninjas24.civilizations.population.Household(id,town,"Family"));
        for (var person : db.getPopulationRepository().findResidents(town)) if (person.status() == Resident.Status.ALIVE)
            db.getPopulationRepository().updateResident(new Resident(person.id(),person.name(),town,id,person.ageSteps(),person.status()));
        db.getDemographicRepository().recordCapacity(id,2); observe(1000,true,0); runtime.pulse(2000,false);
        var first = runtime.views().get(town).needs(); assertEquals(0,first.unassignedResidents());
        assertEquals(5,first.homes().getFirst().foodDemand()); assertEquals(HouseholdNeeds.Housing.CROWDED,first.homes().getFirst().housing());
        assertNull(runtime.views().get(town).treasury());
        db.getDemographicRepository().recordCapacity(id,4); runtime.pulse(3000,false);
        assertEquals(HouseholdNeeds.Housing.SUFFICIENT,runtime.views().get(town).needs().homes().getFirst().housing());
        db.close();db = new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest"));db.initialize();runtime = runtime(settings);
        assertEquals(first,new HouseholdNeedsRepository(db.getConnection()).load(town,1).orElseThrow());
        assertEquals(2,runtime.views().get(town).needs().period());
    }
    @Test void failedNeedsWriteRollsBackFoodAndCheckpointWithRetryableTarget() throws SQLException {
        observe(1000,true,32);
        sql("CREATE TRIGGER reject_needs BEFORE INSERT ON needs_periods BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));
        var before = runtime.views().get(town);assertEquals(0,before.account().state().completedPeriods());assertNull(before.needs());
        assertEquals(36,before.account().state().stocks().get(Commodity.FOOD).units());assertEquals(1,before.queuedPeriods());
        sql("DROP TRIGGER reject_needs");runtime.pulse(2000,false);
        var after = runtime.views().get(town);assertEquals(1,after.needs().period());assertEquals(3,after.needs().unassignedResidents());
        assertEquals(39,after.account().state().stocks().get(Commodity.FOOD).units());
    }
    @Test void schema15UpgradePreservesFoodHistoryWithoutInventingPastHouseholdEvidence() throws SQLException {
        observe(1000,true,32);runtime.pulse(2000,false);var original=runtime.views().get(town).latest();
        SchemaFixtures.rewind(db.getConnection(),15);new SchemaMigrator().migrate(db.getConnection());
        assertEquals(original,runtime.views().get(town).latest());assertNull(runtime.views().get(town).needs());
        runtime.pulse(3000,false);assertEquals(2,runtime.views().get(town).needs().period());
        assertTrue(new TreasuryRepository(db.getConnection()).load(town).isEmpty());
    }
    @Test void partialIsUnknownAndCompleteEvidenceEnrollsOnceUsingRealResidentsNotLegacyCensus() throws SQLException {
        runtime.start(1000); observe(1000,false,0); assertTrue(runtime.views().isEmpty());
        observe(1001,true,32); assertEquals(36,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
        assertEquals(1,runtime.pulse(2001,false)); var report = runtime.views().get(town).latest().reports().getFirst();
        assertEquals(8,report.flow().production()); assertEquals(5,report.demand()); assertEquals(39,report.after().units());
        observe(2002,true,32); assertEquals(39,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
    }
    @Test void fractionalClockRestartBoundedCatchupAndNoDuplicateConsumption() throws SQLException {
        observe(1000,true,32); assertEquals(0,runtime.pulse(1500,false));
        db.close(); db = new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest")); db.initialize(); runtime = runtime(settings);
        runtime.start(1500); assertEquals(0,runtime.views().get(town).account().state().completedPeriods());
        runtime.pulse(2000,false); assertEquals(1,runtime.views().get(town).account().state().completedPeriods());
        runtime.pulse(2000,false); assertEquals(1,runtime.views().get(town).account().state().completedPeriods());
        runtime.start(100000); assertEquals(2,runtime.views().get(town).account().state().completedPeriods());
        assertEquals(2,runtime.views().get(town).queuedPeriods());
        runtime.pulse(100000,false); runtime.pulse(100000,false); assertEquals(4,runtime.views().get(town).account().state().completedPeriods());
    }
    @Test void partialPauseDoesNotBecomeStarvationOrRetroactivelyCatchUpUncertainInterval() throws SQLException {
        observe(1000,true,0); runtime.pulse(2000,false); observe(2001,false,0);
        runtime.pulse(100000,false); assertEquals(1,runtime.views().get(town).account().state().completedPeriods());
        observe(100001,true,32); runtime.pulse(100001,false); assertEquals(1,runtime.views().get(town).account().state().completedPeriods());
        runtime.pulse(101001,false); assertEquals(2,runtime.views().get(town).account().state().completedPeriods());
    }
    @Test void failureAfterAllGoodsWritesKeepsQueuedTargetAndRetriesExactlyOnce() throws SQLException {
        observe(1000,true,32);
        sql("CREATE TRIGGER reject_food BEFORE UPDATE ON economy_accounts BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));
        assertEquals(0,runtime.views().get(town).account().state().completedPeriods()); assertEquals(1,runtime.views().get(town).queuedPeriods());
        assertTrue(db.getCommodityRepository().history(town,1).isEmpty()); assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER reject_food"); runtime.pulse(2000,false); assertEquals(39,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
    }
    @Test void oldEvidenceWrongWorldAndInitializationFailureCannotDamageIdentityOrStocks() throws SQLException {
        observe(1000,true,32); observe(999,false,0); assertTrue(runtime.views().get(town).farm().complete());
        assertThrows(SQLException.class,()->runtime.observe(new FarmObservation(town,UUID.randomUUID(),1001,true,0),1001));
        assertEquals(world,runtime.views().get(town).farm().world());
    }
    @Test void changingFileRecordsForwardTuningWithoutRewritingHistoryOrRefillingReserves() throws SQLException {
        settings = settings(0,new Policy(5000,12500,20000,3)); runtime = runtime(settings); observe(1000,true,32);
        for (long now=2000;now<=14000;now+=1000) runtime.pulse(now,false);
        var original = db.getCommodityRepository().history(town,10).orElseThrow(); assertEquals(Condition.SEVERE_SHORTAGE,original.reports().getFirst().condition());
        var changed = settings(100,new Policy(5000,15000,25000,20)); runtime = runtime(changed); runtime.start(100000);
        assertEquals(13,runtime.views().get(town).account().state().completedPeriods());
        assertEquals(0,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
        assertEquals(original,db.getCommodityRepository().history(town,10).orElseThrow());
        runtime.pulse(101000,false); assertEquals(3,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
        try (var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT after_period,discarded_millis FROM economy_tuning")) {
            assertTrue(r.next()); assertEquals(13,r.getLong(1)); assertEquals(86000,r.getLong(2)); assertFalse(r.next());
        }
    }
    @Test void retuningWithPendingBacklogIsExplicitlyRejected() throws SQLException {
        observe(1000,true,32); runtime.pulse(5000,false);
        var changed = runtime(settings(25,settings.shortages()));
        assertThrows(SQLException.class,()->changed.start(6000)); assertEquals(3,runtime.views().get(town).queuedPeriods());
    }
    @Test void schema14PeriodHistoryUpgradeKeepsExactPastReports() throws SQLException {
        db.getCommodityRepository().initialize(town,Map.of(Commodity.FOOD,12L),settings.shortages());
        var original = db.getCommodityRepository().advance(town,1,Map.of(Commodity.FOOD,new Flow(0,0,5,0,0,0,0)),settings.shortages());
        SchemaFixtures.rewind(db.getConnection(),14); new SchemaMigrator().migrate(db.getConnection());
        assertEquals(original,db.getCommodityRepository().history(town,1).orElseThrow()); assertTrue(runtime.views().isEmpty());
    }
}
