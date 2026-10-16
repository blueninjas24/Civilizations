package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** First general craftsman prospects from actual current upgrade customers. No reservations or free assets. */
public final class CraftsmanOpportunities {
    public record Opportunity(UUID town,WorkplaceOpportunities.Candidate workplace,ProductionRecipe recipe,
            WorkshopStarterPlan.Review starter,long fundedFittings) {}
    public record Review(List<Opportunity> candidates,String explanation){public Review{candidates=List.copyOf(candidates);}}
    private final Connection c;private final WorkshopOpportunityPolicy policy;private final IndustryRuntimePolicy industry;
    private final StartupGrantPolicy grants;private final WorkshopStarterPolicy starter;private final BusinessOperationPolicy operations;
    public CraftsmanOpportunities(Connection c,WorkshopOpportunityPolicy policy,IndustryRuntimePolicy industry,
            StartupGrantPolicy grants,WorkshopStarterPolicy starter,BusinessOperationPolicy operations) {
        this.c=Objects.requireNonNull(c);this.policy=Objects.requireNonNull(policy);this.industry=Objects.requireNonNull(industry);
        this.grants=Objects.requireNonNull(grants);this.starter=Objects.requireNonNull(starter);this.operations=Objects.requireNonNull(operations);
    }
    public Review review(UUID town,long now,Set<UUID> worlds)throws SQLException {
        if(!c.getAutoCommit())throw new SQLException("Craftsman review requires idle SQL owner");c.setAutoCommit(false);
        try{var r=reviewRows(town,now,Set.copyOf(worlds));c.commit();return r;}
        catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
    Review reviewRows(UUID town,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(town);if(now<0)throw new IllegalArgumentException("Negative craftsman evidence time");
        if(c.getAutoCommit())throw new SQLException("Craftsman rows require owner transaction");
        if(!policy.enabled()||!industry.enabled()||!industry.supply().enabled()||!industry.storageUpgrades().enabled()||!starter.enabled())return no("Craftsman investment, processing, input supply, storage customers or starter disabled");
        var markers=new WorkplaceOpportunities(c).review(town,now,policy.evidenceLifetimeMillis(),worlds,policy.markersPerReview(),Set.of(WorkplaceObservation.Kind.CRAFTING_TABLE));
        if(markers.candidates().isEmpty())return no(markers.explanation());
        try(var s=c.prepareStatement("SELECT 1 FROM businesses WHERE settlement_id=? AND industry='craftsman' LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())return no("Existing craftsman identity retained; no duplicate first firm around closure/vacancy");}
        }
        var recipe=industry.recipes().values().stream().filter(r->r.industry().equals("craftsman")&&r.outputs().keySet().equals(Set.of(StorageUpgrades.FITTINGS))).sorted(Comparator.comparing(ProductionRecipe::id)).findFirst();
        if(recipe.isEmpty())return no("Configured craftsman fitting recipe required");
        var funding=new BusinessStartupGrants(c).previewRows(town,"craftsman",policy.startupHundredths(),policy.capitalHundredths(),grants);
        if(!funding.eligible())return no("Protected craftsman startup funding unavailable: "+funding.reason());
        var demand=new StorageUpgradeDemand(c,industry.storageUpgrades(),operations).review(town,worlds,64,Map.of());
        if(!demand.complete())return no(demand.explanation());
        long wanted=0;for(var need:demand.needs())wanted=Math.addExact(wanted,need.units());
        var supplied=availableFittings(town,markers.candidates().getFirst().world(),worlds);
        if(supplied.isEmpty())return no("Fitting supplier review cap exceeded; no partial shortage inferred");
        long uncovered=Math.max(0,wanted-supplied.orElseThrow());var selected=recipe.orElseThrow();
        if(uncovered<selected.outputs().get(StorageUpgrades.FITTINGS))return no("No uncovered current funded storage-fitting customer for one batch");
        var weights=industry.mining().storageSpacePerUnit();var weight=weights.get(StorageUpgrades.FITTINGS);
        if(weight==null||selected.outputs().get(StorageUpgrades.FITTINGS)>policy.capacity()/weight)return no("Craftsman output batch cannot fit configured storage");
        var bundle=WorkshopStarterPlan.review(selected,1,starter,policy.capitalHundredths(),policy.capacity(),industry.mining().localHaulUnitsPerPeriod(),WorkshopPublicStock.read(c,town),weights);
        if(!bundle.eligible())return no("Paid public-surplus starter unavailable: "+bundle.explanation());
        Position center;try(var s=c.prepareStatement("SELECT center_x,center_y,center_z FROM settlements WHERE id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Missing craftsman settlement");center=new Position(r.getInt(1),r.getInt(2),r.getInt(3));}
        }
        var found=new ArrayList<Opportunity>();for(var marker:markers.candidates()) {
            var p=marker.position();long dx=(long)p.x()-center.x(),dz=(long)p.z()-center.z();
            if(Math.abs(dx)<=policy.horizontalRange()&&Math.abs(dz)<=policy.horizontalRange()&&dx*dx+dz*dz<=(long)policy.horizontalRange()*policy.horizontalRange()&&Math.abs((long)p.y()-center.y())<=policy.verticalRange())found.add(new Opportunity(town,marker,selected,bundle,uncovered));
        }
        return new Review(found,found.isEmpty()?"No nearby available crafting table":"Real current funded fitting pressure, paid protected starter and feasible marker; forecast only, no guaranteed sale or free skill");
    }
    private OptionalLong availableFittings(UUID town,UUID world,Set<UUID> worlds)throws SQLException {
        var ids=new ArrayList<UUID>();try(var s=c.prepareStatement("SELECT b.id FROM businesses b JOIN business_sites p ON p.id=b.site_id JOIN industry_stock k ON k.business_id=b.id WHERE b.settlement_id=? AND p.world_id=? AND b.industry IN('craftsman','warehouse') AND k.commodity='storage_fittings' AND k.units>0 ORDER BY b.id LIMIT 65")) {
            s.setString(1,town.toString());s.setString(2,world.toString());try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
        }
        if(ids.size()>64)return OptionalLong.empty();long total=0;var firms=new BusinessRepository(c);var people=new PopulationRepository(c);
        for(var id:ids) {
            if(!worlds.contains(world)||!new BusinessOperations(c,operations).evaluate(id).permitted())continue;
            var founding=firms.findFounding(id).orElseThrow();boolean staffed=false;
            for(var job:firms.currentWorkers(id)){var person=people.findResident(job.residentId()).orElseThrow();if(person.settlementId().equals(town)&&WorkEligibility.evaluate(person,people.findWorkProfile(person.id()).orElseThrow(),founding.workerRequirements()).eligible()){staffed=true;break;}}
            if(staffed)total=Math.addExact(total,new IndustryRepository(c).store(id).orElseThrow().units().get(StorageUpgrades.FITTINGS));
        }
        return OptionalLong.of(total);
    }
    private static Review no(String why){return new Review(List.of(),why);}
}
