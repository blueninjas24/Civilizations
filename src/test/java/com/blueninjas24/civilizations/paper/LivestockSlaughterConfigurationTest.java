package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.LivestockSlaughterPolicy;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockSlaughterConfigurationTest {
    private YamlConfiguration config() throws Exception {
        var config = new YamlConfiguration();
        try (var reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/economy.yml")), StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        return config;
    }

    @Test void packagedDefaultsAndEditsAreExposed() throws Exception {
        var c = config();
        EconomyConfiguration.read(c);
        assertEquals(LivestockSlaughterPolicy.prototype(), EconomyConfiguration.readLivestockSlaughter(c));
        c.set("livestock-slaughter.interval-ticks", 240);
        c.set("livestock-slaughter.candidates-per-review", 32);
        c.set("livestock-slaughter.evidence-max-age-millis", 2500);
        assertEquals(new LivestockSlaughterPolicy(true, 240, 32, 2500), EconomyConfiguration.readLivestockSlaughter(c));
        EconomyConfiguration.read(c);
    }

    @Test void legacyMissingSectionDefaultsDisabled() throws Exception {
        var c = config();
        c.set("livestock-slaughter", null);
        assertEquals(LivestockSlaughterPolicy.disabled(), EconomyConfiguration.readLivestockSlaughter(c));
        EconomyConfiguration.read(c);
        assertFalse(c.contains("livestock-slaughter"));
    }

    @Test void rejectsInvalidTypesFractionsUnknownKeysAndBounds() throws Exception {
        for (var entry : java.util.Map.<String, Object>of(
                "enabled", "true", "interval-ticks", 19, "candidates-per-review", 257,
                "evidence-max-age-millis", 99).entrySet()) {
            var c = config();
            c.set("livestock-slaughter." + entry.getKey(), entry.getValue());
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(c), entry.getKey());
        }
        var fractional = config();
        fractional.set("livestock-slaughter.interval-ticks", 100.5);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(fractional));
        var extra = config();
        extra.set("livestock-slaughter.allow-offline", true);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(extra));
        for (var entry : java.util.Map.<String, Object>of(
                "interval-ticks", 1201, "candidates-per-review", 0,
                "evidence-max-age-millis", 30001).entrySet()) {
            var c = config();
            c.set("livestock-slaughter." + entry.getKey(), entry.getValue());
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(c), entry.getKey());
        }
    }
}
