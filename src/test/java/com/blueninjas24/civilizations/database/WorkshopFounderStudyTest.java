package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorkshopFounderStudyTest {
    @TempDir Path directory;
    WorkshopInvestmentTest fixture;
    StudyPolicy study(long target){return new StudyPolicy(true,1,100,2,8,1,2,Map.of("smithing",new StudyPolicy.Course(target,Map.of(Commodity.COAL,1L))));}
    @BeforeEach void setup()throws Exception {fixture=new WorkshopInvestmentTest();fixture.directory=directory;fixture.setup();fixture.sql("UPDATE economy_stocks SET units=100 WHERE commodity='food'");} // Explicit well-fed training fixture, not application funding.
    @AfterEach void close(){fixture.close();}
    long scalar(String sql)throws SQLException{try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    long xp()throws SQLException{return fixture.db.getPopulationRepository().findWorkProfile(fixture.prospect).orElseThrow().experience().getOrDefault("smithing",0L);}
    EconomyRuntimeRepository runtime(long now,long target)throws Exception {
        return runtime(now,target,WorkshopFoundingPolicy.prototype());
    }
    EconomyRuntimeRepository runtime(long now,long target,WorkshopFoundingPolicy founding)throws Exception {
        var runtime=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.settings,FarmObservation.Policy.prototype(),216);
        runtime.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());
        runtime.configureFarmTools(fixture.upkeep,FarmToolBudgetPolicy.prototype(),IndustryPolicy.prototype());runtime.configureWorkLearning(WorkLearningPolicy.prototype());
        runtime.configureStudy(study(target));runtime.configureWorkshopFounderStudy(fixture.review(WorkshopOpportunityPolicy.prototype(),StartupGrantPolicy.prototype(),fixture.starter),founding,CareerStudyPolicy.prototype());
        runtime.startOnline(now,Set.of(fixture.world));return runtime;
    }
    @Test void realFuturePracticeQualifiesOwnerThenPaidWorkshopStarts()throws Exception {
        var runtime=runtime(3000,10);assertTrue(runtime.reviewWorkshopFounderStudy(fixture.town,3000).contains("prospective workshop owner"));
        runtime.reviewWorkshopFounderStudy(fixture.town,3000);assertEquals(1,scalar("SELECT COUNT(*) FROM workshop_founder_courses"));assertEquals(0,xp());assertTrue(fixture.engine().prepare(fixture.town,0,3000,Set.of(fixture.world)).draft().isEmpty());
        for(int n=1;n<=10;n++){long now=3000+n*1000;runtime.pulse(now,false);assertEquals(n,xp());runtime.reviewWorkshopFounderStudy(fixture.town,now);}
        assertEquals(10,scalar("SELECT COUNT(*) FROM study_spending"));assertEquals(10,scalar("SELECT units FROM economy_stocks WHERE commodity='coal'"));
        var proposal=fixture.engine().prepare(fixture.town,0,13000,Set.of(fixture.world)).draft().orElseThrow();
        fixture.engine().accept(proposal,new WorkplaceOperations.Confirmation(proposal.anchor(),true,13000),13000,Set.of(fixture.world));
        assertEquals(28500,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));
        assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='COMPLETED'"));assertEquals(9,scalar("SELECT units FROM economy_stocks WHERE commodity='coal'"));
    }
    @Test void missingUnknownOrUnfundedOpportunityPausesWithoutSpendingThenResumes()throws Exception {
        var runtime=runtime(3000,10);runtime.reviewWorkshopFounderStudy(fixture.town,3000);
        fixture.markers(4000,false,List.of());runtime.pulse(4000,false);assertEquals(0,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='ACTIVE'"));
        fixture.markers(5000,true,List.of());runtime.pulse(5000,false);assertEquals(0,xp());
        fixture.markers(6000,true,List.of(new WorkplaceObservation.Marker(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(10,64,0),WorkplaceObservation.Kind.SMITHING_TABLE)));
        fixture.sql("UPDATE treasuries SET hundredths=2500");runtime.pulse(6000,false);assertEquals(0,xp());assertEquals(0,scalar("SELECT COUNT(*) FROM study_spending"));
        fixture.sql("UPDATE treasuries SET hundredths=30000");runtime.pulse(7000,false);assertEquals(1,xp());assertEquals(19,scalar("SELECT units FROM economy_stocks WHERE commodity='coal'"));
    }
    @Test void enrollmentNeverAwardsFromAlreadyEarnedFoodBacklog()throws Exception {
        var runtime=runtime(3000,10);runtime.reviewWorkshopFounderStudy(fixture.town,8000);assertEquals(7,scalar("SELECT eligible_after FROM resident_courses"));
        for(int n=0;n<5;n++)runtime.pulse(8000,false);assertEquals(0,xp());assertEquals(7,scalar("SELECT completed_periods FROM economy_accounts"));
        runtime.pulse(9000,false);assertEquals(1,xp());
    }
    @Test void enrollmentAndLearningRollbackRetryWithoutDuplicateSkillOrSupplySpend()throws Exception {
        var runtime=runtime(3000,10);fixture.sql("CREATE TRIGGER reject_workshop_study BEFORE INSERT ON workshop_founder_reviews BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->runtime.reviewWorkshopFounderStudy(fixture.town,3000));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));assertEquals(0,scalar("SELECT COUNT(*) FROM workshop_founder_courses"));
        fixture.sql("DROP TRIGGER reject_workshop_study");runtime.reviewWorkshopFounderStudy(fixture.town,3000);
        fixture.sql("CREATE TRIGGER reject_practice BEFORE INSERT ON study_spending BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(4000,false));assertEquals(0,xp());assertEquals(20,scalar("SELECT units FROM economy_stocks WHERE commodity='coal'"));
        fixture.sql("DROP TRIGGER reject_practice");runtime.pulse(4000,false);runtime.pulse(4000,false);assertEquals(1,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM study_spending"));
    }
    @Test void absenceRestartAndDisabledStudyRetainCourseWithoutCatchup()throws Exception {
        var runtime=runtime(3000,10);runtime.reviewWorkshopFounderStudy(fixture.town,3000);runtime.pulse(4000,false);
        runtime.updateLoadedWorlds(4000,Set.of());runtime.pulse(100000,false);assertEquals(1,xp());
        fixture.db.close();fixture.open();runtime=runtime(100000,10);runtime.pulse(100000,false);assertEquals(1,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM workshop_founder_courses"));
        // Original marker has become stale; do not cancel or spend from unknown evidence.
        runtime.pulse(101000,false);assertEquals(1,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='ACTIVE'"));
        fixture.markers(102000,true,List.of(new WorkplaceObservation.Marker(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(10,64,0),WorkplaceObservation.Kind.SMITHING_TABLE)));
        runtime.pulse(102000,false);assertEquals(2,xp());
    }
    @Test void impossibleTrainingTargetAndProtectedSuppliesNeverEnroll()throws Exception {
        var runtime=runtime(3000,9);assertTrue(runtime.reviewWorkshopFounderStudy(fixture.town,3000).contains("cannot qualify"));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));
        runtime=runtime(3000,10);fixture.sql("UPDATE economy_stocks SET units=2 WHERE commodity='coal'");
        assertTrue(runtime.reviewWorkshopFounderStudy(fixture.town,3000).contains("starter bundle unavailable"));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));
    }
    @Test void takenOpportunityReleasesLearnerRatherThanKeepingPermanentJobLock()throws Exception {
        var runtime=runtime(3000,10);runtime.reviewWorkshopFounderStudy(fixture.town,3000);
        // Another explicitly qualified fixture resident accepts the opportunity first.
        var other=new UUID(0,3);fixture.db.getPopulationRepository().insertResident(new com.blueninjas24.civilizations.population.Resident(other,"Other fixture",fixture.town,null,300,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));
        fixture.db.getPopulationRepository().recordSkillExperience(other,"smithing",10);runtime.pulse(4000,false);
        var draft=fixture.engine().prepare(fixture.town,0,4000,Set.of(fixture.world)).draft().orElseThrow();fixture.engine().accept(draft,new WorkplaceOperations.Confirmation(draft.anchor(),true,4000),4000,Set.of(fixture.world));
        runtime.pulse(5000,false);assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='CANCELLED'"));assertEquals(0,xp());
    }
    @Test void schema62FailureRollsBackAndPreservesAllExistingRowsThenRetries()throws Exception {
        var before=fixture.snapshot();SchemaFixtures.rewind(fixture.db.getConnection(),61);fixture.sql("CREATE VIEW workshop_founder_courses AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(fixture.db.getConnection()));assertEquals(61,scalar("PRAGMA user_version"));assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='workshop_founder_reviews'"));
        fixture.sql("DROP VIEW workshop_founder_courses");new SchemaMigrator().migrate(fixture.db.getConnection());assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));assertEquals(before,fixture.snapshot());
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void disabledFoundingRetainsExistingCourseButDoesNotAwardOrEnroll()throws Exception {
        var runtime=runtime(3000,10);runtime.reviewWorkshopFounderStudy(fixture.town,3000);runtime.pulse(4000,false);assertEquals(1,xp());
        runtime=runtime(4000,10,WorkshopFoundingPolicy.disabled());assertTrue(runtime.reviewWorkshopFounderStudy(fixture.town,4000).contains("disabled"));runtime.pulse(5000,false);
        assertEquals(1,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='ACTIVE'"));assertEquals(1,scalar("SELECT COUNT(*) FROM study_spending"));
    }
    @Test void automaticSqlOwnerReviewSponsorsStudyThenReturnsEarnedFounderForAcceptance()throws Exception {
        fixture.db.close();var time=new java.util.concurrent.atomic.AtomicLong(3000);
        var original=com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype();
        var settings=new com.blueninjas24.civilizations.application.PopulationSession.Settings(0,original.millisPerMonth(),original.maximumOfflineMonths(),original.monthsPerBatch(),original.population(),original.migration(),original.recognition(),original.enrollment());
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),java.util.logging.Logger.getLogger("WorkshopFounderTest"),settings,time::get,fixture.settings,FarmObservation.Policy.prototype())) {
            session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureFarmTools(fixture.upkeep,FarmToolBudgetPolicy.prototype());session.configureWorkLearning(WorkLearningPolicy.prototype());
            session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureIndustryRuntime(fixture.industry);session.configureWorkplaces(WorkplaceObserverPolicy.prototype());
            session.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),fixture.starter);session.configureWorkshopFounding(WorkshopFoundingPolicy.prototype());session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());
            session.configureStudy(study(10));session.configureCareerStudy(CareerStudyPolicy.prototype());session.start(false,Set.of(fixture.world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            var initial=session.reviewAutomaticWorkshopInvestment(fixture.town).get(5,java.util.concurrent.TimeUnit.SECONDS);assertTrue(initial.draft().isEmpty());assertTrue(initial.explanation().contains("prospective workshop owner"));
            for(int n=1;n<=10;n++){time.set(3000+n*1000);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);}
            var review=session.reviewAutomaticWorkshopInvestment(fixture.town).get(5,java.util.concurrent.TimeUnit.SECONDS);assertTrue(review.draft().isPresent(),review.explanation());var proposal=review.draft().orElseThrow();
            session.acceptWorkshopInvestment(proposal,new WorkplaceOperations.Confirmation(proposal.anchor(),true,13000)).get(5,java.util.concurrent.TimeUnit.SECONDS);
        }
        fixture.open();assertEquals(10,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(28500,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());
    }
}
