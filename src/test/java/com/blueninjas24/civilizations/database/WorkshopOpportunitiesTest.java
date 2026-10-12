package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorkshopOpportunitiesTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),farmer=new UUID(0,1),prospect=new UUID(0,2);
    final FarmToolUpkeepPolicy upkeep=new FarmToolUpkeepPolicy(true,1,2,50,8);
    final WorkshopStarterPolicy starter=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L,Commodity.COAL,600L));
    final Map<Commodity,Long> sizes=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,Commodity.STONE,1L,ToolUpkeepPolicy.TOOLS,1L);
    EconomySettings settings;IndustryRuntimePolicy industry;
    void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("WorkshopOpportunityTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Workshop prospect fixture","Fixture",0,64,0,2,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var resident:List.of(farmer,prospect))db.getPopulationRepository().insertResident(new Resident(resident,"Fixture resident",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit fixture treasury");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L,Commodity.IRON,10L,Commodity.COAL,10L),
            Map.of(Commodity.FOOD,BigDecimal.valueOf(2),Commodity.IRON,BigDecimal.valueOf(12),Commodity.COAL,BigDecimal.valueOf(6)),bands);
        var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());
        economy.configureFarmTools(upkeep,FarmToolBudgetPolicy.prototype(),IndustryPolicy.prototype());economy.configureWorkLearning(WorkLearningPolicy.prototype());
        economy.observe(new FarmObservation(town,world,1000,true,16,new VillageObservation.Position(0,64,0)),1000);
        economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);economy.pulse(3000,false);
        var mining=new IndustryPolicy(150,8,32,1024,1,128,Map.of(Commodity.COAL,1L,Commodity.STONE,3L),sizes);
        industry=new IndustryRuntimePolicy(true,1000,8,mining,Map.of("smith_tools",ProductionRecipe.smithTools()),new WorkshopSupplyPolicy(true,1,16,starter.prices()));
        markers(3000,true,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(10,64,0),WorkplaceObservation.Kind.SMITHING_TABLE)));
    }
    void markers(long now,boolean complete,List<WorkplaceObservation.Marker> markers)throws SQLException {
        new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(world,0,0,now,complete,markers));
    }
    WorkshopOpportunities review(WorkshopOpportunityPolicy policy,StartupGrantPolicy grants,WorkshopStarterPolicy starter) {
        return new WorkshopOpportunities(db.getConnection(),policy,industry,BusinessOperationPolicy.prototype(),grants,starter,upkeep,FarmToolBudgetPolicy.prototype());
    }
    WorkshopOpportunities.Review review()throws SQLException{return review(WorkshopOpportunityPolicy.prototype(),StartupGrantPolicy.prototype(),starter).review(town,3000,Set.of(world));}
    void sql(String query)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    Map<String,List<String>> snapshot()throws SQLException {
        var names=new ArrayList<String>();var result=new TreeMap<String,List<String>>();
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")){while(r.next())names.add(r.getString(1));}
        for(var name:names) {var rows=new ArrayList<String>();try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM "+name)) {
            int count=r.getMetaData().getColumnCount();while(r.next()){var fields=new ArrayList<String>();for(int n=1;n<=count;n++)fields.add(r.getString(n));rows.add(fields.toString());}
        }Collections.sort(rows);result.put(name,rows);}return result;
    }
    @AfterEach void close(){if(db!=null)db.close();}
    @Test void actualWorkingFarmRealGrantAndSurplusProduceOnlyReadOnlyFirstWorkshopEvidence()throws Exception {
        var before=snapshot();var result=review();assertEquals(1,result.candidates().size(),result.explanation());
        var candidate=result.candidates().getFirst();assertEquals(FarmEnterprises.businessId(town),candidate.customer());assertEquals(1,candidate.fundedToolNeed());
        assertEquals(3000,candidate.starter().price());assertEquals(Map.of(Commodity.IRON,2L,Commodity.COAL,1L),candidate.starter().goods());
        assertEquals(before,snapshot());assertEquals(0,db.getPopulationRepository().findWorkProfile(prospect).orElseThrow().experience().getOrDefault("smithing",0L));
    }
    @Test void absentStaleFuturePartialAndRemovedMarkersNeverBecomeInvestmentCandidates()throws Exception {
        var engine=review(WorkshopOpportunityPolicy.prototype(),StartupGrantPolicy.prototype(),starter);
        assertTrue(engine.review(town,3000,Set.of()).candidates().isEmpty());assertTrue(engine.review(town,70000,Set.of(world)).candidates().isEmpty());
        assertTrue(engine.review(town,2999,Set.of(world)).candidates().isEmpty());markers(3500,false,List.of());assertTrue(engine.review(town,3500,Set.of(world)).candidates().isEmpty());
        markers(4000,true,List.of());assertTrue(engine.review(town,4000,Set.of(world)).candidates().isEmpty());
    }
    @Test void missingFoodProofFundingOrSurplusDefersWithoutInventingAssets()throws Exception {
        sql("UPDATE needs_periods SET fulfilled=0 WHERE period=2");assertTrue(review().candidates().isEmpty());sql("UPDATE needs_periods SET fulfilled=demand WHERE period=2");
        assertTrue(review(WorkshopOpportunityPolicy.prototype(),StartupGrantPolicy.disabled(),starter).review(town,3000,Set.of(world)).candidates().isEmpty());
        sql("UPDATE economy_stocks SET units=3 WHERE commodity='iron'");var before=snapshot();var result=review();assertTrue(result.candidates().isEmpty());assertTrue(result.explanation().contains("surplus"));assertEquals(before,snapshot());
    }
    @Test void markerBudgetRangeKindAndRecipeStorageAreConservative()throws Exception {
        var narrow=new WorkshopOpportunityPolicy(true,8,60000,9,48,500,4000,1024);
        assertTrue(review(narrow,StartupGrantPolicy.prototype(),starter).review(town,3000,Set.of(world)).candidates().isEmpty());
        markers(3500,true,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(10,64,0),WorkplaceObservation.Kind.BARREL)));
        assertTrue(review(WorkshopOpportunityPolicy.prototype(),StartupGrantPolicy.prototype(),starter).review(town,3500,Set.of(world)).candidates().isEmpty());
        markers(4000,true,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(10,64,0),WorkplaceObservation.Kind.SMITHING_TABLE),new WorkplaceObservation.Marker(new VillageObservation.Position(11,64,0),WorkplaceObservation.Kind.SMITHING_TABLE)));
        var bounded=new WorkshopOpportunityPolicy(true,1,60000,96,48,500,4000,1024);
        var result=review(bounded,StartupGrantPolicy.prototype(),starter).review(town,4000,Set.of(world));assertTrue(result.candidates().isEmpty());assertTrue(result.explanation().contains("budget"));
        var small=new WorkshopOpportunityPolicy(true,8,60000,96,48,500,4000,2);
        assertTrue(review(small,StartupGrantPolicy.prototype(),starter).review(town,4000,Set.of(world)).candidates().isEmpty());
    }
    @Test void existingFirmIdentityPreventsFirstFounderDuplicationEvenWithoutStock()throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,20,64,0,3000,"Explicit existing fixture workshop");var requirements=new JobRequirements(216,Map.of());
        var business=new Business(UUID.randomUUID(),town,"Existing smith",prospect,prospect,"blacksmith",site.id(),1,0);
        db.getBusinessRepository().found(new BusinessFounding(business,site,requirements,requirements,0,"Explicit existing opportunity"));
        var before=snapshot();assertTrue(review().explanation().contains("identity retained"));assertEquals(before,snapshot());
    }
    @Test void ownedToolkitServiceAndChangedPopulationRemoveCustomerPressure()throws Exception {
        var farm=FarmEnterprises.businessId(town);sql("UPDATE business_tools SET service_periods=2 WHERE business_id='"+farm+"'");
        assertTrue(review().candidates().isEmpty());sql("UPDATE business_tools SET service_periods=0 WHERE business_id='"+farm+"'");
        db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"New arrival",town,null,300,Resident.Status.ALIVE));assertTrue(review().candidates().isEmpty());
    }
    @Test void existingRealWarehouseStockCoversTheCustomerWithoutAnotherWorkshop()throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,20,64,0,3000,"Explicit existing warehouse");var requirements=new JobRequirements(216,Map.of());
        var business=new Business(UUID.randomUUID(),town,"Existing warehouse",prospect,prospect,"warehouse",site.id(),1,0);
        db.getBusinessRepository().found(new BusinessFounding(business,site,requirements,requirements,0,"Explicit fixture"));db.getBusinessRepository().hire(UUID.randomUUID(),business.id(),prospect,0);
        db.getIndustryRepository().openStore(business.id(),10,Map.of(ToolUpkeepPolicy.TOOLS,1L),0);
        // Explicit previously delivered fixture stock; the review itself must never create it.
        sql("UPDATE industry_stock SET units=1 WHERE business_id='"+business.id()+"' AND commodity='tools'");
        var before=snapshot();var result=review();assertTrue(result.candidates().isEmpty());assertTrue(result.explanation().contains("Existing local supply"));assertEquals(before,snapshot());
    }
    @Test void outputCatalogAndLegacyDisabledInvestmentCannotBeBypassed()throws Exception {
        var mining=new IndustryPolicy(150,8,32,1024,1,128,Map.of(Commodity.COAL,1L,Commodity.STONE,3L),Map.of(Commodity.IRON,1L,Commodity.COAL,1L,Commodity.STONE,1L));
        industry=new IndustryRuntimePolicy(true,1000,8,mining,industry.recipes(),industry.supply());
        var result=review();assertTrue(result.candidates().isEmpty());assertTrue(result.explanation().contains("output"));
        assertTrue(review(WorkshopOpportunityPolicy.disabled(),StartupGrantPolicy.prototype(),starter).review(town,3000,Set.of(world)).candidates().isEmpty());
    }
    @Test void sqlOwnerRestartRevalidatesWorldAndRecognitionWithoutSpendingOrTraining()throws Exception {
        var before=snapshot();db.close();db=null;var clock=new java.util.concurrent.atomic.AtomicLong(3000);
        var original=PopulationSession.Settings.prototype();var settings=new PopulationSession.Settings(0,original.millisPerMonth(),original.maximumOfflineMonths(),original.monthsPerBatch(),original.population(),original.migration(),original.recognition(),original.enrollment());
        try(var session=new PopulationSession(directory.toFile(),Logger.getLogger("WorkshopOwnerTest"),settings,clock::get,this.settings,FarmObservation.Policy.prototype(),VillagePriorities.Policy.prototype(),OpeningFunds.disabled())) {
            session.configureWorkplaces(WorkplaceObserverPolicy.prototype());session.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),starter);
            session.configureIndustryRuntime(industry);session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());
            session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureFarmTools(upkeep,FarmToolBudgetPolicy.prototype());
            session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureWorkLearning(WorkLearningPolicy.prototype());
            session.start(false,Set.of()).get(5,java.util.concurrent.TimeUnit.SECONDS);assertTrue(session.currentWorkshopOpportunities(town).get(5,java.util.concurrent.TimeUnit.SECONDS).candidates().isEmpty());
            session.updateLoadedWorlds(Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            var result=session.currentWorkshopOpportunities(town).get(5,java.util.concurrent.TimeUnit.SECONDS);assertEquals(1,result.candidates().size(),result.explanation());
        }
        open();assertEquals(before.get("businesses"),snapshot().get("businesses"));assertEquals(before.get("treasury_entries"),snapshot().get("treasury_entries"));assertEquals(before.get("resident_skills"),snapshot().get("resident_skills"));
    }
}
