package com.blueninjas24.civilizations.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticHomePlanningSettingsTest {
    private YamlConfiguration yaml(String text)throws Exception{var y=new YamlConfiguration();y.loadFromString(text);return y;}
    private String defaults()throws Exception{try(var in=getClass().getResourceAsStream("/construction.yml")){return new String(in.readAllBytes(),StandardCharsets.UTF_8).replace("\r\n","\n");}}
    @Test void newInstallDefaultsAreBoundedAndExistingFilesRequireExplicitOptIn()throws Exception{
        var settings=AutomaticHomePlanningSettings.read(yaml(defaults()));assertTrue(settings.enabled());assertEquals(128,settings.cellsPerTick());assertEquals(1,settings.maximumActiveHomes());assertEquals(12,settings.searchRadius());
        assertFalse(AutomaticHomePlanningSettings.read(yaml("format: 1")).enabled());
        assertFalse(AutomaticHomePlanningSettings.read(yaml(defaults().replace("automatic-home-planning:\n  enabled: true","automatic-home-planning:\n  enabled: false"))).enabled());
        assertNotNull(ConstructionBudgetConfiguration.read(yaml(defaults())));
    }
    @Test void malformedUnknownAndUnboundedSettingsFailClosed()throws Exception{
        var text=defaults();
        for(var bad:new String[]{text.replace("cells-per-tick: 128","cells-per-tick: 63"),text.replace("review-seconds: 30","review-seconds: 1.5"),
                text.replace("maximum-active-homes: 1","maximum-active-homes: 0"),text.replace("search-radius: 12","search-radius: 13"),
                text.replace("search-radius: 12","search-raduis: 12"),text.replace("automatic-home-planning:\n  enabled: true","automatic-home-planning:\n  enabled: yes"),
                "automatic-home-planning: true"}){
            // SnakeYAML treats YAML1.1 'yes' as a boolean; quote it to test a textual flag explicitly.
            if(bad.contains("  enabled: yes"))bad=bad.replace("  enabled: yes","  enabled: 'yes'");
            var invalid=bad;assertThrows(IllegalArgumentException.class,()->AutomaticHomePlanningSettings.read(yaml(invalid)));
        }
    }
}
