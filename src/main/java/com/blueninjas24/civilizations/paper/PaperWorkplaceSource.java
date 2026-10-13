package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.WorkplaceScan;
import org.bukkit.ChunkSnapshot;
import java.util.Objects;

/** Block-type reads only; does not access a live block, tile state or inventory. */
public final class PaperWorkplaceSource implements WorkplaceScan.Source {
    private final ChunkSnapshot snapshot;
    private final int minimumY,maximumY;
    public PaperWorkplaceSource(ChunkSnapshot snapshot,int minimumY,int maximumY) {
        this.snapshot=Objects.requireNonNull(snapshot);if(minimumY>=maximumY)throw new IllegalArgumentException("Invalid world heights");this.minimumY=minimumY;this.maximumY=maximumY;
    }
    public WorkplaceScan.Cell cell(int x,int y,int z) {
        if(x<0||x>15||z<0||z>15||y<minimumY||y>=maximumY)return WorkplaceScan.Cell.UNKNOWN;
        return switch(snapshot.getBlockType(x,y,z)){case BARREL->WorkplaceScan.Cell.BARREL;case SMITHING_TABLE->WorkplaceScan.Cell.SMITHING_TABLE;case SMOKER->WorkplaceScan.Cell.SMOKER;case CRAFTING_TABLE->WorkplaceScan.Cell.CRAFTING_TABLE;default->WorkplaceScan.Cell.OTHER;};
    }
    public static org.bukkit.Material material(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind kind){
        return switch(kind){case BARREL->org.bukkit.Material.BARREL;case SMITHING_TABLE->org.bukkit.Material.SMITHING_TABLE;case SMOKER->org.bukkit.Material.SMOKER;case CRAFTING_TABLE->org.bukkit.Material.CRAFTING_TABLE;};
    }
}
