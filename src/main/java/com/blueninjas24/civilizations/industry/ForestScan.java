package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Incremental bounded tree evidence from caller-supplied loaded snapshots; never changes blocks. */
public final class ForestScan {
    public enum Kind { AIR, LOG, LEAF, OTHER, UNKNOWN }
    @FunctionalInterface public interface Source { Kind kind(int x, int y, int z); }

    /** Bounds are absolute coordinates with exclusive upper limits. */
    public record Bounds(int minimumX, int minimumY, int minimumZ,
                         int maximumXExclusive, int maximumYExclusive, int maximumZExclusive) {
        public Bounds {
            long width = (long) maximumXExclusive - minimumX;
            long height = (long) maximumYExclusive - minimumY;
            long depth = (long) maximumZExclusive - minimumZ;
            if (width < 1 || height < 1 || depth < 1
                    || width > 1_000_000L || height > 1_000_000L || depth > 1_000_000L
                    || width * height > 1_000_000L / depth) {
                throw new IllegalArgumentException("Forest scan bounds must contain 1..1000000 cells");
            }
        }
    }

    /** Editable geometry filters; output is canopy-backed trunk coordinates, not timber units. */
    public record Geometry(int minimumVerticalLogs, int horizontalCanopyRadius,
                           int verticalCanopyRadius, int maximumTrunkCoordinates) {
        public Geometry {
            if (minimumVerticalLogs < 2 || minimumVerticalLogs > 32
                    || horizontalCanopyRadius < 1 || horizontalCanopyRadius > 8
                    || verticalCanopyRadius < 1 || verticalCanopyRadius > 8
                    || maximumTrunkCoordinates < 1 || maximumTrunkCoordinates > ForestObservation.MAX_TRUNK_COORDINATES) {
                throw new IllegalArgumentException("Invalid bounded forest geometry");
            }
        }
        public static Geometry prototype() { return new Geometry(3, 3, 2, 512); }
    }

    private final Source source;
    private final Bounds bounds;
    private final Set<Position> excluded;
    private final Geometry geometry;
    private final int width, height, depth;
    private final Kind[] cells;
    private final NavigableSet<Position> trunks = new TreeSet<>(Position.ORDER);
    private int captured;
    private int evaluated;
    private boolean reliable = true;

    public ForestScan(Source source, Bounds bounds, Set<Position> knownPlayerPlacements, Geometry geometry) {
        this.source = Objects.requireNonNull(source);
        this.bounds = Objects.requireNonNull(bounds);
        this.excluded = Set.copyOf(Objects.requireNonNull(knownPlayerPlacements));
        this.geometry = Objects.requireNonNull(geometry);
        width = Math.toIntExact((long) bounds.maximumXExclusive() - bounds.minimumX());
        height = Math.toIntExact((long) bounds.maximumYExclusive() - bounds.minimumY());
        depth = Math.toIntExact((long) bounds.maximumZExclusive() - bounds.minimumZ());
        cells = new Kind[Math.toIntExact((long) width * height * depth)];
    }

    /**
     * Captures at most {@code maximumCells} source cells, then uses any remaining budget to
     * evaluate candidate cells from that immutable buffer. A candidate canopy check reads at
     * most {@code (2 * horizontalCanopyRadius + 1)^2 * (verticalCanopyRadius + 2)} buffered
     * cells, plus at most {@code maximumTrunkCoordinates + 1} buffered cells along its trunk.
     */
    public int advance(int maximumCells) {
        if (maximumCells < 1) throw new IllegalArgumentException("Invalid forest scan budget");
        int used = 0;
        while (captured < cells.length && used < maximumCells) {
            int index = captured++;
            int x = bounds.minimumX() + index % width;
            int z = bounds.minimumZ() + index / width % depth;
            int y = bounds.minimumY() + index / (width * depth);
            Kind kind = Objects.requireNonNull(source.kind(x, y, z));
            cells[index] = kind;
            if (kind == Kind.UNKNOWN) reliable = false;
            used++;
        }
        while (captured == cells.length && evaluated < cells.length && used < maximumCells) {
            evaluate(evaluated++);
            used++;
        }
        return used;
    }

    /** Complete means both snapshot capture and bounded candidate evaluation have finished. */
    public boolean complete() { return captured == cells.length && evaluated == cells.length; }

    /** Copies already-evaluated evidence only; it does not scan cells or perform geometry work. */
    public ForestObservation observation(UUID world, long observedMillis) {
        if (!complete()) throw new IllegalStateException("Forest scan is unfinished");
        return new ForestObservation(world, observedMillis, reliable, List.copyOf(trunks));
    }

    private void evaluate(int index) {
        int x = bounds.minimumX() + index % width;
        int z = bounds.minimumZ() + index / width % depth;
        int y = bounds.minimumY() + index / (width * depth);
        if (!safeRootWindow(x, y, z)) return;

        var root = new Position(x, y, z);
        if (kind(root) != Kind.LOG || excluded.contains(root)) return;
        if (kind(new Position(x, y - 1, z)) == Kind.LOG) return;

        var column = new ArrayList<Position>();
        int top = y;
        while (top < bounds.maximumYExclusive() && column.size() <= geometry.maximumTrunkCoordinates()) {
            var position = new Position(x, top, z);
            if (kind(position) != Kind.LOG) break;
            column.add(position);
            top++;
        }
        if (column.size() > geometry.maximumTrunkCoordinates()) {
            reliable = false;
            return;
        }
        if (column.stream().anyMatch(excluded::contains)
                || column.size() < geometry.minimumVerticalLogs()) return;
        int topY = column.getLast().y();
        if (!safeCanopyWindow(x, topY, z) || !hasNaturalCanopy(x, topY, z)) return;

        for (var position : column) {
            if (trunks.size() == geometry.maximumTrunkCoordinates() && !trunks.contains(position)) {
                reliable = false;
                return;
            }
            trunks.add(position);
        }
    }

    /** The outer geometry margin is deliberately ignored and never makes interior evidence unreliable. */
    private boolean safeRootWindow(int x, int y, int z) {
        int radius = geometry.horizontalCanopyRadius();
        return (long) x - radius >= bounds.minimumX() && (long) x + radius < bounds.maximumXExclusive()
                && (long) z - radius >= bounds.minimumZ() && (long) z + radius < bounds.maximumZExclusive()
                && (long) y - 1 >= bounds.minimumY();
    }

    private boolean safeCanopyWindow(int x, int topY, int z) {
        int radius = geometry.horizontalCanopyRadius();
        return (long) x - radius >= bounds.minimumX() && (long) x + radius < bounds.maximumXExclusive()
                && (long) z - radius >= bounds.minimumZ() && (long) z + radius < bounds.maximumZExclusive()
                && (long) topY - 1 >= bounds.minimumY()
                && (long) topY + geometry.verticalCanopyRadius() < bounds.maximumYExclusive();
    }

    private boolean hasNaturalCanopy(int trunkX, int topY, int trunkZ) {
        int radius = geometry.horizontalCanopyRadius();
        for (int y = topY - 1; y <= topY + geometry.verticalCanopyRadius(); y++) {
            for (int z = trunkZ - radius; z <= trunkZ + radius; z++) {
                for (int x = trunkX - radius; x <= trunkX + radius; x++) {
                    var position = new Position(x, y, z);
                    if (kind(position) == Kind.LEAF && !excluded.contains(position)) return true;
                }
            }
        }
        return false;
    }

    private Kind kind(Position p) {
        if (p.x() < bounds.minimumX() || p.x() >= bounds.maximumXExclusive()
                || p.y() < bounds.minimumY() || p.y() >= bounds.maximumYExclusive()
                || p.z() < bounds.minimumZ() || p.z() >= bounds.maximumZExclusive()) return null;
        int x = p.x() - bounds.minimumX(), y = p.y() - bounds.minimumY(), z = p.z() - bounds.minimumZ();
        return cells[(y * depth + z) * width + x];
    }
}
