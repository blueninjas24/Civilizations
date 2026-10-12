package com.blueninjas24.civilizations.industry;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishStockTest {
    private static final FishingPolicy POLICY = FishingPolicy.prototype();

    @Test void openingNeedsCompleteSuitableHabitatAndHasBoundedAbstractCapacity() {
        assertEquals(new FishStock(320, 240, 0), POLICY.opening(40, true));
        assertEquals(4096, POLICY.opening(4096, true).capacity());
        assertThrows(IllegalArgumentException.class, () -> POLICY.opening(15, true));
        assertThrows(IllegalArgumentException.class, () -> POLICY.opening(40, false));
        assertThrows(IllegalArgumentException.class, () -> POLICY.opening(4097, true));
        assertThrows(IllegalArgumentException.class, () -> FishingPolicy.disabled().opening(40, true));
    }

    @Test void recoveryAndCatchAreSeparateConservedChanges() {
        var result = POLICY.opening(40, true).advance(POLICY, true, 100, 3, 100, 1);
        assertEquals(6, result.recovered());
        assertEquals(12, result.caught());
        assertEquals(new FishStock(320, 234, 4000), result.stock());
        assertEquals(12, result.storageSpace());
        assertTrue(result.explanation().contains("240+6-12=234/320"));
    }

    @Test void catchRequiresActualLaborRequestSiteAndStorage() {
        var stock = new FishStock(320, 300, 0);
        assertEquals(0, stock.advance(POLICY, true, 100, 0, 100, 1).caught());
        assertEquals(0, stock.advance(POLICY, true, 0, 100, 100, 1).caught());
        assertEquals(16, stock.advance(POLICY, true, 100, 100, 100, 1).caught());
        assertEquals(2, stock.advance(POLICY, true, 100, 100, 7, 3).caught());
        assertEquals(0, stock.advance(POLICY, true, 100, 100, 0, 1).caught());
    }

    @Test void fractionalRecoveryAccumulatesWithoutRoundingAwaySmallHabitats() {
        var slow = new FishingPolicy(true, 1, 1, 100, 0, 1, 1, 1);
        var stock = slow.opening(1, true);
        for (int i = 0; i < 9999; i++) stock = stock.advance(slow, true, 0, 0, 0, 1).stock();
        assertEquals(new FishStock(1, 0, 9999), stock);
        assertEquals(new FishStock(1, 1, 0), stock.advance(slow, true, 0, 0, 0, 1).stock());
    }

    @Test void overfishingExhaustsStockAndRestingLetsItRecover() {
        var stock = POLICY.opening(40, true);
        for (int i = 0; i < 100; i++) stock = stock.advance(POLICY, true, 100, 4, 100, 1).stock();
        assertEquals(0, stock.available());
        var depleted = stock.advance(POLICY, true, 100, 4, 100, 1);
        assertTrue(depleted.caught() < POLICY.siteCatchCapacity());
        stock = depleted.stock();
        for (int i = 0; i < 50; i++) stock = stock.advance(POLICY, true, 0, 0, 0, 1).stock();
        assertEquals(320, stock.available());
    }

    @Test void fullHabitatNeverBanksRecoveryAndWorldAbsenceIsExactPause() {
        var full = new FishStock(320, 320, 0);
        assertEquals(full, full.advance(POLICY, true, 0, 0, 0, 1).stock());
        var caught = full.advance(POLICY, true, 16, 4, 100, 1);
        assertEquals(0, caught.recovered());
        assertEquals(0, caught.stock().recoveryRemainder());
        var stock = new FishStock(320, 10, 9999);
        for (int i = 0; i < 1000; i++) assertEquals(stock, stock.advance(POLICY, false, 100, 4, 100, 1).stock());
        assertEquals(stock, stock.advance(FishingPolicy.disabled(), true, 100, 4, 100, 1).stock());
    }

    @Test void retuningOpeningTermsDoesNotRefillExistingStock() {
        var changed = new FishingPolicy(true, 1, 1000, 1_000_000, 100, 200, 4, 16);
        var result = new FishStock(320, 0, 0).advance(changed, true, 0, 0, 0, 1);
        assertEquals(new FishStock(320, 6, 4000), result.stock());
        var famine = new FishingPolicy(true, 16, 8, 4096, 0, 0, 4, 16);
        assertEquals(0, famine.opening(40, true).advance(famine, true, 100, 4, 100, 1).caught());
    }

    @Test void seededTenThousandPeriodsConserveStockWithinAllWorkLimits() {
        var random = new Random(731);
        var stock = POLICY.opening(40, true);
        for (int i = 0; i < 10000; i++) {
            boolean active = random.nextBoolean();
            int workers = random.nextInt(10);
            long requested = random.nextInt(50), space = random.nextInt(50);
            var result = stock.advance(POLICY, active, requested, workers, space, 2);
            assertEquals(stock.available() + result.recovered() - result.caught(), result.stock().available());
            assertTrue(result.stock().available() >= 0 && result.stock().available() <= stock.capacity());
            assertTrue(result.caught() <= requested && result.caught() <= workers * 4L && result.caught() <= 16);
            assertEquals(result.caught() * 2, result.storageSpace());
            assertTrue(result.storageSpace() <= space);
            if (!active) assertEquals(stock, result.stock());
            stock = result.stock();
        }
    }

    @Test void invalidTermsStockAndWorkFailBeforeAnyTransition() {
        assertThrows(IllegalArgumentException.class, () -> new FishStock(1, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> new FishStock(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new FishStock(1, 0, 10000));
        assertThrows(IllegalArgumentException.class, () -> new FishingPolicy(true, 16, 8, 127, 75, 200, 4, 16));
        assertThrows(IllegalArgumentException.class, () -> new FishingPolicy(true, 16, 8, 4096, 101, 200, 4, 16));
        assertThrows(IllegalArgumentException.class, () -> new FishingPolicy(true, 16, 8, 4096, 75, 10001, 4, 16));
        var stock = POLICY.opening(40, true);
        assertThrows(IllegalArgumentException.class, () -> stock.advance(POLICY, true, -1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> stock.advance(POLICY, true, 1, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> stock.advance(POLICY, true, 1, 1, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> stock.advance(POLICY, true, 1, 1, 1, 0));
    }
}
