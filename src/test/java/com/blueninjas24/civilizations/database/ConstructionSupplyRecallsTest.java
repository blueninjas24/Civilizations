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

class ConstructionSupplyRecallsTest {
    @TempDir Path directory;ConstructionSupplyOrdersTest f;
    @BeforeEach void setup()throws Exception{f=new ConstructionSupplyOrdersTest();f.directory=directory;f.setup();}
    @AfterEach void close(){f.close();}
    ConstructionSupplyRecalls recalls(){return new ConstructionSupplyRecalls(f.c());}
    ConstructionSupplyOrders.Receipt order()throws Exception{return f.orders().purchase(f.request(Source.VILLAGE,2),f.proof(f.publicDraft),3001,Set.of(f.f.world));}
    ConstructionSupplyRecalls.Receipt recall(UUID operation)throws Exception{return recalls().recallPending(operation,f.publicDraft.id(),3002,Set.of(f.f.world));}

    @Test void exactPendingSaleUnwindsOnceSurvivesRestartAndCannotDeliverOrRepurchase()throws Exception{
        long money=f.money(),cash=f.f.cash();var order=order();UUID operation=UUID.randomUUID();
        var receipt=recall(operation);assertEquals(50,receipt.refunded());assertEquals(cash,f.f.cash());assertEquals(money,f.money());
        assertEquals(20,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().units().get(Commodity.STONE));assertEquals(receipt,recall(operation));
        assertThrows(SQLException.class,()->recall(UUID.randomUUID()));
        assertThrows(SQLException.class,()->f.orders().purchase(f.request(Source.VILLAGE,1),f.proof(f.publicDraft),3002,Set.of(f.f.world)));
        f.f.db.close();f.f.open();assertEquals(receipt,recall(operation));assertEquals(cash,f.f.cash());
        f.f.economy=new EconomyRuntimeRepository(f.c(),f.f.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);
        f.f.economy.pulse(4000,false);assertEquals(0,ConstructionSupplyOrders.deliveryOnly(f.c()).deliverReady(Set.of(f.f.world),8));
        assertNull(f.orders().deliver(order.request().operation(),Set.of(f.f.world)).deliveredPeriod());assertTrue(f.orders().stock(f.publicDraft.id()).isEmpty());
        assertFalse(new ConstructionSiteReservations(f.c()).releaseUnused(f.publicDraft.id(),4001).active());
    }
    @Test void wholeBatchLateFailureRollsBackEverySupplierAndRefund()throws Exception{
        f.f.sql("UPDATE treasuries SET hundredths=100000");f.f.sql("UPDATE industry_stock SET units=50 WHERE business_id='"+f.seller+"'");
        var buys=new ConstructionVillageProcurement(f.c(),ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),f.runtime,BusinessOperationPolicy.prototype())
                .purchaseRemaining(f.publicDraft.id(),f.proof(f.publicDraft),3001,Set.of(f.f.world));
        String last=buys.purchases().stream().map(r->r.request().operation().toString()).sorted().toList().getLast();long cash=f.f.cash(),money=f.money();
        var stock=f.f.db.getIndustryRepository().store(f.seller).orElseThrow();
        f.f.sql("CREATE TRIGGER reject_last_recall AFTER INSERT ON construction_supply_recalls WHEN NEW.order_id='"+last+"' BEGIN SELECT RAISE(ABORT,'fixture recall failure'); END");
        UUID operation=UUID.randomUUID();assertThrows(SQLException.class,()->recall(operation));assertFalse(recalls().recalled(f.publicDraft.id()));
        assertEquals(cash,f.f.cash());assertEquals(money,f.money());assertEquals(stock,f.f.db.getIndustryRepository().store(f.seller).orElseThrow());
        f.f.sql("DROP TRIGGER reject_last_recall");assertEquals(2940,recall(operation).refunded());assertEquals(100000,f.f.cash());assertEquals(money,f.money());
    }
    @Test void absentWorldAdvancedOwnerTimeAndAlreadyDeliveredCargoCannotBeRecalled()throws Exception{
        var saved=order();assertThrows(SQLException.class,()->recalls().recallPending(UUID.randomUUID(),f.publicDraft.id(),3002,Set.of()));
        f.f.economy.pulse(4000,false);long cash=f.f.cash();assertThrows(SQLException.class,()->recall(UUID.randomUUID()));
        assertEquals(cash,f.f.cash());f.orders().deliver(saved.request().operation(),Set.of(f.f.world));
        assertThrows(SQLException.class,()->recalls().recallPending(UUID.randomUUID(),f.publicDraft.id(),4001,Set.of(f.f.world)));
        assertEquals(Map.of(Commodity.STONE,2L),f.orders().stock(f.publicDraft.id()));
    }
    @Test void sellerWithoutRefundFundsOrStorageRetainsWholePendingSale()throws Exception{
        order();long cash=f.f.cash();f.f.sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+f.seller+"'");
        assertThrows(SQLException.class,()->recall(UUID.randomUUID()));assertEquals(cash,f.f.cash());
        f.f.sql("UPDATE industry_stores SET hundredths=50 WHERE business_id='"+f.seller+"'");
        f.f.sql("UPDATE industry_stock SET units=218 WHERE business_id='"+f.seller+"' AND commodity='timber'");
        assertThrows(SQLException.class,()->recall(UUID.randomUUID()));assertFalse(recalls().recalled(f.publicDraft.id()));assertEquals(cash,f.f.cash());
    }
    @Test void businessFundingCannotUsePublicRecallAndOriginalSpendingAndHaulRemainUsed()throws Exception{
        var privateOrder=f.request(Source.BUSINESS,1);f.orders().purchase(privateOrder,f.proof(f.privateDraft),3001,Set.of(f.f.world));
        assertThrows(SQLException.class,()->recalls().recallPending(UUID.randomUUID(),f.privateDraft.id(),3002,Set.of(f.f.world)));
        order();recall(UUID.randomUUID());assertEquals(3,StorageUpgrades.haulUsed(f.c(),f.f.town,3,1000));
        // Recalled payments stay in immutable purchase history: they cannot reopen the same spending allowance.
        try(var s=f.c().createStatement();var r=s.executeQuery("SELECT sum(price) FROM construction_supply_orders WHERE source='VILLAGE'")){assertEquals(50,r.getLong(1));}
    }
    @Test void migration113FailureAndRetryPreserveMoneyAndOriginalPendingOrders()throws Exception{
        var saved=order();long money=f.money();SchemaFixtures.rewind(f.c(),113);
        f.f.sql("CREATE TABLE construction_supply_recalls(blocker INTEGER)");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.c()));
        try(var s=f.c().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(113,r.getInt(1));}
        assertEquals(saved,f.orders().find(saved.request().operation()).orElseThrow());assertEquals(money,f.money());
        f.f.sql("DROP TABLE construction_supply_recalls");new SchemaMigrator().migrate(f.c());assertEquals(50,recall(UUID.randomUUID()).refunded());
        try(var s=f.c().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void recalledPaidHomeCanReleaseFamilyAndSiteWithoutLosingSaleOrRefundHistory()throws Exception{
        order();UUID family=UUID.randomUUID();
        f.f.db.getPopulationRepository().insertHousehold(new com.blueninjas24.civilizations.population.Household(family,f.f.town,"Retained project family"));
        f.f.sql("UPDATE residents SET household_id='"+family+"' WHERE id='"+f.f.person+"'");f.f.sql("INSERT INTO household_capacity VALUES('"+family+"',0)");
        // Historical valid paid binding; binding creation itself is covered by DwellingOccupancyTest.
        try(var s=f.c().prepareStatement("INSERT INTO housing_project_bindings(draft_id,household_id,settlement_id,world_id,plan_hash,residents,previous_capacity,created_ms,cancel_operation,cancelled_ms) VALUES(?,?,?,?,?,1,0,3000,NULL,NULL)")){
            s.setString(1,f.publicDraft.id().toString());s.setString(2,family.toString());s.setString(3,f.f.town.toString());s.setString(4,f.f.world.toString());s.setString(5,f.publicDraft.template().fingerprint());s.executeUpdate();
        }
        var context=new DwellingOccupancy.Context(f.f.world,true,DwellingInspection.Policy.prototype(),3002);
        assertFalse(new HousingProjectStatus(f.c()).find(f.publicDraft.id(),context).unused());recall(UUID.randomUUID());
        var status=new HousingProjectStatus(f.c()).find(f.publicDraft.id(),context);assertTrue(status.unused());assertTrue(status.pause().orElseThrow().contains("recalled"));assertEquals(1,status.paidOrders());
        assertFalse(new HousingProjects(f.c()).retireUnused(UUID.randomUUID(),f.publicDraft.id(),3002,context).active());
        assertFalse(new ConstructionSiteReservations(f.c()).find(f.publicDraft.id()).orElseThrow().active());
        assertTrue(recalls().recalled(f.publicDraft.id()));assertEquals(2,f.f.db.getPopulationRepository().findResidents(f.f.town).size());
    }
}
