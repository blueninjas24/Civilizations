package com.blueninjas24.civilizations.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseBackupTest {
    @TempDir
    Path directory;

    @Test
    void snapshotsCommittedWalDataAndSchemaVersionWithoutChangingSource() throws Exception {
        Path source = directory.resolve("source.db");
        Path backup = directory.resolve("backup's snapshot.db");
        try (Connection connection = createDatabase(source)) {
            execute(connection, "PRAGMA journal_mode = WAL");
            execute(connection, "PRAGMA wal_autocheckpoint = 0");
            execute(connection, "INSERT INTO test_data VALUES (1, 'committed')");
            assertTrue(Files.exists(directory.resolve("source.db-wal")));
            DatabaseBackup.snapshot(source, backup);
            try (Connection snapshot = connect(backup)) {
                assertEquals("committed", value(snapshot));
                assertEquals(3, version(snapshot));
                execute(snapshot, "UPDATE test_data SET value = 'backup-only'");
            }
            assertEquals("committed", value(connection));
            assertEquals(3, version(connection));
        }
        assertNoStagingFiles();
    }

    @Test
    void excludesUncommittedWritesFromAnotherConnection() throws Exception {
        Path source = directory.resolve("source.db");
        try (Connection connection = createDatabase(source)) {
            execute(connection, "PRAGMA journal_mode = WAL");
            execute(connection, "INSERT INTO test_data VALUES (1, 'before')");
            try (Connection writer = connect(source)) {
                writer.setAutoCommit(false);
                execute(writer, "UPDATE test_data SET value = 'not committed'");
                Path backup = directory.resolve("snapshot.db");
                DatabaseBackup.snapshot(source, backup);
                try (Connection snapshot = connect(backup)) {
                    assertEquals("before", value(snapshot));
                }
                writer.rollback();
            }
        }
    }

    @Test
    void refusesExistingDestinationEvenWhenEmpty() throws Exception {
        Path source = directory.resolve("source.db");
        try (Connection ignored = createDatabase(source)) {
            Path destination = Files.createFile(directory.resolve("existing.db"));
            assertThrows(IOException.class, () -> DatabaseBackup.snapshot(source, destination));
            assertEquals(0, Files.size(destination));
        }
        assertNoStagingFiles();
    }

    @Test
    void refusesToOverwriteSource() throws Exception {
        Path source = directory.resolve("source.db");
        try (Connection connection = createDatabase(source)) {
            assertThrows(IOException.class, () -> DatabaseBackup.snapshot(source, source));
            assertEquals(3, version(connection));
        }
    }

    @Test
    void missingSourceNeverCreatesDatabase() throws Exception {
        Path missing = directory.resolve("missing.db");
        Path backup = directory.resolve("backup.db");
        assertThrows(IOException.class, () -> DatabaseBackup.snapshot(missing, backup));
        assertFalse(Files.exists(missing));
        assertFalse(Files.exists(backup));
        assertNoStagingFiles();
    }

    @Test
    void invalidDatabaseDoesNotPublishBackupOrLeaveStagingFiles() throws Exception {
        Path source = directory.resolve("invalid.db");
        Files.writeString(source, "not a SQLite database");
        Path backup = directory.resolve("backup.db");
        assertThrows(SQLException.class, () -> DatabaseBackup.snapshot(source, backup));
        assertFalse(Files.exists(backup));
        assertEquals("not a SQLite database", Files.readString(source));
        assertNoStagingFiles();
    }

    @Test
    void missingDestinationDirectoryIsNotCreated() throws Exception {
        Path source = directory.resolve("source.db");
        Path destination = directory.resolve("missing/backup.db");
        try (Connection ignored = createDatabase(source)) {
            assertThrows(IOException.class, () -> DatabaseBackup.snapshot(source, destination));
        }
        assertFalse(Files.exists(destination.getParent()));
        assertNoStagingFiles();
    }

    private Connection createDatabase(Path path) throws SQLException {
        Connection connection = connect(path);
        execute(connection, "CREATE TABLE test_data (id INTEGER PRIMARY KEY, value TEXT NOT NULL)");
        execute(connection, "PRAGMA user_version = 3");
        return connection;
    }

    private Connection connect(Path path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String value(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT value FROM test_data WHERE id = 1")) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private int version(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA user_version")) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private void assertNoStagingFiles() throws IOException {
        try (var paths = Files.list(directory)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith(".civilizations-backup-")));
        }
    }
}
