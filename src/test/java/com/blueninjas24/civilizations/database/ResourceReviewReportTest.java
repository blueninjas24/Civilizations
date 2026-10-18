package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ResourceReviewReportTest {
    @TempDir Path directory;

    @Test void oreReportShowsSavedSharesRetirementsGapAndBoundedChunkScopesWithoutWrites() throws Exception {
        var fixture = new MineralSourcesTest();
        fixture.directory = Files.createDirectories(directory.resolve("ore"));
        fixture.setup();
        try {
            var tracking = new OreTrackingRepository(fixture.db.getConnection());
            tracking.begin(2);
            fixture.sources().remove(fixture.world, List.of(fixture.coordinates.getFirst()), 3, "Reviewed report fixture");
            var far = new ArrayList<Position>();
            for (int chunk = 1; chunk <= 35; chunk++) far.add(new Position(chunk * 256, 40, 0));
            fixture.sources().remove(fixture.world, far, 4, "Report scope fixture exclusions");
            var placements = new OrePlacementRepository(fixture.db.getConnection());
            placements.record(fixture.world, List.of(far.getFirst()), 4); // Same coordinate is deduplicated across exclusion tables.
            var placementOnly = new Position(100_000, 40, 0);
            placements.record(fixture.world, List.of(placementOnly), 4);
            var active = new ResourceReviewReport(fixture.db.getConnection()).read(Set.of(fixture.world));
            assertTrue(active.oreTracking().trusted());
            assertFalse(active.oreTracking().cleanStop(), "An unclean-stop bit can indicate capture is currently active");
            assertTrue(active.oreTracking().describe().contains("trusted"));
            tracking.markGap(5);

            var before = databaseSnapshot(fixture.db.getConnection());
            var report = new ResourceReviewReport(fixture.db.getConnection()).snapshot(Set.of(fixture.world));
            assertEquals(before, databaseSnapshot(fixture.db.getConnection()), "The report must not write any saved table");
            assertFalse(report.oreTracking().trusted());
            assertFalse(report.oreTracking().cleanStop());
            assertFalse(report.forestryTracking().initialized());
            assertEquals(4, report.counts().mineralCoordinates());
            assertEquals(75, report.counts().mineralRemainingShares());
            assertEquals(1, report.counts().oreRetiredCoordinates());
            assertEquals(37, report.counts().oreExcludedCoordinates());
            assertTrue(placements.excludes(fixture.world, placementOnly));
            assertEquals(Set.of(fixture.world), report.loadedWorlds());
            assertEquals(ResourceReviewReport.MAX_SCOPES, report.scopes().size());
            assertEquals(7, report.moreScopes());
            assertTrue(report.describe().contains("untrusted; reviewed recovery required"));
            assertTrue(report.describe().contains("clean stop=false can mean capture is currently active"));
            assertTrue(report.describe().contains("no current block truth or recovery applied"));
        } finally {
            fixture.close();
        }
    }

    @Test void forestReportShowsNamedLossesAndExclusionsWhileCaptureGapRemainsUntrusted() throws Exception {
        var fixture = new ForestryPersistenceTest();
        fixture.directory = Files.createDirectories(directory.resolve("forest"));
        fixture.setup();
        try {
            fixture.start();
            var provenance = new ForestryProvenance(fixture.data.db.getConnection());
            provenance.record(fixture.data.world, List.of(new Position(30, 42, 2)),
                    List.of(fixture.trees.getFirst()), 20, "Reviewed report fixture");
            var tracking = new ForestryTrackingRepository(fixture.data.db.getConnection());
            assertFalse(tracking.begin(21).trusted(), "A legacy saved forest is not certified by first tracking start");
            tracking.markGap(22);

            var before = databaseSnapshot(fixture.data.db.getConnection());
            var report = new ResourceReviewReport(fixture.data.db.getConnection()).snapshot(Set.of());
            assertEquals(before, databaseSnapshot(fixture.data.db.getConnection()), "The report must not write any saved table");
            assertTrue(report.forestryTracking().initialized());
            assertFalse(report.forestryTracking().trusted());
            assertEquals(1, report.counts().forestryResources());
            assertEquals(6, report.counts().forestrySupports());
            assertEquals(1, report.counts().forestryLostSupports());
            assertEquals(2, report.counts().forestryExcludedCoordinates());
            assertTrue(report.counts().forestryAvailableUnits() <= 96);
            assertTrue(report.scopes().stream().anyMatch(scope -> scope.resource().equals("forest-loss")
                    && scope.world().equals(fixture.data.world) && scope.chunkX() == 1 && scope.chunkZ() == 0));
            assertTrue(report.describe().contains("forest-loss " + fixture.data.world + " chunk 1,0: 1 coordinates"));
            assertTrue(report.describe().contains("world is not in supplied loaded set"));
        } finally {
            fixture.close();
        }
    }

    private static Map<String, List<List<String>>> databaseSnapshot(Connection connection) throws SQLException {
        var names = new ArrayList<String>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
            while (rows.next()) names.add(rows.getString(1));
        }
        var snapshot = new TreeMap<String, List<List<String>>>();
        for (String name : names) {
            var contents = new ArrayList<List<String>>();
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT * FROM \"" + name.replace("\"", "\"\"") + "\"")) {
                while (rows.next()) {
                    var row = new ArrayList<String>();
                    for (int column = 1; column <= rows.getMetaData().getColumnCount(); column++)
                        row.add(Objects.toString(rows.getObject(column), null));
                    contents.add(Collections.unmodifiableList(row));
                }
            }
            contents.sort(Comparator.comparing(Object::toString));
            snapshot.put(name, List.copyOf(contents));
        }
        return snapshot;
    }
}
