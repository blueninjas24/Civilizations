package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import java.sql.*;
import java.util.*;

/** Welfare history shares its needs/food transaction; saved membership carries welfare across home changes. */
final class HouseholdWellbeingRepository {
    private final Connection connection;
    HouseholdWellbeingRepository(Connection connection) { this.connection = connection; }

    void assess(HouseholdNeeds.Assessment needs, HouseholdWellbeing.Policy policy, long crisisAfter) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Wellbeing must share its food transaction");
        if (!policy.enabled()) return;
        for (var home : needs.homes()) {
            var members = new ArrayList<UUID>();
            var streaks = new ArrayList<Long>();
            long longestStreak = 0;
            long scores = 0;
            try (var s = connection.prepareStatement("""
                    SELECT r.id,w.after_score AS previous_score,m.food_streak FROM residents r
                    LEFT JOIN wellbeing_members m ON m.resident_id=r.id AND m.sequence=
                    (SELECT max(prior.sequence) FROM wellbeing_members prior WHERE prior.resident_id=r.id)
                    LEFT JOIN household_wellbeing w ON w.sequence=m.sequence
                    WHERE r.household_id=? AND r.settlement_id=? AND r.status='ALIVE' ORDER BY r.id
                    """)) {
                s.setString(1, home.household().toString()); s.setString(2, needs.settlement().toString());
                try (var r = s.executeQuery()) {
                    while (r.next()) {
                        members.add(UUID.fromString(r.getString("id")));
                        int score = r.getInt("previous_score"); if (r.wasNull()) score = policy.startingScore();
                        scores = Math.addExact(scores, score);
                        long priorStreak = r.getLong("food_streak");
                        long streak = home.supplied()==null ? priorStreak : home.supplied()==home.foodDemand() ? 0 : Math.incrementExact(priorStreak);
                        streaks.add(streak); longestStreak = Math.max(longestStreak,streak);
                    }
                }
            }
            if (members.size() != Math.addExact(home.adults(),home.children()) || members.isEmpty())
                throw new SQLException("Wellbeing membership and needs disagree");
            // Actual residents retain history across moves; newly observed people use a documented neutral initial score.
            var residentNeeds = new HouseholdNeeds.Home(home.household(),home.adults(),home.children(),home.foodDemand(),home.capacity(),
                    home.supplied(),home.supplied()==null ? null : longestStreak);
            var snapshot = HouseholdWellbeing.assess(residentNeeds, Math.toIntExact(scores / members.size()), policy, crisisAfter);
            try (var s = connection.prepareStatement("""
                    INSERT INTO household_wellbeing (settlement_id,period,household_id,before_score,after_score,condition,
                    food,housing,starting_score,recovery,food_penalty,crisis_penalty,crowding_penalty,crisis_after,food_streak)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """)) {
                s.setString(1, needs.settlement().toString()); s.setLong(2, needs.period()); s.setString(3, home.household().toString());
                s.setInt(4, snapshot.before()); s.setInt(5, snapshot.after()); s.setString(6, snapshot.condition().name());
                s.setString(7, snapshot.food()); s.setString(8, snapshot.housing().name());
                s.setInt(9, policy.startingScore()); s.setInt(10, policy.recovery()); s.setInt(11, policy.foodPenalty());
                s.setInt(12, policy.crisisPenalty()); s.setInt(13, policy.crowdingPenalty()); s.setLong(14, crisisAfter);
                s.setLong(15,snapshot.foodStreak()); s.executeUpdate();
            }
            long sequence;
            try (var s = connection.createStatement(); var r = s.executeQuery("SELECT last_insert_rowid()")) { sequence = r.getLong(1); }
            try (var s = connection.prepareStatement("INSERT INTO wellbeing_members(sequence,resident_id,food_streak) VALUES (?,?,?)")) {
                for (int i=0;i<members.size();i++) { s.setLong(1,sequence); s.setString(2,members.get(i).toString()); s.setLong(3,streaks.get(i)); s.executeUpdate(); }
            }
        }
    }
    List<HouseholdWellbeing.Snapshot> load(UUID town, long period) throws SQLException {
        var result = new ArrayList<HouseholdWellbeing.Snapshot>();
        try (var s = connection.prepareStatement("SELECT * FROM household_wellbeing WHERE settlement_id=? AND period=? ORDER BY household_id")) {
            s.setString(1, town.toString()); s.setLong(2, period);
            try (var r = s.executeQuery()) {
                while (r.next()) result.add(new HouseholdWellbeing.Snapshot(UUID.fromString(r.getString("household_id")),
                        r.getInt("before_score"), r.getInt("after_score"), HouseholdWellbeing.Condition.valueOf(r.getString("condition")),
                        r.getString("food"), HouseholdNeeds.Housing.valueOf(r.getString("housing")),r.getLong("food_streak"),r.getLong("crisis_after"),
                        new HouseholdWellbeing.Policy(true, r.getInt("starting_score"), r.getInt("recovery"), r.getInt("food_penalty"),
                                r.getInt("crisis_penalty"), r.getInt("crowding_penalty"))));
            }
        }
        return List.copyOf(result);
    }
}
