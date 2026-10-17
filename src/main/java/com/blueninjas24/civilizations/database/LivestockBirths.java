package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.LivestockBirth;
import java.sql.*;
import java.util.*;

/** SQL-owner logical newborn registry. No physical spawn, ownership claim, death or legacy backfill inferred. */
public final class LivestockBirths {
    private final Connection connection;
    public LivestockBirths(Connection connection){this.connection=Objects.requireNonNull(connection);}
    static void createSchema(Connection connection)throws SQLException {
        try(var s=connection.createStatement()) {
            s.execute("""
                CREATE TABLE livestock_births (
                    id TEXT PRIMARY KEY NOT NULL CHECK(length(id)=36),
                    business_id TEXT NOT NULL REFERENCES livestock_herds(business_id),
                    birth_period INTEGER NOT NULL CHECK(typeof(birth_period)='integer' AND birth_period>0),
                    ordinal INTEGER NOT NULL CHECK(typeof(ordinal)='integer' AND ordinal BETWEEN 0 AND 999),
                    mature_period INTEGER NOT NULL CHECK(typeof(mature_period)='integer' AND mature_period>birth_period AND mature_period-birth_period<=1000),
                    UNIQUE(business_id,birth_period,ordinal),
                    FOREIGN KEY(business_id,birth_period) REFERENCES livestock_periods(business_id,period)
                )
                """);
        }
    }
    /** Only inside the same new-production transaction, after its actual birth receipt is inserted. */
    List<LivestockBirth> recordRows(UUID business,long period,int count,int maturity)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Newborn identities require the production owner transaction");
        var expected=LivestockBirth.batch(business,period,count,maturity);
        try(var s=connection.prepareStatement("SELECT births FROM livestock_periods WHERE business_id=? AND period=?")) {
            s.setString(1,business.toString());s.setLong(2,period);
            try(var r=s.executeQuery()){if(!r.next()||r.getLong(1)!=count)throw new SQLException("Newborn identities must match the saved production receipt");}
        }
        var old=period(business,period);
        if(!old.isEmpty()) {
            if(!old.equals(expected))throw new SQLException("Saved newborn identities or maturity changed");return old;
        }
        for(var birth:expected)try(var s=connection.prepareStatement("INSERT INTO livestock_births VALUES(?,?,?,?,?)")) {
            s.setString(1,birth.id().toString());s.setString(2,business.toString());s.setLong(3,period);s.setInt(4,birth.ordinal());s.setLong(5,birth.maturePeriod());s.executeUpdate();
        }
        return expected;
    }
    public Optional<LivestockBirth> find(UUID id)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM livestock_births WHERE id=?")) {
            s.setString(1,Objects.requireNonNull(id).toString());try(var r=s.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}
        }
    }
    /** Stable bounded chronological page; (0,-1) begins a scan. It never certifies physical presence. */
    public List<LivestockBirth> page(UUID business,long afterPeriod,int afterOrdinal,int limit)throws SQLException {
        Objects.requireNonNull(business);
        if(afterPeriod<0||afterOrdinal< -1||afterOrdinal>=1000||limit<1||limit>256)throw new IllegalArgumentException("Invalid newborn page");
        try(var s=connection.prepareStatement("SELECT * FROM livestock_births WHERE business_id=? AND (birth_period>? OR (birth_period=? AND ordinal>?)) ORDER BY birth_period,ordinal LIMIT ?")) {
            s.setString(1,business.toString());s.setLong(2,afterPeriod);s.setLong(3,afterPeriod);s.setInt(4,afterOrdinal);s.setInt(5,limit);
            var result=new ArrayList<LivestockBirth>();try(var r=s.executeQuery()){while(r.next())result.add(read(r));}return List.copyOf(result);
        }
    }
    private List<LivestockBirth> period(UUID business,long period)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM livestock_births WHERE business_id=? AND birth_period=? ORDER BY ordinal")) {
            s.setString(1,business.toString());s.setLong(2,period);var result=new ArrayList<LivestockBirth>();
            try(var r=s.executeQuery()){while(r.next())result.add(read(r));}return List.copyOf(result);
        }
    }
    /** Count coverage only, not physical presence or full row validation. Consumers must also read the
     * validated identities. Old anonymous cohorts remain unmapped; never materialize guessed animals. */
    public long unmappedBirths(UUID business)throws SQLException {
        try(var s=connection.prepareStatement("SELECT 1 FROM livestock_herds WHERE business_id=?")) {
            s.setString(1,Objects.requireNonNull(business).toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Saved herd required for birth coverage");}
        }
        try(var s=connection.prepareStatement("""
            SELECT p.period,p.births,COUNT(b.id) AS recorded FROM livestock_periods p
            LEFT JOIN livestock_births b ON b.business_id=p.business_id AND b.birth_period=p.period
            WHERE p.business_id=? GROUP BY p.period,p.births HAVING COUNT(b.id)<>p.births
            """)) {
            s.setString(1,business.toString());long missing=0;
            try(var r=s.executeQuery()){while(r.next()) {
                long gap=Math.subtractExact(r.getLong("births"),r.getLong("recorded"));
                if(gap<0)throw new SQLException("Newborn identities exceed saved birth count");missing=Math.addExact(missing,gap);
            }}return missing;
        }
    }
    private static LivestockBirth read(ResultSet r)throws SQLException {
        try{return new LivestockBirth(UUID.fromString(r.getString("id")),UUID.fromString(r.getString("business_id")),r.getLong("birth_period"),r.getInt("ordinal"),r.getLong("mature_period"));}
        catch(IllegalArgumentException corrupt){throw new SQLException("Saved newborn identity is inconsistent",corrupt);}
    }
}
