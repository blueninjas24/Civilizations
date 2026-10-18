package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.Business;
import com.blueninjas24.civilizations.industry.BusinessFounding;
import com.blueninjas24.civilizations.industry.BusinessSite;
import com.blueninjas24.civilizations.population.JobRequirements;
import com.blueninjas24.civilizations.industry.MineralObservation;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MineralRecoveryTest {
    @TempDir Path directory;
    MineralSourcesTest fixture;
    UUID otherDeposit;
    UUID otherBusiness;

    @BeforeEach void setup() throws Exception {
        fixture = new MineralSourcesTest();
        fixture.directory = Files.createDirectories(directory.resolve("db"));
        fixture.setup();
    }

    @AfterEach void close() {
        if (fixture != null) fixture.close();
    }

    private MineralRecovery recovery() { return new MineralRecovery(fixture.db.getConnection()); }

    private void startTrustedCapture() throws SQLException {
        var tracking = new OreTrackingRepository(fixture.db.getConnection());
        tracking.begin(1);
        tracking.finish(2, true);
    }

    private void loseGlobalTrustKeepCaptureHealthy() throws SQLException {
        var tracking = new OreTrackingRepository(fixture.db.getConnection());
        tracking.begin(1);
        tracking.markGap(2);
        tracking.begin(3);
        assertFalse(tracking.load().orElseThrow().trusted());
        assertTrue(new ResourceRecoveryEpochs(fixture.db.getConnection()).load("ore").orElseThrow().healthy());
    }

    private Map<Position, Boolean> current(MineralRecovery.Plan plan, Position changed) {
        var observed = new HashMap<Position, Boolean>();
        for (var source : plan.sources()) observed.put(source.position(), !source.position().equals(changed));
        return observed;
    }

    private MineralRecovery.Receipt approve(MineralRecovery.Plan plan, Map<Position, Boolean> current,
                                            UUID operation) throws SQLException {
        return recovery().approve(operation, "operator-test", plan, current, 10, 10,
                () -> true);
    }

    private long count(String table) throws SQLException {
        try (var s = fixture.db.getConnection().createStatement(); var r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return r.getLong(1);
        }
    }

    private Map<String, List<List<String>>> snapshot() throws SQLException {
        Connection connection = fixture.db.getConnection();
        var names = new ArrayList<String>();
        try (var s = connection.createStatement(); var r = s.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
            while (r.next()) names.add(r.getString(1));
        }
        var result = new TreeMap<String, List<List<String>>>();
        for (String name : names) {
            var rows = new ArrayList<List<String>>();
            try (var s = connection.createStatement(); var r = s.executeQuery("SELECT * FROM \"" + name + "\"")) {
                int columns = r.getMetaData().getColumnCount();
                while (r.next()) {
                    var row = new ArrayList<String>();
                    for (int i = 1; i <= columns; i++) row.add(Objects.toString(r.getObject(i), null));
                    rows.add(Collections.unmodifiableList(row));
                }
            }
            rows.sort(Comparator.comparing(Object::toString));
            result.put(name, List.copyOf(rows));
        }
        return result;
    }

    private void addOtherDeposit() throws Exception {
        UUID otherSite = UUID.randomUUID();
        var place = new BusinessSite(otherSite, fixture.town, fixture.world, 100, 40, 0, 1,
                "Second explicit fixture site");
        UUID secondResident = UUID.randomUUID();
        fixture.db.getPopulationRepository().insertResident(new com.blueninjas24.civilizations.population.Resident(
                secondResident, "Second Miner", fixture.town, null, 300,
                com.blueninjas24.civilizations.population.Resident.Status.ALIVE));
        otherBusiness = UUID.randomUUID();
        var business = new Business(otherBusiness, fixture.town, "Second Mine", fixture.resident,
                fixture.resident, "mining", otherSite, 1, 0);
        fixture.db.getBusinessRepository().found(new BusinessFounding(business, place,
                new JobRequirements(216, Map.of()), new JobRequirements(216, Map.of()), 0,
                "Second fixture opportunity"));
        fixture.db.getBusinessRepository().hire(UUID.randomUUID(), otherBusiness, secondResident, 0);
        fixture.db.getIndustryRepository().openStore(otherBusiness, 10_000,
                Map.of(Commodity.COAL, 1L, Commodity.STONE, 1L), 1_000);
        var positions = List.of(new Position(10, 40, 0), new Position(11, 40, 0),
                new Position(12, 40, 0), new Position(13, 40, 0));
        otherDeposit = new MineralDiscoveryRepository(fixture.db.getConnection()).discover(
                otherSite, new MineralObservation(fixture.world, 1, true, Commodity.COAL, positions),
                25, Map.of(Commodity.COAL, 1L, Commodity.STONE, 2L)).deposit().id();
    }

    @Test void prepareIsReadOnlyAndRequiresHealthyCurrentCapture() throws Exception {
        loseGlobalTrustKeepCaptureHealthy();
        var before = snapshot();
        var plan = recovery().prepare(fixture.deposit);
        assertEquals(fixture.deposit, plan.deposit());
        assertEquals(100, plan.remaining());
        assertEquals(before, snapshot());

        new OreTrackingRepository(fixture.db.getConnection()).markGap(4);
        assertThrows(SQLException.class, () -> recovery().prepare(fixture.deposit));
    }

    @Test void matchingReviewRestoresOnlySelectedDepositWithoutClearingGlobalTrust() throws Exception {
        addOtherDeposit();
        startTrustedCapture();
        fixture.db.getIndustryRepository().mine(UUID.randomUUID(), otherBusiness, otherDeposit, 7, 1, 100, 100);
        var tracking = new OreTrackingRepository(fixture.db.getConnection());
        tracking.markGap(3);
        tracking.begin(4);
        assertFalse(tracking.load().orElseThrow().trusted());
        var epochs = new ResourceRecoveryEpochs(fixture.db.getConnection());
        UUID generation = epochs.load("ore").orElseThrow().generation();
        tracking.finish(5, true);
        tracking.begin(6);
        assertEquals(generation, epochs.load("ore").orElseThrow().generation());
        assertFalse(tracking.load().orElseThrow().trusted());
        var plan = recovery().prepare(fixture.deposit);

        var receipt = approve(plan, current(plan, null), UUID.randomUUID());
        assertEquals(100, receipt.before());
        assertEquals(0, receipt.lost());
        assertEquals(100, fixture.reserve());
        assertTrue(recovery().permitted(fixture.deposit));
        assertFalse(recovery().permitted(otherDeposit));
        assertFalse(tracking.load().orElseThrow().trusted());
        assertEquals(7, fixture.db.getIndustryRepository().store(otherBusiness).orElseThrow().units().get(Commodity.COAL));
        assertEquals(1, count("industry_production"));
    }

    @Test void confirmedMissingSourceRetiresOnlyItsShareAndPreservesOtherGoodsAndHistory() throws Exception {
        addOtherDeposit();
        startTrustedCapture();
        fixture.db.getIndustryRepository().mine(UUID.randomUUID(), otherBusiness, otherDeposit, 7, 1, 100, 100);
        var tracking = new OreTrackingRepository(fixture.db.getConnection());
        tracking.markGap(3);
        tracking.begin(4);
        var plan = recovery().prepare(fixture.deposit);
        var missing = fixture.coordinates.get(1);

        var receipt = approve(plan, current(plan, missing), UUID.randomUUID());
        assertEquals(25, receipt.lost());
        assertEquals(75, receipt.after());
        assertEquals(List.of(25L, 0L, 25L, 25L), fixture.balances());
        assertEquals(75, fixture.reserve());
        assertEquals(93, fixture.db.getIndustryRepository().deposit(otherDeposit).orElseThrow().remainingBatches());
        assertEquals(7, fixture.db.getIndustryRepository().store(otherBusiness).orElseThrow().units().get(Commodity.COAL));
        assertEquals(1, count("industry_production"));
        assertEquals(1, count("ore_source_removals"));
        assertTrue(recovery().permitted(fixture.deposit));
    }

    @Test void incompleteCurrentEvidenceIsRejectedWithoutAuditOrReserveChange() throws Exception {
        loseGlobalTrustKeepCaptureHealthy();
        var plan = recovery().prepare(fixture.deposit);
        var incomplete = current(plan, null);
        incomplete.remove(fixture.coordinates.getFirst());
        assertThrows(IllegalArgumentException.class, () -> approve(plan, incomplete, UUID.randomUUID()));
        assertEquals(100, fixture.reserve());
        assertEquals(0, count("mineral_recoveries"));
        assertFalse(recovery().permitted(fixture.deposit));
    }

    @Test void stickyCoordinateExclusionCannotBeReenabledByCurrentPositiveEvidence() throws Exception {
        loseGlobalTrustKeepCaptureHealthy();
        var excluded = fixture.coordinates.getFirst();
        new OrePlacementRepository(fixture.db.getConnection()).record(fixture.world, List.of(excluded), 4);
        var plan = recovery().prepare(fixture.deposit);
        assertTrue(plan.sources().stream().filter(source -> source.position().equals(excluded))
                .findFirst().orElseThrow().excluded());

        var receipt = approve(plan, current(plan, null), UUID.randomUUID());
        assertEquals(25, receipt.lost());
        assertEquals(75, fixture.reserve());
        assertEquals(0, fixture.db.getIndustryRepository().store(fixture.mine).orElseThrow().units()
                .getOrDefault(Commodity.COAL, 0L));
        assertTrue(new OrePlacementRepository(fixture.db.getConnection()).excludes(fixture.world, excluded));
    }

    @Test void stalePlanAfterExtractionOrMutationCannotCreateAnAudit() throws Exception {
        startTrustedCapture();
        var afterExtraction = recovery().prepare(fixture.deposit);
        fixture.mine(UUID.randomUUID(), 7, 1);
        assertThrows(SQLException.class, () -> approve(afterExtraction, current(afterExtraction, null), UUID.randomUUID()));

        var afterMutation = recovery().prepare(fixture.deposit);
        new OreMutationRepository(fixture.db.getConnection()).record(fixture.world,
                List.of(fixture.coordinates.get(0)), List.of(fixture.coordinates.get(0)), 3, "Changed after review");
        assertThrows(SQLException.class, () -> approve(afterMutation, current(afterMutation, null), UUID.randomUUID()));
        assertEquals(70, fixture.reserve());
        assertEquals(0, count("mineral_recoveries"));
    }

    @Test void auditFailureRollsBackRetirementsAndReserveLoss() throws Exception {
        loseGlobalTrustKeepCaptureHealthy();
        var plan = recovery().prepare(fixture.deposit);
        try (var s = fixture.db.getConnection().createStatement()) {
            s.execute("CREATE TRIGGER reject_mineral_recovery BEFORE INSERT ON mineral_recoveries BEGIN SELECT RAISE(ABORT,'audit failed'); END");
        }
        assertThrows(SQLException.class, () -> approve(plan, current(plan, fixture.coordinates.getFirst()), UUID.randomUUID()));
        assertEquals(100, fixture.reserve());
        assertEquals(List.of(25L, 25L, 25L, 25L), fixture.balances());
        assertEquals(0, count("ore_source_removals"));
        assertEquals(0, count("mineral_recoveries"));
        assertFalse(recovery().permitted(fixture.deposit));
    }

    @Test void operationReplayAndEvidenceOlderThanThirtySecondsAreRejected() throws Exception {
        loseGlobalTrustKeepCaptureHealthy();
        var plan = recovery().prepare(fixture.deposit);
        var operation = UUID.randomUUID();
        approve(plan, current(plan, null), operation);
        assertThrows(SQLException.class, () -> approve(recovery().prepare(fixture.deposit),
                current(recovery().prepare(fixture.deposit), null), operation));
        var fresh = recovery().prepare(fixture.deposit);
        assertThrows(IllegalArgumentException.class, () -> recovery().approve(UUID.randomUUID(), "operator-test", fresh,
                current(fresh, null), 10, 30_011, () -> true));
        assertEquals(1, count("mineral_recoveries"));
    }

    @Test void scopedPermitAllowsMiningUntilANewGapRevokesIt() throws Exception {
        var tracking = new OreTrackingRepository(fixture.db.getConnection());
        tracking.begin(1);
        tracking.markGap(2);
        tracking.begin(3);
        var plan = recovery().prepare(fixture.deposit);
        approve(plan, current(plan, null), UUID.randomUUID());
        assertFalse(tracking.load().orElseThrow().trusted(), "Global trust remains sticky-false after approval");
        assertTrue(recovery().permitted(fixture.deposit));

        assertEquals(7, fixture.mine(UUID.randomUUID(), 7, 1).quantity());
        var earnedGoods = fixture.db.getIndustryRepository().store(fixture.mine).orElseThrow().units();
        long reserve = fixture.reserve();
        long productionHistory = count("industry_production");

        tracking.markGap(4);
        assertFalse(recovery().permitted(fixture.deposit), "A new generation invalidates the old scoped permit");
        assertThrows(SQLException.class, () -> fixture.mine(UUID.randomUUID(), 7, 2));
        assertEquals(earnedGoods, fixture.db.getIndustryRepository().store(fixture.mine).orElseThrow().units());
        assertEquals(reserve, fixture.reserve());
        assertEquals(productionHistory, count("industry_production"));
    }

    @Test void schema94To95CreatesEmptyRecoveryTablesAndPreservesEveryPriorRow() throws Exception {
        var connection = fixture.db.getConnection();
        var expectedPrior = new TreeMap<>(snapshot());
        expectedPrior.remove("mineral_recoveries");
        expectedPrior.remove("resource_recovery_epochs");

        try (var s = connection.createStatement()) {
            s.execute("DROP TABLE mineral_recoveries");
            s.execute("DROP TABLE resource_recovery_epochs");
            s.execute("PRAGMA user_version=94");
        }
        new SchemaMigrator().migrate(connection);

        var after = new TreeMap<>(snapshot());
        assertEquals(List.of(), after.remove("mineral_recoveries"));
        assertEquals(List.of(), after.remove("resource_recovery_epochs"));
        assertEquals(expectedPrior, after, "Migration must preserve every schema-94 table and row value");
        try (var s = connection.createStatement(); var r = s.executeQuery("PRAGMA user_version")) {
            assertTrue(r.next());
            assertEquals(95, r.getInt(1));
        }
    }
}
