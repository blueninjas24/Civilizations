package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** SQL-owner public supply orders. Goods leave the seller once, remain owned cargo, then enter period imports. */
public final class VillageSupplyPurchases {
    public record Request(UUID operation,UUID town,UUID seller,Commodity commodity,long units,long period) {
        public Request {
            Objects.requireNonNull(operation);Objects.requireNonNull(town);Objects.requireNonNull(seller);Objects.requireNonNull(commodity);
            if(units<1||period<1)throw new IllegalArgumentException("Invalid village supply order");
        }
    }
    public record Receipt(Request request,long price,long duePeriod,Long appliedPeriod) {}
    public record Demand(UUID settlement,long period,Map<Commodity,Long> fundedUnits,long estimatedPrice,String explanation) {
        public Demand {fundedUnits=Collections.unmodifiableMap(new TreeMap<>(fundedUnits));}
    }
    public static final class Declined extends SQLException {Declined(String message){super(message);}}
    private final Connection connection;
    private final VillageSupplyPolicy policy;
    private final StudyPolicy study;
    private final BusinessOperationPolicy operations;
    public VillageSupplyPurchases(Connection connection,VillageSupplyPolicy policy,StudyPolicy study,BusinessOperationPolicy operations) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);
        this.study=Objects.requireNonNull(study);this.operations=Objects.requireNonNull(operations);
    }
    public Receipt purchase(Request request,Set<UUID> worlds)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Village purchase requires idle SQL owner");
        connection.setAutoCommit(false);
        try {var result=purchaseRows(request,Set.copyOf(worlds));connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    Receipt purchaseRows(Request request,Set<UUID> worlds)throws SQLException {
        requireTransaction();
        var old=find(request.operation());
        if(old.isPresent()) {
            if(!old.orElseThrow().request().equals(request))throw new SQLException("Village order identity reused with different terms");
            return old.orElseThrow();
        }
        if(new TreasuryRepository(connection).entry(request.operation()).isPresent())throw new SQLException("Village order identity already belongs to another treasury operation");
        var evidence=evidence(request.town(),request.period(),worlds);
        var businesses=new BusinessRepository(connection);var founding=businesses.findFounding(request.seller()).orElseThrow(()->new Declined("Saved local supplier required"));
        if(!founding.business().settlementId().equals(request.town())||!founding.site().worldId().equals(evidence.world()))throw new Declined("Supplier must share village and present world");
        if(!new BusinessOperations(connection,operations).evaluate(request.seller()).permitted())throw new Declined("Operating supplier required");
        WorkplaceOperations.requirePermitted(connection,request.seller());
        var population=new PopulationRepository(connection);boolean staffed=false;
        for(var job:businesses.currentWorkers(request.seller())) {
            var person=population.findResident(job.residentId()).orElseThrow();
            if(person.settlementId().equals(request.town())&&WorkEligibility.evaluate(person,population.findWorkProfile(person.id()).orElseThrow(),founding.workerRequirements()).eligible()){staffed=true;break;}
        }
        if(!staffed)throw new Declined("Qualified supplier staff required");
        var store=new IndustryRepository(connection).store(request.seller()).orElseThrow(()->new Declined("Actual supplier goods required"));
        long cash=new TreasuryRepository(connection).load(request.town()).orElseThrow(()->new Declined("Real village treasury required")).hundredths();
        long allowed=policy.affordable(request.commodity(),cash,evidence.demand(),spent(request.town(),request.period(),request.commodity().equals(Commodity.FOOD)),
                wanted(request.town(),request.period(),request.commodity(),evidence),store.units().getOrDefault(request.commodity(),0L),
                Math.max(0,policy.haulUnitsPerPeriod()-hauled(request.town(),request.period())));
        if(request.units()>allowed)throw new Declined("No funded current need, supplier stock, budget or public haul capacity");
        long price=Math.multiplyExact(request.units(),policy.prices().get(request.commodity()));
        new TreasuryRepository(connection).postRows(request.operation(),request.town(),-price,"Real village "+request.commodity().id()+" supply purchase");
        try(var s=connection.prepareStatement("UPDATE industry_stock SET units=units-? WHERE business_id=? AND commodity=? AND units>=?")) {
            s.setLong(1,request.units());s.setString(2,request.seller().toString());s.setString(3,request.commodity().id());s.setLong(4,request.units());
            if(s.executeUpdate()!=1)throw new SQLException("Supplier goods changed");
        }
        try(var s=connection.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=? AND hundredths=?")) {
            s.setLong(1,Math.addExact(store.hundredths(),price));s.setString(2,request.seller().toString());s.setLong(3,store.hundredths());
            if(s.executeUpdate()!=1)throw new SQLException("Supplier cash changed");
        }
        try(var s=connection.prepareStatement("INSERT INTO village_supply_orders(operation_id,settlement_id,seller_id,commodity,units,price,period,due_period,policy) VALUES(?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,request.operation().toString());s.setString(2,request.town().toString());s.setString(3,request.seller().toString());s.setString(4,request.commodity().id());
            s.setLong(5,request.units());s.setLong(6,price);s.setLong(7,request.period());s.setLong(8,Math.addExact(request.period(),policy.deliveryPeriods()));s.setString(9,policy.toString());s.executeUpdate();
        }
        return find(request.operation()).orElseThrow();
    }
    private record Evidence(UUID world,long demand,long unmet) {}
    private Evidence evidence(UUID town,long period,Set<UUID> worlds)throws SQLException {
        try(var s=connection.prepareStatement("""
            SELECT w.world_id,n.demand,n.fulfilled,n.unassigned_residents+COALESCE((SELECT SUM(h.adults+h.children)
              FROM household_needs h WHERE h.settlement_id=n.settlement_id AND h.period=n.period),0) AS assessed,
              (SELECT COUNT(*) FROM residents p WHERE p.settlement_id=n.settlement_id AND p.status='ALIVE') AS living
            FROM economy_accounts a JOIN economy_runtime r USING(settlement_id) JOIN settlement_worlds w USING(settlement_id)
              JOIN needs_periods n ON n.settlement_id=a.settlement_id AND n.period=a.completed_periods
            WHERE a.settlement_id=? AND a.completed_periods=? AND r.target_periods=a.completed_periods
            """)) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                if(!r.next()||r.getLong("assessed")!=r.getLong("living"))throw new Declined("Current complete population assessment without historical backlog required");
                UUID world=UUID.fromString(r.getString(1));if(!worlds.contains(world))throw new Declined("Village world absent");
                return new Evidence(world,r.getLong("demand"),Math.max(0,r.getLong("demand")-r.getLong("fulfilled")));
            }
        }
    }
    private long wanted(UUID town,long period,Commodity good,Evidence evidence)throws SQLException {
        var account=new CommodityRepository(connection).read(town).orElseThrow();var stock=account.state().stocks().get(good);
        if(stock==null)return 0;long required=0;
        if(good.equals(Commodity.FOOD))required=evidence.unmet();
        else if(study.enabled()&&evidence.unmet()==0) {
            // Only practice actually reviewed as productive or supply-starved creates a customer.
            // Opportunity/food/labor pauses and completed/cancelled courses do not justify purchases.
            try(var s=connection.prepareStatement("""
                SELECT c.skill,m.units FROM resident_courses c JOIN study_reviews v ON v.course_id=c.id AND v.period=?
                  JOIN study_materials m ON m.course_id=c.id
                  JOIN residents p ON p.id=c.resident_id
                WHERE c.settlement_id=? AND c.status='ACTIVE' AND v.status='ACTIVE' AND m.commodity=?
                  AND p.status='ALIVE' AND p.settlement_id=c.settlement_id
                  AND NOT EXISTS(SELECT 1 FROM business_employment j WHERE j.resident_id=p.id AND j.ended_step IS NULL)
                  AND NOT EXISTS(SELECT 1 FROM businesses b WHERE b.owner_id=p.id AND NOT EXISTS(SELECT 1 FROM business_closures x WHERE x.business_id=b.id))
                  AND COALESCE((SELECT experience FROM resident_skills k WHERE k.resident_id=p.id AND k.skill=c.skill),0)<MIN(c.target,?)
                  AND (v.after_experience>v.before_experience OR v.reason='Actual practice supplies/reserve insufficient; study paused')
                ORDER BY c.id LIMIT ?
                """)) {
                s.setLong(1,period);s.setString(2,town.toString());s.setString(3,good.id());s.setLong(4,study.maximumExperience());s.setInt(5,study.studentsPerPeriod());
                try(var r=s.executeQuery()){while(r.next())if(study.skills().containsKey(r.getString(1)))required=Math.addExact(required,r.getLong(2));}
            }
            if(required>0)required=Math.addExact(required,study.materialReserveUnits());
        }
        long held=Math.addExact(stock.units(),pending(town,good));
        if(good.equals(Commodity.FOOD))held=Math.addExact(held,Math.addExact(new FoodDeliveryRepository(connection).pendingUnits(town),
                Math.addExact(new FoodPurchaseRepository(connection).reservedUnits(town),new FoodBarterRepository(connection).reservedFood(town))));
        return Math.max(0,required-held);
    }
    long pending(UUID town,Commodity good)throws SQLException {
        return sum("SELECT units FROM village_supply_orders WHERE settlement_id=? AND commodity=? AND applied_period IS NULL",town,good.id());
    }
    private long spent(UUID town,long period,boolean food)throws SQLException {
        return sum("SELECT price FROM village_supply_orders WHERE settlement_id=? AND period=? AND commodity"+(food?"=":"<>")+"'food'",town,period);
    }
    private long hauled(UUID town,long period)throws SQLException {return sum("SELECT units FROM village_supply_orders WHERE settlement_id=? AND period=?",town,period);}
    /** Read-only actual public customer evidence for later resource investment; does not reserve cash or invent a business. */
    public Demand demand(UUID town,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(town);Objects.requireNonNull(worlds);
        var account=new CommodityRepository(connection).read(town);
        long period=account.map(a->a.state().completedPeriods()).orElse(0L);
        if(!policy.enabled()||period==0)return new Demand(town,period,Map.of(),0,"Public customers disabled or awaiting a completed food assessment");
        Evidence evidence;try{evidence=evidence(town,period,worlds);}catch(Declined unavailable){return new Demand(town,period,Map.of(),0,unavailable.getMessage());}
        long cash=new TreasuryRepository(connection).load(town).map(t->t.hundredths()).orElse(0L),cost=0;
        long foodSpent=spent(town,period,true),materialSpent=spent(town,period,false),haul=Math.max(0,policy.haulUnitsPerPeriod()-hauled(town,period));
        var goods=new ArrayList<>(policy.prices().keySet());goods.remove(Commodity.FOOD);goods.addFirst(Commodity.FOOD);
        var funded=new TreeMap<Commodity,Long>();
        for(var good:goods) {
            boolean food=good.equals(Commodity.FOOD);
            long units=policy.affordable(good,cash,evidence.demand(),food?foodSpent:materialSpent,wanted(town,period,good,evidence),Long.MAX_VALUE,haul);
            if(units==0)continue;
            long invoice=Math.multiplyExact(units,policy.prices().get(good));funded.put(good,units);cash-=invoice;cost=Math.addExact(cost,invoice);haul-=units;
            if(food)foodSpent=Math.addExact(foodSpent,invoice);else materialSpent=Math.addExact(materialSpent,invoice);
        }
        return new Demand(town,period,funded,cost,"Real current shortage/practice needs after public stock, owned or reserved incoming goods, shared budgets, cash and haul limits; planning only");
    }
    private long sum(String sql,UUID town,Object value)throws SQLException {
        long total=0;try(var s=connection.prepareStatement(sql)){s.setString(1,town.toString());s.setObject(2,value);try(var r=s.executeQuery()){while(r.next())total=Math.addExact(total,r.getLong(1));}}return total;
    }
    /** Bounded deterministic review after the final current food period; no historical purchases. */
    void reviewRows(UUID town,long period,Set<UUID> worlds)throws SQLException {
        requireTransaction();if(!policy.enabled()||worlds==null)return;
        Evidence evidence;try{evidence=evidence(town,period,worlds);}catch(Declined absent){return;}
        var sellers=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id WHERE b.settlement_id=? AND NOT EXISTS(SELECT 1 FROM business_closures c WHERE c.business_id=b.id) ORDER BY b.id LIMIT 16")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next())sellers.add(UUID.fromString(r.getString(1)));}
        }
        if(!sellers.isEmpty())Collections.rotate(sellers,-(int)Math.floorMod(period-1,sellers.size()));
        var goods=new ArrayList<>(policy.prices().keySet());goods.remove(Commodity.FOOD);goods.addFirst(Commodity.FOOD);
        for(var good:goods)for(var seller:sellers) {
            long cash=new TreasuryRepository(connection).load(town).map(t->t.hundredths()).orElse(0L);
            var store=new IndustryRepository(connection).store(seller).orElseThrow();
            long units=policy.affordable(good,cash,evidence.demand(),spent(town,period,good.equals(Commodity.FOOD)),wanted(town,period,good,evidence),
                    store.units().getOrDefault(good,0L),Math.max(0,policy.haulUnitsPerPeriod()-hauled(town,period)));
            if(units==0)continue;
            UUID id=UUID.nameUUIDFromBytes(("civilizations:village-supply:"+town+":"+period+":"+good.id()+":"+seller).getBytes(StandardCharsets.UTF_8));
            try{purchaseRows(new Request(id,town,seller,good,units,period),worlds);}catch(Declined unavailable){/* Expected unstaffed/paused supplier; another may satisfy this actual need. */}
        }
    }
    Map<Commodity,Long> incomingRows(UUID town,long period)throws SQLException {
        requireTransaction();var result=new TreeMap<Commodity,Long>();
        try(var s=connection.prepareStatement("SELECT commodity,units FROM village_supply_orders WHERE settlement_id=? AND applied_period IS NULL AND due_period<=? ORDER BY period,operation_id LIMIT 64")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){while(r.next())result.merge(new Commodity(r.getString(1)),r.getLong(2),Math::addExact);}
        }
        return result;
    }
    void applyRows(UUID town,long period)throws SQLException {
        requireTransaction();try(var s=connection.prepareStatement("UPDATE village_supply_orders SET applied_period=? WHERE operation_id IN (SELECT operation_id FROM village_supply_orders WHERE settlement_id=? AND applied_period IS NULL AND due_period<=? ORDER BY period,operation_id LIMIT 64)")) {
            s.setLong(1,period);s.setString(2,town.toString());s.setLong(3,period);s.executeUpdate();
        }
    }
    public Optional<Receipt> find(UUID id)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM village_supply_orders WHERE operation_id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()){if(!r.next())return Optional.empty();long applied=r.getLong("applied_period");Long arrival=r.wasNull()?null:applied;
                return Optional.of(new Receipt(new Request(id,UUID.fromString(r.getString("settlement_id")),UUID.fromString(r.getString("seller_id")),new Commodity(r.getString("commodity")),r.getLong("units"),r.getLong("period")),r.getLong("price"),r.getLong("due_period"),arrival));}
        }
    }
    String summary(UUID town)throws SQLException {
        long orders=0,units=0;
        try(var s=connection.prepareStatement("SELECT units FROM village_supply_orders WHERE settlement_id=? AND applied_period IS NULL")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next()){orders++;units=Math.addExact(units,r.getLong(1));}}
        }
        return "Village supply customers: "+(policy.enabled()?"enabled":"disabled")+"; owned incoming orders "+orders+", units "+units+"; recorded shortages/active practice only";
    }
    private void requireTransaction()throws SQLException {if(connection.getAutoCommit())throw new SQLException("Village supply rows require SQL-owner transaction");}
}
