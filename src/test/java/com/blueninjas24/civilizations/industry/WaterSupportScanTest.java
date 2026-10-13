package com.blueninjas24.civilizations.industry;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WaterSupportScanTest {
    WaterPatch.Patch patch(int y){return new WaterPatch.Patch(new FisheryObservation(UUID.randomUUID(),0,true,List.of(new FisheryObservation.Column(0,0),new FisheryObservation.Column(1,0))),y);}
    void finish(WaterSupportScan scan){while(!scan.complete())scan.advance(2);}
    @Test void actualDepthFullOverheadAndBudgetAreCheckedWithoutInventingMissingCells() {
        var reads=new int[1];var scan=new WaterSupportScan(p->{reads[0]++;return p.y()>2?WaterSupportScan.Cell.AIR:WaterSupportScan.Cell.WATER;},patch(2),0,8,2);
        assertThrows(IllegalStateException.class,()->scan.observation(10));while(!scan.complete()){int before=reads[0];int used=scan.advance(3);assertTrue(used<=3);assertEquals(used,reads[0]-before);}
        assertEquals(14,reads[0]);assertTrue(scan.allSuitable());assertEquals(2,scan.observation(10).supports().size());
    }
    @Test void roofAndMissingWaterRetireOnlyConfirmedColumnWhileUnknownPreventsInference() {
        var scan=new WaterSupportScan(p->p.x()==1&&p.y()==5?WaterSupportScan.Cell.OTHER:p.x()==0&&p.y()==1?WaterSupportScan.Cell.UNKNOWN:p.y()>2?WaterSupportScan.Cell.AIR:WaterSupportScan.Cell.WATER,patch(2),0,8,2);finish(scan);
        assertFalse(scan.allSuitable());assertEquals(FisheryHabitatObservation.State.UNKNOWN,scan.observation(10).supports().getFirst().state());assertEquals(FisheryHabitatObservation.State.ABSENT,scan.observation(10).supports().getLast().state());
    }
    @Test void changedWorldHeightAndExtremeCoordinatesCannotProduceFalseWater() {
        var scan=new WaterSupportScan(p->WaterSupportScan.Cell.WATER,patch(10),0,8,2);finish(scan);assertFalse(scan.allSuitable());
        var edge=new WaterSupportScan(p->WaterSupportScan.Cell.WATER,patch(Integer.MIN_VALUE),Integer.MIN_VALUE,Integer.MIN_VALUE+2,2);finish(edge);assertFalse(edge.allSuitable());
        assertThrows(IllegalArgumentException.class,()->new WaterSupportScan(p->WaterSupportScan.Cell.WATER,patch(2),0,4097,2));
    }
}
