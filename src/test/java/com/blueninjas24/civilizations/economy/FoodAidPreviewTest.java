package com.blueninjas24.civilizations.economy;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoodAidPreviewTest {
    private final UUID world = new UUID(0, 1), town = new UUID(0, 2), other = new UUID(0, 3);
    @Test void conversionsAreEditableExactAndImmutable() {
        var source = new HashMap<>(Map.of("BREAD", 3L)); var policy = new FoodAidPreview.Policy(true, 32, source);
        source.put("BREAD", 100L); assertEquals(192, policy.units("BREAD", 64));
        assertEquals(7, new FoodAidPreview.Policy(true, 32, Map.of("WHEAT", 7L)).units("WHEAT", 1));
        assertThrows(UnsupportedOperationException.class, () -> policy.unitsPerItem().clear());
    }
    @Test void invalidAmountsMappingsAndRangesFail() {
        var policy = FoodAidPreview.Policy.prototype();
        for (int count : new int[]{0, -1, 65}) assertThrows(IllegalArgumentException.class, () -> policy.units("BREAD", count));
        assertThrows(IllegalArgumentException.class, () -> policy.units("DIAMOND", 1));
        assertThrows(IllegalArgumentException.class, () -> new FoodAidPreview.Policy(true, 129, Map.of("BREAD", 3L)));
        assertThrows(IllegalArgumentException.class, () -> new FoodAidPreview.Policy(true, 32, Map.of("BREAD", 0L)));
        assertThrows(IllegalArgumentException.class, () -> new FoodAidPreview.Policy(true, 32, Map.of()));
    }
    @Test void nearestHomeUsesPermanentWorldAndThreeDimensionalRange() {
        var homes = List.of(new FoodAidPreview.Home(town, world, 32, 64, 0), new FoodAidPreview.Home(other, UUID.randomUUID(), 0, 64, 0));
        assertEquals(town, FoodAidPreview.recipient(world, 0, 64, 0, homes, 32, null).town());
        assertEquals(FoodAidPreview.Match.NONE, FoodAidPreview.recipient(world, 0, 63, 0, homes, 32, null).match());
        assertEquals(FoodAidPreview.Match.NONE, FoodAidPreview.recipient(world, 0, 64, 0, homes, 32, other).match());
    }
    @Test void tiesBetweenTownsRequireExplicitChoiceButHomesOfOneTownDoNot() {
        var a = new FoodAidPreview.Home(town, world, -1, 0, 0); var b = new FoodAidPreview.Home(other, world, 1, 0, 0);
        assertEquals(FoodAidPreview.Match.AMBIGUOUS, FoodAidPreview.recipient(world, 0, 0, 0, List.of(a,b), 32, null).match());
        assertEquals(other, FoodAidPreview.recipient(world, 0, 0, 0, List.of(a,b), 32, other).town());
        assertEquals(town, FoodAidPreview.recipient(world, 0, 0, 0, List.of(a,a), 32, null).town());
    }
    @Test void shuffledEvidenceNeverChangesClosestRecipient() {
        var homes = new ArrayList<>(List.of(new FoodAidPreview.Home(town,world,1,0,0),new FoodAidPreview.Home(other,world,2,0,0)));
        for (int seed=0; seed<1000; seed++) { Collections.shuffle(homes,new Random(seed)); assertEquals(town,FoodAidPreview.recipient(world,0,0,0,homes,32,null).town()); }
    }
    @Test void missingEvidenceAndInvalidCoordinatesCannotInventRecipients() {
        assertEquals(FoodAidPreview.Match.NONE,FoodAidPreview.recipient(world,0,0,0,List.of(),32,null).match());
        assertThrows(IllegalArgumentException.class,()->FoodAidPreview.recipient(world,Double.NaN,0,0,List.of(),32,null));
    }
}
