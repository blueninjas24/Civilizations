package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.Bounds;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/** SQL-owner consent history. Call only after authenticated ownership/confirmation review.
 * This backend neither authenticates players nor grants block changes or usable housing capacity. */
public final class BuildingRegistry {
    /** Expected user/protection conflict, not a broken database or simulation failure. */
    public static final class Declined extends SQLException { public Declined(String message){super(message);} }
    private final Connection connection;
    public BuildingRegistry(Connection connection) { this.connection = Objects.requireNonNull(connection); }
    public enum Action { DONATE, RECLAIM }
    /** Trusted adapter confirms authenticated player declaration/review, never inferred from a sign alone. */
    public record Authorization(UUID actor, boolean administrator, boolean ownershipConfirmed, boolean confirmed) {
        public Authorization { Objects.requireNonNull(actor); }
        /** User-approved policy: explicit self-declaration, not proof of an old Minecraft build's author. */
        public static Authorization playerDeclaration(UUID actor,boolean confirmed) {
            return new Authorization(actor,false,true,confirmed);
        }
    }

    /** Read-only preflight. Confirmation must repeat every check in donate()'s owner transaction. */
    public Optional<Entry> reviewDonation(BuildingLayout layout,UUID donor)throws SQLException {
        Objects.requireNonNull(layout);Objects.requireNonNull(donor);
        var old=find(layout.id());
        if(old.isPresent()) {
            if(!old.get().layout().equals(layout) || !old.get().donor().equals(donor))
                throw new Declined("Building identity/layout/donor differs from saved registration");
            if(old.get().latest()!=null && old.get().latest().action()==Action.DONATE)throw new Declined("Building is already donated");
        }
        requireClear(layout);return old;
    }
    public record Consent(UUID operation, UUID building, UUID actor, Action action, UUID previous,
                          boolean administrator, long atMillis) {}
    public record Entry(BuildingLayout layout, UUID donor, long createdMillis, Consent latest) {}
    public record Exclusion(UUID id, UUID world, UUID actor, Bounds bounds, long createdMillis) {
        public Exclusion {
            Objects.requireNonNull(id);Objects.requireNonNull(world);Objects.requireNonNull(actor);Objects.requireNonNull(bounds);time(createdMillis);
        }
    }

    static void createSchema(Connection c) throws SQLException {
        try (var s = c.createStatement()) {
            s.execute("""
                CREATE TABLE building_records (
                  id TEXT PRIMARY KEY NOT NULL, settlement_id TEXT NOT NULL, world_id TEXT NOT NULL,
                  donor_id TEXT NOT NULL, layout_hash TEXT NOT NULL CHECK(length(layout_hash)=64),
                  layout_snapshot BLOB NOT NULL CHECK(typeof(layout_snapshot)='blob' AND length(layout_snapshot) BETWEEN 1 AND 131072),
                  min_x INTEGER NOT NULL CHECK(typeof(min_x)='integer' AND min_x BETWEEN -2147483648 AND 2147483647),
                  min_y INTEGER NOT NULL CHECK(typeof(min_y)='integer' AND min_y BETWEEN -2147483648 AND 2147483647),
                  min_z INTEGER NOT NULL CHECK(typeof(min_z)='integer' AND min_z BETWEEN -2147483648 AND 2147483647),
                  max_x INTEGER NOT NULL CHECK(typeof(max_x)='integer' AND max_x BETWEEN min_x AND min_x+511 AND max_x<=2147483647),
                  max_y INTEGER NOT NULL CHECK(typeof(max_y)='integer' AND max_y BETWEEN min_y AND min_y+511 AND max_y<=2147483647),
                  max_z INTEGER NOT NULL CHECK(typeof(max_z)='integer' AND max_z BETWEEN min_z AND min_z+511 AND max_z<=2147483647),
                  created_ms INTEGER NOT NULL CHECK(typeof(created_ms)='integer' AND created_ms>=0),
                  CHECK(min_x<=max_x AND min_y<=max_y AND min_z<=max_z),
                  FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
                )
                """);
            s.execute("CREATE INDEX building_plots_by_world ON building_records(world_id,min_x,max_x,min_z,max_z)");
            s.execute("""
                CREATE TABLE building_units (
                  id TEXT PRIMARY KEY NOT NULL, building_id TEXT NOT NULL REFERENCES building_records(id),
                  declared_beds INTEGER NOT NULL CHECK(typeof(declared_beds)='integer' AND declared_beds BETWEEN 1 AND 512)
                )
                """);
            s.execute("CREATE INDEX building_units_by_building ON building_units(building_id,id)");
            s.execute("""
                CREATE TABLE building_consent_events (
                  sequence INTEGER PRIMARY KEY AUTOINCREMENT, operation_id TEXT NOT NULL UNIQUE,
                  building_id TEXT NOT NULL REFERENCES building_records(id), actor_id TEXT NOT NULL,
                  action TEXT NOT NULL CHECK(action IN ('DONATE','RECLAIM')),
                  previous_operation TEXT REFERENCES building_consent_events(operation_id),
                  administrator INTEGER NOT NULL CHECK(typeof(administrator)='integer' AND administrator IN (0,1)),
                  at_ms INTEGER NOT NULL CHECK(typeof(at_ms)='integer' AND at_ms>=0)
                )
                """);
            s.execute("CREATE INDEX building_consent_by_building ON building_consent_events(building_id,sequence)");
            s.execute("""
                CREATE TABLE building_exclusions (
                  id TEXT PRIMARY KEY NOT NULL, world_id TEXT NOT NULL REFERENCES observed_worlds(id), actor_id TEXT NOT NULL,
                  min_x INTEGER NOT NULL CHECK(typeof(min_x)='integer' AND min_x BETWEEN -2147483648 AND 2147483647),
                  min_y INTEGER NOT NULL CHECK(typeof(min_y)='integer' AND min_y BETWEEN -2147483648 AND 2147483647),
                  min_z INTEGER NOT NULL CHECK(typeof(min_z)='integer' AND min_z BETWEEN -2147483648 AND 2147483647),
                  max_x INTEGER NOT NULL CHECK(typeof(max_x)='integer' AND max_x BETWEEN min_x AND min_x+511 AND max_x<=2147483647),
                  max_y INTEGER NOT NULL CHECK(typeof(max_y)='integer' AND max_y BETWEEN min_y AND min_y+511 AND max_y<=2147483647),
                  max_z INTEGER NOT NULL CHECK(typeof(max_z)='integer' AND max_z BETWEEN min_z AND min_z+511 AND max_z<=2147483647),
                  created_ms INTEGER NOT NULL CHECK(typeof(created_ms)='integer' AND created_ms>=0),
                  CHECK(min_x<=max_x AND min_y<=max_y AND min_z<=max_z)
                )
                """);
            s.execute("CREATE INDEX building_exclusions_by_world ON building_exclusions(world_id,min_x,max_x,min_z,max_z)");
            s.execute("""
                CREATE TABLE building_exclusion_releases (
                  operation_id TEXT PRIMARY KEY NOT NULL, exclusion_id TEXT NOT NULL UNIQUE REFERENCES building_exclusions(id),
                  actor_id TEXT NOT NULL, administrator INTEGER NOT NULL CHECK(typeof(administrator)='integer' AND administrator IN (0,1)),
                  released_ms INTEGER NOT NULL CHECK(typeof(released_ms)='integer' AND released_ms>=0)
                )
                """);
        }
    }

    /** Explicit reviewed donation only. Layout/identity are immutable; a later redonation uses the same saved layout. */
    public Consent donate(UUID operation, BuildingLayout layout, UUID donor, Authorization authority,
                          UUID expectedPrevious, long now) throws SQLException {
        Objects.requireNonNull(operation); Objects.requireNonNull(layout); Objects.requireNonNull(donor);
        requireAuthority(authority, donor, true); time(now);
        return transaction(() -> {
            var prior = find(layout.id()); var replay = event(operation);
            if (prior.isPresent() && (!prior.get().layout().equals(layout) || !prior.get().donor().equals(donor)))
                throw new Declined("Building identity/layout/donor differs from saved registration");
            var requested = new Consent(operation,layout.id(),authority.actor(),Action.DONATE,expectedPrevious,authority.administrator(),now);
            if (replay.isPresent()) { exact(replay.get(),requested); return replay.get(); }
            Consent latest = prior.map(Entry::latest).orElse(null);
            requirePrevious(latest,expectedPrevious,now);
            if (latest != null && latest.action() == Action.DONATE) throw new Declined("Building is already donated");
            requireClear(layout);
            if (prior.isEmpty()) insert(layout,donor,now);
            append(requested); return requested;
        });
    }

    /** Withdraw use rights, retaining layout, units and every consent receipt. No world/entity/economic mutations. */
    public Consent reclaim(UUID operation, UUID building, Authorization authority, UUID expectedPrevious, long now) throws SQLException {
        Objects.requireNonNull(operation); Objects.requireNonNull(building); time(now);
        return transaction(() -> {
            var saved = find(building).orElseThrow(() -> new Declined("Unknown building"));
            requireAuthority(authority,saved.donor(),false);
            var requested = new Consent(operation,building,authority.actor(),Action.RECLAIM,expectedPrevious,authority.administrator(),now);
            var replay = event(operation); if (replay.isPresent()) { exact(replay.get(),requested); return replay.get(); }
            requirePrevious(saved.latest(),expectedPrevious,now);
            if (saved.latest() == null || saved.latest().action() != Action.DONATE) throw new Declined("Building has no active donation to reclaim");
            append(requested); return requested;
        });
    }

    public Optional<Entry> find(UUID building) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM building_records WHERE id=?")) {
            s.setString(1,building.toString()); try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                try {
                    var layout = BuildingLayoutSnapshot.decode(r.getBytes("layout_snapshot"));
                    if (!layout.id().equals(building) || !layout.settlement().toString().equals(r.getString("settlement_id"))
                            || !layout.world().toString().equals(r.getString("world_id"))
                            || !layout.fingerprint().equals(r.getString("layout_hash")) || !layout.plot().equals(bounds(r)))
                        throw new SQLException("Saved building layout/index mismatch");
                    var units = new TreeMap<UUID,Integer>();
                    try (var q = connection.prepareStatement("SELECT id,declared_beds FROM building_units WHERE building_id=? LIMIT 65")) {
                        q.setString(1,building.toString()); try (var rows = q.executeQuery()) {
                            while(rows.next()) {
                                if(units.size()>=BuildingLayout.MAX_UNITS)throw new SQLException("Saved dwelling index exceeds building budget");
                                units.put(UUID.fromString(rows.getString(1)),rows.getInt(2));
                            }
                        }
                    }
                    var expected = new TreeMap<UUID,Integer>(); layout.units().forEach(u -> expected.put(u.id(),u.declaredBeds()));
                    if (!expected.equals(units)) throw new SQLException("Saved dwelling index differs from frozen layout");
                    Consent latest = null;
                    try (var q = connection.prepareStatement("SELECT * FROM building_consent_events WHERE building_id=? ORDER BY sequence DESC LIMIT 1")) {
                        q.setString(1,building.toString()); try (var rows = q.executeQuery()) { if(rows.next()) latest = consent(rows); }
                    }
                    return Optional.of(new Entry(layout,UUID.fromString(r.getString("donor_id")),r.getLong("created_ms"),latest));
                } catch (IOException | IllegalArgumentException invalid) { throw new SQLException("Invalid saved building layout",invalid); }
            }
        }
    }

    /** Permission only. Current habitability/occupancy and world presence must be checked separately. */
    public boolean hasDonationPermission(UUID building) throws SQLException {
        var entry = find(building);
        return entry.isPresent() && entry.get().latest() != null && entry.get().latest().action() == Action.DONATE
                && !excluded(entry.get().layout().world(),entry.get().layout().plot());
    }

    /** Explicit protection can cover an old structure without inventing its use, donor or dwelling units. */
    public Exclusion exclude(Exclusion exclusion, Authorization authority) throws SQLException {
        Objects.requireNonNull(exclusion); time(exclusion.createdMillis());
        requireAuthority(authority,exclusion.actor(),true);
        return transaction(() -> {
            var old = exclusion(exclusion.id());
            if (old.isPresent()) { if(!old.get().equals(exclusion)) throw new Declined("Exclusion identity changed"); return old.get(); }
            requireExclusionRights(exclusion,authority);
            try (var s = connection.prepareStatement("INSERT INTO building_exclusions VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,exclusion.id().toString());s.setString(2,exclusion.world().toString());s.setString(3,exclusion.actor().toString());
                bindBounds(s,4,exclusion.bounds());s.setLong(10,exclusion.createdMillis());s.executeUpdate();
            }
            return exclusion;
        });
    }

    public void releaseExclusion(UUID operation, UUID exclusion, Authorization authority, long now) throws SQLException {
        Objects.requireNonNull(operation); time(now);
        transaction(() -> {
            var saved = exclusion(exclusion).orElseThrow(() -> new Declined("Unknown exclusion"));
            requireAuthority(authority,saved.actor(),false);
            if(now < saved.createdMillis()) throw new Declined("Exclusion release precedes protection");
            try (var s = connection.prepareStatement("SELECT * FROM building_exclusion_releases WHERE operation_id=? OR exclusion_id=?")) {
                s.setString(1,operation.toString());s.setString(2,exclusion.toString());try(var r=s.executeQuery()) {
                    if(r.next()) {
                        if(!operation.toString().equals(r.getString("operation_id")) || !exclusion.toString().equals(r.getString("exclusion_id"))
                                || !authority.actor().toString().equals(r.getString("actor_id")) || authority.administrator()!=r.getBoolean("administrator")
                                || now!=r.getLong("released_ms")) throw new Declined("Exclusion already released or release terms changed");
                        return null;
                    }
                }
            }
            try(var s=connection.prepareStatement("INSERT INTO building_exclusion_releases VALUES(?,?,?,?,?)")) {
                s.setString(1,operation.toString());s.setString(2,exclusion.toString());s.setString(3,authority.actor().toString());
                s.setBoolean(4,authority.administrator());s.setLong(5,now);s.executeUpdate();
            }
            return null;
        });
    }

    public boolean excluded(UUID world, Bounds bounds) throws SQLException {
        try(var s=connection.prepareStatement("""
            SELECT 1 FROM building_exclusions e WHERE world_id=? AND min_x<=? AND max_x>=? AND min_z<=? AND max_z>=?
              AND NOT EXISTS(SELECT 1 FROM building_exclusion_releases r WHERE r.exclusion_id=e.id) LIMIT 1
            """)) {
            area(s,world,bounds);try(var r=s.executeQuery()){return r.next();}
        }
    }
    public Optional<Exclusion> findExclusion(UUID id)throws SQLException{return exclusion(id);}
    /** Bounded active IDs only; no snapshot/world scan and no inference of building ownership. */
    public List<UUID> exclusionsAt(UUID world,int x,int z)throws SQLException {
        var result=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT e.id FROM building_exclusions e WHERE world_id=? AND min_x<=? AND max_x>=? AND min_z<=? AND max_z>=?
              AND NOT EXISTS(SELECT 1 FROM building_exclusion_releases r WHERE r.exclusion_id=e.id) ORDER BY e.id LIMIT 33
            """)) {
            s.setString(1,world.toString());s.setInt(2,x);s.setInt(3,x);s.setInt(4,z);s.setInt(5,z);
            try(var r=s.executeQuery()){while(r.next()){
                if(result.size()==32)throw new Declined("Too many overlapping building exclusions; ask an administrator to review");
                result.add(UUID.fromString(r.getString(1)));
            }}
        }
        return List.copyOf(result);
    }
    private void requireExclusionRights(Exclusion e,Authorization authority)throws SQLException {
        if(authority.administrator())return;
        for(String sql:List.of(
                "SELECT 1 FROM building_records WHERE world_id=? AND min_x<=? AND max_x>=? AND min_z<=? AND max_z>=? AND donor_id<>? LIMIT 1",
                "SELECT 1 FROM building_exclusions e WHERE world_id=? AND min_x<=? AND max_x>=? AND min_z<=? AND max_z>=? AND actor_id<>? AND NOT EXISTS(SELECT 1 FROM building_exclusion_releases r WHERE r.exclusion_id=e.id) LIMIT 1",
                "SELECT 1 FROM protected_pen_columns c JOIN active_pen_claims a ON a.pen_id=c.pen_id JOIN protected_pens p ON p.id=c.pen_id WHERE c.world_id=? AND c.x<=? AND c.x>=? AND c.z<=? AND c.z>=? AND p.owner_id<>? LIMIT 1",
                "SELECT 1 FROM construction_site_reservations WHERE world_id=? AND min_x<=? AND max_x>=? AND min_z<=? AND max_z>=? AND released_ms IS NULL LIMIT 1")) {
            try(var s=connection.prepareStatement(sql)) {
                area(s,e.world(),e.bounds());if(!sql.contains("construction_site_reservations"))s.setString(6,e.actor().toString());
                try(var r=s.executeQuery()){if(r.next())throw new Declined("Plot overlaps another recorded owner or reserved construction; ask an administrator to review");}
            }
        }
    }

    private void requireClear(BuildingLayout layout) throws SQLException {
        UUID world=layout.world();Bounds b=layout.plot();
        if(excluded(world,b)) throw new Declined("Building plot is explicitly excluded");
        for(String sql:List.of(
                "SELECT 1 FROM protected_pen_columns p JOIN active_pen_claims a ON a.pen_id=p.pen_id WHERE world_id=? AND x<=? AND x>=? AND z<=? AND z>=? LIMIT 1",
                "SELECT 1 FROM construction_site_reservations WHERE world_id=? AND max_x>=? AND min_x<=? AND max_z>=? AND min_z<=? AND released_ms IS NULL LIMIT 1")) {
            try(var s=connection.prepareStatement(sql)) {
                if(sql.contains("construction_site")) { s.setString(1,world.toString());s.setInt(2,b.min().x());s.setInt(3,b.max().x());s.setInt(4,b.min().z());s.setInt(5,b.max().z()); }
                else area(s,world,b);
                try(var r=s.executeQuery()){if(r.next()) throw new Declined("Building plot overlaps saved protection or construction");}
            }
        }
        try(var s=connection.prepareStatement("SELECT id FROM building_records WHERE world_id=? AND min_x<=? AND max_x>=? AND min_z<=? AND max_z>=? AND id<>? LIMIT 1")) {
            area(s,world,b);s.setString(6,layout.id().toString());try(var r=s.executeQuery()){if(r.next())throw new Declined("Building plots overlap");}
        }
    }
    private void insert(BuildingLayout layout, UUID donor, long now) throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO building_records VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,layout.id().toString());s.setString(2,layout.settlement().toString());s.setString(3,layout.world().toString());s.setString(4,donor.toString());
            s.setString(5,layout.fingerprint());s.setBytes(6,BuildingLayoutSnapshot.encode(layout));bindBounds(s,7,layout.plot());s.setLong(13,now);s.executeUpdate();
        }
        try(var s=connection.prepareStatement("INSERT INTO building_units VALUES(?,?,?)")) {
            for(var unit:layout.units()){s.setString(1,unit.id().toString());s.setString(2,layout.id().toString());s.setInt(3,unit.declaredBeds());s.executeUpdate();}
        }
    }
    private Optional<Consent> event(UUID operation) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM building_consent_events WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(consent(r)):Optional.empty();}
        }
    }
    private void append(Consent event) throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO building_consent_events(operation_id,building_id,actor_id,action,previous_operation,administrator,at_ms) VALUES(?,?,?,?,?,?,?)")) {
            s.setString(1,event.operation().toString());s.setString(2,event.building().toString());s.setString(3,event.actor().toString());s.setString(4,event.action().name());
            s.setString(5,event.previous()==null?null:event.previous().toString());s.setBoolean(6,event.administrator());s.setLong(7,event.atMillis());s.executeUpdate();
        }
    }
    private Optional<Exclusion> exclusion(UUID id) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM building_exclusions WHERE id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(new Exclusion(id,UUID.fromString(r.getString("world_id")),UUID.fromString(r.getString("actor_id")),bounds(r),r.getLong("created_ms"))):Optional.empty();}
        }
    }
    private static Consent consent(ResultSet r) throws SQLException {
        String previous=r.getString("previous_operation");
        return new Consent(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("building_id")),UUID.fromString(r.getString("actor_id")),
                Action.valueOf(r.getString("action")),previous==null?null:UUID.fromString(previous),r.getBoolean("administrator"),r.getLong("at_ms"));
    }
    private static void exact(Consent old, Consent requested) throws SQLException { if(!old.equals(requested)) throw new Declined("Consent operation reused with changed terms"); }
    private static void requirePrevious(Consent latest, UUID expected, long now) throws SQLException {
        if(!Objects.equals(latest==null?null:latest.operation(),expected)) throw new Declined("Building consent changed; review again");
        if(latest!=null && now<latest.atMillis()) throw new Declined("Consent event precedes saved history");
    }
    private static void requireAuthority(Authorization authority, UUID owner, boolean requireOwnership) throws SQLException {
        Objects.requireNonNull(authority);
        if(!authority.confirmed() || (!authority.administrator() && !authority.actor().equals(owner))
                || (requireOwnership && !authority.ownershipConfirmed())) throw new Declined("Confirmed authenticated building authority is required");
    }
    private static void time(long now) { if(now<0) throw new IllegalArgumentException("Negative building record time"); }
    private static void area(PreparedStatement s,UUID world,Bounds b) throws SQLException {
        s.setString(1,world.toString());s.setInt(2,b.max().x());s.setInt(3,b.min().x());s.setInt(4,b.max().z());s.setInt(5,b.min().z());
    }
    private static void bindBounds(PreparedStatement s,int start,Bounds b) throws SQLException {
        s.setInt(start,b.min().x());s.setInt(start+1,b.min().y());s.setInt(start+2,b.min().z());s.setInt(start+3,b.max().x());s.setInt(start+4,b.max().y());s.setInt(start+5,b.max().z());
    }
    private static Bounds bounds(ResultSet r) throws SQLException {
        return new Bounds(new ConstructionTemplate.Position(r.getInt("min_x"),r.getInt("min_y"),r.getInt("min_z")),
                new ConstructionTemplate.Position(r.getInt("max_x"),r.getInt("max_y"),r.getInt("max_z")));
    }
    @FunctionalInterface private interface Work<T> { T run() throws SQLException; }
    private <T> T transaction(Work<T> work) throws SQLException {
        if(!connection.getAutoCommit()) throw new SQLException("Building operation requires no active owner transaction");
        connection.setAutoCommit(false);
        try { T result=work.run();connection.commit();connection.setAutoCommit(true);return result; }
        catch(SQLException|RuntimeException failure) { try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure; }
    }
}
