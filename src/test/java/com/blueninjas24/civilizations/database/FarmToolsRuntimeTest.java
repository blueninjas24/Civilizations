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

class FarmToolsRuntimeTest {
    @TempDir Path directory;DatabaseManager db;EconomyRuntimeRepository runtime;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),farmer=new UUID(0,1),smith=new UUID(0,2),seller=UUID.randomUUID(),farm;
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
}
