package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.Resident;
import java.sql.*;
import java.util.*;

/** SQL-owner immutable offers and online-only deadlines. Expiry is evidence for future closure, not deletion. */
public final class BusinessSaleListings {
    public enum Status { OPEN,SOLD,RESOLVED,EXPIRED }
    public record Listing(UUID business,UUID seller,long price,long deadlineMillis,long createdStep,
                          long lastMillis,long elapsedMillis,Status status,UUID saleOperation) {}
    private final Connection connection;
    public BusinessSaleListings(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public Listing open(UUID business,long price,long deadlineMillis,long now,long step)throws SQLException {
        Objects.requireNonNull(business);
        if(price<1||deadlineMillis<1000||deadlineMillis>86400000||now<0||step<0)throw new IllegalArgumentException("Invalid business listing");
        return transaction(()->{
            var firms=new BusinessRepository(connection);var b=firms.findBusiness(business).orElseThrow(()->new SQLException("Unknown listing business"));
            var old=find(business,b.ownerId());
            if(old.isPresent()) {
                var saved=old.orElseThrow();
                if(saved.price()!=price||saved.deadlineMillis()!=deadlineMillis)throw new SQLException("Listing reused with different accepted terms");
                return saved;
            }
            BusinessClosure.requireOpen(connection,business);
            if(step<b.foundedStep())throw new SQLException("Listing predates founding");
            var owner=new PopulationRepository(connection).findResident(b.ownerId()).orElseThrow();
            if(owner.status()!=Resident.Status.DECEASED)throw new BusinessSales.Declined("Listing requires deceased current owner");
            if(new BusinessSuccession(connection).candidate(business).isPresent())throw new BusinessSales.Declined("Qualified inheritance takes priority over listing");
            try(var s=connection.prepareStatement("INSERT INTO business_sale_listings VALUES(?,?,?,?,?,?,0,'OPEN',NULL)")) {
                s.setString(1,business.toString());s.setString(2,b.ownerId().toString());s.setLong(3,price);s.setLong(4,deadlineMillis);
                s.setLong(5,step);s.setLong(6,now);s.executeUpdate();
            }
            return find(business,b.ownerId()).orElseThrow();
        });
    }
    /** Startup advances only the observation anchor, preserving earned time without downtime catch-up. */
    public void resume(long now)throws SQLException {observe(now,null,true);}
    public void observe(long now,Set<UUID> worlds)throws SQLException {observe(now,worlds==null?null:Set.copyOf(worlds),false);}
    private void observe(long now,Set<UUID> worlds,boolean restart)throws SQLException {
        if(now<0)throw new IllegalArgumentException("Negative listing time");
        transaction(()->{
            var rows=new ArrayList<Listing>();var identities=new HashMap<UUID,UUID>();var owners=new HashMap<UUID,UUID>();
            try(var s=connection.createStatement();var r=s.executeQuery("SELECT l.*,s.world_id,b.owner_id FROM business_sale_listings l JOIN businesses b ON b.id=l.business_id JOIN business_sites s ON s.id=b.site_id WHERE l.status='OPEN' ORDER BY l.business_id,l.seller_id LIMIT 4097")) {
                while(r.next()) {var listing=read(r);rows.add(listing);identities.put(listing.business(),UUID.fromString(r.getString("world_id")));owners.put(listing.business(),UUID.fromString(r.getString("owner_id")));}
            }
            if(rows.size()>4096)throw new SQLException("Listing clock review budget exceeded");
            for(var row:rows) {
                long elapsed=row.elapsedMillis();Status status=row.status();
                if(!row.seller().equals(owners.get(row.business())))status=Status.RESOLVED;
                else if(!restart&&(worlds==null||worlds.contains(identities.get(row.business())))) {
                    long delta=Math.max(now,row.lastMillis())-row.lastMillis();
                    elapsed+=Math.min(delta,row.deadlineMillis()-elapsed);
                    if(elapsed==row.deadlineMillis())status=Status.EXPIRED;
                }
                try(var s=connection.prepareStatement("UPDATE business_sale_listings SET last_ms=?,elapsed_ms=?,status=? WHERE business_id=? AND seller_id=? AND status='OPEN'")) {
                    s.setLong(1,Math.max(now,row.lastMillis()));s.setLong(2,elapsed);s.setString(3,status.name());
                    s.setString(4,row.business().toString());s.setString(5,row.seller().toString());
                    if(s.executeUpdate()!=1)throw new SQLException("Stale listing clock");
                }
            }
            return null;
        });
    }
    /** Called inside the payment transaction; unlisted explicit backend sales remain supported. */
    boolean requireAcceptedPrice(BusinessSales.Request request)throws SQLException {
        var found=find(request.business(),request.seller());if(found.isEmpty())return false;
        var listing=found.orElseThrow();
        if(listing.status()!=Status.OPEN||listing.elapsedMillis()>=listing.deadlineMillis())throw new BusinessSales.Declined("Business listing is no longer open");
        if(listing.price()!=request.price())throw new BusinessSales.Declined("Business purchase differs from accepted listing price");
        return true;
    }
    void sold(BusinessSales.Request request,boolean listed)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Listing completion requires sale transaction");
        if(!listed)return;
        try(var s=connection.prepareStatement("UPDATE business_sale_listings SET status='SOLD',sale_operation=? WHERE business_id=? AND seller_id=? AND status='OPEN'")) {
            s.setString(1,request.operation().toString());s.setString(2,request.business().toString());s.setString(3,request.seller().toString());
            if(s.executeUpdate()!=1)throw new SQLException("Accepted listing missing at sale completion");
        }
    }
    public Optional<Listing> find(UUID business,UUID seller)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM business_sale_listings WHERE business_id=? AND seller_id=?")) {
            s.setString(1,business.toString());s.setString(2,seller.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}
        }
    }
    private static Listing read(ResultSet r)throws SQLException {
        String operation=r.getString("sale_operation");
        return new Listing(UUID.fromString(r.getString("business_id")),UUID.fromString(r.getString("seller_id")),r.getLong("price"),
                r.getLong("deadline_ms"),r.getLong("created_step"),r.getLong("last_ms"),r.getLong("elapsed_ms"),Status.valueOf(r.getString("status")),operation==null?null:UUID.fromString(operation));
    }
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T> T transaction(Work<T> work)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Listing requires idle SQL owner");connection.setAutoCommit(false);
        try {var result=work.run();connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
