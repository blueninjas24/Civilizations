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
    static void createSchema(Connection c)throws SQLException {createSchema(c,false);}
    static void createSchema(Connection c,boolean roadGround)throws SQLException {
        String ddl="""
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
            """;
        if(roadGround)ddl=ddl.replace("CHECK(before_state='minecraft:air')","CHECK(before_state IN ('minecraft:air',"+String.join(",",RoadGroundReview.naturalGroundStates().stream().sorted().map(v->"'"+v+"'").toList())+"))");
        try(var s=c.createStatement()){s.execute(ddl);}
    }
    /** Must be committed BEFORE a future physical writer acts. Earlier intents must be acknowledged. */
    public Intent prepare(UUID operation,UUID draftId,int ordinal,String nativeAfter)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(draftId);Objects.requireNonNull(nativeAfter);
        return transaction(()->prepareRows(operation,draftId,ordinal,nativeAfter));
    }
    /** Economic preparation for a future public writer, not world-write permission or building activation. */
    public Intent preparePublic(UUID operation,UUID draftId,int ordinal,String nativeAfter,
                                ConstructionMaterials.Authority authority,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(authority);var present=Set.copyOf(worlds);
        return transaction(()->{
            var draft=new ConstructionDraftRepository(connection).find(draftId).orElseThrow(()->new SQLException("Unknown public project"));
            if(!present.contains(draft.world())||!authority.draft().equals(draftId)||!authority.world().equals(draft.world())
                    ||!authority.worldLoaded()||!authority.siteCleared()||now<authority.checkedMillis()||now-authority.checkedMillis()>5000)
                throw new SQLException("Public construction requires fresh exact-project site evidence and a present world");
            new ConstructionSiteReservations(connection).requireActiveRows(draft);
            new ConstructionPublicWork(connection).requireReadyRows(draft,ordinal);
            return prepareRows(operation,draftId,ordinal,nativeAfter);
        });
    }
    private Intent prepareRows(UUID operation,UUID draftId,int ordinal,String nativeAfter)throws SQLException {return prepareRows(operation,draftId,ordinal,nativeAfter,"minecraft:air");}
    Intent prepareRows(UUID operation,UUID draftId,int ordinal,String nativeAfter,String before)throws SQLException {
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
            var value=old.orElseThrow();if(!value.draft().equals(draftId)||value.ordinal()!=ordinal||!value.at().equals(at)||!value.after().equals(nativeAfter)||!value.before().equals(before))
                throw new SQLException("Construction operation identity reused with changed terms");
            excludeAuthoredTrees(draft,at,material);
            if(material.endsWith("_bed"))bindAuthoredBedHead(connection,draft,at);
            return value;
        }
        try(var s=connection.prepareStatement("SELECT count(*) FROM construction_block_journal WHERE draft_id=? AND ordinal<? AND state='APPLIED'")){
            s.setString(1,draftId.toString());s.setInt(2,ordinal);try(var r=s.executeQuery()){if(!r.next()||r.getInt(1)!=ordinal)throw new SQLException("Earlier construction intent is incomplete or conflicted");}
        }
        excludeAuthoredTrees(draft,at,material);
        if(material.endsWith("_bed"))bindAuthoredBedHead(connection,draft,at);
        try(var s=connection.prepareStatement("INSERT INTO construction_block_journal VALUES(?,?,?,?,?,?,?,?,'PREPARED')")) {
            s.setString(1,operation.toString());s.setString(2,draftId.toString());s.setInt(3,ordinal);s.setInt(4,at.x());s.setInt(5,at.y());s.setInt(6,at.z());s.setString(7,before);s.setString(8,nativeAfter);s.executeUpdate();
        }
        return new Intent(operation,draftId,ordinal,at,before,nativeAfter,State.PREPARED);
    }
    /** Native plugin writes do not emit player placement events: authored logs must never become natural timber sources. */
    private void excludeAuthoredTrees(ConstructionDraft draft,Position at,String material)throws SQLException {
        if(!material.endsWith("_log")&&!material.endsWith("_wood")&&!material.endsWith("_stem")&&!material.endsWith("_hyphae"))return;
        try(var s=connection.prepareStatement("INSERT INTO forestry_exclusions VALUES(?,?,?,?,?,?) ON CONFLICT(world_id,x,y,z) DO NOTHING")) {
            s.setString(1,draft.world().toString());s.setInt(2,at.x());s.setInt(3,at.y());s.setInt(4,at.z());s.setLong(5,draft.createdMillis());
            s.setString(6,"Authored construction intent "+draft.id());s.executeUpdate();
        }
    }
    /** Before native head placement, save its actual village identity; it must never create a duplicate town. */
    static void bindAuthoredBedHead(Connection c,ConstructionDraft draft,Position at)throws SQLException{
        if(draft.template().purpose()!=ConstructionTemplate.Purpose.HOME)return;
        for(var unit:CivHomeLayout.from(draft).units())for(var bed:unit.beds())if(bed.head().equals(at))
            new VillageIdentityRepository(c).bindBed(draft.world(),new com.blueninjas24.civilizations.settlement.VillageObservation.Position(at.x(),at.y(),at.z()),draft.settlement());
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
                            ||!("minecraft:air".equals(intent.before())||RoadGroundReview.naturalGroundStates().contains(intent.before()))||intent.after()==null||intent.after().length()>256
                            ||!intent.after().matches("minecraft:[a-z0-9_]+(?:\\[[a-z0-9_=,]+\\])?")
                            ||!placement.getValue().split("\\[",2)[0].equals(intent.after().split("\\[",2)[0]))
                        throw new SQLException("Construction intent differs from frozen geometry or material");
                    if(!"minecraft:air".equals(intent.before()))new ConstructionRoadIntents(connection).requireReceipt(intent,draft);
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
