package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.BusinessPurchasePolicy;
import com.blueninjas24.civilizations.population.Resident;
import java.sql.*;
import java.util.*;

/** SQL-owner paid acquisition. Actual business/estate money moves atomically with ownership. */
public final class BusinessSales {
    public record Request(UUID operation,UUID business,UUID seller,UUID buyer,long step,
                          UUID cashSource,long cashAmount,UUID estateSource,long estateAmount) {
        public Request {
            Objects.requireNonNull(operation);Objects.requireNonNull(business);
            Objects.requireNonNull(seller);Objects.requireNonNull(buyer);
            if(step<0||seller.equals(buyer)||cashAmount<0||estateAmount<0
                    ||(cashSource==null)!=(cashAmount==0)||(estateSource==null)!=(estateAmount==0)
                    ||business.equals(cashSource)||Math.addExact(cashAmount,estateAmount)<1)
                throw new IllegalArgumentException("Invalid paid business sale terms");
        }
        public long price(){return Math.addExact(cashAmount,estateAmount);}
    }
    public record Entry(String sourceKind,UUID source,String direction,long before,long after) {}
    private final Connection connection;
    private final BusinessPurchasePolicy policy;
    private final BusinessRepository businesses;
    private final PopulationRepository population;
    private final BusinessSuccession succession;

    public BusinessSales(Connection connection,BusinessPurchasePolicy policy)throws SQLException {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);
        businesses=new BusinessRepository(connection);population=new PopulationRepository(connection);
        succession=new BusinessSuccession(connection);
    }
    public Request sell(Request request)throws SQLException {
        Objects.requireNonNull(request);
        if(!connection.getAutoCommit())throw new SQLException("Business sale requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var old=find(request.operation());
            if(old.isPresent()) {
                if(!old.orElseThrow().equals(request))throw new SQLException("Business sale operation reused with different terms");
            } else sellRows(request);
            connection.commit();connection.setAutoCommit(true);return request;
        }catch(SQLException|RuntimeException failure) {
            try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}
            throw failure;
        }
    }
    private void sellRows(Request q)throws SQLException {
        var target=businesses.findBusiness(q.business()).orElseThrow(()->new SQLException("Unknown sale business"));
        var seller=population.findResident(q.seller()).orElseThrow(()->new SQLException("Unknown seller"));
        if(!target.ownerId().equals(q.seller())||seller.status()!=Resident.Status.DECEASED)
            throw new SQLException("Sale requires the current deceased owner");
        if(q.step()<target.foundedStep()||q.step()<lastTransferStep(q.business()))throw new SQLException("Sale predates ownership history");
        if(succession.candidate(q.business()).isPresent())throw new SQLException("Qualified successor takes priority over resale");
        if(!succession.qualifiedBuyer(q.buyer(),q.business()))throw new SQLException("Buyer is not eligible for this industry");
        long beforeBusiness=0,beforeEstate=0;
        if(q.cashSource()!=null) {
            var payer=businesses.findBusiness(q.cashSource()).orElseThrow(()->new SQLException("Unknown buyer business"));
            if(!payer.ownerId().equals(q.buyer())||!payer.settlementId().equals(target.settlementId()))
                throw new SQLException("Buyer does not own a local funding business");
            beforeBusiness=new IndustryRepository(connection).store(q.cashSource()).orElseThrow(()->new SQLException("Buyer business has no cash account")).hundredths();
            if(q.cashAmount()>policy.businessAvailable(beforeBusiness))throw new SQLException("Business funding limit or retained cash prevents purchase");
        }
        if(q.estateSource()!=null) {
            if(!familyMember(q.buyer(),q.estateSource()))throw new SQLException("Buyer cannot spend an unrelated family estate");
            beforeEstate=estate(q.estateSource()).orElseThrow(()->new SQLException("Buyer estate does not exist"));
            if(q.estateAmount()>policy.estateAvailable(beforeEstate))throw new SQLException("Estate funding limit or balance prevents purchase");
        }
        long sellerBefore=estate(q.seller()).orElse(0L);
        long sellerAfter=Math.addExact(sellerBefore,q.price());
        if(q.seller().equals(q.estateSource()))throw new SQLException("Sale cannot pay itself from its proceeds estate");
        try(var s=connection.prepareStatement("INSERT INTO family_estates(decedent_id,hundredths) VALUES(?,0) ON CONFLICT(decedent_id) DO NOTHING")) {
            s.setString(1,q.seller().toString());s.executeUpdate();
        }
        try(var s=connection.prepareStatement("INSERT INTO business_sales VALUES(?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,q.operation().toString());s.setString(2,q.business().toString());s.setString(3,q.seller().toString());
            s.setString(4,q.buyer().toString());s.setLong(5,q.step());
            nullable(s,6,q.cashSource());s.setLong(7,q.cashAmount());nullable(s,8,q.estateSource());s.setLong(9,q.estateAmount());s.executeUpdate();
        }
        if(q.cashSource()!=null) {
            long after=beforeBusiness-q.cashAmount();
            updateBalance("industry_stores","business_id",q.cashSource(),beforeBusiness,after);
            entry(q.operation(),"BUSINESS",q.cashSource(),"PAYMENT",beforeBusiness,after);
        }
        if(q.estateSource()!=null) {
            long after=beforeEstate-q.estateAmount();
            updateBalance("family_estates","decedent_id",q.estateSource(),beforeEstate,after);
            entry(q.operation(),"ESTATE",q.estateSource(),"PAYMENT",beforeEstate,after);
        }
        updateBalance("family_estates","decedent_id",q.seller(),sellerBefore,sellerAfter);
        entry(q.operation(),"ESTATE",q.seller(),"PROCEEDS",sellerBefore,sellerAfter);
        try(var s=connection.prepareStatement("UPDATE businesses SET owner_id=? WHERE id=? AND owner_id=?")) {
            s.setString(1,q.buyer().toString());s.setString(2,q.business().toString());s.setString(3,q.seller().toString());
            if(s.executeUpdate()!=1)throw new SQLException("Stale sale owner");
        }
    }
    private long lastTransferStep(UUID business)throws SQLException {
        try(var s=connection.prepareStatement("SELECT MAX(observed_step) FROM (SELECT observed_step FROM business_ownership WHERE business_id=? UNION ALL SELECT observed_step FROM business_sales WHERE business_id=?)")) {
            s.setString(1,business.toString());s.setString(2,business.toString());try(var r=s.executeQuery()){r.next();return r.getLong(1);}
        }
    }
    private boolean familyMember(UUID buyer,UUID root)throws SQLException {
        for(var tie:population.findFamilyTies(root))if(tie.first().equals(buyer)||tie.second().equals(buyer))return true;
        return population.findSiblings(root).contains(buyer);
    }
    private void updateBalance(String table,String key,UUID id,long before,long after)throws SQLException {
        // Identifiers are fixed private call sites, never player/config input.
        try(var s=connection.prepareStatement("UPDATE "+table+" SET hundredths=? WHERE "+key+"=? AND hundredths=?")) {
            s.setLong(1,after);s.setString(2,id.toString());s.setLong(3,before);
            if(s.executeUpdate()!=1)throw new SQLException("Stale business sale funds");
        }
    }
    private void entry(UUID op,String kind,UUID source,String direction,long before,long after)throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO business_sale_entries VALUES(?,?,?,?,?,?)")) {
            s.setString(1,op.toString());s.setString(2,kind);s.setString(3,source.toString());s.setString(4,direction);
            s.setLong(5,before);s.setLong(6,after);s.executeUpdate();
        }
    }
    private static void nullable(PreparedStatement s,int index,UUID id)throws SQLException {
        if(id==null)s.setNull(index,Types.VARCHAR);else s.setString(index,id.toString());
    }
    private static UUID uuid(String value){return value==null?null:UUID.fromString(value);}
    public Optional<Long> estate(UUID decedent)throws SQLException {
        try(var s=connection.prepareStatement("SELECT hundredths FROM family_estates WHERE decedent_id=?")) {
            s.setString(1,decedent.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(r.getLong(1)):Optional.empty();}
        }
    }
    public Optional<Request> find(UUID operation)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM business_sales WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                return r.next()?Optional.of(new Request(operation,uuid(r.getString("business_id")),uuid(r.getString("previous_owner")),uuid(r.getString("next_owner")),r.getLong("observed_step"),
                        uuid(r.getString("cash_source")),r.getLong("cash_amount"),uuid(r.getString("estate_source")),r.getLong("estate_amount"))):Optional.empty();
            }
        }
    }
    public List<Entry> entries(UUID operation)throws SQLException {
        var result=new ArrayList<Entry>();
        try(var s=connection.prepareStatement("SELECT * FROM business_sale_entries WHERE operation_id=? ORDER BY source_kind,source_id,direction")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                while(r.next())result.add(new Entry(r.getString("source_kind"),uuid(r.getString("source_id")),r.getString("direction"),r.getLong("before_cash"),r.getLong("after_cash")));
            }
        }
        return List.copyOf(result);
    }
}
