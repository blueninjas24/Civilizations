package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.WarehouseInvestment;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WarehouseStartupCycleTest {
    WarehouseInvestment.Preparation empty(){return new WarehouseInvestment.Preparation(Optional.empty(),"No qualified owner");}
    @Test void boundedRoundRobinDoesNotQueueOrBlockWhileSqlIsPending() {
        var calls=new ArrayList<UUID>();var pending=new CompletableFuture<WarehouseInvestment.Preparation>();
        var cycle=new WarehouseStartupCycle(town->{calls.add(town);return pending;},draft->{fail("No draft to inspect");return Optional.empty();},(d,e)->{fail("No acceptance");return null;});
        var a=new UUID(0,1);var b=new UUID(0,2);
        cycle.tick(List.of(b,a,a));for(int n=0;n<100;n++)cycle.tick(List.of(a,b));assertEquals(List.of(a),calls);
        pending.complete(empty());cycle.tick(List.of(a,b));assertTrue(cycle.diagnostic().contains("No qualified"));
        cycle.tick(List.of(a,b));cycle.tick(List.of(a,b));cycle.tick(List.of(a,b));assertEquals(List.of(a,b,a),calls);
    }
    @Test void EmptyTownSetAndRemovedCursorStillAllowLaterTowns() {
        var calls=new ArrayList<UUID>();var cycle=new WarehouseStartupCycle(town->{calls.add(town);return CompletableFuture.completedFuture(empty());},d->Optional.empty(),(d,e)->null);
        cycle.tick(List.of());assertTrue(calls.isEmpty());var a=new UUID(0,1);var b=new UUID(0,2);
        cycle.tick(List.of(b));cycle.tick(List.of());cycle.tick(List.of(a));assertEquals(List.of(b,a),calls);
    }
    @Test void unexpectedFailureIsReportedAndNextTownCanRetry() {
        var count=new int[1];var cycle=new WarehouseStartupCycle(town->++count[0]==1?CompletableFuture.failedFuture(new IllegalStateException("SQL failure")):CompletableFuture.completedFuture(empty()),d->Optional.empty(),(d,e)->null);
        var towns=List.of(UUID.randomUUID());cycle.tick(towns);assertThrows(CompletionException.class,()->cycle.tick(towns));assertTrue(cycle.diagnostic().contains("SQL failure"));
        cycle.tick(towns);cycle.tick(towns);assertEquals(2,count[0]);
    }
    @Test void excessiveDistinctTownSetFailsBeforeSubmittingAnySql() {
        var cycle=new WarehouseStartupCycle(t->{fail("Budget must precede submission");return null;},d->Optional.empty(),(d,e)->null);
        assertThrows(IllegalStateException.class,()->cycle.tick(java.util.stream.LongStream.range(0,1025).mapToObj(n->new UUID(0,n)).toList()));
    }
}

