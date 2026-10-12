package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WarehouseInvestmentTest {
    @TempDir Path directory;WarehouseOpportunitiesTest chain;WorkshopInvestmentTest fixture;
    final UUID merchant=new UUID(0,3);
    @BeforeEach void setup()throws Exception {
        chain=new WarehouseOpportunitiesTest();chain.directory=directory;chain.initialize(false);fixture=chain.fixture;
        fixture.db.getPopulationRepository().insertResident(new Resident(merchant,"Explicit merchant fixture",fixture.town,null,300,Resident.Status.ALIVE));
        // The normal food period assesses and feeds the additional explicit fixture adult.
        chain.advanceFood();
    }
    @AfterEach void close(){if(fixture!=null)fixture.close();}
    WarehouseInvestment engine(){return engine(WarehouseFoundingPolicy.prototype());}
    WarehouseInvestment engine(WarehouseFoundingPolicy p){return new WarehouseInvestment(fixture.db.getConnection(),chain.engine(WarehouseOpportunityPolicy.prototype()),p,
            WarehouseOpportunityPolicy.prototype(),chain.industry,StartupGrantPolicy.prototype(),216,73);}
    void qualify()throws SQLException{fixture.db.getPopulationRepository().recordSkillExperience(merchant,"commerce",10);}
    WarehouseInvestment.Draft draft()throws Exception{return engine().prepare(fixture.town,0,4000,Set.of(fixture.world)).draft().orElseThrow();}
    WorkplaceOperations.Confirmation proof(WarehouseInvestment.Draft d){return new WorkplaceOperations.Confirmation(d.anchor(),true,4000);}
    @Test void preparationIsReadonlyDeterministicAndRequiresEarnedCommerce()throws Exception {
        var before=fixture.snapshot();var empty=engine().prepare(fixture.town,0,4000,Set.of(fixture.world));assertTrue(empty.draft().isEmpty());assertTrue(empty.explanation().contains("qualified"));assertEquals(before,fixture.snapshot());
        qualify();before=fixture.snapshot();var d=draft();assertEquals(merchant,d.startup().worker());assertEquals(d,draft());assertEquals(before,fixture.snapshot());
        assertTrue(engine(WarehouseFoundingPolicy.disabled()).prepare(fixture.town,0,4000,Set.of(fixture.world)).draft().isEmpty());
    }
    @Test void qualifiedAcceptanceCreatesEmptyFundedStoreJobAndFixedBarrelWithoutFreeGoods()throws Exception {
        qualify();var d=draft();var before=fixture.snapshot();var started=engine().accept(d,proof(d),4000,Set.of(fixture.world));
        assertEquals(4000,started.store().hundredths());assertTrue(started.store().units().values().stream().allMatch(n->n==0));assertEquals(0,started.store().reservedIncomingSpace());
        assertEquals(20000,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());
        assertEquals(merchant,started.worker().residentId());assertEquals(10,fixture.db.getPopulationRepository().findWorkProfile(merchant).orElseThrow().experience().get("commerce"));
        assertTrue(new WorkplaceOperations(fixture.db.getConnection()).anchors(null,64).contains(d.anchor()));
        var after=fixture.snapshot();for(var name:List.of("economy_stocks","mineral_deposits","mineral_yields","mineral_observations","ore_placement_exclusions"))if(before.containsKey(name))assertEquals(before.get(name),after.get(name),name);
        assertTrue(engine().prepare(fixture.town,0,4000,Set.of(fixture.world)).draft().isEmpty());
    }
    @Test void acceptanceRechecksActualCustomerFoodTreasuryAndFiniteSources()throws Exception {
        qualify();var d=draft();
        fixture.sql("UPDATE industry_stores SET hundredths=1000 WHERE hundredths=5000");declinedUnchanged(d);fixture.sql("UPDATE industry_stores SET hundredths=5000 WHERE hundredths=1000");
        fixture.sql("UPDATE needs_periods SET fulfilled=0 WHERE period=3");declinedUnchanged(d);fixture.sql("UPDATE needs_periods SET fulfilled=demand WHERE period=3");
        fixture.sql("UPDATE treasuries SET hundredths=10000");declinedUnchanged(d);fixture.sql("UPDATE treasuries SET hundredths=24500");
        chain.healthy=false;declinedUnchanged(d);chain.healthy=true;
        new OrePlacementRepository(fixture.db.getConnection()).record(fixture.world,List.of(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(20,64,0)),4000);declinedUnchanged(d);
    }
    void declinedUnchanged(WarehouseInvestment.Draft d)throws Exception {
        var before=fixture.snapshot();assertThrows(SQLException.class,()->engine().accept(d,proof(d),4000,Set.of(fixture.world)));assertEquals(before,fixture.snapshot());assertTrue(fixture.db.getConnection().getAutoCommit());
    }
    @Test void changedAgeSkillsOrLivingStatusCannotBypassQualifications()throws Exception {
        qualify();var d=draft();fixture.db.getPopulationRepository().updateResident(new Resident(merchant,"Explicit merchant fixture",fixture.town,null,20,Resident.Status.ALIVE));declinedUnchanged(d);
        fixture.db.getPopulationRepository().updateResident(new Resident(merchant,"Explicit merchant fixture",fixture.town,null,300,Resident.Status.ALIVE));fixture.sql("UPDATE resident_skills SET experience=1 WHERE skill='commerce'");declinedUnchanged(d);
        qualify();fixture.db.getPopulationRepository().updateResident(new Resident(merchant,"Explicit merchant fixture",fixture.town,null,300,Resident.Status.DECEASED));declinedUnchanged(d);
    }
    @Test void activeCourseOrOtherWorkerJobDefersMerchantAndRejectsOldDraft()throws Exception {
        qualify();var d=draft();UUID course=UUID.randomUUID();var c=fixture.db.getConnection();c.setAutoCommit(false);
        new ResidentStudy(c,new StudyPolicy(true,1,100,1,2,1,0,Map.of("commerce",new StudyPolicy.Course(20,Map.of(Commodity.IRON,1L)))),216).enrollRows(course,merchant,fixture.town,"commerce",3);c.commit();c.setAutoCommit(true);
        declinedUnchanged(d);assertTrue(engine().prepare(fixture.town,0,4000,Set.of(fixture.world)).draft().isEmpty());fixture.sql("UPDATE resident_courses SET status='CANCELLED'");
        var smith=fixture.db.getBusinessRepository().findBusinesses(fixture.town).stream().filter(b->b.industry().equals("blacksmith")).findFirst().orElseThrow();
        fixture.db.getPopulationRepository().recordSkillExperience(merchant,"smithing",1);fixture.db.getBusinessRepository().hire(UUID.randomUUID(),smith.id(),merchant,0);declinedUnchanged(d);
    }
    @Test void staleMissingWrongUnloadedOrEarlierPointProofNeverSpends()throws Exception {
        qualify();var d=draft();var before=fixture.snapshot();
        for(var proof:List.of(new WorkplaceOperations.Confirmation(d.anchor(),false,4000),new WorkplaceOperations.Confirmation(d.anchor(),true,3999),
                new WorkplaceOperations.Confirmation(d.anchor(),true,4001),new WorkplaceOperations.Confirmation(new WorkplaceOperations.Anchor(d.anchor().business(),d.anchor().world(),d.anchor().position(),WorkplaceObservation.Kind.SMITHING_TABLE),true,4000))) {
            assertThrows(SQLException.class,()->engine().accept(d,proof,4000,Set.of(fixture.world)));assertEquals(before,fixture.snapshot());
        }
        assertThrows(SQLException.class,()->engine().accept(d,proof(d),4000,Set.of()));assertEquals(before,fixture.snapshot());
        assertThrows(SQLException.class,()->engine().accept(d,proof(d),64001,Set.of(fixture.world)));assertEquals(before,fixture.snapshot());
    }
    @Test void refreshedMarkerRequiresNewProposalAndValidCurrentPointProof()throws Exception {
        qualify();var d=draft();chain.markers(4001,List.of(new WorkplaceObservation.Marker(d.anchor().position(),WorkplaceObservation.Kind.BARREL)));var before=fixture.snapshot();
        assertThrows(SQLException.class,()->engine().accept(d,new WorkplaceOperations.Confirmation(d.anchor(),true,4001),4001,Set.of(fixture.world)));assertEquals(before,fixture.snapshot());
        var next=engine().prepare(fixture.town,0,4001,Set.of(fixture.world)).draft().orElseThrow();assertEquals(d.anchor().business(),next.anchor().business());
        engine().accept(next,new WorkplaceOperations.Confirmation(next.anchor(),true,4001),4001,Set.of(fixture.world));
    }
    @Test void editedTermsAndLateBindingFailureRollbackAllAssetsAndCanRetry()throws Exception {
        qualify();var d=draft();var old=d.startup();var bad=new WarehouseInvestment.Draft(new BusinessStartupGrants.WarehouseRequest(old.founding(),old.capacity()+1,old.space(),old.capital(),old.worker()),d.anchor());declinedUnchanged(bad);
        fixture.sql("CREATE TRIGGER reject_warehouse BEFORE INSERT ON business_workplace_anchors WHEN NEW.kind='BARREL' BEGIN SELECT RAISE(ABORT,'fixture rejection'); END");declinedUnchanged(d);
        fixture.sql("DROP TRIGGER reject_warehouse");engine().accept(d,proof(d),4000,Set.of(fixture.world));assertEquals(20000,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());
    }
    @Test void restartReplayDoesNotChargeTwiceOrRestorePausedMarkerAndRejectsEditedStore()throws Exception {
        qualify();var d=draft();engine().accept(d,proof(d),4000,Set.of(fixture.world));new WorkplaceOperations(fixture.db.getConnection()).confirm(List.of(new WorkplaceOperations.Confirmation(d.anchor(),false,4001)),4001,Set.of(fixture.world));
        fixture.close();fixture.open();var before=fixture.snapshot();engine(WarehouseFoundingPolicy.disabled()).accept(d,proof(d),4000,Set.of(fixture.world));assertEquals(before,fixture.snapshot());
        assertFalse(new BusinessOperations(fixture.db.getConnection(),BusinessOperationPolicy.prototype()).evaluate(d.anchor().business()).permitted());
        var old=d.startup();var changed=new WarehouseInvestment.Draft(new BusinessStartupGrants.WarehouseRequest(old.founding(),old.capacity(),old.space(),old.capital()+1,old.worker()),d.anchor());declinedUnchanged(changed);
    }
    @Test void sqlOwnerPreparesAndAcceptsAndOldConfigurationOptsOut()throws Exception {
        qualify();fixture.db.close();var original=PopulationSession.Settings.prototype();var settings=new PopulationSession.Settings(0,original.millisPerMonth(),original.maximumOfflineMonths(),original.monthsPerBatch(),original.population(),original.migration(),original.recognition(),original.enrollment());
        try(var session=new PopulationSession(directory.toFile(),Logger.getLogger("WarehouseInvestmentTest"),settings,()->4000,fixture.settings,FarmObservation.Policy.prototype())) {
            session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureFarmTools(fixture.upkeep,FarmToolBudgetPolicy.prototype());session.configureWorkLearning(WorkLearningPolicy.prototype());session.configureBusinessOperations(BusinessOperationPolicy.prototype());
            session.configureIndustryRuntime(chain.industry);session.configureWorkplaces(WorkplaceObserverPolicy.prototype());session.configureMineFounding(MineFoundingPolicy.prototype(),StartupGrantPolicy.prototype());session.configureWarehouseOpportunities(WarehouseOpportunityPolicy.prototype());
            session.start(false,Set.of(fixture.world)).get(5,TimeUnit.SECONDS);assertTrue(session.prepareWarehouseInvestment(fixture.town).get(5,TimeUnit.SECONDS).draft().isEmpty());
        }
        try(var session=new PopulationSession(directory.toFile(),Logger.getLogger("WarehouseInvestmentTest"),settings,()->4000,fixture.settings,FarmObservation.Policy.prototype())) {
            session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureFarmTools(fixture.upkeep,FarmToolBudgetPolicy.prototype());session.configureWorkLearning(WorkLearningPolicy.prototype());session.configureBusinessOperations(BusinessOperationPolicy.prototype());
            session.configureIndustryRuntime(chain.industry);session.configureWorkplaces(WorkplaceObserverPolicy.prototype());session.configureMineFounding(MineFoundingPolicy.prototype(),StartupGrantPolicy.prototype());session.configureWarehouseOpportunities(WarehouseOpportunityPolicy.prototype());session.configureWarehouseFounding(WarehouseFoundingPolicy.prototype());
            session.start(false,Set.of(fixture.world)).get(5,TimeUnit.SECONDS);var d=session.prepareWarehouseInvestment(fixture.town).get(5,TimeUnit.SECONDS).draft().orElseThrow();session.acceptWarehouseInvestment(d,proof(d)).get(5,TimeUnit.SECONDS);
            assertTrue(session.currentBusinessAvailability(d.anchor().business()).get(5,TimeUnit.SECONDS).permitted());
        }
        fixture.open();assertEquals(20000,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());
    }
    @Test void unlimitedOwnershipAllowsQualifiedOwnerWithNoOtherWorkerJob()throws Exception {
        qualify();var site=new BusinessSite(UUID.randomUUID(),fixture.town,fixture.world,9,64,0,4000,"Explicit other owned site");var req=new JobRequirements(216,Map.of());
        fixture.db.getBusinessRepository().found(new BusinessFounding(new Business(UUID.randomUUID(),fixture.town,"Other owned firm",merchant,merchant,"blacksmith",site.id(),1,0),site,req,req,0,"Explicit ownership fixture"));
        var d=draft();assertEquals(merchant,d.startup().founding().business().ownerId());engine().accept(d,proof(d),4000,Set.of(fixture.world));
        assertEquals(2,fixture.db.getBusinessRepository().findBusinesses(fixture.town).stream().filter(b->b.ownerId().equals(merchant)).count());
    }
    @Test void acceptedLegacyWarehouseCannotAcquireANewMarkerRequirementViaReplay()throws Exception {
        qualify();var d=draft();new BusinessStartupGrants(fixture.db.getConnection()).startWarehouse(d.startup(),StartupGrantPolicy.prototype());var before=fixture.snapshot();
        assertThrows(SQLException.class,()->engine().accept(d,proof(d),4000,Set.of(fixture.world)));assertEquals(before,fixture.snapshot());
        assertFalse(new WorkplaceOperations(fixture.db.getConnection()).anchors(null,64).contains(d.anchor()));
    }
}
