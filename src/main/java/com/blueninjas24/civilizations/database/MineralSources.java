package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.SourceDepletion;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Saved coordinate shares; adopts existing totals without changing extraction history or goods. */
public final class MineralSources {
    public record Share(UUID world,Position position,UUID deposit,long allocated,long remaining) {}
    private final Connection connection;
    public MineralSources(Connection connection){this.connection=Objects.requireNonNull(connection);}
    private void transactionRequired()throws SQLException{if(connection.getAutoCommit())throw new SQLException("Source rows require owner transaction");}
    boolean ensureRows(UUID deposit)throws SQLException {
        transactionRequired();
        long count,total;
        try(var s=connection.prepareStatement("SELECT COUNT(*) FROM mineral_evidence WHERE deposit_id=?")) {
            s.setString(1,deposit.toString());try(var r=s.executeQuery()){count=r.getLong(1);}
        }
        if(count==0)return false; // Legacy abstract deposits have no coordinates to invent.
        if(count>65536)throw new SQLException("Mineral source count exceeds owner budget");
        try(var s=connection.prepareStatement("SELECT remaining_batches FROM mineral_deposits WHERE id=?")) {
            s.setString(1,deposit.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Unknown source deposit");total=r.getLong(1);}
        }
        boolean adopted;
        try(var s=connection.prepareStatement("SELECT 1 FROM mineral_source_adoptions WHERE deposit_id=?")) {
            s.setString(1,deposit.toString());try(var r=s.executeQuery()){adopted=r.next();}
        }
        if(!adopted) {
            try(var s=connection.prepareStatement("INSERT INTO mineral_source_adoptions VALUES(?,?,?,?)")) {
                s.setString(1,deposit.toString());s.setLong(2,total);s.setLong(3,count);s.setString(4,"Even allocation of saved remaining reserve; no historical depletion invented");s.executeUpdate();
            }
        }
        if(adopted) {
            try(var s=connection.prepareStatement("INSERT INTO mineral_sources SELECT world_id,x,y,z,deposit_id,0,0 FROM mineral_evidence e WHERE deposit_id=? AND NOT EXISTS(SELECT 1 FROM mineral_sources m WHERE m.world_id=e.world_id AND m.x=e.x AND m.y=e.y AND m.z=e.z)")) {
                s.setString(1,deposit.toString());s.executeUpdate();
            }
        }else {
            try(var s=connection.prepareStatement("""
                INSERT INTO mineral_sources
                SELECT world_id,x,y,z,deposit_id,share,share FROM (
                    SELECT e.*, ? / ? + CASE WHEN ROW_NUMBER() OVER(ORDER BY world_id,x,y,z)<=? THEN 1 ELSE 0 END AS share
                    FROM mineral_evidence e WHERE deposit_id=?
                )
                """)) {
                s.setLong(1,total);s.setLong(2,count);s.setLong(3,total%count);s.setString(4,deposit.toString());s.executeUpdate();
            }
        }
        if(sum(deposit)!=total)throw new SQLException("Source shares differ from saved deposit reserve");
        return true;
    }
    public List<Share> shares(UUID deposit)throws SQLException {
        var result=new ArrayList<Share>();
        try(var s=connection.prepareStatement("SELECT world_id,x,y,z,allocated_batches,remaining_batches FROM mineral_sources WHERE deposit_id=? ORDER BY world_id,x,y,z LIMIT 65537")) {
            s.setString(1,deposit.toString());try(var r=s.executeQuery()) {
                while(r.next()){if(result.size()==65536)throw new SQLException("Mineral source read exceeds budget");result.add(new Share(UUID.fromString(r.getString(1)),new Position(r.getInt(2),r.getInt(3),r.getInt(4)),deposit,r.getLong(5),r.getLong(6)));}
            }
        }
        return List.copyOf(result);
    }
    private long sum(UUID deposit)throws SQLException {
        try(var s=connection.prepareStatement("SELECT COALESCE(SUM(remaining_batches),0) FROM mineral_sources WHERE deposit_id=?")) {
            s.setString(1,deposit.toString());try(var r=s.executeQuery()){return r.getLong(1);}
        }
    }
    void depleteRows(UUID operation,UUID deposit,long amount)throws SQLException {
        transactionRequired();var before=shares(deposit);if(before.isEmpty())return;
        long aggregate;
        try(var s=connection.prepareStatement("SELECT remaining_batches FROM mineral_deposits WHERE id=?")) {
            s.setString(1,deposit.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Missing deposit");aggregate=r.getLong(1);}
        }
        if(Math.addExact(aggregate,amount)!=sum(deposit))throw new SQLException("Extraction/source reserve mismatch");
        var after=SourceDepletion.drain(before.stream().map(Share::remaining).toList(),amount);
        for(int i=0;i<before.size();i++) {
            var share=before.get(i);if(share.remaining()==after.get(i))continue;
            update(share,after.get(i));
            try(var s=connection.prepareStatement("INSERT INTO mineral_source_extraction VALUES(?,?,?,?,?,?,?)")) {
                s.setString(1,operation.toString());coordinate(s,2,share.world(),share.position());s.setLong(6,share.remaining());s.setLong(7,after.get(i));s.executeUpdate();
            }
        }
        if(sum(deposit)!=aggregate)throw new SQLException("Incomplete source extraction");
    }
    /** Sticky immutable removals, including coordinates not yet discovered. Never delete identity or output. */
    public long remove(UUID world,Collection<Position> positions,long observedMillis,String reason)throws SQLException {
        Objects.requireNonNull(world);var frozen=List.copyOf(positions);
        if(observedMillis<0||frozen.isEmpty()||frozen.size()>512||new HashSet<>(frozen).size()!=frozen.size()||reason==null||reason.isBlank()||reason.length()>256)throw new IllegalArgumentException("Invalid bounded source removal");
        if(!connection.getAutoCommit())throw new SQLException("Removal requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            long removed=0;
            var expected=new HashMap<UUID,Long>();
            for(var position:frozen) {
                int fresh;
                try(var s=connection.prepareStatement("INSERT INTO ore_source_removals VALUES(?,?,?,?,?,?) ON CONFLICT(world_id,x,y,z) DO NOTHING")) {
                    coordinate(s,1,world,position);s.setLong(5,observedMillis);s.setString(6,reason);fresh=s.executeUpdate();
                }
                if(fresh==0)continue;
                var deposit=new MineralDiscoveryRepository(connection).findAt(world,position);
                if(deposit.isEmpty())continue;
                var saved=deposit.orElseThrow();
                if(!expected.containsKey(saved.id())) {
                    ensureRows(saved.id());
                    expected.put(saved.id(),saved.remainingBatches());
                }
                Share share;
                try(var s=connection.prepareStatement("SELECT deposit_id,allocated_batches,remaining_batches FROM mineral_sources WHERE world_id=? AND x=? AND y=? AND z=?")) {
                    coordinate(s,1,world,position);
                    try(var r=s.executeQuery()) {
                        if(!r.next())throw new SQLException("Missing mineral source coordinate");
                        share=new Share(world,position,UUID.fromString(r.getString(1)),r.getLong(2),r.getLong(3));
                    }
                }
                update(share,0);
                try(var s=connection.prepareStatement("UPDATE mineral_deposits SET remaining_batches=remaining_batches-? WHERE id=? AND remaining_batches>=?")) {
                    s.setLong(1,share.remaining());s.setString(2,share.deposit().toString());s.setLong(3,share.remaining());if(s.executeUpdate()!=1)throw new SQLException("Source retirement exceeds deposit");
                }
                try(var s=connection.prepareStatement("INSERT INTO mineral_source_retirements VALUES(?,?,?,?,?)")) {
                    coordinate(s,1,world,position);s.setLong(5,share.remaining());s.executeUpdate();
                }
                expected.put(share.deposit(),Math.subtractExact(expected.get(share.deposit()),share.remaining()));
                removed=Math.addExact(removed,share.remaining());
            }
            for(var entry:expected.entrySet())if(sum(entry.getKey())!=entry.getValue())throw new SQLException("Retirement/source mismatch");
            connection.commit();connection.setAutoCommit(true);return removed;
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private void update(Share row,long remaining)throws SQLException {
        try(var s=connection.prepareStatement("UPDATE mineral_sources SET remaining_batches=? WHERE world_id=? AND x=? AND y=? AND z=? AND remaining_batches=?")) {
            s.setLong(1,remaining);coordinate(s,2,row.world(),row.position());s.setLong(6,row.remaining());if(s.executeUpdate()!=1)throw new SQLException("Stale mineral source balance");
        }
    }
    private static void coordinate(PreparedStatement s,int start,UUID world,Position p)throws SQLException {
        s.setString(start,world.toString());s.setInt(start+1,p.x());s.setInt(start+2,p.y());s.setInt(start+3,p.z());
    }
}
