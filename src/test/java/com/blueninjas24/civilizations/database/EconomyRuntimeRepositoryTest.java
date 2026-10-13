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
    private void grain(long time,boolean complete,long crops,Long grain)throws SQLException {
        runtime.observe(new FarmObservation(town,world,time,complete,crops,null,grain),time);
    }
    @Test void grainEvidencePersistsWithoutChangingFoodStocksOrInventingIngredients()throws SQLException {
        observe(1000,true,32);runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);
        var account=runtime.views().get(town).account();var history=db.getCommodityRepository().history(town,1).orElseThrow();
        assertNull(runtime.views().get(town).farm().grainCrops());
        grain(2001,true,32,12L);assertEquals(12L,runtime.views().get(town).farm().grainCrops());
        assertEquals(account,runtime.views().get(town).account());assertEquals(history,db.getCommodityRepository().history(town,1).orElseThrow());
        assertEquals(Set.of(Commodity.FOOD),account.state().stocks().keySet());
        db.close();openExisting();assertEquals(12L,runtime.views().get(town).farm().grainCrops());
        assertEquals(account,runtime.views().get(town).account());
    }
    private void openExisting()throws SQLException {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("GrainEvidenceTest"));db.initialize();runtime=runtime(settings);
    }
    @Test void grainFramesPreserveUnknownZeroPartialAndStaleDistinctions()throws SQLException {
        grain(1000,true,32,12L);grain(999,true,32,20L);grain(1000,true,32,20L);
        assertEquals(12L,runtime.views().get(town).farm().grainCrops());
        grain(1001,true,32,0L);assertEquals(0L,runtime.views().get(town).farm().grainCrops());
        grain(1002,false,16,8L);assertFalse(runtime.views().get(town).farm().complete());assertEquals(8L,runtime.views().get(town).farm().grainCrops());
        observe(1003,true,32);assertNull(runtime.views().get(town).farm().grainCrops());
    }
    @Test void lateGrainSaveFailureRollsBackTheWholeFarmFrameAndFoodState()throws SQLException {
        grain(1000,true,32,12L);var before=runtime.views().get(town);
        sql("CREATE TRIGGER reject_grain BEFORE INSERT ON farm_grain_evidence BEGIN SELECT RAISE(ABORT,'fixture rejection'); END");
        assertThrows(SQLException.class,()->grain(1001,true,64,40L));assertEquals(before,runtime.views().get(town));assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER reject_grain");grain(1001,true,64,40L);assertEquals(40L,runtime.views().get(town).farm().grainCrops());
    }
    @Test void migration71PreservesLegacyFoodAndCountsWithoutInventingGrain()throws SQLException {
        observe(1000,true,32);runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);
        var before=runtime.views().get(town);var history=db.getCommodityRepository().history(town,1).orElseThrow();
        SchemaFixtures.rewind(db.getConnection(),70);sql("PRAGMA user_version=70");
        new SchemaMigrator().migrate(db.getConnection());assertEquals(before,runtime.views().get(town));
        assertNull(runtime.views().get(town).farm().grainCrops());assertEquals(history,db.getCommodityRepository().history(town,1).orElseThrow());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM farm_grain_evidence")){assertTrue(r.next());assertEquals(0,r.getLong(1));}
    }
    @Test void migration71FailureKeepsVersionAndAllLegacyBalancesThenRetries()throws SQLException {
        observe(1000,true,32);var before=runtime.views().get(town);
        SchemaFixtures.rewind(db.getConnection(),70);sql("PRAGMA user_version=70");sql("CREATE VIEW farm_grain_evidence AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertTrue(r.next());assertEquals(70,r.getInt(1));}
        sql("DROP VIEW farm_grain_evidence");new SchemaMigrator().migrate(db.getConnection());assertEquals(before,runtime.views().get(town));
    }
    @Test void impossibleGrainSubsetsAndCorruptFramePairingAreRejected()throws SQLException {
        assertThrows(IllegalArgumentException.class,()->new FarmObservation(town,world,1,true,2,null,3L));
        assertThrows(IllegalArgumentException.class,()->new FarmObservation(town,world,1,true,2,null,-1L));
        grain(1000,true,32,12L);sql("UPDATE farm_grain_evidence SET observed_millis=999");
        assertThrows(SQLException.class,()->runtime.views());
    }
    @Test void onlinePauseRetainsFractionAndQueuedGoodsUntilWorldReturns() throws SQLException {
        observe(1000,true,0);runtime.startOnline(1000,Set.of(world));
        runtime.pulse(2500,false);
        assertEquals(1,runtime.views().get(town).account().state().completedPeriods());
        runtime.updateLoadedWorlds(2500,Set.of());
        var before=runtime.views().get(town).account();
        runtime.pulse(1_000_000,false);assertEquals(before,runtime.views().get(town).account());
        runtime.updateLoadedWorlds(1_000_000,Set.of(world));runtime.pulse(1_000_499,false);
        assertEquals(before,runtime.views().get(town).account());
        runtime.pulse(1_000_500,false);assertEquals(2,runtime.views().get(town).account().state().completedPeriods());
    }
    @Test void alreadyEarnedEconomyBacklogWaitsForAbsentWorldWithoutNewOfflineAccrual() throws SQLException {
        observe(1000,true,0);runtime.startOnline(1000,Set.of(world));runtime.pulse(5000,false);
        assertEquals(3,runtime.views().get(town).queuedPeriods());
        runtime.updateLoadedWorlds(5000,Set.of());runtime.pulse(1_000_000,false);
        assertEquals(3,runtime.views().get(town).queuedPeriods());
        runtime=runtime(settings);runtime.startOnline(2_000_000,Set.of());
        assertEquals(3,runtime.views().get(town).queuedPeriods());
        runtime.updateLoadedWorlds(2_000_000,Set.of(world));
        for(int i=0;i<3;i++)runtime.pulse(2_000_000,false);
        assertEquals(4,runtime.views().get(town).account().state().completedPeriods());
        assertEquals(0,runtime.views().get(town).queuedPeriods());
    }
    @Test void explicitFoodReliefEndsCrisisWithoutFabricatedProductionOrDuplicateImports() throws SQLException {
        observe(1000,true,0);for(int period=1;period<=10;period++)runtime.pulse(1000L+1000L*period,false);
        assertEquals(Condition.SEVERE_SHORTAGE,runtime.views().get(town).needs().sharedFood());
        var operation=UUID.randomUUID();assertTrue(runtime.recordDeliveredFood(operation,town,20,"Test relief shipment",11000));
        assertFalse(runtime.recordDeliveredFood(operation,town,20,"Test relief shipment",11000));
        assertEquals(1,runtime.views().get(town).foodAid().pendingDeliveries());
        assertEquals(new FoodImportImpact.Outcome(0,5,5,0,0),runtime.views().get(town).foodImportImpact().orElseThrow());
        runtime.pulse(12000,false);var view=runtime.views().get(town);var food=view.latest().reports().getFirst();
        assertEquals(0,food.flow().production());assertEquals(20,food.flow().imports());assertEquals(15,food.after().units());assertEquals(0,view.needs().shortagePeriods());
        assertEquals(VillagePriorities.Action.MONITOR,view.priority().selected());assertEquals(0,view.foodAid().pendingDeliveries());
        assertEquals(11,new FoodDeliveryRepository(db.getConnection()).find(operation).orElseThrow().appliedPeriod());
        assertEquals(new FoodImportImpact.Outcome(20,5,0,5,15),view.foodImportImpact().orElseThrow());
        runtime.pulse(13000,false);assertEquals(0,runtime.views().get(town).latest().reports().getFirst().flow().imports());
        assertEquals(new FoodImportImpact.Outcome(0,0,0,0,0),runtime.views().get(town).foodImportImpact().orElseThrow());
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
    private void assignLivingFamily() throws SQLException {
        var home=UUID.randomUUID();var people=db.getPopulationRepository();
        people.insertHousehold(new com.blueninjas24.civilizations.population.Household(home,town,"Allocation family"));
        for(var person:people.findResidents(town)) if(person.status()==Resident.Status.ALIVE)
            people.updateResident(new Resident(person.id(),person.name(),town,home,person.ageSteps(),person.status()));
    }
    @Test void householdAllocationPersistsAcrossRestartAndRelief() throws SQLException {
        assignLivingFamily();
        observe(1000,true,0);
        for(int period=1;period<=10;period++)runtime.pulse(1000L+1000L*period,false);
        var before=runtime.views().get(town).needs();
        assertEquals(before.fulfilled(),before.homes().stream().mapToLong(h->h.supplied()).sum()+before.unassignedSupplied());
        assertTrue(before.homes().stream().allMatch(h->h.unmetPeriods()>0));
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest"));db.initialize();runtime=runtime(settings);
        assertEquals(before,runtime.views().get(town).needs());
        runtime.recordDeliveredFood(UUID.randomUUID(),town,20,"Allocation relief",11000);runtime.pulse(12000,false);
        assertTrue(runtime.views().get(town).needs().homes().stream().allMatch(h->h.supplied()==h.foodDemand() && h.unmetPeriods()==0));
    }
    @Test void schema18HistoryKeepsUnknownAllocationWhileNewPeriodsAllocate() throws SQLException {
        assignLivingFamily();
        observe(1000,true,0);runtime.pulse(2000,false);
        var old=runtime.views().get(town).needs();SchemaFixtures.rewind(db.getConnection(),18);
        new SchemaMigrator().migrate(db.getConnection());
        var historical=new HouseholdNeedsRepository(db.getConnection()).load(town,1).orElseThrow();
        assertEquals(old.fulfilled(),historical.fulfilled());assertNull(historical.unassignedSupplied());
        assertTrue(historical.homes().stream().allMatch(h->h.supplied()==null && h.unmetPeriods()==null));
        runtime.pulse(3000,false);assertNotNull(runtime.views().get(town).needs().unassignedSupplied());
    }
    @Test void failedAllocationMigrationRollsBackAllNewColumnsAndRetries() throws SQLException {
        SchemaFixtures.rewind(db.getConnection(),18);
        sql("ALTER TABLE household_needs ADD COLUMN unmet_periods INTEGER");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertTrue(r.next());assertEquals(18,r.getInt(1));}
        assertThrows(SQLException.class,()->sql("SELECT unassigned_demand FROM needs_periods"));
        sql("ALTER TABLE household_needs DROP COLUMN unmet_periods");new SchemaMigrator().migrate(db.getConnection());
        sql("SELECT unassigned_demand FROM needs_periods");
    }

    @Test void foodAppealSurvivesRestartAndDeliveryClosesItWithObservedInputs() throws SQLException {
        observe(1000,true,0);for(int period=1;period<=10;period++)runtime.pulse(1000L+1000L*period,false);
        var appeal=runtime.views().get(town).appeal();assertEquals(FoodAppeals.Status.OPEN,appeal.status());assertEquals(5,appeal.unmet());
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest"));db.initialize();runtime=runtime(settings);
        assertEquals(appeal,runtime.views().get(town).appeal());
        runtime.recordDeliveredFood(UUID.randomUUID(),town,20,"Appeal relief",11000);runtime.pulse(12000,false);
        var closed=runtime.views().get(town).appeal();assertEquals(FoodAppeals.Status.RESOLVED,closed.status());assertEquals(20,closed.imports());assertEquals(0,closed.production());
        assertEquals(appeal,new FoodAppealRepository(db.getConnection()).load(town,10).orElseThrow());
    }
    @Test void appealWriteFailureRollsBackFoodNeedsPrioritiesAndDeliveryUse() throws SQLException {
        observe(1000,true,0);var operation=UUID.randomUUID();runtime.recordDeliveredFood(operation,town,20,"Appeal rollback",1000);
        sql("CREATE TRIGGER reject_appeal BEFORE INSERT ON food_appeals BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));var view=runtime.views().get(town);
        assertEquals(0,view.account().state().completedPeriods());assertNull(view.needs());assertNull(view.priority());assertNull(view.appeal());
        assertNull(new FoodDeliveryRepository(db.getConnection()).find(operation).orElseThrow().appliedPeriod());
        sql("DROP TRIGGER reject_appeal");runtime.pulse(2000,false);assertEquals(1,runtime.views().get(town).appeal().period());
        assertEquals(51,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
    }
    @Test void schema19UpgradeDoesNotInventAppealHistory() throws SQLException {
        observe(1000,true,0);runtime.pulse(2000,false);var old=runtime.views().get(town).needs();
        SchemaFixtures.rewind(db.getConnection(),19);new SchemaMigrator().migrate(db.getConnection());
        assertNull(runtime.views().get(town).appeal());assertEquals(old,runtime.views().get(town).needs());
        runtime.pulse(3000,false);assertEquals(2,runtime.views().get(town).appeal().period());
    }
    private void learningFamine() throws SQLException {
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,
                VillagePriorities.Policy.prototype().withLearning(12,25),0);
        observe(1000,true,0);for(int period=1;period<=10;period++)runtime.pulse(1000L+1000L*period,false);
    }
    private void responseFamine() throws SQLException {
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,
                VillagePriorities.Policy.prototype().withLearning(12,25),42,OpeningFunds.prototype(),HouseholdWellbeing.Policy.disabled(),FoodResponses.Settings.prototype());
        observe(1000,true,0);for(int period=1;period<=10;period++)runtime.pulse(1000L+1000L*period,false);
    }
    @Test void responseComparesActualFundsAndUnknownGoodsAndKeepsHistoryAcrossTradeAndRestart() throws SQLException {
        responseFamine();var before=runtime.views().get(town).response();assertNotNull(before);
        assertTrue(before.candidates().stream().filter(c->c.action()==FoodResponses.Action.OFFER_EMERALDS).findFirst().orElseThrow().feasible());
        assertFalse(before.candidates().stream().filter(c->c.action()==FoodResponses.Action.OFFER_TIMBER).findFirst().orElseThrow().feasible());
        var terms=new FoodPurchases.Terms(UUID.randomUUID(),UUID.randomUUID(),town,4,"WHEAT",4,1,800);
        runtime.reserveFoodPurchase(terms,FoodPurchases.Policy.prototype(),FoodAidPreview.Policy.prototype(),11000);
        runtime.completeFoodPurchase(terms,11000);runtime.pulse(12000,false);
        assertEquals(4,runtime.views().get(town).latest().reports().getFirst().flow().imports());
        assertEquals(before,new FoodResponseRepository(db.getConnection()).load(town,10).orElseThrow());
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest"));db.initialize();
        assertEquals(before,new FoodResponseRepository(db.getConnection()).load(town,10).orElseThrow());
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,VillagePriorities.Policy.prototype(),42,OpeningFunds.disabled());runtime.start(12000);runtime.pulse(13000,false);
        assertNull(runtime.views().get(town).response());assertEquals(before,new FoodResponseRepository(db.getConnection()).load(town,10).orElseThrow());
    }
    @Test void responseFailureRollsBackWholePeriodAndDoesNotLoseIncomingFood() throws SQLException {
        responseFamine();var before=runtime.views().get(town).account().state();var id=UUID.randomUUID();
        runtime.recordDeliveredFood(id,town,5,"Known donation",11000);
        sql("CREATE TRIGGER reject_response BEFORE INSERT ON food_response_candidates BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(12000,false));assertEquals(before,runtime.views().get(town).account().state());
        assertTrue(new FoodResponseRepository(db.getConnection()).load(town,11).isEmpty());assertNull(new FoodDeliveryRepository(db.getConnection()).find(id).orElseThrow().appliedPeriod());
        sql("DROP TRIGGER reject_response");runtime.pulse(12000,false);assertEquals(FoodResponses.Action.WAIT,runtime.views().get(town).response().selected());
        assertEquals(11,new FoodDeliveryRepository(db.getConnection()).find(id).orElseThrow().appliedPeriod());
    }
    @Test void responseMigrationFailureRollsBackNewTablesAndRetainsOlderChoices() throws SQLException {
        learningFamine();var before=runtime.views().get(town).priority();SchemaFixtures.rewind(db.getConnection(),25);
        sql("CREATE TABLE food_response_candidates (collision INTEGER)");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(25,r.getInt(1));}
        assertEquals(before,new VillagePriorityRepository(db.getConnection()).latest(town).orElseThrow());
        assertTrue(new FoodResponseRepository(db.getConnection()).load(town,10).isEmpty());
        sql("DROP TABLE food_response_candidates");new SchemaMigrator().migrate(db.getConnection());
        assertTrue(new FoodResponseRepository(db.getConnection()).load(town,10).isEmpty());
    }
    @Test void missingSavedResponseCandidateFailsRatherThanInventingAnAlternative() throws SQLException {
        responseFamine();sql("DELETE FROM food_response_candidates WHERE period=10 AND action='WAIT'");
        assertThrows(SQLException.class,()->new FoodResponseRepository(db.getConnection()).load(town,10));
    }
    @Test void cashlessAndCoveredPendingSupplyBlockOffersWithoutInventingFunds() throws SQLException {
        responseFamine();new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,-runtime.views().get(town).treasury().hundredths(),"Cash exhaustion scenario");
        runtime.pulse(12000,false);var plan=runtime.views().get(town).response();
        assertEquals(FoodResponses.Action.REQUEST_DONATION,plan.selected());assertFalse(plan.candidates().get(2).feasible());
        runtime.recordDeliveredFood(UUID.randomUUID(),town,100,"Known future delivery",12000);
        // Accrue a backlog: payment options are blocked while catching up; pending supplies cover the target.
        runtime.recordDeliveredFood(UUID.randomUUID(),town,100,"Later delivery",14000);
        runtime.pulse(14000,false);plan=runtime.views().get(town).response();assertEquals(FoodResponses.Action.WAIT,plan.selected());
    }
    @Test void response240PeriodScenarioPreservesPublishedChoicesAndExactDeliveryUseAcrossRestart() throws SQLException {
        var priority=VillagePriorities.Policy.prototype().withLearning(12,25);
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,priority,42,
                OpeningFunds.prototype(),HouseholdWellbeing.Policy.disabled(),FoodResponses.Settings.prototype());observe(1000,true,0);
        com.blueninjas24.civilizations.economy.FoodResponses.Plan first=null;long imported=0;
        for(long period=1;period<=240;period++) {
            if(period%17==0)runtime.recordDeliveredFood(UUID.randomUUID(),town,5,"Scenario delivery",period*1000);
            runtime.pulse(1000+period*1000,false);var view=runtime.views().get(town);assertNotNull(view.response());
            assertTrue(view.response().candidates().stream().anyMatch(c->c.action()==view.response().selected()&&c.feasible()&&c.eligible()));
            imported+=view.foodImportImpact().orElseThrow().imported();if(period==10)first=view.response();
            if(period==120){db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest"));db.initialize();
                runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,priority,42,
                        OpeningFunds.prototype(),HouseholdWellbeing.Policy.disabled(),FoodResponses.Settings.prototype());runtime.start(121000);}
        }
        assertEquals(70,imported);assertEquals(first,new FoodResponseRepository(db.getConnection()).load(town,10).orElseThrow());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM food_response_candidates")){assertEquals(240*5,r.getInt(1));}
    }
    @Test void verifiedEarlierImportsChangeWeightsAndReplayExactlyAcrossRestart() throws SQLException {
        learningFamine();var id=UUID.randomUUID();runtime.recordDeliveredFood(id,town,5,"Known delivery",11000);
        runtime.pulse(12000,false);assertEquals(VillagePriorities.Evidence.none(),runtime.views().get(town).priority().evidence());
        runtime.pulse(13000,false);var choice=runtime.views().get(town).priority();
        assertEquals(new VillagePriorities.Evidence(5,5),choice.evidence());
        assertEquals(1250,choice.candidates().get(1).weight());
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest"));db.initialize();
        assertEquals(choice,new VillagePriorityRepository(db.getConnection()).latest(town).orElseThrow());
        runtime=runtime(settings);runtime.start(13000);runtime.pulse(14000,false);
        assertEquals(0,runtime.views().get(town).priority().policy().learningPeriods());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT imported,avoided_unmet FROM priority_learning WHERE period=12")) {
            assertTrue(r.next());assertEquals(5,r.getLong(1));assertEquals(5,r.getLong(2));
        }
    }
    @Test void failedLearningWriteRollsBackFoodReceiptAndRetryRetainsExactlyOneOutcome() throws SQLException {
        learningFamine();var before=runtime.views().get(town).account().state();var id=UUID.randomUUID();
        runtime.recordDeliveredFood(id,town,5,"Known delivery",11000);
        sql("CREATE TRIGGER reject_learning BEFORE INSERT ON priority_learning BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(12000,false));assertEquals(before,runtime.views().get(town).account().state());
        assertNull(new FoodDeliveryRepository(db.getConnection()).find(id).orElseThrow().appliedPeriod());
        sql("DROP TRIGGER reject_learning");runtime.pulse(12000,false);runtime.pulse(13000,false);
        assertEquals(new VillagePriorities.Evidence(5,5),runtime.views().get(town).priority().evidence());
    }
    @Test void schema24UpgradePreservesLegacyChoicesAndMigrationFailureIsAtomic() throws SQLException {
        observe(1000,true,0);runtime.pulse(2000,false);var legacy=runtime.views().get(town).priority();
        SchemaFixtures.rewind(db.getConnection(),24);sql("CREATE TABLE priority_learning (collision INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(24,r.getInt(1));}
        sql("DROP TABLE priority_learning");new SchemaMigrator().migrate(db.getConnection());
        assertEquals(legacy,new VillagePriorityRepository(db.getConnection()).latest(town).orElseThrow());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT count(*) FROM priority_learning")){assertEquals(0,r.getInt(1));}
    }
    @Test void learningWindowExpiresAndUnverifiedHistoryCannotEarnBonus() throws SQLException {
        learningFamine();var id=UUID.randomUUID();runtime.recordDeliveredFood(id,town,5,"Known delivery",11000);
        runtime.pulse(12000,false);runtime.pulse(13000,false);assertEquals(5,runtime.views().get(town).priority().evidence().imported());
        for(int period=13;period<=30;period++)runtime.pulse(1000L+1000L*period,false);
        assertEquals(VillagePriorities.Evidence.none(),runtime.views().get(town).priority().evidence());
        // A reported import without matching applied receipt must not be treated as a verified action.
        sql("UPDATE food_deliveries SET units=6 WHERE applied_period=11");
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,
                VillagePriorities.Policy.prototype().withLearning(100,25),0);
        runtime.start(31000);runtime.pulse(32000,false);
        assertEquals(VillagePriorities.Evidence.none(),runtime.views().get(town).priority().evidence());
    }
    @Test void missingSavedLearningEvidenceFailsEvenWhenLegacySelectionWouldMatch() throws SQLException {
        learningFamine();sql("DELETE FROM priority_learning WHERE period=(SELECT MAX(period) FROM village_priorities)");
        var failure=assertThrows(SQLException.class,()->new VillagePriorityRepository(db.getConnection()).latest(town));
        assertTrue(failure.getMessage().contains("evidence missing"));
    }
    @Test void seeded240PeriodLearningUsesOnlyAppliedEarlierReceiptsAndSurvivesRestart() throws SQLException {
        var policy=VillagePriorities.Policy.prototype().withLearning(12,25);
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,policy,42);
        observe(1000,true,0);var outcomes=new java.util.TreeMap<Long,FoodImportImpact.Outcome>();long imported=0;
        for(long period=1;period<=240;period++) {
            if(period%17==0)runtime.recordDeliveredFood(UUID.randomUUID(),town,5,"Scenario verified delivery",period*1000);
            runtime.pulse(1000+period*1000,false);var view=runtime.views().get(town);
            var impact=view.foodImportImpact().orElseThrow();outcomes.put(period,impact);imported+=impact.imported();
            var choice=view.priority();long knownImports=0,knownAvoided=0;
            for(var entry:outcomes.subMap(Math.max(1,choice.period()-12),true,choice.period(),false).values()) {
                knownImports+=entry.imported();knownAvoided+=entry.avoidedUnmet();
            }
            assertEquals(new VillagePriorities.Evidence(knownImports,knownAvoided),choice.evidence());
            assertTrue(choice.evidence().bonus(policy)<=25);assertNull(view.treasury());
            if(period==120) {
                db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("FoodRuntimeTest"));db.initialize();
                runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,policy,42);
                runtime.start(121000);assertEquals(choice,runtime.views().get(town).priority());
            }
        }
        assertEquals(14*5,imported);assertEquals(0,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
    }
    @Test void failedAppealMigrationPreservesVersionAndRetriesAfterRepair() throws SQLException {
        SchemaFixtures.rewind(db.getConnection(),19);sql("CREATE TABLE food_appeals (bad_column TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertTrue(r.next());assertEquals(19,r.getInt(1));}
        sql("DROP TABLE food_appeals");new SchemaMigrator().migrate(db.getConnection());
        sql("SELECT priority_period FROM food_appeals");
    }

}
