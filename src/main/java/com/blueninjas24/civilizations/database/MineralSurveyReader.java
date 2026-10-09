package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Read-only SQL-owner validation of provisional evidence; never establishes reserves or businesses. */
public final class MineralSurveyReader {
    public record Patch(MineralObservation evidence,UUID savedDeposit,String explanation) {}
    private final Connection connection;
    public MineralSurveyReader(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public List<Patch> read(UUID town,List<MineralObservation> observations) throws SQLException {
        observations=List.copyOf(observations);Objects.requireNonNull(town);
        if(observations.size()!=2 || !observations.get(0).mineral().equals(com.blueninjas24.civilizations.economy.Commodity.COAL)
                || !observations.get(1).mineral().equals(com.blueninjas24.civilizations.economy.Commodity.IRON))
            throw new IllegalArgumentException("One bounded coal/iron survey pair required");
        var first=observations.getFirst();var second=observations.getLast();
        if(!first.complete() || !second.complete() || !first.world().equals(second.world())
                || first.observedMillis()!=second.observedMillis() || first.ore().size()+second.ore().size()>512)
            throw new SQLException("Complete consistent bounded mineral evidence required");
        var coordinates=new HashSet<Position>(first.ore());
        for(var p:second.ore())if(!coordinates.add(p))throw new SQLException("Survey assigns two minerals to one coordinate");
        if(new OreTrackingRepository(connection).load().map(s->!s.trusted()).orElse(false))
            throw new SQLException("Ore tracking gap; survey paused");
        try(var s=connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()) {
                if(!r.next() || !r.getString(1).equals(first.world().toString()))throw new SQLException("Survey must match saved settlement world");
            }
        }
        var placements=new OrePlacementRepository(connection);var discoveries=new MineralDiscoveryRepository(connection);
        var result=new ArrayList<Patch>();
        for(var observation:observations) {
            var admitted=new ArrayList<Position>();
            for(var p:observation.ore())if(!placements.excludes(first.world(),p))admitted.add(p);
            for(var patch:OreClusters.split(new MineralObservation(first.world(),first.observedMillis(),true,observation.mineral(),admitted))) {
                var matches=new TreeSet<UUID>();
                for(var p:patch.ore())discoveries.findAt(first.world(),p).ifPresent(d->matches.add(d.id()));
                if(matches.size()>1)throw new SQLException("Observed patch spans saved deposits; no automatic merge");
                if(!matches.isEmpty())try(var s=connection.prepareStatement("SELECT mineral FROM mineral_discovery WHERE deposit_id=?")) {
                    s.setString(1,matches.first().toString());try(var r=s.executeQuery()) {
                        if(!r.next() || !r.getString(1).equals(patch.mineral().id()))throw new SQLException("Observed mineral conflicts with saved deposit identity");
                    }
                }
                result.add(new Patch(patch,matches.isEmpty()?null:matches.first(),matches.isEmpty()
                        ?"Unclaimed exposed evidence; funding, staffing and site checks still required"
                        :"Saved deposit identity retained; no new reserves or extraction terms"));
            }
        }
        return List.copyOf(result);
    }
}
