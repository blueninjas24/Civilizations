package com.blueninjas24.civilizations.database;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Headless snapshot utility; never initializes, migrates or restores the source database. */
public final class DatabaseBackup {
    private DatabaseBackup() {}

    public static void snapshot(Path source, Path destination) throws SQLException, IOException {
        Path sourceFile = source.toAbsolutePath().normalize();
        Path destinationFile = destination.toAbsolutePath().normalize();
        if (!Files.isRegularFile(sourceFile)) {
            throw new IOException("Backup source must be an existing SQLite file: " + sourceFile);
        }
        if (Files.exists(destinationFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Backup destination already exists: " + destinationFile);
        }
        Path parent = destinationFile.getParent();
        if (!Files.isDirectory(parent)) {
            throw new IOException("Backup destination directory must already exist: " + parent);
        }

        // A private staging file prevents exposing an interrupted snapshot as a finished backup.
        Path staging = Files.createTempFile(parent, ".civilizations-backup-", ".db");
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(true);
            config.setBusyTimeout(5000);
            try (Connection connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + sourceFile, config.toProperties());
                 var statement = connection.prepareStatement("VACUUM main INTO ?")) {
                statement.setString(1, staging.toString());
                statement.execute();
            }
            try (Connection backup = DriverManager.getConnection(
                    "jdbc:sqlite:" + staging, config.toProperties());
                 var statement = backup.createStatement();
                 var result = statement.executeQuery("PRAGMA integrity_check")) {
                if (!result.next() || !"ok".equals(result.getString(1)) || result.next()) {
                    throw new SQLException("SQLite backup integrity check failed");
                }
            }
            // Default move refuses replacement; even an empty existing destination is protected.
            Files.move(staging, destinationFile);
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    public static void main(String[] arguments) throws SQLException, IOException {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Usage: DatabaseBackup <existing-source.db> <new-backup.db>");
        }
        snapshot(Path.of(arguments[0]), Path.of(arguments[1]));
        System.out.println("SQLite snapshot completed and integrity checked.");
    }
}
