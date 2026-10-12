package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Read-only first mineral-chain warehouse prospects; never invents stock, a miner or a customer. */
public final class WarehouseOpportunities {
    public record Opportunity(UUID town,WorkplaceOpportunities.Candidate workplace,Map<Commodity,Long> missing,
                              List<MineralOpportunities.Evidence> ore,long purchaseHundredths) {
        public Opportunity {missing=Collections.unmodifiableMap(new TreeMap<>(missing));ore=List.copyOf(ore);}
    }
    public record Review(List<Opportunity> candidates,String explanation){public Review{candidates=List.copyOf(candidates);}}
    private final Connection connection;
    private final WarehouseOpportunityPolicy policy;
    private final IndustryRuntimePolicy industry;
    private final BusinessOperationPolicy operations;
    private final StartupGrantPolicy grants;
    private final MineFoundingPolicy mines;
    private final BooleanSupplier mutationHealthy;
    public WarehouseOpportunities(Connection connection,WarehouseOpportunityPolicy policy,IndustryRuntimePolicy industry,
            BusinessOperationPolicy operations,StartupGrantPolicy grants,MineFoundingPolicy mines,BooleanSupplier mutationHealthy) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.industry=Objects.requireNonNull(industry);
        this.operations=Objects.requireNonNull(operations);this.grants=Objects.requireNonNull(grants);this.mines=Objects.requireNonNull(mines);this.mutationHealthy=Objects.requireNonNull(mutationHealthy);
    }
    public Review review(UUID town,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(town);if(now<0)throw new IllegalArgumentException("Invalid warehouse review time");
        if(!connection.getAutoCommit())throw new SQLException("Warehouse review requires idle SQL owner");connection.setAutoCommit(false);
        try{var result=reviewRows(town,now,worlds==null?Set.of():Set.copyOf(worlds));connection.commit();return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
        finally{connection.setAutoCommit(true);}
    }
    Review reviewRows(UUID town,long now,Set<UUID> worlds)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Warehouse opportunity requires owner transaction");
        if(!policy.enabled()||!industry.enabled()||!industry.supply().enabled()||!industry.warehouseSupply().enabled()||!mines.enabled())return no("First mineral-chain warehouse review disabled");
        if(!mutationHealthy.getAsBoolean())return no("Warehouse mineral prospect waits for trusted ore mutation capture");
        var markers=new WorkplaceOpportunities(connection).review(town,now,policy.evidenceLifetimeMillis(),worlds,policy.markersPerReview());
        if(markers.candidates().isEmpty())return no(markers.explanation());
        try(var s=connection.prepareStatement("SELECT 1 FROM businesses WHERE settlement_id=? AND industry='warehouse' LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())return no("Existing warehouse identity retained; first-firm prospect cannot bypass vacancy, closure or competition");}
        }
        var demand=new LocalIndustryDemand(connection,industry,operations,mutationHealthy).review(town,worlds,64);
        if(!demand.complete()||demand.uncovered().isEmpty())return no("Warehouse waits for actual funded workshop input gaps: "+demand.explanation());
        if(!mines.mineralYield().keySet().containsAll(demand.uncovered().keySet()))return no("First mineral-chain review cannot certify unsupported input sources");
        var funding=new BusinessStartupGrants(connection).previewRows(town,"warehouse",policy.startupHundredths(),policy.capitalHundredths(),grants);
        if(!funding.eligible())return no("Warehouse protected funding unavailable: "+funding.reason());
        var mineFunding=new BusinessStartupGrants(connection).previewRows(town,"mining",mines.startupHundredths(),mines.capitalHundredths(),grants);
        if(!mineFunding.eligible())return no("Future mineral supplier funding unavailable: "+mineFunding.reason());
        // Each missing mineral needs its own prospective supplier. Do not assume future public income.
        long startup=Math.addExact(Math.addExact(policy.startupHundredths(),policy.capitalHundredths()),
                Math.multiplyExact(Math.addExact(mines.startupHundredths(),mines.capitalHundredths()),demand.uncovered().size()));
        long spent;
        try(var s=connection.prepareStatement("SELECT COALESCE(SUM(startup_hundredths+capital_hundredths),0) FROM startup_grants WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){spent=r.getLong(1);}
        }
        if(spent>grants.lifetimeLimit()||startup>grants.lifetimeLimit()-spent)return no("Complete warehouse/mineral chain exceeds remaining lifetime grant allowance");
        long cash=new TreasuryRepository(connection).load(town).orElseThrow().hundredths(),keep=Math.max(funding.protectedHundredths(),mineFunding.protectedHundredths());
        if(cash<keep||startup>cash-keep)return no("Complete warehouse/mineral chain would spend protected food money; no future public income assumed");
        long cost=0,space=0;
        for(var e:demand.uncovered().entrySet()) {
            var price=industry.purchasingSupply().unitHundredths().get(e.getKey());var weight=industry.mining().storageSpacePerUnit().get(e.getKey());
            if(price==null||weight==null)return no("Warehouse prospect lacks configured commodity price/storage");
            cost=Math.addExact(cost,Math.multiplyExact(price,e.getValue()));space=Math.addExact(space,Math.multiplyExact(weight,e.getValue()));
            long batchSpace=0;for(var output:mines.bundle(e.getKey(),industry.mining()).entrySet()) {
                var size=industry.mining().storageSpacePerUnit().get(output.getKey());if(size==null)return no("Future mine storage catalog lacks extraction outputs");
                batchSpace=Math.addExact(batchSpace,Math.multiplyExact(size,output.getValue()));
            }
            if(batchSpace>industry.mining().storageCapacity())return no("Future mine extraction bundle cannot fit storage");
        }
        if(cost>policy.capitalHundredths()||space>policy.capacity()||demand.uncovered().values().stream().reduce(0L,Math::addExact)>industry.mining().localHaulUnitsPerPeriod())
            return no("Complete actual workshop gap exceeds warehouse capital, storage or local haul budget");
        var survey=new MineralOpportunities(connection).review(town,now,mines.surveyLifetimeMillis(),worlds);
        if(survey.status()!=MineralOpportunities.Status.CURRENT)return no("Warehouse prospect waits for current ore: "+survey.explanation());
        Position center;
        try(var s=connection.prepareStatement("SELECT center_x,center_y,center_z FROM settlements WHERE id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Unknown warehouse settlement");center=new Position(r.getInt(1),r.getInt(2),r.getInt(3));}
        }
        var ore=new ArrayList<MineralOpportunities.Evidence>();
        for(var evidence:survey.candidates())if(demand.uncovered().containsKey(evidence.mineral())&&within(center,evidence.position(),mines.horizontalRange(),mines.verticalRange())) {
            var p=evidence.position();try(var s=connection.prepareStatement("SELECT 1 FROM business_sites WHERE world_id=? AND x=? AND y=? AND z=? LIMIT 1")) {
                s.setString(1,evidence.world().toString());s.setInt(2,p.x());s.setInt(3,p.y());s.setInt(4,p.z());try(var r=s.executeQuery()){if(!r.next())ore.add(evidence);}
            }
        }
        var types=new HashSet<Commodity>();ore.forEach(e->types.add(e.mineral()));
        if(!types.containsAll(demand.uncovered().keySet()))return no("Not every actual workshop input has current suitable unclaimed ore; no speculative warehouse selected");
        for(var e:demand.uncovered().entrySet()) {
            var coordinates=ore.stream().filter(source->source.mineral().equals(e.getKey())).map(MineralOpportunities.Evidence::position).toList();
            long blocks=OreClusters.split(new MineralObservation(survey.world(),survey.capturedMillis(),true,e.getKey(),coordinates))
                    .stream().mapToLong(patch->patch.ore().size()).max().orElse(0);
            long potential=Math.multiplyExact(Math.multiplyExact(blocks,industry.mining().depositBatchesPerObservedOre()),mines.mineralYield().get(e.getKey()));
            if(potential<e.getValue())return no("Observed finite mineral potential cannot cover complete initial workshop gap");
        }
        var candidates=new ArrayList<Opportunity>();
        for(var marker:markers.candidates())if(marker.kind()==WorkplaceObservation.Kind.BARREL&&within(center,marker.position(),policy.horizontalRange(),policy.verticalRange()))
            candidates.add(new Opportunity(town,marker,demand.uncovered(),ore,cost));
        if(!mutationHealthy.getAsBoolean())return no("Ore mutation trust changed during warehouse review");
        return new Review(candidates,candidates.isEmpty()?"No suitable unclaimed nearby barrel":"Read-only first warehouse prospects for fully fundable workshop gaps and observed mineral sources; no stock, grant or miner reserved; qualifications/current-block acceptance and later supplier startup still required");
    }
    private static boolean within(Position center,Position p,int horizontal,int vertical) {
        long x=(long)p.x()-center.x(),z=(long)p.z()-center.z();return Math.abs(x)<=horizontal&&Math.abs(z)<=horizontal&&x*x+z*z<=(long)horizontal*horizontal&&Math.abs((long)p.y()-center.y())<=vertical;
    }
    private static Review no(String reason){return new Review(List.of(),reason);}
}
