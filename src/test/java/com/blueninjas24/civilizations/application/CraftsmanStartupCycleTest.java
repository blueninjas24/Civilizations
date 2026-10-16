package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.CraftsmanInvestment;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CraftsmanStartupCycleTest {
    CraftsmanInvestment.Preparation empty(){return new CraftsmanInvestment.Preparation(Optional.empty(),"No qualified owner");}
    @Test void boundedRoundRobinDoesNotQueueOrBlockWhileSqlIsPending() {
        var calls=new ArrayList<UUID>();var pending=new CompletableFuture<CraftsmanInvestment.Preparation>();
        var cycle=new CraftsmanStartupCycle(town->{calls.add(town);return pending;},draft->{fail("No draft to inspect");return Optional.empty();},(d,e)->{fail("No acceptance");return null;});
        var a=new UUID(0,1);var b=new UUID(0,2);
        cycle.tick(List.of(b,a,a));for(int n=0;n<100;n++)cycle.tick(List.of(a,b));assertEquals(List.of(a),calls);
        pending.complete(empty());cycle.tick(List.of(a,b));assertTrue(cycle.diagnostic().contains("No qualified"));
        cycle.tick(List.of(a,b));cycle.tick(List.of(a,b));cycle.tick(List.of(a,b));assertEquals(List.of(a,b,a),calls);
    }
    @Test void EmptyTownSetAndRemovedCursorStillAllowLaterTowns() {
        var calls=new ArrayList<UUID>();var cycle=new CraftsmanStartupCycle(town->{calls.add(town);return CompletableFuture.completedFuture(empty());},d->Optional.empty(),(d,e)->null);
        cycle.tick(List.of());assertTrue(calls.isEmpty());var a=new UUID(0,1);var b=new UUID(0,2);
        cycle.tick(List.of(b));cycle.tick(List.of());cycle.tick(List.of(a));assertEquals(List.of(b,a),calls);
    }
    @Test void unexpectedFailureIsReportedAndNextTownCanRetry() {
        var count=new int[1];var cycle=new CraftsmanStartupCycle(town->++count[0]==1?CompletableFuture.failedFuture(new IllegalStateException("SQL failure")):CompletableFuture.completedFuture(empty()),d->Optional.empty(),(d,e)->null);
        var towns=List.of(UUID.randomUUID());cycle.tick(towns);assertThrows(CompletionException.class,()->cycle.tick(towns));assertTrue(cycle.diagnostic().contains("SQL failure"));
        cycle.tick(towns);cycle.tick(towns);assertEquals(2,count[0]);
    }
    @Test void excessiveDistinctTownSetFailsBeforeSubmittingAnySql() {
        var cycle=new CraftsmanStartupCycle(t->{fail("Budget must precede submission");return null;},d->Optional.empty(),(d,e)->null);
        assertThrows(IllegalStateException.class,()->cycle.tick(java.util.stream.LongStream.range(0,1025).mapToObj(n->new UUID(0,n)).toList()));
    }
    CraftsmanInvestment.Draft draft() {
        var owner=UUID.randomUUID();var town=UUID.randomUUID();var world=UUID.randomUUID();
        var site=new com.blueninjas24.civilizations.industry.BusinessSite(UUID.randomUUID(),town,world,0,64,0,1000,"Cycle fixture");
        var firm=new com.blueninjas24.civilizations.industry.Business(UUID.randomUUID(),town,"Cycle craft",owner,owner,"craftsman",site.id(),2,0);
        var requirements=new com.blueninjas24.civilizations.population.JobRequirements(216,Map.of());
        var founding=new com.blueninjas24.civilizations.industry.BusinessFounding(firm,site,requirements,requirements,500,"Cycle fixture");
        var request=new com.blueninjas24.civilizations.database.BusinessStartupGrants.WorkshopRequest(founding,100,Map.of(com.blueninjas24.civilizations.database.StorageUpgrades.FITTINGS,1L),4000,owner);
        var supplies=new com.blueninjas24.civilizations.database.WorkshopStarterSupplies.Request(UUID.randomUUID(),firm.id(),com.blueninjas24.civilizations.industry.ProductionRecipe.storageFittings(),1,0,1,128);
        var anchor=new com.blueninjas24.civilizations.database.WorkplaceOperations.Anchor(firm.id(),world,new com.blueninjas24.civilizations.settlement.VillageObservation.Position(0,64,0),com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.CRAFTING_TABLE);
        return new CraftsmanInvestment.Draft(request,supplies,anchor);
    }
    @Test void unloadedOrMissingPointCannotSubmitAnAcceptance() {
        var d=draft();var observations=new int[1];var accepted=new int[1];
        var cycle=new CraftsmanStartupCycle(t->CompletableFuture.completedFuture(new CraftsmanInvestment.Preparation(Optional.of(d),"Ready")),proposal->{observations[0]++;return Optional.empty();},(proposal,evidence)->{accepted[0]++;return CompletableFuture.completedFuture(null);});
        var towns=List.of(UUID.randomUUID());cycle.tick(towns);cycle.tick(towns);assertEquals(1,observations[0]);assertEquals(0,accepted[0]);
        var missing=new CraftsmanStartupCycle(t->CompletableFuture.completedFuture(new CraftsmanInvestment.Preparation(Optional.of(d),"Ready")),proposal->Optional.of(new com.blueninjas24.civilizations.database.WorkplaceOperations.Confirmation(proposal.anchor(),false,1000)),(proposal,evidence)->{fail("Missing point must never be accepted");return null;});
        missing.tick(towns);missing.tick(towns);
    }
    @Test void oneAcceptanceInFlightPreventsRepeatedInspectionAndSpending() {
        var d=draft();var inspections=new int[1];var payments=new int[1];var pending=new CompletableFuture<Void>();
        var cycle=new CraftsmanStartupCycle(t->CompletableFuture.completedFuture(new CraftsmanInvestment.Preparation(Optional.of(d),"Ready")),proposal->{inspections[0]++;return Optional.of(new com.blueninjas24.civilizations.database.WorkplaceOperations.Confirmation(proposal.anchor(),true,1000));},(proposal,evidence)->{payments[0]++;return pending;});
        var towns=List.of(UUID.randomUUID());cycle.tick(towns);cycle.tick(towns);for(int n=0;n<100;n++)cycle.tick(towns);assertEquals(1,inspections[0]);assertEquals(1,payments[0]);
        pending.complete(null);cycle.tick(towns);assertTrue(cycle.diagnostic().contains("started"));assertEquals(1,payments[0]);
    }

}
