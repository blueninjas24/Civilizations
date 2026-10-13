package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.population.JobRequirements;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FisheryStartupTest {
    @TempDir Path directory;
    BusinessStartupGrantsTest fixture;
    BusinessStartupGrants.WorkshopRequest request;
    FisheryObservation habitat;
    @BeforeEach void setup()throws Exception {
        fixture=new BusinessStartupGrantsTest();fixture.directory=directory;fixture.setup();
        fixture.db.getPopulationRepository().recordSkillExperience(fixture.person,"fishing",10);
        var original=fixture.mine(fixture.person,0).founding();var b=original.business();
        var firm=new Business(b.id(),b.settlementId(),"Fixture fishery",b.founderId(),b.ownerId(),"fishing",b.siteId(),2,0);
        request=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(firm,original.site(),new JobRequirements(216,Map.of("fishing",10L)),new JobRequirements(216,Map.of("fishing",1L)),500,"Explicit revalidated fixture"),100,Map.of(Commodity.FOOD,1L),1000,fixture.person);
        habitat=new FisheryObservation(fixture.world,10,true,java.util.stream.IntStream.range(0,16).mapToObj(x->new FisheryObservation.Column(x,0)).toList());
    }
    @AfterEach void close(){fixture.close();}
    BusinessStartupGrants.Fishery start()throws Exception{return fixture.grants().startFishery(request,habitat,FishingPolicy.prototype(),StartupGrantPolicy.prototype());}
    void unchanged()throws Exception{fixture.unchanged();for(var table:List.of("fishery_resources","fishery_columns","fishery_work"))assertEquals(0,fixture.count(table));}
    @Test void oneTransactionPaysRealCashAndSavesQualifiedWorkerEmptyStoreAndHabitat()throws Exception {
        var saved=start();assertEquals(8500,fixture.cash());assertEquals(1000,saved.startup().store().hundredths());assertEquals(0,saved.startup().store().units().get(Commodity.FOOD));
        assertEquals(128,saved.resource().stock().capacity());assertEquals(96,saved.resource().stock().available());assertEquals(16,fixture.count("fishery_columns"));assertEquals(1,fixture.count("startup_grants"));assertEquals(1,fixture.count("business_employment"));
        fixture.db.close();fixture.open();assertEquals(saved,fixture.grants().startFishery(request,habitat,FishingPolicy.disabled(),StartupGrantPolicy.disabled()));assertEquals(8500,fixture.cash());assertEquals(1,fixture.count("startup_grants"));
    }
    @Test void insufficientSkillAndFoodProtectionCannotLeaveHalfFoundedFisheries()throws Exception {
        fixture.db.getPopulationRepository().recordSkillExperience(fixture.person,"fishing",9);assertThrows(SQLException.class,this::start);unchanged();
        fixture.db.getPopulationRepository().recordSkillExperience(fixture.person,"fishing",10);fixture.sql("UPDATE needs_periods SET fulfilled=0");assertThrows(BusinessStartupGrants.Declined.class,this::start);unchanged();
    }
    @Test void replayAfterActualCatchPreservesSpentHabitatAndOwnedFoodUnderRetunes()throws Exception {
        var opened=start();var id=request.founding().business().id();var industry=fixture.db.getIndustryRepository();
        var catchReceipt=industry.fish(UUID.randomUUID(),id,4,1,FishingPolicy.prototype());assertEquals(4,catchReceipt.quantity());
        var before=new FisheryRepository(fixture.db.getConnection()).find(request.founding().site().id()).orElseThrow();assertTrue(before.stock().available()<opened.resource().stock().available());
        fixture.db.close();fixture.open();var replay=fixture.grants().startFishery(request,habitat,new FishingPolicy(false,16,20,4096,100,10000,100,100),StartupGrantPolicy.disabled());
        assertEquals(before,replay.resource());assertEquals(4,replay.startup().store().units().get(Commodity.FOOD));assertEquals(8500,fixture.cash());assertEquals(1,fixture.count("business_employment"));
    }
    @Test void wrongWorldPartialOrTooSmallWaterRollsBackPaidFirmAndJob()throws Exception {
        habitat=new FisheryObservation(UUID.randomUUID(),10,true,habitat.columns());assertThrows(SQLException.class,this::start);unchanged();
        habitat=new FisheryObservation(fixture.world,10,false,habitat.columns());assertThrows(SQLException.class,this::start);unchanged();
        habitat=new FisheryObservation(fixture.world,10,true,List.of(new FisheryObservation.Column(0,0)));assertThrows(IllegalArgumentException.class,this::start);unchanged();
    }
    @Test void lateColumnFailureRollsBackGrantTreasurySiteWorkerAndResourceThenRetries()throws Exception {
        fixture.sql("CREATE TRIGGER reject_fishery_column BEFORE INSERT ON fishery_columns WHEN NEW.x=15 BEGIN SELECT RAISE(ABORT,'late fixture failure'); END");
        assertThrows(SQLException.class,this::start);unchanged();fixture.sql("DROP TRIGGER reject_fishery_column");start();assertEquals(8500,fixture.cash());
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void changedFootprintReplayCannotPayAgainOrReplaceOldSupports()throws Exception {
        var saved=start();habitat=new FisheryObservation(fixture.world,20,true,java.util.stream.IntStream.range(1,17).mapToObj(x->new FisheryObservation.Column(x,0)).toList());
        assertThrows(SQLException.class,this::start);assertEquals(8500,fixture.cash());assertEquals(16,fixture.count("fishery_columns"));assertEquals(saved.resource(),new FisheryRepository(fixture.db.getConnection()).find(request.founding().site().id()).orElseThrow());
    }
    @Test void healthyReserveCustomerCanFundStartCatchAndPayForRealFoodWithoutCrisis()throws Exception {
        fixture.sql("UPDATE economy_stocks SET units=1 WHERE commodity='food'");
        var buyers=new VillageSupplyPurchases(fixture.db.getConnection(),VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.disabled());
        assertEquals(Map.of(Commodity.FOOD,3L),buyers.demand(fixture.town,Set.of(fixture.world)).fundedUnits());
        start();var id=request.founding().business().id();fixture.db.getIndustryRepository().fish(UUID.randomUUID(),id,4,1,FishingPolicy.prototype());
        buyers.purchase(new VillageSupplyPurchases.Request(UUID.randomUUID(),fixture.town,id,Commodity.FOOD,3,1),Set.of(fixture.world));
        assertEquals(7900,fixture.cash());assertEquals(1600,fixture.db.getIndustryRepository().store(id).orElseThrow().hundredths());
        assertEquals(1,fixture.db.getIndustryRepository().store(id).orElseThrow().units().get(Commodity.FOOD));
        assertEquals(Map.of(),buyers.demand(fixture.town,Set.of(fixture.world)).fundedUnits());
    }

}
