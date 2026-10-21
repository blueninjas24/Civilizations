package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ApartmentDeclaration.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApartmentDeclarationTest {
    private final UUID world=new UUID(0,1),town=new UUID(0,2),building=new UUID(0,3);
    private Bounds box(int x1,int z1,int x2,int z2){return new Bounds(new Position(x1,0,z1),new Position(x2,3,z2));}
    private Draft draft(){var b=box(0,0,8,4);return new Draft(building,town,world,b,b,new Position(4,1,0),List.of(),List.of())
            .shared(box(0,0,8,0)).unit("west",new Position(1,1,1),box(0,1,3,4))
            .unit("east",new Position(6,1,1),box(5,1,8,4));}
    private Map<Position,Cell> cells(){
        var c=new HashMap<Position,Cell>();for(int x=0;x<=8;x++)for(int y=0;y<=3;y++)for(int z=0;z<=4;z++)
            c.put(new Position(x,y,z),Cell.of(y==0||y==3?Type.SUPPORT:Type.PASSABLE));
        for(int x:List.of(1,6)){
            c.put(new Position(x,1,2),Cell.bed(false,"minecraft:white_bed",Direction.SOUTH));
            c.put(new Position(x,1,3),Cell.bed(true,"minecraft:white_bed",Direction.SOUTH));
        }return c;
    }
    private Observation frame(Map<Position,Cell> c){return new Observation(world,HomeDeclaration.regionHash(world,draft().structure()),1000,true,c);}
    @Test void explicitUnitsShareCorridorButKeepDistinctStableIdentityAndBeds(){
        var d=draft();var review=ApartmentDeclaration.review(d,frame(cells()),1000,Policy.prototype());
        assertTrue(review.passes());assertEquals(2,review.layout().units().size());assertEquals(2,review.assessment().accessibleBeds());
        assertTrue(review.layout().units().stream().allMatch(u->u.declaredBeds()==1));
        assertEquals(review.layout(),ApartmentDeclaration.review(d,frame(cells()),1000,Policy.prototype()).layout());
        var reversed=new Draft(d.building(),d.village(),d.world(),d.plot(),d.structure(),d.entrance(),d.shared(),d.units().reversed());
        assertEquals(review.layout(),ApartmentDeclaration.review(reversed,frame(cells()),1000,Policy.prototype()).layout());
        var missingCorridor=new Draft(d.building(),d.village(),d.world(),d.plot(),d.structure(),new Position(1,1,1),List.of(),d.units());
        var blocked=ApartmentDeclaration.review(missingCorridor,frame(cells()),1000,Policy.prototype());
        assertFalse(blocked.passes());assertEquals(1,blocked.assessment().accessibleBeds());
    }
    @Test void draftsAreImmutableAndRejectOverlapInvalidNamesAndSplitBedPairs(){
        var d=draft();assertThrows(IllegalArgumentException.class,()->d.unit("west",new Position(4,1,1),box(4,1,4,4)));
        assertThrows(IllegalArgumentException.class,()->d.room("west",box(3,1,4,4)));
        assertThrows(IllegalArgumentException.class,()->d.shared(box(0,0,0,1)));
        assertThrows(IllegalArgumentException.class,()->d.unit("Wrong Name",new Position(4,1,1),box(4,1,4,4)));
        assertThrows(IllegalArgumentException.class,()->d.plot(box(0,0,7,4)));
        var edited=d.remove("west").unit("west",new Position(1,1,1),box(0,1,3,2));
        assertEquals(2,d.units().size());assertThrows(IllegalArgumentException.class,()->ApartmentDeclaration.review(edited,frame(cells()),1000,Policy.prototype()));
        assertThrows(UnsupportedOperationException.class,()->d.units().clear());
    }
    @Test void unknownWrongWorldExpiredAndDamagedEvidenceCannotPassAllUnits(){
        var d=draft();var c=cells();c.remove(new Position(0,0,0));
        assertThrows(IllegalArgumentException.class,()->ApartmentDeclaration.review(d,frame(c),1000,Policy.prototype()));
        assertThrows(IllegalArgumentException.class,()->ApartmentDeclaration.review(d,frame(cells()),31_001,Policy.prototype()));
        assertThrows(IllegalArgumentException.class,()->ApartmentDeclaration.review(d,new Observation(UUID.randomUUID(),HomeDeclaration.regionHash(world,d.structure()),1000,true,cells()),1000,Policy.prototype()));
        var damaged=cells();damaged.put(new Position(6,3,2),Cell.of(Type.PASSABLE));
        assertFalse(ApartmentDeclaration.review(d,frame(damaged),1000,Policy.prototype()).passes());
    }
}
