package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;

/** SQL-owner claim-before-spawn ledger. Trusted Paper callers must prove loaded safe placement;
 * missing entities never authorize replacement. Failed aliases and ownership provenance stay saved. */
public final class LivestockBirthProjections {
    public enum State { CLAIMED, ACTIVE, FAILED, DECEASED }
    public record Claim(UUID birth,UUID entity,UUID world,LivestockHerd.Species species,int x,int y,int z,long claimedMillis) {
        public Claim {Objects.requireNonNull(birth);Objects.requireNonNull(entity);Objects.requireNonNull(world);Objects.requireNonNull(species);if(claimedMillis<0)throw new IllegalArgumentException("Invalid birth claim time");}
    }
    public record Projection(Claim claim,State state) {}
    private final Connection connection;
    public LivestockBirthProjections(Connection connection){this.connection=Objects.requireNonNull(connection);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE livestock_birth_entities (
                    entity_id TEXT PRIMARY KEY NOT NULL REFERENCES animal_ownership(animal_id),
                    birth_id TEXT NOT NULL REFERENCES livestock_births(id),world_id TEXT NOT NULL REFERENCES observed_worlds(id),
                    species TEXT NOT NULL CHECK(species IN('COW','CHICKEN')),x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                    claimed_ms INTEGER NOT NULL CHECK(typeof(claimed_ms)='integer' AND claimed_ms>=0),
                    UNIQUE(birth_id,entity_id)
                )
                """);
            s.execute("""
                CREATE TABLE livestock_birth_projections (
                    birth_id TEXT PRIMARY KEY NOT NULL REFERENCES livestock_births(id),entity_id TEXT NOT NULL UNIQUE,
                    state TEXT NOT NULL CHECK(state IN('CLAIMED','ACTIVE','FAILED','DECEASED')),
                    FOREIGN KEY(birth_id,entity_id) REFERENCES livestock_birth_entities(birth_id,entity_id)
                )
                """);
        }
    }
    /** Exact replay returns its state, including FAILED/DECEASED; never interpret that as permission to add again. */
    public Projection claim(Claim request,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(request);var present=Set.copyOf(worlds);
        return transaction(()-> {
            var old=forEntity(request.entity());
            if(old.isPresent()){if(!old.get().claim().equals(request))throw new SQLException("Birth entity identity/evidence changed");return old.get();}
            var birth=new LivestockBirths(connection).find(request.birth()).orElseThrow(()->new SQLException("Saved simulated birth required"));
            var herd=new LivestockRepository(connection).load(birth.business()).orElseThrow();
            var founding=new BusinessRepository(connection).findFounding(birth.business()).orElseThrow();
            if(!present.contains(request.world())||!founding.site().worldId().equals(request.world())||herd.species()!=request.species())
                throw new SQLException("Birth claim requires the actual species and present origin world");
            if(new LivestockBirths(connection).unmappedBirths(birth.business())!=0)throw new SQLException("Anonymous birth history needs reconciliation before physical claims");
            var current=find(request.birth());
            if(current.isPresent()&&current.get().state()!=State.FAILED)throw new SQLException("A birth already has a physical claim or confirmed death");
            var ownership=new AnimalOwnershipRepository(connection);
            if(ownership.load(request.entity()).isPresent())throw new SQLException("Existing animal identity cannot become a new offspring");
            try(var s=connection.prepareStatement("SELECT 1 FROM village_entities WHERE entity_id=? LIMIT 1")) {
                s.setString(1,request.entity().toString());try(var r=s.executeQuery()){if(r.next())throw new SQLException("Resident entity identity cannot become livestock");}
            }
            ownership.recordRows(new AnimalOwnershipRepository.Claim(request.entity(),request.world(),LivestockAdoption.Ownership.VILLAGE,
                    null,founding.business().settlementId(),request.claimedMillis(),"Simulated livestock birth "+birth.id()));
            try(var s=connection.prepareStatement("INSERT INTO livestock_birth_entities VALUES(?,?,?,?,?,?,?,?)")) {
                s.setString(1,request.entity().toString());s.setString(2,request.birth().toString());s.setString(3,request.world().toString());s.setString(4,request.species().name());
                s.setInt(5,request.x());s.setInt(6,request.y());s.setInt(7,request.z());s.setLong(8,request.claimedMillis());s.executeUpdate();
            }
            try(var s=connection.prepareStatement("INSERT INTO livestock_birth_projections VALUES(?,?,'CLAIMED') ON CONFLICT(birth_id) DO UPDATE SET entity_id=excluded.entity_id,state='CLAIMED' WHERE livestock_birth_projections.state='FAILED'")) {
                s.setString(1,request.birth().toString());s.setString(2,request.entity().toString());if(s.executeUpdate()!=1)throw new SQLException("Birth claim changed");
            }
            return new Projection(request,State.CLAIMED);
        });
    }
    public Optional<Projection> find(UUID birth)throws SQLException {
        try(var s=connection.prepareStatement("SELECT a.*,p.entity_id AS current_entity,p.state FROM livestock_birth_projections p JOIN livestock_birth_entities a ON a.entity_id=p.entity_id WHERE p.birth_id=?")) {
            s.setString(1,Objects.requireNonNull(birth).toString());try(var r=s.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}
        }
    }
    public Optional<Projection> forEntity(UUID entity)throws SQLException {
        try(var s=connection.prepareStatement("SELECT a.*,p.entity_id AS current_entity,p.state FROM livestock_birth_entities a JOIN livestock_birth_projections p ON p.birth_id=a.birth_id WHERE a.entity_id=?")) {
            s.setString(1,Objects.requireNonNull(entity).toString());try(var r=s.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}
        }
    }
    public boolean activate(UUID birth,UUID entity)throws SQLException{return transition(birth,entity,State.ACTIVE);}
    /** Only when the same main-thread operation positively proved its prepared entity was never added.
     * This is not a missing-entity repair API. ACTIVE/DECEASED entities cannot be failed/replaced. */
    public boolean failedUnspawned(UUID birth,UUID entity)throws SQLException{return transition(birth,entity,State.FAILED);}
    private boolean transition(UUID birth,UUID entity,State target)throws SQLException {
        Objects.requireNonNull(entity);
        return transaction(()-> {
            var old=find(birth);if(old.isEmpty()||!old.get().claim().entity().equals(entity))return false;
            if(old.get().state()==target)return true;if(old.get().state()!=State.CLAIMED)return false;
            try(var s=connection.prepareStatement("UPDATE livestock_birth_projections SET state=? WHERE birth_id=? AND entity_id=? AND state='CLAIMED'")) {
                s.setString(1,target.name());s.setString(2,birth.toString());s.setString(3,entity.toString());return s.executeUpdate()==1;
            }
        });
    }
    private static Projection read(ResultSet r)throws SQLException {
        try {
            var claim=new Claim(UUID.fromString(r.getString("birth_id")),UUID.fromString(r.getString("entity_id")),UUID.fromString(r.getString("world_id")),LivestockHerd.Species.valueOf(r.getString("species")),r.getInt("x"),r.getInt("y"),r.getInt("z"),r.getLong("claimed_ms"));
            return new Projection(claim,claim.entity().toString().equals(r.getString("current_entity"))?State.valueOf(r.getString("state")):State.FAILED);
        }catch(IllegalArgumentException invalid){throw new SQLException("Invalid saved birth projection",invalid);}
    }
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Birth projection requires the complete owner transaction");connection.setAutoCommit(false);
        try{var result=work.run();connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
