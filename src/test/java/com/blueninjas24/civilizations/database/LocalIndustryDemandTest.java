package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LocalIndustryDemandTest {
    @TempDir Path directory;
    DatabaseManager db;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=UUID.randomUUID(),worker=UUID.randomUUID(),smith,warehouse,mine;
    int offset;
    final Commodity tools=new Commodity("tools");
    final Map<Commodity,Long> sizes=Map.of(Commodity.COAL,1L,Commodity.IRON,1L,tools,1L);
    final ProductionRecipe recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),1,1);
    final WorkshopSupplyPolicy supply=new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L));
    IndustryRuntimePolicy policy(){return new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe),supply);}
    LocalIndustryDemand demand(){return new LocalIndustryDemand(db.getConnection(),policy(),BusinessOperationPolicy.prototype());}
    LocalIndustryDemand.Review review()throws Exception{return demand().review(town,Set.of(world),8);}
    void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("IndustryDemandTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(owner,"Owner",town,null,300,Resident.Status.ALIVE));
        db.getPopulationRepository().insertResident(new Resident(worker,"Smith",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Fixture funds");
        smith=business("blacksmith",new UUID(0,1),1000,100);warehouse=business("warehouse",new UUID(0,3),0,100);
        mine=business("mining",new UUID(0,4),0,100);db.getBusinessRepository().hire(UUID.randomUUID(),smith,worker,0);
        new OreTrackingRepository(db.getConnection()).begin(1000);
        new MineralOpportunities(db.getConnection()).observe(town,List.of(
            new MineralObservation(world,1000,true,Commodity.COAL,List.of(new VillageObservation.Position(0,40,0))),
            new MineralObservation(world,1000,true,Commodity.IRON,List.of(new VillageObservation.Position(2,40,0)))));
    }
    UUID business(String kind,UUID id,long cash,long capacity)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,offset++,40,0,1,"Explicit fixture site");
        var b=new Business(id,town,kind,owner,owner,kind,site.id(),2,0);var req=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(b,site,req,req,0,"Fixture"));
        db.getIndustryRepository().openStore(id,capacity,sizes,cash);return id;
    }
    void produce()throws Exception {
        var miner=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(miner,"Miner",town,null,300,Resident.Status.ALIVE));
        db.getBusinessRepository().hire(UUID.randomUUID(),mine,miner,0);var deposit=UUID.randomUUID();
        db.getIndustryRepository().discover(deposit,db.getBusinessRepository().findBusiness(mine).orElseThrow().siteId(),1,10,Map.of(Commodity.IRON,2L,Commodity.COAL,1L));
        db.getIndustryRepository().mine(UUID.randomUUID(),mine,deposit,2,0,2,2);
    }
    void transfer(UUID seller,UUID buyer,Commodity good,long units,boolean deliver)throws Exception {
        var cargo=db.getIndustryRepository().purchase(UUID.randomUUID(),seller,buyer,good,units,0,0,1,128);
        if(deliver)db.getIndustryRepository().deliver(cargo.id(),1);
    }
    void sql(String sql)throws Exception {try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    @AfterEach void close(){if(db!=null)db.close();}
    Map<String,List<String>> snapshot()throws Exception {
        var all=new TreeMap<String,List<String>>();
        for(var table:List.of("businesses","industry_stores","industry_stock","industry_operations","industry_production","local_shipments","treasury_entries","mineral_deposits","mineral_surveys","mineral_survey_members")) {
            var rows=new ArrayList<String>();try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM "+table+" ORDER BY rowid")) {
                while(r.next()){var values=new ArrayList<Object>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)values.add(r.getObject(i));rows.add(values.toString());}
            }all.put(table,rows);
        }return all;
    }
    @Test void realFundedWorkshopGapsMatchCurrentMineralsWithoutSpendingOrRegisteringAnything()throws Exception {
        var before=snapshot();var r=demand().reviewMining(town,1000,100,Set.of(world),8);
        assertTrue(r.demand().complete());assertEquals(Map.of(Commodity.IRON,2L,Commodity.COAL,1L),r.demand().uncovered());
        assertEquals(2,r.candidates().size());assertEquals(500,r.demand().customers().getFirst().estimatedCost());assertEquals(before,snapshot());
        assertEquals(r,demand().reviewMining(town,1000,100,Set.of(world),8));
    }
    @Test void heldAndInTransitGoodsSuppressPressureWithoutBecomingUsableStockEarly()throws Exception {
        produce();transfer(mine,smith,Commodity.IRON,2,true);transfer(mine,smith,Commodity.COAL,1,false);
        assertTrue(review().uncovered().isEmpty());assertEquals(0,db.getIndustryRepository().store(smith).orElseThrow().units().get(Commodity.COAL));
        assertTrue(demand().reviewMining(town,1000,100,Set.of(world),8).candidates().isEmpty());
    }
    @Test void onlyTheActuallyMissingMineralBecomesAnObservedCandidate()throws Exception {
        produce();transfer(mine,smith,Commodity.COAL,1,true);
        var r=demand().reviewMining(town,1000,100,Set.of(world),8);
        assertEquals(Map.of(Commodity.IRON,2L),r.demand().uncovered());
        assertEquals(1,r.candidates().size());assertEquals(Commodity.IRON,r.candidates().getFirst().mineral());
    }
    @Test void warehouseStockIsCountedOnceAcrossCompetingFundedWorkshops()throws Exception {
        produce();transfer(mine,warehouse,Commodity.IRON,2,true);transfer(mine,warehouse,Commodity.COAL,1,true);
        assertTrue(review().uncovered().isEmpty());
        var second=business("blacksmith",new UUID(0,2),1000,100);var employee=UUID.randomUUID();
        db.getPopulationRepository().insertResident(new Resident(employee,"Second smith",town,null,300,Resident.Status.ALIVE));db.getBusinessRepository().hire(UUID.randomUUID(),second,employee,0);
        var r=review();assertEquals(Map.of(Commodity.IRON,2L,Commodity.COAL,1L),r.uncovered());
        assertTrue(r.customers().getFirst().uncovered().isEmpty());assertFalse(r.customers().get(1).uncovered().isEmpty());
        assertEquals(2,db.getIndustryRepository().store(warehouse).orElseThrow().units().get(Commodity.IRON));
    }
    @Test void noWorkersUnaffordableBundleOrInsufficientStorageCreatesNoGap()throws Exception {
        sql("UPDATE industry_stores SET hundredths=499 WHERE business_id='"+smith+"'");assertTrue(review().uncovered().isEmpty());
        sql("UPDATE industry_stores SET hundredths=1000,capacity=2 WHERE business_id='"+smith+"'");assertTrue(review().uncovered().isEmpty());
        sql("UPDATE industry_stores SET capacity=100 WHERE business_id='"+smith+"'");
        for(var job:db.getBusinessRepository().currentWorkers(smith))db.getBusinessRepository().endEmployment(job.id(),0,"Fixture");
        assertTrue(review().uncovered().isEmpty());
    }
    @Test void deceasedOwnerOnlyCreatesPressureWhenApprovedQualifiedStaffCanContinue()throws Exception {
        db.getPopulationRepository().updateResident(new Resident(owner,"Owner",town,null,300,Resident.Status.DECEASED));
        assertFalse(review().uncovered().isEmpty());
        var stopped=new LocalIndustryDemand(db.getConnection(),policy(),BusinessOperationPolicy.disabled());assertTrue(stopped.review(town,Set.of(world),8).uncovered().isEmpty());
    }
    @Test void disabledAbsentUnknownAndBudgetOverflowAreDeferredNotReportedAsZeroDemand()throws Exception {
        assertFalse(demand().review(town,Set.of(),8).complete());assertFalse(demand().review(town,null,8).complete());
        assertFalse(new LocalIndustryDemand(db.getConnection(),IndustryRuntimePolicy.disabled(),BusinessOperationPolicy.prototype()).review(town,Set.of(world),8).complete());
        business("blacksmith",new UUID(0,2),1000,100);assertFalse(demand().review(town,Set.of(world),1).complete());
        assertThrows(IllegalArgumentException.class,()->demand().review(town,Set.of(world),65));
    }
    @Test void missingCurrentEvidenceOrTrackingGapNeverTurnsRealDemandIntoMineCandidates()throws Exception {
        assertTrue(demand().reviewMining(town,1101,100,Set.of(world),8).candidates().isEmpty());
        new OreTrackingRepository(db.getConnection()).markGap(1001);
        var r=demand().reviewMining(town,1001,100,Set.of(world),8);assertFalse(r.demand().uncovered().isEmpty());assertTrue(r.candidates().isEmpty());
    }
    @Test void restartAndActualOwnerBridgeRetainDemandWithoutAllocatingAssets()throws Exception {
        var before=snapshot();db.close();db=null;var clock=new AtomicLong(1000);
        // Fixture DB had an active mutation owner; close its coverage record explicitly before the real owner starts.
        open();new OreTrackingRepository(db.getConnection()).finish(1000,true);db.close();db=null;
        try(var session=new PopulationSession(directory.toFile(),Logger.getLogger("IndustryDemandTest"),PopulationSession.Settings.prototype(),clock::get)) {
            session.configureOreTracking();session.configureIndustryRuntime(policy());session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
            var r=session.miningInputDemand(town,100,8).get(5,TimeUnit.SECONDS);assertEquals(2,r.candidates().size());
            session.updateLoadedWorlds(Set.of()).get(5,TimeUnit.SECONDS);
            assertFalse(session.miningInputDemand(town,100,8).get(5,TimeUnit.SECONDS).demand().complete());
        }
        open();assertEquals(before,snapshot());
    }
}
