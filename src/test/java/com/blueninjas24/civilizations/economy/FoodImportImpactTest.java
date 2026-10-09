package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoodImportImpactTest {
    private final Policy policy = new Policy(5000, 12500, 20000, 3);
    private Report report(long opening, long production, long imports, long demand) {
        return CommodityAccounting.step(new State(0, Map.of(Commodity.FOOD, new Stock(opening, 0))),
                Map.of(Commodity.FOOD, new Flow(production, imports, demand, 0, 0, 0, 0)), policy).reports().getFirst();
    }
    @Test void importsRelieveOnlyTheirMarginalShareAndMayLeaveUnmetDemand() {
        assertEquals(new FoodImportImpact.Outcome(3, 8, 5, 3, 0), FoodImportImpact.assess(report(1, 1, 3, 10)));
    }
    @Test void excessDeliveryBothPreventsShortageAndAddsReserves() {
        assertEquals(new FoodImportImpact.Outcome(12, 4, 0, 4, 8), FoodImportImpact.assess(report(0, 0, 12, 4)));
    }
    @Test void localProductionRecoveryIsNotCreditedToImports() {
        assertEquals(new FoodImportImpact.Outcome(3, 0, 0, 0, 3), FoodImportImpact.assess(report(0, 10, 3, 10)));
    }
    @Test void openingReservesAndZeroDemandDoNotManufactureRelief() {
        assertEquals(0, FoodImportImpact.assess(report(10, 0, 3, 10)).avoidedUnmet());
        assertEquals(new FoodImportImpact.Outcome(3, 0, 0, 0, 3), FoodImportImpact.assess(report(0, 0, 3, 0)));
    }
    @Test void noImportsNeverReceivesCreditEvenDuringFamine() {
        assertEquals(new FoodImportImpact.Outcome(0, 10, 10, 0, 0), FoodImportImpact.assess(report(0, 0, 0, 10)));
    }
    @Test void exactMaximumQuantitiesDoNotOverflowThroughDoubleCounting() {
        assertEquals(new FoodImportImpact.Outcome(Long.MAX_VALUE, Long.MAX_VALUE, 0, Long.MAX_VALUE, 0),
                FoodImportImpact.assess(report(0, 0, Long.MAX_VALUE, Long.MAX_VALUE)));
        assertEquals(Long.MAX_VALUE - 1, FoodImportImpact.assess(report(0, 0, Long.MAX_VALUE, 1)).additionalReserve());
    }
    @Test void contradictoryReportsWrongCommoditiesAndImpossibleOutcomesAreRejected() {
        var valid = report(0, 0, 12, 4);
        assertThrows(IllegalArgumentException.class, () -> FoodImportImpact.assess(new Report(Commodity.TIMBER,
                valid.before(), valid.flow(), valid.available(), valid.demand(), valid.fulfilled(), valid.unmet(), valid.after(), valid.condition())));
        assertThrows(IllegalArgumentException.class, () -> FoodImportImpact.assess(new Report(Commodity.FOOD,
                valid.before(), valid.flow(), valid.available(), valid.demand(), valid.fulfilled(), 1, valid.after(), valid.condition())));
        assertThrows(IllegalArgumentException.class, () -> new FoodImportImpact.Outcome(12, 4, 0, 4, 12));
        assertThrows(IllegalArgumentException.class, () -> new FoodImportImpact.Outcome(-1, 0, 0, 0, 0));
    }
    @Test void otherFoodUsesAreIncludedWithoutInventingResidentMeals() {
        var result = CommodityAccounting.step(new State(0, Map.of(Commodity.FOOD, new Stock(0, 0))),
                Map.of(Commodity.FOOD, new Flow(0, 10, 2, 3, 1, 2, 1)), policy);
        assertEquals(new FoodImportImpact.Outcome(10, 9, 0, 9, 1), FoodImportImpact.assess(result.reports().getFirst()));
    }
    @Test void seededMultiCenturyPeriodsConserveEachImportWithoutCreditingItAgainLater() {
        var random = new SplittableRandom(27419);
        var state = new State(0, Map.of(Commodity.FOOD, new Stock(0, 0)));
        long received = 0, prevented = 0, reserve = 0;
        for (int period = 0; period < 2400; period++) {
            long imported = period % 17 == 0 ? random.nextLong(25) : 0;
            var result = CommodityAccounting.step(state, Map.of(Commodity.FOOD,
                    new Flow(random.nextLong(7), imported, 8, 0, 0, 0, 0)), policy);
            var food = result.reports().getFirst();
            var impact = FoodImportImpact.assess(food);
            assertEquals(imported, impact.avoidedUnmet() + impact.additionalReserve());
            assertTrue(impact.avoidedUnmet() <= food.fulfilled());
            if (imported == 0) assertEquals(0, impact.avoidedUnmet());
            received += imported; prevented += impact.avoidedUnmet(); reserve += impact.additionalReserve();
            state = result.state();
        }
        assertEquals(received, prevented + reserve);
        assertTrue(prevented > 0); assertTrue(reserve > 0);
    }
}
