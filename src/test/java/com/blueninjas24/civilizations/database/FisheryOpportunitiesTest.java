package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FisheryOpportunitiesTest {
    @TempDir Path directory;BusinessStartupGrantsTest fixture;
    FisheryOpportunityPolicy terms=FisheryOpportunityPolicy.prototype();
    IndustryRuntimePolicy industry=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of());
    @BeforeEach void setup()throws Exception {
        fixture=new BusinessStartupGrantsTest();fixture.directory=directory;fixture.setup();
        fixture.sql("UPDATE economy_stocks SET units=1 WHERE commodity='food'");observe(2000,0,64,true);
    }
    @AfterEach void close(){fixture.close();}
    void observe(long time,int chunk,int height,boolean complete)throws Exception {
        var columns=new ArrayList<WaterScan.Column>();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            var status=!complete&&x==15&&z==15?FisheryHabitatObservation.State.UNKNOWN:x==0?FisheryHabitatObservation.State.SUITABLE:FisheryHabitatObservation.State.ABSENT;
            columns.add(new WaterScan.Column(new FisheryObservation.Column(chunk*16+x,z),status,status==FisheryHabitatObservation.State.SUITABLE?height:null));
        }
        new WaterOpportunities(fixture.db.getConnection()).observe(fixture.town,new WaterScan.Observation(fixture.world,chunk,0,time,-64,320,2,columns));
    }
    FisheryOpportunities repo(){return new FisheryOpportunities(fixture.db.getConnection(),terms,WaterObserverPolicy.prototype(),FishingPolicy.prototype(),industry,StartupGrantPolicy.prototype(),VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.disabled());}
    FisheryOpportunities.Review review(long time)throws Exception{return repo().review(fixture.town,time,Set.of(fixture.world));}
    @Test void boundedProspectHasRealCustomerAndFundingWithoutChangingAnyAssets()throws Exception {
        var before=fixture.snapshot();var result=review(2000);assertEquals(1,result.candidates().size());var candidate=result.candidates().getFirst();
        assertEquals(3,candidate.fundedFoodNeed());assertEquals(16,candidate.water().evidence().columns().size());assertEquals(fixture.world,candidate.water().evidence().world());
        assertEquals(before,fixture.snapshot());assertEquals(result,review(2000));fixture.db.close();fixture.open();assertEquals(result,review(2000));
    }
    @Test void customersMustRemainAffordableAfterStartupRatherThanReuseTheSameCash()throws Exception {
        new TreasuryRepository(fixture.db.getConnection()).post(UUID.randomUUID(),fixture.town,-6000,"Explicit fixture spending");
        var buyers=new VillageSupplyPurchases(fixture.db.getConnection(),VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.disabled());
        var before=fixture.snapshot();assertEquals(3,buyers.demand(fixture.town,Set.of(fixture.world)).fundedUnits().get(Commodity.FOOD));
        assertTrue(fixture.grants().preview(fixture.town,"fishing",500,1000,StartupGrantPolicy.prototype()).eligible());assertTrue(review(2000).candidates().isEmpty());
        assertTrue(buyers.demandAfterSpending(fixture.town,Set.of(fixture.world),1500).fundedUnits().isEmpty());assertEquals(before,fixture.snapshot());
        assertThrows(IllegalArgumentException.class,()->buyers.demandAfterSpending(fixture.town,Set.of(fixture.world),-1));
    }
    @Test void partialStaleFutureAbsentAndTooDistantWaterCannotJustifyInvestment()throws Exception {
        assertTrue(review(1999).candidates().isEmpty());assertTrue(review(62001).candidates().isEmpty());assertTrue(repo().review(fixture.town,2000,Set.of()).candidates().isEmpty());
        observe(2001,0,64,false);assertTrue(review(2001).candidates().isEmpty());observe(2002,0,120,true);assertTrue(review(2002).candidates().isEmpty());
        observe(2003,0,64,true);observe(2004,0,64,false);observe(2004,7,64,true);assertTrue(review(2004).candidates().isEmpty());
    }
    @Test void foodProtectionAndFullReserveCannotBeBypassed()throws Exception {
        fixture.sql("UPDATE economy_stocks SET units=4 WHERE commodity='food'");assertTrue(review(2000).candidates().isEmpty());
        fixture.sql("UPDATE economy_stocks SET units=1 WHERE commodity='food'");fixture.sql("UPDATE needs_periods SET fulfilled=0");assertTrue(review(2000).candidates().isEmpty());
        fixture.sql("UPDATE needs_periods SET fulfilled=demand");terms=FisheryOpportunityPolicy.disabled();assertTrue(review(2000).candidates().isEmpty());
    }
    @Test void existingEligibleFoodPreventsNewCapacityButUnstaffedStockDoesNot()throws Exception {
        var original=fixture.producer("quarry",fixture.person,20,1000);
        var request=new BusinessStartupGrants.WorkshopRequest(original.founding(),100,Map.of(Commodity.FOOD,1L),1000,fixture.person);
        fixture.grants().startWorkshop(request,StartupGrantPolicy.prototype());fixture.sql("UPDATE industry_stock SET units=3 WHERE commodity='food'");
        assertTrue(review(2000).candidates().isEmpty());fixture.sql("UPDATE business_employment SET ended_step=1,end_reason='Explicit fixture vacancy'");
        assertEquals(3,review(2000).candidates().getFirst().fundedFoodNeed());
    }
    @Test void savedFisheryIdentityAndUnsupportedTermsCannotBeBypassed()throws Exception {
        var p=terms;terms=new FisheryOpportunityPolicy(true,8,15,60000,96,48,500,1000,256);assertTrue(review(2000).candidates().isEmpty());terms=p;
        var original=fixture.producer("fishing",fixture.person,20,1000);fixture.grants().startWorkshop(original,StartupGrantPolicy.prototype());
        assertTrue(review(2000).candidates().isEmpty());assertTrue(review(2000).explanation().contains("identity"));
    }
    @Test void serializedOwnerReviewRespectsMissingSettingsAndWorldPause()throws Exception {
        fixture.db.close();var time=new java.util.concurrent.atomic.AtomicLong(2000);
        try(var owner=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),java.util.logging.Logger.getLogger("FisheryTest"),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype(),time::get)) {
            owner.configureWater(WaterObserverPolicy.prototype());owner.configureFishing(FishingPolicy.prototype());owner.configureFisheryOpportunities(terms);
            owner.configureIndustryRuntime(industry);owner.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());owner.configureVillageSupplies(VillageSupplyPolicy.prototype());
            owner.start(false,Set.of(fixture.world)).get(5,TimeUnit.SECONDS);assertEquals(1,owner.currentFisheryOpportunities(fixture.town).get(5,TimeUnit.SECONDS).candidates().size());
            owner.updateLoadedWorlds(Set.of()).get(5,TimeUnit.SECONDS);assertTrue(owner.currentFisheryOpportunities(fixture.town).get(5,TimeUnit.SECONDS).candidates().isEmpty());
        }
        fixture.open();assertEquals(10000,fixture.cash());assertEquals(0,fixture.count("businesses"));
    }
}
