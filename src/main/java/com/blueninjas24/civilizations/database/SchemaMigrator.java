package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** SQLite schema upgrades; all pending steps and their version markers commit together. */
final class SchemaMigrator {
    static final int CURRENT_VERSION = 4;

    @FunctionalInterface
    interface Migration {
        void apply(Connection connection) throws SQLException;
    }

    private final List<Migration> migrations;

    SchemaMigrator() {
        this(List.of(
                connection -> {
                    execute(connection, """
                            CREATE TABLE IF NOT EXISTS settlements (
                                id TEXT PRIMARY KEY,
                                name TEXT NOT NULL,
                                world TEXT NOT NULL,
                                center_x INTEGER NOT NULL,
                                center_y INTEGER NOT NULL,
                                center_z INTEGER NOT NULL,
                                discovered_at INTEGER NOT NULL
                            )
                            """);
                    requireColumns(connection, Set.of("id", "name", "world", "center_x",
                            "center_y", "center_z", "discovered_at"));
                },
                connection -> addColumnIfMissing(connection, "population"),
                connection -> addColumnIfMissing(connection, "beds"),
                SchemaMigrator::createPopulationTables
        ));
    }

    // Tests can inject a failing step to verify real SQLite rollback and recovery.
    SchemaMigrator(List<Migration> migrations) {
        this.migrations = List.copyOf(migrations);
    }

    void migrate(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("Schema migration requires a connection without an active transaction");
        }
        int version;
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA user_version")) {
            if (!result.next()) {
                throw new SQLException("Cannot read Civilizations schema version");
            }
            version = result.getInt(1);
        }
        if (version < 0 || version > migrations.size()) {
            throw new SQLException("Unsupported Civilizations schema version " + version
                    + "; maximum supported is " + migrations.size());
        }

        connection.setAutoCommit(false);
        try {
            for (int index = version; index < migrations.size(); index++) {
                migrations.get(index).apply(connection);
                execute(connection, "PRAGMA user_version = " + (index + 1));
            }
            requireColumns(connection, Set.of("id", "name", "world", "center_x", "center_y",
                    "center_z", "discovered_at", "population", "beds"));
            if (migrations.size() >= 4) {
                requireColumns(connection, "households", Set.of("id", "settlement_id", "name"));
                requireColumns(connection, "residents", Set.of("id", "name", "settlement_id",
                        "household_id", "age_steps", "status"));
            }
            connection.commit();
        } catch (SQLException | RuntimeException failure) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
        connection.setAutoCommit(true);
    }

    private static void addColumnIfMissing(Connection connection, String column) throws SQLException {
        if (!columns(connection).contains(column)) {
            execute(connection, "ALTER TABLE settlements ADD COLUMN " + column
                    + " INTEGER NOT NULL DEFAULT 0");
        }
    }

    private static Set<String> columns(Connection connection) throws SQLException {
        return columns(connection, "settlements");
    }

    private static Set<String> columns(Connection connection, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                columns.add(result.getString("name").toLowerCase(java.util.Locale.ROOT));
            }
        }
        return columns;
    }

    private static void requireColumns(Connection connection, Set<String> required) throws SQLException {
        requireColumns(connection, "settlements", required);
    }

    private static void requireColumns(Connection connection, String table, Set<String> required) throws SQLException {
        Set<String> missing = new HashSet<>(required);
        missing.removeAll(columns(connection, table));
        if (!missing.isEmpty()) {
            throw new SQLException("Civilizations " + table + " schema is missing columns: " + missing);
        }
    }

    private static void createPopulationTables(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE households (
                    id TEXT PRIMARY KEY NOT NULL,
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    name TEXT NOT NULL CHECK(length(trim(name)) > 0),
                    UNIQUE(id, settlement_id)
                )
                """);
        execute(connection, """
                CREATE TABLE residents (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL CHECK(length(trim(name)) > 0),
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    household_id TEXT,
                    age_steps INTEGER NOT NULL CHECK(typeof(age_steps) = 'integer' AND age_steps >= 0),
                    status TEXT NOT NULL CHECK(status IN ('ALIVE', 'DECEASED')),
                    FOREIGN KEY(household_id, settlement_id) REFERENCES households(id, settlement_id)
                )
                """);
        execute(connection, "CREATE INDEX households_by_settlement ON households(settlement_id, id)");
        execute(connection, "CREATE INDEX residents_by_settlement ON residents(settlement_id, id)");
        execute(connection, "CREATE INDEX residents_by_household ON residents(household_id, settlement_id)");
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
