package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.BusinessOperationPolicy;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionSupplyReturnsTest {
    @TempDir Path directory;ConstructionSupplyOrdersTest f;
    @BeforeEach void setup()throws Exception{f=new ConstructionSupplyOrdersTest();f.directory=directory;f.setup();}
    @AfterEach void close(){f.close();}
    ConstructionSupplyReturns returns(){return new ConstructionSupplyReturns(f.c());}
    void arrive()throws Exception{
        f.orders().purchase(f.request(Source.VILLAGE,2),f.proof(f.publicDraft),3001,Set.of(f.f.world));
        f.f.economy.pulse(4000,false);assertEquals(1,ConstructionSupplyOrders.deliveryOnly(f.c()).deliverReady(Set.of(f.f.world),8));
    }
    ConstructionSupplyReturns.Receipt send(UUID operation)throws Exception{return returns().returnUnused(operation,f.publicDraft.id(),4001,Set.of(f.f.world),f.runtime);}

    @Test void realRefundCreatesReservedCargoNotInstantStockAndArrivesOnceAfterEarnedTime()throws Exception{
        long money=f.money(),cash=f.f.cash();arrive();UUID operation=UUID.randomUUID();var receipt=send(operation);
        assertEquals(50,receipt.refunded());assertEquals(4,receipt.duePeriod());assertEquals(cash,f.f.cash());assertEquals(money,f.money());
        var store=f.f.db.getIndustryRepository().store(f.seller).orElseThrow();assertEquals(18,store.units().get(Commodity.STONE));assertEquals(2,store.reservedIncomingSpace());
        assertTrue(f.orders().stock(f.publicDraft.id()).isEmpty());assertEquals(receipt,send(operation));
        assertEquals(0,returns().deliverReady(Set.of(f.f.world),8));f.f.economy.pulse(5000,false);
        assertEquals(0,returns().deliverReady(Set.of(),8));assertEquals(1,returns().deliverReady(Set.of(f.f.world),8));assertEquals(0,returns().deliverReady(Set.of(f.f.world),8));
        store=f.f.db.getIndustryRepository().store(f.seller).orElseThrow();assertEquals(20,store.units().get(Commodity.STONE));assertEquals(0,store.reservedIncomingSpace());assertEquals(money,f.money());
    }
    @Test void retiredSiteDoesNotCancelPaidReturnAndRestartPreservesCargoAndRefund()throws Exception{
        arrive();UUID operation=UUID.randomUUID();var receipt=send(operation);assertFalse(new ConstructionSiteReservations(f.c()).releaseUnused(f.publicDraft.id(),4002).active());
        f.f.db.close();f.f.open();assertEquals(receipt,send(operation));assertEquals(2,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().reservedIncomingSpace());
        f.f.economy=new EconomyRuntimeRepository(f.c(),f.f.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);f.f.economy.pulse(5000,false);
        assertEquals(1,returns().deliverReady(Set.of(f.f.world),1));assertEquals(20,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().units().get(Commodity.STONE));
    }
    @Test void supplierCashSpaceHaulAndWorldHoldsNeverDiscardOnsiteGoods()throws Exception{
        arrive();long cash=f.f.cash();assertThrows(SQLException.class,()->returns().returnUnused(UUID.randomUUID(),f.publicDraft.id(),4001,Set.of(),f.runtime));
        f.runtime=f.policy(1);assertThrows(SQLException.class,()->send(UUID.randomUUID()));f.runtime=f.policy(128);
        f.f.sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+f.seller+"'");assertThrows(SQLException.class,()->send(UUID.randomUUID()));
        f.f.sql("UPDATE industry_stores SET hundredths=50 WHERE business_id='"+f.seller+"'");f.f.sql("UPDATE industry_stock SET units=218 WHERE business_id='"+f.seller+"' AND commodity='timber'");
        assertThrows(SQLException.class,()->send(UUID.randomUUID()));assertFalse(returns().dispatched(f.publicDraft.id()));assertEquals(cash,f.f.cash());assertEquals(Map.of(Commodity.STONE,2L),f.orders().stock(f.publicDraft.id()));
    }
    @Test void pendingCargoAndStartedStageCannotBeReturnedOrRebought()throws Exception{
        f.orders().purchase(f.request(Source.VILLAGE,2),f.proof(f.publicDraft),3001,Set.of(f.f.world));assertThrows(SQLException.class,()->send(UUID.randomUUID()));
        f.f.economy.pulse(4000,false);ConstructionSupplyOrders.deliveryOnly(f.c()).deliverReady(Set.of(f.f.world),8);
        f.f.sql("INSERT INTO construction_stage_work VALUES('"+f.publicDraft.id()+"',0,1,3,3)");assertThrows(SQLException.class,()->send(UUID.randomUUID()));
        f.f.sql("DELETE FROM construction_stage_work");send(UUID.randomUUID());
        assertThrows(SQLException.class,()->f.orders().purchase(f.request(Source.VILLAGE,1),f.proof(f.publicDraft),4002,Set.of(f.f.world)));
    }
    @Test void lateBatchFailureRollsBackRefundsAndAllStorageReservations()throws Exception{
        f.f.sql("UPDATE treasuries SET hundredths=100000");f.f.sql("UPDATE industry_stock SET units=50 WHERE business_id='"+f.seller+"'");
        var paid=new ConstructionVillageProcurement(f.c(),ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),f.runtime,BusinessOperationPolicy.prototype()).purchaseRemaining(f.publicDraft.id(),f.proof(f.publicDraft),3001,Set.of(f.f.world));
        f.f.economy.pulse(4000,false);ConstructionSupplyOrders.deliveryOnly(f.c()).deliverReady(Set.of(f.f.world),8);
        long cash=f.f.cash(),money=f.money();var stock=f.orders().stock(f.publicDraft.id());String last=paid.purchases().stream().map(r->r.request().operation().toString()).sorted().toList().getLast();
        f.f.sql("CREATE TRIGGER reject_last_return AFTER INSERT ON construction_supply_returns WHEN NEW.order_id='"+last+"' BEGIN SELECT RAISE(ABORT,'fixture return failure'); END");
        UUID operation=UUID.randomUUID();assertThrows(SQLException.class,()->send(operation));assertEquals(cash,f.f.cash());assertEquals(money,f.money());assertEquals(stock,f.orders().stock(f.publicDraft.id()));assertEquals(0,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().reservedIncomingSpace());
        f.f.sql("DROP TRIGGER reject_last_return");assertEquals(2940,send(operation).refunded());assertEquals(100000,f.f.cash());assertEquals(60,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().reservedIncomingSpace());
    }
    @Test void returnHaulIsSharedWithNewPurchasesAndDeliveryFailureRollsBackArrival()throws Exception{
        arrive();f.runtime=f.policy(2);send(UUID.randomUUID());assertEquals(2,StorageUpgrades.haulUsed(f.c(),f.f.town,4,1000));
        var q=f.request(Source.BUSINESS,1);q=new ConstructionSupplyOrders.Request(q.operation(),q.draft(),q.seller(),q.source(),q.business(),q.commodity(),q.units(),2);
        var blocked=q;assertThrows(SQLException.class,()->f.orders().purchase(blocked,f.proof(f.privateDraft),4001,Set.of(f.f.world)));
        f.f.economy.pulse(5000,false);f.f.sql("CREATE TRIGGER reject_return_arrival AFTER UPDATE OF delivered_period ON construction_supply_returns BEGIN SELECT RAISE(ABORT,'fixture arrival failure'); END");
        assertThrows(SQLException.class,()->returns().deliverReady(Set.of(f.f.world),8));assertEquals(18,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().units().get(Commodity.STONE));assertEquals(2,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().reservedIncomingSpace());
        f.f.sql("DROP TRIGGER reject_return_arrival");assertEquals(1,returns().deliverReady(Set.of(f.f.world),8));
    }
    @Test void migration114FailureAndRetryPreserveArrivedGoodsAndFunds()throws Exception{
        arrive();long money=f.money();SchemaFixtures.rewind(f.c(),114);f.f.sql("CREATE TABLE construction_supply_returns(blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.c()));assertEquals(money,f.money());
        try(var s=f.c().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(114,r.getInt(1));}
        f.f.sql("DROP TABLE construction_supply_returns");new SchemaMigrator().migrate(f.c());assertEquals(Map.of(Commodity.STONE,2L),f.orders().stock(f.publicDraft.id()));send(UUID.randomUUID());
        try(var s=f.c().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void restartedSessionSettlesAcceptedReturnOnlyAfterPresentWorldEarnsAnotherPeriod()throws Exception{
        arrive();send(UUID.randomUUID());f.f.db.close();var clock=new java.util.concurrent.atomic.AtomicLong(100000);
        var p=com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype();
        var settings=new com.blueninjas24.civilizations.application.PopulationSession.Settings(0,p.millisPerMonth(),p.maximumOfflineMonths(),p.monthsPerBatch(),p.population(),p.migration(),p.recognition(),p.enrollment());
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),java.util.logging.Logger.getLogger("ConstructionReturnTest"),settings,clock::get,f.f.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype())){
            session.start(false,Set.of()).get(10,java.util.concurrent.TimeUnit.SECONDS);session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);
            clock.set(200000);session.updateLoadedWorlds(Set.of(f.f.world)).get(10,java.util.concurrent.TimeUnit.SECONDS);session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);
            try(var read=java.sql.DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("civilizations.db"));var s=read.createStatement();var r=s.executeQuery("SELECT delivered_period FROM construction_supply_returns")){
                assertTrue(r.next());assertNull(r.getObject(1),"Downtime and absent-world time never deliver returns");
            }
            clock.set(201000);session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);assertNull(session.status().failure());assertTrue(session.constructionDiagnostic().contains("1 return arrivals"),session.constructionDiagnostic());
        }
        f.f.open();assertEquals(20,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().units().get(Commodity.STONE));assertEquals(0,returns().deliverReady(Set.of(f.f.world),8));
    }
}
