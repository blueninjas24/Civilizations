package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CraftsmanFounderStudyTest {
    @TempDir Path directory;CraftsmanInvestmentTest f;
    @BeforeEach void setup()throws Exception {f=new CraftsmanInvestmentTest();f.directory=directory;f.setup();}
    @AfterEach void close(){f.close();}
    long scalar(String sql)throws Exception{try(var s=f.f.database.getConnection().createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    long xp()throws Exception{return f.f.database.getPopulationRepository().findWorkProfile(f.candidate).orElseThrow().experience().getOrDefault("crafting",0L);}
    EconomyRuntimeRepository runtime(long now,boolean enabled)throws Exception {
        var r=new EconomyRuntimeRepository(f.f.database.getConnection(),f.settings,FarmObservation.Policy.prototype(),216);
        r.configureStudy(StudyPolicy.prototype());r.configureCraftsmanFounderStudy(f.opportunities(),enabled?WorkshopFoundingPolicy.prototype():WorkshopFoundingPolicy.disabled(),CareerStudyPolicy.prototype());r.startOnline(now,Set.of(f.f.world));return r;
    }
    @Test void futureSupplyBackedStudyQualifiesRealFounderWithoutFreeExperience()throws Exception {
        var r=runtime(2000,true);long timber=f.units(Commodity.TIMBER);assertTrue(r.reviewCraftsmanFounderStudy(f.f.town,2000).contains("prospective craftsman owner"));r.reviewCraftsmanFounderStudy(f.f.town,2000);
        assertEquals(1,scalar("SELECT COUNT(*) FROM craftsman_founder_courses"));assertEquals(0,xp());
        for(int n=1;n<=10;n++){r.pulse(2000+n*1000,false);assertEquals(n,xp());}
        assertEquals(timber-10,f.units(Commodity.TIMBER));assertEquals(10,scalar("SELECT COUNT(*) FROM study_spending"));assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='COMPLETED'"));
        var d=f.engine().prepare(f.f.town,0,12000,Set.of(f.f.world)).draft().orElseThrow();f.engine().accept(d,new WorkplaceOperations.Confirmation(d.anchor(),true,12000),12000,Set.of(f.f.world));
        assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(10,xp());assertEquals(timber-13,f.units(Commodity.TIMBER));
        var production=new IndustryRuntime(f.f.database.getConnection(),f.runtime,BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());production.start(12000);production.pulse(13000,Set.of(f.f.world));assertEquals(11,xp());
        production.pulse(13000,Set.of(f.f.world));production.pulse(1000000,Set.of());assertEquals(11,xp(),"Only actual once-recorded production earns work learning");
    }
    @Test void absenceStaleMarkerDisabledFoundingAndMaterialReservePauseWithoutSpending()throws Exception {
        var r=runtime(2000,true);r.reviewCraftsmanFounderStudy(f.f.town,2000);r.pulse(3000,false);assertEquals(1,xp());
        r.updateLoadedWorlds(3000,Set.of());r.pulse(100000,false);assertEquals(1,xp());f.f.database.close();f.f.open();
        r=runtime(100000,true);r.pulse(101000,false);assertEquals(1,xp());f.markers(102000,true,WorkplaceObservation.Kind.CRAFTING_TABLE);r.pulse(102000,false);assertEquals(2,xp());
        f.f.sql("UPDATE economy_stocks SET units=2 WHERE commodity='timber'");r.pulse(103000,false);assertEquals(2,xp());
        f.f.sql("UPDATE economy_stocks SET units=30 WHERE commodity='timber'");r=runtime(103000,false);r.pulse(104000,false);assertEquals(2,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='ACTIVE'"));
    }
    @Test void lostCustomerOrRemovedMarkerPausesAnExistingLearner()throws Exception {
        var r=runtime(2000,true);r.reviewCraftsmanFounderStudy(f.f.town,2000);long timber=f.units(Commodity.TIMBER);
        f.f.sql("UPDATE industry_stock SET units=1 WHERE commodity='coal'");r.pulse(3000,false);assertEquals(0,xp());assertEquals(timber,f.units(Commodity.TIMBER));
        f.f.sql("UPDATE industry_stock SET units=80 WHERE commodity='coal'");f.markers(4000,true,WorkplaceObservation.Kind.SMOKER);r.pulse(4000,false);assertEquals(0,xp());
        f.markers(5000,true,WorkplaceObservation.Kind.CRAFTING_TABLE);r.pulse(5000,false);assertEquals(1,xp());
    }
    @Test void enrollmentAndPracticeReceiptFailureRollbackThenRetryExactlyOnce()throws Exception {
        var r=runtime(2000,true);long timber=f.units(Commodity.TIMBER);f.f.sql("CREATE TRIGGER refuse_craft_study BEFORE INSERT ON craftsman_founder_reviews BEGIN SELECT RAISE(ABORT,'fixture review failure'); END");
        assertThrows(java.sql.SQLException.class,()->r.reviewCraftsmanFounderStudy(f.f.town,2000));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));
        f.f.sql("DROP TRIGGER refuse_craft_study");r.reviewCraftsmanFounderStudy(f.f.town,2000);f.f.sql("CREATE TRIGGER refuse_craft_practice BEFORE INSERT ON study_spending BEGIN SELECT RAISE(ABORT,'fixture practice failure'); END");
        assertThrows(java.sql.SQLException.class,()->r.pulse(3000,false));assertEquals(0,xp());assertEquals(timber,f.units(Commodity.TIMBER));
        f.f.sql("DROP TRIGGER refuse_craft_practice");r.pulse(3000,false);r.pulse(3000,false);assertEquals(1,xp());assertEquals(timber-1,f.units(Commodity.TIMBER));
    }
    @Test void version80MigrationFailurePreservesEveryPredecessorRowAndRetries()throws Exception {
        SchemaFixtures.rewind(f.f.database.getConnection(),80);var before=f.snapshot();f.f.sql("CREATE VIEW craftsman_founder_courses AS SELECT 1");
        assertThrows(java.sql.SQLException.class,()->new SchemaMigrator().migrate(f.f.database.getConnection()));assertEquals(80,scalar("PRAGMA user_version"));assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='craftsman_founder_reviews'"));
        f.f.sql("DROP VIEW craftsman_founder_courses");assertEquals(before,f.snapshot());new SchemaMigrator().migrate(f.f.database.getConnection());
        var after=f.snapshot();assertEquals(List.of(),after.remove("livestock_birth_losses"));assertEquals(List.of(),after.remove("livestock_birth_projections"));assertEquals(List.of(),after.remove("livestock_birth_entities"));assertEquals(List.of(),after.remove("livestock_births"));assertEquals(List.of(),after.remove("livestock_product_sales"));assertEquals(List.of(),after.remove("livestock_capture"));assertEquals(List.of(),after.remove("livestock_animal_losses"));assertEquals(List.of(),after.remove("craftsman_founder_courses"));assertEquals(List.of(),after.remove("craftsman_founder_reviews"));assertEquals(before,after);
        try(var s=f.f.database.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void backlogCannotEnrollOrAwardRetrospectivePractice()throws Exception {
        var r=runtime(2000,true);assertTrue(r.reviewCraftsmanFounderStudy(f.f.town,7000).contains("current food clock"));assertEquals(0,scalar("SELECT COUNT(*) FROM craftsman_founder_courses"));
        for(int n=0;n<5;n++)r.pulse(7000,false);assertEquals(0,xp());assertTrue(r.reviewCraftsmanFounderStudy(f.f.town,7000).contains("prospective craftsman owner"));
        r.pulse(12000,false);assertEquals(0,xp(),"Existing learner cannot practice against a historical queued period");
        for(int n=0;n<5;n++)r.pulse(12000,false);assertTrue(xp()<=1,"Only current final period can earn practice");
    }
    @Test void takenFirstFirmCancelsCourseAndReleasesLearner()throws Exception {
        var r=runtime(2000,true);r.reviewCraftsmanFounderStudy(f.f.town,2000);
        var other=UUID.randomUUID();f.f.database.getPopulationRepository().insertResident(new com.blueninjas24.civilizations.population.Resident(other,"Other qualified founder",f.f.town,null,300,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));
        f.f.database.getPopulationRepository().recordSkillExperience(other,"crafting",10);r.pulse(3000,false);
        var d=f.engine().prepare(f.f.town,0,3000,Set.of(f.f.world)).draft().orElseThrow();f.engine().accept(d,new WorkplaceOperations.Confirmation(d.anchor(),true,3000),3000,Set.of(f.f.world));
        r.pulse(4000,false);assertEquals(0,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='CANCELLED'"));
    }
    @Test void sqlOwnerTrainsThenOpensUsingOnlyFutureActualSupplies()throws Exception {
        long timber=f.units(Commodity.TIMBER);f.f.database.close();var defaults=com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype();
        var settings=new com.blueninjas24.civilizations.application.PopulationSession.Settings(0,defaults.millisPerMonth(),defaults.maximumOfflineMonths(),defaults.monthsPerBatch(),defaults.population(),defaults.migration(),defaults.recognition(),defaults.enrollment());
        var now=new java.util.concurrent.atomic.AtomicLong(2000);
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),java.util.logging.Logger.getLogger("CraftsmanTrainingOwner"),settings,now::get,f.settings,FarmObservation.Policy.prototype())) {
            session.configureIndustryRuntime(f.runtime);session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureWorkplaces(WorkplaceObserverPolicy.prototype());
            session.configureCraftsman(WorkshopFoundingPolicy.prototype(),WorkshopOpportunityPolicy.prototype());session.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),f.starter);
            session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());session.configureStudy(StudyPolicy.prototype());session.configureCareerStudy(CareerStudyPolicy.prototype());
            session.start(false,Set.of(f.f.world)).get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(session.reviewAutomaticCraftsmanInvestment(f.f.town).get(5,java.util.concurrent.TimeUnit.SECONDS).explanation().contains("prospective craftsman owner"));
            for(int n=1;n<=10;n++){now.set(2000+n*1000);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);}
            var proposal=session.reviewAutomaticCraftsmanInvestment(f.f.town).get(5,java.util.concurrent.TimeUnit.SECONDS).draft().orElseThrow();
            session.acceptCraftsmanInvestment(proposal,new WorkplaceOperations.Confirmation(proposal.anchor(),true,12000)).get(5,java.util.concurrent.TimeUnit.SECONDS);
        } finally{f.f.open();}
        assertEquals(10,xp());assertEquals(timber-13,f.units(Commodity.TIMBER));assertEquals(10,scalar("SELECT COUNT(*) FROM study_spending"));assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));
    }

}
