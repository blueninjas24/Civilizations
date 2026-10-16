package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.economy.FarmObservation;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BakeryFounderStudyTest {
    @TempDir Path directory;BakeryInvestmentTest f;
    @BeforeEach void setup()throws Exception {
        f=new BakeryInvestmentTest();f.directory=directory;f.setup();f.opportunity();
        f.sql("UPDATE economy_stocks SET units=100 WHERE commodity='food'");f.sql("UPDATE economy_stocks SET units=30 WHERE commodity='timber'");
    }
    @AfterEach void close(){f.close();}
    long scalar(String sql)throws Exception{try(var s=f.db.getConnection().createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    long xp()throws Exception{return f.db.getPopulationRepository().findWorkProfile(f.person).orElseThrow().experience().getOrDefault("baking",0L);}
    // Large, explicit reserve creates future paid food demand while this test learner remains well fed.
    VillageSupplyPolicy customers(){var p=VillageSupplyPolicy.prototype();return new VillageSupplyPolicy(true,p.foodPeriodBudget(),p.materialPeriodBudget(),p.minimumTreasury(),p.foodReservePeriods(),p.deliveryPeriods(),p.haulUnitsPerPeriod(),p.prices(),100);}
    BakeryOpportunities opportunities(){return new BakeryOpportunities(f.db.getConnection(),WorkshopOpportunityPolicy.prototype(),f.runtime(),StartupGrantPolicy.prototype(),f.starter,customers(),StudyPolicy.prototype(),BusinessOperationPolicy.prototype(),BakeryMarketPolicy.prototype());}
    EconomyRuntimeRepository runtime(long now,boolean enabled)throws Exception {
        var r=new EconomyRuntimeRepository(f.db.getConnection(),f.settings,FarmObservation.Policy.prototype(),216);
        r.configureStudy(StudyPolicy.prototype());r.configureVillageSupplies(customers());
        r.configureBakeryFounderStudy(opportunities(),enabled?BakeryFoundingPolicy.prototype():BakeryFoundingPolicy.disabled(),CareerStudyPolicy.prototype());r.startOnline(now,Set.of(f.world));return r;
    }
    @Test void realFuturePracticeQualifiesBakerAndPaidAcceptanceDoesNotGrantFreeExperience()throws Exception {
        var r=runtime(2000,true);assertTrue(r.reviewBakeryFounderStudy(f.town,2000).contains("prospective bakery owner"));r.reviewBakeryFounderStudy(f.town,2000);
        assertEquals(1,scalar("SELECT COUNT(*) FROM bakery_founder_courses"));assertEquals(0,xp());
        for(int n=1;n<=10;n++){r.pulse(2000+n*1000,false);assertEquals(n,xp());}
        assertEquals(20,scalar("SELECT units FROM economy_stocks WHERE commodity='timber'"));assertEquals(10,scalar("SELECT COUNT(*) FROM study_spending"));
        var engine=new BakeryInvestment(f.db.getConnection(),opportunities(),BakeryFoundingPolicy.prototype(),WorkshopOpportunityPolicy.prototype(),f.runtime(),StartupGrantPolicy.prototype(),f.starter,216,73);
        var draft=engine.prepare(f.town,0,12000,Set.of(f.world)).draft().orElseThrow();engine.accept(draft,new WorkplaceOperations.Confirmation(draft.anchor(),true,12000),12000,Set.of(f.world));
        assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(10,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='COMPLETED'"));
    }
    @Test void absenceMissingMarkerDisabledFoundingAndSuppliesPauseWithoutSpending()throws Exception {
        var r=runtime(2000,true);r.reviewBakeryFounderStudy(f.town,2000);r.pulse(3000,false);assertEquals(1,xp());
        r.updateLoadedWorlds(3000,Set.of());r.pulse(100000,false);assertEquals(1,xp());
        f.db.close();f.open();r=runtime(100000,true);r.pulse(101000,false);assertEquals(1,xp());
        new WorkplaceOpportunities(f.db.getConnection()).observe(f.town,new WorkplaceObservation(f.world,0,0,102000,true,List.of(new WorkplaceObservation.Marker(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(1,64,0),WorkplaceObservation.Kind.SMOKER))));
        r.pulse(102000,false);assertEquals(2,xp());
        f.sql("UPDATE economy_stocks SET units=2 WHERE commodity='timber'");r.pulse(103000,false);assertEquals(2,xp());
        f.sql("UPDATE economy_stocks SET units=30 WHERE commodity='timber'");r=runtime(103000,false);r.pulse(104000,false);assertEquals(2,xp());
        assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='ACTIVE'"));
    }
    @Test void enrollmentAndPracticeFailuresRollbackAndRetryWithoutDuplicateSkill()throws Exception {
        var r=runtime(2000,true);f.sql("CREATE TRIGGER reject_bakery_study BEFORE INSERT ON bakery_founder_reviews BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(java.sql.SQLException.class,()->r.reviewBakeryFounderStudy(f.town,2000));assertEquals(0,scalar("SELECT COUNT(*) FROM resident_courses"));
        f.sql("DROP TRIGGER reject_bakery_study");r.reviewBakeryFounderStudy(f.town,2000);
        f.sql("CREATE TRIGGER reject_baking_supplies BEFORE INSERT ON study_spending BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(java.sql.SQLException.class,()->r.pulse(3000,false));assertEquals(0,xp());assertEquals(30,scalar("SELECT units FROM economy_stocks WHERE commodity='timber'"));
        f.sql("DROP TRIGGER reject_baking_supplies");r.pulse(3000,false);r.pulse(3000,false);assertEquals(1,xp());
    }
    @Test void migration78FailureRetainsEveryPredecessorRowAndRetries()throws Exception {
        SchemaFixtures.rewind(f.db.getConnection(),78);var before=f.snapshot();f.sql("CREATE VIEW bakery_founder_courses AS SELECT 1");
        assertThrows(java.sql.SQLException.class,()->new SchemaMigrator().migrate(f.db.getConnection()));assertEquals(78,scalar("PRAGMA user_version"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='bakery_founder_reviews'"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='bakery_paid_food_market'"));
        f.sql("DROP VIEW bakery_founder_courses");assertEquals(before,f.snapshot());new SchemaMigrator().migrate(f.db.getConnection());
        var after=f.snapshot();assertEquals(List.of(),after.remove("craftsman_founder_courses"));assertEquals(List.of(),after.remove("craftsman_founder_reviews"));assertEquals(List.of(),after.remove("business_storage_upgrades"));assertEquals(List.of(),after.remove("bakery_founder_courses"));assertEquals(List.of(),after.remove("bakery_founder_reviews"));assertEquals(before,after);
        try(var s=f.db.getConnection().createStatement();var fk=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(fk.next());}
    }
    @Test void historicalBacklogCannotSponsorARetroactiveCourse()throws Exception {
        var r=runtime(2000,true);assertTrue(r.reviewBakeryFounderStudy(f.town,7000).contains("waits"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM bakery_founder_courses"));assertEquals(0,xp());
        for(int n=0;n<5;n++)r.pulse(7000,false);assertEquals(0,xp());
        assertTrue(r.reviewBakeryFounderStudy(f.town,7000).contains("prospective bakery owner"));
        r.pulse(8000,false);assertEquals(1,xp());
    }
    @Test void takenFirstBusinessCancelsLearnerAndReleasesTheirJobLock()throws Exception {
        var r=runtime(2000,true);r.reviewBakeryFounderStudy(f.town,2000);
        var other=UUID.randomUUID();f.db.getPopulationRepository().insertResident(new com.blueninjas24.civilizations.population.Resident(other,"Other baker",f.town,null,300,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));
        f.db.getPopulationRepository().recordSkillExperience(other,"baking",10);r.pulse(3000,false);
        var engine=new BakeryInvestment(f.db.getConnection(),opportunities(),BakeryFoundingPolicy.prototype(),WorkshopOpportunityPolicy.prototype(),f.runtime(),StartupGrantPolicy.prototype(),f.starter,216,73);
        var d=engine.prepare(f.town,0,3000,Set.of(f.world)).draft().orElseThrow();engine.accept(d,new WorkplaceOperations.Confirmation(d.anchor(),true,3000),3000,Set.of(f.world));
        r.pulse(4000,false);assertEquals(0,xp());assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='CANCELLED'"));
    }
}
