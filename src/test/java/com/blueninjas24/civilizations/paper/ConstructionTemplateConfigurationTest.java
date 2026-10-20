package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ConstructionTemplate;
import com.blueninjas24.civilizations.economy.Commodity;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionTemplateConfigurationTest {
    @org.junit.jupiter.api.Test void originalFamilyHomeHasFourBedAnchorsAndRequiresRealWool()throws Exception {
        var home=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        org.junit.jupiter.api.Assertions.assertEquals(120,home.blockCount());
        org.junit.jupiter.api.Assertions.assertEquals(4,home.anchors().stream().filter(a->a.role()==com.blueninjas24.civilizations.construction.ConstructionTemplate.Role.BED).count());
        org.junit.jupiter.api.Assertions.assertEquals(12,home.materials().get(new com.blueninjas24.civilizations.economy.Commodity("wool")));
        org.junit.jupiter.api.Assertions.assertEquals(36,home.materials().get(com.blueninjas24.civilizations.economy.Commodity.TIMBER));
    }
    private String source(String id)throws Exception {
        try(var input=getClass().getResourceAsStream("/construction/"+id+".yml")){assertNotNull(input);return new String(input.readAllBytes(),StandardCharsets.UTF_8);}
    }
    private ConstructionTemplate parse(String text)throws Exception {return ConstructionTemplateConfiguration.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));}
    @Test void originalStarterFilesContainActualStagedBlocksCostsAndFunctionalMetadata()throws Exception {
        var storage=parse(source("starter_storehouse"));assertEquals(83,storage.blockCount());assertEquals(90,storage.work());
        assertEquals(8,storage.materials().get(Commodity.STONE));assertEquals(20,storage.materials().get(Commodity.TIMBER));
        assertEquals(1,storage.materials().get(new Commodity("storage_fittings")));assertEquals(4,storage.stages().size());
        assertEquals(ConstructionTemplate.Role.ROAD,storage.anchors().get(1).role());
        var path=parse(source("starter_path"));assertEquals(5,path.blockCount());assertEquals(ConstructionTemplate.Purpose.PATH,path.purpose());
    }
    @Test void costsAndPacingAreEditableAndWhitespaceDoesNotChangeThePinnedBlueprint()throws Exception {
        String yaml=source("starter_path");var original=parse(yaml);
        assertEquals(original.fingerprint(),parse("# changed formatting\n"+yaml).fingerprint());
        var tuned=parse(yaml.replace("stone: 2","stone: 4").replace("work: 5","work: 7"));
        assertEquals(4,tuned.materials().get(Commodity.STONE));assertEquals(7,tuned.work());assertNotEquals(original.fingerprint(),tuned.fingerprint());
    }
    @Test void unsupportedVersionsTyposInvalidLayersAndUnsafeStatesFailClosed()throws Exception {
        String yaml=source("starter_path");
        for(String changed:new String[]{yaml.replace("format: 1","format: 2"),yaml+"\nfree-materials: true\n",yaml.replace("rows: [CCCCC]","rows: [CCCC]"),
                yaml.replace("rows: [CCCCC]","rows: [CCXCC]"),yaml.replace("minecraft:cobblestone","minecraft:tnt"),yaml.replace("stone: 2","stone: 2.5")})
            assertThrows(IllegalArgumentException.class,()->parse(changed));
        assertThrows(IOException.class,()->ConstructionTemplateConfiguration.read(new ByteArrayInputStream(new byte[ConstructionTemplateConfiguration.MAX_FILE_BYTES+1])));
    }
}
