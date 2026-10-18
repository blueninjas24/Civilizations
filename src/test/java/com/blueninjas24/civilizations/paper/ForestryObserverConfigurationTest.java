package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.ForestScan;
import com.blueninjas24.civilizations.industry.ForestryObserverPolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

class ForestryObserverConfigurationTest {
    @Test
    void shippedObserverDefaultsMatchPrototypeAndMissingLegacySectionOptsOut() throws Exception {
        var config = defaults();
        assertEquals(new ForestryObserverPolicy(true, 1, 24, 512, 200,
                new ForestScan.Geometry(3, 3, 2, 512)), EconomyConfiguration.readForestryObserver(config));
        EconomyConfiguration.read(config);

        config.set("forestry-observer", null);
        EconomyConfiguration.read(config);
        assertEquals(ForestryObserverPolicy.disabled(), EconomyConfiguration.readForestryObserver(config));
    }

    @Test
    void malformedObserverBoundsTypesAndKeysAreRejected() throws Exception {
        var invalid = List.of(
                Map.entry("horizontal-radius-chunks", -1), Map.entry("horizontal-radius-chunks", 3),
                Map.entry("vertical-radius", 3), Map.entry("vertical-radius", 65),
                Map.entry("cells-per-tick", 0), Map.entry("cells-per-tick", 4097),
                Map.entry("interval-ticks", 19),
                Map.entry("minimum-vertical-logs", 1), Map.entry("minimum-vertical-logs", 33),
                Map.entry("horizontal-canopy-radius", 0), Map.entry("horizontal-canopy-radius", 9),
                Map.entry("vertical-canopy-radius", 0), Map.entry("vertical-canopy-radius", 9),
                Map.entry("maximum-trunk-coordinates", 0), Map.entry("maximum-trunk-coordinates", 513));
        for (var entry : invalid) {
            var config = defaults();
            config.set("forestry-observer." + entry.getKey(), entry.getValue());
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config), entry.toString());
        }
        var wrongType = defaults();
        wrongType.set("forestry-observer.enabled", "yes");
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(wrongType));
        var unknown = defaults();
        unknown.set("forestry-observer.claim-land", true);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(unknown));
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
