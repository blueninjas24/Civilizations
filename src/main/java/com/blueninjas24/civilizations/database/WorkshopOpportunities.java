package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Current first-workshop investment evidence. Read-only; no fabricated employer or reserved grant. */
public final class WorkshopOpportunities {
    public record Opportunity(UUID town,WorkplaceOpportunities.Candidate workplace,ProductionRecipe recipe,
                              WorkshopStarterPlan.Review starter,UUID customer,long fundedToolNeed) {}
    public record Review(List<Opportunity> candidates,String explanation) {public Review {candidates=List.copyOf(candidates);}}
    private final Connection connection;
    private final WorkshopOpportunityPolicy policy;
    private final IndustryRuntimePolicy industry;
    private final BusinessOperationPolicy operations;
    private final StartupGrantPolicy grants;
    private final WorkshopStarterPolicy starter;
    private final FarmToolUpkeepPolicy farmUpkeep;
    private final FarmToolBudgetPolicy farmBudget;
    public WorkshopOpportunities(Connection connection,WorkshopOpportunityPolicy policy,IndustryRuntimePolicy industry,
            BusinessOperationPolicy operations,StartupGrantPolicy grants,WorkshopStarterPolicy starter,
            FarmToolUpkeepPolicy farmUpkeep,FarmToolBudgetPolicy farmBudget) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.industry=Objects.requireNonNull(industry);
        this.operations=Objects.requireNonNull(operations);this.grants=Objects.requireNonNull(grants);this.starter=Objects.requireNonNull(starter);
        this.farmUpkeep=Objects.requireNonNull(farmUpkeep);this.farmBudget=Objects.requireNonNull(farmBudget);
    }
    public Review review(UUID town,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(town);if(now<0)throw new IllegalArgumentException("Invalid workshop observation time");
        if(!connection.getAutoCommit())throw new SQLException("Workshop review requires idle SQL owner");
        connection.setAutoCommit(false);
        try {var result=reviewRows(town,now,worlds==null?Set.of():Set.copyOf(worlds));connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    Review reviewRows(UUID town,long now,Set<UUID> worlds)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Workshop review rows require owner transaction");
        if(!policy.enabled()||!industry.enabled()||!industry.supply().enabled()||!starter.enabled())return no("First-workshop investment review disabled");
        var workplaces=new WorkplaceOpportunities(connection).review(town,now,policy.evidenceLifetimeMillis(),worlds,policy.markersPerReview(),Set.of(WorkplaceObservation.Kind.SMITHING_TABLE));
        if(workplaces.candidates().isEmpty())return no(workplaces.explanation());
        // First-firm bootstrap only. Existing/closed firms require lifecycle/reuse/competition review.
        try(var s=connection.prepareStatement("SELECT 1 FROM businesses WHERE settlement_id=? AND industry='blacksmith' LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())return no("Existing blacksmith identity retained; first-firm review cannot bypass operation, vacancy or competition checks");}
        }
        var recipe=industry.recipes().values().stream().filter(r->r.industry().equals("blacksmith")&&r.outputs().containsKey(ToolUpkeepPolicy.TOOLS)).findFirst();
        if(recipe.isEmpty())return no("Configured blacksmith toolkit recipe required");
        var customer=new FarmEquipmentDemand(connection,farmUpkeep,farmBudget,operations).review(town,worlds);
        if(customer.units()==0)return no("Workshop waits for actual funded farm equipment need: "+customer.explanation());
        var pressure=new LocalIndustryDemand(connection,industry,operations).withFarmTools(farmUpkeep,farmBudget).review(town,worlds,64);
        if(!pressure.complete()||pressure.uncovered().getOrDefault(ToolUpkeepPolicy.TOOLS,0L)==0)return no("Existing local supply or incomplete demand review: "+pressure.explanation());
        var funding=new BusinessStartupGrants(connection).previewRows(town,"blacksmith",policy.startupHundredths(),policy.capitalHundredths(),grants);
        if(!funding.eligible())return no("Workshop startup funding unavailable: "+funding.reason());
        var stock=WorkshopPublicStock.read(connection,town);
        var sizes=industry.mining().storageSpacePerUnit();var selected=recipe.orElseThrow();long outputSpace=0;
        try {
            for(var output:selected.outputs().entrySet()) {
                var weight=sizes.get(output.getKey());if(weight==null)return no("Configured workshop storage lacks recipe output");
                outputSpace=Math.addExact(outputSpace,Math.multiplyExact(weight,output.getValue()));
            }
        }catch(ArithmeticException overflow){return no("Workshop output storage exceeds supported range");}
        if(outputSpace>policy.capacity())return no("One recipe output cannot fit configured workshop storage");
        var bundle=WorkshopStarterPlan.review(selected,1,starter,policy.capitalHundredths(),policy.capacity(),industry.mining().localHaulUnitsPerPeriod(),stock,sizes);
        if(!bundle.eligible())return no("Workshop starter bundle unavailable: "+bundle.explanation());
        Position center;
        try(var s=connection.prepareStatement("SELECT center_x,center_y,center_z FROM settlements WHERE id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Unknown workshop settlement");center=new Position(r.getInt(1),r.getInt(2),r.getInt(3));}
        }
        var candidates=new ArrayList<Opportunity>();
        for(var marker:workplaces.candidates()) {
            if(marker.kind()!=WorkplaceObservation.Kind.SMITHING_TABLE)continue;
            var p=marker.position();long dx=(long)p.x()-center.x(),dz=(long)p.z()-center.z();
            if(Math.abs(dx)>policy.horizontalRange()||Math.abs(dz)>policy.horizontalRange()
                ||dx*dx+dz*dz>(long)policy.horizontalRange()*policy.horizontalRange()||Math.abs((long)p.y()-center.y())>policy.verticalRange())continue;
            candidates.add(new Opportunity(town,marker,selected,bundle,customer.farm(),customer.units()));
        }
        return new Review(candidates,candidates.isEmpty()?"No suitable nearby unclaimed smithing marker in bounded review"
            :"Fundable first workshop candidates with actual tool customer and paid starter surplus; qualified founder and fresh block acceptance still required; no assets changed");
    }
    private static Review no(String reason){return new Review(List.of(),reason);}
}
