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
        var decision = VillagePriorities.review(seed,needs,policy);
        try (var s = connection.prepareStatement("INSERT INTO village_priorities VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,needs.settlement().toString()); s.setLong(2,needs.period()); s.setLong(3,seed); s.setInt(4,1);
            s.setString(5,decision.foodCondition().name()); s.setLong(6,decision.foodUnmet()); s.setLong(7,decision.crowdedHomes()); s.setString(8,decision.selected().name());
            s.setInt(9,policy.reviewAfterPeriods()); s.setInt(10,policy.foodWeight()); s.setInt(11,policy.crisisMultiplier()); s.setInt(12,policy.housingWeight());
            s.setInt(13,policy.monitorWeight()); s.setInt(14,policy.minimumRelativePercent()); s.executeUpdate();
        }
    }
    Optional<Decision> latest(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM village_priorities WHERE settlement_id=? ORDER BY period DESC LIMIT 1")) {
            s.setString(1,town.toString()); try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                if (r.getInt("replay_version") != 1) throw new SQLException("Unsupported priority replay version");
                var policy = new Policy(r.getInt("review_after"),r.getInt("food_weight"),r.getInt("crisis_multiplier"),r.getInt("housing_weight"),r.getInt("monitor_weight"),r.getInt("relative_percent"));
                var facts = new Facts(town,r.getLong("period"),Condition.valueOf(r.getString("food_condition")),r.getLong("food_unmet"),r.getLong("crowded_homes"));
                var replay = VillagePriorities.review(r.getLong("seed"),facts,policy);
                if (!replay.selected().name().equals(r.getString("selected"))) throw new SQLException("Saved priority selection does not match replay");
                return Optional.of(replay);
            }
        }
    }
}
