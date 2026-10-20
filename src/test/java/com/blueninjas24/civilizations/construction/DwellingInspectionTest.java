package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DwellingInspectionTest {
    private final UUID town=new UUID(0,1),world=new UUID(0,2),building=new UUID(0,3),a=new UUID(0,11),b=new UUID(0,12);
    private static Position p(int x,int y,int z){return new Position(x,y,z);}
    private static Bounds box(int x,int y,int z,int xx,int yy,int zz){return new Bounds(p(x,y,z),p(xx,yy,zz));}
    private static Unit unit(UUID id,Bounds space,Position entrance,Position bed){return new Unit(id,entrance,List.of(space),List.of(new Bed(bed,Direction.EAST)));}
    private BuildingLayout layout() {
        var bounds=box(0,0,0,12,3,6);
        return new BuildingLayout(building,town,world,Kind.HOUSING,bounds,bounds,p(1,1,0),List.of(box(0,0,0,12,3,2)),List.of(
                unit(a,box(0,0,3,5,3,6),p(1,1,3),p(1,1,5)),unit(b,box(6,0,3,12,3,6),p(8,1,3),p(8,1,5))));
    }
    private Map<Position,Cell> cells(BuildingLayout layout) {
        var result=new HashMap<Position,Cell>();
        for(var pos:DwellingInspection.requiredCells(layout))result.put(pos,Cell.of(pos.y()==0||pos.y()==layout.structure().max().y()?Type.SUPPORT:Type.PASSABLE));
        for(var unit:layout.units())for(var bed:unit.beds()) {
            result.put(bed.foot(),Cell.bed(false,"minecraft:white_bed",bed.facing()));
            result.put(bed.head(),Cell.bed(true,"minecraft:white_bed",bed.facing()));
        }
        return result;
    }
    private Assessment inspect(BuildingLayout layout,Map<Position,Cell> cells) {
        return DwellingInspection.inspect(layout,new Observation(world,layout.fingerprint(),1000,true,cells),1000,5000);
    }
    @Test void realIntactAccessibleBedsCountPerUnitWithoutCreatingConsentOrOccupancy() {
        var layout=layout();var result=inspect(layout,cells(layout));
        assertEquals(2,result.accessibleBeds());assertTrue(result.issues().isEmpty());
        assertEquals(1,result.units().get(a).accessibleBeds());assertEquals(1,result.units().get(b).accessibleBeds());
        assertEquals(layout.fingerprint(),result.layoutHash());assertEquals(world,result.world());
        assertThrows(UnsupportedOperationException.class,()->result.units().clear());
    }

    @Test void unknownWrongWorldChangedAndStaleFramesNeverConvertMissingEvidenceToEmptyHousing() {
        var layout=layout();var original=cells(layout);var partial=new HashMap<>(original);partial.remove(p(0,0,0));
        var unknown=new HashMap<>(original);unknown.put(p(0,0,0),Cell.of(Type.UNKNOWN));
        for(var obs:List.of(new Observation(UUID.randomUUID(),layout.fingerprint(),1000,true,original),
                new Observation(world,"0".repeat(64),1000,true,original),new Observation(world,layout.fingerprint(),1001,true,original),
                new Observation(world,layout.fingerprint(),0,true,original),new Observation(world,layout.fingerprint(),1000,false,original),
                new Observation(world,layout.fingerprint(),1000,true,partial),new Observation(world,layout.fingerprint(),1000,true,unknown))) {
            var result=DwellingInspection.inspect(layout,obs,1000,500);
            assertEquals(0,result.accessibleBeds());assertFalse(result.issues().isEmpty());assertTrue(result.units().isEmpty());
        }
        assertEquals(2,inspect(layout,original).accessibleBeds());
    }

    @Test void oneBrokenBedsSupportOrHeadroomPausesOnlyThatUnit() {
        var layout=layout();
        for(var change:Map.of(p(2,1,5),Cell.of(Type.PASSABLE),p(1,0,5),Cell.of(Type.PASSABLE),
                p(1,2,5),Cell.of(Type.BLOCKED),p(1,1,5),Cell.bed(false,"minecraft:white_bed",Direction.WEST),
                p(2,2,5),Cell.of(Type.HAZARD)).entrySet()) {
            var cells=cells(layout);cells.put(change.getKey(),change.getValue());var result=inspect(layout,cells);
            assertEquals(0,result.units().get(a).accessibleBeds());assertEquals(1,result.units().get(b).accessibleBeds());
        }
        var cells=cells(layout);cells.put(p(2,1,5),Cell.bed(true,"minecraft:red_bed",Direction.EAST));
        assertTrue(inspect(layout,cells).units().get(a).issues().contains(Issue.BED_CHANGED));
    }

    @Test void extraPrivateBedsAreNotSilentlyCountedOrReclassifiedAsAnotherHome() {
        var layout=layout();var cells=cells(layout);
        cells.put(p(3,1,5),Cell.bed(false,"minecraft:white_bed",Direction.EAST));
        cells.put(p(4,1,5),Cell.bed(true,"minecraft:white_bed",Direction.EAST));
        var result=inspect(layout,cells);assertEquals(1,result.accessibleBeds());
        assertTrue(result.units().get(a).issues().contains(Issue.UNDECLARED_PRIVATE_BED));
    }
    @Test void bothBedHalvesNeedObservedOverheadCoverAndOnlyExplicitPolicyAllowsOutdoorBeds() {
        var layout=layout();var cells=cells(layout);var roof=p(1,3,5);cells.put(roof,Cell.of(Type.PASSABLE));
        var checked=inspect(layout,cells);assertTrue(checked.policy().requireCoveredBeds());
        assertEquals(0,checked.units().get(a).accessibleBeds());assertEquals(1,checked.units().get(b).accessibleBeds());
        assertTrue(checked.units().get(a).issues().contains(Issue.BED_UNCOVERED));
        var outdoor=DwellingInspection.inspect(layout,new Observation(world,layout.fingerprint(),1000,true,cells),1000,5000,new Policy(false));
        assertEquals(2,outdoor.accessibleBeds());assertFalse(outdoor.policy().requireCoveredBeds());
        // Partial roof shapes can shelter beds, but hazards/leaves/unknown blocked cells cannot.
        cells.put(roof,Cell.of(Type.COVER));assertEquals(2,inspect(layout,cells).accessibleBeds());
        for(var type:List.of(Type.BLOCKED,Type.HAZARD,Type.PASSABLE)) {
            cells.put(roof,Cell.of(type));assertTrue(inspect(layout,cells).units().get(a).issues().contains(Issue.BED_UNCOVERED));
        }
        cells.put(roof,Cell.of(Type.COVER));cells.put(p(2,3,5),Cell.of(Type.PASSABLE));
        assertTrue(inspect(layout,cells).units().get(a).issues().contains(Issue.BED_UNCOVERED));
        // Turning off cover never bypasses missing real beds or headroom checks.
        cells.put(p(1,2,5),Cell.of(Type.BLOCKED));
        outdoor=DwellingInspection.inspect(layout,new Observation(world,layout.fingerprint(),1000,true,cells),1000,5000,new Policy(false));
        assertEquals(0,outdoor.units().get(a).accessibleBeds());assertTrue(outdoor.units().get(a).issues().contains(Issue.BED_OBSTRUCTED));
    }

    @Test void accessCannotCrossAnotherHouseholdsPrivateRooms() {
        var bounds=box(0,0,0,12,3,6);
        var layout=new BuildingLayout(building,town,world,Kind.HOUSING,bounds,bounds,p(1,1,0),List.of(box(0,0,0,2,3,6)),List.of(
                unit(a,box(3,0,0,7,3,6),p(3,1,1),p(4,1,5)),unit(b,box(8,0,0,12,3,6),p(8,1,1),p(9,1,5))));
        var result=inspect(layout,cells(layout));assertEquals(1,result.units().get(a).accessibleBeds());
        assertEquals(0,result.units().get(b).accessibleBeds());assertTrue(result.units().get(b).issues().contains(Issue.UNIT_UNREACHABLE));
    }

    @Test void blockedOrHazardousBuildingEntranceAndTrappedBedsAreRejected() {
        var layout=layout();var cells=cells(layout);cells.put(layout.entrance(),Cell.of(Type.HAZARD));
        var result=inspect(layout,cells);assertEquals(0,result.accessibleBeds());assertTrue(result.issues().contains(Issue.ENTRANCE_UNSAFE));
        cells=cells(layout);
        for(var pos:List.of(p(0,1,5),p(1,1,4),p(1,1,6),p(2,1,4),p(2,1,6),p(3,1,5)))cells.put(pos,Cell.of(Type.BLOCKED));
        result=inspect(layout,cells);assertEquals(1,result.accessibleBeds());assertTrue(result.units().get(a).issues().contains(Issue.BED_UNREACHABLE));
    }

    @Test void upstairsHomesNeedSupportedStepsAndSharedLandingRatherThanVerticalFlight() {
        var bounds=box(0,0,0,12,6,6);
        var layout=new BuildingLayout(building,town,world,Kind.HOUSING,bounds,bounds,p(1,1,0),List.of(box(0,0,0,12,6,2)),List.of(
                unit(a,box(0,0,3,5,3,6),p(1,1,3),p(1,1,5)),unit(b,box(7,3,3,12,6,6),p(8,4,3),p(8,4,5))));
        var cells=cells(layout);
        for(int x=7;x<=12;x++)for(int z=3;z<=6;z++)cells.put(p(x,3,z),Cell.of(Type.SUPPORT));
        for(int x=4;x<=12;x++)for(int z=0;z<=2;z++)cells.put(p(x,3,z),Cell.of(Type.SUPPORT));
        cells.put(p(2,1,1),Cell.of(Type.SUPPORT));cells.put(p(3,2,1),Cell.of(Type.SUPPORT));
        assertEquals(2,inspect(layout,cells).accessibleBeds());
        cells.put(p(2,1,1),Cell.of(Type.PASSABLE));cells.put(p(3,2,1),Cell.of(Type.PASSABLE));
        assertEquals(0,inspect(layout,cells).units().get(b).accessibleBeds());
    }

    @Test void extremeCoordinatesAndOversizedObservationsFailSafely() {
        var bounds=box(Integer.MAX_VALUE-3,Integer.MAX_VALUE-3,0,Integer.MAX_VALUE,Integer.MAX_VALUE,3);
        var unit=unit(a,bounds,p(Integer.MAX_VALUE-3,Integer.MAX_VALUE,0),p(Integer.MAX_VALUE-2,Integer.MAX_VALUE,1));
        var layout=new BuildingLayout(building,town,world,Kind.HOUSING,bounds,bounds,unit.entrance(),List.of(),List.of(unit));
        assertEquals(0,inspect(layout,cells(layout)).accessibleBeds());
        var cells=new HashMap<Position,Cell>();for(int i=0;i<=BuildingLayout.MAX_STRUCTURE_VOLUME;i++)cells.put(p(i,0,0),Cell.of(Type.PASSABLE));
        assertThrows(IllegalArgumentException.class,()->new Observation(world,"0".repeat(64),1000,true,cells));
    }
}
