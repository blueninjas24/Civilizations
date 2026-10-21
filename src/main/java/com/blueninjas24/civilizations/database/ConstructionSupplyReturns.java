package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.IndustryRuntimePolicy;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Exact-price supplier buyback of unused arrived public goods, with real reserved return cargo. */
public final class ConstructionSupplyReturns {
    public record Receipt(UUID operation,UUID draft,long atMillis,long period,long duePeriod,int orders,long refunded){}
    private final Connection c;
    public ConstructionSupplyReturns(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException{
        try(var s=c.createStatement()){s.execute("""
            CREATE TABLE construction_supply_returns (
              order_id TEXT PRIMARY KEY NOT NULL REFERENCES construction_supply_orders(operation_id),
              return_operation TEXT NOT NULL,refund_operation TEXT UNIQUE NOT NULL REFERENCES treasury_entries(operation_id),
              dispatched_ms INTEGER NOT NULL CHECK(typeof(dispatched_ms)='integer' AND dispatched_ms>=0),
              period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=0),
              due_period INTEGER NOT NULL CHECK(typeof(due_period)='integer' AND due_period>period),
              delivered_period INTEGER CHECK(delivered_period IS NULL OR (typeof(delivered_period)='integer' AND delivered_period>=due_period)),
              space_per_unit INTEGER NOT NULL CHECK(typeof(space_per_unit)='integer' AND space_per_unit>0),
              haul_window INTEGER NOT NULL CHECK(typeof(haul_window)='integer' AND haul_window>=0),
              cadence INTEGER NOT NULL CHECK(typeof(cadence)='integer' AND cadence BETWEEN 1000 AND 86400000),
              seller_before INTEGER NOT NULL CHECK(typeof(seller_before)='integer' AND seller_before>=0),
              seller_after INTEGER NOT NULL CHECK(typeof(seller_after)='integer' AND seller_after>=0 AND seller_after<seller_before),
              buyer_before INTEGER NOT NULL CHECK(typeof(buyer_before)='integer' AND buyer_before>=0),
              buyer_after INTEGER NOT NULL CHECK(typeof(buyer_after)='integer' AND buyer_after>buyer_before)
            )
            """);s.execute("CREATE INDEX construction_returns_by_operation ON construction_supply_returns(return_operation,order_id)");}
    }
    public Receipt returnUnused(UUID operation,UUID draftId,long now,Set<UUID> worlds,IndustryRuntimePolicy policy)throws SQLException{
        Objects.requireNonNull(operation);Objects.requireNonNull(draftId);Objects.requireNonNull(policy);var present=Set.copyOf(worlds);
        if(now<0)throw new IllegalArgumentException("Negative return time");
        return transaction(()->{
            var draft=new ConstructionDraftRepository(c).find(draftId).orElseThrow(()->new BuildingRegistry.Declined("Unknown construction project"));
            if(!present.contains(draft.world())||now<draft.createdMillis())throw new BuildingRegistry.Declined("Present original world and current return time required");
            var old=find(operation);if(old.isPresent()){
                if(!old.get().draft().equals(draftId)||old.get().atMillis()!=now)throw new BuildingRegistry.Declined("Return identity/time changed");return old.get();
            }
            if(!policy.enabled()||dispatched(draftId)||new ConstructionSupplyRecalls(c).recalled(draftId))throw new BuildingRegistry.Declined("Return disabled or project already recovered");
            for(var table:List.of("construction_block_journal","construction_stage_work","construction_material_reservations"))
                try(var s=c.prepareStatement("SELECT 1 FROM "+table+" WHERE draft_id=? LIMIT 1")){
                    s.setString(1,draftId.toString());try(var r=s.executeQuery()){if(r.next())throw new BuildingRegistry.Declined("Started or mixed-source supplies require reviewed recovery");}
                }
            long period=currentPeriod(draft.settlement(),now,true),due=Math.addExact(period,policy.mining().localDeliveryPeriods());
            long window=now/policy.millisPerPeriod(),units=0;
            var ids=new ArrayList<UUID>();
            try(var s=c.prepareStatement("SELECT operation_id FROM construction_supply_orders WHERE draft_id=? ORDER BY operation_id LIMIT 129")){
                s.setString(1,draftId.toString());try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
            }
            if(ids.isEmpty()||ids.size()>128)throw new BuildingRegistry.Declined("Return requires1..128 arrived public orders");
            var cargo=ConstructionSupplyOrders.deliveryOnly(c);var orders=new ArrayList<ConstructionSupplyOrders.Receipt>();
            for(var id:ids){var receipt=cargo.find(id).orElseThrow();
                if(receipt.request().source()!=com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source.VILLAGE||receipt.deliveredPeriod()==null||receipt.deliveredPeriod()>period)
                    throw new BuildingRegistry.Declined("All public goods must have arrived before a return shipment");
                units=Math.addExact(units,receipt.request().units());orders.add(receipt);
            }
            long used=StorageUpgrades.haulUsed(c,draft.settlement(),window,policy.millisPerPeriod());
            try(var s=c.prepareStatement("SELECT t.units FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND a.haul_window=? AND a.haul_cadence=?")){
                s.setString(1,draft.settlement().toString());s.setLong(2,window);s.setLong(3,policy.millisPerPeriod());try(var r=s.executeQuery()){while(r.next())used=Math.addExact(used,r.getLong(1));}
            }
            if(used>policy.mining().localHaulUnitsPerPeriod()||units>policy.mining().localHaulUnitsPerPeriod()-used)throw new BuildingRegistry.Declined("Return waits for shared local hauling capacity");
            for(var receipt:orders){var q=receipt.request();var store=new IndustryRepository(c).store(q.seller()).orElseThrow();Long size=store.spacePerUnit().get(q.commodity());
                if(size==null||store.freeSpace()<Math.multiplyExact(size,q.units())||store.hundredths()<receipt.price())throw new BuildingRegistry.Declined("Supplier refund cash or return storage unavailable; all goods retained onsite");
                long before=new TreasuryRepository(c).load(draft.settlement()).orElseThrow().hundredths();
                UUID refund=UUID.nameUUIDFromBytes(("construction-arrived-return-v1|"+operation+"|"+q.operation()).getBytes(StandardCharsets.UTF_8));
                if(new TreasuryRepository(c).entry(refund).isPresent())throw new SQLException("Return refund identity already used");
                try(var s=c.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=? AND hundredths=?")){
                    s.setLong(1,store.hundredths()-receipt.price());s.setString(2,q.seller().toString());s.setLong(3,store.hundredths());if(s.executeUpdate()!=1)throw new SQLException("Return buyer cash changed");
                }
                new TreasuryRepository(c).postRows(refund,draft.settlement(),receipt.price(),"Supplier repurchase of unused construction order "+q.operation());
                try(var s=c.prepareStatement("INSERT INTO construction_supply_returns VALUES(?,?,?,?,?,?,NULL,?,?,?,?,?,?,?)")){
                    int n=1;s.setString(n++,q.operation().toString());s.setString(n++,operation.toString());s.setString(n++,refund.toString());s.setLong(n++,now);s.setLong(n++,period);s.setLong(n++,due);
                    s.setLong(n++,size);s.setLong(n++,window);s.setLong(n++,policy.millisPerPeriod());s.setLong(n++,store.hundredths());s.setLong(n++,store.hundredths()-receipt.price());s.setLong(n++,before);s.setLong(n,Math.addExact(before,receipt.price()));s.executeUpdate();
                }
            }
            return find(operation).orElseThrow();
        });
    }
    public boolean dispatched(UUID draft)throws SQLException{
        try(var s=c.prepareStatement("SELECT r.return_operation FROM construction_supply_returns r JOIN construction_supply_orders o ON o.operation_id=r.order_id WHERE o.draft_id=? LIMIT 1")){
            s.setString(1,draft.toString());try(var r=s.executeQuery()){return r.next()&&find(UUID.fromString(r.getString(1))).orElseThrow().draft().equals(draft);}
        }
    }
    public Optional<Receipt> find(UUID operation)throws SQLException{
        UUID draft=null;long at=-1,period=-1,due=-1,total=0;int count=0;
        try(var s=c.prepareStatement("SELECT r.*,o.draft_id,o.price,o.source,o.delivered_period AS outward_delivered FROM construction_supply_returns r JOIN construction_supply_orders o ON o.operation_id=r.order_id WHERE r.return_operation=? ORDER BY r.order_id LIMIT 129")){
            s.setString(1,operation.toString());try(var r=s.executeQuery()){while(r.next()){
                if(++count>128)throw new SQLException("Return history budget exceeded");UUID current=UUID.fromString(r.getString("draft_id"));
                if(draft==null){draft=current;at=r.getLong("dispatched_ms");period=r.getLong("period");due=r.getLong("due_period");}
                long price=r.getLong("price");
                if(!draft.equals(current)||at!=r.getLong("dispatched_ms")||period!=r.getLong("period")||due!=r.getLong("due_period")||!r.getString("source").equals("VILLAGE")||r.getObject("outward_delivered")==null
                        ||r.getLong("outward_delivered")>period||r.getLong("seller_before")-r.getLong("seller_after")!=price||r.getLong("buyer_after")-r.getLong("buyer_before")!=price)throw new SQLException("Return differs from original arrived public order");
                var refund=new TreasuryRepository(c).entry(UUID.fromString(r.getString("refund_operation"))).orElseThrow();var plan=new ConstructionDraftRepository(c).find(current).orElseThrow();
                if(!refund.settlement().equals(plan.settlement())||!refund.kind().equals("CREDIT")||refund.amount()!=price||refund.balanceAfter()!=r.getLong("buyer_after"))throw new SQLException("Return refund differs from treasury history");
                total=Math.addExact(total,price);
            }}
        }
        if(draft==null)return Optional.empty();try(var s=c.prepareStatement("SELECT count(*) FROM construction_supply_orders WHERE draft_id=?")){
            s.setString(1,draft.toString());try(var r=s.executeQuery()){if(r.getInt(1)!=count)throw new SQLException("Return lost part of original order set");}
        }return Optional.of(new Receipt(operation,draft,at,period,due,count,total));
    }
    /** Accepted return cargo settles independently of policy changes and site/family retirement. */
    public int deliverReady(Set<UUID> worlds,int limit)throws SQLException{
        var present=Set.copyOf(worlds);if(limit<1||limit>64||present.size()>256)throw new IllegalArgumentException("Return delivery review exceeds bounds");
        if(present.isEmpty())return 0;return transaction(()->{
            var ids=new ArrayList<UUID>();String places=String.join(",",Collections.nCopies(present.size(),"?"));
            try(var s=c.prepareStatement("SELECT r.order_id FROM construction_supply_returns r JOIN construction_supply_orders o ON o.operation_id=r.order_id JOIN construction_drafts d ON d.id=o.draft_id JOIN economy_accounts a ON a.settlement_id=d.settlement_id WHERE r.delivered_period IS NULL AND a.completed_periods>=r.due_period AND d.world_id IN ("+places+") ORDER BY r.due_period,r.order_id LIMIT ?")){
                int n=1;for(var world:new TreeSet<>(present))s.setString(n++,world.toString());s.setInt(n,limit);try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
            }
            for(var id:ids){var order=ConstructionSupplyOrders.deliveryOnly(c).find(id).orElseThrow();var q=order.request();var draft=new ConstructionDraftRepository(c).find(q.draft()).orElseThrow();
                long period=currentPeriod(draft.settlement(),0,false),size;
                try(var s=c.prepareStatement("UPDATE construction_supply_returns SET delivered_period=? WHERE order_id=? AND delivered_period IS NULL RETURNING space_per_unit")){
                    s.setLong(1,period);s.setString(2,id.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Return cargo changed");size=r.getLong(1);}
                }
                var store=new IndustryRepository(c).store(q.seller()).orElseThrow();
                if(!Objects.equals(store.spacePerUnit().get(q.commodity()),size)||store.freeSpace()<Math.multiplyExact(q.units(),size))throw new SQLException("Reserved return destination storage changed; cargo retained");
                long stock=store.units().get(q.commodity());try(var s=c.prepareStatement("UPDATE industry_stock SET units=? WHERE business_id=? AND commodity=? AND units=?")){
                    s.setLong(1,Math.addExact(stock,q.units()));s.setString(2,q.seller().toString());s.setString(3,q.commodity().id());s.setLong(4,stock);if(s.executeUpdate()!=1)throw new SQLException("Return destination stock changed");
                }
            }return ids.size();
        });
    }
    private long currentPeriod(UUID town,long now,boolean current)throws SQLException{
        try(var s=c.prepareStatement("SELECT a.completed_periods,r.target_periods,r.last_observed_millis FROM economy_accounts a JOIN economy_runtime r USING(settlement_id) WHERE a.settlement_id=?")){
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next()||(current&&(r.getLong(1)!=r.getLong(2)||now<r.getLong(3))))throw new BuildingRegistry.Declined("Current earned owner period required");return r.getLong(1);}
        }
    }
    static long haulUsed(Connection c,UUID town,long window,long cadence)throws SQLException{
        long total=0;try(var s=c.prepareStatement("SELECT o.units FROM construction_supply_returns r JOIN construction_supply_orders o ON o.operation_id=r.order_id JOIN construction_drafts d ON d.id=o.draft_id WHERE d.settlement_id=? AND r.haul_window=? AND r.cadence=?")){
            s.setString(1,town.toString());s.setLong(2,window);s.setLong(3,cadence);try(var r=s.executeQuery()){while(r.next())total=Math.addExact(total,r.getLong(1));}
        }return total;
    }
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException{
        if(!c.getAutoCommit())throw new SQLException("Supply return requires idle SQL owner");c.setAutoCommit(false);
        try{T result=work.run();c.commit();return result;}catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
}
