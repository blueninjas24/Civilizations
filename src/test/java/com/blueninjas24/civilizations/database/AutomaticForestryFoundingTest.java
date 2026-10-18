package com.blueninjas24.civilizations.database;

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

class AutomaticForestryFoundingTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID town = UUID.randomUUID(), world = UUID.randomUUID();
    final UUID owner = UUID.randomUUID(), customerOwner = UUID.randomUUID(), customer = UUID.randomUUID();
    final Commodity output = new Commodity("fixture_output");
    final Map<Commodity, Long> sizes = Map.of(Commodity.TIMBER, 1L, Commodity.COAL, 1L,
            Commodity.FOOD, 1L, output, 1L);
    final List<VillageObservation.Position> trunks = java.util.stream.IntStream.range(0, 6)
            .mapToObj(i -> new VillageObservation.Position(20 + i, 40, 0)).toList();
    IndustryRuntimePolicy industry;

    @BeforeEach void setup() throws Exception {
        db = new DatabaseManager(directory.toFile(), Logger.getLogger("AutomaticForestryFoundingTest"));
        db.initialize();
        db.saveSettlement(new Settlement(town, "Fixture", "Fixture", 0, 64, 0, 1, 0, 0));
        db.getVillageIdentityRepository().observeWorld(world, "Fixture");
        db.getVillageIdentityRepository().bindSettlement(town, world);
        resident(owner); resident(customerOwner); resident(customer);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(), town, 10_000, "Founding fixture funds");
        var bands = new EnumMap<Condition, Integer>(Condition.class);
        for (var condition : Condition.values()) bands.put(condition, 100);
        var settings = new EconomySettings(1, 3, 2, 2, 1, 4, 100,
                new Policy(5000, 12500, 20000, 3), Map.of(Commodity.FOOD, 100L),
                Map.of(Commodity.FOOD, BigDecimal.ONE), bands);
        var economy = new EconomyRuntimeRepository(db.getConnection(), settings, FarmObservation.Policy.prototype(), 216);
        economy.observe(new FarmObservation(town, world, 1000, true, 0), 1000);
        economy.startOnline(1000, Set.of(world));
        economy.pulse(2000, false);
        addTimberCustomer();
        var recipe = new ProductionRecipe("fixture_timber_craft", "craftsman", Map.of(Commodity.TIMBER, 3L),
                Map.of(output, 1L), 1, 4);
        industry = new IndustryRuntimePolicy(true, 1000, 8,
                new IndustryPolicy(10, 1, 8, 256, 1, 128, Map.of(Commodity.COAL, 1L), sizes),
                Map.of(recipe.id(), recipe), new WorkshopSupplyPolicy(true, 1, 16, Map.of(Commodity.TIMBER, 100L)),
                WarehouseSupplyPolicy.disabled(), ToolUpkeepPolicy.disabled());
    }

    @AfterEach void close() { if (db != null) db.close(); }

    private void resident(UUID id) throws Exception {
        db.getPopulationRepository().insertResident(new Resident(id, "Forestry fixture resident", town, null, 300, Resident.Status.ALIVE));
    }

    private UUID addTimberCustomer() throws Exception {
        var site = new BusinessSite(UUID.randomUUID(), town, world, 50, 40, 0, 1000, "Funded timber customer");
        var requirements = new JobRequirements(216, Map.of());
        var business = new Business(customer, town, "Fixture timber buyer", customerOwner, customerOwner, "craftsman", site.id(), 1, 0);
        db.getBusinessRepository().found(new BusinessFounding(business, site, requirements, requirements, 0, "Funded customer fixture"));
        db.getIndustryRepository().openStore(customer, 100, sizes, 1_000);
        db.getBusinessRepository().hire(UUID.randomUUID(), customer, customerOwner, 0);
        return customer;
    }

    private AutomaticForestryFounding founding() {
        return new AutomaticForestryFounding(db.getConnection(), ForestryFoundingPolicy.prototype(), ForestryPolicy.prototype(),
                industry, BusinessOperationPolicy.disabled(), StartupGrantPolicy.prototype(), 216, 8128L, () -> true);
    }

    private ForestObservation evidence(long captured) { return new ForestObservation(world, captured, true, trunks); }

    private void qualifyFounder() throws Exception { db.getPopulationRepository().recordSkillExperience(owner, "forestry", 10); }

    private long scalar(String query) throws Exception {
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery(query)) { assertTrue(r.next()); return r.getLong(1); }
    }

    private long totalFunds() throws Exception {
        long total = new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery("SELECT COALESCE(SUM(hundredths),0) FROM industry_stores")) {
            assertTrue(r.next()); return Math.addExact(total, r.getLong(1));
        }
    }

    @Test void wrongScopeAndConflictingSameTimeEvidenceCannotReopenUntilFreshEvidence() throws Exception {
        qualifyFounder();var manager=founding();
        assertFalse(manager.observe(town,0,0,evidence(2000)));
        assertTrue(manager.observe(town,1,0,evidence(2000)));
        var changed=new ArrayList<>(trunks);changed.set(0,new VillageObservation.Position(19,40,0));
        assertFalse(manager.observe(town,1,0,new ForestObservation(world,2000,true,changed)));
        assertFalse(manager.observe(town,1,0,evidence(2000)),"A conflicting timestamp stays invalid on replay");
        assertTrue(manager.review(town,1,2000,Set.of(world)).contains("No current complete"));
        assertTrue(manager.observe(town,1,0,new ForestObservation(world,2001,true,List.of())));
        assertFalse(manager.observe(town,1,0,evidence(2000)));
        assertFalse(manager.review(town,2,2001,Set.of(world)).startsWith("Founded"));
        assertTrue(manager.observe(town,1,0,evidence(2002)));
        assertTrue(manager.review(town,3,2002,Set.of(world)).startsWith("Founded"));
    }

    @Test void canopyMutationAfterSnapshotRequiresANewerGeometryCapture() throws Exception {
        qualifyFounder();var manager=founding();assertTrue(manager.observe(town,1,0,evidence(2000)));
        new ForestryProvenance(db.getConnection()).record(world,List.of(),List.of(new VillageObservation.Position(20,41,1)),2001,"Canopy removed after snapshot");
        assertFalse(manager.review(town,1,2001,Set.of(world)).startsWith("Founded"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM forestry_resources"));
        assertTrue(manager.observe(town,1,0,evidence(2002)));
        assertTrue(manager.review(town,2,2002,Set.of(world)).startsWith("Founded"));
    }

    @Test void freshCanopyAndFundedTimberCustomerEnableOnePaidQualifiedFounding() throws Exception {
        qualifyFounder();
        var manager = founding();
        assertTrue(manager.observe(town, 1, 0, evidence(2000)));
        long fundsBefore = totalFunds();
        String result = manager.review(town, 1, 2000, Set.of(world));
        assertTrue(result.startsWith("Founded one qualified logging business"), result);
        assertEquals(1, scalar("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM forestry_resources"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM startup_grants"));
        assertEquals(0, scalar("SELECT COALESCE(SUM(units),0) FROM industry_stock WHERE commodity='timber' AND business_id IN (SELECT id FROM businesses WHERE industry='logging')"));
        assertEquals(96, scalar("SELECT available FROM forestry_resources"));
        assertEquals(fundsBefore - 500, totalFunds(), "The 500 startup component is a real spending sink; 2,000 capital moves into the new store");
        assertEquals(6_500, new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
        assertTrue(db.getIndustryRepository().store(UUID.fromString(value("SELECT id FROM businesses WHERE industry='logging'"))).orElseThrow().hundredths() >= 2_000);
        assertTrue(manager.review(town, 2, 2001, Set.of(world)).contains("Existing staffed logging stock"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));
    }

    @Test void unqualifiedFounderStaleOrAbsentEvidenceCannotCreateAssetsAndOverlapRetriesAtomically() throws Exception {
        var manager = founding();
        assertTrue(manager.observe(town, 1, 0, evidence(2000)));
        assertTrue(manager.review(town, 1, 2000, Set.of(world)).contains("no qualified unemployed founder"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));

        qualifyFounder();
        assertTrue(manager.review(town, 2, 123000, Set.of(world)).contains("No current complete loaded-world forest evidence"));
        assertTrue(manager.observe(town, 1, 0, evidence(123000)));
        assertFalse(manager.review(town, 3, 123000, Set.of()).startsWith("Founded"), "An absent world cannot supply a customer or forest");
        assertEquals(0, scalar("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM forestry_resources"));

        assertTrue(manager.observe(town, 1, 0, evidence(124000)));
        assertFalse(manager.observe(town, 0, 0, evidence(124000)), "Wrong chunk scope must reject overlapping evidence");
        db.getPopulationRepository().recordSkillExperience(owner, "forestry", 10);
        long fundsBefore = totalFunds();
        try (var s = db.getConnection().createStatement()) {
            s.execute("CREATE TRIGGER reject_auto_forestry_grant BEFORE INSERT ON startup_grants BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        }
        assertThrows(SQLException.class, () -> manager.review(town, 4, 124000, Set.of(world)));
        assertEquals(0, scalar("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM forestry_resources"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM forestry_supports"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM startup_grants"));
        assertEquals(fundsBefore, totalFunds());
        try (var s = db.getConnection().createStatement()) { s.execute("DROP TRIGGER reject_auto_forestry_grant"); }
        String founded = manager.review(town, 4, 124000, Set.of(world));
        assertTrue(founded.startsWith("Founded one qualified logging business"), founded);
        assertEquals(1, scalar("SELECT COUNT(*) FROM businesses WHERE industry='logging'"), "Overlapping frames still found at most one site");
        assertEquals(1, scalar("SELECT COUNT(*) FROM forestry_resources"));
        assertEquals(fundsBefore - 500, totalFunds());
    }

    @Test void missingProtectedStartupCashAndProtectedCanopyCannotCreateAssets() throws Exception {
        qualifyFounder();
        var manager = founding();
        assertTrue(manager.observe(town, 1, 0, evidence(2000)));
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(), town, -8_500, "Fixture removes startup liquidity");
        assertFalse(manager.review(town, 1, 2000, Set.of(world)).startsWith("Founded one qualified logging business"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM forestry_resources"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM startup_grants"));

        // Restore only approved fixture cash, then protect each sampled tree coordinate.
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(), town, 8_500, "Fixture restores startup liquidity");
        new ForestryProvenance(db.getConnection()).record(world, trunks, List.of(), 2000, "Fixture protected planted grove");
        assertFalse(manager.review(town, 2, 2000, Set.of(world)).startsWith("Founded"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM forestry_resources"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM forestry_supports"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM startup_grants"));
    }

    private String value(String query) throws Exception {
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery(query)) { assertTrue(r.next()); return r.getString(1); }
    }
}
