package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.LivestockHerd;
import java.sql.*;
import java.util.*;

/** Confirmed physical losses of saved initial UUIDs. Missing observations never establish death. */
public final class LivestockLosses {
    public record Observation(UUID operation,UUID animal,UUID observedWorld,long observedMillis,String reason) {
        public Observation {
            Objects.requireNonNull(operation);Objects.requireNonNull(animal);Objects.requireNonNull(observedWorld);Objects.requireNonNull(reason);
            if(observedMillis<0||reason.isBlank()||reason.length()>256)throw new IllegalArgumentException("Invalid physical livestock loss");
        }
    }
    public record Receipt(Observation observation,UUID business,long afterPeriod) {}
    private final Connection connection;
    public LivestockLosses(Connection connection){this.connection=Objects.requireNonNull(connection);}
    static void createSchema(Connection connection)throws SQLException {
        try(var s=connection.createStatement()) {
            s.execute("""
                CREATE TABLE livestock_animal_losses (
                    animal_id TEXT PRIMARY KEY NOT NULL REFERENCES livestock_initial_animals(animal_id),
                    operation_id TEXT NOT NULL UNIQUE,business_id TEXT NOT NULL REFERENCES livestock_herds(business_id),
                    observed_world TEXT NOT NULL CHECK(length(observed_world)=36),
                    observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0),
                    reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 256),
                    after_period INTEGER NOT NULL CHECK(typeof(after_period)='integer' AND after_period>=0),
                    applied_period INTEGER CHECK(applied_period IS NULL OR (typeof(applied_period)='integer' AND applied_period>after_period)),
                    FOREIGN KEY(business_id,after_period) REFERENCES livestock_periods(business_id,period),
                    FOREIGN KEY(business_id,applied_period) REFERENCES livestock_periods(business_id,period)
                )
                """);
            s.execute("CREATE INDEX pending_livestock_losses ON livestock_animal_losses(business_id,applied_period,animal_id)");
        }
    }
    /** Trusted event evidence only. Unknown/unadopted/player animals are not inserted or claimed. */
    public Optional<Receipt> record(Observation observed)throws SQLException {
        Objects.requireNonNull(observed);if(!connection.getAutoCommit())throw new SQLException("Physical loss requires complete owner transaction");
        connection.setAutoCommit(false);
        try {var result=recordRows(observed);connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private Optional<Receipt> recordRows(Observation observed)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM livestock_animal_losses WHERE animal_id=? OR operation_id=?")) {
            s.setString(1,observed.animal().toString());s.setString(2,observed.operation().toString());try(var r=s.executeQuery()) {
                if(r.next()) {
                    var saved=new Observation(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("animal_id")),UUID.fromString(r.getString("observed_world")),r.getLong("observed_millis"),r.getString("reason"));
                    if(!saved.equals(observed))throw new SQLException("Physical livestock loss identity/evidence changed");
                    return Optional.of(new Receipt(saved,UUID.fromString(r.getString("business_id")),r.getLong("after_period")));
                }
            }
        }
        UUID business;
        try(var s=connection.prepareStatement("SELECT business_id FROM livestock_initial_animals WHERE animal_id=?")) {
            s.setString(1,observed.animal().toString());try(var r=s.executeQuery()){if(!r.next())return Optional.empty();business=UUID.fromString(r.getString(1));}
        }
        var state=new LivestockRepository(connection).load(business).orElseThrow().state();
        try(var s=connection.prepareStatement("INSERT INTO livestock_animal_losses(animal_id,operation_id,business_id,observed_world,observed_millis,reason,after_period) VALUES(?,?,?,?,?,?,?)")) {
            s.setString(1,observed.animal().toString());s.setString(2,observed.operation().toString());s.setString(3,business.toString());s.setString(4,observed.observedWorld().toString());s.setLong(5,observed.observedMillis());s.setString(6,observed.reason());s.setLong(7,state.period());s.executeUpdate();
        }
        new LivestockRepository(connection).load(business).orElseThrow(); // Reject impossible/overdrawn head changes in the same transaction.
        return Optional.of(new Receipt(observed,business,state.period()));
    }
    /** Overlay only unapplied observations; old production receipts and their balances remain immutable. */
    LivestockHerd.State remainingRows(UUID business,LivestockHerd.State before)throws SQLException {
        long adults=before.adults();var young=new TreeMap<Long,Long>();for(var c:before.young())young.put(c.maturePeriod(),c.heads());
        try(var s=connection.prepareStatement("""
            SELECT a.mature_period FROM livestock_animal_losses l JOIN livestock_initial_animals a ON a.animal_id=l.animal_id
            WHERE l.business_id=? AND l.applied_period IS NULL ORDER BY l.animal_id
            """)) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){while(r.next()) {
                long maturity=r.getLong(1);
                if(maturity<=before.period()){if(adults==0)throw new SQLException("Physical loss exceeds retained adult herd");adults--;}
                else {long heads=young.getOrDefault(maturity,0L);if(heads==0)throw new SQLException("Physical loss exceeds retained young cohort");if(heads==1)young.remove(maturity);else young.put(maturity,heads-1);}
            }}
        }
        return new LivestockHerd.State(before.period(),adults,before.capacity(),before.nextBirthPeriod(),young.entrySet().stream().map(e->new LivestockHerd.Young(e.getKey(),e.getValue())).toList());
    }
    void applyRows(UUID business,long period)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Loss application requires production owner transaction");
        try(var s=connection.prepareStatement("UPDATE livestock_animal_losses SET applied_period=? WHERE business_id=? AND applied_period IS NULL")) {s.setLong(1,period);s.setString(2,business.toString());s.executeUpdate();}
    }
}
