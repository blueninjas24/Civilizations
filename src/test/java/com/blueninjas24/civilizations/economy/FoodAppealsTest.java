package com.blueninjas24.civilizations.economy;

import org.junit.jupiter.api.Test;
import java.util.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import static org.junit.jupiter.api.Assertions.*;

class FoodAppealsTest {
    private final UUID town=UUID.randomUUID();
    private FoodAppeals.Snapshot observe(long period,long demand,long supplied,VillagePriorities.Action action,FoodAppeals.Snapshot prior) {
        var result=CommodityAccounting.step(new State(period-1,Map.of(Commodity.FOOD,new Stock(0,0))),
                Map.of(Commodity.FOOD,new Flow(0,supplied,demand,0,0,0,0)),new Policy(5000,12500,20000,3));
        var food=result.reports().getFirst();
        var needs=new HouseholdNeeds.Assessment(town,period,food.condition(),food.after().shortagePeriods(),food.fulfilled(),demand,1,List.of());
        var priority=new VillagePriorities.Decision(town,period,1,VillagePriorities.Policy.prototype(),food.condition(),food.unmet(),0,action,List.of());
        return FoodAppeals.observe(needs,priority,food,prior);
    }
    @Test void appealsRequireActualUnmetFoodAndSelectedPriority() {
        assertEquals(FoodAppeals.Status.OPEN,observe(1,5,0,VillagePriorities.Action.SEEK_FOOD,null).status());
        assertEquals(FoodAppeals.Status.NONE,observe(1,5,5,VillagePriorities.Action.SEEK_FOOD,null).status());
        assertEquals(FoodAppeals.Status.NONE,observe(1,5,0,VillagePriorities.Action.MONITOR,null).status());
    }
    @Test void changesDistinguishImprovementWorseningAndUnchangedPressure() {
        var first=observe(1,5,0,VillagePriorities.Action.SEEK_FOOD,null);
        assertEquals(FoodAppeals.Change.OPENED,first.change());
        var improved=observe(2,5,3,VillagePriorities.Action.SEEK_FOOD,first);
        assertEquals(FoodAppeals.Change.IMPROVED,improved.change());assertEquals(5L,improved.previousUnmet());assertEquals(3,improved.imports());
        var unchanged=observe(3,5,3,VillagePriorities.Action.SEEK_FOOD,improved);
        assertEquals(FoodAppeals.Change.UNCHANGED,unchanged.change());
        assertEquals(FoodAppeals.Change.WORSENED,observe(4,5,0,VillagePriorities.Action.SEEK_FOOD,unchanged).change());
        assertTrue(improved.explanation().contains("does not prove"));
    }
    @Test void recoveryClosesAnAppealAndLaterCrisisOpensANewOne() {
        var open=observe(1,5,0,VillagePriorities.Action.SEEK_FOOD,null);
        var closed=observe(2,5,8,VillagePriorities.Action.MONITOR,open);
        assertEquals(FoodAppeals.Status.RESOLVED,closed.status());assertEquals(0,closed.unmet());
        var quiet=observe(3,5,5,VillagePriorities.Action.MONITOR,closed);assertEquals(FoodAppeals.Status.NONE,quiet.status());
        assertEquals(FoodAppeals.Change.OPENED,observe(4,5,0,VillagePriorities.Action.SEEK_FOOD,quiet).change());
    }
    @Test void priorityChangesWithdrawAnAppealWithoutClaimingShortageEnded() {
        var open=observe(1,5,0,VillagePriorities.Action.SEEK_FOOD,null);
        var withdrawn=observe(2,5,0,VillagePriorities.Action.INVESTIGATE_HOUSING,open);
        assertEquals(FoodAppeals.Status.WITHDRAWN,withdrawn.status());assertEquals(5,withdrawn.unmet());
    }
    @Test void mismatchedHistoryAndInvalidSavedEvidenceAreRejected() {
        var first=observe(1,5,0,VillagePriorities.Action.SEEK_FOOD,null);
        assertThrows(IllegalArgumentException.class,()->observe(3,5,0,VillagePriorities.Action.SEEK_FOOD,first));
        var wrong=new FoodAppeals.Snapshot(UUID.randomUUID(),1,1,FoodAppeals.Status.OPEN,5,null,0,0,FoodAppeals.Change.OPENED);
        assertThrows(IllegalArgumentException.class,()->observe(2,5,0,VillagePriorities.Action.SEEK_FOOD,wrong));
        assertThrows(IllegalArgumentException.class,()->new FoodAppeals.Snapshot(town,1,2,FoodAppeals.Status.OPEN,5,null,0,0,FoodAppeals.Change.OPENED));
        assertThrows(IllegalArgumentException.class,()->new FoodAppeals.Snapshot(town,1,1,FoodAppeals.Status.OPEN,0,null,0,0,FoodAppeals.Change.OPENED));
    }
    @Test void longRepeatedCrisisRecoveryHistoryIsDeterministic() {
        assertEquals(history(),history());
    }
    private List<FoodAppeals.Snapshot> history() {
        var history=new ArrayList<FoodAppeals.Snapshot>();
        for(int period=1;period<=1200;period++) {
            boolean crisis=period%50<25;
            history.add(observe(period,5,crisis?0:8,crisis?VillagePriorities.Action.SEEK_FOOD:VillagePriorities.Action.MONITOR,history.isEmpty()?null:history.getLast()));
        }
        assertTrue(history.stream().anyMatch(s->s.status()==FoodAppeals.Status.RESOLVED));return history;
    }
    @Test void localProductionCanResolveAnAppealWithoutPretendingADonationArrived() {
        var first=observe(1,5,0,VillagePriorities.Action.SEEK_FOOD,null);
        var result=CommodityAccounting.step(new State(1,Map.of(Commodity.FOOD,new Stock(0,1))),Map.of(Commodity.FOOD,new Flow(5,0,5,0,0,0,0)),new Policy(5000,12500,20000,3));
        var report=result.reports().getFirst();
        var needs=new HouseholdNeeds.Assessment(town,2,report.condition(),0,5,5,1,List.of());
        var priority=VillagePriorities.review(1,needs,VillagePriorities.Policy.prototype());
        var recovered=FoodAppeals.observe(needs,priority,report,first);
        assertEquals(FoodAppeals.Status.RESOLVED,recovered.status());assertEquals(5,recovered.production());assertEquals(0,recovered.imports());
    }
    @Test void conflictingReportsAndInventedOutcomeLabelsFailExplicitly() {
        var result=CommodityAccounting.step(new State(0,Map.of(Commodity.FOOD,new Stock(0,0))),Map.of(Commodity.FOOD,new Flow(0,0,5,0,0,0,0)),new Policy(5000,12500,20000,3));
        var report=result.reports().getFirst();
        var needs=new HouseholdNeeds.Assessment(town,1,report.condition(),1,0,6,1,List.of());
        var priority=VillagePriorities.review(1,needs,VillagePriorities.Policy.prototype());
        assertThrows(IllegalArgumentException.class,()->FoodAppeals.observe(needs,priority,report,null));
        assertThrows(IllegalArgumentException.class,()->new FoodAppeals.Snapshot(town,1,1,FoodAppeals.Status.OPEN,5,2L,0,0,FoodAppeals.Change.IMPROVED));
        assertThrows(IllegalArgumentException.class,()->new FoodAppeals.Snapshot(town,1,1,FoodAppeals.Status.NONE,0,null,0,0,FoodAppeals.Change.RESOLVED));
    }

}
