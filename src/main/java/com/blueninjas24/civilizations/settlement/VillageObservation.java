package com.blueninjas24.civilizations.settlement;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable loaded-world observations; no Bukkit references cross the observation boundary. */
public record VillageObservation(UUID worldId, String worldName, long observedMillis,
                                 boolean complete, List<Position> beds, List<Person> villagers) {
    /** A clear border wider than the matching radii prevents unseen connected beds/residents. */
    public record Bounds(int minimumX, int maximumX, int minimumZ, int maximumZ) {
        public Bounds {
            if (minimumX > maximumX || minimumZ > maximumZ) throw new IllegalArgumentException("Invalid observation bounds");
        }
        public boolean encloses(List<Position> beds, int margin) {
            if (margin < 0) throw new IllegalArgumentException("Negative observation margin");
            return beds.stream().allMatch(b -> (long) b.x() - minimumX > margin && (long) maximumX - b.x() > margin
                    && (long) b.z() - minimumZ > margin && (long) maximumZ - b.z() > margin);
        }
    }
    public record Position(int x, int y, int z) {
        public static final Comparator<Position> ORDER = Comparator.comparingInt(Position::x)
                .thenComparingInt(Position::y).thenComparingInt(Position::z);
        public double squaredDistance(Position other) {
            double dx = (double) x - other.x, dy = (double) y - other.y, dz = (double) z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
    public record Person(UUID entityId, Position position, boolean adult, String existingName) {
        public Person {
            Objects.requireNonNull(entityId); Objects.requireNonNull(position);
            if (existingName != null && existingName.isBlank()) existingName = null;
        }
    }
    public VillageObservation {
        Objects.requireNonNull(worldId);
        if (worldName == null || worldName.isBlank() || observedMillis < 0)
            throw new IllegalArgumentException("Invalid observation identity/time");
        beds = beds.stream().sorted(Position.ORDER).toList();
        villagers = villagers.stream().sorted(Comparator.comparing(p -> p.entityId().toString())).toList();
        if (new HashSet<>(beds).size() != beds.size()) throw new IllegalArgumentException("Duplicate bed observation");
        var identities = new HashSet<UUID>();
        for (Person person : villagers) {
            if (!identities.add(person.entityId())) throw new IllegalArgumentException("Duplicate villager observation");
        }
    }
}
