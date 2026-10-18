package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.OreObserverPolicy;
import com.blueninjas24.civilizations.industry.QuarryFoundingPolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class QuarryFoundingConfigurationTest {
    @Test void shippedConfigEnablesBoundedQuarryFoundingAndIndependentSurveys() throws Exception {
        var config = defaults();
        assertEquals(QuarryFoundingPolicy.prototype(), EconomyConfiguration.readQuarryFounding(config));
        assertEquals(new OreObserverPolicy(true, 12, 2048, 200, 1), EconomyConfiguration.readQuarryObserver(config));
        assertTrue(EconomyConfiguration.readCareerStudy(config).industries().containsAll(java.util.Set.of("quarry", "sandpit")));
        assertTrue(EconomyConfiguration.readBusinessStaffing(config).industries().containsAll(java.util.Set.of("quarry", "sandpit")));
        var learnedSkills = EconomyConfiguration.readWorkLearning(config).industrySkills();
        assertEquals("mining", learnedSkills.get("quarry"));
        assertEquals("mining", learnedSkills.get("sandpit"));
        EconomyConfiguration.read(config);
    }

    @Test void missingSectionsDisableBothFeaturesForLegacyConfigs() throws Exception {
        var config = defaults();
        config.set("quarry-founding", null);
        config.set("quarry-observer", null);
        assertFalse(EconomyConfiguration.readQuarryFounding(config).enabled());
        assertFalse(EconomyConfiguration.readQuarryObserver(config).enabled());
        EconomyConfiguration.read(config);
    }

    @Test void foundingSettingsAreStrictAndHonorMineBounds() throws Exception {
        var config = defaults();
        config.set("quarry-founding.horizontal-range", 128);
        config.set("quarry-founding.minimum-age-months", 300);
        assertEquals(128, EconomyConfiguration.readQuarryFounding(config).horizontalRange());
        assertEquals(300, EconomyConfiguration.readQuarryFounding(config).minimumAgeSteps());
        EconomyConfiguration.read(config);

        for (var path : new String[]{"enabled", "settlements-per-pulse", "patches-per-review", "founders-per-review",
                "survey-lifetime-seconds", "horizontal-range", "vertical-range", "minimum-age-months",
                "founder-mining-experience", "worker-capacity", "startup-hundredths", "capital-hundredths"}) {
            var bad = defaults();
            bad.set("quarry-founding." + path, path.equals("enabled") ? "yes" : 1.5);
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(bad), path);
        }
        for (var entry : new Object[][]{{"horizontal-range", 257}, {"vertical-range", 129},
                {"patches-per-review", 65}, {"worker-capacity", 33}, {"survey-lifetime-seconds", 3601}}) {
            var bad = defaults();
            bad.set("quarry-founding." + entry[0], entry[1]);
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(bad), (String) entry[0]);
        }
        var extra = defaults();
        extra.set("quarry-founding.free-excavation", true);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(extra));
    }

    @Test void quarryObserverUsesOreObserverBoundsAndRejectsUnknownSettings() throws Exception {
        var config = defaults();
        config.set("quarry-observer.vertical-radius", 128);
        config.set("quarry-observer.cells-per-tick", 4096);
        config.set("quarry-observer.interval-ticks", 72000);
        config.set("quarry-observer.horizontal-radius-chunks", 2);
        assertEquals(new OreObserverPolicy(true, 128, 4096, 72000, 2), EconomyConfiguration.readQuarryObserver(config));
        EconomyConfiguration.read(config);

        for (var entry : new Object[][]{{"vertical-radius", 129}, {"cells-per-tick", 4097},
                {"interval-ticks", 19}, {"horizontal-radius-chunks", 3}}) {
            var bad = defaults();
            bad.set("quarry-observer." + entry[0], entry[1]);
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(bad), (String) entry[0]);
        }
        var extra = defaults();
        extra.set("quarry-observer.scan-player-claims", true);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(extra));
    }

    private YamlConfiguration defaults() throws Exception {
        var config = new YamlConfiguration();
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/economy.yml"));
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        return config;
    }
}
