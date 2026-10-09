package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.OreScan;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperOreSourceTest {
    @Test void snapshotUsesAbsoluteNegativeYAndRecognizesBothOreVariants() {
        var material = new AtomicReference<>(Material.COAL_ORE);
        var snapshot = (ChunkSnapshot)Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),
                new Class<?>[]{ChunkSnapshot.class},(p,m,a)-> {
                    assertEquals("getBlockType",m.getName());
                    assertArrayEquals(new Object[]{4,-60,5},a); return material.get();
                });
        var source = new PaperOreSource(snapshot,-64,320);
        for (var type : java.util.List.of(Material.COAL_ORE,Material.DEEPSLATE_COAL_ORE)) {
            material.set(type); assertEquals(OreScan.Kind.COAL,source.kind(4,-60,5));
        }
        for (var type : java.util.List.of(Material.IRON_ORE,Material.DEEPSLATE_IRON_ORE)) {
            material.set(type); assertEquals(OreScan.Kind.IRON,source.kind(4,-60,5));
        }
        for (var type : java.util.List.of(Material.AIR,Material.CAVE_AIR,Material.VOID_AIR)) {
            material.set(type); assertEquals(OreScan.Kind.AIR,source.kind(4,-60,5));
        }
        for (var type : java.util.List.of(Material.WATER,Material.LAVA,Material.STONE,Material.DIAMOND_ORE)) {
            material.set(type); assertEquals(OreScan.Kind.SOLID,source.kind(4,-60,5));
        }
    }

    @Test void unavailableChunkNeighborsAndWorldHeightsNeverReadSnapshot() {
        var snapshot = (ChunkSnapshot)Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),
                new Class<?>[]{ChunkSnapshot.class},(p,m,a)-> { throw new AssertionError("Unexpected snapshot read"); });
        var source = new PaperOreSource(snapshot,-64,320);
        for (var point : new int[][]{{-1,0,0},{16,0,0},{0,0,-1},{0,0,16},{0,-65,0},{0,320,0}})
            assertEquals(OreScan.Kind.UNKNOWN,source.kind(point[0],point[1],point[2]));
        assertThrows(IllegalArgumentException.class,()->new PaperOreSource(snapshot,1,1));
    }
}
