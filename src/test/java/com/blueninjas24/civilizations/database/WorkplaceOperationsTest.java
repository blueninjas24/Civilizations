package com.blueninjas24.civilizations.database;

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

class WorkplaceOperationsTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),person=UUID.randomUUID(),business=UUID.randomUUID(),operation=UUID.randomUUID();
    final ProductionRecipe recipe=ProductionRecipe.smithTools();
    final WorkshopStarterPolicy policy=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L,Commodity.COAL,600L));
    BusinessStartupGrants.WorkshopRequest workshop;
    WorkshopStarterSupplies.Request order;
    void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("StarterTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Starter fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Founder",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture treasury");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var units=Map.of(Commodity.FOOD,100L,Commodity.IRON,10L,Commodity.COAL,10L);
        var prices=Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.IRON,BigDecimal.TEN,Commodity.COAL,BigDecimal.TEN);
        var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),units,prices,bands);
        var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,64,0,10,"Explicit fixture workplace");
        var firm=new Business(business,town,"Smith",person,person,"blacksmith",site.id(),2,0);
        var requirements=new JobRequirements(216,Map.of());
        workshop=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(firm,site,requirements,requirements,500,"Fixture opportunity"),
            100,Map.of(Commodity.IRON,1L,Commodity.COAL,1L,ToolUpkeepPolicy.TOOLS,1L),4000,person);
        order=new WorkshopStarterSupplies.Request(operation,business,recipe,1,0,1,128);
    }
    WorkplaceOperations.Anchor anchor(){return new WorkplaceOperations.Anchor(business,world,new VillageObservation.Position(0,64,0),WorkplaceObservation.Kind.SMITHING_TABLE);}
    WorkplaceOperations markers(){return new WorkplaceOperations(db.getConnection());}
    BusinessStartupGrants.Workshop start()throws SQLException{return new BusinessStartupGrants(db.getConnection()).startMarkerWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),order,policy,Set.of(world),new WorkplaceOperations.Confirmation(anchor(),true,1000),1000);}
    void confirm(boolean present,long time)throws SQLException{markers().confirm(List.of(new WorkplaceOperations.Confirmation(anchor(),present,time)),time,Set.of(world));}
    boolean permitted()throws SQLException{return new BusinessOperations(db.getConnection(),BusinessOperationPolicy.disabled()).evaluate(business).permitted();}
    void deliver()throws SQLException{db.getConnection().setAutoCommit(false);supplies().deliverRows(business,1);db.getConnection().commit();db.getConnection().setAutoCommit(true);}
    WorkshopStarterSupplies supplies(){return new WorkshopStarterSupplies(db.getConnection());}
    IndustryRepository.Store store()throws SQLException{return db.getIndustryRepository().store(business).orElseThrow();}
    long cash()throws SQLException{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    long units(Commodity good)throws SQLException{return new CommodityRepository(db.getConnection()).read(town).orElseThrow().state().stocks().get(good).units();}
    long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String query)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    @AfterEach void close(){if(db!=null)db.close();}
    @Test void confirmedRemovalPausesRealProductionPreservingPaidCargoJobsMoneyAndIdentity()throws Exception {
        start();assertTrue(permitted());confirm(false,1001);assertFalse(permitted());
        assertEquals(1,count("business_employment"));assertEquals(1,count("businesses"));assertEquals(1,count("business_sites"));
        assertEquals(8500,cash());assertEquals(1000,store().hundredths());assertEquals(3,store().reservedIncomingSpace());
        deliver();assertEquals(2,store().units().get(Commodity.IRON));assertEquals(1,store().units().get(Commodity.COAL));
        assertThrows(SQLException.class,()->db.getIndustryRepository().process(UUID.randomUUID(),business,recipe,1,1));
        assertEquals(0,count("recipe_runs"));assertEquals(2,count("workplace_operation_history"));
        confirm(true,1002);assertTrue(permitted());
        assertEquals(1,db.getIndustryRepository().process(UUID.randomUUID(),business,recipe,1,1).receipt().quantity());
        assertEquals(1,store().units().get(ToolUpkeepPolicy.TOOLS));assertEquals(8500,cash());assertEquals(3,count("workplace_operation_history"));
    }
    @Test void restartAndUnloadedOrInvalidEvidenceNeverInventRestorationOrRemoval()throws Exception {
        start();confirm(false,1001);db.close();open();assertFalse(permitted());
        markers().confirm(List.of(new WorkplaceOperations.Confirmation(anchor(),true,1002)),1002,Set.of());assertFalse(permitted());
        markers().confirm(List.of(new WorkplaceOperations.Confirmation(anchor(),true,1002)),1002,null);assertFalse(permitted());
        markers().confirm(List.of(new WorkplaceOperations.Confirmation(anchor(),true,1003)),1002,Set.of(world));assertFalse(permitted());
        markers().confirm(List.of(new WorkplaceOperations.Confirmation(anchor(),true,1002)),61003,Set.of(world));assertFalse(permitted());
        confirm(true,1000);assertFalse(permitted());
        assertThrows(SQLException.class,()->confirm(true,1001));assertFalse(permitted());
        confirm(true,1002);confirm(true,1003);assertEquals(3,count("workplace_operation_history"));
        assertEquals(List.of(anchor()),markers().anchors(null,64));assertTrue(markers().anchors(business,64).isEmpty());
    }
    @Test void wrongAnchorOrWrongKindCannotRestoreAndWholePageRollsBack()throws Exception {
        start();confirm(false,1001);
        var wrong=new WorkplaceOperations.Anchor(business,world,new VillageObservation.Position(1,64,0),WorkplaceObservation.Kind.SMITHING_TABLE);
        assertThrows(SQLException.class,()->markers().confirm(List.of(new WorkplaceOperations.Confirmation(anchor(),true,1002),new WorkplaceOperations.Confirmation(wrong,true,1003)),1003,Set.of(world)));
        assertFalse(permitted());assertEquals(2,count("workplace_operation_history"));assertTrue(db.getConnection().getAutoCommit());
        var kind=new WorkplaceOperations.Anchor(business,world,anchor().position(),WorkplaceObservation.Kind.BARREL);
        assertThrows(SQLException.class,()->markers().confirm(List.of(new WorkplaceOperations.Confirmation(kind,true,1002)),1002,Set.of(world)));assertFalse(permitted());
    }
    @Test void lateStartupAttachmentFailureRollsBackEveryGrantGoodsMoneyAndJob()throws Exception {
        sql("CREATE TRIGGER reject_anchor BEFORE INSERT ON business_workplace_anchors BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,this::start);assertEquals(10000,cash());assertEquals(10,units(Commodity.IRON));
        for(var table:List.of("businesses","business_sites","business_employment","industry_stores","startup_grants","workshop_starter_orders","business_workplace_anchors","workplace_operation_history"))assertEquals(0,count(table));
        sql("DROP TRIGGER reject_anchor");start();assertEquals(1,count("business_workplace_anchors"));
    }
    @Test void legacyFirmsAcquireNoMarkerRequirementAndAdminCanDisablePause()throws Exception {
        new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),order,policy,Set.of(world));
        assertTrue(permitted());assertEquals(0,count("business_workplace_anchors"));assertThrows(SQLException.class,this::start);
        assertEquals(8500,cash());assertEquals(0,count("business_workplace_anchors"));
    }
    @Test void adminSettingDoesNotEraseEvidenceAndAcceptedReplayCannotResumeOrRefill()throws Exception {
        start();confirm(false,1001);start();assertFalse(permitted());assertEquals(8500,cash());assertEquals(1,count("startup_grants"));assertEquals(1,count("workshop_starter_orders"));
        markers().configure(new WorkplaceOperationPolicy(false));assertTrue(permitted());assertEquals(2,count("workplace_operation_history"));
        markers().configure(WorkplaceOperationPolicy.prototype());assertFalse(permitted());confirm(true,1002);assertTrue(permitted());
    }
    @Test void directLocalTradeCannotBypassEitherPausedEndpointButPaidDeliveryStillArrives()throws Exception {
        start();deliver();db.getIndustryRepository().process(UUID.randomUUID(),business,recipe,1,1);
        UUID buyer=UUID.randomUUID(),buyerOwner=UUID.randomUUID();
        db.getPopulationRepository().insertResident(new Resident(buyerOwner,"Buyer",town,null,300,Resident.Status.ALIVE));
        var site=new BusinessSite(UUID.randomUUID(),town,world,2,64,0,1000,"Explicit buyer");var req=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(buyer,town,"Warehouse",buyerOwner,buyerOwner,"warehouse",site.id(),1,0),site,req,req,0,"Fixture"));
        db.getIndustryRepository().openStore(buyer,100,Map.of(ToolUpkeepPolicy.TOOLS,1L),0);
        UUID trade=UUID.randomUUID();var shipped=db.getIndustryRepository().purchase(trade,business,buyer,ToolUpkeepPolicy.TOOLS,1,0,1,1,128);
        confirm(false,1001);assertEquals(shipped,db.getIndustryRepository().purchase(trade,business,buyer,ToolUpkeepPolicy.TOOLS,1,0,1,1,128));
        assertThrows(SQLException.class,()->db.getIndustryRepository().purchase(UUID.randomUUID(),business,buyer,ToolUpkeepPolicy.TOOLS,1,0,2,1,128));
        assertThrows(SQLException.class,()->db.getIndustryRepository().purchase(UUID.randomUUID(),buyer,business,ToolUpkeepPolicy.TOOLS,1,0,2,1,128));
        db.getIndustryRepository().deliver(trade,2);assertEquals(1,db.getIndustryRepository().store(buyer).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS));assertFalse(permitted());
    }
    @Test void timedPausedPeriodsDeliverEarnedCargoWithoutWorkOrExperienceAndResumeNormally()throws Exception {
        start();confirm(false,1001);
        var runtime=new IndustryRuntime(db.getConnection(),new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe)),BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());
        runtime.start(10000);runtime.pulse(11000,Set.of(world));runtime.pulse(12000,Set.of(world));
        assertEquals(0,count("recipe_runs"));assertEquals(0,count("industry_learning_reviews"));assertEquals(2,store().units().get(Commodity.IRON));
        assertEquals(0,db.getPopulationRepository().findWorkProfile(person).orElseThrow().experience().getOrDefault("smithing",0L));
        confirm(true,12001);runtime.pulse(13000,Set.of(world));assertEquals(1,store().units().get(ToolUpkeepPolicy.TOOLS));assertEquals(1,count("recipe_runs"));
        assertEquals(1,count("industry_learning_reviews"));assertEquals(8500,cash());
    }
    @Test void markerWarehouseUsesSameAtomicGrantBindingAndCannotChangeItsAnchor()throws Exception {
        var f=workshop.founding();var old=f.business();
        var firm=new Business(old.id(),town,"Warehouse",person,person,"warehouse",old.siteId(),1,0);
        var founding=new BusinessFounding(firm,f.site(),f.founderRequirements(),f.workerRequirements(),500,"Fixture warehouse");
        var request=new BusinessStartupGrants.WarehouseRequest(founding,100,workshop.space(),4000,person);
        var anchor=new WorkplaceOperations.Anchor(business,world,this.anchor().position(),WorkplaceObservation.Kind.BARREL);
        var grants=new BusinessStartupGrants(db.getConnection());
        grants.startMarkerWarehouse(request,StartupGrantPolicy.prototype(),Set.of(world),new WorkplaceOperations.Confirmation(anchor,true,1000),1000);
        assertEquals(5500,cash());assertEquals(4000,store().hundredths());assertEquals(0,count("workshop_starter_orders"));
        markers().confirm(List.of(new WorkplaceOperations.Confirmation(anchor,false,1001)),1001,Set.of(world));assertFalse(permitted());
        grants.startMarkerWarehouse(request,StartupGrantPolicy.prototype(),Set.of(world),new WorkplaceOperations.Confirmation(anchor,true,1000),1000);assertFalse(permitted());assertEquals(5500,cash());
        markers().confirm(List.of(new WorkplaceOperations.Confirmation(anchor,true,1002)),1002,Set.of(world));assertTrue(permitted());
    }
    @Test void wrongMarkerStartupProofLeavesNoBusinessOrSpending()throws Exception {
        var wrong=new WorkplaceOperations.Anchor(business,world,new VillageObservation.Position(1,64,0),WorkplaceObservation.Kind.SMITHING_TABLE);
        var grants=new BusinessStartupGrants(db.getConnection());
        assertThrows(SQLException.class,()->grants.startMarkerWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),order,policy,Set.of(world),new WorkplaceOperations.Confirmation(wrong,true,1000),1000));
        assertThrows(SQLException.class,()->grants.startMarkerWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),order,policy,Set.of(world),new WorkplaceOperations.Confirmation(anchor(),false,1000),1000));
        assertEquals(10000,cash());assertEquals(0,count("businesses"));assertEquals(10,units(Commodity.IRON));
    }
    @Test void actualSqlOwnerChecksWorldMasksAndConfigAcrossRestart()throws Exception {
        start();db.close();var clock=new java.util.concurrent.atomic.AtomicLong(1002);
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),Logger.getLogger("MarkerOperationsTest"),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype(),clock::get)) {
            session.start(false,Set.of()).get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(List.of(anchor()),session.workplaceOperationAnchors(null).get(5,java.util.concurrent.TimeUnit.SECONDS));
            session.confirmWorkplaceOperations(List.of(new WorkplaceOperations.Confirmation(anchor(),false,1002))).get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(session.currentBusinessAvailability(business).get(5,java.util.concurrent.TimeUnit.SECONDS).permitted());
            session.updateLoadedWorlds(Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            session.confirmWorkplaceOperations(List.of(new WorkplaceOperations.Confirmation(anchor(),false,1002))).get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertFalse(session.currentBusinessAvailability(business).get(5,java.util.concurrent.TimeUnit.SECONDS).permitted());
        }
        open();assertFalse(permitted());
    }
    @Test void schema61UpgradeIsAtomicPreservesLegacyAssetsAndCanRetry()throws Exception {
        new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),order,policy,Set.of(world));
        var before=store();SchemaFixtures.rewind(db.getConnection(),60);sql("CREATE VIEW workplace_operation_history AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var st=db.getConnection().createStatement();var r=st.executeQuery("PRAGMA user_version")){assertEquals(60,r.getInt(1));}
        assertEquals(before,store());assertTrue(permitted());sql("DROP VIEW workplace_operation_history");new SchemaMigrator().migrate(db.getConnection());
        assertEquals(before,store());assertEquals(0,count("business_workplace_anchors"));assertEquals(0,count("workplace_operation_history"));assertTrue(permitted());
        try(var st=db.getConnection().createStatement();var r=st.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
