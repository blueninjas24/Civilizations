package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuildingLayoutTest {
    private final UUID building = new UUID(0, 1), settlement = new UUID(0, 2), world = new UUID(0, 3);
    private final Bounds plot = box(-5, -1, -5, 15, 9, 15), structure = box(0, 0, 0, 10, 5, 10);
    private static Position p(int x, int y, int z) { return new Position(x, y, z); }
    private static Bounds box(int x, int y, int z, int xx, int yy, int zz) { return new Bounds(p(x,y,z), p(xx,yy,zz)); }
    private static Unit unit(long id, Bounds space) {
        return new Unit(new UUID(0, id), space.min(), List.of(space),
                List.of(new Bed(space.min(), Direction.EAST)));
    }
    private BuildingLayout layout(List<Bounds> shared, List<Unit> units) {
        return new BuildingLayout(building, settlement, world, Kind.HOUSING, plot, structure, p(0,0,0), shared, units);
    }
    private BuildingLayout apartment() {
        return layout(List.of(box(0,0,0,0,5,10)), List.of(
                unit(11, box(1,0,0,10,2,10)), unit(12, box(1,3,0,10,5,10))));
    }

    @Test void stackedApartmentsHaveSeparateIdentitiesAndSharedAccessNotExtraHomes() {
        var layout = apartment();
        assertEquals(2, layout.units().size()); assertEquals(2, layout.declaredBeds());
        assertTrue(layout.plot().contains(layout.structure()));
        assertFalse(layout.structure().contains(layout.plot()));
        assertFalse(layout.units().get(0).spaces().getFirst().overlaps(layout.units().get(1).spaces().getFirst()));
    }

    @Test void multipleRoomsAndBedsCanRemainOneHouseholdHome() {
        var rooms = List.of(box(0,0,0,3,2,10), box(4,0,0,10,2,10));
        var home = new Unit(new UUID(0,11), p(0,0,0), rooms,
                List.of(new Bed(p(0,0,0),Direction.EAST), new Bed(p(4,0,0),Direction.EAST)));
        var layout = layout(List.of(), List.of(home));
        assertEquals(1, layout.units().size()); assertEquals(2, layout.declaredBeds());
    }

    @Test void privateSpaceCannotOverlapAnotherUnitOrSharedCorridor() {
        var a = unit(11,box(1,0,0,6,2,10));
        var b = unit(12,box(6,0,0,10,2,10));
        assertThrows(IllegalArgumentException.class, () -> layout(List.of(box(0,0,0,0,5,10)),List.of(a,b)));
        assertThrows(IllegalArgumentException.class, () -> layout(List.of(box(0,0,0,2,5,10)),List.of(a)));
        assertThrows(IllegalArgumentException.class, () -> new Unit(a.id(),a.entrance(),List.of(a.spaces().getFirst(),a.spaces().getFirst()),a.beds()));
    }

    @Test void bedPairsMustFitPrivateSpaceAndCannotShareEitherHalf() {
        Bounds room = box(0,0,0,2,2,2);
        assertThrows(IllegalArgumentException.class, () -> new Unit(building,p(0,0,0),List.of(room),List.of(new Bed(p(2,0,0),Direction.EAST))));
        assertThrows(IllegalArgumentException.class, () -> new Unit(building,p(0,0,0),List.of(room),List.of(
                new Bed(p(0,0,0),Direction.EAST),new Bed(p(1,0,0),Direction.EAST))));
        assertThrows(ArithmeticException.class, () -> new Bed(p(Integer.MAX_VALUE,0,0),Direction.EAST));
    }

    @Test void identityEntrancesAndOuterBoundsAreValidated() {
        var a = apartment(); var first = a.units().getFirst();
        assertThrows(IllegalArgumentException.class, () -> layout(a.shared(),List.of(first,first)));
        assertThrows(IllegalArgumentException.class, () -> new Unit(first.id(),p(-1,0,0),first.spaces(),first.beds()));
        assertThrows(IllegalArgumentException.class, () -> new BuildingLayout(building,settlement,world,Kind.HOUSING,
                box(0,0,0,1,1,1),structure,p(0,0,0),a.shared(),a.units()));
        assertThrows(IllegalArgumentException.class, () -> layout(List.of(),List.of(unit(11,box(1,0,0,10,2,10)))));
        assertThrows(IllegalArgumentException.class, () -> layout(List.of(),List.of(unit(11,box(0,0,0,11,2,10)))));
    }

    @Test void housingNeedsDeclaredUnitsButWorkplacesDoNotInventHouseholds() {
        assertThrows(IllegalArgumentException.class, () -> layout(List.of(),List.of()));
        var shop = new BuildingLayout(building,settlement,world,Kind.WORKPLACE,plot,structure,p(0,0,0),List.of(),List.of());
        assertEquals(0,shop.declaredBeds());
        assertThrows(IllegalArgumentException.class, () -> new BuildingLayout(building,settlement,world,Kind.WORKPLACE,
                plot,structure,p(0,0,0),List.of(),apartment().units()));
    }

    @Test void boundedGeometryRejectsOverflowAndOversizedObservations() {
        assertThrows(IllegalArgumentException.class, () -> box(Integer.MIN_VALUE,0,0,Integer.MAX_VALUE,1,1));
        assertThrows(IllegalArgumentException.class, () -> box(1,0,0,0,1,1));
        var huge = box(0,0,0,511,511,511);
        assertEquals(134_217_728L,huge.volume());
        assertThrows(IllegalArgumentException.class, () -> new BuildingLayout(building,settlement,world,Kind.CIVIC,
                huge,huge,p(0,0,0),List.of(),List.of()));
    }

    @Test void snapshotRoundTripIsCanonicalImmutableAndBoundToWorldAndUnitIdentity() throws Exception {
        var original = apartment(); var reversed = new ArrayList<>(original.units()); Collections.reverse(reversed);
        var same = layout(original.shared(),reversed); reversed.clear();
        assertEquals(original,same); assertEquals(original.fingerprint(),same.fingerprint());
        byte[] bytes = BuildingLayoutSnapshot.encode(original);
        assertEquals(original,BuildingLayoutSnapshot.decode(bytes));
        assertArrayEquals(bytes,BuildingLayoutSnapshot.encode(same));
        assertThrows(UnsupportedOperationException.class, () -> same.units().clear());
        assertThrows(UnsupportedOperationException.class, () -> same.units().getFirst().beds().clear());
        var otherWorld = new BuildingLayout(building,settlement,new UUID(0,99),Kind.HOUSING,plot,structure,
                original.entrance(),original.shared(),original.units());
        assertNotEquals(original.fingerprint(),otherWorld.fingerprint());
    }

    @Test void malformedSnapshotsNeverBecomeHousing() throws Exception {
        byte[] bytes = BuildingLayoutSnapshot.encode(apartment());
        for (int length : List.of(0,3,20,bytes.length-1))
            assertThrows(IOException.class, () -> BuildingLayoutSnapshot.decode(Arrays.copyOf(bytes,length)));
        byte[] version = bytes.clone(); version[3] = 2;
        assertThrows(IOException.class, () -> BuildingLayoutSnapshot.decode(version));
        assertThrows(IOException.class, () -> BuildingLayoutSnapshot.decode(Arrays.copyOf(bytes,bytes.length+1)));
        assertThrows(IOException.class, () -> BuildingLayoutSnapshot.decode(new byte[BuildingLayoutSnapshot.MAX_BYTES+1]));
    }
}
