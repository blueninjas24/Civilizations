package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;

/** Bounded current business customers, independent of whether a fittings supplier already exists. */
public final class StorageUpgradeDemand {
    public record Review(boolean complete,List<StorageUpgrades.Need> needs,String explanation){public Review{needs=List.copyOf(needs);}}
    private final Connection c;private final StorageUpgradePolicy policy;private final BusinessOperationPolicy operations;
    public StorageUpgradeDemand(Connection c,StorageUpgradePolicy policy,BusinessOperationPolicy operations){this.c=Objects.requireNonNull(c);this.policy=Objects.requireNonNull(policy);this.operations=Objects.requireNonNull(operations);}
    public Review review(UUID town,Set<UUID> worlds,int limit,Map<UUID,Long> proposedDebits)throws SQLException {
        if(!c.getAutoCommit())return rows(town,worlds,limit,proposedDebits);
        c.setAutoCommit(false);try{var r=rows(town,worlds,limit,proposedDebits);c.commit();return r;}
        catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
    private Review rows(UUID town,Set<UUID> worlds,int limit,Map<UUID,Long> debits)throws SQLException {
        Objects.requireNonNull(town);worlds=Set.copyOf(worlds);debits=Map.copyOf(debits);
        if(limit<1||limit>128||debits.size()>128||debits.values().stream().anyMatch(x->x<0))throw new IllegalArgumentException("Invalid storage customer budget");
        if(!policy.enabled())return new Review(true,List.of(),"New storage customers disabled");
        record Clock(UUID business,long period,long cadence){}var clocks=new ArrayList<Clock>();
        try(var s=c.prepareStatement("SELECT b.id,r.completed,r.cadence FROM businesses b JOIN industry_runtime r ON r.business_id=b.id WHERE b.settlement_id=? ORDER BY b.id LIMIT ?")) {
            s.setString(1,town.toString());s.setInt(2,limit+1);try(var r=s.executeQuery()){while(r.next())clocks.add(new Clock(UUID.fromString(r.getString(1)),r.getLong(2),r.getLong(3)));}
        }
        if(clocks.size()>limit)return new Review(false,List.of(),"Storage customer review cap exceeded; no partial demand inferred");
        var needs=new ArrayList<StorageUpgrades.Need>();var upgrades=new StorageUpgrades(c,policy,operations);
        for(var clock:clocks)upgrades.needRows(clock.business(),clock.period(),clock.cadence(),worlds,debits.getOrDefault(clock.business(),0L)).ifPresent(needs::add);
        return new Review(true,needs,"Real present-world staffed storage pressure, current clocks, operating cash and saved upgrade limits; planning only");
    }
}
