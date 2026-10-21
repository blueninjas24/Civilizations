package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** Real local supplier payment -> owned cargo -> project stock. No native writes, new wallet or extra clock. */
public final class ConstructionSupplyOrders {
    public record Request(UUID operation,UUID draft,UUID seller,Source source,UUID business,Commodity commodity,long units,long period) {
        public Request {
            Objects.requireNonNull(operation);Objects.requireNonNull(draft);Objects.requireNonNull(seller);Objects.requireNonNull(source);Objects.requireNonNull(commodity);
            if((source==Source.BUSINESS)!=(business!=null)||units<1||units>16_000_000_000L||period<0||seller.equals(business))throw new IllegalArgumentException("Invalid construction purchase");
        }
    }
    public record Receipt(Request request,long price,long duePeriod,Long deliveredPeriod,long window,long cadence){}
    public record MaterialNeed(Commodity commodity,long required,long ordered,long inTransit,long delivered,long ready,long escrowed){
        public long missing(){return required-ordered;}
    }
    /** Readiness only: reference prices are not a supplier offer or payment/native authority. */
    public record VillageFunding(UUID draft,long period,List<MaterialNeed> materials,Long remainingReferenceValue,
                                 long cash,long protectedCash,long availableCash,long periodSpent,long haulRemaining,
                                 boolean canOrderAllNow,String explanation){public VillageFunding{materials=List.copyOf(materials);}}
    public static final class Declined extends SQLException {Declined(String reason){super(reason);}}
    private final Connection c;private final ConstructionBudgetPolicy budget;private final ConstructionSupplyPolicy supply;
    private final IndustryRuntimePolicy industry;private final BusinessOperationPolicy operations;
    public ConstructionSupplyOrders(Connection c,ConstructionBudgetPolicy budget,ConstructionSupplyPolicy supply,IndustryRuntimePolicy industry,BusinessOperationPolicy operations) {
        this.c=Objects.requireNonNull(c);this.budget=Objects.requireNonNull(budget);this.supply=Objects.requireNonNull(supply);this.industry=Objects.requireNonNull(industry);this.operations=Objects.requireNonNull(operations);
    }
    /** Settles prepaid saved cargo only; this instance cannot place new purchases. */
    public static ConstructionSupplyOrders deliveryOnly(Connection c) {
        return new ConstructionSupplyOrders(c,ConstructionBudgetPolicy.prototype(),
                new ConstructionSupplyPolicy(false,ConstructionSupplyPolicy.prototype().prices()),
                IndustryRuntimePolicy.disabled(),BusinessOperationPolicy.disabled());
    }
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){s.execute("""
            CREATE TABLE construction_supply_orders (
                operation_id TEXT PRIMARY KEY NOT NULL,draft_id TEXT NOT NULL REFERENCES construction_drafts(id),
                seller_id TEXT NOT NULL REFERENCES industry_stores(business_id),source TEXT NOT NULL CHECK(source IN('BUSINESS','VILLAGE')),
                business_id TEXT REFERENCES industry_stores(business_id),commodity TEXT NOT NULL,
                units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units BETWEEN 1 AND 16000000000),
                price INTEGER NOT NULL CHECK(typeof(price)='integer' AND price>0),
                period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=0),
                due_period INTEGER NOT NULL CHECK(typeof(due_period)='integer' AND due_period>period),
                delivered_period INTEGER CHECK(delivered_period IS NULL OR (typeof(delivered_period)='integer' AND delivered_period>=due_period)),
                haul_window INTEGER NOT NULL CHECK(typeof(haul_window)='integer' AND haul_window>=0),
                cadence INTEGER NOT NULL CHECK(typeof(cadence)='integer' AND cadence BETWEEN 1000 AND 86400000),
                buyer_before INTEGER NOT NULL CHECK(typeof(buyer_before)='integer' AND buyer_before>=price),
                buyer_after INTEGER NOT NULL CHECK(typeof(buyer_after)='integer' AND buyer_after=buyer_before-price),
                seller_before INTEGER NOT NULL CHECK(typeof(seller_before)='integer' AND seller_before>=0),
                seller_after INTEGER NOT NULL CHECK(typeof(seller_after)='integer' AND seller_after>=seller_before AND seller_after-seller_before=price),
                stock_before INTEGER NOT NULL CHECK(typeof(stock_before)='integer' AND stock_before>=units),
                stock_after INTEGER NOT NULL CHECK(typeof(stock_after)='integer' AND stock_after=stock_before-units),policy TEXT NOT NULL,
                CHECK((source='BUSINESS' AND business_id IS NOT NULL AND business_id<>seller_id) OR (source='VILLAGE' AND business_id IS NULL))
            )
            """);s.execute("CREATE INDEX construction_orders_by_draft ON construction_supply_orders(draft_id,operation_id)");}
    }
    public Receipt purchase(Request request,ConstructionMaterials.Authority authority,long now,Set<UUID> worlds)throws SQLException {
        return transaction(()->purchaseRows(request,authority,now,Set.copyOf(worlds)));
    }
    Receipt purchaseRows(Request q,ConstructionMaterials.Authority authority,long now,Set<UUID> worlds)throws SQLException {
        var old=find(q.operation());if(old.isPresent()){if(!old.orElseThrow().request().equals(q))throw new SQLException("Construction order identity changed");return old.orElseThrow();}
        if(!supply.enabled()||!industry.enabled())throw new Declined("Construction purchasing or industry disabled");
        var draft=draft(q.draft());
        if(!worlds.contains(draft.world())||!authority.draft().equals(q.draft())||!authority.world().equals(draft.world())||!authority.worldLoaded()||!authority.siteCleared()||now<authority.checkedMillis()||now-authority.checkedMillis()>5000)throw new Declined("Fresh exact-draft site evidence and present world required");
        new ConstructionSiteReservations(c).requireActiveRows(draft);
        try(var s=c.prepareStatement("SELECT 1 FROM construction_material_reservations WHERE draft_id=?")){s.setString(1,q.draft().toString());try(var r=s.executeQuery()){if(r.next())throw new Declined("Owned-stock and paid-supply routes require separate projects; no double reservation");}}
        Long need=draft.template().materials().get(q.commodity()),unitPrice=supply.prices().get(q.commodity());
        if(need==null||unitPrice==null)throw new Declined("Commodity is not required by the frozen plan or has no construction price");
        long committed=0;int count=0;
        try(var s=c.prepareStatement("SELECT source,business_id,commodity,units FROM construction_supply_orders WHERE draft_id=? LIMIT 129")) {
            s.setString(1,q.draft().toString());try(var r=s.executeQuery()){while(r.next()) {
                if(++count>=128)throw new Declined("Project order budget exhausted; existing goods retained");
                if(!r.getString(1).equals(q.source().name())||!Objects.equals(r.getString(2),q.business()==null?null:q.business().toString()))throw new Declined("Project funding source is already fixed");
                if(r.getString(3).equals(q.commodity().id()))committed=Math.addExact(committed,r.getLong(4));
            }}
        }
        if(committed>need||q.units()>need-committed)throw new Declined("Existing stock and incoming cargo already cover the frozen material need");
        var firms=new BusinessRepository(c);var seller=firms.findFounding(q.seller()).orElseThrow(()->new Declined("Saved supplier required"));
        if(!seller.business().settlementId().equals(draft.settlement())||!seller.site().worldId().equals(draft.world())||!staffed(q.seller()))throw new Declined("Qualified operating local supplier required");
        var stores=new IndustryRepository(c);var from=stores.store(q.seller()).orElseThrow();long stock=from.units().getOrDefault(q.commodity(),0L);
        if(stock<q.units())throw new Declined("Supplier lacks actual material stock");
        long cash,food=0,essential=0;long period=completed(q,draft,true,now);if(period!=q.period())throw new Declined("Construction order requires the current earned owner period without backlog");
        if(q.source()==Source.BUSINESS) {
            var buyer=firms.findFounding(q.business()).orElseThrow();
            if(!buyer.business().settlementId().equals(draft.settlement())||!buyer.site().worldId().equals(draft.world())||!staffed(q.business()))throw new Declined("Qualified same-village business purchaser required");
            var store=stores.store(q.business()).orElseThrow();cash=store.hundredths();
            if(new BusinessTools(c,industry).replacementDue(q.business(),buyer.business().industry(),store)&&store.units().getOrDefault(ToolUpkeepPolicy.TOOLS,0L)==0)essential=industry.tools().toolValueHundredths();
        }else {cash=new TreasuryRepository(c).load(draft.settlement()).orElseThrow(()->new Declined("Real village treasury required")).hundredths();food=foodDemand(draft.settlement(),period);}
        long spent=spent(q.source(),q.source()==Source.VILLAGE?draft.settlement():q.business(),period);
        long price=Math.multiplyExact(unitPrice,q.units());var quote=budget.review(q.source(),draft.template().purpose(),price,new Evidence(cash,food,essential,0,spent));
        if(!quote.allowed())throw new Declined(quote.reason());
        long cadence=industry.millisPerPeriod(),window=now/cadence,hailed=allHaulUsed(draft.settlement(),window,cadence);
        if(hailed>industry.mining().localHaulUnitsPerPeriod()||q.units()>industry.mining().localHaulUnitsPerPeriod()-hailed)throw new Declined("Shared local haul capacity exhausted");
        if(q.source()==Source.VILLAGE) {
            if(new TreasuryRepository(c).entry(q.operation()).isPresent())throw new SQLException("Order identity already used by treasury history");
            new TreasuryRepository(c).postRows(q.operation(),draft.settlement(),-price,"Construction materials for frozen project "+draft.id());
        }else cash(q.business(),cash,cash-price);
        cash(q.seller(),from.hundredths(),Math.addExact(from.hundredths(),price));
        try(var s=c.prepareStatement("UPDATE industry_stock SET units=? WHERE business_id=? AND commodity=? AND units=?")) {
            s.setLong(1,stock-q.units());s.setString(2,q.seller().toString());s.setString(3,q.commodity().id());s.setLong(4,stock);if(s.executeUpdate()!=1)throw new SQLException("Supplier stock changed");
        }
        try(var s=c.prepareStatement("INSERT INTO construction_supply_orders VALUES(?,?,?,?,?,?,?,?,?,?,NULL,?,?,?,?,?,?,?,?,?)")) {
            int n=1;s.setString(n++,q.operation().toString());s.setString(n++,q.draft().toString());s.setString(n++,q.seller().toString());s.setString(n++,q.source().name());s.setString(n++,q.business()==null?null:q.business().toString());s.setString(n++,q.commodity().id());s.setLong(n++,q.units());s.setLong(n++,price);s.setLong(n++,period);s.setLong(n++,Math.addExact(period,industry.mining().localDeliveryPeriods()));
            s.setLong(n++,window);s.setLong(n++,cadence);s.setLong(n++,cash);s.setLong(n++,cash-price);s.setLong(n++,from.hundredths());s.setLong(n++,Math.addExact(from.hundredths(),price));s.setLong(n++,stock);s.setLong(n++,stock-q.units());s.setString(n,budget+"|"+supply);s.executeUpdate();
        }return find(q.operation()).orElseThrow();
    }
    /** Existing earned owner periods supply time; absent worlds cannot deliver or create elapsed time. */
    public Receipt deliver(UUID operation,Set<UUID> worlds)throws SQLException {
        var present=Set.copyOf(worlds);
        return transaction(()->deliverRows(operation,present));
    }
    private Receipt deliverRows(UUID operation,Set<UUID> worlds)throws SQLException {
        var receipt=find(operation).orElseThrow(()->new SQLException("Unknown construction cargo"));if(receipt.deliveredPeriod()!=null||new ConstructionSupplyRecalls(c).recalled(receipt.request().draft()))return receipt;
        var d=draft(receipt.request().draft());if(!worlds.contains(d.world()))return receipt;
        long period=completed(receipt.request(),d,false,0);if(period<receipt.duePeriod())return receipt;
        try(var s=c.prepareStatement("UPDATE construction_supply_orders SET delivered_period=? WHERE operation_id=? AND delivered_period IS NULL")){s.setLong(1,period);s.setString(2,operation.toString());if(s.executeUpdate()!=1)throw new SQLException("Construction cargo changed");}
        return find(operation).orElseThrow();
    }
    /** One bounded owner transaction; absent/early cargo cannot block ready cargo in another world. */
    public int deliverReady(Set<UUID> worlds,int limit)throws SQLException {
        var present=Set.copyOf(worlds);
        if(limit<1||limit>64||present.size()>256)throw new IllegalArgumentException("Construction delivery review exceeds its bounds");
        if(present.isEmpty())return 0;
        return transaction(()->{
            var ids=new ArrayList<UUID>();
            String places=String.join(",",Collections.nCopies(present.size(),"?"));
            try(var s=c.prepareStatement("""
                SELECT o.operation_id FROM construction_supply_orders o
                JOIN construction_drafts d ON d.id=o.draft_id
                LEFT JOIN industry_runtime i ON i.business_id=o.business_id
                LEFT JOIN economy_accounts a ON a.settlement_id=d.settlement_id
                WHERE o.delivered_period IS NULL AND NOT EXISTS(SELECT 1 FROM construction_supply_recalls x WHERE x.order_id=o.operation_id) AND
                ((o.source='BUSINESS' AND i.completed>=o.due_period) OR
                 (o.source='VILLAGE' AND a.completed_periods>=o.due_period))
                AND d.world_id IN (
                """+places+") ORDER BY o.due_period,o.operation_id LIMIT ?")) {
                int n=1;for(var world:new TreeSet<>(present))s.setString(n++,world.toString());s.setInt(n,limit);
                try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
            }
            for(var id:ids)deliverRows(id,present);
            return ids.size();
        });
    }
    public Map<Commodity,Long> stock(UUID draft)throws SQLException {
        var plan=draft(draft);var result=new TreeMap<Commodity,Long>();int count=0;
        try(var s=c.prepareStatement("SELECT commodity,units FROM construction_supply_orders WHERE draft_id=? AND delivered_period IS NOT NULL LIMIT 129")) {
            s.setString(1,draft.toString());try(var r=s.executeQuery()){while(r.next()){if(++count>128)throw new SQLException("Construction cargo history budget exceeded");result.merge(new Commodity(r.getString(1)),r.getLong(2),Math::addExact);}}
        }
        for(var e:result.entrySet())if(e.getValue()>plan.template().materials().getOrDefault(e.getKey(),0L))throw new SQLException("Delivered construction stock exceeds frozen need");return ConstructionPublicWork.unallocated(c,plan,result);
    }
    /** One read-only snapshot of actual orders, escrow, current fed population, reserves and shared haul. */
    public VillageFunding quoteVillageMaterials(UUID draftId,long now,Set<UUID> worlds)throws SQLException{
        var present=Set.copyOf(worlds);if(now<0)throw new IllegalArgumentException("Negative funding observation time");
        return transaction(()->quoteVillageMaterialsRows(draftId,now,present));
    }
    VillageFunding quoteVillageMaterialsRows(UUID draftId,long now,Set<UUID> present)throws SQLException{
        if(c.getAutoCommit())throw new SQLException("Funding snapshot requires the SQL owner transaction");
            var plan=draft(draftId);if(!present.contains(plan.world()))throw new Declined("Absent world cannot authorize a current funding quote");
            new ConstructionSiteReservations(c).requireActiveRows(plan);
            try(var s=c.prepareStatement("SELECT 1 FROM construction_material_reservations WHERE draft_id=?")){
                s.setString(1,draftId.toString());try(var r=s.executeQuery()){if(r.next())throw new Declined("Owned-stock projects retain their separate funding route");}
            }
            var ordered=new TreeMap<Commodity,Long>();var delivered=new TreeMap<Commodity,Long>();int count=0;
            try(var s=c.prepareStatement("SELECT source,business_id,commodity,units,delivered_period FROM construction_supply_orders WHERE draft_id=? LIMIT 129")){
                s.setString(1,draftId.toString());try(var r=s.executeQuery()){while(r.next()){
                    if(++count>128)throw new Declined("Project receipt review budget exceeded; goods retained");
                    if(!r.getString(1).equals("VILLAGE")||r.getString(2)!=null)throw new Declined("Project already uses a different funding source");
                    var commodity=new Commodity(r.getString(3));long units=r.getLong(4);ordered.merge(commodity,units,Math::addExact);
                    if(r.getObject(5)!=null)delivered.merge(commodity,units,Math::addExact);
                }}
            }
            for(var e:ordered.entrySet())if(e.getValue()>plan.template().materials().getOrDefault(e.getKey(),0L))throw new SQLException("Construction commitments exceed frozen material need");
            var ready=stock(draftId);var needs=new ArrayList<MaterialNeed>();Long reference=0L;long missingUnits=0,largestLine=0;
            for(var e:plan.template().materials().entrySet()){
                long total=ordered.getOrDefault(e.getKey(),0L),arrived=delivered.getOrDefault(e.getKey(),0L),free=ready.getOrDefault(e.getKey(),0L),missing=e.getValue()-total;
                needs.add(new MaterialNeed(e.getKey(),e.getValue(),total,total-arrived,arrived,free,arrived-free));missingUnits=Math.addExact(missingUnits,missing);
                if(missing>0){Long price=supply.prices().get(e.getKey());if(price==null)reference=null;
                    else try{long line=Math.multiplyExact(price,missing);largestLine=Math.max(largestLine,line);if(reference!=null)reference=Math.addExact(reference,line);}
                    catch(ArithmeticException unsupported){reference=null;}
                }
            }
            long period=completed(Source.VILLAGE,null,plan,true,now),food=foodDemand(plan.settlement(),period);
            long cash=new TreasuryRepository(c).load(plan.settlement()).orElseThrow(()->new Declined("Real village treasury required")).hundredths();
            long spent=spent(Source.VILLAGE,plan.settlement(),period);var terms=budget.village();
            var quote=budget.review(Source.VILLAGE,plan.template().purpose(),0,new Evidence(cash,food,0,0,spent));
            long haul=Math.max(0,industry.mining().localHaulUnitsPerPeriod()-allHaulUsed(plan.settlement(),now/industry.millisPerPeriod(),industry.millisPerPeriod()));
            boolean permitted=supply.enabled()&&industry.enabled()&&quote.allowed();
            boolean affordable=reference!=null&&reference<=quote.availableCash()&&spent<=terms.perPeriodLimit()&&reference<=terms.perPeriodLimit()-spent&&largestLine<=terms.maximumPurchase();
            boolean canOrder=missingUnits>0&&permitted&&affordable&&missingUnits<=haul;
            String reason=missingUnits==0?"All frozen inputs already ordered; no further purchase requested":!permitted?"Construction purchasing disabled or current funding policy holds":reference==null?"Unpriced or unsupported remaining material value; never treated as free"
                    :!affordable?"Remaining inputs require smaller orders, later spending windows or more unprotected cash":missingUnits>haul?"Remaining inputs exceed this shared hauling window"
                    :"Current funds and haul can cover reference-priced inputs; real suppliers, fresh site evidence and live payment checks still required";
            return new VillageFunding(draftId,period,needs,reference,cash,quote.protectedCash(),quote.availableCash(),spent,haul,canOrder,reason);
    }
    public Optional<Receipt> find(UUID operation)throws SQLException {
        try(var s=c.prepareStatement("SELECT * FROM construction_supply_orders WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()){if(!r.next())return Optional.empty();
                try {
                    String business=r.getString("business_id");var q=new Request(operation,UUID.fromString(r.getString("draft_id")),UUID.fromString(r.getString("seller_id")),Source.valueOf(r.getString("source")),business==null?null:UUID.fromString(business),new Commodity(r.getString("commodity")),r.getLong("units"),r.getLong("period"));
                    var d=draft(q.draft());if(q.units()>d.template().materials().getOrDefault(q.commodity(),0L))throw new SQLException("Construction order differs from frozen need");
                    long delivered=r.getLong("delivered_period");Long arrival=r.wasNull()?null:delivered;
                    return Optional.of(new Receipt(q,r.getLong("price"),r.getLong("due_period"),arrival,r.getLong("haul_window"),r.getLong("cadence")));
                }catch(IllegalArgumentException invalid){throw new SQLException("Invalid construction purchase receipt",invalid);}
            }
        }
    }
    private long completed(Request q,ConstructionDraft d,boolean requireCurrent,long now)throws SQLException {
        return completed(q.source(),q.business(),d,requireCurrent,now);
    }
    private long completed(Source source,UUID business,ConstructionDraft d,boolean requireCurrent,long now)throws SQLException{
        String sql=source==Source.BUSINESS?"SELECT completed,target,cadence,last_ms FROM industry_runtime WHERE business_id=?":"SELECT a.completed_periods,r.target_periods,r.millis_per_period,r.last_observed_millis FROM economy_accounts a JOIN economy_runtime r USING(settlement_id) WHERE a.settlement_id=?";
        try(var s=c.prepareStatement(sql)){s.setString(1,(source==Source.BUSINESS?business:d.settlement()).toString());try(var r=s.executeQuery()){if(!r.next()||(requireCurrent&&(r.getLong(1)!=r.getLong(2)||now<r.getLong(4)||(source==Source.BUSINESS&&r.getLong(3)!=industry.millisPerPeriod()))))throw new Declined("Current saved owner clock required; no invented project time");return r.getLong(1);}}
    }
    private long spent(Source source,UUID owner,long period)throws SQLException{
        long total=0;String spending=source==Source.VILLAGE?"d.settlement_id=? AND o.source='VILLAGE'":"o.business_id=? AND o.source='BUSINESS'";
        try(var s=c.prepareStatement("SELECT o.price FROM construction_supply_orders o JOIN construction_drafts d ON d.id=o.draft_id WHERE "+spending+" AND o.period=?")){
            s.setString(1,owner.toString());s.setLong(2,period);try(var r=s.executeQuery()){while(r.next())total=Math.addExact(total,r.getLong(1));}
        }return total;
    }
    private long allHaulUsed(UUID town,long window,long cadence)throws SQLException{
        long total=StorageUpgrades.haulUsed(c,town,window,cadence);
        try(var s=c.prepareStatement("SELECT t.units FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND a.haul_window=? AND a.haul_cadence=?")){
            s.setString(1,town.toString());s.setLong(2,window);s.setLong(3,cadence);try(var r=s.executeQuery()){while(r.next())total=Math.addExact(total,r.getLong(1));}
        }return total;
    }
    private long foodDemand(UUID town,long period)throws SQLException {
        try(var s=c.prepareStatement("""
            SELECT n.demand,n.fulfilled,n.unassigned_residents+COALESCE((SELECT SUM(h.adults+h.children) FROM household_needs h WHERE h.settlement_id=n.settlement_id AND h.period=n.period),0),
                (SELECT COUNT(*) FROM residents r WHERE r.settlement_id=n.settlement_id AND r.status='ALIVE') FROM needs_periods n WHERE n.settlement_id=? AND n.period=?
            """)) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){if(!r.next()||r.getLong(2)<r.getLong(1)||r.getLong(3)!=r.getLong(4))throw new Declined("Public construction waits for a current complete fed population assessment");return r.getLong(1);}
        }
    }
    boolean staffed(UUID id)throws SQLException {
        if(!new BusinessOperations(c,operations).evaluate(id).permitted())return false;WorkplaceOperations.requirePermitted(c,id);
        var firms=new BusinessRepository(c);var f=firms.findFounding(id).orElseThrow();var people=new PopulationRepository(c);
        for(var job:firms.currentWorkers(id)){var p=people.findResident(job.residentId()).orElseThrow();if(p.settlementId().equals(f.business().settlementId())&&WorkEligibility.evaluate(p,people.findWorkProfile(p.id()).orElseThrow(),f.workerRequirements()).eligible())return true;}return false;
    }
    private void cash(UUID business,long before,long after)throws SQLException {try(var s=c.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=? AND hundredths=?")){s.setLong(1,after);s.setString(2,business.toString());s.setLong(3,before);if(s.executeUpdate()!=1)throw new SQLException("Construction payment balance changed");}}
    private ConstructionDraft draft(UUID id)throws SQLException{return new ConstructionDraftRepository(c).find(id).orElseThrow(()->new SQLException("Frozen construction plan required"));}
    static long haulUsed(Connection c,UUID town,long window,long cadence)throws SQLException {
        long result=0;try(var s=c.prepareStatement("SELECT o.units FROM construction_supply_orders o JOIN construction_drafts d ON d.id=o.draft_id WHERE d.settlement_id=? AND o.haul_window=? AND o.cadence=?")){s.setString(1,town.toString());s.setLong(2,window);s.setLong(3,cadence);try(var r=s.executeQuery()){while(r.next())result=Math.addExact(result,r.getLong(1));}}return result;
    }
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {if(!c.getAutoCommit())throw new SQLException("Construction purchase requires idle SQL owner");c.setAutoCommit(false);try{T v=work.run();c.commit();c.setAutoCommit(true);return v;}catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}}
}
