package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** SQL-owner bounded staffing maintenance and immutable diagnostics. Does not found/hire/produce. */
public final class BusinessRuntime {
    public record View(UUID id, UUID settlement, UUID world, String name, String industry,
                       String founder, String owner, boolean ownerAvailable, int capacity,
                       int workers, int openAssignments, String lastSeparation, long foundedStep,
                       int x, int y, int z, boolean paused, String lastProduction,String lifecycle) {
        public int vacancies() { return capacity - workers; }
    }
    public record Snapshot(long total, Map<UUID,View> businesses) {
        public Snapshot { businesses = Map.copyOf(businesses); }
        public static Snapshot empty() { return new Snapshot(0,Map.of()); }
    }
    private final Connection connection;
    private final BusinessRepository businesses;
    private final int batchSize, cacheSize;
    private final LinkedHashMap<UUID,View> cache = new LinkedHashMap<>();
    private String cursor = "";
    private AutomaticBusinessResale resale;
    public BusinessRuntime(Connection connection, int batchSize, int cacheSize) throws SQLException {
        if(batchSize<1 || batchSize>64 || cacheSize<batchSize || cacheSize>4096)
            throw new IllegalArgumentException("Invalid business runtime budgets");
        this.connection=connection;this.batchSize=batchSize;this.cacheSize=cacheSize;
        businesses=new BusinessRepository(connection);
    }
    public void configureResale(com.blueninjas24.civilizations.industry.BusinessResalePolicy policy,
                                com.blueninjas24.civilizations.industry.BusinessPurchasePolicy funding,long seed) {
        resale=new AutomaticBusinessResale(connection,policy,funding,seed);
    }
    public Snapshot refresh(long completedMonth, Set<UUID> loadedWorlds) throws SQLException {
        return refresh(completedMonth,loadedWorlds,0,false);
    }
    public Snapshot refresh(long completedMonth,Set<UUID> loadedWorlds,long now,boolean allowSales)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Business runtime requires idle SQL owner");
        long total;
        try(var s=connection.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM businesses")) { total=r.getLong(1); }
        var ids = page();
        if(ids.isEmpty() && !cursor.isEmpty()) { cursor="";ids=page(); }
        // Cache eligibility changes immediately, even if this business is outside this bounded page.
        cache.replaceAll((id,v)->withPause(v,loadedWorlds));
        for(var id:ids) {
            var founding=businesses.findFounding(id).orElseThrow(()->new SQLException("Business disappeared during owner refresh"));
            var b=businesses.findBusiness(id).orElseThrow();var site=founding.site();
            boolean paused=loadedWorlds!=null && !loadedWorlds.contains(site.worldId());
            if(!paused) {
                businesses.reconcileBusiness(id,completedMonth);
                if(completedMonth>=b.foundedStep()) {
                    var operation=UUID.nameUUIDFromBytes(("civilizations:business-succession-v1:"+id+":"+b.ownerId())
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    new BusinessSuccession(connection).succeed(operation,id,completedMonth);
                    b=businesses.findBusiness(id).orElseThrow();
                }
                new BusinessClosure(connection).closeIfExpired(id,completedMonth);
                if(allowSales&&resale!=null){resale.review(id,completedMonth,now);b=businesses.findBusiness(id).orElseThrow();}
            }
            int open;
            String separation;
            try(var s=connection.prepareStatement("SELECT COUNT(*) FROM business_employment WHERE business_id=? AND ended_step IS NULL")) {
                s.setString(1,id.toString());try(var r=s.executeQuery()){open=r.getInt(1);}
            }
            try(var s=connection.prepareStatement("SELECT end_reason FROM business_employment WHERE business_id=? AND ended_step IS NOT NULL ORDER BY ended_step DESC,id LIMIT 1")) {
                s.setString(1,id.toString());try(var r=s.executeQuery()){separation=r.next()?r.getString(1):null;}
            }
            String founder,owner;boolean available;
            try(var s=connection.prepareStatement("SELECT f.name,o.name,o.status,o.settlement_id FROM residents f JOIN residents o ON o.id=? WHERE f.id=?")) {
                s.setString(1,b.ownerId().toString());s.setString(2,b.founderId().toString());
                try(var r=s.executeQuery()) {
                    if(!r.next())throw new SQLException("Business resident identity missing");
                    founder=r.getString(1);owner=r.getString(2);available=r.getString(3).equals("ALIVE") && r.getString(4).equals(b.settlementId().toString());
                }
            }
            var view=new View(id,b.settlementId(),site.worldId(),b.name(),b.industry(),founder,owner,available,b.workerCapacity(),
                    businesses.currentWorkers(id).size(),open,separation,b.foundedStep(),site.x(),site.y(),site.z(),paused,lastProduction(id),
                    new BusinessClosure(connection).find(id).map(c->"Closed at administrative month "+c.step()+": "+c.reason()).orElse(
                            resale==null?"Open; ownership/lifecycle requirements still apply":resale.explanation(id).orElse("Open; qualified succession before resale")));
            cache.remove(id);cache.put(id,view);cursor=id.toString();
            while(cache.size()>cacheSize)cache.remove(cache.keySet().iterator().next());
        }
        return new Snapshot(total,cache);
    }
    private String lastProduction(UUID business) throws SQLException {
        try(var s=connection.prepareStatement("SELECT period,explanation FROM industry_runtime_history WHERE business_id=? ORDER BY period DESC LIMIT 1")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()) {
                if(r.next())return "Period "+r.getLong(1)+": "+r.getString(2);
            }
        }
        try(var s=connection.prepareStatement("SELECT p.period,substr(o.explanation,1,768),length(o.explanation) FROM industry_production p JOIN industry_operations o ON o.id=p.operation_id WHERE p.business_id=? ORDER BY p.period DESC LIMIT 1")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){return r.next()?"Period "+r.getLong(1)+": "+r.getString(2)+(r.getInt(3)>768?"...":""):null;}
        }
    }
    private List<UUID> page() throws SQLException {
        var ids=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("SELECT id FROM businesses WHERE id>? ORDER BY id LIMIT ?")) {
            s.setString(1,cursor);s.setInt(2,batchSize);
            try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
        }
        return ids;
    }
    private static View withPause(View v,Set<UUID> worlds) {
        return new View(v.id(),v.settlement(),v.world(),v.name(),v.industry(),v.founder(),v.owner(),v.ownerAvailable(),v.capacity(),
                v.workers(),v.openAssignments(),v.lastSeparation(),v.foundedStep(),v.x(),v.y(),v.z(),worlds!=null && !worlds.contains(v.world()),v.lastProduction(),v.lifecycle());
    }
}
