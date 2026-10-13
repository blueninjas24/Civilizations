package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.economy.FarmObservation;
import com.blueninjas24.civilizations.economy.EconomySettings;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Earned fishing experience and real-vacancy study; no free founder qualification. */
class FisheryLearningTest {
    @TempDir Path directory;
    FisheryWorkplacesTest fixture;
    @BeforeEach void setup()throws Exception {fixture=new FisheryWorkplacesTest();fixture.directory=directory;fixture.setup();fixture.start();}
    @AfterEach void close(){fixture.close();}
    BusinessStartupGrantsTest data(){return fixture.data();}
    long xp(UUID person)throws Exception{return data().db.getPopulationRepository().findWorkProfile(person).orElseThrow().experience().getOrDefault("fishing",0L);}
    IndustryRuntime runtime(WorkLearningPolicy learning,long now)throws Exception {
        var r=new IndustryRuntime(data().db.getConnection(),new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of()),BusinessOperationPolicy.prototype(),learning);
        r.configureFishing(FishingPolicy.prototype());r.start(now);return r;
    }
    @Test void actualCatchLearnsOnceAndAbsenceRestartNeverAwardsPastWork()throws Exception {
        var r=runtime(WorkLearningPolicy.prototype(),1000);r.pulse(2000,Set.of(data().world));assertEquals(11,xp(data().person));
        assertEquals(4,data().db.getIndustryRepository().store(fixture.business).orElseThrow().units().get(Commodity.FOOD));
        r.pulse(2000,Set.of(data().world));r.pulse(100000,Set.of());assertEquals(11,xp(data().person));
        data().db.close();data().open();r=runtime(WorkLearningPolicy.prototype(),100000);r.pulse(100000,Set.of(data().world));assertEquals(11,xp(data().person));
        r.pulse(101000,Set.of(data().world));assertEquals(12,xp(data().person));assertEquals(2,data().count("industry_learning_entries"));assertEquals(8500,data().cash());
    }
    @Test void damagedBankCannotAwardExperienceAndRealRecoveryRestartsLearning()throws Exception {
        fixture.banks().reconcile(fixture.site,fixture.frame(30,fixture.initial,Set.of(),false),30,100);
        var r=runtime(WorkLearningPolicy.prototype(),1000);r.pulse(2000,Set.of(data().world));assertEquals(10,xp(data().person));assertEquals(0,data().count("industry_learning_entries"));
        fixture.banks().reconcile(fixture.site,fixture.frame(40,fixture.initial,Set.of(fixture.replacement),false),40,100);
        r.pulse(3000,Set.of(data().world));assertEquals(11,xp(data().person));assertEquals(1,data().count("industry_learning_entries"));
    }
    @Test void lateLearningFailureRollsBackCatchReserveSkillAndCheckpointTogether()throws Exception {
        var before=new FisheryRepository(data().db.getConnection()).find(fixture.site).orElseThrow();var r=runtime(WorkLearningPolicy.prototype(),1000);
        data().sql("CREATE TRIGGER reject_fish_learning BEFORE INSERT ON industry_learning_entries BEGIN SELECT RAISE(ABORT,'fixture learning failure'); END");
        assertThrows(SQLException.class,()->r.pulse(2000,Set.of(data().world)));assertEquals(10,xp(data().person));assertEquals(0,data().count("industry_production"));assertEquals(before,new FisheryRepository(data().db.getConnection()).find(fixture.site).orElseThrow());
        data().sql("DROP TRIGGER reject_fish_learning");r.pulse(2000,Set.of(data().world));r.pulse(2000,Set.of(data().world));assertEquals(11,xp(data().person));assertEquals(1,data().count("industry_learning_entries"));
    }
    @Test void disabledLearningRetainsCatchesAndUnmappedPolicyCannotInventSkill()throws Exception {
        var r=runtime(WorkLearningPolicy.disabled(),1000);r.pulse(2000,Set.of(data().world));assertEquals(10,xp(data().person));
        assertEquals(4,data().db.getIndustryRepository().store(fixture.business).orElseThrow().units().get(Commodity.FOOD));
        r=runtime(new WorkLearningPolicy(true,5,100,64,Map.of()),2000);r.pulse(3000,Set.of(data().world));assertEquals(10,xp(data().person));assertEquals(0,data().count("industry_learning_entries"));
    }
    @Test void fullStorageStopsSkillAwardsWithoutDeletingGoodsOrRefillingHabitat()throws Exception {
        data().sql("UPDATE industry_stock SET units=100 WHERE commodity='food'");
        var r=runtime(WorkLearningPolicy.prototype(),1000);r.pulse(2000,Set.of(data().world));
        assertEquals(10,xp(data().person));assertEquals(0,data().count("industry_learning_entries"));
        assertEquals(100,data().db.getIndustryRepository().store(fixture.business).orElseThrow().units().get(Commodity.FOOD));
        assertEquals(8500,data().cash());
    }
    @Test void existingFisheryVacancySponsorsRealFutureStudyThenOrdinaryQualifiedHiring()throws Exception {
        var learner=UUID.randomUUID();data().db.getPopulationRepository().insertResident(new Resident(learner,"Fixture learner",data().town,null,300,Resident.Status.ALIVE));
        data().sql("UPDATE economy_stocks SET units=100 WHERE commodity='food'");data().sql("INSERT INTO economy_stocks VALUES('"+data().town+"','timber',50,0)");
        var settings=data().settings;
        var catalog=new EconomySettings(settings.secondsPerPeriod(),settings.maximumOfflinePeriods(),settings.periodsPerBatch(),settings.adultFoodUnits(),settings.childFoodUnits(),settings.foodUnitsPerWorker(),settings.foodOutputPercent(),settings.shortages(),Map.of(Commodity.FOOD,100L,Commodity.TIMBER,50L),Map.of(Commodity.FOOD,java.math.BigDecimal.ONE,Commodity.TIMBER,java.math.BigDecimal.ONE),settings.pricePercent());
        var food=new EconomyRuntimeRepository(data().db.getConnection(),catalog,FarmObservation.Policy.prototype(),216);
        food.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());food.configureStudy(StudyPolicy.prototype());food.startOnline(2000,Set.of(data().world));food.pulse(3000,false);
        assertTrue(food.reviewCareerStudy(fixture.business,CareerStudyPolicy.prototype(),2,3000).contains("1 actual local adults"));assertEquals(0,xp(learner));
        food.reviewCareerStudy(fixture.business,CareerStudyPolicy.prototype(),2,3000);assertEquals(1,data().count("resident_courses"));
        food.pulse(4000,false);assertEquals(1,xp(learner));assertEquals(49,scalar("SELECT units FROM economy_stocks WHERE commodity='timber'"));assertEquals(1,data().db.getBusinessRepository().currentWorkers(fixture.business).size());
        var businesses=new BusinessRuntime(data().db.getConnection(),8,32);businesses.configureStaffing(new BusinessStaffingPolicy(true,64,2,Set.of("fishing")));businesses.refresh(3,Set.of(data().world));
        assertEquals(2,data().db.getBusinessRepository().currentWorkers(fixture.business).size());assertEquals(8500,data().cash());
        food.pulse(5000,false);assertEquals(1,xp(learner));assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='CANCELLED'"));
    }
    long scalar(String query)throws Exception{try(var s=data().db.getConnection().createStatement();var r=s.executeQuery(query)){return r.getLong(1);}}
}
