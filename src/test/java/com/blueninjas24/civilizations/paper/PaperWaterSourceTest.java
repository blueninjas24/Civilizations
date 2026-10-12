package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.WaterScan;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperWaterSourceTest {
    @Test void absoluteNegativeYAndTypeReadsDistinguishActualWaterFromWaterloggedBlocks() {
        var type=new AtomicReference<>(Material.WATER);
        var snapshot=(ChunkSnapshot)Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),new Class<?>[]{ChunkSnapshot.class},(p,m,a)->{
            assertEquals("getBlockType",m.getName());assertArrayEquals(new Object[]{4,-60,5},a);return type.get();});
        var source=new PaperWaterSource(snapshot,-64,320);
        assertEquals(WaterScan.Cell.WATER,source.cell(4,-60,5));
        for(var material:java.util.List.of(Material.AIR,Material.CAVE_AIR,Material.VOID_AIR)){type.set(material);assertEquals(WaterScan.Cell.AIR,source.cell(4,-60,5));}
        for(var material:java.util.List.of(Material.LAVA,Material.ICE,Material.OAK_SLAB,Material.OAK_LEAVES,Material.LILY_PAD)){
            type.set(material);assertEquals(WaterScan.Cell.OTHER,source.cell(4,-60,5));}
    }
    @Test void outOfBoundsAndUnavailableNeighborsNeverReadSnapshot() {
        var snapshot=(ChunkSnapshot)Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),new Class<?>[]{ChunkSnapshot.class},(p,m,a)->{throw new AssertionError("Unexpected read");});
        var source=new PaperWaterSource(snapshot,-64,320);
        for(var point:new int[][]{{-1,0,0},{16,0,0},{0,0,-1},{0,0,16},{0,-65,0},{0,320,0}})
            assertEquals(WaterScan.Cell.UNKNOWN,source.cell(point[0],point[1],point[2]));
        assertThrows(IllegalArgumentException.class,()->new PaperWaterSource(snapshot,1,1));
    }
}
