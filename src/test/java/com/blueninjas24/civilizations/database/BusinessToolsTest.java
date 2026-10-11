package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BusinessToolsTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=UUID.randomUUID(),warehouse=new UUID(0,1),smith=new UUID(0,2),mine=new UUID(0,3),deposit=UUID.randomUUID();
    final Commodity tools=ToolUpkeepPolicy.TOOLS;
    final Map<Commodity,Long> sizes=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,tools,1L);
    final ProductionRecipe recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),2,2);
    IndustryRuntimePolicy policy(boolean enabled,boolean restocking) {
        return new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(100,1,1,100,1,128,Map.of(Commodity.COAL,1L),sizes),Map.of(recipe.id(),recipe),
                new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L)),new WarehouseSupplyPolicy(restocking,16,16),
                new ToolUpkeepPolicy(enabled,0,2,50,400,Set.of("mining")));
    }
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("BusinessToolsTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,4,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(owner,"Owner",town,null,300,Resident.Status.ALIVE));new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,50000,"Real fixture funds");
        for(var id:List.of(warehouse,smith,mine)) {
            String kind=id.equals(warehouse)?"warehouse":id.equals(smith)?"blacksmith":"mining";var req=new JobRequirements(216,Map.of());
            var site=new BusinessSite(UUID.randomUUID(),town,world,(int)id.getLeastSignificantBits(),40,0,1,"Explicit fixture site");
            db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,kind,owner,owner,kind,site.id(),2,0),site,req,req,0,"Real fixture opportunity"));
            db.getIndustryRepository().openStore(id,100,sizes,5000);var person=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(person,"Worker",town,null,300,Resident.Status.ALIVE));db.getBusinessRepository().hire(UUID.randomUUID(),id,person,0);
        }
        var industry=db.getIndustryRepository();industry.discover(deposit,db.getBusinessRepository().findBusiness(mine).orElseThrow().siteId(),1,100,Map.of(Commodity.IRON,2L,Commodity.COAL,1L));
        industry.mine(UUID.randomUUID(),mine,deposit,2,0,2,2);
        for(var e:Map.of(Commodity.IRON,4L,Commodity.COAL,2L).entrySet()){var shipment=industry.purchase(UUID.randomUUID(),mine,smith,e.getKey(),e.getValue(),0,0,1,128);industry.deliver(shipment.id(),1);}
        industry.process(UUID.randomUUID(),smith,recipe,2,1);
        var shipment=industry.purchase(UUID.randomUUID(),smith,warehouse,tools,1,400,0,1,128);industry.deliver(shipment.id(),1);
        for(var job:db.getBusinessRepository().currentWorkers(smith))db.getBusinessRepository().endEmployment(job.id(),0,"Fixture production complete");
    }
    @AfterEach void close(){if(db!=null)db.close();}
    IndustryRuntime runtime(boolean enabled,boolean restocking)throws Exception{var r=new IndustryRuntime(db.getConnection(),policy(enabled,restocking));r.start(1000);return r;}
    IndustryRepository.Store store(UUID id)throws Exception{return db.getIndustryRepository().store(id).orElseThrow();}
    ToolUpkeep.State state()throws Exception{return new BusinessTools(db.getConnection(),policy(true,false)).state(mine);}
    long count(String table)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String text)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(text);}}
    @Test void realReplacementPaymentDelayedArrivalAndProductiveWearAreConserved()throws Exception {
        var runtime=runtime(true,false);runtime.pulse(2000,Set.of(world));assertEquals(4600,store(mine).hundredths());assertEquals(1,store(mine).reservedIncomingSpace());assertEquals(new ToolUpkeep.State(0,0,50),state());assertEquals(98,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
        runtime.pulse(3000,Set.of(world));assertEquals(0,store(mine).units().get(tools));assertEquals(new ToolUpkeep.State(1,1,50),state());assertEquals(97,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
        runtime.pulse(4000,Set.of(world));assertEquals(new ToolUpkeep.State(2,0,50),state());assertEquals(96,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
        assertEquals(1,count("industry_auto_shipments"));assertEquals(1,totalUsed());assertEquals(15000,store(mine).hundredths()+store(warehouse).hundredths()+store(smith).hundredths());
    }
    long totalUsed()throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COALESCE(SUM(tools_used),0) FROM business_tool_reviews")){return r.getLong(1);}}
    @Test void genuinelyNeededToolsDrivePaidWarehousePurchasesFromBlacksmithOutput()throws Exception {
        var runtime=runtime(true,true);runtime.pulse(2000,Set.of(world));runtime.pulse(3000,Set.of(world));runtime.pulse(4000,Set.of(world));
        assertEquals(2,count("industry_auto_shipments"));assertEquals(1,store(warehouse).reservedIncomingSpace());assertEquals(0,store(smith).units().get(tools));
        runtime.pulse(5000,Set.of(world));assertEquals(3,count("industry_auto_shipments"));runtime.pulse(6000,Set.of(world));
        assertEquals(2,totalUsed());assertEquals(15000,store(mine).hundredths()+store(warehouse).hundredths()+store(smith).hundredths());
    }
    @Test void checkpointFailureRollsBackWearDeliveryReserveAndConsumptionReceipt()throws Exception {
        var runtime=runtime(true,false);runtime.pulse(2000,Set.of(world));
        sql("CREATE TRIGGER fail_tool BEFORE UPDATE OF completed ON industry_runtime WHEN NEW.business_id='"+mine+"' BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(3000,Set.of(world)));assertEquals(0,totalUsed());assertEquals(new ToolUpkeep.State(0,0,50),state());assertEquals(1,store(mine).reservedIncomingSpace());assertEquals(98,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
        sql("DROP TRIGGER fail_tool");runtime.pulse(3000,Set.of(world));runtime.pulse(3000,Set.of(world));assertEquals(1,totalUsed());assertEquals(97,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
    }
    @Test void absenceRestartAndDisableDoNotWearOrRefillSavedService()throws Exception {
        var runtime=runtime(true,false);runtime.pulse(2000,Set.of(world));runtime.pulse(3000,Set.of(world));runtime.observeAvailability(3000,Set.of(world));runtime.pulse(100000,Set.of());assertEquals(new ToolUpkeep.State(1,1,50),state());
        db.close();open();runtime=new IndustryRuntime(db.getConnection(),policy(false,false));runtime.start(100000);runtime.pulse(101000,Set.of(world));assertEquals(new ToolUpkeep.State(1,1,50),state());assertEquals(1,totalUsed());
        runtime=new IndustryRuntime(db.getConnection(),policy(true,false));runtime.start(101000);runtime.pulse(102000,Set.of(world));assertEquals(new ToolUpkeep.State(2,0,50),state());assertEquals(1,totalUsed());
    }
    @Test void idleOrDepletedBusinessDoesNotConsumeTools()throws Exception {
        sql("UPDATE mineral_deposits SET remaining_batches=0 WHERE id='"+deposit+"'");var runtime=runtime(true,false);runtime.pulse(2000,Set.of(world));runtime.pulse(3000,Set.of(world));
        assertEquals(0,store(mine).units().get(tools));assertEquals(5000,store(mine).hundredths());assertEquals(0,totalUsed());assertEquals(0,state().productivePeriods());
    }
    @Test void fundedToolDemandIsActualAndPendingCargoPreventsDuplicateOrdering()throws Exception {
        var demand=new LocalIndustryDemand(db.getConnection(),policy(true,false),BusinessOperationPolicy.disabled());assertTrue(demand.review(town,Set.of(world),8).uncovered().isEmpty());
        var runtime=runtime(true,false);runtime.pulse(2000,Set.of(world));assertTrue(demand.review(town,Set.of(world),8).uncovered().isEmpty());
        runtime.pulse(3000,Set.of(world));assertEquals(Map.of(tools,1L),demand.review(town,Set.of(world),8).uncovered());
        sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+mine+"'");assertTrue(demand.review(town,Set.of(world),8).uncovered().isEmpty());
    }
    @Test void schema50UpgradeIsEmptyTransactionalAndRetainsAllPriorAssets()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),50);sql("CREATE TABLE business_tool_reviews(dummy TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(50,r.getInt(1));}
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='business_tools'")){assertEquals(0,r.getInt(1));}
        sql("DROP TABLE business_tool_reviews");new SchemaMigrator().migrate(db.getConnection());new SchemaMigrator().migrate(db.getConnection());
        assertEquals(0,count("business_tools"));assertEquals(0,count("business_tool_reviews"));assertEquals(1,store(warehouse).units().get(tools));assertEquals(1,store(smith).units().get(tools));assertEquals(98,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
    }
    @Test void staffedToolMakerKeepsItsOwnDueKitInsteadOfResellingIt()throws Exception {
        db.getIndustryRepository().consume(UUID.randomUUID(),warehouse,tools,1,IndustryRepository.Use.MAINTENANCE,"Explicit fixture service use");
        UUID worker=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(worker,"Smith",town,null,300,Resident.Status.ALIVE));db.getBusinessRepository().hire(UUID.randomUUID(),smith,worker,0);
        var base=policy(true,true);var both=new IndustryRuntimePolicy(true,1000,8,base.mining(),base.recipes(),base.supply(),base.warehouseSupply(),new ToolUpkeepPolicy(true,0,2,50,400,Set.of("mining","blacksmith")));
        new IndustryRuntime(db.getConnection(),both).start(1000);db.getConnection().setAutoCommit(false);
        new WarehouseSupplies(db.getConnection(),both,BusinessOperationPolicy.disabled()).acquireRows(warehouse,world,1,2);
        db.getConnection().commit();db.getConnection().setAutoCommit(true);assertEquals(0,count("industry_auto_shipments"));assertEquals(1,store(smith).units().get(tools));
    }
    @Test void retainedUnsupportedStoreDoesNotGainFreeToolCapacityOrHiddenWear()throws Exception {
        UUID id=UUID.randomUUID();var req=new JobRequirements(216,Map.of());var site=new BusinessSite(UUID.randomUUID(),town,world,9,40,0,1,"Legacy fixture site");
        db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,"Legacy",owner,owner,"mining",site.id(),1,0),site,req,req,0,"Explicit fixture opportunity"));
        db.getIndustryRepository().openStore(id,20,Map.of(Commodity.COAL,1L),0);
        assertNull(new BusinessTools(db.getConnection(),policy(true,false)).prepare(id,"mining",1));assertFalse(store(id).spacePerUnit().containsKey(tools));assertEquals(0,count("business_tools"));
    }
    @Test void immediateCaptureFailureSuppressesNewMineToolPressureBeforeSqlCanSaveIt()throws Exception {
        var runtime=runtime(true,false);runtime.pulse(2000,Set.of(world));runtime.pulse(3000,Set.of(world));
        assertEquals(Map.of(tools,1L),new LocalIndustryDemand(db.getConnection(),policy(true,false),BusinessOperationPolicy.disabled()).review(town,Set.of(world),8).uncovered());
        assertTrue(new LocalIndustryDemand(db.getConnection(),policy(true,false),BusinessOperationPolicy.disabled(),()->false).review(town,Set.of(world),8).uncovered().isEmpty());
        assertEquals(1,totalUsed());assertEquals(4600,store(mine).hundredths());assertEquals(1,count("industry_auto_shipments"));
    }
}
