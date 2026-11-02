package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.BuildingLayout;
import com.blueninjas24.civilizations.construction.WorkplaceInspection;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.sql.*;
import java.util.*;

/** SQL-owner reuse holds only. A hold is not a habitability inspection, property transfer or work permit. */
public final class BusinessPremises {
    public record Request(UUID operation,UUID business,UUID building,UUID useConsent,String layoutHash,long createdMillis){
        public Request{Objects.requireNonNull(operation);Objects.requireNonNull(business);Objects.requireNonNull(building);Objects.requireNonNull(useConsent);
            if(layoutHash==null||!layoutHash.matches("[0-9a-f]{64}")||createdMillis<0)throw new IllegalArgumentException("Invalid premises reservation");}
    }
    public record Hold(Request request,UUID releaseOperation,Long releasedMillis){public boolean retained(){return releasedMillis==null;}}
    private final Connection c;
    public BusinessPremises(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException{
        try(var s=c.createStatement()){
            s.execute("""
                CREATE TABLE business_premises (
                  operation_id TEXT PRIMARY KEY NOT NULL,business_id TEXT NOT NULL REFERENCES businesses(id),
                  building_id TEXT NOT NULL REFERENCES building_records(id),
                  use_consent TEXT NOT NULL REFERENCES building_consent_events(operation_id),
                  layout_hash TEXT NOT NULL CHECK(length(layout_hash)=64),
                  created_ms INTEGER NOT NULL CHECK(typeof(created_ms)='integer' AND created_ms>=0),
                  release_operation TEXT UNIQUE,released_ms INTEGER,
                  CHECK((release_operation IS NULL AND released_ms IS NULL) OR
                        (release_operation IS NOT NULL AND typeof(released_ms)='integer' AND released_ms>=created_ms))
                )
                """);
            s.execute("CREATE UNIQUE INDEX business_premises_one_building ON business_premises(building_id) WHERE released_ms IS NULL");
            s.execute("CREATE UNIQUE INDEX business_premises_one_business ON business_premises(business_id) WHERE released_ms IS NULL");
            s.execute("""
                CREATE TRIGGER business_premises_release_only BEFORE UPDATE ON business_premises
                WHEN OLD.released_ms IS NOT NULL OR NEW.released_ms IS NULL OR NEW.release_operation IS NULL
                  OR NEW.operation_id IS NOT OLD.operation_id OR NEW.business_id IS NOT OLD.business_id
                  OR NEW.building_id IS NOT OLD.building_id OR NEW.use_consent IS NOT OLD.use_consent
                  OR NEW.layout_hash IS NOT OLD.layout_hash OR NEW.created_ms IS NOT OLD.created_ms
                BEGIN SELECT RAISE(ABORT,'Premises history permits only one retained release'); END
                """);
            s.execute("CREATE TRIGGER business_premises_no_delete BEFORE DELETE ON business_premises BEGIN SELECT RAISE(ABORT,'Premises history is retained'); END");
        }
    }
    public Optional<Hold> find(UUID operation)throws SQLException{
        try(var s=c.prepareStatement("SELECT * FROM business_premises WHERE operation_id=?")){
            s.setString(1,operation.toString());try(var r=s.executeQuery()){
                if(!r.next())return Optional.empty();
                var request=new Request(operation,UUID.fromString(r.getString("business_id")),UUID.fromString(r.getString("building_id")),UUID.fromString(r.getString("use_consent")),r.getString("layout_hash"),r.getLong("created_ms"));
                String release=r.getString("release_operation");return Optional.of(new Hold(request,release==null?null:UUID.fromString(release),release==null?null:r.getLong("released_ms")));
            }
        }
    }
    /** Combine computed physical suitability with the existing transactional hold checks.
     * This still creates only a hold; it cannot activate production or grant a native write permit. */
    public Hold reserveInspected(Request request,WorkplaceInspection.Assessment assessment,Set<UUID> worlds,long now)throws SQLException{
        Objects.requireNonNull(request);Objects.requireNonNull(assessment);
        if(!c.getAutoCommit())throw new SQLException("Inspected premises reservation needs idle SQL owner");
        requireInspection(request,assessment,now);
        // Frozen layout/site/founding terms cannot change between these owner-thread checks and reserve().
        // Current consent, closure, marker freshness and collision checks still occur in its transaction.
        return reserve(request,worlds,now);
    }
    Hold reserveInspectedRows(Request request,WorkplaceInspection.Assessment assessment,Set<UUID> worlds,long now)throws SQLException{
        if(c.getAutoCommit())throw new SQLException("Inspected premises rows require an owner transaction");
        requireInspection(request,assessment,now);return reserveRows(request,worlds,now);
    }
    private void requireInspection(Request request,WorkplaceInspection.Assessment assessment,long now)throws SQLException{
        Objects.requireNonNull(request);Objects.requireNonNull(assessment);
        var evidence=assessment.evidence();var observed=evidence.structure();
        var entry=new BuildingRegistry(c).find(request.building()).orElseThrow(()->new BuildingRegistry.Declined("Unknown inspected premises"));
        var firm=new BusinessRepository(c).findFounding(request.business()).orElseThrow(()->new BuildingRegistry.Declined("Unknown inspected business"));
        var site=firm.site();
        if(!assessment.suitable()||!observed.layoutHash().equals(request.layoutHash())||!entry.layout().fingerprint().equals(observed.layoutHash())
           ||!observed.world().equals(site.worldId())||observed.capturedMillis()>now||now-observed.capturedMillis()>5000
           ||!evidence.marker().equals(new Position(site.x(),site.y(),site.z()))
           ||evidence.kind().isEmpty()||!evidence.kind().orElseThrow().industry().equals(firm.business().industry()))throw new BuildingRegistry.Declined("Fresh suitable exact-building and fixed-workplace inspection required");
    }
    /** Fresh positive fixed-marker evidence locates the firm; it does not prove the whole building usable.
     * Exact replay returns history even after release/reclamation, never renewed permission. */
    public Hold reserve(Request request,Set<UUID> presentWorlds,long now)throws SQLException{
        if(!c.getAutoCommit())throw new SQLException("Premises reservation needs idle SQL owner");c.setAutoCommit(false);
        try{var saved=reserveRows(request,presentWorlds,now);c.commit();return saved;}
        catch(SQLException|RuntimeException failure){rollback(failure);throw failure;}finally{c.setAutoCommit(true);}
    }
    /** Package-local composition only; never commits another owner's transaction. */
    Hold reserveRows(Request request,Set<UUID> presentWorlds,long now)throws SQLException{
        if(c.getAutoCommit())throw new SQLException("Premises rows require an owner transaction");
        Objects.requireNonNull(request);presentWorlds=Set.copyOf(presentWorlds);
        if(now<request.createdMillis())throw new IllegalArgumentException("Reservation time is in the future");
        var old=find(request.operation());if(old.isPresent()){
            if(!old.get().request().equals(request))throw new BuildingRegistry.Declined("Premises operation terms changed");
            return old.get();
        }
        var registry=new BuildingRegistry(c);var entry=registry.find(request.building()).orElseThrow(()->new BuildingRegistry.Declined("Unknown premises building"));
        var layout=entry.layout();
        if(layout.kind()!=BuildingLayout.Kind.WORKPLACE||!presentWorlds.contains(layout.world())||!permission(request,entry,registry))throw new BuildingRegistry.Declined("Current explicit workplace use permission and present world required");
        if(now-request.createdMillis()>5000||request.createdMillis()<entry.latest().atMillis())throw new BuildingRegistry.Declined("Fresh reservation after its current consent required");
        var founding=new BusinessRepository(c).findFounding(request.business()).orElseThrow(()->new BuildingRegistry.Declined("Unknown premises business"));
        var site=founding.site();
        if(!site.settlementId().equals(layout.settlement())||!site.worldId().equals(layout.world())||!layout.structure().contains(new Position(site.x(),site.y(),site.z())))throw new BuildingRegistry.Declined("Fixed business workplace must be inside its local premises");
        BusinessClosure.requireOpen(c,request.business());
        try(var s=c.prepareStatement("SELECT kind,captured_millis,present FROM business_workplace_anchors WHERE business_id=? AND world_id=? AND x=? AND y=? AND z=?")){
            s.setString(1,request.business().toString());s.setString(2,site.worldId().toString());s.setInt(3,site.x());s.setInt(4,site.y());s.setInt(5,site.z());
            try(var r=s.executeQuery()){
                if(!r.next()||!r.getBoolean("present")||r.getLong("captured_millis")>now||now-r.getLong("captured_millis")>5000
                   ||!com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.valueOf(r.getString("kind")).industry().equals(founding.business().industry()))throw new BuildingRegistry.Declined("Fresh positive compatible workplace marker required");
            }
        }
        // A firm without the new hold table is still an occupant; never infer vacancy from absent rows.
        try(var s=c.prepareStatement("""
            SELECT 1 FROM businesses b JOIN business_sites p ON p.id=b.site_id
            WHERE b.id<>? AND p.world_id=? AND p.x BETWEEN ? AND ? AND p.y BETWEEN ? AND ? AND p.z BETWEEN ? AND ?
              AND NOT EXISTS(SELECT 1 FROM business_closures x WHERE x.business_id=b.id) LIMIT 1
            """)){
            var bounds=layout.structure();s.setString(1,request.business().toString());s.setString(2,layout.world().toString());s.setInt(3,bounds.min().x());s.setInt(4,bounds.max().x());s.setInt(5,bounds.min().y());s.setInt(6,bounds.max().y());s.setInt(7,bounds.min().z());s.setInt(8,bounds.max().z());
            try(var r=s.executeQuery()){if(r.next())throw new BuildingRegistry.Declined("Another saved business occupies this building");}
        }
        try(var s=c.prepareStatement("INSERT INTO business_premises(operation_id,business_id,building_id,use_consent,layout_hash,created_ms) VALUES(?,?,?,?,?,?)")){
            s.setString(1,request.operation().toString());s.setString(2,request.business().toString());s.setString(3,request.building().toString());s.setString(4,request.useConsent().toString());s.setString(5,request.layoutHash());s.setLong(6,request.createdMillis());s.executeUpdate();
        }
        return find(request.operation()).orElseThrow();
    }
    /** Release only a closed firm or revoked/excluded use epoch. Absent worlds and missing markers are not vacancy. */
    public Hold release(UUID operation,UUID reservation,long now)throws SQLException{
        if(!c.getAutoCommit())throw new SQLException("Premises release needs idle SQL owner");c.setAutoCommit(false);
        try{var saved=releaseRows(operation,reservation,now);c.commit();return saved;}
        catch(SQLException|RuntimeException failure){rollback(failure);throw failure;}finally{c.setAutoCommit(true);}
    }
    Hold releaseRows(UUID operation,UUID reservation,long now)throws SQLException{
        if(c.getAutoCommit())throw new SQLException("Premises release rows require an owner transaction");
        Objects.requireNonNull(operation);Objects.requireNonNull(reservation);
        var hold=find(reservation).orElseThrow(()->new BuildingRegistry.Declined("Unknown premises reservation"));
        if(hold.releasedMillis()!=null){
            if(!operation.equals(hold.releaseOperation())||now!=hold.releasedMillis())throw new BuildingRegistry.Declined("Premises release terms changed");
            return hold;
        }
        if(now<hold.request().createdMillis())throw new BuildingRegistry.Declined("Release predates reservation");
        var registry=new BuildingRegistry(c);var entry=registry.find(hold.request().building()).orElseThrow();
        if(permission(hold.request(),entry,registry)&&!BusinessClosure.blocked(c,hold.request().business()))throw new BuildingRegistry.Declined("Active permitted business is not vacant");
        try(var s=c.prepareStatement("UPDATE business_premises SET release_operation=?,released_ms=? WHERE operation_id=? AND released_ms IS NULL")){
            s.setString(1,operation.toString());s.setLong(2,now);s.setString(3,reservation.toString());if(s.executeUpdate()!=1)throw new SQLException("Premises reservation changed");
        }
        return find(reservation).orElseThrow();
    }
    private boolean permission(Request request,BuildingRegistry.Entry entry,BuildingRegistry registry)throws SQLException{
        return entry.layout().fingerprint().equals(request.layoutHash())&&entry.latest()!=null&&entry.latest().operation().equals(request.useConsent())&&registry.hasDonationPermission(entry.layout().id());
    }
    /** Restriction only: once formally bound, revoked/released premises cannot fall back to marker-only use.
     * Businesses never bound to formal premises retain their existing P3 behavior. */
    static boolean denied(Connection c,UUID business)throws SQLException{
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA user_version")){if(r.getInt(1)<120)return false;}
        UUID operation=null;
        try(var s=c.prepareStatement("SELECT operation_id FROM business_premises WHERE business_id=? AND released_ms IS NULL")){
            s.setString(1,business.toString());try(var r=s.executeQuery()){if(r.next())operation=UUID.fromString(r.getString(1));}
        }
        if(operation==null){
            try(var s=c.prepareStatement("SELECT 1 FROM business_premises WHERE business_id=? LIMIT 1")){
                s.setString(1,business.toString());try(var r=s.executeQuery()){return r.next();}
            }
        }
        var repository=new BusinessPremises(c);var hold=repository.find(operation).orElseThrow(()->new SQLException("Active premises history disappeared"));
        var registry=new BuildingRegistry(c);var entry=registry.find(hold.request().building()).orElseThrow(()->new SQLException("Active premises building disappeared"));
        return !repository.permission(hold.request(),entry,registry);
    }
    private void rollback(Throwable failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}}
}
