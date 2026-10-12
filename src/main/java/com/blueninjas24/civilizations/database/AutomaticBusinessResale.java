package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;

/** Bounded existing-business sale review; no founding, grants, automatic jobs or ownership cap. */
public final class AutomaticBusinessResale {
    private final Connection connection;
    private final BusinessResalePolicy policy;
    private final BusinessPurchasePolicy funding;
    private final long seed;
    public AutomaticBusinessResale(Connection connection,BusinessResalePolicy policy,BusinessPurchasePolicy funding,long seed) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.funding=Objects.requireNonNull(funding);this.seed=seed;
    }
    public String review(UUID id,long step,long now)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Automatic resale requires idle SQL owner");
        if(step<0||now<0)throw new IllegalArgumentException("Negative resale time");
        connection.setAutoCommit(false);
        try {String result=reviewRows(id,step,now);connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private String reviewRows(UUID id,long step,long now)throws SQLException {
        var firms=new BusinessRepository(connection);var business=firms.findBusiness(id).orElseThrow();
        var people=new PopulationRepository(connection);var owner=people.findResident(business.ownerId()).orElseThrow();
        if(!policy.enabled())return "Automatic resale disabled";
        if(new BusinessClosure(connection).find(id).isPresent())return "Closed business retained; no automatic reopening";
        if(!owner.status().name().equals("DECEASED"))return "Living owner retained; no forced sale";
        if(step<business.foundedStep())throw new SQLException("Resale predates founding");
        if(new BusinessSuccession(connection).candidate(id).isPresent())return "Qualified inheritance takes priority";
        var offers=new BusinessSaleListings(connection);var listing=offers.find(id,business.ownerId()).orElse(null);
        if(listing==null) {
            var store=new IndustryRepository(connection).store(id).orElse(null);
            long cargo=0;try(var s=connection.prepareStatement("SELECT price FROM local_shipments WHERE buyer_id=? AND delivered_period IS NULL")) {
                s.setString(1,id.toString());try(var r=s.executeQuery()){while(r.next())cargo=Math.addExact(cargo,r.getLong(1));}
            }
            try(var s=connection.prepareStatement("SELECT price FROM workshop_starter_orders WHERE business_id=? AND delivered_period IS NULL")) {
                s.setString(1,id.toString());try(var r=s.executeQuery()){while(r.next())cargo=Math.addExact(cargo,r.getLong(1));}
            }
            long price;
            try {price=policy.price(firms.findFounding(id).orElseThrow().startupHundredths(),store==null?0:store.hundredths(),cargo,store==null?Map.of():store.units());}
            catch(IllegalArgumentException|ArithmeticException refusal){return save(id,business.ownerId(),"",step,now,"Sale valuation deferred: "+refusal.getMessage());}
            listing=offers.openRows(id,price,policy.deadlineMillis(),now,step);
        }
        if(listing.status()!=BusinessSaleListings.Status.OPEN)return save(id,business.ownerId(),"",step,now,"Saved sale offer "+listing.status()+"; accepted terms retained");
        var candidates=new ArrayList<UUID>();String cursor=cursor(id,business.ownerId());
        try(var s=connection.prepareStatement("SELECT id FROM residents WHERE settlement_id=? AND status='ALIVE' AND id>? ORDER BY id LIMIT ?")) {
            s.setString(1,business.settlementId().toString());s.setString(2,cursor);s.setInt(3,policy.buyersPerReview());try(var r=s.executeQuery()){while(r.next())candidates.add(UUID.fromString(r.getString(1)));}
        }
        if(candidates.isEmpty()&&!cursor.isEmpty())return save(id,business.ownerId(),"",step,now,"Buyer page wrapped; saved offer/funds retained");
        String next=candidates.isEmpty()?"":candidates.getLast().toString();
        // Seed chooses among this feasible bounded page; saved UUID cursor ensures later residents are reviewed.
        candidates.sort(Comparator.comparingLong(buyer->rank(id,business.ownerId(),buyer)));
        int qualified=0;
        var succession=new BusinessSuccession(connection);var sales=new BusinessSales(connection,funding);
        for(var buyer:candidates) {
            if(!succession.qualifiedBuyer(buyer,id))continue;qualified++;
            var source=businessSource(buyer,business.settlementId(),id);var estate=estateSource(buyer,business.ownerId());
            long estateAvailable=estate==null?0:funding.estateAvailable(estate.cash());
            long estateAmount=Math.min(listing.price(),estateAvailable),cashAmount=listing.price()-estateAmount;
            if(cashAmount>0&&(source==null||funding.businessAvailable(source.cash())<cashAmount))continue;
            var operation=UUID.nameUUIDFromBytes(("civilizations:auto-business-sale-v1:"+id+":"+business.ownerId()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var q=new BusinessSales.Request(operation,id,business.ownerId(),buyer,step,cashAmount==0?null:source.id(),cashAmount,estateAmount==0?null:estate.id(),estateAmount);
            sales.sellRows(q);
            return save(id,business.ownerId(),next,step,now,"Paid automatic sale: "+listing.price()+" hundredths; "+qualified+" qualified candidates reviewed; real estate/business funds; unlimited ownership, one worker job");
        }
        return save(id,business.ownerId(),next,step,now,"For sale: "+listing.price()+" hundredths; reviewed "+candidates.size()+" residents, "+qualified+" qualified; none funded within limits; no money created");
    }
    private record Source(UUID id,long cash) {}
    private Source businessSource(UUID buyer,UUID town,UUID target)throws SQLException {
        try(var s=connection.prepareStatement("""
            SELECT b.id,s.hundredths FROM businesses b JOIN industry_stores s ON s.business_id=b.id
            WHERE b.owner_id=? AND b.settlement_id=? AND b.id<>?
            AND NOT EXISTS(SELECT 1 FROM business_closures c WHERE c.business_id=b.id)
            AND NOT EXISTS(SELECT 1 FROM business_sale_listings l WHERE l.business_id=b.id AND l.seller_id=b.owner_id AND l.status='EXPIRED')
            ORDER BY s.hundredths DESC,b.id LIMIT 1
            """)) {
            s.setString(1,buyer.toString());s.setString(2,town.toString());s.setString(3,target.toString());try(var r=s.executeQuery()){return r.next()?new Source(UUID.fromString(r.getString(1)),r.getLong(2)):null;}
        }
    }
    private Source estateSource(UUID buyer,UUID seller)throws SQLException {
        try(var s=connection.prepareStatement("""
            SELECT e.decedent_id,e.hundredths FROM family_estates e WHERE e.decedent_id<>?
            AND (EXISTS(SELECT 1 FROM family_ties f WHERE (f.first_id=? AND f.second_id=e.decedent_id) OR (f.second_id=? AND f.first_id=e.decedent_id))
                OR EXISTS(SELECT 1 FROM family_ties a JOIN family_ties b ON a.first_id=b.first_id
                    WHERE a.type='PARENT_CHILD' AND b.type='PARENT_CHILD' AND a.second_id=? AND b.second_id=e.decedent_id AND a.second_id<>b.second_id))
            ORDER BY e.hundredths DESC,e.decedent_id LIMIT 1
            """)) {
            s.setString(1,seller.toString());for(int i=2;i<=4;i++)s.setString(i,buyer.toString());try(var r=s.executeQuery()){return r.next()?new Source(UUID.fromString(r.getString(1)),r.getLong(2)):null;}
        }
    }
    private long rank(UUID business,UUID seller,UUID buyer){return UUID.nameUUIDFromBytes((seed+":"+business+":"+seller+":"+buyer).getBytes(java.nio.charset.StandardCharsets.UTF_8)).getMostSignificantBits();}
    private String cursor(UUID id,UUID seller)throws SQLException {
        try(var s=connection.prepareStatement("SELECT buyer_cursor FROM business_lifecycle_reviews WHERE business_id=? AND seller_id=?")) {
            s.setString(1,id.toString());s.setString(2,seller.toString());try(var r=s.executeQuery()){return r.next()?r.getString(1):"";}
        }
    }
    public Optional<String> explanation(UUID id)throws SQLException {
        try(var s=connection.prepareStatement("SELECT reason FROM business_lifecycle_reviews WHERE business_id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(r.getString(1)):Optional.empty();}
        }
    }
    private String save(UUID id,UUID seller,String cursor,long step,long now,String reason)throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO business_lifecycle_reviews(business_id,seller_id,buyer_cursor,step,observed_ms,seed,reason) VALUES(?,?,?,?,?,?,?) ON CONFLICT(business_id) DO UPDATE SET seller_id=excluded.seller_id,buyer_cursor=excluded.buyer_cursor,step=excluded.step,observed_ms=excluded.observed_ms,seed=excluded.seed,reason=excluded.reason")) {
            s.setString(1,id.toString());s.setString(2,seller.toString());s.setString(3,cursor);s.setLong(4,step);s.setLong(5,now);s.setLong(6,seed);s.setString(7,reason);s.executeUpdate();
        }
        return reason;
    }
}
