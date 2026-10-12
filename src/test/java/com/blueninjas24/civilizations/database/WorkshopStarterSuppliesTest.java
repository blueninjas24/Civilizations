package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorkshopStarterSuppliesTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),person=UUID.randomUUID(),business=UUID.randomUUID(),operation=UUID.randomUUID();
    final ProductionRecipe recipe=ProductionRecipe.smithTools();
    final WorkshopStarterPolicy policy=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L,Commodity.COAL,600L));
    BusinessStartupGrants.WorkshopRequest workshop;
    WorkshopStarterSupplies.Request order;
    void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("StarterTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Starter fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Founder",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture treasury");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var units=Map.of(Commodity.FOOD,100L,Commodity.IRON,10L,Commodity.COAL,10L);
        var prices=Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.IRON,BigDecimal.TEN,Commodity.COAL,BigDecimal.TEN);
        var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),units,prices,bands);
        var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,64,0,10,"Explicit fixture workplace");
        var firm=new Business(business,town,"Smith",person,person,"blacksmith",site.id(),2,0);
        var requirements=new JobRequirements(216,Map.of());
        workshop=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(firm,site,requirements,requirements,500,"Fixture opportunity"),
            100,Map.of(Commodity.IRON,1L,Commodity.COAL,1L,ToolUpkeepPolicy.TOOLS,1L),4000,person);
        order=new WorkshopStarterSupplies.Request(operation,business,recipe,1,0,1,128);
    }
    BusinessStartupGrants.Workshop start()throws SQLException{return new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),order,policy,Set.of(world));}
    WorkshopStarterSupplies supplies(){return new WorkshopStarterSupplies(db.getConnection());}
    IndustryRepository.Store store()throws SQLException{return db.getIndustryRepository().store(business).orElseThrow();}
    long cash()throws SQLException{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    long units(Commodity good)throws SQLException{return new CommodityRepository(db.getConnection()).read(town).orElseThrow().state().stocks().get(good).units();}
    long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String query)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    @AfterEach void close(){if(db!=null)db.close();}
    @Test void actualStarterBundleTransfersRealCashAndSurplusReservingPaidCargo()throws Exception {
        long food=units(Commodity.FOOD);var started=start();
        assertEquals(8500,cash());assertEquals(1000,started.store().hundredths());assertEquals(8,units(Commodity.IRON));assertEquals(9,units(Commodity.COAL));assertEquals(food,units(Commodity.FOOD));
        assertEquals(3,store().reservedIncomingSpace());assertEquals(97,store().freeSpace());assertEquals(0,store().units().get(Commodity.IRON));
        var saved=supplies().find(operation).orElseThrow();assertEquals(3000,saved.price());assertEquals(1,saved.foodPeriod());assertNull(saved.deliveredPeriod());assertEquals(2,count("workshop_starter_goods"));
        var grant=new BusinessStartupGrants(db.getConnection()).find(business).orElseThrow();assertEquals(4500,grant.startup()+grant.capital());
    }
    @Test void restartAndDisabledReplayCannotChargeOrDeliverTwice()throws Exception {
        start();var receipt=supplies().find(operation).orElseThrow();db.close();open();
        assertEquals(receipt,supplies().purchase(order,WorkshopStarterPolicy.disabled(),Set.of()));assertEquals(8500,cash());assertEquals(8,units(Commodity.IRON));
        db.getConnection().setAutoCommit(false);assertEquals(0,new WorkshopSupplies(db.getConnection()).deliverRows(business,0));db.getConnection().commit();db.getConnection().setAutoCommit(true);
        var runtime=new IndustryRuntime(db.getConnection(),new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe)));
        runtime.start(1000);runtime.pulse(2000,Set.of());assertEquals(0,store().units().get(ToolUpkeepPolicy.TOOLS));assertNull(supplies().find(operation).orElseThrow().deliveredPeriod());
        runtime.pulse(3000,Set.of(world));runtime.pulse(4000,Set.of(world));
        assertEquals(1,store().units().get(ToolUpkeepPolicy.TOOLS));assertEquals(0,store().reservedIncomingSpace());assertNotNull(supplies().find(operation).orElseThrow().deliveredPeriod());
        runtime.pulse(4000,Set.of(world));assertEquals(1,store().units().get(ToolUpkeepPolicy.TOOLS));assertEquals(8500,cash());
        assertThrows(SQLException.class,()->supplies().purchase(new WorkshopStarterSupplies.Request(UUID.randomUUID(),business,recipe,1,0,1,128),policy,Set.of(world)));
    }
    @Test void changedRecipeOrIdentityTermsCannotReuseAcceptedPayment()throws Exception {
        start();var changed=new ProductionRecipe(recipe.id(),recipe.industry(),Map.of(Commodity.IRON,1L,Commodity.COAL,1L),recipe.outputs(),2,8);
        assertThrows(SQLException.class,()->supplies().purchase(new WorkshopStarterSupplies.Request(operation,business,changed,1,0,1,128),policy,Set.of(world)));assertEquals(8500,cash());
    }
    @Test void paidIncomingBundleSuppressesDuplicatePurchasingAndUpstreamDemand()throws Exception {
        // Enough remaining cash to order one more bundle; paid cargo must prevent that duplicate.
        var limited=new ProductionRecipe(recipe.id(),recipe.industry(),recipe.inputs(),recipe.outputs(),1,1);
        order=new WorkshopStarterSupplies.Request(operation,business,limited,1,0,1,128);
        workshop=new BusinessStartupGrants.WorkshopRequest(workshop.founding(),100,workshop.space(),6000,person);
        var grants=new StartupGrantPolicy(true,7000,7000,20000,2500,2,200,Set.of("blacksmith"));
        new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(workshop,grants,order,policy,Set.of(world));
        var supplies=new WorkshopSupplyPolicy(true,1,16,policy.prices());
        var runtimePolicy=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(limited.id(),limited),supplies);
        var review=new LocalIndustryDemand(db.getConnection(),runtimePolicy,BusinessOperationPolicy.disabled()).review(town,Set.of(world),64);
        assertTrue(review.complete());assertTrue(review.uncovered().isEmpty());assertTrue(review.customers().getFirst().missing().isEmpty());
        db.getConnection().setAutoCommit(false);new WorkshopSupplies(db.getConnection()).acquireRows(business,limited,1,1,1000,supplies,IndustryPolicy.prototype());db.getConnection().commit();db.getConnection().setAutoCommit(true);
        assertEquals(0,count("local_shipments"));assertEquals(1,count("workshop_starter_orders"));
    }
    @Test void paidStarterCargoRemainsAnAssetInOwnerDeathSaleValuation()throws Exception {
        start();db.getPopulationRepository().updateResident(new Resident(person,"Founder",town,null,300,Resident.Status.DECEASED));
        new AutomaticBusinessResale(db.getConnection(),BusinessResalePolicy.prototype(),BusinessPurchasePolicy.prototype(),42).review(business,1,1000);
        assertEquals(5000,new BusinessSaleListings(db.getConnection()).find(business,person).orElseThrow().price());
        assertEquals(3,store().reservedIncomingSpace());assertEquals(8500,cash());
    }
    @Test void legacyStoreRemainsInspectableAfterFailedUpgradeAndNoCargoIsFabricated()throws Exception {
        new BusinessStartupGrants(db.getConnection()).startWorkshop(workshop,StartupGrantPolicy.prototype());
        SchemaFixtures.rewind(db.getConnection(),59);sql("CREATE INDEX pending_workshop_starters ON residents(name)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        assertEquals(4000,store().hundredths());assertEquals(0,store().reservedIncomingSpace());assertEquals(5500,cash());
        sql("DROP INDEX pending_workshop_starters");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("workshop_starter_orders"));
    }
    @Test void protectedMaterialBoundaryWorksAndMissingWholeBundleRollsBackFounding()throws Exception {
        sql("UPDATE economy_stocks SET units=3 WHERE commodity='iron'");assertThrows(SQLException.class,this::start);assertUnchanged();
        sql("UPDATE economy_stocks SET units=4 WHERE commodity='iron'");start();assertEquals(2,units(Commodity.IRON));
    }
    @Test void foodProofPopulationAndAbsentWorldCannotBeBypassed()throws Exception {
        sql("UPDATE needs_periods SET fulfilled=0");assertThrows(SQLException.class,this::start);assertUnchanged();sql("UPDATE needs_periods SET fulfilled=demand");
        assertThrows(SQLException.class,()->new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),order,policy,Set.of()));assertUnchanged();
        db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Arrival",town,null,300,Resident.Status.ALIVE));assertThrows(SQLException.class,this::start);assertUnchanged();
    }
    @Test void priceCapacityCapitalAndCargoCapsAreEnforcedAtomically()throws Exception {
        for(var terms:List.of(new WorkshopStarterPolicy(true,1,2999,2,policy.prices()),WorkshopStarterPolicy.disabled(),new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L)))) {
            assertThrows(SQLException.class,()->new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),order,terms,Set.of(world)));assertUnchanged();
        }
        var small=new BusinessStartupGrants.WorkshopRequest(workshop.founding(),2,workshop.space(),4000,person);
        assertThrows(SQLException.class,()->new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(small,StartupGrantPolicy.prototype(),order,policy,Set.of(world)));assertUnchanged();
        var poor=new BusinessStartupGrants.WorkshopRequest(workshop.founding(),100,workshop.space(),2999,person);
        assertThrows(SQLException.class,()->new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(poor,StartupGrantPolicy.prototype(),order,policy,Set.of(world)));assertUnchanged();
        var tight=new WorkshopStarterSupplies.Request(operation,business,recipe,1,0,1,2);
        assertThrows(SQLException.class,()->new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),tight,policy,Set.of(world)));assertUnchanged();
    }
    @Test void latePurchaseFailureRollsBackEveryPaymentMaterialAndFounderRecord()throws Exception {
        sql("CREATE TRIGGER reject_starter BEFORE INSERT ON workshop_starter_goods WHEN NEW.commodity='iron' BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,this::start);assertUnchanged();assertEquals(10,units(Commodity.COAL));assertEquals(10,units(Commodity.IRON));
        sql("DROP TRIGGER reject_starter");start();assertEquals(8500,cash());
    }
    @Test void deliveryFailureRollsBackInputsAndReceiptTogether()throws Exception {
        start();sql("CREATE TRIGGER reject_delivery BEFORE UPDATE OF delivered_period ON workshop_starter_orders BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        db.getConnection().setAutoCommit(false);assertThrows(SQLException.class,()->new WorkshopSupplies(db.getConnection()).deliverRows(business,1));db.getConnection().rollback();db.getConnection().setAutoCommit(true);
        assertEquals(0,store().units().get(Commodity.IRON));assertEquals(3,store().reservedIncomingSpace());assertNull(supplies().find(operation).orElseThrow().deliveredPeriod());
        sql("DROP TRIGGER reject_delivery");db.getConnection().setAutoCommit(false);assertEquals(1,new WorkshopSupplies(db.getConnection()).deliverRows(business,1));db.getConnection().commit();db.getConnection().setAutoCommit(true);assertEquals(2,store().units().get(Commodity.IRON));
    }
    @Test void migrationFailureIsTransactionalPreservesOldAssetsAndRetriesEmpty()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),59);sql("CREATE INDEX pending_workshop_starters ON residents(name)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(59,r.getInt(1));}
        sql("DROP INDEX pending_workshop_starters");new SchemaMigrator().migrate(db.getConnection());assertUnchanged();start();
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA integrity_check")){assertEquals("ok",r.getString(1));}
    }
    @Test void foodCannotBeSoldAndOverflowCannotPartiallySpend()throws Exception {
        assertThrows(IllegalArgumentException.class,()->new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.FOOD,100L)));
        assertThrows(ArithmeticException.class,()->new WorkshopStarterSupplies.Request(operation,business,recipe,1,Long.MAX_VALUE,1,128));assertUnchanged();
    }
    void assertUnchanged()throws SQLException {
        assertEquals(10000,cash());for(var table:List.of("workshop_starter_orders","workshop_starter_goods","businesses","business_sites","startup_grants","business_employment","industry_stores"))assertEquals(0,count(table),table);
        assertEquals(1,count("treasury_entries"));assertTrue(db.getConnection().getAutoCommit());
    }
}
