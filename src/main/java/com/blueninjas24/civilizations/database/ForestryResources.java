package com.blueninjas24.civilizations.database;

import java.sql.*;

/** Empty additive forestry storage; existing businesses and resources are never inferred from old saves. */
final class ForestryResources {
    private ForestryResources() {}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE forestry_resources (
                    site_id TEXT PRIMARY KEY REFERENCES business_sites(id),world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    capacity INTEGER NOT NULL CHECK(typeof(capacity)='integer' AND capacity BETWEEN 1 AND 1000000000),
                    habitat_capacity INTEGER NOT NULL CHECK(typeof(habitat_capacity)='integer' AND habitat_capacity BETWEEN 0 AND capacity),
                    available INTEGER NOT NULL CHECK(typeof(available)='integer' AND available BETWEEN 0 AND habitat_capacity),
                    remainder INTEGER NOT NULL CHECK(typeof(remainder)='integer' AND remainder BETWEEN 0 AND 9999 AND (available<habitat_capacity OR remainder=0)),
                    observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                    opening_terms TEXT NOT NULL CHECK(length(opening_terms)>0)
                )
                """);
            s.execute("""
                CREATE TABLE forestry_supports (
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                    site_id TEXT NOT NULL REFERENCES forestry_resources(site_id),PRIMARY KEY(world_id,x,y,z)
                )
                """);
            s.execute("CREATE INDEX forestry_supports_site ON forestry_supports(site_id)");
            s.execute("""
                CREATE TABLE forestry_work (
                    operation_id TEXT PRIMARY KEY REFERENCES industry_operations(id),site_id TEXT NOT NULL REFERENCES forestry_resources(site_id),
                    business_id TEXT NOT NULL,period INTEGER NOT NULL,
                    opening_units INTEGER NOT NULL CHECK(opening_units>=0),recovered INTEGER NOT NULL CHECK(recovered>=0),
                    harvested INTEGER NOT NULL CHECK(harvested>=0),closing_units INTEGER NOT NULL CHECK(closing_units=opening_units+recovered-harvested AND closing_units>=0),
                    before_remainder INTEGER NOT NULL CHECK(before_remainder BETWEEN 0 AND 9999),
                    after_remainder INTEGER NOT NULL CHECK(after_remainder BETWEEN 0 AND 9999),policy TEXT NOT NULL,
                    FOREIGN KEY(business_id,period) REFERENCES industry_production(business_id,period)
                )
                """);
            s.execute("""
                CREATE TABLE forestry_habitat_changes (
                    operation_id TEXT PRIMARY KEY,site_id TEXT NOT NULL REFERENCES forestry_resources(site_id),evidence TEXT NOT NULL,
                    captured_ms INTEGER NOT NULL CHECK(captured_ms>=0),before_capacity INTEGER NOT NULL CHECK(before_capacity>=0),
                    after_capacity INTEGER NOT NULL CHECK(after_capacity>=0),opening_units INTEGER NOT NULL CHECK(opening_units>=0),
                    lost INTEGER NOT NULL CHECK(lost>=0),closing_units INTEGER NOT NULL CHECK(closing_units=opening_units-lost AND closing_units>=0),
                    before_remainder INTEGER NOT NULL CHECK(before_remainder BETWEEN 0 AND 9999),
                    after_remainder INTEGER NOT NULL CHECK(after_remainder BETWEEN 0 AND 9999)
                )
                """);
        }
    }
}
