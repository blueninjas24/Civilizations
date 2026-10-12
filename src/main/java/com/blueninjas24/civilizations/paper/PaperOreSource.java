package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.OreScan;
import java.util.Objects;
import org.bukkit.ChunkSnapshot;

/** Read-only snapshot adapter. Caller captures already-loaded chunks on Paper's main thread. */
public final class PaperOreSource implements OreScan.Source {
    private final ChunkSnapshot snapshot;
    private final int minimumY, maximumY;
    private final boolean quarries;

    public PaperOreSource(ChunkSnapshot snapshot, int minimumY, int maximumY) {
        this(snapshot,minimumY,maximumY,false);
    }
    public static PaperOreSource quarries(ChunkSnapshot snapshot,int minimumY,int maximumY) {
        return new PaperOreSource(snapshot,minimumY,maximumY,true);
    }
    private PaperOreSource(ChunkSnapshot snapshot,int minimumY,int maximumY,boolean quarries) {
        if (minimumY >= maximumY) throw new IllegalArgumentException("Invalid world height");
        this.snapshot = Objects.requireNonNull(snapshot);
        this.minimumY = minimumY; this.maximumY = maximumY;
        this.quarries=quarries;
    }

    @Override public OreScan.Kind kind(int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < minimumY || y >= maximumY)
            return OreScan.Kind.UNKNOWN;
        var type = snapshot.getBlockType(x, y, z);
        return switch (type) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> OreScan.Kind.COAL;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> OreScan.Kind.IRON;
            case AIR, CAVE_AIR, VOID_AIR -> OreScan.Kind.AIR;
            case STONE, DEEPSLATE -> quarries?OreScan.Kind.STONE:OreScan.Kind.SOLID;
            case SAND, RED_SAND -> quarries?OreScan.Kind.SAND:OreScan.Kind.SOLID;
            default -> OreScan.Kind.SOLID;
        };
    }
}
