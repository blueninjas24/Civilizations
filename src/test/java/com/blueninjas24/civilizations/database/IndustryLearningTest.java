package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class IndustryLearningTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=UUID.randomUUID(),worker=UUID.randomUUID(),other=UUID.randomUUID(),deposit=UUID.randomUUID();
    UUID mine;IndustryRuntimePolicy production;
    final Commodity tools=new Commodity("tools");
    final Map<Commodity,Long> space=Map.of(Commodity.COAL,1L,Commodity.IRON,1L,tools,1L);
    void open()throws SQLException {db=new DatabaseManager(directory.toFile(),Logger.getLogger("LearningTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Learning Fixture","World",0,64,0,2,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"World");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var id:List.of(owner,worker,other))db.getPopulationRepository().insertResident(new Resident(id,"Fixture",town,null,300,Resident.Status.ALIVE));
        db.getPopulationRepository().recordSkillExperience(owner,"mining",20);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Test-owned capital");
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,0,"Test-owned mining evidence");
        var business=new Business(UUID.randomUUID(),town,"Mine",owner,owner,"mining",site.id(),3,0);mine=business.id();
        db.getBusinessRepository().found(new BusinessFounding(business,site,new JobRequirements(216,Map.of("mining",2L)),new JobRequirements(216,Map.of()),0,"Saved skill requirements"));
        db.getBusinessRepository().hire(UUID.randomUUID(),mine,worker,0);db.getIndustryRepository().openStore(mine,100,space,1000);
        db.getIndustryRepository().discover(deposit,site.id(),1,10,Map.of(Commodity.COAL,1L,Commodity.IRON,2L));
        production=new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(10,1,1,100,1,128,Map.of(Commodity.COAL,1L,Commodity.IRON,2L),space),Map.of("smith_tools",ProductionRecipe.smithTools()));
    }
    @AfterEach void close(){db.close();}
    IndustryRuntime runtime(WorkLearningPolicy learning)throws SQLException {var r=new IndustryRuntime(db.getConnection(),production,BusinessOperationPolicy.prototype(),learning);r.start(1000);return r;}
    long xp(UUID resident,String skill)throws SQLException {return db.getPopulationRepository().findWorkProfile(resident).orElseThrow().experience().getOrDefault(skill,0L);}
    long count(String table)throws SQLException {try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String query)throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(query);}}
    UUID operation(long period) {return UUID.nameUUIDFromBytes(("civilizations:timed-industry-v1:"+mine+":"+period).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    @Test void actualWorkLearnsOnceAndRetuningPreservesOldAwardsAcrossRestartAbsenceAndCeiling()throws Exception {
        var r=runtime(WorkLearningPolicy.prototype());r.pulse(2000,Set.of(world));assertEquals(1,xp(worker,"mining"));assertEquals(20,xp(owner,"mining"));assertEquals(0,xp(other,"mining"));
        var learning=new IndustryLearning(db.getConnection(),WorkLearningPolicy.prototype());assertEquals(List.of(new IndustryLearning.Entry(worker,0,1)),learning.entries(operation(1)));
        var explanation=learning.explanation(operation(1)).orElseThrow();r.pulse(2000,Set.of(world));r.pulse(100000,Set.of());assertEquals(1,xp(worker,"mining"));
        db.close();open();var tuned=new WorkLearningPolicy(true,3,4,64,Map.of("mining","mining"));r=new IndustryRuntime(db.getConnection(),production,BusinessOperationPolicy.prototype(),tuned);r.start(100000);
        r.pulse(101000,Set.of(world));assertEquals(4,xp(worker,"mining"));r.pulse(102000,Set.of(world));assertEquals(4,xp(worker,"mining"));
        assertEquals(2,count("industry_learning_entries"));assertEquals(explanation,new IndustryLearning(db.getConnection(),tuned).explanation(operation(1)).orElseThrow());
        assertEquals(1000,db.getIndustryRepository().store(mine).orElseThrow().hundredths());assertEquals(7,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
    }
    @Test void noWorkersNoOutputDisabledAndUnmappedIndustriesCannotAwardExperience()throws Exception {
        var r=runtime(WorkLearningPolicy.disabled());r.pulse(2000,Set.of(world));assertEquals(0,xp(worker,"mining"));assertTrue(new IndustryLearning(db.getConnection(),WorkLearningPolicy.disabled()).explanation(operation(1)).orElseThrow().contains("disabled"));
        r=new IndustryRuntime(db.getConnection(),production,BusinessOperationPolicy.prototype(),new WorkLearningPolicy(true,1,100,64,Map.of()));r.pulse(3000,Set.of(world));assertEquals(0,xp(worker,"mining"));
        var job=db.getBusinessRepository().currentWorkers(mine).getFirst();db.getBusinessRepository().endEmployment(job.id(),0,"Test separation");
        r=new IndustryRuntime(db.getConnection(),production,BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());r.pulse(4000,Set.of(world));assertEquals(0,count("industry_learning_entries"));
        assertTrue(new IndustryLearning(db.getConnection(),WorkLearningPolicy.prototype()).explanation(operation(3)).orElseThrow().contains("No actual production"));
        assertEquals(8,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
    }
    @Test void lateCheckpointFailureRollsBackProductionExperienceAndAuditThenRetriesOnce()throws Exception {
        var r=runtime(WorkLearningPolicy.prototype());sql("CREATE TRIGGER fail_learning_checkpoint BEFORE UPDATE OF completed ON industry_runtime BEGIN SELECT RAISE(ABORT,'test checkpoint'); END");
        assertThrows(SQLException.class,()->r.pulse(2000,Set.of(world)));assertEquals(0,xp(worker,"mining"));assertEquals(0,count("industry_learning_entries"));assertEquals(0,count("industry_learning_reviews"));assertEquals(10,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
        sql("DROP TRIGGER fail_learning_checkpoint");r.pulse(2000,Set.of(world));assertEquals(1,xp(worker,"mining"));assertEquals(1,count("industry_learning_entries"));assertEquals(9,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
    }
    @Test void failedSkillAuditRestoresSkillGoodsAndReserveTogether()throws Exception {
        var r=runtime(WorkLearningPolicy.prototype());sql("CREATE TRIGGER fail_learning_entry BEFORE INSERT ON industry_learning_entries BEGIN SELECT RAISE(ABORT,'test learning audit'); END");
        assertThrows(SQLException.class,()->r.pulse(2000,Set.of(world)));assertEquals(0,xp(worker,"mining"));assertEquals(0,count("industry_production"));assertEquals(0,count("industry_learning_reviews"));
        sql("DROP TRIGGER fail_learning_entry");r.pulse(2000,Set.of(world));assertEquals(1,xp(worker,"mining"));assertEquals(1,count("industry_learning_entries"));
    }
    @Test void workerBudgetDefersAllAwardsWithoutStoppingRealProductionAndDoesNotRetroactivelyTrain()throws Exception {
        db.getBusinessRepository().hire(UUID.randomUUID(),mine,other,0);var policy=new WorkLearningPolicy(true,1,100,1,Map.of("mining","mining"));var r=runtime(policy);r.pulse(2000,Set.of(world));
        assertEquals(0,xp(worker,"mining"));assertEquals(0,xp(other,"mining"));assertEquals(9,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
        assertTrue(new IndustryLearning(db.getConnection(),policy).explanation(operation(1)).orElseThrow().contains("budget exceeded"));
        r=new IndustryRuntime(db.getConnection(),production,BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());r.pulse(3000,Set.of(world));assertEquals(1,xp(worker,"mining"));assertEquals(1,xp(other,"mining"));
        assertTrue(new IndustryLearning(db.getConnection(),policy).entries(operation(1)).isEmpty());assertEquals(2,count("industry_learning_entries"));
    }
    @Test void gradualExistingStaffLearningCanReachRequiredSuccessionButDoesNotWaiveIt()throws Exception {
        db.getPopulationRepository().updateResident(new Resident(owner,"Fixture",town,null,300,Resident.Status.DECEASED));var r=runtime(WorkLearningPolicy.prototype());
        r.pulse(2000,Set.of(world));assertEquals(1,xp(worker,"mining"));assertTrue(new BusinessSuccession(db.getConnection()).candidate(mine).isEmpty());
        r.pulse(3000,Set.of(world));assertEquals(2,xp(worker,"mining"));new BusinessRuntime(db.getConnection(),8,32).refresh(2,Set.of(world));
        assertEquals(worker,db.getBusinessRepository().findBusiness(mine).orElseThrow().ownerId());assertEquals(owner,db.getBusinessRepository().findFounding(mine).orElseThrow().business().founderId());
        assertEquals(1,count("business_ownership"));assertEquals(1000,db.getIndustryRepository().store(mine).orElseThrow().hundredths());assertEquals(0,count("family_estates"));
    }
    @Test void workshopLearnsSmithingOnlyAfterActualDeliveredInputsAreConsumed()throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,10,40,0,0,"Test workshop");var smith=new Business(UUID.randomUUID(),town,"Smith",other,other,"blacksmith",site.id(),1,0);var req=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(smith,site,req,req,0,"Funded real workshop"));db.getBusinessRepository().hire(UUID.randomUUID(),smith.id(),other,0);db.getIndustryRepository().openStore(smith.id(),100,space,1000);
        var r=runtime(WorkLearningPolicy.prototype());r.pulse(2000,Set.of(world));assertEquals(0,xp(other,"smithing"));
        var industry=db.getIndustryRepository();var iron=UUID.randomUUID();var coal=UUID.randomUUID();industry.purchase(iron,mine,smith.id(),Commodity.IRON,2,100,1,1,128);industry.purchase(coal,mine,smith.id(),Commodity.COAL,1,100,1,1,128);
        r.pulse(3000,Set.of(world));assertEquals(0,xp(other,"smithing"));industry.deliver(iron,2);industry.deliver(coal,2);
        r.pulse(4000,Set.of(world));assertEquals(1,xp(other,"smithing"));assertEquals(1,industry.store(smith.id()).orElseThrow().units().get(tools));assertEquals(0,industry.store(smith.id()).orElseThrow().units().get(Commodity.IRON));
        assertEquals(800,industry.store(smith.id()).orElseThrow().hundredths());assertEquals(1200,industry.store(mine).orElseThrow().hundredths());
    }
    @Test void failedSchema41UpgradeRollsBackWithoutInventingPastLearningAndRetriesSafely()throws Exception {
        var r=runtime(WorkLearningPolicy.disabled());r.pulse(2000,Set.of(world));SchemaFixtures.rewind(db.getConnection(),41);sql("CREATE INDEX resident_industry_learning ON businesses(id)");db.close();
        var failed=new DatabaseManager(directory.toFile(),Logger.getLogger("LearningMigration"));assertThrows(SQLException.class,failed::initialize);failed.close();
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("civilizations.db"));var s=c.createStatement()) {
            try(var rows=s.executeQuery("PRAGMA user_version")){assertEquals(41,rows.getInt(1));}
            try(var rows=s.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='industry_learning_reviews'")){assertEquals(0,rows.getInt(1));}s.execute("DROP INDEX resident_industry_learning");
        }
        open();assertEquals(0,xp(worker,"mining"));assertEquals(9,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());assertEquals(0,count("industry_learning_reviews"));
        var c=db.getConnection();c.setAutoCommit(false);assertTrue(new IndustryLearning(c,WorkLearningPolicy.prototype()).reviewRows(operation(1),mine,false).contains("no retroactive"));c.commit();c.setAutoCommit(true);assertEquals(0,xp(worker,"mining"));
        r=new IndustryRuntime(c,production,BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());r.start(100000);r.pulse(101000,Set.of(world));assertEquals(1,xp(worker,"mining"));
        try(var s=c.createStatement();var rows=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(rows.next());}
    }
}
