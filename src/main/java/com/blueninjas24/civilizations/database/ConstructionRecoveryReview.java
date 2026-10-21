package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source;
import com.blueninjas24.civilizations.industry.IndustryRuntimePolicy;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;

/** Explicit whole-project preview over immutable paid order terms. Existing atomic ledgers execute every refund. */
public final class ConstructionRecoveryReview {
    public enum Action { NONE,RECALL_PENDING,RETURN_ARRIVED }
    public record Quote(UUID draft,UUID village,UUID world,Action action,int orders,long units,long refund,String terms,String explanation){}
    public record Result(Action action,UUID draft,int orders,long refunded,Long duePeriod){}
    private final Connection c;
    public ConstructionRecoveryReview(Connection c){this.c=Objects.requireNonNull(c);}
    public Quote review(UUID id,UUID world,long now,Set<UUID> worlds,IndustryRuntimePolicy policy)throws SQLException{
        if(now<0)throw new IllegalArgumentException("Negative recovery review time");
        if(!c.getAutoCommit())throw new SQLException("Recovery preview requires idle SQL owner");c.setAutoCommit(false);
        try{var result=reviewRows(id,world,now,Set.copyOf(worlds),policy);c.commit();return result;}
        catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
    private Quote reviewRows(UUID id,UUID world,long now,Set<UUID> worlds,IndustryRuntimePolicy policy)throws SQLException{
        var draft=new ConstructionDraftRepository(c).find(id).orElseThrow(()->new BuildingRegistry.Declined("Unknown construction project"));
        if(!draft.world().equals(world)||!worlds.contains(world)||now<draft.createdMillis())throw new BuildingRegistry.Declined("Current original project world required");
        var family=new HousingProjects(c).find(id).orElseThrow(()->new BuildingRegistry.Declined("Recovery controls require a formal family-home project"));
        var orders=orders(id);long units=0,refund=0;for(var o:orders){units=Math.addExact(units,o.request().units());refund=Math.addExact(refund,o.price());}
        if(new ConstructionSupplyRecalls(c).recalled(id)||new ConstructionSupplyReturns(c).dispatched(id))return held(draft,orders,units,refund,"Supplies already recovered; inspect history and use unused cancellation separately if eligible");
        if(!family.active())return held(draft,orders,units,refund,"Completed or cancelled project retains history; no new recovery");
        for(var table:List.of("construction_block_journal","construction_stage_work","construction_material_reservations"))
            try(var s=c.prepareStatement("SELECT 1 FROM "+table+" WHERE draft_id=? LIMIT 1")){
                s.setString(1,id.toString());try(var r=s.executeQuery()){if(r.next())return held(draft,orders,units,refund,"Started work, native intents or mixed-source materials require manual review; no refund or demolition");}
            }
        if(orders.isEmpty()||orders.stream().anyMatch(o->o.request().source()!=Source.VILLAGE))return held(draft,orders,units,refund,"Requires actual public paid supplies; unpaid or private goods use their separate controls");
        long period;
        try(var s=c.prepareStatement("SELECT a.completed_periods,r.target_periods,r.last_observed_millis FROM economy_accounts a JOIN economy_runtime r USING(settlement_id) WHERE a.settlement_id=?")){
            s.setString(1,draft.settlement().toString());try(var r=s.executeQuery()){
                if(!r.next()||r.getLong(1)!=r.getLong(2)||now<r.getLong(3))return held(draft,orders,units,refund,"Current earned owner period without backlog required");period=r.getLong(1);
            }
        }
        boolean pending=orders.stream().allMatch(o->o.deliveredPeriod()==null&&o.request().period()==period);
        boolean arrived=orders.stream().allMatch(o->o.deliveredPeriod()!=null&&o.deliveredPeriod()<=period);
        Action action=pending?Action.RECALL_PENDING:arrived?Action.RETURN_ARRIVED:Action.NONE;
        if(action==Action.NONE)return held(draft,orders,units,refund,"Mixed/advanced pending deliveries must finish before an unused return; no instant reverse delivery");
        if(action==Action.RETURN_ARRIVED&&!policy.enabled())return held(draft,orders,units,refund,"Return hauling is disabled; actual paid assets remain onsite");
        var requiredCash=new HashMap<UUID,Long>();var requiredSpace=new HashMap<UUID,Long>();var stores=new IndustryRepository(c);
        for(var order:orders){var q=order.request();var store=stores.store(q.seller()).orElseThrow();Long size=store.spacePerUnit().get(q.commodity());
            if(size==null)return held(draft,orders,units,refund,"Original supplier no longer accepts this material");
            requiredCash.merge(q.seller(),order.price(),Math::addExact);requiredSpace.merge(q.seller(),Math.multiplyExact(size,q.units()),Math::addExact);
        }
        for(var seller:requiredCash.keySet()){var store=stores.store(seller).orElseThrow();
            if(store.hundredths()<requiredCash.get(seller)||store.freeSpace()<requiredSpace.get(seller))return held(draft,orders,units,refund,"Original suppliers lack exact refund cash or enough storage for the whole batch");
        }
        if(action==Action.RETURN_ARRIVED){
            long window=now/policy.millisPerPeriod(),used=StorageUpgrades.haulUsed(c,draft.settlement(),window,policy.millisPerPeriod());
            try(var s=c.prepareStatement("SELECT t.units FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND a.haul_window=? AND a.haul_cadence=?")){
                s.setString(1,draft.settlement().toString());s.setLong(2,window);s.setLong(3,policy.millisPerPeriod());try(var r=s.executeQuery()){while(r.next())used=Math.addExact(used,r.getLong(1));}
            }
            if(used>policy.mining().localHaulUnitsPerPeriod()||units>policy.mining().localHaulUnitsPerPeriod()-used)return held(draft,orders,units,refund,"Return waits for shared real hauling capacity");
        }
        return new Quote(id,draft.settlement(),world,action,orders.size(),units,refund,hash(draft,action,orders),action==Action.RECALL_PENDING
                ?"Withdraw pending orders in their original earned period: exact goods return and original suppliers refund actual village money"
                :"Original suppliers buy back all arrived unused goods at the exact paid price: real refund now, reserved return cargo arrives in a later earned period");
    }
    private Quote held(com.blueninjas24.civilizations.construction.ConstructionDraft d,List<ConstructionSupplyOrders.Receipt> orders,long units,long refund,String reason){
        return new Quote(d.id(),d.settlement(),d.world(),Action.NONE,orders.size(),units,refund,hash(d,Action.NONE,orders),reason);
    }
    /** The simulation owner excludes other SQL/native changes across this review and existing atomic operation. */
    public Result execute(UUID operation,UUID draftId,UUID world,Action action,String expectedTerms,long submitted,long now,Set<UUID> worlds,IndustryRuntimePolicy policy)throws SQLException{
        Objects.requireNonNull(operation);Objects.requireNonNull(action);Objects.requireNonNull(expectedTerms);
        if(action==Action.NONE||submitted<0||now<submitted||!c.getAutoCommit())throw new BuildingRegistry.Declined("Reviewed actionable recovery, current execution time and idle SQL owner required");
        var draft=new ConstructionDraftRepository(c).find(draftId).orElseThrow(()->new BuildingRegistry.Declined("Unknown construction project"));
        if(!draft.world().equals(world)||!worlds.contains(world)||!hash(draft,action,orders(draftId)).equals(expectedTerms))throw new BuildingRegistry.Declined("Recovery world or exact paid terms changed; review again");
        // Queue time is not an economic clock. Replays keep the first saved event; new work uses current owner time.
        if(action==Action.RECALL_PENDING){var old=new ConstructionSupplyRecalls(c).find(operation);if(old.isPresent()){
            var saved=old.get();if(!saved.draft().equals(draftId))throw new BuildingRegistry.Declined("Recovery operation belongs to another project");return new Result(action,draftId,saved.orders(),saved.refunded(),null);
        }}else{var old=new ConstructionSupplyReturns(c).find(operation);if(old.isPresent()){
            var saved=old.get();if(!saved.draft().equals(draftId))throw new BuildingRegistry.Declined("Recovery operation belongs to another project");return new Result(action,draftId,saved.orders(),saved.refunded(),saved.duePeriod());
        }}
        var quote=review(draftId,world,now,worlds,policy);if(quote.action()!=action||!quote.terms().equals(expectedTerms))throw new BuildingRegistry.Declined("Recovery readiness changed: "+quote.explanation());
        if(action==Action.RECALL_PENDING){var saved=new ConstructionSupplyRecalls(c).recallPending(operation,draftId,now,worlds);return new Result(action,draftId,saved.orders(),saved.refunded(),null);}
        var saved=new ConstructionSupplyReturns(c).returnUnused(operation,draftId,now,worlds,policy);return new Result(action,draftId,saved.orders(),saved.refunded(),saved.duePeriod());
    }
    private List<ConstructionSupplyOrders.Receipt> orders(UUID id)throws SQLException{
        var result=new ArrayList<ConstructionSupplyOrders.Receipt>();try(var s=c.prepareStatement("SELECT operation_id FROM construction_supply_orders WHERE draft_id=? ORDER BY operation_id LIMIT 129")){
            s.setString(1,id.toString());try(var r=s.executeQuery()){while(r.next()){
                if(result.size()==128)throw new BuildingRegistry.Declined("Recovery order review exceeds128 saved receipts");result.add(ConstructionSupplyOrders.deliveryOnly(c).find(UUID.fromString(r.getString(1))).orElseThrow());
            }}
        }return List.copyOf(result);
    }
    private static String hash(com.blueninjas24.civilizations.construction.ConstructionDraft draft,Action action,List<ConstructionSupplyOrders.Receipt> orders){
        var text=new StringBuilder("construction-reviewed-recovery-v1|").append(draft.id()).append('|').append(draft.world()).append('|').append(draft.settlement()).append('|').append(action);
        for(var r:orders){var q=r.request();text.append('|').append(q.operation()).append('|').append(q.seller()).append('|').append(q.source()).append('|').append(q.business()).append('|').append(q.commodity().id()).append('|').append(q.units()).append('|').append(r.price()).append('|').append(q.period()).append('|').append(r.deliveredPeriod());}
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
