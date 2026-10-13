package com.blueninjas24.civilizations.industry;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockHerdTest {
    final LivestockPolicy policy=LivestockPolicy.prototype();
    LivestockHerd.State initial(long capacity){return new LivestockHerd.State(0,2,capacity,0,List.of());}
    LivestockHerd.Plan step(LivestockHerd.State state,long feed,long work,long room,long slaughter){return LivestockHerd.plan(state,feed,work,room,slaughter,policy,LivestockHerd.Species.COW,true);}
    @Test void birthCostsRealExtraFeedAndLaborAndProducesYoungNotImmediateAdults() {
        var p=step(initial(10),6,4,100,0);assertEquals(1,p.births());assertEquals(6,p.feedUsed());assertEquals(4,p.workUsed());assertEquals(2,p.after().adults());assertEquals(List.of(new LivestockHerd.Young(13,1)),p.after().young());assertEquals(7,p.after().nextBirthPeriod());
        assertEquals(0,step(initial(10),5,4,100,0).births());assertEquals(0,step(initial(10),6,3,100,0).births());
    }
    @Test void fullCapacityOrMissingStorageDoesNotInventBirthsAndCapacityLossKeepsAnimals() {
        assertEquals(0,step(initial(2),100,100,100,0).births());assertEquals(0,step(initial(10),100,100,0,0).births());
        var over=step(initial(0),100,100,100,0);assertEquals(2,over.after().heads());assertEquals(0,over.births());
    }
    @Test void breedingCooldownAndMaturityAdvanceOnlyWithActualActivePeriods() {
        var p=step(initial(10),100,100,100,0);var saved=p.after();
        assertEquals(saved,LivestockHerd.plan(saved,100,100,100,0,policy,LivestockHerd.Species.COW,false).after());
        assertEquals(saved,LivestockHerd.plan(saved,100,100,100,0,LivestockPolicy.disabled(),LivestockHerd.Species.COW,true).after());
        for(int n=2;n<=6;n++){p=step(p.after(),100,100,100,0);assertEquals(0,p.births());}
        p=step(p.after(),100,100,100,0);assertEquals(1,p.births());
        for(int n=8;n<=13;n++)p=step(p.after(),100,100,100,0);assertEquals(1,p.matured());assertEquals(3,p.after().adults());
    }
    @Test void slaughterCannotConsumeYoungOrTheAdultBreedingReserve() {
        var state=new LivestockHerd.State(0,3,10,0,List.of(new LivestockHerd.Young(10,2)));var p=step(state,0,100,100,100);
        assertEquals(1,p.slaughtered());assertEquals(2,p.after().adults());assertEquals(state.young(),p.after().young());assertEquals(4,p.meatUnits());
    }
    @Test void longRunConservesHeadsBudgetsAndSpaceAndReplaysExactly() {
        var state=initial(30);long births=0,slaughter=0;
        for(int n=0;n<2400;n++) {
            var p=step(state,100,100,100,n%7==0?1:0);assertEquals(p,step(state,100,100,100,n%7==0?1:0));
            assertEquals(state.heads()+p.births()-p.slaughtered(),p.after().heads());assertTrue(p.after().heads()<=30);
            assertTrue(p.feedUsed()<=100&&p.workUsed()<=100);assertTrue(p.renewableUnits()+p.meatUnits()<=100);
            births+=p.births();slaughter+=p.slaughtered();state=p.after();
        }
        assertEquals(2+births-slaughter,state.heads());assertTrue(births>100);assertTrue(slaughter>100);
    }
    @Test void invalidOverdueCohortsAndExhaustedPeriodCounterRefuseWithoutMutatingState() {
        assertThrows(IllegalArgumentException.class,()->new LivestockHerd.State(5,2,10,0,List.of(new LivestockHerd.Young(5,1))));
        var state=new LivestockHerd.State(Long.MAX_VALUE,2,10,0,List.of());assertThrows(ArithmeticException.class,()->step(state,100,100,100,0));assertEquals(Long.MAX_VALUE,state.period());
    }
}
