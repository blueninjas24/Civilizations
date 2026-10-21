package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionLandPolicy.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import com.blueninjas24.civilizations.economy.Commodity;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionLandPolicyTest {
    private final Position origin=new Position(-17,64,31);
    private ConstructionTemplate template()throws Exception{return ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_storehouse.yml"));}
    private Map<Position,Cell> clean(ConstructionLandPolicy policy,ConstructionTemplate template) {
        var cells=new HashMap<Position,Cell>();for(var p:policy.requiredCells(template,origin))cells.put(p,new Cell(p.y()==63?Kind.NATURAL_LOOKING_GROUND:Kind.AIR,false));
        cells.put(origin.plus(new Position(2,-1,-1)),new Cell(Kind.ROAD,false));return cells;
    }
    @Test void rectangularRotationsAndMirrorsScreenTheirActualFootprintAndConnector() {
        var plan=new ConstructionTemplate("rectangle",1,"Rectangle","Original test",Purpose.WORKSHOP,new Size(3,2,5),
                List.of(new Stage("base",1,Map.of(Commodity.STONE,1L),Map.of(new Position(0,0,0),"minecraft:cobblestone"))),
                List.of(new Anchor(Role.ENTRANCE,new Position(1,1,0),Direction.NORTH),new Anchor(Role.ROAD,new Position(1,-1,-1),Direction.NORTH)));
        var policy=new ConstructionLandPolicy(2);
        for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)){
            var transform=new Transform(rotation,mirror);var cells=new HashMap<Position,Cell>();
            for(var p:policy.requiredCells(plan,transform,origin))cells.put(p,new Cell(p.y()==origin.y()-1?Kind.NATURAL_LOOKING_GROUND:Kind.AIR,false));
            var road=origin.plus(transform.apply(new Position(1,-1,-1),plan.size()));cells.put(road,new Cell(Kind.ROAD,false));
            assertEquals(189,cells.size());assertTrue(policy.review(plan,transform,origin,cells,true).suitable());
            var corner=ConstructionPreview.footprint(plan,transform,origin).maximum();cells.put(corner,new Cell(Kind.OTHER,false));
            assertTrue(policy.review(plan,transform,origin,cells,true).findings().contains(Finding.FOOTPRINT_OCCUPIED));
        }
    }
    @Test void completeClearLandWithBufferedRoadCanPassWithoutPretendingToKnowWhoClearedIt()throws Exception {
        var policy=new ConstructionLandPolicy(4);var template=template();var cells=clean(policy,template);
        assertTrue(policy.review(template,origin,cells,true).suitable());
        assertEquals(845,policy.requiredCells(template,origin).size());
        assertThrows(UnsupportedOperationException.class,()->policy.requiredCells(template,origin).clear());
    }
    @Test void nearbyObstaclesProtectedEmptyPlotsOccupiedRoomsAndRoadsUnderBuildingsAreExcluded()throws Exception {
        var policy=new ConstructionLandPolicy(4);var template=template();
        var cells=clean(policy,template);cells.put(origin.plus(new Position(-2,1,0)),new Cell(Kind.OTHER,false));
        assertTrue(policy.review(template,origin,cells,true).findings().contains(Finding.NEARBY_OBSTACLE));
        cells=clean(policy,template);cells.put(origin.plus(new Position(-4,0,0)),new Cell(Kind.AIR,true));
        assertTrue(policy.review(template,origin,cells,true).findings().contains(Finding.PROTECTED));
        cells=clean(policy,template);cells.put(origin.plus(new Position(2,1,2)),new Cell(Kind.OTHER,false));
        assertTrue(policy.review(template,origin,cells,true).findings().contains(Finding.FOOTPRINT_OCCUPIED));
        cells=clean(policy,template);cells.put(origin.plus(new Position(2,-1,2)),new Cell(Kind.ROAD,false));
        assertTrue(policy.review(template,origin,cells,true).findings().contains(Finding.UNSUITABLE_GROUND));
    }
    @Test void missingSurveyEvidenceAndOversizedProfilesNeverBecomeClearSites()throws Exception {
        var policy=new ConstructionLandPolicy(4);var template=template();var cells=clean(policy,template);
        assertEquals(Set.of(Finding.INCOMPLETE),policy.review(template,origin,cells,false).findings());
        cells.remove(origin.plus(new Position(-4,0,0)));assertEquals(Set.of(Finding.INCOMPLETE),policy.review(template,origin,cells,true).findings());
        var big=new ConstructionTemplate("large",1,"Large","Original test",Purpose.PATH,new Size(64,4,64),
                List.of(new Stage("base",1,Map.of(Commodity.STONE,1L),Map.of(new Position(0,0,0),"minecraft:cobblestone"))),List.of());
        assertThrows(IllegalArgumentException.class,()->policy.requiredCells(big,origin));
    }
}
