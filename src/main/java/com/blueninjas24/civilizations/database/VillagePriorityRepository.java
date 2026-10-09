package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.VillagePriorities.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.Condition;
import java.sql.*;
import java.util.*;

/** Reviews share the food/needs transaction; retained inputs reconstruct the original explanation. */
final class VillagePriorityRepository {
    private final Connection connection;
    VillagePriorityRepository(Connection connection) { this.connection = connection; }
    void consider(HouseholdNeeds.Assessment needs, long seed, Policy policy) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Priority must share the food transaction");
        var previous = latest(needs.settlement()).orElse(null);
        if (previous != null && previous.seed() != seed) throw new SQLException("Priority seed changed; explicit transition required");
        if (!VillagePriorities.shouldReview(needs,policy,previous)) return;
        var evidence = evidence(needs.settlement(),needs.period(),policy);
        var decision = VillagePriorities.review(seed,new Facts(needs.settlement(),needs.period(),needs.sharedFood(),needs.demand()-needs.fulfilled(),VillagePriorities.crowded(needs)),policy,evidence);
        try (var s = connection.prepareStatement("""
                INSERT INTO village_priorities (settlement_id,period,seed,replay_version,food_condition,food_unmet,crowded_homes,selected,
                review_after,food_weight,crisis_multiplier,housing_weight,monitor_weight,relative_percent,learning_replay)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            s.setString(1,needs.settlement().toString()); s.setLong(2,needs.period()); s.setLong(3,seed); s.setInt(4,1);
            s.setString(5,decision.foodCondition().name()); s.setLong(6,decision.foodUnmet()); s.setLong(7,decision.crowdedHomes()); s.setString(8,decision.selected().name());
            s.setInt(9,policy.reviewAfterPeriods()); s.setInt(10,policy.foodWeight()); s.setInt(11,policy.crisisMultiplier()); s.setInt(12,policy.housingWeight());
            s.setInt(13,policy.monitorWeight()); s.setInt(14,policy.minimumRelativePercent());s.setInt(15,policy.learningPeriods()>0?1:0);s.executeUpdate();
        }
        if (policy.learningPeriods()>0) try(var s=connection.prepareStatement("INSERT INTO priority_learning VALUES (?,?,1,?,?,?,?)")) {
            s.setString(1,needs.settlement().toString());s.setLong(2,needs.period());s.setInt(3,policy.learningPeriods());
            s.setInt(4,policy.maximumFoodBonusPercent());s.setLong(5,evidence.imported());s.setLong(6,evidence.avoidedUnmet());s.executeUpdate();
        }
    }
    private Evidence evidence(UUID town,long period,Policy policy) throws SQLException {
        if(policy.learningPeriods()==0)return Evidence.none();
        long imported=0,avoided=0;
        // Only earlier applied deliveries count, not reservations, current-period outcomes or arbitrary incoming estimates.
        try(var s=connection.prepareStatement("""
                SELECT p.* FROM economy_periods p
                WHERE p.settlement_id=? AND p.commodity='food' AND p.period>=? AND p.period<? AND p.imports>0
                AND p.imports=(SELECT COALESCE(SUM(d.units),0) FROM food_deliveries d
                              WHERE d.settlement_id=p.settlement_id AND d.applied_period=p.period)
                ORDER BY p.period
                """)) {
            s.setString(1,town.toString());s.setLong(2,Math.max(1,period-policy.learningPeriods()));s.setLong(3,period);
            try(var r=s.executeQuery()) {while(r.next()) {
                var flow=new CommodityAccounting.Flow(r.getLong("production"),r.getLong("imports"),r.getLong("households"),
                        r.getLong("industry"),r.getLong("construction"),r.getLong("exports"),r.getLong("events"));
                var impact=FoodImportImpact.assessInputs(r.getLong("opening"),flow);
                imported=Math.addExact(imported,impact.imported());avoided=Math.addExact(avoided,impact.avoidedUnmet());
            }}
        }
        return new Evidence(imported,avoided);
    }
    Optional<Decision> latest(UUID town) throws SQLException {
        int schema;
        try(var s=connection.createStatement();var r=s.executeQuery("PRAGMA user_version")){schema=r.getInt(1);}
        // Read retained legacy choices during a failed upgrade; never hide a missing current-schema table.
        String query=schema<25 ? """
                SELECT p.*, 0 AS learning_replay, NULL AS learning_version, NULL AS window_periods,
                NULL AS maximum_bonus, NULL AS imported, NULL AS avoided_unmet FROM village_priorities p
                WHERE p.settlement_id=? ORDER BY p.period DESC LIMIT 1
                """ : """
                SELECT p.*, l.replay_version AS learning_version, l.window_periods, l.maximum_bonus, l.imported, l.avoided_unmet
                FROM village_priorities p LEFT JOIN priority_learning l ON l.settlement_id=p.settlement_id AND l.period=p.period
                WHERE p.settlement_id=? ORDER BY p.period DESC LIMIT 1
                """;
        try (var s = connection.prepareStatement(query)) {
            s.setString(1,town.toString()); try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                if (r.getInt("replay_version") != 1) throw new SQLException("Unsupported priority replay version");
                var policy = new Policy(r.getInt("review_after"),r.getInt("food_weight"),r.getInt("crisis_multiplier"),r.getInt("housing_weight"),r.getInt("monitor_weight"),r.getInt("relative_percent"));
                int version=r.getInt("learning_version");boolean legacy=r.wasNull();
                if((r.getInt("learning_replay")==0)!=legacy)throw new SQLException("Saved priority learning evidence missing or inconsistent");
                if(!legacy && version!=1)throw new SQLException("Unsupported priority learning replay version");
                var evidence=Evidence.none();
                if(!legacy) {policy=policy.withLearning(r.getInt("window_periods"),r.getInt("maximum_bonus"));evidence=new Evidence(r.getLong("imported"),r.getLong("avoided_unmet"));}
                var facts = new Facts(town,r.getLong("period"),Condition.valueOf(r.getString("food_condition")),r.getLong("food_unmet"),r.getLong("crowded_homes"));
                var replay = VillagePriorities.review(r.getLong("seed"),facts,policy,evidence);
                if (!replay.selected().name().equals(r.getString("selected"))) throw new SQLException("Saved priority selection does not match replay");
                return Optional.of(replay);
            }
        }
    }
}
