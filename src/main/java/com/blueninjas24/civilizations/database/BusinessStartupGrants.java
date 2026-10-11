package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Trusted startup composition. Opportunity/demand/site selection stays with its caller. No scheduler or cash creation. */
public final class BusinessStartupGrants {
    public static final class Declined extends SQLException { public Declined(String reason){super(reason);} }
    public record Receipt(UUID business,UUID settlement,long period,long startup,long capital,long protectedCash,
                          long beforeCash,long afterCash,long foodDemand,String policy) {}
    public record Mine(MiningStartupRepository.Started startup,Receipt grant) {}
    public record WorkshopRequest(BusinessFounding founding,long capacity,Map<Commodity,Long> space,long capital,UUID worker) {
        public WorkshopRequest {
            Objects.requireNonNull(founding);Objects.requireNonNull(worker);space=Map.copyOf(space);
            if(founding.business().industry().equals("mining") || capital<0)throw new IllegalArgumentException("Non-mining workshop required");
        }
    }
    public record Workshop(BusinessFounding founding,IndustryRepository.Store store,Employment worker,Receipt grant) {}
    public record WarehouseRequest(BusinessFounding founding,long capacity,Map<Commodity,Long> space,long capital,UUID worker) {
        public WarehouseRequest {
            Objects.requireNonNull(founding);Objects.requireNonNull(worker);space=Map.copyOf(space);
            if(!founding.business().industry().equals("warehouse") || capacity<1 || capital<0 || space.isEmpty()
                    || space.size()>64 || space.values().stream().anyMatch(weight->weight<1))
                throw new IllegalArgumentException("Valid warehouse storage and capital required");
        }
    }
    public record Warehouse(BusinessFounding founding,IndustryRepository.Store store,Employment worker,Receipt grant) {}
    private final Connection connection;
    public BusinessStartupGrants(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public Mine startMine(MiningStartupRepository.Request request,StartupGrantPolicy policy)throws SQLException {
        Objects.requireNonNull(request);Objects.requireNonNull(policy);
        return transaction(()->startMineRows(request,policy));
    }
    /** Caller rechecks current demand/resource/site evidence inside the same complete startup transaction. */
    Mine startMineRows(MiningStartupRepository.Request request,StartupGrantPolicy policy)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Mine grant rows require owner transaction");
        var receipt=prepare(request.founding(),request.workingCapital(),policy);
        var started=new MiningStartupRepository(connection).startRows(request);
        return new Mine(started,save(receipt));
    }
    public Workshop startWorkshop(WorkshopRequest request,StartupGrantPolicy policy)throws SQLException {
        Objects.requireNonNull(request);Objects.requireNonNull(policy);
        return transaction(()->startStockedRows(request,policy,"workshop"));
    }
    /** Same village budget, qualification and transaction as other grants; no free warehouse stock. */
    public Warehouse startWarehouse(WarehouseRequest request,StartupGrantPolicy policy)throws SQLException {
        Objects.requireNonNull(request);Objects.requireNonNull(policy);
        return transaction(()-> {
            var stocked=startStockedRows(new WorkshopRequest(request.founding(),request.capacity(),request.space(),request.capital(),request.worker()),policy,"warehouse");
            return new Warehouse(stocked.founding(),stocked.store(),stocked.worker(),stocked.grant());
        });
    }
    private Workshop startStockedRows(WorkshopRequest request,StartupGrantPolicy policy,String role)throws SQLException {
        var receipt=prepare(request.founding(),request.capital(),policy);
        var businesses=new BusinessRepository(connection);var founding=businesses.foundRows(request.founding());
        var store=new IndustryRepository(connection).openStoreRows(founding.business().id(),request.capacity(),request.space(),request.capital());
        var job=UUID.nameUUIDFromBytes(("civilizations:"+role+"-first-worker-v1:"+founding.business().id()).getBytes(StandardCharsets.UTF_8));
        var worker=businesses.hireRows(job,founding.business().id(),request.worker(),founding.business().foundedStep());
        return new Workshop(founding,store,worker,save(receipt));
    }
    private Receipt prepare(BusinessFounding founding,long capital,StartupGrantPolicy policy)throws SQLException {
        var business=founding.business();var old=find(business.id());
        if(old.isPresent())return old.orElseThrow(); // Accepted terms replay through the startup seam, never a new grant.
        if(new BusinessRepository(connection).findBusiness(business.id()).isPresent())throw new Declined("Existing business cannot claim a retroactive startup grant");
        if(capital<0)throw new IllegalArgumentException("Negative working capital");
        long amount=Math.addExact(founding.startupHundredths(),capital);
        if(amount==0)throw new Declined("No public startup spending to fund");
        var town=business.settlementId();
        var account=new CommodityRepository(connection).read(town).orElseThrow(()->new Declined("Food account not established"));
        long period=account.state().completedPeriods();long demand;
        try(var s=connection.prepareStatement("""
            SELECT n.demand,n.fulfilled,n.unassigned_residents+COALESCE((SELECT SUM(h.adults+h.children)
              FROM household_needs h WHERE h.settlement_id=n.settlement_id AND h.period=n.period),0) AS assessed,
              (SELECT COUNT(*) FROM residents r WHERE r.settlement_id=n.settlement_id AND r.status='ALIVE') AS living
            FROM needs_periods n WHERE n.settlement_id=? AND n.period=?
            """)) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                if(!r.next())throw new Declined("Current food assessment missing; startup waits");
                if(r.getLong("assessed")!=r.getLong("living"))throw new Declined("Population changed since food assessment; startup waits");
                demand=r.getLong("demand");if(r.getLong("fulfilled")<demand)throw new Declined("Food shortage; startup funds stay with the village");
            }
        }
        long periodSpent=0,totalSpent=0;
        try(var s=connection.prepareStatement("""
            SELECT COALESCE(SUM(startup_hundredths+capital_hundredths),0),
              COALESCE(SUM(CASE WHEN period=? THEN startup_hundredths+capital_hundredths ELSE 0 END),0)
            FROM startup_grants WHERE settlement_id=?
            """)) {
            s.setLong(1,period);s.setString(2,town.toString());try(var r=s.executeQuery()){totalSpent=r.getLong(1);periodSpent=r.getLong(2);}
        }
        long cash=new TreasuryRepository(connection).load(town).orElseThrow(()->new Declined("Village treasury unknown")).hundredths();
        var review=policy.review(business.industry(),cash,demand,amount,periodSpent,totalSpent);
        if(!review.eligible())throw new Declined(review.reason());
        return new Receipt(business.id(),town,period,founding.startupHundredths(),capital,review.protectedHundredths(),cash,cash-amount,demand,policy.toString());
    }
    private Receipt save(Receipt receipt)throws SQLException {
        var old=find(receipt.business());if(old.isPresent())return old.orElseThrow();
        long cash=new TreasuryRepository(connection).load(receipt.settlement()).orElseThrow().hundredths();
        if(cash!=receipt.afterCash())throw new SQLException("Startup debit does not match approved grant");
        try(var s=connection.prepareStatement("INSERT INTO startup_grants VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,receipt.business().toString());s.setString(2,receipt.settlement().toString());s.setLong(3,receipt.period());
            s.setLong(4,receipt.startup());s.setLong(5,receipt.capital());s.setLong(6,receipt.protectedCash());
            s.setLong(7,receipt.beforeCash());s.setLong(8,receipt.afterCash());s.setLong(9,receipt.foodDemand());s.setString(10,receipt.policy());s.executeUpdate();
        }
        return receipt;
    }
    public Optional<Receipt> find(UUID business)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM startup_grants WHERE business_id=?")) {
            s.setString(1,Objects.requireNonNull(business).toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                return Optional.of(new Receipt(business,UUID.fromString(r.getString("settlement_id")),r.getLong("period"),
                        r.getLong("startup_hundredths"),r.getLong("capital_hundredths"),r.getLong("protected_hundredths"),
                        r.getLong("before_cash"),r.getLong("after_cash"),r.getLong("food_demand"),r.getString("policy")));
            }
        }
    }
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Startup grant requires idle SQL owner");
        connection.setAutoCommit(false);
        try {var result=work.run();connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
