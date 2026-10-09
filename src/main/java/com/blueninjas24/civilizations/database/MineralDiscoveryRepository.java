package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.MineralObservation;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Coordinate identity only. Caller must establish trusted resource evidence and a real registered site. */
public final class MineralDiscoveryRepository {
    public record Discovery(IndustryRepository.Deposit deposit,boolean created,long addedEvidence,String explanation) {}
    private final Connection connection;
    private final IndustryRepository industry;
    public MineralDiscoveryRepository(Connection connection) throws SQLException {
        this.connection=Objects.requireNonNull(connection);industry=new IndustryRepository(connection);
    }
    public Optional<IndustryRepository.Deposit> findAt(UUID world,Position position) throws SQLException {
        try(var s=connection.prepareStatement("SELECT deposit_id FROM mineral_evidence WHERE world_id=? AND x=? AND y=? AND z=?")) {
            coordinate(s,world,position);try(var r=s.executeQuery()){return r.next()?industry.deposit(UUID.fromString(r.getString(1))):Optional.empty();}
        }
    }
    public long claimedBlocks(UUID deposit) throws SQLException {
        try(var s=connection.prepareStatement("SELECT COUNT(*) FROM mineral_evidence WHERE deposit_id=?")) {
            s.setString(1,deposit.toString());try(var r=s.executeQuery()){return r.getLong(1);}
        }
    }
    public Discovery discover(UUID site,MineralObservation observation,long batchesPerOre,Map<Commodity,Long> bundle) throws SQLException {
        Objects.requireNonNull(site);Objects.requireNonNull(observation);bundle=Map.copyOf(bundle);
        if(!observation.complete() || observation.ore().isEmpty())throw new SQLException("Complete positive ore evidence required");
        if(batchesPerOre<1 || batchesPerOre>1_000_000_000 || bundle.isEmpty() || bundle.size()>32
                || !bundle.containsKey(observation.mineral()) || bundle.values().stream().anyMatch(v->v<1 || v>1_000_000))
            throw new IllegalArgumentException("Invalid finite discovery terms");
        if(!connection.getAutoCommit())throw new SQLException("Discovery requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var result=discoverRows(site,observation,batchesPerOre,bundle);connection.commit();connection.setAutoCommit(true);return result;
        } catch(SQLException|RuntimeException failure) {
            try {connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;
        }
    }
    private Discovery discoverRows(UUID site,MineralObservation observation,long rate,Map<Commodity,Long> bundle) throws SQLException {
        var placements=new OrePlacementRepository(connection);
        for(var position:observation.ore())if(placements.excludes(observation.world(),position))
            throw new SQLException("Survey includes tracked player-placed ore; refresh resource evidence before discovery");
        try(var s=connection.prepareStatement("SELECT world_id FROM business_sites WHERE id=?")) {
            s.setString(1,site.toString());try(var r=s.executeQuery()) {
                if(!r.next() || !r.getString(1).equals(observation.world().toString()))throw new SQLException("Discovery world must match registered site");
            }
        }
        var matches=new TreeSet<UUID>();
        for(var position:observation.ore())findAt(observation.world(),position).ifPresent(d->matches.add(d.id()));
        if(matches.size()>1)throw new SQLException("Survey overlaps multiple saved deposits; no automatic merge or new reserves");
        UUID savedSiteDeposit=null;
        try(var s=connection.prepareStatement("SELECT id FROM mineral_deposits WHERE site_id=?")) {
            s.setString(1,site.toString());try(var r=s.executeQuery()){if(r.next())savedSiteDeposit=UUID.fromString(r.getString(1));}
        }
        UUID id=matches.isEmpty()?savedSiteDeposit:matches.first();
        boolean created=id==null;
        if(created) {
            var anchor=observation.ore().getFirst();
            id=UUID.nameUUIDFromBytes(("civilizations:observed-mineral-v1:"+observation.world()+":"+observation.mineral().id()+":"+
                    anchor.x()+":"+anchor.y()+":"+anchor.z()).getBytes(StandardCharsets.UTF_8));
            industry.discoverRows(id,site,observation.ore().size(),Math.multiplyExact(observation.ore().size(),rate),bundle);
            try(var s=connection.prepareStatement("INSERT INTO mineral_discovery VALUES(?,?,?,?)")) {
                s.setString(1,id.toString());s.setString(2,observation.world().toString());s.setString(3,observation.mineral().id());s.setLong(4,observation.observedMillis());s.executeUpdate();
            }
        } else {
            var saved=industry.deposit(id).orElseThrow();
            if(!saved.siteId().equals(site) || (savedSiteDeposit!=null && !savedSiteDeposit.equals(id)))
                throw new SQLException("Ore already belongs to another saved deposit/site; resolve identity before founding");
            try(var s=connection.prepareStatement("SELECT mineral,observed_millis FROM mineral_discovery WHERE deposit_id=?")) {
                s.setString(1,id.toString());try(var r=s.executeQuery()) {
                    if(!r.next())throw new SQLException("Legacy deposit lacks coordinate identity; explicit adoption required");
                    if(!r.getString(1).equals(observation.mineral().id()))throw new SQLException("Saved mineral identity cannot change");
                    if(observation.observedMillis()<r.getLong(2))throw new SQLException("Survey predates original discovery");
                }
            }
            // The registered site's first snapshot fixes reserves. A disjoint survey must not extend its claims.
            if(matches.isEmpty())throw new SQLException("Survey does not overlap saved site evidence; no assumed geological extension");
        }
        long added=0;
        for(var position:observation.ore()) {
            if(findAt(observation.world(),position).isPresent())continue;
            try(var s=connection.prepareStatement("INSERT INTO mineral_evidence(world_id,x,y,z,deposit_id,observed_millis) VALUES(?,?,?,?,?,?)")) {
                coordinate(s,observation.world(),position);s.setString(5,id.toString());s.setLong(6,observation.observedMillis());s.executeUpdate();added++;
            }
        }
        return new Discovery(industry.deposit(id).orElseThrow(),created,added,created?"Finite deposit established from unique coordinates":
                "Existing deposit retained; linked survey coordinates recorded without reserve refill or changed yields");
    }
    private static void coordinate(PreparedStatement s,UUID world,Position position) throws SQLException {
        s.setString(1,world.toString());s.setInt(2,position.x());s.setInt(3,position.y());s.setInt(4,position.z());
    }
}
