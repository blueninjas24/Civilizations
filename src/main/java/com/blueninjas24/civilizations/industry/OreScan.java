package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Incremental exposed-ore evidence from immutable snapshots; no deposit grouping or block rights. */
public final class OreScan {
    public enum Kind { AIR, COAL, IRON, STONE, SAND, SOLID, UNKNOWN }
    /** Coordinates are chunk-local X/Z and absolute Y; unavailable neighbors must return UNKNOWN. */
    public interface Source { Kind kind(int x, int y, int z); }
    private static final int[][] FACES = {{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
    private final Source source;
    private final int originX, originZ, maximumY;
    private final Set<Position> excluded;
    private final List<Position> coal = new ArrayList<>(), iron = new ArrayList<>();
    private int y, column;
    private boolean reliable = true;
    private final boolean quarry;

    public OreScan(Source source, int chunkX, int chunkZ, int minimumY, int maximumY, Set<Position> excluded) {
        this(source,chunkX,chunkZ,minimumY,maximumY,excluded,false);
    }
    public static OreScan quarries(Source source,int chunkX,int chunkZ,int minimumY,int maximumY,Set<Position> excluded) {
        return new OreScan(source,chunkX,chunkZ,minimumY,maximumY,excluded,true);
    }
    private OreScan(Source source,int chunkX,int chunkZ,int minimumY,int maximumY,Set<Position> excluded,boolean quarry) {
        if (minimumY >= maximumY || minimumY == Integer.MIN_VALUE || maximumY == Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid ore scan bounds");
        this.source = Objects.requireNonNull(source);
        this.quarry=quarry;
        this.excluded = Set.copyOf(excluded);
        originX = Math.multiplyExact(chunkX, 16); originZ = Math.multiplyExact(chunkZ, 16);
        Math.addExact(originX, 15); Math.addExact(originZ, 15);
        y = minimumY; this.maximumY = maximumY;
    }

    /** Visits at most maximumCells cells, each requiring at most seven source reads. */
    public int advance(int maximumCells) {
        if (maximumCells < 1) throw new IllegalArgumentException("Invalid ore scan budget");
        int used = 0;
        while (!complete() && used < maximumCells) {
            int x = column & 15, z = column >> 4;
            inspect(x, y, z);
            used++; column++;
            if (column == 256) { column = 0; y++; }
        }
        return used;
    }

    private void inspect(int x, int y, int z) {
        Kind kind = Objects.requireNonNull(source.kind(x, y, z));
        if (kind == Kind.UNKNOWN) { reliable = false; return; }
        Kind first=quarry?Kind.STONE:Kind.COAL,second=quarry?Kind.SAND:Kind.IRON;
        if (kind != first && kind != second) return;
        var position = new Position(originX + x, y, originZ + z);
        if (excluded.contains(position)) return;
        boolean exposed = false, unknown = false;
        for (var face : FACES) {
            Kind neighbor = Objects.requireNonNull(source.kind(x + face[0], y + face[1], z + face[2]));
            exposed |= neighbor == Kind.AIR; unknown |= neighbor == Kind.UNKNOWN;
        }
        if (!exposed) { if (unknown) reliable = false; return; }
        if (coal.size() + iron.size() == 512) { reliable = false; return; }
        (kind == first ? coal : iron).add(position);
    }

    /** Finished traversal does not imply reliable resource evidence. */
    public boolean complete() { return y == maximumY; }

    /** One observation per mineral, with a shared completeness flag; this does not define deposits. */
    public List<MineralObservation> observations(UUID world, long observedMillis) {
        if (!complete()) throw new IllegalStateException("Ore scan is unfinished");
        return List.of(new MineralObservation(world, observedMillis, reliable, quarry?Commodity.STONE:Commodity.COAL, coal),
                new MineralObservation(world, observedMillis, reliable, quarry?Commodity.SAND:Commodity.IRON, iron));
    }
}
