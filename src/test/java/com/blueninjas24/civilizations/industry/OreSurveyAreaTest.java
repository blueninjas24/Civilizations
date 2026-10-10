package com.blueninjas24.civilizations.industry;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OreSurveyAreaTest {
    @Test void centerThenBoundedRingsVisitEachChunkOnce() {
        var area=OreSurveyArea.around(6,8,2);assertEquals(25,area.size());assertEquals(25,new HashSet<>(area).size());
        assertEquals(new OreSurveyArea.Chunk(0,0),area.getFirst());
        assertTrue(area.subList(1,9).stream().allMatch(c->Math.max(Math.abs(c.x()),Math.abs(c.z()))==1));
        assertTrue(area.subList(9,25).stream().allMatch(c->Math.max(Math.abs(c.x()),Math.abs(c.z()))==2));
        assertEquals(area,OreSurveyArea.around(15,0,2));assertThrows(UnsupportedOperationException.class,()->area.clear());
    }
    @Test void negativeBlocksUseFloorDivisionAndZeroRetainsLegacyScope() {
        assertEquals(List.of(new OreSurveyArea.Chunk(-1,-2)),OreSurveyArea.around(-1,-17,0));
        assertEquals(new OreSurveyArea.Chunk(-1,-2),OreSurveyArea.around(-1,-17,1).getFirst());
    }
    @Test void coordinateEdgesClipWithoutWrappingOrRejectingWholeTown() {
        var area=OreSurveyArea.around(Integer.MAX_VALUE,Integer.MIN_VALUE,2);assertEquals(9,area.size());
        assertTrue(area.stream().allMatch(c->(long)c.x()*16>=Integer.MIN_VALUE && (long)c.x()*16+15<=Integer.MAX_VALUE
                && (long)c.z()*16>=Integer.MIN_VALUE && (long)c.z()*16+15<=Integer.MAX_VALUE));
    }
    @Test void oversizedExplorationAndBudgetsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->OreSurveyArea.around(0,0,3));
        assertThrows(IllegalArgumentException.class,()->OreSurveyArea.around(0,0,-1));
        assertThrows(IllegalArgumentException.class,()->new OreObserverPolicy(true,24,512,400,3));
        assertThrows(IllegalArgumentException.class,()->new OreObserverPolicy(true,24,512,400,-1));
    }
    @Test void oldConstructorRetainsOneChunkAndNewPrototypeAddsOnlyBoundedNeighbors() {
        assertEquals(0,new OreObserverPolicy(true,24,512,400).horizontalRadiusChunks());
        assertEquals(1,OreObserverPolicy.prototype().horizontalRadiusChunks());
        assertEquals(0,OreObserverPolicy.disabled().horizontalRadiusChunks());
    }
}
