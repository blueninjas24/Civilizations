package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;

/** Explicit SQL-owner farm equipment purchase. Scheduling/wear are separate adapters; no free stock or private income. */
public final class FarmToolPurchases {
    public static final class Declined extends SQLException { public Declined(String reason){super(reason);} }
    public record Request(UUID operation,UUID farm,UUID seller,long foodPeriod,long delay,long haulCapacity) {
        public Request {
            Objects.requireNonNull(operation);Objects.requireNonNull(farm);Objects.requireNonNull(seller);
            if(farm.equals(seller)||foodPeriod<1||delay<1||haulCapacity<1)throw new IllegalArgumentException("Invalid farm equipment shipment");
            Math.addExact(foodPeriod,delay);
        }
    }
    public record Receipt(UUID operation,UUID farm,UUID settlement,long period,long price,long protectedCash,
                          long beforeCash,long afterCash,String policy) {}
    private final Connection connection;
    private final BusinessOperationPolicy operations;
    public FarmToolPurchases(Connection connection,BusinessOperationPolicy operations) {
        this.connection=Objects.requireNonNull(connection);this.operations=Objects.requireNonNull(operations);
    }
    /** Existing accepted payment can replay while paused/disabled; a new one requires a present world and current food proof. */
    public Receipt purchase(Request request,FarmToolBudgetPolicy policy,Set<UUID> loadedWorlds)throws SQLException {
        Objects.requireNonNull(request);Objects.requireNonNull(policy);loadedWorlds=Set.copyOf(loadedWorlds);
        if(!connection.getAutoCommit())throw new SQLException("Farm tool funding requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var old=find(request.operation());
            if(old.isPresent()) {
                try(var s=connection.prepareStatement("SELECT terms FROM farm_tool_purchases WHERE operation_id=?")) {
                    s.setString(1,request.operation().toString());try(var r=s.executeQuery()) {
                        if(!r.next()||!request.toString().equals(r.getString(1)))throw new SQLException("Farm payment identity reused with different shipment terms");
                    }
                }
                connection.commit();connection.setAutoCommit(true);return old.orElseThrow();
            }
            var businesses=new BusinessRepository(connection);var industry=new IndustryRepository(connection);
            var founding=businesses.findFounding(request.farm()).orElseThrow(()->new Declined("Saved farm required"));
            var b=businesses.findBusiness(request.farm()).orElseThrow();var town=b.settlementId();
            if(!request.farm().equals(FarmEnterprises.businessId(town))||!b.industry().equals("farming"))throw new Declined("Public farm upkeep cannot fund another industry");
            if(!loadedWorlds.contains(founding.site().worldId()))throw new Declined("Farm world absent; public upkeep paused");
            if(!new BusinessOperations(connection,operations).evaluate(b.id()).permitted())throw new Declined("Operating farm required");
            var population=new PopulationRepository(connection);boolean qualified=false;
            for(var job:businesses.currentWorkers(b.id())) {
                var resident=population.findResident(job.residentId()).orElseThrow();
                if(com.blueninjas24.civilizations.population.WorkEligibility.evaluate(resident,
                        population.findWorkProfile(resident.id()).orElseThrow(),founding.workerRequirements()).eligible()){qualified=true;break;}
            }
            if(!qualified)throw new Declined("Qualified current farm staff required");
            long period=new CommodityRepository(connection).read(town).orElseThrow(()->new Declined("Food account missing")).state().completedPeriods();
            if(period!=request.foodPeriod())throw new Declined("Farm purchase requires the current completed food period");
            try(var s=connection.prepareStatement("""
                SELECT 1 FROM farm_evidence f JOIN economy_periods e ON e.settlement_id=f.settlement_id
                  JOIN farm_learning_reviews l ON l.settlement_id=e.settlement_id AND l.period=e.period
                WHERE f.settlement_id=? AND f.world_id=? AND f.complete=1 AND f.planted_crops>0
                  AND e.period=? AND e.commodity='food' AND e.production>0
                  AND l.business_id=? AND l.production=e.production
                """)) {
                s.setString(1,town.toString());s.setString(2,founding.site().worldId().toString());s.setLong(3,period);s.setString(4,request.farm().toString());
                try(var r=s.executeQuery()){if(!r.next())throw new Declined("Complete working fields and actual current food output required");}
            }
            long demand;
            try(var s=connection.prepareStatement("""
                SELECT n.demand,n.fulfilled,n.unassigned_residents+COALESCE((SELECT SUM(h.adults+h.children)
                  FROM household_needs h WHERE h.settlement_id=n.settlement_id AND h.period=n.period),0) AS assessed,
                  (SELECT COUNT(*) FROM residents r WHERE r.settlement_id=n.settlement_id AND r.status='ALIVE') AS living
                FROM needs_periods n WHERE n.settlement_id=? AND n.period=?
                """)) {
                s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                    if(!r.next()||r.getLong("assessed")!=r.getLong("living"))throw new Declined("Current population food assessment required");
                    demand=r.getLong("demand");if(r.getLong("fulfilled")<demand)throw new Declined("Food shortage; village cash stays protected");
                }
            }
            long spent;
            try(var s=connection.prepareStatement("SELECT COALESCE(SUM(price),0) FROM farm_tool_purchases WHERE settlement_id=? AND period=?")) {
                s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){r.next();spent=r.getLong(1);}
            }
            long cash=new TreasuryRepository(connection).load(town).orElseThrow(()->new Declined("Village treasury missing")).hundredths();
            var review=policy.review(cash,demand,spent);if(!review.eligible())throw new Declined(review.reason());
            var seller=businesses.findBusiness(request.seller()).orElseThrow(()->new Declined("Actual local supplier required"));
            if(!seller.settlementId().equals(town)||(!seller.industry().equals("warehouse")&&!seller.industry().equals("blacksmith")))
                throw new Declined("Farm tools require a local warehouse or blacksmith");
            var saved=industry.store(request.farm());
            if(saved.isPresent()) {
                var store=saved.orElseThrow();
                if(store.hundredths()!=0||!store.spacePerUnit().equals(Map.of(ToolUpkeepPolicy.TOOLS,1L))||store.capacity()!=1)
                    throw new Declined("Existing farm catalog/cash requires an explicit transition; public upkeep does not rewrite it");
                if(store.freeSpace()<1)throw new Declined("Farm already owns a toolkit or paid incoming toolkit");
            } else industry.openStoreRows(request.farm(),1,Map.of(ToolUpkeepPolicy.TOOLS,1L),0);
            new TreasuryRepository(connection).postRows(request.operation(),town,-policy.toolHundredths(),"Public farm toolkit purchase; food-protected upkeep budget");
            // Exact invoice funds pass through the purchase seam in this transaction; no cash remains in the farm.
            try(var s=connection.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=? AND hundredths=0")) {
                s.setLong(1,policy.toolHundredths());s.setString(2,request.farm().toString());if(s.executeUpdate()!=1)throw new SQLException("Farm equipment funding account changed");
            }
            industry.purchaseRows(request.operation(),request.seller(),request.farm(),ToolUpkeepPolicy.TOOLS,1,
                    policy.toolHundredths(),period,request.delay(),request.haulCapacity(),null);
            if(industry.store(request.farm()).orElseThrow().hundredths()!=0)throw new SQLException("Public farm invoice left private cash");
            var receipt=new Receipt(request.operation(),request.farm(),town,period,policy.toolHundredths(),review.protectedHundredths(),cash,cash-policy.toolHundredths(),policy.toString());
            try(var s=connection.prepareStatement("INSERT INTO farm_tool_purchases VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,receipt.operation().toString());s.setString(2,receipt.farm().toString());s.setString(3,town.toString());s.setLong(4,period);
                s.setLong(5,receipt.price());s.setLong(6,receipt.protectedCash());s.setLong(7,cash);s.setLong(8,receipt.afterCash());s.setString(9,receipt.policy());s.setString(10,request.toString());s.executeUpdate();
            }
            connection.commit();connection.setAutoCommit(true);return receipt;
        } catch(SQLException|RuntimeException failure) {
            try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;
        }
    }
    public Optional<Receipt> find(UUID operation)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM farm_tool_purchases WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();return Optional.of(new Receipt(operation,UUID.fromString(r.getString("farm_id")),
                        UUID.fromString(r.getString("settlement_id")),r.getLong("period"),r.getLong("price"),r.getLong("protected_cash"),r.getLong("before_cash"),r.getLong("after_cash"),r.getString("policy")));
            }
        }
    }
}
