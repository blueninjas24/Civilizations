package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {
    @TempDir
    Path temporaryDirectory;

    private Path dataFolder;
    private DatabaseManager database;

    @BeforeEach
    void initializeDatabase() throws SQLException {
        // Every test uses a fresh JUnit-owned directory, never a server path.
        dataFolder = temporaryDirectory.resolve("plugin-data");
        database = new DatabaseManager(dataFolder.toFile(), Logger.getLogger("CivilizationsTest"));
        database.initialize();
    }

    @AfterEach
    void closeDatabase() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void initializesDatabaseAndSettlementSchema() throws SQLException {
        assertTrue(Files.isRegularFile(dataFolder.resolve("civilizations.db")));
        assertFalse(database.getConnection().isClosed());
        try (var statement = database.getConnection().createStatement();
             var result = statement.executeQuery("SELECT id, name, world, center_x, center_y, center_z, "
                     + "discovered_at, population, beds FROM settlements")) {
            assertFalse(result.next());
        }
    }

    @Test
    void insertsNewSettlement() throws SQLException {
        Settlement settlement = settlement();
        database.saveSettlement(settlement);
        try (var statement = database.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM settlements WHERE id = ?")) {
            statement.setString(1, settlement.getId().toString());
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }

    @Test
    void persistsAllFieldsAcrossReopening() throws SQLException {
        Settlement expected = settlement();
        database.saveSettlement(expected);
        database.close();
        database = new DatabaseManager(dataFolder.toFile(), Logger.getLogger("CivilizationsTest"));
        database.initialize();

        Settlement actual = find(expected);
        assertNotNull(actual);
        assertAll(
                () -> assertEquals(expected.getId(), actual.getId()),
                () -> assertEquals(expected.getName(), actual.getName()),
                () -> assertEquals(expected.getWorld(), actual.getWorld()),
                () -> assertEquals(expected.getCenterX(), actual.getCenterX()),
                () -> assertEquals(expected.getCenterY(), actual.getCenterY()),
                () -> assertEquals(expected.getCenterZ(), actual.getCenterZ()),
                () -> assertEquals(expected.getDiscoveredAt(), actual.getDiscoveredAt()),
                () -> assertEquals(expected.getPopulation(), actual.getPopulation()),
                () -> assertEquals(expected.getBeds(), actual.getBeds())
        );
    }

    @Test
    void retrievesWithinRadiusAndExcludesOtherWorldsAndOutsideRadius() throws SQLException {
        Settlement expected = settlement();
        database.saveSettlement(expected);
        Settlement boundary = database.findNearbySettlement(expected.getWorld(),
                expected.getCenterX() + 3, expected.getCenterZ() + 4, 5);
        assertNotNull(boundary);
        assertEquals(expected.getId(), boundary.getId());
        assertNull(database.findNearbySettlement(expected.getWorld(),
                expected.getCenterX() + 3, expected.getCenterZ() + 4, 4));
        assertNull(database.findNearbySettlement("other-world",
                expected.getCenterX(), expected.getCenterZ(), 128));
    }

    @Test
    void updatesPopulationWithoutChangingBedsOrAnotherSettlement() throws SQLException {
        Settlement expected = settlement();
        Settlement other = otherSettlement();
        database.saveSettlement(expected);
        database.saveSettlement(other);
        expected.setPopulation(27);
        database.updatePopulation(expected);
        Settlement actual = find(expected);
        assertNotNull(actual);
        assertEquals(27, actual.getPopulation());
        assertEquals(13, actual.getBeds());
        assertEquals(other.getPopulation(), find(other).getPopulation());
    }

    @Test
    void updatesBedsWithoutChangingPopulationOrAnotherSettlement() throws SQLException {
        Settlement expected = settlement();
        Settlement other = otherSettlement();
        database.saveSettlement(expected);
        database.saveSettlement(other);
        expected.setBeds(31);
        database.updateBeds(expected);
        Settlement actual = find(expected);
        assertNotNull(actual);
        assertEquals(31, actual.getBeds());
        assertEquals(11, actual.getPopulation());
        assertEquals(other.getBeds(), find(other).getBeds());
    }

    private Settlement find(Settlement settlement) throws SQLException {
        return database.findNearbySettlement(settlement.getWorld(),
                settlement.getCenterX(), settlement.getCenterZ(), 0);
    }

    private Settlement settlement() {
        return new Settlement(UUID.randomUUID(), "Stonevale's Haven", "test-world",
                -321, 72, 654, 1_790_000_123_456L, 11, 13);
    }

    private Settlement otherSettlement() {
        return new Settlement(UUID.randomUUID(), "Oakford", "test-world",
                2048, 64, -2048, 1_790_000_000_000L, 4, 7);
    }
}
