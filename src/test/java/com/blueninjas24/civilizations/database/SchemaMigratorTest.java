package com.blueninjas24.civilizations.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SchemaMigratorTest {
    @TempDir
    Path directory;

    @Test
    void freshInstallAndReopeningHaveCurrentVersion() throws SQLException {
        try (Connection connection = connect()) {
            new SchemaMigrator().migrate(connection);
            assertEquals(SchemaMigrator.CURRENT_VERSION, version(connection));
            assertTrue(connection.getAutoCommit());
        }
        try (Connection connection = connect()) {
            new SchemaMigrator().migrate(connection);
            assertEquals(SchemaMigrator.CURRENT_VERSION, version(connection));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void upgradesUnversionedPrototypeSchemasWithoutLosingRows(int censusColumns) throws SQLException {
        try (Connection connection = connect()) {
            createLegacy(connection, censusColumns);
            new SchemaMigrator().migrate(connection);
            assertLegacyRow(connection, censusColumns);
            assertEquals(SchemaMigrator.CURRENT_VERSION, version(connection));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void upgradesVersionedSchemasIncrementally(int startingVersion) throws SQLException {
        try (Connection connection = connect()) {
            createLegacy(connection, startingVersion - 1);
            execute(connection, "PRAGMA user_version = " + startingVersion);
            new SchemaMigrator().migrate(connection);
            assertLegacyRow(connection, startingVersion - 1);
            assertEquals(SchemaMigrator.CURRENT_VERSION, version(connection));
        }
    }

    @Test
    void rollsBackAllPendingStepsAndCanRecover() throws SQLException {
        try (Connection connection = connect()) {
            new SchemaMigrator().migrate(connection);
            createRow(connection, 2);
            SchemaMigrator failing = new SchemaMigrator(List.of(
                    ignored -> {}, ignored -> {}, ignored -> {}, ignored -> {},
                    c -> execute(c, "CREATE TABLE rollback_marker (id INTEGER)"),
                    c -> execute(c, "THIS IS INVALID SQL")
            ));
            assertThrows(SQLException.class, () -> failing.migrate(connection));
            assertEquals(SchemaMigrator.CURRENT_VERSION, version(connection));
            assertTrue(connection.getAutoCommit());
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT COUNT(*) FROM sqlite_master WHERE name = 'rollback_marker'")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            new SchemaMigrator().migrate(connection);
            assertLegacyRow(connection, 2);
        }
    }

    @Test
    void rejectsNewerSchemaWithoutModifyingIt() throws SQLException {
        try (Connection connection = connect()) {
            new SchemaMigrator().migrate(connection);
            createRow(connection, 2);
            execute(connection, "PRAGMA user_version = " + (SchemaMigrator.CURRENT_VERSION + 1));
            SQLException failure = assertThrows(SQLException.class,
                    () -> new SchemaMigrator().migrate(connection));
            assertTrue(failure.getMessage().contains("Unsupported"));
            assertEquals(SchemaMigrator.CURRENT_VERSION + 1, version(connection));
            assertLegacyRow(connection, 2);
        }
    }

    @Test
    void malformedLegacySchemaFailsExplicitlyWithoutAdvancingVersion() throws SQLException {
        try (Connection connection = connect()) {
            execute(connection, "CREATE TABLE settlements (id TEXT PRIMARY KEY)");
            SQLException failure = assertThrows(SQLException.class,
                    () -> new SchemaMigrator().migrate(connection));
            assertTrue(failure.getMessage().contains("missing columns"));
            assertEquals(0, version(connection));
        }
    }

    @Test
    void databaseManagerClosesConnectionWhenMigrationFails() throws SQLException {
        try (Connection connection = connect()) {
            execute(connection, "PRAGMA user_version = " + (SchemaMigrator.CURRENT_VERSION + 1));
        }
        DatabaseManager manager = new DatabaseManager(directory.toFile(), Logger.getLogger("MigrationTest"));
        assertThrows(SQLException.class, manager::initialize);
        assertNull(manager.getConnection());
        manager.close();
        // Recovery uses a fresh connection to the same temporary file.
        try (Connection connection = connect()) {
            execute(connection, "PRAGMA user_version = 0");
        }
        try {
            manager.initialize();
            assertEquals(SchemaMigrator.CURRENT_VERSION, version(manager.getConnection()));
        } finally {
            manager.close();
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("civilizations.db"));
    }

    @Test
    void populationMigrationFailureRollsBackNewTablesAndPreservesLegacyData() throws SQLException {
        try (Connection connection = connect()) {
            createLegacy(connection, 2);
            execute(connection, "PRAGMA user_version = 3");
            execute(connection, "CREATE TABLE residents (id TEXT)");
            assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(connection));
            assertEquals(3, version(connection));
            assertLegacyRow(connection, 2);
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT COUNT(*) FROM sqlite_master WHERE name = 'households'")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            execute(connection, "DROP TABLE residents"); // Only this test's disposable database.
            new SchemaMigrator().migrate(connection);
            assertEquals(SchemaMigrator.CURRENT_VERSION, version(connection));
            assertLegacyRow(connection, 2);
        }
    }

    private static void createLegacy(Connection connection, int censusColumns) throws SQLException {
        String optionalColumns = censusColumns >= 1 ? ", population INTEGER NOT NULL DEFAULT 0" : "";
        if (censusColumns == 2) {
            optionalColumns += ", beds INTEGER NOT NULL DEFAULT 0";
        }
        execute(connection, "CREATE TABLE settlements (id TEXT PRIMARY KEY, name TEXT NOT NULL, "
                + "world TEXT NOT NULL, center_x INTEGER NOT NULL, center_y INTEGER NOT NULL, "
                + "center_z INTEGER NOT NULL, discovered_at INTEGER NOT NULL" + optionalColumns + ")");
        createRow(connection, censusColumns);
    }

    private static void createRow(Connection connection, int censusColumns) throws SQLException {
        String columns = censusColumns >= 1 ? ", population" : "";
        String values = censusColumns >= 1 ? ", 11" : "";
        if (censusColumns == 2) {
            columns += ", beds";
            values += ", 13";
        }
        execute(connection, "INSERT INTO settlements (id, name, world, center_x, center_y, center_z, "
                + "discovered_at" + columns + ") VALUES ('legacy-id', 'Stonevale', 'world', "
                + "-10, 70, 20, 1790000123456" + values + ")");
    }

    private static void assertLegacyRow(Connection connection, int originalCensusColumns) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT * FROM settlements")) {
            assertTrue(result.next());
            assertEquals("legacy-id", result.getString("id"));
            assertEquals("Stonevale", result.getString("name"));
            assertEquals("world", result.getString("world"));
            assertEquals(-10, result.getInt("center_x"));
            assertEquals(70, result.getInt("center_y"));
            assertEquals(20, result.getInt("center_z"));
            assertEquals(1_790_000_123_456L, result.getLong("discovered_at"));
            assertEquals(originalCensusColumns >= 1 ? 11 : 0, result.getInt("population"));
            assertEquals(originalCensusColumns == 2 ? 13 : 0, result.getInt("beds"));
            assertFalse(result.next());
        }
    }

    private static int version(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA user_version")) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
