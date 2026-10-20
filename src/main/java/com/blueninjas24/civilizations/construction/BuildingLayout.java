package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.Direction;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.security.*;
import java.util.*;

/** Declared geometry, not consent, observed habitability, ownership or occupied housing capacity. */
public record BuildingLayout(UUID id, UUID settlement, UUID world, Kind kind, Bounds plot,
                             Bounds structure, Position entrance, List<Bounds> shared,
                             List<Unit> units) {
    public static final int MAX_UNITS = 64, MAX_SPACES = 16, MAX_SHARED = 128, MAX_BEDS = 512;
    public static final int MAX_STRUCTURE_VOLUME = 65_536;
    public enum Kind { HOUSING, WORKPLACE, CIVIC, INFRASTRUCTURE }

    /** Inclusive world coordinates. Plot land and physical structure are deliberately separate. */
    public record Bounds(Position min, Position max) implements Comparable<Bounds> {
        public Bounds {
            Objects.requireNonNull(min); Objects.requireNonNull(max);
            if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z())
                throw new IllegalArgumentException("Reversed building bounds");
            // Bound arithmetic before multiplying volumes or iterating observations.
            if (span(min.x(), max.x()) > 512 || span(min.y(), max.y()) > 512
                    || span(min.z(), max.z()) > 512)
                throw new IllegalArgumentException("Building bounds exceed geometry safety limit");
        }
        private static long span(int min, int max) { return (long) max - min + 1; }
        public long volume() { return span(min.x(), max.x()) * span(min.y(), max.y()) * span(min.z(), max.z()); }
        public boolean contains(Position p) {
            return p.x() >= min.x() && p.x() <= max.x() && p.y() >= min.y() && p.y() <= max.y()
                    && p.z() >= min.z() && p.z() <= max.z();
        }
        public boolean contains(Bounds b) { return contains(b.min) && contains(b.max); }
        public boolean overlaps(Bounds b) {
            return min.x() <= b.max.x() && max.x() >= b.min.x()
                    && min.y() <= b.max.y() && max.y() >= b.min.y()
                    && min.z() <= b.max.z() && max.z() >= b.min.z();
        }
        @Override public int compareTo(Bounds b) { int c = min.compareTo(b.min); return c == 0 ? max.compareTo(b.max) : c; }
    }

    public record Bed(Position foot, Direction facing) implements Comparable<Bed> {
        public Bed { Objects.requireNonNull(foot); Objects.requireNonNull(facing); headAt(foot, facing); }
        public Position head() { return headAt(foot, facing); }
        private static Position headAt(Position foot, Direction direction) {
            return foot.plus(switch (direction) {
                case NORTH -> new Position(0, 0, -1); case EAST -> new Position(1, 0, 0);
                case SOUTH -> new Position(0, 0, 1); case WEST -> new Position(-1, 0, 0);
            });
        }
        @Override public int compareTo(Bed b) { int c = foot.compareTo(b.foot); return c == 0 ? facing.compareTo(b.facing) : c; }
    }

    /** Multiple boxes support irregular rooms; separate units must always be explicitly declared. */
    public record Unit(UUID id, Position entrance, List<Bounds> spaces, List<Bed> beds) {
        public Unit {
            Objects.requireNonNull(id); Objects.requireNonNull(entrance);
            if (spaces.isEmpty() || spaces.size() > MAX_SPACES || beds.isEmpty() || beds.size() > MAX_BEDS)
                throw new IllegalArgumentException("Dwelling needs bounded private space and declared beds");
            spaces = sorted(spaces); beds = beds.stream().sorted().toList();
            disjoint(spaces);
            if (!contains(spaces, entrance)) throw new IllegalArgumentException("Unit entrance outside its private space");
            var cells = new HashSet<Position>();
            for (var bed : beds)
                if (!contains(spaces, bed.foot) || !contains(spaces, bed.head())
                        || !cells.add(bed.foot) || !cells.add(bed.head()))
                    throw new IllegalArgumentException("Beds need distinct complete pairs inside the unit");
        }
        public int declaredBeds() { return beds.size(); }
    }

    public BuildingLayout {
        Objects.requireNonNull(id); Objects.requireNonNull(settlement); Objects.requireNonNull(world);
        Objects.requireNonNull(kind); Objects.requireNonNull(plot); Objects.requireNonNull(structure);
        Objects.requireNonNull(entrance);
        if (!plot.contains(structure) || structure.volume() > MAX_STRUCTURE_VOLUME || !structure.contains(entrance))
            throw new IllegalArgumentException("Structure must be bounded within its plot and contain its entrance");
        if (shared.size() > MAX_SHARED || units.size() > MAX_UNITS
                || (kind == Kind.HOUSING ? units.isEmpty() : !units.isEmpty()))
            throw new IllegalArgumentException("Invalid typed building/unit count");
        shared = sorted(shared); units = units.stream().sorted(Comparator.comparing(Unit::id)).toList();
        var regions = new ArrayList<>(shared); var ids = new HashSet<UUID>(); int beds = 0;
        for (var unit : units) {
            if (!ids.add(unit.id)) throw new IllegalArgumentException("Duplicate dwelling identity");
            regions.addAll(unit.spaces); beds = Math.addExact(beds, unit.declaredBeds());
        }
        if (beds > MAX_BEDS) throw new IllegalArgumentException("Building bed budget exceeded");
        for (var region : regions) if (!structure.contains(region))
            throw new IllegalArgumentException("Private/shared space exceeds the structure");
        disjoint(regions);
        if (kind == Kind.HOUSING && !contains(regions, entrance))
            throw new IllegalArgumentException("Housing entrance must belong to shared or private space");
    }

    /** Declaration only: never use as currently usable settlement capacity without inspection and consent. */
    public int declaredBeds() { return units.stream().mapToInt(Unit::declaredBeds).sum(); }
    public String fingerprint() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(BuildingLayoutSnapshot.encode(this)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static List<Bounds> sorted(List<Bounds> spaces) { return spaces.stream().sorted().toList(); }
    private static boolean contains(List<Bounds> spaces, Position p) { return spaces.stream().anyMatch(b -> b.contains(p)); }
    private static void disjoint(List<Bounds> spaces) {
        for (int i = 0; i < spaces.size(); i++) for (int j = i + 1; j < spaces.size(); j++)
            if (spaces.get(i).overlaps(spaces.get(j))) throw new IllegalArgumentException("Private/shared spaces overlap");
    }
}
