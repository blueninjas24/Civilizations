package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperHomeSiteSurveyBoundsTest {
    @Test void normalBedLevelVillageCenterIncludesGroundAndEntireSelectedRoof(){
        var bed=new Position(0,64,0); // Bed and new foundation at64, natural road/ground at63.
        var small=PaperHomeSiteSurvey.bounds(bed,12,4);assertTrue(small.contains(new Position(0,63,0)));assertTrue(small.contains(new Position(0,67,0)));assertEquals(67,small.maximum().y());
        var large=PaperHomeSiteSurvey.bounds(bed,12,5);assertTrue(large.contains(new Position(0,68,0)));assertEquals(61,large.minimum().y());assertEquals(68,large.maximum().y());
    }
    @Test void oversizedAndOverflowingCoverageRefusesBeforeNativeReads(){
        assertThrows(IllegalArgumentException.class,()->PaperHomeSiteSurvey.bounds(new Position(0,64,0),12,64));
        assertThrows(IllegalArgumentException.class,()->PaperHomeSiteSurvey.bounds(new Position(0,64,0),13,4));
        assertThrows(ArithmeticException.class,()->PaperHomeSiteSurvey.bounds(new Position(0,Integer.MAX_VALUE,0),12,5));
    }
}
