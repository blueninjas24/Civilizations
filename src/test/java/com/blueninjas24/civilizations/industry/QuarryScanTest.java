package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.blueninjas24.civilizations.industry.OreScan.Kind.*;

class QuarryScanTest {
    List<MineralObservation> finish(OreScan scan,int budget){while(!scan.complete())assertTrue(scan.advance(budget)<=budget);return scan.observations(new UUID(0,1),10);}
    @Test void exposedEvidenceExcludesPlacementsBuriedResourcesAndOre() {
        OreScan.Source source=(x,y,z)->{
            if(y==1)return AIR;
            if(x==4&&z==4&&y==0)return STONE;
            if(x==5&&z==5&&y==0)return SAND;
            if(x==6&&z==6&&y==0)return COAL;
            if(x==7&&z==7&&y==0)return SAND;
            if(x==8&&z==8&&y==-1)return STONE;
            return SOLID;
        };
        var evidence=finish(OreScan.quarries(source,0,0,-1,1,Set.of(new Position(7,0,7))),7);
        assertTrue(evidence.stream().allMatch(MineralObservation::complete));
        assertEquals(Commodity.STONE,evidence.getFirst().mineral());assertEquals(List.of(new Position(4,0,4)),evidence.getFirst().ore());
        assertEquals(Commodity.SAND,evidence.getLast().mineral());assertEquals(List.of(new Position(5,0,5)),evidence.getLast().ore());
        var old=finish(new OreScan(source,0,0,-1,1,Set.of()),7);
        assertEquals(List.of(new Position(6,0,6)),old.getFirst().ore());assertTrue(old.getLast().ore().isEmpty());
    }
    @Test void scanBoundsReadsAndDoesNotDependOnTickBudget() {
        var reads=new AtomicInteger();OreScan.Source source=(x,y,z)->{reads.incrementAndGet();return y==1?AIR:STONE;};
        var scan=OreScan.quarries(source,-1,2,0,1,Set.of());assertEquals(3,scan.advance(3));assertTrue(reads.get()<=21);
        assertEquals(finish(scan,3),finish(OreScan.quarries(source,-1,2,0,1,Set.of()),100));
    }
    @Test void unknownAndOverflowNeverBecomeCompletePositiveEvidence() {
        var unknown=finish(OreScan.quarries((x,y,z)->x<0?UNKNOWN:STONE,0,0,0,1,Set.of()),10);
        assertFalse(unknown.getFirst().complete());
        var overflow=finish(OreScan.quarries((x,y,z)->y%2==1?AIR:STONE,0,0,0,5,Set.of()),100);
        assertFalse(overflow.getFirst().complete());assertEquals(512,overflow.getFirst().ore().size());
    }
    @Test void abundanceWorkAndStorageAreSeparateLimits() {
        var p=QuarryPolicy.prototype();assertTrue(p.sand().batchesPerWorker()>p.stone().batchesPerWorker());
        assertEquals(64,p.stone().reserveBatchesPerBlock());assertEquals(3,p.stone().unitsPerBatch());
        var plan=MineProduction.calculate(128,100,1,6,32,8,p.bundle(Commodity.STONE),Map.of(Commodity.STONE,1L));
        assertEquals(2,plan.batches());assertEquals(6,plan.outputs().get(Commodity.STONE));
        assertThrows(IllegalArgumentException.class,()->p.terms(Commodity.COAL));
        assertThrows(IllegalArgumentException.class,()->new QuarryPolicy.Terms(0,1,1,1));
    }
}
