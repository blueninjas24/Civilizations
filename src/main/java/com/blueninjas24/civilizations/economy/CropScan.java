package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Bounded snapshot reads with empty-section skipping. All growth stages count as planted capacity. */
public final class CropScan {
    public enum Kind { NONE, OTHER, GRAIN }
    public interface Source {
        boolean emptySection(int section);
        boolean plantedCrop(int x, int y, int z);
        default Kind cropKind(int x,int y,int z) { return plantedCrop(x,y,z)?Kind.OTHER:Kind.NONE; }
    }
    private final Source source;
    private final int chunkX, chunkZ, maximumY;
    private int y, column;
    private final List<Position> crops = new ArrayList<>();
    private final List<Position> grainCrops = new ArrayList<>();
    public CropScan(Source source, int chunkX, int chunkZ, int minimumY, int maximumY) {
        if (minimumY >= maximumY) throw new IllegalArgumentException("Invalid crop scan bounds");
        this.source = Objects.requireNonNull(source); this.chunkX = chunkX; this.chunkZ = chunkZ; y = minimumY; this.maximumY = maximumY;
    }
    public int advance(int maximumReads) {
        if (maximumReads < 1) throw new IllegalArgumentException("Invalid crop scan budget");
        int used = 0;
        while (y < maximumY && used < maximumReads) {
            if (column == 0 && source.emptySection(Math.floorDiv(y, 16))) { y = Math.min(maximumY, Math.multiplyExact(Math.floorDiv(y, 16) + 1, 16)); continue; }
            int x = column & 15, z = column >> 4;
            var kind=Objects.requireNonNull(source.cropKind(x,y,z));
            if (kind!=Kind.NONE) {
                var position=new Position(Math.addExact(Math.multiplyExact(chunkX,16),x),y,Math.addExact(Math.multiplyExact(chunkZ,16),z));
                crops.add(position);
                if(kind==Kind.GRAIN)grainCrops.add(position);
            }
            column++; used++;
            if (column == 256) { column = 0; y++; }
        }
        return used;
    }
    public boolean complete() { return y == maximumY; }
    public List<Position> crops() { if (!complete()) throw new IllegalStateException("Incomplete crop scan"); return List.copyOf(crops); }
    public List<Position> grainCrops() { if (!complete()) throw new IllegalStateException("Incomplete crop scan"); return List.copyOf(grainCrops); }
    public static boolean near(Position crop, Position home, FarmObservation.Policy policy) {
        long dx = (long)crop.x() - home.x(), dz = (long)crop.z() - home.z();
        if (Math.abs(dx) > policy.radius() || Math.abs(dz) > policy.radius()) return false;
        return dx * dx + dz * dz <= (long)policy.radius() * policy.radius() && Math.abs((long)crop.y() - home.y()) <= policy.verticalRadius();
    }
}
