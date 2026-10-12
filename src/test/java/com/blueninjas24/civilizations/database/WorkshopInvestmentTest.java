package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
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

class WorkshopInvestmentTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),farmer=new UUID(0,1),prospect=new UUID(0,2);
    final FarmToolUpkeepPolicy upkeep=new FarmToolUpkeepPolicy(true,1,2,50,8);
    final WorkshopStarterPolicy starter=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L,Commodity.COAL,600L));
    final Map<Commodity,Long> sizes=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,Commodity.STONE,1L,ToolUpkeepPolicy.TOOLS,1L);
    EconomySettings settings;IndustryRuntimePolicy industry;
    void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("WorkshopOpportunityTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Workshop prospect fixture","Fixture",0,64,0,2,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var resident:List.of(farmer,prospect))db.getPopulationRepository().insertResident(new Resident(resident,"Fixture resident",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit fixture treasury");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L,Commodity.IRON,10L,Commodity.COAL,10L),
            Map.of(Commodity.FOOD,BigDecimal.valueOf(2),Commodity.IRON,BigDecimal.valueOf(12),Commodity.COAL,BigDecimal.valueOf(6)),bands);
        var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());
        economy.configureFarmTools(upkeep,FarmToolBudgetPolicy.prototype(),IndustryPolicy.prototype());economy.configureWorkLearning(WorkLearningPolicy.prototype());
        economy.observe(new FarmObservation(town,world,1000,true,16,new VillageObservation.Position(0,64,0)),1000);
        economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);economy.pulse(3000,false);
        var mining=new IndustryPolicy(150,8,32,1024,1,128,Map.of(Commodity.COAL,1L,Commodity.STONE,3L),sizes);
        industry=new IndustryRuntimePolicy(true,1000,8,mining,Map.of("smith_tools",ProductionRecipe.smithTools()),new WorkshopSupplyPolicy(true,1,16,starter.prices()));
        markers(3000,true,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(10,64,0),WorkplaceObservation.Kind.SMITHING_TABLE)));
    }
    void markers(long now,boolean complete,List<WorkplaceObservation.Marker> markers)throws SQLException {
        new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(world,0,0,now,complete,markers));
    }
    WorkshopOpportunities review(WorkshopOpportunityPolicy policy,StartupGrantPolicy grants,WorkshopStarterPolicy starter) {
        return new WorkshopOpportunities(db.getConnection(),policy,industry,BusinessOperationPolicy.prototype(),grants,starter,upkeep,FarmToolBudgetPolicy.prototype());
    }
    WorkshopOpportunities.Review review()throws SQLException{return review(WorkshopOpportunityPolicy.prototype(),StartupGrantPolicy.prototype(),starter).review(town,3000,Set.of(world));}
    void sql(String query)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    Map<String,List<String>> snapshot()throws SQLException {
        var names=new ArrayList<String>();var result=new TreeMap<String,List<String>>();
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")){while(r.next())names.add(r.getString(1));}
        for(var name:names) {var rows=new ArrayList<String>();try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM "+name)) {
            int count=r.getMetaData().getColumnCount();while(r.next()){var fields=new ArrayList<String>();for(int n=1;n<=count;n++)fields.add(r.getString(n));rows.add(fields.toString());}
        }Collections.sort(rows);result.put(name,rows);}return result;
    }
    @AfterEach void close(){if(db!=null)db.close();}
    WorkshopInvestment engine(){return engine(WorkshopFoundingPolicy.prototype());}
    WorkshopInvestment engine(WorkshopFoundingPolicy policy){return new WorkshopInvestment(db.getConnection(),review(WorkshopOpportunityPolicy.prototype(),StartupGrantPolicy.prototype(),starter),policy,WorkshopOpportunityPolicy.prototype(),industry,StartupGrantPolicy.prototype(),starter,216,73);}
    void qualified()throws SQLException{db.getPopulationRepository().recordSkillExperience(prospect,"smithing",10);}
    @Test void automaticCycleUsesActualPaidStartupAndDoesNotRepeatAcceptedInvestment()throws Exception {
        qualified();var accepted=new int[1];var pending=new java.util.concurrent.CompletableFuture<Void>();
        var cycle=new com.blueninjas24.civilizations.application.WorkshopStartupCycle(t->{try{return java.util.concurrent.CompletableFuture.completedFuture(engine().prepare(t,0,3000,Set.of(world)));}catch(SQLException e){return java.util.concurrent.CompletableFuture.failedFuture(e);}},
            d->Optional.of(proof(d)),(d,e)->{try{engine().accept(d,e,3000,Set.of(world));accepted[0]++;return pending;}catch(SQLException failure){return java.util.concurrent.CompletableFuture.failedFuture(failure);}});
        cycle.tick(List.of(town));cycle.tick(List.of(town));for(int n=0;n<10;n++)cycle.tick(List.of(town));assertEquals(1,accepted[0]);
        pending.complete(null);cycle.tick(List.of(town));var before=snapshot();for(int n=0;n<10;n++)cycle.tick(List.of(town));
        assertEquals(1,accepted[0]);assertEquals(before,snapshot());assertEquals(28500,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
    }
    @Test void automaticCycleDoesNotSpendForUnknownOrMissingMarkerAndRechecksChangedFunds()throws Exception {
        qualified();var mode=new int[1];var accepted=new int[1];
        var cycle=new com.blueninjas24.civilizations.application.WorkshopStartupCycle(t->{try{return java.util.concurrent.CompletableFuture.completedFuture(engine().prepare(t,0,3000,Set.of(world)));}catch(SQLException e){return java.util.concurrent.CompletableFuture.failedFuture(e);}},
            d->mode[0]==0?Optional.empty():Optional.of(new WorkplaceOperations.Confirmation(d.anchor(),mode[0]>1,3000)),
            (d,e)->{try{accepted[0]++;return java.util.concurrent.CompletableFuture.completedFuture(engine().accept(d,e,3000,Set.of(world)));}catch(SQLException failure){return java.util.concurrent.CompletableFuture.failedFuture(failure);}});
        var before=snapshot();cycle.tick(List.of(town));cycle.tick(List.of(town));assertEquals(before,snapshot());assertEquals(0,accepted[0]);
        mode[0]=1;cycle.tick(List.of(town));cycle.tick(List.of(town));assertEquals(before,snapshot());assertEquals(0,accepted[0]);
        mode[0]=2;cycle.tick(List.of(town));sql("UPDATE treasuries SET hundredths=2500");before=snapshot();cycle.tick(List.of(town));cycle.tick(List.of(town));
        assertEquals(before,snapshot());assertEquals(1,accepted[0]);assertTrue(cycle.diagnostic().contains("deferred"));
        sql("UPDATE treasuries SET hundredths=30000");cycle.tick(List.of(town));cycle.tick(List.of(town));cycle.tick(List.of(town));assertEquals(2,accepted[0]);
        assertEquals(28500,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
    }
    WorkshopInvestment.Draft draft()throws Exception{return engine().prepare(town,0,3000,Set.of(world)).draft().orElseThrow();}
    WorkplaceOperations.Confirmation proof(WorkshopInvestment.Draft draft){return new WorkplaceOperations.Confirmation(draft.anchor(),true,3000);}
    @Test void unqualifiedAdultsNeverReceiveFreeExperienceAndPreparationCreatesNoAssets()throws Exception {
        var before=snapshot();var empty=engine().prepare(town,0,3000,Set.of(world));assertTrue(empty.draft().isEmpty());assertTrue(empty.explanation().contains("qualified"));assertEquals(before,snapshot());
        qualified();before=snapshot();var proposal=draft();assertEquals(prospect,proposal.startup().worker());assertEquals(proposal,draft());assertEquals(before,snapshot());
        assertTrue(engine(WorkshopFoundingPolicy.disabled()).prepare(town,0,3000,Set.of(world)).draft().isEmpty());
    }
    @Test void qualifiedFundedInvestmentConservesMoneyGoodsAndKeepsItsMarkerBinding()throws Exception {
        qualified();var proposal=draft();var started=engine().accept(proposal,proof(proposal),3000,Set.of(world));
        assertEquals(1000,started.store().hundredths());assertEquals(3,started.store().reservedIncomingSpace());
        assertEquals(28500,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
        assertEquals(18,new CommodityRepository(db.getConnection()).read(town).orElseThrow().state().stocks().get(Commodity.IRON).units());
        assertEquals(10,db.getPopulationRepository().findWorkProfile(prospect).orElseThrow().experience().get("smithing"));
        assertEquals(List.of(proposal.anchor()),new WorkplaceOperations(db.getConnection()).anchors(null,64));
        assertTrue(engine().prepare(town,0,3000,Set.of(world)).draft().isEmpty());
    }
    @Test void acceptanceRechecksFoodFundsAndSurplusInsteadOfTrustingOldProposal()throws Exception {
        qualified();var proposal=draft();sql("UPDATE needs_periods SET fulfilled=0 WHERE period=2");var before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,proof(proposal),3000,Set.of(world)));assertEquals(before,snapshot());
        sql("UPDATE needs_periods SET fulfilled=demand WHERE period=2");sql("UPDATE economy_stocks SET units=3 WHERE commodity='iron'");before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,proof(proposal),3000,Set.of(world)));assertEquals(before,snapshot());
        sql("UPDATE economy_stocks SET units=20 WHERE commodity='iron'");sql("UPDATE treasuries SET hundredths=2500");before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,proof(proposal),3000,Set.of(world)));assertEquals(before,snapshot());
    }
    @Test void changedQualificationEmploymentOrActiveStudyCannotBypassFirstOwnerChecks()throws Exception {
        qualified();var proposal=draft();db.getPopulationRepository().updateResident(new Resident(prospect,"Fixture resident",town,null,20,Resident.Status.ALIVE));var before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,proof(proposal),3000,Set.of(world)));assertEquals(before,snapshot());
        db.getPopulationRepository().updateResident(new Resident(prospect,"Fixture resident",town,null,300,Resident.Status.ALIVE));
        sql("UPDATE resident_skills SET experience=1 WHERE skill='smithing'");before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,proof(proposal),3000,Set.of(world)));assertEquals(before,snapshot());
        qualified();UUID course=UUID.randomUUID();db.getConnection().setAutoCommit(false);
        new ResidentStudy(db.getConnection(),new StudyPolicy(true,1,100,1,2,1,0,Map.of("commerce",new StudyPolicy.Course(10,Map.of(Commodity.IRON,1L)))),216).enrollRows(course,prospect,town,"commerce",2);
        db.getConnection().commit();db.getConnection().setAutoCommit(true);before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,proof(proposal),3000,Set.of(world)));assertEquals(before,snapshot());
        sql("UPDATE resident_courses SET status='CANCELLED' WHERE id='"+course+"'");
        qualified();var site=new BusinessSite(UUID.randomUUID(),town,world,20,64,0,3000,"Explicit second employer fixture");var req=new JobRequirements(216,Map.of());
        UUID employer=UUID.randomUUID();db.getBusinessRepository().found(new BusinessFounding(new Business(employer,town,"Other employer",prospect,prospect,"warehouse",site.id(),1,0),site,req,req,0,"Explicit fixture"));
        db.getIndustryRepository().openStore(employer,16,Map.of(ToolUpkeepPolicy.TOOLS,1L),0);db.getBusinessRepository().hire(UUID.randomUUID(),employer,prospect,0);before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,proof(proposal),3000,Set.of(world)));assertEquals(before,snapshot());
    }
    @Test void refreshedRemovedWrongOrUnloadedMarkerRequiresNewValidPreparation()throws Exception {
        qualified();var proposal=draft();var before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,new WorkplaceOperations.Confirmation(proposal.anchor(),false,3000),3000,Set.of(world)));assertEquals(before,snapshot());
        assertThrows(SQLException.class,()->engine().accept(proposal,proof(proposal),3000,Set.of()));assertEquals(before,snapshot());
        markers(3001,true,List.of(new WorkplaceObservation.Marker(proposal.anchor().position(),WorkplaceObservation.Kind.SMITHING_TABLE)));before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,new WorkplaceOperations.Confirmation(proposal.anchor(),true,3001),3001,Set.of(world)));assertEquals(before,snapshot());
        var next=engine().prepare(town,0,3001,Set.of(world)).draft().orElseThrow();engine().accept(next,new WorkplaceOperations.Confirmation(next.anchor(),true,3001),3001,Set.of(world));
    }
    @Test void changedProposalTermsAndLateSqlFailureRollbackEverything()throws Exception {
        qualified();var proposal=draft();var old=proposal.startup();var changed=new BusinessStartupGrants.WorkshopRequest(old.founding(),old.capacity()+1,old.space(),old.capital(),old.worker());
        var bad=new WorkshopInvestment.Draft(changed,proposal.supplies(),proposal.anchor());var before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(bad,proof(proposal),3000,Set.of(world)));assertEquals(before,snapshot());
        sql("CREATE TRIGGER reject_workplace BEFORE INSERT ON business_workplace_anchors BEGIN SELECT RAISE(ABORT,'fixture rejection'); END");before=snapshot();
        assertThrows(SQLException.class,()->engine().accept(proposal,proof(proposal),3000,Set.of(world)));assertEquals(before,snapshot());assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER reject_workplace");engine().accept(proposal,proof(proposal),3000,Set.of(world));
    }
    @Test void acceptedReplayAfterRestartCannotChargeAgainOrClearConfirmedPause()throws Exception {
        qualified();var proposal=draft();engine().accept(proposal,proof(proposal),3000,Set.of(world));
        new WorkplaceOperations(db.getConnection()).confirm(List.of(new WorkplaceOperations.Confirmation(proposal.anchor(),false,3001)),3001,Set.of(world));db.close();open();var before=snapshot();
        engine(WorkshopFoundingPolicy.disabled()).accept(proposal,proof(proposal),3000,Set.of(world));assertEquals(before,snapshot());
        assertFalse(new BusinessOperations(db.getConnection(),BusinessOperationPolicy.prototype()).evaluate(proposal.anchor().business()).permitted());
    }
    @Test void qualifiedPageDoesNotStarveBehindUnqualifiedResidentsAndSeededChoiceIsStable()throws Exception {
        qualified();for(int i=3;i<90;i++)db.getPopulationRepository().insertResident(new Resident(new UUID(0,i),"Unqualified fixture",town,null,300,Resident.Status.ALIVE));
        UUID last=new UUID(1,1);db.getPopulationRepository().insertResident(new Resident(last,"Qualified last fixture",town,null,300,Resident.Status.ALIVE));
        sql("UPDATE resident_skills SET experience=0 WHERE resident_id='"+prospect+"' AND skill='smithing'");db.getPopulationRepository().recordSkillExperience(last,"smithing",10);
        // Explicit supplied/funded population fixture isolates bounded qualified-page behavior.
        sql("UPDATE needs_periods SET unassigned_residents=90,demand=90,fulfilled=90 WHERE period=2");sql("UPDATE economy_stocks SET units=1000 WHERE commodity='food'");sql("UPDATE treasuries SET hundredths=100000");
        var bounded=new WorkshopFoundingPolicy(true,1,216,10,1,2);var proposal=engine(bounded).prepare(town,0,3000,Set.of(world));
        assertTrue(proposal.draft().isPresent(),proposal.explanation());assertEquals(last,proposal.draft().orElseThrow().startup().worker());
        assertEquals(proposal,engine(bounded).prepare(town,0,3000,Set.of(world)));
    }
    @Test void acceptedFirstWorkshopProducesAndSellsARealToolkitToItsWorkingFarmCustomer()throws Exception {
        qualified();var proposal=draft();engine().accept(proposal,proof(proposal),3000,Set.of(world));
        var runtime=new IndustryRuntime(db.getConnection(),industry,BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());runtime.start(3000);runtime.pulse(4000,Set.of(world));
        UUID smith=proposal.anchor().business();assertEquals(1,db.getIndustryRepository().store(smith).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS));
        var order=new FarmToolPurchases.Request(UUID.randomUUID(),FarmEnterprises.businessId(town),smith,2,1,128);
        new FarmToolPurchases(db.getConnection(),BusinessOperationPolicy.prototype()).purchase(order,FarmToolBudgetPolicy.prototype(),Set.of(world));
        assertEquals(5000,db.getIndustryRepository().store(smith).orElseThrow().hundredths());assertEquals(0,db.getIndustryRepository().store(smith).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS));
        assertEquals(24500,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
        assertEquals(1,db.getIndustryRepository().store(FarmEnterprises.businessId(town)).orElseThrow().reservedIncomingSpace());
        assertEquals(1,db.getIndustryRepository().recipeRun(UUID.nameUUIDFromBytes(("civilizations:timed-industry-v1:"+smith+":1").getBytes(java.nio.charset.StandardCharsets.UTF_8))).orElseThrow().produced().get(ToolUpkeepPolicy.TOOLS));
    }
    @Test void actualSqlOwnerPreparesAndAcceptsWithFreshProofAndRealMoney()throws Exception {
        qualified();db.close();var time=new java.util.concurrent.atomic.AtomicLong(3000);var original=PopulationSession.Settings.prototype();
        var config=new PopulationSession.Settings(0,original.millisPerMonth(),original.maximumOfflineMonths(),original.monthsPerBatch(),original.population(),original.migration(),original.recognition(),original.enrollment());
        try(var session=new PopulationSession(directory.toFile(),Logger.getLogger("InvestmentTest"),config,time::get,settings,FarmObservation.Policy.prototype())) {
            session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureFarmTools(upkeep,FarmToolBudgetPolicy.prototype());session.configureWorkLearning(WorkLearningPolicy.prototype());
            session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureIndustryRuntime(industry);session.configureWorkplaces(WorkplaceObserverPolicy.prototype());
            session.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),starter);session.configureWorkshopFounding(WorkshopFoundingPolicy.prototype());session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());
            session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);var proposal=session.prepareWorkshopInvestment(town).get(5,java.util.concurrent.TimeUnit.SECONDS).draft().orElseThrow();
            session.acceptWorkshopInvestment(proposal,proof(proposal)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(session.currentBusinessAvailability(proposal.anchor().business()).get(5,java.util.concurrent.TimeUnit.SECONDS).permitted());
        }
        open();assertEquals(28500,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
    }
}
