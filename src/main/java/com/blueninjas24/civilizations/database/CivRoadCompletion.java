package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Permanent provenance for fully paid, acknowledged and freshly inspected local paving. */
public final class CivRoadCompletion {
    public record Receipt(UUID operation,UUID draft,UUID road,UUID village,UUID world,String planHash,long completedMillis){}
    private final Connection c;
    public CivRoadCompletion(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){
            s.execute("""
                CREATE TABLE civ_roads (
                  id TEXT PRIMARY KEY NOT NULL,draft_id TEXT UNIQUE NOT NULL REFERENCES construction_drafts(id),
                  operation_id TEXT UNIQUE NOT NULL,settlement_id TEXT NOT NULL,world_id TEXT NOT NULL,
                  plan_hash TEXT NOT NULL CHECK(length(plan_hash)=64),
                  completed_ms INTEGER NOT NULL CHECK(typeof(completed_ms)='integer' AND completed_ms>=0),
                  UNIQUE(id,world_id),FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
                )
                """);
            s.execute("""
                CREATE TABLE civ_road_cells (
                  road_id TEXT NOT NULL,world_id TEXT NOT NULL,
                  world_x INTEGER NOT NULL CHECK(typeof(world_x)='integer' AND world_x BETWEEN -2147483648 AND 2147483647),
                  world_y INTEGER NOT NULL CHECK(typeof(world_y)='integer' AND world_y BETWEEN -2147483648 AND 2147483647),
                  world_z INTEGER NOT NULL CHECK(typeof(world_z)='integer' AND world_z BETWEEN -2147483648 AND 2147483647),
                  after_state TEXT NOT NULL CHECK(after_state IN('minecraft:cobblestone','minecraft:stone','minecraft:stone_bricks','minecraft:andesite','minecraft:diorite','minecraft:granite','minecraft:cobbled_deepslate')),
                  operation_id TEXT UNIQUE NOT NULL REFERENCES construction_block_journal(operation_id),
                  PRIMARY KEY(world_id,world_x,world_y,world_z),
                  FOREIGN KEY(road_id,world_id) REFERENCES civ_roads(id,world_id)
                )
                """);
            s.execute("CREATE INDEX civ_road_local_cells ON civ_road_cells(world_id,world_x,world_z,world_y)");
            for(var table:List.of("civ_roads","civ_road_cells"))for(var action:List.of("UPDATE","DELETE"))
                s.execute("CREATE TRIGGER "+table+"_history_"+action.toLowerCase(Locale.ROOT)+" BEFORE "+action+" ON "+table+" BEGIN SELECT RAISE(ABORT,'Completed road history is immutable'); END");
        }
    }
    public Optional<Receipt> findDraft(UUID id)throws SQLException {
        if(!available())return Optional.empty();
        try(var s=c.prepareStatement("SELECT * FROM civ_roads WHERE draft_id=?")){
            s.setString(1,id.toString());try(var r=s.executeQuery()){
                if(!r.next())return Optional.empty();
                var draft=new ConstructionDraftRepository(c).find(id).orElseThrow(()->new SQLException("Completed road lost its frozen draft"));
                var receipt=new Receipt(UUID.fromString(r.getString("operation_id")),id,UUID.fromString(r.getString("id")),UUID.fromString(r.getString("settlement_id")),UUID.fromString(r.getString("world_id")),r.getString("plan_hash"),r.getLong("completed_ms"));
                if(!receipt.road().equals(identity(draft))||!receipt.world().equals(draft.world())||!receipt.village().equals(draft.settlement())||!receipt.planHash().equals(draft.template().fingerprint())||receipt.completedMillis()<draft.createdMillis())
                    throw new SQLException("Completed road differs from its permanent origin");
                geometry(draft);return Optional.of(receipt);
            }
        }
    }
    /** Expected provenance only. Callers must still match fresh physical states and current protection. */
    public Map<Position,String> expectedStates(UUID world,ConstructionPreview.Bounds bounds)throws SQLException {
        Objects.requireNonNull(world);Objects.requireNonNull(bounds);
        if((long)bounds.maximum().x()-bounds.minimum().x()>=96||(long)bounds.maximum().z()-bounds.minimum().z()>=96)throw new IllegalArgumentException("Road lookup exceeds local review bounds");
        if(!available())return Map.of();
        var result=new TreeMap<Position,String>();var drafts=new HashMap<UUID,ConstructionDraft>();
        try(var s=c.prepareStatement("SELECT cells.*,r.draft_id FROM civ_road_cells cells JOIN civ_roads r ON r.id=cells.road_id AND r.world_id=cells.world_id WHERE cells.world_id=? AND world_x BETWEEN ? AND ? AND world_y BETWEEN ? AND ? AND world_z BETWEEN ? AND ? ORDER BY world_x,world_y,world_z LIMIT 4097")){
            s.setString(1,world.toString());s.setInt(2,bounds.minimum().x());s.setInt(3,bounds.maximum().x());s.setInt(4,bounds.minimum().y());s.setInt(5,bounds.maximum().y());s.setInt(6,bounds.minimum().z());s.setInt(7,bounds.maximum().z());
            try(var r=s.executeQuery()){while(r.next()){
                if(result.size()>=4096)throw new SQLException("Road provenance exceeds local cell budget");
                var id=UUID.fromString(r.getString("draft_id"));var draft=drafts.get(id);
                if(draft==null){if(drafts.size()>=128)throw new SQLException("Road provenance exceeds local project budget");findDraft(id).orElseThrow(()->new SQLException("Missing road origin"));draft=new ConstructionDraftRepository(c).find(id).orElseThrow();drafts.put(id,draft);}
                var at=new Position(r.getInt("world_x"),r.getInt("world_y"),r.getInt("world_z"));var state=r.getString("after_state");
                var offset=new Position(Math.subtractExact(at.x(),draft.origin().x()),Math.subtractExact(at.y(),draft.origin().y()),Math.subtractExact(at.z(),draft.origin().z()));
                var intent=new ConstructionBlockJournal(c).find(UUID.fromString(r.getString("operation_id"))).orElseThrow(()->new SQLException("Missing completed road journal"));
                if(!draft.world().equals(world)||!identity(draft).toString().equals(r.getString("road_id"))||!state.equals(draft.template().stages().getFirst().blocks().get(offset))||!intent.draft().equals(id)||!intent.at().equals(at)||!state.equals(intent.after())||intent.state()!=ConstructionBlockJournal.State.APPLIED)
                    throw new SQLException("Road cell differs from its permanent paid origin");
                result.put(at,state);
            }}
        }
        return Collections.unmodifiableMap(result);
    }
    private boolean available()throws SQLException{try(var s=c.createStatement();var r=s.executeQuery("PRAGMA user_version")){return r.getInt(1)>=118;}}
    /** Retained road history is not vacant building land when its visible surface is removed. */
    boolean occupiesProjectGround(ConstructionDraft draft)throws SQLException {
        var bounds=ConstructionPreview.footprint(draft.template(),draft.transform(),draft.origin());
        var ground=new ConstructionPreview.Bounds(new Position(bounds.minimum().x(),Math.subtractExact(bounds.minimum().y(),1),bounds.minimum().z()),bounds.maximum());
        var saved=expectedStates(draft.world(),ground);if(saved.isEmpty())return false;
        if(draft.template().purpose()!=ConstructionTemplate.Purpose.PATH)return true;
        for(var stage:draft.template().stages())for(var at:stage.blocks().keySet())if(saved.containsKey(draft.origin().plus(draft.transform().apply(at,draft.template().size()))))return true;
        return false;
    }
    public Receipt complete(UUID operation,UUID draftId,RoadGroundReview.Observation observation,int buffer,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(observation);var present=Set.copyOf(worlds);
        if(now<0)throw new IllegalArgumentException("Negative road completion time");
        if(!c.getAutoCommit())throw new SQLException("Road completion requires idle SQL owner");c.setAutoCommit(false);
        try{
            var draft=new ConstructionDraftRepository(c).find(draftId).orElseThrow(()->new SQLException("Unknown completed road"));
            var design=geometry(draft);var old=findDraft(draftId);
            if(old.isPresent()){
                if(!old.get().operation().equals(operation))throw new SQLException("Road completion operation changed");
                c.commit();return old.get(); // Historical receipt only; never repairs or renews native authority.
            }
            if(!present.contains(draft.world())||now<draft.createdMillis())throw new SQLException("Completed road world must be present at a valid time");
            var sites=new ConstructionSiteReservations(c);sites.requireActiveRows(draft);
            if(new ConstructionWorkQueue(c).reviewRows(draftId).state()!=ConstructionWorkQueue.State.COMPLETE)throw new SQLException("Road waits for all paid work and acknowledged output");
            var required=RoadGroundReview.requiredCells(design,buffer);
            var min=new Position(required.stream().mapToInt(Position::x).min().orElseThrow(),draft.origin().y(),required.stream().mapToInt(Position::z).min().orElseThrow());
            var max=new Position(required.stream().mapToInt(Position::x).max().orElseThrow(),Math.addExact(draft.origin().y(),2),required.stream().mapToInt(Position::z).max().orElseThrow());
            if(!new ConstructionLandRepository(c).protectedColumns(draft.world(),new ConstructionPreview.Bounds(min,max),draftId).isEmpty())throw new SQLException("Completed road clearance or access is newly protected");
            var intents=new ArrayList<ConstructionBlockJournal.Intent>();var states=new TreeMap<Position,String>();
            try(var s=c.prepareStatement("SELECT operation_id FROM construction_block_journal WHERE draft_id=? ORDER BY ordinal")){
                s.setString(1,draftId.toString());try(var r=s.executeQuery()){while(r.next()){
                    if(intents.size()>=64)throw new SQLException("Completed road exceeds bounded journal");
                    var intent=new ConstructionBlockJournal(c).find(UUID.fromString(r.getString(1))).orElseThrow();
                    if(intent.ordinal()!=intents.size()||intent.state()!=ConstructionBlockJournal.State.APPLIED||!RoadGroundReview.naturalGroundStates().contains(intent.before()))throw new SQLException("Road completion requires an exact paid ground journal");
                    intents.add(intent);states.put(intent.at(),intent.after());
                }}
            }
            var registered=RoadGroundReview.registeredWithin(expectedStates(draft.world(),new ConstructionPreview.Bounds(min,max)),required);
            var authoritative=new RoadGroundReview.Observation(observation.world(),observation.capturedMillis(),observation.complete(),observation.states(),observation.protectedCells(),registered);
            if(intents.size()!=draft.template().blockCount()||!RoadGroundReview.inspect(draft.world(),design,authoritative,now,states,buffer).suitable())throw new SQLException("Completed road native inspection differs from its acknowledged plan");
            var road=identity(draft);
            try(var s=c.prepareStatement("INSERT INTO civ_roads VALUES(?,?,?,?,?,?,?)")){
                s.setString(1,road.toString());s.setString(2,draftId.toString());s.setString(3,operation.toString());s.setString(4,draft.settlement().toString());s.setString(5,draft.world().toString());s.setString(6,draft.template().fingerprint());s.setLong(7,now);s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO civ_road_cells VALUES(?,?,?,?,?,?,?)")){
                for(var intent:intents){s.setString(1,road.toString());s.setString(2,draft.world().toString());s.setInt(3,intent.at().x());s.setInt(4,intent.at().y());s.setInt(5,intent.at().z());s.setString(6,intent.after());s.setString(7,intent.operation().toString());s.executeUpdate();}
            }
            try(var s=c.prepareStatement("UPDATE construction_site_reservations SET released_ms=? WHERE draft_id=? AND released_ms IS NULL")){
                s.setLong(1,now);s.setString(2,draftId.toString());if(s.executeUpdate()!=1)throw new SQLException("Completed road site changed");
            }
            var result=findDraft(draftId).orElseThrow();c.commit();return result;
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
    private static UUID identity(ConstructionDraft draft){return UUID.nameUUIDFromBytes(("civilizations:road:v1:"+draft.world()+":"+draft.id()).getBytes(StandardCharsets.UTF_8));}
    private static LocalRoadDesign.Geometry geometry(ConstructionDraft draft)throws SQLException {
        try{return LocalRoadDesign.geometry(draft);}catch(IllegalArgumentException invalid){throw new SQLException("Completion requires exact frozen local-road geometry",invalid);}
    }
}
