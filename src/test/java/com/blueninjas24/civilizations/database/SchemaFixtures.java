package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Reconstruct prior schema shapes only inside test-owned temporary databases. */
final class SchemaFixtures {
    private SchemaFixtures() {}
    static void rewind(Connection connection, int version) throws SQLException {
        try (var statement = connection.createStatement()) {
            if(version<49)statement.execute("DROP TABLE startup_grants");
            if(version<48)statement.execute("DROP TABLE mineral_opportunities");
            if(version<46){for(var table:new String[]{"mineral_source_retirements","mineral_source_extraction","mineral_sources","mineral_source_adoptions","ore_source_removals"})statement.execute("DROP TABLE "+table);}
            if(version<45){statement.execute("DROP TABLE farm_learning_entries");statement.execute("DROP TABLE farm_learning_reviews");}
            if(version<44){statement.execute("DROP TABLE business_lifecycle_reviews");statement.execute("DROP INDEX business_local_portfolio");}
            if(version<43){statement.execute("DROP TABLE study_spending");statement.execute("DROP TABLE study_reviews");statement.execute("DROP TABLE study_materials");statement.execute("DROP TABLE resident_courses");}
            if(version<42){statement.execute("DROP TABLE industry_learning_entries");statement.execute("DROP TABLE industry_learning_reviews");}
            if(version<41)statement.execute("DROP TABLE business_closures");
            if(version<40)statement.execute("DROP TABLE business_sale_listings");
            if(version<39){statement.execute("DROP TABLE business_sale_entries");statement.execute("DROP TABLE business_sales");statement.execute("DROP TABLE family_estates");}
            if(version<38)statement.execute("DROP TABLE business_ownership");
            if(version<37 && version>=36){statement.execute("DROP INDEX auto_shipment_haul_cadence");statement.execute("ALTER TABLE industry_auto_shipments DROP COLUMN haul_cadence");}
            if(version<36) {statement.execute("DROP TABLE industry_auto_shipments");statement.execute("DROP INDEX shipment_buyer_identity");statement.execute("DROP INDEX pending_local_shipments_buyer");}
            if(version<35) {statement.execute("DROP TABLE industry_runtime_history");statement.execute("DROP TABLE industry_runtime_tuning");statement.execute("DROP TABLE industry_runtime");}
            if(version<34)statement.execute("DROP TABLE ore_tracking");
            if(version<33)statement.execute("DROP TABLE player_ore_placements");
            if(version<32){statement.execute("DROP TABLE mineral_evidence");statement.execute("DROP TABLE mineral_discovery");}
            if(version<31){statement.execute("DROP TABLE recipe_flows");statement.execute("DROP TABLE recipe_runs");}
            if(version<30)for(var column:java.util.List.of("site_x","site_y","site_z"))statement.execute("ALTER TABLE farm_evidence DROP COLUMN "+column);
            if(version<29) {statement.execute("DROP TABLE population_active_worlds");statement.execute("DROP TABLE online_calendar");}
            if(version<28) {
                statement.execute("DROP TABLE local_shipments"); statement.execute("DROP TABLE industry_production"); statement.execute("DROP TABLE industry_operations");
                statement.execute("DROP TABLE industry_stock"); statement.execute("DROP TABLE industry_stores");
                statement.execute("DROP TABLE mineral_yields"); statement.execute("DROP TABLE mineral_deposits");
            }
            if (version < 27) {
                statement.execute("DROP TABLE business_employment"); statement.execute("DROP TABLE business_skill_requirements");
                statement.execute("DROP TABLE businesses"); statement.execute("DROP TABLE business_sites");
            }
            if (version < 26) {statement.execute("DROP TABLE food_response_candidates");statement.execute("DROP TABLE food_responses");}
            if (version < 25) { statement.execute("DROP TABLE priority_learning"); statement.execute("DROP INDEX applied_food_learning"); statement.execute("ALTER TABLE village_priorities DROP COLUMN learning_replay"); }
            if (version < 24) { statement.execute("DROP TABLE barter_stock_moves"); statement.execute("DROP TABLE food_barters"); }
            if (version < 23) statement.execute("DROP TABLE food_bargains");
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
