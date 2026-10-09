package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.BusinessDiagnostics;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class RecipeRepositoryTest {
    @TempDir Path directory;
    DatabaseManager db;IndustryRepository industry;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=UUID.randomUUID(),worker=UUID.randomUUID(),deposit=UUID.randomUUID();
    Business mine,warehouse,smith;
    static final Commodity TOOLS=new Commodity("tools");
    static final Map<Commodity,Long> SPACE=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,Commodity.STONE,1L,TOOLS,1L);
    static final Map<Commodity,Long> BUNDLE=Map.of(Commodity.IRON,2L,Commodity.COAL,1L,Commodity.STONE,3L);
    @BeforeEach void setup() throws Exception {
        open();db.saveSettlement(new Settlement(town,"Workshop test","World",0,64,0,1,0,0));
        var identity=db.getVillageIdentityRepository();identity.observeWorld(world,"World");identity.bindSettlement(town,world);
        for(var id:List.of(owner,worker))db.getPopulationRepository().insertResident(new Resident(id,"Fixture",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Test initial capital");
        mine=business("mining",0);warehouse=business("warehouse",10);smith=business("blacksmith",20);
        db.getBusinessRepository().hire(UUID.randomUUID(),mine.id(),owner,0);db.getBusinessRepository().hire(UUID.randomUUID(),smith.id(),worker,0);
        industry.openStore(mine.id(),20000,SPACE,0);industry.openStore(warehouse.id(),20000,SPACE,12000);industry.openStore(smith.id(),20000,SPACE,12000);
        industry.discover(deposit,mine.siteId(),8,500,BUNDLE);
    }
    void open() throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("RecipeTest"));db.initialize();industry=db.getIndustryRepository();}
    @AfterEach void close(){db.close();}
    Business business(String kind,int x) throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,64,0,1,"Explicit test workplace");
        var b=new Business(UUID.randomUUID(),town,kind,owner,owner,kind,site.id(),4,0);
        var req=new JobRequirements(216,Map.of());db.getBusinessRepository().found(new BusinessFounding(b,site,req,req,0,"Test opportunity"));return b;
    }
    void supply(long period,long batches) throws Exception {
        industry.mine(UUID.randomUUID(),mine.id(),deposit,batches,period,8,8);
        for(var entry:Map.of(Commodity.IRON,2*batches,Commodity.COAL,batches).entrySet()) {
            var shipment=industry.purchase(UUID.randomUUID(),mine.id(),smith.id(),entry.getKey(),entry.getValue(),entry.getValue(),period,1,128);
            industry.deliver(shipment.id(),period+1);
        }
    }
    long units(Business b,Commodity g) throws Exception {return industry.store(b.id()).orElseThrow().units().get(g);}
    long money() throws Exception {
        long total=new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();
        for(var b:List.of(mine,warehouse,smith))total+=industry.store(b.id()).orElseThrow().hundredths();return total;
    }
    void sql(String text) throws Exception {try(var s=db.getConnection().createStatement()){s.execute(text);}}
    long count(String table) throws Exception {try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    @Test void deliveredInputsAreTransformedWithFullImmutableHistoryAndNoNewMoney() throws Exception {
        supply(0,4);var operation=UUID.randomUUID();var recipe=ProductionRecipe.smithTools();
        var run=industry.process(operation,smith.id(),recipe,8,1);
        assertEquals(2,run.receipt().quantity());assertEquals(1,run.workers());assertEquals(recipe.inputs(),run.inputPerBatch());
        assertEquals(Map.of(Commodity.IRON,4L,Commodity.COAL,2L),run.consumed());assertEquals(Map.of(TOOLS,2L),run.produced());
        assertEquals(4,units(smith,Commodity.IRON));assertEquals(2,units(smith,Commodity.COAL));assertEquals(2,units(smith,TOOLS));assertEquals(30000,money());
        assertThrows(UnsupportedOperationException.class,()->run.produced().clear());assertEquals(run,industry.process(operation,smith.id(),recipe,8,1));
        db.close();open();assertEquals(run,industry.recipeRun(operation).orElseThrow());assertEquals(run,industry.process(operation,smith.id(),recipe,8,1));
        var changed=new ProductionRecipe(recipe.id(),recipe.industry(),Map.of(Commodity.IRON,1L),recipe.outputs(),2,8);
        assertThrows(SQLException.class,()->industry.process(operation,smith.id(),changed,8,1));
        assertThrows(SQLException.class,()->industry.process(UUID.randomUUID(),smith.id(),recipe,8,1));
        assertThrows(SQLException.class,()->industry.process(UUID.randomUUID(),smith.id(),recipe,8,0));assertEquals(30000,money());
    }
    @Test void noInputsAndUndeliveredCargoDoNotProduceGoodsOrConsumeLaborTwice() throws Exception {
        var empty=industry.process(UUID.randomUUID(),smith.id(),ProductionRecipe.smithTools(),8,0);assertEquals(0,empty.receipt().quantity());
        industry.mine(UUID.randomUUID(),mine.id(),deposit,2,0,8,8);
        var iron=industry.purchase(UUID.randomUUID(),mine.id(),smith.id(),Commodity.IRON,4,4,0,1,128);
        var coal=industry.purchase(UUID.randomUUID(),mine.id(),smith.id(),Commodity.COAL,2,2,0,1,128);
        assertEquals(0,industry.process(UUID.randomUUID(),smith.id(),ProductionRecipe.smithTools(),8,1).receipt().quantity());
        industry.deliver(iron.id(),1);industry.deliver(coal.id(),1);
        assertThrows(SQLException.class,()->industry.process(UUID.randomUUID(),smith.id(),ProductionRecipe.smithTools(),8,1));
        assertEquals(2,industry.process(UUID.randomUUID(),smith.id(),ProductionRecipe.smithTools(),8,2).receipt().quantity());
    }
    @Test void deadWorkersStopOutputImmediatelyWithoutDeletingHistory() throws Exception {
        supply(0,2);db.getPopulationRepository().updateResident(new Resident(worker,"Fixture",town,null,300,Resident.Status.DECEASED));
        var run=industry.process(UUID.randomUUID(),smith.id(),ProductionRecipe.smithTools(),8,1);
        assertEquals(0,run.workers());assertEquals(0,run.receipt().quantity());assertEquals(4,units(smith,Commodity.IRON));
        assertEquals(1,db.getBusinessRepository().employmentHistory(smith.id()).size());
    }
    @Test void mismatchedIndustryAndUnsupportedStoreCatalogFailWithoutAnyProductionHistory() throws Exception {
        assertThrows(SQLException.class,()->industry.process(UUID.randomUUID(),mine.id(),ProductionRecipe.smithTools(),1,1));
        var bad=new ProductionRecipe("missing","blacksmith",Map.of(new Commodity("gold"),1L),Map.of(TOOLS,1L),1,1);
        assertThrows(SQLException.class,()->industry.process(UUID.randomUUID(),smith.id(),bad,1,1));assertEquals(0,count("recipe_runs"));
    }
    @Test void movedWorkersAreExcludedAndPendingCargoReservesOutputSpace() throws Exception {
        supply(0,2);var other=UUID.randomUUID();db.saveSettlement(new Settlement(other,"Other","World",50,64,0,1,0,0));
        db.getVillageIdentityRepository().bindSettlement(other,world);
        db.getPopulationRepository().updateMigratedResident(new Resident(worker,"Fixture",other,null,300,Resident.Status.ALIVE),town);
        assertEquals(0,industry.process(UUID.randomUUID(),smith.id(),ProductionRecipe.smithTools(),8,1).workers());
        db.getPopulationRepository().updateMigratedResident(new Resident(worker,"Fixture",town,null,300,Resident.Status.ALIVE),other);
        sql("UPDATE industry_stores SET capacity=8 WHERE business_id='"+smith.id()+"'");
        var pending=industry.purchase(UUID.randomUUID(),mine.id(),smith.id(),Commodity.STONE,2,2,1,1,128);
        assertEquals(2,industry.store(smith.id()).orElseThrow().reservedIncomingSpace());
        var grow=new ProductionRecipe("bulky","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(TOOLS,4L),2,8);
        assertEquals(0,industry.process(UUID.randomUUID(),smith.id(),grow,8,2).receipt().quantity());
        assertNull(industry.shipment(pending.id()).orElseThrow().deliveredPeriod());assertEquals(4,units(smith,Commodity.IRON));
    }
    @Test void outputWriteAndFlowHistoryFailuresRollBackInputsOutputsMarkersAndReceipts() throws Exception {
        supply(0,2);var operation=UUID.randomUUID();
        sql("CREATE TRIGGER fail_tool BEFORE UPDATE ON industry_stock WHEN NEW.commodity='tools' BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->industry.process(operation,smith.id(),ProductionRecipe.smithTools(),8,1));
        assertEquals(4,units(smith,Commodity.IRON));assertEquals(0,units(smith,TOOLS));assertEquals(0,count("recipe_runs"));
        sql("DROP TRIGGER fail_tool");sql("CREATE TRIGGER fail_flow BEFORE INSERT ON recipe_flows BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->industry.process(operation,smith.id(),ProductionRecipe.smithTools(),8,1));
        assertEquals(4,units(smith,Commodity.IRON));assertTrue(industry.recipeRun(operation).isEmpty());
        sql("DROP TRIGGER fail_flow");assertEquals(2,industry.process(operation,smith.id(),ProductionRecipe.smithTools(),8,1).receipt().quantity());assertEquals(0,units(smith,Commodity.IRON));
    }
    @Test void pendingStorageReservationsAndInputSpaceFreedDetermineWholeBatches() throws Exception {
        supply(0,2);sql("UPDATE industry_stores SET capacity=7 WHERE business_id='"+smith.id()+"'");
        var recipe=new ProductionRecipe("bulky","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(TOOLS,4L),2,8);
        assertEquals(1,industry.process(UUID.randomUUID(),smith.id(),recipe,8,1).receipt().quantity());
        assertEquals(0,industry.store(smith.id()).orElseThrow().freeSpace());
        assertEquals(1,industry.process(UUID.randomUUID(),smith.id(),ProductionRecipe.smithTools(),8,2).receipt().quantity());
        assertEquals(2,industry.store(smith.id()).orElseThrow().freeSpace());
    }
    @Test void schema30FailurePreservesStockFundingAndRetriesWithoutInventedRuns() throws Exception {
        supply(0,2);SchemaFixtures.rewind(db.getConnection(),30);sql("CREATE TABLE recipe_flows(bad TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(30,r.getInt(1));}
        assertEquals(4,units(smith,Commodity.IRON));assertEquals(30000,money());
        sql("DROP TABLE recipe_flows");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("recipe_runs"));assertEquals(0,count("recipe_flows"));
    }
    @Test void immutableRuntimeDiagnosticsExplainActualWorkshopProduction() throws Exception {
        supply(0,2);var runtime=new BusinessRuntime(db.getConnection(),8,16);var before=runtime.refresh(0,Set.of(world));
        assertNull(before.businesses().get(smith.id()).lastProduction());
        industry.process(UUID.randomUUID(),smith.id(),ProductionRecipe.smithTools(),8,1);var after=runtime.refresh(0,Set.of(world));
        assertTrue(after.businesses().get(smith.id()).lastProduction().contains("consumed={coal=2, iron=4}"));
        assertTrue(BusinessDiagnostics.describe(after,town).stream().anyMatch(s->s.contains("Latest industrial production")));
        assertNull(before.businesses().get(smith.id()).lastProduction());assertTrue(runtime.refresh(0,Set.of()).businesses().get(smith.id()).paused());
    }
    @Test void paidMineWarehouseWorkshop240PeriodChainConservesCapitalResourcesAndToolsAcrossRestart() throws Exception {
        long completed=0;
        for(int step=0;step<240;step++) {
            long p=3L*step;if(step==120){db.close();open();}
            industry.mine(UUID.randomUUID(),mine.id(),deposit,2,p,8,8);
            for(var e:Map.of(Commodity.IRON,4L,Commodity.COAL,2L).entrySet()) {
                var a=industry.purchase(UUID.randomUUID(),mine.id(),warehouse.id(),e.getKey(),e.getValue(),e.getValue(),p,1,128);industry.deliver(a.id(),p+1);
                var b=industry.purchase(UUID.randomUUID(),warehouse.id(),smith.id(),e.getKey(),e.getValue(),2*e.getValue(),p+1,1,128);industry.deliver(b.id(),p+2);
            }
            var id=UUID.randomUUID();var run=industry.process(id,smith.id(),ProductionRecipe.smithTools(),8,p+2);
            assertEquals(2,run.receipt().quantity());assertEquals(run,industry.process(id,smith.id(),ProductionRecipe.smithTools(),8,p+2));
            // Explicit test maintenance demand, not a fake live NPC purchase or unexplained disposal.
            industry.consume(UUID.randomUUID(),smith.id(),TOOLS,2,IndustryRepository.Use.MAINTENANCE,"Fixture machinery requires two tool units per completed cycle");completed+=2;
            assertEquals(30000,money());assertEquals(500-completed,industry.deposit(deposit).orElseThrow().remainingBatches());
            assertEquals(3*completed,units(mine,Commodity.STONE));assertEquals(0,units(smith,TOOLS));
            for(var g:List.of(Commodity.IRON,Commodity.COAL))for(var business:List.of(mine,warehouse,smith))assertEquals(0,units(business,g));
        }
        assertEquals(480,completed);assertEquals(240,count("recipe_runs"));assertEquals(720,count("recipe_flows"));
    }
}
