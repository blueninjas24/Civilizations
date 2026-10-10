package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.Resident;
import java.sql.*;
import java.util.*;

/** Retained unsold closure. Stops new work without deleting assets, cargo, premises or history. */
public final class BusinessClosure {
    public record Closure(UUID business,UUID seller,long step,String reason) {}
    private final Connection connection;
    public BusinessClosure(Connection connection){this.connection=Objects.requireNonNull(connection);}

    /** Called only by the present-world owner refresh, after qualified inheritance. */
    public Optional<Closure> closeIfExpired(UUID id,long step)throws SQLException {
        Objects.requireNonNull(id);if(step<0)throw new IllegalArgumentException("Negative closure step");
        if(!connection.getAutoCommit())throw new SQLException("Closure requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var saved=find(id);Optional<Closure> result=saved;
            if(saved.isEmpty())result=closeRows(id,step);
            connection.commit();connection.setAutoCommit(true);return result;
        }catch(SQLException|RuntimeException failure) {
            try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;
        }
    }
    private Optional<Closure> closeRows(UUID id,long step)throws SQLException {
        var firms=new BusinessRepository(connection);var business=firms.findBusiness(id).orElseThrow(()->new SQLException("Unknown closing business"));
        var offer=new BusinessSaleListings(connection).find(id,business.ownerId());
        if(offer.isEmpty()||offer.orElseThrow().status()!=BusinessSaleListings.Status.EXPIRED)return Optional.empty();
        if(new PopulationRepository(connection).findResident(business.ownerId()).orElseThrow().status()!=Resident.Status.DECEASED
                ||new BusinessSuccession(connection).candidate(id).isPresent())return Optional.empty();
        if(step<business.foundedStep()||step<offer.orElseThrow().createdStep())throw new SQLException("Closure predates business sale history");
        String reason="Unsold after saved online sale deadline; assets and premises retained";
        try(var s=connection.prepareStatement("INSERT INTO business_closures(business_id,seller_id,observed_step,reason) VALUES(?,?,?,?)")) {
            s.setString(1,id.toString());s.setString(2,business.ownerId().toString());s.setLong(3,step);s.setString(4,reason);s.executeUpdate();
        }
        // CHECK constraints reject a future job start; closure and all separations roll back together.
        try(var s=connection.prepareStatement("UPDATE business_employment SET ended_step=?,end_reason=? WHERE business_id=? AND ended_step IS NULL")) {
            s.setLong(1,step);s.setString(2,reason);s.setString(3,id.toString());s.executeUpdate();
        }
        return find(id);
    }
    public Optional<Closure> find(UUID id)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM business_closures WHERE business_id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(new Closure(id,UUID.fromString(r.getString("seller_id")),r.getLong("observed_step"),r.getString("reason"))):Optional.empty();}
        }
    }
    static void requireOpen(Connection connection,UUID id)throws SQLException {
        if(blocked(connection,id))throw new BusinessSales.Declined("Business closed or unsold deadline expired; assets/history retained");
    }
    /** Expired firms stop immediately, even before their bounded closure refresh visits them. */
    static boolean blocked(Connection connection,UUID id)throws SQLException {
        try(var s=connection.prepareStatement("SELECT 1 FROM business_closures WHERE business_id=? UNION ALL SELECT 1 FROM business_sale_listings l JOIN businesses b ON b.id=l.business_id WHERE b.id=? AND l.seller_id=b.owner_id AND l.status='EXPIRED' LIMIT 1")) {
            s.setString(1,id.toString());s.setString(2,id.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
}
