package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.simulation.CivilizationTime;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Owner-thread online clocks and bounded atomic work for existing funded/staffed industries. */
public final class IndustryRuntime {
    private record Clock(UUID business, UUID world, String industry, long cadence,
                         CivilizationTime.Anchor anchor, long completed) {}
    private final Connection connection;
    private final IndustryRuntimePolicy policy;
    private final BusinessOperationPolicy operations;
    private final WorkLearningPolicy learning;
    private final java.util.function.BooleanSupplier mutationHealthy;
    private String cursor = "";
    public IndustryRuntime(Connection connection, IndustryRuntimePolicy policy) {
        this(connection,policy,BusinessOperationPolicy.disabled());
    }
    public IndustryRuntime(Connection connection, IndustryRuntimePolicy policy,BusinessOperationPolicy operations) {
        this(connection,policy,operations,WorkLearningPolicy.disabled());
    }
    public IndustryRuntime(Connection connection, IndustryRuntimePolicy policy,BusinessOperationPolicy operations,WorkLearningPolicy learning) {
        this(connection,policy,operations,learning,()->true);
    }
    public IndustryRuntime(Connection connection, IndustryRuntimePolicy policy,BusinessOperationPolicy operations,WorkLearningPolicy learning,java.util.function.BooleanSupplier mutationHealthy) {
        this.connection=Objects.requireNonNull(connection); this.policy=Objects.requireNonNull(policy);
        this.operations=Objects.requireNonNull(operations);
        this.learning=Objects.requireNonNull(learning);
        this.mutationHealthy=Objects.requireNonNull(mutationHealthy);
    }
    /** Restart preserves earned work/fractions but earns no downtime. */
    public void start(long now) throws SQLException {
        if (!policy.enabled()) return;
        observe(now,null,true);
    }
    public int pulse(long now, Set<UUID> worlds) throws SQLException {
        if (!policy.enabled()) return 0;
        worlds=worlds==null?null:Set.copyOf(worlds);
        observe(now,worlds,false);
        var pending=new ArrayList<Clock>();
        for(var c:clocks()) if(c.completed()<c.anchor().targetSteps() && (worlds==null || worlds.contains(c.world()))) pending.add(c);
        pending.sort(Comparator.comparingInt((Clock c)->c.business().toString().compareTo(cursor)>0?0:1).thenComparing(c->c.business().toString()));
        int done=0;
        for(var c:pending) {
            if(done>=policy.businessesPerPulse()) break;
            work(c);cursor=c.business().toString();done++;
        }
        return done;
    }
    /** Record the old world's earned interval without executing production at the boundary. */
    public void observeAvailability(long now, Set<UUID> worlds) throws SQLException {
        if(policy.enabled())observe(now,worlds==null?null:Set.copyOf(worlds),false);
    }
    private void observe(long now, Set<UUID> worlds, boolean restart) throws SQLException {
        if(now<0)throw new IllegalArgumentException("Invalid industry observation time");
        transaction(()-> {
            // New firms begin now, above any legacy manual production periods. Never backfill pre-enrollment.
            var candidates=new ArrayList<UUID>();
            int scanned=0;
            try(var s=connection.createStatement();var r=s.executeQuery("SELECT b.id,b.industry FROM businesses b JOIN industry_stores k ON k.business_id=b.id WHERE NOT EXISTS(SELECT 1 FROM industry_runtime c WHERE c.business_id=b.id) ORDER BY b.id LIMIT 4097")) {
                while(r.next()) {scanned++;if(supported(r.getString(2)))candidates.add(UUID.fromString(r.getString(1)));}
            }
            if(scanned>4096)throw new SQLException("Industry clock enrollment budget exceeded");
            for(var id:candidates)try(var s=connection.prepareStatement("INSERT INTO industry_runtime SELECT ?,?,?,0,COALESCE(MAX(period),0),COALESCE(MAX(period),0) FROM industry_production WHERE business_id=?")) {
                s.setString(1,id.toString());s.setLong(2,policy.millisPerPeriod());s.setLong(3,now);s.setString(4,id.toString());s.executeUpdate();
            }
            for(var c:clocks()) {
                long cadence=c.cadence();var before=c.anchor();
                if(cadence!=policy.millisPerPeriod()) {
                    if(c.completed()!=before.targetSteps())throw new SQLException("Finish queued industry periods under old cadence before retuning");
                    try(var s=connection.prepareStatement("INSERT INTO industry_runtime_tuning(business_id,after_period,old_cadence,new_cadence,discarded_remainder,observed_ms) VALUES(?,?,?,?,?,?)")) {
                        s.setString(1,c.business().toString());s.setLong(2,c.completed());s.setLong(3,cadence);s.setLong(4,policy.millisPerPeriod());s.setLong(5,before.remainderMillis());s.setLong(6,now);s.executeUpdate();
                    }
                    cadence=policy.millisPerPeriod();before=new CivilizationTime.Anchor(Math.max(now,before.lastObservedMillis()),0,before.targetSteps());
                }
                var after=restart?new CivilizationTime.Anchor(Math.max(now,before.lastObservedMillis()),before.remainderMillis(),before.targetSteps())
                        :CivilizationTime.accrue(before,now,cadence,worlds==null || worlds.contains(c.world())?Long.MAX_VALUE:0).anchor();
                try(var s=connection.prepareStatement("UPDATE industry_runtime SET cadence=?,last_ms=?,remainder=?,target=? WHERE business_id=?")) {
                    s.setLong(1,cadence);s.setLong(2,after.lastObservedMillis());s.setLong(3,after.remainderMillis());s.setLong(4,after.targetSteps());s.setString(5,c.business().toString());s.executeUpdate();
                }
            }
            return null;
        });
    }
    private boolean supported(String kind) {return kind.equals("mining") || (kind.equals("warehouse") && policy.warehouseSupply().enabled()) || policy.recipes().values().stream().anyMatch(r->r.industry().equals(kind));}
    private List<Clock> clocks() throws SQLException {
        var result=new ArrayList<Clock>();
        try(var s=connection.createStatement();var r=s.executeQuery("SELECT c.*,b.industry,p.world_id FROM industry_runtime c JOIN businesses b ON b.id=c.business_id JOIN business_sites p ON p.id=b.site_id ORDER BY c.business_id LIMIT 4097")) {
            while(r.next()) result.add(new Clock(UUID.fromString(r.getString("business_id")),UUID.fromString(r.getString("world_id")),r.getString("industry"),r.getLong("cadence"),
                    new CivilizationTime.Anchor(r.getLong("last_ms"),r.getLong("remainder"),r.getLong("target")),r.getLong("completed")));
        }
        if(result.size()>4096)throw new SQLException("Industry clock budget exceeded");
        return List.copyOf(result);
    }
    private void work(Clock clock) throws SQLException {
        transaction(()-> {
            long period=Math.incrementExact(clock.completed());
            var id=clock.business();
            String reason;
            var availability=new BusinessOperations(connection,operations).evaluate(id);
            var industry=new IndustryRepository(connection);
            var supplies=new WorkshopSupplies(connection,operations);
            var tools=new BusinessTools(connection,policy);
            int arrived=supplies.deliverRows(id,period);
            var operation=UUID.nameUUIDFromBytes(("civilizations:timed-industry-v1:"+id+":"+period).getBytes(StandardCharsets.UTF_8));
            boolean freshProduction;
            try(var s=connection.prepareStatement("SELECT 1 FROM industry_operations WHERE id=?")) {
                s.setString(1,operation.toString());try(var r=s.executeQuery()){freshProduction=!r.next();}
            }
            if(!availability.permitted())reason=availability.explanation();
            else if(clock.industry().equals("mining")) {
                var deposits=new ArrayList<UUID>();
                try(var s=connection.prepareStatement("SELECT d.id FROM mineral_deposits d JOIN businesses b ON b.site_id=d.site_id WHERE b.id=? ORDER BY d.id LIMIT 2")) {
                    s.setString(1,id.toString());try(var r=s.executeQuery()){while(r.next())deposits.add(UUID.fromString(r.getString(1)));}
                }
                if(deposits.size()!=1)reason="Paused: one unambiguous saved finite deposit required";
                else if(!industry.sourceHistoryTrusted(deposits.getFirst())||(!mutationHealthy.getAsBoolean()&&industry.hasCoordinateSources(deposits.getFirst())))
                    reason="Paused: ore mutation history requires reviewed recovery; reserve and goods retained";
                else {
                    var supply=tools.acquireRows(id,clock.industry(),period,clock.anchor().lastObservedMillis()/policy.millisPerPeriod(),operations);
                    long maximum=Math.min(policy.mining().siteBatchCapacity(),Math.multiplyExact(new BusinessRepository(connection).currentWorkers(id).size(),policy.mining().batchesPerWorker()));
                    var wear=tools.prepare(id,clock.industry(),maximum);
                    var receipt=industry.mineRows(operation,id,deposits.getFirst(),wear==null?policy.mining().siteBatchCapacity():wear.requested(),period,policy.mining().batchesPerWorker(),policy.mining().siteBatchCapacity());
                    reason=supply+"; "+receipt.explanation()+"; "+tools.finishRows(operation,id,period,wear,receipt.quantity());
                }
            } else if(clock.industry().equals("warehouse")) {
                reason=new WarehouseSupplies(connection,policy,operations,mutationHealthy).acquireRows(id,clock.world(),period,clock.anchor().lastObservedMillis()/policy.millisPerPeriod(),learning);
            } else {
                var recipe=policy.recipes().values().stream().filter(r->r.industry().equals(clock.industry())).findFirst();
                if(recipe.isEmpty())reason="Paused: no configured industry recipe";
                else {
                    long haulWindow=clock.anchor().lastObservedMillis()/policy.millisPerPeriod();
                    String upkeep=tools.acquireRows(id,clock.industry(),period,haulWindow,operations);
                    String supply=supplies.acquireRows(id,recipe.orElseThrow(),period,haulWindow,policy.millisPerPeriod(),policy.purchasingSupply(),policy.mining());
                    long maximum=Math.min(recipe.orElseThrow().siteCapacity(),Math.multiplyExact(new BusinessRepository(connection).currentWorkers(id).size(),recipe.orElseThrow().batchesPerWorker()));
                    var wear=tools.prepare(id,clock.industry(),maximum);
                    var receipt=industry.processRows(operation,id,recipe.orElseThrow(),wear==null?recipe.orElseThrow().siteCapacity():wear.requested(),period).receipt();
                    reason=upkeep+"; "+supply+"; "+receipt.explanation()+"; "+tools.finishRows(operation,id,period,wear,receipt.quantity());
                }
            }
            reason="Owned cargo delivered: "+arrived+"; "+availability.explanation()+"; "+reason+"; "+new IndustryLearning(connection,learning).reviewRows(operation,id,freshProduction);
            try(var s=connection.prepareStatement("INSERT INTO industry_runtime_history VALUES(?,?,?)")) {
                s.setString(1,id.toString());s.setLong(2,period);s.setString(3,reason.substring(0,Math.min(1024,reason.length())));s.executeUpdate();
            }
            try(var s=connection.prepareStatement("UPDATE industry_runtime SET completed=? WHERE business_id=? AND completed=?")) {
                s.setLong(1,period);s.setString(2,id.toString());s.setLong(3,clock.completed());
                if(s.executeUpdate()!=1)throw new SQLException("Industry checkpoint changed during owner work");
            }
            return null;
        });
    }
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T> T transaction(Work<T> action)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Industry runtime requires idle SQL owner");
        connection.setAutoCommit(false);
        try {var result=action.run();connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
