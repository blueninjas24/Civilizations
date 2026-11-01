package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.sql.*;
import java.util.*;

/** Retained road -> frozen future home -> existing household binding. No land, spending or native authority. */
public final class HousingRoadProjects {
    public record Link(UUID roadDraft,UUID homeDraft,long createdMillis){}
    private final Connection c;
    public HousingRoadProjects(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException{
        try(var s=c.createStatement()){
            s.execute("""
                CREATE TABLE housing_road_projects (
                  road_draft TEXT PRIMARY KEY NOT NULL REFERENCES construction_drafts(id),
                  home_draft TEXT UNIQUE NOT NULL REFERENCES housing_project_bindings(draft_id),
                  created_ms INTEGER NOT NULL CHECK(typeof(created_ms)='integer' AND created_ms>=0),
                  CHECK(road_draft<>home_draft)
                )
                """);
            for(var action:List.of("UPDATE","DELETE"))s.execute("CREATE TRIGGER housing_road_history_"+action.toLowerCase(Locale.ROOT)+" BEFORE "+action+" ON housing_road_projects BEGIN SELECT RAISE(ABORT,'Housing road purpose history is immutable'); END");
        }
    }
    private boolean available()throws SQLException{try(var s=c.createStatement();var r=s.executeQuery("PRAGMA user_version")){return r.getInt(1)>=119;}}
    public Optional<Link> findRoad(UUID road)throws SQLException{
        if(!available())return Optional.empty();
        try(var s=c.prepareStatement("SELECT * FROM housing_road_projects WHERE road_draft=?")){
            s.setString(1,road.toString());try(var r=s.executeQuery()){
                if(!r.next())return Optional.empty();var link=new Link(road,UUID.fromString(r.getString("home_draft")),r.getLong("created_ms"));
                validate(link);return Optional.of(link);
            }
        }
    }
    public Optional<Link> findHome(UUID home)throws SQLException{
        if(!available())return Optional.empty();
        try(var s=c.prepareStatement("SELECT road_draft FROM housing_road_projects WHERE home_draft=?")){
            s.setString(1,home.toString());try(var r=s.executeQuery()){return r.next()?findRoad(UUID.fromString(r.getString(1))):Optional.empty();}
        }
    }
    /** Both immutable drafts must exist, but remain unused. The household reservation and link commit together. */
    public Link bind(UUID road,HousingProjects.Proposal home,DwellingOccupancy.Context context,HousingProjectPlanner planner,long seed,long period)throws SQLException{
        if(!c.getAutoCommit())throw new SQLException("Housing road purpose needs idle SQL owner");c.setAutoCommit(false);
        try{var saved=bindRows(road,home,context,planner,seed,period);c.commit();return saved;
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
    Link bindRows(UUID road,HousingProjects.Proposal home,DwellingOccupancy.Context context,HousingProjectPlanner planner,long seed,long period)throws SQLException{
            if(c.getAutoCommit())throw new SQLException("Road binding requires SQL owner transaction");
            var old=findRoad(road);
            if(old.isPresent()){
                var binding=new HousingProjects(c).find(old.get().homeDraft()).orElseThrow();
                if(!binding.proposal().equals(home)||!binding.world().equals(context.world()))throw new BuildingRegistry.Declined("Road purpose identity changed");
                return old.get(); // Receipt only: cancellation/completion never reactivate.
            }
            var link=new Link(road,home.draft(),context.now());
            validateGeometry(link);
            for(var id:List.of(road,home.draft()))for(var table:List.of("construction_site_reservations","construction_block_journal","construction_supply_orders","construction_material_reservations"))
                try(var s=c.prepareStatement("SELECT 1 FROM "+table+" WHERE draft_id=? LIMIT 1")){
                    s.setString(1,id.toString());try(var r=s.executeQuery()){if(r.next())throw new BuildingRegistry.Declined("Bind road purpose before any land, money or native work");}
                }
            var roadDraft=draft(road);
            if(!context.present()||!context.world().equals(roadDraft.world())||context.now()<roadDraft.createdMillis())throw new BuildingRegistry.Declined("Road purpose needs its current present world");
            if(new HousingProjects(c).find(home.draft()).isPresent())throw new BuildingRegistry.Declined("New access purpose requires a new household proposal");
            new HousingProjects(c).bindRows(home,context,planner,seed,period);
            try(var s=c.prepareStatement("INSERT INTO housing_road_projects VALUES(?,?,?)")){
                s.setString(1,road.toString());s.setString(2,home.draft().toString());s.setLong(3,context.now());s.executeUpdate();
            }
            return findRoad(road).orElseThrow();
    }
    Optional<String> pauseReasonRows(ConstructionDraft road)throws SQLException{
        if(c.getAutoCommit())throw new SQLException("Road beneficiary review requires SQL owner transaction");
        var link=findRoad(road.id());
        if(link.isEmpty()){
            var access=findHome(road.id());
            if(access.isPresent()&&new CivRoadCompletion(c).findDraft(access.orElseThrow().roadDraft()).isEmpty())
                return Optional.of("Frozen home waits for its retained access road to complete and register");
            return Optional.empty();
        }
        var home=draft(link.get().homeDraft());var binding=new HousingProjects(c).find(home.id()).orElseThrow();
        if(!binding.active())return Optional.of("Road's household proposal is cancelled or completed; retained access requires review");
        // Started infrastructure retains the same recovery/completion rule as started homes.
        try(var s=c.prepareStatement("SELECT 1 FROM construction_block_journal WHERE draft_id=? LIMIT 1")){
            s.setString(1,road.id().toString());try(var r=s.executeQuery()){if(r.next())return Optional.empty();}
        }
        var need=new HousingProjects(c).pauseReasonRows(home);if(need.isPresent())return need;
        if(new ConstructionSiteReservations(c).hasProtectedRequiredColumns(home,road.id()))return Optional.of("Future home/access is newly protected; unstarted road paused");
        if(new CivRoadCompletion(c).occupiesProjectGround(home))return Optional.of("Retained infrastructure occupies the future home; unstarted road paused");
        return Optional.empty();
    }
    /** A family reservation cannot be freed while its access project still holds land or unrecovered work. */
    void requireRetiredRoadRows(UUID home)throws SQLException{
        var link=findHome(home);if(link.isEmpty())return;UUID road=link.get().roadDraft();
        var site=new ConstructionSiteReservations(c).find(road);
        if(site.isPresent()&&site.get().active())throw new BuildingRegistry.Declined("Release/recover the access road before cancelling its household proposal");
        if(new CivRoadCompletion(c).findDraft(road).isPresent())return; // Completed public paving and purpose history remain.
        try(var s=c.prepareStatement("SELECT 1 FROM construction_block_journal WHERE draft_id=? LIMIT 1")){
            s.setString(1,road.toString());try(var r=s.executeQuery()){if(r.next())throw new BuildingRegistry.Declined("Started access road retains its household purpose until reviewed handoff");}
        }
    }
    private void validate(Link link)throws SQLException{
        validateGeometry(link);var home=draft(link.homeDraft());var binding=new HousingProjects(c).find(home.id()).orElseThrow(()->new SQLException("Road lost its household purpose"));
        if(!binding.village().equals(home.settlement())||!binding.world().equals(home.world())||!binding.planHash().equals(home.template().fingerprint())||link.createdMillis()<binding.proposal().createdMillis())throw new SQLException("Road purpose differs from its frozen household home");
    }
    private void validateGeometry(Link link)throws SQLException{
        var road=draft(link.roadDraft());var home=draft(link.homeDraft());LocalRoadDesign.Geometry geometry;
        try{geometry=LocalRoadDesign.geometry(road);}catch(IllegalArgumentException invalid){throw new SQLException("Housing access needs frozen local-road geometry",invalid);}
        if(home.template().purpose()!=Purpose.HOME||!road.settlement().equals(home.settlement())||!road.world().equals(home.world())||link.createdMillis()<Math.max(home.createdMillis(),road.createdMillis()))throw new BuildingRegistry.Declined("Road and future home village/world/time differ");
        var footprint=ConstructionPreview.footprint(home.template(),home.transform(),home.origin());var paved=new HashSet<Position>();
        for(var local:geometry.template().stages().getFirst().blocks().keySet()){
            var at=road.origin().plus(local);paved.add(at);
            if(at.x()>=footprint.minimum().x()&&at.x()<=footprint.maximum().x()&&at.z()>=footprint.minimum().z()&&at.z()<=footprint.maximum().z())throw new BuildingRegistry.Declined("Access road crosses its future home");
        }
        boolean connected=false;for(var anchor:home.template().anchors())if(anchor.role()==Role.ROAD){
            var at=home.origin().plus(home.transform().apply(anchor.at(),home.template().size()));if(paved.contains(at))connected=true;
        }
        if(!connected)throw new BuildingRegistry.Declined("Road does not provide the frozen home's declared access");
    }
    private ConstructionDraft draft(UUID id)throws SQLException{return new ConstructionDraftRepository(c).find(id).orElseThrow(()->new BuildingRegistry.Declined("Unknown frozen access/home draft"));}
}
