package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Initially empty, opportunity-linked forestry founder training records for schema 94. */
final class ForestryCourses {
    private ForestryCourses() {}

    static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE forestry_founder_reviews (
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=1),
                    opportunity TEXT NOT NULL,
                    after_resident TEXT NOT NULL,
                    policy TEXT NOT NULL,
                    explanation TEXT NOT NULL,
                    PRIMARY KEY(settlement_id,period)
                )
                """);
            statement.execute("""
                CREATE TABLE forestry_founder_courses (
                    course_id TEXT PRIMARY KEY REFERENCES resident_courses(id),
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    enrolled_period INTEGER NOT NULL CHECK(typeof(enrolled_period)='integer' AND enrolled_period>=1),
                    FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id),
                    FOREIGN KEY(settlement_id,enrolled_period) REFERENCES forestry_founder_reviews(settlement_id,period)
                        DEFERRABLE INITIALLY DEFERRED
                )
                """);
            statement.execute("CREATE INDEX active_forestry_founders ON forestry_founder_courses(settlement_id,course_id)");
        }
    }
}
