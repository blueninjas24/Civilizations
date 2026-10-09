package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class FoodPurchaseRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager db;
    private EconomyRuntimeRepository runtime;
    private final UUID town = UUID.randomUUID(), world = UUID.randomUUID();
    private final FoodPurchases.Policy policy = FoodPurchases.Policy.prototype();
    private final FoodAidPreview.Policy foods = FoodAidPreview.Policy.prototype();
    @BeforeEach void setup() throws SQLException {
        reopen(); db.saveSettlement(new Settlement(town,"Purchase fixture","world",0,64,0,1,0,2));
        db.getVillageIdentityRepository().observeWorld(world,"world"); db.getVillageIdentityRepository().bindSettlement(town,world);
        for(int i=0;i<2;i++) db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Fixture",town,null,300,Resident.Status.ALIVE));
        runtime.observe(new FarmObservation(town,world,1000,true,0),1000); runtime.pulse(61000,false);
        assertEquals(VillagePriorities.Action.SEEK_FOOD,runtime.views().get(town).priority().selected());
    }
    private void reopen() throws SQLException {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("PurchaseTest")); db.initialize();
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);
        for(var band:CommodityAccounting.Condition.values())bands.put(band,100);
        var settings=new EconomySettings(60,0,2,2,1,0,0,new CommodityAccounting.Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,0L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,
                VillagePriorities.Policy.prototype(),0,OpeningFunds.prototype());
    }
    @AfterEach void close(){db.close();}
    private FoodPurchases.Terms terms(long units,int emeralds){return new FoodPurchases.Terms(UUID.randomUUID(),UUID.randomUUID(),town,units,"WHEAT",(int)units,emeralds,800);}
    private String reserve(FoodPurchases.Terms terms)throws SQLException{return runtime.reserveFoodPurchase(terms,policy,foods,61000);}
    private long balance()throws SQLException{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    private void sql(String value)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(value);}}
    @Test void reservationHoldsActualFundsButDoesNotCreateGoodsAndRetriesExactlyOnce()throws SQLException{
        var terms=terms(8,2);assertEquals("RESERVED",reserve(terms));assertEquals(3200,balance());
        assertEquals("RESERVED",reserve(terms));assertEquals(3200,balance());assertEquals(0,runtime.views().get(town).foodAid().pendingDeliveries());
        db.close();reopen();runtime.start(61000);assertEquals("RESERVED",reserve(terms));assertEquals(3200,balance());
        assertEquals(8,new FoodPurchaseRepository(db.getConnection()).reservedUnits(town));
    }
    @Test void provenExchangeCommitsFutureDeliveryOnceAndCannotBeRefunded()throws SQLException{
        var terms=terms(8,2);reserve(terms);assertTrue(runtime.completeFoodPurchase(terms,181000));
        assertFalse(runtime.completeFoodPurchase(terms,181000));assertEquals(3200,balance());
        assertThrows(SQLException.class,()->runtime.cancelFoodPurchase(terms));
        var receipt=new FoodDeliveryRepository(db.getConnection()).find(terms.operation()).orElseThrow();assertEquals(3,receipt.eligibleAfterPeriod());
        runtime.pulse(181000,false);assertEquals(0,runtime.views().get(town).latest().reports().getFirst().flow().imports());
        runtime.pulse(181000,false);assertEquals(0,runtime.views().get(town).latest().reports().getFirst().flow().imports());
        runtime.pulse(241000,false);assertEquals(8,runtime.views().get(town).latest().reports().getFirst().flow().imports());
        db.close();reopen();assertFalse(runtime.completeFoodPurchase(terms,241000));assertEquals("COMPLETED",reserve(terms));
    }
    @Test void unexchangedCancellationRefundsOnceAndNeverCreditsFood()throws SQLException{
        var terms=terms(8,2);reserve(terms);assertTrue(runtime.cancelFoodPurchase(terms));assertFalse(runtime.cancelFoodPurchase(terms));
        assertEquals(4800,balance());assertEquals("CANCELLED",reserve(terms));assertEquals(0,new FoodPurchaseRepository(db.getConnection()).reservedUnits(town));
        assertThrows(SQLException.class,()->runtime.completeFoodPurchase(terms,61000));assertEquals(0,runtime.views().get(town).foodAid().pendingDeliveries());
    }
    @Test void competingPlayersCannotOverbuyNeedOrSpendTheSameBudget()throws SQLException{
        reserve(terms(8,2));assertThrows(SQLException.class,()->reserve(terms(8,2)));assertEquals(3200,balance());
        runtime.recordDeliveredFood(UUID.randomUUID(),town,4,"Fixture donor",61000);
        assertThrows(SQLException.class,()->reserve(terms(4,1)));assertEquals(3200,balance());
    }
    @Test void playerCanHaveOnlyOneReservationAndFailureRollsBackSecondDebit()throws SQLException{
        var first=terms(4,1);reserve(first);
        var second=new FoodPurchases.Terms(UUID.randomUUID(),first.player(),town,4,"WHEAT",4,1,800);
        assertThrows(SQLException.class,()->reserve(second));assertEquals(4000,balance());
        runtime.cancelFoodPurchase(first);assertEquals("RESERVED",reserve(second));
    }
    @Test void invalidOrStaleOffersCannotReserveAndRecoveryKeepsAcceptedTerms()throws SQLException{
        assertThrows(SQLException.class,()->reserve(terms(4,2))); // More than its current value.
        var terms=terms(8,2);
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(terms,FoodPurchases.Policy.disabled(),foods,61000));
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(terms,new FoodPurchases.Policy(true,900,3,64),foods,61000));
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(terms,policy,foods,121000)); // Queued period.
        assertEquals(4800,balance());reserve(terms);
        assertEquals("RESERVED",runtime.reserveFoodPurchase(terms,FoodPurchases.Policy.disabled(),foods,61000));
        assertTrue(runtime.completeFoodPurchase(terms,61000));
        var changed=new FoodPurchases.Terms(terms.operation(),terms.player(),town,8,"WHEAT",8,1,800);
        assertThrows(SQLException.class,()->reserve(changed));assertEquals(3200,balance());
    }
    @Test void insufficientFundsAndPartialEvidenceBlockOnlyNewOffers()throws SQLException{
        var treasury=new TreasuryRepository(db.getConnection());treasury.post(UUID.randomUUID(),town,-4400,"Fixture spending");
        assertThrows(SQLException.class,()->reserve(terms(4,1)));assertEquals(400,balance());
        treasury.post(UUID.randomUUID(),town,800,"Fixture revenue");var accepted=terms(4,1);reserve(accepted);
        runtime.observe(new FarmObservation(town,world,61001,false,0),61001);
        assertThrows(SQLException.class,()->reserve(terms(4,1)));assertTrue(runtime.completeFoodPurchase(accepted,61001));
    }
    @Test void injectedReservationAndCompletionFailuresRollbackAndRetry()throws SQLException{
        var terms=terms(8,2);sql("CREATE TRIGGER reject_purchase BEFORE INSERT ON food_purchases BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->reserve(terms));assertEquals(4800,balance());
        sql("DROP TRIGGER reject_purchase");reserve(terms);
        sql("CREATE TRIGGER reject_finish BEFORE UPDATE ON food_purchases BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.completeFoodPurchase(terms,61000));assertEquals(0,runtime.views().get(town).foodAid().pendingDeliveries());
        assertEquals("RESERVED",reserve(terms));sql("DROP TRIGGER reject_finish");assertTrue(runtime.completeFoodPurchase(terms,61000));assertEquals(3200,balance());
    }
    @Test void cancellationReceiptFailureRollsBackStateAndRefundTogether()throws SQLException{
        var terms=terms(8,2);reserve(terms);
        sql("CREATE TRIGGER reject_refund BEFORE INSERT ON treasury_entries WHEN NEW.kind='CREDIT' BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.cancelFoodPurchase(terms));assertEquals(3200,balance());assertEquals("RESERVED",reserve(terms));
        sql("DROP TRIGGER reject_refund");assertTrue(runtime.cancelFoodPurchase(terms));assertEquals(4800,balance());
    }
    @Test void migrationPreservesExistingHistoryAndFailureKeepsVersion20ForRetry()throws SQLException{
        var needs=runtime.views().get(town).needs();SchemaFixtures.rewind(db.getConnection(),20);
        sql("CREATE TABLE food_purchases(bad_column TEXT)");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(20,r.getInt(1));}
        assertEquals(needs,runtime.views().get(town).needs());assertEquals(4800,balance());
        sql("DROP TABLE food_purchases");new SchemaMigrator().migrate(db.getConnection());assertEquals("RESERVED",reserve(terms(4,1)));
    }
    @Test void exactEmeraldAmountsFloorAndClampWithoutFloatingMoney(){
        assertEquals(0,FoodPurchases.maximumPayment(799,4800,policy));assertEquals(2,FoodPurchases.maximumPayment(2399,1600,policy));
        assertEquals(64,FoodPurchases.maximumPayment(Long.MAX_VALUE,Long.MAX_VALUE,policy));
        assertThrows(IllegalArgumentException.class,()->FoodPurchases.maximumPayment(-1,0,policy));
        assertThrows(IllegalArgumentException.class,()->new FoodPurchases.Policy(true,0,3,64));
    }
}
