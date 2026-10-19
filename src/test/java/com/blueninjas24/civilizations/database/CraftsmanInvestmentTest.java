package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.Path;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CraftsmanInvestmentTest {
    @TempDir Path directory;IndustryRepositoryTest f;UUID candidate=UUID.randomUUID();
    IndustryRuntimePolicy runtime;WorkshopStarterPolicy starter;EconomyRuntimeRepository economy;EconomySettings settings;
    @BeforeEach void setup()throws Exception {
        f=new IndustryRepositoryTest();f.directory=directory;f.setup();f.stores(100);
        f.database.getBusinessRepository().hire(UUID.randomUUID(),f.warehouse.id(),f.founder,0);
        f.database.getPopulationRepository().insertResident(new Resident(candidate,"Potential craft founder",f.town,null,300,Resident.Status.ALIVE));
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var stock=Map.of(Commodity.FOOD,100L,Commodity.TIMBER,10L,Commodity.STONE,10L);
        var prices=Map.of(Commodity.FOOD,BigDecimal.valueOf(2),Commodity.TIMBER,BigDecimal.valueOf(4),Commodity.STONE,BigDecimal.valueOf(3));
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),stock,prices,bands);
        economy=new EconomyRuntimeRepository(f.database.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.observe(new FarmObservation(f.town,f.world,1000,true,0),1000);economy.startOnline(1000,Set.of(f.world));economy.pulse(2000,false);
        starter=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.TIMBER,400L,Commodity.STONE,300L));
        var original=IndustryPolicy.prototype();var weights=new TreeMap<>(original.storageSpacePerUnit());weights.put(StorageUpgrades.FITTINGS,1L);
        var storage=new IndustryPolicy(150,8,32,1024,1,128,original.miningBundle(),weights);
        var recipe=ProductionRecipe.storageFittings();runtime=new IndustryRuntimePolicy(true,1000,8,storage,Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,starter.prices()),WarehouseSupplyPolicy.disabled(),ToolUpkeepPolicy.disabled(),StorageUpgradePolicy.prototype());
        f.sql("UPDATE industry_stock SET units=80 WHERE business_id='"+f.warehouse.id()+"' AND commodity='coal'");
        f.sql("INSERT INTO industry_runtime VALUES('"+f.warehouse.id()+"',1000,1000,0,0,0)");
        markers(1000,true,WorkplaceObservation.Kind.CRAFTING_TABLE);
    }
    @AfterEach void close(){if(f!=null)f.close();}
    void markers(long time,boolean complete,WorkplaceObservation.Kind kind)throws SQLException {
        new WorkplaceOpportunities(f.database.getConnection()).observe(f.town,new WorkplaceObservation(f.world,0,0,time,complete,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(1,64,0),kind))));
    }
    CraftsmanOpportunities opportunities(){return new CraftsmanOpportunities(f.database.getConnection(),WorkshopOpportunityPolicy.prototype(),runtime,StartupGrantPolicy.prototype(),starter,BusinessOperationPolicy.prototype());}
    CraftsmanInvestment engine(){return engine(WorkshopFoundingPolicy.prototype());}
    CraftsmanInvestment engine(WorkshopFoundingPolicy policy){return new CraftsmanInvestment(f.database.getConnection(),opportunities(),policy,WorkshopOpportunityPolicy.prototype(),runtime,StartupGrantPolicy.prototype(),starter,216,73);}
    CraftsmanInvestment.Draft draft()throws Exception{return engine().prepare(f.town,0,1000,Set.of(f.world)).draft().orElseThrow();}
    WorkplaceOperations.Confirmation proof(CraftsmanInvestment.Draft d,boolean present){return new WorkplaceOperations.Confirmation(d.anchor(),present,1000);}
    Map<String,List<String>> snapshot()throws Exception {
        var data=new TreeMap<String,List<String>>();var names=new ArrayList<String>();try(var s=f.database.getConnection().createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")){while(r.next())names.add(r.getString(1));}
        for(var name:names){var rows=new ArrayList<String>();try(var s=f.database.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM \""+name.replace("\"","\"\"")+"\"")){while(r.next()){var row=new ArrayList<Object>();for(int n=1;n<=r.getMetaData().getColumnCount();n++)row.add(r.getObject(n));rows.add(row.toString());}}Collections.sort(rows);data.put(name,rows);}return data;
    }
    void qualify()throws SQLException{f.database.getPopulationRepository().recordSkillExperience(candidate,"crafting",10);}
    long units(Commodity good)throws SQLException{return new CommodityRepository(f.database.getConnection()).read(f.town).orElseThrow().state().stocks().get(good).units();}
    @Test void alternateEditableRecipeCanOpenProduceAndSellWithoutUnavailableBaselineMaterials()throws Exception {
        qualify();f.sql("UPDATE economy_stocks SET units=0 WHERE commodity='timber'");
        var alternative=new ProductionRecipe("z_stone_fittings","craftsman",Map.of(Commodity.STONE,3L),Map.of(StorageUpgrades.FITTINGS,1L),1,4);
        var recipes=new TreeMap<>(runtime.recipes());recipes.put(alternative.id(),alternative);
        runtime=new IndustryRuntimePolicy(runtime.enabled(),runtime.millisPerPeriod(),runtime.businessesPerPulse(),runtime.mining(),recipes,
                runtime.supply(),runtime.warehouseSupply(),runtime.tools(),runtime.storageUpgrades());
        var d=draft();assertEquals(alternative,d.supplies().recipe());long stone=units(Commodity.STONE);
        var opened=engine().accept(d,proof(d,true),1000,Set.of(f.world));
        assertEquals(stone-3,units(Commodity.STONE));assertEquals(0,units(Commodity.TIMBER));assertEquals(3100,opened.store().hundredths());
        // Incoming paid starter materials guide choice but cannot become products before delivery.
        assertEquals(alternative,craftChoice(d,runtime));
        assertEquals(0,f.industry.store(d.anchor().business()).orElseThrow().units().get(StorageUpgrades.FITTINGS));
        var live=new IndustryRuntime(f.database.getConnection(),runtime,BusinessOperationPolicy.prototype());live.start(1000);
        assertEquals(0,live.pulse(1500,Set.of(f.world)));
        for(long time=2000;time<=6000;time+=1000)live.pulse(time,Set.of(f.world));
        var buyer=f.industry.store(f.warehouse.id()).orElseThrow();var maker=f.industry.store(d.anchor().business()).orElseThrow();
        assertEquals(200,buyer.capacity());assertEquals(5100,maker.hundredths());assertEquals(0,maker.units().get(StorageUpgrades.FITTINGS));
        assertEquals(29500,new TreasuryRepository(f.database.getConnection()).load(f.town).orElseThrow().hundredths()+buyer.hundredths()+maker.hundredths()+f.stock(f.shop).hundredths());
        var before=snapshot();engine().accept(d,proof(d,true),1000,Set.of(f.world));assertEquals(before,snapshot());
        // With the paid starter consumed, an actual affordable warehouse route still selects the alternate.
        f.sql("UPDATE industry_stock SET units=3 WHERE business_id='"+f.warehouse.id()+"' AND commodity='stone'");
        before=snapshot();assertEquals(alternative,craftChoice(d,runtime));assertEquals(before,snapshot());
        f.sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+d.anchor().business()+"'");
        assertEquals(ProductionRecipe.storageFittings(),craftChoice(d,runtime),"An unaffordable optional recipe cannot justify purchasing");
    }
    @Test void oldCraftStoreCatalogueIsRetainedWhenAdminsAddOtherProducts()throws Exception {
        qualify();var d=draft();engine().accept(d,proof(d,true),1000,Set.of(f.world));
        var unsupported=new ProductionRecipe("a_new_product","craftsman",Map.of(Commodity.STONE,1L),Map.of(new Commodity("lamp"),1L),1,1);
        var recipes=new TreeMap<>(runtime.recipes());recipes.put(unsupported.id(),unsupported);
        var changed=new IndustryRuntimePolicy(true,1000,8,runtime.mining(),recipes,runtime.supply(),runtime.warehouseSupply(),runtime.tools(),runtime.storageUpgrades());
        var before=snapshot();assertEquals(ProductionRecipe.storageFittings(),craftChoice(d,changed));
        assertEquals(before,snapshot());assertFalse(f.industry.store(d.anchor().business()).orElseThrow().units().containsKey(new Commodity("lamp")));
    }
    ProductionRecipe craftChoice(CraftsmanInvestment.Draft draft,IndustryRuntimePolicy policy)throws SQLException {
        return CraftRecipeChoice.select(f.database.getConnection(),draft.anchor().business(),policy,BusinessOperationPolicy.prototype(),f.industry.store(draft.anchor().business()).orElseThrow().hundredths()).orElseThrow();
    }
    @Test void currentCustomerReviewIsReadOnlyAndMissingSkillIsNeverGranted()throws Exception {
        var before=snapshot();assertEquals(1,opportunities().review(f.town,1000,Set.of(f.world)).candidates().size());assertTrue(engine().prepare(f.town,0,1000,Set.of(f.world)).draft().isEmpty());assertEquals(before,snapshot());
        qualify();var d=draft();before=snapshot();assertEquals(candidate,d.startup().worker());assertEquals(d,draft());assertEquals(before,snapshot());
        f.database.close();f.open();assertEquals(d,draft());assertEquals(before,snapshot());
        assertTrue(engine(WorkshopFoundingPolicy.disabled()).prepare(f.town,0,1000,Set.of(f.world)).draft().isEmpty());
    }
    @Test void realPaidStartupProductionAndCustomerInstallationConserveGoodsAndCash()throws Exception {
        qualify();var d=draft();long timber=units(Commodity.TIMBER),stone=units(Commodity.STONE);
        var opened=engine().accept(d,proof(d,true),1000,Set.of(f.world));assertEquals(2500,opened.store().hundredths());assertEquals(7,opened.store().reservedIncomingSpace());
        assertEquals(timber-3,units(Commodity.TIMBER));assertEquals(stone-1,units(Commodity.STONE));var before=snapshot();engine().accept(d,proof(d,true),1000,Set.of(f.world));assertEquals(before,snapshot());
        var live=new IndustryRuntime(f.database.getConnection(),runtime,BusinessOperationPolicy.prototype());live.start(1000);
        for(long time=2000;time<=6000;time+=1000)live.pulse(time,Set.of(f.world));
        var buyer=f.industry.store(f.warehouse.id()).orElseThrow();var maker=f.industry.store(d.anchor().business()).orElseThrow();
        assertEquals(200,buyer.capacity());assertEquals(80,buyer.units().get(Commodity.COAL));assertEquals(4000,buyer.hundredths());assertEquals(4500,maker.hundredths());assertEquals(0,maker.units().get(StorageUpgrades.FITTINGS));
        long treasury=new TreasuryRepository(f.database.getConnection()).load(f.town).orElseThrow().hundredths();assertEquals(29500,treasury+buyer.hundredths()+maker.hundredths()+f.stock(f.shop).hundredths());
        assertTrue(opportunities().review(f.town,1000,Set.of(f.world)).candidates().isEmpty(),"Retained first firm blocks duplicate founding");
    }
    @Test void noNeedAbsentWorldBacklogAndChangedStockCannotJustifyOpening()throws Exception {
        qualify();var d=draft();
        long timber=units(Commodity.TIMBER);
        var mutations=List.of(
            List.of("UPDATE industry_stock SET units=1 WHERE business_id='"+f.warehouse.id()+"' AND commodity='coal'","UPDATE industry_stock SET units=80 WHERE business_id='"+f.warehouse.id()+"' AND commodity='coal'"),
            List.of("UPDATE industry_runtime SET target=1","UPDATE industry_runtime SET target=0"),
            List.of("UPDATE economy_stocks SET units=2 WHERE commodity='timber'","UPDATE economy_stocks SET units="+timber+" WHERE commodity='timber'"),
            List.of("UPDATE industry_stores SET hundredths=2500 WHERE business_id='"+f.warehouse.id()+"'","UPDATE industry_stores SET hundredths=6000 WHERE business_id='"+f.warehouse.id()+"'"));
        for(var pair:mutations) {
            f.sql(pair.get(0));var before=snapshot();assertThrows(BusinessStartupGrants.Declined.class,()->engine().accept(d,proof(d,true),1000,Set.of(f.world)));assertEquals(before,snapshot());f.sql(pair.get(1));
        }
        var before=snapshot();assertThrows(BusinessStartupGrants.Declined.class,()->engine().accept(d,proof(d,true),1000,Set.of()));assertEquals(before,snapshot());
        assertThrows(SQLException.class,()->engine().accept(d,proof(d,false),1000,Set.of(f.world)));assertEquals(before,snapshot());
    }
    @Test void finalFailureRollsBackAllMoneyGoodsIdentityWorkerAndMarkerThenRetries()throws Exception {
        qualify();var d=draft();f.sql("CREATE TRIGGER refuse_craft BEFORE INSERT ON workshop_starter_orders BEGIN SELECT RAISE(ABORT,'fixture final order failure'); END");var before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(d,proof(d,true),1000,Set.of(f.world)));assertEquals(before,snapshot());f.sql("DROP TRIGGER refuse_craft");engine().accept(d,proof(d,true),1000,Set.of(f.world));
        f.database.close();f.open();before=snapshot();engine(WorkshopFoundingPolicy.disabled()).accept(d,proof(d,true),1000,Set.of(f.world));assertEquals(before,snapshot());
    }
    @Test void markerFreshnessPublicReservesAndProtectedGrantBudgetAreMandatory()throws Exception {
        assertTrue(opportunities().review(f.town,70001,Set.of(f.world)).candidates().isEmpty());assertTrue(opportunities().review(f.town,1000,Set.of()).candidates().isEmpty());
        markers(1001,false,WorkplaceObservation.Kind.CRAFTING_TABLE);assertTrue(opportunities().review(f.town,1001,Set.of(f.world)).candidates().isEmpty());
        markers(1002,true,WorkplaceObservation.Kind.SMOKER);assertTrue(opportunities().review(f.town,1002,Set.of(f.world)).candidates().isEmpty());
        markers(1003,true,WorkplaceObservation.Kind.CRAFTING_TABLE);assertFalse(opportunities().review(f.town,1003,Set.of(f.world)).candidates().isEmpty());
        f.sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");economy.pulse(3000,false);assertTrue(opportunities().review(f.town,1003,Set.of(f.world)).candidates().isEmpty());
    }
    @Test void changedEmploymentOrQualificationDeclinesWithoutPartialOpening()throws Exception {
        qualify();var d=draft();f.database.getBusinessRepository().hire(UUID.randomUUID(),f.warehouse.id(),candidate,0);var before=snapshot();
        assertThrows(BusinessStartupGrants.Declined.class,()->engine().accept(d,proof(d,true),1000,Set.of(f.world)));assertEquals(before,snapshot());
    }
    @Test void actualStockedSupplierCoversNeedButUnstaffedStockDoesNotInventCoverage()throws Exception {
        var source=f.business("warehouse",40);f.industry.openStore(source.id(),100,Map.of(StorageUpgrades.FITTINGS,1L),0);
        var worker=UUID.randomUUID();f.database.getPopulationRepository().insertResident(new Resident(worker,"Fixture supplier worker",f.town,null,300,Resident.Status.ALIVE));
        var job=f.database.getBusinessRepository().hire(UUID.randomUUID(),source.id(),worker,0);
        f.sql("UPDATE industry_stock SET units=1 WHERE business_id='"+source.id()+"' AND commodity='storage_fittings'");
        // Reassess the actual larger roster before asking for public startup money.
        economy.pulse(3000,false);
        var before=snapshot();assertTrue(opportunities().review(f.town,1000,Set.of(f.world)).candidates().isEmpty());assertEquals(before,snapshot());
        f.database.getBusinessRepository().endEmployment(job.id(),1,"Fixture supplier unavailable");
        assertFalse(opportunities().review(f.town,1000,Set.of(f.world)).candidates().isEmpty());
    }
    @Test void sqlOwnerSessionPreparesAndAcceptsWithoutSharingItsConnection()throws Exception {
        qualify();f.database.close();var prototype=com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype();
        var sessionSettings=new com.blueninjas24.civilizations.application.PopulationSession.Settings(0,prototype.millisPerMonth(),prototype.maximumOfflineMonths(),prototype.monthsPerBatch(),prototype.population(),prototype.migration(),prototype.recognition(),prototype.enrollment());
        CraftsmanInvestment.Draft proposal;
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),java.util.logging.Logger.getLogger("CraftsmanOwnerTest"),sessionSettings,()->1000,settings,FarmObservation.Policy.prototype())) {
            session.configureIndustryRuntime(runtime);session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureWorkplaces(WorkplaceObserverPolicy.prototype());
            session.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),starter);session.configureCraftsman(WorkshopFoundingPolicy.prototype(),WorkshopOpportunityPolicy.prototype());
            session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());session.start(false,Set.of(f.world)).get(10,java.util.concurrent.TimeUnit.SECONDS);
            proposal=session.prepareCraftsmanInvestment(f.town).get(5,java.util.concurrent.TimeUnit.SECONDS).draft().orElseThrow();
            session.acceptCraftsmanInvestment(proposal,proof(proposal,true)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertThrows(IllegalStateException.class,()->session.configureCraftsman(WorkshopFoundingPolicy.disabled(),WorkshopOpportunityPolicy.disabled()));
        } finally{f.open();}
        assertEquals(candidate,f.database.getBusinessRepository().findBusiness(proposal.anchor().business()).orElseThrow().ownerId());
        assertEquals(2500,f.industry.store(proposal.anchor().business()).orElseThrow().hundredths());
    }

}
