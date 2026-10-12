package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticMineFoundingTest {
    @TempDir Path directory;
    DatabaseManager db;
    UUID town=new UUID(1,1),world=new UUID(1,2),owner=new UUID(0,1),smithWorker=new UUID(0,2),warehouseWorker=new UUID(0,3),founder=new UUID(0,4),other=new UUID(0,5);
    UUID smith=new UUID(2,1),warehouse=new UUID(2,2);
    MineFoundingPolicy founding=MineFoundingPolicy.prototype();
    boolean healthy=true;
    int batches=1;
    long capture=1999;
    EconomySettings economySettings;
    final Map<Commodity,Long> sizes=Map.of(Commodity.COAL,1L,Commodity.IRON,1L,Commodity.STONE,1L,ToolUpkeepPolicy.TOOLS,1L);
    IndustryRuntimePolicy industry() {
        var recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),batches,batches);
        return new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of("tools",recipe),new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L)),new WarehouseSupplyPolicy(true,16,16));
    }
    AutomaticMineFounding automation(){return new AutomaticMineFounding(db.getConnection(),founding,industry(),BusinessOperationPolicy.prototype(),StartupGrantPolicy.prototype(),216,42,()->healthy);}
    String review()throws Exception{return automation().review(town,1,Math.max(2000,capture),Set.of(world));}
    @BeforeEach void setup()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticMineTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,40,0,5,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var id:List.of(owner,smithWorker,warehouseWorker,founder,other))db.getPopulationRepository().insertResident(new Resident(id,"Fixture resident",town,null,300,Resident.Status.ALIVE));
        for(var id:List.of(founder,other))db.getPopulationRepository().recordSkillExperience(id,"mining",10);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,100000,"Explicit fixture funds");
        business(smith,"blacksmith",0,20000,smithWorker);business(warehouse,"warehouse",1,10000,warehouseWorker);
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var condition:Condition.values())bands.put(condition,100);
        var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L,Commodity.STONE,20L,Commodity.TIMBER,20L),Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.STONE,BigDecimal.ONE,Commodity.TIMBER,BigDecimal.ONE),bands);
        economySettings=settings;
        var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,VillagePriorities.Policy.prototype(),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype().seed());
        economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
        new OreTrackingRepository(db.getConnection()).begin(1000);survey(List.of(new VillageObservation.Position(12,40,0),new VillageObservation.Position(13,40,0),new VillageObservation.Position(30,40,0)));
    }
    void business(UUID id,String kind,int x,long cash,UUID worker)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,40,0,1,"Explicit fixture site");var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,kind,owner,owner,kind,site.id(),1,0),site,requirements,requirements,0,"Fixture"));
        db.getIndustryRepository().openStore(id,1000,sizes,cash);db.getBusinessRepository().hire(UUID.randomUUID(),id,worker,0);
    }
    void survey(List<VillageObservation.Position> coal)throws Exception {
        capture++;
        new MineralOpportunities(db.getConnection()).observe(town,List.of(new MineralObservation(world,capture,true,Commodity.COAL,coal),new MineralObservation(world,capture,true,Commodity.IRON,List.of())));
    }
    void sql(String query)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    long scalar(String query)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getLong(1);}}
    long cash()throws Exception{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    long mines()throws Exception{return scalar("SELECT COUNT(*) FROM businesses WHERE industry='mining'");}
    UUID mine()throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT id FROM businesses WHERE industry='mining' ORDER BY id")){assertTrue(r.next());return UUID.fromString(r.getString(1));}}
    void unchanged()throws Exception{assertEquals(0,mines());assertEquals(70000,cash());assertEquals(0,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(0,scalar("SELECT COUNT(*) FROM mineral_deposits"));assertTrue(db.getConnection().getAutoCommit());}
    @AfterEach void close(){if(db!=null)db.close();}

    @Test void realDemandFundsQualifiedFounderButCreatesNoInventoryOrUnobservedMineral()throws Exception {
        assertTrue(review().startsWith("Founded one coal mine"));assertEquals(1,mines());assertEquals(67500,cash());
        var business=db.getBusinessRepository().findBusiness(mine()).orElseThrow();assertEquals(founder,business.ownerId());assertEquals(founder,db.getBusinessRepository().currentWorkers(mine()).getFirst().residentId());
        var store=db.getIndustryRepository().store(mine()).orElseThrow();assertEquals(2000,store.hundredths());assertTrue(store.units().values().stream().allMatch(n->n==0));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT id FROM mineral_deposits")) {
            assertTrue(r.next());var deposit=db.getIndustryRepository().deposit(UUID.fromString(r.getString(1))).orElseThrow();
            assertEquals(Map.of(Commodity.COAL,2L,Commodity.STONE,3L),deposit.bundle());assertTrue(deposit.remainingBatches()==150||deposit.remainingBatches()==300);assertFalse(r.next());
        }
        assertEquals(500,scalar("SELECT startup_hundredths FROM startup_grants"));assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));
        review();assertEquals(1,mines());assertEquals(67500,cash());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void failureAtFinalReceiptRollsBackAllAssetsClaimsMoneyAndJobsThenRetries()throws Exception {
        sql("CREATE TRIGGER reject_grant BEFORE INSERT ON startup_grants BEGIN SELECT RAISE(ABORT,'injected receipt failure'); END");
        assertThrows(SQLException.class,this::review);unchanged();assertEquals(2,scalar("SELECT COUNT(*) FROM business_sites"));assertEquals(2,scalar("SELECT COUNT(*) FROM business_employment"));
        sql("DROP TRIGGER reject_grant");assertTrue(review().startsWith("Founded one"));assertEquals(1,mines());
    }
    @Test void immediateMutationGapBeforeCommitRollsBackEvenCompletedStartupRows()throws Exception {
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var adapter=new AutomaticMineFounding(db.getConnection(),founding,industry(),BusinessOperationPolicy.prototype(),StartupGrantPolicy.prototype(),216,42,()->calls.incrementAndGet()==1);
        assertTrue(adapter.review(town,1,2000,Set.of(world)).contains("all assets rolled back"));unchanged();assertEquals(2,scalar("SELECT COUNT(*) FROM business_employment"));
    }
    @Test void absentWorldStaleFutureOrUntrustedCaptureCannotFoundMine()throws Exception {
        var adapter=automation();adapter.review(town,1,2000,Set.of());unchanged();adapter.review(town,1,62001,Set.of(world));unchanged();adapter.review(town,1,1999,Set.of(world));unchanged();
        healthy=false;assertTrue(review().contains("trust guard"));unchanged();healthy=true;new OreTrackingRepository(db.getConnection()).markGap(2000);review();unchanged();
    }
    @Test void noActualCustomerNoWarehouseFundsOrNoWarehouseStaffCannotFoundMine()throws Exception {
        sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+smith+"'");review();unchanged();
        sql("UPDATE industry_stores SET hundredths=20000 WHERE business_id='"+smith+"'");sql("UPDATE industry_stores SET hundredths=99 WHERE business_id='"+warehouse+"'");review();unchanged();
        sql("UPDATE industry_stores SET hundredths=10000 WHERE business_id='"+warehouse+"'");db.getBusinessRepository().endEmployment(db.getBusinessRepository().currentWorkers(warehouse).getFirst().id(),1,"Fixture");review();unchanged();
    }
    @Test void insufficientSkillChildOrExistingJobCannotBecomeFreeFounder()throws Exception {
        db.getPopulationRepository().recordSkillExperience(founder,"mining",9);db.getPopulationRepository().recordSkillExperience(other,"mining",9);review();unchanged();
        db.getPopulationRepository().recordSkillExperience(founder,"mining",10);db.getPopulationRepository().updateResident(new Resident(founder,"Fixture resident",town,null,100,Resident.Status.ALIVE));review();unchanged();
        db.getPopulationRepository().updateResident(new Resident(founder,"Fixture resident",town,null,300,Resident.Status.ALIVE));
        db.getBusinessRepository().endEmployment(db.getBusinessRepository().currentWorkers(smith).getFirst().id(),1,"Fixture");db.getBusinessRepository().hire(UUID.randomUUID(),smith,founder,1);review();unchanged();
    }
    @Test void outsideRangeAndOccupiedAnchorsNeverGrantExcavationRights()throws Exception {
        survey(List.of(new VillageObservation.Position(97,40,0),new VillageObservation.Position(12,89,0)));review();unchanged();
        survey(List.of(new VillageObservation.Position(0,40,0)));review();unchanged();
    }
    @Test void unmetFoodNeedsProtectTreasuryBeforeAnySiteOrResourceClaim()throws Exception {
        sql("UPDATE needs_periods SET fulfilled=0");assertTrue(review().startsWith("Mine founding waits:"));unchanged();
    }
    @Test void actualRemainingCapacityPreventsDuplicatesButBiggerFundedNeedCanSupportSecondMine()throws Exception {
        review();assertEquals(1,mines());review();assertEquals(1,mines());
        batches=32;review();assertEquals(2,mines());assertEquals(65000,cash());review();assertEquals(2,mines());assertEquals(2,scalar("SELECT COUNT(*) FROM startup_grants"));
    }
    @Test void restartRetainsClaimsAndDoesNotRefillOrDebitAgain()throws Exception {
        review();long balance=cash(),remaining=scalar("SELECT SUM(remaining_batches) FROM mineral_deposits");
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticMineTest"));db.initialize();review();assertEquals(1,mines());assertEquals(balance,cash());assertEquals(remaining,scalar("SELECT SUM(remaining_batches) FROM mineral_deposits"));
    }
    @Test void disabledSettingsLeaveLegacyVillagesUnchanged()throws Exception {
        founding=MineFoundingPolicy.disabled();assertEquals("Automatic mine founding disabled",review());unchanged();
    }
    @Test void actualSqlOwnerFoundingThenTimedWorkPauseAndRestartNeverCreateBackfillOrDuplicateAssets()throws Exception {
        new OreTrackingRepository(db.getConnection()).finish(2000,true);db.close();db=null;
        var clock=new java.util.concurrent.atomic.AtomicLong(2000);
        try(var session=ownerSession(clock)) {
            session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);
            clock.set(3000);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);
            session.updateLoadedWorlds(Set.of()).get(5,java.util.concurrent.TimeUnit.SECONDS);clock.set(100000);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);
        }
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticMineTest"));db.initialize();assertEquals(1,mines());assertEquals(67500,cash());
        long remaining=scalar("SELECT SUM(remaining_batches) FROM mineral_deposits");assertTrue(remaining<scalar("SELECT SUM(initial_batches) FROM mineral_deposits"));
        long produced=scalar("SELECT SUM(units) FROM industry_stock WHERE commodity='coal'");assertTrue(produced>0);db.close();db=null;
        try(var session=ownerSession(clock)) {session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);}
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticMineTest"));db.initialize();assertEquals(1,mines());assertEquals(remaining,scalar("SELECT SUM(remaining_batches) FROM mineral_deposits"));assertEquals(produced,scalar("SELECT SUM(units) FROM industry_stock WHERE commodity='coal'"));
    }
    com.blueninjas24.civilizations.application.PopulationSession ownerSession(java.util.concurrent.atomic.AtomicLong clock) {
        var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),Logger.getLogger("AutomaticMineTest"),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype(),clock::get,economySettings,FarmObservation.Policy.prototype());
        session.configureOreTracking();session.configureIndustryRuntime(industry());session.configureMineFounding(founding,StartupGrantPolicy.prototype());return session;
    }
    @Test void protectedBudgetIsExplainedBeforeMissingFounderWithoutSpendingOrClaimingOre()throws Exception {
        sql("UPDATE resident_skills SET experience=0 WHERE skill='mining'");
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,-67500,"Explicit fixture prior spending");
        var message=review();assertTrue(message.contains("protected food"),message);assertFalse(message.contains("no qualified"),message);
        assertEquals(2500,cash());assertEquals(0,mines());assertEquals(0,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(0,scalar("SELECT COUNT(*) FROM mineral_deposits"));
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,67500,"Explicit fixture refund");
        assertTrue(review().contains("no qualified"));unchanged();
    }

    EconomyRuntimeRepository founderStudy(AutomaticMineFounding mines,long target)throws Exception {
        var runtime=new EconomyRuntimeRepository(db.getConnection(),economySettings,FarmObservation.Policy.prototype(),216,VillagePriorities.Policy.prototype(),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype().seed());
        var study=new StudyPolicy(true,1,100,2,8,1,2,Map.of("mining",new StudyPolicy.Course(target,Map.of(Commodity.STONE,1L,Commodity.TIMBER,1L))));
        runtime.configureStudy(study);runtime.configureMineFounderStudy(mines,CareerStudyPolicy.prototype(),founding);runtime.startOnline(2000,Set.of(world));return runtime;
    }
    void unqualified()throws Exception{sql("DELETE FROM resident_skills WHERE skill='mining'");}
    long xp(UUID person)throws Exception{return db.getPopulationRepository().findWorkProfile(person).orElseThrow().experience().getOrDefault("mining",0L);}
    long villageStock(String commodity)throws Exception{return scalar("SELECT units FROM economy_stocks WHERE commodity='"+commodity+"'");}
    @Test void realOpportunityTrainsOneFounderOverFuturePeriodsThenOpensMineWithRealFunds()throws Exception {
        unqualified();var engine=automation();var runtime=founderStudy(engine,10);
        String initial=engine.review(town,1,2000,Set.of(world));assertTrue(initial.contains("prospective mining owner"),initial);
        assertEquals(0,mines());assertEquals(70000,cash());assertEquals(0,xp(founder));assertEquals(1,scalar("SELECT COUNT(*) FROM mine_founder_courses"));
        engine.review(town,1,2000,Set.of(world));assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses"));
        for(int n=1;n<=10;n++){long now=2000+n*1000;runtime.pulse(now,false);assertEquals(n,xp(founder));engine.review(town,n,now,Set.of(world));assertEquals(n==10?1:0,mines());}
        assertEquals(0,xp(other));assertEquals(90,villageStock("stone"));assertEquals(90,villageStock("timber"));assertEquals(20,scalar("SELECT COUNT(*) FROM study_spending"));
        assertEquals(67500,cash());assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(1,scalar("SELECT COUNT(*) FROM mine_founder_courses"));
        assertEquals(founder,db.getBusinessRepository().findBusiness(mine()).orElseThrow().ownerId());
    }
    @Test void vanishedOreCancelsLinkedFounderBeforeAnyPracticeGoodsOrSkillsAreSpent()throws Exception {
        unqualified();var engine=automation();var runtime=founderStudy(engine,10);engine.review(town,1,2000,Set.of(world));survey(List.of());
        runtime.pulse(3000,false);assertEquals(0,xp(founder));assertEquals(100,villageStock("stone"));assertEquals(0,scalar("SELECT COUNT(*) FROM study_spending"));
        assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='CANCELLED'"));unchanged();
    }
    @Test void lostRealCustomersOrProtectedFundingCancelFounderStudyWithoutFreeProgress()throws Exception {
        unqualified();var engine=automation();var runtime=founderStudy(engine,10);engine.review(town,1,2000,Set.of(world));
        sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+smith+"'");runtime.pulse(3000,false);assertEquals(0,xp(founder));assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='CANCELLED'"));
        sql("UPDATE industry_stores SET hundredths=20000 WHERE business_id='"+smith+"'");engine.review(town,1,3000,Set.of(world));assertEquals(2,scalar("SELECT COUNT(*) FROM resident_courses"));
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,-67500,"Explicit fixture spending");runtime.pulse(4000,false);
        assertEquals(0,xp(founder));assertEquals(0,scalar("SELECT COUNT(*) FROM study_spending"));assertEquals(2,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='CANCELLED'"));assertEquals(0,mines());assertEquals(2500,cash());
    }
    @Test void futureOnlyEnrollmentCannotLearnFromAlreadyEarnedFoodBacklog()throws Exception {
        unqualified();var engine=automation();var runtime=founderStudy(engine,10);
        engine.review(town,1,7000,Set.of(world));assertEquals(6,scalar("SELECT eligible_after FROM resident_courses"));
        for(int n=0;n<5;n++)runtime.pulse(7000,false);assertEquals(6,scalar("SELECT completed_periods FROM economy_accounts"));assertEquals(0,xp(founder));assertEquals(0,scalar("SELECT COUNT(*) FROM study_spending"));
        runtime.pulse(8000,false);assertEquals(1,xp(founder));assertEquals(99,villageStock("stone"));
    }
    @Test void founderEnrollmentAndLearningFailuresRollBackThenRetryExactlyOnce()throws Exception {
        unqualified();var engine=automation();var runtime=founderStudy(engine,10);
        sql("CREATE TRIGGER reject_founder BEFORE INSERT ON mine_founder_reviews BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->engine.review(town,1,2000,Set.of(world)));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));assertEquals(0,scalar("SELECT COUNT(*) FROM mine_founder_courses"));unchanged();
        sql("DROP TRIGGER reject_founder");engine.review(town,1,2000,Set.of(world));
        sql("CREATE TRIGGER reject_learning BEFORE INSERT ON study_spending BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(3000,false));assertEquals(0,xp(founder));assertEquals(100,villageStock("stone"));assertEquals(1,scalar("SELECT completed_periods FROM economy_accounts"));
        sql("DROP TRIGGER reject_learning");runtime.pulse(3000,false);runtime.pulse(3000,false);assertEquals(1,xp(founder));assertEquals(2,scalar("SELECT COUNT(*) FROM study_spending"));
    }
    @Test void absenceAndRestartRetainFounderCourseWithoutBackfillOrDuplicateSponsorship()throws Exception {
        unqualified();var engine=automation();var runtime=founderStudy(engine,10);engine.review(town,1,2000,Set.of(world));runtime.pulse(3000,false);
        runtime.updateLoadedWorlds(3000,Set.of());runtime.pulse(100000,false);assertEquals(1,xp(founder));assertEquals(99,villageStock("stone"));
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticMineTest"));db.initialize();engine=automation();runtime=founderStudy(engine,10);
        runtime.updateLoadedWorlds(100000,Set.of(world));runtime.pulse(100000,false);assertEquals(1,xp(founder));assertEquals(1,scalar("SELECT COUNT(*) FROM mine_founder_courses"));
        survey(List.of(new VillageObservation.Position(12,40,0),new VillageObservation.Position(13,40,0)));capture=100000;new MineralOpportunities(db.getConnection()).observe(town,List.of(new MineralObservation(world,capture,true,Commodity.COAL,List.of(new VillageObservation.Position(12,40,0),new VillageObservation.Position(13,40,0))),new MineralObservation(world,capture,true,Commodity.IRON,List.of())));
        runtime.pulse(101000,false);assertEquals(2,xp(founder));assertEquals(98,villageStock("stone"));
    }
    @Test void tooLowStudyTargetOrMissingSuppliesNeverEnrollFakeFounders()throws Exception {
        unqualified();var engine=automation();founderStudy(engine,9);assertTrue(engine.review(town,1,2000,Set.of(world)).contains("cannot qualify"));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));
        engine=automation();founderStudy(engine,10);sql("UPDATE economy_stocks SET units=2 WHERE commodity='stone'");assertTrue(engine.review(town,1,2000,Set.of(world)).contains("practice supplies"));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));unchanged();
    }
    @Test void schema59EmptyUpgradeAndFailedDdlPreserveOldDataAndRecover()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),58);sql("CREATE VIEW mine_founder_courses AS SELECT id FROM residents");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertEquals(58,scalar("PRAGMA user_version"));assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE name='mine_founder_reviews'"));
        sql("DROP VIEW mine_founder_courses");new SchemaMigrator().migrate(db.getConnection());assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));assertEquals(0,scalar("SELECT COUNT(*) FROM mine_founder_courses"));unchanged();
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }

    @Test void actualSqlOwnerAutomaticallyTrainsThenFoundsWithoutInventedSkillsOrBackfill()throws Exception {
        unqualified();new OreTrackingRepository(db.getConnection()).finish(2000,true);db.close();db=null;
        var clock=new java.util.concurrent.atomic.AtomicLong(2000);
        try(var session=ownerSession(clock)) {
            session.configureStudy(StudyPolicy.prototype());session.configureCareerStudy(CareerStudyPolicy.prototype());
            session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);
            for(int n=1;n<=10;n++){clock.set(2000+n*1000);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);}
        }
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticMineTest"));db.initialize();
        assertEquals(1,mines());assertEquals(10,xp(founder));assertEquals(0,xp(other));assertEquals(1,scalar("SELECT COUNT(*) FROM mine_founder_courses"));
        assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='COMPLETED'"));assertEquals(90,villageStock("stone"));assertEquals(90,villageStock("timber"));assertEquals(67500,cash());
    }
    @Test void actualSqlOwnerWithLegacyDisabledCareerSettingsCannotGrantFreeFounderTraining()throws Exception {
        unqualified();new OreTrackingRepository(db.getConnection()).finish(2000,true);db.close();db=null;
        var clock=new java.util.concurrent.atomic.AtomicLong(2000);
        try(var session=ownerSession(clock)){session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);clock.set(3000);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);}
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticMineTest"));db.initialize();
        assertEquals(0,mines());assertEquals(0,xp(founder));assertEquals(0,scalar("SELECT COUNT(*) FROM mine_founder_courses"));unchanged();
    }

}
