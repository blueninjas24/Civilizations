package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BakeryCraftsmanMarkerTest {
    YamlConfiguration config(){return YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(getClass().getResourceAsStream("/economy.yml"),java.nio.charset.StandardCharsets.UTF_8));}
    @Test void adminsCanSelectNewKindsWhileLegacyListsAndMissingSectionsRemainUnchanged() {
        var config=config();EconomyConfiguration.read(config);assertEquals(Set.of(WorkplaceObservation.Kind.values()),EconomyConfiguration.readWorkplaceObserver(config).kinds());
        config.set("workplace-observer.markers",List.of("barrel","smithing_table"));String before=config.saveToString();
        assertEquals(Set.of(WorkplaceObservation.Kind.BARREL,WorkplaceObservation.Kind.SMITHING_TABLE),EconomyConfiguration.readWorkplaceObserver(config).kinds());assertEquals(before,config.saveToString());
        config.set("workplace-observer.markers",List.of("smoker","crafting_table"));assertEquals(Set.of(WorkplaceObservation.Kind.SMOKER,WorkplaceObservation.Kind.CRAFTING_TABLE),EconomyConfiguration.readWorkplaceObserver(config).kinds());
        for(var invalid:List.of(List.of("smoker","smoker"),List.of("furnace"),List.of(1),List.of("barrel","smithing_table","smoker","crafting_table","furnace"))) {
            config.set("workplace-observer.markers",invalid);assertThrows(IllegalArgumentException.class,()->EconomyConfiguration.readWorkplaceObserver(config));
        }
        config.set("workplace-observer",null);assertFalse(EconomyConfiguration.readWorkplaceObserver(config).enabled());
    }
    @Test void snapshotSourceRecognizesOnlyBlockTypesWithoutLiveStateOrInventoryAccess() {
        var snapshot=(ChunkSnapshot)Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),new Class<?>[]{ChunkSnapshot.class},(p,m,a)->{
            if(m.getName().equals("getBlockType"))return (int)a[0]==1?Material.SMOKER:(int)a[0]==2?Material.CRAFTING_TABLE:Material.AIR;
            throw new AssertionError("Unexpected snapshot access "+m.getName());
        });
        var source=new PaperWorkplaceSource(snapshot,0,128);
        var scan=new WorkplaceScan(source,0,0,64,65,Set.of(WorkplaceObservation.Kind.SMOKER,WorkplaceObservation.Kind.CRAFTING_TABLE));
        while(!scan.complete())assertTrue(scan.advance(17)<=17);
        var evidence=scan.observation(new UUID(0,1),1000);assertTrue(evidence.complete());assertEquals(32,evidence.markers().size());
        assertEquals(Set.of("bakery","craftsman"),new HashSet<>(evidence.markers().stream().map(WorkplaceObservation.Marker::industry).toList()));
        assertEquals(Material.SMOKER,PaperWorkplaceSource.material(WorkplaceObservation.Kind.SMOKER));assertEquals(Material.CRAFTING_TABLE,PaperWorkplaceSource.material(WorkplaceObservation.Kind.CRAFTING_TABLE));
    }
    @Test void disabledNewKindsDoNotBecomeCandidatesOrBlacksmithMarkers() {
        var scan=new WorkplaceScan((x,y,z)->x==0?WorkplaceScan.Cell.SMOKER:x==1?WorkplaceScan.Cell.CRAFTING_TABLE:WorkplaceScan.Cell.OTHER,0,0,64,65,Set.of(WorkplaceObservation.Kind.SMITHING_TABLE));
        scan.advance(256);assertTrue(scan.observation(new UUID(0,1),1000).markers().isEmpty());
        assertEquals("blacksmith",WorkplaceObservation.Kind.SMITHING_TABLE.industry());assertEquals("warehouse",WorkplaceObservation.Kind.BARREL.industry());
    }
}
