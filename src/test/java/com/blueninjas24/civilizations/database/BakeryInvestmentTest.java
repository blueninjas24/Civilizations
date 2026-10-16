package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Qualified founding and acceptance with current evidence in temporary databases. */
class BakeryInvestmentTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),person=UUID.randomUUID(),business=UUID.randomUUID(),orderId=UUID.randomUUID();
    final Commodity grain=new Commodity("grain"),furniture=new Commodity("furniture");
    BusinessStartupGrants.WorkshopRequest request;WorkshopStarterSupplies.Request order;
    ProductionRecipe recipe;WorkshopStarterPolicy starter;WorkplaceOperations.Anchor anchor;
    EconomySettings settings;
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("BakeryInvestmentTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Processing fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Fixture founder",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture treasury");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var stock=Map.of(Commodity.FOOD,100L,grain,10L,Commodity.TIMBER,10L,Commodity.STONE,10L);
        var prices=new HashMap<Commodity,BigDecimal>();stock.keySet().forEach(g->prices.put(g,BigDecimal.ONE));
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),stock,prices,bands);
        var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
    }
    @AfterEach void close(){if(db!=null)db.close();}
    void prepare(WorkplaceObservation.Kind kind) {
        var inputs=kind==WorkplaceObservation.Kind.SMOKER?Map.of(grain,3L):Map.of(Commodity.TIMBER,2L,Commodity.STONE,1L);
        var outputs=kind==WorkplaceObservation.Kind.SMOKER?Map.of(Commodity.FOOD,3L):Map.of(furniture,1L);
        recipe=new ProductionRecipe("fixture_processing",kind.industry(),inputs,outputs,1,1);
        var prices=new HashMap<Commodity,Long>();inputs.keySet().forEach(g->prices.put(g,100L));
        starter=new WorkshopStarterPolicy(true,1,4000,2,prices);
        var space=new HashMap<Commodity,Long>();inputs.keySet().forEach(g->space.put(g,1L));outputs.keySet().forEach(g->space.put(g,1L));
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,64,0,1000,"Explicit processing fixture marker");
        var firm=new Business(business,town,"Fixture processor",person,person,kind.industry(),site.id(),2,0);
        var requirements=new JobRequirements(216,Map.of());
        request=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(firm,site,requirements,requirements,500,"Trusted fixture opportunity"),100,space,4000,person);
        order=new WorkshopStarterSupplies.Request(orderId,business,recipe,1,0,1,128);
        anchor=new WorkplaceOperations.Anchor(business,world,new VillageObservation.Position(0,64,0),kind);
    }
    BusinessStartupGrants.Workshop start(StartupGrantPolicy grants,boolean present,Set<UUID> worlds)throws SQLException {
        return new BusinessStartupGrants(db.getConnection()).startMarkerWorkshopWithSupplies(request,grants,order,starter,worlds,new WorkplaceOperations.Confirmation(anchor,present,1000),1000);
    }
    long cash()throws SQLException{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    IndustryRepository.Store store()throws SQLException{return db.getIndustryRepository().store(business).orElseThrow();}
    long units(Commodity good)throws SQLException{return new CommodityRepository(db.getConnection()).read(town).orElseThrow().state().stocks().get(good).units();}
    void sql(String query)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    Map<String,List<String>> snapshot()throws SQLException {
        var names=new ArrayList<String>();var result=new TreeMap<String,List<String>>();
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")){while(r.next())names.add(r.getString(1));}
        for(var name:names) {
            var rows=new ArrayList<String>();try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM \""+name.replace("\"","\"\"")+"\"")) {
                while(r.next()){var row=new ArrayList<Object>();for(int c=1;c<=r.getMetaData().getColumnCount();c++)row.add(r.getObject(c));rows.add(row.toString());}
            }Collections.sort(rows);result.put(name,rows);
        }return result;
    }
    void opportunity()throws Exception {
        prepare(WorkplaceObservation.Kind.SMOKER);recipe=ProductionRecipe.bakeryBread();
        sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");
        new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(world,0,0,1000,true,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(1,64,0),WorkplaceObservation.Kind.SMOKER))));
    }
    IndustryRuntimePolicy runtime() {
        var original=IndustryPolicy.prototype();var space=new HashMap<>(original.storageSpacePerUnit());space.put(grain,1L);
        var storage=new IndustryPolicy(150,8,32,1024,1,128,original.miningBundle(),space);
        return new IndustryRuntimePolicy(true,1000,8,storage,Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,starter.prices()));
    }
    BakeryInvestment engine(){return engine(BakeryFoundingPolicy.prototype());}
    BakeryInvestment engine(BakeryFoundingPolicy policy) {
        var terms=WorkshopOpportunityPolicy.prototype();var industry=runtime();
        var opportunities=new BakeryOpportunities(db.getConnection(),terms,industry,StartupGrantPolicy.prototype(),starter,VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.prototype());
        return new BakeryInvestment(db.getConnection(),opportunities,policy,terms,industry,StartupGrantPolicy.prototype(),starter,216,73);
    }
    BakeryInvestment.Draft draft()throws Exception{return engine().prepare(town,0,1000,Set.of(world)).draft().orElseThrow();}
    WorkplaceOperations.Confirmation proof(BakeryInvestment.Draft d){return new WorkplaceOperations.Confirmation(d.anchor(),true,1000);}
    void qualified()throws SQLException{db.getPopulationRepository().recordSkillExperience(person,"baking",10);}
    @Test void preparationIsSeededReadOnlyAndNeverGrantsMissingSkills()throws Exception {
        opportunity();var before=snapshot();assertTrue(engine().prepare(town,0,1000,Set.of(world)).draft().isEmpty());assertEquals(before,snapshot());
        qualified();before=snapshot();var proposal=draft();assertEquals(person,proposal.startup().worker());assertEquals(proposal,draft());assertEquals(before,snapshot());
        db.close();open();assertEquals(proposal,draft());assertEquals(before,snapshot());
        assertTrue(engine(BakeryFoundingPolicy.disabled()).prepare(town,0,1000,Set.of(world)).draft().isEmpty());
    }
    @Test void acceptancePaysActualCapitalAndIngredientsOnceThenTimedBreadCanBeSold()throws Exception {
        opportunity();qualified();var proposal=draft();var saved=engine().accept(proposal,proof(proposal),1000,Set.of(world));var id=proposal.anchor().business();
        assertEquals(5800,cash());assertEquals(3700,saved.store().hundredths());assertEquals(3,saved.store().reservedIncomingSpace());assertEquals(7,units(grain));
        var before=snapshot();engine().accept(proposal,proof(proposal),1000,Set.of(world));assertEquals(before,snapshot());
        var live=new IndustryRuntime(db.getConnection(),runtime(),BusinessOperationPolicy.prototype());live.start(1000);live.pulse(2000,Set.of(world));
        assertEquals(3,db.getIndustryRepository().store(id).orElseThrow().units().get(Commodity.FOOD));
        var buyers=new VillageSupplyPurchases(db.getConnection(),VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.prototype());
        buyers.purchase(new VillageSupplyPurchases.Request(UUID.randomUUID(),town,id,Commodity.FOOD,3,1),Set.of(world));
        assertEquals(5200,cash());assertEquals(4300,db.getIndustryRepository().store(id).orElseThrow().hundredths());assertEquals(3,buyers.pending(town,Commodity.FOOD));
        db.close();open();before=snapshot();engine(BakeryFoundingPolicy.disabled()).accept(proposal,proof(proposal),1000,Set.of(world));assertEquals(before,snapshot());
    }
    @Test void acceptanceRechecksFoodCustomerIngredientsAndTreasuryInsteadOfTrustingDraft()throws Exception {
        opportunity();qualified();var d=draft();
        for(var change:List.of("UPDATE economy_stocks SET units=100 WHERE commodity='food'","UPDATE economy_stocks SET units=4 WHERE commodity='grain'","UPDATE treasuries SET hundredths=2500","UPDATE needs_periods SET fulfilled=0")) {
            sql(change);var before=snapshot();assertThrows(SQLException.class,()->engine().accept(d,proof(d),1000,Set.of(world)));assertEquals(before,snapshot());
            sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");sql("UPDATE economy_stocks SET units=10 WHERE commodity='grain'");sql("UPDATE treasuries SET hundredths=10000");sql("UPDATE needs_periods SET fulfilled=demand");
        }
        assertNotNull(engine().accept(d,proof(d),1000,Set.of(world)));
    }
    @Test void lostQualificationsOrActiveStudyPreventStartupWithoutPartialDebits()throws Exception {
        opportunity();qualified();var d=draft();sql("UPDATE resident_skills SET experience=1 WHERE skill='baking'");var before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(d,proof(d),1000,Set.of(world)));assertEquals(before,snapshot());
        qualified();db.getPopulationRepository().updateResident(new Resident(person,"Founder",town,null,20,Resident.Status.ALIVE));before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(d,proof(d),1000,Set.of(world)));assertEquals(before,snapshot());
        db.getPopulationRepository().updateResident(new Resident(person,"Founder",town,null,300,Resident.Status.ALIVE));
        db.getConnection().setAutoCommit(false);try{new ResidentStudy(db.getConnection(),StudyPolicy.prototype(),216).enrollRows(UUID.randomUUID(),person,town,"commerce",1);db.getConnection().commit();}finally{db.getConnection().setAutoCommit(true);}
        before=snapshot();assertThrows(SQLException.class,()->engine().accept(d,proof(d),1000,Set.of(world)));assertEquals(before,snapshot());
    }
    @Test void wrongAbsentOrChangedMarkerRequiresNewPreparation()throws Exception {
        opportunity();qualified();var d=draft();var before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(d,new WorkplaceOperations.Confirmation(d.anchor(),false,1000),1000,Set.of(world)));assertEquals(before,snapshot());
        assertThrows(SQLException.class,()->engine().accept(d,proof(d),1000,Set.of()));assertEquals(before,snapshot());
        new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(world,0,0,1001,true,List.of(new WorkplaceObservation.Marker(d.anchor().position(),WorkplaceObservation.Kind.SMOKER))));
        before=snapshot();assertThrows(SQLException.class,()->engine().accept(d,new WorkplaceOperations.Confirmation(d.anchor(),true,1001),1001,Set.of(world)));assertEquals(before,snapshot());
        var next=engine().prepare(town,0,1001,Set.of(world)).draft().orElseThrow();assertNotNull(engine().accept(next,new WorkplaceOperations.Confirmation(next.anchor(),true,1001),1001,Set.of(world)));
    }
    @Test void forgedTermsAndLastWriteFailureRollBackAllTablesAndRetrySafely()throws Exception {
        opportunity();qualified();var d=draft();var s=d.startup();var bad=new BakeryInvestment.Draft(new BusinessStartupGrants.WorkshopRequest(s.founding(),s.capacity()+1,s.space(),s.capital(),s.worker()),d.supplies(),d.anchor());var before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(bad,proof(d),1000,Set.of(world)));assertEquals(before,snapshot());
        sql("CREATE TRIGGER reject_bakery BEFORE INSERT ON business_workplace_anchors BEGIN SELECT RAISE(ABORT,'fixture failure'); END");before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(d,proof(d),1000,Set.of(world)));assertEquals(before,snapshot());assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER reject_bakery");assertNotNull(engine().accept(d,proof(d),1000,Set.of(world)));
    }
    @Test void acceptedReplayCannotRestoreMissingMarkerOrRefillGoodsAfterRestart()throws Exception {
        opportunity();qualified();var d=draft();engine().accept(d,proof(d),1000,Set.of(world));
        new WorkplaceOperations(db.getConnection()).confirm(List.of(new WorkplaceOperations.Confirmation(d.anchor(),false,1001)),1001,Set.of(world));
        db.close();open();var before=snapshot();engine(BakeryFoundingPolicy.disabled()).accept(d,proof(d),1000,Set.of(world));assertEquals(before,snapshot());
        assertFalse(new BusinessOperations(db.getConnection(),BusinessOperationPolicy.prototype()).evaluate(d.anchor().business()).permitted());
    }
    @Test void preservedHarvestedGrainCanFundAStarterWithoutCopyingItIntoGenericStocks()throws Exception {
        opportunity();qualified();sql("UPDATE economy_stocks SET units=0 WHERE commodity='grain'");sql("INSERT INTO grain_stocks VALUES('"+town+"',10)");
        var d=draft();var before=snapshot();assertEquals(d,draft());assertEquals(before,snapshot());engine().accept(d,proof(d),1000,Set.of(world));
        assertEquals(7,new GrainHarvestRepository(db.getConnection()).stock(town));assertEquals(0,units(grain));
        var store=db.getIndustryRepository().store(d.anchor().business()).orElseThrow();assertEquals(3,store.reservedIncomingSpace());assertEquals(5800,cash());
        db.close();open();before=snapshot();engine().accept(d,proof(d),1000,Set.of(world));assertEquals(before,snapshot());
    }
    @Test void mixedPublicGrainLedgersRetainTheCombinedReserveAndRollbackTogether()throws Exception {
        opportunity();qualified();sql("UPDATE economy_stocks SET units=3 WHERE commodity='grain'");sql("INSERT INTO grain_stocks VALUES('"+town+"',2)");
        var d=draft();sql("CREATE TRIGGER reject_generic_grain BEFORE UPDATE ON economy_stocks WHEN OLD.commodity='grain' BEGIN SELECT RAISE(ABORT,'fixture second-ledger rejection'); END");
        var before=snapshot();assertThrows(SQLException.class,()->engine().accept(d,proof(d),1000,Set.of(world)));assertEquals(before,snapshot());
        sql("DROP TRIGGER reject_generic_grain");engine().accept(d,proof(d),1000,Set.of(world));assertEquals(0,new GrainHarvestRepository(db.getConnection()).stock(town));assertEquals(2,units(grain));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT before_units,after_units FROM workshop_starter_goods WHERE commodity='grain'")){assertTrue(r.next());assertEquals(5,r.getLong(1));assertEquals(2,r.getLong(2));}
    }
    @Test void actualSqlOwnerPreparesAndAcceptsWithoutSharingItsConnection()throws Exception {
        opportunity();qualified();var industry=runtime();db.close();var time=new java.util.concurrent.atomic.AtomicLong(2000);
        var original=com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype();
        var config=new com.blueninjas24.civilizations.application.PopulationSession.Settings(0,original.millisPerMonth(),original.maximumOfflineMonths(),original.monthsPerBatch(),original.population(),original.migration(),original.recognition(),original.enrollment());
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),Logger.getLogger("BakerySqlOwnerTest"),config,time::get,settings,FarmObservation.Policy.prototype())) {
            session.configureIndustryRuntime(industry);session.configureWorkplaces(WorkplaceObserverPolicy.prototype());session.configureBusinessOperations(BusinessOperationPolicy.prototype());
            session.configureVillageSupplies(VillageSupplyPolicy.prototype());session.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),starter);
            session.configureBakery(BakeryFoundingPolicy.prototype(),WorkshopOpportunityPolicy.prototype());session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());
            session.start(false,Set.of(world)).get(10,java.util.concurrent.TimeUnit.SECONDS);
            var proposal=session.prepareBakeryInvestment(town).get(5,java.util.concurrent.TimeUnit.SECONDS).draft().orElseThrow();
            session.acceptBakeryInvestment(proposal,new WorkplaceOperations.Confirmation(proposal.anchor(),true,2000)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(session.currentBusinessAvailability(proposal.anchor().business()).get(5,java.util.concurrent.TimeUnit.SECONDS).permitted());
            assertThrows(IllegalStateException.class,()->session.configureBakery(BakeryFoundingPolicy.disabled(),WorkshopOpportunityPolicy.disabled()));
        }
        open();assertEquals(5800,cash());
    }
}
