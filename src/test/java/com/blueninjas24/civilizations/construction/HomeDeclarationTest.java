package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HomeDeclarationTest {
    private final UUID world=new UUID(0,1),village=new UUID(0,2),building=new UUID(0,3);
    private final Bounds bounds=new Bounds(new Position(0,0,0),new Position(5,3,5));
    private final Position entrance=new Position(2,1,0);
    private Map<Position,Cell> cells() {
        var cells=new HashMap<Position,Cell>();
        for(int x=0;x<=5;x++)for(int y=0;y<=3;y++)for(int z=0;z<=5;z++)cells.put(new Position(x,y,z),Cell.of(y==0||y==3?Type.SUPPORT:Type.PASSABLE));
        for(int x:List.of(1,3)) {
            cells.put(new Position(x,1,2),Cell.bed(false,"minecraft:white_bed",Direction.SOUTH));
            cells.put(new Position(x,1,3),Cell.bed(true,"minecraft:white_bed",Direction.SOUTH));
        }
        return cells;
    }
    private Observation frame(Map<Position,Cell> cells){return new Observation(world,HomeDeclaration.regionHash(world,bounds),1000,true,cells);}
    private HomeDeclaration.Review review(Observation o){return HomeDeclaration.singleHousehold(building,village,world,bounds,bounds,entrance,o,1000,Policy.prototype());}
    @Test void explicitSingleHomeKeepsAllBedsInOneStableUnitAndRebindsEvidenceToExactLayout() {
        var r=review(frame(cells()));assertTrue(r.passes());assertEquals(1,r.layout().units().size());assertEquals(2,r.assessment().accessibleBeds());
        assertEquals(r.layout(),review(frame(cells())).layout());assertEquals(r.layout().fingerprint(),r.assessment().layoutHash());
        assertNotEquals(HomeDeclaration.regionHash(world,bounds),r.assessment().layoutHash());
        var changed=cells();changed.put(new Position(1,3,2),Cell.of(Type.PASSABLE));
        assertFalse(review(frame(changed)).passes());assertEquals(r.layout(),review(frame(changed)).layout());
        var wrong=new BuildingLayout(UUID.randomUUID(),village,world,Kind.HOUSING,bounds,bounds,entrance,List.of(),r.layout().units());
        assertThrows(IllegalArgumentException.class,()->new HomeDeclaration.Review(wrong,r.assessment()));
    }
    @Test void partialOldWrongRegionAndOrphanedBedsNeverBecomeReadyForDonation() {
        var cells=cells();cells.remove(new Position(0,0,0));assertThrows(IllegalArgumentException.class,()->review(frame(cells)));
        assertThrows(IllegalArgumentException.class,()->review(new Observation(world,"0".repeat(64),1000,true,cells())));
        assertThrows(IllegalArgumentException.class,()->review(new Observation(UUID.randomUUID(),HomeDeclaration.regionHash(world,bounds),1000,true,cells())));
        assertThrows(IllegalArgumentException.class,()->review(new Observation(world,HomeDeclaration.regionHash(world,bounds),1001,true,cells())));
        var orphan=cells();orphan.put(new Position(1,1,3),Cell.of(Type.PASSABLE));assertFalse(review(frame(orphan)).passes());
        var none=cells();none.replaceAll((p,c)->c.type()==Type.BED_FOOT||c.type()==Type.BED_HEAD?Cell.of(Type.PASSABLE):c);
        assertThrows(IllegalArgumentException.class,()->review(frame(none)));
        assertThrows(IllegalArgumentException.class,()->HomeDeclaration.regionHash(world,new Bounds(new Position(0,0,0),new Position(511,511,511))));
    }
}
