package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.FoodAppeals.*;
import java.sql.*;
import java.util.*;

/** One retained observation per new food period, inside its existing transaction. */
final class FoodAppealRepository {
    private final Connection connection;
    FoodAppealRepository(Connection connection) { this.connection=connection; }
    void save(Snapshot appeal) throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Appeal must share the food transaction");
        try(var s=connection.prepareStatement("INSERT INTO food_appeals (settlement_id,period,priority_period,status,unmet,previous_unmet,imports,production,change) VALUES (?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,appeal.settlement().toString());s.setLong(2,appeal.period());s.setLong(3,appeal.priorityPeriod());s.setString(4,appeal.status().name());
            s.setLong(5,appeal.unmet());if(appeal.previousUnmet()==null)s.setNull(6,Types.BIGINT);else s.setLong(6,appeal.previousUnmet());
            s.setLong(7,appeal.imports());s.setLong(8,appeal.production());s.setString(9,appeal.change().name());s.executeUpdate();
        }
    }
    Optional<Snapshot> load(UUID town,long period) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM food_appeals WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);
            try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();long prior=r.getLong("previous_unmet");boolean unknown=r.wasNull();
                return Optional.of(new Snapshot(town,period,r.getLong("priority_period"),Status.valueOf(r.getString("status")),r.getLong("unmet"),
                        unknown?null:prior,r.getLong("imports"),r.getLong("production"),Change.valueOf(r.getString("change"))));
            }
        }
    }
}
