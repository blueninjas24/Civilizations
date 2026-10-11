package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.UpkeepContributionPolicy;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.UUID;

/** Only composed with a verified, actual public farm invoice; no independent cash collection. */
final class FarmUpkeepContributions {
    private final Connection connection;
    FarmUpkeepContributions(Connection connection){this.connection=connection;}
    void fundRows(FarmToolPurchases.Request request,UUID town,long needed,UpkeepContributionPolicy policy)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Upkeep contribution requires invoice transaction");
        UUID seller=request.seller();
        var industry=new IndustryRepository(connection);long before=industry.store(seller).orElseThrow(()->new FarmToolPurchases.Declined("Supplier cash account missing")).hundredths();
        var opening=new TreasuryRepository(connection).entry(id("civilizations:business-capital:"+seller));
        long capital=opening.map(TreasuryRepository.Entry::amount).orElse(0L);
        long sales=total("SELECT COALESCE(SUM(price),0) FROM local_shipments WHERE seller_id=?",seller,null);
        long contributed=total("SELECT COALESCE(SUM(amount),0) FROM upkeep_contributions WHERE business_id=?",seller,null);
        long businessSpent=total("SELECT COALESCE(SUM(amount),0) FROM upkeep_contributions WHERE business_id=? AND period=?",seller,request.foodPeriod());
        long villageSpent=total("SELECT COALESCE(SUM(amount),0) FROM upkeep_contributions WHERE settlement_id=? AND period=?",town,request.foodPeriod());
        if(needed<1||policy.available(before,capital,sales,contributed,businessSpent,villageSpent)<needed)
            throw new FarmToolPurchases.Declined("Supplier cannot fund upkeep without exceeding earned cash, operating reserve or contribution limits",policy.enabled());
        long keep=Math.addExact(capital,policy.operatingReserve());
        UUID operation=id("civilizations:farm-upkeep-contribution-v1:"+request.operation());
        long treasuryBefore=new TreasuryRepository(connection).load(town).orElseThrow().hundredths();
        try(var s=connection.prepareStatement("UPDATE industry_stores SET hundredths=? WHERE business_id=? AND hundredths=?")) {
            s.setLong(1,before-needed);s.setString(2,seller.toString());s.setLong(3,before);
            if(s.executeUpdate()!=1)throw new SQLException("Supplier cash changed during upkeep contribution");
        }
        var credit=new TreasuryRepository(connection).postRows(operation,town,needed,"Earned spare supplier cash for actual public farm toolkit invoice");
        try(var s=connection.prepareStatement("INSERT INTO upkeep_contributions VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,operation.toString());s.setString(2,request.operation().toString());s.setString(3,seller.toString());s.setString(4,town.toString());
            s.setLong(5,request.foodPeriod());s.setLong(6,needed);s.setLong(7,keep);s.setLong(8,before);s.setLong(9,before-needed);
            s.setLong(10,treasuryBefore);s.setLong(11,credit.balanceAfter());s.setString(12,policy.toString());s.executeUpdate();
        }
    }
    private long total(String sql,UUID id,Long period)throws SQLException {
        try(var s=connection.prepareStatement(sql)){s.setString(1,id.toString());if(period!=null)s.setLong(2,period);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}
    }
    private static UUID id(String value){return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));}
}
