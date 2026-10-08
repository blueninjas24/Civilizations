package com.blueninjas24.civilizations.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SimulationHarnessIT {
    @TempDir
    Path directory;

    @Test
    void packagedHeadlessProbeIsRepeatableAcrossBatchSizes() throws Exception {
        String expected = run("17");
        assertTrue(expected.startsWith("mode=synthetic-probe seed=42 completedSteps=10000 checksum="));
        assertEquals(expected, run("1000"));
    }

    private String run(String batchSize) throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path output = directory.resolve("probe-" + batchSize + ".txt");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-cp", System.getProperty("civilizations.packagedJar"), SimulationHarness.class.getName(),
                "42", "10000", batchSize).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Headless probe timed out");
            assertEquals(0, process.exitValue(), () -> readOutput(output));
            return Files.readString(output).trim();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
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
