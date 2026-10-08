package com.blueninjas24.civilizations.paper;

import org.bukkit.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class PaperCropSourceTest {
    @Test void plantedCropsRequireFarmlandAndNegativeSectionsUseRelativeSnapshotIndex() {
        var crop=new AtomicReference<>(Material.WHEAT); var floor=new AtomicReference<>(Material.FARMLAND);
        var snapshot=(ChunkSnapshot)Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),new Class<?>[]{ChunkSnapshot.class},(p,m,a)->switch(m.getName()) {
            case "isSectionEmpty" -> { assertEquals(0,a[0]); yield true; }
            case "getBlockType" -> (int)a[1]==64 ? crop.get() : floor.get();
            default -> throw new AssertionError(m.getName());
        });
        var source=new PaperCropSource(snapshot,-64); assertTrue(source.emptySection(-4));
        for(var type:java.util.List.of(Material.WHEAT,Material.CARROTS,Material.POTATOES,Material.BEETROOTS)) { crop.set(type);assertTrue(source.plantedCrop(0,64,0)); }
        floor.set(Material.DIRT);assertFalse(source.plantedCrop(0,64,0)); floor.set(Material.FARMLAND);crop.set(Material.GRASS_BLOCK);assertFalse(source.plantedCrop(0,64,0));
        assertFalse(source.plantedCrop(0,-64,0));
    }
}
