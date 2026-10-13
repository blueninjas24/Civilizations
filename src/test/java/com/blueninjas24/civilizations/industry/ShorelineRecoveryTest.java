package com.blueninjas24.civilizations.industry;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShorelineRecoveryTest {
    final ShorelineScan.Position original=new ShorelineScan.Position(-1,65,0);
    final WaterPatch.Patch habitat=new WaterPatch.Patch(new FisheryObservation(UUID.randomUUID(),100,true,List.of(new FisheryObservation.Column(0,0))),64);
    final Map<ShorelineScan.Position,ShorelineScan.Cell> cells=new HashMap<>();
    ShorelineScan.Cell cell(ShorelineScan.Position p){return cells.getOrDefault(p,ShorelineScan.Cell.AIR);}
    void safe(ShorelineScan.Position p){cells.put(new ShorelineScan.Position(p.x(),p.y()-1,p.z()),ShorelineScan.Cell.SAFE_FOOTING);}
    ShorelineRecovery recovery(int distance){return new ShorelineRecovery(this::cell,habitat,original,0,distance);}
    ShorelineRecovery.Decision finish(ShorelineRecovery r){while(!r.complete())r.advance(1);return r.decision();}
    @Test void safeOriginalStaysEvenWhenOtherBanksAreUnknown() {
        safe(original);cells.put(new ShorelineScan.Position(1,64,0),ShorelineScan.Cell.UNKNOWN);
        var result=finish(recovery(16));assertEquals(ShorelineRecovery.Action.KEEP,result.action());assertEquals(original,result.position());assertTrue(result.confirmedSafe());
    }
    @Test void accidentalFootingLossFindsDeterministicNearestSafeBankWithoutChangingHabitat() {
        safe(new ShorelineScan.Position(0,65,-1));safe(new ShorelineScan.Position(0,65,1));safe(new ShorelineScan.Position(1,65,0));
        var result=finish(recovery(16));assertEquals(ShorelineRecovery.Action.RELOCATE,result.action());assertEquals(new ShorelineScan.Position(0,65,-1),result.position());assertTrue(result.confirmedSafe());
        assertEquals(1,habitat.evidence().columns().size());assertEquals(100,habitat.evidence().observedMillis());assertEquals(result,finish(recovery(16)));
    }
    @Test void unknownOriginalDoesNotBecomeDamageOrTriggerRelocation() {
        cells.put(new ShorelineScan.Position(-1,64,0),ShorelineScan.Cell.UNKNOWN);safe(new ShorelineScan.Position(1,65,0));
        var result=finish(recovery(16));assertEquals(ShorelineRecovery.Action.WAIT_UNKNOWN,result.action());assertEquals(original,result.position());assertFalse(result.confirmedSafe());assertFalse(result.currentConfirmedUnsafe());
    }
    @Test void incompleteAlternativesWaitButKnownSafeReplacementCanStillBeUsed() {
        cells.put(new ShorelineScan.Position(0,64,1),ShorelineScan.Cell.UNKNOWN);var waiting=finish(recovery(16));assertEquals(ShorelineRecovery.Action.WAIT_UNKNOWN,waiting.action());assertTrue(waiting.currentConfirmedUnsafe());
        safe(new ShorelineScan.Position(1,65,0));assertEquals(ShorelineRecovery.Action.RELOCATE,finish(recovery(16)).action());
    }
    @Test void noReplacementRequestsRepairAndRestorationResumesTheSameWorkplace() {
        var result=finish(recovery(16));assertEquals(ShorelineRecovery.Action.NEEDS_REPAIR,result.action());assertEquals(original,result.position());assertFalse(result.confirmedSafe());
        safe(original);assertEquals(ShorelineRecovery.Action.KEEP,finish(recovery(16)).action());
        cells.put(original,ShorelineScan.Cell.OTHER);assertEquals(ShorelineRecovery.Action.NEEDS_REPAIR,finish(recovery(16)).action());
    }
    @Test void distanceAndStrictReadBudgetsBoundTheSearch() {
        safe(new ShorelineScan.Position(1,65,0));assertEquals(ShorelineRecovery.Action.NEEDS_REPAIR,finish(recovery(1)).action());assertEquals(ShorelineRecovery.Action.RELOCATE,finish(recovery(2)).action());
        var reads=new int[1];var r=new ShorelineRecovery(p->{reads[0]++;return cell(p);},habitat,original,0,16);
        assertThrows(IllegalStateException.class,r::decision);
        while(!r.complete()){int before=reads[0];int used=r.advance(2);assertTrue(used<=2);assertEquals(used,reads[0]-before);}
        assertEquals(12,reads[0]);assertThrows(IllegalArgumentException.class,()->r.advance(0));
    }
    @Test void foreignStandingPointsAndOverflowCannotEscapeOriginalFootprint() {
        assertThrows(IllegalArgumentException.class,()->new ShorelineRecovery(this::cell,habitat,new ShorelineScan.Position(10,65,10),0,16));
        assertThrows(IllegalArgumentException.class,()->recovery(257));
        var edge=new WaterPatch.Patch(new FisheryObservation(UUID.randomUUID(),100,true,List.of(new FisheryObservation.Column(Integer.MIN_VALUE,0))),64);
        var current=new ShorelineScan.Position(Integer.MIN_VALUE,65,-1);var other=new ShorelineScan.Position(Integer.MIN_VALUE+1,65,0);
        var r=new ShorelineRecovery(p->p.equals(new ShorelineScan.Position(other.x(),64,other.z()))?ShorelineScan.Cell.SAFE_FOOTING:ShorelineScan.Cell.AIR,edge,current,0,2);
        assertEquals(other,finish(r).position());
    }
}
