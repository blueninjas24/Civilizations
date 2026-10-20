package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.population.*;
import java.sql.*;
import java.util.*;

/** Earned public labor and stage material escrow inside the actual food-period transaction. No native writes. */
public final class ConstructionPublicWork {
    private final Connection c;
    public ConstructionPublicWork(Connection c) { this.c=Objects.requireNonNull(c); }
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE construction_stage_work (
                    draft_id TEXT NOT NULL REFERENCES construction_drafts(id),
                    stage INTEGER NOT NULL CHECK(typeof(stage)='integer' AND stage BETWEEN 0 AND 15),
                    work INTEGER NOT NULL CHECK(typeof(work)='integer' AND work BETWEEN 1 AND 1000000000),
                    first_period INTEGER NOT NULL CHECK(typeof(first_period)='integer' AND first_period>=1),
                    last_period INTEGER NOT NULL CHECK(typeof(last_period)='integer' AND last_period>=first_period),
                    PRIMARY KEY(draft_id,stage)
                )
                """);
            s.execute("""
                CREATE TABLE construction_public_work (
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=1),
                    resident_id TEXT NOT NULL REFERENCES residents(id),
                    draft_id TEXT NOT NULL,stage INTEGER NOT NULL,
                    effort INTEGER NOT NULL CHECK(typeof(effort)='integer' AND effort BETWEEN 1 AND 10000),
                    policy TEXT NOT NULL,
                    PRIMARY KEY(settlement_id,period,resident_id),UNIQUE(resident_id,period),
                    FOREIGN KEY(draft_id,stage) REFERENCES construction_stage_work(draft_id,stage)
                )
                """);
            s.execute("CREATE INDEX construction_public_work_by_stage ON construction_public_work(draft_id,stage)");
        }
    }
    /** Only dedicated farm jobs make unemployed labor unambiguously free; legacy output counts every adult. */
    int advanceRows(UUID town,long period,ConstructionLaborPolicy policy,long adulthood,long seed,boolean dedicatedFarmJobs)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Construction labor requires the food owner's transaction");
        if(!policy.enabled()||!dedicatedFarmJobs)return 0;
        try(var s=c.prepareStatement("SELECT n.demand,n.fulfilled FROM needs_periods n JOIN economy_accounts a ON a.settlement_id=n.settlement_id AND a.completed_periods=n.period JOIN economy_runtime e ON e.settlement_id=n.settlement_id AND e.target_periods=n.period WHERE n.settlement_id=? AND n.period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);
            try(var r=s.executeQuery()){if(!r.next()||r.getLong(2)<r.getLong(1))return 0;}
        }
        var candidates=new ArrayList<UUID>();
        try(var s=c.prepareStatement("""
            SELECT DISTINCT d.id FROM construction_drafts d
            JOIN construction_site_reservations p ON p.draft_id=d.id AND p.released_ms IS NULL
            JOIN construction_supply_orders o ON o.draft_id=d.id AND o.source='VILLAGE'
            WHERE d.settlement_id=? AND o.delivered_period<?
            ORDER BY d.created_ms,d.id LIMIT 16
            """)) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){while(r.next())candidates.add(UUID.fromString(r.getString(1)));}
        }
        var used=new HashSet<UUID>();
        try(var s=c.prepareStatement("SELECT resident_id FROM construction_public_work WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){while(r.next())used.add(UUID.fromString(r.getString(1)));}
        }
        int total=0;
        for(var id:candidates) {
            if(used.size()>=policy.villageWorkers())break;
            var draft=new ConstructionDraftRepository(c).find(id).orElseThrow();
            var site=new ConstructionSiteReservations(c).find(id).orElseThrow();
            if(!new ConstructionLandRepository(c).protectedColumns(draft.world(),site.footprint(),id).isEmpty())continue;
            int stage=nextStage(draft);if(stage<0)continue;
            var terms=draft.template().stages().get(stage);long before=work(id,stage);
            if(before==0) {
                var stock=availableBefore(draft,period);
                if(terms.materials().entrySet().stream().anyMatch(e->stock.getOrDefault(e.getKey(),0L)<e.getValue()))continue;
            }
            var workers=workers(town,period,policy.effortPerWorker(),used);
            var proposal=policy.propose(town,null,new JobRequirements(adulthood,Map.of()),workers,seed,period,true);
            long remaining=terms.work()-before,after=before;var booked=new ArrayList<ConstructionLaborPolicy.Allocation>();
            for(var allocation:proposal.allocations()) {
                if(remaining==0||used.size()+booked.size()>=policy.villageWorkers())break;
                int effort=(int)Math.min(remaining,allocation.constructionEffort());
                if(effort>0){booked.add(new ConstructionLaborPolicy.Allocation(allocation.resident(),effort,policy.effortPerWorker()-effort));remaining-=effort;after+=effort;}
            }
            if(booked.isEmpty())continue;
            try(var s=c.prepareStatement("INSERT INTO construction_stage_work VALUES(?,?,?,?,?) ON CONFLICT(draft_id,stage) DO UPDATE SET work=excluded.work,last_period=excluded.last_period")) {
                s.setString(1,id.toString());s.setInt(2,stage);s.setLong(3,after);s.setLong(4,period);s.setLong(5,period);s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO construction_public_work VALUES(?,?,?,?,?,?,?)")) {
                for(var allocation:booked) {
                    s.setString(1,town.toString());s.setLong(2,period);s.setString(3,allocation.resident().toString());s.setString(4,id.toString());s.setInt(5,stage);s.setInt(6,allocation.constructionEffort());s.setString(7,policy.toString());s.executeUpdate();
                    used.add(allocation.resident());total=Math.addExact(total,allocation.constructionEffort());
                }
            }
        }
        return total;
    }
    /** A completed stage waits for native acknowledgement before earning the next stage. */
    private int nextStage(ConstructionDraft d)throws SQLException {
        int offset=0;
        for(int index=0;index<d.template().stages().size();index++) {
            var stage=d.template().stages().get(index);long earned=work(d.id(),index);
            if(earned>stage.work())throw new SQLException("Saved construction work exceeds frozen stage");
            if(earned<stage.work())return index;
            try(var s=c.prepareStatement("SELECT COUNT(*) FROM construction_block_journal WHERE draft_id=? AND ordinal>=? AND ordinal<? AND state='APPLIED'")) {
                s.setString(1,d.id().toString());s.setInt(2,offset);s.setInt(3,offset+stage.blocks().size());
                try(var r=s.executeQuery()){if(!r.next()||r.getInt(1)!=stage.blocks().size())return -1;}
            }
            offset+=stage.blocks().size();
        }
        return -1;
    }
    void requireReadyRows(ConstructionDraft draft,int ordinal)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Economic intent validation requires the journal transaction");
        if(ordinal<0||ordinal>=draft.template().blockCount())throw new IllegalArgumentException("Ordinal outside frozen public plan");
        int offset=0,index=0;
        while(ordinal>=offset+draft.template().stages().get(index).blocks().size())offset+=draft.template().stages().get(index++).blocks().size();
        long needed=draft.template().stages().get(index).work();
        if(work(draft.id(),index)!=needed)throw new SQLException("Public construction waits for fully earned stage labor");
        try(var s=c.prepareStatement("SELECT COALESCE(SUM(effort),0) FROM construction_public_work WHERE draft_id=? AND stage=?")) {
            s.setString(1,draft.id().toString());s.setInt(2,index);
            try(var r=s.executeQuery()){if(!r.next()||r.getLong(1)!=needed)throw new SQLException("Stage work does not match saved public labor receipts");}
        }
        // Validates that the escrowed frozen costs are backed by actual delivered supplier goods.
        ConstructionSupplyOrders.deliveryOnly(c).stock(draft.id());
    }
    public long work(UUID draft,int stage)throws SQLException {
        try(var s=c.prepareStatement("SELECT work FROM construction_stage_work WHERE draft_id=? AND stage=?")) {
            s.setString(1,draft.toString());s.setInt(2,stage);try(var r=s.executeQuery()){return r.next()?r.getLong(1):0;}
        }
    }
    private List<ConstructionLaborPolicy.Worker> workers(UUID town,long period,int effort,Set<UUID> used)throws SQLException {
        var result=new ArrayList<ConstructionLaborPolicy.Worker>();var people=new PopulationRepository(c);
        try(var s=c.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE'
            AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
            AND NOT EXISTS(SELECT 1 FROM businesses b WHERE b.owner_id=r.id AND NOT EXISTS(SELECT 1 FROM business_closures x WHERE x.business_id=b.id))
            AND NOT EXISTS(SELECT 1 FROM resident_courses x WHERE x.resident_id=r.id AND x.status='ACTIVE')
            AND NOT EXISTS(SELECT 1 FROM study_reviews v JOIN resident_courses x ON x.id=v.course_id WHERE x.resident_id=r.id AND v.period=? AND v.after_experience>v.before_experience)
            ORDER BY r.id LIMIT 1025
            """)) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){while(r.next()) {
                var id=UUID.fromString(r.getString(1));if(used.contains(id))continue;
                result.add(new ConstructionLaborPolicy.Worker(people.findResident(id).orElseThrow(),people.findWorkProfile(id).orElseThrow(),null,effort));
            }}
        }
        return result.size()>1024?List.of():List.copyOf(result);
    }
    private Map<Commodity,Long> availableBefore(ConstructionDraft draft,long period)throws SQLException {
        var goods=new TreeMap<Commodity,Long>();
        try(var s=c.prepareStatement("SELECT commodity,units FROM construction_supply_orders WHERE draft_id=? AND source='VILLAGE' AND delivered_period<? LIMIT 129")) {
            s.setString(1,draft.id().toString());s.setLong(2,period);int count=0;
            try(var r=s.executeQuery()){while(r.next()){if(++count>128)throw new SQLException("Construction cargo budget exceeded");goods.merge(new Commodity(r.getString(1)),r.getLong(2),Math::addExact);}}
        }
        return unallocated(c,draft,goods);
    }
    /** Stage material quantities are frozen in the saved template; its first earned work escrows the entire cost once. */
    static Map<Commodity,Long> unallocated(Connection c,ConstructionDraft draft,Map<Commodity,Long> delivered)throws SQLException {
        var goods=new TreeMap<Commodity,Long>(delivered);
        try(var s=c.prepareStatement("SELECT stage FROM construction_stage_work WHERE draft_id=? ORDER BY stage")) {
            s.setString(1,draft.id().toString());try(var r=s.executeQuery()){while(r.next()) {
                int stage=r.getInt(1);if(stage<0||stage>=draft.template().stages().size())throw new SQLException("Saved construction stage outside frozen plan");
                for(var item:draft.template().stages().get(stage).materials().entrySet()) {
                    long stock=goods.getOrDefault(item.getKey(),0L);if(stock<item.getValue())throw new SQLException("Stage escrow exceeds delivered material stock");
                    goods.put(item.getKey(),stock-item.getValue());
                }
            }}
        }
        goods.values().removeIf(n->n==0);return Collections.unmodifiableMap(goods);
    }
}
