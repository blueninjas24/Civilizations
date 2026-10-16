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

class FisheryPersistenceTest {
    @TempDir Path directory;
    BusinessStartupGrantsTest fixture;
    UUID business,site;
    FishingPolicy policy=FishingPolicy.prototype();
    @BeforeEach void setup() throws Exception {
        fixture=new BusinessStartupGrantsTest();fixture.directory=directory;fixture.setup();
        fixture.db.getPopulationRepository().recordSkillExperience(fixture.person,"fishing",10);
        var original=fixture.mine(fixture.person,0).founding();var b=original.business();site=b.siteId();business=b.id();
        var requirements=new JobRequirements(216,Map.of("fishing",1L));
        var firm=new Business(b.id(),b.settlementId(),"Fixture fishery",b.founderId(),b.ownerId(),"fishing",b.siteId(),2,0);
        fixture.grants().startWorkshop(new BusinessStartupGrants.WorkshopRequest(
                new BusinessFounding(firm,original.site(),requirements,requirements,500,"Explicit suitable fishery fixture"),
                100,Map.of(Commodity.FOOD,1L),1000,fixture.person),StartupGrantPolicy.prototype());
        fisheries().open(site,observation(fixture.world,0),policy);
    }
    @AfterEach void close(){fixture.close();}
    FisheryObservation observation(UUID world,int offset){return new FisheryObservation(world,10,true,
            java.util.stream.IntStream.range(0,40).mapToObj(x->new FisheryObservation.Column(x+offset,0)).toList());}
    FisheryRepository fisheries(){return new FisheryRepository(fixture.db.getConnection());}
    IndustryRepository industry()throws SQLException{return fixture.db.getIndustryRepository();}
    FishStock stock()throws Exception{return fisheries().find(site).orElseThrow().stock();}
    long food()throws Exception{return industry().store(business).orElseThrow().units().get(Commodity.FOOD);}
    IndustryRuntime runtime(long now)throws Exception{
        var p=new IndustryRuntimePolicy(true,1000,1,IndustryPolicy.prototype(),Map.of());
        var runtime=new IndustryRuntime(fixture.db.getConnection(),p);runtime.configureFishing(policy);runtime.start(now);return runtime;
    }
    FisheryHabitatObservation habitat(long time,int suitable) {
        return new FisheryHabitatObservation(fixture.world,time,true,java.util.stream.IntStream.range(0,40)
                .mapToObj(x->new FisheryHabitatObservation.Support(new FisheryObservation.Column(x,0),
                        x<suitable?FisheryHabitatObservation.State.SUITABLE:FisheryHabitatObservation.State.ABSENT)).toList());
    }
    @Test void confirmedLossRestorationRestartAndReplayPreserveOriginalTermsAndOwnedGoods()throws Exception {
        industry().fish(UUID.randomUUID(),business,100,1,policy);var original=fisheries().find(site).orElseThrow();
        var op=UUID.randomUUID();var evidence=habitat(20,20);var change=fisheries().habitat(op,site,evidence);
        assertEquals(82,change.lost());assertEquals(new FishStock(160,160,0),stock());assertEquals(4,food());
        fixture.db.close();fixture.open();assertEquals(change,fisheries().habitat(op,site,evidence));
        assertThrows(SQLException.class,()->fisheries().habitat(op,site,habitat(20,40)));
        fisheries().habitat(UUID.randomUUID(),site,habitat(30,40));assertEquals(new FishStock(320,160,0),stock());
        assertEquals(change,fisheries().habitat(op,site,evidence));assertEquals(new FishStock(320,160,0),stock());
        assertEquals(40,fisheries().columns(site).size());assertEquals(original.openingTerms(),fisheries().find(site).orElseThrow().openingTerms());
        assertEquals(2,fixture.count("fishery_habitat_changes"));assertEquals(4,food());assertEquals(8500,fixture.cash());
    }
    @Test void unknownIncompleteWrongWorldStaleAndMissingSupportsCannotDestroyFish()throws Exception {
        var before=fixture.snapshot();var valid=habitat(20,0);
        var unknown=new ArrayList<>(valid.supports());unknown.set(0,new FisheryHabitatObservation.Support(unknown.getFirst().column(),FisheryHabitatObservation.State.UNKNOWN));
        for(var bad:List.of(new FisheryHabitatObservation(fixture.world,20,false,valid.supports()),
                new FisheryHabitatObservation(fixture.world,20,true,unknown),
                new FisheryHabitatObservation(UUID.randomUUID(),20,true,valid.supports()),habitat(10,0),
                new FisheryHabitatObservation(fixture.world,20,true,valid.supports().subList(0,39)))) {
            assertThrows(SQLException.class,()->fisheries().habitat(UUID.randomUUID(),site,bad));assertEquals(before,fixture.snapshot());
        }
        var shifted=new ArrayList<>(valid.supports());shifted.set(0,new FisheryHabitatObservation.Support(new FisheryObservation.Column(-1,0),FisheryHabitatObservation.State.ABSENT));
        assertThrows(SQLException.class,()->fisheries().habitat(UUID.randomUUID(),site,new FisheryHabitatObservation(fixture.world,20,true,shifted)));
        assertEquals(before,fixture.snapshot());
    }
    @Test void dryHabitatNormalClockCannotCatchOrRecoverAndRestorationRecoversGradually()throws Exception {
        fisheries().habitat(UUID.randomUUID(),site,habitat(20,0));var runtime=runtime(1000);
        for(int i=2;i<=8;i++)assertEquals(1,runtime.pulse(i*1000L,Set.of(fixture.world)));
        assertEquals(new FishStock(0,0,0),stock());assertEquals(0,food());
        fixture.db.close();fixture.open();assertEquals(new FishStock(0,0,0),stock());
        fisheries().habitat(UUID.randomUUID(),site,habitat(30,40));assertEquals(new FishStock(320,0,0),stock());
        assertEquals(4,industry().fish(UUID.randomUUID(),business,100,8,policy).quantity());assertEquals(new FishStock(320,2,4000),stock());
    }
    @Test void lateHabitatAuditFailureRollsBackAllStateThenExactRetrySucceeds()throws Exception {
        fixture.sql("CREATE TRIGGER reject_habitat BEFORE INSERT ON fishery_habitat_changes BEGIN SELECT RAISE(ABORT,'late fixture failure'); END");
        var before=fixture.snapshot();var op=UUID.randomUUID();var evidence=habitat(20,0);
        assertThrows(SQLException.class,()->fisheries().habitat(op,site,evidence));assertEquals(before,fixture.snapshot());
        assertTrue(fixture.db.getConnection().getAutoCommit());fixture.sql("DROP TRIGGER reject_habitat");
        assertEquals(240,fisheries().habitat(op,site,evidence).lost());assertEquals(1,fixture.count("fishery_habitat_changes"));
    }
    @Test void reducedCapacityWithStockBelowLimitLosesNothingAndRetunesCannotRestoreIt()throws Exception {
        fixture.sql("UPDATE fishery_resources SET available=10,remainder=4000");
        assertEquals(0,fisheries().habitat(UUID.randomUUID(),site,habitat(20,20)).lost());assertEquals(new FishStock(160,10,4000),stock());
        var retuned=new FishingPolicy(true,1,100,10000,100,200,4,16);
        assertEquals(stock(),fisheries().open(site,observation(fixture.world,0),retuned).stock());
        industry().fish(UUID.randomUUID(),business,0,1,retuned);assertEquals(new FishStock(160,13,6000),stock());
        assertThrows(SQLException.class,()->fixture.sql("UPDATE fishery_resources SET available=161"));
        assertThrows(SQLException.class,()->fixture.sql("UPDATE fishery_resources SET habitat_capacity=321"));
    }
    @Test void migration67FailureRollsBackColumnAndRetryPreservesStockIdentityAndHistory()throws Exception {
        industry().fish(UUID.randomUUID(),business,100,1,policy);var stock=stock();var columns=fisheries().columns(site);
        SchemaFixtures.rewind(fixture.db.getConnection(),66);fixture.sql("PRAGMA user_version=66");var before=fixture.snapshot();
        fixture.sql("CREATE VIEW fishery_habitat_changes AS SELECT 1");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(fixture.db.getConnection()));
        assertEquals(before,fixture.snapshot());assertTrue(fixture.db.getConnection().getAutoCommit());
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM fishery_resources")) {
            assertEquals(7,r.getMetaData().getColumnCount());
        }
        fixture.sql("DROP VIEW fishery_habitat_changes");new SchemaMigrator().migrate(fixture.db.getConnection());
        var after=fixture.snapshot();for(var added:List.of("livestock_capture","livestock_animal_losses","craftsman_founder_courses","craftsman_founder_reviews","business_storage_upgrades","bakery_founder_courses","bakery_founder_reviews","bakery_private_starters","livestock_herds","livestock_initial_animals","livestock_periods","livestock_young"))assertEquals(List.of(),after.remove(added));assertEquals(List.of(),after.remove("protected_pen_animals"));assertEquals(List.of(),after.remove("protected_pen_columns"));assertEquals(List.of(),after.remove("protected_pens"));assertEquals(List.of(),after.remove("animal_ownership"));assertEquals(List.of(),after.remove("farm_market_harvests"));assertEquals(List.of(),after.remove("farm_market_stores"));assertEquals(List.of(),after.remove("grain_harvests"));assertEquals(List.of(),after.remove("grain_stocks"));assertEquals(List.of(),after.remove("farm_grain_evidence"));assertEquals(List.of(),after.remove("fishery_founder_courses"));assertEquals(List.of(),after.remove("fishery_workplace_history"));assertEquals(List.of(),after.remove("fishery_workplaces"));assertEquals(List.of(),after.remove("water_columns"));assertEquals(List.of(),after.remove("water_surveys"));assertEquals(List.of(),after.remove("fishery_habitat_changes"));
        after.remove("fishery_resources");var originalRows=before.remove("fishery_resources");assertEquals(before,after);
        var preservedRows=new ArrayList<String>();
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM fishery_resources")) {
            while(r.next()) {
                var row=new ArrayList<Object>();for(int c=1;c<=7;c++)row.add(r.getObject(c));preservedRows.add(row.toString());
                assertNull(r.getObject("habitat_capacity"));
            }
        }
        Collections.sort(preservedRows);assertEquals(originalRows,preservedRows);
        assertEquals(stock,stock());assertEquals(columns,fisheries().columns(site));assertEquals(1,fixture.count("fishery_work"));assertEquals(4,food());
        assertEquals(0,fixture.count("fishery_habitat_changes"));
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void initializationCatchRestartAndRevisitConserveStockFoodCashAndIdentity()throws Exception {
        var before=fisheries().find(site).orElseThrow();var op=UUID.randomUUID();
        var receipt=industry().fish(op,business,100,1,policy);
        assertEquals(4,receipt.quantity());assertEquals(4,food());assertEquals(new FishStock(320,242,4000),stock());assertEquals(8500,fixture.cash());
        fixture.db.close();fixture.open();var saved=stock();
        assertEquals(receipt,industry().fish(op,business,100,1,policy));assertEquals(saved,stock());assertEquals(4,food());
        var retuned=new FishingPolicy(true,1,100,10000,100,200,4,16);
        var revisit=fisheries().open(site,observation(fixture.world,0),retuned);
        assertEquals(saved,revisit.stock());assertEquals(before.openingTerms(),revisit.openingTerms());
        assertThrows(SQLException.class,()->industry().fish(op,business,99,1,policy));
        assertThrows(SQLException.class,()->industry().fish(UUID.randomUUID(),business,100,1,policy));
        assertEquals(1,fixture.count("fishery_work"));assertEquals(40,fixture.count("fishery_columns"));
    }
    @Test void lateHistoryFailureRollsBackResourceFoodReceiptThenExactRetrySucceeds()throws Exception {
        var before=fixture.snapshot();fixture.sql("CREATE TRIGGER reject_fish BEFORE INSERT ON fishery_work BEGIN SELECT RAISE(ABORT,'fixture late failure'); END");
        var op=UUID.randomUUID();assertThrows(SQLException.class,()->industry().fish(op,business,100,1,policy));assertEquals(before,fixture.snapshot());
        assertTrue(fixture.db.getConnection().getAutoCommit());fixture.sql("DROP TRIGGER reject_fish");
        assertEquals(4,industry().fish(op,business,100,1,policy).quantity());assertEquals(1,fixture.count("fishery_work"));
    }
    @Test void unknownPartialOverlappingAndChangedWaterCannotMintAnotherResource()throws Exception {
        var before=fixture.snapshot();
        assertThrows(SQLException.class,()->fisheries().open(site,observation(UUID.randomUUID(),0),policy));
        assertThrows(SQLException.class,()->fisheries().open(site,new FisheryObservation(fixture.world,20,false,observation(fixture.world,0).columns()),policy));
        assertThrows(SQLException.class,()->fisheries().open(site,observation(fixture.world,1),policy));
        assertEquals(before,fixture.snapshot());
        var otherFounding=fixture.mine(fixture.person,50).founding();var other=otherFounding.site();fixture.db.getBusinessRepository().found(otherFounding);
        before=fixture.snapshot();assertThrows(SQLException.class,()->fisheries().open(other.id(),observation(fixture.world,-20),policy));
        assertEquals(before,fixture.snapshot());assertEquals(1,fixture.count("fishery_resources"));
        assertThrows(IllegalArgumentException.class,()->new FisheryObservation(fixture.world,1,true,List.of(new FisheryObservation.Column(0,0),new FisheryObservation.Column(0,0))));
    }
    @Test void depletedStockRecoversDuringRestButUnqualifiedWorkersCannotCatch()throws Exception {
        fixture.sql("UPDATE fishery_resources SET available=0,remainder=0");
        var intensive=new FishingPolicy(true,16,8,4096,75,200,16,16);
        assertEquals(6,industry().fish(UUID.randomUUID(),business,100,1,intensive).quantity());assertEquals(0,stock().available());
        // Explicit regression fixture: future qualification rules can invalidate an old job.
        fixture.sql("UPDATE resident_skills SET experience=0 WHERE skill='fishing'");
        assertEquals(0,industry().fish(UUID.randomUUID(),business,100,2,policy).quantity());
        assertEquals(6,food());assertEquals(new FishStock(320,6,8000),stock());
        for(int i=3;i<=52;i++)industry().fish(UUID.randomUUID(),business,0,i,policy);
        assertEquals(new FishStock(320,320,0),stock());assertEquals(6,food());assertEquals(8500,fixture.cash());
    }
    @Test void fullOwnedStorageLimitsCatchAndUnavailableOwnerOnlyAllowsEcologicalRecovery()throws Exception {
        fixture.sql("UPDATE industry_stock SET units=99 WHERE commodity='food'");
        assertEquals(1,industry().fish(UUID.randomUUID(),business,100,1,policy).quantity());assertEquals(100,food());
        assertEquals(0,industry().fish(UUID.randomUUID(),business,100,2,policy).quantity());
        fixture.sql("UPDATE fishery_resources SET available=0,remainder=0");
        fixture.db.getPopulationRepository().updateResident(new Resident(fixture.person,"Fixture",fixture.town,null,300,Resident.Status.DECEASED));
        assertEquals(0,industry().fish(UUID.randomUUID(),business,100,3,policy).quantity());assertEquals(6,stock().available());assertEquals(100,food());
    }
    @Test void normalClockPausesAbsenceAndRestartRetainsFractionWithoutOfflineRecovery()throws Exception {
        var runtime=runtime(1000);assertEquals(0,runtime.pulse(1500,Set.of(fixture.world)));
        assertEquals(1,runtime.pulse(2000,Set.of(fixture.world)));assertEquals(4,food());var before=stock();
        runtime.observeAvailability(2500,Set.of(fixture.world));assertEquals(0,runtime.pulse(100000,Set.of()));assertEquals(before,stock());
        runtime.observeAvailability(100000,Set.of());assertEquals(0,runtime.pulse(100499,Set.of(fixture.world)));
        assertEquals(1,runtime.pulse(100500,Set.of(fixture.world)));assertEquals(8,food());
        runtime.pulse(101000,Set.of(fixture.world));fixture.db.close();fixture.open();runtime=runtime(1_000_000);
        assertEquals(0,runtime.pulse(1_000_499,Set.of(fixture.world)));assertEquals(1,runtime.pulse(1_000_500,Set.of(fixture.world)));assertEquals(12,food());
        assertEquals(3,fixture.count("fishery_work"));assertEquals(3,fixture.count("industry_runtime_history"));
    }
    @Test void clockCheckpointFailureRollsBackCatchAndRecoveryButRetainsEarnedWork()throws Exception {
        var runtime=runtime(1000);var before=stock();
        fixture.sql("CREATE TRIGGER reject_fish_clock BEFORE UPDATE OF completed ON industry_runtime BEGIN SELECT RAISE(ABORT,'fixture checkpoint'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,Set.of(fixture.world)));
        assertEquals(before,stock());assertEquals(0,food());assertEquals(0,fixture.count("fishery_work"));assertEquals(0,fixture.count("industry_production"));
        fixture.sql("DROP TRIGGER reject_fish_clock");assertEquals(1,runtime.pulse(2000,Set.of(fixture.world)));assertEquals(4,food());
    }
    @Test void disablingRecoveryPreservesResourceWithoutLaterDisabledPeriodCatchup()throws Exception {
        var runtime=runtime(1000);runtime.pulse(2000,Set.of(fixture.world));var before=stock();
        runtime.configureFishing(FishingPolicy.disabled());assertEquals(1,runtime.pulse(3000,Set.of(fixture.world)));
        assertEquals(before,stock());assertEquals(4,food());
        runtime.configureFishing(policy);runtime.pulse(4000,Set.of(fixture.world));assertEquals(8,food());
        assertEquals(3,fixture.count("fishery_work"));
    }
    @Test void actualCatchCanFeedARealPaidVillageCustomerAndArriveBeforeConsumption()throws Exception {
        industry().fish(UUID.randomUUID(),business,100,1,policy);assertEquals(4,food());
        // Genuine accelerated public consumption exhausts opening reserves; no invented buyer or stock deletion.
        for(int second=3;second<=52;second++)fixture.economy.pulse(second*1000L,false);
        var buyers=new VillageSupplyPurchases(fixture.db.getConnection(),VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.disabled());
        buyers.purchase(new VillageSupplyPurchases.Request(UUID.randomUUID(),fixture.town,business,Commodity.FOOD,2,51),Set.of(fixture.world));
        assertEquals(2,food());assertEquals(8100,fixture.cash());assertEquals(1400,industry().store(business).orElseThrow().hundredths());
        var economy=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.settings,
                com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);
        economy.configureVillageSupplies(VillageSupplyPolicy.prototype());economy.startOnline(52000,Set.of(fixture.world));economy.pulse(53000,false);
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("SELECT demand,fulfilled FROM needs_periods WHERE period=52")) {
            assertTrue(r.next());assertEquals(2,r.getLong(1));assertEquals(2,r.getLong(2));
        }
        // Healthy households now trigger a real emergency-reserve order for the remaining caught food.
        assertEquals(2,fixture.count("village_supply_orders"));assertEquals(0,food());assertEquals(7700,fixture.cash());
    }
    @Test void migration66FailureIsTransactionalAndRetryPreservesAllOldRows()throws Exception {
        // Reconstruct schema65 only in this @TempDir database; no fishery existed in that schema.
        SchemaFixtures.rewind(fixture.db.getConnection(),65);fixture.sql("PRAGMA user_version=65");var before=fixture.snapshot();
        fixture.sql("CREATE VIEW fishery_columns AS SELECT 1");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(fixture.db.getConnection()));
        assertEquals(before,fixture.snapshot());assertTrue(fixture.db.getConnection().getAutoCommit());
        fixture.sql("DROP VIEW fishery_columns");new SchemaMigrator().migrate(fixture.db.getConnection());var after=fixture.snapshot();
        for(var table:List.of("livestock_capture","livestock_animal_losses","craftsman_founder_courses","craftsman_founder_reviews","business_storage_upgrades","bakery_founder_courses","bakery_founder_reviews","bakery_private_starters","livestock_herds","livestock_initial_animals","livestock_periods","livestock_young","protected_pen_animals","protected_pen_columns","protected_pens","animal_ownership","farm_market_harvests","farm_market_stores","grain_harvests","grain_stocks","farm_grain_evidence","fishery_founder_courses","fishery_workplace_history","fishery_workplaces","fishery_resources","fishery_columns","fishery_work","fishery_habitat_changes","water_surveys","water_columns"))assertEquals(List.of(),after.remove(table));assertEquals(before,after);
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
