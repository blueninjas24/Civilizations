package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.settlement.BedScan;
import org.bukkit.ChunkSnapshot;
import org.bukkit.block.data.type.Bed;

/** Pinned Paper snapshots use array-relative empty-section indexes, not world section Y. */
final class PaperBedSource implements BedScan.Source {
    private final ChunkSnapshot snapshot;
    private final int minimumSection;
    PaperBedSource(ChunkSnapshot snapshot, int minimumY) {
        this.snapshot = snapshot; this.minimumSection = Math.floorDiv(minimumY, 16);
    }
    @Override public boolean emptySection(int sectionY) {
        return snapshot.isSectionEmpty(sectionY - minimumSection);
    }
    @Override public boolean headBed(int x, int y, int z) {
        return snapshot.getBlockType(x, y, z).name().endsWith("_BED")
                && snapshot.getBlockData(x, y, z) instanceof Bed bed && bed.getPart() == Bed.Part.HEAD;
    }
}
