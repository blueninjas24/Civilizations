package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** Clean-stop fence: a crash or capture failure never silently certifies complete placement history. */
public final class OreTrackingRepository {
    public record State(boolean cleanStop, boolean trusted, long observedMillis) {}
    private final Connection connection;
    public OreTrackingRepository(Connection connection) { this.connection=Objects.requireNonNull(connection); }
    public Optional<State> load() throws SQLException {
        try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT clean_stop,trusted,observed_millis FROM ore_tracking WHERE id=1")) {
            return result.next()?Optional.of(new State(result.getInt(1)==1,result.getInt(2)==1,result.getLong(3))):Optional.empty();
        }
    }
    /** Before live capture, persist a dirty fence. An unclean prior stop leaves trust false until reviewed. */
    public State begin(long now) throws SQLException {
        return change(now,true,true,false);
    }
    /** Only after accepted placement work drains. Healthy shutdown cannot repair a previous gap. */
    public State finish(long now,boolean healthy) throws SQLException {
        return change(now,false,healthy,true);
    }
    public State markGap(long now) throws SQLException {return change(now,false,false,false);}
    private State change(long now,boolean begin,boolean healthy,boolean cleanStop) throws SQLException {
        if(now<0)throw new IllegalArgumentException("Invalid tracking timestamp");
        if(!connection.getAutoCommit())throw new SQLException("Tracking transition requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var previous=load();
            if(!begin && previous.isEmpty())throw new SQLException("Tracking has not begun");
            ResourceRecoveryEpochs.transitionRows(connection,"ore",begin,
                    previous.map(State::cleanStop).orElse(false),!begin&&!healthy);
            boolean trusted=previous.map(s->s.trusted() && (!begin || s.cleanStop())).orElse(true) && healthy;
            var state=new State(cleanStop,trusted,now);
            try(var statement=connection.prepareStatement("""
                    INSERT INTO ore_tracking VALUES(1,?,?,?)
                    ON CONFLICT(id) DO UPDATE SET clean_stop=excluded.clean_stop,trusted=excluded.trusted,observed_millis=excluded.observed_millis
                    """)) {
                statement.setInt(1,state.cleanStop()?1:0);statement.setInt(2,state.trusted()?1:0);statement.setLong(3,now);statement.executeUpdate();
            }
            connection.commit();connection.setAutoCommit(true);return state;
        } catch(SQLException|RuntimeException failure) {
            try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;
        }
    }
}
