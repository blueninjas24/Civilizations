package com.blueninjas24.civilizations.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseBackupIT {
    @TempDir
    Path directory;

    @Test
    void packagedJarRunsBackupWithoutPaperOrTestDependencies() throws Exception {
        Path source = directory.resolve("source.db");
        Path backup = directory.resolve("backup.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + source);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sample (value TEXT)");
            statement.execute("INSERT INTO sample VALUES ('packaged CLI')");
        }
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Path output = directory.resolve("cli-output.txt");
        Process process = new ProcessBuilder(java.toString(), "-cp",
                System.getProperty("civilizations.packagedJar"),
                DatabaseBackup.class.getName(), source.toString(), backup.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Packaged CLI timed out");
            assertEquals(0, process.exitValue(), () -> readOutput(output));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + backup);
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT value FROM sample")) {
            assertTrue(result.next());
            assertEquals("packaged CLI", result.getString(1));
        }
    }

    private String readOutput(Path output) {
        try {
            return Files.readString(output);
        } catch (java.io.IOException failure) {
            return failure.toString();
        }
    }
}
