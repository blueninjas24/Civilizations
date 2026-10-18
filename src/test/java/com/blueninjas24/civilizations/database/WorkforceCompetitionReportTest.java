package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorkforceCompetitionReportTest {
    @TempDir Path folder;
    private DatabaseManager db;
    private final UUID town = new UUID(1, 1), world = new UUID(2, 2);
    private UUID owner, closedOwner;

    @BeforeEach void setup() throws Exception {
        db = new DatabaseManager(folder.toFile(), Logger.getLogger("WorkforceCompetitionReportTest"));
        db.initialize();
        db.saveSettlement(new Settlement(town, "Fixture", "Fixture", 0, 64, 0, 1, 0, 0));
        db.getVillageIdentityRepository().observeWorld(world, "Fixture");
        db.getVillageIdentityRepository().bindSettlement(town, world);
        owner = person(1, 300, Map.of());
        closedOwner = person(2, 300, Map.of());
        new TreasuryRepository(db.getConnection()).initialize(new UUID(3, 3), town, 10_000, "Fixture report funds");
    }

    @AfterEach void close() { if (db != null) db.close(); }

    private UUID person(long id, long age, Map<String, Long> skills) throws Exception {
        UUID resident = new UUID(0, id);
        db.getPopulationRepository().insertResident(new Resident(resident, "Resident " + id, town, null, age, Resident.Status.ALIVE));
        for (var skill : skills.entrySet()) db.getPopulationRepository().recordSkillExperience(resident, skill.getKey(), skill.getValue());
        return resident;
    }

    private UUID business(long number, String industry, UUID founder, int capacity, JobRequirements worker,
                          long cash) throws Exception {
        return business(number, industry, founder, capacity, worker, cash, true);
    }

    private UUID business(long number, String industry, UUID founder, int capacity, JobRequirements worker,
                          long cash, boolean createStore) throws Exception {
        UUID id = new UUID(10, number), siteId = new UUID(11, number);
        var site = new BusinessSite(siteId, town, world, Math.toIntExact(number), 40, 0, 1000, "Explicit read-only fixture site");
        var firm = new Business(id, town, "Fixture " + industry, founder, founder, industry, siteId, capacity, 0);
        db.getBusinessRepository().found(new BusinessFounding(firm, site, new JobRequirements(216, Map.of()), worker, 0,
                "Explicit workforce report fixture"));
        if (createStore) db.getIndustryRepository().openStore(id, 100, Map.of(Commodity.TIMBER, 1L), cash);
        return id;
    }

    private long scalar(String sql) throws SQLException {
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery(sql)) {
            assertTrue(r.next()); return r.getLong(1);
        }
    }

    private long[] persistedRows() throws SQLException {
        return new long[]{scalar("SELECT COUNT(*) FROM businesses"), scalar("SELECT COUNT(*) FROM business_employment"),
                scalar("SELECT COUNT(*) FROM resident_courses"), scalar("SELECT COUNT(*) FROM startup_grants"),
                scalar("SELECT COALESCE(SUM(hundredths),0) FROM treasuries"),
                scalar("SELECT COALESCE(SUM(hundredths),0) FROM industry_stores")};
    }

    @Test void comparesOverlappingQualificationsAgeAndUnavailableFirmWithoutChangingRows() throws Exception {
        UUID common = person(10, 300, Map.of("mining", 2L, "forestry", 3L));
        person(11, 300, Map.of("mining", 2L));
        person(12, 300, Map.of("forestry", 3L));
        person(13, 100, Map.of("mining", 2L, "forestry", 3L)); // Below every saved worker age floor.
        UUID student = person(14, 300, Map.of("mining", 2L, "forestry", 3L));
        UUID employed = person(15, 300, Map.of("mining", 2L, "forestry", 3L));
        try (var s = db.getConnection().prepareStatement("""
                INSERT INTO resident_courses(id,resident_id,settlement_id,skill,target,gain,eligible_after,status)
                VALUES(?,?,?,?,?,?,?,'ACTIVE')
                """)) {
            s.setString(1, UUID.randomUUID().toString()); s.setString(2, student.toString()); s.setString(3, town.toString());
            s.setString(4, "forestry"); s.setInt(5, 10); s.setInt(6, 1); s.setInt(7, 0); s.executeUpdate();
        }
        UUID mining = business(1, "mining", owner, 2, new JobRequirements(216, Map.of("mining", 2L)), 500);
        UUID forestry = business(2, "logging", owner, 2, new JobRequirements(216, Map.of("forestry", 3L)), 600);
        UUID unavailable = business(3, "quarry", closedOwner, 1, new JobRequirements(216, Map.of("mining", 2L)), 700);
        db.getBusinessRepository().hire(UUID.randomUUID(), mining, employed, 0);
        db.getPopulationRepository().updateResident(new Resident(closedOwner, "Resident 2", town, null, 300, Resident.Status.DECEASED));

        var before = persistedRows();
        var snapshot = new WorkforceCompetitionReport(db.getConnection()).read(town, BusinessOperationPolicy.disabled());

        assertTrue(snapshot.complete());
        assertEquals(4, snapshot.residentCandidatePool()); // Includes the idle owner, though that owner qualifies for neither role.
        var firms = new HashMap<UUID, WorkforceCompetitionReport.Firm>();
        snapshot.firms().forEach(firm -> firms.put(firm.id(), firm));
        assertEquals(1, firms.get(mining).workers());
        assertEquals(1, firms.get(mining).vacancies());
        assertEquals(2, firms.get(mining).qualifiedIdleCandidates());
        assertEquals(2, firms.get(forestry).vacancies());
        assertEquals(2, firms.get(forestry).qualifiedIdleCandidates());
        assertTrue(firms.get(mining).available());
        assertFalse(firms.get(unavailable).available());
        assertTrue(firms.get(unavailable).availabilityReason().contains("owner unavailable"));
        assertEquals(500, firms.get(mining).businessCashHundredths());
        assertEquals(600, firms.get(forestry).businessCashHundredths());
        assertEquals(700, firms.get(unavailable).businessCashHundredths());
        assertTrue(snapshot.describe().stream().anyMatch(line -> line.contains("overlap")));
        assertTrue(snapshot.describe().stream().anyMatch(line -> line.contains("not a ranking")));
        assertTrue(snapshot.describe().stream().anyMatch(line -> line.contains("UUID/page order")));
        assertArrayEquals(before, persistedRows(), "The report must not change firms, jobs, courses, grants or balances");
        assertTrue(snapshot.firms().stream().anyMatch(firm -> firm.qualifiedIdleCandidates() == 2));
    }

    @Test void unknownSettlementReturnsAnExplicitIncompleteSnapshot() throws Exception {
        var before = persistedRows();
        UUID unknown = new UUID(99, 99);
        var snapshot = new WorkforceCompetitionReport(db.getConnection()).read(unknown, BusinessOperationPolicy.prototype());
        assertEquals(unknown, snapshot.settlement());
        assertFalse(snapshot.complete());
        assertTrue(snapshot.firms().isEmpty());
        assertTrue(snapshot.explanation().contains("Unknown settlement"));
        assertTrue(snapshot.describe().getFirst().contains("Unknown settlement"));
        assertArrayEquals(before, persistedRows());
    }

    @Test void missingBusinessStoreIsReportedAsUnknownCash() throws Exception {
        business(1, "mining", owner, 1, new JobRequirements(216, Map.of()), 100, false);
        var snapshot = new WorkforceCompetitionReport(db.getConnection()).read(town, BusinessOperationPolicy.prototype());
        assertTrue(snapshot.complete());
        assertNull(snapshot.firms().getFirst().businessCashHundredths());
        assertTrue(snapshot.describe().stream().anyMatch(line -> line.contains("unknown (no saved business store)")));
    }

    @Test void failsClosedWhenFirmBudgetIsExceeded() throws Exception {
        for (int i = 1; i <= WorkforceCompetitionReport.MAX_FIRMS + 1; i++)
            business(i, "industry_" + i, owner, 1, new JobRequirements(216, Map.of()), 0);
        var before = persistedRows();
        var snapshot = new WorkforceCompetitionReport(db.getConnection()).read(town, BusinessOperationPolicy.prototype());
        assertFalse(snapshot.complete());
        assertTrue(snapshot.firms().isEmpty());
        assertTrue(snapshot.explanation().contains("firm review budget"));
        assertTrue(snapshot.describe().getFirst().contains("no partial comparison"));
        assertArrayEquals(before, persistedRows());
    }

    @Test void failsClosedWhenResidentCandidateBudgetIsExceeded() throws Exception {
        business(1, "mining", owner, 1, new JobRequirements(216, Map.of()), 0);
        for (int i = 100; i < 100 + WorkforceCompetitionReport.MAX_RESIDENT_CANDIDATES + 1; i++)
            person(i, 300, Map.of());
        var before = persistedRows();
        var snapshot = new WorkforceCompetitionReport(db.getConnection()).read(town, BusinessOperationPolicy.prototype());
        assertFalse(snapshot.complete());
        assertTrue(snapshot.firms().isEmpty());
        assertTrue(snapshot.explanation().contains("resident candidate page"));
        assertArrayEquals(before, persistedRows());
    }
}
