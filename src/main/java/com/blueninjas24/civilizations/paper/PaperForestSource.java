package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.ForestScan;
import com.blueninjas24.civilizations.industry.OreSurveyArea;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Axis;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshot-only environmental tree evidence. It does not certify ownership or authorize adoption.
 * Capture the supplied immutable chunk snapshots on the Paper main thread before constructing it.
 */
public final class PaperForestSource implements ForestScan.Source {
    private static final int WORLD_BORDER = 29_999_984;

    private final Map<OreSurveyArea.Chunk, ChunkSnapshot> snapshots;
    private final int minimumY;
    private final int maximumY;

    public PaperForestSource(Map<OreSurveyArea.Chunk, ChunkSnapshot> snapshots, int minimumY, int maximumY) {
        if (minimumY >= maximumY) throw new IllegalArgumentException("Invalid forest snapshot height range");
        this.snapshots = Map.copyOf(Objects.requireNonNull(snapshots));
        this.minimumY = minimumY;
        this.maximumY = maximumY;
    }

    @Override public ForestScan.Kind kind(int x, int y, int z) {
        if (x < -WORLD_BORDER || x > WORLD_BORDER || z < -WORLD_BORDER || z > WORLD_BORDER
                || y < minimumY || y >= maximumY) return ForestScan.Kind.UNKNOWN;

        var key = new OreSurveyArea.Chunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        var snapshot = snapshots.get(key);
        if (snapshot == null) return ForestScan.Kind.UNKNOWN;

        Material material = snapshot.getBlockType(Math.floorMod(x, 16), y, Math.floorMod(z, 16));
        if (material == null) return ForestScan.Kind.UNKNOWN;
        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR)
            return ForestScan.Kind.AIR;
        if (!Tag.LOGS.isTagged(material) && !Tag.LEAVES.isTagged(material)) return ForestScan.Kind.OTHER;
        var data = snapshot.getBlockData(Math.floorMod(x, 16), y, Math.floorMod(z, 16));
        if (Tag.LOGS.isTagged(material))
            return data instanceof Orientable orientable && orientable.getAxis() == Axis.Y
                    ? ForestScan.Kind.LOG : ForestScan.Kind.OTHER;
        if (Tag.LEAVES.isTagged(material))
            return data instanceof Leaves leaves && !leaves.isPersistent()
                    ? ForestScan.Kind.LEAF : ForestScan.Kind.OTHER;
        return ForestScan.Kind.OTHER;
    }
}
