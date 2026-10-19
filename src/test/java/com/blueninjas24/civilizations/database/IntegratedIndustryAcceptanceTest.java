package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.economy.CommodityAccounting.Condition;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import com.blueninjas24.civilizations.settlement.VillageRecognition;
import com.blueninjas24.civilizations.database.VillageEnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Ten accelerated calendar years through the real SQL owner, with finite industry work and a real remote buyer. */
class IntegratedIndustryAcceptanceTest {
    @TempDir Path directory;

    @Test void seededOwnerRunKeepsShortageAndFiniteAssetsHonestAcrossRemoteSaleAbsenceAndRestart() throws Exception {
        var scenario = new ResourceInvestmentTest();
        scenario.directory = directory;
        PopulationSession session = null;
        try {
            scenario.setup();
            var f = scenario.fixture;
            assertTrue(scenario.review(2001).contains("mining"));
            var mine = f.extraction("mining");
            long initialReserve = remaining(f);
            assertTrue(initialReserve > 0);

            // The local family member is deliberately unqualified; the same-world remote owner is qualified and funded.
            f.db.getPopulationRepository().updateResident(new Resident(f.founder, "Elder founder", f.town, null, 1600, Resident.Status.ALIVE));
            f.db.getPopulationRepository().insertFamilyTie(new FamilyTie(f.founder, f.other, FamilyTie.Type.PARENT_CHILD));
            f.sql("DELETE FROM resident_skills WHERE resident_id='" + f.other + "' AND skill='mining'");
            var remoteTown = new UUID(8, 1);
            var remoteOwner = new UUID(8, 2);
            var remoteBusiness = new UUID(8, 3);
            f.db.saveSettlement(new Settlement(remoteTown, "Remote village", "Fixture", 200, 64, 0, 1, 0, 0));
            f.db.getVillageIdentityRepository().bindSettlement(remoteTown, f.world);
            new TreasuryRepository(f.db.getConnection()).initialize(new UUID(8, 6), remoteTown, 1_250_000, "Actual remote village reserve");
            f.db.getPopulationRepository().insertResident(new Resident(remoteOwner, "Qualified remote owner", remoteTown, null, 300, Resident.Status.ALIVE));
            f.db.getPopulationRepository().recordSkillExperience(remoteOwner, "mining", 10);
            f.db.getPopulationRepository().recordSkillExperience(remoteOwner, "farming", 10);
            var remoteSite = new BusinessSite(new UUID(8, 4), remoteTown, f.world, 200, 64, 0, 1, "Explicit remote buyer workplace");
            var remoteFirm = new Business(remoteBusiness, remoteTown, "Remote buyer", remoteOwner, remoteOwner, "mining", remoteSite.id(), 1, 0);
            var requirements = new JobRequirements(216, Map.of());
            f.db.getBusinessRepository().found(new BusinessFounding(remoteFirm, remoteSite, requirements, requirements, 0, "Real buyer fixture"));
            f.db.getIndustryRepository().openStore(remoteBusiness, 100, Map.of(Commodity.STONE, 1L), 1_000_000);

            // Make a measured, real household food shortage; no relief, customer, or production is injected.
            f.sql("UPDATE economy_stocks SET units=0 WHERE settlement_id='" + f.town + "' AND commodity='food'");
            var expectedMoney = money(f);
            var population = PopulationPolicy.prototype();
            long seed = PopulationSession.Settings.prototype().seed();
            f.db.getDemographicRepository().initializeClock(seed, population, MigrationPolicy.disabled());
            f.db.close();

            var now = new AtomicLong(2001);
            var settings = new PopulationSession.Settings(seed, 1000, 0, 2, population, MigrationPolicy.disabled(),
                    VillageRecognition.Policy.prototype(), VillageEnrollmentRepository.Policy.prototype());
            session = session(now, settings, scenario);
            await(session.start(false, Set.of(f.world)));
            assertTenYears(session, now, f, mine, remoteOwner, 60);
            session.close();
            session = null;

            // Online restart discards elapsed server downtime. The saved calendar, source and sale receipt resume exactly.
            now.addAndGet(100_000);
            session = session(now, settings, scenario);
            await(session.start(false, Set.of(f.world)));
            long beforePulseMonths = session.status().completedMonths();
            var beforePulseProduction = session.businessView().businesses().get(mine).lastProduction();
            long beforePulseEconomyPeriods = session.economyView().get(f.town).account().state().completedPeriods();
            await(session.pulse());
            assertEquals(beforePulseMonths, session.status().completedMonths());
            assertEquals(beforePulseProduction, session.businessView().businesses().get(mine).lastProduction());
            assertEquals(beforePulseEconomyPeriods, session.economyView().get(f.town).account().state().completedPeriods());
            assertTenYears(session, now, f, mine, remoteOwner, 120);
            session.close();
            session = null;
            f.db = new DatabaseManager(directory.toFile(), Logger.getLogger("IntegratedIndustryAcceptance"));
            f.db.initialize();

            assertEquals(120, f.db.getDemographicRepository().load(population, MigrationPolicy.disabled()).orElseThrow().completedSteps());
            assertEquals(1, f.scalar("SELECT COUNT(*) FROM business_sales"));
            assertEquals(remoteOwner, f.db.getBusinessRepository().findBusiness(mine).orElseThrow().ownerId());
            assertEquals(f.town, f.db.getBusinessRepository().findBusiness(mine).orElseThrow().settlementId());
            assertTrue(remaining(f) < initialReserve, "Actual work must draw down the finite source");
            assertEquals(expectedMoney, money(f), "Production, shortage and the paid remote sale conserve saved funds");
            assertTrue(f.db.getIndustryRepository().store(mine).isPresent(), "The local business inventory remains attached to its original site");
            assertTrue(f.scalar("SELECT COUNT(*) FROM needs_periods WHERE demand>fulfilled")>0,
                    "A real historical food deficit must be recorded; later population changes may reduce demand");
            try (var statement = f.db.getConnection().createStatement(); var rows = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(rows.next());
            }
        } finally {
            if (session != null) session.close();
            scenario.close();
        }
    }

    private PopulationSession session(AtomicLong now, PopulationSession.Settings settings, ResourceInvestmentTest scenario) {
        var session = new PopulationSession(directory.toFile(), Logger.getLogger("IntegratedIndustryAcceptance"), settings,
                now::get, scenario.fixture.economySettings, com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype());
        session.configureIndustryRuntime(scenario.policy);
        session.configureBusinessOperations(BusinessOperationPolicy.prototype());
        session.configureBusinessPurchases(BusinessPurchasePolicy.prototype());
        var resale = BusinessResalePolicy.prototype();
        session.configureAutomaticResale(new BusinessResalePolicy(true, resale.minimumSiteHundredths(), resale.startupRecoveryPercent(),
                4000, resale.buyersPerReview(), resale.stockValues()));
        session.configureBusinessStaffing(new BusinessStaffingPolicy(true, 64, 2, Set.of("mining")));
        return session;
    }

    private void assertTenYears(PopulationSession session, AtomicLong now, AutomaticQuarryFoundingTest f, UUID mine,
                                UUID remoteOwner, int targetMonth) throws Exception {
        while (session.status().completedMonths() < targetMonth) {
            long nextMonth = session.status().completedMonths() + 1;
            now.addAndGet(1000);
            if (nextMonth == 30) {
                await(session.pulse());
                assertEquals(30, session.status().completedMonths());
                await(session.updateLoadedWorlds(Set.of()));
                var industryBefore = session.businessView().businesses().get(mine);
                var economyBefore = session.economyView().get(f.town).account().state().completedPeriods();
                for (int absent = 0; absent < 3; absent++) {
                    now.addAndGet(1000);
                    await(session.pulse());
                }
                assertEquals(30, session.status().completedMonths());
                assertEquals(industryBefore.lastProduction(), session.businessView().businesses().get(mine).lastProduction());
                assertEquals(economyBefore, session.economyView().get(f.town).account().state().completedPeriods());
                await(session.updateLoadedWorlds(Set.of(f.world)));
            } else {
                await(session.pulse());
            }
            assertTrue(session.status().completedMonths() <= targetMonth);
            assertNull(session.status().failure(),"Every owner pulse must finish without a hidden runtime failure");
        }
        assertTrue(session.status().completedMonths() >= targetMonth);
        // The sale is generated by ordinary owner death, local family qualification and actual remote business funds.
        if (targetMonth == 120) {
            var business = session.businessView().businesses().get(mine);
            assertNotNull(business);
            assertEquals("Qualified remote owner", business.owner(),business.lifecycle());
        }
    }

    private static long remaining(AutomaticQuarryFoundingTest f) throws Exception {
        return f.scalar("SELECT COALESCE(SUM(remaining_batches),0) FROM mineral_deposits");
    }

    private static com.blueninjas24.civilizations.economy.HouseholdNeeds.Assessment latestNeeds(
            AutomaticQuarryFoundingTest f, UUID town) throws SQLException {
        long period;
        try (var statement = f.db.getConnection().prepareStatement(
                "SELECT MAX(period) FROM needs_periods WHERE settlement_id=?")) {
            statement.setString(1, town.toString());
            try (var rows = statement.executeQuery()) { rows.next(); period = rows.getLong(1); }
        }
        return new HouseholdNeedsRepository(f.db.getConnection()).load(town, period).orElseThrow();
    }

    private static long money(AutomaticQuarryFoundingTest f) throws SQLException {
        try (var statement = f.db.getConnection().createStatement(); var rows = statement.executeQuery(
                "SELECT (SELECT COALESCE(SUM(hundredths),0) FROM treasuries)+(SELECT COALESCE(SUM(hundredths),0) FROM industry_stores)+(SELECT COALESCE(SUM(hundredths),0) FROM family_estates)")) {
            rows.next(); return rows.getLong(1);
        }
    }

    private static <T> T await(java.util.concurrent.CompletableFuture<T> work) throws Exception {
        return work.get(5, TimeUnit.SECONDS);
    }
}
