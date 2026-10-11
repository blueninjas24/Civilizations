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

class FarmToolPurchasesTest {
    @TempDir Path directory;DatabaseManager db;EconomyRuntimeRepository economy;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),farmer=new UUID(0,1),merchant=new UUID(0,2),seller=UUID.randomUUID();
    UUID farm;FarmToolPurchases.Request request;
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("FarmEquipmentTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Farm equipment fixture","Fixture",0,64,0,2,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var person:List.of(farmer,merchant))db.getPopulationRepository().insertResident(new Resident(person,"Fixture adult",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,20000,"Explicit disposable fixture funds");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L),Map.of(Commodity.FOOD,BigDecimal.ONE),bands);
        economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));
        economy.observe(new FarmObservation(town,world,1000,true,64,new VillageObservation.Position(0,64,0)),1000);
        economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);farm=FarmEnterprises.businessId(town);
        assertEquals(1,new BusinessRepository(db.getConnection()).currentWorkers(farm).size());
        var site=new BusinessSite(UUID.randomUUID(),town,world,10,64,0,1000,"Explicit fixture workshop");
        var requirements=new JobRequirements(216,Map.of());
        var business=new Business(seller,town,"Fixture smith",merchant,merchant,"blacksmith",site.id(),1,0);
        var businesses=new BusinessRepository(db.getConnection());businesses.found(new BusinessFounding(business,site,requirements,requirements,0,"Explicit fixture"));
        businesses.hire(UUID.randomUUID(),seller,merchant,0);
        db.getIndustryRepository().openStore(seller,100,Map.of(Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L),0);
        // Raw fixture input is explicit; tools must be made by the real recipe before purchase.
        sql("UPDATE industry_stock SET units=2 WHERE commodity='iron'");
        db.getIndustryRepository().process(UUID.randomUUID(),seller,new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),2,2),2,0);
        assertEquals(2,stock(seller));request=new FarmToolPurchases.Request(UUID.randomUUID(),farm,seller,1,1,10);
    }
    FarmToolPurchases purchases(){return new FarmToolPurchases(db.getConnection(),BusinessOperationPolicy.disabled());}
    FarmToolBudgetPolicy policy(){return FarmToolBudgetPolicy.prototype();}
    FarmToolBudgetPolicy generous(){return new FarmToolBudgetPolicy(true,4000,12000,2500,2,200);}
    @Test void actualPublicPaymentAndManufacturedGoodsConserveThroughDeliveryRestartAndDisabledReplay()throws Exception {
        var beforeFood=new CommodityRepository(db.getConnection()).read(town).orElseThrow();
        var receipt=purchases().purchase(request,policy(),Set.of(world));
        assertEquals(16000,cash());assertEquals(4000,db.getIndustryRepository().store(seller).orElseThrow().hundredths());
        assertEquals(0,db.getIndustryRepository().store(farm).orElseThrow().hundredths());assertEquals(1,stock(seller));assertEquals(0,stock(farm));
        assertEquals(1,db.getIndustryRepository().store(farm).orElseThrow().reservedIncomingSpace());
        assertEquals(beforeFood,new CommodityRepository(db.getConnection()).read(town).orElseThrow());
        assertThrows(SQLException.class,()->db.getIndustryRepository().deliver(request.operation(),1));
        db.close();open();assertEquals(receipt,purchases().purchase(request,FarmToolBudgetPolicy.disabled(),Set.of()));
        db.getIndustryRepository().deliver(request.operation(),2);db.getIndustryRepository().deliver(request.operation(),2);
        assertEquals(1,stock(farm));assertEquals(1,count("farm_tool_purchases"));assertEquals(16000,cash());
        var changed=new FarmToolPurchases.Request(request.operation(),farm,seller,1,2,10);
        assertThrows(SQLException.class,()->purchases().purchase(changed,policy(),Set.of(world)));assertEquals(16000,cash());
        health();
    }
    @Test void auditedPublicEquipmentDoesNotDisableUnrelatedWorkshopDemandButUnknownFarmCatalogStillDefers()throws Exception {
        var recipe=new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),1,1);
        var runtime=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe),
                new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,100L)));
        purchases().purchase(request,policy(),Set.of(world));
        var demand=new LocalIndustryDemand(db.getConnection(),runtime,BusinessOperationPolicy.disabled());
        assertTrue(demand.review(town,Set.of(world),8).complete());
        assertEquals(Map.of(Commodity.IRON,1L),demand.review(town,Set.of(world),8).uncovered());
        // Deliberate corrupt/unsupported fixture: do not broadly ignore every farm store.
        sql("UPDATE industry_stores SET capacity=2 WHERE business_id='"+farm+"'");
        assertFalse(demand.review(town,Set.of(world),8).complete());assertEquals(16000,cash());
    }
    @Test void noDoubleOrderForHeldOrIncomingKitEvenWithHigherBudget()throws Exception {
        purchases().purchase(request,policy(),Set.of(world));
        var second=new FarmToolPurchases.Request(UUID.randomUUID(),farm,seller,1,1,10);
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(second,generous(),Set.of(world)));
        db.getIndustryRepository().deliver(request.operation(),2);
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(second,generous(),Set.of(world)));
        assertEquals(1,stock(seller));assertEquals(16000,cash());assertEquals(1,count("local_shipments"));
    }
    @Test void periodBudgetSurvivesUseAndRestartAndResumesOnlyAfterRealFoodPeriod()throws Exception {
        purchases().purchase(request,policy(),Set.of(world));db.getIndustryRepository().deliver(request.operation(),2);
        db.getIndustryRepository().consume(UUID.randomUUID(),farm,ToolUpkeepPolicy.TOOLS,1,IndustryRepository.Use.MAINTENANCE,"Explicit fixture productive farm maintenance");
        var second=new FarmToolPurchases.Request(UUID.randomUUID(),farm,seller,1,1,10);
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(second,policy(),Set.of(world)));assertEquals(16000,cash());
        economy.pulse(3000,false);var next=new FarmToolPurchases.Request(UUID.randomUUID(),farm,seller,2,1,10);
        purchases().purchase(next,policy(),Set.of(world));assertEquals(12000,cash());assertEquals(2,count("farm_tool_purchases"));assertEquals(0,stock(seller));
        db.close();open();purchases().purchase(next,FarmToolBudgetPolicy.disabled(),Set.of());assertEquals(12000,cash());health();
    }
    @Test void staleFoodUnknownPopulationShortageIncompleteEvidenceAndAbsentWorldRefuseWithoutSpending()throws Exception {
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,policy(),Set.of()));unchanged();
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,FarmToolBudgetPolicy.disabled(),Set.of(world)));unchanged();
        sql("UPDATE needs_periods SET fulfilled=0");assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,policy(),Set.of(world)));unchanged();
        sql("UPDATE needs_periods SET fulfilled=demand");sql("UPDATE farm_evidence SET complete=0");assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,policy(),Set.of(world)));unchanged();
        sql("UPDATE farm_evidence SET complete=1");sql("UPDATE economy_accounts SET completed_periods=2");assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,policy(),Set.of(world)));unchanged();
        sql("UPDATE economy_accounts SET completed_periods=1");db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"New arrival",town,null,300,Resident.Status.ALIVE));
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,policy(),Set.of(world)));unchanged();
    }
    @Test void legacyFoodProductionWithoutSavedFarmWorkProofCannotClaimEquipmentBudget()throws Exception {
        sql("DELETE FROM farm_learning_reviews");
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,policy(),Set.of(world)));unchanged();
    }
    @Test void unavailableGoodsAndAuditFailureRollbackEquipmentStoreCashCargoAndQuota()throws Exception {
        sql("UPDATE industry_stock SET units=0 WHERE commodity='tools'");assertThrows(SQLException.class,()->purchases().purchase(request,policy(),Set.of(world)));unchanged(0);
        sql("UPDATE industry_stock SET units=2 WHERE commodity='tools'");
        sql("CREATE TRIGGER reject_farm_invoice BEFORE INSERT ON farm_tool_purchases BEGIN SELECT RAISE(ABORT,'fixture invoice failure'); END");
        assertThrows(SQLException.class,()->purchases().purchase(request,policy(),Set.of(world)));unchanged();
        sql("DROP TRIGGER reject_farm_invoice");purchases().purchase(request,policy(),Set.of(world));assertEquals(16000,cash());health();
    }
    @Test void legacyFarmStoreCatalogAndCashArePreservedInsteadOfSilentlyRewritten()throws Exception {
        db.getIndustryRepository().openStore(farm,10,Map.of(Commodity.STONE,1L),0);
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,policy(),Set.of(world)));
        assertEquals(Map.of(Commodity.STONE,0L),db.getIndustryRepository().store(farm).orElseThrow().units());assertEquals(20000,cash());assertEquals(2,stock(seller));
    }
    @Test void unstaffedClosedOrNonfarmBusinessCannotClaimPublicEquipmentFunds()throws Exception {
        var nonfarm=new FarmToolPurchases.Request(UUID.randomUUID(),seller,farm,1,1,10);
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(nonfarm,policy(),Set.of(world)));unchanged();
        db.getPopulationRepository().updateResident(new Resident(farmer,"Fixture adult",town,null,300,Resident.Status.DECEASED));
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,policy(),Set.of(world)));unchanged();
    }
    @Test void savedAssignmentCannotSubstituteForCurrentWorkerQualifications()throws Exception {
        db.getPopulationRepository().updateResident(new Resident(farmer,"Fixture adult",town,null,100,Resident.Status.ALIVE));
        var declined=assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,policy(),Set.of(world)));
        assertTrue(declined.getMessage().contains("Qualified"));unchanged();
    }
    @Test void protectedCashUsesFoodBufferOrMinimumAndExactBoundaryIsAllowed()throws Exception {
        var guarded=new FarmToolBudgetPolicy(true,4000,4000,16001,2,200);
        assertThrows(FarmToolPurchases.Declined.class,()->purchases().purchase(request,guarded,Set.of(world)));unchanged();
        var boundary=new FarmToolBudgetPolicy(true,4000,4000,16000,2,200);
        assertEquals(16000,purchases().purchase(request,boundary,Set.of(world)).protectedCash());assertEquals(16000,cash());
    }
    @Test void schema52UpgradeIsEmptyAndFailedMigrationPreservesAssetsThenRetries()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),52);sql("CREATE INDEX village_farm_tool_budget ON residents(name)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){r.next();assertEquals(52,r.getInt(1));}
        sql("DROP INDEX village_farm_tool_budget");new SchemaMigrator().migrate(db.getConnection());unchanged();health();
        purchases().purchase(request,policy(),Set.of(world));assertEquals(16000,cash());
    }
    long stock(UUID business)throws SQLException{return db.getIndustryRepository().store(business).orElseThrow().units().getOrDefault(ToolUpkeepPolicy.TOOLS,0L);}
    long cash()throws SQLException{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}}
    void sql(String sql)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    void unchanged()throws SQLException{unchanged(2);}
    void unchanged(long tools)throws SQLException {
        assertEquals(20000,cash());assertEquals(tools,stock(seller));assertEquals(0,count("farm_tool_purchases"));assertEquals(0,count("local_shipments"));
        assertTrue(db.getIndustryRepository().store(farm).isEmpty());assertTrue(db.getConnection().getAutoCommit());
    }
    void health()throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}}
    @AfterEach void close(){if(db!=null)db.close();}
}
