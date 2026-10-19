package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Ten actual demographic years, with the same seed/geology but different saved family skill history. */
class IndustryHistoryScenarioTest {
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void qualifiedFamilyPreservesAnIndustryWhileAnUnfundedUnqualifiedFamilyCannot(boolean qualifiedHeir) throws Exception {
        var scenario = new ResourceInvestmentTest();
        scenario.directory = directory;
        try {
            scenario.setup();
            var f = scenario.fixture;
            assertTrue(scenario.review(2000).contains("mining"));
            var mine = f.extraction("mining");
            var parent = f.db.getPopulationRepository().findResident(f.founder).orElseThrow();
            // Explicit elderly fixture, ordinary unmodified prototype mortality (not a forced death).
            f.db.getPopulationRepository().updateResident(new Resident(parent.id(), parent.name(), parent.settlementId(),
                    parent.householdId(), 1200, Resident.Status.ALIVE));
            f.db.getPopulationRepository().insertFamilyTie(new FamilyTie(f.founder, f.other, FamilyTie.Type.PARENT_CHILD));
            if (!qualifiedHeir) f.sql("DELETE FROM resident_skills WHERE resident_id='" + f.other + "' AND skill='mining'");
            var population = PopulationPolicy.prototype();
            f.db.getDemographicRepository().initializeClock(42, population);
            var work = industry(f, scenario.policy);
            var lifecycle = lifecycle(f);
            var offers = new BusinessSaleListings(f.db.getConnection());
            long now = 2000;
            work.start(now);
            // Real earned goods exist before the owner enters the longer demographic scenario.
            for (int period = 0; period < 4; period++) work.pulse(now += 1000, Set.of(f.world));
            long openingReserve = remaining(f);
            assertTrue(openingReserve < f.scalar("SELECT SUM(initial_batches) FROM mineral_deposits"));
            for (int month = 1; month <= 120; month++) {
                now += 1000;
                var advanced = f.db.getDemographicRepository().advance(month, 1, population,
                        MigrationPolicy.disabled(), ignored -> Set.of(f.world));
                assertEquals(month, advanced.checkpoint().completedSteps());
                offers.observe(now, Set.of(f.world));
                lifecycle.refresh(month, Set.of(f.world), now, true);
                work.pulse(now, Set.of(f.world));
                if (month == 60) {
                    long cash = money(f), reserve = remaining(f), receipts = scenario.count("industry_runtime_history");
                    var beforePeople = f.db.getDemographicRepository().load(population).orElseThrow();
                    f.db.close();
                    f.db = new DatabaseManager(directory.toFile(), Logger.getLogger("IndustryHistoryScenario"));
                    f.db.initialize();
                    assertEquals(beforePeople, f.db.getDemographicRepository().load(population).orElseThrow());
                    now += 100_000;
                    work = industry(f, scenario.policy);work.start(now);
                    lifecycle = lifecycle(f);offers = new BusinessSaleListings(f.db.getConnection());offers.resume(now);
                    assertEquals(0, work.pulse(now, Set.of(f.world)));
                    assertEquals(cash, money(f));assertEquals(reserve, remaining(f));
                    assertEquals(receipts, scenario.count("industry_runtime_history"));
                }
            }
            var finalPeople = f.db.getDemographicRepository().load(population).orElseThrow();
            assertEquals(120, finalPeople.completedSteps());
            assertEquals(Resident.Status.DECEASED, f.db.getPopulationRepository().findResident(f.founder).orElseThrow().status());
            assertEquals(99500, money(f), "Transfers and closure cannot create or destroy retained cash");
            assertEquals(1, scenario.count("startup_grants"));
            assertEquals(f.founder, f.db.getBusinessRepository().findFounding(mine).orElseThrow().business().founderId());
            assertTrue(f.db.getIndustryRepository().store(mine).isPresent());
            assertTrue(remaining(f) <= openingReserve);
            if (qualifiedHeir) {
                assertEquals(f.other, f.db.getBusinessRepository().findBusiness(mine).orElseThrow().ownerId());
                assertEquals(1, scenario.count("business_ownership"));
                assertTrue(new BusinessClosure(f.db.getConnection()).find(mine).isEmpty());
                assertFalse(f.db.getBusinessRepository().currentWorkers(mine).isEmpty());
            } else {
                assertEquals(f.founder, f.db.getBusinessRepository().findBusiness(mine).orElseThrow().ownerId());
                assertEquals(0, scenario.count("business_ownership"));
                assertTrue(new BusinessClosure(f.db.getConnection()).find(mine).isPresent());
                assertTrue(f.db.getBusinessRepository().currentWorkers(mine).isEmpty());
                long reserve = remaining(f), cash = money(f);
                var retained = f.db.getIndustryRepository().store(mine).orElseThrow();
                work.pulse(now + 1000, Set.of(f.world));
                assertEquals(reserve, remaining(f));assertEquals(cash, money(f));
                assertEquals(retained, f.db.getIndustryRepository().store(mine).orElseThrow());
            }
            try (var s = f.db.getConnection().createStatement(); var rows = s.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(rows.next());
            }
        } finally {
            scenario.close();
        }
    }

    private static BusinessRuntime lifecycle(AutomaticQuarryFoundingTest f) throws Exception {
        var runtime = new BusinessRuntime(f.db.getConnection(), 8, 32);
        runtime.configureStaffing(new BusinessStaffingPolicy(true, 64, 2, Set.of("mining")));
        var original = BusinessResalePolicy.prototype();
        runtime.configureResale(new BusinessResalePolicy(true, original.minimumSiteHundredths(), original.startupRecoveryPercent(),
                4000, original.buyersPerReview(), original.stockValues()), BusinessPurchasePolicy.prototype(), 42);
        return runtime;
    }

    private static IndustryRuntime industry(AutomaticQuarryFoundingTest f, IndustryRuntimePolicy policy) {
        var runtime = new IndustryRuntime(f.db.getConnection(), policy, BusinessOperationPolicy.prototype());
        runtime.configureQuarries(QuarryPolicy.prototype());
        return runtime;
    }

    private static long remaining(AutomaticQuarryFoundingTest f) throws Exception {
        return f.scalar("SELECT SUM(remaining_batches) FROM mineral_deposits");
    }

    private static long money(AutomaticQuarryFoundingTest f) throws Exception {
        return f.cash() + f.scalar("SELECT COALESCE(SUM(hundredths),0) FROM industry_stores")
                + f.scalar("SELECT COALESCE(SUM(hundredths),0) FROM family_estates");
    }
}
