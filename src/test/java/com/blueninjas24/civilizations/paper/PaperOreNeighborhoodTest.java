package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.bukkit.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

class PaperOreNeighborhoodTest {
    final AtomicInteger reads=new AtomicInteger();
    ChunkSnapshot snapshot(int x,int z,String world,Map<Position,Material> blocks) {
        return (ChunkSnapshot)Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),new Class<?>[]{ChunkSnapshot.class},(p,m,a)->switch(m.getName()) {
            case "getX" -> x;
            case "getZ" -> z;
            case "getWorldName" -> world;
            case "getBlockType" -> {reads.incrementAndGet();yield blocks.getOrDefault(new Position((int)a[0],(int)a[1],(int)a[2]),Material.DIRT);}
            default -> throw new AssertionError("Unexpected snapshot method: "+m.getName());
        });
    }
    OreScan scan(OreScan.Source source,boolean quarry) {var s=quarry?OreScan.quarries(source,0,0,0,3,Set.of()):new OreScan(source,0,0,0,3,Set.of());while(!s.complete())assertTrue(s.advance(7)<=7);return s;}
    @Test void knownEastFaceMakesRealBoundaryOreEvidenceCompleteWithoutInventingUnknownAir() {
        var centre=snapshot(0,0,"Fixture",Map.of(new Position(15,1,4),Material.COAL_ORE));
        var east=snapshot(1,0,"Fixture",Map.of(new Position(0,1,4),Material.AIR));
        var missing=scan(PaperOreNeighborhood.ores(Map.of(new OreSurveyArea.Chunk(0,0),centre),0,0,0,3),false).observations(new UUID(0,1),10);assertFalse(missing.getFirst().complete());assertTrue(missing.getFirst().ore().isEmpty());
        var evidence=scan(PaperOreNeighborhood.ores(Map.of(new OreSurveyArea.Chunk(0,0),centre,new OreSurveyArea.Chunk(1,0),east),0,0,0,3),false).observations(new UUID(0,1),10);assertTrue(evidence.getFirst().complete());assertEquals(List.of(new Position(15,1,4)),evidence.getFirst().ore());
    }
    @Test void stoneAndSandUseSeparateKnownCardinalFacesWithoutChangingDefaultOreMode() {
        var centre=snapshot(0,0,"Fixture",Map.of(new Position(15,1,4),Material.DEEPSLATE,new Position(4,1,0),Material.RED_SAND));
        var east=snapshot(1,0,"Fixture",Map.of(new Position(0,1,4),Material.AIR));var north=snapshot(0,-1,"Fixture",Map.of(new Position(4,1,15),Material.CAVE_AIR));
        var map=Map.of(new OreSurveyArea.Chunk(0,0),centre,new OreSurveyArea.Chunk(1,0),east,new OreSurveyArea.Chunk(0,-1),north);
        var found=scan(PaperOreNeighborhood.quarries(map,0,0,0,3),true).observations(new UUID(0,1),10);assertTrue(found.getFirst().complete());assertEquals(Commodity.STONE,found.getFirst().mineral());assertEquals(1,found.getFirst().ore().size());assertEquals(Commodity.SAND,found.getLast().mineral());assertEquals(1,found.getLast().ore().size());
        assertTrue(scan(PaperOreNeighborhood.ores(map,0,0,0,3),false).observations(new UUID(0,1),10).stream().allMatch(o->o.ore().isEmpty()));
    }
    @Test void missingAndDiagonalAndOutOfHeightCellsRemainUnknownWithoutSnapshotCalls() {
        var centre=snapshot(0,0,"Fixture",Map.of());var source=PaperOreNeighborhood.quarries(Map.of(new OreSurveyArea.Chunk(0,0),centre),0,0,0,3);reads.set(0);
        for(var p:List.of(new Position(-1,1,0),new Position(16,1,0),new Position(-1,1,-1),new Position(17,1,0),new Position(0,-1,0),new Position(0,3,0)))assertEquals(OreScan.Kind.UNKNOWN,source.kind(p.x(),p.y(),p.z()));assertEquals(0,reads.get());
    }
    @Test void coordinatesAndWorldsMustMatchAndInputMapsAreDefensivelyCopied() {
        var centre=snapshot(0,0,"Fixture",Map.of(new Position(0,1,0),Material.COAL_ORE));var map=new HashMap<OreSurveyArea.Chunk,ChunkSnapshot>();map.put(new OreSurveyArea.Chunk(0,0),centre);var source=PaperOreNeighborhood.ores(map,0,0,0,3);map.clear();assertEquals(OreScan.Kind.COAL,source.kind(0,1,0));
        assertThrows(NullPointerException.class,()->PaperOreNeighborhood.ores(Map.of(new OreSurveyArea.Chunk(1,0),centre),0,0,0,3));
        assertThrows(IllegalArgumentException.class,()->PaperOreNeighborhood.ores(Map.of(new OreSurveyArea.Chunk(0,0),snapshot(1,0,"Fixture",Map.of())),0,0,0,3));
        assertThrows(IllegalArgumentException.class,()->PaperOreNeighborhood.ores(Map.of(new OreSurveyArea.Chunk(0,0),centre,new OreSurveyArea.Chunk(1,0),snapshot(1,0,"Other",Map.of())),0,0,0,3));
        assertThrows(IllegalArgumentException.class,()->PaperOreNeighborhood.ores(Map.of(new OreSurveyArea.Chunk(0,0),centre,new OreSurveyArea.Chunk(1,1),snapshot(1,1,"Fixture",Map.of())),0,0,0,3));
    }
    @Test void negativeChunkCoordinatesKeepAbsoluteIdentityAndHaloResourcesAreNotSurveyMembers() {
        var centre=snapshot(-3,-5,"Fixture",Map.of(new Position(0,1,4),Material.COAL_ORE));
        var west=snapshot(-4,-5,"Fixture",Map.of(new Position(15,1,4),Material.AIR,new Position(15,1,8),Material.IRON_ORE));
        var source=PaperOreNeighborhood.ores(Map.of(new OreSurveyArea.Chunk(-3,-5),centre,new OreSurveyArea.Chunk(-4,-5),west),-3,-5,0,3);
        assertEquals(OreScan.Kind.AIR,source.kind(-1,1,4));assertEquals(OreScan.Kind.IRON,source.kind(-1,1,8));
        var scan=new OreScan(source,-3,-5,0,3,Set.of());while(!scan.complete())scan.advance(8);
        var evidence=scan.observations(new UUID(0,1),10);assertTrue(evidence.getFirst().complete());assertEquals(List.of(new Position(-48,1,-76)),evidence.getFirst().ore());assertTrue(evidence.getLast().ore().isEmpty());
    }
    @Test void impossibleNeighborCoordinatesAndInvalidHeightFailBeforeAnyBlockRead() {
        int edge=Math.floorDiv(Integer.MAX_VALUE-15,16);var centre=snapshot(edge,0,"Fixture",Map.of());
        assertThrows(ArithmeticException.class,()->PaperOreNeighborhood.ores(Map.of(new OreSurveyArea.Chunk(edge,0),centre,new OreSurveyArea.Chunk(edge+1,0),snapshot(edge+1,0,"Fixture",Map.of())),edge,0,0,3));
        assertThrows(IllegalArgumentException.class,()->PaperOreNeighborhood.ores(Map.of(new OreSurveyArea.Chunk(edge,0),centre),edge,0,1,1));assertEquals(0,reads.get());
    }
}
