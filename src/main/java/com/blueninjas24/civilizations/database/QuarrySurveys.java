package com.blueninjas24.civilizations.database;

import java.sql.*;

/** Additive independent stone/sand frames; no changes to existing evidence, deposits or reserves. */
final class QuarrySurveys {
    private QuarrySurveys() {}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE quarry_opportunities (
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                    mineral TEXT NOT NULL CHECK(mineral IN('stone','sand')),
                    first_seen_millis INTEGER NOT NULL CHECK(typeof(first_seen_millis)='integer' AND first_seen_millis>=0),
                    last_seen_millis INTEGER NOT NULL CHECK(typeof(last_seen_millis)='integer' AND last_seen_millis>=first_seen_millis),
                    PRIMARY KEY(settlement_id,world_id,x,y,z)
                )
                """);
            s.execute("""
                CREATE TABLE quarry_surveys (
                    settlement_id TEXT NOT NULL,world_id TEXT NOT NULL,scope_x INTEGER NOT NULL,scope_z INTEGER NOT NULL,
                    captured_millis INTEGER NOT NULL CHECK(typeof(captured_millis)='integer' AND captured_millis>=0),
                    fingerprint TEXT NOT NULL CHECK(length(fingerprint)=64),PRIMARY KEY(settlement_id,scope_x,scope_z),UNIQUE(settlement_id,world_id,scope_x,scope_z),
                    FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
                )
                """);
            s.execute("""
                CREATE TABLE quarry_survey_members (
                    settlement_id TEXT NOT NULL,world_id TEXT NOT NULL,scope_x INTEGER NOT NULL,scope_z INTEGER NOT NULL,x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                    PRIMARY KEY(settlement_id,scope_x,scope_z,world_id,x,y,z),
                    FOREIGN KEY(settlement_id,world_id,scope_x,scope_z) REFERENCES quarry_surveys(settlement_id,world_id,scope_x,scope_z),
                    FOREIGN KEY(settlement_id,world_id,x,y,z) REFERENCES quarry_opportunities(settlement_id,world_id,x,y,z)
                )
                """);
            s.execute("""
                CREATE TABLE quarry_founder_reviews (
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),period INTEGER NOT NULL CHECK(period>=1),
                    opportunity TEXT NOT NULL,after_resident TEXT NOT NULL,policy TEXT NOT NULL,explanation TEXT NOT NULL,
                    PRIMARY KEY(settlement_id,period)
                )
                """);
            s.execute("""
                CREATE TABLE quarry_founder_courses (
                    course_id TEXT PRIMARY KEY REFERENCES resident_courses(id),settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),mineral TEXT NOT NULL CHECK(mineral IN('stone','sand')),
                    x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,enrolled_period INTEGER NOT NULL CHECK(enrolled_period>=1),
                    FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id),
                    FOREIGN KEY(settlement_id,enrolled_period) REFERENCES quarry_founder_reviews(settlement_id,period) DEFERRABLE INITIALLY DEFERRED
                )
                """);
            s.execute("CREATE INDEX active_quarry_founders ON quarry_founder_courses(settlement_id,course_id)");
        }
    }
}
