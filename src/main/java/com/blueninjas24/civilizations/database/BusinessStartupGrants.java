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
    public record Fishery(Workshop startup,FisheryRepository.Resource resource) {}
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
    /** Trusted caller must revalidate actual water, shoreline and demand; paid firm and habitat commit together. */
    public Fishery startFishery(WorkshopRequest request,FisheryObservation habitat,FishingPolicy fishing,StartupGrantPolicy grants)throws SQLException {
        Objects.requireNonNull(request);Objects.requireNonNull(habitat);Objects.requireNonNull(fishing);Objects.requireNonNull(grants);
        if(!request.founding().business().industry().equals("fishing")||!request.space().containsKey(Commodity.FOOD))
            throw new IllegalArgumentException("Fishery requires fishing industry and food storage");
        return transaction(()->startFisheryRows(request,habitat,fishing,grants));
    }
    Fishery startFisheryRows(WorkshopRequest request,FisheryObservation habitat,FishingPolicy fishing,StartupGrantPolicy grants)throws SQLException {
        var started=startStockedRows(request,grants,"workshop");
        var resource=new FisheryRepository(connection).openRows(started.founding().site().id(),habitat,fishing);
        return new Fishery(started,resource);
    }
    /** First confirmed shoreline and paid habitat startup commit together; legacy resources are never guessed. */
    public Fishery startShoreFishery(WorkshopRequest request,WaterPatch.Patch original,int depth,int rise,int distance,
            ShorelineEvidence evidence,long now,long maximumAge,FishingPolicy fishing,StartupGrantPolicy grants)throws SQLException {
        if(!request.founding().business().industry().equals("fishing")||!request.space().containsKey(com.blueninjas24.civilizations.economy.Commodity.FOOD))throw new IllegalArgumentException("Fishery requires fishing industry and food storage");
        return transaction(()->startShoreFisheryRows(request,original,depth,rise,distance,evidence,now,maximumAge,fishing,grants));
    }
    Fishery startShoreFisheryRows(WorkshopRequest request,WaterPatch.Patch original,int depth,int rise,int distance,
            ShorelineEvidence evidence,long now,long maximumAge,FishingPolicy fishing,StartupGrantPolicy grants)throws SQLException {
        var site=request.founding().site().id();var banks=new FisheryWorkplaces(connection);
        if(new FisheryRepository(connection).find(site).isPresent()&&banks.find(site).isEmpty())throw new Declined("Legacy fishery cannot acquire guessed bank terms through startup replay");
        var started=startFisheryRows(request,original.evidence(),fishing,grants);
        banks.attachRows(site,original,depth,rise,distance,evidence,now,maximumAge);return started;
    }
    /** One atomic first-workshop investment; failed supply checks leave no grant, site or firm. */
    public Workshop startWorkshopWithSupplies(WorkshopRequest request,StartupGrantPolicy grantPolicy,
            WorkshopStarterSupplies.Request supplies,WorkshopStarterPolicy supplyPolicy,Set<UUID> worlds)throws SQLException {
        if(!request.founding().business().id().equals(supplies.business()))throw new IllegalArgumentException("Starter business mismatch");
        return transaction(()-> {
            var started=startStockedRows(request,grantPolicy,"workshop");
            new WorkshopStarterSupplies(connection).purchaseRows(supplies,supplyPolicy,worlds);
            var id=request.founding().business().id();
            return new Workshop(started.founding(),new IndustryRepository(connection).store(id).orElseThrow(),started.worker(),started.grant());
        });
    }
    /** Fresh loaded-block proof and binding commit with the paid startup. Caller still chooses real opportunity/qualified founder. */
    public Workshop startMarkerWorkshopWithSupplies(WorkshopRequest request,StartupGrantPolicy grants,
            WorkshopStarterSupplies.Request supplies,WorkshopStarterPolicy starter,Set<UUID> worlds,
            WorkplaceOperations.Confirmation evidence,long now)throws SQLException {
        return transaction(()->startMarkerWorkshopRows(request,grants,supplies,starter,worlds,evidence,now));
    }
    Workshop startMarkerWorkshopRows(WorkshopRequest request,StartupGrantPolicy grants,
            WorkshopStarterSupplies.Request supplies,WorkshopStarterPolicy starter,Set<UUID> worlds,
            WorkplaceOperations.Confirmation evidence,long now)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Marker startup rows require complete owner transaction");
        if(!request.founding().business().id().equals(supplies.business())||!supplies.business().equals(evidence.anchor().business()))
            throw new IllegalArgumentException("Marker startup identity mismatch");
        if(now<0||!evidence.present()||evidence.capturedMillis()>now||now-evidence.capturedMillis()>60000
                ||worlds==null||!worlds.contains(evidence.anchor().world()))throw new Declined("Fresh present loaded-world marker required");
        var markers=new WorkplaceOperations(connection);
        boolean existing=new BusinessRepository(connection).findBusiness(supplies.business()).isPresent();
        if(existing) {
            var saved=markers.matchesSavedAnchor(evidence.anchor());
            if(!saved)throw new Declined("Existing legacy business cannot acquire a marker requirement through startup replay");
        }
        var started=startStockedRows(request,grants,"workshop");
        new WorkshopStarterSupplies(connection).purchaseRows(supplies,starter,worlds);
        if(!existing)markers.attachRows(evidence.anchor(),evidence.capturedMillis());
        return new Workshop(started.founding(),new IndustryRepository(connection).store(supplies.business()).orElseThrow(),started.worker(),started.grant());
    }
    /** Private first-input purchases compose with this empty paid startup in the same owner transaction. */
    Workshop startMarkerEmptyWorkshopRows(WorkshopRequest request,StartupGrantPolicy grants,Set<UUID> worlds,
            WorkplaceOperations.Confirmation evidence,long now)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Marker startup requires complete owner transaction");
        var id=request.founding().business().id();
        if(!id.equals(evidence.anchor().business())||now<0||!evidence.present()||evidence.capturedMillis()>now
                ||now-evidence.capturedMillis()>60000||worlds==null||!worlds.contains(evidence.anchor().world()))
            throw new Declined("Matching fresh present loaded-world marker required");
        var markers=new WorkplaceOperations(connection);boolean existing=new BusinessRepository(connection).findBusiness(id).isPresent();
        if(existing&&!markers.matchesSavedAnchor(evidence.anchor()))throw new Declined("Legacy business cannot acquire a marker through private startup replay");
        var started=startStockedRows(request,grants,"workshop");
        if(!existing)markers.attachRows(evidence.anchor(),evidence.capturedMillis());return started;
    }
    /** Same village budget, qualification and transaction as other grants; no free warehouse stock. */
    public Warehouse startWarehouse(WarehouseRequest request,StartupGrantPolicy policy)throws SQLException {
        Objects.requireNonNull(request);Objects.requireNonNull(policy);
        return transaction(()-> {
            var stocked=startStockedRows(new WorkshopRequest(request.founding(),request.capacity(),request.space(),request.capital(),request.worker()),policy,"warehouse");
            return new Warehouse(stocked.founding(),stocked.store(),stocked.worker(),stocked.grant());
        });
    }
    public Warehouse startMarkerWarehouse(WarehouseRequest request,StartupGrantPolicy policy,Set<UUID> worlds,
            WorkplaceOperations.Confirmation evidence,long now)throws SQLException {
        return transaction(()->startMarkerWarehouseRows(request,policy,worlds,evidence,now));
    }
    /** Shares opportunity revalidation and paid startup in one complete SQL-owner transaction. */
    Warehouse startMarkerWarehouseRows(WarehouseRequest request,StartupGrantPolicy policy,Set<UUID> worlds,
            WorkplaceOperations.Confirmation evidence,long now)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Marker warehouse rows require complete owner transaction");
        if(!request.founding().business().id().equals(evidence.anchor().business()))throw new IllegalArgumentException("Marker warehouse identity mismatch");
        if(now<0||!evidence.present()||evidence.capturedMillis()>now||now-evidence.capturedMillis()>60000
                ||worlds==null||!worlds.contains(evidence.anchor().world()))throw new Declined("Fresh present loaded-world marker required");
            var markers=new WorkplaceOperations(connection);
            boolean existing=new BusinessRepository(connection).findBusiness(evidence.anchor().business()).isPresent();
            if(existing&&!markers.matchesSavedAnchor(evidence.anchor()))throw new Declined("Legacy warehouse cannot acquire marker requirement through startup replay");
            var stocked=startStockedRows(new WorkshopRequest(request.founding(),request.capacity(),request.space(),request.capital(),request.worker()),policy,"warehouse");
            if(!existing)markers.attachRows(evidence.anchor(),evidence.capturedMillis());
            return new Warehouse(stocked.founding(),stocked.store(),stocked.worker(),stocked.grant());
    }
    private Workshop startStockedRows(WorkshopRequest request,StartupGrantPolicy policy,String role)throws SQLException {
        var receipt=prepare(request.founding(),request.capital(),policy);
        var businesses=new BusinessRepository(connection);var founding=businesses.foundRows(request.founding());
        var store=new IndustryRepository(connection).openStoreRows(founding.business().id(),request.capacity(),request.space(),request.capital());
        var job=UUID.nameUUIDFromBytes(("civilizations:"+role+"-first-worker-v1:"+founding.business().id()).getBytes(StandardCharsets.UTF_8));
        var worker=businesses.hireRows(job,founding.business().id(),request.worker(),founding.business().foundedStep());
        return new Workshop(founding,store,worker,save(receipt));
    }
    /** Read-only current affordability, not a reservation or permission to found a business. */
    public StartupGrantPolicy.Review preview(UUID town,String industry,long startup,long capital,StartupGrantPolicy policy)throws SQLException {
        return transaction(()->previewRows(town,industry,startup,capital,policy));
    }
    /** Shares the caller's owner transaction; acceptance must still recheck all evidence. */
    StartupGrantPolicy.Review previewRows(UUID town,String industry,long startup,long capital,StartupGrantPolicy policy)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Startup preview rows require owner transaction");
        try{return budget(town,industry,startup,capital,policy).review();}
        catch(Declined refusal){return new StartupGrantPolicy.Review(false,0,refusal.getMessage());}
    }
    private record Budget(long period,long cash,long demand,StartupGrantPolicy.Review review) {}
    private Receipt prepare(BusinessFounding founding,long capital,StartupGrantPolicy policy)throws SQLException {
        var business=founding.business();var old=find(business.id());
        if(old.isPresent())return old.orElseThrow(); // Accepted terms replay through the startup seam, never a new grant.
        if(new BusinessRepository(connection).findBusiness(business.id()).isPresent())throw new Declined("Existing business cannot claim a retroactive startup grant");
        var budget=budget(business.settlementId(),business.industry(),founding.startupHundredths(),capital,policy);
        if(!budget.review().eligible())throw new Declined(budget.review().reason());
        long amount=Math.addExact(founding.startupHundredths(),capital);
        return new Receipt(business.id(),business.settlementId(),budget.period(),founding.startupHundredths(),capital,
                budget.review().protectedHundredths(),budget.cash(),budget.cash()-amount,budget.demand(),policy.toString());
    }
    private Budget budget(UUID town,String industry,long startup,long capital,StartupGrantPolicy policy)throws SQLException {
        Objects.requireNonNull(town);Objects.requireNonNull(policy);
        if(industry==null||!industry.matches("[a-z][a-z0-9_-]{0,63}")||startup<0||capital<0)throw new IllegalArgumentException("Invalid startup preview terms");
        long amount=Math.addExact(startup,capital);
        if(amount==0)throw new Declined("No public startup spending to fund");
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
        return new Budget(period,cash,demand,policy.review(industry,cash,demand,amount,periodSpent,totalSpent));
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
