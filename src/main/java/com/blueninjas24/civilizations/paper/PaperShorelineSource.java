package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.ShorelineScan;
import org.bukkit.*;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Main-thread loaded-only block checks; conservative full opaque footing and two air blocks. */
public final class PaperShorelineSource implements ShorelineScan.Source {
    private final World world;
    private final BooleanSupplier primaryThread;
    public PaperShorelineSource(World world){this(world,Bukkit::isPrimaryThread);}
    PaperShorelineSource(World world,BooleanSupplier primaryThread){this.world=Objects.requireNonNull(world);this.primaryThread=Objects.requireNonNull(primaryThread);}
    @Override public ShorelineScan.Cell cell(ShorelineScan.Position position) {
        if(!primaryThread.getAsBoolean())throw new IllegalStateException("Shoreline checks require main thread");
        if(position.y()<world.getMinHeight()||position.y()>=world.getMaxHeight())return ShorelineScan.Cell.OTHER;
        if(!world.isChunkLoaded(Math.floorDiv(position.x(),16),Math.floorDiv(position.z(),16)))return ShorelineScan.Cell.UNKNOWN;
        var material=world.getBlockAt(position.x(),position.y(),position.z()).getType();
        if(material==Material.AIR||material==Material.CAVE_AIR||material==Material.VOID_AIR)return ShorelineScan.Cell.AIR;
        if(material.isSolid()&&material.isOccluding()&&material!=Material.MAGMA_BLOCK)return ShorelineScan.Cell.SAFE_FOOTING;
        return ShorelineScan.Cell.OTHER;
    }
}
