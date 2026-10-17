package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockWorkingAreaTest {
    final UUID world=new UUID(0,1);final Position home=new Position(0,64,0);
    @Test void inclusiveThreeDimensionalRadiusAndForeignWorldAreExplicit() {
        var area=new LivestockWorkingArea(5);assertTrue(area.contains(world,home,world,new Position(3,68,0)));
        assertFalse(area.contains(world,home,world,new Position(3,68,1)));assertFalse(area.contains(world,home,UUID.randomUUID(),home));
        assertThrows(IllegalArgumentException.class,()->new LivestockWorkingArea(0));assertThrows(IllegalArgumentException.class,()->new LivestockWorkingArea(257));
        assertFalse(new LivestockWorkingArea(256).contains(world,new Position(Integer.MIN_VALUE,0,Integer.MIN_VALUE),world,new Position(Integer.MAX_VALUE,0,Integer.MAX_VALUE)));
    }
    @Test void awayAdultsKeepHeadsAndCapacityButDoNotMakeProductsOrBreed() {
        var before=new LivestockHerd.State(0,2,8,0,List.of());
        var plan=LivestockHerd.plan(before,100,100,100,0,LivestockPolicy.prototype(),LivestockHerd.Species.COW,true,1);
        assertEquals(2,plan.after().adults());assertEquals(2,plan.after().heads());assertEquals(1,plan.renewableUnits());assertEquals(1,plan.feedUsed());assertEquals(0,plan.births());
        var none=LivestockHerd.plan(before,100,100,100,100,LivestockPolicy.prototype(),LivestockHerd.Species.COW,true,2);
        assertEquals(2,none.after().heads());assertEquals(0,none.feedUsed());assertEquals(0,none.slaughtered());
    }
    @Test void youngAnimalsStillMatureWhileAwayAndReturnCanRestoreBreeding() {
        var before=new LivestockHerd.State(0,1,8,0,List.of(new LivestockHerd.Young(1,1)));
        var away=LivestockHerd.plan(before,100,100,100,0,LivestockPolicy.prototype(),LivestockHerd.Species.CHICKEN,true,1);
        assertEquals(1,away.matured());assertEquals(2,away.after().adults());assertEquals(1,away.renewableUnits());assertEquals(0,away.births());
        var back=LivestockHerd.plan(away.after(),100,100,100,0,LivestockPolicy.prototype(),LivestockHerd.Species.CHICKEN,true,0);
        assertEquals(2,back.renewableUnits());assertEquals(1,back.births());assertEquals(3,back.after().heads());
        assertEquals(before,LivestockHerd.plan(before,100,100,100,0,LivestockPolicy.prototype(),LivestockHerd.Species.COW,false,1).after());
    }
    @Test void unavailableCountsCannotSilentlyRemoveOrInventAnimals() {
        var before=new LivestockHerd.State(0,2,8,0,List.of());
        var invalidBefore=before;
        assertThrows(IllegalArgumentException.class,()->LivestockHerd.plan(invalidBefore,100,100,100,0,LivestockPolicy.prototype(),LivestockHerd.Species.COW,true,3));
        for(int n=0;n<240;n++) {
            var p=LivestockHerd.plan(before,100,100,100,0,LivestockPolicy.prototype(),LivestockHerd.Species.COW,true,2);
            assertEquals(2,p.after().heads());assertEquals(0,p.renewableUnits());assertEquals(0,p.births());before=p.after();
        }
    }
}
