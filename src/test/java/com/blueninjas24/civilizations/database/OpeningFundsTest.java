package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.Map;
import java.util.EnumMap;
import java.math.BigDecimal;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class OpeningFundsTest {
    @TempDir Path directory;
    private DatabaseManager db;
    private final UUID town = UUID.randomUUID(), world = UUID.randomUUID();
    @BeforeEach void open() throws SQLException {
        reopen(); db.saveSettlement(new Settlement(town, "Funds fixture", "world", 0, 64, 0, 1, 999, 2));
        db.getVillageIdentityRepository().observeWorld(world, "world");
        db.getVillageIdentityRepository().bindSettlement(town, world);
        resident(300, Resident.Status.ALIVE); resident(10, Resident.Status.ALIVE); resident(300, Resident.Status.DECEASED);
    }
    private void reopen() throws SQLException {
        db = new DatabaseManager(directory.toFile(), Logger.getLogger("OpeningFundsTest")); db.initialize();
    }
    @AfterEach void close() { db.close(); }
    private void resident(long age, Resident.Status status) throws SQLException {
        db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(), "Fixture", town, null, age, status));
    }
    private EconomyRuntimeRepository runtime(OpeningFunds funds) {
        var bands = new EnumMap<CommodityAccounting.Condition, Integer>(CommodityAccounting.Condition.class);
        for (var band : CommodityAccounting.Condition.values()) bands.put(band, 100);
        var settings = new EconomySettings(60, 0, 2, 2, 1, 4, 100,
                new CommodityAccounting.Policy(5000, 12500, 20000, 3), Map.of(Commodity.FOOD, 12L),
                Map.of(Commodity.FOOD, BigDecimal.valueOf(2)), bands);
        return new EconomyRuntimeRepository(db.getConnection(), settings, FarmObservation.Policy.prototype(),
                216, VillagePriorities.Policy.prototype(), 0, funds);
    }
    private void observe(EconomyRuntimeRepository runtime, long time, boolean complete) throws SQLException {
        runtime.observe(new FarmObservation(town, world, time, complete, 0), time);
    }
    private TreasuryRepository treasury() { return new TreasuryRepository(db.getConnection()); }
    private long entries() throws SQLException {
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery("SELECT COUNT(*) FROM treasury_entries")) { return r.getLong(1); }
    }
    private void sql(String text) throws SQLException { try (var s = db.getConnection().createStatement()) { s.execute(text); } }

    @Test void reliableEnrollmentFundsOnlyLivingPeopleAndNeverAddsMoneyForNewResidentsOrRestart() throws SQLException {
        var runtime = runtime(OpeningFunds.prototype()); observe(runtime, 1000, false);
        assertTrue(treasury().load(town).isEmpty()); observe(runtime, 1001, true);
        assertEquals(4800, treasury().load(town).orElseThrow().hundredths()); assertEquals(1, entries());
        treasury().post(UUID.randomUUID(), town, -4800, "Fixture spending");
        resident(1, Resident.Status.ALIVE); observe(runtime, 1002, true); observe(runtime, 1002, true);
        assertEquals(0, treasury().load(town).orElseThrow().hundredths());
        db.close(); reopen(); runtime(new OpeningFunds(true, 9900)).start(1002);
        assertEquals(0, treasury().load(town).orElseThrow().hundredths()); assertEquals(2, entries());
    }
    @Test void explicitEnableSeedsExistingUnfundedEconomyOnceWithoutRewritingGoodsOrPeriods() throws SQLException {
        var runtime = runtime(OpeningFunds.disabled()); observe(runtime, 1000, true);
        var before = runtime.views().get(town).account(); assertTrue(treasury().load(town).isEmpty());
        runtime(new OpeningFunds(true, 1225)).start(1000);
        assertEquals(2450, treasury().load(town).orElseThrow().hundredths());
        assertEquals(before, runtime.views().get(town).account());
        runtime(OpeningFunds.disabled()).start(1000); runtime(new OpeningFunds(true, 1)).start(1000);
        assertEquals(2450, treasury().load(town).orElseThrow().hundredths()); assertEquals(1, entries());
    }
    @Test void zeroGrantIsAnInitializedAccountAndManualTreasuriesArePreserved() throws SQLException {
        observe(runtime(new OpeningFunds(true, 0)), 1000, true);
        runtime(OpeningFunds.prototype()).start(1000);
        assertEquals(0, treasury().load(town).orElseThrow().hundredths()); assertEquals(1, entries());
        // A separate village's previously administered account is not overwritten.
        var other = UUID.randomUUID(); db.saveSettlement(new Settlement(other, "Existing", "world", 10, 64, 0, 1, 0, 1));
        db.getVillageIdentityRepository().bindSettlement(other, world);
        db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(), "Fixture", other, null, 300, Resident.Status.ALIVE));
        treasury().initialize(UUID.randomUUID(), other, 777, "Fixture existing capital");
        runtime(OpeningFunds.prototype()).observe(new FarmObservation(other, world, 1000, true, 0), 1000);
        assertEquals(777, treasury().load(other).orElseThrow().hundredths());
    }
    @Test void fundingFailureRollsBackNewEconomyAndObservationThenRetrySeedsOnce() throws SQLException {
        sql("CREATE TRIGGER reject_opening BEFORE INSERT ON treasury_entries BEGIN SELECT RAISE(ABORT,'injected'); END");
        var runtime = runtime(OpeningFunds.prototype());
        assertThrows(SQLException.class, () -> observe(runtime, 1000, true));
        assertTrue(runtime.views().isEmpty()); assertTrue(treasury().load(town).isEmpty());
        assertTrue(new CommodityRepository(db.getConnection()).load(town).isEmpty());
        sql("DROP TRIGGER reject_opening"); observe(runtime, 1000, true);
        assertEquals(4800, treasury().load(town).orElseThrow().hundredths()); assertEquals(1, entries());
    }
    @Test void startupFundingFailureLeavesExistingUnfundedHistoryIntactAndCanRetry() throws SQLException {
        var old = runtime(OpeningFunds.disabled()); observe(old, 1000, true); var before = old.views().get(town).account();
        sql("CREATE TRIGGER reject_opening BEFORE INSERT ON treasury_entries BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class, () -> runtime(OpeningFunds.prototype()).start(1000));
        assertTrue(treasury().load(town).isEmpty()); assertEquals(before, old.views().get(town).account());
        sql("DROP TRIGGER reject_opening"); runtime(OpeningFunds.prototype()).start(1000);
        assertEquals(4800, treasury().load(town).orElseThrow().hundredths()); assertEquals(1, entries());
    }
    @Test void invalidPopulationOrAmountsAndOverflowNeverWrapMoney() {
        assertThrows(IllegalArgumentException.class, () -> new OpeningFunds(true, -1));
        assertThrows(IllegalArgumentException.class, () -> new OpeningFunds(true, 100_000_001));
        assertThrows(IllegalArgumentException.class, () -> OpeningFunds.prototype().amount(-1));
        assertThrows(ArithmeticException.class, () -> OpeningFunds.prototype().amount(Long.MAX_VALUE));
        assertEquals(0, OpeningFunds.prototype().amount(0));
    }
}
