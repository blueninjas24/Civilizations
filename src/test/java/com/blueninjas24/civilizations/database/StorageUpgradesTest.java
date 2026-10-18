package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.Resident;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StorageUpgradesTest {
    @TempDir Path directory;IndustryRepositoryTest f;UUID seller;
    @BeforeEach void setup()throws Exception {
        f=new IndustryRepositoryTest();f.directory=directory;f.setup();f.stores(100);
        seller=f.business("craftsman",30).id();f.industry.openStore(seller,100,Map.of(StorageUpgrades.FITTINGS,1L,Commodity.TIMBER,1L,Commodity.STONE,1L),0);
        f.database.getBusinessRepository().hire(UUID.randomUUID(),f.warehouse.id(),f.founder,0);
        var worker=UUID.randomUUID();f.database.getPopulationRepository().insertResident(new Resident(worker,"Craft worker",f.town,null,300,Resident.Status.ALIVE));f.database.getBusinessRepository().hire(UUID.randomUUID(),seller,worker,0);
        f.sql("UPDATE industry_stock SET units=80 WHERE business_id='"+f.warehouse.id()+"' AND commodity='coal'");f.sql("UPDATE industry_stock SET units=8 WHERE business_id='"+seller+"' AND commodity='storage_fittings'");
        f.sql("INSERT INTO industry_runtime VALUES('"+f.warehouse.id()+"',1000,1000,0,0,0)");
    }
    @AfterEach void close(){f.close();}
    StorageUpgrades engine(){return engine(StorageUpgradePolicy.prototype());}
    StorageUpgrades engine(StorageUpgradePolicy policy){return new StorageUpgrades(f.database.getConnection(),policy,BusinessOperationPolicy.prototype());}
    StorageUpgrades.Draft draft()throws Exception{return engine().prepare(UUID.randomUUID(),f.warehouse.id(),seller,0,1,1000,Set.of(f.world)).orElseThrow();}
    IndustryRepository.Store buyer()throws Exception{return f.industry.store(f.warehouse.id()).orElseThrow();}
    long scalar(String sql)throws Exception{try(var s=f.database.getConnection().createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    Map<String,List<String>> snapshot()throws Exception {
        var data=new TreeMap<String,List<String>>();var names=new ArrayList<String>();try(var s=f.database.getConnection().createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")){while(r.next())names.add(r.getString(1));}
        for(var name:names){var rows=new ArrayList<String>();try(var s=f.database.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM \""+name.replace("\"","\"\"")+"\"")){while(r.next()){var values=new ArrayList<Object>();for(int n=1;n<=r.getMetaData().getColumnCount();n++)values.add(r.getObject(n));rows.add(values.toString());}}Collections.sort(rows);data.put(name,rows);}return data;
    }
    @Test void paidFittingsRemainOwnedCargoUntilInstallationWithoutExtendingBuyerCatalogue()throws Exception {
        var before=snapshot();var d=draft();assertEquals(before,snapshot());var catalogue=buyer().spacePerUnit();var receipt=engine().accept(d,128,Set.of(f.world));
        assertNull(receipt.installedPeriod());assertEquals(100,buyer().capacity());assertEquals(4000,buyer().hundredths());assertEquals(2000,f.industry.store(seller).orElseThrow().hundredths());
        assertEquals(7,f.industry.store(seller).orElseThrow().units().get(StorageUpgrades.FITTINGS));assertEquals(2000,StorageUpgrades.capitalValue(f.database.getConnection(),f.warehouse.id()));
        assertEquals(0,engine().deliver(f.warehouse.id(),0,Set.of(f.world)));assertEquals(0,engine().deliver(f.warehouse.id(),1,Set.of()));
        assertEquals(1,engine().deliver(f.warehouse.id(),1,Set.of(f.world)));assertEquals(200,buyer().capacity());assertEquals(catalogue,buyer().spacePerUnit());assertEquals(80,buyer().units().get(Commodity.COAL));
        assertEquals(0,engine().deliver(f.warehouse.id(),1,Set.of(f.world)));assertEquals(2000,StorageUpgrades.capitalValue(f.database.getConnection(),f.warehouse.id()));
        assertEquals(30000,new TreasuryRepository(f.database.getConnection()).load(f.town).orElseThrow().hundredths()+buyer().hundredths()+f.industry.store(seller).orElseThrow().hundredths()+f.stock(f.shop).hundredths());
    }
    @Test void exactReplaySurvivesRestartDisabledPolicyAndConsumedFittings()throws Exception {
        var d=draft();engine().accept(d,128,Set.of(f.world));engine().deliver(f.warehouse.id(),1,Set.of(f.world));f.database.close();f.open();var before=snapshot();
        var replay=engine(StorageUpgradePolicy.disabled()).accept(d,1,Set.of());assertEquals(1,replay.installedPeriod());assertEquals(before,snapshot());
        var changed=new StorageUpgrades.Draft(d.operation(),d.buyer(),d.seller(),d.period(),d.haulWindow(),d.cadence(),d.ordinal(),d.baseCapacity(),d.beforeCapacity(),d.afterCapacity(),d.units(),d.price()+1,d.duePeriod());
        assertThrows(SQLException.class,()->engine().accept(changed,128,Set.of(f.world)));assertEquals(before,snapshot());
    }
    @Test void changedNeedsFundsStaffWorldAndBacklogRefuseWithoutSpending()throws Exception {
        var d=draft();
        assertTrue(engine().prepare(UUID.randomUUID(),f.warehouse.id(),seller,0,99,1000,Set.of(f.world)).isEmpty(),"Hauling window must match the saved clock");
        for(var sql:List.of("UPDATE industry_stock SET units=1 WHERE business_id='"+f.warehouse.id()+"' AND commodity='coal'","UPDATE industry_stores SET hundredths=2500 WHERE business_id='"+f.warehouse.id()+"'","UPDATE industry_runtime SET target=1","UPDATE industry_stock SET units=0 WHERE business_id='"+seller+"' AND commodity='storage_fittings'")) {
            f.sql(sql);var before=snapshot();assertThrows(StorageUpgrades.Declined.class,()->engine().accept(d,128,Set.of(f.world)));assertEquals(before,snapshot());
            f.sql("UPDATE industry_stock SET units=80 WHERE business_id='"+f.warehouse.id()+"' AND commodity='coal'");f.sql("UPDATE industry_stores SET hundredths=6000 WHERE business_id='"+f.warehouse.id()+"'");f.sql("UPDATE industry_runtime SET target=0");f.sql("UPDATE industry_stock SET units=8 WHERE business_id='"+seller+"' AND commodity='storage_fittings'");
        }
        var before=snapshot();assertThrows(StorageUpgrades.Declined.class,()->engine().accept(d,128,Set.of()));assertEquals(before,snapshot());
        f.sql("UPDATE business_employment SET ended_step=1,end_reason='Fixture departure' WHERE business_id='"+seller+"'");before=snapshot();assertThrows(StorageUpgrades.Declined.class,()->engine().accept(d,128,Set.of(f.world)));assertEquals(before,snapshot());
    }
    @Test void paymentAndInstallationFailuresRollbackEveryRowAndRetryExactlyOnce()throws Exception {
        var d=draft();f.sql("CREATE TRIGGER reject_storage BEFORE INSERT ON business_storage_upgrades BEGIN SELECT RAISE(ABORT,'fixture final receipt failure'); END");var before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(d,128,Set.of(f.world)));assertEquals(before,snapshot());f.sql("DROP TRIGGER reject_storage");engine().accept(d,128,Set.of(f.world));
        f.sql("CREATE TRIGGER reject_install BEFORE UPDATE ON business_storage_upgrades BEGIN SELECT RAISE(ABORT,'fixture installation failure'); END");before=snapshot();
        assertThrows(SQLException.class,()->engine().deliver(f.warehouse.id(),1,Set.of(f.world)));assertEquals(before,snapshot());f.sql("DROP TRIGGER reject_install");assertEquals(1,engine().deliver(f.warehouse.id(),1,Set.of(f.world)));
    }
    @Test void pendingUpgradeAndSavedBaselineHardCapsPreventRunawayExpansion()throws Exception {
        var d=draft();engine().accept(d,128,Set.of(f.world));assertTrue(engine().prepare(UUID.randomUUID(),f.warehouse.id(),seller,0,1,1000,Set.of(f.world)).isEmpty());
        engine().deliver(f.warehouse.id(),1,Set.of(f.world));f.sql("UPDATE industry_runtime SET target=1,completed=1,last_ms=2000");f.sql("UPDATE industry_stock SET units=200 WHERE business_id='"+f.warehouse.id()+"' AND commodity='coal'");
        assertTrue(engine().prepare(UUID.randomUUID(),f.warehouse.id(),seller,1,2,1000,Set.of(f.world)).isEmpty());assertEquals(200,buyer().capacity());
        f.sql("UPDATE industry_stores SET capacity=250 WHERE business_id='"+f.warehouse.id()+"'");assertThrows(SQLException.class,()->engine().prepare(UUID.randomUUID(),f.warehouse.id(),seller,1,2,1000,Set.of(f.world)));
    }
    @Test void ordinaryCargoAndFittingsShareClockedHaulingAndOperationIdentity()throws Exception {
        var d=draft();engine().accept(d,1,Set.of(f.world));var before=snapshot();
        f.sql("UPDATE industry_stock SET units=1 WHERE business_id='"+f.mine.id()+"' AND commodity='coal'");
        f.database.getConnection().setAutoCommit(false);try{assertThrows(SQLException.class,()->f.industry.purchaseRows(UUID.randomUUID(),f.mine.id(),f.warehouse.id(),Commodity.COAL,1,1,0,1,1,1L,1000L));f.database.getConnection().rollback();}finally{f.database.getConnection().setAutoCommit(true);}
        assertThrows(SQLException.class,()->f.industry.purchase(d.operation(),f.mine.id(),f.warehouse.id(),Commodity.COAL,1,1,0,1,128));assertEquals(1,scalar("SELECT COUNT(*) FROM business_storage_upgrades"));assertEquals(100,buyer().capacity());
    }
    @Test void freshAndVersion79MigrationPreserveEveryOldRowAndRetryAfterFailure()throws Exception {
        SchemaFixtures.rewind(f.database.getConnection(),79);var before=snapshot();f.sql("CREATE VIEW business_storage_upgrades AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.database.getConnection()));assertEquals(79,scalar("PRAGMA user_version"));
        f.sql("DROP VIEW business_storage_upgrades");assertEquals(before,snapshot());new SchemaMigrator().migrate(f.database.getConnection());var after=snapshot();assertEquals(List.of(),after.remove("livestock_learning_reviews"));assertEquals(List.of(),after.remove("livestock_learning_entries"));assertEquals(List.of(),after.remove("livestock_slaughter"));assertEquals(List.of(),after.remove("quarry_founder_courses"));assertEquals(List.of(),after.remove("quarry_founder_reviews"));assertEquals(List.of(),after.remove("quarry_survey_members"));assertEquals(List.of(),after.remove("quarry_surveys"));assertEquals(List.of(),after.remove("quarry_opportunities"));assertEquals(List.of(),after.remove("active_pen_claims"));assertEquals(List.of(),after.remove("pen_releases"));assertEquals(List.of(),after.remove("livestock_presence"));assertEquals(List.of(),after.remove("livestock_birth_losses"));assertEquals(List.of(),after.remove("livestock_birth_projections"));assertEquals(List.of(),after.remove("livestock_birth_entities"));assertEquals(List.of(),after.remove("livestock_births"));assertEquals(List.of(),after.remove("livestock_product_sales"));assertEquals(List.of(),after.remove("livestock_capture"));assertEquals(List.of(),after.remove("livestock_animal_losses"));assertEquals(List.of(),after.remove("craftsman_founder_courses"));assertEquals(List.of(),after.remove("craftsman_founder_reviews"));assertEquals(List.of(),after.remove("business_storage_upgrades"));assertEquals(before,after);
        try(var s=f.database.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void realCraftingAndAutomaticPaidDeliveryIncreaseCapacityOnlyAfterArrival()throws Exception {
        f.sql("UPDATE industry_stock SET units=0 WHERE business_id='"+seller+"' AND commodity='storage_fittings'");
        f.sql("UPDATE industry_stock SET units=3 WHERE business_id='"+seller+"' AND commodity='timber'");f.sql("UPDATE industry_stock SET units=1 WHERE business_id='"+seller+"' AND commodity='stone'");
        var recipe=ProductionRecipe.storageFittings();assertEquals(1,f.industry.process(UUID.randomUUID(),seller,recipe,1,0).receipt().quantity());
        var p=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe),WorkshopSupplyPolicy.disabled(),WarehouseSupplyPolicy.disabled(),ToolUpkeepPolicy.disabled(),StorageUpgradePolicy.prototype());
        var live=new IndustryRuntime(f.database.getConnection(),p,BusinessOperationPolicy.prototype());live.start(1000);live.pulse(2000,Set.of(f.world));
        assertEquals(100,buyer().capacity());assertEquals(1,scalar("SELECT COUNT(*) FROM business_storage_upgrades"));assertEquals(4000,buyer().hundredths());
        live.pulse(100000,Set.of());assertEquals(100,buyer().capacity());f.database.close();f.open();
        live=new IndustryRuntime(f.database.getConnection(),p,BusinessOperationPolicy.prototype());live.start(100000);live.pulse(101000,Set.of(f.world));
        assertEquals(200,buyer().capacity());assertEquals(80,buyer().units().get(Commodity.COAL));assertEquals(0,f.industry.store(seller).orElseThrow().units().get(StorageUpgrades.FITTINGS));
        assertEquals(1,scalar("SELECT SUM(units) FROM recipe_flows WHERE commodity='storage_fittings' AND direction='OUTPUT'"));
        var before=snapshot();live.pulse(101000,Set.of(f.world));assertEquals(before,snapshot());
    }
    @Test void ordinaryClockedCargoAlsoBlocksAnUpgradeWhenSharedTransportIsFull()throws Exception {
        var d=draft();f.sql("UPDATE industry_stock SET units=1 WHERE business_id='"+f.mine.id()+"' AND commodity='coal'");
        f.database.getConnection().setAutoCommit(false);try{f.industry.purchaseRows(UUID.randomUUID(),f.mine.id(),f.warehouse.id(),Commodity.COAL,1,1,0,1,1,1L,1000L);f.database.getConnection().commit();}finally{f.database.getConnection().setAutoCommit(true);}
        var before=snapshot();assertThrows(StorageUpgrades.Declined.class,()->engine().accept(d,1,Set.of(f.world)));assertEquals(before,snapshot());
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void ResaleRetainsPaidFittingValueBeforeAndAfterInstallation(boolean install)throws Exception {
        var d=draft();engine().accept(d,128,Set.of(f.world));if(install)engine().deliver(f.warehouse.id(),1,Set.of(f.world));
        f.database.getPopulationRepository().updateResident(new Resident(f.founder,"Founder",f.town,null,300,Resident.Status.DECEASED));
        var policy=BusinessResalePolicy.prototype();new AutomaticBusinessResale(f.database.getConnection(),policy,BusinessPurchasePolicy.prototype(),0).review(f.warehouse.id(),1,2000);
        var listing=new BusinessSaleListings(f.database.getConnection()).find(f.warehouse.id(),f.founder).orElseThrow();
        assertEquals(policy.price(0,buyer().hundredths(),2000,buyer().units()),listing.price());
    }
    @Test void independentUpgradeCountCapSurvivesRestartAndRetuning()throws Exception {
        var p=StorageUpgradePolicy.prototype();var small=new StorageUpgradePolicy(true,80,25,2,100,8192,2500,1,800,1,8,p.industries());
        for(int n=0;n<2;n++) {
            f.sql("UPDATE industry_runtime SET target="+n+",completed="+n+",last_ms="+(1000*(n+1)));
            f.sql("UPDATE industry_stock SET units="+buyer().capacity()+" WHERE business_id='"+f.warehouse.id()+"' AND commodity='coal'");
            var d=engine(small).prepare(UUID.randomUUID(),f.warehouse.id(),seller,n,n+1,1000,Set.of(f.world)).orElseThrow();engine(small).accept(d,128,Set.of(f.world));engine(small).deliver(f.warehouse.id(),n+1,Set.of(f.world));
        }
        f.database.close();f.open();f.sql("UPDATE industry_runtime SET target=2,completed=2,last_ms=3000");f.sql("UPDATE industry_stock SET units=150 WHERE business_id='"+f.warehouse.id()+"' AND commodity='coal'");
        assertTrue(engine(small).prepare(UUID.randomUUID(),f.warehouse.id(),seller,2,3,1000,Set.of(f.world)).isEmpty());assertEquals(150,buyer().capacity());
    }
    @Test void fundedCustomerEvidenceIsReadOnlyWithoutASupplierAndAccountsForOtherPlannedSpending()throws Exception {
        f.sql("UPDATE industry_stock SET units=0 WHERE business_id='"+seller+"' AND commodity='storage_fittings'");var before=snapshot();
        var demand=new StorageUpgradeDemand(f.database.getConnection(),StorageUpgradePolicy.prototype(),BusinessOperationPolicy.prototype());
        var review=demand.review(f.town,Set.of(f.world),64,Map.of());assertTrue(review.complete());assertEquals(1,review.needs().size());assertEquals(2000,review.needs().getFirst().price());assertEquals(before,snapshot());
        assertTrue(demand.review(f.town,Set.of(f.world),64,Map.of(f.warehouse.id(),3000L)).needs().isEmpty());
        assertTrue(demand.review(f.town,Set.of(),64,Map.of()).needs().isEmpty());
        f.sql("UPDATE industry_runtime SET target=1");assertTrue(demand.review(f.town,Set.of(f.world),64,Map.of()).needs().isEmpty());
        f.sql("INSERT INTO industry_runtime VALUES('"+seller+"',1000,1000,0,0,0)");
        var capped=demand.review(f.town,Set.of(f.world),1,Map.of());assertFalse(capped.complete());assertTrue(capped.needs().isEmpty());
    }
    @Test void warehousesDoNotStockASecondFittingForTheirOwnInstallation()throws Exception {
        var recipe=ProductionRecipe.storageFittings();var prices=Map.of(Commodity.COAL,100L,StorageUpgrades.FITTINGS,800L,Commodity.TIMBER,100L,Commodity.STONE,100L);
        var idleRecipe=new ProductionRecipe("fixture_shop","shop",Map.of(Commodity.COAL,1L),Map.of(Commodity.STONE,1L),1,1);
        var p=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe,idleRecipe.id(),idleRecipe),new WorkshopSupplyPolicy(true,1,16,prices),new WarehouseSupplyPolicy(true,16,16),ToolUpkeepPolicy.disabled(),StorageUpgradePolicy.prototype());
        // The fixture's known idle shop has no staff and creates no demand; its money/stock stay intact.
        var all=new LocalIndustryDemand(f.database.getConnection(),p,BusinessOperationPolicy.prototype()).review(f.town,Set.of(f.world),16);assertTrue(all.complete());assertEquals(1,all.uncovered().get(StorageUpgrades.FITTINGS));
        var self=new LocalIndustryDemand(f.database.getConnection(),p,BusinessOperationPolicy.prototype()).excludingStorageBuyer(f.warehouse.id()).review(f.town,Set.of(f.world),16);assertTrue(self.complete());assertFalse(self.uncovered().containsKey(StorageUpgrades.FITTINGS));
        f.database.getConnection().setAutoCommit(false);try {
            new WarehouseSupplies(f.database.getConnection(),p,BusinessOperationPolicy.prototype()).acquireRows(f.warehouse.id(),f.world,1,1);f.database.getConnection().commit();
        }finally{f.database.getConnection().setAutoCommit(true);}
        assertEquals(0,scalar("SELECT COUNT(*) FROM local_shipments WHERE commodity='storage_fittings'"));assertEquals(6000,buyer().hundredths());
    }
}
