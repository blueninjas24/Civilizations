package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionPreviewTest {
    private final UUID world=new UUID(0,1);
    private final ConstructionTemplate template=ConstructionTemplateTest.template();
    private final Position origin=new Position(-32,65,-16);
    private Map<Position,Cell> cells(Transform transform) {
        var cells=new HashMap<Position,Cell>();
        for(var point:ConstructionPreview.requiredCells(template,transform,origin))cells.put(point,new Cell(point.y()>=origin.y(),point.y()<origin.y(),false,false));
        for(var anchor:template.anchors())if(anchor.role()==Role.ROAD)cells.put(origin.plus(transform.apply(anchor.at(),template.size())),new Cell(false,true,true,false));
        return cells;
    }
    private ApprovedArea area(Transform transform){return new ApprovedArea(new UUID(0,2),world,ConstructionPreview.footprint(template,transform,origin));}
    private Assessment review(Observation observation,ApprovedArea approval,Transform transform){return ConstructionPreview.assess(template,transform,world,origin,observation,approval,1100,1000);}
    @Test void completeSuitableTerrainNeverInfersDevelopmentPermission() {
        var transform=Transform.identity();var observation=new Observation(world,origin,1000,true,false,cells(transform));
        assertEquals(Set.of(Issue.NO_APPROVED_AREA),review(observation,null,transform).issues());
        assertTrue(review(observation,area(transform),transform).suitable());
        assertFalse(review(observation,new ApprovedArea(UUID.randomUUID(),UUID.randomUUID(),area(transform).bounds()),transform).suitable());
    }
    @Test void rotatedMirroredSparseBlueprintChecksTheWholeRoomAndRealRoadApproach() {
        for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)) {
            var transform=new Transform(rotation,mirror);var cells=cells(transform);
            assertTrue(review(new Observation(world,origin,1000,true,false,cells),area(transform),transform).suitable());
            var interior=origin.plus(transform.apply(new Position(1,1,2),template.size()));
            cells.put(interior,new Cell(false,false,false,false));
            assertTrue(review(new Observation(world,origin,1000,true,false,cells),area(transform),transform).issues().contains(Issue.OBSTRUCTED));
            cells=cells(transform);var road=origin.plus(transform.apply(template.anchors().getLast().at(),template.size()));
            cells.put(road,new Cell(false,true,false,false));
            assertTrue(review(new Observation(world,origin,1000,true,false,cells),area(transform),transform).issues().contains(Issue.ROAD_REQUIRED));
        }
    }
    @Test void unknownStaleFutureOrMovedFramesCannotAuthorizeConstruction() {
        var transform=Transform.identity();var approval=area(transform);var cells=cells(transform);
        var frames=List.of(new Observation(world,origin,0,true,false,cells),new Observation(world,origin,1200,true,false,cells),
                new Observation(world,origin,1000,false,false,cells),new Observation(UUID.randomUUID(),origin,1000,true,false,cells),
                new Observation(world,new Position(1,65,0),1000,true,false,cells));
        for(var frame:frames)assertFalse(review(frame,approval,transform).suitable());
        cells.remove(origin);var partial=new Observation(world,origin,1000,true,false,cells);
        assertTrue(review(partial,approval,transform).issues().contains(Issue.INCOMPLETE));
        assertFalse(review(partial,approval,transform).issues().contains(Issue.UNSUPPORTED));
    }
    @Test void protectedSitesEntitiesAndUnstableFootingsRemainBlockedEvenInsideApprovedArea() {
        var transform=Transform.identity();var approval=area(transform);var cells=cells(transform);
        assertTrue(review(new Observation(world,origin,1000,true,true,cells),approval,transform).issues().contains(Issue.ENTITY_PRESENT));
        cells.put(origin,new Cell(true,false,false,true));
        assertTrue(review(new Observation(world,origin,1000,true,false,cells),approval,transform).issues().contains(Issue.PROTECTED));
        cells=cells(transform);cells.put(origin.plus(new Position(0,-1,0)),new Cell(false,false,false,false));
        var frame=new Observation(world,origin,1000,true,false,cells);cells.clear();
        assertTrue(review(frame,approval,transform).issues().contains(Issue.UNSUPPORTED));
    }
}
