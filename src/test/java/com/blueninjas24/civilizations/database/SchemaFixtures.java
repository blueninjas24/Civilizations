package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Reconstruct prior schema shapes only inside test-owned temporary databases. */
final class SchemaFixtures {
    private SchemaFixtures() {}
    static void rewind(Connection connection, int version) throws SQLException {
        try (var statement = connection.createStatement()) {
            if (version < 22) { statement.execute("DROP TABLE wellbeing_members"); statement.execute("DROP TABLE household_wellbeing"); }
            if (version < 21) statement.execute("DROP TABLE food_purchases");
            if (version < 20) statement.execute("DROP TABLE food_appeals");
            if (version < 19) {
                for (var column : java.util.List.of("supplied", "unmet_periods")) statement.execute("ALTER TABLE household_needs DROP COLUMN " + column);
                for (var column : java.util.List.of("unassigned_demand", "unassigned_supplied")) statement.execute("ALTER TABLE needs_periods DROP COLUMN " + column);
            }
            if (version < 18) statement.execute("DROP TABLE food_deliveries");
            if (version < 17) statement.execute("DROP TABLE village_priorities");
            if (version < 16) {
                statement.execute("DROP TABLE household_needs"); statement.execute("DROP TABLE needs_periods");
                statement.execute("DROP TABLE treasury_entries"); statement.execute("DROP TABLE treasuries");
            }
            if (version < 15) {
                statement.execute("DROP TABLE farm_evidence"); statement.execute("DROP TABLE economy_runtime"); statement.execute("DROP TABLE economy_tuning");
                if (version == 14) for (var column : java.util.List.of("severe_bps", "surplus_bps", "abundant_bps", "severe_after"))
                    statement.execute("ALTER TABLE economy_periods DROP COLUMN " + column);
            }
            if (version < 14) {
                statement.execute("DROP TABLE economy_periods");
                statement.execute("DROP TABLE economy_stocks");
                statement.execute("DROP TABLE economy_accounts");
            }
            if (version < 13) {
                statement.execute("DROP TABLE resident_projection");
                statement.execute("DROP TABLE resident_lifecycle_events");
            }
            if (version == 11) statement.execute("ALTER TABLE village_beds DROP COLUMN household_id");
            if (version < 11) {
                statement.execute("DROP TABLE village_entities");
                statement.execute("DROP TABLE village_beds");
                statement.execute("DROP TABLE settlement_worlds");
                statement.execute("DROP TABLE observed_worlds");
            }
            if (version < 10) statement.execute("DROP TABLE civilization_time");
            if (version < 9) statement.execute("DROP TABLE resident_skills");
            if (version < 7) {
                statement.execute("DROP TRIGGER reject_mixed_aging_clock");
                statement.execute("DROP TABLE demographic_clock");
                statement.execute("DROP TABLE population_events");
                statement.execute("DROP TABLE household_capacity");
            }
            if (version < 6) statement.execute("DROP TABLE population_clocks");
            if (version < 5) statement.execute("DROP TABLE family_ties");
            statement.execute("PRAGMA user_version = " + version);
        }
    }
}
