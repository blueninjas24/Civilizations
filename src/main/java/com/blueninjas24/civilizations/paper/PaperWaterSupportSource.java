package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.WaterSupportScan;
import org.bukkit.*;
import java.util.Objects;

/** Main-thread live block proof only; never requests a chunk load or changes a block. */
public final class PaperWaterSupportSource implements WaterSupportScan.Source {
    private final World world;
    public PaperWaterSupportSource(World world){this.world=Objects.requireNonNull(world);}
    @Override public WaterSupportScan.Cell cell(WaterSupportScan.Position p) {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Live water support needs main thread");
        if(p.y()<world.getMinHeight()||p.y()>=world.getMaxHeight())return WaterSupportScan.Cell.OTHER;
        if(!world.isChunkLoaded(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16)))return WaterSupportScan.Cell.UNKNOWN;
        var material=world.getBlockAt(p.x(),p.y(),p.z()).getType();
        return material==Material.WATER?WaterSupportScan.Cell.WATER:material.isAir()?WaterSupportScan.Cell.AIR:WaterSupportScan.Cell.OTHER;
    }
}
