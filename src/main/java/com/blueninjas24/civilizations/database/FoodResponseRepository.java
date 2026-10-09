package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;
import com.blueninjas24.civilizations.economy.FoodResponses;
import com.blueninjas24.civilizations.economy.FoodResponses.*;

/** Immutable published comparisons share their food period transaction and replay their saved candidate inputs. */
final class FoodResponseRepository {
    private final Connection connection;
    FoodResponseRepository(Connection connection){this.connection=connection;}
    void save(Plan plan)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Response must share food transaction");
        try(var s=connection.prepareStatement("INSERT INTO food_responses VALUES (?,?,?,1,?,?)")) {
            s.setString(1,plan.settlement().toString());s.setLong(2,plan.period());s.setLong(3,plan.seed());s.setInt(4,plan.relativePercent());s.setString(5,plan.selected().name());s.executeUpdate();
        }
        try(var s=connection.prepareStatement("INSERT INTO food_response_candidates VALUES (?,?,?,?,?,?)")) {
            for(var c:plan.candidates()){s.setString(1,plan.settlement().toString());s.setLong(2,plan.period());s.setString(3,c.action().name());s.setInt(4,c.feasible()?1:0);s.setLong(5,c.weight());s.setString(6,c.reason());s.executeUpdate();}
        }
    }
    Optional<Plan> load(UUID town,long period)throws SQLException {
        int schema;try(var s=connection.createStatement();var r=s.executeQuery("PRAGMA user_version")){schema=r.getInt(1);}
        if(schema<26)return Optional.empty();
        long seed;int relative;String selected;
        try(var s=connection.prepareStatement("SELECT * FROM food_responses WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();if(r.getInt("replay_version")!=1)throw new SQLException("Unsupported food response replay");
                seed=r.getLong("seed");relative=r.getInt("relative_percent");selected=r.getString("selected");
            }
        }
        var raw=new ArrayList<Candidate>();
        try(var s=connection.prepareStatement("SELECT * FROM food_response_candidates WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){while(r.next())raw.add(new Candidate(Action.valueOf(r.getString("action")),r.getInt("feasible")==1,false,r.getLong("weight"),r.getString("reason")));}
        }
        try {
            var replay=FoodResponses.choose(town,period,seed,relative,raw);
            if(!replay.selected().name().equals(selected))throw new SQLException("Saved response differs from replay");
            return Optional.of(replay);
        }catch(IllegalArgumentException invalid){throw new SQLException("Invalid saved response comparison",invalid);}
    }
}
