package com.blueninjas24.civilizations.industry;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkplaceScanTest {
    @Test void boundedSnapshotTraversalFindsOnlyEnabledKindsAtNegativeCoordinates() {
        var policy=WorkplaceObserverPolicy.prototype();var reads=new java.util.concurrent.atomic.AtomicInteger();
        WorkplaceScan.Source source=(x,y,z)->{reads.incrementAndGet();return x==0&&z==0?WorkplaceScan.Cell.BARREL:x==1&&z==0?WorkplaceScan.Cell.SMITHING_TABLE:WorkplaceScan.Cell.OTHER;};
        var scan=new WorkplaceScan(source,-1,-2,64,65,policy.kinds());assertThrows(IllegalStateException.class,()->scan.observation(UUID.randomUUID(),1000));
        while(!scan.complete()){int before=reads.get();assertTrue(scan.advance(17)<=17);assertTrue(reads.get()-before<=17);}
        var o=scan.observation(UUID.randomUUID(),1000);assertTrue(o.complete());assertEquals(2,o.markers().size());assertEquals(-16,o.markers().getFirst().position().x());assertEquals(-32,o.markers().getFirst().position().z());
        assertEquals(256,reads.get());assertEquals(0,scan.advance(1));
        var only=new WorkplaceScan(source,0,0,64,65,Set.of(WorkplaceObservation.Kind.BARREL));only.advance(256);assertEquals(1,only.observation(o.world(),1000).markers().size());
    }
    @Test void unknownCellsAndOverflowProducePartialEvidenceNotPermission() {
        var policy=WorkplaceObserverPolicy.prototype();var unknown=new WorkplaceScan((x,y,z)->WorkplaceScan.Cell.UNKNOWN,0,0,0,1,policy.kinds());unknown.advance(256);assertFalse(unknown.observation(UUID.randomUUID(),0).complete());
        var crowded=new WorkplaceScan((x,y,z)->WorkplaceScan.Cell.BARREL,0,0,0,3,policy.kinds());crowded.advance(768);var o=crowded.observation(UUID.randomUUID(),0);assertFalse(o.complete());assertEquals(512,o.markers().size());
        assertThrows(IllegalArgumentException.class,()->crowded.advance(0));assertThrows(ArithmeticException.class,()->new WorkplaceScan((x,y,z)->WorkplaceScan.Cell.OTHER,Integer.MAX_VALUE,0,0,1,policy.kinds()));
    }
}
