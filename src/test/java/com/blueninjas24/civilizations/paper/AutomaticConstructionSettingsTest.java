package com.blueninjas24.civilizations.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticConstructionSettingsTest {
    private YamlConfiguration yaml(String text)throws Exception{var y=new YamlConfiguration();y.loadFromString(text);return y;}
    @Test void existingFilesStayDisabledWithoutMutationAndNewDefaultsAreBounded()throws Exception{
        var old=yaml("format: 1");assertFalse(AutomaticConstructionSettings.read(old).enabled());assertFalse(old.contains("automatic-construction"));
        String defaults;try(var in=getClass().getResourceAsStream("/construction.yml")){defaults=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        var config=yaml(defaults);assertEquals(new AutomaticConstructionSettings(true,128,10,500),AutomaticConstructionSettings.read(config));assertNotNull(ConstructionBudgetConfiguration.read(config));
        config.set("automatic-construction.enabled",false);assertFalse(AutomaticConstructionSettings.read(config).enabled());
    }
    @Test void missingMistypedUnknownAndUnboundedControlsRefuseInsteadOfEnabling()throws Exception{
        String text="automatic-construction:\n  enabled: true\n  cells-per-tick: 128\n  review-seconds: 10\n  block-interval-ms: 500\n";
        for(var invalid:new String[]{"automatic-construction: true",text.replace("enabled: true","enabled: 'yes'"),text.replace("128","257"),text.replace("10","0"),text.replace("500","60001"),text.replace("500","1.5"),text.replace("block-interval-ms","block-intervals-ms"),text.replace("  review-seconds: 10\n","")})
            assertThrows(IllegalArgumentException.class,()->AutomaticConstructionSettings.read(yaml(invalid)));
    }
}
