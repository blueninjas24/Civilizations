package com.blueninjas24.civilizations.database;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Withdraw a whole public purchase before its earned owner period advances. Never erases sale history. */
public final class ConstructionSupplyRecalls {
    public record Receipt(UUID operation,UUID draft,long atMillis,long period,int orders,long refunded){}
    private final Connection c;
    public ConstructionSupplyRecalls(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException{
        try(var s=c.createStatement()){s.execute("""
            CREATE TABLE construction_supply_recalls (
              order_id TEXT PRIMARY KEY NOT NULL REFERENCES construction_supply_orders(operation_id),
              recall_operation TEXT NOT NULL,refund_operation TEXT UNIQUE NOT NULL REFERENCES treasury_entries(operation_id),
              recalled_ms INTEGER NOT NULL CHECK(typeof(recalled_ms)='integer' AND recalled_ms>=0),
              period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=0),
              seller_before INTEGER NOT NULL CHECK(typeof(seller_before)='integer' AND seller_before>=0),
              seller_after INTEGER NOT NULL CHECK(typeof(seller_after)='integer' AND seller_after>=0 AND seller_after<seller_before),
              stock_before INTEGER NOT NULL CHECK(typeof(stock_before)='integer' AND stock_before>=0),
              stock_after INTEGER NOT NULL CHECK(typeof(stock_after)='integer' AND stock_after>stock_before),
              buyer_before INTEGER NOT NULL CHECK(typeof(buyer_before)='integer' AND buyer_before>=0),
              buyer_after INTEGER NOT NULL CHECK(typeof(buyer_after)='integer' AND buyer_after>buyer_before)
            )
            """);s.execute("CREATE INDEX construction_recalls_by_operation ON construction_supply_recalls(recall_operation,order_id)");}
    }
    public Receipt recallPending(UUID operation,UUID draftId,long now,Set<UUID> worlds)throws SQLException{
        Objects.requireNonNull(operation);Objects.requireNonNull(draftId);var present=Set.copyOf(worlds);
        if(now<0)throw new IllegalArgumentException("Negative recall time");
        if(!c.getAutoCommit())throw new SQLException("Supply recall requires idle SQL owner");c.setAutoCommit(false);
        try{
            var draft=new ConstructionDraftRepository(c).find(draftId).orElseThrow(()->new BuildingRegistry.Declined("Unknown construction project"));
            if(!present.contains(draft.world())||now<draft.createdMillis())throw new BuildingRegistry.Declined("Present original world and current recall time required");
            var old=find(operation);
            if(old.isPresent()){
                if(!old.get().draft().equals(draftId)||old.get().atMillis()!=now)throw new BuildingRegistry.Declined("Recall identity/time changed");
                c.commit();return old.get();
            }
            if(recalled(draftId))throw new BuildingRegistry.Declined("Project supplies already recalled under another operation");
            for(var table:List.of("construction_block_journal","construction_stage_work","construction_material_reservations"))
                try(var s=c.prepareStatement("SELECT 1 FROM "+table+" WHERE draft_id=? LIMIT 1")){
                    s.setString(1,draftId.toString());try(var r=s.executeQuery()){if(r.next())throw new BuildingRegistry.Declined("Started or mixed-source project requires reviewed recovery");}
                }
            long period;
            try(var s=c.prepareStatement("SELECT a.completed_periods,r.target_periods,r.last_observed_millis FROM economy_accounts a JOIN economy_runtime r USING(settlement_id) WHERE a.settlement_id=?")){
                s.setString(1,draft.settlement().toString());try(var r=s.executeQuery()){
                    if(!r.next()||r.getLong(1)!=r.getLong(2)||now<r.getLong(3))throw new BuildingRegistry.Declined("Current earned owner period without backlog required");period=r.getLong(1);
                }
            }
            var ids=new ArrayList<UUID>();
            try(var s=c.prepareStatement("SELECT operation_id FROM construction_supply_orders WHERE draft_id=? ORDER BY operation_id LIMIT 129")){
                s.setString(1,draftId.toString());try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
            }
            if(ids.isEmpty()||ids.size()>128)throw new BuildingRegistry.Declined("Recall requires1..128 saved pending orders");
            var cargo=ConstructionSupplyOrders.deliveryOnly(c);var stores=new IndustryRepository(c);long refunded=0;
            for(var id:ids){
                var receipt=cargo.find(id).orElseThrow();var q=receipt.request();
                if(q.source()!=com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source.VILLAGE||q.period()!=period||receipt.deliveredPeriod()!=null)
                    throw new BuildingRegistry.Declined("Only entirely undelivered public orders in their original earned period can be recalled");
                var seller=stores.store(q.seller()).orElseThrow();Long size=seller.spacePerUnit().get(q.commodity());
                if(size==null||seller.freeSpace()<Math.multiplyExact(size,q.units())||seller.hundredths()<receipt.price())
                    throw new BuildingRegistry.Declined("Original supplier cannot accept all goods and repay exact funds; project assets retained");
                long stock=seller.units().getOrDefault(q.commodity(),0L),after=Math.addExact(stock,q.units());
                long cash=new TreasuryRepository(c).load(draft.settlement()).orElseThrow().hundredths();
                UUID refund=UUID.nameUUIDFromBytes(("construction-pending-recall-v1|"+operation+"|"+id).getBytes(StandardCharsets.UTF_8));
                if(new TreasuryRepository(c).entry(refund).isPresent())throw new SQLException("Recall refund identity already used");
                try(var s=c.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=? AND hundredths=?")){
                    s.setLong(1,seller.hundredths()-receipt.price());s.setString(2,q.seller().toString());s.setLong(3,seller.hundredths());if(s.executeUpdate()!=1)throw new SQLException("Recall supplier cash changed");
                }
                try(var s=c.prepareStatement("UPDATE industry_stock SET units=? WHERE business_id=? AND commodity=? AND units=?")){
                    s.setLong(1,after);s.setString(2,q.seller().toString());s.setString(3,q.commodity().id());s.setLong(4,stock);if(s.executeUpdate()!=1)throw new SQLException("Recall supplier stock changed");
                }
                new TreasuryRepository(c).postRows(refund,draft.settlement(),receipt.price(),"Withdraw pending construction order "+id);
                try(var s=c.prepareStatement("INSERT INTO construction_supply_recalls VALUES(?,?,?,?,?,?,?,?,?,?,?)")){
                    s.setString(1,id.toString());s.setString(2,operation.toString());s.setString(3,refund.toString());s.setLong(4,now);s.setLong(5,period);
                    s.setLong(6,seller.hundredths());s.setLong(7,seller.hundredths()-receipt.price());s.setLong(8,stock);s.setLong(9,after);s.setLong(10,cash);s.setLong(11,Math.addExact(cash,receipt.price()));s.executeUpdate();
                }
                refunded=Math.addExact(refunded,receipt.price());
            }
            var saved=find(operation).orElseThrow();if(saved.refunded()!=refunded)throw new SQLException("Recall receipt total differs");
            c.commit();return saved;
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
        finally{c.setAutoCommit(true);}
    }
    public boolean recalled(UUID draft)throws SQLException{
        try(var s=c.prepareStatement("SELECT r.recall_operation FROM construction_supply_recalls r JOIN construction_supply_orders o ON o.operation_id=r.order_id WHERE o.draft_id=? LIMIT 1")){
            s.setString(1,draft.toString());try(var r=s.executeQuery()){return r.next()&&find(UUID.fromString(r.getString(1))).orElseThrow().draft().equals(draft);}
        }
    }
    public Optional<Receipt> find(UUID operation)throws SQLException{
        UUID draft=null;long at=-1,period=-1,total=0;int count=0;
        try(var s=c.prepareStatement("""
            SELECT r.*,o.draft_id,o.price,o.units,o.source,o.delivered_period,o.period AS purchase_period
            FROM construction_supply_recalls r JOIN construction_supply_orders o ON o.operation_id=r.order_id
            WHERE r.recall_operation=? ORDER BY r.order_id LIMIT 129
            """)){
            s.setString(1,operation.toString());try(var r=s.executeQuery()){while(r.next()){
                if(++count>128)throw new SQLException("Recall receipt budget exceeded");
                UUID current=UUID.fromString(r.getString("draft_id"));long time=r.getLong("recalled_ms"),epoch=r.getLong("period"),price=r.getLong("price");
                if(draft==null){draft=current;at=time;period=epoch;}
                if(!draft.equals(current)||at!=time||period!=epoch||!r.getString("source").equals("VILLAGE")||r.getObject("delivered_period")!=null||r.getLong("purchase_period")!=period
                        ||r.getLong("seller_before")-r.getLong("seller_after")!=price||r.getLong("buyer_after")-r.getLong("buyer_before")!=price
                        ||r.getLong("stock_after")-r.getLong("stock_before")!=r.getLong("units"))throw new SQLException("Recall receipt differs from original pending purchases");
                var refund=new TreasuryRepository(c).entry(UUID.fromString(r.getString("refund_operation"))).orElseThrow(()->new SQLException("Recall refund history missing"));
                var plan=new ConstructionDraftRepository(c).find(current).orElseThrow();
                if(!refund.settlement().equals(plan.settlement())||!refund.kind().equals("CREDIT")||refund.amount()!=price||refund.balanceAfter()!=r.getLong("buyer_after"))
                    throw new SQLException("Recall refund differs from saved treasury history");
                total=Math.addExact(total,price);
            }}
        }
        if(draft==null)return Optional.empty();
        try(var s=c.prepareStatement("SELECT count(*) FROM construction_supply_orders WHERE draft_id=?")){
            s.setString(1,draft.toString());try(var r=s.executeQuery()){if(r.getInt(1)!=count)throw new SQLException("Recall lost part of its original order set");}
        }
        return Optional.of(new Receipt(operation,draft,at,period,count,total));
    }
}
