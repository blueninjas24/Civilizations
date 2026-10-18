package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.ForestryPolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

class ForestryConfigurationTest {
    @Test
    void shippedPrototypeIsEnabledAndEditable() throws Exception {
        var config = defaults();
        assertEquals(ForestryPolicy.prototype(), EconomyConfiguration.readForestryResources(config));
        EconomyConfiguration.read(config);

        config.set("forestry-resources.recovery-basis-points", 250);
        config.set("forestry-resources.units-per-worker", 6);
        EconomyConfiguration.read(config);
        var policy = EconomyConfiguration.readForestryResources(config);
        assertEquals(250, policy.recoveryBasisPoints());
        assertEquals(6, policy.unitsPerWorker());
    }

    @Test
    void missingSectionKeepsLegacyInstallationDisabled() throws Exception {
        var config = defaults();
        config.set("forestry-resources", null);
        EconomyConfiguration.read(config);
        assertEquals(ForestryPolicy.disabled(), EconomyConfiguration.readForestryResources(config));
        assertFalse(EconomyConfiguration.readForestryResources(config).enabled());
    }

    @Test
    void malformedValuesAndUnknownKeysAreRejected() throws Exception {
        for (Object value : new Object[]{-1, 10001, 1.5, "many", true}) {
            var config = defaults();
            config.set("forestry-resources.recovery-basis-points", value);
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        }
        var wrongType = defaults();
        wrongType.set("forestry-resources.enabled", "yes");
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(wrongType));
        var unknown = defaults();
        unknown.set("forestry-resources.free-regrowth", true);
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
