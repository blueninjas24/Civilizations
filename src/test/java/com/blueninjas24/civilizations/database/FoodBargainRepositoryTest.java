package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class FoodBargainRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager db;private EconomyRuntimeRepository runtime;
    private final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),player=UUID.randomUUID();
    private final FoodBargaining.Policy policy=FoodBargaining.Policy.prototype();
    private final FoodPurchases.Policy purchases=FoodPurchases.Policy.prototype();
    private final FoodAidPreview.Policy foods=FoodAidPreview.Policy.prototype();
    @BeforeEach void setup() throws SQLException {
        open();db.saveSettlement(new Settlement(town,"Bargain fixture","world",0,64,0,1,0,2));
        db.getVillageIdentityRepository().observeWorld(world,"world");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(int i=0;i<2;i++)db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Fixture",town,null,300,Resident.Status.ALIVE));
        runtime.observe(new FarmObservation(town,world,1000,true,0),1000);runtime.pulse(61000,false);
    }
    private void open() throws SQLException {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("BargainTest"));db.initialize();
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);
        for(var band:CommodityAccounting.Condition.values())bands.put(band,100);
        var settings=new EconomySettings(60,0,2,2,1,0,0,new CommodityAccounting.Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,0L),Map.of(Commodity.FOOD,BigDecimal.valueOf(3)),bands);
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,VillagePriorities.Policy.prototype(),0,OpeningFunds.prototype());
    }
    @AfterEach void close(){db.close();}
    private FoodPurchases.Terms ask(int emeralds){return new FoodPurchases.Terms(UUID.randomUUID(),player,town,8,"WHEAT",8,emeralds,800);}
    private FoodBargaining.Deal discuss(int amount,long now) throws SQLException {return runtime.bargain(ask(amount),purchases,foods,policy,now);}
    private void sql(String text) throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(text);}}
    private long balance() throws SQLException {return runtime.views().get(town).treasury().hundredths();}
    @Test void counterAndAcceptancePersistWithoutSpendingUntilReservedAndCompleteOnce() throws SQLException {
        var counter=discuss(5,61000);assertEquals(FoodBargaining.Status.COUNTER,counter.result().status());assertEquals(3,counter.result().offer());
        assertEquals(4800,balance());assertEquals(0,runtime.views().get(town).foodAid().pendingDeliveries());
        db.close();open();runtime.start(61000);var accepted=runtime.bargainAcceptance(player,town,policy,61000);
        assertEquals(counter.paymentTerms(),accepted);assertEquals("RESERVED",runtime.reserveFoodPurchase(accepted,purchases,foods,policy,61000));
        assertEquals(2400,balance());assertEquals(FoodBargaining.Status.RESERVED,new FoodBargainRepository(db.getConnection()).find(accepted.operation()).orElseThrow().result().status());
        assertTrue(runtime.completeFoodPurchase(accepted,61000));assertFalse(runtime.completeFoodPurchase(accepted,61000));assertEquals(2400,balance());
        assertThrows(SQLException.class,()->runtime.bargainAcceptance(player,town,policy,61000));
    }
    @Test void closedDealSurvivesRestartAndQuickMenuCannotBypassCooldown() throws SQLException {
        discuss(5,61000);discuss(5,61000);var closed=discuss(5,61000);assertEquals(FoodBargaining.Status.CLOSED,closed.result().status());
        db.close();open();runtime.start(61000);assertEquals(closed,discuss(1,61000));
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(ask(1),purchases,foods,policy,61000));assertEquals(4800,balance());
        runtime.pulse(121000,false);assertEquals(FoodBargaining.Status.AGREED,discuss(1,121000).result().status());
    }
    @Test void changedSupersededExpiredAndReusedOperationsCannotSpend() throws SQLException {
        var first=discuss(5,61000);var second=discuss(2,61000);
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(first.paymentTerms(),purchases,foods,policy,61000));
        var t=second.paymentTerms();var altered=new FoodPurchases.Terms(t.operation(),player,town,t.units(),t.item(),t.count(),3,800);
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(altered,purchases,foods,policy,61000));
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(t,purchases,foods,policy,106000));assertEquals(4800,balance());
        assertThrows(SQLException.class,()->runtime.bargain(altered,purchases,foods,policy,61000));
    }
    @Test void operationRetryDoesNotConsumePatienceAndChangedGoodsDoNotErasePreviousFailures() throws SQLException {
        var request=ask(5);var first=runtime.bargain(request,purchases,foods,policy,61000);
        assertEquals(first,runtime.bargain(request,purchases,foods,policy,61000));
        var changed=new FoodPurchases.Terms(UUID.randomUUID(),player,town,3,"BREAD",1,5,800);
        assertEquals(2,runtime.bargain(changed,purchases,foods,policy,61000).result().failedAttempts());
        assertEquals(FoodBargaining.Status.CLOSED,discuss(5,61000).result().status());
    }
    @Test void currentBudgetChangesTheCounterAndAcceptedQuotesRecheckActualFunds() throws SQLException {
        var original=discuss(5,61000);new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,-4000,"Fixture expense");
        var lower=discuss(5,61000);assertEquals(1,lower.result().offer());
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(original.paymentTerms(),purchases,foods,policy,61000));
        assertEquals("RESERVED",runtime.reserveFoodPurchase(lower.paymentTerms(),purchases,foods,policy,61000));assertEquals(0,balance());
    }
    @Test void failedDiscussionOrPurchaseRollsBackHistoryPatienceAndFundsTogether() throws SQLException {
        sql("CREATE TRIGGER reject_bargain BEFORE INSERT ON food_bargains BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->discuss(5,61000));sql("DROP TRIGGER reject_bargain");var deal=discuss(5,61000);assertEquals(1,deal.result().failedAttempts());
        sql("CREATE TRIGGER reject_purchase BEFORE INSERT ON food_purchases BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(deal.paymentTerms(),purchases,foods,policy,61000));assertEquals(4800,balance());
        assertEquals(FoodBargaining.Status.COUNTER,new FoodBargainRepository(db.getConnection()).find(deal.requested().operation()).orElseThrow().result().status());
        sql("DROP TRIGGER reject_purchase");assertEquals("RESERVED",runtime.reserveFoodPurchase(deal.paymentTerms(),purchases,foods,policy,61000));
    }
    @Test void disabledNewDiscussionsDoNotPreventAnAlreadyReservedPaymentRecovery() throws SQLException {
        var deal=discuss(2,61000);var t=deal.paymentTerms();runtime.reserveFoodPurchase(t,purchases,foods,policy,61000);
        assertThrows(SQLException.class,()->runtime.bargain(ask(1),purchases,foods,FoodBargaining.Policy.disabled(),61000));
        assertEquals("RESERVED",runtime.reserveFoodPurchase(t,FoodPurchases.Policy.disabled(),foods,FoodBargaining.Policy.disabled(),61000));
        assertTrue(runtime.completeFoodPurchase(t,61000));assertEquals(3200,balance());
    }
    @Test void schema22MigrationPreservesNeedsAndMoneyAndFailureLeavesNoInventedBargains() throws SQLException {
        var needs=runtime.views().get(town).needs();SchemaFixtures.rewind(db.getConnection(),22);sql("CREATE TABLE food_bargains(bad TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(22,r.getInt(1));}
        assertEquals(needs,runtime.views().get(town).needs());assertEquals(4800,balance());
        sql("DROP TABLE food_bargains");new SchemaMigrator().migrate(db.getConnection());assertEquals(1,discuss(5,61000).result().failedAttempts());
    }
}
