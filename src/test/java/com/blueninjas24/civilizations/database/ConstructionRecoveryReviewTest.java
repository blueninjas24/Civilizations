package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.population.Household;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionRecoveryReviewTest {
    @TempDir Path directory;ConstructionSupplyOrdersTest f;
    @BeforeEach void setup()throws Exception{
        f=new ConstructionSupplyOrdersTest();f.directory=directory;f.setup();f.f.sql("UPDATE treasuries SET hundredths=100000");
        UUID family=UUID.randomUUID();var d=f.publicDraft;f.f.db.getPopulationRepository().insertHousehold(new Household(family,d.settlement(),"Recovery fixture family"));
        f.f.sql("UPDATE residents SET household_id='"+family+"' WHERE id='"+f.f.person+"'");f.f.sql("INSERT INTO household_capacity VALUES('"+family+"',0)");
        try(var s=f.c().prepareStatement("INSERT INTO housing_project_bindings(draft_id,household_id,settlement_id,world_id,plan_hash,residents,previous_capacity,created_ms,cancel_operation,cancelled_ms) VALUES(?,?,?,?,?,1,0,3000,NULL,NULL)")){
            s.setString(1,d.id().toString());s.setString(2,family.toString());s.setString(3,d.settlement().toString());s.setString(4,d.world().toString());s.setString(5,d.template().fingerprint());s.executeUpdate();
        }
        f.f.economy.pulse(4000,false);purchase(2);
    }
    @AfterEach void close(){f.close();}
    void purchase(long units)throws Exception{var d=f.publicDraft;f.orders().purchase(new ConstructionSupplyOrders.Request(UUID.randomUUID(),d.id(),f.seller,Source.VILLAGE,null,Commodity.STONE,units,3),new ConstructionMaterials.Authority(d.id(),d.world(),4000,true,true),4001,Set.of(d.world()));}
    ConstructionRecoveryReview review(){return new ConstructionRecoveryReview(f.c());}
    ConstructionRecoveryReview.Quote quote(long now)throws Exception{return review().review(f.publicDraft.id(),f.f.world,now,Set.of(f.f.world),f.runtime);}
    ConstructionRecoveryReview.Result execute(UUID operation,ConstructionRecoveryReview.Quote q,long time)throws Exception{return review().execute(operation,q.draft(),q.world(),q.action(),q.terms(),time,time,Set.of(q.world()),f.runtime);}
    @Test void readOnlyPreviewThenExactPendingRefundSurvivesRestartAndCannotDuplicate()throws Exception{
        long changes;try(var s=f.c().createStatement();var r=s.executeQuery("SELECT total_changes()")){changes=r.getLong(1);}
        var q=quote(4002);assertEquals(ConstructionRecoveryReview.Action.RECALL_PENDING,q.action());assertEquals(2,q.units());assertEquals(50,q.refund());
        try(var s=f.c().createStatement();var r=s.executeQuery("SELECT total_changes()")){assertEquals(changes,r.getLong(1));}
        UUID operation=UUID.randomUUID();long money=f.money();var saved=execute(operation,q,4003);assertEquals(50,saved.refunded());assertEquals(money,f.money());assertEquals(20,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().units().get(Commodity.STONE));
        f.f.db.close();f.f.open();assertEquals(saved,execute(operation,q,4003));assertEquals(money,f.money());assertEquals(ConstructionRecoveryReview.Action.NONE,quote(4004).action());
        assertTrue(new ConstructionSiteReservations(f.c()).find(q.draft()).orElseThrow().active());assertTrue(new HousingProjects(f.c()).find(q.draft()).orElseThrow().active());
    }
    @Test void changedPaidTermsOrAdvancedPendingPeriodCannotConfirmOldPreview()throws Exception{
        var q=quote(4002);purchase(1);long money=f.money();assertThrows(BuildingRegistry.Declined.class,()->execute(UUID.randomUUID(),q,4003));assertEquals(money,f.money());
        var updated=quote(4004);f.f.economy.pulse(5000,false);assertThrows(BuildingRegistry.Declined.class,()->execute(UUID.randomUUID(),updated,5001));assertEquals(money,f.money());
    }
    @Test void queuedConfirmationUsesCurrentOwnerTimeWhileExactRetryKeepsTheOriginalRefund()throws Exception{
        var q=quote(4002);UUID operation=UUID.randomUUID();f.f.economy.pulse(4500,false);long money=f.money();
        var saved=review().execute(operation,q.draft(),q.world(),q.action(),q.terms(),4003,4501,Set.of(q.world()),f.runtime);assertEquals(money,f.money());
        f.f.economy.pulse(5500,false);assertEquals(saved,review().execute(operation,q.draft(),q.world(),q.action(),q.terms(),4003,5501,Set.of(q.world()),f.runtime));assertEquals(money,f.money());
    }
    @Test void arrivedBuybackUsesRealRefundReservedCargoAndLaterEarnedArrival()throws Exception{
        f.f.economy.pulse(5000,false);assertEquals(1,ConstructionSupplyOrders.deliveryOnly(f.c()).deliverReady(Set.of(f.f.world),8));
        var q=quote(5001);assertEquals(ConstructionRecoveryReview.Action.RETURN_ARRIVED,q.action());long money=f.money();UUID operation=UUID.randomUUID();var saved=execute(operation,q,5002);assertNotNull(saved.duePeriod());assertEquals(money,f.money());
        assertTrue(f.orders().stock(q.draft()).isEmpty());assertEquals(18,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().units().get(Commodity.STONE));
        f.f.economy.pulse(6000,false);assertEquals(1,new ConstructionSupplyReturns(f.c()).deliverReady(Set.of(f.f.world),8));assertEquals(20,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().units().get(Commodity.STONE));assertEquals(saved,execute(operation,q,5002));
    }
    @Test void noRealSupplierRefundCashOrAnyNativeIntentRequiresReviewWithoutMutation()throws Exception{
        f.f.sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+f.seller+"'");assertEquals(ConstructionRecoveryReview.Action.NONE,quote(4002).action());
        f.f.sql("UPDATE industry_stores SET hundredths=50 WHERE business_id='"+f.seller+"'");var q=quote(4003);assertEquals(ConstructionRecoveryReview.Action.RECALL_PENDING,q.action());
        new ConstructionBlockJournal(f.c()).prepare(UUID.randomUUID(),q.draft(),0,"minecraft:cobblestone");assertEquals(ConstructionRecoveryReview.Action.NONE,quote(4004).action());assertThrows(BuildingRegistry.Declined.class,()->execute(UUID.randomUUID(),q,4005));
    }
    @Test void savedInspectionReportsExactUnresolvedCoordinatesWithoutNativeOrDatabaseChanges()throws Exception{
        var d=f.publicDraft;var intent=new ConstructionBlockJournal(f.c()).prepare(UUID.randomUUID(),d.id(),0,"minecraft:cobblestone");long changes;try(var s=f.c().createStatement();var r=s.executeQuery("SELECT total_changes()")){changes=r.getLong(1);}
        // Use absent-world history mode: even an unbacked development intent grants no new permission.
        var lines=new ConstructionProjectInspection(f.c()).inspect(d.id(),d.world(),false);assertTrue(lines.stream().anyMatch(s->s.contains(intent.operation().toString())));assertTrue(lines.contains("Saved after: minecraft:cobblestone"));
        assertThrows(BuildingRegistry.Declined.class,()->new ConstructionProjectInspection(f.c()).inspect(d.id(),UUID.randomUUID(),true));
        try(var s=f.c().createStatement();var r=s.executeQuery("SELECT total_changes()")){assertEquals(changes,r.getLong(1));}
    }
}
