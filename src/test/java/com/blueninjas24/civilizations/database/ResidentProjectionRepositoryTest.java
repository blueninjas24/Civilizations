package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class ResidentProjectionRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private ResidentProjectionRepository projections;
    private final UUID world = UUID.randomUUID(), firstEntity = UUID.randomUUID(), secondEntity = UUID.randomUUID();
    private UUID settlement, home, person;
    private final VillageObservation.Position position = new VillageObservation.Position(0, 64, 0);
    private final PopulationPolicy policy = new PopulationPolicy(12, 216, 240, 480, 720, 1, 0, 0);
    private void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(), Logger.getLogger("ProjectionTest")); database.initialize();
        projections = database.getResidentProjectionRepository();
    }
    @BeforeEach void setup() throws SQLException {
        open();
        var observation = new VillageObservation(world, "test-world", 1000, true,
                List.of(position, new VillageObservation.Position(2, 64, 0)), List.of(
                new VillageObservation.Person(firstEntity, position, true, "Alice"),
                new VillageObservation.Person(secondEntity, position, true, "Bob")));
        var imported = database.getVillageEnrollmentRepository().enroll(observation,
                VillageRecognition.recognize(observation, VillageRecognition.Policy.prototype()).candidates().getFirst(),
                VillageEnrollmentRepository.Policy.prototype());
        settlement = imported.settlementId(); person = imported.observedResidents().get(firstEntity).id();
        home = imported.observedResidents().get(firstEntity).householdId();
        database.getDemographicRepository().initializeClock(42, policy);
    }
    @AfterEach void close() { database.close(); }
    private UUID newborn() throws SQLException {
        database.getDemographicRepository().advance(1, 1, policy);
        return database.getPopulationRepository().findResidents(settlement).stream().filter(p -> p.ageSteps() == 0).findFirst().orElseThrow().id();
    }

    @Test void importsHaveOneActiveIncarnationAndImmutableHomeAndAliasSnapshots() throws SQLException {
        var saved = projections.snapshot();
        assertEquals(2, saved.projections().size());
        assertEquals(person, saved.aliases().get(new ResidentProjectionRepository.EntityKey(world, firstEntity)));
        assertEquals(ResidentProjectionRepository.State.ACTIVE, saved.projections().get(person).state());
        assertEquals(position, saved.homes().get(home).position());
        assertThrows(UnsupportedOperationException.class, () -> saved.projections().clear());
        assertFalse(projections.claim(person, world, UUID.randomUUID(), home, position));
        database.close(); open(); assertEquals(saved, projections.snapshot());
    }

    @Test void claimSurvivesRestartAndPreventsSecondSpawnUntilExplicitRetirement() throws SQLException {
        UUID child = newborn(), entity = UUID.randomUUID();
        assertTrue(projections.claim(child, world, entity, home, position));
        database.close(); open();
        assertEquals(ResidentProjectionRepository.State.CLAIMED, projections.snapshot().projections().get(child).state());
        assertFalse(projections.claim(child, world, UUID.randomUUID(), home, position));
        assertFalse(projections.activate(child, UUID.randomUUID()));
        assertTrue(projections.activate(child, entity));
        assertFalse(projections.activate(child, entity));
        assertFalse(projections.retire(child, UUID.randomUUID()));
        assertTrue(projections.retire(child, entity));
        assertFalse(projections.claim(child, world, entity, home, position));
        UUID replacement = UUID.randomUUID(); assertTrue(projections.claim(child, world, replacement, home, position));
        assertEquals(child, projections.snapshot().aliases().get(new ResidentProjectionRepository.EntityKey(world, entity)));
        assertFalse(projections.recordPhysicalDeath(world, entity, 2000, "FIRE"));
        assertTrue(projections.activate(child, replacement));
    }

    @Test void wrongWorldHomeAndDeceasedPeopleCannotBeClaimedOrActivated() throws SQLException {
        UUID child = newborn(), entity = UUID.randomUUID();
        assertFalse(projections.claim(child, UUID.randomUUID(), entity, home, position));
        assertFalse(projections.claim(child, world, entity, UUID.randomUUID(), position));
        assertTrue(projections.claim(child, world, entity, home, position));
        var resident = database.getPopulationRepository().findResident(child).orElseThrow();
        database.getPopulationRepository().updateResident(new Resident(child, resident.name(), settlement, home, 0, Resident.Status.DECEASED));
        assertFalse(projections.activate(child, entity));
        assertTrue(projections.retire(child, entity));
        assertFalse(projections.claim(child, world, UUID.randomUUID(), home, position));
    }

    @Test void physicalDeathAtMonthZeroIsIdempotentAndPreservesFamilyHistory() throws SQLException {
        var before = database.getDemographicRepository().readPopulationState();
        assertFalse(projections.recordPhysicalDeath(world, UUID.randomUUID(), 1500, "FIRE"));
        assertTrue(projections.recordPhysicalDeath(world, firstEntity, 1500, "FIRE"));
        assertFalse(projections.recordPhysicalDeath(world, firstEntity, 1600, "FIRE"));
        var after = database.getDemographicRepository().readPopulationState();
        assertEquals(before.familyTies(), after.familyTies());
        var original = before.residents().stream().filter(p -> p.id().equals(person)).findFirst().orElseThrow();
        var deceased = database.getPopulationRepository().findResident(person).orElseThrow();
        assertEquals(original.name(), deceased.name()); assertEquals(original.ageSteps(), deceased.ageSteps());
        assertEquals(Resident.Status.DECEASED, deceased.status());
        assertEquals(1, database.findNearbySettlement("test-world", 0, 0, 10).getPopulation());
        assertEquals(0, projections.recentDeaths(10).getFirst().completedMonth());
        assertEquals(1, projections.recentDeaths(10).size());
        database.close(); open(); assertEquals(1, projections.recentDeaths(10).size());
        assertFalse(projections.recordPhysicalDeath(world, firstEntity, 1700, "FIRE"));
    }

    @Test void deathAndClaimFailuresRollBackAllChangesAndPermitRetry() throws SQLException {
        var before = projections.snapshot();
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_count BEFORE UPDATE ON settlements BEGIN SELECT RAISE(ABORT, 'injected'); END");
        }
        assertThrows(SQLException.class, () -> projections.recordPhysicalDeath(world, firstEntity, 1500, "FIRE"));
        assertEquals(before, projections.snapshot()); assertTrue(projections.recentDeaths(10).isEmpty());
        assertEquals(Resident.Status.ALIVE, database.getPopulationRepository().findResident(person).orElseThrow().status());
        try (var statement = database.getConnection().createStatement()) { statement.execute("DROP TRIGGER fail_count"); }
        UUID child = newborn(), entity = UUID.randomUUID();
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_claim BEFORE INSERT ON resident_projection BEGIN SELECT RAISE(ABORT, 'injected'); END");
        }
        assertThrows(SQLException.class, () -> projections.claim(child, world, entity, home, position));
        assertFalse(projections.snapshot().aliases().containsKey(new ResidentProjectionRepository.EntityKey(world, entity)));
        try (var statement = database.getConnection().createStatement()) { statement.execute("DROP TRIGGER fail_claim"); }
        assertTrue(projections.claim(child, world, entity, home, position));
    }

    @Test void schemaTwelveUpgradePreservesPeopleAndQuarantinesAmbiguousAliases() throws SQLException {
        var before = database.getDemographicRepository().readPopulationState();
        SchemaFixtures.rewind(database.getConnection(), 12);
        database.getVillageIdentityRepository().bindEntity(world, UUID.randomUUID(), person);
        new SchemaMigrator().migrate(database.getConnection());
        assertEquals(before, database.getDemographicRepository().readPopulationState());
        assertFalse(projections.snapshot().projections().containsKey(person));
        assertEquals(1, projections.snapshot().projections().size());
        assertFalse(projections.claim(person, world, UUID.randomUUID(), home, position));
    }

    @Test void failedMigrationRollsBackItsNewTablesAndVersionWithoutTouchingPeople() throws SQLException {
        var before = database.getDemographicRepository().readPopulationState();
        SchemaFixtures.rewind(database.getConnection(), 12);
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("CREATE TABLE resident_lifecycle_events(dummy TEXT)");
        }
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(database.getConnection()));
        try (var statement = database.getConnection().createStatement()) {
            try (var result = statement.executeQuery("PRAGMA user_version")) { assertEquals(12, result.getInt(1)); }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name = 'resident_projection'")) { assertEquals(0, result.getInt(1)); }
            statement.execute("DROP TABLE resident_lifecycle_events");
        }
        assertEquals(before, database.getDemographicRepository().readPopulationState());
        new SchemaMigrator().migrate(database.getConnection());
        assertEquals(2, projections.snapshot().projections().size());
    }

    @Test void birthsUpdateSettlementCountWithoutWaitingForAnotherObservation() throws SQLException {
        newborn(); assertEquals(3, database.findNearbySettlement("test-world", 0, 0, 10).getPopulation());
        UUID legacy = UUID.randomUUID(); database.saveSettlement(new Settlement(legacy, "Legacy", "legacy-world", 1000, 64, 0, 0, 7, 7));
        database.getDemographicRepository().advance(2, 1, policy);
        assertEquals(7, database.findNearbySettlement("legacy-world", 1000, 0, 10).getPopulation());
    }

    @Test void movingLastPeopleAwayClearsEnrolledSourceCountButPreservesTheirProjectionAliases() throws SQLException {
        UUID destination = UUID.randomUUID(), destinationHome = UUID.randomUUID();
        database.saveSettlement(new Settlement(destination, "Destination", "test-world", 1000, 64, 0, 0, 0, 2));
        database.getVillageIdentityRepository().bindSettlement(destination, world);
        database.getPopulationRepository().insertHousehold(new Household(destinationHome, destination, "Destination home"));
        for (var resident : database.getPopulationRepository().findResidents(settlement)) {
            database.getPopulationRepository().updateMigratedResident(new Resident(resident.id(), resident.name(), destination,
                    destinationHome, resident.ageSteps(), resident.status()), settlement);
        }
        DemographicRepository.synchronizeSettlementCounts(database.getConnection());
        assertEquals(0, database.findNearbySettlement("test-world", 0, 0, 10).getPopulation());
        assertEquals(2, database.findNearbySettlement("test-world", 1000, 0, 10).getPopulation());
        assertEquals(person, projections.snapshot().aliases().get(new ResidentProjectionRepository.EntityKey(world, firstEntity)));
    }
}
