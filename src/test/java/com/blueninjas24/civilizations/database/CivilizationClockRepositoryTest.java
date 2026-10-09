package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.PopulationPolicy;
import com.blueninjas24.civilizations.simulation.CivilizationTime;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class CivilizationClockRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private CivilizationClockRepository clock;
    private final PopulationPolicy policy = PopulationPolicy.prototype();

    private void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(), Logger.getLogger("ClockTest"));
        database.initialize();
        clock = database.getCivilizationClockRepository();
    }
    @BeforeEach void initialize() throws SQLException { open(); }
    @AfterEach void close() { database.close(); }

    @Test void explicitInitializationRequiresDemographicClockAndDoesNotReinitialize() throws SQLException {
        assertTrue(clock.load(30).isEmpty());
        assertThrows(SQLException.class, () -> clock.initialize(1000, 30));
        database.getDemographicRepository().initializeClock(42, policy);
        database.getDemographicRepository().advance(8, 8, policy);
        clock.initialize(1000, 30);
        assertEquals(new CivilizationTime.Anchor(1000, 0, 8), clock.load(30).orElseThrow());
        assertThrows(SQLException.class, () -> clock.initialize(2000, 30));
        assertThrows(SQLException.class, () -> clock.load(60));
    }

    @Test void onlineAdoptionAuditsLegacyBacklogAndNeverAddsDowntime() throws SQLException {
        database.getDemographicRepository().initializeClock(42,policy);
        clock.initialize(1000,30); clock.accrue(1100,30,Long.MAX_VALUE);
        assertEquals(new CivilizationTime.Anchor(1_000_000,10,0),clock.resumeOnline(1_000_000,30));
        try(var s=database.getConnection().createStatement();var r=s.executeQuery("SELECT legacy_target,adopted_completed FROM online_calendar")) {
            assertEquals(3,r.getLong(1));assertEquals(0,r.getLong(2));
        }
        assertThrows(SQLException.class,()->clock.accrue(1_000_030,30,Long.MAX_VALUE));
        assertThrows(SQLException.class,()->database.getDemographicRepository().advance(1,1,policy));
        var world=java.util.UUID.randomUUID();
        clock.accrueOnline(1_000_020,30,java.util.Set.of(world));
        assertEquals(1,clock.resumeOnline(2_000_000,30).targetSteps());
        assertEquals(java.util.Set.of(world),clock.activeWorlds().apply(1));
    }

    @Test void failedOnlineAccrualRollsBackWorldMaskAndTimeTogether() throws SQLException {
        database.getDemographicRepository().initializeClock(42,policy);clock.initialize(1000,30);clock.resumeOnline(1000,30);
        try(var s=database.getConnection().createStatement()) { s.execute("CREATE TRIGGER fail_online BEFORE UPDATE ON civilization_time BEGIN SELECT RAISE(ABORT,'injected'); END"); }
        assertThrows(SQLException.class,()->clock.accrueOnline(1090,30,java.util.Set.of(java.util.UUID.randomUUID())));
        assertEquals(0,clock.load(30).orElseThrow().targetSteps());
        assertThrows(IllegalStateException.class,()->clock.activeWorlds().apply(1));
        assertTrue(database.getConnection().getAutoCommit());
    }

    @Test void missingMaskCannotSilentlyAgeAllWorldsAndMigrationFailureKeepsVersion28() throws SQLException {
        database.getDemographicRepository().initializeClock(42,policy);clock.initialize(1000,30);
        SchemaFixtures.rewind(database.getConnection(),28);
        try(var s=database.getConnection().createStatement()) { s.execute("CREATE TABLE population_active_worlds(conflict TEXT)"); }
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(database.getConnection()));
        try(var s=database.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")) { assertEquals(28,r.getInt(1)); }
        try(var s=database.getConnection().createStatement()) { s.execute("DROP TABLE population_active_worlds"); }
        new SchemaMigrator().migrate(database.getConnection());clock.resumeOnline(1000,30);
        try(var s=database.getConnection().createStatement()) { s.execute("UPDATE civilization_time SET target_steps=1"); }
        assertThrows(IllegalStateException.class,()->database.getDemographicRepository().advance(1,1,policy,
                com.blueninjas24.civilizations.population.MigrationPolicy.disabled(),clock.activeWorlds()));
        assertEquals(0,database.getDemographicRepository().load(policy).orElseThrow().completedSteps());
    }

    @Test void fractionalTimeAndQueuedCatchUpSurviveRestartsWithoutRepeating() throws SQLException {
        database.getDemographicRepository().initializeClock(42, policy);
        clock.initialize(1000, 30);
        clock.accrue(1019, 30, Long.MAX_VALUE);
        database.close(); open();
        var queued = clock.accrue(1100, 30, Long.MAX_VALUE);
        assertEquals(new CivilizationTime.Anchor(1100, 10, 3), queued.anchor());
        var first = database.getDemographicRepository().advance(queued.anchor().targetSteps(), 1, policy);
        assertEquals(2, first.remainingSteps());
        database.close(); open();
        assertEquals(queued.anchor(), clock.load(30).orElseThrow());
        assertEquals(2, database.getDemographicRepository().advance(clock.load(30).orElseThrow().targetSteps(), 1, policy)
                .checkpoint().completedSteps());
        assertEquals(0, clock.accrue(1100, 30, Long.MAX_VALUE).addedSteps());
        database.getDemographicRepository().advance(3, 1, policy);
        assertEquals(0, database.getDemographicRepository().advance(3, 1, policy).processedSteps());
    }

    @Test void failedPopulationCatchUpPreservesQueuedTimeAndCanRetry() throws SQLException {
        database.getDemographicRepository().initializeClock(42, policy);
        clock.initialize(1000, 30);
        clock.accrue(1090, 30, Long.MAX_VALUE);
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_progress BEFORE UPDATE ON demographic_clock BEGIN SELECT RAISE(ABORT, 'injected'); END");
        }
        assertThrows(SQLException.class, () -> database.getDemographicRepository().advance(3, 1, policy));
        assertEquals(3, clock.load(30).orElseThrow().targetSteps());
        assertEquals(0, database.getDemographicRepository().load(policy).orElseThrow().completedSteps());
        try (var statement = database.getConnection().createStatement()) { statement.execute("DROP TRIGGER fail_progress"); }
        assertEquals(3, database.getDemographicRepository().advance(3, 3, policy).checkpoint().completedSteps());
    }

    @Test void failedAccrualRollsBackBothTimestampAndQueuedTarget() throws SQLException {
        database.getDemographicRepository().initializeClock(42, policy);
        clock.initialize(1000, 30);
        var before = clock.load(30);
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_time BEFORE UPDATE ON civilization_time BEGIN SELECT RAISE(ABORT, 'injected'); END");
        }
        assertThrows(SQLException.class, () -> clock.accrue(1090, 30, Long.MAX_VALUE));
        assertEquals(before, clock.load(30));
        assertTrue(database.getConnection().getAutoCommit());
        try (var statement = database.getConnection().createStatement()) { statement.execute("DROP TRIGGER fail_time"); }
        assertEquals(3, clock.accrue(1090, 30, Long.MAX_VALUE).addedSteps());
    }

    @Test void snapshotsRetainFractionAndBacklog() throws Exception {
        database.getDemographicRepository().initializeClock(42, policy);
        clock.initialize(1000, 30);
        clock.accrue(1100, 30, Long.MAX_VALUE);
        Path snapshot = directory.resolve("calendar-snapshot.db");
        DatabaseBackup.snapshot(directory.resolve("civilizations.db"), snapshot);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + snapshot)) {
            assertEquals(clock.load(30), new CivilizationClockRepository(connection).load(30));
        }
    }

    @Test void cappedOfflineElapsedAndBackwardsClockPersistWithoutRecrediting() throws SQLException {
        database.getDemographicRepository().initializeClock(42, policy);
        clock.initialize(1000, 30);
        assertEquals(910, clock.accrue(2000, 30, 90).discardedMillis());
        database.close(); open();
        assertEquals(new CivilizationTime.Anchor(2000, 0, 3), clock.accrue(1500, 30, 90).anchor());
        assertEquals(0, clock.accrue(2000, 30, 90).addedSteps());
        assertEquals(1, clock.accrue(2030, 30, 90).addedSteps());
    }

    @Test void targetOverflowRollsBackTimestampAndKeepsConnectionUsable() throws SQLException {
        database.getDemographicRepository().initializeClock(42, policy);
        clock.initialize(1000, 1);
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("UPDATE civilization_time SET target_steps = 9223372036854775807 WHERE id = 1");
        }
        var before = clock.load(1);
        assertThrows(ArithmeticException.class, () -> clock.accrue(1001, 1, Long.MAX_VALUE));
        assertEquals(before, clock.load(1));
        assertTrue(database.getConnection().getAutoCommit());
    }

    @Test void nestedTransactionsAndUnexpectedManualProgressFailExplicitly() throws SQLException {
        database.getDemographicRepository().initializeClock(42, policy);
        clock.initialize(1000, 30);
        database.getConnection().setAutoCommit(false);
        assertThrows(SQLException.class, () -> clock.accrue(1010, 30, Long.MAX_VALUE));
        assertThrows(SQLException.class, () -> clock.load(30));
        database.getConnection().rollback(); database.getConnection().setAutoCommit(true);
        database.getDemographicRepository().advance(1, 1, policy);
        assertThrows(SQLException.class, () -> clock.load(30));
    }

    @Test void versionNineUpgradePreservesClockAndRetriesAfterFailure() throws SQLException {
        database.getDemographicRepository().initializeClock(42, policy);
        database.getDemographicRepository().advance(2, 2, policy);
        SchemaFixtures.rewind(database.getConnection(), 9);
        try (var statement = database.getConnection().createStatement()) { statement.execute("CREATE TABLE civilization_time(conflict TEXT)"); }
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(database.getConnection()));
        try (var statement = database.getConnection().createStatement(); var result = statement.executeQuery("PRAGMA user_version")) {
            assertEquals(9, result.getInt(1));
        }
        try (var statement = database.getConnection().createStatement()) { statement.execute("DROP TABLE civilization_time"); }
        new SchemaMigrator().migrate(database.getConnection());
        assertEquals(2, database.getDemographicRepository().load(policy).orElseThrow().completedSteps());
        clock.initialize(1000, 30);
        assertEquals(2, clock.load(30).orElseThrow().targetSteps());
    }
}
