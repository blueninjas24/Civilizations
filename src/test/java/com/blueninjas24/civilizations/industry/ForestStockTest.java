package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForestStockTest {
    @Test void suitableTreesGiveAbstractCapacityAndHarvestNeedsWorkAndStorage() {
        var evidence = new ForestObservation(UUID.randomUUID(), 100, true,
                java.util.stream.IntStream.range(0, 6).mapToObj(y -> new Position(0, y, 0)).toList());
        var policy = ForestryPolicy.prototype();
        var opening = policy.opening(evidence);
        assertEquals(96, opening.capacity());
        var work = opening.advance(policy, true, 100, 1, 6, 2);
        assertEquals(3, work.harvested());
        assertEquals(6, work.storageSpace());
        assertEquals(93, work.stock().available());
        assertEquals(0, opening.advance(policy, true, 100, 0, 100, 2).harvested());
        assertEquals(0, opening.advance(policy, true, 0, 1, 100, 2).harvested());
        assertThrows(IllegalArgumentException.class, () -> policy.opening(
                new ForestObservation(evidence.world(), 100, false, evidence.trunks())));
    }

    @Test void fractionsRecoverGraduallyOnlyInPresentWorldAndNeverBankBeyondCapacity() {
        var policy = new ForestryPolicy(true, 1, 1, 100, 100, 2500, 4, 16);
        var stock = new ForestStock(3, 0, 0);
        assertEquals(stock, stock.advance(policy, false, 10, 1, 100, 1).stock());
        for (int period = 0; period < 4; period++) stock = stock.advance(policy, true, 0, 0, 100, 1).stock();
        assertEquals(new ForestStock(3, 3, 0), stock);
        assertEquals(stock, stock.advance(policy, true, 0, 0, 100, 1).stock());
        var harvested = stock.advance(policy, true, 10, 1, 100, 1);
        assertEquals(3, harvested.harvested());
        assertEquals(new ForestStock(3, 0, 0), harvested.stock());
    }

    @Test void confirmedHabitatLossDoesNotRestoreStockWhenSupportsReturn() {
        var opening = new ForestStock(96, 80, 50);
        var loss = opening.habitat(96, 6, 3);
        assertEquals(32, loss.lost());
        assertEquals(new ForestStock(48, 48, 0), loss.after());
        var restored = loss.after().habitat(96, 6, 6);
        assertEquals(48, restored.after().available());
        assertEquals(96, restored.after().capacity());
        assertEquals(0, restored.lost());
        assertEquals(new ForestStock(0, 0, 0), restored.after().habitat(96, 6, 0).after());
    }
}
