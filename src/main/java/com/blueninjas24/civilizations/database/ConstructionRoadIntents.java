package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.sql.*;
import java.util.*;

/** Paid exact road-ground intents. No Paper operations or native placement permission. */
public final class ConstructionRoadIntents {
    private final Connection c;
    public ConstructionRoadIntents(Connection c){this.c=Objects.requireNonNull(c);}
    static void migrate(Connection c)throws SQLException {
        try(var s=c.createStatement()){s.execute("ALTER TABLE construction_block_journal RENAME TO construction_block_journal_v116");}
        ConstructionBlockJournal.createSchema(c,true);
        try(var s=c.createStatement()){
            s.execute("INSERT INTO construction_block_journal SELECT * FROM construction_block_journal_v116");
            s.execute("DROP TABLE construction_block_journal_v116");
            s.execute("CREATE INDEX construction_authored_positions ON construction_block_journal(world_x,world_y,world_z,draft_id)");
            s.execute("""
                CREATE TABLE construction_road_ground (
                  operation_id TEXT PRIMARY KEY NOT NULL REFERENCES construction_block_journal(operation_id) DEFERRABLE INITIALLY DEFERRED,
                  draft_id TEXT NOT NULL REFERENCES construction_drafts(id),
                  ordinal INTEGER NOT NULL CHECK(typeof(ordinal)='integer' AND ordinal BETWEEN 0 AND 63),
                  world_x INTEGER NOT NULL CHECK(typeof(world_x)='integer' AND world_x BETWEEN -2147483648 AND 2147483647),
                  world_y INTEGER NOT NULL CHECK(typeof(world_y)='integer' AND world_y BETWEEN -2147483648 AND 2147483647),
                  world_z INTEGER NOT NULL CHECK(typeof(world_z)='integer' AND world_z BETWEEN -2147483648 AND 2147483647),
                  before_state TEXT NOT NULL CHECK(before_state IN (
                """+String.join(",",RoadGroundReview.naturalGroundStates().stream().sorted().map(v->"'"+v+"'").toList())+")), checked_ms INTEGER NOT NULL CHECK(typeof(checked_ms)='integer' AND checked_ms>=0), UNIQUE(draft_id,ordinal), UNIQUE(draft_id,world_x,world_y,world_z))");
            s.execute("""
                CREATE TRIGGER construction_ground_intent_required BEFORE INSERT ON construction_block_journal
                WHEN NEW.before_state<>'minecraft:air' AND NOT EXISTS(
                  SELECT 1 FROM construction_road_ground g WHERE g.operation_id=NEW.operation_id
                  AND g.draft_id=NEW.draft_id AND g.ordinal=NEW.ordinal AND g.world_x=NEW.world_x
                  AND g.world_y=NEW.world_y AND g.world_z=NEW.world_z AND g.before_state=NEW.before_state)
                BEGIN SELECT RAISE(ABORT,'Road ground intent requires exact paid preparation receipt'); END
                """);
            s.execute("CREATE TRIGGER construction_ground_history_update BEFORE UPDATE ON construction_road_ground BEGIN SELECT RAISE(ABORT,'Road ground history is immutable'); END");
            s.execute("CREATE TRIGGER construction_ground_history_delete BEFORE DELETE ON construction_road_ground BEGIN SELECT RAISE(ABORT,'Road ground history is immutable'); END");
        }
    }
    public ConstructionBlockJournal.Intent prepare(UUID operation,UUID draftId,int ordinal,String nativeAfter,
            LocalRoadDesign.Design design,RoadGroundReview.Observation observation,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(design);Objects.requireNonNull(observation);var present=Set.copyOf(worlds);
        if(!c.getAutoCommit())throw new SQLException("Road intent preparation requires idle SQL owner");c.setAutoCommit(false);
        try{
            var draft=new ConstructionDraftRepository(c).find(draftId).orElseThrow(()->new SQLException("Unknown road draft"));
            requireDesign(draft,design);
            var placements=draft.template().stages().getFirst().blocks().entrySet().stream().toList();
            if(ordinal<0||ordinal>=placements.size()||!placements.get(ordinal).getValue().equals(nativeAfter))throw new SQLException("Road ordinal or exact output differs from frozen design");
            var journal=new ConstructionBlockJournal(c);var old=journal.find(operation);
            if(old.isPresent()){
                var receipt=old.orElseThrow();if(!receipt.draft().equals(draftId)||receipt.ordinal()!=ordinal||!receipt.after().equals(nativeAfter)||"minecraft:air".equals(receipt.before()))throw new SQLException("Road operation identity reused");
                c.commit();return receipt; // Historical receipt only; no fresh native authority.
            }
            if(!present.contains(draft.world()))throw new SQLException("Road world is absent");
            new ConstructionSiteReservations(c).requireActiveRows(draft);
            var footprint=ConstructionPreview.footprint(draft.template(),draft.transform(),draft.origin());var end=design.connection();
            var accessBounds=new ConstructionPreview.Bounds(new Position(Math.min(footprint.minimum().x(),end.x()),draft.origin().y(),Math.min(footprint.minimum().z(),end.z())),
                    new Position(Math.max(footprint.maximum().x(),end.x()),Math.addExact(draft.origin().y(),2),Math.max(footprint.maximum().z(),end.z())));
            if(!new ConstructionLandRepository(c).protectedColumns(draft.world(),accessBounds,draft.id()).isEmpty())throw new SQLException("Road corridor or retained access is newly protected");
            new ConstructionPublicWork(c).requireReadyRows(draft,ordinal);
            var prefix=new HashMap<Position,String>();
            try(var s=c.prepareStatement("SELECT operation_id FROM construction_block_journal WHERE draft_id=? AND state='APPLIED' ORDER BY ordinal")){
                s.setString(1,draftId.toString());try(var r=s.executeQuery()){while(r.next()){var applied=journal.find(UUID.fromString(r.getString(1))).orElseThrow();prefix.put(applied.at(),applied.after());}}
            }
            var review=RoadGroundReview.inspect(draft.world(),design,observation,now,prefix);
            if(!review.suitable())throw new SQLException("Road ground review refused: "+review.issues());
            var at=draft.origin().plus(placements.get(ordinal).getKey());var before=review.beforeStates().get(at);
            if(before==null)throw new SQLException("New road intent has no exact natural before-state");
            try(var s=c.prepareStatement("INSERT INTO construction_road_ground VALUES(?,?,?,?,?,?,?,?)")){
                s.setString(1,operation.toString());s.setString(2,draftId.toString());s.setInt(3,ordinal);s.setInt(4,at.x());s.setInt(5,at.y());s.setInt(6,at.z());s.setString(7,before);s.setLong(8,observation.capturedMillis());s.executeUpdate();
            }
            var intent=journal.prepareRows(operation,draftId,ordinal,nativeAfter,before);c.commit();return intent;
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
    private static void requireDesign(ConstructionDraft draft,LocalRoadDesign.Design design)throws SQLException{
        var t=draft.template();
        if(!t.id().equals("local_road_access")||t.purpose()!=Purpose.PATH||t.size().y()!=1||t.stages().size()!=1||t.blockCount()>64||!draft.transform().equals(Transform.identity())
                ||!draft.origin().equals(design.origin())||!t.fingerprint().equals(design.template().fingerprint())||t.anchors().size()!=1
                ||t.anchors().getFirst().role()!=Role.ROAD||!draft.origin().plus(t.anchors().getFirst().at()).equals(design.connection()))throw new SQLException("Ground replacement requires exact frozen local-road design");
    }
    void requireReceipt(ConstructionBlockJournal.Intent intent,ConstructionDraft draft)throws SQLException{
        var t=draft.template();
        if(t.purpose()!=Purpose.PATH||!t.id().equals("local_road_access")||t.size().y()!=1||t.stages().size()!=1||t.blockCount()>64||t.anchors().size()!=1||!draft.transform().equals(Transform.identity()))throw new SQLException("Ground intent is not a local-road draft");
        try{new LocalRoadDesign.Design(draft.origin(),draft.origin().plus(t.anchors().getFirst().at()),t,t.fingerprint());}
        catch(IllegalArgumentException|IndexOutOfBoundsException invalid){throw new SQLException("Invalid retained road geometry",invalid);}
        try(var s=c.prepareStatement("SELECT 1 FROM construction_road_ground WHERE operation_id=? AND draft_id=? AND ordinal=? AND world_x=? AND world_y=? AND world_z=? AND before_state=?")){
            s.setString(1,intent.operation().toString());s.setString(2,intent.draft().toString());s.setInt(3,intent.ordinal());s.setInt(4,intent.at().x());s.setInt(5,intent.at().y());s.setInt(6,intent.at().z());s.setString(7,intent.before());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Missing exact road-ground preparation receipt");}
        }
    }
}
