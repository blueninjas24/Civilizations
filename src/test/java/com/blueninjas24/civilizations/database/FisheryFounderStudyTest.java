package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FisheryFounderStudyTest {
    @TempDir Path directory;FisheryOpportunitiesTest fixture;EconomyRuntimeRepository food;
    FisheryFoundingPolicy founding=FisheryFoundingPolicy.prototype();
    ShorelineScan.Position bank=new ShorelineScan.Position(-1,65,0);
    BusinessStartupGrantsTest data(){return fixture.fixture;}
    @BeforeEach void setup()throws Exception {
        fixture=new FisheryOpportunitiesTest();fixture.directory=directory;fixture.setup();
        data().db.getPopulationRepository().insertResident(new Resident(new UUID(0,1),"Fixture farmer",data().town,null,300,Resident.Status.ALIVE));
        data().sql("UPDATE economy_stocks SET units=7 WHERE commodity='food'");data().sql("INSERT INTO economy_stocks VALUES('"+data().town+"','timber',20,0)");
        var s=data().settings;var settings=new EconomySettings(s.secondsPerPeriod(),s.maximumOfflinePeriods(),s.periodsPerBatch(),s.adultFoodUnits(),s.childFoodUnits(),s.foodUnitsPerWorker(),s.foodOutputPercent(),s.shortages(),Map.of(Commodity.FOOD,100L,Commodity.TIMBER,20L),Map.of(Commodity.FOOD,java.math.BigDecimal.ONE,Commodity.TIMBER,java.math.BigDecimal.ONE),s.pricePercent());
        data().settings=settings;food=new EconomyRuntimeRepository(data().db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        food.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());food.configureStudy(StudyPolicy.prototype());
        configure(food);food.observe(new FarmObservation(data().town,data().world,2000,true,16,new VillageObservation.Position(8,64,8)),2000);food.startOnline(2000,Set.of(data().world));food.pulse(3000,false);
        assertEquals(1,data().db.getBusinessRepository().currentWorkers(FarmEnterprises.businessId(data().town)).size());assertEquals(7,stock(Commodity.FOOD));
    }
    @AfterEach void close(){fixture.close();}
    void configure(EconomyRuntimeRepository runtime){runtime.configureFisheryFounderStudy(fixture.repo(),founding,FisheryShorelinePolicy.prototype(),fixture.terms,CareerStudyPolicy.prototype());}
    FisheryOpportunities.Opportunity opportunity(long now)throws Exception{var review=fixture.review(now);assertFalse(review.candidates().isEmpty(),review.explanation());return review.candidates().getFirst();}
    ShorelineEvidence frame(FisheryOpportunities.Opportunity o,long now,boolean safe) {
        var scan=new ShorelineScan(p->safe&&p.equals(new ShorelineScan.Position(bank.x(),bank.y()-1,bank.z()))?ShorelineScan.Cell.SAFE_FOOTING:ShorelineScan.Cell.AIR,o.water(),2);
        while(!scan.complete())scan.advance(4096);return new ShorelineEvidence(data().world,bank,now,scan.assessments());
    }
    String enroll(long now)throws Exception{var o=opportunity(now);return food.reviewFisheryFounderStudy(o,frame(o,now,true),now);}
    long xp()throws Exception{return data().db.getPopulationRepository().findWorkProfile(data().person).orElseThrow().experience().getOrDefault("fishing",0L);}
    long stock(Commodity good)throws Exception{return new CommodityRepository(data().db.getConnection()).read(data().town).orElseThrow().state().stocks().get(good).units();}
    @Test void realFarmSupportedFuturePracticeQualifiesThenPaysFirstFisheryOnce()throws Exception {
        assertTrue(enroll(3000).contains("course enrolled"));assertEquals(0,xp());assertEquals(10000,data().cash());enroll(3000);assertEquals(1,data().count("fishery_founder_courses"));
        for(int n=1;n<=10;n++){food.pulse(3000+n*1000,false);assertEquals(n,xp());assertEquals(7,stock(Commodity.FOOD));}
        assertEquals(10,stock(Commodity.TIMBER));assertEquals(10,data().count("study_spending"));
        var investment=new FisheryInvestment(data().db.getConnection(),fixture.repo(),founding,fixture.terms,fixture.industry,WaterObserverPolicy.prototype(),FisheryShorelinePolicy.prototype(),StartupGrantPolicy.prototype(),216,42);
        var proposal=investment.prepare(data().town,2,13000,Set.of(data().world)).proposal().orElseThrow();investment.accept(proposal,frame(proposal.opportunity(),13000,true),13000,Set.of(data().world),FishingPolicy.prototype());
        assertEquals(8500,data().cash());assertEquals(1,data().count("startup_grants"));assertEquals(1,data().count("fishery_resources"));assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='COMPLETED'"));
    }
    @Test void unknownWaterAndLostFundsPausePracticeWithoutDiscardingCourseOrMaterials()throws Exception {
        enroll(3000);fixture.observe(4000,0,64,false);food.pulse(4000,false);assertEquals(0,xp());assertEquals(20,stock(Commodity.TIMBER));
        fixture.observe(5000,0,64,true);data().sql("UPDATE treasuries SET hundredths=2000");food.pulse(5000,false);assertEquals(0,xp());
        data().sql("UPDATE treasuries SET hundredths=10000");food.pulse(6000,false);assertEquals(1,xp());assertEquals(19,stock(Commodity.TIMBER));
    }
    @Test void unsafeWrongWorldAndIncompleteBanksCannotSponsorLearners()throws Exception {
        var o=opportunity(3000);assertThrows(SQLException.class,()->food.reviewFisheryFounderStudy(o,frame(o,3000,false),3000));
        var safe=frame(o,3000,true);assertThrows(SQLException.class,()->food.reviewFisheryFounderStudy(o,new ShorelineEvidence(UUID.randomUUID(),bank,3000,safe.assessments()),3000));
        assertThrows(SQLException.class,()->food.reviewFisheryFounderStudy(o,new ShorelineEvidence(data().world,bank,3000,List.of(safe.assessments().stream().filter(a->a.position().equals(bank)).findFirst().orElseThrow())),3000));assertEquals(0,data().count("resident_courses"));assertEquals(10000,data().cash());
    }
    @Test void aDifferentSafeReplacementCannotCertifyAnUnsafeProposedTrainingBank()throws Exception {
        var o=opportunity(3000);var safe=frame(o,3000,true);
        assertThrows(SQLException.class,()->food.reviewFisheryFounderStudy(o,new ShorelineEvidence(data().world,new ShorelineScan.Position(0,65,-1),3000,safe.assessments()),3000));
        assertEquals(0,data().count("resident_courses"));assertEquals(10000,data().cash());
    }
    @Test void lateCourseAndPracticeFailuresRollBackAllRowsBeforeExactRetry()throws Exception {
        data().sql("CREATE TRIGGER reject_fish_course BEFORE INSERT ON fishery_founder_courses BEGIN SELECT RAISE(ABORT,'fixture failure'); END");assertThrows(SQLException.class,()->enroll(3000));assertEquals(0,data().count("resident_courses"));
        data().sql("DROP TRIGGER reject_fish_course");enroll(3000);data().sql("CREATE TRIGGER reject_fish_practice BEFORE INSERT ON study_spending BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->food.pulse(4000,false));assertEquals(0,xp());assertEquals(20,stock(Commodity.TIMBER));data().sql("DROP TRIGGER reject_fish_practice");food.pulse(4000,false);food.pulse(4000,false);assertEquals(1,xp());assertEquals(1,data().count("study_spending"));
    }
    @Test void prospectiveTrainingPreviewCannotPermitActualBacklogPurchases()throws Exception {
        var customers=new VillageSupplyPurchases(data().db.getConnection(),VillageSupplyPolicy.prototype(),StudyPolicy.prototype(),BusinessOperationPolicy.prototype());
        data().sql("UPDATE economy_runtime SET target_periods=completed_periods+1 FROM economy_accounts WHERE economy_runtime.settlement_id=economy_accounts.settlement_id");
        assertTrue(customers.demand(data().town,Set.of(data().world)).fundedUnits().isEmpty());assertFalse(customers.demandForTrainingAfterSpending(data().town,Set.of(data().world),1500).fundedUnits().isEmpty());
        assertThrows(VillageSupplyPurchases.Declined.class,()->customers.purchase(new VillageSupplyPurchases.Request(UUID.randomUUID(),data().town,UUID.randomUUID(),Commodity.FOOD,1,2),Set.of(data().world)));
        data().sql("UPDATE economy_runtime SET target_periods=target_periods+1");assertTrue(customers.demandForTrainingAfterSpending(data().town,Set.of(data().world),1500).fundedUnits().isEmpty());assertEquals(10000,data().cash());
    }
    @Test void migration70FailureRetainsEveryPredecessorRowThenRetriesWithoutInventingCourses()throws Exception {
        SchemaFixtures.rewind(data().db.getConnection(),69);data().sql("PRAGMA user_version=69");var before=data().snapshot();data().sql("CREATE VIEW fishery_founder_courses AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(data().db.getConnection()));assertEquals(69,scalar("PRAGMA user_version"));assertEquals(before,data().snapshot());data().sql("DROP VIEW fishery_founder_courses");new SchemaMigrator().migrate(data().db.getConnection());var after=data().snapshot();for(var added:List.of("livestock_learning_reviews","livestock_learning_entries","livestock_slaughter","quarry_founder_courses","quarry_founder_reviews","quarry_survey_members","quarry_surveys","quarry_opportunities","active_pen_claims","pen_releases","livestock_presence","livestock_birth_losses","livestock_birth_projections","livestock_birth_entities","livestock_births","livestock_product_sales","livestock_capture","livestock_animal_losses","craftsman_founder_courses","craftsman_founder_reviews","business_storage_upgrades","bakery_founder_courses","bakery_founder_reviews","bakery_private_starters","livestock_herds","livestock_initial_animals","livestock_periods","livestock_young"))assertEquals(List.of(),after.remove(added));assertEquals(List.of(),after.remove("protected_pen_animals"));assertEquals(List.of(),after.remove("protected_pen_columns"));assertEquals(List.of(),after.remove("protected_pens"));assertEquals(List.of(),after.remove("animal_ownership"));assertEquals(List.of(),after.remove("farm_market_harvests"));assertEquals(List.of(),after.remove("farm_market_stores"));assertEquals(List.of(),after.remove("grain_harvests"));assertEquals(List.of(),after.remove("grain_stocks"));assertEquals(List.of(),after.remove("farm_grain_evidence"));assertEquals(List.of(),after.remove("fishery_founder_courses"));assertEquals(before,after);
    }
    @Test void staleBankProofPausesAllBacklogThenFreshProofOnlyPermitsFuturePractice()throws Exception {
        enroll(3000);
        for(int n=0;n<80;n++)food.pulse(64000,false);
        assertEquals(scalar("SELECT target_periods FROM economy_runtime"),scalar("SELECT completed_periods FROM economy_accounts"),"Existing earned food backlog must be completed before refreshing the course");
        assertEquals(0,xp());assertEquals(20,stock(Commodity.TIMBER));
        fixture.observe(64000,0,64,true);assertTrue(enroll(64000).contains("fresh safe bank"));
        food.pulse(64000,false);assertEquals(0,xp());food.pulse(65000,false);assertEquals(1,xp());assertEquals(19,stock(Commodity.TIMBER));
        assertEquals(1,data().count("fishery_founder_courses"));
    }
    @Test void absentWorldAndRestartRetainCourseWithoutOfflineTraining()throws Exception {
        enroll(3000);food.pulse(4000,false);assertEquals(1,xp());
        food.updateLoadedWorlds(4000,Set.of());food.pulse(50000,false);assertEquals(1,xp());
        data().db.close();data().open();food=new EconomyRuntimeRepository(data().db.getConnection(),data().settings,FarmObservation.Policy.prototype(),216);
        food.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());food.configureStudy(StudyPolicy.prototype());configure(food);food.startOnline(50000,Set.of(data().world));food.pulse(50000,false);assertEquals(1,xp());
        food.pulse(51000,false);assertEquals(2,xp());assertEquals(1,data().count("fishery_founder_courses"));assertEquals(18,stock(Commodity.TIMBER));
    }
    @Test void availableQualifiedOwnerCannotJustifyAnExtraCourse()throws Exception {
        data().db.getPopulationRepository().recordSkillExperience(data().person,"fishing",10);
        assertTrue(enroll(3000).contains("already available"));assertEquals(0,data().count("resident_courses"));assertEquals(20,stock(Commodity.TIMBER));assertEquals(10000,data().cash());
    }
    @Test void disablingFoundingPreservesAcceptedCourseButPausesFurtherPractice()throws Exception {
        enroll(3000);founding=FisheryFoundingPolicy.disabled();
        food=new EconomyRuntimeRepository(data().db.getConnection(),data().settings,FarmObservation.Policy.prototype(),216);
        food.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());food.configureStudy(StudyPolicy.prototype());configure(food);food.startOnline(3000,Set.of(data().world));
        assertTrue(enroll(3000).contains("disabled"));food.pulse(4000,false);
        assertEquals(0,xp());assertEquals(20,stock(Commodity.TIMBER));assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='ACTIVE'"));
    }
    long scalar(String query)throws Exception{try(var s=data().db.getConnection().createStatement();var r=s.executeQuery(query)){return r.getLong(1);}}
}
