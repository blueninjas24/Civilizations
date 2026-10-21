package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionSupplyOrdersTest {
    @TempDir Path directory;BusinessStartupGrantsTest f;UUID seller,buyer;IndustryRuntimePolicy runtime;int siteOffset;
    ConstructionDraft privateDraft,publicDraft;
    @BeforeEach void setup()throws Exception {
        if(f==null)f=new BusinessStartupGrantsTest();f.directory=directory;f.setup();
        var other=UUID.randomUUID();f.db.getPopulationRepository().insertResident(new Resident(other,"Fixture builder",f.town,null,300,Resident.Status.ALIVE));
        var firms=f.db.getBusinessRepository();
        for(boolean source:List.of(true,false)) {
            var founding=f.producer("warehouse",source?f.person:other,(source?20:40)+siteOffset,0).founding();firms.found(founding);
            var id=founding.business().id();firms.hire(UUID.randomUUID(),id,source?f.person:other,0);
            f.db.getIndustryRepository().openStore(id,256,Map.of(Commodity.STONE,1L,Commodity.TIMBER,1L,new Commodity("wool"),1L),source?0:4000);
            if(source)seller=id;else buyer=id;
        }
        f.sql("UPDATE industry_stock SET units=20 WHERE business_id='"+seller+"'");
        f.economy.pulse(3000,false);f.sql("INSERT INTO industry_runtime VALUES('"+buyer+"',1000,3000,0,2,2)");
        runtime=policy(128);privateDraft=draft("starter_storehouse",100+siteOffset);publicDraft=draft("starter_home",120+siteOffset);
    }
    @AfterEach void close(){f.db.close();}
    IndustryRuntimePolicy policy(long haul){var p=IndustryPolicy.prototype();var transport=new IndustryPolicy(p.depositBatchesPerObservedOre(),p.batchesPerWorker(),p.siteBatchCapacity(),p.storageCapacity(),1,haul,p.miningBundle(),p.storageSpacePerUnit());return new IndustryRuntimePolicy(true,1000,8,transport,Map.of(),WorkshopSupplyPolicy.disabled(),new WarehouseSupplyPolicy(true,16,16),ToolUpkeepPolicy.disabled());}
    ConstructionDraft draft(String name,int x)throws Exception {
        var plan=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/"+name+".yml"));
        var d=new ConstructionDraft(UUID.randomUUID(),f.town,f.world,new Position(x,64,0),Transform.identity(),plan,3000);
        new ConstructionDraftRepository(c()).save(d);new ConstructionSiteReservations(c()).reserve(d.id(),proof(d),3001);return d;
    }
    Connection c(){return f.db.getConnection();}
    ConstructionMaterials.Authority proof(ConstructionDraft d){return new ConstructionMaterials.Authority(d.id(),f.world,3000,true,true);}
    ConstructionSupplyOrders orders(){return new ConstructionSupplyOrders(c(),ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),runtime,BusinessOperationPolicy.prototype());}
    ConstructionSupplyOrders.Request request(Source source,long units){return new ConstructionSupplyOrders.Request(UUID.randomUUID(),(source==Source.VILLAGE?publicDraft:privateDraft).id(),seller,source,source==Source.BUSINESS?buyer:null,Commodity.STONE,units,2);}
    long money()throws Exception{return f.cash()+f.db.getIndustryRepository().store(seller).orElseThrow().hundredths()+f.db.getIndustryRepository().store(buyer).orElseThrow().hundredths();}
    @ParameterizedTest @EnumSource(Source.class)
    void actualCashAndGoodsTransferOnceThenRequireEarnedOwnerTimeForDeliveryAfterRestart(Source source)throws Exception {
        var q=request(source,2);var d=source==Source.VILLAGE?publicDraft:privateDraft;long cash=money();
        var receipt=orders().purchase(q,proof(d),3001,Set.of(f.world));assertEquals(50,receipt.price());assertNull(receipt.deliveredPeriod());assertEquals(cash,money());
        assertEquals(18,f.db.getIndustryRepository().store(seller).orElseThrow().units().get(Commodity.STONE));assertTrue(orders().stock(d.id()).isEmpty());
        assertNull(orders().deliver(q.operation(),Set.of(f.world)).deliveredPeriod());
        assertThrows(SQLException.class,()->new ConstructionSiteReservations(c()).releaseUnused(d.id(),3002));
        f.db.close();f.open();assertEquals(receipt,new ConstructionSupplyOrders(c(),ConstructionBudgetPolicy.prototype(),new ConstructionSupplyPolicy(false,ConstructionSupplyPolicy.prototype().prices()),runtime,BusinessOperationPolicy.prototype()).purchase(q,proof(d),90000,Set.of()));
        assertEquals(cash,money());assertNull(orders().deliver(q.operation(),Set.of()).deliveredPeriod());
        if(source==Source.VILLAGE) {
            f.economy=new EconomyRuntimeRepository(c(),f.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);f.economy.startOnline(3000,Set.of(f.world));f.economy.pulse(4000,false);
        }else {var industries=new IndustryRuntime(c(),runtime,BusinessOperationPolicy.prototype());industries.start(3000);industries.pulse(4000,Set.of(f.world));}
        assertEquals(3L,orders().deliver(q.operation(),Set.of(f.world)).deliveredPeriod());assertEquals(Map.of(Commodity.STONE,2L),orders().stock(d.id()));
        assertEquals(3L,orders().deliver(q.operation(),Set.of()).deliveredPeriod());assertEquals(Map.of(Commodity.STONE,2L),orders().stock(d.id()));
        assertEquals(0,f.db.getIndustryRepository().store(buyer).orElseThrow().units().get(Commodity.STONE),"Goods arrive at project, never duplicated into business stock");
    }
    @Test void publicShortageStalePopulationAndProtectedCashPreventSpending()throws Exception {
        var q=request(Source.VILLAGE,1);long before=money();f.sql("UPDATE needs_periods SET fulfilled=0 WHERE period=2");
        assertThrows(SQLException.class,()->orders().purchase(q,proof(publicDraft),3001,Set.of(f.world)));
        f.sql("UPDATE needs_periods SET fulfilled=demand WHERE period=2");f.db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"New fixture child",f.town,null,1,Resident.Status.ALIVE));
        assertThrows(SQLException.class,()->orders().purchase(q,proof(publicDraft),3001,Set.of(f.world)));
        assertTrue(orders().find(q.operation()).isEmpty());assertEquals(before,money());
    }
    @Test void publicFundingReadinessIsReadOnlyAndSubtractsAlreadyPaidIncomingMaterials()throws Exception {
        long cash=money(),writes;try(var s=c().createStatement();var r=s.executeQuery("SELECT total_changes()")){writes=r.getLong(1);}
        var quote=orders().quoteVillageMaterials(publicDraft.id(),3001,Set.of(f.world));
        assertEquals(2940L,quote.remainingReferenceValue());assertEquals(2,quote.period());assertTrue(quote.protectedCash()>=3500);assertEquals(0,quote.periodSpent());
        var stone=quote.materials().stream().filter(m->m.commodity().equals(Commodity.STONE)).findFirst().orElseThrow();assertEquals(12,stone.required());assertEquals(12,stone.missing());assertEquals(0,stone.ordered());
        try(var s=c().createStatement();var r=s.executeQuery("SELECT total_changes()")){assertEquals(writes,r.getLong(1));}assertEquals(cash,money());
        orders().purchase(request(Source.VILLAGE,2),proof(publicDraft),3001,Set.of(f.world));
        var after=orders().quoteVillageMaterials(publicDraft.id(),3001,Set.of(f.world));assertEquals(2890L,after.remainingReferenceValue());assertEquals(50,after.periodSpent());
        stone=after.materials().stream().filter(m->m.commodity().equals(Commodity.STONE)).findFirst().orElseThrow();assertEquals(10,stone.missing());assertEquals(2,stone.inTransit());assertEquals(0,stone.ready());assertEquals(cash,money());
    }
    @Test void publicFundingReadinessCannotTreatAbsentUnfedOrProtectedMoneyAsAvailable()throws Exception {
        assertThrows(SQLException.class,()->orders().quoteVillageMaterials(publicDraft.id(),3001,Set.of()));
        f.sql("UPDATE needs_periods SET fulfilled=0 WHERE period=2");assertThrows(SQLException.class,()->orders().quoteVillageMaterials(publicDraft.id(),3001,Set.of(f.world)));
        f.sql("UPDATE needs_periods SET fulfilled=demand WHERE period=2");f.sql("UPDATE treasuries SET hundredths=3500 WHERE settlement_id='"+f.town+"'");long money=money();
        var quote=orders().quoteVillageMaterials(publicDraft.id(),3001,Set.of(f.world));assertFalse(quote.canOrderAllNow());assertEquals(0,quote.availableCash());assertEquals(money,money());
        assertTrue(quote.explanation().contains("unprotected cash"));
    }
    @Test void sharedHaulAndUnpricedMaterialsHoldReadinessWithoutSpendingOrInventingZeroPrices()throws Exception {
        runtime=policy(4);var quote=orders().quoteVillageMaterials(publicDraft.id(),3001,Set.of(f.world));assertEquals(4,quote.haulRemaining());assertFalse(quote.canOrderAllNow());
        orders().purchase(request(Source.VILLAGE,2),proof(publicDraft),3001,Set.of(f.world));
        assertEquals(2,orders().quoteVillageMaterials(publicDraft.id(),3001,Set.of(f.world)).haulRemaining());
        var unpriced=new ConstructionSupplyOrders(c(),ConstructionBudgetPolicy.prototype(),new ConstructionSupplyPolicy(true,Map.of(Commodity.STONE,25L,Commodity.TIMBER,40L)),runtime,BusinessOperationPolicy.prototype());
        long cash=money();var unknown=unpriced.quoteVillageMaterials(publicDraft.id(),3001,Set.of(f.world));assertNull(unknown.remainingReferenceValue());assertFalse(unknown.canOrderAllNow());assertTrue(unknown.explanation().contains("Unpriced"));assertEquals(cash,money());
    }
    @Test void protectedPublicReserveAndWrongSiteCannotBecomeARealPurchase()throws Exception {
        var q=request(Source.VILLAGE,1);f.sql("UPDATE treasuries SET hundredths=3500 WHERE settlement_id='"+f.town+"'");long before=money();
        assertThrows(SQLException.class,()->orders().purchase(q,proof(publicDraft),3001,Set.of(f.world)));assertEquals(before,money());
        assertThrows(SQLException.class,()->orders().purchase(request(Source.BUSINESS,1),proof(publicDraft),3001,Set.of(f.world)));
        assertEquals(20,f.db.getIndustryRepository().store(seller).orElseThrow().units().get(Commodity.STONE));
    }
    @ParameterizedTest @EnumSource(Source.class)
    void lateReceiptFailureRollsBackPayerSupplierStockAndTreasuryEntry(Source source)throws Exception {
        var q=request(source,2);var d=source==Source.VILLAGE?publicDraft:privateDraft;long before=money();
        f.sql("CREATE TRIGGER reject_construction_order AFTER INSERT ON construction_supply_orders BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->orders().purchase(q,proof(d),3001,Set.of(f.world)));assertEquals(before,money());
        assertEquals(20,f.db.getIndustryRepository().store(seller).orElseThrow().units().get(Commodity.STONE));assertTrue(orders().find(q.operation()).isEmpty());assertTrue(new TreasuryRepository(c()).entry(q.operation()).isEmpty());
        f.sql("DROP TRIGGER reject_construction_order");assertEquals(50,orders().purchase(q,proof(d),3001,Set.of(f.world)).price());
    }
    @Test void frozenNeedPreventsOverbuyAndOwnedStockCannotDoubleFundPaidProject()throws Exception {
        var q=request(Source.BUSINESS,8);orders().purchase(q,proof(privateDraft),3001,Set.of(f.world));
        assertThrows(SQLException.class,()->orders().purchase(request(Source.BUSINESS,1),proof(privateDraft),3001,Set.of(f.world)));
        assertThrows(SQLException.class,()->new ConstructionMaterials(c()).reserveBusiness(UUID.randomUUID(),privateDraft.id(),buyer,proof(privateDraft),3001,ConstructionBudgetPolicy.prototype()));
        assertEquals(12,f.db.getIndustryRepository().store(seller).orElseThrow().units().get(Commodity.STONE));
    }
    @Test void constructionAndOrdinaryIndustryShareHaulCapacityInBothDirections()throws Exception {
        runtime=policy(4);c().setAutoCommit(false);
        try{f.db.getIndustryRepository().purchaseRows(UUID.randomUUID(),seller,buyer,Commodity.STONE,2,50,2,1,4,3L,1000L);c().commit();}finally{c().setAutoCommit(true);}
        assertThrows(SQLException.class,()->orders().purchase(request(Source.BUSINESS,3),proof(privateDraft),3001,Set.of(f.world)));
        orders().purchase(request(Source.BUSINESS,2),proof(privateDraft),3001,Set.of(f.world));
        c().setAutoCommit(false);try{assertThrows(SQLException.class,()->f.db.getIndustryRepository().purchaseRows(UUID.randomUUID(),seller,buyer,Commodity.STONE,1,25,2,1,4,3L,1000L));}finally{c().rollback();c().setAutoCommit(true);}
        assertEquals(2,StorageUpgrades.haulUsed(c(),f.town,3,1000));
    }
    @Test void version105UpgradePreservesSitesStocksAndMoneyThroughFailureAndRetry()throws Exception {
        long before=money();SchemaFixtures.rewind(c(),105);f.sql("CREATE TABLE construction_supply_orders(blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(c()));assertEquals(before,money());
        assertEquals(privateDraft,new ConstructionDraftRepository(c()).find(privateDraft.id()).orElseThrow());
        assertTrue(new ConstructionSiteReservations(c()).find(privateDraft.id()).orElseThrow().active());
        try(var st=c().createStatement();var r=st.executeQuery("PRAGMA user_version")){assertEquals(105,r.getInt(1));}
        f.sql("DROP TABLE construction_supply_orders");new SchemaMigrator().migrate(c());
        var q=request(Source.BUSINESS,2);orders().purchase(q,proof(privateDraft),3001,Set.of(f.world));assertEquals(before,money());
        try(var st=c().createStatement();var r=st.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }

    @Test void boundedSettlementSkipsEarlyCargoAndWholeBatchRollsBackOnLateFailure()throws Exception {
        var early=request(Source.BUSINESS,1);var ready=request(Source.VILLAGE,1);
        orders().purchase(early,proof(privateDraft),3001,Set.of(f.world));
        orders().purchase(ready,proof(publicDraft),3001,Set.of(f.world));
        f.economy.pulse(4000,false);
        var delivery=ConstructionSupplyOrders.deliveryOnly(c());
        assertThrows(SQLException.class,()->delivery.purchase(request(Source.BUSINESS,1),proof(privateDraft),3001,Set.of(f.world)));
        assertEquals(0,delivery.deliverReady(Set.of(),1));
        assertEquals(1,delivery.deliverReady(Set.of(f.world),1));
        assertNull(orders().find(early.operation()).orElseThrow().deliveredPeriod());
        assertEquals(Map.of(Commodity.STONE,1L),orders().stock(publicDraft.id()));
        assertEquals(0,delivery.deliverReady(Set.of(f.world),1));
        // Two newly ready receipts in one transaction: failure on the second undoes the first.
        var other=request(Source.BUSINESS,1);orders().purchase(other,proof(privateDraft),3001,Set.of(f.world));
        f.sql("UPDATE industry_runtime SET completed=3,target=3 WHERE business_id='"+buyer+"'");
        var last=List.of(early.operation().toString(),other.operation().toString()).stream().sorted().toList().getLast();
        f.sql("CREATE TRIGGER reject_arrival AFTER UPDATE OF delivered_period ON construction_supply_orders WHEN NEW.operation_id='"+last+"' BEGIN SELECT RAISE(ABORT,'fixture arrival failure'); END");
        assertThrows(SQLException.class,()->delivery.deliverReady(Set.of(f.world),2));
        assertNull(orders().find(early.operation()).orElseThrow().deliveredPeriod());
        assertNull(orders().find(other.operation()).orElseThrow().deliveredPeriod());assertTrue(orders().stock(privateDraft.id()).isEmpty());
        f.sql("DROP TRIGGER reject_arrival");assertEquals(2,delivery.deliverReady(Set.of(f.world),2));
        assertEquals(Map.of(Commodity.STONE,2L),orders().stock(privateDraft.id()));assertEquals(0,delivery.deliverReady(Set.of(f.world),2));
    }
    @Test void restartedOwnerLoopSettlesSavedCargoOnlyAfterPresentWorldEarnsTheRemainingPeriod()throws Exception {
        var q=request(Source.VILLAGE,1);orders().purchase(q,proof(publicDraft),3001,Set.of(f.world));f.db.close();
        var time=new java.util.concurrent.atomic.AtomicLong(100000);
        var p=com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype();
        var settings=new com.blueninjas24.civilizations.application.PopulationSession.Settings(0,p.millisPerMonth(),p.maximumOfflineMonths(),p.monthsPerBatch(),p.population(),p.migration(),p.recognition(),p.enrollment());
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),java.util.logging.Logger.getLogger("ConstructionCargoTest"),
                settings,time::get,f.settings,
                com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype())) {
            session.start(false,Set.of()).get(10,java.util.concurrent.TimeUnit.SECONDS);
            session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);
            time.set(200000);session.updateLoadedWorlds(Set.of(f.world)).get(10,java.util.concurrent.TimeUnit.SECONDS);
            session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);
            try(var read=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("civilizations.db"));var st=read.createStatement();var r=st.executeQuery("SELECT delivered_period FROM construction_supply_orders")) {
                assertTrue(r.next());r.getLong(1);assertTrue(r.wasNull(),"No delivery from server downtime or absent-world time");
            }
            time.set(201000);session.pulse().get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertNull(session.status().failure());assertTrue(session.constructionDiagnostic().contains("1 arrivals"),session.constructionDiagnostic()+"; economy="+session.economyFailure()+"; status="+session.status());
        }
        f.open();assertEquals(3L,orders().find(q.operation()).orElseThrow().deliveredPeriod());
        assertEquals(Map.of(Commodity.STONE,1L),orders().stock(publicDraft.id()));
    }

}
