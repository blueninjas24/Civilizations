package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.FamilyTie;
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

import static com.blueninjas24.civilizations.population.FamilyTie.Type.*;
import static org.junit.jupiter.api.Assertions.*;

class FamilyRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private PopulationRepository repository;
    private UUID settlement;

    @BeforeEach
    void initialize() throws SQLException {
        open();
        settlement = settlement();
    }

    private void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(), Logger.getLogger("FamilyTest"));
        database.initialize();
        repository = database.getPopulationRepository();
    }

    @AfterEach void close() { database.close(); }

    @Test
    void parentAndPartnerLinksSurviveRestartDeathAndSnapshot() throws Exception {
        Resident parent = person(settlement);
        Resident child = person(settlement()); // Families need not share a settlement or household.
        Resident partner = person(settlement);
        FamilyTie ancestry = new FamilyTie(parent.id(), child.id(), PARENT_CHILD);
        FamilyTie partnership = new FamilyTie(parent.id(), partner.id(), PARTNER);
        repository.insertFamilyTie(ancestry);
        repository.insertFamilyTie(partnership);
        repository.updateResident(new Resident(parent.id(), parent.name(), parent.settlementId(), null,
                parent.ageSteps(), Resident.Status.DECEASED));
        database.close();
        open();
        assertEquals(List.of(ancestry), repository.findFamilyTies(child.id()));
        assertEquals(List.of(partnership), repository.findFamilyTies(partner.id()));
        Path snapshot = directory.resolve("family-snapshot.db");
        DatabaseBackup.snapshot(directory.resolve("civilizations.db"), snapshot);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + snapshot);
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            PopulationRepository restored = new PopulationRepository(connection);
            assertEquals(repository.findFamilyTies(parent.id()), restored.findFamilyTies(parent.id()));
            try (var result = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(result.next());
            }
        }
    }

    @Test
    void partnersAreCanonicalAndCannotBeDuplicatedInReverse() throws SQLException {
        UUID a = person(settlement).id();
        UUID b = person(settlement).id();
        FamilyTie forward = new FamilyTie(a, b, PARTNER);
        FamilyTie reverse = new FamilyTie(b, a, PARTNER);
        assertEquals(forward, reverse);
        repository.insertFamilyTie(forward);
        assertThrows(SQLException.class, () -> repository.insertFamilyTie(reverse));
        assertEquals(List.of(forward), repository.findFamilyTies(a));
        assertThrows(UnsupportedOperationException.class, () -> repository.findFamilyTies(a).clear());
    }

    @Test
    void rejectsAncestryCyclesAtAnyDepthButAcceptsSharedAncestors() throws SQLException {
        UUID grandparent = person(settlement).id();
        UUID parent = person(settlement).id();
        UUID child = person(settlement).id();
        repository.insertFamilyTie(new FamilyTie(grandparent, parent, PARENT_CHILD));
        repository.insertFamilyTie(new FamilyTie(parent, child, PARENT_CHILD));
        assertThrows(SQLException.class,
                () -> repository.insertFamilyTie(new FamilyTie(child, parent, PARENT_CHILD)));
        assertThrows(SQLException.class,
                () -> repository.insertFamilyTie(new FamilyTie(child, grandparent, PARENT_CHILD)));
        UUID secondParent = person(settlement).id();
        repository.insertFamilyTie(new FamilyTie(grandparent, secondParent, PARENT_CHILD));
        repository.insertFamilyTie(new FamilyTie(secondParent, child, PARENT_CHILD));
        assertEquals(2, repository.findFamilyTies(grandparent).size());
        assertTrue(database.getConnection().getAutoCommit());
    }

    @Test
    void derivesUniqueSiblingsThroughSharedParentsWithoutUsingHouseholdOrPartner() throws SQLException {
        UUID parent = person(settlement).id();
        UUID otherParent = person(settlement).id();
        UUID child = person(settlement).id();
        UUID sibling = person(settlement()).id();
        UUID halfSibling = person(settlement).id();
        UUID unrelatedPartner = person(settlement).id();
        for (UUID p : List.of(parent, otherParent)) {
            repository.insertFamilyTie(new FamilyTie(p, child, PARENT_CHILD));
            repository.insertFamilyTie(new FamilyTie(p, sibling, PARENT_CHILD));
        }
        repository.insertFamilyTie(new FamilyTie(parent, halfSibling, PARENT_CHILD));
        repository.insertFamilyTie(new FamilyTie(child, unrelatedPartner, PARTNER));
        List<UUID> expected = List.of(sibling, halfSibling).stream()
                .sorted(java.util.Comparator.comparing(UUID::toString)).toList();
        assertEquals(expected, repository.findSiblings(child));
        assertTrue(repository.findSiblings(unrelatedPartner).isEmpty());
        assertTrue(repository.findSiblings(UUID.randomUUID()).isEmpty());
    }

    @Test
    void rejectsUnknownRelativesSelfLinksAndInvalidSqlWithoutOverwritingHistory() throws SQLException {
        UUID a = person(settlement).id();
        UUID b = person(settlement).id();
        assertThrows(IllegalArgumentException.class, () -> new FamilyTie(a, a, PARENT_CHILD));
        assertThrows(NullPointerException.class, () -> new FamilyTie(a, b, null));
        assertThrows(SQLException.class,
                () -> repository.insertFamilyTie(new FamilyTie(a, UUID.randomUUID(), PARENT_CHILD)));
        repository.insertFamilyTie(new FamilyTie(a, b, PARENT_CHILD));
        try (var statement = database.getConnection().createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE family_ties SET type = 'PARTNER'"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM residents"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO family_ties VALUES ('" + a + "', '" + a + "', 'PARENT_CHILD')"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO family_ties VALUES ('" + a + "', '" + b + "', 'UNKNOWN')"));
        }
        assertEquals(List.of(new FamilyTie(a, b, PARENT_CHILD)), repository.findFamilyTies(a));
    }

    @Test
    void failedFamilyMigrationPreservesVersionFourResidentsAndCanRetry() throws SQLException {
        Resident person = person(settlement);
        try (var statement = database.getConnection().createStatement()) {
            // Reconstruct the previous-version shape inside this temporary database only.
            statement.execute("DROP TABLE family_ties");
            statement.execute("PRAGMA user_version = 4");
            statement.execute("CREATE TABLE family_ties (first_id TEXT)");
        }
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(database.getConnection()));
        assertEquals(person, repository.findResident(person.id()).orElseThrow());
        try (var statement = database.getConnection().createStatement()) {
            try (var result = statement.executeQuery("PRAGMA user_version")) {
                assertEquals(4, result.getInt(1));
            }
            statement.execute("DROP TABLE family_ties");
        }
        new SchemaMigrator().migrate(database.getConnection());
        assertEquals(person, repository.findResident(person.id()).orElseThrow());
        assertTrue(repository.findFamilyTies(person.id()).isEmpty());
        new SchemaMigrator().migrate(database.getConnection()); // Idempotent reopen.
    }

    private Resident person(UUID settlementId) throws SQLException {
        Resident person = new Resident(UUID.randomUUID(), "Test Person", settlementId, null, 0, Resident.Status.ALIVE);
        repository.insertResident(person);
        return person;
    }

    private UUID settlement() throws SQLException {
        UUID id = UUID.randomUUID();
        database.saveSettlement(new Settlement(id, "Test", "temporary-world", 0, 64, 0, 0, 0, 0));
        return id;
    }
}
