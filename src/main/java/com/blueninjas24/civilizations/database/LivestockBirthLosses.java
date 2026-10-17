package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;
import com.blueninjas24.civilizations.database.LivestockLosses.*;
import com.blueninjas24.civilizations.database.LivestockBirthProjections.State;

/** Confirmed death of a current offspring body. Called only inside the existing complete loss transaction. */
final class LivestockBirthLosses {
    private final Connection connection;
    LivestockBirthLosses(Connection connection){this.connection=connection;}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE livestock_birth_losses (
                    birth_id TEXT PRIMARY KEY NOT NULL REFERENCES livestock_births(id),entity_id TEXT NOT NULL UNIQUE,
                    operation_id TEXT NOT NULL UNIQUE,business_id TEXT NOT NULL REFERENCES livestock_herds(business_id),
                    observed_world TEXT NOT NULL CHECK(length(observed_world)=36),
                    observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                    reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 256),
                    after_period INTEGER NOT NULL CHECK(typeof(after_period)='integer' AND after_period>=0),
                    applied_period INTEGER CHECK(applied_period IS NULL OR (typeof(applied_period)='integer' AND applied_period>after_period)),
                    FOREIGN KEY(birth_id,entity_id) REFERENCES livestock_birth_entities(birth_id,entity_id),
                    FOREIGN KEY(business_id,after_period) REFERENCES livestock_periods(business_id,period),
                    FOREIGN KEY(business_id,applied_period) REFERENCES livestock_periods(business_id,period)
                )
                """);
            s.execute("CREATE INDEX pending_birth_losses ON livestock_birth_losses(business_id,applied_period,entity_id)");
        }
    }
    Optional<Receipt> replayRows(Observation observed)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM livestock_birth_losses WHERE entity_id=? OR operation_id=?")) {
            s.setString(1,observed.animal().toString());s.setString(2,observed.operation().toString());
            try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                var saved=new Observation(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("entity_id")),UUID.fromString(r.getString("observed_world")),r.getLong("observed_millis"),r.getString("reason"));
                if(!saved.equals(observed))throw new SQLException("Offspring death identity/evidence changed");
                return Optional.of(new Receipt(saved,UUID.fromString(r.getString("business_id")),r.getLong("after_period")));
            }
        }
    }
    Optional<Receipt> recordRows(Observation observed)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Offspring loss requires owner transaction");
        var projection=new LivestockBirthProjections(connection).forEntity(observed.animal());
        if(projection.isEmpty()||projection.get().state()==State.FAILED)return Optional.empty();
        if(projection.get().state()==State.DECEASED)throw new SQLException("Deceased offspring lacks matching evidence");
        var birth=new LivestockBirths(connection).find(projection.get().claim().birth()).orElseThrow();
        var state=new LivestockRepository(connection).load(birth.business()).orElseThrow().state();
        try(var s=connection.prepareStatement("INSERT INTO livestock_birth_losses(birth_id,entity_id,operation_id,business_id,observed_world,observed_millis,reason,after_period) VALUES(?,?,?,?,?,?,?,?)")) {
            s.setString(1,birth.id().toString());s.setString(2,observed.animal().toString());s.setString(3,observed.operation().toString());s.setString(4,birth.business().toString());
            s.setString(5,observed.observedWorld().toString());s.setLong(6,observed.observedMillis());s.setString(7,observed.reason());s.setLong(8,state.period());s.executeUpdate();
        }
        try(var s=connection.prepareStatement("UPDATE livestock_birth_projections SET state='DECEASED' WHERE birth_id=? AND entity_id=? AND state IN('CLAIMED','ACTIVE')")) {
            s.setString(1,birth.id().toString());s.setString(2,observed.animal().toString());if(s.executeUpdate()!=1)throw new SQLException("Offspring body changed before death save");
        }
        new LivestockRepository(connection).load(birth.business()).orElseThrow();
        return Optional.of(new Receipt(observed,birth.business(),state.period()));
    }
}
