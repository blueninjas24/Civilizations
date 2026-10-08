package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** SQLite schema upgrades; all pending steps and their version markers commit together. */
final class SchemaMigrator {
    static final int CURRENT_VERSION = 11;

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
                SchemaMigrator::createPopulationTables,
                SchemaMigrator::createFamilyTable,
                SchemaMigrator::createPopulationClock,
                SchemaMigrator::createDemographicStorage,
                SchemaMigrator::extendMigrationEvents,
                connection -> execute(connection, """
                        CREATE TABLE resident_skills (
                            resident_id TEXT NOT NULL REFERENCES residents(id),
                            skill TEXT NOT NULL CHECK(length(trim(skill)) > 0),
                            experience INTEGER NOT NULL CHECK(typeof(experience) = 'integer' AND experience >= 0),
                            PRIMARY KEY(resident_id, skill)
                        )
                        """),
                connection -> execute(connection, """
                        CREATE TABLE civilization_time (
                            id INTEGER PRIMARY KEY CHECK(id = 1) REFERENCES demographic_clock(id),
                            millis_per_step INTEGER NOT NULL CHECK(typeof(millis_per_step) = 'integer' AND millis_per_step > 0),
                            last_observed_millis INTEGER NOT NULL CHECK(typeof(last_observed_millis) = 'integer' AND last_observed_millis >= 0),
                            remainder_millis INTEGER NOT NULL CHECK(typeof(remainder_millis) = 'integer' AND remainder_millis >= 0 AND remainder_millis < millis_per_step),
                            target_steps INTEGER NOT NULL CHECK(typeof(target_steps) = 'integer' AND target_steps >= 0)
                        )
                        """),
                SchemaMigrator::createVillageIdentity
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
            if (migrations.size() >= 5) {
                requireColumns(connection, "family_ties", Set.of("first_id", "second_id", "type"));
            }
            if (migrations.size() >= 6) {
                requireColumns(connection, "population_clocks", Set.of("settlement_id", "seed",
                        "completed_steps", "replay_version"));
            }
            if (migrations.size() >= 7) {
                requireColumns(connection, "demographic_clock", Set.of("id", "seed", "completed_steps", "replay_version", "policy"));
                requireColumns(connection, "household_capacity", Set.of("household_id", "capacity"));
                requireColumns(connection, "population_events", Set.of("sequence", "step", "type", "resident_id",
                        "settlement_id", "first_relative", "second_relative", "reason"));
            }
            if (migrations.size() >= 8) {
                requireColumns(connection, "population_events", Set.of("source_settlement_id"));
            }
            if (migrations.size() >= 9) {
                requireColumns(connection, "resident_skills", Set.of("resident_id", "skill", "experience"));
            }
            if (migrations.size() >= 10) {
                requireColumns(connection, "civilization_time", Set.of("id", "millis_per_step",
                        "last_observed_millis", "remainder_millis", "target_steps"));
            }
            if (migrations.size() >= 11) {
                requireColumns(connection, "observed_worlds", Set.of("id", "name"));
                requireColumns(connection, "settlement_worlds", Set.of("settlement_id", "world_id"));
                requireColumns(connection, "village_beds", Set.of("world_id", "x", "y", "z", "settlement_id"));
                requireColumns(connection, "village_entities", Set.of("world_id", "entity_id", "resident_id"));
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

    private static void createFamilyTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE family_ties (
                    first_id TEXT NOT NULL REFERENCES residents(id),
                    second_id TEXT NOT NULL REFERENCES residents(id),
                    type TEXT NOT NULL CHECK(type IN ('PARENT_CHILD', 'PARTNER')),
                    PRIMARY KEY(first_id, second_id, type),
                    CHECK(first_id <> second_id),
                    CHECK(type <> 'PARTNER' OR first_id < second_id)
                )
                """);
        execute(connection, "CREATE INDEX family_by_second ON family_ties(second_id, type, first_id)");
        // Check the existing descendants of the child before admitting a parent edge.
        // UNION (not UNION ALL) bounds traversal even if a manually damaged graph has a cycle.
        execute(connection, """
                CREATE TRIGGER reject_parent_cycle BEFORE INSERT ON family_ties
                WHEN NEW.type = 'PARENT_CHILD'
                BEGIN
                    SELECT RAISE(ABORT, 'Parent relationship would create an ancestry cycle')
                    WHERE EXISTS (
                        WITH RECURSIVE descendants(id) AS (
                            SELECT NEW.second_id
                            UNION
                            SELECT f.second_id FROM family_ties f JOIN descendants d ON f.first_id = d.id
                            WHERE f.type = 'PARENT_CHILD'
                        )
                        SELECT 1 FROM descendants WHERE id = NEW.first_id
                    );
                END
                """);
        // Recorded ties are historical facts. Corrections/new relationship lifecycles need an explicit API.
        execute(connection, """
                CREATE TRIGGER reject_family_update BEFORE UPDATE ON family_ties
                BEGIN
                    SELECT RAISE(ABORT, 'Family ties cannot be overwritten');
                END
                """);
    }

    private static void createPopulationClock(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE population_clocks (
                    settlement_id TEXT PRIMARY KEY NOT NULL REFERENCES settlements(id),
                    seed INTEGER NOT NULL CHECK(typeof(seed) = 'integer'),
                    completed_steps INTEGER NOT NULL CHECK(typeof(completed_steps) = 'integer' AND completed_steps >= 0),
                    replay_version INTEGER NOT NULL CHECK(typeof(replay_version) = 'integer' AND replay_version > 0)
                )
                """);
    }

    private static void createDemographicStorage(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE demographic_clock (
                    id INTEGER PRIMARY KEY CHECK(id = 1),
                    seed INTEGER NOT NULL CHECK(typeof(seed) = 'integer'),
                    completed_steps INTEGER NOT NULL CHECK(typeof(completed_steps) = 'integer' AND completed_steps >= 0),
                    replay_version INTEGER NOT NULL CHECK(typeof(replay_version) = 'integer' AND replay_version > 0),
                    policy TEXT NOT NULL
                )
                """);
        execute(connection, """
                CREATE TABLE household_capacity (
                    household_id TEXT PRIMARY KEY NOT NULL REFERENCES households(id),
                    capacity INTEGER NOT NULL CHECK(typeof(capacity) = 'integer' AND capacity >= 0)
                )
                """);
        execute(connection, """
                CREATE TABLE population_events (
                    sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                    step INTEGER NOT NULL CHECK(typeof(step) = 'integer' AND step > 0),
                    type TEXT NOT NULL CHECK(type IN ('BIRTH', 'DEATH')),
                    resident_id TEXT NOT NULL REFERENCES residents(id),
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    first_relative TEXT REFERENCES residents(id),
                    second_relative TEXT REFERENCES residents(id),
                    reason TEXT NOT NULL,
                    UNIQUE(step, type, resident_id),
                    CHECK((type = 'BIRTH' AND first_relative IS NOT NULL AND second_relative IS NOT NULL)
                       OR (type = 'DEATH' AND first_relative IS NULL AND second_relative IS NULL))
                )
                """);
        execute(connection, "CREATE INDEX events_by_resident ON population_events(resident_id, sequence)");
        execute(connection, """
                CREATE TRIGGER reject_mixed_demographic_clock BEFORE INSERT ON demographic_clock
                WHEN EXISTS (SELECT 1 FROM population_clocks)
                BEGIN SELECT RAISE(ABORT, 'Age-only clocks already exist'); END
                """);
        execute(connection, """
                CREATE TRIGGER reject_mixed_aging_clock BEFORE INSERT ON population_clocks
                WHEN EXISTS (SELECT 1 FROM demographic_clock)
                BEGIN SELECT RAISE(ABORT, 'Demographic clock already exists'); END
                """);
    }

    private static void extendMigrationEvents(Connection connection) throws SQLException {
        execute(connection, "ALTER TABLE population_events RENAME TO population_events_v7");
        execute(connection, """
                CREATE TABLE population_events (
                    sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                    step INTEGER NOT NULL CHECK(typeof(step) = 'integer' AND step > 0),
                    type TEXT NOT NULL CHECK(type IN ('BIRTH', 'DEATH', 'MIGRATION', 'ARRIVAL')),
                    resident_id TEXT NOT NULL REFERENCES residents(id),
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    first_relative TEXT REFERENCES residents(id),
                    second_relative TEXT REFERENCES residents(id),
                    reason TEXT NOT NULL,
                    source_settlement_id TEXT REFERENCES settlements(id),
                    UNIQUE(step, type, resident_id),
                    CHECK((type = 'BIRTH' AND first_relative IS NOT NULL AND second_relative IS NOT NULL)
                       OR (type <> 'BIRTH' AND first_relative IS NULL AND second_relative IS NULL)),
                    CHECK((type = 'MIGRATION' AND source_settlement_id IS NOT NULL AND source_settlement_id <> settlement_id)
                       OR (type <> 'MIGRATION' AND source_settlement_id IS NULL))
                )
                """);
        execute(connection, """
                INSERT INTO population_events(sequence, step, type, resident_id, settlement_id, first_relative, second_relative, reason)
                SELECT sequence, step, type, resident_id, settlement_id, first_relative, second_relative, reason
                FROM population_events_v7 ORDER BY sequence
                """);
        execute(connection, "DROP TABLE population_events_v7");
        execute(connection, "CREATE INDEX events_by_resident ON population_events(resident_id, sequence)");
    }

    private static void createVillageIdentity(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE observed_worlds (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL CHECK(length(trim(name)) > 0)
                )
                """);
        execute(connection, """
                CREATE TABLE settlement_worlds (
                    settlement_id TEXT PRIMARY KEY NOT NULL REFERENCES settlements(id),
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    UNIQUE(settlement_id, world_id)
                )
                """);
        execute(connection, """
                CREATE TABLE village_beds (
                    world_id TEXT NOT NULL,
                    x INTEGER NOT NULL CHECK(typeof(x) = 'integer'),
                    y INTEGER NOT NULL CHECK(typeof(y) = 'integer'),
                    z INTEGER NOT NULL CHECK(typeof(z) = 'integer'),
                    settlement_id TEXT NOT NULL,
                    PRIMARY KEY(world_id, x, y, z),
                    FOREIGN KEY(settlement_id, world_id) REFERENCES settlement_worlds(settlement_id, world_id)
                )
                """);
        execute(connection, """
                CREATE TABLE village_entities (
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    entity_id TEXT NOT NULL,
                    resident_id TEXT NOT NULL REFERENCES residents(id),
                    PRIMARY KEY(world_id, entity_id)
                )
                """);
        execute(connection, "CREATE INDEX village_entities_by_resident ON village_entities(resident_id)");
        execute(connection, "CREATE INDEX settlement_beds ON village_beds(settlement_id)");
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
