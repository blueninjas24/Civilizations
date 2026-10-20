package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.sql.*;
import java.util.*;

/** Durable before/after intent. SQL owner only; no world calls or economic/land authority. */
public final class ConstructionBlockJournal {
    public enum State { PREPARED,APPLIED,CONFLICT }
    public record Intent(UUID operation,UUID draft,int ordinal,Position at,String before,String after,State state){}
    private final Connection connection;
    public ConstructionBlockJournal(Connection connection){this.connection=Objects.requireNonNull(connection);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){s.execute("""
            CREATE TABLE construction_block_journal (
                operation_id TEXT PRIMARY KEY NOT NULL,
                draft_id TEXT NOT NULL REFERENCES construction_drafts(id),
                ordinal INTEGER NOT NULL CHECK(typeof(ordinal)='integer' AND ordinal BETWEEN 0 AND 16383),
                world_x INTEGER NOT NULL CHECK(typeof(world_x)='integer' AND world_x BETWEEN -2147483648 AND 2147483647),
                world_y INTEGER NOT NULL CHECK(typeof(world_y)='integer' AND world_y BETWEEN -2147483648 AND 2147483647),
                world_z INTEGER NOT NULL CHECK(typeof(world_z)='integer' AND world_z BETWEEN -2147483648 AND 2147483647),
                before_state TEXT NOT NULL CHECK(before_state='minecraft:air'),
                after_state TEXT NOT NULL CHECK(length(after_state) BETWEEN 1 AND 256),
                state TEXT NOT NULL CHECK(state IN ('PREPARED','APPLIED','CONFLICT')),
                UNIQUE(draft_id,ordinal), UNIQUE(draft_id,world_x,world_y,world_z)
            )
            """);}
    }
    /** Must be committed BEFORE a future physical writer acts. Earlier intents must be acknowledged. */
    public Intent prepare(UUID operation,UUID draftId,int ordinal,String nativeAfter)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(draftId);Objects.requireNonNull(nativeAfter);
        return transaction(()->{
            var draft=new ConstructionDraftRepository(connection).find(draftId).orElseThrow(()->new SQLException("Unknown construction draft"));
            var placements=new ArrayList<Map.Entry<Position,String>>();draft.template().stages().forEach(stage->placements.addAll(stage.blocks().entrySet()));
            if(ordinal<0||ordinal>=placements.size())throw new IllegalArgumentException("Construction ordinal outside frozen plan");
            var placement=placements.get(ordinal);var at=draft.origin().plus(draft.transform().apply(placement.getKey(),draft.template().size()));
            // Block-state orientation is validated by the Paper adapter. The SQL owner forbids material substitution.
            String material=placement.getValue().split("\\[",2)[0];
            if(!nativeAfter.matches("minecraft:[a-z0-9_]+(?:\\[[a-z0-9_=,]+\\])?")||nativeAfter.length()>256||!material.equals(nativeAfter.split("\\[",2)[0]))
                throw new IllegalArgumentException("Native state changes the frozen plan material");
            var old=find(operation);
            if(old.isPresent()) {
                var value=old.orElseThrow();if(!value.draft().equals(draftId)||value.ordinal()!=ordinal||!value.at().equals(at)||!value.after().equals(nativeAfter))
                    throw new SQLException("Construction operation identity reused with changed terms");
                return value;
            }
            try(var s=connection.prepareStatement("SELECT count(*) FROM construction_block_journal WHERE draft_id=? AND ordinal<? AND state='APPLIED'")){
                s.setString(1,draftId.toString());s.setInt(2,ordinal);try(var r=s.executeQuery()){if(!r.next()||r.getInt(1)!=ordinal)throw new SQLException("Earlier construction intent is incomplete or conflicted");}
            }
            try(var s=connection.prepareStatement("INSERT INTO construction_block_journal VALUES(?,?,?,?,?,?,'minecraft:air',?,'PREPARED')")) {
                s.setString(1,operation.toString());s.setString(2,draftId.toString());s.setInt(3,ordinal);s.setInt(4,at.x());s.setInt(5,at.y());s.setInt(6,at.z());s.setString(7,nativeAfter);s.executeUpdate();
            }
            return new Intent(operation,draftId,ordinal,at,"minecraft:air",nativeAfter,State.PREPARED);
        });
    }
    /** An exact observed after-state is required; a conflict is sticky until a future explicit resolution workflow. */
    public Intent acknowledge(UUID operation,String observed)throws SQLException {
        Objects.requireNonNull(observed);return transaction(()->{
            var old=find(operation).orElseThrow(()->new SQLException("Unknown construction intent"));
            if(old.state()==State.CONFLICT)throw new SQLException("Construction conflict requires explicit review");
            State next=observed.equals(old.after())?State.APPLIED:State.CONFLICT;
            if(old.state()!=next)try(var s=connection.prepareStatement("UPDATE construction_block_journal SET state=? WHERE operation_id=?")) {
                s.setString(1,next.name());s.setString(2,operation.toString());s.executeUpdate();
            }
            return new Intent(old.operation(),old.draft(),old.ordinal(),old.at(),old.before(),old.after(),next);
        });
    }
    public Optional<Intent> find(UUID operation)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM construction_block_journal WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                try{
                    var intent=new Intent(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("draft_id")),r.getInt("ordinal"),
                        new Position(r.getInt("world_x"),r.getInt("world_y"),r.getInt("world_z")),r.getString("before_state"),r.getString("after_state"),State.valueOf(r.getString("state")));
                    var draft=new ConstructionDraftRepository(connection).find(intent.draft()).orElseThrow(()->new SQLException("Construction intent lost its frozen draft"));
                    var placements=new ArrayList<Map.Entry<Position,String>>();draft.template().stages().forEach(stage->placements.addAll(stage.blocks().entrySet()));
                    if(intent.ordinal()<0||intent.ordinal()>=placements.size())throw new SQLException("Construction intent ordinal outside frozen plan");
                    var placement=placements.get(intent.ordinal());
                    if(!intent.at().equals(draft.origin().plus(draft.transform().apply(placement.getKey(),draft.template().size())))
                            ||!"minecraft:air".equals(intent.before())||intent.after()==null||intent.after().length()>256
                            ||!intent.after().matches("minecraft:[a-z0-9_]+(?:\\[[a-z0-9_=,]+\\])?")
                            ||!placement.getValue().split("\\[",2)[0].equals(intent.after().split("\\[",2)[0]))
                        throw new SQLException("Construction intent differs from frozen geometry or material");
                    return Optional.of(intent);
                }
                catch(IllegalArgumentException invalid){throw new SQLException("Invalid construction intent",invalid);}
            }
        }
    }
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Construction journal requires no active owner transaction");connection.setAutoCommit(false);
        try{T result=work.run();connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
