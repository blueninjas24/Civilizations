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

class WarehouseSuppliesTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=UUID.randomUUID();
    final UUID warehouse=new UUID(0,1),smith=new UUID(0,2),mine=new UUID(0,3);
    UUID deposit,warehouseWorker;
    final Commodity tools=new Commodity("tools");
    final Map<Commodity,Long> sizes=Map.of(Commodity.COAL,1L,Commodity.IRON,1L,tools,1L);
    final ProductionRecipe recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),1,1);
    IndustryRuntimePolicy policy(boolean enabled,long haul,int delay) {
        return new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(10,1,1,100,delay,haul,Map.of(Commodity.COAL,1L),sizes),Map.of(recipe.id(),recipe),
                new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L)),new WarehouseSupplyPolicy(enabled,16,16));
    }
    void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("WarehouseSuppliesTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Test","Fixture",0,64,0,4,0,0));
        var identities=db.getVillageIdentityRepository();identities.observeWorld(world,"Fixture");identities.bindSettlement(town,world);
        resident(owner);new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Real fixture capital");
        business(warehouse,"warehouse",1000);business(smith,"blacksmith",1000);business(mine,"mining",0);
        warehouseWorker=worker(warehouse);worker(smith);worker(mine);
        deposit=UUID.randomUUID();var site=db.getBusinessRepository().findFounding(mine).orElseThrow().site();
        db.getIndustryRepository().discover(deposit,site.id(),1,10,Map.of(Commodity.IRON,2L,Commodity.COAL,1L));
        db.getIndustryRepository().mine(UUID.randomUUID(),mine,deposit,2,0,2,2);
        for(var e:db.getBusinessRepository().currentWorkers(mine))db.getBusinessRepository().endEmployment(e.id(),0,"Fixture production complete");
    }
    void resident(UUID id)throws Exception {db.getPopulationRepository().insertResident(new Resident(id,"Fixture",town,null,300,Resident.Status.ALIVE));}
    UUID worker(UUID id)throws Exception {UUID person=UUID.randomUUID();resident(person);db.getBusinessRepository().hire(UUID.randomUUID(),id,person,0);return person;}
    void business(UUID id,String kind,long funds)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,(int)id.getLeastSignificantBits(),40,0,1,"Explicit fixture site");
        var b=new Business(id,town,kind,owner,owner,kind,site.id(),2,0);var req=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(b,site,req,req,0,"Actual fixture opportunity"));db.getIndustryRepository().openStore(id,100,sizes,funds);
    }
    @AfterEach void close(){if(db!=null)db.close();}
    IndustryRepository.Store store(UUID id)throws Exception{return db.getIndustryRepository().store(id).orElseThrow();}
    long count(String table)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String query)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    IndustryRuntime runtime(boolean enabled,long haul,int delay)throws Exception {var r=new IndustryRuntime(db.getConnection(),policy(enabled,haul,delay));r.start(1000);return r;}
    @Test void mineWarehouseWorkshopChainPaysTwiceAndOnlyDeliveredInputsProduce()throws Exception {
        var runtime=runtime(true,128,1);runtime.pulse(2000,Set.of(world));
        assertEquals(500,store(warehouse).hundredths());assertEquals(500,store(mine).hundredths());assertEquals(1000,store(smith).hundredths());
        assertEquals(3,store(warehouse).reservedIncomingSpace());assertEquals(0,store(warehouse).units().get(Commodity.IRON));assertEquals(0,store(smith).units().get(tools));
        runtime.pulse(3000,Set.of(world));assertEquals(1000,store(warehouse).hundredths());assertEquals(500,store(smith).hundredths());assertEquals(0,store(smith).units().get(tools));
        runtime.pulse(4000,Set.of(world));assertEquals(1,store(smith).units().get(tools));assertEquals(4,count("industry_auto_shipments"));
        assertEquals(2000,store(mine).hundredths()+store(warehouse).hundredths()+store(smith).hundredths());
        assertEquals(8,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());assertEquals(8000,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
        try(var s=db.getConnection().prepareStatement("SELECT COUNT(*) FROM industry_learning_reviews WHERE business_id=?")){s.setString(1,warehouse.toString());try(var r=s.executeQuery()){assertEquals(0,r.getLong(1));}}
    }
    @Test void competingWarehousesCountPaidPendingCargoOnceAcrossRestart()throws Exception {
        UUID second=new UUID(0,4);business(second,"warehouse",1000);worker(second);
        var runtime=runtime(true,128,3);runtime.pulse(2000,Set.of(world));assertEquals(2,count("industry_auto_shipments"));
        runtime.pulse(3000,Set.of(world));assertEquals(2,count("industry_auto_shipments"));assertEquals(1000,store(second).hundredths());
        db.close();open();runtime=new IndustryRuntime(db.getConnection(),policy(true,128,3));runtime.start(100000);runtime.pulse(100000,Set.of(world));
        assertEquals(2,count("industry_auto_shipments"));assertEquals(3,store(warehouse).reservedIncomingSpace());runtime.pulse(101000,Set.of(world));assertEquals(2,count("industry_auto_shipments"));
    }
    @Test void disabledRestockingStillDeliversPaidCargoAfterWorldReturn()throws Exception {
        var runtime=runtime(true,128,1);runtime.pulse(2000,Set.of(world));runtime.observeAvailability(2000,Set.of(world));runtime.pulse(100000,Set.of());
        assertEquals(3,store(warehouse).reservedIncomingSpace());
        runtime=new IndustryRuntime(db.getConnection(),policy(false,128,1));runtime.start(100000);runtime.pulse(101000,Set.of(world));
        assertEquals(0,store(warehouse).reservedIncomingSpace());assertEquals(4,count("industry_auto_shipments"));assertEquals(500,store(mine).hundredths());
    }
    @Test void checkpointFailureRollsBackGoodsFundsCargoAndRetry()throws Exception {
        var runtime=runtime(true,128,1);sql("CREATE TRIGGER fail_work BEFORE UPDATE OF completed ON industry_runtime WHEN NEW.business_id='"+warehouse+"' BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,Set.of(world)));assertEquals(1000,store(warehouse).hundredths());assertEquals(0,store(mine).hundredths());assertEquals(4,store(mine).units().get(Commodity.IRON));assertEquals(0,count("industry_auto_shipments"));
        sql("DROP TRIGGER fail_work");runtime.pulse(2000,Set.of(world));runtime.pulse(2000,Set.of(world));assertEquals(500,store(warehouse).hundredths());assertEquals(2,count("industry_auto_shipments"));
    }
    @Test void missingCustomerMoneyAndStaffPreventPurchases()throws Exception {
        sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+smith+"'");var runtime=runtime(true,128,1);runtime.pulse(2000,Set.of(world));assertEquals(0,count("industry_auto_shipments"));
        sql("UPDATE industry_stores SET hundredths=1000 WHERE business_id='"+smith+"'");
        for(var e:db.getBusinessRepository().currentWorkers(warehouse))db.getBusinessRepository().endEmployment(e.id(),0,"Fixture staff departure");
        runtime.pulse(3000,Set.of(world));assertEquals(0,count("industry_auto_shipments"));assertEquals(1000,store(warehouse).hundredths());
    }
    @Test void sharedHaulingCapBoundsBothLegs()throws Exception {
        var runtime=runtime(true,3,1);runtime.pulse(2000,Set.of(world));assertEquals(2,count("industry_auto_shipments"));
        runtime.pulse(3000,Set.of(world));assertEquals(4,count("industry_auto_shipments"));
        runtime.pulse(4000,Set.of(world));assertEquals(1,store(smith).units().get(tools));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT SUM(t.units) FROM industry_auto_shipments a JOIN local_shipments t ON t.id=a.shipment_id GROUP BY haul_window,haul_cadence")){while(r.next())assertTrue(r.getLong(1)<=3);}
    }
    @Test void approvedStaffedOperationContinuesAfterOwnerDeath()throws Exception {
        db.getPopulationRepository().updateResident(new Resident(owner,"Fixture",town,null,300,Resident.Status.DECEASED));
        var runtime=new IndustryRuntime(db.getConnection(),policy(true,128,1),BusinessOperationPolicy.prototype());runtime.start(1000);
        // Producer needs qualified staff too while for sale, even to sell its saved stock.
        runtime.pulse(2000,Set.of(world));assertEquals(0,count("industry_auto_shipments"));
        worker(mine);runtime.pulse(3000,Set.of(world));assertEquals(2,count("industry_auto_shipments"));assertEquals(500,store(warehouse).hundredths());
    }
    @Test void exceededProducerBudgetDefersRatherThanInferringPartialSupply()throws Exception {
        business(new UUID(0,4),"mining",0);
        var base=policy(true,128,1);var bounded=new IndustryRuntimePolicy(true,1000,8,base.mining(),base.recipes(),base.supply(),new WarehouseSupplyPolicy(true,16,1));
        var runtime=new IndustryRuntime(db.getConnection(),bounded);runtime.start(1000);runtime.pulse(2000,Set.of(world));assertEquals(0,count("industry_auto_shipments"));
    }
    @Test void absentWorldAndMissingProducerGoodsNeverSpendOnPromisedReserves()throws Exception {
        var runtime=runtime(true,128,1);runtime.pulse(2000,Set.of());assertEquals(0,count("industry_auto_shipments"));
        sql("UPDATE industry_stock SET units=0 WHERE business_id='"+mine+"'");runtime.pulse(3000,Set.of(world));assertEquals(0,count("industry_auto_shipments"));assertEquals(1000,store(warehouse).hundredths());assertEquals(8,db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
    }
    IndustryRuntime commerce(WorkLearningPolicy learning)throws Exception {
        var r=new IndustryRuntime(db.getConnection(),policy(true,128,1),BusinessOperationPolicy.prototype(),learning);r.start(1000);return r;
    }
    long commerceExperience(UUID person)throws Exception {
        return db.getPopulationRepository().findWorkProfile(person).orElseThrow().experience().getOrDefault("commerce",0L);
    }
    @Test void actualPaidOrdersTrainEmployeesOnceNotOwnersIdlePeriodsOrAbsentWorlds()throws Exception {
        var r=commerce(WorkLearningPolicy.prototype());r.pulse(2000,Set.of(world));
        assertEquals(1,commerceExperience(warehouseWorker));assertEquals(0,commerceExperience(owner));
        assertEquals(1,count("commerce_learning_reviews"));assertEquals(2,count("commerce_learning_receipts"));assertEquals(1,count("commerce_learning_entries"));
        r.pulse(2000,Set.of(world));r.pulse(3000,Set.of(world));r.pulse(100000,Set.of());
        assertEquals(1,commerceExperience(warehouseWorker));assertEquals(1,count("commerce_learning_reviews"));
        db.close();open();r=new IndustryRuntime(db.getConnection(),policy(true,128,1),BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());r.start(100000);
        r.pulse(100000,Set.of(world));assertEquals(1,commerceExperience(warehouseWorker));
        assertEquals(2000,store(mine).hundredths()+store(warehouse).hundredths()+store(smith).hundredths());
        try(var q=db.getConnection().prepareStatement("SELECT COUNT(*) FROM industry_learning_reviews WHERE business_id=?")){q.setString(1,warehouse.toString());try(var rows=q.executeQuery()){assertEquals(0,rows.getLong(1));}} // No phantom warehouse production.
    }
    @Test void commerceCheckpointFailureRollsBackSkillPaymentsAndReceiptsTogether()throws Exception {
        var r=commerce(WorkLearningPolicy.prototype());sql("CREATE TRIGGER fail_commerce BEFORE UPDATE OF completed ON industry_runtime WHEN NEW.business_id='"+warehouse+"' BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->r.pulse(2000,Set.of(world)));
        assertEquals(0,commerceExperience(warehouseWorker));assertEquals(0,count("commerce_learning_reviews"));assertEquals(0,count("commerce_learning_receipts"));assertEquals(0,count("local_shipments"));assertEquals(1000,store(warehouse).hundredths());
        sql("DROP TRIGGER fail_commerce");r.pulse(2000,Set.of(world));assertEquals(1,commerceExperience(warehouseWorker));assertEquals(2,count("commerce_learning_receipts"));
    }
    @Test void failedCommerceAuditRollsBackActualTradeAndRetriesOnce()throws Exception {
        var r=commerce(WorkLearningPolicy.prototype());sql("CREATE TRIGGER fail_commerce_audit BEFORE INSERT ON commerce_learning_entries BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->r.pulse(2000,Set.of(world)));assertEquals(0,commerceExperience(warehouseWorker));assertEquals(0,count("local_shipments"));assertEquals(0,count("commerce_learning_reviews"));
        sql("DROP TRIGGER fail_commerce_audit");r.pulse(2000,Set.of(world));r.pulse(2000,Set.of(world));assertEquals(1,commerceExperience(warehouseWorker));
    }
    @Test void disabledCommerceNeverBackfillsAwards()throws Exception {
        var r=commerce(WorkLearningPolicy.disabled());r.pulse(2000,Set.of(world));assertEquals(0,commerceExperience(warehouseWorker));assertEquals(2,count("commerce_learning_receipts"));
        r=new IndustryRuntime(db.getConnection(),policy(true,128,1),BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());r.pulse(3000,Set.of(world));assertEquals(0,commerceExperience(warehouseWorker));
        // No genuine replacement orders exist yet. A fresh fixture is not required to prove no backfill.
        assertEquals(0,count("commerce_learning_entries"));
    }
    @Test void workerLimitDefersAllCommerceAwardsButPreservesPaidOrders()throws Exception {
        UUID second=worker(warehouse);var r=commerce(new WorkLearningPolicy(true,1,100,1,Map.of("warehouse","commerce")));r.pulse(2000,Set.of(world));
        assertEquals(0,commerceExperience(warehouseWorker));assertEquals(0,commerceExperience(second));assertEquals(2,count("commerce_learning_receipts"));assertEquals(500,store(warehouse).hundredths());
    }
    @Test void failedSchema55CommerceUpgradePreservesAssetsAndRetriesWithoutHistoricalAwards()throws Exception {
        var r=runtime(true,128,1);r.pulse(2000,Set.of(world));SchemaFixtures.rewind(db.getConnection(),55);sql("CREATE TABLE commerce_learning_entries(id TEXT)");db.close();
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("CommerceMigration"));assertThrows(SQLException.class,db::initialize);db.close();
        try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("civilizations.db"));var s=c.createStatement()) {
            try(var rows=s.executeQuery("PRAGMA user_version")){assertEquals(55,rows.getInt(1));}
            try(var rows=s.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='commerce_learning_reviews'")){assertEquals(0,rows.getInt(1));}
            s.execute("DROP TABLE commerce_learning_entries");
        }
        open();assertEquals(0,count("commerce_learning_reviews"));assertEquals(500,store(warehouse).hundredths());assertEquals(3,store(warehouse).reservedIncomingSpace());
        r=new IndustryRuntime(db.getConnection(),policy(true,128,1),BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());r.start(100000);r.pulse(101000,Set.of(world));assertEquals(0,commerceExperience(warehouseWorker));
    }

    @Test void unmappedCommerceAndSavedCeilingNeverInventOrEraseSkills()throws Exception {
        db.getPopulationRepository().recordSkillExperience(warehouseWorker,"commerce",7);
        var r=commerce(new WorkLearningPolicy(true,1,5,64,Map.of("warehouse","commerce")));r.pulse(2000,Set.of(world));assertEquals(7,commerceExperience(warehouseWorker));assertEquals(0,count("commerce_learning_entries"));
        assertEquals(2,count("commerce_learning_receipts"));
    }
    @Test void unrelatedIndustryMappingCannotTrainWarehouseStaff()throws Exception {
        var r=commerce(new WorkLearningPolicy(true,1,100,64,Map.of("mining","mining")));r.pulse(2000,Set.of(world));
        assertEquals(0,commerceExperience(warehouseWorker));assertEquals(0,count("commerce_learning_entries"));assertEquals(2,count("commerce_learning_receipts"));
    }

}
