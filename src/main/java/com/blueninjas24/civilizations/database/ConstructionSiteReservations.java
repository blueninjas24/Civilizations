package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.Bounds;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.sql.*;
import java.util.*;

/** SQL-owner scheduler reservations, not land ownership, funding, labor credit or permission to write. */
public final class ConstructionSiteReservations {
    public record Reservation(UUID draft,UUID world,Bounds footprint,long checkedMillis,long reservedMillis,Long releasedMillis) {
        public boolean active(){return releasedMillis==null;}
    }
    private final Connection c;
    public ConstructionSiteReservations(Connection connection){c=Objects.requireNonNull(connection);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE construction_site_reservations (
                    draft_id TEXT PRIMARY KEY NOT NULL REFERENCES construction_drafts(id),
                    world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    min_x INTEGER NOT NULL CHECK(typeof(min_x)='integer' AND min_x BETWEEN -2147483648 AND 2147483647),
                    min_y INTEGER NOT NULL CHECK(typeof(min_y)='integer' AND min_y BETWEEN -2147483648 AND 2147483647),
                    min_z INTEGER NOT NULL CHECK(typeof(min_z)='integer' AND min_z BETWEEN -2147483648 AND 2147483647),
                    max_x INTEGER NOT NULL CHECK(typeof(max_x)='integer' AND max_x BETWEEN min_x AND min_x+63 AND max_x<=2147483647),
                    max_y INTEGER NOT NULL CHECK(typeof(max_y)='integer' AND max_y BETWEEN min_y AND min_y+63 AND max_y<=2147483647),
                    max_z INTEGER NOT NULL CHECK(typeof(max_z)='integer' AND max_z BETWEEN min_z AND min_z+63 AND max_z<=2147483647),
                    checked_ms INTEGER NOT NULL CHECK(typeof(checked_ms)='integer' AND checked_ms>=0),
                    reserved_ms INTEGER NOT NULL CHECK(typeof(reserved_ms)='integer' AND reserved_ms>=checked_ms AND reserved_ms-checked_ms<=5000),
                    released_ms INTEGER CHECK(released_ms IS NULL OR (typeof(released_ms)='integer' AND released_ms>=reserved_ms))
                )
                """);
            s.execute("CREATE INDEX construction_active_sites ON construction_site_reservations(world_id,min_x,max_x,min_z,max_z) WHERE released_ms IS NULL");
            s.execute("""
                CREATE TRIGGER construction_site_overlap BEFORE INSERT ON construction_site_reservations
                WHEN NEW.released_ms IS NULL AND EXISTS(SELECT 1 FROM construction_site_reservations r
                    WHERE r.world_id=NEW.world_id AND r.released_ms IS NULL
                    AND r.min_x<=NEW.max_x AND r.max_x>=NEW.min_x AND r.min_z<=NEW.max_z AND r.max_z>=NEW.min_z)
                BEGIN SELECT RAISE(ABORT,'Construction site already reserved'); END
                """);
        }
    }
    public Reservation reserve(UUID draftId,ConstructionMaterials.Authority authority,long now)throws SQLException {
        Objects.requireNonNull(authority);if(now<0)throw new IllegalArgumentException("Negative reservation time");
        return transaction(()->reserveRows(draftId,authority,now));
    }
    Reservation reserveRows(UUID draftId,ConstructionMaterials.Authority authority,long now)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Site reservation requires the SQL owner's transaction");
        var old=find(draftId);if(old.isPresent()) {
            if(!old.orElseThrow().active())throw new SQLException("Released construction draft cannot reserve land again; submit a new proposal");
            return old.orElseThrow(); // Saved receipt only: no new native action or refreshed authority.
        }
        var draft=draft(draftId);requireHouseholdProject(draft);var bounds=ConstructionPreview.footprint(draft.template(),draft.transform(),draft.origin());
        if(!authority.draft().equals(draft.id())||!authority.world().equals(draft.world())||!authority.worldLoaded()||!authority.siteCleared()
                ||authority.checkedMillis()>now||now-authority.checkedMillis()>5000)
            throw new SQLException("Fresh complete world/site/protection evidence required before reserving land");
        if(!new ConstructionLandRepository(c).protectedColumns(draft.world(),bounds).isEmpty())
            throw new SQLException("Saved protected plot or active construction occupies this site");
        try(var s=c.prepareStatement("INSERT INTO construction_site_reservations VALUES(?,?,?,?,?,?,?,?,?,?,NULL)")) {
            s.setString(1,draftId.toString());s.setString(2,draft.world().toString());
            s.setInt(3,bounds.minimum().x());s.setInt(4,bounds.minimum().y());s.setInt(5,bounds.minimum().z());
            s.setInt(6,bounds.maximum().x());s.setInt(7,bounds.maximum().y());s.setInt(8,bounds.maximum().z());
            s.setLong(9,authority.checkedMillis());s.setLong(10,now);s.executeUpdate();
        }
        return find(draftId).orElseThrow();
    }
    /** Release is limited to unused proposals or completely returned stockpiles; any prepared native work blocks it. */
    public Reservation releaseUnused(UUID draftId,long now)throws SQLException {
        if(now<0)throw new IllegalArgumentException("Negative release time");
        return transaction(()->releaseUnusedRows(draftId,now));
    }
    Reservation releaseUnusedRows(UUID draftId,long now)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Unused site release requires the SQL owner's transaction");
        var saved=find(draftId).orElseThrow(()->new SQLException("Construction site not reserved"));
        if(!saved.active())return saved;
        if(now<saved.reservedMillis())throw new SQLException("Construction release precedes reservation");
        try(var s=c.prepareStatement("SELECT 1 FROM construction_supply_orders WHERE draft_id=? LIMIT 1")) {
            s.setString(1,draftId.toString());try(var r=s.executeQuery()){if(r.next()&&!new ConstructionSupplyRecalls(c).recalled(draftId)&&!new ConstructionSupplyReturns(c).dispatched(draftId))throw new SQLException("Paid supplier cargo/stock retains its project site until reviewed return handling");}
        }
        try(var s=c.prepareStatement("SELECT 1 FROM construction_block_journal WHERE draft_id=? LIMIT 1")) {
            s.setString(1,draftId.toString());try(var r=s.executeQuery()){if(r.next())throw new SQLException("Started construction requires reviewed physical recovery and site history");}
        }
        try(var s=c.prepareStatement("SELECT operation_id FROM construction_material_reservations WHERE draft_id=?")) {
            s.setString(1,draftId.toString());try(var r=s.executeQuery()){if(r.next()&&!new ConstructionMaterialReturns(c).returned(UUID.fromString(r.getString(1))))throw new SQLException("Funded construction retains its site until all unused materials return");}
        }
        try(var s=c.prepareStatement("UPDATE construction_site_reservations SET released_ms=? WHERE draft_id=? AND released_ms IS NULL")) {
            s.setLong(1,now);s.setString(2,draftId.toString());if(s.executeUpdate()!=1)throw new SQLException("Construction reservation changed");
        }
        return find(draftId).orElseThrow();
    }
    /** Rechecked in the same owner transaction as a new material debit. A new player protection pauses the project. */
    void requireActiveRows(ConstructionDraft draft)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Construction authority review requires the material owner transaction");
        requireHouseholdProject(draft);
        if(new ConstructionSupplyRecalls(c).recalled(draft.id())||new ConstructionSupplyReturns(c).dispatched(draft.id()))throw new SQLException("Project supplies recovered; new purchasing/work prohibited");
        var saved=find(draft.id()).orElseThrow(()->new SQLException("Materials require an active saved site reservation"));
        if(!saved.active()||!new ConstructionLandRepository(c).protectedColumns(draft.world(),saved.footprint(),draft.id()).isEmpty())
            throw new SQLException("Construction site released or newly protected; material reservation paused");
    }
    private void requireHouseholdProject(ConstructionDraft draft)throws SQLException {
        var reason=new HousingProjects(c).pauseReasonRows(draft);
        if(reason.isPresent())throw new SQLException(reason.orElseThrow()+"; no new land, materials or block intent authorized");
    }
    public Optional<Reservation> find(UUID draftId)throws SQLException {
        Objects.requireNonNull(draftId);
        try(var s=c.prepareStatement("SELECT * FROM construction_site_reservations WHERE draft_id=?")) {
            s.setString(1,draftId.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                try {
                    var bounds=new Bounds(new Position(r.getInt("min_x"),r.getInt("min_y"),r.getInt("min_z")),new Position(r.getInt("max_x"),r.getInt("max_y"),r.getInt("max_z")));
                    var world=UUID.fromString(r.getString("world_id"));var draft=draft(draftId);
                    if(!world.equals(draft.world())||!bounds.equals(ConstructionPreview.footprint(draft.template(),draft.transform(),draft.origin())))throw new SQLException("Reserved site differs from its frozen proposal");
                    long released=r.getLong("released_ms");Long release=r.wasNull()?null:released;
                    return Optional.of(new Reservation(draftId,world,bounds,r.getLong("checked_ms"),r.getLong("reserved_ms"),release));
                }catch(IllegalArgumentException invalid){throw new SQLException("Invalid construction site receipt",invalid);}
            }
        }
    }
    private ConstructionDraft draft(UUID id)throws SQLException{return new ConstructionDraftRepository(c).find(id).orElseThrow(()->new SQLException("Unknown frozen construction draft"));}
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!c.getAutoCommit())throw new SQLException("Construction site reservation requires idle SQL owner");c.setAutoCommit(false);
        try{T value=work.run();c.commit();c.setAutoCommit(true);return value;}
        catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
