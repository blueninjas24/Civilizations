package com.blueninjas24.civilizations.industry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockWorkTest {
    final LivestockWork.Terms terms = new LivestockWork.Terms(2, 2, 3, 5, 2);
    @Test void renewableProductsConsumeRealFeedAndWorkWithoutKillingAnimals() {
        var plan = LivestockWork.plan(4, 8, 4, 100, 0, terms);
        assertEquals(4, plan.retainedAdults()); assertEquals(8, plan.feedUsed());
        assertEquals(4, plan.workUsed()); assertEquals(12, plan.renewableUnits()); assertEquals(0, plan.meatUnits());
    }
    @Test void requestedSlaughterCannotEatBreedingReserveOrExceedPeriodLimit() {
        var plan = LivestockWork.plan(6, 0, 100, 100, 100, terms);
        assertEquals(2, plan.slaughtered()); assertEquals(4, plan.retainedAdults());
        assertEquals(0, LivestockWork.plan(2, 0, 100, 100, 100, terms).slaughtered());
        assertEquals(1, LivestockWork.plan(3, 0, 100, 100, 100, terms).slaughtered());
    }
    @Test void fullStorageAndNoLaborCannotCreateGoodsOrConsumeAnimals() {
        for (var plan : new LivestockWork.Plan[] {
                LivestockWork.plan(6, 100, 100, 0, 100, terms), LivestockWork.plan(6, 100, 0, 100, 100, terms)}) {
            assertEquals(6, plan.retainedAdults()); assertEquals(0, plan.feedUsed());
            assertEquals(0, plan.renewableUnits()); assertEquals(0, plan.slaughtered());
        }
    }
    @Test void feedShortageDoesNotInventRenewableOutput() {
        assertEquals(0, LivestockWork.plan(6, 1, 6, 100, 0, terms).renewableUnits());
        assertEquals(3, LivestockWork.plan(6, 3, 6, 100, 0, terms).renewableUnits());
    }
    @Test void extremeInputsRemainBoundedWithoutMultiplicationOverflow() {
        var plan = LivestockWork.plan(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, terms);
        assertTrue(plan.feedUsed() >= 0); assertTrue(plan.renewableUnits() >= 0);
        assertTrue(plan.slaughtered() <= 2); assertTrue(plan.meatUnits() <= Long.MAX_VALUE - plan.renewableUnits());
    }
    @Test void smallScenarioSweepConservesAnimalsFeedWorkAndSpace() {
        for (int adults = 0; adults < 9; adults++) for (int feed = 0; feed < 9; feed++)
            for (int work = 0; work < 9; work++) for (int space = 0; space < 18; space++) {
                var p = LivestockWork.plan(adults, feed, work, space, 20, terms);
                assertEquals(adults, p.retainedAdults() + p.slaughtered());
                assertTrue(p.retainedAdults() >= Math.min(adults, terms.breedingReserve()));
                assertTrue(p.feedUsed() <= feed); assertTrue(p.workUsed() <= work);
                assertTrue(p.renewableUnits() + p.meatUnits() <= space); assertTrue(p.slaughtered() <= 2);
            }
    }
}
