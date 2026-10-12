package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WarehouseFounderStudyTest {
    @TempDir Path directory;WarehouseInvestmentTest investment;WorkshopInvestmentTest fixture;
    @BeforeEach void setup()throws Exception {
        investment=new WarehouseInvestmentTest();investment.directory=directory;investment.setup();fixture=investment.fixture;
        fixture.sql("UPDATE economy_stocks SET units=100 WHERE commodity='food'"); // Explicit well-fed fixture, never real server stock.
    }
    @AfterEach void close(){if(fixture!=null)fixture.close();}
    long scalar(String sql)throws SQLException{try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    long xp()throws SQLException{return fixture.db.getPopulationRepository().findWorkProfile(investment.merchant).orElseThrow().experience().getOrDefault("commerce",0L);}
    StudyPolicy study(long target){return new StudyPolicy(true,1,100,2,8,1,2,Map.of("commerce",new StudyPolicy.Course(target,Map.of(Commodity.COAL,1L))));}
    EconomyRuntimeRepository runtime(long now,long target)throws Exception{return runtime(now,target,WarehouseFoundingPolicy.prototype());}
    EconomyRuntimeRepository runtime(long now,long target,WarehouseFoundingPolicy founding)throws Exception {
        var runtime=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.settings,FarmObservation.Policy.prototype(),216);
        runtime.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());runtime.configureFarmTools(fixture.upkeep,FarmToolBudgetPolicy.prototype(),IndustryPolicy.prototype());runtime.configureWorkLearning(WorkLearningPolicy.prototype());
        runtime.configureStudy(study(target));runtime.configureWarehouseFounderStudy(investment.chain.engine(WarehouseOpportunityPolicy.prototype()),founding,CareerStudyPolicy.prototype());
        // Both checks are present, proving workshop-specific pause logic does not block warehouse courses.
        runtime.configureWorkshopFounderStudy(fixture.review(WorkshopOpportunityPolicy.prototype(),StartupGrantPolicy.prototype(),fixture.starter),WorkshopFoundingPolicy.prototype(),CareerStudyPolicy.prototype());
        runtime.startOnline(now,Set.of(fixture.world));return runtime;
    }
    @Test void realFutureCommercePracticeEndsInPaidWarehouseThroughOrdinaryCycle()throws Exception {
        var runtime=runtime(4000,10);assertTrue(runtime.reviewWarehouseFounderStudy(fixture.town,4000).contains("prospective warehouse owner"));runtime.reviewWarehouseFounderStudy(fixture.town,4000);
        assertEquals(1,scalar("SELECT COUNT(*) FROM warehouse_founder_courses"));assertEquals(0,xp());assertTrue(investment.engine().prepare(fixture.town,0,4000,Set.of(fixture.world)).draft().isEmpty());
        for(int n=1;n<=10;n++){long now=4000+n*1000;runtime.pulse(now,false);assertEquals(n,xp());}
        assertEquals(10,scalar("SELECT COUNT(*) FROM study_spending"));assertEquals(9,scalar("SELECT units FROM economy_stocks WHERE commodity='coal'"));
        var accepted=new int[1];var cycle=new WarehouseStartupCycle(t->{try{return CompletableFuture.completedFuture(investment.engine().prepare(t,0,14000,Set.of(fixture.world)));}catch(SQLException e){return CompletableFuture.failedFuture(e);}},
            d->Optional.of(new WorkplaceOperations.Confirmation(d.anchor(),true,14000)),(d,e)->{try{accepted[0]++;return CompletableFuture.completedFuture(investment.engine().accept(d,e,14000,Set.of(fixture.world)));}catch(SQLException ex){return CompletableFuture.failedFuture(ex);}});
        for(int n=0;n<12;n++)cycle.tick(List.of(fixture.town));assertEquals(1,accepted[0]);assertEquals(20000,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());
        assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='COMPLETED'"));assertEquals(2,scalar("SELECT COUNT(*) FROM startup_grants"));
    }
    @Test void unknownMarkerMissingOreAndUnaffordableChainPauseAndResumeWithoutFreePractice()throws Exception {
        var runtime=runtime(4000,10);runtime.reviewWarehouseFounderStudy(fixture.town,4000);
        fixture.markers(5000,false,List.of());runtime.pulse(5000,false);assertEquals(0,xp());
        investment.chain.markers(6000,List.of(new WorkplaceObservation.Marker(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(8,64,0),WorkplaceObservation.Kind.BARREL)));
        investment.chain.healthy=false;runtime.pulse(6000,false);assertEquals(0,xp());investment.chain.healthy=true;
        fixture.sql("UPDATE treasuries SET hundredths=10000");runtime.pulse(7000,false);assertEquals(0,xp());assertEquals(0,scalar("SELECT COUNT(*) FROM study_spending"));
        fixture.sql("UPDATE treasuries SET hundredths=24500");runtime.pulse(8000,false);assertEquals(1,xp());assertEquals(18,scalar("SELECT units FROM economy_stocks WHERE commodity='coal'"));
    }
    @Test void newEnrollmentCannotLearnFromEarnedBacklog()throws Exception {
        var runtime=runtime(4000,10);runtime.reviewWarehouseFounderStudy(fixture.town,9000);assertEquals(8,scalar("SELECT eligible_after FROM resident_courses"));
        for(int n=0;n<5;n++)runtime.pulse(9000,false);assertEquals(0,xp());assertEquals(8,scalar("SELECT completed_periods FROM economy_accounts"));runtime.pulse(10000,false);assertEquals(1,xp());
    }
    @Test void enrollmentAndPracticeRollbackThenRetryOnce()throws Exception {
        var runtime=runtime(4000,10);fixture.sql("CREATE TRIGGER reject_merchant BEFORE INSERT ON warehouse_founder_reviews BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->runtime.reviewWarehouseFounderStudy(fixture.town,4000));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));assertEquals(0,scalar("SELECT COUNT(*) FROM warehouse_founder_courses"));
        fixture.sql("DROP TRIGGER reject_merchant");runtime.reviewWarehouseFounderStudy(fixture.town,4000);fixture.sql("CREATE TRIGGER reject_merchant_practice BEFORE INSERT ON study_spending BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(5000,false));assertEquals(0,xp());assertEquals(19,scalar("SELECT units FROM economy_stocks WHERE commodity='coal'"));
        fixture.sql("DROP TRIGGER reject_merchant_practice");runtime.pulse(5000,false);runtime.pulse(5000,false);assertEquals(1,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM study_spending"));
    }
    @Test void worldAbsenceAndRestartDoNotCatchUpOrForgetProspect()throws Exception {
        var runtime=runtime(4000,10);runtime.reviewWarehouseFounderStudy(fixture.town,4000);runtime.pulse(5000,false);runtime.updateLoadedWorlds(5000,Set.of());runtime.pulse(100000,false);assertEquals(1,xp());
        fixture.close();fixture.open();runtime=runtime(100000,10);runtime.pulse(101000,false);assertEquals(1,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='ACTIVE'"));
        investment.chain.markers(102000,List.of(new WorkplaceObservation.Marker(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(8,64,0),WorkplaceObservation.Kind.BARREL)));
        investment.chain.ore(102000,List.of(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(20,64,0)),List.of(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(25,64,0)));
        runtime.pulse(102000,false);assertEquals(2,xp());
    }
    @Test void disabledFoundingOrInsufficientTargetAndRealSuppliesDoNotAwardSkills()throws Exception {
        var runtime=runtime(4000,9);assertTrue(runtime.reviewWarehouseFounderStudy(fixture.town,4000).contains("cannot qualify"));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));
        runtime=runtime(4000,10);fixture.sql("UPDATE economy_stocks SET units=2 WHERE commodity='coal'");assertTrue(runtime.reviewWarehouseFounderStudy(fixture.town,4000).contains("practice supplies"));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));
        fixture.sql("UPDATE economy_stocks SET units=19 WHERE commodity='coal'");runtime.pulse(5000,false);runtime.reviewWarehouseFounderStudy(fixture.town,5000);runtime=runtime(5000,10,WarehouseFoundingPolicy.disabled());runtime.pulse(6000,false);
        assertEquals(0,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='ACTIVE'"));
    }
    @Test void anotherQualifiedMerchantTakingOpportunityReleasesUnfinishedLearner()throws Exception {
        var runtime=runtime(4000,10);runtime.reviewWarehouseFounderStudy(fixture.town,4000);var other=new UUID(0,4);
        fixture.db.getPopulationRepository().insertResident(new com.blueninjas24.civilizations.population.Resident(other,"Other explicit merchant",fixture.town,null,300,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));fixture.db.getPopulationRepository().recordSkillExperience(other,"commerce",10);
        runtime.pulse(5000,false);var d=investment.engine().prepare(fixture.town,0,5000,Set.of(fixture.world)).draft().orElseThrow();investment.engine().accept(d,new WorkplaceOperations.Confirmation(d.anchor(),true,5000),5000,Set.of(fixture.world));runtime.pulse(6000,false);
        assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='CANCELLED'"));assertEquals(0,xp());
    }
    @Test void schema63FailedUpgradeRollsBackAllNewTablesAndRetriesPreservingEveryOldRow()throws Exception {
        var before=fixture.snapshot();SchemaFixtures.rewind(fixture.db.getConnection(),62);fixture.sql("CREATE VIEW warehouse_founder_courses AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(fixture.db.getConnection()));assertEquals(62,scalar("PRAGMA user_version"));assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='warehouse_founder_reviews'"));
        fixture.sql("DROP VIEW warehouse_founder_courses");new SchemaMigrator().migrate(fixture.db.getConnection());assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));assertEquals(before,fixture.snapshot());
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void actualSqlOwnerSponsorsMerchantThenPreparesAndAcceptsEarnedOwner()throws Exception {
        fixture.close();var time=new java.util.concurrent.atomic.AtomicLong(4000);var original=PopulationSession.Settings.prototype();var settings=new PopulationSession.Settings(0,original.millisPerMonth(),original.maximumOfflineMonths(),original.monthsPerBatch(),original.population(),original.migration(),original.recognition(),original.enrollment());
        try(var session=new PopulationSession(directory.toFile(),java.util.logging.Logger.getLogger("WarehouseFounderTest"),settings,time::get,fixture.settings,FarmObservation.Policy.prototype())) {
            session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureFarmTools(fixture.upkeep,FarmToolBudgetPolicy.prototype());session.configureWorkLearning(WorkLearningPolicy.prototype());session.configureBusinessOperations(BusinessOperationPolicy.prototype());
            session.configureIndustryRuntime(investment.chain.industry);session.configureWorkplaces(WorkplaceObserverPolicy.prototype());session.configureMineFounding(MineFoundingPolicy.prototype(),StartupGrantPolicy.prototype());session.configureWarehouseOpportunities(WarehouseOpportunityPolicy.prototype());session.configureWarehouseFounding(WarehouseFoundingPolicy.prototype());session.configureStudy(study(10));session.configureCareerStudy(CareerStudyPolicy.prototype());
            session.start(false,Set.of(fixture.world)).get(5,TimeUnit.SECONDS);var initial=session.reviewAutomaticWarehouseInvestment(fixture.town).get(5,TimeUnit.SECONDS);assertTrue(initial.draft().isEmpty());assertTrue(initial.explanation().contains("prospective warehouse owner"));
            for(int n=1;n<=10;n++){time.set(4000+n*1000);session.pulse().get(5,TimeUnit.SECONDS);}
            var result=session.reviewAutomaticWarehouseInvestment(fixture.town).get(5,TimeUnit.SECONDS);assertTrue(result.draft().isPresent(),result.explanation());var d=result.draft().orElseThrow();session.acceptWarehouseInvestment(d,new WorkplaceOperations.Confirmation(d.anchor(),true,14000)).get(5,TimeUnit.SECONDS);
        }
        fixture.open();assertEquals(10,xp());assertEquals(2,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(20000,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());
    }
    @Test void cycleWaitsForActualLoadedBarrelAndDoesNotRepeatPaidStartup()throws Exception {
        investment.qualify();var present=new int[1];var accepted=new int[1];var pending=new CompletableFuture<Void>();
        var cycle=new WarehouseStartupCycle(t->{try{return CompletableFuture.completedFuture(investment.engine().prepare(t,0,4000,Set.of(fixture.world)));}catch(SQLException e){return CompletableFuture.failedFuture(e);}},
            d->present[0]==0?Optional.empty():Optional.of(new WorkplaceOperations.Confirmation(d.anchor(),present[0]>1,4000)),(d,e)->{try{accepted[0]++;investment.engine().accept(d,e,4000,Set.of(fixture.world));return pending;}catch(SQLException ex){return CompletableFuture.failedFuture(ex);}});
        var before=fixture.snapshot();for(int n=0;n<6;n++)cycle.tick(List.of(fixture.town));assertEquals(before,fixture.snapshot());present[0]=1;for(int n=0;n<6;n++)cycle.tick(List.of(fixture.town));assertEquals(before,fixture.snapshot());
        present[0]=2;for(int n=0;n<20;n++)cycle.tick(List.of(fixture.town));assertEquals(1,accepted[0]);pending.complete(null);for(int n=0;n<12;n++)cycle.tick(List.of(fixture.town));assertEquals(1,accepted[0]);
    }
}
