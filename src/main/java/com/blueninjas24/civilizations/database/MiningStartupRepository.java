package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Atomic trusted startup seam; automatic policy must establish suitability, demand and freshness first. */
public final class MiningStartupRepository {
    public record Request(BusinessFounding founding,MineralObservation evidence,long batchesPerOre,
                          Map<Commodity,Long> bundle,long storageCapacity,Map<Commodity,Long> spacePerUnit,long workingCapital,UUID worker) {
        public Request {
            Objects.requireNonNull(founding);Objects.requireNonNull(evidence);Objects.requireNonNull(worker);
            bundle=Map.copyOf(bundle);spacePerUnit=Map.copyOf(spacePerUnit);
            String kind=founding.business().industry();
            boolean matching=kind.equals("mining")&&Set.of(Commodity.COAL,Commodity.IRON).contains(evidence.mineral())
                ||kind.equals("quarry")&&evidence.mineral().equals(Commodity.STONE)
                ||kind.equals("sandpit")&&evidence.mineral().equals(Commodity.SAND);
            if(!matching || !spacePerUnit.keySet().containsAll(bundle.keySet()))
                throw new IllegalArgumentException("Matching extraction industry/resource and storage for every product required");
        }
    }
    public record Started(BusinessFounding founding,MineralDiscoveryRepository.Discovery discovery,
                          IndustryRepository.Store store,Employment firstEmployment) {}
    private final Connection connection;
    public MiningStartupRepository(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public Started start(Request request) throws SQLException {
        Objects.requireNonNull(request);
        if(!connection.getAutoCommit())throw new SQLException("Mine startup requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var result=startRows(request);
            connection.commit();connection.setAutoCommit(true);return result;
        } catch(SQLException|RuntimeException failure) {
            try {connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;
        }
    }
    /** Composition with checked grant accounting; caller owns the complete transaction. */
    Started startRows(Request request)throws SQLException {
            if(connection.getAutoCommit())throw new SQLException("Mine startup rows require owner transaction");
            var businesses=new BusinessRepository(connection);var industry=new IndustryRepository(connection);
            var founding=businesses.foundRows(request.founding());var business=founding.business();
            var discovery=new MineralDiscoveryRepository(connection).discoverRows(business.siteId(),request.evidence(),request.batchesPerOre(),request.bundle());
            var store=industry.openStoreRows(business.id(),request.storageCapacity(),request.spacePerUnit(),request.workingCapital());
            var hire=UUID.nameUUIDFromBytes(("civilizations:mine-first-worker-v1:"+business.id()).getBytes(StandardCharsets.UTF_8));
            var employment=businesses.hireRows(hire,business.id(),request.worker(),business.foundedStep());
            return new Started(founding,discovery,store,employment);
    }
}
