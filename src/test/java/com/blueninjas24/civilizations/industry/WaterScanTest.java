package com.blueninjas24.civilizations.industry;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaterScanTest {
    private static WaterScan.Observation scan(WaterScan.Source source,int depth) {
        var scan=new WaterScan(source,-1,-2,-4,8,depth);
        while(!scan.complete())scan.advance(17);
        return scan.observation(UUID.randomUUID(),100);
    }
    @Test void strictBudgetAndNegativeAbsoluteCoordinatesSurviveIncrementalTraversal() {
        var reads=new AtomicInteger();
        var scan=new WaterScan((x,y,z)->{reads.incrementAndGet();return y>=4?WaterScan.Cell.AIR:y>=2?WaterScan.Cell.WATER:WaterScan.Cell.OTHER;},-1,-2,-4,8,2);
        assertThrows(IllegalStateException.class,()->scan.observation(UUID.randomUUID(),0));
        while(!scan.complete()){int before=reads.get();assertTrue(scan.advance(7)<=7);assertTrue(reads.get()-before<=7);}
        assertEquals(12*256,reads.get());assertEquals(0,scan.advance(1));
        var evidence=scan.observation(UUID.randomUUID(),0);assertTrue(evidence.complete());assertEquals(256,evidence.suitableColumns().size());
        assertEquals(new FisheryObservation.Column(-16,-32),evidence.columns().getFirst().position());assertEquals(3,evidence.columns().getFirst().surfaceY());
    }
    @Test void depthShallowWaterAndSolidCoveredWaterAreDistinct() {
        WaterScan.Source source=(x,y,z)->y>=4?WaterScan.Cell.AIR:y==3?WaterScan.Cell.WATER:y==2&&x==0?WaterScan.Cell.WATER:WaterScan.Cell.OTHER;
        assertEquals(16,scan(source,2).suitableColumns().size());assertEquals(256,scan(source,1).suitableColumns().size());
        var covered=scan((x,y,z)->y>=5?WaterScan.Cell.AIR:y==4?WaterScan.Cell.OTHER:y>=2?WaterScan.Cell.WATER:WaterScan.Cell.OTHER,2);
        assertTrue(covered.complete());assertEquals(0,covered.suitableColumns().size());
    }
    @Test void SeparatedWaterLayersCannotBeCountedAsContinuousDepth() {
        var evidence=scan((x,y,z)->y>=4?WaterScan.Cell.AIR:y==3||y==1?WaterScan.Cell.WATER:WaterScan.Cell.AIR,2);
        assertTrue(evidence.complete());assertTrue(evidence.suitableColumns().isEmpty());
    }
    @Test void unknownAnywhereKeepsItsColumnUnknownEvenWithApparentlySuitableWater() {
        var evidence=scan((x,y,z)->x==0&&y==-4?WaterScan.Cell.UNKNOWN:y>=4?WaterScan.Cell.AIR:y>=2?WaterScan.Cell.WATER:WaterScan.Cell.OTHER,2);
        assertFalse(evidence.complete());assertEquals(240,evidence.suitableColumns().size());
        assertEquals(16,evidence.columns().stream().filter(c->c.state()==FisheryHabitatObservation.State.UNKNOWN).count());
        assertNull(evidence.columns().getFirst().surfaceY());
        assertFalse(scan((x,y,z)->WaterScan.Cell.UNKNOWN,2).complete());
    }
    @Test void captureMetadataAndImmutableEvidenceCannotBeForgedAsAnEmptyCompleteSurvey() {
        var evidence=scan((x,y,z)->WaterScan.Cell.AIR,2);
        assertTrue(evidence.complete());assertEquals(256,evidence.columns().size());assertEquals(-4,evidence.minimumY());assertEquals(8,evidence.maximumY());
        assertThrows(UnsupportedOperationException.class,()->evidence.columns().clear());
        assertThrows(IllegalArgumentException.class,()->new WaterScan.Observation(evidence.world(),-1,-2,100,-4,8,2,List.of()));
        assertThrows(IllegalArgumentException.class,()->new WaterScan.Observation(evidence.world(),0,0,100,-4,8,2,evidence.columns()));
        assertThrows(IllegalArgumentException.class,()->new WaterScan.Column(new FisheryObservation.Column(0,0),FisheryHabitatObservation.State.UNKNOWN,3));
    }
    @Test void invalidBoundsDepthBudgetsAndOverflowRejectBeforeScanning() {
        WaterScan.Source source=(x,y,z)->WaterScan.Cell.AIR;
        assertThrows(IllegalArgumentException.class,()->new WaterScan(source,0,0,1,1,1));
        assertThrows(IllegalArgumentException.class,()->new WaterScan(source,0,0,-64,320,0));
        assertThrows(IllegalArgumentException.class,()->new WaterScan(source,0,0,0,2,3));
        assertThrows(IllegalArgumentException.class,()->new WaterScan(source,0,0,Integer.MIN_VALUE,Integer.MAX_VALUE,2));
        assertThrows(ArithmeticException.class,()->new WaterScan(source,Integer.MAX_VALUE,0,-64,320,2));
        assertThrows(IllegalArgumentException.class,()->new WaterScan(source,0,0,-64,320,2).advance(0));
    }
}
