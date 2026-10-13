package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/** Trusted backend composition only: these fixture recipes do not enable automatic founding. */
class ProcessingWorkshopStartupTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),person=UUID.randomUUID(),business=UUID.randomUUID(),orderId=UUID.randomUUID();
    final Commodity grain=new Commodity("grain"),furniture=new Commodity("furniture");
    BusinessStartupGrants.WorkshopRequest request;WorkshopStarterSupplies.Request order;
    ProductionRecipe recipe;WorkshopStarterPolicy starter;WorkplaceOperations.Anchor anchor;
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("ProcessingWorkshopStartupTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Processing fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Fixture founder",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture treasury");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var stock=Map.of(Commodity.FOOD,100L,grain,10L,Commodity.TIMBER,10L,Commodity.STONE,10L);
        var prices=new HashMap<Commodity,BigDecimal>();stock.keySet().forEach(g->prices.put(g,BigDecimal.ONE));
        var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),stock,prices,bands);
        var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
    }
    @AfterEach void close(){if(db!=null)db.close();}
    void prepare(WorkplaceObservation.Kind kind) {
        var inputs=kind==WorkplaceObservation.Kind.SMOKER?Map.of(grain,3L):Map.of(Commodity.TIMBER,2L,Commodity.STONE,1L);
        var outputs=kind==WorkplaceObservation.Kind.SMOKER?Map.of(Commodity.FOOD,3L):Map.of(furniture,1L);
        recipe=new ProductionRecipe("fixture_processing",kind.industry(),inputs,outputs,1,1);
        var prices=new HashMap<Commodity,Long>();inputs.keySet().forEach(g->prices.put(g,100L));
        starter=new WorkshopStarterPolicy(true,1,4000,2,prices);
        var space=new HashMap<Commodity,Long>();inputs.keySet().forEach(g->space.put(g,1L));outputs.keySet().forEach(g->space.put(g,1L));
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,64,0,1000,"Explicit processing fixture marker");
        var firm=new Business(business,town,"Fixture processor",person,person,kind.industry(),site.id(),2,0);
        var requirements=new JobRequirements(216,Map.of());
        request=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(firm,site,requirements,requirements,500,"Trusted fixture opportunity"),100,space,4000,person);
        order=new WorkshopStarterSupplies.Request(orderId,business,recipe,1,0,1,128);
        anchor=new WorkplaceOperations.Anchor(business,world,new VillageObservation.Position(0,64,0),kind);
    }
    BusinessStartupGrants.Workshop start(StartupGrantPolicy grants,boolean present,Set<UUID> worlds)throws SQLException {
        return new BusinessStartupGrants(db.getConnection()).startMarkerWorkshopWithSupplies(request,grants,order,starter,worlds,new WorkplaceOperations.Confirmation(anchor,present,1000),1000);
    }
    long cash()throws SQLException{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    IndustryRepository.Store store()throws SQLException{return db.getIndustryRepository().store(business).orElseThrow();}
    long units(Commodity good)throws SQLException{return new CommodityRepository(db.getConnection()).read(town).orElseThrow().state().stocks().get(good).units();}
    void sql(String query)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    Map<String,List<String>> snapshot()throws SQLException {
        var names=new ArrayList<String>();var result=new TreeMap<String,List<String>>();
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")){while(r.next())names.add(r.getString(1));}
        for(var name:names) {
            var rows=new ArrayList<String>();try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM \""+name.replace("\"","\"\"")+"\"")) {
                while(r.next()){var row=new ArrayList<Object>();for(int c=1;c<=r.getMetaData().getColumnCount();c++)row.add(r.getObject(c));rows.add(row.toString());}
            }Collections.sort(rows);result.put(name,rows);
        }return result;
    }
    @ParameterizedTest @EnumSource(value=WorkplaceObservation.Kind.class,names={"SMOKER","CRAFTING_TABLE"})
    void paidStartupDeliveryAndProcessingConserveRealCashAndIngredientsAcrossRestart(WorkplaceObservation.Kind kind)throws Exception {
        prepare(kind);long food=units(Commodity.FOOD);var started=start(StartupGrantPolicy.prototype(),true,Set.of(world));
        assertEquals(5800,cash());assertEquals(3700,started.store().hundredths());assertEquals(9500,cash()+store().hundredths());
        assertEquals(4500,started.grant().startup()+started.grant().capital());assertEquals(2500,started.grant().protectedCash());
        assertEquals(3,store().reservedIncomingSpace());assertEquals(food,units(Commodity.FOOD));
        for(var input:recipe.inputs().entrySet()){assertEquals(10-input.getValue(),units(input.getKey()));assertEquals(0,store().units().get(input.getKey()));}
        var before=snapshot();db.close();open();assertEquals(before,snapshot());
        assertEquals(started,start(StartupGrantPolicy.disabled(),true,Set.of(world)));assertEquals(before,snapshot());
        db.getConnection().setAutoCommit(false);
        try{assertEquals(0,new WorkshopStarterSupplies(db.getConnection()).deliverRows(business,0));assertEquals(1,new WorkshopStarterSupplies(db.getConnection()).deliverRows(business,1));db.getConnection().commit();}finally{db.getConnection().setAutoCommit(true);}
        var operation=UUID.randomUUID();assertEquals(1,db.getIndustryRepository().process(operation,business,recipe,1,1).receipt().quantity());
        for(var input:recipe.inputs().keySet())assertEquals(0,store().units().get(input));
        for(var output:recipe.outputs().entrySet())assertEquals(output.getValue(),store().units().get(output.getKey()));
        assertEquals(0,store().reservedIncomingSpace());assertEquals(food,units(Commodity.FOOD));assertEquals(9500,cash()+store().hundredths());
        before=snapshot();assertEquals(1,db.getIndustryRepository().process(operation,business,recipe,1,1).receipt().quantity());assertEquals(before,snapshot());
    }
    @ParameterizedTest @EnumSource(value=WorkplaceObservation.Kind.class,names={"SMOKER","CRAFTING_TABLE"})
    void finalAnchorFailureRollsBackEveryTableAndAllowsSafeRetry(WorkplaceObservation.Kind kind)throws Exception {
        prepare(kind);sql("CREATE TRIGGER reject_processing_anchor BEFORE INSERT ON business_workplace_anchors BEGIN SELECT RAISE(ABORT,'fixture final attachment failure'); END");
        var before=snapshot();assertThrows(SQLException.class,()->start(StartupGrantPolicy.prototype(),true,Set.of(world)));assertEquals(before,snapshot());assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER reject_processing_anchor");assertNotNull(start(StartupGrantPolicy.prototype(),true,Set.of(world)));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @ParameterizedTest @EnumSource(value=WorkplaceObservation.Kind.class,names={"SMOKER","CRAFTING_TABLE"})
    void markerAbsenceWorldAbsenceAndAdminOptOutNeverSpendOrCreateIdentity(WorkplaceObservation.Kind kind)throws Exception {
        prepare(kind);var before=snapshot();
        assertThrows(SQLException.class,()->start(StartupGrantPolicy.prototype(),false,Set.of(world)));assertEquals(before,snapshot());
        assertThrows(SQLException.class,()->start(StartupGrantPolicy.prototype(),true,Set.of()));assertEquals(before,snapshot());
        var p=StartupGrantPolicy.prototype();var optedOut=new StartupGrantPolicy(true,p.maximumGrant(),p.perPeriodLimit(),p.lifetimeLimit(),p.minimumTreasury(),p.foodReservePeriods(),p.hundredthsPerFoodUnit(),Set.of("blacksmith"));
        assertThrows(SQLException.class,()->start(optedOut,true,Set.of(world)));assertEquals(before,snapshot());
        sql("UPDATE needs_periods SET fulfilled=0");before=snapshot();assertThrows(SQLException.class,()->start(p,true,Set.of(world)));assertEquals(before,snapshot());
    }
    @ParameterizedTest @EnumSource(value=WorkplaceObservation.Kind.class,names={"SMOKER","CRAFTING_TABLE"})
    void startupGrantDoesNotWaiveSavedFounderOrWorkerQualifications(WorkplaceObservation.Kind kind)throws Exception {
        prepare(kind);var old=request.founding();var requirement=new JobRequirements(216,Map.of("fixture_processing",1L));
        request=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(old.business(),old.site(),requirement,requirement,old.startupHundredths(),old.reason()),request.capacity(),request.space(),request.capital(),request.worker());
        var before=snapshot();assertThrows(SQLException.class,()->start(StartupGrantPolicy.prototype(),true,Set.of(world)));assertEquals(before,snapshot());
        db.getPopulationRepository().recordSkillExperience(person,"fixture_processing",1);assertNotNull(start(StartupGrantPolicy.prototype(),true,Set.of(world)));
    }
    @Test void bakeryFoodHasARealPaidVillageCustomerAndPausedWorkplaceCannotSellIt()throws Exception {
        prepare(WorkplaceObservation.Kind.SMOKER);start(StartupGrantPolicy.prototype(),true,Set.of(world));
        db.getConnection().setAutoCommit(false);
        try{new WorkshopStarterSupplies(db.getConnection()).deliverRows(business,1);db.getConnection().commit();}finally{db.getConnection().setAutoCommit(true);}
        db.getIndustryRepository().process(UUID.randomUUID(),business,recipe,1,1);
        // Explicit fixture depletion creates a real reserve need; no fake customer consumes goods.
        sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");
        var buyers=new VillageSupplyPurchases(db.getConnection(),VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.disabled());
        assertEquals(4,buyers.demand(town,Set.of(world)).fundedUnits().get(Commodity.FOOD));
        var purchase=new VillageSupplyPurchases.Request(UUID.randomUUID(),town,business,Commodity.FOOD,3,1);
        var markers=new WorkplaceOperations(db.getConnection());markers.confirm(List.of(new WorkplaceOperations.Confirmation(anchor,false,1001)),1001,Set.of(world));
        var before=snapshot();assertThrows(SQLException.class,()->buyers.purchase(purchase,Set.of(world)));assertEquals(before,snapshot());
        markers.confirm(List.of(new WorkplaceOperations.Confirmation(anchor,true,1002)),1002,Set.of(world));
        var paid=buyers.purchase(purchase,Set.of(world));assertEquals(600,paid.price());assertEquals(5200,cash());assertEquals(4300,store().hundredths());
        assertEquals(0,store().units().get(Commodity.FOOD));assertEquals(3,buyers.pending(town,Commodity.FOOD));assertEquals(0,units(Commodity.FOOD));
        before=snapshot();assertEquals(paid,buyers.purchase(purchase,Set.of(world)));assertEquals(before,snapshot());assertEquals(9500,cash()+store().hundredths());
    }
    BakeryOpportunities prospects(WorkshopOpportunityPolicy limits,VillageSupplyPolicy customers,IndustryRuntimePolicy runtime) {
        return new BakeryOpportunities(db.getConnection(),limits,runtime,StartupGrantPolicy.prototype(),starter,customers,StudyPolicy.disabled(),BusinessOperationPolicy.disabled());
    }
    IndustryRuntimePolicy bakeryRuntime() {
        var original=IndustryPolicy.prototype();var space=new HashMap<>(original.storageSpacePerUnit());space.putAll(request.space());
        var storage=new IndustryPolicy(original.depositBatchesPerObservedOre(),original.batchesPerWorker(),original.siteBatchCapacity(),original.storageCapacity(),original.localDeliveryPeriods(),original.localHaulUnitsPerPeriod(),original.miningBundle(),space);
        return new IndustryRuntimePolicy(true,1000,8,storage,Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,starter.prices()));
    }
    void bakeryMarker(long time,boolean complete)throws SQLException {
        new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(world,0,0,time,complete,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(1,64,0),WorkplaceObservation.Kind.SMOKER))));
    }
    @Test void bakeryProspectUsesRealReserveDemandAfterStartupAndIsReadOnlyAcrossRestart()throws Exception {
        prepare(WorkplaceObservation.Kind.SMOKER);bakeryMarker(1000,true);sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");
        var runtime=bakeryRuntime();var before=snapshot();var planned=prospects(WorkshopOpportunityPolicy.prototype(),VillageSupplyPolicy.prototype(),runtime).review(town,1000,Set.of(world));
        assertEquals(1,planned.candidates().size());var candidate=planned.candidates().getFirst();
        assertEquals(4,candidate.fundedFoodUnits());assertEquals(1,candidate.foodPeriod());assertEquals(300,candidate.starter().price());assertEquals(Map.of(grain,3L),candidate.starter().goods());
        assertEquals(before,snapshot());db.close();open();assertEquals(planned,prospects(WorkshopOpportunityPolicy.prototype(),VillageSupplyPolicy.prototype(),runtime).review(town,1000,Set.of(world)));assertEquals(before,snapshot());
    }
    @Test void bakeryProspectRefusesUnfundedOrAlreadySuppliedMealsAndMissingIngredients()throws Exception {
        prepare(WorkplaceObservation.Kind.SMOKER);bakeryMarker(1000,true);var runtime=bakeryRuntime();var planner=prospects(WorkshopOpportunityPolicy.prototype(),VillageSupplyPolicy.prototype(),runtime);
        var before=snapshot();assertTrue(planner.review(town,1000,Set.of(world)).candidates().isEmpty());assertEquals(before,snapshot());
        sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");
        var lowBudget=new VillageSupplyPolicy(true,399,1000,2500,2,1,32,Map.of(Commodity.FOOD,200L),2);
        before=snapshot();assertTrue(prospects(WorkshopOpportunityPolicy.prototype(),lowBudget,runtime).review(town,1000,Set.of(world)).explanation().contains("one bakery batch"));assertEquals(before,snapshot());
        sql("UPDATE economy_stocks SET units=4 WHERE commodity='grain'");before=snapshot();assertTrue(planner.review(town,1000,Set.of(world)).explanation().contains("surplus"));assertEquals(before,snapshot());
    }
    @Test void bakeryProspectRequiresFreshSmokerAndCannotInferOpportunityFromAbsentOrPartialWorld()throws Exception {
        prepare(WorkplaceObservation.Kind.SMOKER);bakeryMarker(1000,true);sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");
        var planner=prospects(WorkshopOpportunityPolicy.prototype(),VillageSupplyPolicy.prototype(),bakeryRuntime());var before=snapshot();
        assertTrue(planner.review(town,1000,Set.of()).candidates().isEmpty());assertTrue(planner.review(town,61001,Set.of(world)).candidates().isEmpty());assertEquals(before,snapshot());
        bakeryMarker(1001,false);before=snapshot();assertTrue(planner.review(town,1001,Set.of(world)).candidates().isEmpty());assertEquals(before,snapshot());
    }
    @Test void bakeryProspectDoesNotSpendCustomerCashTwiceOrBypassExistingBusinessIdentity()throws Exception {
        prepare(WorkplaceObservation.Kind.SMOKER);bakeryMarker(1000,true);sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");
        var runtime=bakeryRuntime();var planner=prospects(WorkshopOpportunityPolicy.prototype(),VillageSupplyPolicy.prototype(),runtime);
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,-3000,"Explicit fixture spending");
        // Startup is individually affordable at the exact treasury floor, but leaves no meal-purchase funds.
        var before=snapshot();assertTrue(planner.review(town,1000,Set.of(world)).explanation().contains("after startup spending"));assertEquals(before,snapshot());
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,3000,"Explicit fixture refund");start(StartupGrantPolicy.prototype(),true,Set.of(world));
        before=snapshot();assertTrue(planner.review(town,1000,Set.of(world)).explanation().contains("Existing bakery identity"));assertEquals(before,snapshot());
    }
    UUID existingMealSupplier(int x,boolean staffed)throws SQLException {
        var owner=person; // Multiple ownership is approved; keep the assessed population unchanged.
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,64,0,1000,"Explicit existing supplier");var id=UUID.randomUUID();
        var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,"Existing producer",owner,owner,"farming",site.id(),1,1),site,requirements,requirements,0,"Explicit fixture supplier"));
        db.getIndustryRepository().openStore(id,100,Map.of(Commodity.FOOD,1L),0);
        sql("UPDATE industry_stock SET units=4 WHERE business_id='"+id+"' AND commodity='food'"); // Explicit fixture stock.
        if(staffed)db.getBusinessRepository().hire(UUID.randomUUID(),id,owner,1);return id;
    }
    @Test void existingQualifiedMealsCoverBakeryDemandButUnstaffedStockDoesNot()throws Exception {
        prepare(WorkplaceObservation.Kind.SMOKER);bakeryMarker(1000,true);sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");
        var planner=prospects(WorkshopOpportunityPolicy.prototype(),VillageSupplyPolicy.prototype(),bakeryRuntime());
        var supplier=existingMealSupplier(2,false);var before=snapshot();assertEquals(1,planner.review(town,1000,Set.of(world)).candidates().size());assertEquals(before,snapshot());
        var owner=db.getBusinessRepository().findBusiness(supplier).orElseThrow().ownerId();db.getBusinessRepository().hire(UUID.randomUUID(),supplier,owner,1);
        before=snapshot();assertTrue(planner.review(town,1000,Set.of(world)).explanation().contains("Existing available meals"));assertEquals(before,snapshot());
    }
    @Test void supplierReviewCapCannotTurnIncompleteEvidenceIntoAStartupOpportunity()throws Exception {
        prepare(WorkplaceObservation.Kind.SMOKER);bakeryMarker(1000,true);sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");
        for(int x=2;x<67;x++)existingMealSupplier(x,false);
        var before=snapshot();var review=prospects(WorkshopOpportunityPolicy.prototype(),VillageSupplyPolicy.prototype(),bakeryRuntime()).review(town,1000,Set.of(world));
        assertTrue(review.candidates().isEmpty());assertTrue(review.explanation().contains("cap exceeded"));assertEquals(before,snapshot());
    }
}
