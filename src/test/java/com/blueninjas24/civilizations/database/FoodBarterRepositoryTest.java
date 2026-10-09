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

class FoodBarterRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager db;private EconomyRuntimeRepository runtime;
    private final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),player=UUID.randomUUID();
    private final FoodBarters.Policy policy=FoodBarters.Policy.prototype();
    private final FoodAidPreview.Policy foods=FoodAidPreview.Policy.prototype();
    @BeforeEach void setup()throws SQLException {
        open();db.saveSettlement(new Settlement(town,"Barter fixture","world",0,64,0,1,0,2));
        db.getVillageIdentityRepository().observeWorld(world,"world");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(int i=0;i<2;i++)db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Fixture",town,null,300,Resident.Status.ALIVE));
        runtime.observe(new FarmObservation(town,world,1000,true,0),1000);runtime.pulse(61000,false);
    }
    private void open()throws SQLException {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("BarterTest"));db.initialize();
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);
        for(var band:CommodityAccounting.Condition.values())bands.put(band,100);
        var settings=new EconomySettings(60,0,2,2,1,0,0,new CommodityAccounting.Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,0L,Commodity.TIMBER,8L,Commodity.STONE,4L),
                Map.of(Commodity.FOOD,BigDecimal.valueOf(3),Commodity.TIMBER,BigDecimal.valueOf(4),Commodity.STONE,BigDecimal.valueOf(3)),bands);
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,VillagePriorities.Policy.prototype(),0,OpeningFunds.prototype());
    }
    @AfterEach void close(){db.close();}
    private FoodBarters.Terms quote(UUID p,long units)throws SQLException {
        return runtime.quoteFoodBarter(UUID.randomUUID(),p,town,units,"WHEAT",(int)units,Commodity.TIMBER,policy,foods,61000);
    }
    private String reserve(FoodBarters.Terms terms)throws SQLException{return runtime.reserveFoodBarter(terms,policy,foods,61000);}
    private long timber()throws SQLException{return runtime.views().get(town).account().state().stocks().get(Commodity.TIMBER).units();}
    private long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    private void sql(String text)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(text);}}
    @Test void quoteMovesNothingAndReserveRestartCompletionCreditsFoodExactlyOnce()throws SQLException {
        var terms=quote(player,8);assertEquals(6,terms.paymentCount());assertEquals(16,timber());assertEquals(0,count("food_barters"));
        var history=db.getCommodityRepository().history(town,1).orElseThrow();assertEquals("RESERVED",reserve(terms));assertEquals(10,timber());
        assertEquals("RESERVED",reserve(terms));assertEquals(1,count("barter_stock_moves"));assertEquals(4800,runtime.views().get(town).treasury().hundredths());
        db.close();open();runtime.start(61000);assertTrue(runtime.completeFoodBarter(terms,61000));assertFalse(runtime.completeFoodBarter(terms,61000));
        assertEquals(10,timber());assertEquals(1,runtime.views().get(town).foodAid().pendingDeliveries());
        runtime.pulse(121000,false);var food=runtime.views().get(town).latest().reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow();
        assertEquals(8,food.flow().imports());assertEquals(history,db.getCommodityRepository().history(town,1).orElseThrow());
        assertThrows(SQLException.class,()->runtime.cancelFoodBarter(terms));
    }
    @Test void cancellationAfterPeriodsRestoresOnlyHeldGoodsOnceWithoutFoodOrMoney()throws SQLException {
        var terms=quote(player,8);reserve(terms);runtime.pulse(121000,false);assertTrue(runtime.cancelFoodBarter(terms));assertFalse(runtime.cancelFoodBarter(terms));
        assertEquals(16,timber());assertEquals(2,count("barter_stock_moves"));assertEquals(0,count("food_deliveries"));
        assertThrows(SQLException.class,()->runtime.completeFoodBarter(terms,121000));
    }
    @Test void simultaneousOffersCannotSpendSameNeedOrReservedStock()throws SQLException {
        var first=quote(player,8);var second=quote(UUID.randomUUID(),8);reserve(first);
        assertThrows(SQLException.class,()->reserve(second));assertEquals(10,timber());assertEquals(1,count("food_barters"));
        var small=quote(UUID.randomUUID(),4);reserve(small);assertEquals(7,timber());
        assertThrows(SQLException.class,()->quote(UUID.randomUUID(),1));
    }
    @Test void scarceChangedOrInsufficientSuppliesAndConfigurationAreRefused()throws SQLException {
        var terms=quote(player,8);sql("UPDATE economy_stocks SET units=2 WHERE commodity='timber'");assertThrows(SQLException.class,()->reserve(terms));
        sql("UPDATE economy_stocks SET units=16,shortage_periods=1 WHERE commodity='timber'");assertThrows(SQLException.class,()->quote(player,8));
        sql("UPDATE economy_stocks SET shortage_periods=0 WHERE commodity='timber'");
        var changed=new FoodBarters.Policy(true,3,64,Map.of(Commodity.TIMBER,new FoodBarters.Goods("OAK_LOG",2,2)));
        assertThrows(SQLException.class,()->runtime.reserveFoodBarter(terms,changed,foods,61000));
        assertThrows(SQLException.class,()->runtime.reserveFoodBarter(terms,FoodBarters.Policy.disabled(),foods,61000));
    }
    @Test void failedReserveAndCancelRollBackGoodsReceiptAndStatusTogether()throws SQLException {
        var terms=quote(player,8);sql("CREATE TRIGGER reject_move BEFORE INSERT ON barter_stock_moves BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->reserve(terms));assertEquals(16,timber());assertEquals(0,count("food_barters"));
        sql("DROP TRIGGER reject_move");reserve(terms);sql("CREATE TRIGGER reject_move BEFORE INSERT ON barter_stock_moves BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.cancelFoodBarter(terms));assertEquals(10,timber());
        assertEquals(FoodBarterRepository.Status.RESERVED,new FoodBarterRepository(db.getConnection()).matching(terms).status());
    }
    @Test void failedCompletionCannotCreditFoodAndRecoveryIgnoresNewPolicy()throws SQLException {
        var terms=quote(player,8);reserve(terms);sql("CREATE TRIGGER reject_finish BEFORE UPDATE OF status ON food_barters BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.completeFoodBarter(terms,61000));assertEquals(0,count("food_deliveries"));
        sql("DROP TRIGGER reject_finish");assertEquals("RESERVED",runtime.reserveFoodBarter(terms,FoodBarters.Policy.disabled(),FoodAidPreview.Policy.prototype(),61000));
        assertTrue(runtime.completeFoodBarter(terms,61000));assertEquals(10,timber());
    }
    @Test void playerCannotHaveBothReservedPaymentTypesAndIdsCannotChangeTerms()throws SQLException {
        var terms=quote(player,8);reserve(terms);
        var purchase=new FoodPurchases.Terms(UUID.randomUUID(),player,town,1,"WHEAT",1,1,800);
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(purchase,FoodPurchases.Policy.prototype(),foods,61000));
        var altered=new FoodBarters.Terms(terms.operation(),player,town,8,"WHEAT",8,Commodity.TIMBER,"OAK_LOG",5,1);
        assertThrows(SQLException.class,()->reserve(altered));runtime.cancelFoodBarter(terms);
        assertThrows(SQLException.class,()->runtime.reserveFoodPurchase(new FoodPurchases.Terms(terms.operation(),player,town,8,"WHEAT",8,3,800),FoodPurchases.Policy.prototype(),foods,61000));
        var large=new FoodPurchases.Terms(purchase.operation(),player,town,8,"WHEAT",8,3,800);runtime.reserveFoodPurchase(large,FoodPurchases.Policy.prototype(),foods,61000);
        assertThrows(SQLException.class,()->reserve(quote(player,1)));
    }
    @Test void noCashRequiredButFoodNeedCooldownAndMaterialDemandStillConstrainBarter()throws SQLException {
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,-4800,"Fixture spent funds");assertEquals(6,quote(player,8).paymentCount());
        sql("UPDATE economy_periods SET construction=1 WHERE commodity='timber'");assertThrows(SQLException.class,()->quote(player,8));
        sql("UPDATE economy_periods SET construction=0 WHERE commodity='timber'");
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,4800,"Fixture funds");
        for(int i=0;i<3;i++)runtime.bargain(new FoodPurchases.Terms(UUID.randomUUID(),player,town,8,"WHEAT",8,5,800),FoodPurchases.Policy.prototype(),foods,FoodBargaining.Policy.prototype(),61000);
        assertThrows(SQLException.class,()->quote(player,8));
    }
    @Test void failedSchema23UpgradePreservesPreviousHistoryAndRecoveryCreatesNoBarters()throws SQLException {
        var needs=runtime.views().get(town).needs();SchemaFixtures.rewind(db.getConnection(),23);sql("CREATE TABLE food_barters(bad TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(23,r.getInt(1));}
        assertEquals(needs,runtime.views().get(town).needs());assertEquals(16,timber());
        sql("DROP TABLE food_barters");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("food_barters"));assertEquals(0,count("barter_stock_moves"));
    }
    @Test void accelerated240PeriodFamineNeverReplenishesBarterStocksAndConservesEveryTradeAcrossRestart()throws SQLException {
        var random=new Random(8741);long exported=0,delivered=0,imports=0;
        for(int period=1;period<=240;period++) {
            long now=61000L+(period-1)*60000L;
            if(period%37==0){db.close();open();runtime.start(now);}
            else if(period>1)runtime.pulse(now,false);
            var view=runtime.views().get(town);
            imports+=view.latest().reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().flow().imports();
            if(view.needs().demand()>view.needs().fulfilled()&&timber()>2) {
                var terms=runtime.quoteFoodBarter(UUID.randomUUID(),player,town,4,"WHEAT",4,Commodity.TIMBER,policy,foods,now);
                runtime.reserveFoodBarter(terms,policy,foods,now);
                if(random.nextBoolean()){runtime.cancelFoodBarter(terms);assertFalse(runtime.cancelFoodBarter(terms));}
                else {runtime.completeFoodBarter(terms,now);assertFalse(runtime.completeFoodBarter(terms,now));exported+=terms.paymentUnits();delivered+=terms.foodUnits();}
            }
            assertEquals(16,timber()+exported);assertTrue(timber()>=2);
            assertEquals(4800,runtime.views().get(town).treasury().hundredths());
        }
        runtime.pulse(61000L+240*60000L,false);
        imports+=runtime.views().get(town).latest().reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().flow().imports();
        assertEquals(delivered,imports);assertTrue(exported>0);assertEquals(0,new FoodBarterRepository(db.getConnection()).reservedFood(town));
    }
    @Test void mixedTwoVillage240PeriodEconomyConservesCashMaterialsAndFoodAcrossAllTradeTypesAndRestart()throws SQLException {
        var other=UUID.randomUUID();db.saveSettlement(new Settlement(other,"Second fixture","world",100,64,0,1,0,2));
        db.getVillageIdentityRepository().bindSettlement(other,world);
        for(int i=0;i<2;i++)db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Fixture",other,null,300,Resident.Status.ALIVE));
        runtime.observe(new FarmObservation(other,world,1000,true,0),1000);
        var random=new Random(19017);var villages=List.of(town,other);
        long paid=0,foodDelivered=0,foodImported=0,timberExported=0,stoneExported=0;
        for(int period=1;period<=240;period++) {
            long now=61000L+(period-1)*60000L;runtime.pulse(now,false);
            for(var village:villages) {
                var view=runtime.views().get(village);
                assertEquals(period,view.account().state().completedPeriods());
                foodImported+=view.latest().reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().flow().imports();
                if(view.needs().demand()<=view.needs().fulfilled())continue;
                switch(random.nextInt(3)) {
                    case 0->{if(view.treasury().hundredths()<800)break;
                        var terms=new FoodPurchases.Terms(UUID.randomUUID(),player,village,4,"WHEAT",4,1,800);
                        runtime.reserveFoodPurchase(terms,FoodPurchases.Policy.prototype(),foods,now);
                        if(random.nextInt(4)==0)runtime.cancelFoodPurchase(terms);
                        else {runtime.completeFoodPurchase(terms,now);assertFalse(runtime.completeFoodPurchase(terms,now));paid+=800;foodDelivered+=4;}
                    }
                    case 1->{var commodity=view.account().state().stocks().get(Commodity.TIMBER).units()>2?Commodity.TIMBER:Commodity.STONE;
                        if(view.account().state().stocks().get(commodity).units()<=2)break;
                        var terms=runtime.quoteFoodBarter(UUID.randomUUID(),player,village,4,"WHEAT",4,commodity,policy,foods,now);
                        runtime.reserveFoodBarter(terms,policy,foods,now);
                        if(random.nextInt(4)==0)runtime.cancelFoodBarter(terms);
                        else {runtime.completeFoodBarter(terms,now);assertFalse(runtime.completeFoodBarter(terms,now));foodDelivered+=4;
                            if(commodity.equals(Commodity.TIMBER))timberExported+=terms.paymentUnits();else stoneExported+=terms.paymentUnits();}
                    }
                    default->{var operation=UUID.randomUUID();assertTrue(runtime.recordDeliveredFood(operation,village,4,"Fixture delivered donation",now));
                        assertFalse(runtime.recordDeliveredFood(operation,village,4,"Fixture delivered donation",now));foodDelivered+=4;}
                }
            }
            if(period%31==0){db.close();open();runtime.start(now);}
            long money=0,timber=0,stone=0;
            for(var view:runtime.views().values()){money+=view.treasury().hundredths();timber+=view.account().state().stocks().get(Commodity.TIMBER).units();stone+=view.account().state().stocks().get(Commodity.STONE).units();}
            assertEquals(9600,money+paid);assertEquals(32,timber+timberExported);assertEquals(16,stone+stoneExported);
        }
        runtime.pulse(61000L+240*60000L,false);
        for(var view:runtime.views().values())foodImported+=view.latest().reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().flow().imports();
        assertEquals(foodDelivered,foodImported);assertTrue(paid>0);assertTrue(timberExported>0);assertTrue(stoneExported>0);
        for(var village:villages)assertEquals(0,runtime.views().get(village).foodAid().pendingDeliveries());
    }
}
