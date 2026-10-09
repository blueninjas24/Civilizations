package com.blueninjas24.civilizations.industry;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MineralObservationTest {
    @Test void observationOrderIsCanonicalAndCoordinatesImmutable() {
        var a=new Position(-1,-64,0);var b=new Position(2,40,0);var world=UUID.randomUUID();
        assertEquals(new MineralObservation(world,1,true,Commodity.COAL,List.of(a,b)),new MineralObservation(world,1,true,Commodity.COAL,List.of(b,a)));
        assertThrows(UnsupportedOperationException.class,()->new MineralObservation(world,1,true,Commodity.COAL,List.of(a,b)).ore().clear());
    }
    @Test void duplicatesAndUnboundedOrUnsupportedSurveysAreRejected() {
        var p=new Position(0,0,0);var world=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new MineralObservation(world,1,true,Commodity.COAL,List.of(p,p)));
        assertThrows(IllegalArgumentException.class,()->new MineralObservation(world,1,true,Commodity.FOOD,List.of(p)));
        assertThrows(IllegalArgumentException.class,()->new MineralObservation(world,-1,true,Commodity.COAL,List.of(p)));
        assertThrows(IllegalArgumentException.class,()->new MineralObservation(world,1,true,Commodity.COAL,java.util.stream.IntStream.range(0,513).mapToObj(x->new Position(x,0,0)).toList()));
    }
    @Test void unknownAndEmptyEvidenceDoNotClaimDepletion() {
        assertFalse(new MineralObservation(UUID.randomUUID(),1,false,Commodity.IRON,List.of()).complete());
        assertTrue(new MineralObservation(UUID.randomUUID(),1,true,Commodity.IRON,List.of()).ore().isEmpty());
    }
}
