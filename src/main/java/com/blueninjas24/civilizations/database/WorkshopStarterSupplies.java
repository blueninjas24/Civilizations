package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** Paid, one-time public surplus cargo. Composes with founding on the SQL owner. */
public final class WorkshopStarterSupplies {
    public record Request(UUID operation,UUID business,ProductionRecipe recipe,int batches,
                          long period,long delay,long haulCapacity) {
        public Request {
            Objects.requireNonNull(operation);Objects.requireNonNull(business);Objects.requireNonNull(recipe);
            if(batches<1||batches>16||period<0||delay<1||haulCapacity<1)
                throw new IllegalArgumentException("Invalid starter cargo request");
            Math.addExact(period,delay);
        }
        String terms(){return operation+":"+business+":"+recipe.id()+":"+recipe.industry()+":"+
            new TreeMap<>(recipe.inputs())+":"+new TreeMap<>(recipe.outputs())+":"+recipe.batchesPerWorker()+":"+
            recipe.siteCapacity()+":"+batches+":"+period+":"+delay+":"+haulCapacity;}
    }
    public record Receipt(UUID operation,UUID business,UUID settlement,long foodPeriod,long duePeriod,
                          long price,long beforeCash,long afterCash,Long deliveredPeriod) {}
    private final Connection connection;
    public WorkshopStarterSupplies(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public Receipt purchase(Request request,WorkshopStarterPolicy policy,Set<UUID> worlds)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Starter purchase requires idle SQL owner");
        connection.setAutoCommit(false);
        try {var result=purchaseRows(request,policy,worlds);connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    Receipt purchaseRows(Request request,WorkshopStarterPolicy policy,Set<UUID> worlds)throws SQLException {
        requireTransaction();Objects.requireNonNull(policy);Objects.requireNonNull(worlds);
        try(var s=connection.prepareStatement("SELECT terms FROM workshop_starter_orders WHERE operation_id=?")) {
            s.setString(1,request.operation().toString());try(var r=s.executeQuery()) {
                if(r.next()) {if(!request.terms().equals(r.getString(1)))throw new SQLException("Starter identity reused with changed terms");return find(request.operation()).orElseThrow();}
            }
        }
        if(!policy.enabled()||request.batches()>policy.maximumBatches())throw new SQLException("Starter supplies disabled or batch cap exceeded");
        var businesses=new BusinessRepository(connection);var founding=businesses.findFounding(request.business()).orElseThrow(()->new SQLException("Unknown workshop"));
        var business=founding.business();
        if(!business.industry().equals(request.recipe().industry())||Set.of("mining","farming","warehouse").contains(business.industry()))
            throw new SQLException("Starter materials require matching processing workshop");
        if(!worlds.contains(founding.site().worldId()))throw new SQLException("Starter world absent");
        try(var s=connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")) {
            s.setString(1,business.settlementId().toString());try(var r=s.executeQuery()) {
                if(!r.next()||!founding.site().worldId().toString().equals(r.getString(1)))throw new SQLException("Workshop world does not match village");
            }
        }
        if(!new BusinessOperations(connection,BusinessOperationPolicy.disabled()).evaluate(business.id()).permitted())throw new SQLException("Workshop owner unavailable");
        var population=new PopulationRepository(connection);boolean qualified=false;
        for(var job:businesses.currentWorkers(business.id())) {
            var person=population.findResident(job.residentId()).orElseThrow();
            if(WorkEligibility.evaluate(person,population.findWorkProfile(person.id()).orElseThrow(),founding.workerRequirements()).eligible())qualified=true;
        }
        if(!qualified)throw new SQLException("Workshop lacks qualified existing staff");
        var grant=new BusinessStartupGrants(connection).find(business.id()).orElseThrow(()->new SQLException("No accepted workshop startup grant"));
        var account=new CommodityRepository(connection).read(business.settlementId()).orElseThrow(()->new SQLException("Village supplies unknown"));
        if(grant.period()!=account.state().completedPeriods())throw new SQLException("Starter purchase must share startup food period");
        requireFood(business.settlementId(),grant.period());
        try(var s=connection.prepareStatement("SELECT 1 FROM workshop_starter_orders WHERE business_id=? UNION ALL SELECT 1 FROM industry_production WHERE business_id=? LIMIT 1")) {
            s.setString(1,business.id().toString());s.setString(2,business.id().toString());try(var r=s.executeQuery()) {if(r.next())throw new SQLException("Workshop starter entitlement already used or production started");}
        }
        var store=new IndustryRepository(connection).store(business.id()).orElseThrow();
        if(store.reservedIncomingSpace()!=0||store.units().values().stream().anyMatch(v->v!=0))throw new SQLException("Starter purchase requires newly empty workshop");
        var stockUnits=WorkshopPublicStock.read(connection,business.settlementId());
        var plan=WorkshopStarterPlan.review(request.recipe(),request.batches(),policy,store.hundredths(),store.freeSpace(),request.haulCapacity(),stockUnits,store.spacePerUnit());
        if(!plan.eligible())throw new SQLException(plan.explanation());
        var goods=plan.goods();long price=plan.price();
        try(var s=connection.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=? AND hundredths=?")) {
            s.setLong(1,store.hundredths()-price);s.setString(2,business.id().toString());s.setLong(3,store.hundredths());if(s.executeUpdate()!=1)throw new SQLException("Starter capital changed");
        }
        new TreasuryRepository(connection).postRows(request.operation(),business.settlementId(),price,"Paid workshop starter materials");
        try(var s=connection.prepareStatement("INSERT INTO workshop_starter_orders(operation_id,business_id,settlement_id,food_period,due_period,price,before_cash,after_cash,terms,policy) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,request.operation().toString());s.setString(2,business.id().toString());s.setString(3,business.settlementId().toString());s.setLong(4,grant.period());s.setLong(5,Math.addExact(request.period(),request.delay()));s.setLong(6,price);s.setLong(7,store.hundredths());s.setLong(8,store.hundredths()-price);s.setString(9,request.terms());s.setString(10,policy.toString());s.executeUpdate();
        }
        for(var entry:goods.entrySet()) {
            long before=stockUnits.get(entry.getKey()),after=before-entry.getValue();
            WorkshopPublicStock.debit(connection,business.settlementId(),entry.getKey(),entry.getValue(),before);
            try(var s=connection.prepareStatement("INSERT INTO workshop_starter_goods VALUES(?,?,?,?,?,?,?)")) {
                s.setString(1,request.operation().toString());s.setString(2,entry.getKey().id());s.setLong(3,entry.getValue());s.setLong(4,policy.prices().get(entry.getKey()));s.setLong(5,policy.reserveUnits());s.setLong(6,before);s.setLong(7,after);s.executeUpdate();
            }
        }
        return find(request.operation()).orElseThrow();
    }
    private void requireFood(UUID town,long period)throws SQLException {
        try(var s=connection.prepareStatement("""
            SELECT n.demand,n.fulfilled,n.unassigned_residents+COALESCE((SELECT SUM(h.adults+h.children)
              FROM household_needs h WHERE h.settlement_id=n.settlement_id AND h.period=n.period),0),
              (SELECT COUNT(*) FROM residents r WHERE r.settlement_id=n.settlement_id AND r.status='ALIVE')
            FROM needs_periods n WHERE n.settlement_id=? AND n.period=?
            """)) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                if(!r.next()||r.getLong(2)<r.getLong(1)||r.getLong(3)!=r.getLong(4))throw new SQLException("Current complete food assessment required for starter supplies");
            }
        }
    }
    public Optional<Receipt> find(UUID operation)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM workshop_starter_orders WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();long delivered=r.getLong("delivered_period");Long arrival=r.wasNull()?null:delivered;
                return Optional.of(new Receipt(operation,UUID.fromString(r.getString("business_id")),UUID.fromString(r.getString("settlement_id")),r.getLong("food_period"),r.getLong("due_period"),r.getLong("price"),r.getLong("before_cash"),r.getLong("after_cash"),arrival));
            }
        }
    }
    Map<Commodity,Long> pending(UUID business)throws SQLException {
        var result=new TreeMap<Commodity,Long>();
        try(var s=connection.prepareStatement("SELECT g.commodity,g.units FROM workshop_starter_goods g JOIN workshop_starter_orders o ON o.operation_id=g.operation_id WHERE o.business_id=? AND o.delivered_period IS NULL ORDER BY g.commodity")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){while(r.next())result.put(new Commodity(r.getString(1)),r.getLong(2));}
        }
        return result;
    }
    int deliverRows(UUID business,long period)throws SQLException {
        requireTransaction();UUID operation=null;
        try(var s=connection.prepareStatement("SELECT operation_id FROM workshop_starter_orders WHERE business_id=? AND delivered_period IS NULL AND due_period<=?")) {
            s.setString(1,business.toString());s.setLong(2,period);try(var r=s.executeQuery()){if(r.next())operation=UUID.fromString(r.getString(1));}
        }
        if(operation==null)return 0;
        var goods=pending(business);var store=new IndustryRepository(connection).store(business).orElseThrow();
        for(var entry:goods.entrySet())try(var s=connection.prepareStatement("UPDATE industry_stock SET units=? WHERE business_id=? AND commodity=? AND units=?")) {
            long before=store.units().get(entry.getKey());s.setLong(1,Math.addExact(before,entry.getValue()));s.setString(2,business.toString());s.setString(3,entry.getKey().id());s.setLong(4,before);if(s.executeUpdate()!=1)throw new SQLException("Starter destination stock changed");
        }
        try(var s=connection.prepareStatement("UPDATE workshop_starter_orders SET delivered_period=? WHERE operation_id=? AND delivered_period IS NULL")) {
            s.setLong(1,period);s.setString(2,operation.toString());if(s.executeUpdate()!=1)throw new SQLException("Starter delivery changed");
        }
        return 1;
    }
    private void requireTransaction()throws SQLException {if(connection.getAutoCommit())throw new SQLException("Starter rows require active SQL owner transaction");}
}
