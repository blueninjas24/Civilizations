package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.sql.*;
import java.util.*;

/** SQL-owner backend for a dedicated herd business. Not wired to automatic adoption or live ticks.
 * Initial animals require confirmed village ownership and trusted species/age observations.
 * Births are logical cohorts; physical reconciliation must precede live activation. No slaughter API
 * until retiring physical animals can be coordinated without duplicate drops/products. */
public final class LivestockRepository {
    public record Herd(UUID business, LivestockHerd.Species species, Commodity feed, LivestockHerd.State state) {}
    private final Connection connection;
    public LivestockRepository(Connection connection) { this.connection=Objects.requireNonNull(connection); }

    static void createSchema(Connection connection)throws SQLException {
        try(var s=connection.createStatement()) {
            s.execute("""
                    CREATE TABLE livestock_herds (
                        business_id TEXT PRIMARY KEY NOT NULL REFERENCES industry_stores(business_id),
                        species TEXT NOT NULL CHECK(species IN('COW','CHICKEN')),
                        capacity INTEGER NOT NULL CHECK(typeof(capacity)='integer' AND capacity BETWEEN 0 AND 1000000),
                        feed TEXT NOT NULL,opening_terms TEXT NOT NULL,
                        FOREIGN KEY(business_id,feed) REFERENCES industry_stock(business_id,commodity)
                    )
                    """);
            s.execute("""
                    CREATE TABLE livestock_initial_animals (
                        animal_id TEXT PRIMARY KEY NOT NULL REFERENCES animal_ownership(animal_id),
                        business_id TEXT NOT NULL REFERENCES livestock_herds(business_id),
                        mature_period INTEGER NOT NULL CHECK(typeof(mature_period)='integer' AND mature_period BETWEEN 0 AND 1000)
                    )
                    """);
            s.execute("""
                    CREATE TABLE livestock_periods (
                        business_id TEXT NOT NULL REFERENCES livestock_herds(business_id),
                        period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=0),
                        operation_id TEXT UNIQUE,terms TEXT NOT NULL,
                        adults INTEGER NOT NULL CHECK(typeof(adults)='integer' AND adults BETWEEN 0 AND 1000000),
                        next_birth INTEGER NOT NULL CHECK(typeof(next_birth)='integer' AND next_birth>=0),
                        matured INTEGER NOT NULL CHECK(typeof(matured)='integer' AND matured>=0),
                        births INTEGER NOT NULL CHECK(typeof(births)='integer' AND births>=0),
                        feed_used INTEGER NOT NULL CHECK(typeof(feed_used)='integer' AND feed_used>=0),
                        work_used INTEGER NOT NULL CHECK(typeof(work_used)='integer' AND work_used>=0),
                        renewable_units INTEGER NOT NULL CHECK(typeof(renewable_units)='integer' AND renewable_units>=0),
                        before_feed INTEGER NOT NULL CHECK(typeof(before_feed)='integer' AND before_feed>=feed_used),
                        after_feed INTEGER NOT NULL CHECK(typeof(after_feed)='integer' AND after_feed=before_feed-feed_used),
                        before_product INTEGER NOT NULL CHECK(typeof(before_product)='integer' AND before_product>=0),
                        after_product INTEGER NOT NULL CHECK(typeof(after_product)='integer' AND after_product=before_product+renewable_units),
                        available_work INTEGER NOT NULL CHECK(typeof(available_work)='integer' AND available_work>=work_used),
                        free_space INTEGER NOT NULL CHECK(typeof(free_space)='integer' AND free_space>=renewable_units),
                        explanation TEXT NOT NULL CHECK(length(explanation)>0),
                        PRIMARY KEY(business_id,period),CHECK((period=0 AND operation_id IS NULL) OR (period>0 AND operation_id IS NOT NULL))
                    )
                    """);
            s.execute("""
                    CREATE TABLE livestock_young (
                        business_id TEXT NOT NULL,period INTEGER NOT NULL,
                        mature_period INTEGER NOT NULL CHECK(typeof(mature_period)='integer' AND mature_period>period),
                        heads INTEGER NOT NULL CHECK(typeof(heads)='integer' AND heads BETWEEN 1 AND 1000000),
                        PRIMARY KEY(business_id,period,mature_period),
                        FOREIGN KEY(business_id,period) REFERENCES livestock_periods(business_id,period)
                    )
                    """);
            s.execute("CREATE INDEX livestock_initial_herd ON livestock_initial_animals(business_id,animal_id)");
        }
    }

    /** Each physical UUID may seed only one herd, ever. Zero denotes observed adults;
     * positive values are trusted remaining active periods to maturity. No goods/cash supplied here. */
    public Herd open(UUID business,LivestockHerd.Species species,Commodity feed,long capacity,
                     Map<UUID,Integer> observedAnimals)throws SQLException {
        Objects.requireNonNull(business);Objects.requireNonNull(species);Objects.requireNonNull(feed);
        var animals=new TreeMap<>(Map.copyOf(observedAnimals));
        if(capacity<0||capacity>1_000_000||animals.isEmpty()||animals.size()>4096||
                animals.values().stream().anyMatch(v->v<0||v>1000))throw new IllegalArgumentException("Invalid herd opening observations");
        if(feed.equals(product(species)))throw new IllegalArgumentException("Feed must differ from herd output");
        String terms=species+"|"+feed.id()+"|"+capacity+"|"+animals;
        return transaction(()->{
            try(var s=connection.prepareStatement("SELECT opening_terms FROM livestock_herds WHERE business_id=?")) {
                s.setString(1,business.toString());try(var r=s.executeQuery()) {
                    if(r.next()) {if(!terms.equals(r.getString(1)))throw new SQLException("Herd opening identity/observations changed");return load(business).orElseThrow();}
                }
            }
            var b=new BusinessRepository(connection).findBusiness(business).orElseThrow(()->new SQLException("Unknown herd business"));
            if(!b.industry().equals("livestock"))throw new SQLException("A dedicated livestock business is required; existing producers retain their labor");
            BusinessClosure.requireOpen(connection,business);WorkplaceOperations.requirePermitted(connection,business);
            // Existing hybrid crop farms retain their exact catalogue and labor accounting.
            try(var s=connection.prepareStatement("SELECT 1 FROM farm_market_stores WHERE business_id=?")) {
                s.setString(1,business.toString());try(var r=s.executeQuery()){if(r.next())throw new SQLException("Hybrid crop farm needs an explicit combined-labor/catalogue transition");}
            }
            var store=new IndustryRepository(connection).store(business).orElseThrow();
            requireCatalogue(store,feed,species);
            var founding=new BusinessRepository(connection).findFounding(business).orElseThrow();
            var owners=new AnimalOwnershipRepository(connection);
            long adults=0;var cohorts=new TreeMap<Long,Long>();
            for(var animal:animals.entrySet()) {
                var claim=owners.load(animal.getKey()).orElseThrow(()->new SQLException("Animal lacks saved ownership"));
                if(claim.kind()!=LivestockAdoption.Ownership.VILLAGE||!b.settlementId().equals(claim.settlement())||
                        !founding.site().worldId().equals(claim.originWorld()))throw new SQLException("Animal ownership/world differs from herd");
                if(animal.getValue()==0)adults++;else cohorts.merge(animal.getValue().longValue(),1L,Math::addExact);
            }
            try(var s=connection.prepareStatement("INSERT INTO livestock_herds VALUES(?,?,?,?,?)")) {
                s.setString(1,business.toString());s.setString(2,species.name());s.setLong(3,capacity);s.setString(4,feed.id());s.setString(5,terms);s.executeUpdate();
            }
            for(var animal:animals.entrySet())try(var s=connection.prepareStatement("INSERT INTO livestock_initial_animals VALUES(?,?,?)")) {
                s.setString(1,animal.getKey().toString());s.setString(2,business.toString());s.setInt(3,animal.getValue());s.executeUpdate();
            }
            var young=cohorts.entrySet().stream().map(e->new LivestockHerd.Young(e.getKey(),e.getValue())).toList();
            var state=new LivestockHerd.State(0,adults,capacity,0,young);
            save(business,null,terms,new LivestockHerd.Plan(state,0,0,0,0,0,0,0),store,feed,species,0,"Confirmed initial animal roster; no production or capital created");
            return new Herd(business,species,feed,state);
        });
    }

    public Optional<Herd> load(UUID business)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM livestock_herds WHERE business_id=?")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                long period;
                try(var q=connection.prepareStatement("SELECT MAX(period) FROM livestock_periods WHERE business_id=?")) {
                    q.setString(1,business.toString());try(var rows=q.executeQuery()){rows.next();period=rows.getLong(1);if(rows.wasNull())throw new SQLException("Herd checkpoint missing");}
                }
                var plan=readPlan(business,period,r.getLong("capacity"));
                return Optional.of(new Herd(business,LivestockHerd.Species.valueOf(r.getString("species")),new Commodity(r.getString("feed")),plan.after()));
            }
        }
    }

    /** Atomic paid feed -> eggs/milk and cohort checkpoint. One herd per dedicated business;
     * caller supplies bounded work rates, not invented worker counts or supplies.
     * Disabled/absent worlds return unchanged without reserving an operation or accruing time. */
    public LivestockHerd.Plan advance(UUID operation,UUID business,long expectedPeriod,long workPerWorker,long siteWorkLimit,
                                      LivestockPolicy policy,BusinessOperationPolicy operations,boolean worldActive)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(business);Objects.requireNonNull(policy);Objects.requireNonNull(operations);
        if(expectedPeriod<1||workPerWorker<1||workPerWorker>1_000_000||siteWorkLimit<0||siteWorkLimit>1_000_000)
            throw new IllegalArgumentException("Invalid herd work request");
        String terms=business+"|"+expectedPeriod+"|"+workPerWorker+"|"+siteWorkLimit+"|"+policy+"|"+operations+"|"+worldActive;
        return transaction(()->{
            var herd=load(business).orElseThrow(()->new SQLException("Herd not initialized"));
            try(var s=connection.prepareStatement("SELECT business_id,period,terms FROM livestock_periods WHERE operation_id=?")) {
                s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                    if(r.next()){if(!terms.equals(r.getString("terms")))throw new SQLException("Herd operation reused with changed terms");return readPlan(business,r.getLong("period"),herd.state().capacity());}
                }
            }
            if(!worldActive||!policy.enabled())return new LivestockHerd.Plan(herd.state(),0,0,0,0,0,0,0);
            if(expectedPeriod!=Math.incrementExact(herd.state().period()))throw new SQLException("Herd period stale or skipped");
            var businesses=new BusinessRepository(connection);var people=new PopulationRepository(connection);
            long workers=0;
            var availability=new BusinessOperations(connection,operations).evaluate(business);
            if(availability.permitted()) {
                var requirements=businesses.findFounding(business).orElseThrow().workerRequirements();
                for(var job:businesses.currentWorkers(business)) {
                    var person=people.findResident(job.residentId()).orElseThrow();
                    if(WorkEligibility.evaluate(person,people.findWorkProfile(person.id()).orElseThrow(),requirements).eligible())workers++;
                }
            }
            var store=new IndustryRepository(connection).store(business).orElseThrow();requireCatalogue(store,herd.feed(),herd.species());
            // Conservative output budget: never borrow incoming reservations or anticipated feed space.
            long work=Math.min(siteWorkLimit,Math.multiplyExact(workers,workPerWorker));
            var plan=LivestockHerd.plan(herd.state(),store.units().get(herd.feed()),work,
                    store.freeSpace(),0,policy,herd.species(),true);
            changeStock(business,herd.feed(),-plan.feedUsed());changeStock(business,product(herd.species()),plan.renewableUnits());
            if(new IndustryRepository(connection).store(business).orElseThrow().freeSpace()<0)throw new SQLException("Herd output exceeded storage");
            save(business,operation,terms,plan,store,herd.feed(),herd.species(),work,
                    availability.explanation()+"; qualified workers="+workers+"; work="+work+"; feed="+store.units().get(herd.feed())+
                    "; output space="+store.freeSpace()+"; births="+plan.births()+"; product="+plan.renewableUnits());return plan;
        });
    }

    private static Commodity product(LivestockHerd.Species species){return new Commodity(species==LivestockHerd.Species.COW?"milk":"eggs");}
    private static void requireCatalogue(IndustryRepository.Store store,Commodity feed,LivestockHerd.Species species)throws SQLException {
        for(var commodity:List.of(feed,product(species)))if(!Long.valueOf(1).equals(store.spacePerUnit().get(commodity)))
            throw new SQLException("Herd store must support unit-sized feed and species product");
    }
    private void changeStock(UUID business,Commodity commodity,long delta)throws SQLException {
        try(var s=connection.prepareStatement("UPDATE industry_stock SET units=units+? WHERE business_id=? AND commodity=? AND units+?>=0")) {
            s.setLong(1,delta);s.setString(2,business.toString());s.setString(3,commodity.id());s.setLong(4,delta);
            if(s.executeUpdate()!=1)throw new SQLException("Herd stock missing or insufficient");
        }
    }
    private void save(UUID business,UUID operation,String terms,LivestockHerd.Plan plan,IndustryRepository.Store before,
                      Commodity feed,LivestockHerd.Species species,long work,String explanation)throws SQLException {
        var state=plan.after();
        try(var s=connection.prepareStatement("INSERT INTO livestock_periods VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,business.toString());s.setLong(2,state.period());s.setString(3,operation==null?null:operation.toString());s.setString(4,terms);
            s.setLong(5,state.adults());s.setLong(6,state.nextBirthPeriod());s.setLong(7,plan.matured());s.setLong(8,plan.births());
            s.setLong(9,plan.feedUsed());s.setLong(10,plan.workUsed());s.setLong(11,plan.renewableUnits());
            s.setLong(12,before.units().get(feed));s.setLong(13,Math.subtractExact(before.units().get(feed),plan.feedUsed()));
            s.setLong(14,before.units().get(product(species)));s.setLong(15,Math.addExact(before.units().get(product(species)),plan.renewableUnits()));
            s.setLong(16,work);s.setLong(17,before.freeSpace());s.setString(18,explanation);s.executeUpdate();
        }
        for(var cohort:state.young())try(var s=connection.prepareStatement("INSERT INTO livestock_young VALUES(?,?,?,?)")) {
            s.setString(1,business.toString());s.setLong(2,state.period());s.setLong(3,cohort.maturePeriod());s.setLong(4,cohort.heads());s.executeUpdate();
        }
    }
    private LivestockHerd.Plan readPlan(UUID business,long period,long capacity)throws SQLException {
        var young=new ArrayList<LivestockHerd.Young>();
        try(var s=connection.prepareStatement("SELECT mature_period,heads FROM livestock_young WHERE business_id=? AND period=? ORDER BY mature_period")) {
            s.setString(1,business.toString());s.setLong(2,period);try(var r=s.executeQuery()){while(r.next())young.add(new LivestockHerd.Young(r.getLong(1),r.getLong(2)));}
        }
        try(var s=connection.prepareStatement("SELECT * FROM livestock_periods WHERE business_id=? AND period=?")) {
            s.setString(1,business.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                if(!r.next())throw new SQLException("Missing herd period");
                return new LivestockHerd.Plan(new LivestockHerd.State(period,r.getLong("adults"),capacity,r.getLong("next_birth"),young),
                        r.getLong("matured"),r.getLong("births"),0,r.getLong("feed_used"),r.getLong("work_used"),r.getLong("renewable_units"),0);
            }
        }
    }
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Herd requires no active transaction");
        connection.setAutoCommit(false);
        try{T result=work.run();connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
