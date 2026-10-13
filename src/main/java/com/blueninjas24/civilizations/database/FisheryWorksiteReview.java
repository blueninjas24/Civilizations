package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** One owner transaction for exact live habitat plus safe-bank recovery, retaining legacy behavior. */
public final class FisheryWorksiteReview {
    private final Connection connection;
    public FisheryWorksiteReview(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public FisheryWorkplaces.Receipt reconcile(UUID site,ShorelineEvidence bank,FisheryHabitatObservation habitat,long now,long maximumAge,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(bank);Objects.requireNonNull(habitat);
        if(worlds==null||!worlds.contains(bank.world())||!bank.world().equals(habitat.world())||bank.capturedMillis()!=habitat.observedMillis())throw new SQLException("Current same-world/time bank and habitat proof required");
        if(!connection.getAutoCommit())throw new SQLException("Worksite review requires idle owner");connection.setAutoCommit(false);
        try {
            var banks=new FisheryWorkplaces(connection);var old=banks.find(site).orElseThrow();var fisheries=new FisheryRepository(connection);var resource=fisheries.find(site).orElseThrow();
            if(!resource.world().equals(habitat.world())||!fisheries.columns(site).equals(habitat.supports().stream().map(FisheryHabitatObservation.Support::column).toList()))throw new SQLException("Exact original habitat support coverage required even when unchanged or unknown");
            var evidence=new ShorelineEvidence(bank.world(),bank.origin(),bank.capturedMillis(),bank.assessments(),FisheryWorkplaces.hash(habitat.toString()));
            var receipt=banks.reconcileRows(site,evidence,now,maximumAge);
            if(habitat.authoritative()&&habitat.observedMillis()>=old.observedMillis()) {
                long originalCapacity;
                try(var s=connection.prepareStatement("SELECT capacity FROM fishery_resources WHERE site_id=?")){s.setString(1,site.toString());try(var r=s.executeQuery()){r.next();originalCapacity=r.getLong(1);}}
                long target=java.math.BigInteger.valueOf(originalCapacity).multiply(java.math.BigInteger.valueOf(habitat.suitableColumns())).divide(java.math.BigInteger.valueOf(habitat.supports().size())).longValueExact();
                // Stable support reads refresh the bank frame but do not grow an audit per timer tick.
                if(target!=resource.stock().capacity())fisheries.habitatRows(UUID.nameUUIDFromBytes(("civilizations:live-fish-habitat-v1:"+site+":"+habitat.observedMillis()).getBytes(StandardCharsets.UTF_8)),site,habitat);
            }
            connection.commit();connection.setAutoCommit(true);return receipt;
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
