package com.blueninjas24.civilizations.industry;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShorelineScanTest {
    WaterPatch.Patch patch(int x,int z,int y){return new WaterPatch.Patch(new FisheryObservation(UUID.randomUUID(),100,true,List.of(new FisheryObservation.Column(x,z))),y);}
    @Test void drySolidFootingAndTwoAirBlocksAreRequiredUnderStrictBudgets() {
        var reads=new AtomicInteger();var scan=new ShorelineScan(p->{reads.incrementAndGet();return p.y()==63?ShorelineScan.Cell.SAFE_FOOTING:ShorelineScan.Cell.AIR;},patch(0,0,63),0);
        assertThrows(IllegalStateException.class,scan::suitable);
        while(!scan.complete()){int before=reads.get();assertTrue(scan.advance(1)<=1);assertEquals(1,reads.get()-before);}
        assertEquals(12,reads.get());assertEquals(4,scan.suitable().size());assertTrue(scan.suitable().stream().allMatch(p->p.y()==64));assertEquals(0,scan.advance(1));assertFalse(scan.encounteredUnknown());
    }
    @Test void obstructionUnsupportedGroundAndUnknownNeverMakeWorkplaces() {
        var scan=new ShorelineScan(p->p.x()==-1?ShorelineScan.Cell.UNKNOWN:p.x()==1?ShorelineScan.Cell.AIR:p.z()==-1?ShorelineScan.Cell.OTHER:p.y()==63?ShorelineScan.Cell.SAFE_FOOTING:ShorelineScan.Cell.AIR,patch(0,0,63),0);
        while(!scan.complete())scan.advance(7);assertTrue(scan.encounteredUnknown());assertEquals(List.of(new ShorelineScan.Position(0,64,1)),scan.suitable());
    }
    @Test void searchFindsRaisedBankButNotACliffBeyondCallerLimit() {
        for(int rise:List.of(1,2)){var scan=new ShorelineScan(p->p.y()<=65?ShorelineScan.Cell.SAFE_FOOTING:ShorelineScan.Cell.AIR,patch(0,0,63),rise);while(!scan.complete())scan.advance(8);assertEquals(rise==2?4:0,scan.suitable().size());}
    }
    @Test void candidateCoordinatesCannotWrapAndBoundsReject() {
        var scan=new ShorelineScan(p->ShorelineScan.Cell.AIR,patch(Integer.MAX_VALUE,Integer.MIN_VALUE,63),0);while(!scan.complete())scan.advance(4096);assertTrue(scan.suitable().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new ShorelineScan(p->ShorelineScan.Cell.AIR,patch(0,0,63),5));assertThrows(IllegalArgumentException.class,()->scan.advance(4097));
    }
}
