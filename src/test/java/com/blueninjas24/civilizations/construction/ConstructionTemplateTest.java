package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.economy.Commodity;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionTemplateTest {
    static ConstructionTemplate template() {
        return new ConstructionTemplate("fixture",1,"Authored test","Original test fixture",Purpose.STORAGE,new Size(3,2,5),
                List.of(new Stage("floor",10,Map.of(Commodity.STONE,2L),Map.of(new Position(0,0,0),"minecraft:cobblestone",new Position(2,0,4),"minecraft:oak_planks"))),
                List.of(new Anchor(Role.ENTRANCE,new Position(1,1,0),Direction.NORTH),new Anchor(Role.ROAD,new Position(1,-1,-1),Direction.NORTH)));
    }
    @Test void everyOrientationPreservesDistinctBlocksAndEntranceDirectionIncludingOutsideRoadConnector() {
        var template=template();
        for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)) {
            var transform=new Transform(rotation,mirror);var size=transform.size(template.size());var occupied=new HashSet<Position>();
            for(int y=0;y<2;y++)for(int z=0;z<5;z++)for(int x=0;x<3;x++) {
                var moved=transform.apply(new Position(x,y,z),template.size());assertTrue(size.contains(moved));assertTrue(occupied.add(moved));
            }
            assertEquals(30,occupied.size());
            var entrance=transform.apply(new Position(1,1,0),template.size());var forward=transform.apply(new Position(1,1,-1),template.size());
            var direction=Direction.NORTH.transform(transform);
            int dx=direction==Direction.EAST?1:direction==Direction.WEST?-1:0;
            int dz=direction==Direction.SOUTH?1:direction==Direction.NORTH?-1:0;
            assertEquals(new Position(entrance.x()+dx,entrance.y(),entrance.z()+dz),forward);
        }
    }
    @Test void fingerprintPinsSemanticCostsAndLayoutWithoutDependingOnMapOrderOrMutableInputs() {
        var first=template();var stage=first.stages().getFirst();var blocks=new LinkedHashMap<Position,String>();
        stage.blocks().entrySet().stream().sorted(Map.Entry.<Position,String>comparingByKey().reversed()).forEach(e->blocks.put(e.getKey(),e.getValue()));
        var copy=new ConstructionTemplate(first.id(),1,first.name(),first.attribution(),first.purpose(),first.size(),
                List.of(new Stage(stage.id(),stage.work(),stage.materials(),blocks)),first.anchors());
        assertEquals(first.fingerprint(),copy.fingerprint());blocks.clear();assertEquals(2,copy.blockCount());
        assertThrows(UnsupportedOperationException.class,()->copy.stages().getFirst().blocks().clear());
        var changed=new ConstructionTemplate(first.id(),2,first.name(),first.attribution(),first.purpose(),first.size(),first.stages(),first.anchors());
        assertNotEquals(first.fingerprint(),changed.fingerprint());
        changed=new ConstructionTemplate(first.id(),1,first.name(),first.attribution(),first.purpose(),first.size(),
                List.of(new Stage(stage.id(),11,Map.of(Commodity.STONE,3L),stage.blocks())),first.anchors());
        assertNotEquals(first.fingerprint(),changed.fingerprint());
    }
    @Test void malformedOverlappingDangerousAndOversizeBlueprintsAreRejectedBeforePlanning() {
        var first=template();var stage=first.stages().getFirst();
        assertThrows(IllegalArgumentException.class,()->new ConstructionTemplate(first.id(),1,first.name(),first.attribution(),first.purpose(),first.size(),
                List.of(stage,new Stage("overlap",1,stage.materials(),stage.blocks())),first.anchors()));
        for(String state:List.of("minecraft:tnt","minecraft:air","minecraft:lava","minecraft:command_block","minecraft:stone\n"))
            assertThrows(IllegalArgumentException.class,()->new Stage("unsafe",1,Map.of(Commodity.STONE,1L),Map.of(new Position(0,0,0),state)));
        assertThrows(IllegalArgumentException.class,()->new Size(64,64,64));
        assertThrows(IllegalArgumentException.class,()->new ConstructionTemplate("missing_entrance",1,"Test","Test",Purpose.HOME,new Size(1,1,1),List.of(stage),List.of()));
        assertThrows(ArithmeticException.class,()->ConstructionPreview.requiredCells(first,Transform.identity(),new Position(Integer.MAX_VALUE,64,0)));
    }
}
