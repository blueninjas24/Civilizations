package com.blueninjas24.civilizations.database;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedTerrainMigrationTest {
    @Test void migration99AddsOnlyEmptyTableAndRollsBackCleanlyBeforeRetry(@TempDir Path directory) throws Exception {
        IndustryRepositoryTest fixture = new IndustryRepositoryTest();
        fixture.directory = directory;
        fixture.setup();
        try {
            fixture.stores(1024);
            fixture.industry.discover(fixture.deposit, fixture.mine.siteId(), 8, 1200, IndustryRepositoryTest.BUNDLE);
            fixture.industry.mine(java.util.UUID.randomUUID(), fixture.mine.id(), fixture.deposit, 100, 0, 8, 32);

            Connection connection = fixture.database.getConnection();
            SchemaFixtures.rewind(connection, 98);
            assertEquals(98, scalar(connection, "PRAGMA user_version"));
            Map<String, List<List<String>>> before = snapshot(connection, "generated_resource_chunks");
            assertEquals(173, before.size(), "Expected every predecessor table in the version 98 fixture");

            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE generated_resource_chunks(conflict TEXT)");
            }
            assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(connection));
            assertEquals(98, scalar(connection, "PRAGMA user_version"));
            assertEquals(before, snapshot(connection, "generated_resource_chunks"));
            assertEquals(1, scalar(connection,
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='generated_resource_chunks'"));

            try (var statement = connection.createStatement()) { statement.execute("DROP TABLE generated_resource_chunks"); }
            new SchemaMigrator().migrate(connection);
            assertEquals(SchemaMigrator.CURRENT_VERSION, scalar(connection, "PRAGMA user_version"));
            Map<String, List<List<String>>> after = snapshot(connection, "");
            for(var additive:List.of("construction_drafts","construction_block_journal","construction_material_reservations","construction_material_items","construction_site_reservations","construction_material_returns","construction_supply_orders","construction_stage_work","construction_public_work","building_records","building_units","building_consent_events","building_exclusions","building_exclusion_releases","dwelling_reviews","dwelling_review_units","dwelling_current_reviews","dwelling_assignments","dwelling_active_occupancy","dwelling_departures","household_capacity_sources","dwelling_bed_claims"))
                assertEquals(List.of(),after.remove(additive),"Later construction additions must start empty");
            assertEquals(before.size() + 1, after.size());
            List<List<String>> newTable = after.remove("generated_resource_chunks");
            assertEquals(List.of(), newTable);
            assertEquals(before, after, "Migration 99 must preserve every predecessor table and row value");

            assertEquals("ok", textScalar(connection, "PRAGMA integrity_check"));
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM generated_resource_chunks"));
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(rows.next(), "Migration must leave no foreign-key violations");
            }
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT kind,world_id,chunk_x,chunk_z,generation,observed_ms FROM generated_resource_chunks")) {
                assertFalse(rows.next());
            }
        } finally {
            fixture.close();
        }
    }

    private static int scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }

    private static String textScalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }

    /** Captures every user table and every row with SQLite value types retained. */
    private static Map<String, List<List<String>>> snapshot(Connection connection, String excludedTable) throws SQLException {
        Map<String, List<List<String>>> snapshot = new TreeMap<>();
        List<String> tables = new ArrayList<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
            while (rows.next()) {
                String table = rows.getString(1);
                if (!table.equals(excludedTable)) tables.add(table);
            }
        }
        for (String table : tables) {
            List<String> columns = new ArrayList<>();
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "PRAGMA table_info(" + quote(table) + ")")) {
                while (rows.next()) columns.add(rows.getString("name"));
            }
            String projection = columns.stream().map(GeneratedTerrainMigrationTest::quote)
                    .collect(java.util.stream.Collectors.joining(","));
            List<List<String>> values = new ArrayList<>();
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT " + projection + " FROM " + quote(table))) {
                while (rows.next()) {
                    List<String> row = new ArrayList<>(columns.size());
                    for (int index = 1; index <= columns.size(); index++) row.add(normalize(rows.getObject(index)));
                    values.add(List.copyOf(row));
                }
            }
            values.sort(Comparator.comparing(Object::toString));
            snapshot.put(table, List.copyOf(values));
        }
        return snapshot;
    }

    private static String normalize(Object value) {
        if (value == null) return "null";
        if (value instanceof byte[] bytes) return "blob:" + Base64.getEncoder().encodeToString(bytes);
        return value.getClass().getName() + ":" + value;
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
