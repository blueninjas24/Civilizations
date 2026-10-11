package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Owner-thread finite minerals, business-owned stock/capital and delayed same-town purchases. */
public final class IndustryRepository {
    public record Deposit(UUID id, UUID siteId, long initialBatches, long remainingBatches, long observedOre,
                          Map<Commodity,Long> bundle) { public Deposit { bundle=Map.copyOf(bundle); } }
    public record Store(UUID businessId,long capacity,long hundredths,Map<Commodity,Long> units,
                        Map<Commodity,Long> spacePerUnit,long reservedIncomingSpace) {
        public Store { units=Map.copyOf(units); spacePerUnit=Map.copyOf(spacePerUnit); }
        public long usedSpace() {
            long sum=0; for(var entry:units.entrySet())sum=Math.addExact(sum,Math.multiplyExact(entry.getValue(),spacePerUnit.get(entry.getKey())));
            return sum;
        }
        public long freeSpace() { return Math.subtractExact(Math.subtractExact(capacity,usedSpace()),reservedIncomingSpace); }
    }
    public record Receipt(UUID id,long quantity,String explanation) {}
    public record Shipment(UUID id,UUID seller,UUID buyer,Commodity commodity,long units,long price,
                           long dispatchedPeriod,long duePeriod,Long deliveredPeriod) {}
    public enum Use { HOUSEHOLD_NEED, INDUSTRY_INPUT, MAINTENANCE }
    public record RecipeRun(UUID operation,UUID business,long period,String recipe,long requested,long workers,
                            long batchesPerWorker,long siteCapacity,Receipt receipt,
                            Map<Commodity,Long> inputPerBatch,Map<Commodity,Long> outputPerBatch,
                            Map<Commodity,Long> consumed,Map<Commodity,Long> produced) {
        public RecipeRun {
            inputPerBatch=Map.copyOf(inputPerBatch);outputPerBatch=Map.copyOf(outputPerBatch);
            consumed=Map.copyOf(consumed);produced=Map.copyOf(produced);
        }
    }
    private final Connection connection;
    private final BusinessRepository businesses;
    public IndustryRepository(Connection connection) throws SQLException {
        this.connection=Objects.requireNonNull(connection); businesses=new BusinessRepository(connection);
    }

    /** Trusted adapter discovery only. New IDs cannot reopen the same saved site; depleted deposits remain depleted. */
    public Deposit discover(UUID id,UUID site,long observedOre,long initialBatches,Map<Commodity,Long> bundle) throws SQLException {
        return transaction(()->discoverRows(id,site,observedOre,initialBatches,bundle));
    }
    /** Coordinate discovery composes these rows with its evidence in the same SQL-owner transaction. */
    Deposit discoverRows(UUID id,UUID site,long observedOre,long initialBatches,Map<Commodity,Long> bundle) throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Deposit rows require an owner transaction");
        Objects.requireNonNull(id); Objects.requireNonNull(site); bundle=Map.copyOf(bundle);
        if(observedOre<1 || initialBatches<1 || bundle.isEmpty() || bundle.size()>32 || bundle.values().stream().anyMatch(v->v<1))
            throw new IllegalArgumentException("Invalid finite mineral discovery");
        String terms=terms("deposit",id,site,observedOre,initialBatches,catalog(bundle));
        Map<Commodity,Long> yields=bundle;
            try(var s=connection.prepareStatement("SELECT terms FROM mineral_deposits WHERE id=?")) {
                s.setString(1,id.toString());try(var r=s.executeQuery()) {
                    if(r.next()) { if(!terms.equals(r.getString(1)))throw new SQLException("Deposit identity reused with different discovery"); return deposit(id).orElseThrow(); }
                }
            }
            try(var s=connection.prepareStatement("INSERT INTO mineral_deposits(id,site_id,initial_batches,remaining_batches,observed_ore,terms) VALUES(?,?,?,?,?,?)")) {
                s.setString(1,id.toString());s.setString(2,site.toString());s.setLong(3,initialBatches);s.setLong(4,initialBatches);s.setLong(5,observedOre);s.setString(6,terms);s.executeUpdate();
            }
            for(var entry:new TreeMap<>(yields).entrySet())try(var s=connection.prepareStatement("INSERT INTO mineral_yields(deposit_id,commodity,units) VALUES(?,?,?)")) {
                s.setString(1,id.toString());s.setString(2,entry.getKey().id());s.setLong(3,entry.getValue());s.executeUpdate();
            }
            return deposit(id).orElseThrow();
    }

    public Optional<Deposit> deposit(UUID id) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM mineral_deposits WHERE id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                return Optional.of(new Deposit(id,UUID.fromString(r.getString("site_id")),r.getLong("initial_batches"),r.getLong("remaining_batches"),
                        r.getLong("observed_ore"),readCatalog("SELECT commodity,units FROM mineral_yields WHERE deposit_id=?",id)));
            }
        }
    }

    /** Starts empty. Capital is transferred from real village funds, never minted from stock or profession. */
    public Store openStore(UUID business,long capacity,Map<Commodity,Long> space,long capital) throws SQLException {
        space=Map.copyOf(space);
        if(capacity<1 || capital<0 || space.isEmpty() || space.size()>64 || space.values().stream().anyMatch(v->v<1))
            throw new IllegalArgumentException("Invalid business store opening");
        Map<Commodity,Long> sizes=space;
        return transaction(()->openStoreRows(business,capacity,sizes,capital));
    }
    Store openStoreRows(UUID business,long capacity,Map<Commodity,Long> space,long capital) throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Store rows require an owner transaction");
        space=Map.copyOf(space);
        if(capacity<1 || capital<0 || space.isEmpty() || space.size()>64 || space.values().stream().anyMatch(v->v<1))
            throw new IllegalArgumentException("Invalid business store opening");
        String terms=terms("store",business,capacity,catalog(space),capital);Map<Commodity,Long> sizes=space;
        var b=businesses.findBusiness(business).orElseThrow(()->new SQLException("Unknown business"));
        try(var s=connection.prepareStatement("SELECT terms FROM industry_stores WHERE business_id=?")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()) {
                if(r.next()) { if(!terms.equals(r.getString(1)))throw new SQLException("Store opening terms changed; explicit capacity transition required"); return store(business).orElseThrow(); }
            }
        }
        BusinessClosure.requireOpen(connection,business);
        if(capital>0)new TreasuryRepository(connection).postRows(UUID.nameUUIDFromBytes(("civilizations:business-capital:"+business).getBytes(StandardCharsets.UTF_8)),
                b.settlementId(),-capital,"Business working capital allocation");
        try(var s=connection.prepareStatement("INSERT INTO industry_stores(business_id,capacity,hundredths,terms) VALUES(?,?,?,?)")) {
            s.setString(1,business.toString());s.setLong(2,capacity);s.setLong(3,capital);s.setString(4,terms);s.executeUpdate();
        }
        for(var entry:new TreeMap<>(sizes).entrySet())try(var s=connection.prepareStatement("INSERT INTO industry_stock(business_id,commodity,space_per_unit,units) VALUES(?,?,?,0)")) {
            s.setString(1,business.toString());s.setString(2,entry.getKey().id());s.setLong(3,entry.getValue());s.executeUpdate();
        }
        return store(business).orElseThrow();
    }

    public Optional<Store> store(UUID business) throws SQLException {
        try(var s=connection.prepareStatement("SELECT capacity,hundredths FROM industry_stores WHERE business_id=?")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                long reserved=0;
                try(var q=connection.prepareStatement("""
                        SELECT t.units,k.space_per_unit FROM local_shipments t JOIN industry_stock k
                        ON k.business_id=t.buyer_id AND k.commodity=t.commodity
                        WHERE t.buyer_id=? AND t.delivered_period IS NULL
                        """)) {
                    q.setString(1,business.toString());try(var pending=q.executeQuery()) {
                        while(pending.next())reserved=Math.addExact(reserved,Math.multiplyExact(pending.getLong(1),pending.getLong(2)));
                    }
                }
                var result=new Store(business,r.getLong(1),r.getLong(2),readCatalog("SELECT commodity,units FROM industry_stock WHERE business_id=?",business),
                        readCatalog("SELECT commodity,space_per_unit FROM industry_stock WHERE business_id=?",business),reserved);
                if(result.freeSpace()<0)throw new SQLException("Business storage exceeds saved capacity");
                return Optional.of(result);
            }
        }
    }

    /** Exactly one mining plan per business/economic period. Output, rock byproducts, depletion and receipt commit together. */
    public Receipt mine(UUID operation,UUID business,UUID deposit,long requested,long period,long perWorker,long siteCapacity) throws SQLException {
        if(requested<0 || period<0 || perWorker<1 || siteCapacity<1)throw new IllegalArgumentException("Invalid production request");
        return transaction(()->mineRows(operation,business,deposit,requested,period,perWorker,siteCapacity));
    }
    Receipt mineRows(UUID operation,UUID business,UUID deposit,long requested,long period,long perWorker,long siteCapacity) throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Production rows require active SQL owner transaction");
        if(requested<0 || period<0 || perWorker<1 || siteCapacity<1)throw new IllegalArgumentException("Invalid production request");
        String terms=terms("mine",business,deposit,requested,period,perWorker,siteCapacity);
        var old=receipt(operation,terms);if(old.isPresent())return old.orElseThrow();
        BusinessClosure.requireOpen(connection,business);
        try(var s=connection.prepareStatement("SELECT MAX(period) FROM industry_production WHERE business_id=?")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()) {
                if(r.next()) { long last=r.getLong(1);if(!r.wasNull() && period<=last)throw new SQLException("Mining period is stale or already completed"); }
            }
        }
        var b=businesses.findBusiness(business).orElseThrow(()->new SQLException("Unknown business"));
        var d=deposit(deposit).orElseThrow(()->new SQLException("Unknown deposit"));
        if(!b.industry().equals("mining") || !b.siteId().equals(d.siteId()))throw new SQLException("Mining business must occupy the actual deposit site");
        var stock=store(business).orElseThrow(()->new SQLException("Missing business storage"));
        var sources=new MineralSources(connection);sources.ensureRows(deposit);
        var plan=MineProduction.calculate(d.remainingBatches(),requested,businesses.currentWorkers(business).size(),perWorker,siteCapacity,
                stock.freeSpace(),d.bundle(),stock.spacePerUnit());
        for(var output:plan.outputs().entrySet())changeStock(business,output.getKey(),output.getValue());
        try(var s=connection.prepareStatement("UPDATE mineral_deposits SET remaining_batches=? WHERE id=?")) {
            s.setLong(1,d.remainingBatches()-plan.batches());s.setString(2,deposit.toString());s.executeUpdate();
        }
        var saved=save(operation,terms,plan.batches(),plan.explanation());
        try(var s=connection.prepareStatement("INSERT INTO industry_production(business_id,period,operation_id) VALUES(?,?,?)")) {
            s.setString(1,business.toString());s.setLong(2,period);s.setString(3,operation.toString());s.executeUpdate();
        }
        sources.depleteRows(operation,deposit,plan.batches());
        return saved;
    }

    /** Complete processing batches atomically consume delivered inputs and add outputs; no new money. */
    public RecipeRun process(UUID operation,UUID business,ProductionRecipe recipe,long requested,long period) throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(business);Objects.requireNonNull(recipe);
        if(requested<0 || period<0)throw new IllegalArgumentException("Invalid recipe request");
        return transaction(()->processRows(operation,business,recipe,requested,period));
    }
    RecipeRun processRows(UUID operation,UUID business,ProductionRecipe recipe,long requested,long period) throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Production rows require active SQL owner transaction");
        Objects.requireNonNull(operation);Objects.requireNonNull(business);Objects.requireNonNull(recipe);
        if(requested<0 || period<0)throw new IllegalArgumentException("Invalid recipe request");
        String operationTerms=terms("recipe-v1",business,recipe.id(),recipe.industry(),catalog(recipe.inputs()),catalog(recipe.outputs()),
                recipe.batchesPerWorker(),recipe.siteCapacity(),requested,period);
        if(receipt(operation,operationTerms).isPresent())return recipeRun(operation).orElseThrow(()->new SQLException("Recipe receipt missing run"));
        BusinessClosure.requireOpen(connection,business);
        var b=businesses.findBusiness(business).orElseThrow(()->new SQLException("Unknown business"));
        if(!b.industry().equals(recipe.industry()))throw new SQLException("Recipe does not match business industry");
        try(var s=connection.prepareStatement("SELECT MAX(period) FROM industry_production WHERE business_id=?")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()) {
                if(r.next()){long last=r.getLong(1);if(!r.wasNull() && period<=last)throw new SQLException("Production period is stale or already completed");}
            }
        }
        var stock=store(business).orElseThrow(()->new SQLException("Missing business store"));
        var goods=new HashSet<>(recipe.inputs().keySet());goods.addAll(recipe.outputs().keySet());
        if(!stock.units().keySet().containsAll(goods))throw new SQLException("Store catalog does not support recipe; explicit store transition required");
        int workers=businesses.currentWorkers(business).size();
        var plan=RecipeProduction.calculate(recipe,requested,workers,stock.freeSpace(),stock.units(),stock.spacePerUnit());
        for(var e:plan.consumed().entrySet())changeStock(business,e.getKey(),-e.getValue());
        for(var e:plan.produced().entrySet())changeStock(business,e.getKey(),e.getValue());
        save(operation,operationTerms,plan.batches(),plan.explanation());
        try(var s=connection.prepareStatement("INSERT INTO industry_production(business_id,period,operation_id) VALUES(?,?,?)")) {
            s.setString(1,business.toString());s.setLong(2,period);s.setString(3,operation.toString());s.executeUpdate();
        }
        try(var s=connection.prepareStatement("INSERT INTO recipe_runs VALUES(?,?,?,?,1,?,?,?,?)")) {
            s.setString(1,operation.toString());s.setString(2,business.toString());s.setLong(3,period);s.setString(4,recipe.id());
            s.setLong(5,requested);s.setLong(6,workers);s.setLong(7,recipe.batchesPerWorker());s.setLong(8,recipe.siteCapacity());s.executeUpdate();
        }
        saveRecipeFlows(operation,"INPUT",recipe.inputs(),plan.consumed());
        saveRecipeFlows(operation,"OUTPUT",recipe.outputs(),plan.produced());
        store(business).orElseThrow();return recipeRun(operation).orElseThrow();
    }

    private void saveRecipeFlows(UUID operation,String direction,Map<Commodity,Long> perBatch,Map<Commodity,Long> amounts) throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO recipe_flows VALUES(?,?,?,?,?)")) {
            for(var e:new TreeMap<>(perBatch).entrySet()) {
                s.setString(1,operation.toString());s.setString(2,e.getKey().id());s.setString(3,direction);
                s.setLong(4,e.getValue());s.setLong(5,amounts.get(e.getKey()));s.executeUpdate();
            }
        }
    }
    public Optional<RecipeRun> recipeRun(UUID operation) throws SQLException {
        try(var s=connection.prepareStatement("SELECT r.*,o.quantity,o.explanation FROM recipe_runs r JOIN industry_operations o ON o.id=r.operation_id WHERE r.operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                var input=new TreeMap<Commodity,Long>();var output=new TreeMap<Commodity,Long>();
                var consumed=new TreeMap<Commodity,Long>();var produced=new TreeMap<Commodity,Long>();
                try(var f=connection.prepareStatement("SELECT * FROM recipe_flows WHERE operation_id=? ORDER BY commodity,direction")) {
                    f.setString(1,operation.toString());try(var row=f.executeQuery()){while(row.next()) {
                        var good=new Commodity(row.getString("commodity"));boolean in=row.getString("direction").equals("INPUT");
                        (in?input:output).put(good,row.getLong("per_batch"));(in?consumed:produced).put(good,row.getLong("units"));
                    }}
                }
                if(input.isEmpty() || output.isEmpty())throw new SQLException("Recipe run missing material history");
                return Optional.of(new RecipeRun(operation,UUID.fromString(r.getString("business_id")),r.getLong("period"),r.getString("recipe_id"),
                        r.getLong("requested"),r.getLong("workers"),r.getLong("batches_per_worker"),r.getLong("site_capacity"),
                        new Receipt(operation,r.getLong("quantity"),r.getString("explanation")),input,output,consumed,produced));
            }
        }
    }

    /** Buyer owns reserved cargo after payment. Actual money transfers once; destination storage is reserved in advance. */
    public Shipment purchase(UUID operation,UUID seller,UUID buyer,Commodity commodity,long units,long price,long period,long delay,long haulCapacity) throws SQLException {
        if(seller.equals(buyer) || units<1 || price<0 || period<0 || delay<1 || haulCapacity<1)throw new IllegalArgumentException("Invalid local purchase");
        Math.addExact(period,delay);
        return transaction(()->purchaseRows(operation,seller,buyer,commodity,units,price,period,delay,haulCapacity,null));
    }
    Shipment purchaseRows(UUID operation,UUID seller,UUID buyer,Commodity commodity,long units,long price,long period,long delay,long haulCapacity,Long haulWindow) throws SQLException {
        return purchaseRows(operation,seller,buyer,commodity,units,price,period,delay,haulCapacity,haulWindow,null);
    }
    Shipment purchaseRows(UUID operation,UUID seller,UUID buyer,Commodity commodity,long units,long price,long period,long delay,long haulCapacity,Long haulWindow,Long haulCadence) throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Shipment rows require active SQL owner transaction");
        if(seller.equals(buyer) || units<1 || price<0 || period<0 || delay<1 || haulCapacity<1)throw new IllegalArgumentException("Invalid local purchase");
        if(haulWindow!=null && haulWindow<0)throw new IllegalArgumentException("Negative shared hauling window");
        if(haulCadence!=null && (haulWindow==null || haulCadence<1000 || haulCadence>86400000))throw new IllegalArgumentException("Invalid hauling clock identity");
        long due=Math.addExact(period,delay);String terms=haulWindow==null?terms("purchase",seller,buyer,commodity.id(),units,price,period,delay,haulCapacity)
                :haulCadence==null?terms("purchase-clock-v1",seller,buyer,commodity.id(),units,price,period,delay,haulCapacity,haulWindow)
                :terms("purchase-clock-v2",seller,buyer,commodity.id(),units,price,period,delay,haulCapacity,haulWindow,haulCadence);
        if(receipt(operation,terms).isPresent())return shipment(operation).orElseThrow();
        BusinessClosure.requireOpen(connection,seller);BusinessClosure.requireOpen(connection,buyer);
        var origin=businesses.findBusiness(seller).orElseThrow(()->new SQLException("Unknown seller"));
        var destination=businesses.findBusiness(buyer).orElseThrow(()->new SQLException("Unknown buyer"));
        if(!origin.settlementId().equals(destination.settlementId()))throw new SQLException("Local transport cannot cross settlements; regional routes belong to Phase 5");
        long dispatched=0;
        String haulingSql=haulWindow==null?"SELECT t.units FROM local_shipments t JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND t.dispatched_period=? AND NOT EXISTS(SELECT 1 FROM industry_auto_shipments a WHERE a.shipment_id=t.id)"
                :"SELECT t.units FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND a.haul_window=? AND "
                +(haulCadence==null?"a.haul_cadence IS NULL":"a.haul_cadence=?");
        try(var s=connection.prepareStatement(haulingSql)) {
            s.setString(1,origin.settlementId().toString());s.setLong(2,haulWindow==null?period:haulWindow);
            if(haulCadence!=null)s.setLong(3,haulCadence);
            try(var r=s.executeQuery()) { while(r.next())dispatched=Math.addExact(dispatched,r.getLong(1)); }
        }
        if(Math.addExact(dispatched,units)>haulCapacity)throw new SQLException("Local haul capacity exhausted for this period");
        var from=store(seller).orElseThrow(()->new SQLException("Seller store missing"));
        var to=store(buyer).orElseThrow(()->new SQLException("Buyer store missing"));
        Long size=to.spacePerUnit().get(commodity);
        if(size==null || Math.multiplyExact(size,units)>to.freeSpace())throw new SQLException("Destination lacks supported free storage");
        if(from.units().getOrDefault(commodity,0L)<units)throw new SQLException("Seller lacks actual goods");
        if(to.hundredths()<price)throw new SQLException("Buyer lacks purchasing funds");
        changeStock(seller,commodity,-units);setCapital(buyer,to.hundredths()-price);setCapital(seller,Math.addExact(from.hundredths(),price));
        save(operation,terms,units,"Actual local goods purchase; paid "+price+" hundredths; cargo reserved until period "+due);
        try(var s=connection.prepareStatement("INSERT INTO local_shipments(id,seller_id,buyer_id,commodity,units,price,dispatched_period,due_period) VALUES(?,?,?,?,?,?,?,?)")) {
            s.setString(1,operation.toString());s.setString(2,seller.toString());s.setString(3,buyer.toString());s.setString(4,commodity.id());
            s.setLong(5,units);s.setLong(6,price);s.setLong(7,period);s.setLong(8,due);s.executeUpdate();
        }
        if(haulWindow!=null)try(var s=connection.prepareStatement("INSERT INTO industry_auto_shipments(shipment_id,clock_business_id,haul_window,haul_cadence) VALUES(?,?,?,?)")) {
            s.setString(1,operation.toString());s.setString(2,buyer.toString());s.setLong(3,haulWindow);
            if(haulCadence==null)s.setNull(4,Types.INTEGER);else s.setLong(4,haulCadence);s.executeUpdate();
        }
        return shipment(operation).orElseThrow();
    }

    /** No early/duplicate delivery. Pending space becomes real stock in the same transaction. */
    public Shipment deliver(UUID operation,long period) throws SQLException {
        if(period<0)throw new IllegalArgumentException("Negative delivery period");
        return transaction(()->deliverRows(operation,period));
    }
    Shipment deliverRows(UUID operation,long period) throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Shipment rows require active SQL owner transaction");
        if(period<0)throw new IllegalArgumentException("Negative delivery period");
        var cargo=shipment(operation).orElseThrow(()->new SQLException("Unknown shipment"));
        if(cargo.deliveredPeriod()!=null)return cargo;
        if(period<cargo.duePeriod())throw new SQLException("Cargo has not arrived");
        changeStock(cargo.buyer(),cargo.commodity(),cargo.units());
        try(var s=connection.prepareStatement("UPDATE local_shipments SET delivered_period=? WHERE id=? AND delivered_period IS NULL")) {
            s.setLong(1,period);s.setString(2,operation.toString());if(s.executeUpdate()!=1)throw new SQLException("Shipment changed during delivery");
        }
        store(cargo.buyer()).orElseThrow();return shipment(operation).orElseThrow();
    }

    /** Real abstract use, with caller-established demand/reason; no fake NPC shopper or unlimited spending. */
    public Receipt consume(UUID operation,UUID business,Commodity commodity,long units,Use use,String reason) throws SQLException {
        Objects.requireNonNull(use);Business.text(reason,2048);if(units<1)throw new IllegalArgumentException("Consumption must be positive");
        String terms=terms("consume",business,commodity.id(),units,use,reason);
        return transaction(()-> {
            var old=receipt(operation,terms);if(old.isPresent())return old.orElseThrow();
            changeStock(business,commodity,-units);return save(operation,terms,units,"Recorded "+use+": "+reason);
        });
    }

    public Optional<Shipment> shipment(UUID id) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM local_shipments WHERE id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();long value=r.getLong("delivered_period");Long delivered=r.wasNull()?null:value;
                return Optional.of(new Shipment(id,UUID.fromString(r.getString("seller_id")),UUID.fromString(r.getString("buyer_id")),
                        new Commodity(r.getString("commodity")),r.getLong("units"),r.getLong("price"),r.getLong("dispatched_period"),r.getLong("due_period"),delivered));
            }
        }
    }

    private void changeStock(UUID business,Commodity commodity,long delta) throws SQLException {
        try(var s=connection.prepareStatement("SELECT units FROM industry_stock WHERE business_id=? AND commodity=?")) {
            s.setString(1,business.toString());s.setString(2,commodity.id());try(var r=s.executeQuery()) {
                if(!r.next())throw new SQLException("Unsupported business commodity");long after=Math.addExact(r.getLong(1),delta);
                if(after<0)throw new SQLException("Insufficient business stock");
                try(var update=connection.prepareStatement("UPDATE industry_stock SET units=? WHERE business_id=? AND commodity=?")) {
                    update.setLong(1,after);update.setString(2,business.toString());update.setString(3,commodity.id());update.executeUpdate();
                }
            }
        }
    }
    private void setCapital(UUID business,long balance) throws SQLException {
        try(var s=connection.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=?")) {
            s.setLong(1,balance);s.setString(2,business.toString());if(s.executeUpdate()!=1)throw new SQLException("Business capital account missing");
        }
    }
    private Map<Commodity,Long> readCatalog(String query,UUID id) throws SQLException {
        Map<Commodity,Long> result=new TreeMap<>();try(var s=connection.prepareStatement(query)) {
            s.setString(1,id.toString());try(var r=s.executeQuery()) { while(r.next())result.put(new Commodity(r.getString(1)),r.getLong(2)); }
        }return Map.copyOf(result);
    }
    private Optional<Receipt> receipt(UUID id,String terms) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM industry_operations WHERE id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();if(!terms.equals(r.getString("terms")))throw new SQLException("Industry operation identity reused with different terms");
                return Optional.of(new Receipt(id,r.getLong("quantity"),r.getString("explanation")));
            }
        }
    }
    private Receipt save(UUID id,String terms,long quantity,String explanation) throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO industry_operations(id,terms,quantity,explanation) VALUES(?,?,?,?)")) {
            s.setString(1,id.toString());s.setString(2,terms);s.setLong(3,quantity);s.setString(4,explanation);s.executeUpdate();
        }return new Receipt(id,quantity,explanation);
    }
    private static String catalog(Map<Commodity,Long> map) {
        StringBuilder result=new StringBuilder();new TreeMap<>(map).forEach((key,value)->result.append(terms(key.id(),value)));return result.toString();
    }
    private static String terms(Object... inputs) {
        StringBuilder result=new StringBuilder();for(Object input:inputs) { String value=Objects.requireNonNull(input).toString();result.append(value.length()).append(':').append(value); }
        return result.toString();
    }
    @FunctionalInterface private interface Work<T> { T run() throws SQLException; }
    private <T> T transaction(Work<T> work) throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Industry operation requires no active transaction");connection.setAutoCommit(false);
        try { T result=work.run();connection.commit();connection.setAutoCommit(true);return result; }
        catch(SQLException|RuntimeException failure) { try { connection.rollback();connection.setAutoCommit(true); }catch(SQLException rollback) { failure.addSuppressed(rollback); }throw failure; }
    }
}
