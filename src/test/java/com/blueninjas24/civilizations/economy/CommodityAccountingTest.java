package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.blueninjas24.civilizations.economy.Commodity.*;

class CommodityAccountingTest {
    // Illustrative test inputs, not chosen gameplay defaults.
    private final Policy policy = new Policy(5000, 12500, 20000, 3);
    private State state(long food) { return new State(0, Map.of(FOOD, new Stock(food, 0))); }
    private Flow demand(long production, long imports, long demand) { return new Flow(production, imports, demand, 0, 0, 0, 0); }
    @Test void drawsReservesBeforeUnmetDemandAndExplainsEveryQuantity() {
        var result = CommodityAccounting.step(state(20), Map.of(FOOD, new Flow(2, 3, 10, 4, 5, 3, 2)), policy);
        var report = result.reports().getFirst();
        assertEquals(25, report.available()); assertEquals(24, report.demand()); assertEquals(24, report.fulfilled());
        assertEquals(0, report.unmet()); assertEquals(new Stock(1, 0), report.after()); assertEquals(Condition.BALANCED, report.condition());
        assertTrue(report.explanation().contains("reserve 20, produced 2, imported 3"));
    }
    @Test void prolongedCommoditySpecificDeficitsEscalateAndRecoveryClearsDuration() {
        State before = new State(0, Map.of(FOOD, new Stock(0, 0), TIMBER, new Stock(20, 0)));
        for (int i = 1; i <= 3; i++) {
            var result = CommodityAccounting.step(before, Map.of(FOOD, demand(5, 0, 10), TIMBER, Flow.idle()), policy);
            var food = result.reports().stream().filter(r -> r.commodity().equals(FOOD)).findFirst().orElseThrow();
            assertEquals(i < 3 ? Condition.SHORTAGE : Condition.SEVERE_SHORTAGE, food.condition());
            assertEquals(5, food.unmet()); assertEquals(i, food.after().shortagePeriods());
            assertEquals(new Stock(20, 0), result.state().stocks().get(TIMBER)); before = result.state();
        }
        var recovery = CommodityAccounting.step(before, Map.of(FOOD, demand(0, 10, 10), TIMBER, Flow.idle()), policy);
        assertEquals(new Stock(0, 0), recovery.state().stocks().get(FOOD));
    }
    @Test void smallDeficitDoesNotBecomeSevereJustBecauseTimePasses() {
        var result = CommodityAccounting.step(new State(30, Map.of(FOOD, new Stock(0, 30))), Map.of(FOOD, demand(9, 0, 10)), policy);
        assertEquals(Condition.SHORTAGE, result.reports().getFirst().condition());
    }
    @Test void coverageBandsIncludeTheirBoundaryAndIdleIsNotInventedAbundance() {
        assertEquals(Condition.BALANCED, CommodityAccounting.step(state(0), Map.of(FOOD, demand(10, 0, 10)), policy).reports().getFirst().condition());
        assertEquals(Condition.SURPLUS, CommodityAccounting.step(state(25), Map.of(FOOD, demand(0, 0, 20)), policy).reports().getFirst().condition());
        assertEquals(Condition.ABUNDANT, CommodityAccounting.step(state(40), Map.of(FOOD, demand(0, 0, 20)), policy).reports().getFirst().condition());
        assertEquals(Condition.BALANCED, CommodityAccounting.step(state(40), Map.of(FOOD, Flow.idle()), policy).reports().getFirst().condition());
    }
    @Test void invalidAmountsPoliciesAndIncompleteEvidenceFailExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> new Stock(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> demand(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Policy(10000, 12500, 20000, 3));
        assertThrows(IllegalArgumentException.class, () -> CommodityAccounting.step(state(1), Map.of(), policy));
        assertThrows(IllegalArgumentException.class, () -> CommodityAccounting.step(state(1), Map.of(FOOD, Flow.idle(), IRON, Flow.idle()), policy));
        assertThrows(IllegalArgumentException.class, () -> new Commodity("Food"));
    }
    @Test void quantityOverflowFailsRatherThanCreatingOrDestroyingGoodsAndRatioComparisonRemainsExact() {
        assertThrows(ArithmeticException.class, () -> CommodityAccounting.step(state(Long.MAX_VALUE), Map.of(FOOD, demand(1, 0, 1)), policy));
        assertThrows(ArithmeticException.class, () -> new Flow(0, 0, Long.MAX_VALUE, 1, 0, 0, 0).demand());
        var result = CommodityAccounting.step(state(Long.MAX_VALUE), Map.of(FOOD, demand(0, 0, Long.MAX_VALUE)), policy);
        assertEquals(Condition.BALANCED, result.reports().getFirst().condition()); assertEquals(0, result.reports().getFirst().after().units());
    }
    @Test void callerMapOrderDoesNotChangeResultsAndSnapshotsAreImmutable() {
        var first = new LinkedHashMap<Commodity, Stock>(); first.put(TIMBER, new Stock(7, 0)); first.put(FOOD, new Stock(3, 0));
        var state = new State(0, first); first.clear();
        var flows = Map.of(TIMBER, Flow.idle(), FOOD, Flow.idle());
        assertEquals(CommodityAccounting.step(state, flows, policy), CommodityAccounting.step(new State(0, state.stocks()), flows, policy));
        assertThrows(UnsupportedOperationException.class, () -> state.stocks().clear());
        assertEquals(FOOD, CommodityAccounting.step(state, flows, policy).reports().getFirst().commodity());
    }
    @Test void centuryScenarioConservesSupplyThroughCrisisAndRecoveryWithoutPopulationEffects() {
        var before = state(30); long added = 30, fulfilled = 0;
        for (int period = 1; period <= 1200; period++) {
            long output = period >= 100 && period <= 120 ? 0 : 10;
            long imported = period == 121 ? 30 : 0;
            var result = CommodityAccounting.step(before, Map.of(FOOD, demand(output, imported, 10)), policy);
            var report = result.reports().getFirst(); added += output + imported; fulfilled += report.fulfilled();
            assertEquals(added, fulfilled + report.after().units());
            if (period == 105) assertEquals(Condition.SEVERE_SHORTAGE, report.condition());
            if (period == 121) assertEquals(0, report.after().shortagePeriods());
            before = result.state();
        }
        assertEquals(1200, before.completedPeriods()); assertEquals(30, before.stocks().get(FOOD).units());
    }
}
