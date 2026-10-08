package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class VillageIdentityRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private VillageIdentityRepository identity;
    private UUID world, settlement, entity;
    private Resident person;
    private static VillageObservation.Position bed(int x) { return new VillageObservation.Position(x, 64, 0); }
    private VillageRecognition.Candidate candidate(List<VillageObservation.Position> beds, UUID observedEntity) {
        return new VillageRecognition.Candidate(beds,
                observedEntity == null ? List.of() : List.of(new VillageObservation.Person(observedEntity, bed(0), true, null)), false);
    }
    private void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(), Logger.getLogger("VillageIdentityTest"));
        database.initialize(); identity = database.getVillageIdentityRepository();
    }
    @BeforeEach void initialize() throws SQLException {
        open(); world = UUID.randomUUID(); settlement = addSettlement("same-name"); entity = UUID.randomUUID();
        person = new Resident(UUID.randomUUID(), "Original", settlement, null, 240, Resident.Status.ALIVE);
        database.getPopulationRepository().insertResident(person);
        identity.observeWorld(world, "same-name"); identity.bindSettlement(settlement, world);
    }
    @AfterEach void close() { database.close(); }
    private UUID addSettlement(String worldName) throws SQLException {
        UUID id = UUID.randomUUID(); database.saveSettlement(new Settlement(id, "Test", worldName, 0, 64, 0, 0, 0, 0)); return id;
    }

    @Test void repeatEvidenceAndWorldRenameRetainSavedTownAndPersonAfterRestart() throws SQLException {
        identity.bindBed(world, bed(126), settlement); identity.bindBed(world, bed(130), settlement);
        identity.bindEntity(world, entity, person.id());
        identity.bindBed(world, bed(126), settlement); identity.bindEntity(world, entity, person.id());
        identity.observeWorld(world, "renamed-folder");
        database.close(); open();
        var result = identity.match(world, candidate(List.of(bed(130)), entity));
        assertEquals(new VillageIdentityRepository.Match(VillageIdentityRepository.MatchType.EXISTING, List.of(settlement)), result);
        assertEquals(person.id(), identity.findResident(world, entity).orElseThrow());
        assertEquals(world, identity.findWorld(settlement).orElseThrow());
        assertEquals(person, database.getPopulationRepository().findResident(person.id()).orElseThrow());
    }

    @Test void differentWorldWithSameFolderAndCoordinatesCannotMatchOrRebind() throws SQLException {
        UUID other = UUID.randomUUID(); identity.observeWorld(other, "same-name");
        identity.bindBed(world, bed(0), settlement); identity.bindEntity(world, entity, person.id());
        assertEquals(VillageIdentityRepository.MatchType.NEW, identity.match(other, candidate(List.of(bed(0)), entity)).type());
        assertThrows(SQLException.class, () -> identity.bindSettlement(settlement, other));
        assertThrows(SQLException.class, () -> identity.bindBed(other, bed(0), settlement));
        assertThrows(SQLException.class, () -> identity.bindEntity(other, UUID.randomUUID(), person.id()));
        assertEquals(world, identity.findWorld(settlement).orElseThrow());
    }

    @Test void overlappingSavedTownEvidenceReportsConflictWithoutMovingBedsOrPeople() throws SQLException {
        UUID second = addSettlement("same-name"); identity.bindSettlement(second, world);
        identity.bindBed(world, bed(0), settlement); identity.bindBed(world, bed(5), second);
        var result = identity.match(world, candidate(List.of(bed(5), bed(0)), null));
        assertEquals(VillageIdentityRepository.MatchType.CONFLICT, result.type());
        assertEquals(2, result.settlementIds().size());
        assertThrows(SQLException.class, () -> identity.bindBed(world, bed(0), second));
        assertEquals(settlement, identity.match(world, candidate(List.of(bed(0)), null)).settlementIds().getFirst());
        assertEquals(person, database.getPopulationRepository().findResident(person.id()).orElseThrow());
    }

    @Test void entityFallbackMatchesWithoutObservedOriginalBedsAndConflictsRemainVisible() throws SQLException {
        identity.bindEntity(world, entity, person.id());
        assertEquals(VillageIdentityRepository.MatchType.EXISTING, identity.match(world, candidate(List.of(bed(100)), entity)).type());
        UUID second = addSettlement("same-name"); identity.bindSettlement(second, world); identity.bindBed(world, bed(100), second);
        assertEquals(VillageIdentityRepository.MatchType.CONFLICT, identity.match(world, candidate(List.of(bed(100)), entity)).type());
        assertEquals(0, database.getPopulationRepository().countLivingResidents(second));
    }

    @Test void unknownReferencesAndEntityOverwriteFailWhileDeceasedIdentityRemains() throws SQLException {
        assertThrows(SQLException.class, () -> identity.bindSettlement(UUID.randomUUID(), world));
        assertThrows(SQLException.class, () -> identity.bindEntity(world, entity, UUID.randomUUID()));
        identity.bindEntity(world, entity, person.id());
        Resident other = new Resident(UUID.randomUUID(), "Other", settlement, null, 240, Resident.Status.ALIVE);
        database.getPopulationRepository().insertResident(other);
        assertThrows(SQLException.class, () -> identity.bindEntity(world, entity, other.id()));
        database.getPopulationRepository().updateResident(new Resident(person.id(), person.name(), settlement, null, 240, Resident.Status.DECEASED));
        assertEquals(person.id(), identity.findResident(world, entity).orElseThrow());
        assertEquals(person.id(), identity.findResident(world, entity).orElseThrow());
        assertTrue(identity.findResident(world, UUID.randomUUID()).isEmpty());
    }

    @Test void callerTransactionCanRollBackBindingsAndMatchingDoesNotCommitIt() throws SQLException {
        database.getConnection().setAutoCommit(false);
        identity.bindBed(world, bed(0), settlement); identity.bindEntity(world, entity, person.id());
        assertEquals(VillageIdentityRepository.MatchType.EXISTING, identity.match(world, candidate(List.of(bed(0)), entity)).type());
        assertFalse(database.getConnection().getAutoCommit());
        database.getConnection().rollback(); database.getConnection().setAutoCommit(true);
        assertEquals(VillageIdentityRepository.MatchType.NEW, identity.match(world, candidate(List.of(bed(0)), entity)).type());
    }

    @Test void consistentSnapshotsKeepEveryIdentityLink() throws Exception {
        identity.bindBed(world, bed(0), settlement); identity.bindEntity(world, entity, person.id());
        Path snapshot = directory.resolve("identity-snapshot.db");
        DatabaseBackup.snapshot(directory.resolve("civilizations.db"), snapshot);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + snapshot)) {
            var restored = new VillageIdentityRepository(connection);
            assertEquals(identity.match(world, candidate(List.of(bed(0)), entity)), restored.match(world, candidate(List.of(bed(0)), entity)));
            assertEquals(identity.findResident(world, entity), restored.findResident(world, entity));
        }
    }

    @Test void legacyRowsAreNotSilentlyBoundAndVersionTenUpgradeCanRetry() throws SQLException {
        UUID legacy = addSettlement("legacy-folder");
        SchemaFixtures.rewind(database.getConnection(), 10);
        try (var statement = database.getConnection().createStatement()) { statement.execute("CREATE TABLE observed_worlds(conflict TEXT)"); }
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(database.getConnection()));
        try (var statement = database.getConnection().createStatement(); var result = statement.executeQuery("PRAGMA user_version")) {
            assertEquals(10, result.getInt(1));
        }
        try (var statement = database.getConnection().createStatement()) { statement.execute("DROP TABLE observed_worlds"); }
        new SchemaMigrator().migrate(database.getConnection());
        assertTrue(identity.findWorld(legacy).isEmpty());
        assertEquals(person, database.getPopulationRepository().findResident(person.id()).orElseThrow());
    }

    @Test void migrationUsesBoundWorldIdsRatherThanRenamableOrDuplicateFolderNames() throws SQLException {
        var policy = new PopulationPolicy(12, 216, 240, 480, 720, 0, 0, 0);
        UUID destination = addSettlement("same-name"); UUID otherWorld = UUID.randomUUID();
        identity.observeWorld(otherWorld, "same-name"); identity.bindSettlement(destination, otherWorld);
        var home = new Household(UUID.randomUUID(), destination, "Vacant");
        database.getPopulationRepository().insertHousehold(home);
        var simulation = database.getDemographicRepository(); var migration = new MigrationPolicy(1, 0);
        simulation.recordCapacity(home.id(), 4); simulation.initializeClock(42, policy, migration);
        simulation.advance(1, 1, policy, migration);
        assertEquals(settlement, database.getPopulationRepository().findResident(person.id()).orElseThrow().settlementId());
        assertThrows(SQLException.class, () -> database.getPopulationRepository().updateMigratedResident(
                new Resident(person.id(), person.name(), destination, home.id(), 241, Resident.Status.ALIVE), settlement));
        UUID sameWorldDestination = addSettlement("renamed-folder"); identity.bindSettlement(sameWorldDestination, world);
        var safeHome = new Household(UUID.randomUUID(), sameWorldDestination, "Safe");
        database.getPopulationRepository().insertHousehold(safeHome); simulation.recordCapacity(safeHome.id(), 4);
        simulation.advance(2, 1, policy, migration);
        assertEquals(sameWorldDestination, database.getPopulationRepository().findResident(person.id()).orElseThrow().settlementId());
    }
}
