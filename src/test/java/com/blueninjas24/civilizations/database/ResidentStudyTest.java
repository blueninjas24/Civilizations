package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class ResidentStudyTest {
    @TempDir Path directory;DatabaseManager db;EconomyRuntimeRepository runtime;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),person=UUID.randomUUID(),other=UUID.randomUUID(),course=UUID.randomUUID();
    EconomySettings settings;
    StudyPolicy policy(long target,int students,long keep){return new StudyPolicy(true,1,100,students,8,1,keep,Map.of("mining",new StudyPolicy.Course(target,Map.of(Commodity.STONE,1L,Commodity.TIMBER,1L))));}
    void open()throws SQLException{db=new DatabaseManager(directory.toFile(),Logger.getLogger("StudyTest"));db.initialize();}
    EconomyRuntimeRepository runtime(StudyPolicy p)throws SQLException {
        var r=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);r.configureStudy(p);return r;
    }
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Study fixture","World",0,64,0,2,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"World");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var id:List.of(person,other))db.getPopulationRepository().insertResident(new Resident(id,"Learner",town,null,300,Resident.Status.ALIVE));
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,25L,Commodity.STONE,10L,Commodity.TIMBER,10L),Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.STONE,BigDecimal.ONE,Commodity.TIMBER,BigDecimal.ONE),bands);
        runtime=runtime(policy(2,2,2));runtime.observe(new FarmObservation(town,world,1000,true,0),1000);runtime.startOnline(1000,Set.of(world));
    }
    @AfterEach void close(){db.close();}
    void sql(String query)throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(query);}}
    long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    long xp(UUID id)throws SQLException{return db.getPopulationRepository().findWorkProfile(id).orElseThrow().experience().getOrDefault("mining",0L);}
    long stock(Commodity c)throws SQLException{return db.getCommodityRepository().load(town).orElseThrow().state().stocks().get(c).units();}
    ResidentStudy.Course saved()throws SQLException{return new ResidentStudy(db.getConnection(),StudyPolicy.disabled(),216).find(course).orElseThrow();}
    @Test void actualSuppliesAndSkillsCommitOnceCompleteAndSurviveRetuningRestart()throws Exception {
        var accepted=runtime.enrollStudy(course,person,town,"mining",1000);runtime.pulse(2000,false);
        assertEquals(1,xp(person));assertEquals(19,stock(Commodity.STONE));assertEquals(19,stock(Commodity.TIMBER));assertEquals(46,stock(Commodity.FOOD));
        assertEquals(2,count("study_spending"));var history=db.getCommodityRepository().history(town,1).orElseThrow();
        assertEquals(1,history.reports().stream().filter(r->r.commodity().equals(Commodity.STONE)).findFirst().orElseThrow().flow().events());
        runtime.pulse(2000,false);assertEquals(1,xp(person));
        db.close();open();runtime=runtime(new StudyPolicy(true,5,100,2,8,1,2,Map.of("mining",new StudyPolicy.Course(50,Map.of(Commodity.STONE,8L)))));runtime.startOnline(100000,Set.of(world));
        assertEquals(accepted,runtime.enrollStudy(course,person,town,"mining",100000));assertEquals(1,xp(person));runtime.pulse(101000,false);
        assertEquals(2,xp(person));assertEquals("COMPLETED",saved().status());assertEquals(18,stock(Commodity.STONE));assertEquals(18,stock(Commodity.TIMBER));
        runtime.pulse(102000,false);assertEquals(2,xp(person));assertEquals(4,count("study_spending"));assertEquals(2,count("study_reviews"));
        assertThrows(SQLException.class,()->runtime.enrollStudy(course,other,town,"mining",102000));
    }
    @Test void failuresRollBackGoodsSkillsCoursesAndFoodCheckpointThenRetryOnce()throws Exception {
        runtime.enrollStudy(course,person,town,"mining",1000);
        sql("CREATE TRIGGER reject_study BEFORE INSERT ON study_spending BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));assertEquals(0,xp(person));assertEquals(20,stock(Commodity.STONE));assertEquals(50,stock(Commodity.FOOD));assertEquals(0,count("study_reviews"));assertEquals(0,count("economy_periods"));assertEquals("ACTIVE",saved().status());
        sql("DROP TRIGGER reject_study");runtime.pulse(2000,false);assertEquals(1,xp(person));assertEquals(2,count("study_spending"));
        sql("CREATE TRIGGER reject_checkpoint BEFORE UPDATE ON economy_accounts BEGIN SELECT RAISE(ABORT,'late'); END");
        assertThrows(SQLException.class,()->runtime.pulse(3000,false));assertEquals(1,xp(person));assertEquals(19,stock(Commodity.STONE));assertEquals("ACTIVE",saved().status());
        sql("DROP TRIGGER reject_checkpoint");runtime.pulse(3000,false);assertEquals(2,xp(person));assertEquals("COMPLETED",saved().status());
    }
    @Test void foodShortageAndMaterialReservePauseWithoutSpendingOrFreeSkill()throws Exception {
        runtime.enrollStudy(course,person,town,"mining",1000);sql("UPDATE economy_stocks SET units=4 WHERE commodity='food'");runtime.pulse(2000,false);
        assertEquals(0,xp(person));assertEquals(20,stock(Commodity.STONE));assertEquals(0,count("study_spending"));
        runtime.recordDeliveredFood(UUID.randomUUID(),town,100,"Test-owned food",2000);
        sql("UPDATE economy_stocks SET units=2 WHERE commodity='stone'");runtime.pulse(3000,false);assertEquals(0,xp(person));assertEquals(2,stock(Commodity.STONE));assertEquals(0,count("study_spending"));
        sql("UPDATE economy_stocks SET units=3 WHERE commodity='stone'");runtime.pulse(4000,false);assertEquals(1,xp(person));assertEquals(2,stock(Commodity.STONE));assertEquals(19,stock(Commodity.TIMBER));
    }
    @Test void budgetRotatesStudentsRatherThanSpendingSuppliesForDeferredPeople()throws Exception {
        runtime=runtime(policy(10,1,2));runtime.startOnline(1000,Set.of(world));runtime.enrollStudy(course,person,town,"mining",1000);runtime.enrollStudy(UUID.randomUUID(),other,town,"mining",1000);
        runtime.pulse(2000,false);assertEquals(1,xp(person)+xp(other));runtime.pulse(3000,false);assertEquals(1,xp(person));assertEquals(1,xp(other));assertEquals(18,stock(Commodity.STONE));assertEquals(4,count("study_spending"));
    }
    @Test void absentWorldPartialEvidenceAndBacklogCannotAwardPastStudy()throws Exception {
        runtime.enrollStudy(course,person,town,"mining",5000);for(int i=0;i<4;i++)runtime.pulse(5000,false);assertEquals(0,xp(person));
        runtime.updateLoadedWorlds(5000,Set.of());runtime.pulse(100000,false);assertEquals(0,xp(person));
        assertThrows(SQLException.class,()->runtime.enrollStudy(UUID.randomUUID(),other,town,"mining",100000));
        runtime.updateLoadedWorlds(100000,Set.of(world));runtime.observe(new FarmObservation(town,world,100001,false,0),100001);runtime.pulse(101001,false);assertEquals(0,xp(person));
        runtime.observe(new FarmObservation(town,world,101002,true,0),101002);runtime.pulse(102002,false);assertEquals(1,xp(person));
    }
    @Test void deadUnderageEmployedOrAlreadySkilledCannotEnrollAndDeathCancelsHistory()throws Exception {
        sql("UPDATE residents SET age_steps=10 WHERE id='"+other+"'");assertThrows(SQLException.class,()->runtime.enrollStudy(UUID.randomUUID(),other,town,"mining",1000));
        db.getPopulationRepository().recordSkillExperience(other,"mining",10);sql("UPDATE residents SET age_steps=300 WHERE id='"+other+"'");assertThrows(SQLException.class,()->runtime.enrollStudy(UUID.randomUUID(),other,town,"mining",1000));
        runtime.enrollStudy(course,person,town,"mining",1000);assertThrows(SQLException.class,()->runtime.enrollStudy(UUID.randomUUID(),person,town,"mining",1000));
        runtime.pulse(2000,false);sql("UPDATE residents SET status='DECEASED' WHERE id='"+person+"'");runtime.pulse(3000,false);assertEquals(1,xp(person));assertEquals("CANCELLED",saved().status());assertEquals(19,stock(Commodity.STONE));assertEquals(2,count("study_spending"));
    }
    @Test void hiredLearnerCancelsWithoutLosingEarnedSkillAndOwnerCannotEnroll()throws Exception {
        runtime.enrollStudy(course,person,town,"mining",1000);runtime.pulse(2000,false);
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,60,0,0,"Test-owned field");var business=new Business(UUID.randomUUID(),town,"Test farm",other,other,"farming",site.id(),2,0);
        db.getBusinessRepository().found(new BusinessFounding(business,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Test fixture"));db.getBusinessRepository().hire(UUID.randomUUID(),business.id(),person,0);
        assertThrows(SQLException.class,()->runtime.enrollStudy(UUID.randomUUID(),other,town,"mining",2000));
        runtime.pulse(3000,false);assertEquals("CANCELLED",saved().status());assertEquals(1,xp(person));assertEquals(19,stock(Commodity.STONE));
    }
    @Test void disabledLearningLowerCeilingAndLegacyAdultFieldLaborDoNotSpendPracticeSupplies()throws Exception {
        runtime.enrollStudy(course,person,town,"mining",1000);runtime=runtime(StudyPolicy.disabled());runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);assertEquals(0,xp(person));assertEquals(20,stock(Commodity.STONE));
        runtime=runtime(policy(2,2,2));runtime.startOnline(2000,Set.of(world));runtime.observe(new FarmObservation(town,world,2001,true,100),2001);runtime.pulse(3000,false);assertEquals(0,xp(person));assertEquals(20,stock(Commodity.STONE));
        runtime.observe(new FarmObservation(town,world,3001,true,0),3001);runtime.pulse(4000,false);assertEquals(1,xp(person));
        runtime=runtime(new StudyPolicy(true,1,1,2,8,1,2,Map.of("mining",new StudyPolicy.Course(1,Map.of(Commodity.STONE,1L)))));runtime.startOnline(4000,Set.of(world));runtime.pulse(5000,false);assertEquals(1,xp(person));assertEquals(19,stock(Commodity.STONE));
    }
    @Test void schema42UpgradeFailureRollsBackAndRetryPreservesExistingSkillsAndStock()throws Exception {
        db.getPopulationRepository().recordSkillExperience(person,"mining",1);SchemaFixtures.rewind(db.getConnection(),42);
        sql("CREATE INDEX village_active_courses ON residents(name)");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        assertEquals(1,xp(person));assertEquals(20,stock(Commodity.STONE));
        sql("DROP INDEX village_active_courses");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("resident_courses"));
        runtime.enrollStudy(course,person,town,"mining",1000);runtime.pulse(2000,false);assertEquals(2,xp(person));assertEquals("COMPLETED",saved().status());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void completedStudyCanMeetRealFounderRequirementsWithoutCreatingCapital()throws Exception {
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,1000,"Actual fixture investment");
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,0,"Actual fixture opportunity");var business=new Business(UUID.randomUUID(),town,"Qualified fixture",person,person,"mining",site.id(),2,0);
        var request=new BusinessFounding(business,site,new JobRequirements(216,Map.of("mining",2L)),new JobRequirements(216,Map.of()),100,"Actual fixture startup");
        assertThrows(SQLException.class,()->db.getBusinessRepository().found(request));runtime.enrollStudy(course,person,town,"mining",1000);
        runtime.pulse(2000,false);assertThrows(SQLException.class,()->db.getBusinessRepository().found(request));runtime.pulse(3000,false);
        assertEquals(request,db.getBusinessRepository().found(request));assertEquals(900,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());assertEquals(18,stock(Commodity.STONE));assertEquals("COMPLETED",saved().status());
    }
    @Test void smallerReviewBudgetDefersAllExistingCoursesAndOldReceiptRemainsReplayable()throws Exception {
        runtime.enrollStudy(course,person,town,"mining",1000);runtime.enrollStudy(UUID.randomUUID(),other,town,"mining",1000);
        runtime=runtime(new StudyPolicy(true,1,100,1,1,1,2,policy(2,2,2).skills()));runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);
        assertEquals(0,xp(person)+xp(other));assertEquals(20,stock(Commodity.STONE));assertEquals(0,count("study_reviews"));
        assertTrue(runtime.views().get(town).study().contains("exceed current village budget"));
        runtime=runtime(policy(2,2,2));runtime.startOnline(2000,Set.of(world));runtime.pulse(3000,false);assertEquals(1,xp(person));assertEquals(1,xp(other));
        runtime=runtime(StudyPolicy.disabled());runtime.startOnline(3000,Set.of());assertEquals("ACTIVE",runtime.enrollStudy(course,person,town,"mining",3000).status());
    }
    @Test void commerceStudyUsesRealPracticeMaterialsAndCannotInstantlyChangeCareers()throws Exception {
        runtime=runtime(StudyPolicy.prototype());runtime.startOnline(1000,Set.of(world));runtime.enrollStudy(course,person,town,"commerce",1000);
        runtime.pulse(2000,false);assertEquals(1,db.getPopulationRepository().findWorkProfile(person).orElseThrow().experience().getOrDefault("commerce",0L));
        assertEquals(19,stock(Commodity.TIMBER));assertEquals(20,stock(Commodity.STONE));assertEquals("ACTIVE",saved().status());assertEquals(10,saved().target());
        runtime.pulse(2000,false);assertEquals(19,stock(Commodity.TIMBER));assertEquals(0,count("businesses"));
    }

}
