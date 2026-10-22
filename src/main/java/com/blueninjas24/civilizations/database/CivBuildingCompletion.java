package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Records a fully paid, acknowledged, currently verified Civ home. Habitability/occupancy remain separate. */
public final class CivBuildingCompletion {
    public record Evidence(ConstructionMaterials.Authority authority,Map<Position,String> cells){
        public Evidence{Objects.requireNonNull(authority);if(cells.size()>ConstructionPreview.MAX_OBSERVATION_CELLS)throw new IllegalArgumentException("Completion evidence exceeds native cell bounds");cells=Map.copyOf(cells);}
    }
    public record Receipt(UUID operation,UUID draft,UUID building,UUID village,UUID world,String planHash,String layoutHash,UUID useConsent,long completedMillis){}
    private final Connection c;
    public CivBuildingCompletion(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException{
        try(var s=c.createStatement()){s.execute("""
            CREATE TABLE civ_built_buildings (
              building_id TEXT PRIMARY KEY NOT NULL REFERENCES building_records(id),
              draft_id TEXT UNIQUE NOT NULL REFERENCES construction_drafts(id),operation_id TEXT UNIQUE NOT NULL,
              settlement_id TEXT NOT NULL,world_id TEXT NOT NULL,
              plan_hash TEXT NOT NULL CHECK(length(plan_hash)=64),layout_hash TEXT NOT NULL CHECK(length(layout_hash)=64),
              use_consent TEXT UNIQUE NOT NULL REFERENCES building_consent_events(operation_id),
              completed_ms INTEGER NOT NULL CHECK(typeof(completed_ms)='integer' AND completed_ms>=0),
              FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
            )
            """);
            s.execute("ALTER TABLE housing_project_bindings ADD COLUMN completed_building TEXT REFERENCES building_records(id)");
            s.execute("ALTER TABLE housing_project_bindings ADD COLUMN completed_ms INTEGER CHECK((completed_ms IS NULL AND completed_building IS NULL) OR (typeof(completed_ms)='integer' AND completed_ms>=created_ms AND completed_building IS NOT NULL AND cancelled_ms IS NULL))");
            s.execute("DROP INDEX one_active_home_project_per_household");
            s.execute("CREATE UNIQUE INDEX one_active_home_project_per_household ON housing_project_bindings(household_id) WHERE cancelled_ms IS NULL AND completed_ms IS NULL");
            s.execute("CREATE UNIQUE INDEX one_project_per_completed_home ON housing_project_bindings(completed_building) WHERE completed_building IS NOT NULL");
            s.execute("CREATE INDEX construction_authored_positions ON construction_block_journal(world_x,world_y,world_z,draft_id)");}
        // Existing authored bed intents are positive provenance, not guessed natural/player houses.
        try(var s=c.createStatement();var r=s.executeQuery("SELECT draft_id,world_x,world_y,world_z,after_state FROM construction_block_journal WHERE after_state LIKE 'minecraft:%bed[%' ORDER BY draft_id,ordinal")){
            UUID previous=null;ConstructionDraft draft=null;
            while(r.next()){
                if(!r.getString(5).split("\\[",2)[0].endsWith("_bed"))continue;
                UUID id=UUID.fromString(r.getString(1));if(!id.equals(previous)){draft=new ConstructionDraftRepository(c).find(id).orElseThrow();previous=id;}
                ConstructionBlockJournal.bindAuthoredBedHead(c,draft,new Position(r.getInt(2),r.getInt(3),r.getInt(4)));
            }
        }
    }
    static boolean schemaAvailable(Connection c)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery("PRAGMA user_version")){return r.getInt(1)>=116;}}
    /** Raw origin view used by the registry without recursive registry lookup. Village contributor is not a player donor. */
    static boolean villageOwned(Connection c,BuildingLayout layout,UUID contributor)throws SQLException{
        if(!schemaAvailable(c))return false;
        try(var s=c.prepareStatement("SELECT b.*,d.template_hash,d.world_id AS draft_world,d.settlement_id AS draft_village,e.building_id AS use_building,e.actor_id AS use_actor,e.action AS use_action,e.at_ms AS use_time,e.administrator AS use_admin FROM civ_built_buildings b LEFT JOIN construction_drafts d ON d.id=b.draft_id LEFT JOIN building_consent_events e ON e.operation_id=b.use_consent WHERE b.building_id=?")){
            s.setString(1,layout.id().toString());try(var r=s.executeQuery()){
                if(!r.next())return false;
                if(!layout.settlement().equals(contributor)||!contributor.toString().equals(r.getString("settlement_id"))||!contributor.toString().equals(r.getString("draft_village"))
                        ||!layout.world().toString().equals(r.getString("world_id"))||!layout.world().toString().equals(r.getString("draft_world"))
                        ||!layout.fingerprint().equals(r.getString("layout_hash"))||!r.getString("plan_hash").equals(r.getString("template_hash"))
                        ||!layout.id().toString().equals(r.getString("use_building"))||!contributor.toString().equals(r.getString("use_actor"))||!"DONATE".equals(r.getString("use_action"))
                        ||r.getBoolean("use_admin")||r.getLong("use_time")!=r.getLong("completed_ms"))throw new SQLException("Civ building origin differs from frozen village/world/plan/layout");
                return true;
            }
        }
    }
    public Optional<Receipt> findDraft(UUID draft)throws SQLException{
        if(!schemaAvailable(c))return Optional.empty();
        try(var s=c.prepareStatement("SELECT * FROM civ_built_buildings WHERE draft_id=?")){
            s.setString(1,draft.toString());try(var r=s.executeQuery()){if(!r.next())return Optional.empty();
                return Optional.of(new Receipt(UUID.fromString(r.getString("operation_id")),draft,UUID.fromString(r.getString("building_id")),UUID.fromString(r.getString("settlement_id")),
                        UUID.fromString(r.getString("world_id")),r.getString("plan_hash"),r.getString("layout_hash"),UUID.fromString(r.getString("use_consent")),r.getLong("completed_ms")));
            }
        }
    }
    public Receipt completeHome(UUID operation,UUID draftId,Evidence evidence,long now,Set<UUID> worlds)throws SQLException{
        Objects.requireNonNull(operation);Objects.requireNonNull(draftId);Objects.requireNonNull(evidence);var present=Set.copyOf(worlds);
        if(now<0)throw new IllegalArgumentException("Negative completion time");if(!c.getAutoCommit())throw new SQLException("Civ completion requires idle SQL owner");c.setAutoCommit(false);
        try{
            var draft=new ConstructionDraftRepository(c).find(draftId).orElseThrow(()->new BuildingRegistry.Declined("Unknown completed-home proposal"));
            if(!present.contains(draft.world()))throw new BuildingRegistry.Declined("Completed home world must be present");
            if(draft.template().purpose()!=ConstructionTemplate.Purpose.HOME)throw new BuildingRegistry.Declined("First completion prototype accepts public homes only");
            var layout=CivHomeLayout.from(draft);var old=findDraft(draftId);
            if(old.isPresent()){
                var saved=old.get();if(!saved.operation().equals(operation)||!saved.building().equals(layout.id())||!saved.layoutHash().equals(layout.fingerprint())||!saved.planHash().equals(draft.template().fingerprint()))throw new BuildingRegistry.Declined("Completion identity or frozen layout changed");
                var entry=new BuildingRegistry(c).find(saved.building()).orElseThrow();if(!entry.villageOwned())throw new SQLException("Completed home lost its village origin");c.commit();return saved;
            }
            var authority=evidence.authority();
            if(!authority.draft().equals(draftId)||!authority.world().equals(draft.world())||!authority.worldLoaded()||!authority.siteCleared()||authority.checkedMillis()>now||now-authority.checkedMillis()>5000||now<draft.createdMillis())throw new BuildingRegistry.Declined("Fresh complete exact-home native evidence required");
            if(new ConstructionWorkQueue(c).reviewRows(draftId).state()!=ConstructionWorkQueue.State.COMPLETE)throw new BuildingRegistry.Declined("Home waits for all paid stage work and acknowledged native blocks");
            var required=ConstructionPreview.requiredCells(draft.template(),draft.transform(),draft.origin());
            if(!evidence.cells().keySet().equals(required))throw new BuildingRegistry.Declined("Final native coverage differs from frozen home");
            int count=0;try(var s=c.prepareStatement("SELECT world_x,world_y,world_z,after_state,state FROM construction_block_journal WHERE draft_id=? ORDER BY ordinal LIMIT 16385")){
                s.setString(1,draftId.toString());try(var r=s.executeQuery()){while(r.next()){
                    var at=new Position(r.getInt(1),r.getInt(2),r.getInt(3));if(!r.getString(5).equals("APPLIED")||!r.getString(4).equals(evidence.cells().get(at)))throw new BuildingRegistry.Declined("Final native home differs from acknowledged output");count++;
                }}
            }
            if(count!=draft.template().blockCount())throw new BuildingRegistry.Declined("Incomplete native home journal");
            var registry=new BuildingRegistry(c);if(registry.find(layout.id()).isPresent())throw new BuildingRegistry.Declined("Completed home building identity already belongs to another record");
            var site=new ConstructionSiteReservations(c).find(draftId).orElseThrow();
            if(!site.active()||new ConstructionSiteReservations(c).hasProtectedRequiredColumns(draft,draftId))throw new BuildingRegistry.Declined("Completed home land/access authority changed");
            // Legacy donor_id is the typed contributor slot. Origin metadata below makes this a village, never a player declaration.
            registry.insertRows(layout,draft.settlement(),now);UUID use=UUID.nameUUIDFromBytes(("civ-built-use-v1|"+draftId).getBytes(StandardCharsets.UTF_8));
            registry.appendRows(new BuildingRegistry.Consent(use,layout.id(),draft.settlement(),BuildingRegistry.Action.DONATE,null,false,now));
            try(var s=c.prepareStatement("INSERT INTO civ_built_buildings VALUES(?,?,?,?,?,?,?,?,?)")){
                s.setString(1,layout.id().toString());s.setString(2,draftId.toString());s.setString(3,operation.toString());s.setString(4,draft.settlement().toString());s.setString(5,draft.world().toString());s.setString(6,draft.template().fingerprint());s.setString(7,layout.fingerprint());s.setString(8,use.toString());s.setLong(9,now);s.executeUpdate();
            }
            try(var s=c.prepareStatement("UPDATE housing_project_bindings SET completed_building=?,completed_ms=? WHERE draft_id=? AND cancelled_ms IS NULL AND completed_ms IS NULL")){
                s.setString(1,layout.id().toString());s.setLong(2,now);s.setString(3,draftId.toString());
                int changed=s.executeUpdate();if(changed==0&&new HousingProjects(c).find(draftId).isPresent())throw new SQLException("Home beneficiary completion changed");
            }
            try(var s=c.prepareStatement("UPDATE construction_site_reservations SET released_ms=? WHERE draft_id=? AND released_ms IS NULL")){
                s.setLong(1,now);s.setString(2,draftId.toString());if(s.executeUpdate()!=1)throw new SQLException("Completed home site changed");
            }
            var saved=findDraft(draftId).orElseThrow();c.commit();return saved;
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
}
