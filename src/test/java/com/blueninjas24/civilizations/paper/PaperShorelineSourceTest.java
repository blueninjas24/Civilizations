package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.ShorelineScan;
import org.bukkit.*;
import org.bukkit.block.Block;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperShorelineSourceTest {
    AtomicReference<Material> material=new AtomicReference<>(Material.STONE);
    AtomicBoolean loaded=new AtomicBoolean(true);
    AtomicInteger reads=new AtomicInteger();
    World world() {
        var block=(Block)Proxy.newProxyInstance(Block.class.getClassLoader(),new Class<?>[]{Block.class},(p,m,a)->{if(m.getName().equals("getType")){reads.incrementAndGet();return material.get();}throw new AssertionError("Unexpected block call: "+m.getName());});
        return (World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->switch(m.getName()) {
            case "getMinHeight"->-64;case "getMaxHeight"->320;
            case "isChunkLoaded"->{assertEquals(-1,a[0]);assertEquals(-1,a[1]);yield loaded.get();}
            case "getBlockAt"->block;
            default->throw new AssertionError("Unexpected world call: "+m.getName());
        });
    }
    @Test void mainThreadHeightAndLoadedGuardsNeverForceChunkOrReadMissingBlocks() {
        var world=world();var source=new PaperShorelineSource(world,()->true);var p=new ShorelineScan.Position(-1,64,-1);
        loaded.set(false);assertEquals(ShorelineScan.Cell.UNKNOWN,source.cell(p));assertEquals(0,reads.get());
        assertEquals(ShorelineScan.Cell.OTHER,source.cell(new ShorelineScan.Position(-1,320,-1)));assertEquals(0,reads.get());
        assertThrows(IllegalStateException.class,()->new PaperShorelineSource(world,()->false).cell(p));assertEquals(0,reads.get());
    }
}
