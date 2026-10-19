package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedForestryFoundingTest {
    @TempDir Path directory;
    private AutomaticForestryFoundingTest fixture;
    private ForestryTrackingRepository tracking;

    @BeforeEach void setup() throws Exception {
        fixture = new AutomaticForestryFoundingTest();
        fixture.directory = directory;
        fixture.setup();
        fixture.db.getPopulationRepository().recordSkillExperience(fixture.owner, "forestry", 10);
        tracking = new ForestryTrackingRepository(fixture.db.getConnection());
        if (tracking.load().isEmpty()) tracking.begin(1_000);
        tracking.markGap(2_100);
        tracking.begin(2_200);
        assertFalse(tracking.load().orElseThrow().trusted());
        assertTrue(new ResourceRecoveryEpochs(fixture.db.getConnection()).load("forest").orElseThrow().healthy());
    }

    @AfterEach void close() { if (fixture != null) fixture.close(); }

    @Test void onlyCurrentGeneratedFrameCanOpenLoggingAfterGlobalGap() throws Exception {
        var connection = fixture.db.getConnection();
        var recovery = new ResourceRecoveryEpochs(connection).load("forest").orElseThrow();
        var terrain = new GeneratedTerrainRepository(connection);
        assertTrue(terrain.recordGenerated("forest", fixture.world, 1, 0, recovery.generation(), 2_300));

        var manager = manager(() -> true);
        assertTrue(manager.observe(fixture.town, 1, 0, evidence(2_250)));
        long before = totalFunds();
        assertFalse(manager.review(fixture.town, 1, 2_300, Set.of(fixture.world)).startsWith("Founded"),
                "A certificate recorded after the frame cannot establish its origin");
        assertEquals(0, count("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));
        assertEquals(before, totalFunds());

        assertTrue(manager.observe(fixture.town, 1, 0, evidence(2_400)));
        var paused = manager(() -> false);
        assertTrue(paused.observe(fixture.town, 1, 0, evidence(2_400)));
        assertTrue(paused.review(fixture.town, 1, 2_400, Set.of(fixture.world)).contains("live provenance trust guard"));
        assertEquals(0, count("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));
        assertEquals(before, totalFunds());

        String result = manager.review(fixture.town, 1, 2_400, Set.of(fixture.world));
        assertTrue(result.startsWith("Founded one qualified logging business"), result);
        assertEquals(1, count("SELECT COUNT(*) FROM businesses WHERE industry='logging'"));
        assertEquals(1, count("SELECT COUNT(*) FROM forestry_resources"));
        assertEquals(before - 500, totalFunds());
    }

    private AutomaticForestryFounding manager(java.util.function.BooleanSupplier currentCapture) {
        return new AutomaticForestryFounding(fixture.db.getConnection(), ForestryFoundingPolicy.prototype(),
                ForestryPolicy.prototype(), fixture.industry, BusinessOperationPolicy.disabled(),
                StartupGrantPolicy.prototype(), 216, 8128L, currentCapture);
    }

    private ForestObservation evidence(long observed) {
        return new ForestObservation(fixture.world, observed, true, fixture.trunks);
    }

    private long count(String sql) throws Exception {
        try (var statement = fixture.db.getConnection().createStatement(); var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private long totalFunds() throws Exception {
        long total = new TreasuryRepository(fixture.db.getConnection()).load(fixture.town).orElseThrow().hundredths();
        try (var statement = fixture.db.getConnection().createStatement();
             var result = statement.executeQuery("SELECT COALESCE(SUM(hundredths),0) FROM industry_stores")) {
            assertTrue(result.next());
            return Math.addExact(total, result.getLong(1));
        }
    }
}
