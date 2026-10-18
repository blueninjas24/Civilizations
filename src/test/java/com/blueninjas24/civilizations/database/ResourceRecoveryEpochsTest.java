package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceRecoveryEpochsTest {
    private Connection connection() throws Exception {
        var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        ResourceRecoveryEpochs.createSchema(connection);
        return connection;
    }

    private void transition(Connection connection, boolean begin, boolean priorClean, boolean gap) throws Exception {
        connection.setAutoCommit(false);
        try {
            ResourceRecoveryEpochs.transitionRows(connection, "ore", begin, priorClean, gap);
            connection.commit();
        } finally {
            connection.setAutoCommit(true);
        }
    }

    @Test void gapRevokesHealthyEpochAndRepeatedGapDoesNotRotateAgain() throws Exception {
        try (var connection = connection()) {
            transition(connection, true, false, false);
            var epoch = new ResourceRecoveryEpochs(connection);
            var healthy = epoch.load("ore").orElseThrow();
            assertTrue(healthy.healthy());

            transition(connection, false, false, true);
            var gap = epoch.load("ore").orElseThrow();
            assertFalse(gap.healthy());
            assertNotEquals(healthy.generation(), gap.generation());

            transition(connection, false, false, true);
            assertEquals(gap, epoch.load("ore").orElseThrow());
        }
    }

    @Test void uncleanBeginStartsANewGeneration() throws Exception {
        try (var connection = connection()) {
            transition(connection, true, false, false);
            var epoch = new ResourceRecoveryEpochs(connection);
            UUID first = epoch.load("ore").orElseThrow().generation();
            transition(connection, true, false, false);
            var afterCrash = epoch.load("ore").orElseThrow();
            assertTrue(afterCrash.healthy());
            assertNotEquals(first, afterCrash.generation());
        }
    }

}
