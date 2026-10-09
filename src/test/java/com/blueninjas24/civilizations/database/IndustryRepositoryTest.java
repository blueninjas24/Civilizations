package com.blueninjas24.civilizations.database;

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

class IndustryRepositoryTest {
    @TempDir Path directory;
    DatabaseManager database;
    IndustryRepository industry;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),founder=UUID.randomUUID(),deposit=UUID.randomUUID();
    Business mine,warehouse,shop;
    static final Map<Commodity,Long> BUNDLE=Map.of(Commodity.COAL,1L,Commodity.STONE,3L);
    static final Map<Commodity,Long> SPACE=Map.of(Commodity.COAL,1L,Commodity.STONE,1L);
    @BeforeEach void setup() throws SQLException {
        open();database.saveSettlement(new Settlement(town,"Test","Test World",0,64,0,1,0,0));
        var identity=database.getVillageIdentityRepository();identity.observeWorld(world,"Test World");identity.bindSettlement(town,world);
        database.getPopulationRepository().insertResident(new Resident(founder,"Founder",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(database.getConnection()).initialize(UUID.randomUUID(),town,30000,"Test-only opening funds");
        mine=business("mining",0);warehouse=business("warehouse",10);shop=business("shop",20);
        for(int i=0;i<3;i++) {
            UUID worker=UUID.randomUUID();database.getPopulationRepository().insertResident(new Resident(worker,"Miner",town,null,300,Resident.Status.ALIVE));
            database.getBusinessRepository().hire(UUID.randomUUID(),mine.id(),worker,0);
        }
    }
    void open() throws SQLException {
        database=new DatabaseManager(directory.toFile(),Logger.getLogger("IndustryTest"));database.initialize();industry=database.getIndustryRepository();
    }
    @AfterEach void close() { database.close(); }
    Business business(String kind,int x) throws SQLException {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,-20,0,1,"Trusted test existing site");
        var business=new Business(UUID.randomUUID(),town,kind,founder,founder,kind,site.id(),4,0);
        database.getBusinessRepository().found(new BusinessFounding(business,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Test opportunity"));
        return business;
    }
    void stores(long capacity) throws SQLException {
        industry.openStore(mine.id(),capacity,SPACE,0);industry.openStore(warehouse.id(),capacity,SPACE,6000);industry.openStore(shop.id(),capacity,SPACE,5000);
    }
    IndustryRepository.Store stock(Business business) throws SQLException { return industry.store(business.id()).orElseThrow(); }
    void sql(String text) throws SQLException {try(var s=database.getConnection().createStatement()){s.execute(text);}}
    long quantity(Business business,Commodity commodity) throws SQLException {return stock(business).units().get(commodity);}
    long allMoney() throws SQLException {
        return new TreasuryRepository(database.getConnection()).load(town).orElseThrow().hundredths()+stock(mine).hundredths()+stock(warehouse).hundredths()+stock(shop).hundredths();
    }

    @Test void finiteMineDepletionStorageAndExactReplaySurviveRestartWithoutRefill() throws SQLException {
        stores(1024);industry.discover(deposit,mine.siteId(),8,30,BUNDLE);
        UUID first=UUID.randomUUID();var receipt=industry.mine(first,mine.id(),deposit,100,0,8,32);
        assertEquals(24,receipt.quantity());assertEquals(24,quantity(mine,Commodity.COAL));assertEquals(72,quantity(mine,Commodity.STONE));
        assertEquals(6,industry.deposit(deposit).orElseThrow().remainingBatches());
        assertEquals(receipt,industry.mine(first,mine.id(),deposit,100,0,8,32));
        assertThrows(SQLException.class,()->industry.mine(UUID.randomUUID(),mine.id(),deposit,100,0,8,32));
        database.close();open();
        assertEquals(6,industry.discover(deposit,mine.siteId(),8,30,BUNDLE).remainingBatches());
        assertThrows(SQLException.class,()->industry.discover(UUID.randomUUID(),mine.siteId(),8,30,BUNDLE));
        assertThrows(SQLException.class,()->industry.discover(deposit,mine.siteId(),8,3000,BUNDLE));
        assertEquals(6,industry.mine(UUID.randomUUID(),mine.id(),deposit,100,1,8,32).quantity());
        assertEquals(0,industry.mine(UUID.randomUUID(),mine.id(),deposit,100,2,8,32).quantity());
        assertEquals(30,quantity(mine,Commodity.COAL));assertEquals(90,quantity(mine,Commodity.STONE));
        assertEquals(0,industry.deposit(deposit).orElseThrow().remainingBatches());assertEquals(30000,allMoney());
    }

    @Test void warehouseBuysAndResellsActualGoodsMoneyAndDelayedOwnershipExactlyOnce() throws SQLException {
        stores(1024);industry.discover(deposit,mine.siteId(),8,1200,BUNDLE);industry.mine(UUID.randomUUID(),mine.id(),deposit,100,0,8,32);
        UUID purchase=UUID.randomUUID();var shipment=industry.purchase(purchase,mine.id(),warehouse.id(),Commodity.COAL,24,200,0,1,128);
        assertEquals(shipment,industry.purchase(purchase,mine.id(),warehouse.id(),Commodity.COAL,24,200,0,1,128));
        assertEquals(0,quantity(mine,Commodity.COAL));assertEquals(0,quantity(warehouse,Commodity.COAL));assertEquals(24,stock(warehouse).reservedIncomingSpace());
        assertEquals(200,stock(mine).hundredths());assertEquals(5800,stock(warehouse).hundredths());assertEquals(30000,allMoney());
        assertThrows(SQLException.class,()->industry.deliver(purchase,0));
        assertThrows(SQLException.class,()->industry.purchase(UUID.randomUUID(),warehouse.id(),shop.id(),Commodity.COAL,24,300,0,1,128));
        database.close();open();industry.deliver(purchase,1);industry.deliver(purchase,2);
        assertEquals(24,quantity(warehouse,Commodity.COAL));assertEquals(0,stock(warehouse).reservedIncomingSpace());
        UUID resale=UUID.randomUUID();industry.purchase(resale,warehouse.id(),shop.id(),Commodity.COAL,24,360,1,1,128);industry.deliver(resale,2);
        assertEquals(0,quantity(warehouse,Commodity.COAL));assertEquals(24,quantity(shop,Commodity.COAL));
        assertEquals(6160,stock(warehouse).hundredths());assertEquals(4640,stock(shop).hundredths());assertEquals(30000,allMoney());
        UUID use=UUID.randomUUID();var used=industry.consume(use,shop.id(),Commodity.COAL,4,IndustryRepository.Use.HOUSEHOLD_NEED,"Explicit test cooking fuel demand");
        assertEquals(used,industry.consume(use,shop.id(),Commodity.COAL,4,IndustryRepository.Use.HOUSEHOLD_NEED,"Explicit test cooking fuel demand"));
        assertEquals(20,quantity(shop,Commodity.COAL));
        assertThrows(SQLException.class,()->industry.consume(use,shop.id(),Commodity.COAL,4,IndustryRepository.Use.MAINTENANCE,"Changed reason"));
        assertThrows(SQLException.class,()->industry.consume(UUID.randomUUID(),shop.id(),Commodity.COAL,21,IndustryRepository.Use.HOUSEHOLD_NEED,"Demand exceeds supplies"));
    }

    @Test void fullStorageLimitsBothOreAndByproductsAndReservesPendingDeliveries() throws SQLException {
        stores(20);industry.discover(deposit,mine.siteId(),8,1200,BUNDLE);
        assertEquals(5,industry.mine(UUID.randomUUID(),mine.id(),deposit,100,0,8,32).quantity());
        assertEquals(0,stock(mine).freeSpace());assertEquals(0,industry.mine(UUID.randomUUID(),mine.id(),deposit,100,1,8,32).quantity());
        UUID purchase=UUID.randomUUID();industry.purchase(purchase,mine.id(),warehouse.id(),Commodity.STONE,15,30,1,1,128);
        assertEquals(5,stock(warehouse).freeSpace());
        industry.purchase(UUID.randomUUID(),mine.id(),warehouse.id(),Commodity.COAL,5,10,1,1,128);
        assertEquals(0,stock(warehouse).freeSpace());
        industry.mine(UUID.randomUUID(),mine.id(),deposit,100,2,8,32);
        assertThrows(SQLException.class,()->industry.purchase(UUID.randomUUID(),mine.id(),warehouse.id(),Commodity.COAL,1,1,2,1,128));
        assertEquals(5,quantity(mine,Commodity.COAL));industry.deliver(purchase,2);assertEquals(0,stock(warehouse).freeSpace());
    }

    @Test void miningWriteFailureRollsBackDepositAllByproductsAndProgressForRetry() throws SQLException {
        stores(1024);industry.discover(deposit,mine.siteId(),8,1200,BUNDLE);
        sql("CREATE TRIGGER fail_mining_receipt BEFORE INSERT ON industry_operations BEGIN SELECT RAISE(ABORT,'test receipt failure'); END");
        UUID operation=UUID.randomUUID();assertThrows(SQLException.class,()->industry.mine(operation,mine.id(),deposit,100,0,8,32));
        assertEquals(1200,industry.deposit(deposit).orElseThrow().remainingBatches());assertEquals(0,stock(mine).usedSpace());
        sql("DROP TRIGGER fail_mining_receipt");assertEquals(24,industry.mine(operation,mine.id(),deposit,100,0,8,32).quantity());
    }

    @Test void failedPurchaseAndDeliveryNeverLoseMoneyDuplicateGoodsOrReleaseSpaceEarly() throws SQLException {
        stores(1024);industry.discover(deposit,mine.siteId(),8,1200,BUNDLE);industry.mine(UUID.randomUUID(),mine.id(),deposit,100,0,8,32);
        sql("CREATE TRIGGER fail_dispatch BEFORE INSERT ON local_shipments BEGIN SELECT RAISE(ABORT,'test shipment failure'); END");
        UUID operation=UUID.randomUUID();assertThrows(SQLException.class,()->industry.purchase(operation,mine.id(),warehouse.id(),Commodity.COAL,24,200,0,1,128));
        assertEquals(24,quantity(mine,Commodity.COAL));assertEquals(0,stock(mine).hundredths());assertEquals(6000,stock(warehouse).hundredths());
        sql("DROP TRIGGER fail_dispatch");industry.purchase(operation,mine.id(),warehouse.id(),Commodity.COAL,24,200,0,1,128);
        sql("CREATE TRIGGER fail_arrival BEFORE UPDATE ON local_shipments BEGIN SELECT RAISE(ABORT,'test arrival failure'); END");
        assertThrows(SQLException.class,()->industry.deliver(operation,1));assertEquals(0,quantity(warehouse,Commodity.COAL));assertEquals(24,stock(warehouse).reservedIncomingSpace());
        sql("DROP TRIGGER fail_arrival");industry.deliver(operation,1);assertEquals(24,quantity(warehouse,Commodity.COAL));assertEquals(30000,allMoney());
    }

    @Test void missingFundsUnknownOutputsOtherSitesAndHaulLimitsBlockImpossibleActions() throws SQLException {
        stores(1024);industry.discover(deposit,mine.siteId(),8,1200,BUNDLE);industry.mine(UUID.randomUUID(),mine.id(),deposit,100,0,8,32);
        assertThrows(SQLException.class,()->industry.purchase(UUID.randomUUID(),mine.id(),warehouse.id(),Commodity.COAL,24,6001,0,1,128));
        assertThrows(SQLException.class,()->industry.purchase(UUID.randomUUID(),mine.id(),warehouse.id(),new Commodity("diamonds"),1,1,0,1,128));
        assertThrows(SQLException.class,()->industry.mine(UUID.randomUUID(),warehouse.id(),deposit,1,0,8,32));
        industry.purchase(UUID.randomUUID(),mine.id(),warehouse.id(),Commodity.COAL,24,24,0,1,24);
        assertThrows(SQLException.class,()->industry.purchase(UUID.randomUUID(),mine.id(),warehouse.id(),Commodity.STONE,1,1,0,1,24));
        assertEquals(72,quantity(mine,Commodity.STONE));assertEquals(30000,allMoney());
    }

    @Test void storeCapitalIsAllocatedOnceAndFailedOpeningLeavesVillageFundsIntact() throws SQLException {
        sql("CREATE TRIGGER fail_stock_opening BEFORE INSERT ON industry_stock BEGIN SELECT RAISE(ABORT,'test store failure'); END");
        assertThrows(SQLException.class,()->industry.openStore(warehouse.id(),100,SPACE,5000));
        assertTrue(industry.store(warehouse.id()).isEmpty());assertEquals(30000,new TreasuryRepository(database.getConnection()).load(town).orElseThrow().hundredths());
        sql("DROP TRIGGER fail_stock_opening");industry.openStore(warehouse.id(),100,SPACE,5000);
        database.close();open();assertEquals(5000,industry.openStore(warehouse.id(),100,SPACE,5000).hundredths());
        assertEquals(25000,new TreasuryRepository(database.getConnection()).load(town).orElseThrow().hundredths());
        assertThrows(SQLException.class,()->industry.openStore(warehouse.id(),1000,SPACE,5000));
    }

    @Test void schema27UpgradeFailurePreservesBusinessesJobsAndFunds() throws SQLException {
        SchemaFixtures.rewind(database.getConnection(),27);sql("CREATE TABLE local_shipments (collision TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(database.getConnection()));
        assertEquals(mine,database.getBusinessRepository().findFounding(mine.id()).orElseThrow().business());
        assertEquals(3,database.getBusinessRepository().currentWorkers(mine.id()).size());
        try(var s=database.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")) {assertTrue(r.next());assertEquals(27,r.getInt(1));}
        assertThrows(SQLException.class,()->industry.deposit(deposit));sql("DROP TABLE local_shipments");new SchemaMigrator().migrate(database.getConnection());
        stores(1024);assertEquals(30000,allMoney());
    }

    @Test void consistentSnapshotRetainsDepletionOwnedCargoAndBusinessCapital() throws Exception {
        stores(1024);industry.discover(deposit,mine.siteId(),8,1200,BUNDLE);
        industry.mine(UUID.randomUUID(),mine.id(),deposit,100,0,8,32);
        UUID purchase=UUID.randomUUID();var cargo=industry.purchase(purchase,mine.id(),warehouse.id(),Commodity.COAL,24,200,0,1,128);
        Path backup=directory.resolve("industry-snapshot.db");DatabaseBackup.snapshot(directory.resolve("civilizations.db"),backup);
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+backup);var s=c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");var restored=new IndustryRepository(c);
            assertEquals(1176,restored.deposit(deposit).orElseThrow().remainingBatches());assertEquals(cargo,restored.shipment(purchase).orElseThrow());
            assertEquals(200,restored.store(mine.id()).orElseThrow().hundredths());
            assertEquals(24,restored.store(warehouse.id()).orElseThrow().reservedIncomingSpace());
            restored.deliver(purchase,1);assertEquals(24L,restored.store(warehouse.id()).orElseThrow().units().get(Commodity.COAL));
            try(var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
        }
        assertNull(industry.shipment(purchase).orElseThrow().deliveredPeriod());
    }

    @Test void twoHundredFortyPeriodsConserveCoalRockAndCapitalThroughWarehousingUseAndRestart() throws SQLException {
        stores(256);industry.discover(deposit,mine.siteId(),8,1200,BUNDLE);
        long coalConsumed=0,rockConsumed=0;
        for(long period=0;period<240;period++) {
            long step=period*3;
            industry.mine(UUID.randomUUID(),mine.id(),deposit,100,step,8,32);
            for(var commodity:List.of(Commodity.COAL,Commodity.STONE)) {
                long available=quantity(mine,commodity);
                if(available>0) {
                    UUID shipment=UUID.randomUUID();industry.purchase(shipment,mine.id(),warehouse.id(),commodity,available,0,step,1,128);industry.deliver(shipment,step+1);
                    UUID resale=UUID.randomUUID();industry.purchase(resale,warehouse.id(),shop.id(),commodity,available,0,step+1,1,128);industry.deliver(resale,step+2);
                    industry.consume(UUID.randomUUID(),shop.id(),commodity,available,IndustryRepository.Use.INDUSTRY_INPUT,"Explicit test downstream recipe inputs");
                    if(commodity.equals(Commodity.COAL))coalConsumed+=available;else rockConsumed+=available;
                }
            }
            assertEquals(30000,allMoney());if(period==120){database.close();open();}
        }
        assertEquals(1200,coalConsumed);assertEquals(3600,rockConsumed);assertEquals(0,industry.deposit(deposit).orElseThrow().remainingBatches());
        assertEquals(0,stock(mine).usedSpace()+stock(warehouse).usedSpace()+stock(shop).usedSpace());
    }
}
