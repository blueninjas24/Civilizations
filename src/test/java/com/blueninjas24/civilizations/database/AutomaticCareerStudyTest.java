package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticCareerStudyTest {
    @TempDir Path folder;DatabaseManager db;EconomyRuntimeRepository economy;BusinessRuntime runtime;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=new UUID(0,1),student=new UUID(0,2),other=new UUID(0,3),firm=new UUID(1,1);
    EconomySettings settings;
    StudyPolicy study(){return new StudyPolicy(true,1,100,2,8,1,2,Map.of("mining",new StudyPolicy.Course(2,Map.of(Commodity.TIMBER,1L,Commodity.STONE,1L))));}
    void open()throws Exception{db=new DatabaseManager(folder.toFile(),Logger.getLogger("CareerStudyTest"));db.initialize();}
    void setupRuntime(long now)throws Exception {
        economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);economy.configureStudy(study());economy.startOnline(now,Set.of(world));
        runtime=new BusinessRuntime(db.getConnection(),8,32);runtime.configureStaffing(new BusinessStaffingPolicy(true,64,1,Set.of("mining")));runtime.configureCareerStudy(economy,CareerStudyPolicy.prototype());
    }
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,3,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var id:List.of(owner,student,other))db.getPopulationRepository().insertResident(new Resident(id,"Fixture",town,null,300,Resident.Status.ALIVE));
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,0,"Existing fixture mine");var b=new Business(firm,town,"Existing mine",owner,owner,"mining",site.id(),1,0);
        db.getBusinessRepository().found(new BusinessFounding(b,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of("mining",2L)),0,"Existing actual vacancy"));
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,25L,Commodity.TIMBER,10L,Commodity.STONE,10L),Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.TIMBER,BigDecimal.ONE,Commodity.STONE,BigDecimal.ONE),bands);
        economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);economy.configureStudy(study());economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));
        runtime=new BusinessRuntime(db.getConnection(),8,32);runtime.configureStaffing(new BusinessStaffingPolicy(true,64,1,Set.of("mining")));runtime.configureCareerStudy(economy,CareerStudyPolicy.prototype());economy.pulse(2000,false);
    }
    @AfterEach void close(){db.close();}
    void sql(String text)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(text);}}
    long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    long xp(UUID id)throws SQLException{return db.getPopulationRepository().findWorkProfile(id).orElseThrow().experience().getOrDefault("mining",0L);}
    long timber()throws SQLException{return db.getCommodityRepository().load(town).orElseThrow().state().stocks().get(Commodity.TIMBER).units();}
    ResidentStudy.Course course()throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT course_id FROM career_study_enrollments LIMIT 1")){return new ResidentStudy(db.getConnection(),study(),216).find(UUID.fromString(r.getString(1))).orElseThrow();}}
    @Test void vacancySelectsRealAdultStudiesWithActualSuppliesAndNormalHiring()throws Exception {
        assertTrue(runtime.refresh(1,Set.of(world),2000,false).businesses().get(firm).lifecycle().contains("1 actual local adults"));
        assertEquals(student,course().resident());assertEquals(0,xp(student));assertEquals(30,timber());assertEquals(0,count("business_employment"));
        runtime.refresh(1,Set.of(world),2000,false);assertEquals(1,count("resident_courses"));
        economy.pulse(3000,false);runtime.refresh(1,Set.of(world),3000,false);assertEquals(1,xp(student));assertEquals(29,timber());assertEquals(0,count("business_employment"));
        economy.pulse(4000,false);runtime.refresh(1,Set.of(world),4000,false);assertEquals(2,xp(student));assertEquals(28,timber());assertEquals("COMPLETED",course().status());
        assertEquals(student,db.getBusinessRepository().currentWorkers(firm).getFirst().residentId());assertEquals(1,count("business_hiring_reviews"));assertEquals(0,xp(owner));assertEquals(0,xp(other));assertEquals(1,count("businesses"));
    }
    @Test void disappearingVacancyCancelsBeforePracticeGoodsOrSkillsAreSpent()throws Exception {
        economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2000);db.getPopulationRepository().recordSkillExperience(other,"mining",2);db.getBusinessRepository().hire(UUID.randomUUID(),firm,other,1);
        economy.pulse(3000,false);assertEquals("CANCELLED",course().status());assertEquals(30,timber());assertEquals(0,xp(student));
    }
    @Test void unavailableOwnerCancelsSponsoredStudyButDoesNotTouchManualCourses()throws Exception {
        economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2000);economy.enrollStudy(UUID.randomUUID(),other,town,"mining",2000);
        db.getPopulationRepository().updateResident(new Resident(owner,"Fixture",town,null,300,Resident.Status.DECEASED));economy.pulse(3000,false);
        assertEquals("CANCELLED",course().status());assertEquals(0,xp(student));assertEquals(1,xp(other));assertEquals(29,timber());
    }
    @Test void disabledAbsentAndIncompleteEvidenceDoNotEnrollOrSpend()throws Exception {
        economy.reviewCareerStudy(firm,CareerStudyPolicy.disabled(),1,2000);assertEquals(0,count("resident_courses"));
        economy.updateLoadedWorlds(2000,Set.of());assertTrue(economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2000).contains("present-world"));assertEquals(0,count("career_study_reviews"));
        economy.updateLoadedWorlds(2000,Set.of(world));economy.observe(new FarmObservation(town,world,2100,false,0),2100);
        assertTrue(economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2100).contains("complete"));assertEquals(0,count("resident_courses"));assertEquals(30,timber());
    }
    @Test void unsupportedSkillOrInsufficientCourseTargetCannotCreateFakeQualification()throws Exception {
        sql("UPDATE business_skill_requirements SET experience=3 WHERE business_id='"+firm+"' AND role='WORKER'");
        economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2000);assertEquals(0,count("resident_courses"));assertEquals(0,xp(student));
    }
    @Test void failedCareerAuditRollsBackCourseMaterialsAndClockThenRetries()throws Exception {
        sql("CREATE TRIGGER fail_career BEFORE INSERT ON career_study_reviews BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2500));assertEquals(0,count("resident_courses"));assertEquals(0,count("study_materials"));assertEquals(0,count("career_study_enrollments"));assertEquals(30,timber());
        sql("DROP TRIGGER fail_career");economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2500);economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2500);assertEquals(1,count("resident_courses"));assertEquals(1,count("career_study_reviews"));
    }
    @Test void restartAndAbsentWorldPreserveAcceptedCourseAndNoCatchup()throws Exception {
        economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2000);economy.pulse(3000,false);economy.updateLoadedWorlds(3000,Set.of());economy.pulse(100000,false);assertEquals(1,xp(student));
        db.close();open();setupRuntime(100000);runtime.refresh(1,Set.of(world),100000,false);assertEquals(1,count("resident_courses"));assertEquals(29,timber());
        economy.pulse(101000,false);runtime.refresh(1,Set.of(world),101000,false);assertEquals(2,xp(student));assertEquals(student,db.getBusinessRepository().currentWorkers(firm).getFirst().residentId());
    }
    @Test void coursesCannotTrainInsideAlreadyEarnedBacklog()throws Exception {
        economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,5000);assertEquals(4,course().eligibleAfter());for(int i=0;i<4;i++)economy.pulse(5000,false);assertEquals(0,xp(student));assertEquals(30,timber());
        economy.pulse(6000,false);assertEquals(1,xp(student));assertEquals(29,timber());
    }
    @Test void failedSchema56UpgradeRetainsOldCoursesAndRetriesEmptyCareerAudit()throws Exception {
        var old=UUID.randomUUID();economy.enrollStudy(old,other,town,"mining",2000);SchemaFixtures.rewind(db.getConnection(),56);sql("CREATE INDEX career_vacancy_courses ON businesses(id)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(56,r.getInt(1));}
        sql("DROP INDEX career_vacancy_courses");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("career_study_reviews"));assertEquals(1,count("resident_courses"));assertEquals(old,new ResidentStudy(db.getConnection(),study(),216).find(old).orElseThrow().id());
    }
    @Test void untimedViewRefreshCannotEnrollIntoUnobservedEarnedTime()throws Exception {
        runtime.refresh(1,Set.of(world));assertEquals(0,count("resident_courses"));
        runtime.refresh(1,Set.of(world),5000,false);assertEquals(4,course().eligibleAfter());assertEquals(0,xp(student));
    }
    @Test void perVacancyAndVillageBudgetsDoNotOverbookRealJobs()throws Exception {
        economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,2000);economy.pulse(3000,false);economy.reviewCareerStudy(firm,CareerStudyPolicy.prototype(),1,3000);
        assertEquals(1,count("resident_courses"));assertEquals(2,count("career_study_reviews"));assertEquals(1,count("career_study_enrollments"));assertEquals(0,xp(other));
    }
    @Test void candidateEligibilityAndSavedReviewSurviveRestart()throws Exception {
        sql("UPDATE residents SET age_steps=100 WHERE id='"+student+"'"); // Child is never a candidate.
        var policy=new CareerStudyPolicy(true,1,1,Set.of("mining"));economy.reviewCareerStudy(firm,policy,1,2000);assertEquals(other,course().resident());
        db.close();open();setupRuntime(2000);economy.reviewCareerStudy(firm,policy,1,2000);assertEquals(1,count("resident_courses"));assertEquals(other,course().resident());
    }

    @Test void savedCandidatePagesRotatePastAlreadyQualifiedPeopleAfterRestart()throws Exception {
        db.getPopulationRepository().recordSkillExperience(student,"mining",2);var policy=new CareerStudyPolicy(true,1,1,Set.of("mining"));
        economy.reviewCareerStudy(firm,policy,1,2000);assertEquals(0,count("resident_courses"));
        db.close();open();setupRuntime(2000);economy.pulse(3000,false);economy.reviewCareerStudy(firm,policy,1,3000);
        assertEquals(other,course().resident());assertEquals(1,count("resident_courses"));assertEquals(0,xp(other));
    }

}
