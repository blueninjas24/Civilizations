package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.VillageObservation;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedTerrainRepositoryTest {
    private static final UUID WORLD = UUID.randomUUID();

    private Connection connection(Path path, boolean initialize) throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        try (var statement = connection.createStatement()) { statement.execute("PRAGMA foreign_keys=ON"); }
        if (!initialize) return connection;
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE observed_worlds(id TEXT PRIMARY KEY,name TEXT NOT NULL)");
            statement.execute("CREATE TABLE ore_tracking(id INTEGER PRIMARY KEY, legacy_value TEXT NOT NULL)");
            statement.execute("INSERT INTO ore_tracking(legacy_value) VALUES('preserve-me')");
            statement.execute("INSERT INTO observed_worlds(id,name) VALUES('" + WORLD + "','test')");
        }
        ResourceRecoveryEpochs.createSchema(connection);
        GeneratedTerrainRepository.createSchema(connection);
        return connection;
    }

    private UUID begin(Connection connection, String kind, boolean clean) throws Exception {
        connection.setAutoCommit(false);
        try {
            ResourceRecoveryEpochs.transitionRows(connection, kind, true, clean, false);
            connection.commit();
        } finally {
            connection.setAutoCommit(true);
        }
        return new ResourceRecoveryEpochs(connection).load(kind).orElseThrow().generation();
    }

    private void transition(Connection connection, String kind, boolean begin, boolean clean, boolean gap) throws Exception {
        connection.setAutoCommit(false);
        try {
            ResourceRecoveryEpochs.transitionRows(connection, kind, begin, clean, gap);
            connection.commit();
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private int observed(Connection connection, String kind, UUID world, int x, int z) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT observed_ms FROM generated_resource_chunks WHERE kind=? AND world_id=? AND chunk_x=? AND chunk_z=?
                """)) {
            statement.setString(1, kind); statement.setString(2, world.toString());
            statement.setInt(3, x); statement.setInt(4, z);
            try (var rows = statement.executeQuery()) { assertTrue(rows.next()); return rows.getInt(1); }
        }
    }

    @Test void coverageUsesNegativeFloorDivisionAndAcceptsMinecraftBlockBounds(@TempDir Path directory) throws Exception {
        try (Connection connection = connection(directory.resolve("generated.sqlite"), true)) {
            UUID generation = begin(connection, "ore", false);
            var repository = new GeneratedTerrainRepository(connection);
            assertTrue(repository.recordGenerated("ore", WORLD, -1, -1, generation, 10));
            assertTrue(repository.covers("ore", WORLD, List.of(new VillageObservation.Position(-1, 64, -1),
                    new VillageObservation.Position(-16, 64, -16))));
            assertFalse(repository.covers("ore", WORLD, List.of(new VillageObservation.Position(-1, 64, -1),
                    new VillageObservation.Position(-17, 64, -1))));

            int minChunk = Math.floorDiv(Integer.MIN_VALUE, 16);
            int maxChunk = Math.floorDiv(Integer.MAX_VALUE, 16);
            assertTrue(repository.recordGenerated("ore", WORLD, minChunk, minChunk, generation, 11));
            assertTrue(repository.recordGenerated("ore", WORLD, maxChunk, maxChunk, generation, 12));
            assertTrue(repository.covers("ore", WORLD, List.of(new VillageObservation.Position(Integer.MIN_VALUE, 64, Integer.MIN_VALUE),
                    new VillageObservation.Position(Integer.MAX_VALUE, 64, Integer.MAX_VALUE))));
            assertThrows(IllegalArgumentException.class,
                    () -> repository.recordGenerated("ore", WORLD, maxChunk + 1, 0, generation, 13));
            assertThrows(IllegalArgumentException.class, () -> repository.covers("ore", WORLD, List.of()));
        }
    }

    @Test void cleanRestartPreservesCoverageButGapAndNewEpochCannotRecertifyOldChunk(@TempDir Path directory) throws Exception {
        Path databasePath = directory.resolve("generated.sqlite");
        UUID generation;
        try (Connection connection = connection(databasePath, true)) {
            generation = begin(connection, "forest", false);
            var repository = new GeneratedTerrainRepository(connection);
            assertTrue(repository.recordGenerated("forest", WORLD, 3, -2, generation, 20));
            assertTrue(repository.covers("forest", WORLD, List.of(new VillageObservation.Position(48, 64, -17))));

            transition(connection, "forest", true, true, false);
            assertEquals(generation, new ResourceRecoveryEpochs(connection).load("forest").orElseThrow().generation());
            assertTrue(repository.covers("forest", WORLD, List.of(new VillageObservation.Position(48, 64, -17))));
        }

        try (Connection connection = connection(databasePath, false)) {
            var repository = new GeneratedTerrainRepository(connection);
            assertTrue(repository.covers("forest", WORLD, List.of(new VillageObservation.Position(48, 64, -17))));

            transition(connection, "forest", false, false, true);
            assertFalse(repository.covers("forest", WORLD, List.of(new VillageObservation.Position(48, 64, -17))));
            assertFalse(repository.recordGenerated("forest", WORLD, 3, -2, generation, 21));
            UUID afterGap = begin(connection, "forest", false);
            assertNotEquals(generation, afterGap);
            assertFalse(repository.recordGenerated("forest", WORLD, 3, -2, afterGap, 22));
            assertFalse(repository.covers("forest", WORLD, List.of(new VillageObservation.Position(48, 64, -17))));
            assertEquals(20, observed(connection, "forest", WORLD, 3, -2));
        }
    }

    @Test void lateInsertFailureRollsBackAndRetryIsSafeWithoutTouchingLegacyRows(@TempDir Path directory) throws Exception {
        try (Connection connection = connection(directory.resolve("generated.sqlite"), true)) {
            UUID generation = begin(connection, "ore", false);
            var repository = new GeneratedTerrainRepository(connection);
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TRIGGER fail_selected_generated_chunk BEFORE INSERT ON generated_resource_chunks
                        WHEN NEW.chunk_x=7 BEGIN SELECT RAISE(ABORT,'late insert failure'); END
                        """);
            }
            assertThrows(SQLException.class, () -> repository.recordGenerated("ore", WORLD, 7, 8, generation, 30));
            assertTrue(connection.getAutoCommit());
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT COUNT(*) FROM generated_resource_chunks")) { assertTrue(rows.next()); assertEquals(0, rows.getInt(1)); }

            try (var statement = connection.createStatement()) { statement.execute("DROP TRIGGER fail_selected_generated_chunk"); }
            assertTrue(repository.recordGenerated("ore", WORLD, 7, 8, generation, 31));
            assertTrue(repository.recordGenerated("ore", WORLD, 7, 8, generation, 32));
            assertEquals(31, observed(connection, "ore", WORLD, 7, 8));
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT COUNT(*) FROM ore_tracking WHERE legacy_value='preserve-me'")) {
                assertTrue(rows.next()); assertEquals(1, rows.getInt(1));
            }
        }
    }
}
