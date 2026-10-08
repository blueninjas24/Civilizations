package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.Settlement;
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

class PopulationSimulationRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private PopulationRepository people;
    private PopulationSimulationRepository simulation;
    private UUID settlement;

    @BeforeEach void initialize() throws SQLException {
        open();
        settlement = settlement();
    }

    private void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(), Logger.getLogger("AgingTest"));
        database.initialize();
        people = database.getPopulationRepository();
        simulation = database.getPopulationSimulationRepository();
    }

    @AfterEach void close() { database.close(); }

    @Test
    void checkpointsResumeAcrossRestartWithoutDoubleAgingOrAgingTheDead() throws SQLException {
        Resident living = person(settlement, 123, Resident.Status.ALIVE);
        Resident deceased = person(settlement, 456, Resident.Status.DECEASED);
        simulation.initializeClock(settlement, Long.MIN_VALUE);
        var first = simulation.advanceAging(settlement, 10, 3);
        assertEquals(3, first.processedSteps());
        assertEquals(7, first.remainingSteps());
        database.close();
        open();
        var second = simulation.advanceAging(settlement, 10, 100);
        assertTrue(second.complete());
        assertEquals(7, second.processedSteps());
        assertEquals(133, people.findResident(living.id()).orElseThrow().ageSteps());
        assertEquals(deceased, people.findResident(deceased.id()).orElseThrow());
        assertEquals(Long.MIN_VALUE, second.checkpoint().seed());
        var retry = simulation.advanceAging(settlement, 10, 1);
        assertEquals(0, retry.processedSteps());
        assertEquals(second.checkpoint(), retry.checkpoint());
        assertEquals(1, people.countLivingResidents(settlement));
    }

    @Test
    void savedBatchSizesProduceIdenticalStateAndPreserveFamilyLinks() throws SQLException {
        Resident person = person(settlement, 0, Resident.Status.ALIVE);
        Resident parent = person(settlement, 200, Resident.Status.ALIVE);
        var tie = new com.blueninjas24.civilizations.population.FamilyTie(parent.id(), person.id(),
                com.blueninjas24.civilizations.population.FamilyTie.Type.PARENT_CHILD);
        people.insertFamilyTie(tie);
        simulation.initializeClock(settlement, 42);
        var expected = com.blueninjas24.civilizations.simulation.SimulationRunner.advance(
                simulation.load(settlement).orElseThrow(), 1000, 1000,
                (state, context) -> com.blueninjas24.civilizations.population.PopulationAging.step(state));
        for (int i = 0; i < 143; i++) {
            simulation.advanceAging(settlement, 1000, 7);
        }
        assertEquals(expected.checkpoint(), simulation.load(settlement).orElseThrow());
        assertEquals(List.of(tie), people.findFamilyTies(person.id()));
    }

    @Test
    void ageOverflowRollsBackBatchAndAllowsRetryAfterCorrection() throws SQLException {
        Resident person = person(settlement, Long.MAX_VALUE - 1, Resident.Status.ALIVE);
        simulation.initializeClock(settlement, 1);
        assertThrows(ArithmeticException.class, () -> simulation.advanceAging(settlement, 2, 2));
        assertEquals(0, simulation.load(settlement).orElseThrow().completedSteps());
        assertEquals(person, people.findResident(person.id()).orElseThrow());
        assertTrue(database.getConnection().getAutoCommit());
        assertTrue(simulation.advanceAging(settlement, 1, 1).complete());
        assertEquals(Long.MAX_VALUE, people.findResident(person.id()).orElseThrow().ageSteps());
    }

    @Test
    void failureAfterPersonUpdatesRollsBackPeopleAndClockTogether() throws SQLException {
        Resident person = person(settlement, 10, Resident.Status.ALIVE);
        simulation.initializeClock(settlement, 1);
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("""
                    CREATE TRIGGER fail_clock BEFORE UPDATE ON population_clocks
                    BEGIN SELECT RAISE(ABORT, 'test write failure'); END
                    """);
        }
        assertThrows(SQLException.class, () -> simulation.advanceAging(settlement, 3, 3));
        assertEquals(person, people.findResident(person.id()).orElseThrow());
        assertEquals(0, simulation.load(settlement).orElseThrow().completedSteps());
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("DROP TRIGGER fail_clock");
        }
        simulation.advanceAging(settlement, 3, 3);
        assertEquals(13, people.findResident(person.id()).orElseThrow().ageSteps());
    }

    @Test
    void uninitializedInvalidAndDuplicateRequestsDoNotSilentlyCreateOrResetClocks() throws SQLException {
        assertTrue(simulation.load(settlement).isEmpty());
        assertThrows(SQLException.class, () -> simulation.advanceAging(settlement, 1, 1));
        assertThrows(SQLException.class, () -> simulation.initializeClock(UUID.randomUUID(), 1));
        simulation.initializeClock(settlement, 99);
        assertThrows(SQLException.class, () -> simulation.initializeClock(settlement, 100));
        assertThrows(IllegalArgumentException.class, () -> simulation.advanceAging(settlement, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> simulation.advanceAging(settlement, 1, 0));
        assertEquals(99, simulation.load(settlement).orElseThrow().seed());
        assertTrue(simulation.advanceAging(settlement, 2, 2).complete()); // Empty settlement is supported.
        assertThrows(IllegalArgumentException.class, () -> simulation.advanceAging(settlement, 1, 1));
        assertEquals(2, simulation.load(settlement).orElseThrow().completedSteps());
    }

    @Test
    void refusesFutureReplayVersionsAndInvalidStoredStepsWithoutChangingPeople() throws SQLException {
        Resident person = person(settlement, 1, Resident.Status.ALIVE);
        simulation.initializeClock(settlement, 9);
        try (var statement = database.getConnection().createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE population_clocks SET completed_steps = -1"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE population_clocks SET completed_steps = 1.5"));
            statement.executeUpdate("UPDATE population_clocks SET replay_version = 2");
        }
        assertThrows(SQLException.class, () -> simulation.load(settlement));
        assertThrows(SQLException.class, () -> simulation.advanceAging(settlement, 10, 10));
        assertEquals(person, people.findResident(person.id()).orElseThrow());
        assertTrue(database.getConnection().getAutoCommit());
    }

    @Test
    void anotherSettlementAndCallerTransactionAreUnaffected() throws SQLException {
        UUID other = settlement();
        Resident person = person(other, 50, Resident.Status.ALIVE);
        simulation.initializeClock(settlement, 1);
        simulation.advanceAging(settlement, 1, 1);
        assertEquals(person, people.findResident(person.id()).orElseThrow());
        assertTrue(simulation.load(other).isEmpty());
        database.getConnection().setAutoCommit(false);
        try {
            assertThrows(SQLException.class, () -> simulation.load(settlement));
            assertThrows(SQLException.class, () -> simulation.advanceAging(settlement, 2, 1));
            assertFalse(database.getConnection().getAutoCommit());
        } finally {
            database.getConnection().rollback();
            database.getConnection().setAutoCommit(true);
        }
    }

    @Test
    void snapshotRestoresSeedProgressAndAgeForContinuation() throws Exception {
        person(settlement, 5, Resident.Status.ALIVE);
        simulation.initializeClock(settlement, 123);
        simulation.advanceAging(settlement, 10, 4);
        Path snapshot = directory.resolve("clock-snapshot.db");
        DatabaseBackup.snapshot(directory.resolve("civilizations.db"), snapshot);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + snapshot);
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            PopulationSimulationRepository restored = new PopulationSimulationRepository(connection);
            assertEquals(simulation.load(settlement), restored.load(settlement));
            assertEquals(simulation.advanceAging(settlement, 10, 10), restored.advanceAging(settlement, 10, 10));
        }
    }

    @Test
    void versionFiveMigrationFailurePreservesPopulationAndRetries() throws SQLException {
        Resident person = person(settlement, 5, Resident.Status.ALIVE);
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("DROP TABLE population_clocks");
            statement.execute("PRAGMA user_version = 5");
            statement.execute("CREATE TABLE population_clocks (settlement_id TEXT)");
        }
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(database.getConnection()));
        try (var statement = database.getConnection().createStatement()) {
            try (var result = statement.executeQuery("PRAGMA user_version")) {
                assertEquals(5, result.getInt(1));
            }
            statement.execute("DROP TABLE population_clocks");
        }
        new SchemaMigrator().migrate(database.getConnection());
        assertEquals(person, people.findResident(person.id()).orElseThrow());
        assertTrue(simulation.load(settlement).isEmpty());
    }

    private UUID settlement() throws SQLException {
        UUID id = UUID.randomUUID();
        database.saveSettlement(new Settlement(id, "Test", "temporary-world", 0, 64, 0, 0, 0, 0));
        return id;
    }

    private Resident person(UUID settlementId, long age, Resident.Status status) throws SQLException {
        Resident person = new Resident(UUID.randomUUID(), "Test Person", settlementId, null, age, status);
        people.insertResident(person);
        return person;
    }
}
