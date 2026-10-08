package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.Household;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PopulationRepositoryTest {
    @TempDir
    Path directory;
    private DatabaseManager database;
    private PopulationRepository repository;
    private UUID settlement;

    @BeforeEach
    void initialize() throws SQLException {
        open();
        settlement = addSettlement();
    }

    private void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(), Logger.getLogger("PopulationTest"));
        database.initialize();
        repository = database.getPopulationRepository();
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void allResidentAndHouseholdFieldsSurviveRestart() throws SQLException {
        Household household = household(settlement);
        Resident resident = resident(settlement, household.id());
        repository.insertHouseholdWithResidents(household, List.of(resident));
        database.close();
        open();
        assertEquals(resident, repository.findResident(resident.id()).orElseThrow());
        assertEquals(List.of(household), repository.findHouseholds(settlement));
        assertEquals(List.of(resident), repository.findResidents(settlement));
        assertEquals(1, repository.countLivingResidents(settlement));
    }

    @Test
    void snapshotPreservesPeopleHouseholdsAndPopulationSchema() throws Exception {
        Household household = household(settlement);
        Resident person = resident(settlement, household.id());
        repository.insertHouseholdWithResidents(household, List.of(person));
        Path backup = directory.resolve("population-snapshot.db");
        DatabaseBackup.snapshot(directory.resolve("civilizations.db"), backup);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + backup);
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            PopulationRepository restored = new PopulationRepository(connection);
            assertEquals(person, restored.findResident(person.id()).orElseThrow());
            assertEquals(List.of(household), restored.findHouseholds(settlement));
            try (var result = statement.executeQuery("PRAGMA user_version")) {
                assertTrue(result.next());
                assertEquals(SchemaMigrator.CURRENT_VERSION, result.getInt(1));
            }
            try (var result = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(result.next());
            }
        }
    }

    @Test
    void nullableHouseholdAndVeryLargeAgeRoundTrip() throws SQLException {
        Resident person = new Resident(UUID.randomUUID(), "Élodie's Home", settlement, null,
                4_000_000_000L, Resident.Status.ALIVE);
        repository.insertResident(person);
        assertEquals(person, repository.findResident(person.id()).orElseThrow());
    }

    @Test
    void updatesKeepIdentityAndRetainDeceasedPeopleWithoutCountingThemAsLiving() throws SQLException {
        Resident person = resident(settlement, null);
        repository.insertResident(person);
        Household household = household(settlement);
        repository.insertHousehold(household);
        Resident updated = new Resident(person.id(), "Mara's New Name", settlement,
                household.id(), 900, Resident.Status.DECEASED);
        repository.updateResident(updated);
        database.close();
        open();
        assertEquals(updated, repository.findResident(person.id()).orElseThrow());
        assertEquals(List.of(updated), repository.findResidents(settlement));
        assertEquals(0, repository.countLivingResidents(settlement));
    }

    @Test
    void readsAreIsolatedBySettlementAndHaveStableUuidOrder() throws SQLException {
        UUID other = addSettlement();
        Resident later = new Resident(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                "Later", settlement, null, 2, Resident.Status.ALIVE);
        Resident earlier = new Resident(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Earlier", settlement, null, 3, Resident.Status.ALIVE);
        repository.insertResident(later);
        repository.insertResident(resident(other, null));
        repository.insertResident(earlier);
        assertEquals(List.of(earlier, later), repository.findResidents(settlement));
        assertEquals(2, repository.countLivingResidents(settlement));
        assertEquals(1, repository.countLivingResidents(other));
        assertThrows(UnsupportedOperationException.class, () -> repository.findResidents(settlement).clear());
    }

    @Test
    void rejectsMissingSettlementsAndUnknownHouseholds() throws SQLException {
        assertThrows(SQLException.class, () -> repository.insertHousehold(household(UUID.randomUUID())));
        assertThrows(SQLException.class, () -> repository.insertResident(resident(UUID.randomUUID(), null)));
        assertThrows(SQLException.class, () -> repository.insertResident(resident(settlement, UUID.randomUUID())));
        assertEquals(0, repository.countLivingResidents(settlement));
    }

    @Test
    void rejectsHouseholdInDifferentSettlement() throws SQLException {
        Household otherHousehold = household(addSettlement());
        repository.insertHousehold(otherHousehold);
        assertThrows(SQLException.class,
                () -> repository.insertResident(resident(settlement, otherHousehold.id())));
    }

    @Test
    void duplicateIdentityDoesNotOverwriteExistingPerson() throws SQLException {
        Resident person = resident(settlement, null);
        repository.insertResident(person);
        assertThrows(SQLException.class, () -> repository.insertResident(
                new Resident(person.id(), "Replacement", settlement, null, 0, Resident.Status.ALIVE)));
        assertEquals(person, repository.findResident(person.id()).orElseThrow());
    }

    @Test
    void householdBatchRollsBackEveryNewRowOnDuplicateResident() throws SQLException {
        Household household = household(settlement);
        Resident person = resident(settlement, household.id());
        assertThrows(SQLException.class,
                () -> repository.insertHouseholdWithResidents(household, List.of(person, person)));
        assertTrue(repository.findHouseholds(settlement).isEmpty());
        assertTrue(repository.findResidents(settlement).isEmpty());
        assertTrue(database.getConnection().getAutoCommit());
        repository.insertHouseholdWithResidents(household, List.of(person));
        assertEquals(List.of(person), repository.findResidents(settlement));
    }

    @Test
    void householdBatchValidatesMembershipBeforeWriting() throws SQLException {
        Household household = household(settlement);
        assertThrows(IllegalArgumentException.class, () -> repository.insertHouseholdWithResidents(
                household, List.of(resident(settlement, null))));
        assertTrue(repository.findHouseholds(settlement).isEmpty());
        assertTrue(database.getConnection().getAutoCommit());
    }

    @Test
    void refusesUnknownUpdatesAndImplicitSettlementTransfers() throws SQLException {
        Resident person = resident(settlement, null);
        assertThrows(SQLException.class, () -> repository.updateResident(person));
        repository.insertResident(person);
        Resident moved = new Resident(person.id(), person.name(), addSettlement(), null,
                person.ageSteps(), person.status());
        assertThrows(SQLException.class, () -> repository.updateResident(moved));
        assertEquals(person, repository.findResident(person.id()).orElseThrow());
        assertTrue(repository.findResident(UUID.randomUUID()).isEmpty());
    }

    @Test
    void referencedHouseholdsAndSettlementsCannotBeSilentlyDeleted() throws SQLException {
        Household household = household(settlement);
        repository.insertHouseholdWithResidents(household, List.of(resident(settlement, household.id())));
        try (var statement = database.getConnection().createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM households"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM settlements"));
        }
        assertEquals(1, repository.countLivingResidents(settlement));
    }

    @Test
    void rejectsInvalidStoredAgeAndStatusAndDisabledForeignKeys() throws SQLException {
        Resident person = resident(settlement, null);
        repository.insertResident(person);
        try (var statement = database.getConnection().createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE residents SET age_steps = -1"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE residents SET age_steps = 1.5"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE residents SET status = 'UNKNOWN'"));
            statement.execute("PRAGMA foreign_keys = OFF");
            assertThrows(SQLException.class, database::getPopulationRepository);
            statement.execute("PRAGMA foreign_keys = ON");
        }
        assertEquals(person, repository.findResident(person.id()).orElseThrow());
    }

    @Test
    void refusesNestedHouseholdTransactionsWithoutCommittingCallerWork() throws SQLException {
        database.getConnection().setAutoCommit(false);
        try {
            assertThrows(SQLException.class, () -> repository.insertHouseholdWithResidents(
                    household(settlement), List.of()));
            assertFalse(database.getConnection().getAutoCommit());
        } finally {
            database.getConnection().rollback();
            database.getConnection().setAutoCommit(true);
        }
        assertTrue(repository.findHouseholds(settlement).isEmpty());
    }

    private UUID addSettlement() throws SQLException {
        UUID id = UUID.randomUUID();
        database.saveSettlement(new Settlement(id, "Test Village", "temporary-world", 1, 64, 2,
                1000, 0, 0));
        return id;
    }

    private Household household(UUID settlementId) {
        return new Household(UUID.randomUUID(), settlementId, "Mara's Household");
    }

    private Resident resident(UUID settlementId, UUID householdId) {
        return new Resident(UUID.randomUUID(), "Mara", settlementId, householdId, 123456, Resident.Status.ALIVE);
    }
}
