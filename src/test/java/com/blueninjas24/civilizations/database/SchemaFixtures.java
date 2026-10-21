package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Reconstruct prior schema shapes only inside test-owned temporary databases. */
final class SchemaFixtures {
    private SchemaFixtures() {}
    /** Additive schemas95-110 must create only empty tables in earlier migration fixtures. */
    static void removeEmptyRecoveryAndAllocationAdditions(java.util.Map<String,? extends java.util.List<?>> snapshot) {
        for(var table:java.util.List.of("mineral_recoveries","resource_recovery_epochs","forest_recoveries",
                "business_allocation_state","business_allocation_reviews","resource_investment_state","resource_investment_reviews","generated_resource_chunks",
                "construction_drafts","construction_block_journal","construction_material_reservations","construction_material_items","construction_site_reservations","construction_material_returns","construction_supply_orders","construction_stage_work","construction_public_work","building_records","building_units","building_consent_events","building_exclusions","building_exclusion_releases",
                "dwelling_reviews","dwelling_review_units","dwelling_current_reviews","dwelling_assignments","dwelling_active_occupancy","dwelling_departures","household_capacity_sources","dwelling_bed_claims"))
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(),snapshot.remove(table),table+" must exist and start empty");
    }
    static void rewind(Connection connection, int version) throws SQLException {
        try (var statement = connection.createStatement()) {
            if(version<110){statement.execute("DROP TABLE IF EXISTS dwelling_bed_claims");statement.execute("DROP TABLE IF EXISTS household_capacity_sources");}
            if(version<109)for(var table:java.util.List.of("dwelling_departures","dwelling_active_occupancy","dwelling_assignments","dwelling_current_reviews","dwelling_review_units","dwelling_reviews"))statement.execute("DROP TABLE IF EXISTS "+table);
            if(version<108){statement.execute("DROP TABLE IF EXISTS building_exclusion_releases");statement.execute("DROP TABLE IF EXISTS building_exclusions");statement.execute("DROP TABLE IF EXISTS building_consent_events");statement.execute("DROP TABLE IF EXISTS building_units");statement.execute("DROP TABLE IF EXISTS building_records");}
            if(version<107){statement.execute("DROP TABLE IF EXISTS construction_public_work");statement.execute("DROP TABLE IF EXISTS construction_stage_work");}
            if(version<106)statement.execute("DROP TABLE IF EXISTS construction_supply_orders");
            if(version<105)statement.execute("DROP TABLE IF EXISTS construction_material_returns");
            if(version<104)statement.execute("DROP TABLE IF EXISTS construction_site_reservations");
            if(version<102){statement.execute("DROP TABLE IF EXISTS construction_material_items");statement.execute("DROP TABLE IF EXISTS construction_material_reservations");}
            if(version<101)statement.execute("DROP TABLE IF EXISTS construction_block_journal");
            if(version<100)statement.execute("DROP TABLE IF EXISTS construction_drafts");
            if(version<99)statement.execute("DROP TABLE IF EXISTS generated_resource_chunks");
            if(version<98){statement.execute("DROP TABLE IF EXISTS resource_investment_reviews");statement.execute("DROP TABLE IF EXISTS resource_investment_state");}
            if(version<97){statement.execute("DROP TABLE IF EXISTS business_allocation_reviews");statement.execute("DROP TABLE IF EXISTS business_allocation_state");}
            if(version<96)statement.execute("DROP TABLE IF EXISTS forest_recoveries");
            if(version<95){statement.execute("DROP TABLE IF EXISTS mineral_recoveries");statement.execute("DROP TABLE IF EXISTS resource_recovery_epochs");}
            if(version<94){statement.execute("DROP TABLE IF EXISTS forestry_founder_courses");statement.execute("DROP TABLE IF EXISTS forestry_founder_reviews");}
            if(version<93){statement.execute("DROP TABLE IF EXISTS forestry_source_losses");statement.execute("DROP TABLE IF EXISTS forestry_exclusions");statement.execute("DROP TABLE IF EXISTS forestry_tracking");}
            if(version<92){statement.execute("DROP TABLE IF EXISTS forestry_work");statement.execute("DROP TABLE IF EXISTS forestry_supports");statement.execute("DROP TABLE IF EXISTS forestry_habitat_changes");statement.execute("DROP TABLE IF EXISTS forestry_resources");}
            if(version<91){statement.execute("DROP TABLE IF EXISTS quarry_founder_courses");statement.execute("DROP TABLE IF EXISTS quarry_founder_reviews");statement.execute("DROP TABLE IF EXISTS quarry_survey_members");statement.execute("DROP TABLE IF EXISTS quarry_surveys");statement.execute("DROP TABLE IF EXISTS quarry_opportunities");}
            if(version<90)statement.execute("DROP TABLE IF EXISTS livestock_slaughter");
            if(version<89){statement.execute("DROP TABLE IF EXISTS livestock_learning_entries");statement.execute("DROP TABLE IF EXISTS livestock_learning_reviews");}
            if(version<88){
                statement.execute("DROP TRIGGER IF EXISTS active_pen_overlap");
                statement.execute("DROP TABLE IF EXISTS pen_releases");
                statement.execute("DROP TABLE IF EXISTS active_pen_claims");
                statement.execute("DROP INDEX IF EXISTS protected_pen_location");
                if(version>=75)statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS fixture_old_pen_location ON protected_pen_columns(world_id,standing_y,x,z)");
            }
            if(version<87)statement.execute("DROP TABLE IF EXISTS livestock_presence");
            if(version<86){statement.execute("DROP TABLE IF EXISTS livestock_birth_losses");statement.execute("DROP TABLE IF EXISTS livestock_birth_projections");statement.execute("DROP TABLE IF EXISTS livestock_birth_entities");}
            if(version<85)statement.execute("DROP TABLE IF EXISTS livestock_births");
            if(version<84)statement.execute("DROP TABLE IF EXISTS livestock_product_sales");
            if(version<83)statement.execute("DROP TABLE IF EXISTS livestock_capture");
            if(version<82)statement.execute("DROP TABLE IF EXISTS livestock_animal_losses");
            if(version<81){statement.execute("DROP TABLE IF EXISTS craftsman_founder_courses");statement.execute("DROP TABLE IF EXISTS craftsman_founder_reviews");}
            if(version<80)statement.execute("DROP TABLE IF EXISTS business_storage_upgrades");
            if(version<79){statement.execute("DROP TABLE IF EXISTS bakery_founder_courses");statement.execute("DROP TABLE IF EXISTS bakery_founder_reviews");statement.execute("DROP INDEX IF EXISTS bakery_paid_food_market");}
            if(version<78)statement.execute("DROP TABLE IF EXISTS bakery_private_starters");
            if(version<76){statement.execute("DROP TABLE IF EXISTS livestock_young");statement.execute("DROP TABLE IF EXISTS livestock_periods");statement.execute("DROP TABLE IF EXISTS livestock_initial_animals");statement.execute("DROP TABLE IF EXISTS livestock_herds");}
            if(version<75){statement.execute("DROP TABLE IF EXISTS protected_pen_animals");statement.execute("DROP TABLE IF EXISTS protected_pen_columns");statement.execute("DROP TABLE IF EXISTS protected_pens");}
            if(version<74)statement.execute("DROP TABLE IF EXISTS animal_ownership");
            if(version<73){statement.execute("DROP TABLE IF EXISTS farm_market_harvests");statement.execute("DROP TABLE IF EXISTS farm_market_stores");}
            if(version<72){statement.execute("DROP TABLE IF EXISTS grain_harvests");statement.execute("DROP TABLE IF EXISTS grain_stocks");}
            if(version<71)statement.execute("DROP TABLE IF EXISTS farm_grain_evidence");
            if(version<70)statement.execute("DROP TABLE IF EXISTS fishery_founder_courses");
            if(version<69){statement.execute("DROP TABLE fishery_workplace_history");statement.execute("DROP TABLE fishery_workplaces");}
            if(version<68){statement.execute("DROP TABLE water_columns");statement.execute("DROP TABLE water_surveys");}
            if(version<67){statement.execute("DROP TABLE fishery_habitat_changes");statement.execute("ALTER TABLE fishery_resources DROP COLUMN habitat_capacity");}
            if(version<66){statement.execute("DROP TABLE fishery_work");statement.execute("DROP TABLE fishery_columns");statement.execute("DROP TABLE fishery_resources");}
            if(version<65)statement.execute("DROP TABLE village_supply_orders");
            if(version<63){statement.execute("DROP TABLE warehouse_founder_courses");statement.execute("DROP TABLE warehouse_founder_reviews");}
            if(version<62){statement.execute("DROP TABLE workshop_founder_courses");statement.execute("DROP TABLE workshop_founder_reviews");}
            if(version<61){statement.execute("DROP TABLE workplace_operation_history");statement.execute("DROP TABLE business_workplace_anchors");statement.execute("DROP TABLE workplace_operation_settings");}
            if(version<60){statement.execute("DROP TABLE workshop_starter_goods");statement.execute("DROP TABLE workshop_starter_orders");}
            if(version<59){statement.execute("DROP TABLE mine_founder_courses");statement.execute("DROP TABLE mine_founder_reviews");}
            if(version<58){statement.execute("DROP TABLE workplace_markers");statement.execute("DROP TABLE workplace_sightings");statement.execute("DROP TABLE workplace_surveys");}
            if(version<57){statement.execute("DROP TABLE career_study_enrollments");statement.execute("DROP TABLE career_study_reviews");}
            if(version<56){statement.execute("DROP TABLE commerce_learning_entries");statement.execute("DROP TABLE commerce_learning_receipts");statement.execute("DROP TABLE commerce_learning_reviews");}
            if(version<55){statement.execute("DROP TABLE upkeep_contributions");statement.execute("DROP INDEX local_supplier_sales");}
            if(version<54)statement.execute("DROP TABLE farm_tool_work");
            if(version<53)statement.execute("DROP TABLE farm_tool_purchases");
            if(version<52)statement.execute("DROP TABLE business_hiring_reviews");
            if(version<51){statement.execute("DROP TABLE business_tool_reviews");statement.execute("DROP TABLE business_tools");}
            if(version<50){statement.execute("DROP TABLE mineral_survey_members");statement.execute("DROP TABLE mineral_surveys");}
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
