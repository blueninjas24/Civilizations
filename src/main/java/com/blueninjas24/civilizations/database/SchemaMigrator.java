package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** SQLite schema upgrades; all pending steps and their version markers commit together. */
final class SchemaMigrator {
    static final int CURRENT_VERSION = 18;

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
                SchemaMigrator::createVillageIdentity,
                connection -> execute(connection,
                        "ALTER TABLE village_beds ADD COLUMN household_id TEXT REFERENCES households(id)"),
                SchemaMigrator::createResidentProjection,
                SchemaMigrator::createCommodityAccounting,
                SchemaMigrator::createFoodRuntime,
                SchemaMigrator::createHouseholdNeedsAndTreasury,
                connection -> execute(connection, """
                        CREATE TABLE village_priorities (
                            settlement_id TEXT NOT NULL,
                            period INTEGER NOT NULL,
                            seed INTEGER NOT NULL CHECK(typeof(seed)='integer'),
                            replay_version INTEGER NOT NULL CHECK(replay_version=1),
                            food_condition TEXT NOT NULL CHECK(food_condition IN ('SEVERE_SHORTAGE','SHORTAGE','BALANCED','SURPLUS','ABUNDANT')),
                            food_unmet INTEGER NOT NULL CHECK(typeof(food_unmet)='integer' AND food_unmet>=0),
                            crowded_homes INTEGER NOT NULL CHECK(typeof(crowded_homes)='integer' AND crowded_homes>=0),
                            selected TEXT NOT NULL CHECK(selected IN ('MONITOR','SEEK_FOOD','INVESTIGATE_HOUSING')),
                            review_after INTEGER NOT NULL CHECK(review_after BETWEEN 1 AND 1000),
                            food_weight INTEGER NOT NULL CHECK(food_weight BETWEEN 1 AND 1000),
                            crisis_multiplier INTEGER NOT NULL CHECK(crisis_multiplier BETWEEN 1 AND 100),
                            housing_weight INTEGER NOT NULL CHECK(housing_weight BETWEEN 1 AND 1000),
                            monitor_weight INTEGER NOT NULL CHECK(monitor_weight BETWEEN 1 AND 1000),
                            relative_percent INTEGER NOT NULL CHECK(relative_percent BETWEEN 0 AND 100),
                            PRIMARY KEY(settlement_id,period),
                            FOREIGN KEY(settlement_id,period) REFERENCES needs_periods(settlement_id,period)
                        )
                        """),
                connection -> {
                    execute(connection, """
                            CREATE TABLE food_deliveries (
                                sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                                operation_id TEXT NOT NULL UNIQUE,
                                settlement_id TEXT NOT NULL REFERENCES economy_runtime(settlement_id),
                                units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>0),
                                source TEXT NOT NULL CHECK(length(trim(source)) BETWEEN 1 AND 256),
                                eligible_after INTEGER NOT NULL CHECK(typeof(eligible_after)='integer' AND eligible_after>=0),
                                applied_period INTEGER CHECK(applied_period IS NULL OR (typeof(applied_period)='integer' AND applied_period>eligible_after)),
                                FOREIGN KEY(settlement_id,applied_period) REFERENCES needs_periods(settlement_id,period)
                            )
                            """);
                    execute(connection,"CREATE INDEX pending_food_deliveries ON food_deliveries(settlement_id,sequence) WHERE applied_period IS NULL");
                }
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
            if (migrations.size() >= 12) requireColumns(connection, "village_beds", Set.of("household_id"));
            if (migrations.size() >= 13) {
                requireColumns(connection, "resident_projection", Set.of("resident_id", "world_id", "entity_id", "state", "household_id", "x", "y", "z"));
                requireColumns(connection, "resident_lifecycle_events", Set.of("sequence", "resident_id", "completed_month", "observed_millis", "reason"));
            }
            if (migrations.size() >= 14) {
                requireColumns(connection, "economy_accounts", Set.of("settlement_id", "completed_periods", "replay_version",
                        "severe_bps", "surplus_bps", "abundant_bps", "severe_after"));
                requireColumns(connection, "economy_stocks", Set.of("settlement_id", "commodity", "units", "shortage_periods"));
                requireColumns(connection, "economy_periods", Set.of("settlement_id", "commodity", "period", "opening", "prior_shortage",
                        "production", "imports", "households", "industry", "construction", "exports", "events"));
            }
            if (migrations.size() >= 15) {
                requireColumns(connection, "farm_evidence", Set.of("settlement_id", "world_id", "observed_millis", "complete", "planted_crops"));
                requireColumns(connection, "economy_runtime", Set.of("settlement_id", "millis_per_period", "last_observed_millis", "remainder_millis", "target_periods", "settings"));
                requireColumns(connection, "economy_tuning", Set.of("sequence", "settlement_id", "after_period", "observed_millis", "discarded_millis", "old_settings", "new_settings"));
                requireColumns(connection, "economy_periods", Set.of("severe_bps", "surplus_bps", "abundant_bps", "severe_after"));
            }
            if (migrations.size() >= 16) {
                requireColumns(connection, "needs_periods", Set.of("settlement_id", "period", "food_condition", "shortage_periods", "fulfilled", "demand", "unassigned_residents"));
                requireColumns(connection, "household_needs", Set.of("settlement_id", "period", "household_id", "adults", "children", "food_demand", "capacity"));
                requireColumns(connection, "treasuries", Set.of("settlement_id", "hundredths"));
                requireColumns(connection, "treasury_entries", Set.of("sequence", "operation_id", "settlement_id", "kind", "amount", "balance_after", "reason"));
            }
            if (migrations.size() >= 17) requireColumns(connection, "village_priorities", Set.of("settlement_id", "period", "seed", "replay_version",
                    "food_condition", "food_unmet", "crowded_homes", "selected", "review_after", "food_weight", "crisis_multiplier", "housing_weight", "monitor_weight", "relative_percent"));
            if (migrations.size() >= 18) requireColumns(connection,"food_deliveries",Set.of("sequence","operation_id","settlement_id","units","source","eligible_after","applied_period"));
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

    private static void createResidentProjection(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE resident_projection (
                    resident_id TEXT PRIMARY KEY NOT NULL REFERENCES residents(id),
                    world_id TEXT NOT NULL,
                    entity_id TEXT NOT NULL,
                    state TEXT NOT NULL CHECK(state IN ('CLAIMED', 'ACTIVE', 'RETIRED')),
                    household_id TEXT REFERENCES households(id),
                    x INTEGER, y INTEGER, z INTEGER,
                    CHECK((x IS NULL AND y IS NULL AND z IS NULL AND state <> 'CLAIMED')
                       OR (typeof(x) = 'integer' AND typeof(y) = 'integer' AND typeof(z) = 'integer')),
                    UNIQUE(world_id, entity_id),
                    FOREIGN KEY(world_id, entity_id) REFERENCES village_entities(world_id, entity_id)
                )
                """);
        // Only unambiguous existing identities can become the current physical incarnation.
        execute(connection, """
                INSERT INTO resident_projection(resident_id, world_id, entity_id, state, household_id)
                SELECT e.resident_id, e.world_id, e.entity_id, 'ACTIVE', r.household_id
                FROM village_entities e JOIN residents r ON r.id = e.resident_id
                WHERE (SELECT COUNT(*) FROM village_entities other WHERE other.resident_id = e.resident_id) = 1
                """);
        execute(connection, """
                CREATE TABLE resident_lifecycle_events (
                    sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                    resident_id TEXT NOT NULL UNIQUE REFERENCES residents(id),
                    completed_month INTEGER NOT NULL CHECK(typeof(completed_month) = 'integer' AND completed_month >= 0),
                    observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis) = 'integer' AND observed_millis >= 0),
                    reason TEXT NOT NULL CHECK(length(trim(reason)) > 0)
                )
                """);
    }

    private static void createCommodityAccounting(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE economy_accounts (
                    settlement_id TEXT PRIMARY KEY NOT NULL REFERENCES settlements(id),
                    completed_periods INTEGER NOT NULL CHECK(typeof(completed_periods) = 'integer' AND completed_periods >= 0),
                    replay_version INTEGER NOT NULL CHECK(replay_version = 1),
                    severe_bps INTEGER NOT NULL CHECK(typeof(severe_bps) = 'integer' AND severe_bps >= 0 AND severe_bps < 10000),
                    surplus_bps INTEGER NOT NULL CHECK(typeof(surplus_bps) = 'integer' AND surplus_bps > 10000),
                    abundant_bps INTEGER NOT NULL CHECK(typeof(abundant_bps) = 'integer' AND abundant_bps > surplus_bps),
                    severe_after INTEGER NOT NULL CHECK(typeof(severe_after) = 'integer' AND severe_after > 0)
                )
                """);
        execute(connection, """
                CREATE TABLE economy_stocks (
                    settlement_id TEXT NOT NULL REFERENCES economy_accounts(settlement_id),
                    commodity TEXT NOT NULL CHECK(length(commodity) BETWEEN 1 AND 64),
                    units INTEGER NOT NULL CHECK(typeof(units) = 'integer' AND units >= 0),
                    shortage_periods INTEGER NOT NULL CHECK(typeof(shortage_periods) = 'integer' AND shortage_periods >= 0),
                    PRIMARY KEY(settlement_id, commodity)
                )
                """);
        execute(connection, """
                CREATE TABLE economy_periods (
                    settlement_id TEXT NOT NULL,
                    commodity TEXT NOT NULL,
                    period INTEGER NOT NULL CHECK(typeof(period) = 'integer' AND period > 0),
                    opening INTEGER NOT NULL CHECK(typeof(opening) = 'integer' AND opening >= 0),
                    prior_shortage INTEGER NOT NULL CHECK(typeof(prior_shortage) = 'integer' AND prior_shortage >= 0),
                    production INTEGER NOT NULL CHECK(typeof(production) = 'integer' AND production >= 0),
                    imports INTEGER NOT NULL CHECK(typeof(imports) = 'integer' AND imports >= 0),
                    households INTEGER NOT NULL CHECK(typeof(households) = 'integer' AND households >= 0),
                    industry INTEGER NOT NULL CHECK(typeof(industry) = 'integer' AND industry >= 0),
                    construction INTEGER NOT NULL CHECK(typeof(construction) = 'integer' AND construction >= 0),
                    exports INTEGER NOT NULL CHECK(typeof(exports) = 'integer' AND exports >= 0),
                    events INTEGER NOT NULL CHECK(typeof(events) = 'integer' AND events >= 0),
                    PRIMARY KEY(settlement_id, period, commodity),
                    FOREIGN KEY(settlement_id, commodity) REFERENCES economy_stocks(settlement_id, commodity)
                )
                """);
    }

    private static void createFoodRuntime(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE farm_evidence (
                    settlement_id TEXT PRIMARY KEY NOT NULL,
                    world_id TEXT NOT NULL,
                    observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis) = 'integer' AND observed_millis >= 0),
                    complete INTEGER NOT NULL CHECK(complete IN (0, 1)),
                    planted_crops INTEGER NOT NULL CHECK(typeof(planted_crops) = 'integer' AND planted_crops BETWEEN 0 AND 100000),
                    FOREIGN KEY(settlement_id, world_id) REFERENCES settlement_worlds(settlement_id, world_id)
                )
                """);
        execute(connection, """
                CREATE TABLE economy_runtime (
                    settlement_id TEXT PRIMARY KEY NOT NULL REFERENCES economy_accounts(settlement_id),
                    millis_per_period INTEGER NOT NULL CHECK(typeof(millis_per_period) = 'integer' AND millis_per_period > 0),
                    last_observed_millis INTEGER NOT NULL CHECK(typeof(last_observed_millis) = 'integer' AND last_observed_millis >= 0),
                    remainder_millis INTEGER NOT NULL CHECK(typeof(remainder_millis) = 'integer' AND remainder_millis >= 0 AND remainder_millis < millis_per_period),
                    target_periods INTEGER NOT NULL CHECK(typeof(target_periods) = 'integer' AND target_periods >= 0),
                    settings TEXT NOT NULL
                )
                """);
        execute(connection, """
                CREATE TABLE economy_tuning (
                    sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                    settlement_id TEXT NOT NULL REFERENCES economy_accounts(settlement_id),
                    after_period INTEGER NOT NULL CHECK(typeof(after_period) = 'integer' AND after_period >= 0),
                    observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis) = 'integer' AND observed_millis >= 0),
                    discarded_millis INTEGER NOT NULL CHECK(typeof(discarded_millis) = 'integer' AND discarded_millis >= 0),
                    old_settings TEXT NOT NULL,
                    new_settings TEXT NOT NULL
                )
                """);
        for (var column : List.of("severe_bps", "surplus_bps", "abundant_bps", "severe_after")) {
            execute(connection, "ALTER TABLE economy_periods ADD COLUMN " + column + " INTEGER NOT NULL DEFAULT 0");
            execute(connection, "UPDATE economy_periods SET " + column + " = (SELECT " + column + " FROM economy_accounts WHERE settlement_id = economy_periods.settlement_id)");
        }
    }

    private static void createHouseholdNeedsAndTreasury(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE needs_periods (
                    settlement_id TEXT NOT NULL REFERENCES economy_accounts(settlement_id),
                    period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>0),
                    food_condition TEXT NOT NULL CHECK(food_condition IN ('SEVERE_SHORTAGE','SHORTAGE','BALANCED','SURPLUS','ABUNDANT')),
                    shortage_periods INTEGER NOT NULL CHECK(typeof(shortage_periods)='integer' AND shortage_periods>=0),
                    fulfilled INTEGER NOT NULL CHECK(typeof(fulfilled)='integer' AND fulfilled>=0),
                    demand INTEGER NOT NULL CHECK(typeof(demand)='integer' AND demand>=fulfilled),
                    unassigned_residents INTEGER NOT NULL CHECK(typeof(unassigned_residents)='integer' AND unassigned_residents>=0),
                    PRIMARY KEY(settlement_id,period)
                )
                """);
        execute(connection, """
                CREATE TABLE household_needs (
                    settlement_id TEXT NOT NULL,
                    period INTEGER NOT NULL,
                    household_id TEXT NOT NULL REFERENCES households(id),
                    adults INTEGER NOT NULL CHECK(typeof(adults)='integer' AND adults>=0),
                    children INTEGER NOT NULL CHECK(typeof(children)='integer' AND children>=0),
                    food_demand INTEGER NOT NULL CHECK(typeof(food_demand)='integer' AND food_demand>=0),
                    capacity INTEGER CHECK(capacity IS NULL OR (typeof(capacity)='integer' AND capacity>=0)),
                    PRIMARY KEY(settlement_id,period,household_id),
                    FOREIGN KEY(settlement_id,period) REFERENCES needs_periods(settlement_id,period)
                )
                """);
        execute(connection, """
                CREATE TABLE treasuries (
                    settlement_id TEXT PRIMARY KEY NOT NULL REFERENCES settlements(id),
                    hundredths INTEGER NOT NULL CHECK(typeof(hundredths)='integer' AND hundredths>=0)
                )
                """);
        execute(connection, """
                CREATE TABLE treasury_entries (
                    sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                    operation_id TEXT NOT NULL UNIQUE,
                    settlement_id TEXT NOT NULL REFERENCES treasuries(settlement_id),
                    kind TEXT NOT NULL CHECK(kind IN ('OPENING','CREDIT','DEBIT')),
                    amount INTEGER NOT NULL CHECK(typeof(amount)='integer' AND amount>=0 AND (kind='OPENING' OR amount>0)),
                    balance_after INTEGER NOT NULL CHECK(typeof(balance_after)='integer' AND balance_after>=0),
                    reason TEXT NOT NULL CHECK(length(trim(reason))>0)
                )
                """);
        execute(connection, "CREATE INDEX treasury_history ON treasury_entries(settlement_id,sequence)");
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
