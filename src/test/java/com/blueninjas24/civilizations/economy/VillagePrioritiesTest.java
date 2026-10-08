package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.economy.VillagePriorities.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.Condition;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VillagePrioritiesTest {
    private final UUID town=UUID.randomUUID();
    private HouseholdNeeds.Assessment facts(long period,Condition food,long unmet,Integer housing) {
        var homes=List.of(new HouseholdNeeds.Home(UUID.randomUUID(),2,0,4,housing));
        return new HouseholdNeeds.Assessment(town,period,food,unmet>0?3:0,4-unmet,4,0,homes);
    }
    @Test void impossiblePrioritiesStayBlockedRegardlessOfSeed() {
        var needs=facts(1,Condition.BALANCED,0,null);
        for(long seed=0;seed<1000;seed++) {
            var choice=VillagePriorities.review(seed,needs,Policy.prototype()); assertEquals(Action.MONITOR,choice.selected());
            assertFalse(choice.candidates().get(1).feasible());assertFalse(choice.candidates().get(2).feasible());
        }
    }
    @Test void reproducibleWeightedSelectionCanChooseBothReasonableResponsesRatherThanAlwaysMaximum() {
        var needs=facts(7,Condition.SHORTAGE,2,1);var selected=new HashSet<Action>();
        for(long seed=0;seed<500;seed++) {
            var choice=VillagePriorities.review(seed,needs,Policy.prototype());assertEquals(choice,VillagePriorities.review(seed,needs,Policy.prototype()));selected.add(choice.selected());
        }
        assertEquals(Set.of(Action.SEEK_FOOD,Action.INVESTIGATE_HOUSING),selected);
    }
    @Test void crisisFiltersLowPriorityAlternativesAndAlwaysNeedsActualUnmetFood() {
        var needs=facts(8,Condition.SEVERE_SHORTAGE,4,1);
        var choice=VillagePriorities.review(42,needs,Policy.prototype());assertEquals(Action.SEEK_FOOD,choice.selected());
        assertTrue(choice.candidates().get(2).feasible());assertFalse(choice.candidates().get(2).eligible());
        assertTrue(choice.explanation().contains("not an executed purchase"));
        assertThrows(UnsupportedOperationException.class,()->choice.candidates().clear());
    }
    @Test void reviewsAreSpacedButAppearanceEscalationRecoveryAndSettingsChangesReconsiderEarly() {
        var previous=VillagePriorities.review(42,facts(1,Condition.BALANCED,0,4),Policy.prototype());
        assertFalse(VillagePriorities.shouldReview(facts(2,Condition.ABUNDANT,0,4),Policy.prototype(),previous));
        assertTrue(VillagePriorities.shouldReview(facts(6,Condition.BALANCED,0,4),Policy.prototype(),previous));
        assertTrue(VillagePriorities.shouldReview(facts(2,Condition.SHORTAGE,1,4),Policy.prototype(),previous));
        assertTrue(VillagePriorities.shouldReview(facts(2,Condition.BALANCED,0,1),Policy.prototype(),previous));
        previous=VillagePriorities.review(42,facts(3,Condition.SHORTAGE,1,4),Policy.prototype());
        assertTrue(VillagePriorities.shouldReview(facts(4,Condition.SEVERE_SHORTAGE,4,4),Policy.prototype(),previous));
        assertTrue(VillagePriorities.shouldReview(facts(4,Condition.BALANCED,0,4),Policy.prototype(),previous));
        assertTrue(VillagePriorities.shouldReview(facts(4,Condition.SHORTAGE,1,4),new Policy(10,5,2,3,1,25),previous));
    }
    @Test void longCrisisRecoveryHistoriesAreDeterministicAndBoundedByReviewCadence() {
        assertEquals(scenario(),scenario());var history=scenario();assertTrue(history.size()<300);
        assertEquals(Action.SEEK_FOOD,history.stream().filter(d->d.period()==101).findFirst().orElseThrow().selected());
        assertEquals(Action.MONITOR,history.stream().filter(d->d.period()==701).findFirst().orElseThrow().selected());
    }
    private List<Decision> scenario() {
        var history=new ArrayList<Decision>();Decision previous=null;
        for(int period=1;period<=1200;period++) {
            boolean crisis=period>=101&&period<=700;var needs=facts(period,crisis?Condition.SEVERE_SHORTAGE:Condition.BALANCED,crisis?4:0,4);
            if(VillagePriorities.shouldReview(needs,Policy.prototype(),previous)){previous=VillagePriorities.review(42,needs,Policy.prototype());history.add(previous);}
        }
        return history;
    }
    @Test void invalidPolicyAndStaleEvidenceFailClearly() {
        assertThrows(IllegalArgumentException.class,()->new Policy(0,10,3,6,1,25));
        var previous=VillagePriorities.review(42,facts(5,Condition.BALANCED,0,4),Policy.prototype());
        assertThrows(IllegalArgumentException.class,()->VillagePriorities.shouldReview(facts(5,Condition.BALANCED,0,4),Policy.prototype(),previous));
    }
}
