package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** End-to-end saved livestock products, warehouse delivery and timed bakery receipts. */
class BakeryLivestockChainTest {
    @TempDir Path directory;
    LivestockSupplyChainTest fixture;
    UUID bakery;
    final Commodity grain=new Commodity("grain"),milk=new Commodity("milk"),eggs=new Commodity("eggs");
    final UUID chicken=new UUID(0,81);
    final AtomicBoolean healthy=new AtomicBoolean(true);

    @BeforeEach void setup() throws Exception {
        fixture=new LivestockSupplyChainTest();fixture.directory=directory;fixture.setup();
        var c=fixture.f.f.db.getConnection();var town=fixture.f.f.town;var world=fixture.f.f.world;
        UUID worker=new UUID(3,81), site=new UUID(4,81);
        fixture.f.f.db.getPopulationRepository().insertResident(new Resident(worker,"Bakery worker",town,null,300,Resident.Status.ALIVE));
        var workplace=new BusinessSite(site,town,world,12,64,0,1,"Explicit bakery integration fixture");
        var firm=new Business(new UUID(5,81),town,"Fixture bakery",worker,worker,"bakery",site,1,0);bakery=firm.id();
        var req=new JobRequirements(216,Map.of());
        new BusinessRepository(c).found(new BusinessFounding(firm,workplace,req,req,0,"Explicit bakery integration fixture"));
        new BusinessRepository(c).hire(UUID.randomUUID(),bakery,worker,0);
        var catalog=new HashMap<Commodity,Long>();for(var good:List.of(grain,milk,eggs,Commodity.FOOD))catalog.put(good,1L);
        new IndustryRepository(c).openStore(bakery,64,catalog,2000);

        // Add a genuinely saved chicken herd. Animal identity, village ownership, business,
        // worker and store all exist before the herd repository accepts its opening roster.
        UUID chickenWorker=new UUID(3,82),chickenSite=new UUID(4,82);
        fixture.f.f.db.getPopulationRepository().insertResident(new Resident(chickenWorker,"Chicken worker",town,null,300,Resident.Status.ALIVE));
        var chickenWorkplace=new BusinessSite(chickenSite,town,world,16,64,0,1,"Explicit chicken integration fixture");
        var chickenFirm=new Business(chicken,town,"Fixture chicken herd",chickenWorker,chickenWorker,"livestock",chickenSite,1,0);
        var chickenReq=new JobRequirements(216,Map.of());new BusinessRepository(c).found(new BusinessFounding(chickenFirm,chickenWorkplace,chickenReq,chickenReq,0,"Explicit chicken integration fixture"));
        new BusinessRepository(c).hire(UUID.randomUUID(),chicken,chickenWorker,0);
        new IndustryRepository(c).openStore(chicken,64,Map.of(grain,1L,eggs,1L),500);
        UUID animal=new UUID(6,82);
        new AnimalOwnershipRepository(c).record(new AnimalOwnershipRepository.Claim(animal,world,LivestockAdoption.Ownership.VILLAGE,null,town,0,"Explicit saved integration fixture"));
        new LivestockRepository(c).open(chicken,LivestockHerd.Species.CHICKEN,grain,8,Map.of(animal,0));

        // Extend the warehouse catalogue with an empty supported slot; no product is seeded.
        fixture.f.f.sql("INSERT INTO industry_stock(business_id,commodity,space_per_unit,units) VALUES('"+fixture.warehouse+"','eggs',1,0)");
        fixture.f.f.sql("UPDATE industry_stock SET units=40 WHERE business_id='"+fixture.farm+"' AND commodity='grain'");
    }

    @AfterEach void close(){fixture.close();}
    long scalar(String sql)throws Exception{return fixture.scalar(sql);}
    IndustryRuntime runtime(Map<String,ProductionRecipe> recipes) {
        var old=fixture.policy;
        var policy=new IndustryRuntimePolicy(true,1000,16,old.mining(),recipes,
                new WorkshopSupplyPolicy(true,2,16,Map.of(grain,100L,milk,200L,eggs,100L)),
                new WarehouseSupplyPolicy(true,16,16));
        var live=new IndustryRuntime(fixture.f.f.db.getConnection(),policy,BusinessOperationPolicy.prototype());
        live.configureLivestock(LivestockPolicy.prototype(),LivestockRuntimePolicy.prototype(),LivestockWorkingArea.prototype(),healthy::get);
        return live;
    }
    @Test void realHerdProductsReachBakeryAndCakeReceiptConservesGoodsAndCash() throws Exception {
        var bread=ProductionRecipe.bakeryBread();var cake=ProductionRecipe.bakeryCake();
        var live=runtime(Map.of(bread.id(),bread,cake.id(),cake));live.start(1000);
        long initialMoney=fixture.f.f.cash()+scalar("SELECT SUM(hundredths) FROM industry_stores");
        for(int period=2;period<=24;period++) {
            live.pulse(period*1000L,Set.of(fixture.f.f.world));
            assertEquals(40,scalar("SELECT COALESCE(SUM(units),0) FROM industry_stock WHERE commodity='grain'")+scalar("SELECT COALESCE(SUM(units),0) FROM local_shipments WHERE commodity='grain' AND delivered_period IS NULL")+scalar("SELECT COALESCE(SUM(feed_used),0) FROM livestock_periods")+scalar("SELECT COALESCE(SUM(f.units),0) FROM recipe_flows f JOIN recipe_runs r USING(operation_id) WHERE f.direction='INPUT' AND f.commodity='grain'"));
            assertEquals(initialMoney,fixture.f.f.cash()+scalar("SELECT SUM(hundredths) FROM industry_stores"));
        }
        for(var source:Map.of(fixture.herd,"milk",chicken,"eggs").entrySet()) {
            assertTrue(scalar("SELECT COUNT(*) FROM local_shipments WHERE seller_id='"+source.getKey()+"' AND buyer_id='"+fixture.warehouse+"' AND commodity='"+source.getValue()+"'")>0);
            assertTrue(scalar("SELECT COUNT(*) FROM local_shipments WHERE seller_id='"+fixture.warehouse+"' AND buyer_id='"+bakery+"' AND commodity='"+source.getValue()+"'")>0);
            assertEquals(scalar("SELECT COALESCE(SUM(renewable_units),0) FROM livestock_periods WHERE business_id='"+source.getKey()+"'"),
                    scalar("SELECT COALESCE(SUM(units),0) FROM industry_stock WHERE commodity='"+source.getValue()+"'")
                    +scalar("SELECT COALESCE(SUM(units),0) FROM local_shipments WHERE commodity='"+source.getValue()+"' AND delivered_period IS NULL")
                    +scalar("SELECT COALESCE(SUM(units),0) FROM recipe_flows WHERE direction='INPUT' AND commodity='"+source.getValue()+"'"));
        }
        assertTrue(scalar("SELECT COUNT(*) FROM recipe_runs WHERE business_id='"+bakery+"' AND recipe_id='bakery_cake' AND period>0")>0,"Cake should use delivered milk and eggs");
        assertEquals(8,scalar("SELECT MAX(units) FROM recipe_flows f JOIN recipe_runs r USING(operation_id) WHERE r.business_id='"+bakery+"' AND f.direction='OUTPUT' AND f.commodity='food' AND r.recipe_id='bakery_cake'"));
        var before=scalar("SELECT COUNT(*) FROM recipe_runs WHERE business_id='"+bakery+"'");
        live.pulse(24000,Set.of(fixture.f.f.world));assertEquals(before,scalar("SELECT COUNT(*) FROM recipe_runs WHERE business_id='"+bakery+"'"));
        live.start(24000);live.pulse(25000,Set.of(fixture.f.f.world));
        assertEquals(before+1,scalar("SELECT COUNT(*) FROM recipe_runs WHERE business_id='"+bakery+"'"));
        var economy=fixture.f.f.economy;
        for(int n=3;n<=35;n++)economy.pulse(n*1000L,false);
        var buyers=new VillageSupplyPurchases(fixture.f.f.db.getConnection(),VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.prototype());
        var worlds=Set.of(fixture.f.f.world);var need=buyers.demand(fixture.f.f.town,worlds);
        long units=Math.min(need.fundedUnits().getOrDefault(Commodity.FOOD,0L),fixture.f.f.db.getIndustryRepository().store(bakery).orElseThrow().units().get(Commodity.FOOD));
        assertTrue(units>0,need.explanation());
        long cash=fixture.f.f.cash(),sellerCash=fixture.f.f.db.getIndustryRepository().store(bakery).orElseThrow().hundredths();
        var order=new VillageSupplyPurchases.Request(UUID.randomUUID(),fixture.f.f.town,bakery,Commodity.FOOD,units,need.period());
        var paid=buyers.purchase(order,worlds);assertEquals(cash-paid.price(),fixture.f.f.cash());
        assertEquals(sellerCash+paid.price(),fixture.f.f.db.getIndustryRepository().store(bakery).orElseThrow().hundredths());
        assertEquals(paid,buyers.purchase(order,worlds));economy.pulse(36000,false);
        assertEquals(paid.duePeriod(),buyers.find(order.operation()).orElseThrow().appliedPeriod());
        assertEquals(initialMoney,fixture.f.f.cash()+scalar("SELECT SUM(hundredths) FROM industry_stores"));
    }

    @Test void bakeryUsesBreadWhenLegacyStoreCannotSupportEggsAndAbsentWorldDoesNotAccrue() throws Exception {
        // A legacy catalogue remains usable without an implicit inventory migration.
        var recipes=Map.of("bakery_bread",ProductionRecipe.bakeryBread(),"bakery_cake",ProductionRecipe.bakeryCake());
        fixture.f.f.sql("UPDATE industry_stores SET capacity=64 WHERE business_id='"+bakery+"'");
        fixture.f.f.sql("DELETE FROM industry_stock WHERE business_id='"+bakery+"' AND commodity='eggs'");
        fixture.f.f.sql("DELETE FROM industry_stock WHERE business_id='"+bakery+"' AND commodity='milk'");
        var live=runtime(recipes);live.start(1000);live.pulse(2000,Set.of(fixture.f.f.world));
        assertEquals(1,scalar("SELECT COUNT(*) FROM recipe_runs WHERE business_id='"+bakery+"' AND recipe_id='bakery_bread'"));
        long saved=scalar("SELECT COUNT(*) FROM industry_production WHERE business_id='"+bakery+"'");
        live.observeAvailability(9000,Set.of());live.pulse(9000,Set.of(fixture.f.f.world));
        assertEquals(saved,scalar("SELECT COUNT(*) FROM industry_production WHERE business_id='"+bakery+"'"));
    }
    @Test void noMilkOrEggSupplyFallsBackAndLateFailureCannotSpendOrConsumeTwice()throws Exception {
        var live=runtime(Map.of("bakery_bread",ProductionRecipe.bakeryBread(),"bakery_cake",ProductionRecipe.bakeryCake()));live.start(1000);
        fixture.f.f.sql("CREATE TRIGGER fail_bakery_checkpoint BEFORE UPDATE OF completed ON industry_runtime WHEN NEW.business_id='"+bakery+"' BEGIN SELECT RAISE(ABORT,'fixture bakery checkpoint'); END");
        long cash=fixture.f.f.db.getIndustryRepository().store(bakery).orElseThrow().hundredths();
        assertThrows(java.sql.SQLException.class,()->live.pulse(2000,Set.of(fixture.f.f.world)));
        assertEquals(cash,fixture.f.f.db.getIndustryRepository().store(bakery).orElseThrow().hundredths());
        assertEquals(0,scalar("SELECT COUNT(*) FROM local_shipments WHERE buyer_id='"+bakery+"'"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM recipe_runs WHERE business_id='"+bakery+"'"));
        fixture.f.f.sql("DROP TRIGGER fail_bakery_checkpoint");live.pulse(2000,Set.of(fixture.f.f.world));
        assertEquals(1,scalar("SELECT COUNT(*) FROM recipe_runs WHERE business_id='"+bakery+"' AND recipe_id='bakery_bread'"));
    }
}
