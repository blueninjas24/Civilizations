package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.Resident;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BusinessCompetitionTest {
    @TempDir Path directory;LocalIndustryDemandTest fixture;UUID second;
    BusinessStaffingPolicy staffing=new BusinessStaffingPolicy(true,64,1,Set.of("mining"));
    @BeforeEach void setup()throws Exception {
        fixture=new LocalIndustryDemandTest();fixture.directory=directory;fixture.setup();
        second=fixture.business("mining",new UUID(0,5),0,100);
        deposit(fixture.mine,Commodity.COAL);deposit(second,Commodity.IRON);
    }
    @AfterEach void close(){if(fixture!=null)fixture.close();}
    void deposit(UUID business,Commodity good)throws Exception {
        fixture.db.getIndustryRepository().discover(UUID.randomUUID(),fixture.db.getBusinessRepository().findBusiness(business).orElseThrow().siteId(),1,100,Map.of(good,1L));
    }
    BusinessCompetition competition(){return new BusinessCompetition(fixture.db.getConnection(),BusinessAllocationPolicy.prototype(),staffing,fixture.policy(),BusinessOperationPolicy.prototype(),fixture.demand(),
            new VillageSupplyPurchases(fixture.db.getConnection(),VillageSupplyPolicy.disabled(),StudyPolicy.disabled(),BusinessOperationPolicy.prototype()));}
    String review(long now)throws Exception{return competition().review(fixture.town,1,now,Set.of(fixture.world),new AutomaticBusinessStaffing(fixture.db.getConnection(),staffing));}
    long count(String table)throws Exception{try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    @Test void strongestRealFundedDemandWinsOverEarlierUuidWithoutSpendingOrChangingExistingJobs()throws Exception {
        var smithJob=fixture.db.getBusinessRepository().currentWorkers(fixture.smith);long money=new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths();
        assertTrue(review(1000).contains("funded unmet value=400"));
        assertEquals(1,fixture.db.getBusinessRepository().currentWorkers(second).size());assertTrue(fixture.db.getBusinessRepository().currentWorkers(fixture.mine).isEmpty());
        assertEquals(smithJob,fixture.db.getBusinessRepository().currentWorkers(fixture.smith));
        assertEquals(money,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());
        assertEquals(1,count("business_allocation_reviews"));assertEquals(0,count("industry_operations"));
    }
    @Test void equalScoresRotateAfterRestartAndRespectReviewInterval()throws Exception {
        fixture.sql("UPDATE mineral_yields SET commodity='coal' WHERE deposit_id IN (SELECT id FROM mineral_deposits WHERE site_id=(SELECT site_id FROM businesses WHERE id='"+second+"'))");
        review(1000);assertEquals(1,fixture.db.getBusinessRepository().currentWorkers(fixture.mine).size());
        fixture.db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"New available adult",fixture.town,null,300,Resident.Status.ALIVE));
        assertTrue(review(2000).contains("interval"));assertEquals(1,count("business_allocation_reviews"));
        fixture.db.close();fixture.open();review(6000);
        assertEquals(1,fixture.db.getBusinessRepository().currentWorkers(second).size());assertEquals(2,count("business_allocation_reviews"));
    }
    @Test void failedSelectionAuditRollsBackHiringAndFairnessCursorAndRetriesSameChoice()throws Exception {
        fixture.sql("CREATE TRIGGER reject_allocation BEFORE INSERT ON business_allocation_reviews BEGIN SELECT RAISE(ABORT,'audit failure'); END");
        assertThrows(SQLException.class,()->review(1000));assertTrue(fixture.db.getBusinessRepository().currentWorkers(second).isEmpty());
        assertEquals(0,count("business_allocation_state"));assertEquals(0,count("business_hiring_reviews"));
        fixture.sql("DROP TRIGGER reject_allocation");review(1000);assertEquals(1,fixture.db.getBusinessRepository().currentWorkers(second).size());
    }
    @Test void noFundingUnknownWorldOrExhaustedResourcesCannotJustifyHiring()throws Exception {
        fixture.sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+fixture.smith+"'");review(1000);assertEquals(0,count("business_hiring_reviews"));
        fixture.sql("UPDATE industry_stores SET hundredths=1000 WHERE business_id='"+fixture.smith+"'");
        assertTrue(competition().review(fixture.town,1,1000,Set.of(),new AutomaticBusinessStaffing(fixture.db.getConnection(),staffing)).contains("absent"));
        fixture.sql("UPDATE mineral_deposits SET remaining_batches=0");review(1000);assertEquals(0,count("business_hiring_reviews"));
    }
    @Test void completeVillageComparisonSelectsOutsideRuntimeBusinessPage()throws Exception {
        var runtime=new BusinessRuntime(fixture.db.getConnection(),1,32);runtime.configureStaffing(staffing);runtime.configureCompetition(competition());
        runtime.refresh(1,Set.of(fixture.world),1000,false);
        assertEquals(1,fixture.db.getBusinessRepository().currentWorkers(second).size());
        assertEquals(1,count("business_allocation_reviews"));
    }
}
