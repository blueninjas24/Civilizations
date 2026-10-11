package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class IndustryRuntimeTest {
    @TempDir Path directory;
    DatabaseManager db;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),miner=UUID.randomUUID(),smith=UUID.randomUUID();
    UUID business,deposit;
    final Commodity tools=new Commodity("tools");
    Map<Commodity,Long> space=Map.of(Commodity.COAL,1L,Commodity.IRON,1L,Commodity.STONE,1L,tools,1L);
    IndustryRuntimePolicy policy(long cadence,int budget) {
        return new IndustryRuntimePolicy(true,cadence,budget,new IndustryPolicy(10,1,2,100,1,128,
                Map.of(Commodity.COAL,1L,Commodity.IRON,2L,Commodity.STONE,1L),space),Map.of("smith_tools",ProductionRecipe.smithTools()));
    }
    void open() throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("IndustryRuntimeTest"));db.initialize();}
    @BeforeEach void setup() throws Exception {
        open();db.saveSettlement(new Settlement(town,"Test","Fixture",0,64,0,2,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var person:List.of(miner,smith))db.getPopulationRepository().insertResident(new Resident(person,"Fixture",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Actual test capital");
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,10,"Explicit test evidence");
        var b=new Business(UUID.randomUUID(),town,"Mine",miner,miner,"mining",site.id(),2,0);
        var req=new JobRequirements(216,Map.of());
        var saved=new MiningStartupRepository(db.getConnection()).start(new MiningStartupRepository.Request(
                new BusinessFounding(b,site,req,req,500,"Funded test opportunity"),
                new MineralObservation(world,10,true,Commodity.IRON,List.of(new VillageObservation.Position(0,40,0))),
                10,Map.of(Commodity.COAL,1L,Commodity.IRON,2L,Commodity.STONE,1L),100,space,1000,miner));
        business=b.id();deposit=saved.discovery().deposit().id();
    }
    @AfterEach void close(){if(db!=null)db.close();}
    long stock(Commodity commodity)throws Exception{return db.getIndustryRepository().store(business).orElseThrow().units().get(commodity);}
    long remaining()throws Exception{return db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches();}
    long count(String table)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String statement)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(statement);}}
    @Test void actualTimeProducesOnceAndRestartPreservesFractionWithoutDowntime() throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,8));runtime.start(1000);
        assertEquals(0,runtime.pulse(1500,Set.of(world)));assertEquals(0,stock(Commodity.COAL));
        assertEquals(1,runtime.pulse(2000,Set.of(world)));assertEquals(1,stock(Commodity.COAL));assertEquals(9,remaining());
        assertEquals(0,runtime.pulse(2000,Set.of(world)));runtime.pulse(2500,Set.of(world));
        db.close();open();runtime=new IndustryRuntime(db.getConnection(),policy(1000,8));runtime.start(100_000);
        assertEquals(0,runtime.pulse(100_499,Set.of(world)));assertEquals(1,runtime.pulse(100_500,Set.of(world)));
        assertEquals(2,stock(Commodity.COAL));assertEquals(2,count("industry_runtime_history"));
        assertEquals(8500,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
    }
    @Test void absentWorldPausesButRetainsAlreadyEarnedQueuedWork() throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,1));runtime.start(1000);
        assertEquals(1,runtime.pulse(4000,Set.of(world))); // Three earned; one processed.
        assertEquals(0,runtime.pulse(100_000,Set.of()));assertEquals(9,remaining());
        assertEquals(1,runtime.pulse(100_000,Set.of(world)));assertEquals(1,runtime.pulse(100_000,Set.of(world)));
        assertEquals(0,runtime.pulse(100_000,Set.of(world)));assertEquals(7,remaining());
    }
    @Test void lateCheckpointFailureRollsBackGoodsReservesReceiptsThenRetryUsesSamePeriod() throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,8));runtime.start(1000);
        sql("CREATE TRIGGER fail_progress BEFORE UPDATE OF completed ON industry_runtime BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,Set.of(world)));
        assertEquals(10,remaining());assertEquals(0,stock(Commodity.COAL));assertEquals(0,count("industry_production"));assertEquals(0,count("industry_runtime_history"));
        sql("DROP TRIGGER fail_progress");assertEquals(1,runtime.pulse(2000,Set.of(world)));assertEquals(9,remaining());assertEquals(1,count("industry_production"));
    }
    @Test void unavailableOwnerPausesWithoutInventingInheritanceOrSpending() throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,8));runtime.start(1000);
        db.getPopulationRepository().updateResident(new Resident(miner,"Fixture",town,null,300,Resident.Status.DECEASED));
        assertEquals(1,runtime.pulse(2000,Set.of(world)));assertEquals(10,remaining());assertEquals(0,stock(Commodity.COAL));
        assertEquals(0,count("industry_production"));assertEquals(1,count("industry_runtime_history"));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT explanation FROM industry_runtime_history")){assertTrue(r.getString(1).contains("owner unavailable"));}
        var view=new BusinessRuntime(db.getConnection(),8,32).refresh(1,Set.of(world)).businesses().get(business);
        assertTrue(view.lastProduction().contains("owner unavailable"));assertFalse(view.ownerAvailable());
    }
    private void staffedAfterDeath()throws Exception {
        db.getBusinessRepository().endEmployment(db.getBusinessRepository().currentWorkers(business).getFirst().id(),0,"Fixture owner stops working");
        db.getBusinessRepository().hire(UUID.randomUUID(),business,smith,0);
        db.getPopulationRepository().updateResident(new Resident(miner,"Fixture",town,null,300,Resident.Status.DECEASED));
    }
    @Test void approvedExistingStaffContinueFiniteMiningWithoutPromotionOrMoneyCreation()throws Exception {
        staffedAfterDeath();var runtime=new IndustryRuntime(db.getConnection(),policy(1000,8),BusinessOperationPolicy.prototype());runtime.start(1000);
        runtime.pulse(2000,Set.of(world));assertEquals(1,stock(Commodity.COAL));assertEquals(9,remaining());
        assertEquals(miner,db.getBusinessRepository().findBusiness(business).orElseThrow().ownerId());assertEquals(0,count("business_ownership"));
        assertEquals(1000,db.getIndustryRepository().store(business).orElseThrow().hundredths());assertEquals(0,count("family_estates"));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT explanation FROM industry_runtime_history")){assertTrue(r.getString(1).contains("Qualified existing staff"));}
        runtime.pulse(100000,Set.of());assertEquals(9,remaining());
    }
    @Test void adminCanPauseStaffedFirmWithoutEndingJobsOrResettingAssets()throws Exception {
        staffedAfterDeath();var runtime=new IndustryRuntime(db.getConnection(),policy(1000,8),new BusinessOperationPolicy(true,false));runtime.start(1000);
        runtime.pulse(2000,Set.of(world));assertEquals(10,remaining());assertEquals(0,stock(Commodity.COAL));assertEquals(1,db.getBusinessRepository().currentWorkers(business).size());
    }
    @Test void approvalDoesNotTreatAMovedLivingOwnerAsDeceased()throws Exception {
        UUID other=UUID.randomUUID();db.saveSettlement(new Settlement(other,"Elsewhere","Fixture",20,64,0,1,0,0));
        sql("UPDATE residents SET settlement_id='"+other+"' WHERE id='"+miner+"'");
        db.getBusinessRepository().hire(UUID.randomUUID(),business,smith,0);
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,8),BusinessOperationPolicy.prototype());runtime.start(1000);runtime.pulse(2000,Set.of(world));
        assertEquals(10,remaining());assertEquals(0,stock(Commodity.COAL));
    }
    @Test void ownerDeathWithoutQualifiedExistingStaffDoesNotInventWorkers()throws Exception {
        staffedAfterDeath();db.getPopulationRepository().updateResident(new Resident(smith,"Fixture",town,null,100,Resident.Status.ALIVE));
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,8),BusinessOperationPolicy.prototype());runtime.start(1000);runtime.pulse(2000,Set.of(world));
        assertEquals(10,remaining());assertEquals(0,stock(Commodity.COAL));assertEquals(0,count("industry_production"));
    }
    @Test void deliveredInputsAreConsumedByTimedWorkshopAndBudgetDoesNotStarveEitherFirm() throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,3,64,0,10,"Explicit workshop test");
        var b=new Business(UUID.randomUUID(),town,"Smith",smith,smith,"blacksmith",site.id(),1,0);var req=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(b,site,req,req,0,"Test workshop"));db.getBusinessRepository().hire(UUID.randomUUID(),b.id(),smith,0);
        var industry=db.getIndustryRepository();industry.openStore(b.id(),100,space,1000);
        industry.mine(UUID.randomUUID(),business,deposit,1,0,1,2);
        for(var e:Map.of(Commodity.COAL,1L,Commodity.IRON,2L).entrySet()) {
            var shipment=industry.purchase(UUID.randomUUID(),business,b.id(),e.getKey(),e.getValue(),100,0,1,128);industry.deliver(shipment.id(),1);
        }
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,1));runtime.start(1000);
        runtime.pulse(2000,Set.of(world));runtime.pulse(2000,Set.of(world));
        assertEquals(1,industry.store(b.id()).orElseThrow().units().get(tools));assertEquals(0,industry.store(b.id()).orElseThrow().units().get(Commodity.IRON));
        assertEquals(2,count("industry_runtime_history"));assertEquals(8,remaining());
    }
    @Test void capacityAndFiniteDepletionStopOutputWithoutDiscardingGoodsOrCreatingMoney() throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,1));runtime.start(1000);
        for(int i=0;i<12;i++)runtime.pulse(13_000,Set.of(world));
        assertEquals(0,remaining());assertEquals(10,stock(Commodity.COAL));assertEquals(12,count("industry_runtime_history"));
        assertEquals(1000,db.getIndustryRepository().store(business).orElseThrow().hundredths());
        // Fresh replay/clock cannot refill the exhausted coordinates.
        runtime.start(100_000);runtime.pulse(101_000,Set.of(world));assertEquals(10,stock(Commodity.COAL));
    }
    @Test void retuneRequiresEmptyQueueAndRecordsDiscardedFractionAndTransactionContracts() throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,1));runtime.start(1000);runtime.pulse(2500,Set.of(world));
        var changed=new IndustryRuntime(db.getConnection(),policy(2000,1));changed.start(5000);assertEquals(1,count("industry_runtime_tuning"));
        assertEquals(0,changed.pulse(6999,Set.of(world)));assertEquals(1,changed.pulse(7000,Set.of(world)));
        changed.pulse(11000,Set.of(world));
        assertThrows(SQLException.class,()->new IndustryRuntime(db.getConnection(),policy(1000,1)).start(12000));
        db.getConnection().setAutoCommit(false);assertThrows(SQLException.class,()->changed.start(12000));db.getConnection().rollback();db.getConnection().setAutoCommit(true);
        assertThrows(SQLException.class,()->db.getIndustryRepository().mineRows(UUID.randomUUID(),business,deposit,1,100,1,1));
    }
    @Test void schema34UpgradeIsTransactionalAndRetainsExistingIndustry() throws Exception {
        SchemaFixtures.rewind(db.getConnection(),34);sql("CREATE TABLE industry_runtime(bad TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertEquals(10,remaining());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(34,r.getInt(1));}
        sql("DROP TABLE industry_runtime");new SchemaMigrator().migrate(db.getConnection());assertEquals(10,remaining());assertEquals(0,count("industry_runtime"));
    }
    @Test void fullStorageAndDeadEmployeeStopWorkWithLivingOwnerAndRetainExistingStock() throws Exception {
        sql("UPDATE industry_stores SET capacity=4");
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,8));runtime.start(1000);
        runtime.pulse(2000,Set.of(world));assertEquals(9,remaining());
        runtime.pulse(3000,Set.of(world));assertEquals(9,remaining());assertEquals(1,stock(Commodity.COAL));
        var businesses=db.getBusinessRepository();
        businesses.endEmployment(businesses.currentWorkers(business).getFirst().id(),0,"Owner leaves extraction work");
        businesses.hire(UUID.randomUUID(),business,smith,0);sql("UPDATE industry_stores SET capacity=100");
        db.getPopulationRepository().updateResident(new Resident(smith,"Fixture",town,null,300,Resident.Status.DECEASED));
        runtime.pulse(4000,Set.of(world));assertEquals(9,remaining());assertEquals(1,stock(Commodity.COAL));
        assertEquals(3,count("industry_runtime_history"));
    }
    private PopulationSession session(AtomicLong now) {
        var settings=new PopulationSession.Settings(42,1000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),MigrationPolicy.disabled(),VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
        var session=new PopulationSession(directory.toFile(),Logger.getLogger("IndustrySessionTest"),settings,now::get);
        session.configureIndustryRuntime(policy(1000,8));return session;
    }
    @Test void actualOwnerSavesMutationBeforeNextProductionAndGapPersistsThroughRestart()throws Exception {
        db.close();var now=new AtomicLong(1000);
        try(var session=session(now)) {
            session.configureOreTracking();session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
            now.set(2000);session.pulse().get(5,TimeUnit.SECONDS);
            var origin=new VillageObservation.Position(0,40,0);
            var result=session.recordOreMutations(world,"Fixture",List.of(),List.of(origin),2100,"Player break").get(5,TimeUnit.SECONDS);
            assertEquals(9,result.retiredBatches());now.set(3000);session.pulse().get(5,TimeUnit.SECONDS);
            session.invalidateOreTracking("Injected capture failure");now.set(4000);session.pulse().get(5,TimeUnit.SECONDS);assertFalse(session.oreDiscoveryReady());
        }
        open();assertEquals(0,remaining());assertEquals(1,stock(Commodity.COAL));assertFalse(new OreTrackingRepository(db.getConnection()).load().orElseThrow().trusted());db.close();
        try(var session=session(now)) {
            session.configureOreTracking();session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);assertFalse(session.oreDiscoveryReady());
        }
        open();assertEquals(1,stock(Commodity.COAL));
    }
    @Test void persistedAndImmediateCaptureGapsPauseCoordinateMiningWithSavedExplanation()throws Exception {
        var healthy=new java.util.concurrent.atomic.AtomicBoolean(true);
        var runtime=new IndustryRuntime(db.getConnection(),policy(1000,8),BusinessOperationPolicy.disabled(),WorkLearningPolicy.disabled(),healthy::get);
        runtime.start(1000);healthy.set(false);runtime.pulse(2000,Set.of(world));assertEquals(10,remaining());assertEquals(0,stock(Commodity.COAL));
        try(var q=db.getConnection().createStatement();var r=q.executeQuery("SELECT explanation FROM industry_runtime_history")){assertTrue(r.getString(1).contains("reviewed recovery"));}
        new OreTrackingRepository(db.getConnection()).begin(2100);new OreTrackingRepository(db.getConnection()).markGap(2200);healthy.set(true);
        runtime.pulse(3000,Set.of(world));assertEquals(10,remaining());assertEquals(0,count("industry_production"));
    }
    @Test void failedProductionDoesNotBlockPopulationWorldPauseAndRemainsDiagnosed() throws Exception {
        sql("CREATE TRIGGER fail_progress BEFORE UPDATE OF completed ON industry_runtime BEGIN SELECT RAISE(ABORT,'injected progress'); END");
        db.close();var now=new AtomicLong(1000);
        try(var session=session(now)) {
            session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
            now.set(2000);session.pulse().get(5,TimeUnit.SECONDS);assertTrue(session.industryRuntimeDiagnostic().contains("injected progress"));
            session.updateLoadedWorlds(Set.of()).get(5,TimeUnit.SECONDS);
            long age=session.snapshot().orElseThrow().residents().stream().filter(r->r.id().equals(miner)).findFirst().orElseThrow().ageSteps();
            now.set(100_000);session.pulse().get(5,TimeUnit.SECONDS);
            assertEquals(age,session.snapshot().orElseThrow().residents().stream().filter(r->r.id().equals(miner)).findFirst().orElseThrow().ageSteps());
        }
        open();assertEquals(10,remaining());assertEquals(0,count("industry_production"));
    }
    @Test void failedIndustryBoundarySuspendsIndustryButStillChangesPopulationWorldMask() throws Exception {
        new IndustryRuntime(db.getConnection(),policy(1000,8)).start(1000);
        sql("CREATE TRIGGER fail_boundary BEFORE UPDATE OF last_ms ON industry_runtime WHEN NEW.last_ms>OLD.last_ms BEGIN SELECT RAISE(ABORT,'injected boundary'); END");
        db.close();var now=new AtomicLong(1000);
        try(var session=session(now)) {
            session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
            now.set(2000);session.updateLoadedWorlds(Set.of()).get(5,TimeUnit.SECONDS);
            assertTrue(session.industryRuntimeDiagnostic().contains("restart"));
            long age=session.snapshot().orElseThrow().residents().stream().filter(r->r.id().equals(miner)).findFirst().orElseThrow().ageSteps();
            now.set(100_000);session.pulse().get(5,TimeUnit.SECONDS);session.updateLoadedWorlds(Set.of(world)).get(5,TimeUnit.SECONDS);
            assertEquals(age,session.snapshot().orElseThrow().residents().stream().filter(r->r.id().equals(miner)).findFirst().orElseThrow().ageSteps());
            assertTrue(session.industryRuntimeDiagnostic().contains("restart"));
        }
        open();assertEquals(10,remaining());sql("DROP TRIGGER fail_boundary");db.close();
        now.set(1_000_000);
        try(var session=session(now)) {
            session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);now.set(1_001_000);session.pulse().get(5,TimeUnit.SECONDS);
            assertTrue(session.industryRuntimeDiagnostic().contains("active"));
        }
        open();assertEquals(9,remaining());
    }
}
