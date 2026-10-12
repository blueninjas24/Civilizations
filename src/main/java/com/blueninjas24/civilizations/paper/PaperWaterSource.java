package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.WaterScan;
import java.util.Objects;
import org.bukkit.ChunkSnapshot;

/** Immutable snapshot block types only. Caller captures an already-loaded chunk on Paper's main thread. */
public final class PaperWaterSource implements WaterScan.Source {
    private final ChunkSnapshot snapshot;
    private final int minimumY,maximumY;
    public PaperWaterSource(ChunkSnapshot snapshot,int minimumY,int maximumY) {
        this.snapshot=Objects.requireNonNull(snapshot);
        if(minimumY>=maximumY)throw new IllegalArgumentException("Invalid world heights");
        this.minimumY=minimumY;this.maximumY=maximumY;
    }
    @Override public WaterScan.Cell cell(int x,int y,int z) {
        if(x<0||x>15||z<0||z>15||y<minimumY||y>=maximumY)return WaterScan.Cell.UNKNOWN;
        return switch(snapshot.getBlockType(x,y,z)) {
            case AIR,CAVE_AIR,VOID_AIR -> WaterScan.Cell.AIR;
            case WATER -> WaterScan.Cell.WATER;
            default -> WaterScan.Cell.OTHER;
        };
    }
}
