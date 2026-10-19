package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Same people, seed, buyers and budget; only observed geography changes. Not a population-year simulation. */
class ResourceGeographyScenarioTest {
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"coal", "stone", "neither"})
    void geographyControlsPaidStartupAndLongRunNeverRefillsFiniteSources(String geology) throws Exception {
        var scenario = new ResourceInvestmentTest();
        scenario.directory = directory;
        try {
            scenario.setup();
            var f = scenario.fixture;
            var original = scenario.policy;
            // Affordable real customers can buy the whole source. No periodic cash or stock injection.
            scenario.policy = new IndustryRuntimePolicy(true, 1000, 8, original.mining(), original.recipes(),
                    new WorkshopSupplyPolicy(true, 1, 16, Map.of(Commodity.COAL, 25L, Commodity.STONE, 25L)),
                    original.warehouseSupply());
            observe(f, geology);
            scenario.investment = scenario.service();
            String decision = scenario.review(2001);
            boolean viable = !geology.equals("neither");
            String kind = geology.equals("coal") ? "mining" : "quarry";
            assertEquals(viable ? 1 : 0, scenario.count("startup_grants"), decision);
            assertEquals(geology.equals("coal") ? 1 : 0, f.firms("mining"), decision);
            assertEquals(geology.equals("stone") ? 1 : 0, f.firms("quarry"), decision);
            assertEquals(viable ? 57500 : 60000, f.cash());

            long initialReserve = remaining(f);
            var runtime = runtime(f, scenario.policy);
            long now = 2001;
            runtime.start(now);
            for (int period = 0; period < 80; period++) {
                now += 1000;
                runtime.pulse(now, Set.of(f.world));
                if (period == 19) {
                    // An absent world cannot extract, trade or earn a backlog, even after a long gap.
                    long reserve = remaining(f), money = money(f), history = scenario.count("industry_runtime_history");
                    for (int absent = 0; absent < 20; absent++) {
                        now += 1000;
                        assertEquals(0, runtime.pulse(now, Set.of()));
                    }
                    assertEquals(reserve, remaining(f));
                    assertEquals(money, money(f));
                    assertEquals(history, scenario.count("industry_runtime_history"));
                    assertEquals(0, runtime.pulse(now, Set.of(f.world)));
                }
                if (period == 39) {
                    long reserve = remaining(f), money = money(f), history = scenario.count("industry_runtime_history");
                    f.db.close();
                    f.db = new DatabaseManager(directory.toFile(), Logger.getLogger("ResourceGeographyScenario"));
                    f.db.initialize();
                    runtime = runtime(f, scenario.policy);
                    now += 100_000; // Offline time earns nothing.
                    runtime.start(now);
                    assertEquals(0, runtime.pulse(now, Set.of(f.world)));
                    assertEquals(reserve, remaining(f));
                    assertEquals(money, money(f));
                    assertEquals(history, scenario.count("industry_runtime_history"));
                }
            }
            assertEquals(viable ? 99500 : 100000, money(f), "Only the actual startup fee leaves village/business cash");
            long finalReserve = remaining(f);
            if (viable) {
                assertTrue(initialReserve > 0);
                assertTrue(finalReserve < initialReserve, "Actual work must draw down the finite source");
                if (geology.equals("stone")) assertEquals(0, finalReserve);
                // Coal also produces stone. Full stores or exhausted customer budgets legitimately stop work;
                // no fictional consumer is added merely to guarantee complete extraction.
                assertTrue(f.scalar("SELECT SUM(units) FROM industry_stock WHERE commodity='" +
                        (geology.equals("coal") ? "tools" : "cut_material") + "'") > 0);
                assertTrue(scenario.count("industry_runtime_history") >= 80);
            } else {
                assertEquals(0, initialReserve);
                assertEquals(0, scenario.count("mineral_deposits"));
            }
            long earnedGoods = f.scalar("SELECT SUM(units) FROM industry_stock");
            // Fresh rediscovery of exactly the same coordinates is not a new deposit or a refill.
            f.capture = now;
            observe(f, geology);
            scenario.investment = scenario.service();
            scenario.review(now + 1);
            assertEquals(viable ? 1 : 0, scenario.count("startup_grants"));
            assertEquals(finalReserve, remaining(f));
            assertEquals(earnedGoods, f.scalar("SELECT SUM(units) FROM industry_stock"));
            assertEquals(viable ? 99500 : 100000, money(f));
            try (var s = f.db.getConnection().createStatement(); var rows = s.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(rows.next());
            }
        } finally {
            scenario.close();
        }
    }

    private static void observe(AutomaticQuarryFoundingTest f, String geology) throws Exception {
        long observed = f.capture + 1;
        new MineralOpportunities(f.db.getConnection()).observe(f.town, List.of(
                new MineralObservation(f.world, observed, true, Commodity.COAL, geology.equals("coal") ?
                        List.of(new Position(40, 40, 0), new Position(41, 40, 0)) : List.of()),
                new MineralObservation(f.world, observed, true, Commodity.IRON, List.of())));
        f.survey(geology.equals("stone") ? List.of(new Position(12, 40, 0), new Position(13, 40, 0)) : List.of(), List.of());
    }

    private static IndustryRuntime runtime(AutomaticQuarryFoundingTest f, IndustryRuntimePolicy policy) {
        var runtime = new IndustryRuntime(f.db.getConnection(), policy, BusinessOperationPolicy.prototype());
        runtime.configureQuarries(QuarryPolicy.prototype());
        return runtime;
    }

    private static long remaining(AutomaticQuarryFoundingTest f) throws Exception {
        return f.scalar("SELECT COALESCE(SUM(remaining_batches),0) FROM mineral_deposits");
    }

    private static long money(AutomaticQuarryFoundingTest f) throws Exception {
        return f.cash() + f.scalar("SELECT COALESCE(SUM(hundredths),0) FROM industry_stores");
    }
}
