package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.economy.CropScan;
import org.bukkit.*;

final class PaperCropSource implements CropScan.Source {
    private final ChunkSnapshot snapshot;
    private final int minimumY;
    PaperCropSource(ChunkSnapshot snapshot, int minimumY) { this.snapshot = snapshot; this.minimumY = minimumY; }
    @Override public boolean emptySection(int section) { return snapshot.isSectionEmpty(section - Math.floorDiv(minimumY, 16)); }
    @Override public boolean plantedCrop(int x, int y, int z) {
        if (y <= minimumY) return false;
        var crop = snapshot.getBlockType(x, y, z);
        return (crop == Material.WHEAT || crop == Material.CARROTS || crop == Material.POTATOES || crop == Material.BEETROOTS)
                && snapshot.getBlockType(x, y - 1, z) == Material.FARMLAND;
    }
}
