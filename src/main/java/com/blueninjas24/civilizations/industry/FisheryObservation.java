package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Trusted complete open-water evidence; Paper suitability scanning is a separate adapter. */
public record FisheryObservation(UUID world, long observedMillis, boolean complete, List<Column> columns) {
    public record Column(int x, int z) {}
    public FisheryObservation {
        Objects.requireNonNull(world);
        columns = List.copyOf(columns);
        if (observedMillis < 0 || columns.isEmpty() || columns.size() > 4096
                || new HashSet<>(columns).size() != columns.size())
            throw new IllegalArgumentException("Invalid fishery water evidence");
        columns = columns.stream().sorted(Comparator.comparingInt(Column::x).thenComparingInt(Column::z)).toList();
    }
}
