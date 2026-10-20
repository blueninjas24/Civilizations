package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** SQLite schema upgrades; all pending steps and their version markers commit together. */
final class SchemaMigrator {
    static final int CURRENT_VERSION = 109;

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
                },
                connection -> {
                    for (var column : List.of("unassigned_demand", "unassigned_supplied"))
                        execute(connection, "ALTER TABLE needs_periods ADD COLUMN " + column + " INTEGER CHECK(" + column + " IS NULL OR (typeof(" + column + ")='integer' AND " + column + ">=0))");
                    for (var column : List.of("supplied", "unmet_periods"))
                        execute(connection, "ALTER TABLE household_needs ADD COLUMN " + column + " INTEGER CHECK(" + column + " IS NULL OR (typeof(" + column + ")='integer' AND " + column + ">=0))");
                },
                connection -> execute(connection, """
                        CREATE TABLE food_appeals (
                            settlement_id TEXT NOT NULL,
                            period INTEGER NOT NULL,
                            priority_period INTEGER NOT NULL CHECK(priority_period>0 AND priority_period<=period),
                            status TEXT NOT NULL CHECK(status IN ('NONE','OPEN','RESOLVED','WITHDRAWN')),
                            unmet INTEGER NOT NULL CHECK(typeof(unmet)='integer' AND unmet>=0),
                            previous_unmet INTEGER CHECK(previous_unmet IS NULL OR (typeof(previous_unmet)='integer' AND previous_unmet>=0)),
                            imports INTEGER NOT NULL CHECK(typeof(imports)='integer' AND imports>=0),
                            production INTEGER NOT NULL CHECK(typeof(production)='integer' AND production>=0),
                            change TEXT NOT NULL CHECK(change IN ('NONE','OPENED','IMPROVED','UNCHANGED','WORSENED','RESOLVED','WITHDRAWN')),
                            PRIMARY KEY(settlement_id,period),
                            FOREIGN KEY(settlement_id,period) REFERENCES needs_periods(settlement_id,period),
                            FOREIGN KEY(settlement_id,priority_period) REFERENCES village_priorities(settlement_id,period),
                            CHECK(status<>'OPEN' OR unmet>0), CHECK(status<>'RESOLVED' OR unmet=0)
                        )
                        """),
                connection -> {
                    execute(connection, """
                            CREATE TABLE food_purchases (
                                operation_id TEXT PRIMARY KEY NOT NULL,
                                player_id TEXT NOT NULL,
                                settlement_id TEXT NOT NULL REFERENCES economy_runtime(settlement_id),
                                units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units BETWEEN 1 AND 64000000),
                                item TEXT NOT NULL CHECK(length(item) BETWEEN 1 AND 32),
                                item_count INTEGER NOT NULL CHECK(typeof(item_count)='integer' AND item_count BETWEEN 1 AND 64),
                                emeralds INTEGER NOT NULL CHECK(typeof(emeralds)='integer' AND emeralds BETWEEN 1 AND 64),
                                rate INTEGER NOT NULL CHECK(typeof(rate)='integer' AND rate BETWEEN 1 AND 100000000),
                                status TEXT NOT NULL CHECK(status IN ('RESERVED','COMPLETED','CANCELLED')),
                                accepted_period INTEGER NOT NULL CHECK(typeof(accepted_period)='integer' AND accepted_period>0)
                            )
                            """);
                    execute(connection, "CREATE UNIQUE INDEX one_reserved_purchase_per_player ON food_purchases(player_id) WHERE status='RESERVED'");
                    execute(connection, "CREATE INDEX reserved_purchase_food ON food_purchases(settlement_id) WHERE status='RESERVED'");
                },
                connection -> {
                    execute(connection, """
                            CREATE TABLE household_wellbeing (
                                sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                                settlement_id TEXT NOT NULL,
                                period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>0),
                                household_id TEXT NOT NULL REFERENCES households(id),
                                before_score INTEGER NOT NULL CHECK(before_score BETWEEN 0 AND 100),
                                after_score INTEGER NOT NULL CHECK(after_score BETWEEN 0 AND 100),
                                condition TEXT NOT NULL CHECK(condition IN ('UNKNOWN','SECURE','PRESSURED','FOOD_CRISIS')),
                                food TEXT NOT NULL CHECK(food IN ('UNKNOWN','SUPPLIED','UNDERSUPPLIED')),
                                housing TEXT NOT NULL CHECK(housing IN ('UNKNOWN','SUFFICIENT','CROWDED')),
                                starting_score INTEGER NOT NULL CHECK(starting_score BETWEEN 0 AND 100),
                                recovery INTEGER NOT NULL CHECK(recovery BETWEEN 0 AND 100),
                                food_penalty INTEGER NOT NULL CHECK(food_penalty BETWEEN 0 AND 100),
                                crisis_penalty INTEGER NOT NULL CHECK(crisis_penalty BETWEEN food_penalty AND 100),
                                crowding_penalty INTEGER NOT NULL CHECK(crowding_penalty BETWEEN 0 AND 100),
                                crisis_after INTEGER NOT NULL CHECK(typeof(crisis_after)='integer' AND crisis_after>0),
                                food_streak INTEGER NOT NULL CHECK(typeof(food_streak)='integer' AND food_streak>=0),
                                UNIQUE(settlement_id,period,household_id),
                                FOREIGN KEY(settlement_id,period) REFERENCES needs_periods(settlement_id,period)
                            )
                            """);
                    execute(connection,"CREATE INDEX latest_household_wellbeing ON household_wellbeing(household_id,sequence DESC)");
                    execute(connection,"""
                            CREATE TABLE wellbeing_members (
                                sequence INTEGER NOT NULL REFERENCES household_wellbeing(sequence),
                                resident_id TEXT NOT NULL REFERENCES residents(id),
                                food_streak INTEGER NOT NULL CHECK(typeof(food_streak)='integer' AND food_streak>=0),
                                PRIMARY KEY(sequence,resident_id)
                            )
                            """);
                    execute(connection,"CREATE INDEX latest_resident_wellbeing ON wellbeing_members(resident_id,sequence DESC)");
                },
                connection -> {
                    execute(connection,"""
                            CREATE TABLE food_bargains (
                                sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                                operation_id TEXT UNIQUE NOT NULL, player_id TEXT NOT NULL,
                                settlement_id TEXT NOT NULL REFERENCES economy_runtime(settlement_id),
                                units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units BETWEEN 1 AND 64000000),
                                item TEXT NOT NULL CHECK(length(item) BETWEEN 1 AND 32),
                                item_count INTEGER NOT NULL CHECK(typeof(item_count)='integer' AND item_count BETWEEN 1 AND 64),
                                requested INTEGER NOT NULL CHECK(typeof(requested)='integer' AND requested BETWEEN 1 AND 64),
                                offered INTEGER NOT NULL CHECK(typeof(offered)='integer' AND offered BETWEEN 0 AND 64),
                                rate INTEGER NOT NULL CHECK(typeof(rate)='integer' AND rate BETWEEN 1 AND 100000000),
                                status TEXT NOT NULL CHECK(status IN ('AGREED','COUNTER','CLOSED','RESERVED')),
                                failed_attempts INTEGER NOT NULL CHECK(typeof(failed_attempts)='integer' AND failed_attempts BETWEEN 0 AND 100),
                                observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                                expires_millis INTEGER NOT NULL CHECK(typeof(expires_millis)='integer' AND expires_millis>=observed_millis),
                                cooldown_until INTEGER NOT NULL CHECK(typeof(cooldown_until)='integer' AND cooldown_until>=0),
                                maximum_attempts INTEGER NOT NULL CHECK(typeof(maximum_attempts)='integer' AND maximum_attempts BETWEEN 1 AND 100),
                                cooldown_seconds INTEGER NOT NULL CHECK(typeof(cooldown_seconds)='integer' AND cooldown_seconds BETWEEN 1 AND 86400),
                                offer_seconds INTEGER NOT NULL CHECK(typeof(offer_seconds)='integer' AND offer_seconds BETWEEN 1 AND 3600)
                            )
                            """);
                    execute(connection,"CREATE INDEX current_player_bargain ON food_bargains(player_id,settlement_id,sequence DESC)");
                },
                connection -> {
                    execute(connection,"""
                            CREATE TABLE food_barters (
                                operation_id TEXT PRIMARY KEY NOT NULL, player_id TEXT NOT NULL,
                                settlement_id TEXT NOT NULL REFERENCES economy_runtime(settlement_id),
                                food_units INTEGER NOT NULL CHECK(typeof(food_units)='integer' AND food_units BETWEEN 1 AND 64000000),
                                food_item TEXT NOT NULL CHECK(length(food_item) BETWEEN 1 AND 32),
                                food_count INTEGER NOT NULL CHECK(typeof(food_count)='integer' AND food_count BETWEEN 1 AND 64),
                                commodity TEXT NOT NULL, payment_item TEXT NOT NULL,
                                payment_count INTEGER NOT NULL CHECK(typeof(payment_count)='integer' AND payment_count BETWEEN 1 AND 64),
                                units_per_item INTEGER NOT NULL CHECK(typeof(units_per_item)='integer' AND units_per_item BETWEEN 1 AND 1000000),
                                status TEXT NOT NULL CHECK(status IN ('RESERVED','COMPLETED','CANCELLED')),
                                accepted_period INTEGER NOT NULL CHECK(typeof(accepted_period)='integer' AND accepted_period>=0),
                                FOREIGN KEY(settlement_id,commodity) REFERENCES economy_stocks(settlement_id,commodity),
                                CHECK((commodity='timber' AND payment_item='OAK_LOG') OR (commodity='stone' AND payment_item='COBBLESTONE'))
                            )
                            """);
                    execute(connection,"CREATE UNIQUE INDEX one_reserved_barter_per_player ON food_barters(player_id) WHERE status='RESERVED'");
                    execute(connection,"CREATE INDEX reserved_barter_food ON food_barters(settlement_id) WHERE status='RESERVED'");
                    execute(connection,"""
                            CREATE TABLE barter_stock_moves (
                                sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                                operation_id TEXT NOT NULL REFERENCES food_barters(operation_id),
                                phase TEXT NOT NULL CHECK(phase IN ('RESERVE','CANCEL')),
                                after_period INTEGER NOT NULL CHECK(typeof(after_period)='integer' AND after_period>=0),
                                before_units INTEGER NOT NULL CHECK(typeof(before_units)='integer' AND before_units>=0),
                                after_units INTEGER NOT NULL CHECK(typeof(after_units)='integer' AND after_units>=0),
                                UNIQUE(operation_id,phase),
                                CHECK((phase='RESERVE' AND after_units<before_units) OR (phase='CANCEL' AND after_units>before_units))
                            )
                            """);
                },
                connection -> {
                    execute(connection,"ALTER TABLE village_priorities ADD COLUMN learning_replay INTEGER NOT NULL DEFAULT 0 CHECK(learning_replay IN (0,1))");
                    execute(connection,"""
                            CREATE TABLE priority_learning (
                                settlement_id TEXT NOT NULL, period INTEGER NOT NULL,
                                replay_version INTEGER NOT NULL CHECK(replay_version=1),
                                window_periods INTEGER NOT NULL CHECK(typeof(window_periods)='integer' AND window_periods BETWEEN 1 AND 100),
                                maximum_bonus INTEGER NOT NULL CHECK(typeof(maximum_bonus)='integer' AND maximum_bonus BETWEEN 1 AND 100),
                                imported INTEGER NOT NULL CHECK(typeof(imported)='integer' AND imported>=0),
                                avoided_unmet INTEGER NOT NULL CHECK(typeof(avoided_unmet)='integer' AND avoided_unmet BETWEEN 0 AND imported),
                                PRIMARY KEY(settlement_id,period),
                                FOREIGN KEY(settlement_id,period) REFERENCES village_priorities(settlement_id,period)
                            )
                            """);
                    execute(connection,"CREATE INDEX applied_food_learning ON food_deliveries(settlement_id,applied_period)");
                },
                connection -> {
                    execute(connection,"""
                            CREATE TABLE food_responses (
                                settlement_id TEXT NOT NULL,period INTEGER NOT NULL,seed INTEGER NOT NULL CHECK(typeof(seed)='integer'),
                                replay_version INTEGER NOT NULL CHECK(replay_version=1),
                                relative_percent INTEGER NOT NULL CHECK(relative_percent BETWEEN 0 AND 100),
                                selected TEXT NOT NULL CHECK(selected IN ('WAIT','REQUEST_DONATION','OFFER_EMERALDS','OFFER_TIMBER','OFFER_STONE')),
                                PRIMARY KEY(settlement_id,period),FOREIGN KEY(settlement_id,period) REFERENCES needs_periods(settlement_id,period)
                            )
                            """);
                    execute(connection,"""
                            CREATE TABLE food_response_candidates (
                                settlement_id TEXT NOT NULL,period INTEGER NOT NULL,
                                action TEXT NOT NULL CHECK(action IN ('WAIT','REQUEST_DONATION','OFFER_EMERALDS','OFFER_TIMBER','OFFER_STONE')),
                                feasible INTEGER NOT NULL CHECK(feasible IN (0,1)),
                                weight INTEGER NOT NULL CHECK(typeof(weight)='integer' AND weight BETWEEN 1 AND 1000),
                                reason TEXT NOT NULL CHECK(length(reason) BETWEEN 1 AND 2048),
                                PRIMARY KEY(settlement_id,period,action),FOREIGN KEY(settlement_id,period) REFERENCES food_responses(settlement_id,period)
                            )
                            """);
                },
                SchemaMigrator::createBusinesses,
                SchemaMigrator::createIndustryStores,
                connection -> {
                    execute(connection,"""
                            CREATE TABLE online_calendar (
                                id INTEGER PRIMARY KEY CHECK(id=1), replay_version INTEGER NOT NULL CHECK(replay_version=1),
                                adopted_millis INTEGER NOT NULL CHECK(typeof(adopted_millis)='integer' AND adopted_millis>=0),
                                legacy_target INTEGER NOT NULL CHECK(typeof(legacy_target)='integer' AND legacy_target>=0),
                                adopted_completed INTEGER NOT NULL CHECK(typeof(adopted_completed)='integer' AND adopted_completed>=0 AND adopted_completed<=legacy_target)
                            )
                            """);
                    execute(connection,"""
                            CREATE TABLE population_active_worlds (
                                from_step INTEGER PRIMARY KEY CHECK(from_step>0),
                                through_step INTEGER NOT NULL CHECK(typeof(through_step)='integer' AND through_step>=from_step), worlds TEXT NOT NULL
                            )
                            """);
                    execute(connection,"CREATE INDEX population_world_queue ON population_active_worlds(through_step)");
                },
                connection -> {
                    for(var coordinate:List.of("site_x","site_y","site_z"))
                        execute(connection,"ALTER TABLE farm_evidence ADD COLUMN "+coordinate+" INTEGER CHECK("+coordinate+" IS NULL OR typeof("+coordinate+")='integer')");
                },
                connection -> {
                    execute(connection,"""
                            CREATE TABLE recipe_runs (
                                operation_id TEXT PRIMARY KEY REFERENCES industry_operations(id),
                                business_id TEXT NOT NULL REFERENCES businesses(id),
                                period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=0),
                                recipe_id TEXT NOT NULL CHECK(length(recipe_id) BETWEEN 1 AND 64),
                                replay_version INTEGER NOT NULL CHECK(replay_version=1),
                                requested INTEGER NOT NULL CHECK(typeof(requested)='integer' AND requested>=0),
                                workers INTEGER NOT NULL CHECK(typeof(workers)='integer' AND workers>=0),
                                batches_per_worker INTEGER NOT NULL CHECK(typeof(batches_per_worker)='integer' AND batches_per_worker>0),
                                site_capacity INTEGER NOT NULL CHECK(typeof(site_capacity)='integer' AND site_capacity>0),
                                UNIQUE(business_id,period)
                            )
                            """);
                    execute(connection,"""
                            CREATE TABLE recipe_flows (
                                operation_id TEXT NOT NULL REFERENCES recipe_runs(operation_id),
                                commodity TEXT NOT NULL CHECK(length(commodity) BETWEEN 1 AND 64),
                                direction TEXT NOT NULL CHECK(direction IN ('INPUT','OUTPUT')),
                                per_batch INTEGER NOT NULL CHECK(typeof(per_batch)='integer' AND per_batch>0),
                                units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>=0),
                            PRIMARY KEY(operation_id,commodity,direction)
                            )
                            """);
                },
                connection -> {
                    execute(connection,"""
                            CREATE TABLE mineral_discovery (
                                deposit_id TEXT PRIMARY KEY NOT NULL REFERENCES mineral_deposits(id),
                                world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                                mineral TEXT NOT NULL CHECK(mineral IN ('coal','iron')),
                                observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                                UNIQUE(deposit_id,world_id)
                            )
                            """);
                    execute(connection,"""
                            CREATE TABLE mineral_evidence (
                                world_id TEXT NOT NULL, deposit_id TEXT NOT NULL,
                                x INTEGER NOT NULL CHECK(typeof(x)='integer'),
                                y INTEGER NOT NULL CHECK(typeof(y)='integer'),
                                z INTEGER NOT NULL CHECK(typeof(z)='integer'),
                                observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                                PRIMARY KEY(world_id,x,y,z),
                                FOREIGN KEY(deposit_id,world_id) REFERENCES mineral_discovery(deposit_id,world_id)
                            )
                            """);
                    execute(connection,"CREATE INDEX mineral_evidence_by_deposit ON mineral_evidence(deposit_id)");
                },
                connection -> {
                    execute(connection,"""
                            CREATE TABLE player_ore_placements (
                                world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                                x INTEGER NOT NULL CHECK(typeof(x)='integer'),
                                y INTEGER NOT NULL CHECK(typeof(y)='integer'),
                                z INTEGER NOT NULL CHECK(typeof(z)='integer'),
                                observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                                PRIMARY KEY(world_id,x,y,z)
                            )
                            """);
                },
                connection -> execute(connection,"""
                        CREATE TABLE ore_tracking (
                            id INTEGER PRIMARY KEY CHECK(id=1),
                            clean_stop INTEGER NOT NULL CHECK(clean_stop IN (0,1)),
                            trusted INTEGER NOT NULL CHECK(trusted IN (0,1)),
                            observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0)
                        )
                        """),
                connection -> {
                    execute(connection,"""
                            CREATE TABLE industry_runtime (
                                business_id TEXT PRIMARY KEY NOT NULL REFERENCES industry_stores(business_id),
                                cadence INTEGER NOT NULL CHECK(typeof(cadence)='integer' AND cadence BETWEEN 1000 AND 86400000),
                                last_ms INTEGER NOT NULL CHECK(typeof(last_ms)='integer' AND last_ms>=0),
                                remainder INTEGER NOT NULL CHECK(typeof(remainder)='integer' AND remainder>=0 AND remainder<cadence),
                                target INTEGER NOT NULL CHECK(typeof(target)='integer' AND target>=0),
                                completed INTEGER NOT NULL CHECK(typeof(completed)='integer' AND completed>=0 AND completed<=target)
                            )
                            """);
                    execute(connection,"""
                            CREATE TABLE industry_runtime_history (
                                business_id TEXT NOT NULL REFERENCES industry_runtime(business_id),
                                period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>0),
                                explanation TEXT NOT NULL CHECK(length(explanation) BETWEEN 1 AND 1024),
                                PRIMARY KEY(business_id,period)
                            )
                            """);
                    execute(connection,"""
                            CREATE TABLE industry_runtime_tuning (
                                sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                                business_id TEXT NOT NULL REFERENCES industry_runtime(business_id),
                                after_period INTEGER NOT NULL CHECK(typeof(after_period)='integer' AND after_period>=0),
                                old_cadence INTEGER NOT NULL CHECK(typeof(old_cadence)='integer' AND old_cadence BETWEEN 1000 AND 86400000),
                                new_cadence INTEGER NOT NULL CHECK(typeof(new_cadence)='integer' AND new_cadence BETWEEN 1000 AND 86400000),
                                discarded_remainder INTEGER NOT NULL CHECK(typeof(discarded_remainder)='integer' AND discarded_remainder>=0 AND discarded_remainder<old_cadence),
                                observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0)
                            )
                            """);
                },
                connection -> {
                    execute(connection,"CREATE UNIQUE INDEX shipment_buyer_identity ON local_shipments(id,buyer_id)");
                    execute(connection,"""
                            CREATE TABLE industry_auto_shipments (
                                shipment_id TEXT PRIMARY KEY NOT NULL,
                                clock_business_id TEXT NOT NULL REFERENCES industry_runtime(business_id),
                                haul_window INTEGER NOT NULL CHECK(typeof(haul_window)='integer' AND haul_window>=0),
                                FOREIGN KEY(shipment_id,clock_business_id) REFERENCES local_shipments(id,buyer_id)
                            )
                            """);
                    execute(connection,"CREATE INDEX auto_shipment_clock ON industry_auto_shipments(clock_business_id,shipment_id)");
                    execute(connection,"CREATE INDEX auto_shipment_haul ON industry_auto_shipments(haul_window,shipment_id)");
                    execute(connection,"CREATE INDEX pending_local_shipments_buyer ON local_shipments(buyer_id,due_period,id) WHERE delivered_period IS NULL");
                },
                connection -> {
                    // Legacy rows lack guaranteed cadence: retain cargo without guessing historical terms.
                    execute(connection,"ALTER TABLE industry_auto_shipments ADD COLUMN haul_cadence INTEGER CHECK(haul_cadence IS NULL OR (typeof(haul_cadence)='integer' AND haul_cadence BETWEEN 1000 AND 86400000))");
                    execute(connection,"CREATE INDEX auto_shipment_haul_cadence ON industry_auto_shipments(haul_cadence,haul_window,shipment_id)");
                },
                connection -> {
                    execute(connection,"""
                            CREATE TABLE business_ownership (
                                operation_id TEXT PRIMARY KEY,
                                business_id TEXT NOT NULL REFERENCES businesses(id),
                                previous_owner TEXT NOT NULL REFERENCES residents(id),
                                next_owner TEXT NOT NULL REFERENCES residents(id),
                                observed_step INTEGER NOT NULL CHECK(typeof(observed_step)='integer' AND observed_step>=0),
                                kind TEXT NOT NULL CHECK(kind IN ('FAMILY','EMPLOYEE')),
                                CHECK(previous_owner<>next_owner),
                                UNIQUE(business_id,previous_owner)
                            )
                            """);
                    execute(connection,"CREATE INDEX business_owner_history ON business_ownership(business_id,observed_step,operation_id)");
                },
                SchemaMigrator::createBusinessSales,
                connection -> {
                    execute(connection,"""
                            CREATE TABLE business_sale_listings (
                                business_id TEXT NOT NULL REFERENCES businesses(id),
                                seller_id TEXT NOT NULL REFERENCES residents(id),
                                price INTEGER NOT NULL CHECK(typeof(price)='integer' AND price>0),
                                deadline_ms INTEGER NOT NULL CHECK(typeof(deadline_ms)='integer' AND deadline_ms BETWEEN 1000 AND 86400000),
                                created_step INTEGER NOT NULL CHECK(typeof(created_step)='integer' AND created_step>=0),
                                last_ms INTEGER NOT NULL CHECK(typeof(last_ms)='integer' AND last_ms>=0),
                                elapsed_ms INTEGER NOT NULL CHECK(typeof(elapsed_ms)='integer' AND elapsed_ms>=0 AND elapsed_ms<=deadline_ms),
                                status TEXT NOT NULL CHECK(status IN ('OPEN','SOLD','RESOLVED','EXPIRED')),
                                sale_operation TEXT REFERENCES business_sales(operation_id),
                                CHECK((status='SOLD' AND sale_operation IS NOT NULL) OR (status<>'SOLD' AND sale_operation IS NULL)),
                                CHECK(status<>'EXPIRED' OR elapsed_ms=deadline_ms),
                                PRIMARY KEY(business_id,seller_id)
                            )
                            """);
                    execute(connection,"CREATE INDEX business_open_sale_listings ON business_sale_listings(last_ms,business_id) WHERE status='OPEN'");
                },
                connection -> {
                    execute(connection,"""
                            CREATE TABLE business_closures (
                                business_id TEXT PRIMARY KEY NOT NULL REFERENCES businesses(id),
                                seller_id TEXT NOT NULL REFERENCES residents(id),
                                observed_step INTEGER NOT NULL CHECK(typeof(observed_step)='integer' AND observed_step>=0),
                                reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 2048),
                                FOREIGN KEY(business_id,seller_id) REFERENCES business_sale_listings(business_id,seller_id)
                            )
                            """);
                    execute(connection,"CREATE INDEX business_closure_history ON business_closures(observed_step,business_id)");
                },
                connection -> {
                    execute(connection,"""
                            CREATE TABLE industry_learning_reviews (
                                operation_id TEXT PRIMARY KEY NOT NULL REFERENCES industry_production(operation_id),
                                business_id TEXT NOT NULL REFERENCES businesses(id),
                                period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=0),
                                enabled INTEGER NOT NULL CHECK(enabled IN (0,1)),
                                skill TEXT CHECK(skill IS NULL OR length(trim(skill)) BETWEEN 1 AND 64),
                                gain INTEGER NOT NULL CHECK(typeof(gain)='integer' AND gain BETWEEN 1 AND 1000),
                                ceiling INTEGER NOT NULL CHECK(typeof(ceiling)='integer' AND ceiling BETWEEN 1 AND 1000000000),
                                worker_budget INTEGER NOT NULL CHECK(typeof(worker_budget)='integer' AND worker_budget BETWEEN 1 AND 1000),
                                explanation TEXT NOT NULL CHECK(length(trim(explanation)) BETWEEN 1 AND 2048),
                                FOREIGN KEY(business_id,period) REFERENCES industry_production(business_id,period)
                            )
                            """);
                    execute(connection,"""
                            CREATE TABLE industry_learning_entries (
                                operation_id TEXT NOT NULL REFERENCES industry_learning_reviews(operation_id),
                                resident_id TEXT NOT NULL REFERENCES residents(id),
                                before_experience INTEGER NOT NULL CHECK(typeof(before_experience)='integer' AND before_experience>=0),
                                after_experience INTEGER NOT NULL CHECK(typeof(after_experience)='integer' AND after_experience>before_experience),
                                PRIMARY KEY(operation_id,resident_id)
                            )
                            """);
                    execute(connection,"CREATE INDEX resident_industry_learning ON industry_learning_entries(resident_id,operation_id)");
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE resident_courses (
                            id TEXT PRIMARY KEY NOT NULL,
                            resident_id TEXT NOT NULL REFERENCES residents(id),
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),
                            skill TEXT NOT NULL CHECK(length(trim(skill)) BETWEEN 1 AND 64),
                            target INTEGER NOT NULL CHECK(typeof(target)='integer' AND target BETWEEN 1 AND 1000000000),
                            gain INTEGER NOT NULL CHECK(typeof(gain)='integer' AND gain BETWEEN 1 AND 1000),
                            eligible_after INTEGER NOT NULL CHECK(typeof(eligible_after)='integer' AND eligible_after>=0),
                            status TEXT NOT NULL CHECK(status IN ('ACTIVE','COMPLETED','CANCELLED'))
                        )
                        """);
                    execute(connection,"CREATE UNIQUE INDEX active_resident_course ON resident_courses(resident_id) WHERE status='ACTIVE'");
                    execute(connection,"CREATE INDEX village_active_courses ON resident_courses(settlement_id,id) WHERE status='ACTIVE'");
                    execute(connection,"""
                        CREATE TABLE study_materials (
                            course_id TEXT NOT NULL REFERENCES resident_courses(id),
                            commodity TEXT NOT NULL CHECK(commodity<>'food'),
                            units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units BETWEEN 1 AND 1000000),
                            PRIMARY KEY(course_id,commodity)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE study_reviews (
                            course_id TEXT NOT NULL REFERENCES resident_courses(id),
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),
                            period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>0),
                            before_experience INTEGER NOT NULL CHECK(typeof(before_experience)='integer' AND before_experience>=0),
                            after_experience INTEGER NOT NULL CHECK(typeof(after_experience)='integer' AND after_experience>=before_experience),
                            status TEXT NOT NULL CHECK(status IN ('ACTIVE','COMPLETED','CANCELLED')),
                            reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 2048),
                            food_reserve INTEGER NOT NULL CHECK(food_reserve BETWEEN 0 AND 100),
                            material_reserve INTEGER NOT NULL CHECK(material_reserve BETWEEN 0 AND 1000000000),
                            student_budget INTEGER NOT NULL CHECK(student_budget BETWEEN 1 AND 32),
                            food_commodity TEXT NOT NULL DEFAULT 'food' CHECK(food_commodity='food'),
                            PRIMARY KEY(course_id,period),
                            FOREIGN KEY(settlement_id,food_commodity,period) REFERENCES economy_periods(settlement_id,commodity,period)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE study_spending (
                            course_id TEXT NOT NULL,
                            period INTEGER NOT NULL,
                            commodity TEXT NOT NULL CHECK(commodity<>'food'),
                            units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units BETWEEN 1 AND 1000000),
                            PRIMARY KEY(course_id,period,commodity),
                            FOREIGN KEY(course_id,period) REFERENCES study_reviews(course_id,period),
                            FOREIGN KEY(course_id,commodity) REFERENCES study_materials(course_id,commodity)
                        )
                        """);
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE business_lifecycle_reviews (
                            business_id TEXT PRIMARY KEY NOT NULL REFERENCES businesses(id),
                            seller_id TEXT NOT NULL REFERENCES residents(id),
                            buyer_cursor TEXT NOT NULL,
                            step INTEGER NOT NULL CHECK(typeof(step)='integer' AND step>=0),
                            observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                            seed INTEGER NOT NULL CHECK(typeof(seed)='integer'),
                            reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 2048)
                        )
                        """);
                    execute(connection,"CREATE INDEX business_local_portfolio ON businesses(owner_id,settlement_id,id)");
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE farm_learning_reviews (
                            settlement_id TEXT NOT NULL,
                            period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>0),
                            commodity TEXT NOT NULL CHECK(commodity='food'),
                            business_id TEXT NOT NULL REFERENCES businesses(id),
                            field_slots INTEGER NOT NULL CHECK(typeof(field_slots)='integer' AND field_slots>=0),
                            production INTEGER NOT NULL CHECK(typeof(production)='integer' AND production>=0),
                            enabled INTEGER NOT NULL CHECK(enabled IN (0,1)),
                            skill TEXT CHECK(skill IS NULL OR length(trim(skill)) BETWEEN 1 AND 64),
                            gain INTEGER NOT NULL CHECK(typeof(gain)='integer' AND gain BETWEEN 1 AND 1000),
                            ceiling INTEGER NOT NULL CHECK(typeof(ceiling)='integer' AND ceiling BETWEEN 1 AND 1000000000),
                            worker_budget INTEGER NOT NULL CHECK(typeof(worker_budget)='integer' AND worker_budget BETWEEN 1 AND 1000),
                            explanation TEXT NOT NULL CHECK(length(trim(explanation)) BETWEEN 1 AND 2048),
                            PRIMARY KEY(settlement_id,period),
                            FOREIGN KEY(settlement_id,period,commodity) REFERENCES economy_periods(settlement_id,period,commodity)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE farm_learning_entries (
                            settlement_id TEXT NOT NULL,
                            period INTEGER NOT NULL,
                            resident_id TEXT NOT NULL REFERENCES residents(id),
                            before_experience INTEGER NOT NULL CHECK(typeof(before_experience)='integer' AND before_experience>=0),
                            after_experience INTEGER NOT NULL CHECK(typeof(after_experience)='integer' AND after_experience>before_experience),
                            PRIMARY KEY(settlement_id,period,resident_id),
                            FOREIGN KEY(settlement_id,period) REFERENCES farm_learning_reviews(settlement_id,period)
                        )
                        """);
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE mineral_source_adoptions (
                            deposit_id TEXT PRIMARY KEY NOT NULL REFERENCES mineral_deposits(id),
                            remaining_batches INTEGER NOT NULL CHECK(typeof(remaining_batches)='integer' AND remaining_batches>=0),
                            source_count INTEGER NOT NULL CHECK(typeof(source_count)='integer' AND source_count BETWEEN 1 AND 65536),
                            explanation TEXT NOT NULL CHECK(length(trim(explanation)) BETWEEN 1 AND 256)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE mineral_sources (
                            world_id TEXT NOT NULL, x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                            deposit_id TEXT NOT NULL REFERENCES mineral_deposits(id),
                            allocated_batches INTEGER NOT NULL CHECK(typeof(allocated_batches)='integer' AND allocated_batches>=0),
                            remaining_batches INTEGER NOT NULL CHECK(typeof(remaining_batches)='integer' AND remaining_batches BETWEEN 0 AND allocated_batches),
                            PRIMARY KEY(world_id,x,y,z),
                            FOREIGN KEY(world_id,x,y,z) REFERENCES mineral_evidence(world_id,x,y,z)
                        )
                        """);
                    execute(connection,"CREATE INDEX mineral_sources_by_deposit ON mineral_sources(deposit_id)");
                    execute(connection,"""
                        CREATE TABLE mineral_source_extraction (
                            operation_id TEXT NOT NULL REFERENCES industry_operations(id),
                            world_id TEXT NOT NULL,x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                            before_batches INTEGER NOT NULL CHECK(typeof(before_batches)='integer' AND before_batches>0),
                            after_batches INTEGER NOT NULL CHECK(typeof(after_batches)='integer' AND after_batches>=0 AND after_batches<before_batches),
                            PRIMARY KEY(operation_id,world_id,x,y,z),
                            FOREIGN KEY(world_id,x,y,z) REFERENCES mineral_sources(world_id,x,y,z)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE ore_source_removals (
                            world_id TEXT NOT NULL REFERENCES observed_worlds(id),x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                            observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                            reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 256),
                            PRIMARY KEY(world_id,x,y,z)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE mineral_source_retirements (
                            world_id TEXT NOT NULL,x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                            removed_batches INTEGER NOT NULL CHECK(typeof(removed_batches)='integer' AND removed_batches>=0),
                            PRIMARY KEY(world_id,x,y,z),
                            FOREIGN KEY(world_id,x,y,z) REFERENCES mineral_sources(world_id,x,y,z),
                            FOREIGN KEY(world_id,x,y,z) REFERENCES ore_source_removals(world_id,x,y,z)
                        )
                        """);
                    try(var q=connection.createStatement();var r=q.executeQuery("SELECT 1 FROM mineral_evidence GROUP BY deposit_id HAVING COUNT(*)>65536 LIMIT 1")) {
                        if(r.next())throw new SQLException("Legacy source count exceeds adoption budget; preserve save for review");
                    }
                    execute(connection,"""
                        INSERT INTO mineral_source_adoptions
                        SELECT d.id,d.remaining_batches,COUNT(*),'Schema 46: even allocation of saved remaining reserve; history retained'
                        FROM mineral_deposits d JOIN mineral_evidence e ON e.deposit_id=d.id GROUP BY d.id
                        """);
                    execute(connection,"""
                        INSERT INTO mineral_sources
                        SELECT world_id,x,y,z,deposit_id,share,share FROM (
                            SELECT e.*,d.remaining_batches / COUNT(*) OVER(PARTITION BY d.id) +
                                CASE WHEN ROW_NUMBER() OVER(PARTITION BY d.id ORDER BY world_id,x,y,z)<=
                                    d.remaining_batches % COUNT(*) OVER(PARTITION BY d.id) THEN 1 ELSE 0 END AS share
                            FROM mineral_evidence e JOIN mineral_deposits d ON d.id=e.deposit_id
                        )
                        """);
                },
                connection -> {
                    // Older capture certified placements, not removal of saved supporting blocks.
                    // Never silently certify old coordinate reserves under the stronger contract.
                    execute(connection,"""
                        INSERT INTO ore_tracking(id,clean_stop,trusted,observed_millis)
                        SELECT 1,1,0,0 WHERE EXISTS(SELECT 1 FROM mineral_sources)
                        ON CONFLICT(id) DO UPDATE SET trusted=0
                        """);
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE mineral_opportunities (
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),
                            world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                            x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL,
                            mineral TEXT NOT NULL CHECK(mineral IN ('coal','iron')),
                            first_seen_millis INTEGER NOT NULL CHECK(typeof(first_seen_millis)='integer' AND first_seen_millis>=0),
                            last_seen_millis INTEGER NOT NULL CHECK(typeof(last_seen_millis)='integer' AND last_seen_millis>=first_seen_millis),
                            PRIMARY KEY(settlement_id,world_id,x,y,z)
                        )
                        """);
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE startup_grants (
                            business_id TEXT PRIMARY KEY REFERENCES businesses(id),
                            settlement_id TEXT NOT NULL REFERENCES economy_accounts(settlement_id),
                            period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>0),
                            startup_hundredths INTEGER NOT NULL CHECK(typeof(startup_hundredths)='integer' AND startup_hundredths>=0),
                            capital_hundredths INTEGER NOT NULL CHECK(typeof(capital_hundredths)='integer' AND capital_hundredths>=0),
                            protected_hundredths INTEGER NOT NULL CHECK(typeof(protected_hundredths)='integer' AND protected_hundredths>=0),
                            before_cash INTEGER NOT NULL CHECK(typeof(before_cash)='integer' AND before_cash>=0),
                            after_cash INTEGER NOT NULL CHECK(typeof(after_cash)='integer' AND after_cash>=protected_hundredths),
                            food_demand INTEGER NOT NULL CHECK(typeof(food_demand)='integer' AND food_demand>=0),
                            policy TEXT NOT NULL CHECK(length(policy) BETWEEN 1 AND 4096),
                            CHECK(typeof(startup_hundredths+capital_hundredths)='integer' AND startup_hundredths+capital_hundredths>0),
                            CHECK(before_cash-after_cash=startup_hundredths+capital_hundredths),
                            FOREIGN KEY(settlement_id,period) REFERENCES needs_periods(settlement_id,period)
                        )
                        """);
                    execute(connection,"CREATE INDEX village_startup_grants ON startup_grants(settlement_id,period)");
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE mineral_surveys (
                            settlement_id TEXT PRIMARY KEY NOT NULL,
                            world_id TEXT NOT NULL,
                            captured_millis INTEGER NOT NULL CHECK(typeof(captured_millis)='integer' AND captured_millis>=0),
                            fingerprint TEXT NOT NULL CHECK(length(fingerprint)=64),
                            UNIQUE(settlement_id,world_id),
                            FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE mineral_survey_members (
                            settlement_id TEXT NOT NULL, world_id TEXT NOT NULL,
                            x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL,
                            PRIMARY KEY(settlement_id,world_id,x,y,z),
                            FOREIGN KEY(settlement_id,world_id) REFERENCES mineral_surveys(settlement_id,world_id),
                            FOREIGN KEY(settlement_id,world_id,x,y,z) REFERENCES mineral_opportunities(settlement_id,world_id,x,y,z)
                        )
                        """);
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE business_tools (
                            business_id TEXT PRIMARY KEY NOT NULL REFERENCES businesses(id),
                            productive_periods INTEGER NOT NULL CHECK(typeof(productive_periods)='integer' AND productive_periods>=0),
                            service_periods INTEGER NOT NULL CHECK(typeof(service_periods)='integer' AND service_periods>=0),
                            remainder_percent INTEGER NOT NULL CHECK(typeof(remainder_percent)='integer' AND remainder_percent BETWEEN 0 AND 99)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE business_tool_reviews (
                            operation_id TEXT PRIMARY KEY NOT NULL REFERENCES industry_operations(id),
                            business_id TEXT NOT NULL REFERENCES businesses(id),
                            period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=0),
                            tools_used INTEGER NOT NULL CHECK(typeof(tools_used)='integer' AND tools_used BETWEEN 0 AND 1),
                            policy TEXT NOT NULL,
                            explanation TEXT NOT NULL
                        )
                        """);
                },
                connection -> execute(connection,"""
                    CREATE TABLE business_hiring_reviews (
                        employment_id TEXT PRIMARY KEY NOT NULL REFERENCES business_employment(id),
                        policy TEXT NOT NULL,
                        explanation TEXT NOT NULL
                    )
                    """),
                connection -> {
                    execute(connection,"""
                        CREATE TABLE farm_tool_purchases (
                            operation_id TEXT PRIMARY KEY NOT NULL REFERENCES local_shipments(id),
                            farm_id TEXT NOT NULL REFERENCES businesses(id),
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),
                            period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=1),
                            price INTEGER NOT NULL CHECK(typeof(price)='integer' AND price>0),
                            protected_cash INTEGER NOT NULL CHECK(typeof(protected_cash)='integer' AND protected_cash>=0),
                            before_cash INTEGER NOT NULL CHECK(typeof(before_cash)='integer' AND before_cash>=0),
                            after_cash INTEGER NOT NULL CHECK(typeof(after_cash)='integer' AND after_cash>=protected_cash AND after_cash=before_cash-price),
                            policy TEXT NOT NULL,
                            terms TEXT NOT NULL
                        )
                        """);
                    execute(connection,"CREATE INDEX village_farm_tool_budget ON farm_tool_purchases(settlement_id,period)");
                },
                connection -> execute(connection,"""
                    CREATE TABLE farm_tool_work (
                        settlement_id TEXT NOT NULL REFERENCES settlements(id),
                        period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=1),
                        commodity TEXT NOT NULL CHECK(commodity='food'),
                        farm_id TEXT NOT NULL REFERENCES businesses(id),
                        output INTEGER NOT NULL CHECK(typeof(output)='integer' AND output>=0),
                        requested INTEGER NOT NULL CHECK(typeof(requested)='integer' AND requested>=output),
                        tools_used INTEGER NOT NULL CHECK(tools_used IN(0,1)),
                        policy TEXT NOT NULL,
                        explanation TEXT NOT NULL,
                        PRIMARY KEY(settlement_id,period),
                        FOREIGN KEY(settlement_id,period,commodity) REFERENCES economy_periods(settlement_id,period,commodity)
                    )
                    """),
                connection -> {
                    execute(connection,"""
                        CREATE TABLE upkeep_contributions (
                            operation_id TEXT PRIMARY KEY REFERENCES treasury_entries(operation_id),
                            invoice_id TEXT NOT NULL UNIQUE REFERENCES farm_tool_purchases(operation_id) DEFERRABLE INITIALLY DEFERRED,
                            business_id TEXT NOT NULL REFERENCES industry_stores(business_id),
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),
                            period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=1),
                            amount INTEGER NOT NULL CHECK(typeof(amount)='integer' AND amount>0),
                            protected_cash INTEGER NOT NULL CHECK(typeof(protected_cash)='integer' AND protected_cash>=0),
                            business_before INTEGER NOT NULL CHECK(typeof(business_before)='integer' AND business_before>=0),
                            business_after INTEGER NOT NULL CHECK(typeof(business_after)='integer' AND business_after>=protected_cash AND business_after=business_before-amount),
                            treasury_before INTEGER NOT NULL CHECK(typeof(treasury_before)='integer' AND treasury_before>=0),
                            treasury_after INTEGER NOT NULL CHECK(typeof(treasury_after)='integer' AND treasury_after=treasury_before+amount),
                            policy TEXT NOT NULL
                        )
                        """);
                    execute(connection,"CREATE INDEX village_upkeep_budget ON upkeep_contributions(settlement_id,period)");
                    execute(connection,"CREATE INDEX business_upkeep_budget ON upkeep_contributions(business_id,period)");
                    execute(connection,"CREATE INDEX local_supplier_sales ON local_shipments(seller_id)");
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE commerce_learning_reviews (
                            business_id TEXT NOT NULL REFERENCES businesses(id),
                            period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>0),
                            enabled INTEGER NOT NULL CHECK(enabled IN(0,1)),
                            skill TEXT CHECK(skill IS NULL OR length(trim(skill)) BETWEEN 1 AND 64),
                            gain INTEGER NOT NULL CHECK(typeof(gain)='integer' AND gain BETWEEN 1 AND 1000),
                            ceiling INTEGER NOT NULL CHECK(typeof(ceiling)='integer' AND ceiling BETWEEN 1 AND 1000000000),
                            worker_budget INTEGER NOT NULL CHECK(typeof(worker_budget)='integer' AND worker_budget BETWEEN 1 AND 1000),
                            explanation TEXT NOT NULL,
                            PRIMARY KEY(business_id,period),
                            FOREIGN KEY(business_id,period) REFERENCES industry_runtime_history(business_id,period) DEFERRABLE INITIALLY DEFERRED
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE commerce_learning_receipts (
                            business_id TEXT NOT NULL,
                            period INTEGER NOT NULL,
                            shipment_id TEXT NOT NULL UNIQUE REFERENCES local_shipments(id),
                            PRIMARY KEY(business_id,period,shipment_id),
                            FOREIGN KEY(business_id,period) REFERENCES commerce_learning_reviews(business_id,period)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE commerce_learning_entries (
                            business_id TEXT NOT NULL,
                            period INTEGER NOT NULL,
                            resident_id TEXT NOT NULL REFERENCES residents(id),
                            before_experience INTEGER NOT NULL CHECK(typeof(before_experience)='integer' AND before_experience>=0),
                            after_experience INTEGER NOT NULL CHECK(typeof(after_experience)='integer' AND after_experience>before_experience),
                            PRIMARY KEY(business_id,period,resident_id),
                            FOREIGN KEY(business_id,period) REFERENCES commerce_learning_reviews(business_id,period)
                        )
                        """);
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE career_study_reviews (
                            business_id TEXT NOT NULL REFERENCES businesses(id),
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),
                            period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>0),
                            commodity TEXT NOT NULL CHECK(commodity='food'),
                            after_resident TEXT NOT NULL,
                            policy TEXT NOT NULL,
                            explanation TEXT NOT NULL,
                            PRIMARY KEY(business_id,period),
                            FOREIGN KEY(settlement_id,period,commodity) REFERENCES economy_periods(settlement_id,period,commodity)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE career_study_enrollments (
                            course_id TEXT PRIMARY KEY REFERENCES resident_courses(id),
                            business_id TEXT NOT NULL REFERENCES businesses(id),
                            period INTEGER NOT NULL,
                            FOREIGN KEY(business_id,period) REFERENCES career_study_reviews(business_id,period) DEFERRABLE INITIALLY DEFERRED
                        )
                        """);
                    execute(connection,"CREATE INDEX career_vacancy_courses ON career_study_enrollments(business_id,course_id)");
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE workplace_surveys (
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),
                            world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                            chunk_x INTEGER NOT NULL,chunk_z INTEGER NOT NULL,
                            captured_millis INTEGER NOT NULL CHECK(typeof(captured_millis)='integer' AND captured_millis>=0),
                            complete INTEGER NOT NULL CHECK(complete IN(0,1)),fingerprint TEXT NOT NULL,
                            PRIMARY KEY(settlement_id,world_id,chunk_x,chunk_z),
                            FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE workplace_sightings (
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                            x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                            kind TEXT NOT NULL CHECK(kind IN('BARREL','SMITHING_TABLE')),
                            first_seen_millis INTEGER NOT NULL CHECK(first_seen_millis>=0),
                            last_seen_millis INTEGER NOT NULL CHECK(last_seen_millis>=first_seen_millis),
                            PRIMARY KEY(settlement_id,world_id,x,y,z,kind),
                            FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE workplace_markers (
                            settlement_id TEXT NOT NULL,world_id TEXT NOT NULL,chunk_x INTEGER NOT NULL,chunk_z INTEGER NOT NULL,
                            x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,kind TEXT NOT NULL,
                            PRIMARY KEY(settlement_id,world_id,x,y,z),
                            FOREIGN KEY(settlement_id,world_id,chunk_x,chunk_z) REFERENCES workplace_surveys(settlement_id,world_id,chunk_x,chunk_z),
                            FOREIGN KEY(settlement_id,world_id,x,y,z,kind) REFERENCES workplace_sightings(settlement_id,world_id,x,y,z,kind)
                        )
                        """);
                    execute(connection,"CREATE INDEX workplace_coordinate_members ON workplace_markers(world_id,x,y,z)");
                },
                connection -> {
                    execute(connection,"""
                        CREATE TABLE mine_founder_reviews (
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),
                            period INTEGER NOT NULL CHECK(period>=1),opportunity TEXT NOT NULL,
                            after_resident TEXT NOT NULL,policy TEXT NOT NULL,explanation TEXT NOT NULL,
                            PRIMARY KEY(settlement_id,period)
                        )
                        """);
                    execute(connection,"""
                        CREATE TABLE mine_founder_courses (
                            course_id TEXT PRIMARY KEY REFERENCES resident_courses(id),
                            settlement_id TEXT NOT NULL REFERENCES settlements(id),world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                            mineral TEXT NOT NULL CHECK(mineral IN('coal','iron')),x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                            enrolled_period INTEGER NOT NULL CHECK(enrolled_period>=1),
                            FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id),
                            FOREIGN KEY(settlement_id,enrolled_period) REFERENCES mine_founder_reviews(settlement_id,period) DEFERRABLE INITIALLY DEFERRED
                        )
                        """);
                    execute(connection,"CREATE INDEX active_mine_founders ON mine_founder_courses(settlement_id,course_id)");
                },
                SchemaMigrator::createWorkshopStarterSupplies,
                SchemaMigrator::createWorkplaceOperations,
                SchemaMigrator::createWorkshopFounderStudy,
                SchemaMigrator::createWarehouseFounderStudy,
                SchemaMigrator::extendFiniteResourceKinds,
                SchemaMigrator::createVillageSupplyOrders,
                SchemaMigrator::createFisheries,
                SchemaMigrator::createFisheryHabitatChanges,
                SchemaMigrator::createWaterSurveys,
                SchemaMigrator::createFisheryWorkplaces,
                SchemaMigrator::createFisheryFounderStudy,
                connection -> execute(connection,"""
                        CREATE TABLE farm_grain_evidence (
                            settlement_id TEXT PRIMARY KEY REFERENCES farm_evidence(settlement_id),
                            observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                            grain_crops INTEGER NOT NULL CHECK(typeof(grain_crops)='integer' AND grain_crops>=0 AND grain_crops<=100000)
                        )
                        """),
                SchemaMigrator::createGrainHarvests,
                SchemaMigrator::createFarmMarket,
                SchemaMigrator::createAnimalOwnership,
                SchemaMigrator::createPenProtection,
                LivestockRepository::createSchema,
                WorkplaceMarkerMigration::apply,
                BakeryPrivateInputs::createSchema,
                SchemaMigrator::createBakeryFounderStudy,
                StorageUpgrades::createSchema,
                SchemaMigrator::createCraftsmanFounderStudy,
                LivestockLosses::createSchema,
                LivestockCapture::createSchema,
                LivestockProductSales::createSchema,
                LivestockBirths::createSchema,
                connection -> {LivestockBirthProjections.createSchema(connection);LivestockBirthLosses.createSchema(connection);},
                LivestockPresence::createSchema,
                PenProtectionLifecycle::createSchema,
                LivestockLearning::createSchema,
                LivestockSlaughter::createSchema,
                QuarrySurveys::createSchema,
                ForestryResources::createSchema,
                ForestryProvenance::createSchema,
                ForestryCourses::createSchema,
                MineralRecovery::createSchema,
                ForestRecovery::createSchema,
                BusinessCompetition::createSchema,
                ResourceInvestment::createSchema,
                GeneratedTerrainRepository::createSchema,
                ConstructionDraftRepository::createSchema,
                ConstructionBlockJournal::createSchema,
                ConstructionMaterials::createSchema,
                SheepMigration::apply,
                ConstructionSiteReservations::createSchema,
                ConstructionMaterialReturns::createSchema,
                ConstructionSupplyOrders::createSchema,
                ConstructionPublicWork::createSchema,
                BuildingRegistry::createSchema,
                DwellingOccupancy::createSchema


        ));
    }

    private static void createPenProtection(Connection c)throws SQLException {
        execute(c,"""
                CREATE TABLE protected_pens (
                    id TEXT PRIMARY KEY, world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    owner_id TEXT NOT NULL, standing_y INTEGER NOT NULL,
                    sign_x INTEGER NOT NULL, sign_y INTEGER NOT NULL, sign_z INTEGER NOT NULL,
                    seed_x INTEGER NOT NULL, seed_z INTEGER NOT NULL,
                    confirmed_millis INTEGER NOT NULL CHECK(typeof(confirmed_millis)='integer' AND confirmed_millis>=0),
                    UNIQUE(id,world_id,standing_y)
                )
                """);
        execute(c,"""
                CREATE TABLE protected_pen_columns (
                    pen_id TEXT NOT NULL, world_id TEXT NOT NULL, standing_y INTEGER NOT NULL,
                    x INTEGER NOT NULL,z INTEGER NOT NULL,kind TEXT NOT NULL CHECK(kind IN('INTERIOR','BOUNDARY')),
                    PRIMARY KEY(pen_id,x,z),UNIQUE(world_id,standing_y,x,z),
                    FOREIGN KEY(pen_id,world_id,standing_y) REFERENCES protected_pens(id,world_id,standing_y)
                )
                """);
        execute(c,"""
                CREATE TABLE protected_pen_animals (
                    pen_id TEXT NOT NULL REFERENCES protected_pens(id),
                    animal_id TEXT NOT NULL REFERENCES animal_ownership(animal_id),
                    PRIMARY KEY(pen_id,animal_id)
                )
                """);
    }

    private static void createAnimalOwnership(Connection c)throws SQLException {
        execute(c,"""
                CREATE TABLE animal_ownership (
                    animal_id TEXT PRIMARY KEY,
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    kind TEXT NOT NULL CHECK(kind IN('PLAYER','VILLAGE')),
                    player_id TEXT,
                    settlement_id TEXT REFERENCES settlements(id),
                    recorded_millis INTEGER NOT NULL CHECK(typeof(recorded_millis)='integer' AND recorded_millis>=0),
                    source TEXT NOT NULL CHECK(length(trim(source)) BETWEEN 1 AND 256),
                    CHECK((kind='PLAYER' AND player_id IS NOT NULL AND settlement_id IS NULL)
                       OR (kind='VILLAGE' AND player_id IS NULL AND settlement_id IS NOT NULL)),
                    FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
                )
                """);
        execute(c,"CREATE INDEX village_animal_owners ON animal_ownership(settlement_id,animal_id)");
    }

    private static void createFarmMarket(Connection c)throws SQLException {
        execute(c,"""
                CREATE TABLE farm_market_stores (
                    business_id TEXT PRIMARY KEY REFERENCES businesses(id),
                    capacity INTEGER NOT NULL CHECK(typeof(capacity)='integer' AND capacity BETWEEN 2 AND 1000001),
                    adopted_millis INTEGER NOT NULL CHECK(typeof(adopted_millis)='integer' AND adopted_millis>=0),
                    prior_terms TEXT NOT NULL CHECK(length(prior_terms)>0),
                    policy TEXT NOT NULL CHECK(length(policy) BETWEEN 1 AND 4096)
                )
                """);
        execute(c,"""
                CREATE TABLE farm_market_harvests (
                    settlement_id TEXT NOT NULL REFERENCES economy_accounts(settlement_id),
                    period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=1),
                    business_id TEXT NOT NULL REFERENCES farm_market_stores(business_id),
                    potential_work INTEGER NOT NULL CHECK(typeof(potential_work)='integer' AND potential_work>=0),
                    actual_work INTEGER NOT NULL CHECK(typeof(actual_work)='integer' AND actual_work BETWEEN 0 AND potential_work),
                    public_food INTEGER NOT NULL CHECK(typeof(public_food)='integer' AND public_food>=0),
                    business_food INTEGER NOT NULL CHECK(typeof(business_food)='integer' AND business_food>=0),
                    grain_work INTEGER NOT NULL CHECK(typeof(grain_work)='integer' AND grain_work>=0),
                    grain_units INTEGER NOT NULL CHECK(typeof(grain_units)='integer' AND grain_units>=0),
                    policy TEXT NOT NULL CHECK(length(policy) BETWEEN 1 AND 4096),
                    explanation TEXT NOT NULL CHECK(length(explanation) BETWEEN 1 AND 4096),
                    grain_policy TEXT NOT NULL CHECK(length(grain_policy) BETWEEN 1 AND 4096),
                    wheat INTEGER CHECK(wheat IS NULL OR (typeof(wheat)='integer' AND wheat>=0 AND wheat<=crops)),
                    crops INTEGER NOT NULL CHECK(typeof(crops)='integer' AND crops BETWEEN 0 AND 100000),
                    observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                    units_per_grain_work INTEGER NOT NULL CHECK(typeof(units_per_grain_work)='integer' AND units_per_grain_work BETWEEN 1 AND 1000),
                    village_share_percent INTEGER NOT NULL CHECK(typeof(village_share_percent)='integer' AND village_share_percent BETWEEN 0 AND 100),
                    PRIMARY KEY(settlement_id,period),
                    CHECK(typeof(public_food+business_food+grain_work)='integer' AND actual_work=public_food+business_food+grain_work),
                    CHECK(typeof(grain_work*units_per_grain_work)='integer' AND grain_units=grain_work*units_per_grain_work),
                    CHECK(grain_work=0 OR (wheat IS NOT NULL AND wheat>0)),
                    CHECK(public_food<=((public_food+business_food)*village_share_percent)/100)
                )
                """);
    }
    private static void createGrainHarvests(Connection connection)throws SQLException {
        execute(connection,"""
                CREATE TABLE grain_stocks (
                    settlement_id TEXT PRIMARY KEY REFERENCES economy_accounts(settlement_id),
                    units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>=0)
                )
                """);
        execute(connection,"""
                CREATE TABLE grain_harvests (
                    settlement_id TEXT NOT NULL REFERENCES grain_stocks(settlement_id) DEFERRABLE INITIALLY DEFERRED,
                    period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=1),
                    harvest INTEGER NOT NULL CHECK(typeof(harvest)='integer' AND harvest>=0),
                    food_output INTEGER NOT NULL CHECK(typeof(food_output)='integer' AND food_output>=0),
                    diverted INTEGER NOT NULL CHECK(typeof(diverted)='integer' AND diverted>=0),
                    grain_units INTEGER NOT NULL CHECK(typeof(grain_units)='integer' AND grain_units>=0),
                    opening_grain INTEGER NOT NULL CHECK(typeof(opening_grain)='integer' AND opening_grain>=0),
                    closing_grain INTEGER NOT NULL CHECK(typeof(closing_grain)='integer' AND closing_grain>=opening_grain),
                    protected_food INTEGER NOT NULL CHECK(typeof(protected_food)='integer' AND protected_food>=0),
                    available_food INTEGER NOT NULL CHECK(typeof(available_food)='integer' AND available_food>=0),
                    wheat INTEGER CHECK(wheat IS NULL OR (typeof(wheat)='integer' AND wheat>=0 AND wheat<=crops)),
                    crops INTEGER NOT NULL CHECK(typeof(crops)='integer' AND crops>=0 AND crops<=100000),
                    units_per_harvest INTEGER NOT NULL CHECK(typeof(units_per_harvest)='integer' AND units_per_harvest BETWEEN 1 AND 1000),
                    capacity INTEGER NOT NULL CHECK(typeof(capacity)='integer' AND capacity BETWEEN 1 AND 1000000),
                    policy TEXT NOT NULL CHECK(length(policy) BETWEEN 1 AND 4096),
                    explanation TEXT NOT NULL CHECK(length(explanation) BETWEEN 1 AND 4096),
                    PRIMARY KEY(settlement_id,period),
                    CHECK(typeof(food_output+diverted)='integer' AND harvest=food_output+diverted),
                    CHECK(typeof(diverted*units_per_harvest)='integer' AND grain_units=diverted*units_per_harvest),
                    CHECK(typeof(opening_grain+grain_units)='integer' AND closing_grain=opening_grain+grain_units),
                    CHECK(closing_grain<=MAX(opening_grain,capacity)),
                    CHECK(diverted=0 OR (wheat IS NOT NULL AND wheat>0 AND available_food-diverted>=protected_food))
                )
                """);
    }
    // Tests can inject a failing step to verify real SQLite rollback and recovery.
    /** Historical-schema test harness using the real ordered migrations; never performs a downgrade. */
    static SchemaMigrator throughVersion(int version) {
        if(version<1||version>CURRENT_VERSION)throw new IllegalArgumentException("Invalid historical schema version");
        return new SchemaMigrator(new SchemaMigrator().migrations.subList(0,version));
    }
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

        // SQLite's documented parent-table rebuild requires FK enforcement off BEFORE
        // the transaction. Validate all foreign keys before commit and restore enforcement.
        boolean restoreForeignKeys=false;
        if((version>=32 && version<64 && migrations.size()>=64)||(version<103 && migrations.size()>=103)) {
            try(var s=connection.createStatement();var r=s.executeQuery("PRAGMA foreign_keys")){restoreForeignKeys=r.next()&&r.getInt(1)==1;}
            if(restoreForeignKeys)execute(connection,"PRAGMA foreign_keys=OFF");
        }
        connection.setAutoCommit(false);
        try {
            for (int index = version; index < migrations.size(); index++) {
                migrations.get(index).apply(connection);
                execute(connection, "PRAGMA user_version = " + (index + 1));
            }
            requireColumns(connection, Set.of("id", "name", "world", "center_x", "center_y",
                    "center_z", "discovered_at", "population", "beds"));
            if(migrations.size()>=71)requireColumns(connection,"farm_grain_evidence",Set.of("settlement_id","observed_millis","grain_crops"));
            if(migrations.size()>=72){requireColumns(connection,"grain_stocks",Set.of("settlement_id","units"));requireColumns(connection,"grain_harvests",Set.of("settlement_id","period","harvest","food_output","grain_units","policy"));}
            if(migrations.size()>=73){requireColumns(connection,"farm_market_stores",Set.of("business_id","capacity","prior_terms","policy"));requireColumns(connection,"farm_market_harvests",Set.of("settlement_id","period","business_id","actual_work","public_food","business_food","grain_units","grain_policy"));}
            if(migrations.size()>=74)requireColumns(connection,"animal_ownership",Set.of("animal_id","world_id","kind","player_id","settlement_id","recorded_millis","source"));
            if(migrations.size()>=75){requireColumns(connection,"protected_pens",Set.of("id","world_id","owner_id","standing_y","confirmed_millis"));requireColumns(connection,"protected_pen_columns",Set.of("pen_id","world_id","standing_y","x","z","kind"));requireColumns(connection,"protected_pen_animals",Set.of("pen_id","animal_id"));}
            if(migrations.size()>=76){requireColumns(connection,"livestock_herds",Set.of("business_id","species","capacity","feed","opening_terms"));requireColumns(connection,"livestock_periods",Set.of("business_id","period","operation_id","terms","adults","births","feed_used","renewable_units"));requireColumns(connection,"livestock_young",Set.of("business_id","period","mature_period","heads"));requireColumns(connection,"livestock_initial_animals",Set.of("animal_id","business_id","mature_period"));}
            if(migrations.size()>=78)requireColumns(connection,"bakery_private_starters",Set.of("business_id","shipment_id","seller_id","terms"));
            if(migrations.size()>=88){requireColumns(connection,"active_pen_claims",Set.of("pen_id"));requireColumns(connection,"pen_releases",Set.of("operation_id","pen_id","actor_id","released_millis","reason"));}
            if(migrations.size()>=89){requireColumns(connection,"livestock_learning_reviews",Set.of("business_id","period","work_used","skill","policy","explanation"));requireColumns(connection,"livestock_learning_entries",Set.of("business_id","period","resident_id","before_experience","after_experience"));}
            if(migrations.size()>=90)requireColumns(connection,"livestock_slaughter",Set.of("id","animal_id","business_id","period","world_id","species","meat_units","prepared_ms","state","loss_operation","explanation","terms"));
            if (migrations.size() >= 91) {
                requireColumns(connection,"quarry_opportunities",Set.of("settlement_id","world_id","x","y","z","mineral","first_seen_millis","last_seen_millis"));
                requireColumns(connection,"quarry_surveys",Set.of("settlement_id","world_id","scope_x","scope_z","captured_millis","fingerprint"));
                requireColumns(connection,"quarry_survey_members",Set.of("settlement_id","world_id","scope_x","scope_z","x","y","z"));
                requireColumns(connection,"quarry_founder_reviews",Set.of("settlement_id","period","opportunity","after_resident","policy","explanation"));
                requireColumns(connection,"quarry_founder_courses",Set.of("course_id","settlement_id","world_id","mineral","x","y","z","enrolled_period"));
            }
            if (migrations.size() >= 92) {
                requireColumns(connection,"forestry_resources",Set.of("site_id","world_id","capacity","habitat_capacity","available","remainder","observed_ms","opening_terms"));
                requireColumns(connection,"forestry_supports",Set.of("world_id","x","y","z","site_id"));
                requireColumns(connection,"forestry_work",Set.of("operation_id","site_id","business_id","period","opening_units","recovered","harvested","closing_units","before_remainder","after_remainder","policy"));
                requireColumns(connection,"forestry_habitat_changes",Set.of("operation_id","site_id","evidence","captured_ms","before_capacity","after_capacity","opening_units","lost","closing_units","before_remainder","after_remainder"));
            }
            if(migrations.size()>=93) {
                requireColumns(connection,"forestry_tracking",Set.of("id","clean_stop","trusted","observed_millis"));
                requireColumns(connection,"forestry_exclusions",Set.of("world_id","x","y","z","observed_ms","reason"));
                requireColumns(connection,"forestry_source_losses",Set.of("world_id","x","y","z","observed_ms","reason"));
            }
            if(migrations.size()>=95) {
                requireColumns(connection,"resource_recovery_epochs",Set.of("kind","generation","healthy"));
                requireColumns(connection,"mineral_recoveries",Set.of("operation_id","deposit_id","generation","actor","observed_ms","before_batches","lost_batches","after_batches","evidence"));
            }
            if(migrations.size()>=96)requireColumns(connection,"forest_recoveries",Set.of("operation_id","site_id","generation","actor","observed_ms","before_capacity","after_capacity","opening_units","closing_units","evidence"));
            if(migrations.size()>=97){
                requireColumns(connection,"business_allocation_state",Set.of("settlement_id","last_business","sequence","observed_ms"));
                requireColumns(connection,"business_allocation_reviews",Set.of("settlement_id","sequence","business_id","observed_step","observed_ms","urgent","demand_value","policy","explanation"));
            }
            if(migrations.size()>=98){
                requireColumns(connection,"resource_investment_state",Set.of("settlement_id","last_proposal","sequence","observed_ms"));
                requireColumns(connection,"resource_investment_reviews",Set.of("business_id","settlement_id","sequence","proposal","industry","observed_step","observed_ms","demand_value","policy","explanation"));
            }
            if(migrations.size()>=102){requireColumns(connection,"construction_material_reservations",Set.of("operation_id","draft_id","business_id","reserved_ms"));requireColumns(connection,"construction_material_items",Set.of("operation_id","commodity","units","before_units","after_units"));}
            if(migrations.size()>=109){
                requireColumns(connection,"dwelling_reviews",Set.of("operation_id","building_id","consent_id","fingerprint","observed_ms","recorded_ms","require_cover"));
                requireColumns(connection,"dwelling_review_units",Set.of("review_id","unit_id","beds","issues"));
                requireColumns(connection,"dwelling_current_reviews",Set.of("building_id","review_id"));
                requireColumns(connection,"dwelling_assignments",Set.of("operation_id","household_id","unit_id","review_id","previous_assignment","at_ms"));
                requireColumns(connection,"dwelling_active_occupancy",Set.of("household_id","unit_id","assignment_id"));
                requireColumns(connection,"dwelling_departures",Set.of("operation_id","assignment_id","reason","consent_id","at_ms"));
            }
            if(migrations.size()>=108){
                requireColumns(connection,"building_records",Set.of("id","settlement_id","world_id","donor_id","layout_hash","layout_snapshot","min_x","max_x","created_ms"));
                requireColumns(connection,"building_units",Set.of("id","building_id","declared_beds"));
                requireColumns(connection,"building_consent_events",Set.of("operation_id","building_id","actor_id","action","previous_operation","administrator","at_ms"));
                requireColumns(connection,"building_exclusions",Set.of("id","world_id","actor_id","min_x","max_x","created_ms"));
                requireColumns(connection,"building_exclusion_releases",Set.of("operation_id","exclusion_id","actor_id","administrator","released_ms"));
            }
            if(migrations.size()>=107){requireColumns(connection,"construction_stage_work",Set.of("draft_id","stage","work","first_period","last_period"));requireColumns(connection,"construction_public_work",Set.of("draft_id","stage","resident_id","period","effort"));}
            if(migrations.size()>=106)requireColumns(connection,"construction_supply_orders",Set.of("operation_id","draft_id","seller_id","source","business_id","commodity","units","price","due_period","delivered_period","haul_window","cadence"));
            if(migrations.size()>=105)requireColumns(connection,"construction_material_returns",Set.of("return_operation","reservation_operation","commodity","units","before_units","after_units","returned_ms"));
            if(migrations.size()>=104)requireColumns(connection,"construction_site_reservations",Set.of("draft_id","world_id","min_x","max_x","reserved_ms","released_ms"));
            if(migrations.size()>=101)requireColumns(connection,"construction_block_journal",Set.of("operation_id","draft_id","ordinal","world_x","world_y","world_z","before_state","after_state","state"));
            if(migrations.size()>=100)requireColumns(connection,"construction_drafts",Set.of("id","settlement_id","world_id","origin_x","origin_y","origin_z","rotation","mirrored","template_hash","template_snapshot","created_ms"));
            if(migrations.size()>=99)requireColumns(connection,"generated_resource_chunks",Set.of("kind","world_id","chunk_x","chunk_z","generation","observed_ms"));
            if(migrations.size()>=94) {
                requireColumns(connection,"forestry_founder_courses",Set.of("course_id","settlement_id","world_id","x","y","z","enrolled_period"));
                requireColumns(connection,"forestry_founder_reviews",Set.of("settlement_id","period","opportunity","after_resident","policy","explanation"));
            }
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
            if (migrations.size() >= 25) requireColumns(connection, "priority_learning", Set.of("settlement_id","period","replay_version","window_periods","maximum_bonus","imported","avoided_unmet"));
            if (migrations.size() >= 25) requireColumns(connection, "village_priorities", Set.of("learning_replay"));
            if (migrations.size() >= 26) {
                requireColumns(connection,"food_responses",Set.of("settlement_id","period","seed","replay_version","relative_percent","selected"));
                requireColumns(connection,"food_response_candidates",Set.of("settlement_id","period","action","feasible","weight","reason"));
            }
            if (migrations.size() >= 27) {
                requireColumns(connection,"business_sites",Set.of("id","settlement_id","world_id","x","y","z","observed_millis","evidence"));
                requireColumns(connection,"businesses",Set.of("id","settlement_id","name","founder_id","owner_id","industry","site_id","worker_capacity","founded_step","startup_hundredths","reason","founder_min_age","worker_min_age"));
                requireColumns(connection,"business_skill_requirements",Set.of("business_id","role","skill","experience"));
                requireColumns(connection,"business_employment",Set.of("id","business_id","resident_id","started_step","ended_step","end_reason"));
            }
            if(migrations.size()>=28) {
                requireColumns(connection,"mineral_deposits",Set.of("id","site_id","initial_batches","remaining_batches","observed_ore","terms"));
                requireColumns(connection,"mineral_yields",Set.of("deposit_id","commodity","units"));
                requireColumns(connection,"industry_stores",Set.of("business_id","capacity","hundredths","terms"));
                requireColumns(connection,"industry_stock",Set.of("business_id","commodity","space_per_unit","units"));
                requireColumns(connection,"industry_operations",Set.of("id","terms","quantity","explanation"));
                requireColumns(connection,"industry_production",Set.of("business_id","period","operation_id"));
                requireColumns(connection,"local_shipments",Set.of("id","seller_id","buyer_id","commodity","units","price","dispatched_period","due_period","delivered_period"));
            }
            if(migrations.size()>=29) {
                requireColumns(connection,"online_calendar",Set.of("id","replay_version","adopted_millis","legacy_target","adopted_completed"));
                requireColumns(connection,"population_active_worlds",Set.of("from_step","through_step","worlds"));
            }
            if(migrations.size()>=30)requireColumns(connection,"farm_evidence",Set.of("site_x","site_y","site_z"));
            if(migrations.size()>=31) {
                requireColumns(connection,"recipe_runs",Set.of("operation_id","business_id","period","recipe_id","replay_version","requested","workers","batches_per_worker","site_capacity"));
                requireColumns(connection,"recipe_flows",Set.of("operation_id","commodity","direction","per_batch","units"));
            }
            if(migrations.size()>=32) {
                requireColumns(connection,"mineral_discovery",Set.of("deposit_id","world_id","mineral","observed_millis"));
                requireColumns(connection,"mineral_evidence",Set.of("world_id","deposit_id","x","y","z","observed_millis"));
            }
            if(migrations.size()>=33)requireColumns(connection,"player_ore_placements",Set.of("world_id","x","y","z","observed_millis"));
            if(migrations.size()>=34)requireColumns(connection,"ore_tracking",Set.of("id","clean_stop","trusted","observed_millis"));
            if(migrations.size()>=35) {
                requireColumns(connection,"industry_runtime",Set.of("business_id","cadence","last_ms","remainder","target","completed"));
                requireColumns(connection,"industry_runtime_history",Set.of("business_id","period","explanation"));
                requireColumns(connection,"industry_runtime_tuning",Set.of("sequence","business_id","after_period","old_cadence","new_cadence","discarded_remainder","observed_ms"));
            }
            if(migrations.size()>=36)requireColumns(connection,"industry_auto_shipments",Set.of("shipment_id","clock_business_id","haul_window"));
            if(migrations.size()>=37)requireColumns(connection,"industry_auto_shipments",Set.of("haul_cadence"));
            if(migrations.size()>=38)requireColumns(connection,"business_ownership",Set.of("operation_id","business_id","previous_owner","next_owner","observed_step","kind"));
            if(migrations.size()>=39) {
                requireColumns(connection,"family_estates",Set.of("decedent_id","hundredths"));
                requireColumns(connection,"business_sales",Set.of("operation_id","business_id","previous_owner","next_owner","observed_step","cash_source","cash_amount","estate_source","estate_amount"));
                requireColumns(connection,"business_sale_entries",Set.of("operation_id","source_kind","source_id","direction","before_cash","after_cash"));
            }
            if(migrations.size()>=40)requireColumns(connection,"business_sale_listings",Set.of("business_id","seller_id","price","deadline_ms","created_step","last_ms","elapsed_ms","status","sale_operation"));
            if(migrations.size()>=41)requireColumns(connection,"business_closures",Set.of("business_id","seller_id","observed_step","reason"));
            if(migrations.size()>=42) {
                requireColumns(connection,"industry_learning_reviews",Set.of("operation_id","business_id","period","enabled","skill","gain","ceiling","worker_budget","explanation"));
                requireColumns(connection,"industry_learning_entries",Set.of("operation_id","resident_id","before_experience","after_experience"));
            }
            if(migrations.size()>=43) {
                requireColumns(connection,"resident_courses",Set.of("id","resident_id","settlement_id","skill","target","gain","eligible_after","status"));
                requireColumns(connection,"study_materials",Set.of("course_id","commodity","units"));
                requireColumns(connection,"study_reviews",Set.of("course_id","settlement_id","period","before_experience","after_experience","reason"));
                requireColumns(connection,"study_spending",Set.of("course_id","period","commodity","units"));
            }
            if(migrations.size()>=49)requireColumns(connection,"startup_grants",Set.of("business_id","settlement_id","period","startup_hundredths","capital_hundredths","protected_hundredths","before_cash","after_cash","food_demand","policy"));
            if(migrations.size()>=50) {
                requireColumns(connection,"mineral_surveys",Set.of("settlement_id","world_id","captured_millis","fingerprint"));
                requireColumns(connection,"mineral_survey_members",Set.of("settlement_id","world_id","x","y","z"));
            }
            if(migrations.size()>=63){
                requireColumns(connection,"warehouse_founder_reviews",Set.of("settlement_id","period","after_resident","policy","explanation"));
                requireColumns(connection,"warehouse_founder_courses",Set.of("course_id","settlement_id","world_id","x","y","z","enrolled_period"));
            }
            if(migrations.size()>=81){
                requireColumns(connection,"craftsman_founder_reviews",Set.of("settlement_id","period","after_resident","policy","explanation"));
                requireColumns(connection,"craftsman_founder_courses",Set.of("course_id","settlement_id","world_id","x","y","z","enrolled_period"));
            }
            if(migrations.size()>=80)requireColumns(connection,"business_storage_upgrades",Set.of("operation_id","buyer_id","seller_id","period","haul_window","cadence","ordinal","base_capacity","before_capacity","after_capacity","units","price","due_period","installed_period","policy"));
            if(migrations.size()>=79){
                requireColumns(connection,"bakery_founder_reviews",Set.of("settlement_id","period","after_resident","policy","explanation"));
                requireColumns(connection,"bakery_founder_courses",Set.of("course_id","settlement_id","world_id","x","y","z","enrolled_period"));
            }
            if(migrations.size()>=62){
                requireColumns(connection,"workshop_founder_reviews",Set.of("settlement_id","period","after_resident","policy","explanation"));
                requireColumns(connection,"workshop_founder_courses",Set.of("course_id","settlement_id","world_id","x","y","z","enrolled_period"));
            }
            if(migrations.size()>=61){
                requireColumns(connection,"business_workplace_anchors",Set.of("business_id","world_id","x","y","z","kind","captured_millis","present"));
                requireColumns(connection,"workplace_operation_history",Set.of("business_id","captured_millis","present"));
                requireColumns(connection,"workplace_operation_settings",Set.of("singleton","pause_on_missing"));
            }
            if(migrations.size()>=60){
                requireColumns(connection,"workshop_starter_orders",Set.of("operation_id","business_id","settlement_id","food_period","due_period","price","before_cash","after_cash","terms","policy","delivered_period"));
                requireColumns(connection,"workshop_starter_goods",Set.of("operation_id","commodity","units","unit_price","protected_units","before_units","after_units"));
            }
            if(migrations.size()>=59){
                requireColumns(connection,"mine_founder_reviews",Set.of("settlement_id","period","opportunity","after_resident","policy","explanation"));
                requireColumns(connection,"mine_founder_courses",Set.of("course_id","settlement_id","world_id","mineral","x","y","z","enrolled_period"));
            }
            if(migrations.size()>=58){
                requireColumns(connection,"workplace_surveys",Set.of("settlement_id","world_id","chunk_x","chunk_z","captured_millis","complete","fingerprint"));
                requireColumns(connection,"workplace_sightings",Set.of("settlement_id","world_id","x","y","z","kind","first_seen_millis","last_seen_millis"));
                requireColumns(connection,"workplace_markers",Set.of("settlement_id","world_id","chunk_x","chunk_z","x","y","z","kind"));
            }
            if(migrations.size()>=57){
                requireColumns(connection,"career_study_reviews",Set.of("business_id","settlement_id","period","commodity","after_resident","policy","explanation"));
                requireColumns(connection,"career_study_enrollments",Set.of("course_id","business_id","period"));
            }
            if(migrations.size()>=56){
                requireColumns(connection,"commerce_learning_reviews",Set.of("business_id","period","enabled","skill","gain","ceiling","worker_budget","explanation"));
                requireColumns(connection,"commerce_learning_receipts",Set.of("business_id","period","shipment_id"));
                requireColumns(connection,"commerce_learning_entries",Set.of("business_id","period","resident_id","before_experience","after_experience"));
            }
            if(migrations.size()>=55)requireColumns(connection,"upkeep_contributions",Set.of("operation_id","invoice_id","business_id","settlement_id","period","amount","protected_cash","business_before","business_after","treasury_before","treasury_after","policy"));
            if(migrations.size()>=54)requireColumns(connection,"farm_tool_work",Set.of("settlement_id","period","commodity","farm_id","output","requested","tools_used","policy","explanation"));
            if(migrations.size()>=53)requireColumns(connection,"farm_tool_purchases",Set.of("operation_id","farm_id","settlement_id","period","price","protected_cash","before_cash","after_cash","policy","terms"));
            if(migrations.size()>=52)requireColumns(connection,"business_hiring_reviews",Set.of("employment_id","policy","explanation"));
            if(migrations.size()>=51) {
                requireColumns(connection,"business_tools",Set.of("business_id","productive_periods","service_periods","remainder_percent"));
                requireColumns(connection,"business_tool_reviews",Set.of("operation_id","business_id","period","tools_used","policy","explanation"));
            }
            if(migrations.size()>=48)requireColumns(connection,"mineral_opportunities",Set.of("settlement_id","world_id","x","y","z","mineral","first_seen_millis","last_seen_millis"));
            if(migrations.size()>=46) {
                requireColumns(connection,"mineral_source_adoptions",Set.of("deposit_id","remaining_batches","source_count","explanation"));
                requireColumns(connection,"mineral_sources",Set.of("world_id","x","y","z","deposit_id","allocated_batches","remaining_batches"));
                requireColumns(connection,"mineral_source_extraction",Set.of("operation_id","world_id","x","y","z","before_batches","after_batches"));
                requireColumns(connection,"ore_source_removals",Set.of("world_id","x","y","z","observed_millis","reason"));
                requireColumns(connection,"mineral_source_retirements",Set.of("world_id","x","y","z","removed_batches"));
            }
            if(migrations.size()>=45) {
                requireColumns(connection,"farm_learning_reviews",Set.of("settlement_id","period","commodity","business_id","field_slots","production","enabled","skill","gain","ceiling","worker_budget","explanation"));
                requireColumns(connection,"farm_learning_entries",Set.of("settlement_id","period","resident_id","before_experience","after_experience"));
            }
            if(migrations.size()>=44)requireColumns(connection,"business_lifecycle_reviews",Set.of("business_id","seller_id","buyer_cursor","step","observed_ms","seed","reason"));
            if (migrations.size() >= 17) requireColumns(connection, "village_priorities", Set.of("settlement_id", "period", "seed", "replay_version",
                    "food_condition", "food_unmet", "crowded_homes", "selected", "review_after", "food_weight", "crisis_multiplier", "housing_weight", "monitor_weight", "relative_percent"));
            if (migrations.size() >= 18) requireColumns(connection,"food_deliveries",Set.of("sequence","operation_id","settlement_id","units","source","eligible_after","applied_period"));
            if (migrations.size() >= 19) {
                requireColumns(connection,"needs_periods",Set.of("unassigned_demand","unassigned_supplied"));
                requireColumns(connection,"household_needs",Set.of("supplied","unmet_periods"));
            }
            if (migrations.size() >= 20) requireColumns(connection,"food_appeals",Set.of("settlement_id","period","priority_period","status","unmet","previous_unmet","imports","production","change"));
            if (migrations.size() >= 21) requireColumns(connection,"food_purchases",Set.of("operation_id","player_id","settlement_id","units","item","item_count","emeralds","rate","status","accepted_period"));
            if (migrations.size() >= 22) requireColumns(connection,"household_wellbeing",Set.of("sequence","settlement_id","period","household_id","before_score","after_score","condition","food","housing","starting_score","recovery","food_penalty","crisis_penalty","crowding_penalty","crisis_after","food_streak"));
            if (migrations.size() >= 22) requireColumns(connection,"wellbeing_members",Set.of("sequence","resident_id","food_streak"));
            if (migrations.size() >= 23) requireColumns(connection,"food_bargains",Set.of("sequence","operation_id","player_id","settlement_id","units","item","item_count","requested","offered","rate","status","failed_attempts","observed_millis","expires_millis","cooldown_until","maximum_attempts","cooldown_seconds","offer_seconds"));
            if (migrations.size() >= 24) requireColumns(connection,"food_barters",Set.of("operation_id","player_id","settlement_id","food_units","food_item","food_count","commodity","payment_item","payment_count","units_per_item","status","accepted_period"));
            if (migrations.size() >= 24) requireColumns(connection,"barter_stock_moves",Set.of("sequence","operation_id","phase","after_period","before_units","after_units"));
            if(migrations.size()>=64)try(var s=connection.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")) {
                if(r.next())throw new SQLException("Schema upgrade leaves invalid foreign keys in "+r.getString(1));
            }
            connection.commit();
        } catch (SQLException | RuntimeException failure) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            if(restoreForeignKeys)try{execute(connection,"PRAGMA foreign_keys=ON");}catch(SQLException restore){failure.addSuppressed(restore);}
            throw failure;
        }
        connection.setAutoCommit(true);
        if(restoreForeignKeys)execute(connection,"PRAGMA foreign_keys=ON");
    }

    static void createWaterSurveys(Connection connection) throws SQLException {
        execute(connection,"""
            CREATE TABLE water_surveys (
                settlement_id TEXT NOT NULL,world_id TEXT NOT NULL,
                chunk_x INTEGER NOT NULL CHECK(typeof(chunk_x)='integer' AND chunk_x BETWEEN -134217728 AND 134217727),
                chunk_z INTEGER NOT NULL CHECK(typeof(chunk_z)='integer' AND chunk_z BETWEEN -134217728 AND 134217727),
                captured_millis INTEGER NOT NULL CHECK(typeof(captured_millis)='integer' AND captured_millis>=0),
                complete INTEGER NOT NULL CHECK(complete IN(0,1)),
                minimum_y INTEGER NOT NULL CHECK(typeof(minimum_y)='integer'),
                maximum_y INTEGER NOT NULL CHECK(typeof(maximum_y)='integer'),
                minimum_depth INTEGER NOT NULL CHECK(typeof(minimum_depth)='integer' AND minimum_depth BETWEEN 1 AND 64),
                fingerprint TEXT NOT NULL,
                CHECK(maximum_y-minimum_y BETWEEN 1 AND 4096 AND minimum_depth<=maximum_y-minimum_y),
                PRIMARY KEY(settlement_id,world_id,chunk_x,chunk_z),
                FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
            )
            """);
        execute(connection,"""
            CREATE TABLE water_columns (
                settlement_id TEXT NOT NULL,world_id TEXT NOT NULL,chunk_x INTEGER NOT NULL,chunk_z INTEGER NOT NULL,
                x INTEGER NOT NULL CHECK(typeof(x)='integer' AND x BETWEEN chunk_x*16 AND chunk_x*16+15),
                z INTEGER NOT NULL CHECK(typeof(z)='integer' AND z BETWEEN chunk_z*16 AND chunk_z*16+15),
                surface_y INTEGER NOT NULL CHECK(typeof(surface_y)='integer'),
                PRIMARY KEY(settlement_id,world_id,x,z),
                FOREIGN KEY(settlement_id,world_id,chunk_x,chunk_z) REFERENCES water_surveys(settlement_id,world_id,chunk_x,chunk_z)
            )
            """);
        execute(connection,"CREATE INDEX current_water_evidence ON water_surveys(settlement_id,world_id,complete,captured_millis)");
    }

    static void createFisheryHabitatChanges(Connection connection) throws SQLException {
        // NULL preserves the original capacity in all old saves, without a resource backfill/refill.
        execute(connection, """
                ALTER TABLE fishery_resources ADD COLUMN habitat_capacity INTEGER
                CHECK(habitat_capacity IS NULL OR (typeof(habitat_capacity)='integer'
                    AND habitat_capacity BETWEEN 0 AND capacity AND available<=habitat_capacity
                    AND (available<habitat_capacity OR remainder=0)))
                """);
        execute(connection, """
                CREATE TABLE fishery_habitat_changes (
                    operation_id TEXT PRIMARY KEY NOT NULL,
                    site_id TEXT NOT NULL REFERENCES fishery_resources(site_id),
                    evidence TEXT NOT NULL,
                    observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                    before_capacity INTEGER NOT NULL CHECK(typeof(before_capacity)='integer' AND before_capacity BETWEEN 0 AND 1000000000),
                    after_capacity INTEGER NOT NULL CHECK(typeof(after_capacity)='integer' AND after_capacity BETWEEN 0 AND 1000000000),
                    opening_units INTEGER NOT NULL CHECK(typeof(opening_units)='integer' AND opening_units BETWEEN 0 AND before_capacity),
                    lost INTEGER NOT NULL CHECK(typeof(lost)='integer' AND lost>=0),
                    closing_units INTEGER NOT NULL CHECK(typeof(closing_units)='integer' AND closing_units BETWEEN 0 AND after_capacity),
                    before_remainder INTEGER NOT NULL CHECK(typeof(before_remainder)='integer' AND before_remainder BETWEEN 0 AND 9999),
                    after_remainder INTEGER NOT NULL CHECK(typeof(after_remainder)='integer' AND after_remainder BETWEEN 0 AND 9999),
                    CHECK(closing_units=opening_units-lost),
                    CHECK(opening_units<before_capacity OR before_remainder=0),
                    CHECK(closing_units<after_capacity OR after_remainder=0)
                )
                """);
        execute(connection, "CREATE INDEX fishery_habitat_history ON fishery_habitat_changes(site_id,observed_ms)");
    }

    static void createFisheries(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE fishery_resources (
                    site_id TEXT PRIMARY KEY NOT NULL REFERENCES business_sites(id),
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    capacity INTEGER NOT NULL CHECK(typeof(capacity)='integer' AND capacity BETWEEN 1 AND 1000000000),
                    available INTEGER NOT NULL CHECK(typeof(available)='integer' AND available BETWEEN 0 AND capacity),
                    remainder INTEGER NOT NULL CHECK(typeof(remainder)='integer' AND remainder BETWEEN 0 AND 9999),
                    observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                    opening_terms TEXT NOT NULL,
                    CHECK(available<capacity OR remainder=0), UNIQUE(site_id,world_id)
                )
                """);
        execute(connection, """
                CREATE TABLE fishery_columns (
                    world_id TEXT NOT NULL,x INTEGER NOT NULL CHECK(typeof(x)='integer'),z INTEGER NOT NULL CHECK(typeof(z)='integer'),site_id TEXT NOT NULL,
                    PRIMARY KEY(world_id,x,z),
                    FOREIGN KEY(site_id,world_id) REFERENCES fishery_resources(site_id,world_id)
                )
                """);
        execute(connection, "CREATE INDEX fishery_site_columns ON fishery_columns(site_id,x,z)");
        execute(connection, """
                CREATE TABLE fishery_work (
                    operation_id TEXT PRIMARY KEY NOT NULL REFERENCES industry_operations(id),
                    site_id TEXT NOT NULL REFERENCES fishery_resources(site_id),business_id TEXT NOT NULL,
                    period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=0),
                    opening_units INTEGER NOT NULL CHECK(typeof(opening_units)='integer' AND opening_units>=0),
                    recovered INTEGER NOT NULL CHECK(typeof(recovered)='integer' AND recovered>=0),
                    caught INTEGER NOT NULL CHECK(typeof(caught)='integer' AND caught>=0),
                    closing_units INTEGER NOT NULL CHECK(typeof(closing_units)='integer' AND closing_units>=0),
                    before_remainder INTEGER NOT NULL CHECK(typeof(before_remainder)='integer' AND before_remainder BETWEEN 0 AND 9999),
                    after_remainder INTEGER NOT NULL CHECK(typeof(after_remainder)='integer' AND after_remainder BETWEEN 0 AND 9999),
                    policy_terms TEXT NOT NULL,
                    CHECK(closing_units=opening_units+recovered-caught),
                    FOREIGN KEY(business_id,period) REFERENCES industry_production(business_id,period)
                )
                """);
    }
    static void createVillageSupplyOrders(Connection connection)throws SQLException {
        execute(connection,"""
            CREATE TABLE village_supply_orders (
                operation_id TEXT PRIMARY KEY NOT NULL REFERENCES treasury_entries(operation_id),
                settlement_id TEXT NOT NULL REFERENCES economy_runtime(settlement_id),
                seller_id TEXT NOT NULL REFERENCES businesses(id),
                commodity TEXT NOT NULL CHECK(length(trim(commodity)) BETWEEN 1 AND 64),
                units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>0),
                price INTEGER NOT NULL CHECK(typeof(price)='integer' AND price>0),
                period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>0),
                due_period INTEGER NOT NULL CHECK(typeof(due_period)='integer' AND due_period>period),
                applied_period INTEGER CHECK(applied_period IS NULL OR (typeof(applied_period)='integer' AND applied_period>=due_period)),
                policy TEXT NOT NULL,
                FOREIGN KEY(settlement_id,period) REFERENCES needs_periods(settlement_id,period),
                FOREIGN KEY(settlement_id,applied_period) REFERENCES needs_periods(settlement_id,period)
            )
            """);
        execute(connection,"CREATE INDEX pending_village_supplies ON village_supply_orders(settlement_id,due_period) WHERE applied_period IS NULL");
    }
    static void extendFiniteResourceKinds(Connection connection)throws SQLException {
        execute(connection,"""
            CREATE TABLE mineral_discovery_v64 (
                deposit_id TEXT PRIMARY KEY NOT NULL REFERENCES mineral_deposits(id),
                world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                mineral TEXT NOT NULL CHECK(mineral IN ('coal','iron','stone','sand')),
                observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                UNIQUE(deposit_id,world_id)
            )
            """);
        execute(connection,"INSERT INTO mineral_discovery_v64 SELECT * FROM mineral_discovery");
        execute(connection,"DROP TABLE mineral_discovery");
        execute(connection,"ALTER TABLE mineral_discovery_v64 RENAME TO mineral_discovery");
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

    private static void createIndustryStores(Connection connection) throws SQLException {
        execute(connection,"""
                CREATE TABLE mineral_deposits (
                    id TEXT PRIMARY KEY NOT NULL, site_id TEXT NOT NULL UNIQUE REFERENCES business_sites(id),
                    initial_batches INTEGER NOT NULL CHECK(typeof(initial_batches)='integer' AND initial_batches>0),
                    remaining_batches INTEGER NOT NULL CHECK(typeof(remaining_batches)='integer' AND remaining_batches BETWEEN 0 AND initial_batches),
                    observed_ore INTEGER NOT NULL CHECK(typeof(observed_ore)='integer' AND observed_ore>0),
                    terms TEXT NOT NULL
                )
                """);
        execute(connection,"""
                CREATE TABLE mineral_yields (
                    deposit_id TEXT NOT NULL REFERENCES mineral_deposits(id), commodity TEXT NOT NULL,
                    units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>0), PRIMARY KEY(deposit_id,commodity)
                )
                """);
        execute(connection,"""
                CREATE TABLE industry_stores (
                    business_id TEXT PRIMARY KEY NOT NULL REFERENCES businesses(id),
                    capacity INTEGER NOT NULL CHECK(typeof(capacity)='integer' AND capacity>0),
                    hundredths INTEGER NOT NULL CHECK(typeof(hundredths)='integer' AND hundredths>=0), terms TEXT NOT NULL
                )
                """);
        execute(connection,"""
                CREATE TABLE industry_stock (
                    business_id TEXT NOT NULL REFERENCES industry_stores(business_id),commodity TEXT NOT NULL,
                    space_per_unit INTEGER NOT NULL CHECK(typeof(space_per_unit)='integer' AND space_per_unit>0),
                    units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>=0), PRIMARY KEY(business_id,commodity)
                )
                """);
        execute(connection,"""
                CREATE TABLE industry_operations (
                    id TEXT PRIMARY KEY NOT NULL,terms TEXT NOT NULL,
                    quantity INTEGER NOT NULL CHECK(typeof(quantity)='integer' AND quantity>=0),explanation TEXT NOT NULL
                )
                """);
        execute(connection,"""
                CREATE TABLE industry_production (
                    business_id TEXT NOT NULL REFERENCES businesses(id),
                    period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=0),
                    operation_id TEXT NOT NULL UNIQUE REFERENCES industry_operations(id),
                    PRIMARY KEY(business_id,period)
                )
                """);
        execute(connection,"""
                CREATE TABLE local_shipments (
                    id TEXT PRIMARY KEY NOT NULL REFERENCES industry_operations(id),
                    seller_id TEXT NOT NULL REFERENCES industry_stores(business_id), buyer_id TEXT NOT NULL REFERENCES industry_stores(business_id),
                    commodity TEXT NOT NULL,units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>0),
                    price INTEGER NOT NULL CHECK(typeof(price)='integer' AND price>=0),
                    dispatched_period INTEGER NOT NULL CHECK(typeof(dispatched_period)='integer' AND dispatched_period>=0),
                    due_period INTEGER NOT NULL CHECK(typeof(due_period)='integer' AND due_period>dispatched_period),
                    delivered_period INTEGER CHECK(delivered_period IS NULL OR (typeof(delivered_period)='integer' AND delivered_period>=due_period)),
                    CHECK(seller_id<>buyer_id),
                    FOREIGN KEY(seller_id,commodity) REFERENCES industry_stock(business_id,commodity),
                    FOREIGN KEY(buyer_id,commodity) REFERENCES industry_stock(business_id,commodity)
                )
                """);
        execute(connection,"CREATE INDEX local_shipments_waiting ON local_shipments(buyer_id,due_period,id) WHERE delivered_period IS NULL");
    }

    private static void createBusinesses(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE business_sites (
                    id TEXT PRIMARY KEY NOT NULL,
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL,
                    observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                    evidence TEXT NOT NULL CHECK(length(trim(evidence)) BETWEEN 1 AND 2048),
                    UNIQUE(id,settlement_id),
                    UNIQUE(world_id,x,y,z)
                )
                """);
        execute(connection, """
                CREATE TABLE businesses (
                    id TEXT PRIMARY KEY NOT NULL,
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 128),
                    founder_id TEXT NOT NULL REFERENCES residents(id),
                    owner_id TEXT NOT NULL REFERENCES residents(id),
                    industry TEXT NOT NULL CHECK(length(trim(industry)) BETWEEN 1 AND 64),
                    site_id TEXT NOT NULL UNIQUE,
                    worker_capacity INTEGER NOT NULL CHECK(typeof(worker_capacity)='integer' AND worker_capacity BETWEEN 1 AND 1000),
                    founded_step INTEGER NOT NULL CHECK(typeof(founded_step)='integer' AND founded_step>=0),
                    startup_hundredths INTEGER NOT NULL CHECK(typeof(startup_hundredths)='integer' AND startup_hundredths>=0),
                    reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 2048),
                    founder_min_age INTEGER NOT NULL CHECK(typeof(founder_min_age)='integer' AND founder_min_age>=0),
                    worker_min_age INTEGER NOT NULL CHECK(typeof(worker_min_age)='integer' AND worker_min_age>=0),
                    FOREIGN KEY(site_id,settlement_id) REFERENCES business_sites(id,settlement_id)
                )
                """);
        execute(connection, """
                CREATE TABLE business_skill_requirements (
                    business_id TEXT NOT NULL REFERENCES businesses(id),
                    role TEXT NOT NULL CHECK(role IN ('FOUNDER','WORKER')),
                    skill TEXT NOT NULL CHECK(length(trim(skill)) BETWEEN 1 AND 64),
                    experience INTEGER NOT NULL CHECK(typeof(experience)='integer' AND experience>=0),
                    PRIMARY KEY(business_id,role,skill)
                )
                """);
        execute(connection, """
                CREATE TABLE business_employment (
                    id TEXT PRIMARY KEY NOT NULL,
                    business_id TEXT NOT NULL REFERENCES businesses(id),
                    resident_id TEXT NOT NULL REFERENCES residents(id),
                    started_step INTEGER NOT NULL CHECK(typeof(started_step)='integer' AND started_step>=0),
                    ended_step INTEGER CHECK(ended_step IS NULL OR (typeof(ended_step)='integer' AND ended_step>=started_step)),
                    end_reason TEXT,
                    CHECK((ended_step IS NULL AND end_reason IS NULL) OR
                          (ended_step IS NOT NULL AND end_reason IS NOT NULL AND length(trim(end_reason)) BETWEEN 1 AND 2048))
                )
                """);
        execute(connection,"CREATE UNIQUE INDEX one_current_business_job ON business_employment(resident_id) WHERE ended_step IS NULL");
        execute(connection,"CREATE INDEX business_jobs_by_business ON business_employment(business_id,started_step,id)");
        execute(connection,"CREATE INDEX businesses_by_settlement ON businesses(settlement_id,id)");
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

    private static void createBusinessSales(Connection connection)throws SQLException {
        execute(connection,"""
                CREATE TABLE family_estates (
                    decedent_id TEXT PRIMARY KEY REFERENCES residents(id),
                    hundredths INTEGER NOT NULL CHECK(typeof(hundredths)='integer' AND hundredths>=0)
                )
                """);
        execute(connection,"""
                CREATE TABLE business_sales (
                    operation_id TEXT PRIMARY KEY,
                    business_id TEXT NOT NULL REFERENCES businesses(id),
                    previous_owner TEXT NOT NULL REFERENCES residents(id),
                    next_owner TEXT NOT NULL REFERENCES residents(id),
                    observed_step INTEGER NOT NULL CHECK(typeof(observed_step)='integer' AND observed_step>=0),
                    cash_source TEXT REFERENCES industry_stores(business_id),
                    cash_amount INTEGER NOT NULL CHECK(typeof(cash_amount)='integer' AND cash_amount>=0),
                    estate_source TEXT REFERENCES family_estates(decedent_id),
                    estate_amount INTEGER NOT NULL CHECK(typeof(estate_amount)='integer' AND estate_amount>=0),
                    CHECK((cash_amount=0 AND cash_source IS NULL) OR (cash_amount>0 AND cash_source IS NOT NULL)),
                    CHECK((estate_amount=0 AND estate_source IS NULL) OR (estate_amount>0 AND estate_source IS NOT NULL)),
                    CHECK(typeof(cash_amount+estate_amount)='integer' AND cash_amount+estate_amount>0),
                    CHECK(previous_owner<>next_owner AND (cash_source IS NULL OR cash_source<>business_id)),
                    UNIQUE(business_id,previous_owner)
                )
                """);
        execute(connection,"""
                CREATE TABLE business_sale_entries (
                    operation_id TEXT NOT NULL REFERENCES business_sales(operation_id),
                    source_kind TEXT NOT NULL CHECK(source_kind IN ('BUSINESS','ESTATE')),
                    source_id TEXT NOT NULL,
                    direction TEXT NOT NULL CHECK(direction IN ('PAYMENT','PROCEEDS')),
                    before_cash INTEGER NOT NULL CHECK(typeof(before_cash)='integer' AND before_cash>=0),
                    after_cash INTEGER NOT NULL CHECK(typeof(after_cash)='integer' AND after_cash>=0),
                    CHECK((direction='PAYMENT' AND before_cash>after_cash) OR (direction='PROCEEDS' AND after_cash>before_cash AND source_kind='ESTATE')),
                    PRIMARY KEY(operation_id,source_kind,source_id,direction)
                )
                """);
        execute(connection,"CREATE INDEX business_sale_history ON business_sales(business_id,observed_step,operation_id)");
        execute(connection,"CREATE INDEX estate_sale_history ON business_sale_entries(source_kind,source_id,operation_id)");
    }

    private static void createWorkshopStarterSupplies(Connection connection)throws SQLException {
        execute(connection,"""
            CREATE TABLE workshop_starter_orders (
                operation_id TEXT PRIMARY KEY NOT NULL,
                business_id TEXT NOT NULL UNIQUE REFERENCES industry_stores(business_id),
                settlement_id TEXT NOT NULL REFERENCES settlements(id),
                food_period INTEGER NOT NULL CHECK(typeof(food_period)='integer' AND food_period>=1),
                due_period INTEGER NOT NULL CHECK(typeof(due_period)='integer' AND due_period>=1),
                price INTEGER NOT NULL CHECK(typeof(price)='integer' AND price>0),
                before_cash INTEGER NOT NULL CHECK(typeof(before_cash)='integer' AND before_cash>=price),
                after_cash INTEGER NOT NULL CHECK(typeof(after_cash)='integer' AND after_cash=before_cash-price),
                terms TEXT NOT NULL,policy TEXT NOT NULL,
                delivered_period INTEGER CHECK(delivered_period IS NULL OR (typeof(delivered_period)='integer' AND delivered_period>=due_period)),
                FOREIGN KEY(settlement_id,food_period) REFERENCES needs_periods(settlement_id,period)
            )
            """);
        execute(connection,"""
            CREATE TABLE workshop_starter_goods (
                operation_id TEXT NOT NULL REFERENCES workshop_starter_orders(operation_id),
                commodity TEXT NOT NULL CHECK(commodity<>'food'),
                units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>0),
                unit_price INTEGER NOT NULL CHECK(typeof(unit_price)='integer' AND unit_price>0),
                protected_units INTEGER NOT NULL CHECK(typeof(protected_units)='integer' AND protected_units>=0),
                before_units INTEGER NOT NULL CHECK(typeof(before_units)='integer' AND before_units>=units+protected_units),
                after_units INTEGER NOT NULL CHECK(typeof(after_units)='integer' AND after_units=before_units-units),
                PRIMARY KEY(operation_id,commodity)
            )
            """);
        execute(connection,"CREATE INDEX pending_workshop_starters ON workshop_starter_orders(business_id,due_period) WHERE delivered_period IS NULL");
    }
    private static void createWorkplaceOperations(Connection connection)throws SQLException {
        execute(connection,"""
            CREATE TABLE business_workplace_anchors (
                business_id TEXT PRIMARY KEY NOT NULL REFERENCES businesses(id),
                world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                kind TEXT NOT NULL CHECK(kind IN ('BARREL','SMITHING_TABLE')),
                captured_millis INTEGER NOT NULL CHECK(typeof(captured_millis)='integer' AND captured_millis>=0),
                present INTEGER NOT NULL CHECK(present IN (0,1)),
                UNIQUE(world_id,x,y,z)
            )
            """);
        execute(connection,"""
            CREATE TABLE workplace_operation_history (
                business_id TEXT NOT NULL REFERENCES business_workplace_anchors(business_id),
                captured_millis INTEGER NOT NULL CHECK(typeof(captured_millis)='integer' AND captured_millis>=0),
                present INTEGER NOT NULL CHECK(present IN (0,1)),
                PRIMARY KEY(business_id,captured_millis)
            )
            """);
        execute(connection,"CREATE TABLE workplace_operation_settings (singleton INTEGER PRIMARY KEY CHECK(singleton=1),pause_on_missing INTEGER NOT NULL CHECK(pause_on_missing IN (0,1)))");
        execute(connection,"INSERT INTO workplace_operation_settings VALUES(1,1)");
    }
    private static void createWarehouseFounderStudy(Connection connection)throws SQLException {
        execute(connection,"""
            CREATE TABLE warehouse_founder_reviews (
                settlement_id TEXT NOT NULL REFERENCES settlements(id),period INTEGER NOT NULL CHECK(period>=1),
                after_resident TEXT NOT NULL,policy TEXT NOT NULL,explanation TEXT NOT NULL,
                PRIMARY KEY(settlement_id,period)
            )
            """);
        execute(connection,"""
            CREATE TABLE warehouse_founder_courses (
                course_id TEXT PRIMARY KEY REFERENCES resident_courses(id),settlement_id TEXT NOT NULL REFERENCES settlements(id),
                world_id TEXT NOT NULL REFERENCES observed_worlds(id),x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                enrolled_period INTEGER NOT NULL CHECK(enrolled_period>=1),
                FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id),
                FOREIGN KEY(settlement_id,enrolled_period) REFERENCES warehouse_founder_reviews(settlement_id,period) DEFERRABLE INITIALLY DEFERRED
            )
            """);
        execute(connection,"CREATE INDEX active_warehouse_founders ON warehouse_founder_courses(settlement_id,course_id)");
    }
    private static void createWorkshopFounderStudy(Connection connection)throws SQLException {
        execute(connection,"""
            CREATE TABLE workshop_founder_reviews (
                settlement_id TEXT NOT NULL REFERENCES settlements(id),period INTEGER NOT NULL CHECK(period>=1),
                after_resident TEXT NOT NULL,policy TEXT NOT NULL,explanation TEXT NOT NULL,
                PRIMARY KEY(settlement_id,period)
            )
            """);
        execute(connection,"""
            CREATE TABLE workshop_founder_courses (
                course_id TEXT PRIMARY KEY REFERENCES resident_courses(id),settlement_id TEXT NOT NULL REFERENCES settlements(id),
                world_id TEXT NOT NULL REFERENCES observed_worlds(id),x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                enrolled_period INTEGER NOT NULL CHECK(enrolled_period>=1),
                FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id),
                FOREIGN KEY(settlement_id,enrolled_period) REFERENCES workshop_founder_reviews(settlement_id,period) DEFERRABLE INITIALLY DEFERRED
            )
            """);
        execute(connection,"CREATE INDEX active_workshop_founders ON workshop_founder_courses(settlement_id,course_id)");
    }
    private static void createCraftsmanFounderStudy(Connection connection)throws SQLException {
        execute(connection,"""
            CREATE TABLE craftsman_founder_reviews (
                settlement_id TEXT NOT NULL REFERENCES settlements(id),period INTEGER NOT NULL CHECK(period>=1),
                after_resident TEXT NOT NULL,policy TEXT NOT NULL,explanation TEXT NOT NULL,
                PRIMARY KEY(settlement_id,period)
            )
            """);
        execute(connection,"""
            CREATE TABLE craftsman_founder_courses (
                course_id TEXT PRIMARY KEY REFERENCES resident_courses(id),settlement_id TEXT NOT NULL REFERENCES settlements(id),
                world_id TEXT NOT NULL REFERENCES observed_worlds(id),x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                enrolled_period INTEGER NOT NULL CHECK(enrolled_period>=1),
                FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id),
                FOREIGN KEY(settlement_id,enrolled_period) REFERENCES craftsman_founder_reviews(settlement_id,period) DEFERRABLE INITIALLY DEFERRED
            )
            """);
        execute(connection,"CREATE INDEX active_craftsman_founders ON craftsman_founder_courses(settlement_id,course_id)");
    }
    private static void createBakeryFounderStudy(Connection connection)throws SQLException {
        execute(connection,"CREATE INDEX IF NOT EXISTS bakery_paid_food_market ON village_supply_orders(settlement_id,commodity,period,operation_id)");
        execute(connection,"""
            CREATE TABLE bakery_founder_reviews (
                settlement_id TEXT NOT NULL REFERENCES settlements(id),period INTEGER NOT NULL CHECK(period>=1),
                after_resident TEXT NOT NULL,policy TEXT NOT NULL,explanation TEXT NOT NULL,
                PRIMARY KEY(settlement_id,period)
            )
            """);
        execute(connection,"""
            CREATE TABLE bakery_founder_courses (
                course_id TEXT PRIMARY KEY REFERENCES resident_courses(id),settlement_id TEXT NOT NULL REFERENCES settlements(id),
                world_id TEXT NOT NULL REFERENCES observed_worlds(id),x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                enrolled_period INTEGER NOT NULL CHECK(enrolled_period>=1),
                FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id),
                FOREIGN KEY(settlement_id,enrolled_period) REFERENCES bakery_founder_reviews(settlement_id,period) DEFERRABLE INITIALLY DEFERRED
            )
            """);
        execute(connection,"CREATE INDEX active_bakery_founders ON bakery_founder_courses(settlement_id,course_id)");
    }
    static void createFisheryWorkplaces(Connection connection)throws SQLException {
        execute(connection,"""
            CREATE TABLE fishery_workplaces (
                site_id TEXT PRIMARY KEY NOT NULL REFERENCES fishery_resources(site_id),world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                surface_y INTEGER NOT NULL,minimum_depth INTEGER NOT NULL CHECK(minimum_depth BETWEEN 1 AND 64),
                maximum_rise INTEGER NOT NULL CHECK(maximum_rise BETWEEN 0 AND 4),maximum_distance INTEGER NOT NULL CHECK(maximum_distance BETWEEN 0 AND 256),
                x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,unsafe INTEGER NOT NULL CHECK(unsafe IN(0,1)),
                observed_ms INTEGER NOT NULL CHECK(observed_ms>=0),opening_hash TEXT NOT NULL,latest_fingerprint TEXT NOT NULL
            )
            """);
        execute(connection,"""
            CREATE TABLE fishery_workplace_history (
                site_id TEXT NOT NULL REFERENCES fishery_workplaces(site_id),observed_ms INTEGER NOT NULL CHECK(observed_ms>=0),
                before_x INTEGER NOT NULL,before_y INTEGER NOT NULL,before_z INTEGER NOT NULL,
                after_x INTEGER NOT NULL,after_y INTEGER NOT NULL,after_z INTEGER NOT NULL,
                before_unsafe INTEGER NOT NULL CHECK(before_unsafe IN(0,1)),after_unsafe INTEGER NOT NULL CHECK(after_unsafe IN(0,1)),
                action TEXT NOT NULL CHECK(action IN('KEEP','RELOCATE','WAIT_UNKNOWN','NEEDS_REPAIR')),
                confirmed_safe INTEGER NOT NULL CHECK(confirmed_safe IN(0,1)),confirmed_damage INTEGER NOT NULL CHECK(confirmed_damage IN(0,1)),
                fingerprint TEXT NOT NULL,explanation TEXT NOT NULL,PRIMARY KEY(site_id,observed_ms)
            )
            """);
    }
    static void createFisheryFounderStudy(Connection connection)throws SQLException {
        execute(connection,"""
            CREATE TABLE fishery_founder_courses (
                course_id TEXT PRIMARY KEY NOT NULL REFERENCES resident_courses(id),
                settlement_id TEXT NOT NULL REFERENCES settlements(id),world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                habitat_key TEXT NOT NULL,x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                observed_ms INTEGER NOT NULL CHECK(observed_ms>=0),period INTEGER NOT NULL CHECK(period>0)
            )
            """);
        execute(connection,"CREATE INDEX fishery_founder_towns ON fishery_founder_courses(settlement_id,course_id)");
    }
    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
