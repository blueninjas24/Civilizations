package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.application.PopulationSession;
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

class FarmToolsRuntimeTest {
    @TempDir Path directory;DatabaseManager db;EconomyRuntimeRepository runtime;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),farmer=new UUID(0,1),smith=new UUID(0,2),seller=new UUID(0,10),farm;
    final FarmToolUpkeepPolicy upkeep=new FarmToolUpkeepPolicy(true,1,2,50,8);
    EconomySettings settings;
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Farm tool fixture","Fixture",0,64,0,2,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var person:List.of(farmer,smith))db.getPopulationRepository().insertResident(new Resident(person,"Fixture resident",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit fixture funding");
        var site=new BusinessSite(UUID.randomUUID(),town,world,10,64,0,1000,"Explicit fixture smith");
        var requirements=new JobRequirements(216,Map.of());var b=new Business(seller,town,"Smith",smith,smith,"blacksmith",site.id(),1,0);
        db.getBusinessRepository().found(new BusinessFounding(b,site,requirements,requirements,0,"Explicit fixture"));db.getBusinessRepository().hire(UUID.randomUUID(),seller,smith,0);
        db.getIndustryRepository().openStore(seller,100,Map.of(Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L),0);
        sql("UPDATE industry_stock SET units=3 WHERE commodity='iron'");
        db.getIndustryRepository().process(UUID.randomUUID(),seller,new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),3,3),3,0);
        settings=settings(4);create(upkeep);observe(1000,16);runtime.startOnline(1000,Set.of(world));farm=FarmEnterprises.businessId(town);
    }
    EconomySettings settings(int output) {
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        return new EconomySettings(1,3,2,2,1,output,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
    }
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("FarmToolsRuntimeTest"));db.initialize();}
    void create(FarmToolUpkeepPolicy policy)throws Exception {
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        runtime.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());
        runtime.configureFarmTools(policy,FarmToolBudgetPolicy.prototype(),IndustryPolicy.prototype());runtime.configureWorkLearning(WorkLearningPolicy.prototype());
    }
    void observe(long now,long crops)throws Exception{runtime.observe(new FarmObservation(town,world,now,true,crops,crops==0?null:new VillageObservation.Position(0,64,0)),now);}
    long output(long period)throws SQLException{try(var s=db.getConnection().prepareStatement("SELECT production FROM economy_periods WHERE settlement_id=? AND period=? AND commodity='food'")){s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
    long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}}
    long cash()throws SQLException{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    ToolUpkeep.State state()throws SQLException{return new FarmTools(db.getConnection(),upkeep,FarmToolBudgetPolicy.prototype(),BusinessOperationPolicy.prototype(),IndustryPolicy.prototype(),UpkeepContributionPolicy.disabled()).state(farm);}
    @Test void actualFoodRuntimeRecyclesOnlyRealSupplierSurplusAndPausesWithAbsentWorld()throws Exception {
        UUID buyer=UUID.randomUUID();var requirements=new JobRequirements(216,Map.of());
        var site=new BusinessSite(UUID.randomUUID(),town,world,20,64,0,1000,"Explicit fixture warehouse");
        db.getBusinessRepository().found(new BusinessFounding(new Business(buyer,town,"Fixture buyer",smith,smith,"warehouse",site.id(),1,0),site,requirements,requirements,0,"Explicit fixture"));
        db.getIndustryRepository().openStore(buyer,10,Map.of(ToolUpkeepPolicy.TOOLS,1L),26000);
        db.getIndustryRepository().purchase(UUID.randomUUID(),seller,buyer,ToolUpkeepPolicy.TOOLS,1,26000,0,1,10);
        create(upkeep);runtime.configureUpkeepContributions(UpkeepContributionPolicy.prototype());runtime.startOnline(1000,Set.of(world));
        runtime.pulse(2000,false);assertEquals(1,count("upkeep_contributions"));assertEquals(2500,cash());assertEquals(27500,db.getIndustryRepository().store(seller).orElseThrow().hundredths());
        runtime.pulse(3000,false);assertEquals(2,count("upkeep_contributions"));assertEquals(2500,cash());assertEquals(4,output(2));
        runtime.updateLoadedWorlds(3000,Set.of());runtime.pulse(100000,false);
        assertEquals(2,count("upkeep_contributions"));assertEquals(2,count("farm_tool_purchases"));assertEquals(30000,cash()+db.getIndustryRepository().store(seller).orElseThrow().hundredths());health();
    }
    @Test void boundedOrderReviewSkipsPoorSupplierAndPaysFundableStockedSupplierInSamePeriod()throws Exception {
        UUID rich=new UUID(0,20),worker=new UUID(0,3),buyer=UUID.randomUUID();var requirements=new JobRequirements(216,Map.of());
        db.getPopulationRepository().insertResident(new Resident(worker,"Fixture smith",town,null,300,Resident.Status.ALIVE));
        var site=new BusinessSite(UUID.randomUUID(),town,world,20,64,0,1000,"Explicit second smith");
        db.getBusinessRepository().found(new BusinessFounding(new Business(rich,town,"Funded smith",smith,smith,"blacksmith",site.id(),1,0),site,requirements,requirements,0,"Explicit fixture"));
        db.getBusinessRepository().hire(UUID.randomUUID(),rich,worker,0);
        db.getIndustryRepository().openStore(rich,100,Map.of(Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L),0);
        sql("UPDATE industry_stock SET units=2 WHERE commodity='iron' AND business_id='"+rich+"'");
        db.getIndustryRepository().process(UUID.randomUUID(),rich,new ProductionRecipe("fixture_funded_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),2,2),2,0);
        var buyerSite=new BusinessSite(UUID.randomUUID(),town,world,30,64,0,1000,"Explicit fixture warehouse");
        db.getBusinessRepository().found(new BusinessFounding(new Business(buyer,town,"Buyer",smith,smith,"warehouse",buyerSite.id(),1,0),buyerSite,requirements,requirements,0,"Explicit fixture"));
        db.getIndustryRepository().openStore(buyer,10,Map.of(ToolUpkeepPolicy.TOOLS,1L),26000);
        db.getIndustryRepository().purchase(UUID.randomUUID(),rich,buyer,ToolUpkeepPolicy.TOOLS,1,26000,0,1,10);
        create(upkeep);runtime.configureUpkeepContributions(UpkeepContributionPolicy.prototype());runtime.startOnline(1000,Set.of(world));
        runtime.pulse(2000,false);assertEquals(1,count("farm_tool_purchases"));assertEquals(1,count("upkeep_contributions"));
        assertEquals(2500,cash());assertEquals(0,db.getIndustryRepository().store(seller).orElseThrow().hundredths());
        assertEquals(3,db.getIndustryRepository().store(seller).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS));
        assertEquals(27500,db.getIndustryRepository().store(rich).orElseThrow().hundredths());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT business_id FROM upkeep_contributions")){r.next();assertEquals(rich.toString(),r.getString(1));}
        runtime.pulse(2000,false);assertEquals(1,count("upkeep_contributions"));health();
    }
    void sql(String sql)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    @Test void actualFoodWorkPaysDeliversAndConsumesRealToolsWithoutDuplicateWearOrMoney()throws Exception {
        for(long now=2000;now<=5000;now+=1000)runtime.pulse(now,false);
        for(long period=1;period<=4;period++)assertEquals(4,output(period));
        assertEquals(new ToolUpkeep.State(4,1,0),state());assertEquals(3,count("farm_tool_purchases"));assertEquals(18000,cash());
        assertEquals(12000,db.getIndustryRepository().store(seller).orElseThrow().hundredths());assertEquals(0,db.getIndustryRepository().store(farm).orElseThrow().hundredths());
        assertEquals(1,db.getIndustryRepository().store(farm).orElseThrow().reservedIncomingSpace());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT SUM(tools_used) FROM farm_tool_work")){r.next();assertEquals(2,r.getLong(1));}
        runtime.pulse(5000,false);assertEquals(4,count("farm_tool_work"));assertEquals(3,count("farm_tool_purchases"));assertEquals(18000,cash());
        assertTrue(runtime.views().get(town).farmBusiness().contains("kits used 1"));health();
    }
    @Test void missingToolsSlowRealFoodAndCarryFractionWithoutAwardingZeroWorkExperience()throws Exception {
        sql("UPDATE industry_stock SET units=0 WHERE commodity='tools'");settings=settings(1);create(upkeep);runtime.startOnline(1000,Set.of(world));
        runtime.pulse(2000,false);runtime.pulse(3000,false);assertEquals(1,output(1));assertEquals(0,output(2));
        assertEquals(50,state().remainderPercent());assertEquals(1,db.getPopulationRepository().findWorkProfile(farmer).orElseThrow().experience().get("farming"));
        runtime.pulse(4000,false);assertEquals(1,output(3));assertEquals(new ToolUpkeep.State(2,0,0),state());assertEquals(0,count("farm_tool_purchases"));assertEquals(30000,cash());
    }
    @Test void zeroFieldsCanReceivePaidEquipmentButNeverConsumeItOrWearService()throws Exception {
        runtime.pulse(2000,false);observe(2000,0);runtime.pulse(3000,false);
        assertEquals(0,output(2));assertEquals(new ToolUpkeep.State(1,0,0),state());
        assertEquals(1,db.getIndustryRepository().store(farm).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS));assertEquals(26000,cash());
        observe(3000,16);runtime.pulse(4000,false);assertEquals(4,output(3));assertEquals(new ToolUpkeep.State(2,1,0),state());
    }
    @Test void foodAuditFailureRollsBackArrivalUseLearningAndCheckpointThenRetriesOnce()throws Exception {
        runtime.pulse(2000,false);
        sql("CREATE TRIGGER reject_farm_work BEFORE INSERT ON farm_tool_work BEGIN SELECT RAISE(ABORT,'fixture work failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(3000,false));assertEquals(1,runtime.views().get(town).account().state().completedPeriods());
        assertEquals(new ToolUpkeep.State(1,0,0),state());assertEquals(1,db.getIndustryRepository().store(farm).orElseThrow().reservedIncomingSpace());assertEquals(26000,cash());
        sql("DROP TRIGGER reject_farm_work");runtime.pulse(3000,false);assertEquals(2,count("farm_tool_work"));assertEquals(new ToolUpkeep.State(2,1,0),state());assertEquals(22000,cash());health();
    }
    @Test void absentWorldAndRestartPausePaidCargoWearAndPurchasingWithoutOfflineCatchup()throws Exception {
        runtime.pulse(2000,false);runtime.updateLoadedWorlds(2000,Set.of());runtime.pulse(100000,false);assertEquals(new ToolUpkeep.State(1,0,0),state());assertEquals(26000,cash());
        db.close();open();create(upkeep);runtime.startOnline(1000000,Set.of());runtime.pulse(1001000,false);assertEquals(1,count("farm_tool_work"));
        runtime.updateLoadedWorlds(1001000,Set.of(world));runtime.pulse(1002000,false);assertEquals(new ToolUpkeep.State(2,1,0),state());assertEquals(22000,cash());assertEquals(2,count("farm_tool_purchases"));health();
    }
    @Test void disablingNewUpkeepPreservesServiceAndStillDeliversAlreadyPaidEquipment()throws Exception {
        runtime.pulse(2000,false);db.close();open();create(FarmToolUpkeepPolicy.disabled());runtime.startOnline(2000,Set.of(world));runtime.pulse(3000,false);
        assertEquals(4,output(2));assertEquals(new ToolUpkeep.State(1,0,0),state());assertEquals(1,db.getIndustryRepository().store(farm).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS));assertEquals(26000,cash());
        create(upkeep);runtime.startOnline(3000,Set.of(world));runtime.pulse(4000,false);assertEquals(new ToolUpkeep.State(2,1,0),state());assertEquals(22000,cash());
    }
    @Test void unrelatedMiningMapsAndSettingsCannotCreateAFalseFarmRetune()throws Exception {
        runtime.pulse(2000,false);var original=IndustryPolicy.prototype();
        var unrelated=new IndustryPolicy(151,4,16,200,original.localDeliveryPeriods(),original.localHaulUnitsPerPeriod(),
                Map.of(Commodity.IRON,1L,Commodity.STONE,4L),original.storageSpacePerUnit());
        var reopened=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        reopened.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());
        reopened.configureFarmTools(upkeep,FarmToolBudgetPolicy.prototype(),unrelated);reopened.configureWorkLearning(WorkLearningPolicy.prototype());
        reopened.startOnline(2000,Set.of(world));assertEquals(0,count("economy_tuning"));
        reopened.pulse(3000,false);assertEquals(new ToolUpkeep.State(2,1,0),state());assertEquals(22000,cash());
    }
    @Test void schema53UpgradeHasNoInventedToolsOrWearAndFailurePreservesCurrentSave()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),53);sql("CREATE TABLE farm_tool_work (fixture TEXT)");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){r.next();assertEquals(53,r.getInt(1));}
        sql("DROP TABLE farm_tool_work");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("farm_tool_work"));assertEquals(0,count("business_tools"));assertEquals(30000,cash());
        runtime.pulse(2000,false);assertEquals(1,count("farm_tool_work"));health();
    }
    void health()throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}}
    @AfterEach void close(){if(db!=null)db.close();}
    void makeUnfilledNeed()throws Exception {
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        runtime.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());
        runtime.configureFarmTools(upkeep,FarmToolBudgetPolicy.disabled(),IndustryPolicy.prototype());runtime.configureWorkLearning(WorkLearningPolicy.prototype());
        runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);
        assertEquals(0,count("farm_tool_purchases"));assertEquals(4,output(1));
    }
    FarmEquipmentDemand demand(){return new FarmEquipmentDemand(db.getConnection(),upkeep,FarmToolBudgetPolicy.prototype(),BusinessOperationPolicy.prototype());}
    IndustryRuntimePolicy warehousePolicy() {
        var recipe=new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),1,1);
        return new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,100L)),new WarehouseSupplyPolicy(true,16,16),new ToolUpkeepPolicy(true,3,8,50,4000,Set.of()));
    }
    UUID warehouse(long capital)throws Exception {
        UUID worker=new UUID(0,3);db.getPopulationRepository().insertResident(new Resident(worker,"Warehouse fixture worker",town,null,300,Resident.Status.ALIVE));
        var site=new BusinessSite(UUID.randomUUID(),town,world,20,64,0,1000,"Explicit fixture warehouse");var req=new JobRequirements(216,Map.of());UUID id=UUID.randomUUID();
        db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,"Warehouse",worker,worker,"warehouse",site.id(),1,0),site,req,req,0,"Explicit fixture"));
        db.getBusinessRepository().hire(UUID.randomUUID(),id,worker,0);db.getIndustryRepository().openStore(id,100,Map.of(Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L),capital);return id;
    }
    @Test void actualFundedFarmDemandReviewIsReadOnlyAndAbsenceOrDisabledPolicyHasNoCustomer()throws Exception {
        makeUnfilledNeed();long entries=count("treasury_entries"),stock=db.getIndustryRepository().store(seller).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS);
        var review=demand().review(town,Set.of(world));assertEquals(1,review.units());assertEquals(4000,review.invoice());assertEquals(farm,review.farm());
        assertEquals(review,demand().review(town,Set.of(world)));assertEquals(0,demand().review(town,Set.of()).units());assertEquals(0,demand().review(town,null).units());
        assertEquals(0,new FarmEquipmentDemand(db.getConnection(),FarmToolUpkeepPolicy.disabled(),FarmToolBudgetPolicy.prototype(),BusinessOperationPolicy.prototype()).review(town,Set.of(world)).units());
        assertEquals(30000,cash());assertEquals(entries,count("treasury_entries"));assertEquals(stock,db.getIndustryRepository().store(seller).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS));assertEquals(0,count("local_shipments"));
    }
    @Test void farmsWithoutRealWorkFoodAssessmentOrFundsNeverCreateWarehouseDemand()throws Exception {
        makeUnfilledNeed();observe(2000,0);assertEquals(0,demand().review(town,Set.of(world)).units());observe(2001,16);
        sql("UPDATE needs_periods SET fulfilled=0");assertEquals(0,demand().review(town,Set.of(world)).units());sql("UPDATE needs_periods SET fulfilled=demand");
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,-27500,"Explicit fixture spending");assertEquals(0,demand().review(town,Set.of(world)).units());
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,27500,"Explicit fixture refund");assertEquals(1,demand().review(town,Set.of(world)).units());
        db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Fixture arrival",town,null,300,Resident.Status.ALIVE));assertEquals(0,demand().review(town,Set.of(world)).units());
    }
    @Test void existingServiceOrOwnedIncomingEquipmentPreventsDuplicateFarmPressure()throws Exception {
        makeUnfilledNeed();sql("UPDATE business_tools SET service_periods=2");assertEquals(0,demand().review(town,Set.of(world)).units());sql("UPDATE business_tools SET service_periods=0");
        var request=new FarmToolPurchases.Request(UUID.randomUUID(),farm,seller,1,1,8);
        new FarmToolPurchases(db.getConnection(),BusinessOperationPolicy.prototype()).purchase(request,FarmToolBudgetPolicy.prototype(),Set.of(world));
        assertEquals(0,demand().review(town,Set.of(world)).units());assertEquals(1,db.getIndustryRepository().store(farm).orElseThrow().reservedIncomingSpace());
        assertEquals(26000,cash());assertEquals(1,count("farm_tool_purchases"));
    }
    @Test void legacyFarmCatalogCannotBeReinterpretedAsDemand()throws Exception {
        makeUnfilledNeed();db.getIndustryRepository().openStore(farm,2,Map.of(Commodity.IRON,1L),0);assertEquals(0,demand().review(town,Set.of(world)).units());
        assertEquals(2,db.getIndustryRepository().store(farm).orElseThrow().capacity());assertEquals(30000,cash());
    }
    @Test void mismatchedCurrentFoodHistoryDoesNotCreateFarmEquipmentCustomer()throws Exception {
        makeUnfilledNeed();assertEquals(1,demand().review(town,Set.of(world)).units());
        sql("UPDATE farm_learning_reviews SET production=production+1");assertEquals(0,demand().review(town,Set.of(world)).units());
        assertEquals(30000,cash());assertEquals(0,count("farm_tool_purchases"));health();
    }
    @Test void warehouseBuysOneRealToolkitForPublicFarmThenResellsAfterDeliveryWithoutMintedCash()throws Exception {
        var buyer=warehouse(4000);db.getIndustryRepository().consume(UUID.randomUUID(),seller,ToolUpkeepPolicy.TOOLS,2,IndustryRepository.Use.MAINTENANCE,"Explicit fixture prior maintenance");
        makeUnfilledNeed();create(upkeep);runtime.startOnline(2000,Set.of(world));
        var industry=new IndustryRuntime(db.getConnection(),warehousePolicy(),BusinessOperationPolicy.prototype());industry.configureFarmToolDemand(upkeep,FarmToolBudgetPolicy.prototype());industry.start(2000);
        industry.pulse(3000,Set.of(world));assertEquals(1,count("industry_auto_shipments"));assertEquals(0,db.getIndustryRepository().store(buyer).orElseThrow().hundredths());assertEquals(4000,db.getIndustryRepository().store(seller).orElseThrow().hundredths());
        assertEquals(1,db.getIndustryRepository().store(buyer).orElseThrow().reservedIncomingSpace());industry.pulse(3000,Set.of(world));assertEquals(1,count("industry_auto_shipments"));
        runtime.pulse(3000,false);assertEquals(0,count("farm_tool_purchases"));industry.pulse(4000,Set.of(world));runtime.pulse(4000,false);
        assertEquals(1,count("farm_tool_purchases"));assertEquals(22000,cash());assertEquals(4000,db.getIndustryRepository().store(buyer).orElseThrow().hundredths());
        runtime.pulse(5000,false);assertTrue(state().servicePeriods()>0);assertEquals(1,count("farm_tool_purchases"));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT (SELECT SUM(hundredths) FROM treasuries)+(SELECT SUM(hundredths) FROM industry_stores)")){r.next();assertEquals(30000,r.getLong(1));}health();
    }
    @Test void sharedWarehouseStockCoversPublicFarmOnceAndBoundedCustomerOverflowDefers()throws Exception {
        var buyer=warehouse(4000);makeUnfilledNeed();
        var policy=warehousePolicy();var review=new LocalIndustryDemand(db.getConnection(),policy,BusinessOperationPolicy.prototype()).withFarmTools(upkeep,FarmToolBudgetPolicy.prototype()).review(town,Set.of(world),16);
        assertEquals(1,review.uncovered().get(ToolUpkeepPolicy.TOOLS));assertEquals(1,review.customers().stream().filter(c->c.business().equals(farm)).count());
        db.getIndustryRepository().purchase(UUID.randomUUID(),seller,buyer,ToolUpkeepPolicy.TOOLS,1,4000,1,1,8);db.getIndustryRepository().deliver(shipmentTo(buyer),2);
        review=new LocalIndustryDemand(db.getConnection(),policy,BusinessOperationPolicy.prototype()).withFarmTools(upkeep,FarmToolBudgetPolicy.prototype()).review(town,Set.of(world),16);assertFalse(review.uncovered().containsKey(ToolUpkeepPolicy.TOOLS));
        review=new LocalIndustryDemand(db.getConnection(),policy,BusinessOperationPolicy.prototype()).withFarmTools(upkeep,FarmToolBudgetPolicy.prototype()).review(town,Set.of(world),1);assertFalse(review.complete());assertTrue(review.customers().isEmpty());
    }

    UUID shipmentTo(UUID buyer)throws Exception{try(var s=db.getConnection().prepareStatement("SELECT id FROM local_shipments WHERE buyer_id=?")){s.setString(1,buyer.toString());try(var r=s.executeQuery()){r.next();return UUID.fromString(r.getString(1));}}}

    @Test void normalSessionWiresPublicFarmDemandAndRestartAbsencePreservesPaidCargo()throws Exception {
        var buyer=warehouse(4000);db.getIndustryRepository().consume(UUID.randomUUID(),seller,ToolUpkeepPolicy.TOOLS,2,IndustryRepository.Use.MAINTENANCE,"Explicit fixture prior maintenance");
        makeUnfilledNeed();db.close();db=null;
        var clock=new java.util.concurrent.atomic.AtomicLong(2000);
        try(var session=session(clock)) {
            session.start(false,Set.of(world)).get(10,java.util.concurrent.TimeUnit.SECONDS);
            clock.set(3000);session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);
            open();assertEquals(1,count("industry_auto_shipments"));assertEquals(1,db.getIndustryRepository().store(buyer).orElseThrow().reservedIncomingSpace());db.close();db=null;
            session.updateLoadedWorlds(Set.of()).get(10,java.util.concurrent.TimeUnit.SECONDS);
            clock.set(100000);session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);
        }
        clock.set(1000000);
        try(var session=session(clock)) {
            session.start(false,Set.of()).get(10,java.util.concurrent.TimeUnit.SECONDS);
            open();assertEquals(1,count("industry_auto_shipments"));assertEquals(0,count("farm_tool_purchases"));db.close();db=null;
            session.updateLoadedWorlds(Set.of(world)).get(10,java.util.concurrent.TimeUnit.SECONDS);
            session.observeFarms(new FarmObservation(town,world,clock.get(),true,16,new VillageObservation.Position(0,64,0))).get(10,java.util.concurrent.TimeUnit.SECONDS);
            clock.set(1001000);session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);
            clock.set(1002000);session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);
        }
        open();assertEquals(1,count("industry_auto_shipments"));assertEquals(1,count("farm_tool_purchases"));assertTrue(state().servicePeriods()>0);
        assertEquals(22000,cash());assertEquals(4000,db.getIndustryRepository().store(buyer).orElseThrow().hundredths());assertEquals(4000,db.getIndustryRepository().store(seller).orElseThrow().hundredths());health();
    }
    PopulationSession session(java.util.concurrent.atomic.AtomicLong clock) {
        var defaults=PopulationSession.Settings.prototype();
        var population=new PopulationSession.Settings(0,defaults.millisPerMonth(),defaults.maximumOfflineMonths(),defaults.monthsPerBatch(),defaults.population(),defaults.migration(),defaults.recognition(),defaults.enrollment());
        var session=new PopulationSession(directory.toFile(),Logger.getLogger("FarmToolsRuntimeTest"),population,clock::get,settings,FarmObservation.Policy.prototype());
        session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureFarmTools(upkeep,FarmToolBudgetPolicy.prototype());
        session.configureWorkLearning(WorkLearningPolicy.prototype());session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureIndustryRuntime(warehousePolicy());return session;
    }

}
