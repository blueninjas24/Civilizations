package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class WorkProfileRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private PopulationRepository people;
    private Resident person;

    @BeforeEach void initialize() throws SQLException {
        open();
        UUID settlement = UUID.randomUUID();
        database.saveSettlement(new Settlement(settlement, "Test", "temporary-world", 0, 64, 0, 0, 0, 0));
        person = new Resident(UUID.randomUUID(), "Worker", settlement, null, 216, Resident.Status.ALIVE);
        people.insertResident(person);
    }
    private void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(), Logger.getLogger("WorkProfileTest"));
        database.initialize();
        people = database.getPopulationRepository();
    }
    @AfterEach void close() { database.close(); }

    @Test
    void skillsSurviveRestartAndRetryingTotalsDoesNotDoubleLearning() throws SQLException {
        assertEquals(new WorkProfile(person.id(), Map.of()), people.findWorkProfile(person.id()).orElseThrow());
        people.recordSkillExperience(person.id(), "farming", 10);
        people.recordSkillExperience(person.id(), "farming", 10);
        people.recordSkillExperience(person.id(), "carpentry", 4_000_000_000L);
        database.close();
        open();
        assertEquals(new WorkProfile(person.id(), Map.of("farming", 10L, "carpentry", 4_000_000_000L)),
                people.findWorkProfile(person.id()).orElseThrow());
        assertTrue(people.findWorkProfile(UUID.randomUUID()).isEmpty());
    }

    @Test
    void skillUpdatesAreIsolatedAndDeceasedKnowledgeRemains() throws SQLException {
        people.recordSkillExperience(person.id(), "farming", 5);
        people.recordSkillExperience(person.id(), "carpentry", 2);
        people.recordSkillExperience(person.id(), "farming", 8);
        people.updateResident(new Resident(person.id(), person.name(), person.settlementId(), null, 500, Resident.Status.DECEASED));
        var profile = people.findWorkProfile(person.id()).orElseThrow();
        assertEquals(Map.of("farming", 8L, "carpentry", 2L), profile.experience());
        assertFalse(WorkEligibility.evaluate(people.findResident(person.id()).orElseThrow(), profile, new JobRequirements(216, Map.of())).eligible());
    }

    @Test
    void snapshotsKeepSkillCountersForRestoredPeople() throws Exception {
        people.recordSkillExperience(person.id(), "farming", Long.MAX_VALUE);
        Path snapshot = directory.resolve("skills-snapshot.db");
        DatabaseBackup.snapshot(directory.resolve("civilizations.db"), snapshot);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + snapshot); var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            var restored = new PopulationRepository(connection);
            assertEquals(people.findWorkProfile(person.id()), restored.findWorkProfile(person.id()));
        }
    }

    @Test
    void badExperienceAndMissingPeopleFailWithoutOverwritingValidSkills() throws SQLException {
        people.recordSkillExperience(person.id(), "farming", 10);
        assertThrows(SQLException.class, () -> people.recordSkillExperience(UUID.randomUUID(), "farming", 1));
        assertThrows(IllegalArgumentException.class, () -> people.recordSkillExperience(person.id(), " ", 1));
        assertThrows(IllegalArgumentException.class, () -> people.recordSkillExperience(person.id(), "farming", -1));
        try (var statement = database.getConnection().createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE resident_skills SET experience = 1.5"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE resident_skills SET experience = -1"));
        }
        assertEquals(Map.of("farming", 10L), people.findWorkProfile(person.id()).orElseThrow().experience());
    }

    @Test
    void failedVersionEightMigrationPreservesPeopleAndRetries() throws SQLException {
        SchemaFixtures.rewind(database.getConnection(), 8);
        try (var statement = database.getConnection().createStatement()) { statement.execute("CREATE TABLE resident_skills (resident_id TEXT)"); }
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(database.getConnection()));
        assertEquals(person, people.findResident(person.id()).orElseThrow());
        try (var statement = database.getConnection().createStatement()) {
            try (var result = statement.executeQuery("PRAGMA user_version")) { assertEquals(8, result.getInt(1)); }
            statement.execute("DROP TABLE resident_skills");
        }
        new SchemaMigrator().migrate(database.getConnection());
        assertEquals(Map.of(), people.findWorkProfile(person.id()).orElseThrow().experience());
    }
}
