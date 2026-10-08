package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Reconstruct prior schema shapes only inside test-owned temporary databases. */
final class SchemaFixtures {
    private SchemaFixtures() {}
    static void rewind(Connection connection, int version) throws SQLException {
        try (var statement = connection.createStatement()) {
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
