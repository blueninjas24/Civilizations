package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

class PopulationConfigurationTest {
    static YamlConfiguration defaults() throws Exception {
        var config = new YamlConfiguration();
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                PopulationConfigurationTest.class.getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        return config;
    }

    @Test void bundledYamlMatchesTheDocumentedPrototype() throws Exception {
        assertEquals(PopulationSession.Settings.prototype(), PopulationConfiguration.read(defaults()));
    }

    @ParameterizedTest @ValueSource(strings = {"30000", "30.5", "true"})
    void cadenceMustBeAnActualIntegerRatherThanAStringOrTruncatedDecimal(String raw) throws Exception {
        var config = defaults();
        Object invalid = raw.equals("30.5") ? 30.5 : raw.equals("true") ? true : raw;
        config.set("population.millis-per-month", invalid);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> PopulationConfiguration.read(config))
                .getMessage().contains("population.millis-per-month"));
    }

    @Test void oversizedBudgetsCannotWrapIntoSmallPositiveIntegers() throws Exception {
        var config = defaults(); config.set("population.months-per-batch", 4_294_967_298L);
        assertThrows(IllegalArgumentException.class, () -> PopulationConfiguration.read(config));
    }

    @Test void invalidProbabilitiesAndRecognitionBoundsAreRejected() throws Exception {
        var config = defaults(); config.set("population.birth-chance", Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> PopulationConfiguration.read(config));
        var invalidBounds = defaults(); invalidBounds.set("recognition.maximum-beds", 513);
        assertThrows(IllegalArgumentException.class, () -> PopulationConfiguration.read(invalidBounds));
    }

    @Test void missingValuesAndQuotedBooleansAreRejected() throws Exception {
        var config = defaults(); config.set("population.seed", null);
        assertThrows(IllegalArgumentException.class, () -> PopulationConfiguration.read(config));
        var quoted = defaults(); quoted.set("enrollment.form-partnerships", "false");
        assertThrows(IllegalArgumentException.class, () -> PopulationConfiguration.read(quoted));
    }

    @Test void editableSettingsReachTheWorkerRatherThanBeingIgnored() throws Exception {
        var config = defaults(); config.set("population.seed", -44L);
        config.set("population.millis-per-month", 60_000L);
        config.set("population.birth-chance", 0);
        config.set("enrollment.form-partnerships", false);
        var actual = PopulationConfiguration.read(config);
        assertEquals(-44, actual.seed()); assertEquals(60_000, actual.millisPerMonth());
        assertEquals(0, actual.population().birthChance()); assertFalse(actual.enrollment().partnerships().enabled());
    }
}
