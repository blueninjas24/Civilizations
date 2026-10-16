package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** Durable live-capture fence. Clean shutdown never repairs missing history or unknown legacy herds. */
public final class LivestockCapture {
    public record State(boolean cleanStop,boolean trusted,long observedMillis) {}
    private final Connection connection;
    public LivestockCapture(Connection connection){this.connection=Objects.requireNonNull(connection);}
    static void createSchema(Connection connection)throws SQLException {
        try(var s=connection.createStatement()){s.execute("""
            CREATE TABLE livestock_capture (
                id INTEGER PRIMARY KEY CHECK(id=1),clean_stop INTEGER NOT NULL CHECK(clean_stop IN(0,1)),
                trusted INTEGER NOT NULL CHECK(trusted IN(0,1)),
                observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0)
            )
            """);}
    }
    public Optional<State> load()throws SQLException {
        try(var s=connection.createStatement();var r=s.executeQuery("SELECT clean_stop,trusted,observed_millis FROM livestock_capture WHERE id=1")) {
            return r.next()?Optional.of(new State(r.getInt(1)==1,r.getInt(2)==1,r.getLong(3))):Optional.empty();
        }
    }
    public State begin(long now)throws SQLException{return change(now,true,true,false);}
    public State finish(long now,boolean drained)throws SQLException{return change(now,false,drained,true);}
    public State markGap(long now)throws SQLException{return change(now,false,false,false);}
    private State change(long now,boolean begin,boolean healthy,boolean clean)throws SQLException {
        if(now<0)throw new IllegalArgumentException("Invalid livestock capture timestamp");
        if(!connection.getAutoCommit())throw new SQLException("Capture fence requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var old=load();if(!begin&&old.isEmpty())throw new SQLException("Livestock capture has not begun");
            boolean trusted;
            if(old.isPresent()){var previous=old.orElseThrow();trusted=previous.trusted()&&(!begin||previous.cleanStop());}
            else {try(var s=connection.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM livestock_herds")){if(!r.next())throw new SQLException("Herd census unavailable");trusted=r.getLong(1)==0;}}
            var next=new State(clean,trusted&&healthy,now);
            try(var s=connection.prepareStatement("INSERT INTO livestock_capture VALUES(1,?,?,?) ON CONFLICT(id) DO UPDATE SET clean_stop=excluded.clean_stop,trusted=excluded.trusted,observed_millis=excluded.observed_millis")) {
                s.setInt(1,next.cleanStop()?1:0);s.setInt(2,next.trusted()?1:0);s.setLong(3,now);s.executeUpdate();
            }
            connection.commit();connection.setAutoCommit(true);return next;
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
