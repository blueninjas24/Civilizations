package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class VillageEnrollmentRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private VillageEnrollmentRepository enrollment;
    private final UUID world = UUID.randomUUID(), firstEntity = UUID.randomUUID(), secondEntity = UUID.randomUUID();
    private static VillageObservation.Position bed(int x) { return new VillageObservation.Position(x, 64, 0); }
    private VillageObservation observed(boolean complete) {
        return new VillageObservation(world, "test-world", 1000, complete, List.of(bed(0), bed(2)), List.of(
                new VillageObservation.Person(firstEntity, bed(0), true, "Existing Name"),
                new VillageObservation.Person(secondEntity, bed(2), true, null)));
    }
    private VillageEnrollmentRepository.Result enroll(VillageObservation observation) throws SQLException {
        return enrollment.enroll(observation, VillageRecognition.recognize(observation, VillageRecognition.Policy.prototype()).candidates().getFirst(),
                VillageEnrollmentRepository.Policy.prototype());
    }
    private void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(), Logger.getLogger("EnrollmentTest")); database.initialize();
        enrollment = database.getVillageEnrollmentRepository();
    }
    @BeforeEach void initialize() throws SQLException { open(); }
    @AfterEach void close() { database.close(); }

    @Test void newVillageCreatesStablePeopleHomesCapacityAndApprovedPartnershipInOneImport() throws SQLException {
        var result = enroll(observed(true));
        assertTrue(result.createdSettlement()); assertEquals(2, result.importedPeople()); assertEquals(1, result.createdPartnerships());
        var people = database.getPopulationRepository();
        var residents = people.findResidents(result.settlementId());
        assertEquals(2, residents.size());
        assertEquals(1, people.findHouseholds(result.settlementId()).size());
        assertTrue(residents.stream().allMatch(p -> p.ageSteps() == 240));
        assertEquals("Existing Name", result.observedResidents().get(firstEntity).name());
        assertEquals(world, database.getVillageIdentityRepository().findWorld(result.settlementId()).orElseThrow());
        var state = database.getDemographicRepository().readPopulationState();
        assertEquals(4, state.householdCapacity().values().iterator().next());
        assertEquals(1, state.familyTies().size());
        assertEquals(2, database.findNearbySettlement("test-world", 0, 0, 10).getPopulation());
        assertEquals(2, database.findNearbySettlement("test-world", 0, 0, 10).getBeds());
    }

    @Test void restartWorldRenameAndRepeatedObservationNeverDuplicateOrResetPeople() throws SQLException {
        var first = enroll(observed(true));
        var original = first.observedResidents().get(firstEntity);
        var changed = new Resident(original.id(), original.name(), first.settlementId(), original.householdId(), 300, Resident.Status.ALIVE);
        database.getPopulationRepository().updateResident(changed);
        database.close(); open();
        var source = observed(true);
        var renamed = new VillageObservation(world, "renamed-world", 2000, true, source.beds(), source.villagers());
        var result = enroll(renamed);
        assertFalse(result.createdSettlement()); assertEquals(first.settlementId(), result.settlementId());
        assertEquals(0, result.importedPeople()); assertEquals(0, result.createdPartnerships());
        assertEquals(changed, result.observedResidents().get(firstEntity));
        assertEquals(2, database.getPopulationRepository().countLivingResidents(first.settlementId()));
    }

    @Test void partialObservationsPreservePeopleAndInvalidateUnknownHousingRatherThanCensusZero() throws SQLException {
        var first = enroll(observed(true));
        enroll(observed(false));
        assertEquals(2, database.getPopulationRepository().countLivingResidents(first.settlementId()));
        assertTrue(database.getDemographicRepository().readPopulationState().householdCapacity().isEmpty());
        enroll(observed(true));
        assertEquals(4, database.getDemographicRepository().readPopulationState().householdCapacity().values().iterator().next());
    }

    @Test void deceasedEntityEvidenceIsRetainedWithoutResurrectingOrReimportingIt() throws SQLException {
        var first = enroll(observed(true)); var original = first.observedResidents().get(firstEntity);
        database.getPopulationRepository().updateResident(new Resident(original.id(), original.name(), first.settlementId(), original.householdId(),
                original.ageSteps(), Resident.Status.DECEASED));
        var repeated = enroll(observed(true));
        assertEquals(0, repeated.importedPeople());
        assertEquals(Resident.Status.DECEASED, repeated.observedResidents().get(firstEntity).status());
        assertEquals(1, database.getPopulationRepository().countLivingResidents(first.settlementId()));
    }

    @Test void failedFinalSaveRollsBackEveryIdentityPersonHomeAndFamilyThenRetries() throws SQLException {
        try (var statement = database.getConnection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_enrollment BEFORE UPDATE ON settlements BEGIN SELECT RAISE(ABORT, 'injected'); END");
        }
        assertThrows(SQLException.class, () -> enroll(observed(true)));
        assertTrue(database.getDemographicRepository().readPopulationState().residents().isEmpty());
        assertTrue(database.getDemographicRepository().readPopulationState().households().isEmpty());
        assertTrue(database.getVillageIdentityRepository().findResident(world, firstEntity).isEmpty());
        assertEquals(VillageIdentityRepository.MatchType.NEW, database.getVillageIdentityRepository().match(world,
                VillageRecognition.recognize(observed(true), VillageRecognition.Policy.prototype()).candidates().getFirst()).type());
        try (var statement = database.getConnection().createStatement()) { statement.execute("DROP TRIGGER fail_enrollment"); }
        assertEquals(2, enroll(observed(true)).importedPeople());
    }

    @Test void unboundLegacySettlementBlocksNewEnrollmentWithoutAdoptionOrReset() throws SQLException {
        UUID legacy = UUID.randomUUID(); database.saveSettlement(new Settlement(legacy, "Legacy", "old-folder", 50000, 64, 0, 0, 5, 5));
        assertThrows(SQLException.class, () -> enroll(observed(true)));
        assertTrue(database.getVillageIdentityRepository().findWorld(legacy).isEmpty());
        assertTrue(database.getDemographicRepository().readPopulationState().residents().isEmpty());
        assertEquals(5, database.findNearbySettlement("old-folder", 50000, 0, 10).getPopulation());
    }

    @Test void childrenKeepUnknownAncestryAndPairingCanBeDisabled() throws SQLException {
        var source = observed(true);
        var childSource = new VillageObservation(world, "test-world", 1000, true, source.beds(), List.of(source.villagers().getFirst(),
                new VillageObservation.Person(UUID.randomUUID(), bed(2), false, "Child")));
        var result = enroll(childSource);
        assertEquals(0, result.createdPartnerships());
        assertTrue(database.getDemographicRepository().readPopulationState().familyTies().isEmpty());
        assertEquals(0, result.observedResidents().values().stream().filter(p -> p.name().equals("Child")).findFirst().orElseThrow().ageSteps());
        var disabled = new VillageEnrollmentRepository.Policy(8, 2, 240, 0, new PartnershipFormation.Policy(false, 216));
        var adults = observed(true);
        var extra = enrollment.enroll(adults, VillageRecognition.recognize(adults, VillageRecognition.Policy.prototype()).candidates().getFirst(), disabled);
        assertEquals(0, extra.createdPartnerships());
    }

    @Test void separatedHomesStayDistinctAndNestedTransactionsAreRejected() throws SQLException {
        var source = observed(true);
        var wider = new VillageObservation(world, "test-world", 1000, true, List.of(bed(0), bed(2), bed(20)), source.villagers());
        var result = enroll(wider);
        assertEquals(2, database.getPopulationRepository().findHouseholds(result.settlementId()).size());
        assertEquals(2, database.getDemographicRepository().readPopulationState().householdCapacity().size());
        database.getConnection().setAutoCommit(false);
        assertThrows(SQLException.class, () -> enroll(wider));
        database.getConnection().rollback(); database.getConnection().setAutoCommit(true);
    }

    @Test void enrollmentFeedsSavedBirthsAndCalendarWithoutRecountingVisibleEntities() throws SQLException {
        var imported = enroll(observed(true));
        var policy = new PopulationPolicy(12, 216, 240, 480, 720, 1, 0, 0);
        var simulation = database.getDemographicRepository(); simulation.initializeClock(42, policy);
        var clock = database.getCivilizationClockRepository(); clock.initialize(1000, 30);
        var queued = clock.accrue(1030, 30, Long.MAX_VALUE);
        simulation.advance(queued.anchor().targetSteps(), 1, policy);
        assertEquals(3, database.getPopulationRepository().countLivingResidents(imported.settlementId()));
        var repeated = enroll(observed(true));
        assertEquals(0, repeated.importedPeople());
        assertEquals(3, database.findNearbySettlement("test-world", 0, 0, 10).getPopulation());
        assertEquals(1, simulation.findEvents(0, 100).size());
    }

    @Test void newlyConnectingBedsCannotSilentlyMergeExistingHouseholds() throws SQLException {
        var source = observed(true);
        var separate = new VillageObservation(world, "test-world", 1000, true, List.of(bed(0), bed(2), bed(20)), source.villagers());
        var first = enroll(separate);
        var before = database.getDemographicRepository().readPopulationState();
        var connected = new VillageObservation(world, "test-world", 2000, true,
                List.of(bed(0), bed(2), bed(8), bed(14), bed(20)), source.villagers());
        assertThrows(SQLException.class, () -> enroll(connected));
        assertEquals(before, database.getDemographicRepository().readPopulationState());
        assertEquals(2, database.getPopulationRepository().findHouseholds(first.settlementId()).size());
    }

    @Test void schemaElevenUpgradePreservesIdentityAndRetriesColumnConflict() throws SQLException {
        var first = enroll(observed(true));
        SchemaFixtures.rewind(database.getConnection(), 11);
        try (var statement = database.getConnection().createStatement()) { statement.execute("ALTER TABLE village_beds ADD COLUMN household_id TEXT"); }
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(database.getConnection()));
        try (var statement = database.getConnection().createStatement(); var result = statement.executeQuery("PRAGMA user_version")) {
            assertEquals(11, result.getInt(1));
        }
        try (var statement = database.getConnection().createStatement()) { statement.execute("ALTER TABLE village_beds DROP COLUMN household_id"); }
        new SchemaMigrator().migrate(database.getConnection());
        assertEquals(world, database.getVillageIdentityRepository().findWorld(first.settlementId()).orElseThrow());
        assertEquals(first.observedResidents().get(firstEntity).id(), database.getVillageIdentityRepository().findResident(world, firstEntity).orElseThrow());
        assertEquals(2, database.getPopulationRepository().countLivingResidents(first.settlementId()));
    }
}
