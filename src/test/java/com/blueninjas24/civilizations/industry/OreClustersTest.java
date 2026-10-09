package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OreClustersTest {
    private final UUID world=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private MineralObservation sample(boolean complete,List<Position> positions) {
        return new MineralObservation(world,123,complete,Commodity.COAL,positions);
    }
    @Test void distantPatchesRemainSeparateAndConnectedBranchesStayTogether() {
        var a=new Position(-16,-60,0);var b=new Position(-15,-60,0);var c=new Position(-15,-59,0);
        var d=new Position(100,10,0);var e=new Position(100,10,1);
        var result=OreClusters.split(sample(true,List.of(e,c,a,d,b)));
        assertEquals(List.of(sample(true,List.of(a,b,c)),sample(true,List.of(d,e))),result);
        assertThrows(UnsupportedOperationException.class,result::clear);
        assertThrows(UnsupportedOperationException.class,()->result.getFirst().ore().clear());
    }
    @Test void diagonalsAndUnobservedGapsDoNotInventHiddenConnections() {
        var result=OreClusters.split(sample(true,List.of(new Position(0,0,0),new Position(1,1,0),new Position(3,1,0))));
        assertEquals(3,result.size());assertTrue(result.stream().allMatch(o->o.ore().size()==1));
    }
    @Test void incompleteEvidenceFailsEvenIfSomeOreOrNoOreIsKnown() {
        assertThrows(IllegalArgumentException.class,()->OreClusters.split(sample(false,List.of(new Position(0,0,0)))));
        assertThrows(IllegalArgumentException.class,()->OreClusters.split(sample(false,List.of())));
        assertTrue(OreClusters.split(sample(true,List.of())).isEmpty());
    }
    @Test void evidenceOrderDoesNotChangePatchesAndEveryPositionOccursExactlyOnce() {
        var positions=new ArrayList<Position>();
        for(int x=0;x<8;x++)for(int y=0;y<8;y++)for(int z=0;z<8;z++)positions.add(new Position(x,y,z));
        var expected=OreClusters.split(sample(true,positions));assertEquals(1,expected.size());
        for(int seed=0;seed<20;seed++) {
            Collections.shuffle(positions,new Random(seed));var actual=OreClusters.split(sample(true,positions));
            assertEquals(expected,actual);assertEquals(512,actual.getFirst().ore().size());
            assertEquals(new HashSet<>(positions),new HashSet<>(actual.getFirst().ore()));
        }
    }
    @Test void integerBoundariesCannotWrapOppositeEndsIntoOnePatch() {
        for(int axis=0;axis<3;axis++) {
            var a=new Position(axis==0?Integer.MIN_VALUE:0,axis==1?Integer.MIN_VALUE:0,axis==2?Integer.MIN_VALUE:0);
            var b=new Position(axis==0?Integer.MAX_VALUE:0,axis==1?Integer.MAX_VALUE:0,axis==2?Integer.MAX_VALUE:0);
            assertEquals(2,OreClusters.split(sample(true,List.of(a,b))).size());
        }
    }
    @Test void chunksAreNotPatchBordersAndWorldMineralTimestampRemainUnchanged() {
        var observation=new MineralObservation(world,456,true,Commodity.IRON,List.of(new Position(-1,40,0),new Position(0,40,0),new Position(15,40,0),new Position(16,40,0)));
        var result=OreClusters.split(observation);assertEquals(2,result.size());
        for(var patch:result) {
            assertEquals(world,patch.world());assertEquals(456,patch.observedMillis());assertEquals(Commodity.IRON,patch.mineral());assertTrue(patch.complete());assertEquals(2,patch.ore().size());
        }
    }
}
