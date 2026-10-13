package com.blueninjas24.civilizations.industry;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaterPatchTest {
    UUID world=UUID.randomUUID();
    WaterPatch.Sample sample(int x,int z,int y,long time){return new WaterPatch.Sample(new FisheryObservation.Column(x,z),y,time);}
    List<WaterPatch.Patch> choose(List<WaterPatch.Sample> samples,int minimum,int maximum,int sites){return WaterPatch.choose(world,samples,0,0,1000,100,minimum,maximum,sites);}
    @Test void onlyCardinalSameLevelConnectedWaterMakesOneSite() {
        var samples=List.of(sample(0,0,64,1000),sample(1,0,64,1000),sample(2,1,64,1000),sample(2,0,63,1000));
        var patches=choose(samples,2,64,8);assertEquals(1,patches.size());assertEquals(List.of(new FisheryObservation.Column(0,0),new FisheryObservation.Column(1,0)),patches.getFirst().evidence().columns());assertEquals(64,patches.getFirst().surfaceY());
    }
    @Test void completeFreshCrossChunkEvidenceKeepsOldestCapture() {
        var samples=List.of(sample(-1,0,64,901),sample(0,0,64,999),sample(1,0,64,899),sample(0,1,64,1001));
        var evidence=choose(samples,2,64,1).getFirst().evidence();assertEquals(901,evidence.observedMillis());assertEquals(world,evidence.world());assertTrue(evidence.complete());assertEquals(2,evidence.columns().size());
    }
    @Test void boundedOceanPatchIsConnectedNearestAndIndependentOfInputOrder() {
        var samples=new ArrayList<WaterPatch.Sample>();for(int x=-16;x<16;x++)for(int z=-16;z<16;z++)samples.add(sample(x,z,64,1000));
        var expected=choose(samples,16,32,4);assertEquals(1,expected.size());assertEquals(32,expected.getFirst().evidence().columns().size());assertTrue(expected.getFirst().evidence().columns().contains(new FisheryObservation.Column(0,0)));
        var columns=new HashSet<>(expected.getFirst().evidence().columns());var visited=new HashSet<FisheryObservation.Column>();var queue=new ArrayDeque<FisheryObservation.Column>();queue.add(new FisheryObservation.Column(0,0));visited.add(queue.getFirst());
        while(!queue.isEmpty()){var p=queue.removeFirst();for(var n:List.of(new FisheryObservation.Column(p.x()-1,p.z()),new FisheryObservation.Column(p.x()+1,p.z()),new FisheryObservation.Column(p.x(),p.z()-1),new FisheryObservation.Column(p.x(),p.z()+1)))if(columns.contains(n)&&visited.add(n))queue.add(n);}
        assertEquals(columns,visited);var random=new Random(72);for(int i=0;i<100;i++){Collections.shuffle(samples,random);assertEquals(expected,choose(samples,16,32,4));}
    }
    @Test void nearestEligibleComponentsAreLimitedWithoutCombiningPuddles() {
        var samples=List.of(sample(0,0,64,1000),sample(10,0,64,1000),sample(11,0,64,1000),sample(20,0,64,1000),sample(21,0,64,1000));
        var patches=choose(samples,2,8,1);assertEquals(1,patches.size());assertEquals(10,patches.getFirst().evidence().columns().getFirst().x());assertTrue(choose(samples,3,8,8).isEmpty());
    }
    @Test void extremaDoNotWrapOrOverflowDistance() {
        var samples=List.of(sample(Integer.MIN_VALUE,0,64,1000),sample(Integer.MAX_VALUE,0,64,1000),sample(Integer.MAX_VALUE-1,0,64,1000));
        var result=WaterPatch.choose(world,samples,Integer.MIN_VALUE,0,1000,100,2,8,1);assertEquals(2,result.getFirst().evidence().columns().size());assertEquals(Integer.MAX_VALUE-1,result.getFirst().evidence().columns().getFirst().x());
    }
    @Test void duplicatesAndInvalidBudgetsRejectRatherThanInventPartialEvidence() {
        var s=sample(0,0,64,1000);assertThrows(IllegalArgumentException.class,()->choose(List.of(s,s),1,8,1));assertThrows(IllegalArgumentException.class,()->choose(List.of(s),2,1,1));assertThrows(IllegalArgumentException.class,()->choose(List.of(s),1,4097,1));assertThrows(IllegalArgumentException.class,()->choose(List.of(s),1,8,65));
        assertTrue(choose(List.of(),1,8,1).isEmpty());
    }
}
