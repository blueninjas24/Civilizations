package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** Separate owner consent prerequisite, never a block-write permit or permission to consume player contents. */
public final class BuildingRenovations {
    public enum Action { GRANT,REVOKE }
    public record Request(UUID operation,UUID building,UUID useConsent,UUID previous,String layoutHash,Action action,long atMillis){
        public Request{Objects.requireNonNull(operation);Objects.requireNonNull(building);Objects.requireNonNull(useConsent);Objects.requireNonNull(action);
            if(layoutHash==null||!layoutHash.matches("[0-9a-f]{64}")||atMillis<0)throw new IllegalArgumentException("Invalid renovation terms");}
    }
    /** Authenticated adapter facts. Grant requires the recorded donor, even for an administrator. */
    public record Authority(UUID actor,UUID world,boolean worldPresent,boolean permitted,boolean administrator,boolean confirmed){
        public Authority{Objects.requireNonNull(actor);Objects.requireNonNull(world);}
    }
    public record Receipt(Request request,UUID actor,boolean administrator){}
    private final Connection c;
    public BuildingRenovations(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException{
        try(var s=c.createStatement()){
            s.execute("""
                CREATE TABLE building_renovation_events (
                  sequence INTEGER PRIMARY KEY AUTOINCREMENT,operation_id TEXT NOT NULL UNIQUE,
                  building_id TEXT NOT NULL REFERENCES building_records(id),
                  use_consent_id TEXT NOT NULL REFERENCES building_consent_events(operation_id),
                  previous_event TEXT REFERENCES building_renovation_events(operation_id),
                  layout_hash TEXT NOT NULL CHECK(length(layout_hash)=64),
                  action TEXT NOT NULL CHECK(action IN('GRANT','REVOKE')),actor_id TEXT NOT NULL,
                  administrator INTEGER NOT NULL CHECK(administrator IN(0,1)),
                  at_ms INTEGER NOT NULL CHECK(typeof(at_ms)='integer' AND at_ms>=0)
                )
                """);
            s.execute("CREATE INDEX building_renovations_latest ON building_renovation_events(building_id,sequence)");
        }
    }
    /** Read-only review inputs. Confirmation must repeat authority, layout and current-use checks. */
    public Optional<Receipt> latest(UUID building)throws SQLException{
        try(var s=c.prepareStatement("SELECT * FROM building_renovation_events WHERE building_id=? ORDER BY sequence DESC LIMIT 1")){
            s.setString(1,building.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}
        }
    }
    public Optional<Receipt> receipt(UUID operation)throws SQLException{
        try(var s=c.prepareStatement("SELECT * FROM building_renovation_events WHERE operation_id=?")){
            s.setString(1,operation.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}
        }
    }
    public boolean hasPermission(UUID building,UUID world)throws SQLException{
        var registry=new BuildingRegistry(c);var entry=registry.find(building);var current=latest(building);
        if(entry.isEmpty()||current.isEmpty()||!entry.get().layout().world().equals(world)||!registry.hasDonationPermission(building))return false;
        var request=current.get().request();
        return request.action()==Action.GRANT&&request.useConsent().equals(entry.get().latest().operation())
                &&request.layoutHash().equals(entry.get().layout().fingerprint());
    }
    public Receipt confirm(Request request,Authority authority)throws SQLException{
        Objects.requireNonNull(request);Objects.requireNonNull(authority);
        if(!c.getAutoCommit())throw new SQLException("Renovation confirmation requires its SQL-owner transaction");
        c.setAutoCommit(false);
        try{
            var registry=new BuildingRegistry(c);var entry=registry.find(request.building()).orElseThrow(()->new BuildingRegistry.Declined("Unknown building"));
            if(!authority.worldPresent()||!authority.world().equals(entry.layout().world())||!authority.permitted()||!authority.confirmed())
                throw new BuildingRegistry.Declined("Explicit current renovation confirmation and world/permission required");
            if(!entry.donor().equals(authority.actor())&&(request.action()==Action.GRANT||!authority.administrator()))
                throw new BuildingRegistry.Declined("Only the recorded donor can grant renovations; administrators may revoke only");
            var expected=new Receipt(request,authority.actor(),authority.administrator());var replay=receipt(request.operation());
            if(replay.isPresent()){
                if(!replay.get().equals(expected))throw new BuildingRegistry.Declined("Renovation operation reused with changed terms");
                c.commit();c.setAutoCommit(true);return replay.get(); // Receipt only: never restore a later revoked/reclaimed grant.
            }
            if(entry.latest()==null||request.action()==Action.GRANT&&!registry.hasDonationPermission(request.building())||!request.useConsent().equals(entry.latest().operation())
                    ||!request.layoutHash().equals(entry.layout().fingerprint())||request.atMillis()<entry.latest().atMillis())
                throw new BuildingRegistry.Declined("Building use, layout or donation changed; review renovation consent again");
            var previous=latest(request.building());
            if(!Objects.equals(request.previous(),previous.map(p->p.request().operation()).orElse(null))
                    ||previous.isPresent()&&request.atMillis()<previous.get().request().atMillis())
                throw new BuildingRegistry.Declined("Renovation consent history changed; review again");
            try(var s=c.prepareStatement("INSERT INTO building_renovation_events(operation_id,building_id,use_consent_id,previous_event,layout_hash,action,actor_id,administrator,at_ms) VALUES(?,?,?,?,?,?,?,?,?)")){
                s.setString(1,request.operation().toString());s.setString(2,request.building().toString());s.setString(3,request.useConsent().toString());
                if(request.previous()==null)s.setNull(4,Types.VARCHAR);else s.setString(4,request.previous().toString());
                s.setString(5,request.layoutHash());s.setString(6,request.action().name());s.setString(7,authority.actor().toString());
                s.setBoolean(8,authority.administrator());s.setLong(9,request.atMillis());s.executeUpdate();
            }
            c.commit();c.setAutoCommit(true);return expected;
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private Receipt read(ResultSet r)throws SQLException{
        String previous=r.getString("previous_event");
        return new Receipt(new Request(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("building_id")),
                UUID.fromString(r.getString("use_consent_id")),previous==null?null:UUID.fromString(previous),r.getString("layout_hash"),
                Action.valueOf(r.getString("action")),r.getLong("at_ms")),UUID.fromString(r.getString("actor_id")),r.getBoolean("administrator"));
    }
}
