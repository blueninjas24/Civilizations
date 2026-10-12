package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Explicit status for every original support; omission is never evidence that water was removed. */
public record FisheryHabitatObservation(UUID world, long observedMillis, boolean complete, List<Support> supports) {
    public enum State { SUITABLE, ABSENT, UNKNOWN }
    public record Support(FisheryObservation.Column column, State state) {
        public Support { Objects.requireNonNull(column); Objects.requireNonNull(state); }
    }
    public FisheryHabitatObservation {
        Objects.requireNonNull(world);
        supports = List.copyOf(supports);
        if (observedMillis < 0 || supports.isEmpty() || supports.size() > 4096
                || supports.stream().map(Support::column).distinct().count() != supports.size())
            throw new IllegalArgumentException("Invalid fish habitat observation");
        supports = supports.stream().sorted(Comparator.comparingInt((Support s) -> s.column().x())
                .thenComparingInt(s -> s.column().z())).toList();
    }
    public boolean authoritative() {
        return complete && supports.stream().noneMatch(s -> s.state() == State.UNKNOWN);
    }
    public int suitableColumns() {
        return Math.toIntExact(supports.stream().filter(s -> s.state() == State.SUITABLE).count());
    }
}
