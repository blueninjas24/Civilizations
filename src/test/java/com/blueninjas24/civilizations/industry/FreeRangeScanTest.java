package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FreeRangeScanTest {
    @Test void eachTickIsBoundedAndEveryCellMustBePositivelyClear(){
        var calls=new java.util.concurrent.atomic.AtomicInteger();var scan=new FreeRangeScan(p->{calls.incrementAndGet();return FreeRangeScan.Cell.CLEAR;},List.of(new Position(0,64,0)),2);
        assertEquals(3,scan.advance(3));assertFalse(scan.complete());assertThrows(IllegalStateException.class,scan::result);
        while(!scan.complete())assertTrue(scan.advance(3)<=3);assertEquals(25,calls.get());assertEquals(FreeRangeScan.Cell.CLEAR,scan.result());assertEquals(0,scan.advance(3));
    }
    @Test void anyBarrierOrUnavailableCellRefusesWithoutScanningTheRest(){
        for(var result:List.of(FreeRangeScan.Cell.BARRIER,FreeRangeScan.Cell.UNKNOWN)){
            var scan=new FreeRangeScan(p->result,List.of(new Position(0,64,0)),8);assertEquals(1,scan.advance(512));assertEquals(result,scan.result());
        }
        assertThrows(IllegalArgumentException.class,()->new FreeRangeScan(p->FreeRangeScan.Cell.CLEAR,List.of(new Position(0,64,0),new Position(100,64,0)),8));
    }
}
