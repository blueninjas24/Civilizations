package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.economy.FarmObservation;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.JobRequirements;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ForestryFounderStudyTest {
    @TempDir Path directory;
    private BakeryInvestmentTest fixture;
    private UUID buyer = UUID.randomUUID(), buyerOwner = UUID.randomUUID();
    private Commodity furniture = new Commodity("forestry_study_fixture_output");
    private List<Position> trunks;
    private IndustryRuntimePolicy industry;
    private AutomaticForestryFounding opportunities;
    private EconomyRuntimeRepository economy;

    @BeforeEach void setup() throws Exception {
        fixture = new BakeryInvestmentTest();
        fixture.directory = directory;
        fixture.setup();
        fixture.db.getPopulationRepository().insertResident(new Resident(buyerOwner, "Timber customer", fixture.town, null, 300, Resident.Status.ALIVE));
        var recipe = new ProductionRecipe("forestry_study_timber_customer", "craftsman",
                Map.of(Commodity.TIMBER, 2L, Commodity.STONE, 1L), Map.of(furniture, 1L), 1, 1);
        var requirements = new JobRequirements(216, Map.of());
        var site = new BusinessSite(UUID.randomUUID(), fixture.town, fixture.world, 80, 40, 0, 1000, "Actual timber buyer");
        var business = new Business(buyer, fixture.town, "Funded timber customer", buyerOwner, buyerOwner,
                "craftsman", site.id(), 1, 0);
        fixture.db.getBusinessRepository().found(new BusinessFounding(business, site, requirements, requirements, 0,
                "Actual recipe-backed timber customer"));
        new TreasuryRepository(fixture.db.getConnection()).post(UUID.randomUUID(), fixture.town, 10_000,
                "Explicit fixture customer funding");
        fixture.db.getIndustryRepository().openStore(buyer, 1000,
                Map.of(Commodity.TIMBER, 1L, Commodity.STONE, 1L, furniture, 1L), 10_000);
        fixture.db.getBusinessRepository().hire(UUID.randomUUID(), buyer, buyerOwner, 0);
        fixture.sql("UPDATE economy_stocks SET units=50 WHERE commodity='timber'");
        fixture.sql("UPDATE economy_stocks SET units=30 WHERE commodity='stone'");

        trunks = java.util.stream.IntStream.range(0, 6).mapToObj(i -> new Position(20 + i, 40, 0)).toList();
        var base = IndustryPolicy.prototype();
        industry = new IndustryRuntimePolicy(true, 1000, 8,
                new IndustryPolicy(base.depositBatchesPerObservedOre(), base.batchesPerWorker(), base.siteBatchCapacity(),
                        base.storageCapacity(), base.localDeliveryPeriods(), base.localHaulUnitsPerPeriod(),
                        base.miningBundle(), base.storageSpacePerUnit()),
                Map.of(recipe.id(), recipe), new WorkshopSupplyPolicy(true, 1, 16,
                        Map.of(Commodity.TIMBER, 100L, Commodity.STONE, 100L)), WarehouseSupplyPolicy.disabled());
        new ForestryTrackingRepository(fixture.db.getConnection()).begin(1000);
        new ForestryTrackingRepository(fixture.db.getConnection()).finish(1500, true);
        opportunities = new AutomaticForestryFounding(fixture.db.getConnection(), ForestryFoundingPolicy.prototype(),
                ForestryPolicy.prototype(), industry, BusinessOperationPolicy.disabled(), StartupGrantPolicy.prototype(),
                216, 73, () -> true);
        assertTrue(opportunities.observe(fixture.town, 1, 0, evidence(2000)));

        economy = new EconomyRuntimeRepository(fixture.db.getConnection(), fixture.settings,
                FarmObservation.Policy.prototype(), 216);
        economy.configureStudy(StudyPolicy.prototype());
        economy.configureForestryFounderStudy(opportunities, CareerStudyPolicy.prototype(), ForestryFoundingPolicy.prototype());
        economy.startOnline(2000, Set.of(fixture.world));
        // The added customer owner must be included in an actual food-period assessment before startup funding is reviewed.
        economy.pulse(3000, false);
    }

    @AfterEach void close() { if (fixture != null) fixture.close(); }

    private ForestObservation evidence(long captured) { return new ForestObservation(fixture.world, captured, true, trunks); }

    private long scalar(String query) throws SQLException {
        try (var s = fixture.db.getConnection().createStatement(); var r = s.executeQuery(query)) {
            assertTrue(r.next()); return r.getLong(1);
        }
    }

    private long experience() throws SQLException {
        return fixture.db.getPopulationRepository().findWorkProfile(fixture.person).orElseThrow().experience().getOrDefault("forestry", 0L);
    }

    @Test void currentFundedOpportunityEnrolsGraduallyConsumesActualMaterialsAndQualifiesOwner() throws Exception {
        var enrolled = opportunities.review(fixture.town, 1, 3000, Set.of(fixture.world));
        assertTrue(enrolled.contains("One prospective forestry owner enrolled"), enrolled);
        assertEquals(1, scalar("SELECT COUNT(*) FROM forestry_founder_courses"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM forestry_founder_reviews"));
        assertEquals(0, experience(), "Enrollment itself grants no skill");

        for (int period = 1; period <= 10; period++) economy.pulse(3000 + period * 1000L, false);
        assertEquals(10, experience());
        assertEquals("COMPLETED", scalarString("SELECT status FROM resident_courses WHERE skill='forestry'"));
        assertEquals(20, scalar("SELECT SUM(units) FROM study_spending WHERE commodity='timber'"));
        assertEquals(10, scalar("SELECT SUM(units) FROM study_spending WHERE commodity='stone'"));
        assertEquals(30, scalar("SELECT units FROM economy_stocks WHERE commodity='timber'"));
        assertEquals(20, scalar("SELECT units FROM economy_stocks WHERE commodity='stone'"));

        String founded = opportunities.review(fixture.town, 2, 14000, Set.of(fixture.world));
        assertTrue(founded.startsWith("Founded one qualified logging business"), founded);
        assertEquals(1, scalar("SELECT COUNT(*) FROM forestry_resources"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM startup_grants WHERE business_id IN (SELECT id FROM businesses WHERE industry='logging')"));

        var removed = trunks.getFirst();
        var protectedTree = new Position(60, 45, 4);
        new ForestryProvenance(fixture.db.getConnection()).record(fixture.world, List.of(protectedTree), List.of(removed),
                15000, "Fixture source change after training");
        assertEquals(1, scalar("SELECT COUNT(*) FROM forestry_tracking"));
        assertEquals(2, scalar("SELECT COUNT(*) FROM forestry_exclusions"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM forestry_source_losses"));

        SchemaFixtures.rewind(fixture.db.getConnection(), 93);
        var before = snapshotPrior93();
        fixture.sql("CREATE TABLE forestry_founder_reviews (migration_blocker INTEGER)");
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(fixture.db.getConnection()));
        assertEquals(93, scalar("PRAGMA user_version"));
        assertEquals(before, snapshotPrior93());
        assertEquals(0, scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='forestry_founder_courses'"));
        fixture.sql("DROP TABLE forestry_founder_reviews");
        new SchemaMigrator().migrate(fixture.db.getConnection());
        assertEquals(SchemaMigrator.CURRENT_VERSION, scalar("PRAGMA user_version"));
        assertEquals(before, snapshotPrior93());
        assertEquals(0, scalar("SELECT COUNT(*) FROM forestry_founder_courses"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM forestry_founder_reviews"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM generated_resource_chunks"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM construction_drafts"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM construction_block_journal"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM construction_material_reservations"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM construction_material_items"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM construction_site_reservations"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM construction_material_returns"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM construction_supply_orders"));
        for(var table:java.util.List.of("building_records","building_units","building_consent_events","building_exclusions","building_exclusion_releases","dwelling_reviews","dwelling_review_units","dwelling_current_reviews","dwelling_assignments","dwelling_active_occupancy","dwelling_departures","household_capacity_sources","dwelling_bed_claims")) assertEquals(0,scalar("SELECT COUNT(*) FROM "+table));
        assertEquals(0, scalar("SELECT COUNT(*) FROM construction_stage_work"));assertEquals(0, scalar("SELECT COUNT(*) FROM construction_public_work"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM forestry_tracking"));
        assertEquals(2, scalar("SELECT COUNT(*) FROM forestry_exclusions"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM forestry_source_losses"));
        try (var s = fixture.db.getConnection().createStatement(); var r = s.executeQuery("PRAGMA foreign_key_check")) {
            assertFalse(r.next());
        }
    }

    @Test void currentCanopyLossAndWorldAbsenceStopStudyBeforeAnyFurtherAward() throws Exception {
        assertTrue(opportunities.review(fixture.town, 1, 3000, Set.of(fixture.world)).contains("One prospective forestry owner enrolled"));
        economy.pulse(4000, false);
        economy.pulse(5000, false);
        assertEquals(2, experience());
        assertTrue(opportunities.observe(fixture.town, 1, 0, new ForestObservation(fixture.world, 5000, true, List.of())));
        economy.pulse(6000, false);
        assertEquals(2, experience(), "A complete empty replacement frame cancels rather than extends the linked opportunity");
        assertEquals("CANCELLED", scalarString("SELECT status FROM resident_courses WHERE skill='forestry'"));
        assertEquals(6, scalar("SELECT SUM(units) FROM study_spending"));

        // A new opportunity can enroll, but a world-absence update makes its study ineligible immediately.
        assertTrue(opportunities.observe(fixture.town, 1, 0, evidence(6000)));
        assertTrue(opportunities.review(fixture.town, 2, 6000, Set.of(fixture.world)).contains("One prospective forestry owner enrolled"));
        economy.updateLoadedWorlds(6000, Set.of());
        economy.pulse(7000, false);
        assertEquals(2, experience());
        assertEquals(1, scalar("SELECT COUNT(*) FROM resident_courses WHERE skill='forestry' AND status='CANCELLED'"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM resident_courses WHERE skill='forestry' AND status='ACTIVE'"));
        assertEquals(6, scalar("SELECT SUM(units) FROM study_spending"));

        // A long offline-world interval pauses the active study. Fresh evidence and a restored world
        // resume from the saved backlog without awarding periods from the absence interval.
        economy.updateLoadedWorlds(200_000, Set.of());
        assertTrue(opportunities.observe(fixture.town, 1, 0, evidence(200_000)));
        economy.updateLoadedWorlds(200_000, Set.of(fixture.world));
        economy.pulse(200_000, false);
        assertEquals(2, experience());
        assertEquals(6, scalar("SELECT SUM(units) FROM study_spending"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM resident_courses WHERE skill='forestry' AND status='ACTIVE'"));
    }

    @Test void restartWithoutCachedForestFramesWaitsUntilFreshEvidenceReturns() throws Exception {
        assertTrue(opportunities.review(fixture.town, 1, 3000, Set.of(fixture.world))
                .contains("One prospective forestry owner enrolled"));
        economy.pulse(4000, false);
        assertEquals(1, experience());
        assertEquals(3, scalar("SELECT SUM(units) FROM study_spending"));

        // Recreate both runtime services over the same persistent database, with no observer cache.
        opportunities = new AutomaticForestryFounding(fixture.db.getConnection(), ForestryFoundingPolicy.prototype(),
                ForestryPolicy.prototype(), industry, BusinessOperationPolicy.disabled(), StartupGrantPolicy.prototype(),
                216, 73, () -> true);
        economy = new EconomyRuntimeRepository(fixture.db.getConnection(), fixture.settings,
                FarmObservation.Policy.prototype(), 216);
        economy.configureStudy(StudyPolicy.prototype());
        economy.configureForestryFounderStudy(opportunities, CareerStudyPolicy.prototype(), ForestryFoundingPolicy.prototype());
        economy.startOnline(100_000, Set.of(fixture.world));
        economy.pulse(101_000, false);
        assertEquals("ACTIVE", scalarString("SELECT status FROM resident_courses WHERE skill='forestry'"));
        assertEquals(1, experience(), "Missing transient cache after restart must wait, not cancel or award");
        assertEquals(3, scalar("SELECT SUM(units) FROM study_spending"));

        assertTrue(opportunities.observe(fixture.town, 1, 0, evidence(101_000)));
        economy.pulse(102_000, false);
        assertEquals(2, experience());
        assertEquals(6, scalar("SELECT SUM(units) FROM study_spending"));
        assertEquals("ACTIVE", scalarString("SELECT status FROM resident_courses WHERE skill='forestry'"));
    }

    private String scalarString(String query) throws SQLException {
        try (var s = fixture.db.getConnection().createStatement(); var r = s.executeQuery(query)) {
            assertTrue(r.next()); return r.getString(1);
        }
    }

    /** Compare every schema93 row; exclude only later additive tables. */
    private Map<String, List<List<String>>> snapshotPrior93() throws SQLException {
        var state = new TreeMap<String, List<List<String>>>();
        var names = new ArrayList<String>();
        try (var s = fixture.db.getConnection().createStatement(); var r = s.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                        "AND name NOT IN ('forestry_founder_courses','forestry_founder_reviews','resource_recovery_epochs','mineral_recoveries','forest_recoveries','business_allocation_reviews','business_allocation_state','resource_investment_reviews','resource_investment_state','generated_resource_chunks','construction_drafts','construction_block_journal','construction_material_reservations','construction_material_items','construction_site_reservations','construction_material_returns','construction_supply_orders','construction_stage_work','construction_public_work','building_records','building_units','building_consent_events','building_exclusions','building_exclusion_releases','dwelling_reviews','dwelling_review_units','dwelling_current_reviews','dwelling_assignments','dwelling_active_occupancy','dwelling_departures','household_capacity_sources','dwelling_bed_claims') ORDER BY name")) {
            while (r.next()) names.add(r.getString(1));
        }
        for (var name : names) {
            var rows = new ArrayList<List<String>>();
            try (var s = fixture.db.getConnection().createStatement(); var r = s.executeQuery("SELECT * FROM \"" + name + "\"")) {
                while (r.next()) {
                    var row = new ArrayList<String>();
                    for (int column = 1; column <= r.getMetaData().getColumnCount(); column++)
                        row.add(Objects.toString(r.getObject(column), null));
                    rows.add(Collections.unmodifiableList(row));
                }
            }
            rows.sort(Comparator.comparing(Object::toString));
            state.put(name, List.copyOf(rows));
        }
        return state;
    }
}
