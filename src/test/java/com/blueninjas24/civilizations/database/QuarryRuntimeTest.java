package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class QuarryRuntimeTest {
    @TempDir Path directory;QuarryPersistenceTest fixture;
    MiningStartupRepository.Request request;UUID business,deposit;
    @BeforeEach void setup()throws Exception{fixture=new QuarryPersistenceTest();fixture.directory=directory;fixture.setup();}
    @AfterEach void close(){fixture.close();}
    BusinessStartupGrantsTest data(){return fixture.fixture;}
    void start(String kind)throws Exception{request=fixture.request(kind);var result=data().grants().startMine(request,StartupGrantPolicy.prototype());business=request.founding().business().id();deposit=result.startup().discovery().deposit().id();}
    IndustryRuntime runtime(QuarryPolicy quarry,long now)throws Exception {
        var r=new IndustryRuntime(data().db.getConnection(),new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of()),BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());r.configureQuarries(quarry);r.start(now);return r;
    }
    long xp()throws Exception{return data().db.getPopulationRepository().findWorkProfile(data().person).orElseThrow().experience().getOrDefault("mining",0L);}
    long remaining()throws Exception{return data().db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches();}
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void ordinaryTimerUsesResourceSpecificRatesAndNeverEarnsAbsentOrDowntimeWork(String kind)throws Exception {
        start(kind);var terms=QuarryPolicy.prototype().terms(fixture.resource(kind));var r=runtime(QuarryPolicy.prototype(),1000);r.pulse(2000,Set.of(data().world));
        assertEquals(128-terms.batchesPerWorker(),remaining());assertEquals(terms.batchesPerWorker()*terms.unitsPerBatch(),data().db.getIndustryRepository().store(business).orElseThrow().units().get(fixture.resource(kind)));assertEquals(1,xp());
        r.pulse(2000,Set.of(data().world));r.pulse(100000,Set.of());assertEquals(1,xp());data().db.close();data().open();r=runtime(QuarryPolicy.prototype(),100000);r.pulse(100000,Set.of(data().world));assertEquals(1,xp());r.pulse(101000,Set.of(data().world));assertEquals(2,xp());assertEquals(128-2*terms.batchesPerWorker(),remaining());assertEquals(8500,data().cash());
    }
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void disabledPolicyRetainsClockAssetsAndDoesNotBackfillWhenReenabled(String kind)throws Exception {
        start(kind);var r=runtime(QuarryPolicy.prototype(),1000);r.pulse(2000,Set.of(data().world));long before=remaining();
        r=runtime(QuarryPolicy.disabled(),2000);r.pulse(3000,Set.of(data().world));assertEquals(before,remaining());assertEquals(1,xp());
        r=runtime(QuarryPolicy.prototype(),3000);r.pulse(3000,Set.of(data().world));assertEquals(before,remaining());r.pulse(4000,Set.of(data().world));assertEquals(2,xp());assertEquals(2,data().count("industry_production"));assertEquals(3,data().count("industry_runtime_history"));
    }
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void absentOldSettingsCannotEnrollNewExtractionClocks(String kind)throws Exception {
        start(kind);var r=runtime(QuarryPolicy.disabled(),1000);r.pulse(2000,Set.of(data().world));
        assertEquals(0,data().count("industry_runtime"));assertEquals(0,data().count("industry_production"));assertEquals(128,remaining());assertEquals(0,xp());
    }
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void captureGapStopsExtractionWithoutResettingOtherShares(String kind)throws Exception {
        start(kind);var r=runtime(QuarryPolicy.prototype(),1000);var tracking=new OreTrackingRepository(data().db.getConnection());tracking.begin(10);tracking.markGap(20);
        r.pulse(2000,Set.of(data().world));assertEquals(128,remaining());assertEquals(0,xp());assertEquals(0,data().count("industry_production"));
    }
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void removingOneSupportLosesOnlyItsRemainingShareAndOtherBlocksKeepWorking(String kind)throws Exception {
        start(kind);var r=runtime(QuarryPolicy.prototype(),1000);r.pulse(2000,Set.of(data().world));
        long lost=new MineralSources(data().db.getConnection()).remove(data().world,List.of(fixture.blocks.getFirst()),20,"Fixture lost supporting block");
        var rate=QuarryPolicy.prototype().terms(fixture.resource(kind)).batchesPerWorker();assertEquals(128-rate-lost,remaining());
        r.pulse(3000,Set.of(data().world));assertEquals(128-2*rate-lost,remaining());assertEquals(2,xp());
        assertEquals(0,new MineralSources(data().db.getConnection()).remove(data().world,List.of(fixture.blocks.getFirst()),30,"Repeated fixture loss"));
    }
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void lateCheckpointFailureRestoresReserveGoodsSkillAndReceiptThenRetries(String kind)throws Exception {
        start(kind);var r=runtime(QuarryPolicy.prototype(),1000);data().sql("CREATE TRIGGER reject_quarry_checkpoint BEFORE UPDATE OF completed ON industry_runtime BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->r.pulse(2000,Set.of(data().world)));assertEquals(128,remaining());assertEquals(0,xp());assertEquals(0,data().count("industry_production"));
        data().sql("DROP TRIGGER reject_quarry_checkpoint");r.pulse(2000,Set.of(data().world));r.pulse(2000,Set.of(data().world));assertEquals(1,xp());assertEquals(1,data().count("industry_production"));
    }
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void workSpeedRetuningPreservesOriginalAbundanceAndOutputBundle(String kind)throws Exception {
        start(kind);var original=data().db.getIndustryRepository().deposit(deposit).orElseThrow().bundle();var tuned=new QuarryPolicy(true,new QuarryPolicy.Terms(999,999,1,1),new QuarryPolicy.Terms(999,999,1,1));
        var r=runtime(tuned,1000);r.pulse(2000,Set.of(data().world));assertEquals(127,remaining());assertEquals(original,data().db.getIndustryRepository().deposit(deposit).orElseThrow().bundle());assertEquals(QuarryPolicy.prototype().terms(fixture.resource(kind)).unitsPerBatch(),data().db.getIndustryRepository().store(business).orElseThrow().units().get(fixture.resource(kind)));
    }
}
