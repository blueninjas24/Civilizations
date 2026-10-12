package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WarehouseOpportunitiesTest {
    @TempDir Path directory;WorkshopInvestmentTest fixture;IndustryRuntimePolicy industry;boolean healthy=true;
    @BeforeEach void setup()throws Exception {initialize(true);}
    void initialize(boolean laterPeriod)throws Exception {
        fixture=new WorkshopInvestmentTest();fixture.directory=directory;fixture.setup();fixture.qualified();var draft=fixture.draft();fixture.engine().accept(draft,fixture.proof(draft),3000,Set.of(fixture.world));
        var runtime=new IndustryRuntime(fixture.db.getConnection(),fixture.industry,BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());runtime.start(3000);runtime.pulse(4000,Set.of(fixture.world));
        new FarmToolPurchases(fixture.db.getConnection(),BusinessOperationPolicy.prototype()).purchase(new FarmToolPurchases.Request(UUID.randomUUID(),FarmEnterprises.businessId(fixture.town),draft.anchor().business(),2,1,128),FarmToolBudgetPolicy.prototype(),Set.of(fixture.world));
        // A later real food period replenishes the shared period allowance, not village money.
        if(laterPeriod)advanceFood();
        industry=new IndustryRuntimePolicy(true,1000,8,fixture.industry.mining(),fixture.industry.recipes(),fixture.industry.supply(),new WarehouseSupplyPolicy(true,16,16));
        markers(4000,List.of(new WorkplaceObservation.Marker(new Position(8,64,0),WorkplaceObservation.Kind.BARREL)));
        new OreTrackingRepository(fixture.db.getConnection()).begin(1000);ore(4000,List.of(new Position(20,64,0)),List.of(new Position(25,64,0)));
    }
    void advanceFood()throws Exception {
        var food=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.settings,FarmObservation.Policy.prototype(),216);
        food.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());food.configureFarmTools(fixture.upkeep,FarmToolBudgetPolicy.prototype(),IndustryPolicy.prototype());food.configureWorkLearning(WorkLearningPolicy.prototype());
        food.startOnline(3000,Set.of(fixture.world));food.pulse(4000,false);
    }
    @AfterEach void close(){fixture.close();}
    void markers(long time,List<WorkplaceObservation.Marker> markers)throws SQLException{fixture.markers(time,true,markers);}
    void ore(long time,List<Position> coal,List<Position> iron)throws SQLException {
        new MineralOpportunities(fixture.db.getConnection()).observe(fixture.town,List.of(new MineralObservation(fixture.world,time,true,Commodity.COAL,coal),new MineralObservation(fixture.world,time,true,Commodity.IRON,iron)));
    }
    WarehouseOpportunities engine(WarehouseOpportunityPolicy policy){return new WarehouseOpportunities(fixture.db.getConnection(),policy,industry,BusinessOperationPolicy.prototype(),StartupGrantPolicy.prototype(),MineFoundingPolicy.prototype(),()->healthy);}
    WarehouseOpportunities.Review review()throws SQLException{return engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,4000,Set.of(fixture.world));}
    @Test void actualPaidWorkshopGapAndSuitableOreProduceReadonlyDeterministicProspect()throws Exception {
        var before=fixture.snapshot();var result=review();assertEquals(1,result.candidates().size(),result.explanation());var option=result.candidates().getFirst();
        assertEquals(Map.of(Commodity.COAL,1L,Commodity.IRON,2L),option.missing());assertEquals(3000,option.purchaseHundredths());assertEquals(2,option.ore().size());assertEquals(WorkplaceObservation.Kind.BARREL,option.workplace().kind());
        assertEquals(result,review());assertEquals(before,fixture.snapshot());fixture.db.close();fixture.open();assertEquals(result,review());assertEquals(before,fixture.snapshot());
    }
    @Test void noFreeSourceFromMissingOreTrackingGapOrImmediateMutationFailure()throws Exception {
        healthy=false;var before=fixture.snapshot();assertTrue(review().candidates().isEmpty());assertEquals(before,fixture.snapshot());healthy=true;
        ore(4001,List.of(),List.of(new Position(25,64,0)));assertTrue(engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,4001,Set.of(fixture.world)).candidates().isEmpty());
        ore(4002,List.of(new Position(20,64,0)),List.of(new Position(25,64,0)));new OreTrackingRepository(fixture.db.getConnection()).markGap(4002);
        assertTrue(engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,4002,Set.of(fixture.world)).candidates().isEmpty());
    }
    @Test void fundingStoragePricesAndHaulMustCoverTheCompleteActualGap()throws Exception {
        var tiny=new WarehouseOpportunityPolicy(true,8,60000,96,48,500,2000,1024);assertTrue(engine(tiny).review(fixture.town,4000,Set.of(fixture.world)).candidates().isEmpty());
        assertTrue(engine(new WarehouseOpportunityPolicy(true,8,60000,96,48,500,4000,2)).review(fixture.town,4000,Set.of(fixture.world)).candidates().isEmpty());
        fixture.sql("UPDATE treasuries SET hundredths=2500");var before=fixture.snapshot();assertTrue(review().candidates().isEmpty());assertEquals(before,fixture.snapshot());
    }
    @Test void staleFutureAbsentDisabledAndOutOfRangeMarkersNeverQualify()throws Exception {
        assertTrue(engine(WarehouseOpportunityPolicy.disabled()).review(fixture.town,4000,Set.of(fixture.world)).candidates().isEmpty());
        assertTrue(engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,4000,Set.of()).candidates().isEmpty());
        assertTrue(engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,3999,Set.of(fixture.world)).candidates().isEmpty());
        assertTrue(engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,64001,Set.of(fixture.world)).candidates().isEmpty());
        assertTrue(engine(new WarehouseOpportunityPolicy(true,8,60000,4,48,500,4000,1024)).review(fixture.town,4000,Set.of(fixture.world)).candidates().isEmpty());
    }
    @Test void changedCustomerMoneyAndExistingWarehouseIdentityBlockFirstFirmBypass()throws Exception {
        fixture.sql("UPDATE industry_stores SET hundredths=1000 WHERE hundredths=5000");assertTrue(review().candidates().isEmpty());
        fixture.sql("UPDATE industry_stores SET hundredths=5000 WHERE hundredths=1000");
        var site=new BusinessSite(UUID.randomUUID(),fixture.town,fixture.world,9,64,0,4000,"Explicit retained warehouse fixture");var req=new JobRequirements(216,Map.of());
        fixture.db.getBusinessRepository().found(new BusinessFounding(new Business(UUID.randomUUID(),fixture.town,"Warehouse fixture",fixture.prospect,fixture.prospect,"warehouse",site.id(),1,0),site,req,req,0,"Fixture"));
        var result=review();assertTrue(result.candidates().isEmpty());assertTrue(result.explanation().contains("identity retained"));
    }
    @Test void placementAndResourceRangeExcludeOnlyActualSupportingCoordinates()throws Exception {
        new OrePlacementRepository(fixture.db.getConnection()).record(fixture.world,List.of(new Position(20,64,0)),4000);
        assertTrue(review().candidates().isEmpty());
        ore(4001,List.of(new Position(150,64,0)),List.of(new Position(25,64,0)));assertTrue(engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,4001,Set.of(fixture.world)).candidates().isEmpty());
    }
    @Test void actualSqlOwnerExposesReadonlyCandidatesAndLegacyPolicyOptsOut()throws Exception {
        fixture.db.close();var original=com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype();
        var settings=new com.blueninjas24.civilizations.application.PopulationSession.Settings(0,original.millisPerMonth(),original.maximumOfflineMonths(),original.monthsPerBatch(),original.population(),original.migration(),original.recognition(),original.enrollment());
        for(boolean enabled:List.of(false,true))try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),java.util.logging.Logger.getLogger("WarehouseProspectTest"),settings,()->4000,fixture.settings,FarmObservation.Policy.prototype())) {
            session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureFarmTools(fixture.upkeep,FarmToolBudgetPolicy.prototype());session.configureWorkLearning(WorkLearningPolicy.prototype());session.configureBusinessOperations(BusinessOperationPolicy.prototype());
            session.configureIndustryRuntime(industry);session.configureWorkplaces(WorkplaceObserverPolicy.prototype());session.configureMineFounding(MineFoundingPolicy.prototype(),StartupGrantPolicy.prototype());
            if(enabled)session.configureWarehouseOpportunities(WarehouseOpportunityPolicy.prototype());
            session.start(false,Set.of(fixture.world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            var result=session.currentWarehouseOpportunities(fixture.town).get(5,java.util.concurrent.TimeUnit.SECONDS);assertEquals(enabled?1:0,result.candidates().size(),result.explanation());
        }
        fixture.open();assertEquals(24500,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM startup_grants")){assertEquals(1,r.getInt(1));}
    }
    @Test void samePeriodWorkshopGrantCannotBeBypassedByWarehouseProspect()throws Exception {
        fixture.close();directory=directory.resolve("same-period");initialize(false);var before=fixture.snapshot();var blocked=review();assertTrue(blocked.candidates().isEmpty());assertTrue(blocked.explanation().contains("period grant budget"));assertEquals(before,fixture.snapshot());
        advanceFood();assertEquals(1,review().candidates().size());assertEquals(24500,new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths());
    }
    @Test void excessMarkersDeferEntireReviewInsteadOfSelectingPartialPage()throws Exception {
        markers(4001,java.util.stream.IntStream.range(0,9).mapToObj(z->new WorkplaceObservation.Marker(new Position(8,64,z),WorkplaceObservation.Kind.BARREL)).toList());var before=fixture.snapshot();
        var result=engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,4001,Set.of(fixture.world));assertTrue(result.candidates().isEmpty());assertTrue(result.explanation().contains("budget exceeded"));assertEquals(before,fixture.snapshot());
    }
    @Test void separateFundableGrantsDoNotCertifyAnUnaffordableWholeChain()throws Exception {
        fixture.sql("UPDATE treasuries SET hundredths=10000");var before=fixture.snapshot();var result=review();assertTrue(result.candidates().isEmpty());assertTrue(result.explanation().contains("chain would spend protected"));assertEquals(before,fixture.snapshot());
    }
    @Test void separateLifetimeFeasibilityDoesNotCertifyAllNeededSupplierGrants()throws Exception {
        var p=StartupGrantPolicy.prototype();var limit=new StartupGrantPolicy(true,p.maximumGrant(),p.perPeriodLimit(),13000,p.minimumTreasury(),p.foodReservePeriods(),p.hundredthsPerFoodUnit(),p.industries());
        var engine=new WarehouseOpportunities(fixture.db.getConnection(),WarehouseOpportunityPolicy.prototype(),industry,BusinessOperationPolicy.prototype(),limit,MineFoundingPolicy.prototype(),()->true);
        var before=fixture.snapshot();var result=engine.review(fixture.town,4000,Set.of(fixture.world));assertTrue(result.candidates().isEmpty());assertTrue(result.explanation().contains("remaining lifetime"));assertEquals(before,fixture.snapshot());
    }
    @Test void visibleSourcesMustContainEnoughFinitePotentialForInitialGap()throws Exception {
        var old=industry.mining();var scarce=new IndustryPolicy(1,old.batchesPerWorker(),old.siteBatchCapacity(),old.storageCapacity(),old.localDeliveryPeriods(),old.localHaulUnitsPerPeriod(),old.miningBundle(),old.storageSpacePerUnit());
        industry=new IndustryRuntimePolicy(true,1000,8,scarce,industry.recipes(),industry.supply(),industry.warehouseSupply());
        var before=fixture.snapshot();var result=review();assertTrue(result.candidates().isEmpty());assertTrue(result.explanation().contains("finite mineral potential"));assertEquals(before,fixture.snapshot());
    }
    @Test void disconnectedOreCannotBePooledIntoOneAffordableSupplier()throws Exception {
        var old=industry.mining();var scarce=new IndustryPolicy(1,old.batchesPerWorker(),old.siteBatchCapacity(),old.storageCapacity(),old.localDeliveryPeriods(),old.localHaulUnitsPerPeriod(),old.miningBundle(),old.storageSpacePerUnit());
        industry=new IndustryRuntimePolicy(true,1000,8,scarce,industry.recipes(),industry.supply(),industry.warehouseSupply());
        ore(4001,List.of(new Position(20,64,0)),List.of(new Position(25,64,0),new Position(27,64,0)));
        assertTrue(engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,4001,Set.of(fixture.world)).candidates().isEmpty());
        ore(4002,List.of(new Position(20,64,0)),List.of(new Position(25,64,0),new Position(26,64,0)));
        assertEquals(1,engine(WarehouseOpportunityPolicy.prototype()).review(fixture.town,4002,Set.of(fixture.world)).candidates().size());
    }
}
