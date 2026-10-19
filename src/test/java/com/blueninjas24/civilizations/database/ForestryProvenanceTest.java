package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.population.JobRequirements;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ForestryProvenanceTest {
    @TempDir Path directory;
    private ForestryPersistenceTest fixture;
    private List<Position> trees;

    @BeforeEach void setup() throws Exception {
        fixture = new ForestryPersistenceTest();
        fixture.directory = directory;
        fixture.setup();
        trees = fixture.trees;
    }

    @AfterEach void close() {
        if (fixture != null && fixture.data != null && fixture.data.db != null) fixture.data.db.close();
    }

    @Test void cleanRestartKeepsTrustButGapIsSticky() throws Exception {
        var tracking = new ForestryTrackingRepository(fixture.data.db.getConnection());
        var first = tracking.begin(100);
        assertFalse(first.cleanStop());
        assertTrue(first.trusted());
        assertEquals(new ForestryTrackingRepository.State(true, true, 200), tracking.finish(200, true));
        assertEquals(new ForestryTrackingRepository.State(false, true, 300), tracking.begin(300));
        assertEquals(new ForestryTrackingRepository.State(true, true, 400), tracking.finish(400, true));

        var gap = tracking.markGap(500);
        assertEquals(new ForestryTrackingRepository.State(false, false, 500), gap);
        assertEquals(new ForestryTrackingRepository.State(true, false, 600), tracking.finish(600, true));
        assertEquals(new ForestryTrackingRepository.State(false, false, 700), tracking.begin(700));

    }

    @Test void firstTrackingRunDoesNotSilentlyCertifyLegacySavedForests() throws Exception {
        fixture.start();
        var legacy = new ForestryTrackingRepository(fixture.data.db.getConnection()).begin(800);
        assertFalse(legacy.trusted());
        fixture.data.db.close();
        fixture.data.open();
        var reopened = new ForestryTrackingRepository(fixture.data.db.getConnection());
        assertEquals(new ForestryTrackingRepository.State(false, false, 800), reopened.load().orElseThrow());
        assertFalse(reopened.finish(900, true).trusted());
    }

    @Test void untrustedProvenanceRejectsNewDirectWorkButAllowsExactSavedReceiptReplay() throws Exception {
        fixture.start();
        var industry = fixture.data.db.getIndustryRepository();
        UUID existingOperation = UUID.randomUUID();
        var earned = industry.log(existingOperation, fixture.firm(), 4, 1, ForestryPolicy.prototype());
        assertEquals(4, earned.quantity());

        var tracker = new ForestryTrackingRepository(fixture.data.db.getConnection());
        assertFalse(tracker.begin(20).trusted(), "First tracking cannot certify the already-saved forest");
        assertFalse(tracker.markGap(21).trusted(), "A detected gap keeps the provenance fence closed");
        assertEquals(earned, industry.log(existingOperation, fixture.firm(), 4, 1, ForestryPolicy.prototype()),
                "An exact old receipt remains replayable even after provenance becomes untrusted and a gap is recorded");
        assertThrows(SQLException.class,
                () -> industry.log(UUID.randomUUID(), fixture.firm(), 4, 2, ForestryPolicy.prototype()));
        assertEquals(4, industry.store(fixture.firm()).orElseThrow().units().getOrDefault(Commodity.TIMBER, 0L));
        assertEquals(1, fixture.data.count("forestry_work"));
        assertEquals(1, fixture.data.count("industry_production"));
    }

    @Test void unhealthyRuntimePreservesEarnedPendingTimberWorkAndDoesNotBackfillPause() throws Exception {
        var tracking = new ForestryTrackingRepository(fixture.data.db.getConnection());
        tracking.begin(900);
        assertTrue(tracking.finish(1000, true).trusted());
        fixture.start();

        // A separate ordinary craftsman clock keeps advancing while forestry is paused.
        UUID unrelatedWorker = UUID.randomUUID(), unrelatedBusiness = UUID.randomUUID();
        fixture.data.db.getPopulationRepository().insertResident(
                new Resident(unrelatedWorker, "Unrelated craftsman", fixture.data.town, null, 300, Resident.Status.ALIVE));
        var site = new BusinessSite(UUID.randomUUID(), fixture.data.town, fixture.data.world, 120, 40, 0, 1000,
                "Independent craftsman clock fixture");
        var requirements = new JobRequirements(216, Map.of());
        var business = new Business(unrelatedBusiness, fixture.data.town, "Independent craftsman", unrelatedWorker,
                unrelatedWorker, "craftsman", site.id(), 1, 0);
        fixture.data.db.getBusinessRepository().found(new BusinessFounding(business, site, requirements,
                requirements, 0, "Independent craftsman runtime fixture"));
        fixture.data.db.getIndustryRepository().openStore(unrelatedBusiness, 100,
                Map.of(Commodity.FOOD, 1L, new Commodity("fixture_output"), 1L), 1000);
        fixture.data.db.getBusinessRepository().hire(UUID.randomUUID(), unrelatedBusiness, unrelatedWorker, 0);

        var recipe = new ProductionRecipe("unrelated_clock", "craftsman", Map.of(Commodity.FOOD, 1L),
                Map.of(new Commodity("fixture_output"), 1L), 1, 1);
        var runtime = new IndustryRuntime(fixture.data.db.getConnection(), new IndustryRuntimePolicy(true, 1000, 8,
                IndustryPolicy.prototype(), Map.of(recipe.id(), recipe)));
        var healthy = new AtomicBoolean(true);
        runtime.configureForestry(ForestryPolicy.prototype(), healthy::get);
        runtime.start(1000);

        runtime.observeAvailability(2000, Set.of(fixture.data.world)); // Earn a forestry period before the fence closes.
        healthy.set(false);
        assertEquals(1, runtime.pulse(3000, Set.of(fixture.data.world)));
        assertEquals(0, fixture.data.count("forestry_work"));
        assertEquals(96, fixture.forests().find(fixture.site()).orElseThrow().stock().available());
        assertEquals(1, fixture.data.count("industry_production"), "The unrelated business still works during the fence");

        healthy.set(true);
        assertEquals(2, runtime.pulse(3000, Set.of(fixture.data.world)));
        assertEquals(1, fixture.data.count("forestry_work"), "Previously earned logging work resumes without being discarded");
        assertEquals(92, fixture.forests().find(fixture.site()).orElseThrow().stock().available());

        healthy.set(false);
        runtime.pulse(4000, Set.of(fixture.data.world));
        runtime.pulse(5000, Set.of(fixture.data.world));
        healthy.set(true);
        assertEquals(0, runtime.pulse(5000, Set.of(fixture.data.world)), "The fenced outage must not backfill work");
        assertEquals(1, fixture.data.count("forestry_work"));
        assertEquals(4, scalar("SELECT COUNT(*) FROM industry_production WHERE business_id='"+unrelatedBusiness+"'"),
                "The unrelated work clock continues through forestry-only pauses");
        assertEquals(2, runtime.pulse(6000, Set.of(fixture.data.world)));
        assertEquals(2, fixture.data.count("forestry_work"));
        assertEquals(88, fixture.forests().find(fixture.site()).orElseThrow().stock().available());
    }

    @Test void exclusionsAndNamedSourceLossReplayAtomicallyWithoutRefillingOrErasingEarnedGoods() throws Exception {
        fixture.start();
        UUID operation = UUID.randomUUID();
        var work = fixture.data.db.getIndustryRepository().log(operation, fixture.firm(), 4, 1, ForestryPolicy.prototype());
        assertEquals(4, work.quantity());
        var repository = fixture.forests();
        var provenance = new ForestryProvenance(fixture.data.db.getConnection());
        var initial = repository.find(fixture.site()).orElseThrow();
        assertEquals(92, initial.stock().available());

        var placed = new Position(30, 42, 2);
        var placement = provenance.record(fixture.data.world, List.of(placed), List.of(), 20, "Newly observed planted tree");
        assertEquals(new ForestryProvenance.Result(1, 0, 0), placement);
        assertTrue(provenance.excludes(fixture.data.world, placed));
        assertTrue(provenance.aroundChunk(fixture.data.world, 1, 0, 8, 32).contains(placed));
        assertEquals(initial, repository.find(fixture.site()).orElseThrow());
        assertEquals(new ForestryProvenance.Result(0, 0, 0),
                provenance.record(fixture.data.world, List.of(placed), List.of(), 21, "Replay planted tree"));

        var removed = trees.getFirst();
        fixture.data.sql("CREATE TRIGGER reject_forest_loss BEFORE INSERT ON forestry_habitat_changes BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class, () -> provenance.record(fixture.data.world, List.of(), List.of(removed), 30, "Storm removed support"));
        assertEquals(initial, repository.find(fixture.site()).orElseThrow(), "The stock reduction must roll back with its audit");
        assertFalse(provenance.excludes(fixture.data.world, removed), "The protective exclusion must roll back atomically");
        assertEquals(0, fixture.data.count("forestry_source_losses"));
        assertEquals(0, fixture.data.count("forestry_habitat_changes"));
        assertEquals(4, fixture.data.db.getIndustryRepository().store(fixture.firm()).orElseThrow().units().get(Commodity.TIMBER));

        fixture.data.sql("DROP TRIGGER reject_forest_loss");
        var loss = provenance.record(fixture.data.world, List.of(), List.of(removed), 30, "Storm removed support");
        assertEquals(new ForestryProvenance.Result(1, 1, 12), loss);
        assertTrue(provenance.excludes(fixture.data.world, removed));
        var stockAfterLoss = repository.find(fixture.site()).orElseThrow();
        assertEquals(new ForestStock(80, 80, 0), stockAfterLoss.stock());
        assertEquals(4, fixture.data.db.getIndustryRepository().store(fixture.firm()).orElseThrow().units().get(Commodity.TIMBER),
                "Earned timber goods remain owned after standing-source loss");
        assertEquals("Storm removed support", scalarString("SELECT reason FROM forestry_source_losses WHERE world_id='"+
                fixture.data.world+"' AND x="+removed.x()+" AND y="+removed.y()+" AND z="+removed.z()));
        assertEquals(new ForestryProvenance.Result(0, 0, 0),
                provenance.record(fixture.data.world, List.of(), List.of(removed), 30, "Storm removed support"));
        assertEquals(stockAfterLoss, repository.open(fixture.site(), fixture.evidence(), ForestryPolicy.disabled()),
                "Reopening the original footprint must not restore lost standing stock");
        assertEquals(stockAfterLoss, repository.find(fixture.site()).orElseThrow());
        assertEquals(1, fixture.data.count("forestry_source_losses"));
        assertEquals(1, fixture.data.count("forestry_habitat_changes"));

        var allSuitable = new TreeMap<Position, Boolean>(Position.ORDER);
        trees.forEach(position -> allSuitable.put(position, true));
        assertThrows(SQLException.class, () -> repository.habitat(UUID.randomUUID(), fixture.site(), fixture.data.world,
                31, allSuitable), "Fresh positive habitat evidence cannot restore a retired source coordinate");

        var other = fixture.data.producer("logging", fixture.data.person, 25, 1000);
        fixture.data.db.getBusinessRepository().found(other.founding());
        assertThrows(SQLException.class, () -> repository.open(other.founding().site().id(),
                new ForestObservation(fixture.data.world, 32, true, trees), ForestryPolicy.prototype()),
                "A new site cannot claim a coordinate protected by the source-loss history");
    }

    @Test void populatedSchema92UpgradePreservesEveryOldRowIncludingForestryRowsAcrossFailureAndRetry() throws Exception {
        fixture.start();
        fixture.data.db.getIndustryRepository().log(UUID.randomUUID(), fixture.firm(), 4, 1, ForestryPolicy.prototype());
        var verdicts = new TreeMap<Position, Boolean>(Position.ORDER);
        trees.forEach(position -> verdicts.put(position, !position.equals(trees.getFirst())));
        fixture.forests().habitat(UUID.randomUUID(), fixture.site(), fixture.data.world, 20, verdicts);
        assertEquals(1, fixture.data.count("forestry_resources"));
        assertEquals(trees.size(), fixture.data.count("forestry_supports"));
        assertEquals(1, fixture.data.count("forestry_work"));
        assertEquals(1, fixture.data.count("forestry_habitat_changes"));

        SchemaFixtures.rewind(fixture.data.db.getConnection(), 92);
        var before = snapshotIncludingPriorForestry();
        fixture.data.sql("CREATE TABLE forestry_tracking(blocker INTEGER)");
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(fixture.data.db.getConnection()));
        assertEquals(92, scalar("PRAGMA user_version"));
        assertEquals(before, snapshotIncludingPriorForestry());
        assertEquals(1, scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='forestry_tracking'"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='forestry_exclusions'"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='forestry_source_losses'"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='forestry_founder_reviews'"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='forestry_founder_courses'"));

        fixture.data.sql("DROP TABLE forestry_tracking");
        new SchemaMigrator().migrate(fixture.data.db.getConnection());
        assertEquals(SchemaMigrator.CURRENT_VERSION, scalar("PRAGMA user_version"));
        assertEquals(before, snapshotIncludingPriorForestry());
        for (var table : List.of("forestry_tracking", "forestry_exclusions", "forestry_source_losses","forestry_founder_reviews","forestry_founder_courses"))
            assertEquals(0, fixture.data.count(table), table);
        try (var statement = fixture.data.db.getConnection().createStatement();
             var result = statement.executeQuery("PRAGMA foreign_key_check")) {
            assertFalse(result.next());
        }
    }

    private long scalar(String query) throws SQLException {
        try (var statement = fixture.data.db.getConnection().createStatement(); var result = statement.executeQuery(query)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private String scalarString(String query) throws SQLException {
        try (var statement = fixture.data.db.getConnection().createStatement(); var result = statement.executeQuery(query)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    /** Exclude only additions after schema92; every schema92 table, including forestry, remains compared. */
    private Map<String, List<List<String>>> snapshotIncludingPriorForestry() throws SQLException {
        var result = new TreeMap<String, List<List<String>>>();
        var names = new ArrayList<String>();
        try (var statement = fixture.data.db.getConnection().createStatement();
             var rows = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                     "AND name NOT IN ('forestry_tracking','forestry_exclusions','forestry_source_losses','forestry_founder_reviews','forestry_founder_courses','resource_recovery_epochs','mineral_recoveries','forest_recoveries','business_allocation_reviews','business_allocation_state','resource_investment_reviews','resource_investment_state') ORDER BY name")) {
            while (rows.next()) names.add(rows.getString(1));
        }
        for (var name : names) {
            var tableRows = new ArrayList<List<String>>();
            try (var statement = fixture.data.db.getConnection().createStatement();
                 var rows = statement.executeQuery("SELECT * FROM \"" + name + "\"")) {
                while (rows.next()) {
                    var row = new ArrayList<String>();
                    for (int column = 1; column <= rows.getMetaData().getColumnCount(); column++)
                        row.add(Objects.toString(rows.getObject(column), null));
                    tableRows.add(Collections.unmodifiableList(row));
                }
            }
            tableRows.sort(Comparator.comparing(Object::toString));
            result.put(name, List.copyOf(tableRows));
        }
        return result;
    }
}
