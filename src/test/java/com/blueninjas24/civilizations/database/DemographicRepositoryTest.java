package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import com.blueninjas24.civilizations.simulation.SimulationRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DemographicRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private PopulationRepository people;
    private DemographicRepository simulation;
    private UUID settlement;
    private Household household;
    private Resident first;
    private Resident second;

    @BeforeEach void initialize() throws SQLException {
        open();
        settlement = UUID.randomUUID();
        database.saveSettlement(new Settlement(settlement, "Test", "temporary-world", 0, 64, 0, 0, 0, 0));
        household = new Household(UUID.randomUUID(), settlement, "Home");
        first = new Resident(new UUID(0, 1), "First", settlement, household.id(), 240, Resident.Status.ALIVE);
        second = new Resident(new UUID(0, 2), "Second", settlement, household.id(), 240, Resident.Status.ALIVE);
        people.insertHouseholdWithResidents(household, List.of(first, second));
        people.insertFamilyTie(new FamilyTie(first.id(), second.id(), FamilyTie.Type.PARTNER));
    }

    private void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(), Logger.getLogger("DemographicTest"));
        database.initialize();
        people = database.getPopulationRepository();
        simulation = database.getDemographicRepository();
    }

    @AfterEach void close() { database.close(); }

    @Test
    void birthPeopleParentsEventsAndClockSurviveRestartWithoutRepeating() throws SQLException {
        var policy = policy(1, 0);
        simulation.recordCapacity(household.id(), 4);
        simulation.initializeClock(42, policy);
        var batch = simulation.advance(1, 1, policy);
        assertEquals(3, batch.checkpoint().state().livingCount());
        var events = simulation.findEvents(0, 1000);
        assertEquals(1, events.size());
        assertEquals(List.of(first.id(), second.id()), events.getFirst().event().relatedResidents());
        database.close();
        open();
        assertEquals(batch.checkpoint(), simulation.load(policy).orElseThrow());
        assertEquals(events, simulation.findEvents(0, 1000));
        assertEquals(0, simulation.advance(1, 100, policy).processedSteps());
        assertEquals(events, simulation.findEvents(0, 1000));
        UUID childId = events.getFirst().event().residentId();
        assertEquals(2, people.findFamilyTies(childId).size());
        assertEquals(0, people.findResident(childId).orElseThrow().ageSteps());
    }

    @Test
    void deathsRetainRecordsAndSequencePagesDoNotLoseEventsFromSameMonth() throws SQLException {
        var policy = policy(0, 1);
        simulation.initializeClock(42, policy);
        simulation.advance(3, 3, policy);
        assertEquals(0, people.countLivingResidents(settlement));
        assertEquals(2, people.findResidents(settlement).size());
        var firstPage = simulation.findEvents(0, 1);
        var secondPage = simulation.findEvents(firstPage.getFirst().sequence(), 1);
        assertEquals(firstPage.getFirst().event().step(), secondPage.getFirst().event().step());
        assertNotEquals(firstPage.getFirst().event().residentId(), secondPage.getFirst().event().residentId());
        assertTrue(simulation.findEvents(secondPage.getFirst().sequence(), 1).isEmpty());
        assertThrows(UnsupportedOperationException.class, firstPage::clear);
        assertThrows(IllegalArgumentException.class, () -> simulation.findEvents(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> simulation.findEvents(0, 1001));
    }

    @Test
    void eventWriteFailureRollsBackNewbornLinksAgesAndProgressThenRetriesIdentically() throws SQLException {
        var policy = policy(1, 0);
        simulation.recordCapacity(household.id(), 4);
        simulation.initializeClock(42, policy);
        var before = simulation.load(policy).orElseThrow();
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_event BEFORE INSERT ON population_events BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        }
        assertThrows(SQLException.class, () -> simulation.advance(1, 1, policy));
        assertEquals(before, simulation.load(policy).orElseThrow());
        assertTrue(simulation.findEvents(0, 1000).isEmpty());
        assertTrue(database.getConnection().getAutoCommit());
        try (var statement = database.getConnection().createStatement()) { statement.execute("DROP TRIGGER fail_event"); }
        var expected = SimulationRunner.advance(before, 1, 1,
                (state, context) -> PopulationStep.advance(state, context, policy).state());
        assertEquals(expected, simulation.advance(1, 1, policy));
        assertEquals(1, simulation.findEvents(0, 1000).size());
    }

    @Test
    void snapshotAndBatchingPreserveCenturyReplayIncludingAllEvents() throws Exception {
        var policy = policy(0.2, 0.01);
        simulation.recordCapacity(household.id(), 8);
        simulation.initializeClock(42, policy);
        Path snapshot = directory.resolve("demographic-snapshot.db");
        DatabaseBackup.snapshot(directory.resolve("civilizations.db"), snapshot);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + snapshot);
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            var restored = new DemographicRepository(connection);
            var all = restored.advance(1200, 1200, policy);
            var batch = simulation.advance(1200, 7, policy);
            database.close();
            open();
            while (!batch.complete()) batch = simulation.advance(1200, 31, policy);
            assertEquals(all.checkpoint(), batch.checkpoint());
            assertEquals(restored.findEvents(0, 1000), simulation.findEvents(0, 1000));
            assertFalse(simulation.findEvents(0, 1000).isEmpty());
            try (var result = statement.executeQuery("PRAGMA foreign_key_check")) { assertFalse(result.next()); }
        }
    }

    @Test
    void clocksNeverOverwriteSeedMixModesOrSilentlyChangeReplaySettings() throws SQLException {
        var policy = policy(0, 0);
        assertTrue(simulation.load(policy).isEmpty());
        assertThrows(SQLException.class, () -> simulation.advance(1, 1, policy));
        simulation.initializeClock(42, policy);
        assertThrows(SQLException.class, () -> simulation.initializeClock(43, policy));
        assertThrows(SQLException.class, () -> database.getPopulationSimulationRepository().initializeClock(settlement, 1));
        assertThrows(SQLException.class, () -> simulation.load(policy(1, 0)));
        assertThrows(SQLException.class, () -> simulation.advance(10, 10, policy(1, 0)));
        assertEquals(42, simulation.load(policy).orElseThrow().seed());
        try (var statement = database.getConnection().createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE demographic_clock SET completed_steps = -1"));
            statement.executeUpdate("UPDATE demographic_clock SET replay_version = 999");
        }
        assertThrows(SQLException.class, () -> simulation.load(policy));
        assertEquals(first, people.findResident(first.id()).orElseThrow());
    }

    @Test
    void oldAgingModeUnknownCapacitiesAndCallerTransactionsAreHandledExplicitly() throws SQLException {
        assertThrows(SQLException.class, () -> simulation.recordCapacity(UUID.randomUUID(), 1));
        assertThrows(IllegalArgumentException.class, () -> simulation.recordCapacity(household.id(), -1));
        database.getPopulationSimulationRepository().initializeClock(settlement, 1);
        assertThrows(SQLException.class, () -> simulation.initializeClock(1, policy(0, 0)));
        database.getConnection().setAutoCommit(false);
        try {
            assertThrows(SQLException.class, () -> simulation.load(policy(0, 0)));
            assertThrows(SQLException.class, () -> simulation.advance(1, 1, policy(0, 0)));
            assertFalse(database.getConnection().getAutoCommit());
        } finally {
            database.getConnection().rollback();
            database.getConnection().setAutoCommit(true);
        }
    }

    @Test
    void unknownCapacityBlocksBirthAndMeasuredCapacityCanBeUpdated() throws SQLException {
        var policy = policy(1, 0);
        simulation.initializeClock(42, policy);
        assertEquals(2, simulation.advance(1, 1, policy).checkpoint().state().livingCount());
        simulation.recordCapacity(household.id(), 2);
        assertEquals(2, simulation.advance(2, 1, policy).checkpoint().state().livingCount());
        simulation.recordCapacity(household.id(), 3);
        assertEquals(3, simulation.advance(3, 1, policy).checkpoint().state().livingCount());
        assertThrows(IllegalArgumentException.class, () -> simulation.advance(2, 1, policy));
        assertThrows(IllegalArgumentException.class, () -> simulation.advance(4, 0, policy));
    }

    @Test
    void failedVersionSixUpgradeRollsBackClockAndCapacityTablesWithoutLosingPeople() throws SQLException {
        try (var statement = database.getConnection().createStatement()) {
            SchemaFixtures.rewind(database.getConnection(), 6);
            statement.execute("CREATE TABLE population_events (sequence INTEGER)");
        }
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(database.getConnection()));
        assertEquals(first, people.findResident(first.id()).orElseThrow());
        try (var statement = database.getConnection().createStatement()) {
            try (var result = statement.executeQuery("PRAGMA user_version")) { assertEquals(6, result.getInt(1)); }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name IN ('demographic_clock', 'household_capacity')")) {
                assertEquals(0, result.getInt(1));
            }
            statement.execute("DROP TABLE population_events");
        }
        new SchemaMigrator().migrate(database.getConnection());
        simulation.initializeClock(42, policy(0, 0));
        assertEquals(2, simulation.advance(1, 1, policy(0, 0)).checkpoint().state().livingCount());
    }

    @Test
    void householdMigrationPersistsAcrossRestartAndRetainsOriginEvents() throws SQLException {
        people.recordSkillExperience(first.id(), "carpentry", 10);
        var originalProfile = people.findWorkProfile(first.id());
        Household destination = destination("temporary-world");
        var policy = policy(0, 0);
        var migration = new MigrationPolicy(1, 0);
        simulation.recordCapacity(household.id(), 1);
        simulation.recordCapacity(destination.id(), 4);
        simulation.initializeClock(42, policy, migration);
        simulation.advance(1, 1, policy, migration);
        database.close();
        open();
        assertEquals(0, people.countLivingResidents(settlement));
        assertEquals(2, people.countLivingResidents(destination.settlementId()));
        Resident moved = people.findResident(first.id()).orElseThrow();
        assertEquals(destination.id(), moved.householdId());
        assertEquals(first.name(), moved.name());
        assertEquals(241, moved.ageSteps());
        assertEquals(originalProfile, people.findWorkProfile(first.id()));
        assertEquals(1, people.findFamilyTies(first.id()).size());
        assertEquals(2, simulation.findEvents(0, 1000).size());
        assertTrue(simulation.findEvents(0, 1000).stream().allMatch(e -> e.event().sourceSettlementId().equals(settlement)));
        assertEquals(0, simulation.advance(1, 1, policy, migration).processedSteps());
    }

    @Test
    void migrationEventFailureRollsBackWholeHouseholdAndRetries() throws SQLException {
        Household destination = destination("temporary-world");
        var policy = policy(0, 0);
        var migration = new MigrationPolicy(1, 0);
        simulation.recordCapacity(household.id(), 1);
        simulation.recordCapacity(destination.id(), 4);
        simulation.initializeClock(42, policy, migration);
        var before = simulation.load(policy, migration);
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_move BEFORE INSERT ON population_events WHEN NEW.type = 'MIGRATION' BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        }
        assertThrows(SQLException.class, () -> simulation.advance(1, 1, policy, migration));
        assertEquals(before, simulation.load(policy, migration));
        assertEquals(first, people.findResident(first.id()).orElseThrow());
        assertTrue(simulation.findEvents(0, 1000).isEmpty());
        try (var statement = database.getConnection().createStatement()) { statement.execute("DROP TRIGGER fail_move"); }
        simulation.advance(1, 1, policy, migration);
        assertEquals(2, people.countLivingResidents(destination.settlementId()));
    }

    @Test
    void databaseWorldBoundaryAndUnknownCapacityBlockMigration() throws SQLException {
        Household destination = destination("another-world");
        var policy = policy(0, 0);
        var migration = new MigrationPolicy(1, 0);
        simulation.recordCapacity(household.id(), 1);
        simulation.recordCapacity(destination.id(), 4);
        simulation.initializeClock(42, policy, migration);
        simulation.advance(1, 1, policy, migration);
        assertEquals(settlement, people.findResident(first.id()).orElseThrow().settlementId());
        assertTrue(simulation.findEvents(0, 1000).isEmpty());
        assertThrows(SQLException.class, () -> simulation.load(policy, new MigrationPolicy(0.5, 0)));
    }

    @Test
    void movedThenDeceasedWithinOneBatchRetainsDestinationAndBothHistoricalEvents() throws SQLException {
        Household destination = destination("temporary-world");
        var policy = new PopulationPolicy(12, 216, 240, 480, 250, 0, 0, 1);
        var migration = new MigrationPolicy(1, 0);
        simulation.recordCapacity(household.id(), 1);
        simulation.recordCapacity(destination.id(), 4);
        simulation.initializeClock(42, policy, migration);
        simulation.advance(30, 30, policy, migration);
        assertEquals(0, people.countLivingResidents(destination.settlementId()));
        for (Resident original : List.of(first, second)) {
            var retained = people.findResident(original.id()).orElseThrow();
            assertEquals(Resident.Status.DECEASED, retained.status());
            assertEquals(destination.settlementId(), retained.settlementId());
        }
        assertEquals(4, simulation.findEvents(0, 1000).size());
    }

    @Test
    void snapshotCenturyReplayMatchesForBirthDeathMovesAndExternalArrivals() throws Exception {
        Household destination = destination("temporary-world");
        var policy = policy(0.1, 0.01);
        var migration = new MigrationPolicy(0.5, 0.2);
        simulation.recordCapacity(household.id(), 1);
        simulation.recordCapacity(destination.id(), 4);
        simulation.initializeClock(42, policy, migration);
        Path snapshot = directory.resolve("migration-snapshot.db");
        DatabaseBackup.snapshot(directory.resolve("civilizations.db"), snapshot);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + snapshot);
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            var restored = new DemographicRepository(connection);
            var all = restored.advance(1200, 1200, policy, migration);
            var partial = simulation.advance(1200, 13, policy, migration);
            while (!partial.complete()) partial = simulation.advance(1200, 29, policy, migration);
            assertEquals(all.checkpoint(), partial.checkpoint());
            var events = allEvents(simulation);
            assertEquals(allEvents(restored), events);
            long births = events.stream().filter(e -> e.event().type() == PopulationEvent.Type.BIRTH).count();
            long arrivals = events.stream().filter(e -> e.event().type() == PopulationEvent.Type.ARRIVAL).count();
            long deaths = events.stream().filter(e -> e.event().type() == PopulationEvent.Type.DEATH).count();
            assertEquals(2 + births + arrivals - deaths, partial.checkpoint().state().livingCount());
            assertTrue(arrivals > 0);
            assertTrue(events.stream().anyMatch(e -> e.event().type() == PopulationEvent.Type.MIGRATION));
            try (var result = statement.executeQuery("PRAGMA foreign_key_check")) { assertFalse(result.next()); }
        }
    }

    @Test
    void versionSevenEventRebuildPreservesSequencesAndRollsBackNameConflicts() throws SQLException {
        var policy = policy(0, 1);
        simulation.initializeClock(42, policy);
        simulation.advance(1, 1, policy);
        var originalEvents = simulation.findEvents(0, 1000);
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("CREATE TABLE old_events AS SELECT sequence, step, type, resident_id, settlement_id, first_relative, second_relative, reason FROM population_events");
            statement.execute("DROP TABLE population_events");
            statement.execute("ALTER TABLE old_events RENAME TO population_events");
            statement.execute("CREATE INDEX events_by_resident ON population_events(resident_id, sequence)");
            SchemaFixtures.rewind(database.getConnection(), 7);
            statement.execute("CREATE TABLE population_events_v7 (sequence INTEGER)");
        }
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(database.getConnection()));
        try (var statement = database.getConnection().createStatement()) {
            try (var result = statement.executeQuery("PRAGMA user_version")) { assertEquals(7, result.getInt(1)); }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM population_events")) { assertEquals(2, result.getInt(1)); }
            statement.execute("DROP TABLE population_events_v7");
        }
        new SchemaMigrator().migrate(database.getConnection());
        assertEquals(originalEvents, simulation.findEvents(0, 1000));
        // A schema upgrade must not silently rewrite an older replay convention.
        try (var statement = database.getConnection().createStatement()) { statement.executeUpdate("UPDATE demographic_clock SET replay_version = 2"); }
        assertThrows(SQLException.class, () -> simulation.load(policy));
        assertEquals(originalEvents, simulation.findEvents(0, 1000));
    }

    private Household destination(String world) throws SQLException {
        UUID id = UUID.randomUUID();
        database.saveSettlement(new Settlement(id, "Destination", world, 100, 64, 0, 0, 0, 0));
        Household destination = new Household(UUID.randomUUID(), id, "Vacant home");
        people.insertHousehold(destination);
        return destination;
    }

    private List<DemographicRepository.StoredEvent> allEvents(DemographicRepository repository) throws SQLException {
        var events = new java.util.ArrayList<DemographicRepository.StoredEvent>();
        long cursor = 0;
        while (true) {
            var page = repository.findEvents(cursor, 100);
            if (page.isEmpty()) return List.copyOf(events);
            events.addAll(page);
            cursor = page.getLast().sequence();
        }
    }

    private PopulationPolicy policy(double births, double deaths) {
        return new PopulationPolicy(12, 216, 240, 480, 720, births, deaths, 0);
    }
}
