package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorkshopSuppliesTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=UUID.randomUUID();
    UUID warehouse,smith,mine,deposit;
    int siteOffset;
    final Commodity tools=new Commodity("tools");
    final Map<Commodity,Long> sizes=Map.of(Commodity.COAL,1L,Commodity.IRON,1L,Commodity.STONE,1L,tools,1L);
    final ProductionRecipe recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),1,1);
    final WorkshopSupplyPolicy supply=new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L));
    IndustryRuntimePolicy policy(WorkshopSupplyPolicy supplies,long haul) {return new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(10,1,1,100,1,haul,Map.of(Commodity.COAL,1L),sizes),Map.of(recipe.id(),recipe),supplies);}
    void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("WorkshopSuppliesTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Test","Fixture",0,64,0,3,0,0));
        var identities=db.getVillageIdentityRepository();identities.observeWorld(world,"Fixture");identities.bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(owner,"Owner",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Real fixture capital");
        warehouse=business("warehouse",null,0);smith=business("blacksmith",null,1000);mine=business("mining",new UUID(0,300),0);
        UUID worker=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(worker,"Worker",town,null,300,Resident.Status.ALIVE));
        db.getBusinessRepository().hire(UUID.randomUUID(),smith,worker,0);
        UUID miner=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(miner,"Miner",town,null,300,Resident.Status.ALIVE));
        db.getBusinessRepository().hire(UUID.randomUUID(),mine,miner,0);
        deposit=UUID.randomUUID();var site=db.getBusinessRepository().findFounding(mine).orElseThrow().site();
        db.getIndustryRepository().discover(deposit,site.id(),1,10,Map.of(Commodity.IRON,2L,Commodity.COAL,1L));
        db.getIndustryRepository().mine(UUID.randomUUID(),mine,deposit,2,0,2,2);
        for(var item:Map.of(Commodity.IRON,4L,Commodity.COAL,2L).entrySet()) {
            var cargo=db.getIndustryRepository().purchase(UUID.randomUUID(),mine,warehouse,item.getKey(),item.getValue(),0,0,1,128);
            db.getIndustryRepository().deliver(cargo.id(),1);
        }
        // Producer remains an actual source, but no further mine work in these supply tests.
        for(var e:db.getBusinessRepository().currentWorkers(mine))db.getBusinessRepository().endEmployment(e.id(),0,"Fixture production complete");
    }
    UUID business(String kind,UUID id,long funds)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,siteOffset++,40,0,1,"Explicit fixture site");
        var b=new Business(id==null?UUID.randomUUID():id,town,kind,owner,owner,kind,site.id(),2,0);
        var req=new JobRequirements(216,Map.of());db.getBusinessRepository().found(new BusinessFounding(b,site,req,req,0,"Actual fixture opportunity"));
        db.getIndustryRepository().openStore(b.id(),100,sizes,funds);return b.id();
    }
    @AfterEach void close(){if(db!=null)db.close();}
    IndustryRepository.Store store(UUID id)throws Exception{return db.getIndustryRepository().store(id).orElseThrow();}
    long count(String table)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String query)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    @Test void actualCustomerPaysWaitsProducesAndRestartCannotRepeatPayment()throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128));runtime.start(1000);runtime.pulse(2000,Set.of(world));
        assertEquals(500,store(smith).hundredths());assertEquals(500,store(warehouse).hundredths());assertEquals(0,store(smith).units().get(tools));
        assertEquals(0,store(smith).units().get(Commodity.IRON));assertEquals(3,store(smith).reservedIncomingSpace());assertEquals(2,count("industry_auto_shipments"));
        runtime.pulse(2000,Set.of(world));assertEquals(500,store(smith).hundredths());
        db.close();open();runtime=new IndustryRuntime(db.getConnection(),policy(supply,128));runtime.start(100000);
        runtime.pulse(100000,Set.of(world));assertEquals(0,store(smith).units().get(tools));
        runtime.pulse(101000,Set.of(world));assertEquals(1,store(smith).units().get(tools));assertEquals(500,store(smith).hundredths());assertEquals(0,store(smith).reservedIncomingSpace());
        runtime.pulse(102000,Set.of(world));assertEquals(0,store(smith).hundredths());assertEquals(1000,store(warehouse).hundredths());
        runtime.pulse(103000,Set.of(world));assertEquals(2,store(smith).units().get(tools));assertEquals(0,store(warehouse).units().get(Commodity.IRON));
        runtime.pulse(104000,Set.of(world));assertEquals(2,store(smith).units().get(tools));assertEquals(4,count("industry_auto_shipments"));
        assertEquals(9000,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
    }
    @Test void staffedWarehouseAndWorkshopContinueConservedPurchasesAfterOwnerDeath()throws Exception {
        UUID staff=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(staff,"Warehouse Staff",town,null,300,Resident.Status.ALIVE));
        db.getBusinessRepository().hire(UUID.randomUUID(),warehouse,staff,0);
        db.getPopulationRepository().updateResident(new Resident(owner,"Owner",town,null,300,Resident.Status.DECEASED));
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128),BusinessOperationPolicy.prototype());runtime.start(1000);
        runtime.pulse(2000,Set.of(world));assertEquals(500,store(smith).hundredths());assertEquals(500,store(warehouse).hundredths());
        runtime.pulse(3000,Set.of(world));assertEquals(1,store(smith).units().get(tools));assertEquals(500,store(smith).hundredths());
        assertEquals(owner,db.getBusinessRepository().findBusiness(smith).orElseThrow().ownerId());assertEquals(0,count("business_ownership"));assertEquals(0,count("family_estates"));
    }
    @Test void warehouseWithoutStaffCannotSellAutomaticallyAfterOwnerDeath()throws Exception {
        db.getPopulationRepository().updateResident(new Resident(owner,"Owner",town,null,300,Resident.Status.DECEASED));
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128),BusinessOperationPolicy.prototype());runtime.start(1000);runtime.pulse(2000,Set.of(world));
        assertEquals(1000,store(smith).hundredths());assertEquals(0,count("industry_auto_shipments"));assertEquals(4,store(warehouse).units().get(Commodity.IRON));
    }
    @Test void checkpointFailureRollsBackPurchasesMoneyTagsThenDeliveryAndRecipe()throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128));runtime.start(1000);
        sql("CREATE TRIGGER fail_work BEFORE UPDATE OF completed ON industry_runtime WHEN NEW.business_id='"+smith+"' BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,Set.of(world)));assertEquals(1000,store(smith).hundredths());assertEquals(0,count("industry_auto_shipments"));assertEquals(4,store(warehouse).units().get(Commodity.IRON));
        sql("DROP TRIGGER fail_work");runtime.pulse(2000,Set.of(world));
        sql("CREATE TRIGGER fail_work BEFORE UPDATE OF completed ON industry_runtime WHEN NEW.business_id='"+smith+"' BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(3000,Set.of(world)));assertEquals(0,store(smith).units().get(tools));assertEquals(3,store(smith).reservedIncomingSpace());
        sql("DROP TRIGGER fail_work");runtime.pulse(3000,Set.of(world));assertEquals(1,store(smith).units().get(tools));assertEquals(2,count("industry_auto_shipments"));
    }
    @Test void disabledNewPurchasesStillDeliverAlreadyOwnedCargoAfterWorldReturn()throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128));runtime.start(1000);runtime.pulse(2000,Set.of(world));
        runtime.observeAvailability(2000,Set.of(world));runtime.pulse(100000,Set.of());assertEquals(0,store(smith).units().get(tools));
        runtime=new IndustryRuntime(db.getConnection(),policy(WorkshopSupplyPolicy.disabled(),128));runtime.start(100000);
        runtime.pulse(101000,Set.of(world));assertEquals(1,store(smith).units().get(tools));
        runtime.pulse(102000,Set.of(world));assertEquals(1,store(smith).units().get(tools));assertEquals(500,store(smith).hundredths());
    }
    @Test void unavailableOwnerDoesNotBuyButPaidCargoRemainsOwnedAndArrives()throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128));runtime.start(1000);runtime.pulse(2000,Set.of(world));
        db.getPopulationRepository().updateResident(new Resident(owner,"Owner",town,null,300,Resident.Status.DECEASED));
        runtime.pulse(3000,Set.of(world));assertEquals(2,store(smith).units().get(Commodity.IRON));assertEquals(0,store(smith).units().get(tools));
        assertEquals(500,store(smith).hundredths());assertEquals(2,count("industry_auto_shipments"));
    }
    @Test void legacyCargoCountsForOrderingButNeverUsesPrivateAutomaticClock()throws Exception {
        for(var e:Map.of(Commodity.IRON,2L,Commodity.COAL,1L).entrySet())db.getIndustryRepository().purchase(UUID.randomUUID(),warehouse,smith,e.getKey(),e.getValue(),100,100,1,128);
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128));runtime.start(1000);
        runtime.pulse(2000,Set.of(world));runtime.pulse(3000,Set.of(world));
        assertEquals(0,count("industry_auto_shipments"));assertEquals(0,store(smith).units().get(tools));assertEquals(3,store(smith).reservedIncomingSpace());assertEquals(800,store(smith).hundredths());
    }
    @Test void warehouseMustHaveEligibleOwnerAndActualStockAndWorkshopNeedsLabor()throws Exception {
        for(var e:db.getBusinessRepository().currentWorkers(smith))db.getBusinessRepository().endEmployment(e.id(),0,"No workforce");
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128));runtime.start(1000);runtime.pulse(2000,Set.of(world));assertEquals(0,count("industry_auto_shipments"));
        UUID worker=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(worker,"Worker",town,null,300,Resident.Status.ALIVE));db.getBusinessRepository().hire(UUID.randomUUID(),smith,worker,0);
        sql("UPDATE industry_stock SET units=0 WHERE business_id='"+warehouse+"' AND commodity='iron'");
        runtime.pulse(3000,Set.of(world));assertEquals(0,count("industry_auto_shipments"));assertEquals(1000,store(smith).hundredths());
    }
    @Test void sharedHaulingWindowBoundsDifferentPrivatePeriodsAndMigrationIsTransactional()throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128));runtime.start(1000);
        db.getConnection().setAutoCommit(false);
        db.getIndustryRepository().purchaseRows(UUID.randomUUID(),warehouse,smith,Commodity.COAL,1,100,1,1,1,2L);
        assertThrows(SQLException.class,()->db.getIndustryRepository().purchaseRows(UUID.randomUUID(),warehouse,smith,Commodity.COAL,1,100,99,1,1,2L));
        db.getIndustryRepository().purchaseRows(UUID.randomUUID(),warehouse,smith,Commodity.COAL,1,100,99,1,1,3L);
        db.getConnection().commit();db.getConnection().setAutoCommit(true);assertEquals(2,count("industry_auto_shipments"));
        // Wrong buyer clock cannot adopt a shipment, even through direct SQL.
        sql("INSERT INTO industry_runtime(business_id,cadence,last_ms,remainder,target,completed) VALUES('"+warehouse+"',1000,1000,0,0,0)");
        assertThrows(SQLException.class,()->sql("UPDATE industry_auto_shipments SET clock_business_id='"+warehouse+"'"));
        SchemaFixtures.rewind(db.getConnection(),35);sql("CREATE TABLE industry_auto_shipments(bad TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(35,r.getInt(1));}
        sql("DROP TABLE industry_auto_shipments");new SchemaMigrator().migrate(db.getConnection());
        assertEquals(0,count("industry_auto_shipments"));assertEquals(800,store(smith).hundredths());assertEquals(2,store(smith).reservedIncomingSpace());
    }
    @Test void slowerIndustryCadenceCannotMistakeHistoricHaulingForCurrentCapacity()throws Exception {
        UUID otherBuyer=business("blacksmith",null,10);
        db.getBusinessRepository().hire(UUID.randomUUID(),mine,owner,0);
        db.getIndustryRepository().mine(UUID.randomUUID(),mine,deposit,1,1,1,1);
        var oldPolicy=policy(supply,3);new IndustryRuntime(db.getConnection(),oldPolicy).start(1000);
        db.getConnection().setAutoCommit(false);
        for(var e:Map.of(Commodity.IRON,2L,Commodity.COAL,1L).entrySet())
            db.getIndustryRepository().purchaseRows(UUID.randomUUID(),mine,otherBuyer,e.getKey(),e.getValue(),1,100,1,3,99L);
        db.getConnection().commit();db.getConnection().setAutoCommit(true);
        // Window 99 under 1000 ms was a different time from window 99 under 2000 ms.
        var slower=new IndustryRuntime(db.getConnection(),new IndustryRuntimePolicy(true,2000,8,oldPolicy.mining(),oldPolicy.recipes(),supply));
        slower.start(196000);slower.pulse(198000,Set.of(world));
        assertEquals(500,store(smith).hundredths(),"Old clock namespace must not exhaust current transport");
        assertEquals(4,count("industry_auto_shipments"));
    }
    @Test void cadenceIdentitySharesCurrentQuotaPreservesReplayAndKeepsManualNamespace()throws Exception {
        new IndustryRuntime(db.getConnection(),policy(supply,128)).start(1000);
        UUID operation=UUID.randomUUID();db.getConnection().setAutoCommit(false);
        var first=db.getIndustryRepository().purchaseRows(operation,warehouse,smith,Commodity.COAL,1,100,0,1,1,99L,1000L);
        assertEquals(first,db.getIndustryRepository().purchaseRows(operation,warehouse,smith,Commodity.COAL,1,100,0,1,1,99L,1000L));
        assertThrows(SQLException.class,()->db.getIndustryRepository().purchaseRows(operation,warehouse,smith,Commodity.COAL,1,100,0,1,1,99L,2000L));
        assertThrows(SQLException.class,()->db.getIndustryRepository().purchaseRows(UUID.randomUUID(),warehouse,smith,Commodity.COAL,1,100,99,1,1,99L,1000L));
        assertThrows(IllegalArgumentException.class,()->db.getIndustryRepository().purchaseRows(UUID.randomUUID(),warehouse,smith,Commodity.COAL,1,100,99,1,1,null,1000L));
        db.getIndustryRepository().purchaseRows(UUID.randomUUID(),warehouse,smith,Commodity.COAL,1,100,99,1,1,99L,2000L);
        db.getConnection().commit();db.getConnection().setAutoCommit(true);
        assertEquals(800,store(smith).hundredths());assertEquals(200,store(warehouse).hundredths());assertEquals(2,count("industry_auto_shipments"));
        db.getIndustryRepository().purchase(UUID.randomUUID(),warehouse,smith,Commodity.IRON,1,1,99,1,1);
        assertEquals(799,store(smith).hundredths());assertEquals(201,store(warehouse).hundredths());
    }
    @Test void schema36UpgradeRollsBackThenRetainsAndDeliversLegacyAutomaticCargo()throws Exception {
        var runtime=new IndustryRuntime(db.getConnection(),policy(supply,128));runtime.start(1000);runtime.pulse(2000,Set.of(world));
        SchemaFixtures.rewind(db.getConnection(),36);
        sql("CREATE INDEX auto_shipment_haul_cadence ON industry_auto_shipments(haul_window)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(36,r.getInt(1));}
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM pragma_table_info('industry_auto_shipments') WHERE name='haul_cadence'")){assertEquals(0,r.getInt(1));}
        assertEquals(500,store(smith).hundredths());assertEquals(3,store(smith).reservedIncomingSpace());assertEquals(2,count("industry_auto_shipments"));
        sql("DROP INDEX auto_shipment_haul_cadence");new SchemaMigrator().migrate(db.getConnection());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM industry_auto_shipments WHERE haul_cadence IS NULL")){assertEquals(2,r.getInt(1));}
        runtime.start(100000);runtime.pulse(101000,Set.of(world));assertEquals(1,store(smith).units().get(tools));
        assertEquals(0,store(smith).reservedIncomingSpace());assertEquals(500,store(smith).hundredths());assertEquals(500,store(warehouse).hundredths());
    }
}
