package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** SQL-owner purchase of real fittings followed by delayed installation. Never extends stock catalogues. */
public final class StorageUpgrades {
    public static final Commodity FITTINGS=new Commodity("storage_fittings");
    public record Draft(UUID operation,UUID buyer,UUID seller,long period,long haulWindow,long cadence,int ordinal,
            long baseCapacity,long beforeCapacity,long afterCapacity,long units,long price,long duePeriod) {
        public Draft {
            Objects.requireNonNull(operation);Objects.requireNonNull(buyer);Objects.requireNonNull(seller);
            if(buyer.equals(seller)||period<0||haulWindow<0||cadence<1000||cadence>86400000||ordinal<1||ordinal>32
                    ||baseCapacity<1||beforeCapacity<baseCapacity||afterCapacity<=beforeCapacity||units<1||price<1||duePeriod<=period)
                throw new IllegalArgumentException("Invalid storage upgrade draft");
        }
    }
    public record Receipt(Draft draft,Long installedPeriod,String policy) {}
    public static final class Declined extends SQLException {Declined(String message){super(message);}}
    private final Connection c;private final StorageUpgradePolicy policy;private final BusinessOperationPolicy operations;
    public StorageUpgrades(Connection c,StorageUpgradePolicy policy,BusinessOperationPolicy operations){this.c=Objects.requireNonNull(c);this.policy=Objects.requireNonNull(policy);this.operations=Objects.requireNonNull(operations);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){s.execute("""
            CREATE TABLE business_storage_upgrades (
                operation_id TEXT PRIMARY KEY,buyer_id TEXT NOT NULL REFERENCES industry_runtime(business_id),
                seller_id TEXT NOT NULL REFERENCES industry_stores(business_id),
                period INTEGER NOT NULL CHECK(period>=0),haul_window INTEGER NOT NULL CHECK(haul_window>=0),
                cadence INTEGER NOT NULL CHECK(cadence BETWEEN 1000 AND 86400000),ordinal INTEGER NOT NULL CHECK(ordinal BETWEEN 1 AND 32),
                base_capacity INTEGER NOT NULL CHECK(base_capacity>0),before_capacity INTEGER NOT NULL CHECK(before_capacity>=base_capacity),
                after_capacity INTEGER NOT NULL CHECK(after_capacity>before_capacity),units INTEGER NOT NULL CHECK(units>0),price INTEGER NOT NULL CHECK(price>0),
                due_period INTEGER NOT NULL CHECK(due_period>period),installed_period INTEGER CHECK(installed_period>=due_period),
                policy TEXT NOT NULL,CHECK(buyer_id<>seller_id),UNIQUE(buyer_id,ordinal),UNIQUE(buyer_id,period)
            )
            """);s.execute("CREATE INDEX storage_upgrade_haul ON business_storage_upgrades(haul_window,cadence,seller_id)");}
    }
    public Optional<Draft> prepare(UUID operation,UUID buyer,UUID seller,long period,long window,long cadence,Set<UUID> worlds)throws SQLException {
        return transaction(()->prepareRows(operation,buyer,seller,period,window,cadence,Set.copyOf(worlds)));
    }
    Optional<Draft> prepareRows(UUID operation,UUID buyer,UUID seller,long period,long window,long cadence,Set<UUID> worlds)throws SQLException {
        owner();if(!policy.enabled()||buyer.equals(seller))return Optional.empty();
        var firms=new BusinessRepository(c);var destination=firms.findFounding(buyer).orElseThrow();var source=firms.findFounding(seller).orElseThrow();
        if(!policy.industries().contains(destination.business().industry())||!Set.of("craftsman","warehouse").contains(source.business().industry())
                ||!destination.business().settlementId().equals(source.business().settlementId())||!destination.site().worldId().equals(source.site().worldId())
                ||!worlds.contains(destination.site().worldId())||!staffed(buyer)||!staffed(seller))return Optional.empty();
        var need=needRows(buyer,period,cadence,worlds,0);
        if(need.isEmpty()||new IndustryRepository(c).store(seller).orElseThrow().units().getOrDefault(FITTINGS,0L)<policy.unitsPerUpgrade())return Optional.empty();
        try(var s=c.prepareStatement("SELECT last_ms FROM industry_runtime WHERE business_id=?")){s.setString(1,buyer.toString());try(var r=s.executeQuery()){if(!r.next()||r.getLong(1)/cadence!=window)return Optional.empty();}}
        var n=need.orElseThrow();return Optional.of(new Draft(operation,buyer,seller,period,window,cadence,n.ordinal(),n.baseCapacity(),n.beforeCapacity(),n.afterCapacity(),n.units(),n.price(),Math.addExact(period,policy.deliveryPeriods())));
    }
    public record Need(UUID buyer,int ordinal,long baseCapacity,long beforeCapacity,long afterCapacity,long units,long price) {}
    Optional<Need> needRows(UUID buyer,long period,long cadence,Set<UUID> worlds,long proposedDebit)throws SQLException {
        owner();if(proposedDebit<0)throw new IllegalArgumentException("Negative proposed business spending");
        var founding=new BusinessRepository(c).findFounding(buyer).orElseThrow();
        if(!policy.enabled()||!policy.industries().contains(founding.business().industry())||!worlds.contains(founding.site().worldId())||!staffed(buyer))return Optional.empty();
        try(var s=c.prepareStatement("SELECT cadence,completed,target FROM industry_runtime WHERE business_id=?")) {
            s.setString(1,buyer.toString());try(var r=s.executeQuery()){if(!r.next()||r.getLong(1)!=cadence||r.getLong(2)!=period||r.getLong(3)!=period)return Optional.empty();}
        }
        var store=new IndustryRepository(c).store(buyer).orElseThrow();
        long base=store.capacity();int count=0;long expected=0;
        try(var s=c.prepareStatement("SELECT * FROM business_storage_upgrades WHERE buyer_id=? ORDER BY ordinal LIMIT 33")) {
            s.setString(1,buyer.toString());try(var r=s.executeQuery()){while(r.next()) {
                if(++count>32)throw new SQLException("Storage upgrade history budget exceeded");
                if(r.getInt("ordinal")!=count)throw new SQLException("Storage upgrade history gap");
                if(count==1){base=r.getLong("base_capacity");expected=base;}
                if(r.getLong("base_capacity")!=base||r.getLong("before_capacity")!=expected)throw new SQLException("Storage upgrade capacity chain changed");
                expected=r.getLong("after_capacity");if(r.getObject("installed_period")==null)return Optional.empty();
                if(r.getLong("period")==period)return Optional.empty();
            }}
        }
        if(count>0&&expected!=store.capacity())throw new SQLException("Saved storage changed outside audited upgrade chain");
        long next=policy.nextCapacity(base,store.capacity(),count,store.capacity()-store.freeSpace(),Math.max(0,store.hundredths()-proposedDebit));
        if(next==store.capacity())return Optional.empty();
        return Optional.of(new Need(buyer,count+1,base,store.capacity(),next,policy.unitsPerUpgrade(),policy.invoice()));
    }
    public Receipt accept(Draft draft,long haulCapacity,Set<UUID> worlds)throws SQLException {
        return transaction(()->acceptRows(draft,haulCapacity,Set.copyOf(worlds)));
    }
    Receipt acceptRows(Draft d,long haulCapacity,Set<UUID> worlds)throws SQLException {
        owner();var old=find(d.operation());if(old.isPresent()){if(!old.orElseThrow().draft().equals(d))throw new SQLException("Storage operation reused with different terms");return old.orElseThrow();}
        if(haulCapacity<1)throw new IllegalArgumentException("Positive local hauling capacity required");
        try(var s=c.prepareStatement("SELECT 1 FROM industry_operations WHERE id=?")){s.setString(1,d.operation().toString());try(var r=s.executeQuery()){if(r.next())throw new SQLException("Storage operation already used by ordinary industry");}}
        if(!prepareRows(d.operation(),d.buyer(),d.seller(),d.period(),d.haulWindow(),d.cadence(),worlds).filter(d::equals).isPresent())throw new Declined("Storage need, cash, goods, staffing, capacity or current world/clock changed");
        var firms=new BusinessRepository(c);var town=firms.findBusiness(d.buyer()).orElseThrow().settlementId();
        if(Math.addExact(haulUsed(c,town,d.haulWindow(),d.cadence()),ordinaryHaul(town,d.haulWindow(),d.cadence()))>haulCapacity-d.units())throw new Declined("Shared local hauling capacity exhausted");
        var industry=new IndustryRepository(c);var buyer=industry.store(d.buyer()).orElseThrow();var seller=industry.store(d.seller()).orElseThrow();
        try(var s=c.prepareStatement("UPDATE industry_stock SET units=units-? WHERE business_id=? AND commodity=? AND units>=?")) {
            s.setLong(1,d.units());s.setString(2,d.seller().toString());s.setString(3,FITTINGS.id());s.setLong(4,d.units());if(s.executeUpdate()!=1)throw new SQLException("Fittings changed");
        }
        cash(d.buyer(),buyer.hundredths(),buyer.hundredths()-d.price());cash(d.seller(),seller.hundredths(),Math.addExact(seller.hundredths(),d.price()));
        try(var s=c.prepareStatement("INSERT INTO business_storage_upgrades VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,NULL,?)")) {
            int n=1;s.setString(n++,d.operation().toString());s.setString(n++,d.buyer().toString());s.setString(n++,d.seller().toString());
            s.setLong(n++,d.period());s.setLong(n++,d.haulWindow());s.setLong(n++,d.cadence());s.setInt(n++,d.ordinal());
            s.setLong(n++,d.baseCapacity());s.setLong(n++,d.beforeCapacity());s.setLong(n++,d.afterCapacity());s.setLong(n++,d.units());s.setLong(n++,d.price());s.setLong(n++,d.duePeriod());s.setString(n,policy.toString());s.executeUpdate();
        }return find(d.operation()).orElseThrow();
    }
    public int deliver(UUID buyer,long period,Set<UUID> worlds)throws SQLException {return transaction(()->deliverRows(buyer,period,Set.copyOf(worlds)));}
    int deliverRows(UUID buyer,long period,Set<UUID> worlds)throws SQLException {
        owner();var founding=new BusinessRepository(c).findFounding(buyer).orElseThrow();
        if(!worlds.contains(founding.site().worldId())||!staffed(buyer))return 0;
        var pending=new ArrayList<Receipt>();try(var s=c.prepareStatement("SELECT operation_id FROM business_storage_upgrades WHERE buyer_id=? AND installed_period IS NULL AND due_period<=? ORDER BY ordinal LIMIT 2")) {
            s.setString(1,buyer.toString());s.setLong(2,period);try(var r=s.executeQuery()){while(r.next())pending.add(find(UUID.fromString(r.getString(1))).orElseThrow());}
        }
        if(pending.size()>1)throw new SQLException("Multiple pending storage installations");
        for(var receipt:pending) {
            var d=receipt.draft();try(var s=c.prepareStatement("UPDATE industry_stores SET capacity=? WHERE business_id=? AND capacity=?")) {
                s.setLong(1,d.afterCapacity());s.setString(2,buyer.toString());s.setLong(3,d.beforeCapacity());if(s.executeUpdate()!=1)throw new SQLException("Installation requires its accepted saved capacity; paid fittings retained");
            }
            try(var s=c.prepareStatement("UPDATE business_storage_upgrades SET installed_period=? WHERE operation_id=? AND installed_period IS NULL")) {
                s.setLong(1,period);s.setString(2,d.operation().toString());if(s.executeUpdate()!=1)throw new SQLException("Storage installation changed");
            }
        }return pending.size();
    }
    public Optional<Receipt> find(UUID operation)throws SQLException {
        try(var s=c.prepareStatement("SELECT * FROM business_storage_upgrades WHERE operation_id=?")){s.setString(1,operation.toString());try(var r=s.executeQuery()){if(!r.next())return Optional.empty();
            var d=new Draft(operation,UUID.fromString(r.getString("buyer_id")),UUID.fromString(r.getString("seller_id")),r.getLong("period"),r.getLong("haul_window"),r.getLong("cadence"),r.getInt("ordinal"),r.getLong("base_capacity"),r.getLong("before_capacity"),r.getLong("after_capacity"),r.getLong("units"),r.getLong("price"),r.getLong("due_period"));
            long applied=r.getLong("installed_period");return Optional.of(new Receipt(d,r.wasNull()?null:applied,r.getString("policy")));
        }}
    }
    static long haulUsed(Connection c,UUID town,long window,long cadence)throws SQLException {
        int version;try(var v=c.createStatement();var r=v.executeQuery("PRAGMA user_version")){if(!r.next()||(version=r.getInt(1))<80)return 0;}
        // This existing shared nonstandard-cargo counter also covers project-owned construction shipments.
        long total=version>=106?ConstructionSupplyOrders.haulUsed(c,town,window,cadence):0;
        if(version>=115)total=Math.addExact(total,ConstructionSupplyReturns.haulUsed(c,town,window,cadence));
        try(var s=c.prepareStatement("SELECT u.units FROM business_storage_upgrades u JOIN businesses b ON b.id=u.seller_id WHERE b.settlement_id=? AND u.haul_window=? AND u.cadence=?")) {
            s.setString(1,town.toString());s.setLong(2,window);s.setLong(3,cadence);try(var r=s.executeQuery()){while(r.next())total=Math.addExact(total,r.getLong(1));}
        }return total;
    }
    static long capitalValue(Connection c,UUID business)throws SQLException {
        try(var v=c.createStatement();var r=v.executeQuery("PRAGMA user_version")){if(!r.next()||r.getInt(1)<80)return 0;}
        long total=0;try(var s=c.prepareStatement("SELECT price FROM business_storage_upgrades WHERE buyer_id=?")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){while(r.next())total=Math.addExact(total,r.getLong(1));}
        }return total;
    }
    String reviewRows(UUID buyer,long period,long window,long cadence,long haulCapacity,Set<UUID> worlds)throws SQLException {
        owner();if(!policy.enabled())return "New storage upgrade purchases disabled";
        var founding=new BusinessRepository(c).findFounding(buyer).orElseThrow();var sellers=new ArrayList<UUID>();
        try(var s=c.prepareStatement("""
            SELECT b.id FROM businesses b JOIN business_sites p ON p.id=b.site_id JOIN industry_stock k ON k.business_id=b.id
            WHERE b.settlement_id=? AND p.world_id=? AND b.industry IN('craftsman','warehouse') AND b.id<>?
              AND k.commodity='storage_fittings' AND k.units>=? ORDER BY b.id LIMIT ?
            """)) {
            s.setString(1,founding.business().settlementId().toString());s.setString(2,founding.site().worldId().toString());s.setString(3,buyer.toString());s.setLong(4,policy.unitsPerUpgrade());s.setInt(5,policy.sellersPerReview()+1);
            try(var r=s.executeQuery()){while(r.next())sellers.add(UUID.fromString(r.getString(1)));}
        }
        if(sellers.size()>policy.sellersPerReview())return "Storage supplier review budget exceeded; no partial choice";
        for(var seller:sellers) {
            UUID operation=UUID.nameUUIDFromBytes(("civilizations:storage-upgrade-v1:"+buyer+":"+period).getBytes(StandardCharsets.UTF_8));
            var draft=prepareRows(operation,buyer,seller,period,window,cadence,worlds);if(draft.isEmpty())continue;
            try{var paid=acceptRows(draft.orElseThrow(),haulCapacity,worlds);return "Paid real storage fittings; capacity "+paid.draft().beforeCapacity()+" -> "+paid.draft().afterCapacity()+" after delivery";}
            catch(Declined unavailable){return "Storage upgrade waits: "+unavailable.getMessage();}
        }return "No affordable current storage pressure and stocked qualified fitting supplier";
    }
    private long ordinaryHaul(UUID town,long window,long cadence)throws SQLException {
        long total=0;try(var s=c.prepareStatement("SELECT t.units FROM local_shipments t JOIN industry_auto_shipments a ON a.shipment_id=t.id JOIN businesses b ON b.id=t.seller_id WHERE b.settlement_id=? AND a.haul_window=? AND a.haul_cadence=?")) {
            s.setString(1,town.toString());s.setLong(2,window);s.setLong(3,cadence);try(var r=s.executeQuery()){while(r.next())total=Math.addExact(total,r.getLong(1));}
        }return total;
    }
    private boolean staffed(UUID id)throws SQLException {
        if(!new BusinessOperations(c,operations).evaluate(id).permitted())return false;
        var firms=new BusinessRepository(c);var founding=firms.findFounding(id).orElseThrow();var people=new PopulationRepository(c);
        for(var job:firms.currentWorkers(id)){var p=people.findResident(job.residentId()).orElseThrow();if(p.settlementId().equals(founding.business().settlementId())&&WorkEligibility.evaluate(p,people.findWorkProfile(p.id()).orElseThrow(),founding.workerRequirements()).eligible())return true;}return false;
    }
    private void cash(UUID id,long before,long after)throws SQLException {
        try(var s=c.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=? AND hundredths=?")){s.setLong(1,after);s.setString(2,id.toString());s.setLong(3,before);if(s.executeUpdate()!=1)throw new SQLException("Business cash changed");}
    }
    private void owner()throws SQLException{if(c.getAutoCommit())throw new SQLException("Storage rows require owner transaction");}
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!c.getAutoCommit())throw new SQLException("Storage upgrades require idle SQL owner");c.setAutoCommit(false);
        try{var r=work.run();c.commit();return r;}catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
}
