package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import java.sql.*;
import java.util.*;

/** One public owner, with old harvested grain retained in its original ledger. Never copies balances. */
final class WorkshopPublicStock {
    private WorkshopPublicStock(){}
    static Map<Commodity,Long> read(Connection c,UUID town)throws SQLException {
        var result=new TreeMap<Commodity,Long>();
        new CommodityRepository(c).read(town).orElseThrow(()->new SQLException("Village supplies unknown")).state().stocks().forEach((g,s)->result.put(g,s.units()));
        long grain=new GrainHarvestRepository(c).stock(town);
        if(grain>0)result.merge(FarmMarketStore.GRAIN,grain,Math::addExact);
        return Collections.unmodifiableMap(result);
    }
    /** Debit dedicated harvested grain first, then any explicit generic public grain stock. */
    static void debit(Connection c,UUID town,Commodity good,long units,long expected)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Public starter stock requires owner transaction");
        if(units<1||units>expected||read(c,town).getOrDefault(good,0L)!=expected)throw new SQLException("Public starter stock changed");
        long dedicated=good.equals(FarmMarketStore.GRAIN)?new GrainHarvestRepository(c).stock(town):0;
        long genericBefore=expected-dedicated,remainder=units;
        if(good.equals(FarmMarketStore.GRAIN)) {
            long before=dedicated,taken=Math.min(before,remainder);
            if(taken>0)try(var s=c.prepareStatement("UPDATE grain_stocks SET units=? WHERE settlement_id=? AND units=?")) {
                s.setLong(1,before-taken);s.setString(2,town.toString());s.setLong(3,before);if(s.executeUpdate()!=1)throw new SQLException("Harvested starter grain changed");
            }
            remainder-=taken;
        }
        if(remainder>0)try(var s=c.prepareStatement("UPDATE economy_stocks SET units=units-? WHERE settlement_id=? AND commodity=? AND units=?")) {
            s.setLong(1,remainder);s.setString(2,town.toString());s.setString(3,good.id());s.setLong(4,genericBefore);if(s.executeUpdate()!=1)throw new SQLException("Public starter material changed");
        }
    }
}
