package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VillageSupplyPurchasesTest {
    @TempDir Path directory;
    BusinessStartupGrantsTest fixture=new BusinessStartupGrantsTest();UUID seller,learner,course;
    @BeforeEach void setup()throws Exception {
        fixture.directory=directory;fixture.setup();
        var original=fixture.producer("fishing",fixture.person,20,1000);
        var request=new BusinessStartupGrants.WorkshopRequest(original.founding(),100,
                Map.of(Commodity.FOOD,1L,Commodity.STONE,1L,Commodity.TIMBER,1L),1000,fixture.person);
        fixture.grants().startWorkshop(request,StartupGrantPolicy.prototype());seller=request.founding().business().id();
        // Explicit fixture goods, never a production seeding or server-database path.
        fixture.sql("UPDATE industry_stock SET units=10 WHERE business_id='"+seller+"'");
        fixture.sql("INSERT INTO economy_stocks VALUES('"+fixture.town+"','stone',0,0),('"+fixture.town+"','timber',0,0)");
        learner=UUID.randomUUID();fixture.db.getPopulationRepository().insertResident(new Resident(learner,"Learner",fixture.town,null,300,Resident.Status.ALIVE));
        course=UUID.randomUUID();
        var old=fixture.settings;
        fixture.settings=new com.blueninjas24.civilizations.economy.EconomySettings(old.secondsPerPeriod(),old.maximumOfflinePeriods(),old.periodsPerBatch(),
                old.adultFoodUnits(),old.childFoodUnits(),old.foodUnitsPerWorker(),old.foodOutputPercent(),old.shortages(),
                Map.of(Commodity.FOOD,100L,Commodity.STONE,0L,Commodity.TIMBER,0L),
                Map.of(Commodity.FOOD,java.math.BigDecimal.ONE,Commodity.STONE,java.math.BigDecimal.ONE,Commodity.TIMBER,java.math.BigDecimal.ONE),old.pricePercent());
        fixture.economy=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);
        fixture.economy.startOnline(2000,worlds());
    }
    @AfterEach void close()throws Exception{fixture.db.close();}
    // These original regression cases exercise the retained shortage-only configuration.
    static VillageSupplyPolicy shortageOnly(){var p=VillageSupplyPolicy.prototype();return new VillageSupplyPolicy(p.enabled(),p.foodPeriodBudget(),p.materialPeriodBudget(),p.minimumTreasury(),p.foodReservePeriods(),p.deliveryPeriods(),p.haulUnitsPerPeriod(),p.prices());}
    VillageSupplyPurchases buyer(VillageSupplyPolicy policy){return new VillageSupplyPurchases(fixture.db.getConnection(),policy,StudyPolicy.prototype(),BusinessOperationPolicy.prototype());}
    VillageSupplyPurchases.Request order(Commodity good,long units){return new VillageSupplyPurchases.Request(UUID.randomUUID(),fixture.town,seller,good,units,3);}
    Set<UUID> worlds(){return Set.of(fixture.world);}
    long scalar(String sql)throws Exception{try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    long stock(Commodity good)throws Exception{return fixture.db.getCommodityRepository().load(fixture.town).orElseThrow().state().stocks().get(good).units();}
    void shortage()throws Exception {
        fixture.economy.pulse(3000,false);fixture.sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");fixture.economy.pulse(4000,false);
        assertEquals(4,scalar("SELECT demand-fulfilled FROM needs_periods WHERE period=3"));
    }
    @Test void actualShortageTransfersCashGoodsAndImportsOnlyAtArrivalAfterRestart()throws Exception {
        shortage();var request=order(Commodity.FOOD,4);var paid=buyer(shortageOnly()).purchase(request,worlds());
        assertEquals(800,paid.price());assertEquals(4,paid.duePeriod());assertNull(paid.appliedPeriod());assertEquals(7700,fixture.cash());assertEquals(0,stock(Commodity.FOOD));
        assertEquals(1800,fixture.db.getIndustryRepository().store(seller).orElseThrow().hundredths());
        assertEquals(6,fixture.db.getIndustryRepository().store(seller).orElseThrow().units().get(Commodity.FOOD));
        fixture.db.close();fixture.open();
        assertEquals(paid,buyer(VillageSupplyPolicy.disabled()).purchase(request,Set.of()));assertEquals(7700,fixture.cash());
        fixture.economy=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);
        fixture.economy.startOnline(4000,worlds());fixture.economy.pulse(5000,false);
        assertEquals(4,scalar("SELECT imports FROM economy_periods WHERE period=4 AND commodity='food'"));
        assertEquals(0,scalar("SELECT demand-fulfilled FROM needs_periods WHERE period=4"));assertEquals(4,buyer(VillageSupplyPolicy.disabled()).find(request.operation()).orElseThrow().appliedPeriod());
        fixture.economy.pulse(6000,false);assertEquals(0,scalar("SELECT imports FROM economy_periods WHERE period=5 AND commodity='food'"));
    }
    @Test void pendingPaidCargoStopsDuplicateShortagePurchasesAndChangedIdentityFails()throws Exception {
        shortage();var request=order(Commodity.FOOD,4);var purchases=buyer(shortageOnly());purchases.purchase(request,worlds());
        var before=fixture.snapshot();assertThrows(VillageSupplyPurchases.Declined.class,()->purchases.purchase(order(Commodity.FOOD,1),worlds()));
        assertThrows(SQLException.class,()->purchases.purchase(new VillageSupplyPurchases.Request(request.operation(),fixture.town,seller,Commodity.FOOD,3,3),worlds()));assertEquals(before,fixture.snapshot());
    }
    @Test void lateReceiptFailureRollsBackMoneyStockAndCargoThenRetriesOnce()throws Exception {
        shortage();var before=fixture.snapshot();fixture.sql("CREATE TRIGGER reject_public_order BEFORE INSERT ON village_supply_orders BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        var request=order(Commodity.FOOD,4);assertThrows(SQLException.class,()->buyer(shortageOnly()).purchase(request,worlds()));assertEquals(before,fixture.snapshot());
        assertTrue(fixture.db.getConnection().getAutoCommit());fixture.sql("DROP TRIGGER reject_public_order");buyer(shortageOnly()).purchase(request,worlds());assertEquals(7700,fixture.cash());
    }
    @Test void unrelatedTreasuryReceiptCannotStandInForAPurchasePayment()throws Exception {
        shortage();var request=order(Commodity.FOOD,4);
        new TreasuryRepository(fixture.db.getConnection()).post(request.operation(),fixture.town,-800,"Real village food supply purchase");
        var before=fixture.snapshot();assertThrows(SQLException.class,()->buyer(shortageOnly()).purchase(request,worlds()));assertEquals(before,fixture.snapshot());
    }
    @Test void absentWorldUnqualifiedStaffAndHistoricalBacklogCannotSpend()throws Exception {
        shortage();var request=order(Commodity.FOOD,4);var before=fixture.snapshot();
        assertThrows(VillageSupplyPurchases.Declined.class,()->buyer(shortageOnly()).purchase(request,Set.of()));assertEquals(before,fixture.snapshot());
        fixture.sql("UPDATE economy_runtime SET target_periods=4");assertThrows(VillageSupplyPurchases.Declined.class,()->buyer(shortageOnly()).purchase(request,worlds()));fixture.sql("UPDATE economy_runtime SET target_periods=3");
        fixture.sql("UPDATE business_employment SET ended_step=1,end_reason='Fixture ended employment'");assertThrows(VillageSupplyPurchases.Declined.class,()->buyer(shortageOnly()).purchase(request,worlds()));assertEquals(8500,fixture.cash());
    }
    void study(boolean automatic)throws Exception {
        fixture.economy=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);
        fixture.economy.configureStudy(StudyPolicy.prototype());if(automatic)fixture.economy.configureVillageSupplies(shortageOnly());
        fixture.economy.startOnline(2000,worlds());fixture.economy.enrollStudy(course,learner,fixture.town,"mining",2000);
        fixture.economy.pulse(3000,false);if(!automatic)fixture.economy.pulse(4000,false);
    }
    @Test void actualPracticeShortageBuysOnlyRequiredMaterialsAndTheirReserve()throws Exception {
        study(false);var purchases=buyer(shortageOnly());
        purchases.purchase(order(Commodity.STONE,3),worlds());purchases.purchase(order(Commodity.TIMBER,3),worlds());
        assertEquals(7750,fixture.cash());assertEquals(0,stock(Commodity.STONE));assertEquals(0,stock(Commodity.TIMBER));
        fixture.economy.pulse(5000,false);assertEquals(2,stock(Commodity.STONE));assertEquals(2,stock(Commodity.TIMBER));
        assertEquals(1,fixture.db.getPopulationRepository().findWorkProfile(learner).orElseThrow().experience().get("mining"));
        assertEquals(2,scalar("SELECT COUNT(*) FROM village_supply_orders WHERE applied_period=4"));
    }
    @Test void ordinaryFoodRuntimeAutomaticallyBuysOnceThenUsesDeliveredTrainingGoods()throws Exception {
        study(true);assertEquals(2,scalar("SELECT COUNT(*) FROM village_supply_orders"));assertEquals(7750,fixture.cash());
        fixture.economy.pulse(3000,false);assertEquals(2,scalar("SELECT COUNT(*) FROM village_supply_orders"));
        fixture.economy.pulse(4000,false);assertEquals(1,fixture.db.getPopulationRepository().findWorkProfile(learner).orElseThrow().experience().get("mining"));
        assertEquals(2,scalar("SELECT COUNT(*) FROM village_supply_orders WHERE applied_period=3"));
    }
    @Test void noCourseOrFoodShortageDoesNotCreateAnImaginaryCustomer()throws Exception {
        fixture.economy.pulse(3000,false);fixture.economy.pulse(4000,false);var before=fixture.snapshot();
        for(var good:List.of(Commodity.FOOD,Commodity.STONE,Commodity.TIMBER))assertThrows(VillageSupplyPurchases.Declined.class,()->buyer(shortageOnly()).purchase(order(good,1),worlds()));
        assertEquals(before,fixture.snapshot());
    }
    @Test void readOnlyPublicCustomerEvidenceCountsSharedCashBudgetsAndIncomingExactlyOnce()throws Exception {
        study(false);var purchases=buyer(shortageOnly());var before=fixture.snapshot();
        var demand=purchases.demand(fixture.town,worlds());assertEquals(Map.of(Commodity.STONE,3L,Commodity.TIMBER,3L),demand.fundedUnits());assertEquals(750,demand.estimatedPrice());assertEquals(before,fixture.snapshot());
        purchases.purchase(order(Commodity.STONE,3),worlds());assertEquals(Map.of(Commodity.TIMBER,3L),purchases.demand(fixture.town,worlds()).fundedUnits());
        var limited=new VillageSupplyPolicy(true,2000,400,2500,2,1,32,shortageOnly().prices());
        assertEquals(Map.of(),buyer(limited).demand(fixture.town,worlds()).fundedUnits());assertEquals(Map.of(),purchases.demand(fixture.town,Set.of()).fundedUnits());
        fixture.db.close();fixture.open();assertEquals(Map.of(Commodity.TIMBER,3L),buyer(shortageOnly()).demand(fixture.town,worlds()).fundedUnits());
    }
    @Test void materialBudgetAndFoodProtectionCannotBeBypassedWithAnotherCommodity()throws Exception {
        study(false);var limited=new VillageSupplyPolicy(true,2000,300,2500,2,1,32,shortageOnly().prices());
        buyer(limited).purchase(order(Commodity.STONE,3),worlds());assertThrows(VillageSupplyPurchases.Declined.class,()->buyer(limited).purchase(order(Commodity.TIMBER,1),worlds()));
        fixture.sql("UPDATE treasuries SET hundredths=2500");assertThrows(VillageSupplyPurchases.Declined.class,()->buyer(shortageOnly()).purchase(order(Commodity.TIMBER,1),worlds()));
    }
    @Test void cancelledOrNowEmployedLearnerDoesNotJustifyPurchases()throws Exception {
        study(false);fixture.sql("UPDATE resident_courses SET status='CANCELLED'");var before=fixture.snapshot();
        assertThrows(VillageSupplyPurchases.Declined.class,()->buyer(shortageOnly()).purchase(order(Commodity.STONE,1),worlds()));assertEquals(before,fixture.snapshot());
    }
    @Test void failedDeliveryPeriodRollsBackImportsConsumptionAndCargoThenRetries()throws Exception {
        study(false);buyer(shortageOnly()).purchase(order(Commodity.STONE,3),worlds());buyer(shortageOnly()).purchase(order(Commodity.TIMBER,3),worlds());
        var before=fixture.snapshot();fixture.sql("CREATE TRIGGER reject_delivery BEFORE UPDATE OF applied_period ON village_supply_orders BEGIN SELECT RAISE(ABORT,'fixture failed arrival'); END");
        assertThrows(SQLException.class,()->fixture.economy.pulse(5000,false));
        // The existing runtime deliberately retains earned time in a separate transaction.
        // Every period effect, asset and cargo row must still roll back.
        var after=fixture.snapshot();before.remove("economy_runtime");after.remove("economy_runtime");assertEquals(before,after);
        assertEquals(4,scalar("SELECT target_periods FROM economy_runtime"));assertEquals(5000,scalar("SELECT last_observed_millis FROM economy_runtime"));
        assertEquals(0,stock(Commodity.STONE));
        fixture.sql("DROP TRIGGER reject_delivery");fixture.economy.pulse(5000,false);assertEquals(2,stock(Commodity.STONE));assertEquals(2,scalar("SELECT COUNT(*) FROM village_supply_orders WHERE applied_period=4"));
    }
    @Test void absentWorldPausesOwnedCargoAndRestoredWorldResumesWithoutCatchup()throws Exception {
        shortage();buyer(shortageOnly()).purchase(order(Commodity.FOOD,4),worlds());
        fixture.economy.updateLoadedWorlds(4000,Set.of());fixture.economy.pulse(100000,false);assertEquals(0,scalar("SELECT COUNT(*) FROM village_supply_orders WHERE applied_period IS NOT NULL"));
        fixture.economy.updateLoadedWorlds(100000,worlds());fixture.economy.pulse(101000,false);assertEquals(4,scalar("SELECT applied_period FROM village_supply_orders"));
    }
    @Test void migration65RetainsOldAssetsAndFailureLeavesVersion64Retryable()throws Exception {
        SchemaFixtures.rewind(fixture.db.getConnection(),64);fixture.sql("PRAGMA user_version=64");var before=fixture.snapshot();
        fixture.sql("CREATE VIEW village_supply_orders AS SELECT 1");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(fixture.db.getConnection()));
        assertEquals(64,scalar("PRAGMA user_version"));assertEquals(before,fixture.snapshot());fixture.sql("DROP VIEW village_supply_orders");
        new SchemaMigrator().migrate(fixture.db.getConnection());var after=fixture.snapshot();for(var added:List.of("bakery_private_starters","livestock_herds","livestock_initial_animals","livestock_periods","livestock_young"))assertEquals(List.of(),after.remove(added));assertEquals(List.of(),after.remove("protected_pen_animals"));assertEquals(List.of(),after.remove("protected_pen_columns"));assertEquals(List.of(),after.remove("protected_pens"));assertEquals(List.of(),after.remove("animal_ownership"));assertEquals(List.of(),after.remove("farm_market_harvests"));assertEquals(List.of(),after.remove("farm_market_stores"));assertEquals(List.of(),after.remove("grain_harvests"));assertEquals(List.of(),after.remove("grain_stocks"));assertEquals(List.of(),after.remove("farm_grain_evidence"));assertEquals(List.of(),after.remove("fishery_founder_courses"));assertEquals(List.of(),after.remove("fishery_workplace_history"));assertEquals(List.of(),after.remove("fishery_workplaces"));for(var table:List.of("village_supply_orders","fishery_resources","fishery_columns","fishery_work","fishery_habitat_changes","water_surveys","water_columns"))assertEquals(List.of(),after.remove(table));assertEquals(before,after);
        assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    void healthyLowReserve()throws Exception {
        fixture.economy.pulse(3000,false);fixture.economy.pulse(4000,false);
        fixture.sql("UPDATE economy_stocks SET units=3 WHERE commodity='food'");
        assertEquals(0,scalar("SELECT demand-fulfilled FROM needs_periods WHERE period=3"));
    }
    @Test void healthyVillageHasFundedFoodCustomerAndProtectedStartupAtTheSameTime()throws Exception {
        healthyLowReserve();var purchases=buyer(VillageSupplyPolicy.prototype());var before=fixture.snapshot();
        assertEquals(Map.of(Commodity.FOOD,5L),purchases.demand(fixture.town,worlds()).fundedUnits());
        assertTrue(fixture.grants().preview(fixture.town,"fishing",500,1000,StartupGrantPolicy.prototype()).eligible());
        assertEquals(before,fixture.snapshot());
        var paid=purchases.purchase(order(Commodity.FOOD,5),worlds());assertEquals(1000,paid.price());assertEquals(7500,fixture.cash());assertEquals(3,stock(Commodity.FOOD));
        assertEquals(2000,fixture.db.getIndustryRepository().store(seller).orElseThrow().hundredths());
        assertEquals(5,fixture.db.getIndustryRepository().store(seller).orElseThrow().units().get(Commodity.FOOD));
        assertEquals(Map.of(),purchases.demand(fixture.town,worlds()).fundedUnits());
        fixture.db.close();fixture.open();assertEquals(paid,buyer(VillageSupplyPolicy.disabled()).purchase(paid.request(),Set.of()));
        fixture.economy=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);
        fixture.economy.startOnline(4000,worlds());fixture.economy.pulse(5000,false);
        assertEquals(5,scalar("SELECT imports FROM economy_periods WHERE period=4 AND commodity='food'"));assertEquals(4,stock(Commodity.FOOD));
    }
    @Test void pendingAidAndPaidCargoReduceReserveExactlyOnce()throws Exception {
        healthyLowReserve();fixture.economy.recordDeliveredFood(UUID.randomUUID(),fixture.town,2,"Explicit donated fixture",4000);
        var purchases=buyer(VillageSupplyPolicy.prototype());assertEquals(Map.of(Commodity.FOOD,3L),purchases.demand(fixture.town,worlds()).fundedUnits());
        purchases.purchase(order(Commodity.FOOD,3),worlds());assertEquals(Map.of(),purchases.demand(fixture.town,worlds()).fundedUnits());
        var before=fixture.snapshot();assertThrows(VillageSupplyPurchases.Declined.class,()->purchases.purchase(order(Commodity.FOOD,1),worlds()));assertEquals(before,fixture.snapshot());
    }
    @Test void shortagePlusReserveStillRespectsFoodBudgetAndCannotUnlockStartupDuringCrisis()throws Exception {
        shortage();var purchases=buyer(VillageSupplyPolicy.prototype());assertEquals(Map.of(Commodity.FOOD,10L),purchases.demand(fixture.town,worlds()).fundedUnits());
        assertFalse(fixture.grants().preview(fixture.town,"fishing",500,1000,StartupGrantPolicy.prototype()).eligible());
        purchases.purchase(order(Commodity.FOOD,10),worlds());assertEquals(6500,fixture.cash());assertEquals(Map.of(),purchases.demand(fixture.town,worlds()).fundedUnits());
    }
    @Test void healthyReserveOrdersWaitForPresentWorldCompletePopulationAndRealMoney()throws Exception {
        healthyLowReserve();var purchases=buyer(VillageSupplyPolicy.prototype());assertEquals(Map.of(),purchases.demand(fixture.town,Set.of()).fundedUnits());
        fixture.sql("UPDATE needs_periods SET unassigned_residents=0 WHERE period=3");assertEquals(Map.of(),purchases.demand(fixture.town,worlds()).fundedUnits());
        fixture.sql("UPDATE needs_periods SET unassigned_residents=2 WHERE period=3");
        new TreasuryRepository(fixture.db.getConnection()).post(UUID.randomUUID(),fixture.town,-6000,"Explicit fixture low balance");
        assertEquals(Map.of(),purchases.demand(fixture.town,worlds()).fundedUnits());assertEquals(2500,fixture.cash());
    }
    @Test void normalFoodRuntimeBuysEmergencyReserveWithoutARecordedShortage()throws Exception {
        healthyLowReserve();fixture.sql("UPDATE economy_stocks SET units=7 WHERE commodity='food'");
        fixture.economy=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);
        fixture.economy.configureVillageSupplies(VillageSupplyPolicy.prototype());fixture.economy.startOnline(4000,worlds());
        fixture.economy.pulse(5000,false);assertEquals(0,scalar("SELECT demand-fulfilled FROM needs_periods WHERE period=4"));
        assertEquals(1,scalar("SELECT COUNT(*) FROM village_supply_orders"));assertEquals(5,scalar("SELECT units FROM village_supply_orders"));
        assertEquals(7500,fixture.cash());fixture.economy.pulse(5000,false);assertEquals(1,scalar("SELECT COUNT(*) FROM village_supply_orders"));
    }

}
