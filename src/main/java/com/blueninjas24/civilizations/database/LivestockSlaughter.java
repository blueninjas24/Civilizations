package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Saved exact-body permits. Goods are credited only by a confirmed intentional native death. */
public final class LivestockSlaughter {
    public static final String REASON_PREFIX="civilizations:business_slaughter:";
    public record Permit(UUID id,UUID animal,UUID business,UUID world,LivestockHerd.Species species,long period,long units,long preparedMillis) {}
    private final Connection c;
    public LivestockSlaughter(Connection connection){c=Objects.requireNonNull(connection);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE livestock_slaughter (
                    id TEXT PRIMARY KEY NOT NULL,animal_id TEXT NOT NULL REFERENCES animal_ownership(animal_id),
                    business_id TEXT NOT NULL REFERENCES livestock_herds(business_id),period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=1),
                    world_id TEXT NOT NULL CHECK(length(world_id)=36),species TEXT NOT NULL CHECK(species IN('COW','CHICKEN')),
                    meat_units INTEGER NOT NULL CHECK(typeof(meat_units)='integer' AND meat_units BETWEEN 1 AND 1000000),prepared_ms INTEGER NOT NULL CHECK(typeof(prepared_ms)='integer' AND prepared_ms>=0),
                    state TEXT NOT NULL CHECK(state IN('PENDING','CONFIRMED','CANCELLED')),loss_operation TEXT UNIQUE,
                    explanation TEXT NOT NULL,terms TEXT NOT NULL,
                    CHECK((state='CONFIRMED' AND loss_operation IS NOT NULL) OR (state<>'CONFIRMED' AND loss_operation IS NULL)),
                    FOREIGN KEY(business_id,period) REFERENCES livestock_periods(business_id,period)
                )
                """);
            s.execute("CREATE UNIQUE INDEX slaughter_active_animal ON livestock_slaughter(animal_id) WHERE state<>'CANCELLED'");
            s.execute("CREATE INDEX slaughter_business_period ON livestock_slaughter(business_id,period,state)");
        }
    }
    public boolean pending(UUID business)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM livestock_slaughter WHERE business_id=? AND state='PENDING'")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
    public int pendingCount()throws SQLException {try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM livestock_slaughter WHERE state='PENDING'")){return r.getInt(1);}}
    public Optional<Permit> prepare(LivestockPresence.Sight sight,long now,Set<UUID> worlds,LivestockPolicy policy,
            LivestockSlaughterPolicy dispatch,BusinessOperationPolicy operations,LivestockWorkingArea area,BooleanSupplier valid)throws SQLException {
        return transaction(()-> {
            if(!policy.enabled()||!dispatch.enabled()||!valid.getAsBoolean()||now<sight.observedMillis()
                    ||now-sight.observedMillis()>dispatch.evidenceMaxAgeMillis()||!worlds.contains(sight.world()))return Optional.empty();
            var member=new LivestockPresence(c).member(sight.animal());if(member.isEmpty())return Optional.empty();
            var m=member.orElseThrow();
            double dx=(double)sight.position().x()-m.home().x(),dy=(double)sight.position().y()-m.home().y(),dz=(double)sight.position().z()-m.home().z();
            if(!m.world().equals(sight.world())||m.species()!=sight.species()||m.maturePeriod()>m.herdPeriod()
                    ||dx*dx+dy*dy+dz*dz>(double)area.radiusBlocks()*area.radiusBlocks()||m.herdPeriod()<1)return Optional.empty();
            if(!new BusinessOperations(c,operations).evaluate(m.business()).permitted())return Optional.empty();
            if(new LivestockRepository(c).qualifiedWorkers(m.business(),operations)==0)return Optional.empty();
            long limit,used;
            try(var s=c.prepareStatement("SELECT p.available_work,p.work_used FROM livestock_periods p JOIN industry_runtime r ON r.business_id=p.business_id AND r.completed=p.period AND r.target=r.completed JOIN industry_runtime_history h ON h.business_id=p.business_id AND h.period=p.period WHERE p.business_id=? AND p.period=?")) {
                s.setString(1,m.business().toString());s.setLong(2,m.herdPeriod());try(var r=s.executeQuery()){if(!r.next())return Optional.empty();limit=r.getLong(1);used=r.getLong(2);}
            }
            var terms=policy.terms(m.species());
            var herd=new LivestockRepository(c).load(m.business()).orElseThrow();
            long away=new LivestockPresence(c).unavailableAdultsRows(m.business(),m.herdPeriod(),area);
            if(herd.state().adults()-away<=terms.breedingReserve()||terms.maximumSlaughterPerPeriod()==0)return Optional.empty();
            var id=UUID.nameUUIDFromBytes(("civilizations:slaughter-v1:"+m.business()+":"+m.herdPeriod()+":"+m.animal()).getBytes(StandardCharsets.UTF_8));
            try(var s=c.prepareStatement("SELECT * FROM livestock_slaughter WHERE id=?")) {
                s.setString(1,id.toString());try(var r=s.executeQuery()){if(r.next())return r.getString("state").equals("PENDING")?Optional.of(read(r)):Optional.empty();}
            }
            if(pending(m.business()))return Optional.empty();
            long spent;
            try(var s=c.prepareStatement("SELECT COUNT(*) FROM livestock_slaughter WHERE business_id=? AND period=? AND state<>'CANCELLED'")) {
                s.setString(1,m.business().toString());s.setLong(2,m.herdPeriod());try(var r=s.executeQuery()){spent=r.getLong(1);}
            }
            if(spent>=terms.maximumSlaughterPerPeriod()||limit-used-spent<1||herd.state().adults()-away<=terms.breedingReserve())return Optional.empty();
            var store=new IndustryRepository(c).store(m.business()).orElseThrow();long space=Math.multiplyExact(terms.meatUnitsPerAnimal(),store.spacePerUnit().getOrDefault(Commodity.FOOD,1L));
            if(store.freeSpace()<space||!valid.getAsBoolean())return Optional.empty();
            ensureFoodSlot(m.business());
            var permit=new Permit(id,m.animal(),m.business(),m.world(),m.species(),m.herdPeriod(),terms.meatUnitsPerAnimal(),now);
            try(var s=c.prepareStatement("INSERT INTO livestock_slaughter VALUES(?,?,?,?,?,?,?,?,'PENDING',NULL,?,?)")) {
                s.setString(1,id.toString());s.setString(2,m.animal().toString());s.setString(3,m.business().toString());s.setLong(4,m.herdPeriod());
                s.setString(5,m.world().toString());s.setString(6,m.species().name());s.setLong(7,permit.units());s.setLong(8,now);
                s.setString(9,"Exact mature body, breeding reserve, unused real care-period work and reserved food storage; awaiting native death");s.setString(10,policy+"|"+dispatch+"|"+area);s.executeUpdate();
            }
            if(!valid.getAsBoolean())throw new SQLException("Livestock capture changed during slaughter preparation");
            return Optional.of(permit);
        });
    }
    private void ensureFoodSlot(UUID business)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM livestock_slaughter WHERE business_id=? AND NOT EXISTS(SELECT 1 FROM industry_stock WHERE business_id=? AND commodity='food') LIMIT 1")) {
            s.setString(1,business.toString());s.setString(2,business.toString());try(var r=s.executeQuery()){if(r.next())throw new SQLException("Recorded slaughter food slot disappeared; reviewed recovery required");}
        }
        try(var s=c.prepareStatement("INSERT INTO industry_stock(business_id,commodity,space_per_unit,units) VALUES(?,'food',1,0) ON CONFLICT(business_id,commodity) DO NOTHING")) {
            s.setString(1,business.toString());s.executeUpdate();
        }
    }
    public void cancel(UUID permit,String reason)throws SQLException {
        if(reason==null||reason.isBlank()||reason.length()>256)throw new IllegalArgumentException("Invalid cancellation explanation");
        transaction(()->{try(var s=c.prepareStatement("UPDATE livestock_slaughter SET state='CANCELLED',explanation=? WHERE id=? AND state='PENDING'")) {
            s.setString(1,reason);s.setString(2,permit.toString());s.executeUpdate();}return null;});
    }
    /** Composes with the existing exact native-loss receipt in the same transaction. */
    void observedLossRows(LivestockLosses.Observation loss,Optional<LivestockLosses.Receipt> receipt)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Slaughter confirmation requires the native loss transaction");
        if(!loss.reason().startsWith(REASON_PREFIX)) {
            try(var s=c.prepareStatement("UPDATE livestock_slaughter SET state='CANCELLED',explanation='Ordinary actual death; normal native loot, no business goods' WHERE animal_id=? AND state='PENDING'")) {s.setString(1,loss.animal().toString());s.executeUpdate();}
            return;
        }
        UUID id;try{id=UUID.fromString(loss.reason().substring(REASON_PREFIX.length()));}catch(IllegalArgumentException bad){throw new SQLException("Invalid intentional death permit",bad);}
        try(var s=c.prepareStatement("SELECT * FROM livestock_slaughter WHERE id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()) {
                if(!r.next())throw new SQLException("Unknown intentional death permit");var permit=read(r);
                if(!permit.animal().equals(loss.animal())||!permit.world().equals(loss.observedWorld())||receipt.isEmpty()
                        ||!permit.business().equals(receipt.orElseThrow().business())||loss.observedMillis()<permit.preparedMillis())throw new SQLException("Intentional native death differs from saved permit");
                if(r.getString("state").equals("CONFIRMED")){if(!loss.operation().toString().equals(r.getString("loss_operation")))throw new SQLException("Changed slaughter death receipt");return;}
                if(!r.getString("state").equals("PENDING"))throw new SQLException("Cancelled slaughter cannot credit goods");
                try(var q=c.prepareStatement("UPDATE livestock_slaughter SET state='CONFIRMED',loss_operation=?,explanation='Confirmed intentional native death; goods credited exactly once, native drops suppressed' WHERE id=? AND state='PENDING'")) {
                    q.setString(1,loss.operation().toString());q.setString(2,id.toString());if(q.executeUpdate()!=1)throw new SQLException("Slaughter permit changed");
                }
                try(var q=c.prepareStatement("UPDATE industry_stock SET units=units+? WHERE business_id=? AND commodity='food'")) {
                    q.setLong(1,permit.units());q.setString(2,permit.business().toString());if(q.executeUpdate()!=1)throw new SQLException("Reserved slaughter stock slot missing");
                }
                new IndustryRepository(c).store(permit.business()).orElseThrow();
            }
        }
    }
    private static Permit read(ResultSet r)throws SQLException {return new Permit(UUID.fromString(r.getString("id")),UUID.fromString(r.getString("animal_id")),UUID.fromString(r.getString("business_id")),UUID.fromString(r.getString("world_id")),LivestockHerd.Species.valueOf(r.getString("species")),r.getLong("period"),r.getLong("meat_units"),r.getLong("prepared_ms"));}
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!c.getAutoCommit())throw new SQLException("Slaughter requires idle SQL owner");c.setAutoCommit(false);
        try{var result=work.run();c.commit();c.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
