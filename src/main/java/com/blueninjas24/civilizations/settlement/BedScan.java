package com.blueninjas24.civilizations.settlement;

import java.util.ArrayList;
import java.util.List;

/** Incremental read-only bed scan. Empty sections skip cheaply; no world or chunk loading. */
public final class BedScan {
    public interface Source {
        boolean emptySection(int sectionY);
        boolean headBed(int x, int y, int z);
    }
    private final Source source;
    private final int chunkX, chunkZ, maximumY, maximumBeds;
    private int y, column;
    private final List<VillageObservation.Position> beds = new ArrayList<>();

    public BedScan(Source source, int chunkX, int chunkZ, int minimumY, int maximumY, int maximumBeds) {
        if (minimumY >= maximumY || minimumY % 16 != 0 || maximumY % 16 != 0 || maximumBeds < 1)
            throw new IllegalArgumentException("Invalid scan bounds");
        this.source = java.util.Objects.requireNonNull(source);
        this.chunkX = chunkX; this.chunkZ = chunkZ; this.y = minimumY;
        this.maximumY = maximumY; this.maximumBeds = maximumBeds;
    }
    /** Return block reads performed; caller can additionally enforce a wall-time budget. */
    public int advance(int maximumReads) {
        if (maximumReads < 1) throw new IllegalArgumentException("Read budget must be positive");
        int reads = 0;
        while (y < maximumY && reads < maximumReads) {
            if (column == 0 && y % 16 == 0 && source.emptySection(Math.floorDiv(y, 16))) {
                y += 16; continue;
            }
            int x = column & 15, z = column >> 4;
            if (source.headBed(x, y, z)) {
                if (beds.size() == maximumBeds) throw new IllegalArgumentException("Chunk bed count exceeds observation budget");
                beds.add(new VillageObservation.Position(Math.addExact(Math.multiplyExact(chunkX, 16), x), y,
                        Math.addExact(Math.multiplyExact(chunkZ, 16), z)));
            }
            reads++; column++;
            if (column == 256) { column = 0; y++; }
        }
        return reads;
    }
    public boolean complete() { return y == maximumY; }
    public List<VillageObservation.Position> beds() {
        if (!complete()) throw new IllegalStateException("Chunk scan incomplete");
        return List.copyOf(beds);
    }
}
