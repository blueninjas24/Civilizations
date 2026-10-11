package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticBusinessStaffingTest {
    @TempDir Path folder;DatabaseManager db;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=new UUID(0,1);
    void open()throws Exception {db=new DatabaseManager(folder.toFile(),Logger.getLogger("StaffingTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        person(owner,300,0);new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Fixture funding");
    }
    void person(UUID id,long age,long skill)throws Exception {
        db.getPopulationRepository().insertResident(new Resident(id,"Fixture resident",town,null,age,Resident.Status.ALIVE));
        db.getPopulationRepository().recordSkillExperience(id,"mining",skill);
    }
    UUID found(int number,int capacity)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,number,40,0,1000,"Explicit existing site");
        var b=new Business(new UUID(1,number),town,"Existing firm",owner,owner,"mining",site.id(),capacity,0);
        db.getBusinessRepository().found(new BusinessFounding(b,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of("mining",2L)),0,"Fixture founding"));
        return b.id();
    }
    BusinessStaffingPolicy policy(int candidates,int hires){return new BusinessStaffingPolicy(true,candidates,hires,Set.of("mining"));}
    long count(String table)throws SQLException {try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String text)throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(text);}}
    @AfterEach void close(){db.close();}
    @Test void hiresOnlyActuallyQualifiedAdultsAndHonorsCapacityAndBudget()throws Exception {
        var firm=found(1,3);person(new UUID(0,2),100,2);person(new UUID(0,3),300,1);person(new UUID(0,4),300,2);person(new UUID(0,5),300,3);
        var staffing=new AutomaticBusinessStaffing(db.getConnection(),policy(64,1));
        assertTrue(staffing.review(firm,1).contains("1 qualified"));assertEquals(new UUID(0,4),db.getBusinessRepository().currentWorkers(firm).getFirst().residentId());
        staffing.review(firm,1);staffing.review(firm,1);assertEquals(2,db.getBusinessRepository().currentWorkers(firm).size());assertEquals(2,count("business_hiring_reviews"));
        assertEquals(10000,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());assertEquals(0,count("industry_operations"));
        assertEquals(1,db.getPopulationRepository().findWorkProfile(new UUID(0,3)).orElseThrow().experience().get("mining"));
    }
    @Test void competingFirmsCannotPoachOrAssignTheSameWorkerTwice()throws Exception {
        var a=found(1,1);var b=found(2,1);var worker=new UUID(0,2);person(worker,300,2);
        var staffing=new AutomaticBusinessStaffing(db.getConnection(),policy(64,2));staffing.review(a,1);staffing.review(b,1);
        assertEquals(1,db.getBusinessRepository().currentWorkers(a).size());assertEquals(0,db.getBusinessRepository().currentWorkers(b).size());assertEquals(1,count("business_hiring_reviews"));
        var old=db.getBusinessRepository().currentWorkers(a).getFirst();db.getBusinessRepository().endEmployment(old.id(),2,"Explicit vacancy change");
        staffing.review(b,2);assertEquals(1,db.getBusinessRepository().currentWorkers(b).size());assertEquals(2,count("business_hiring_reviews"));
        assertEquals(2,db.getBusinessRepository().findEmployment(old.id()).orElseThrow().endedStep());
    }
    @Test void candidatePagesRotatePastEarlyUnqualifiedResidents()throws Exception {
        var firm=found(1,1);person(new UUID(0,2),300,0);person(new UUID(0,3),300,2);
        var staffing=new AutomaticBusinessStaffing(db.getConnection(),policy(1,1));
        staffing.review(firm,1);staffing.review(firm,1);assertEquals(0,count("business_hiring_reviews"));
        staffing.review(firm,1);assertEquals(1,count("business_hiring_reviews"));
    }
    @Test void failedAuditRollsBackJobAndRetriesTheSameCandidatePage()throws Exception {
        var firm=found(1,1);person(new UUID(0,2),300,2);var staffing=new AutomaticBusinessStaffing(db.getConnection(),policy(64,1));
        sql("CREATE TRIGGER fail_hire BEFORE INSERT ON business_hiring_reviews BEGIN SELECT RAISE(ABORT,'fixture'); END");
        assertThrows(SQLException.class,()->staffing.review(firm,1));assertEquals(0,count("business_employment"));assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER fail_hire");staffing.review(firm,1);assertEquals(1,count("business_hiring_reviews"));
    }
    @Test void absentWorldOwnerDeathAndUnsupportedIndustriesDoNotGrantHiringAuthority()throws Exception {
        var firm=found(1,1);person(new UUID(0,2),300,2);var runtime=new BusinessRuntime(db.getConnection(),8,32);runtime.configureStaffing(policy(64,1));
        assertTrue(runtime.refresh(1,Set.of()).businesses().get(firm).paused());assertEquals(0,count("business_employment"));
        db.getPopulationRepository().updateResident(new Resident(owner,"Fixture resident",town,null,300,Resident.Status.DECEASED));
        assertTrue(new AutomaticBusinessStaffing(db.getConnection(),policy(64,1)).review(firm,1).contains("Owner unavailable"));assertEquals(0,count("business_employment"));
    }
    @Test void restartDisableAndSameMonthRehirePreserveDistinctEpisodes()throws Exception {
        var firm=found(1,1);person(new UUID(0,2),300,2);var staffing=new AutomaticBusinessStaffing(db.getConnection(),policy(64,1));staffing.review(firm,1);
        var old=db.getBusinessRepository().currentWorkers(firm).getFirst();db.close();open();
        new AutomaticBusinessStaffing(db.getConnection(),BusinessStaffingPolicy.disabled()).review(firm,1);assertEquals(1,count("business_employment"));
        db.getBusinessRepository().endEmployment(old.id(),1,"Explicit ended episode");new AutomaticBusinessStaffing(db.getConnection(),policy(64,1)).review(firm,1);
        assertEquals(2,count("business_employment"));assertEquals(2,count("business_hiring_reviews"));assertNotEquals(old.id(),db.getBusinessRepository().currentWorkers(firm).getFirst().id());
    }
    @Test void unsupportedAndExpiredClosedFirmsCannotRecruit()throws Exception {
        var firm=found(1,1);person(new UUID(0,2),300,2);
        var excluded=new BusinessStaffingPolicy(true,64,1,Set.of("warehouse"));
        assertTrue(new AutomaticBusinessStaffing(db.getConnection(),excluded).review(firm,1).contains("outside"));assertEquals(0,count("business_employment"));
        db.getPopulationRepository().updateResident(new Resident(owner,"Fixture resident",town,null,300,Resident.Status.DECEASED));
        var listings=new BusinessSaleListings(db.getConnection());listings.open(firm,100,1000,0,1);listings.observe(1000,Set.of(world));
        assertTrue(new BusinessClosure(db.getConnection()).closeIfExpired(firm,1).isPresent());
        assertTrue(new AutomaticBusinessStaffing(db.getConnection(),policy(64,1)).review(firm,1).contains("Closed"));assertEquals(0,count("business_employment"));
    }
    @Test void schema51UpgradeIsEmptyAndFailureKeepsPriorJobsAndTreasury()throws Exception {
        var firm=found(1,1);person(new UUID(0,2),300,2);var manual=UUID.randomUUID();db.getBusinessRepository().hire(manual,firm,new UUID(0,2),0);
        SchemaFixtures.rewind(db.getConnection(),51);sql("CREATE TABLE business_hiring_reviews(collision TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(51,r.getInt(1));}
        sql("DROP TABLE business_hiring_reviews");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("business_hiring_reviews"));assertEquals(1,count("business_employment"));
        assertEquals(manual,db.getBusinessRepository().currentWorkers(firm).getFirst().id());assertEquals(10000,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
    }
}
