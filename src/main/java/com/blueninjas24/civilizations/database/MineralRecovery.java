package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Explicit, resource-scoped recovery. Current evidence cannot reconstruct edits during a gap. */
public final class MineralRecovery {
    public static final int MAXIMUM_SOURCES = 4096;
    public record Source(Position position, long remaining, boolean excluded) {}
    public record Plan(UUID deposit, UUID world, String mineral, UUID generation,
                       long remaining, List<Source> sources) {
        public Plan { sources = List.copyOf(sources); }
    }
    public record Receipt(UUID operation, UUID deposit, long before, long lost, long after) {}
    private final Connection c;
    public MineralRecovery(Connection c) { this.c = Objects.requireNonNull(c); }
    static void createSchema(Connection c) throws SQLException {
        ResourceRecoveryEpochs.createSchema(c);
        try (var s = c.createStatement()) {
            s.execute("""
                CREATE TABLE mineral_recoveries (
                    operation_id TEXT PRIMARY KEY NOT NULL,
                    deposit_id TEXT NOT NULL REFERENCES mineral_deposits(id), generation TEXT NOT NULL,
                    actor TEXT NOT NULL CHECK(length(trim(actor)) BETWEEN 1 AND 80),
                    observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                    before_batches INTEGER NOT NULL CHECK(typeof(before_batches)='integer' AND before_batches>=0),
                    lost_batches INTEGER NOT NULL CHECK(typeof(lost_batches)='integer' AND lost_batches BETWEEN 0 AND before_batches),
                    after_batches INTEGER NOT NULL CHECK(after_batches=before_batches-lost_batches),
                    evidence TEXT NOT NULL CHECK(length(evidence)>0)
                )
                """);
            s.execute("CREATE INDEX mineral_recovery_scope ON mineral_recoveries(deposit_id,generation)");
        }
    }
    public List<UUID> deposits() throws SQLException {
        var ids = new ArrayList<UUID>();
        try (var s = c.createStatement(); var r = s.executeQuery("SELECT deposit_id FROM mineral_discovery ORDER BY deposit_id LIMIT 33")) {
            while (r.next()) ids.add(UUID.fromString(r.getString(1)));
        }
        return List.copyOf(ids);
    }
    /** Read only, including legacy even-share preview; no adoption until explicit confirmation. */
    public Plan prepare(UUID deposit) throws SQLException {
        var epoch = new ResourceRecoveryEpochs(c).load("ore").filter(ResourceRecoveryEpochs.State::healthy)
                .orElseThrow(() -> new SQLException("Current ore capture is unavailable; restart with working capture before reviewing"));
        UUID world; String mineral; long remaining;
        try (var s = c.prepareStatement("SELECT d.world_id,d.mineral,m.remaining_batches FROM mineral_discovery d JOIN mineral_deposits m ON m.id=d.deposit_id WHERE d.deposit_id=?")) {
            s.setString(1, deposit.toString());
            try (var r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("Saved coordinate-backed deposit not found");
                world=UUID.fromString(r.getString(1)); mineral=r.getString(2); remaining=r.getLong(3);
            }
        }
        var positions = new ArrayList<Position>();
        try (var s = c.prepareStatement("SELECT world_id,x,y,z FROM mineral_evidence WHERE deposit_id=? ORDER BY world_id,x,y,z LIMIT ?")) {
            s.setString(1,deposit.toString()); s.setInt(2,MAXIMUM_SOURCES+1);
            try (var r = s.executeQuery()) { while(r.next()) {
                if(positions.size()==MAXIMUM_SOURCES)throw new SQLException("Recovery source budget exceeded; no partial approval");
                if(!world.toString().equals(r.getString(1)))throw new SQLException("Resource evidence world mismatch");
                positions.add(new Position(r.getInt(2),r.getInt(3),r.getInt(4)));
            } }
        }
        if(positions.isEmpty())throw new SQLException("No saved source coordinates; recovery cannot invent them");
        var shares = new MineralSources(c).shares(deposit);
        boolean adopted;
        try(var s=c.prepareStatement("SELECT 1 FROM mineral_source_adoptions WHERE deposit_id=?")) {
            s.setString(1,deposit.toString());try(var r=s.executeQuery()){adopted=r.next();}
        }
        if(!adopted&&!shares.isEmpty())throw new SQLException("Unadopted source rows require investigation");
        var balances = new HashMap<Position,Long>();
        for(var share:shares) {
            if(!share.world().equals(world)||!positions.contains(share.position()))throw new SQLException("Resource source/evidence mismatch");
            balances.put(share.position(),share.remaining());
        }
        var result = new ArrayList<Source>(); long total=0; int i=0;
        for(var p:positions) {
            long balance=adopted?balances.getOrDefault(p,0L):remaining/positions.size()+(i<remaining%positions.size()?1:0);
            total=Math.addExact(total,balance); i++;
            result.add(new Source(p,balance,new OrePlacementRepository(c).excludes(world,p)));
        }
        if(total!=remaining)throw new SQLException("Resource shares disagree with reserve");
        return new Plan(deposit,world,mineral,epoch.generation(),remaining,result);
    }
    public boolean permitted(UUID deposit) throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM mineral_recoveries r JOIN resource_recovery_epochs e ON e.kind='ore' AND e.generation=r.generation AND e.healthy=1 WHERE r.deposit_id=? LIMIT 1")) {
            s.setString(1,deposit.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
    /** All saved coordinates must be observed; false means confirmed changed, never unloaded/unknown. */
    public Receipt approve(UUID operation,String actor,Plan plan,Map<Position,Boolean> current,
                           long observed,long now,BooleanSupplier captureHealthy) throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(plan);Objects.requireNonNull(captureHealthy);
        var evidence=Map.copyOf(current);
        if(actor==null||actor.isBlank()||actor.length()>80||observed<0||now<observed||now-observed>30_000)
            throw new IllegalArgumentException("Invalid or expired recovery approval");
        var expected=new HashSet<Position>();plan.sources().forEach(s->expected.add(s.position()));
        if(!expected.equals(evidence.keySet()))throw new IllegalArgumentException("Complete exact resource evidence required");
        if(!c.getAutoCommit())throw new SQLException("Recovery requires idle SQL owner");
        c.setAutoCommit(false);
        try {
            // An operation ID is single use, including successful retries with new evidence.
            try(var s=c.prepareStatement("SELECT 1 FROM mineral_recoveries WHERE operation_id=?")) {
                s.setString(1,operation.toString());try(var r=s.executeQuery()){if(r.next())throw new SQLException("Recovery approval already applied; request a fresh review");}
            }
            if(!captureHealthy.getAsBoolean()||!prepare(plan.deposit()).equals(plan))
                throw new SQLException("Capture or saved resource changed; request a fresh review");
            new MineralSources(c).ensureRows(plan.deposit());
            var lost=plan.sources().stream().filter(s->s.excluded()||!evidence.get(s.position())).map(Source::position).toList();
            long removed=0;
            for(int from=0;from<lost.size();from+=512)
                removed=Math.addExact(removed,new MineralSources(c).removeRows(plan.world(),lost.subList(from,Math.min(lost.size(),from+512)),now,"Explicit admin-approved current resource review"));
            var lostSet=Set.copyOf(lost);
            if(new MineralSources(c).shares(plan.deposit()).stream().anyMatch(s->lostSet.contains(s.position())&&s.remaining()!=0))
                throw new SQLException("Excluded source retains reserve; historical inconsistency requires investigation");
            long after=new IndustryRepository(c).deposit(plan.deposit()).orElseThrow().remainingBatches();
            if(after!=Math.subtractExact(plan.remaining(),removed))throw new SQLException("Recovery reserve mismatch");
            // Source IDs and zero-share retired coordinates remain recorded; audit records exact verdicts.
            try(var s=c.prepareStatement("INSERT INTO mineral_recoveries VALUES(?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,operation.toString());s.setString(2,plan.deposit().toString());s.setString(3,plan.generation().toString());s.setString(4,actor);
                s.setLong(5,now);s.setLong(6,plan.remaining());s.setLong(7,removed);s.setLong(8,after);
                var ordered=new TreeMap<Position,Boolean>(Position.ORDER);ordered.putAll(evidence);
                s.setString(9,plan+"; current="+ordered);
                s.executeUpdate();
            }
            if(!captureHealthy.getAsBoolean())throw new SQLException("Capture failed before recovery commit");
            c.commit();c.setAutoCommit(true);return new Receipt(operation,plan.deposit(),plan.remaining(),removed,after);
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
