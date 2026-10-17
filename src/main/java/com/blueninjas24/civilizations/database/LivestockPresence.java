package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Latest positively observed locations of already saved living herd members. Unknown never means away. */
public final class LivestockPresence {
    public record Member(UUID animal,UUID business,LivestockHerd.Species species,UUID world,Position home,long maturePeriod,long herdPeriod) {
        public Member {Objects.requireNonNull(animal);Objects.requireNonNull(business);Objects.requireNonNull(species);Objects.requireNonNull(world);Objects.requireNonNull(home);if(maturePeriod<0||herdPeriod<0)throw new IllegalArgumentException("Invalid saved livestock age");}
    }
    public record Sight(UUID animal,LivestockHerd.Species species,UUID world,Position position,long observedMillis) {
        public Sight {Objects.requireNonNull(animal);Objects.requireNonNull(species);Objects.requireNonNull(world);Objects.requireNonNull(position);if(observedMillis<0)throw new IllegalArgumentException("Invalid livestock observation time");}
    }
    private static final String MEMBERS="""
        WITH members AS (
            SELECT a.animal_id,a.business_id,a.mature_period FROM livestock_initial_animals a
            LEFT JOIN livestock_animal_losses l ON l.animal_id=a.animal_id WHERE l.animal_id IS NULL
            UNION ALL
            SELECT p.entity_id,b.business_id,b.mature_period FROM livestock_birth_projections p
            JOIN livestock_births b ON b.id=p.birth_id WHERE p.state='ACTIVE'
        )
        """;
    private final Connection connection;
    public LivestockPresence(Connection connection){this.connection=Objects.requireNonNull(connection);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE livestock_presence (
                    animal_id TEXT PRIMARY KEY NOT NULL REFERENCES animal_ownership(animal_id),
                    business_id TEXT NOT NULL REFERENCES livestock_herds(business_id),
                    species TEXT NOT NULL CHECK(species IN('COW','CHICKEN')),
                    world_id TEXT NOT NULL CHECK(length(world_id)=36),
                    x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,
                    observed_millis INTEGER NOT NULL CHECK(typeof(observed_millis)='integer' AND observed_millis>=0)
                )
                """);
            s.execute("CREATE INDEX livestock_presence_business ON livestock_presence(business_id,animal_id)");
        }
    }
    /** UUID-text seek pagination; at most256 native lookups need be prepared at once. */
    public List<Member> page(UUID after,int limit)throws SQLException {
        if(limit<1||limit>256)throw new IllegalArgumentException("Invalid livestock member page size");
        String query=MEMBERS+"""
            SELECT m.*,h.species,s.world_id,s.x,s.y,s.z,
                (SELECT MAX(p.period) FROM livestock_periods p WHERE p.business_id=m.business_id) AS herd_period FROM members m
            JOIN livestock_herds h ON h.business_id=m.business_id JOIN businesses b ON b.id=m.business_id
            JOIN business_sites s ON s.id=b.site_id WHERE m.animal_id>? ORDER BY m.animal_id LIMIT ?
            """;
        try(var s=connection.prepareStatement(query)) {
            s.setString(1,after==null?"":after.toString());s.setInt(2,limit);
            try(var r=s.executeQuery()){var result=new ArrayList<Member>();while(r.next())result.add(member(r));return List.copyOf(result);}
        }
    }
    Optional<Member> member(UUID animal)throws SQLException {
        try(var s=connection.prepareStatement(MEMBERS+"""
            SELECT m.*,h.species,s.world_id,s.x,s.y,s.z,
                (SELECT MAX(p.period) FROM livestock_periods p WHERE p.business_id=m.business_id) AS herd_period FROM members m
            JOIN livestock_herds h ON h.business_id=m.business_id JOIN businesses b ON b.id=m.business_id
            JOIN business_sites s ON s.id=b.site_id WHERE m.animal_id=?
            """)) {
            s.setString(1,animal.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(member(r)):Optional.empty();}
        }
    }
    private static Member member(ResultSet r)throws SQLException {
        long period=r.getLong("herd_period");if(r.wasNull())throw new SQLException("Saved herd checkpoint missing for animal age");
        return new Member(UUID.fromString(r.getString("animal_id")),UUID.fromString(r.getString("business_id")),LivestockHerd.Species.valueOf(r.getString("species")),UUID.fromString(r.getString("world_id")),new Position(r.getInt("x"),r.getInt("y"),r.getInt("z")),r.getLong("mature_period"),period);
    }
    public Optional<Sight> find(UUID animal)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM livestock_presence WHERE animal_id=?")) {
            s.setString(1,Objects.requireNonNull(animal).toString());try(var r=s.executeQuery()) {
                return r.next()?Optional.of(new Sight(animal,LivestockHerd.Species.valueOf(r.getString("species")),UUID.fromString(r.getString("world_id")),new Position(r.getInt("x"),r.getInt("y"),r.getInt("z")),r.getLong("observed_millis"))):Optional.empty();
            }
        }
    }
    /** Trusted positive loaded-body evidence. Omitted animals keep their last known eligibility. */
    public int record(List<Sight> observations)throws SQLException {
        var sights=List.copyOf(observations);if(sights.size()>256)throw new IllegalArgumentException("Livestock sight budget exceeded");
        var ids=new HashSet<UUID>();for(var sight:sights)if(!ids.add(sight.animal()))throw new IllegalArgumentException("Duplicate livestock sight");
        if(!connection.getAutoCommit())throw new SQLException("Presence requires complete owner transaction");connection.setAutoCommit(false);
        try {
            int changed=0;
            for(var sight:sights) {
                var animal=member(sight.animal());if(animal.isEmpty())continue;
                if(animal.get().species()!=sight.species())throw new SQLException("Observed livestock species differs from saved herd");
                var old=find(sight.animal());
                if(old.isPresent()) {
                    if(old.get().observedMillis()>sight.observedMillis())continue;
                    if(old.get().observedMillis()==sight.observedMillis()) {
                        if(!old.get().equals(sight))throw new SQLException("Livestock position evidence changed at the same time");continue;
                    }
                }
                try(var s=connection.prepareStatement("INSERT INTO livestock_presence VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(animal_id) DO UPDATE SET world_id=excluded.world_id,x=excluded.x,y=excluded.y,z=excluded.z,observed_millis=excluded.observed_millis")) {
                    s.setString(1,sight.animal().toString());s.setString(2,animal.get().business().toString());s.setString(3,sight.species().name());s.setString(4,sight.world().toString());s.setInt(5,sight.position().x());s.setInt(6,sight.position().y());s.setInt(7,sight.position().z());s.setLong(8,sight.observedMillis());changed+=s.executeUpdate();
                }
            }
            connection.commit();connection.setAutoCommit(true);return changed;
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    /** Query only living members mature in the requested next period. Floating-point deltas avoid
     * integer overflow at opposite world-coordinate extremes. No row means never confirmed away. */
    long unavailableAdultsRows(UUID business,long nextPeriod,LivestockWorkingArea policy)throws SQLException {
        Objects.requireNonNull(policy);
        try(var s=connection.prepareStatement(MEMBERS+"""
            SELECT COUNT(*) FROM members m JOIN livestock_presence p ON p.animal_id=m.animal_id AND p.business_id=m.business_id
            JOIN businesses b ON b.id=m.business_id JOIN business_sites s ON s.id=b.site_id
            WHERE m.business_id=? AND m.mature_period<=? AND
            (p.world_id<>s.world_id OR
             (CAST(p.x AS REAL)-s.x)*(CAST(p.x AS REAL)-s.x)+
             (CAST(p.y AS REAL)-s.y)*(CAST(p.y AS REAL)-s.y)+
             (CAST(p.z AS REAL)-s.z)*(CAST(p.z AS REAL)-s.z)>?)
            """)) {
            s.setString(1,business.toString());s.setLong(2,nextPeriod);s.setLong(3,(long)policy.radiusBlocks()*policy.radiusBlocks());try(var r=s.executeQuery()){r.next();return r.getLong(1);}
        }
    }
}
