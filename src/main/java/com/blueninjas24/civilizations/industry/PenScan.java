package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Bounded, incremental horizontal enclosure evidence. Never grants ownership by itself. */
public final class PenScan {
    public record Column(int x, int z) {}
    public enum Cell { INTERIOR, BARRIER, OPEN, UNKNOWN }
    public enum State { ENCLOSED, OPEN, UNKNOWN, LIMIT_EXCEEDED }
    /** Adapter must certify footing/headroom and barriers at the pen's animal level.
     * Gates are structural barriers even while open; unloaded/ambiguous terrain is UNKNOWN.
     * OPEN denotes a known escape, not simply an ordinary interior air column. */
    public interface Source { Cell cell(Column position); }
    public record Limits(int radius, int maximumArea) {
        public Limits {
            if (radius < 1 || radius > 64 || maximumArea < 1 || maximumArea > 4096)
                throw new IllegalArgumentException("Invalid pen scan limits");
        }
        public static Limits prototype() { return new Limits(32, 1024); }
    }
    public record Evidence(State state, Set<Column> interior, Set<Column> boundary) {
        public Evidence {
            Objects.requireNonNull(state);
            interior = Set.copyOf(interior);
            boundary = Set.copyOf(boundary);
            if (!Collections.disjoint(interior, boundary)
                    || state == State.ENCLOSED && (interior.isEmpty() || boundary.isEmpty()))
                throw new IllegalArgumentException("Invalid pen evidence");
        }
    }

    private final Source source;
    private final Column seed;
    private final Limits limits;
    private final Deque<Column> queue = new ArrayDeque<>();
    private final Set<Column> scheduled = new HashSet<>();
    private final Set<Column> interior = new HashSet<>(), boundary = new HashSet<>();
    private State terminal;

    public PenScan(Source source, Column seed, Limits limits) {
        this.source = Objects.requireNonNull(source);
        this.seed = Objects.requireNonNull(seed);
        this.limits = Objects.requireNonNull(limits);
        queue.add(seed);
        scheduled.add(seed);
    }

    /** At most budget source reads. A rejected scan never returns a claimable partial footprint. */
    public int advance(int budget) {
        if (budget < 1 || budget > 4096) throw new IllegalArgumentException("Invalid pen tick budget");
        int used = 0;
        while (!complete() && used < budget) {
            var position = queue.removeFirst();
            var cell = Objects.requireNonNull(source.cell(position));
            used++;
            switch (cell) {
                case UNKNOWN -> terminal = State.UNKNOWN;
                case OPEN -> terminal = State.OPEN;
                case BARRIER -> {
                    if (position.equals(seed)) terminal = State.OPEN;
                    else boundary.add(position);
                }
                case INTERIOR -> {
                    interior.add(position);
                    if (interior.size() > limits.maximumArea()) terminal = State.LIMIT_EXCEEDED;
                    else for (var offset : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                        long x = (long) position.x() + offset[0], z = (long) position.z() + offset[1];
                        if (Math.abs(x - seed.x()) > limits.radius() || Math.abs(z - seed.z()) > limits.radius()
                                || x < Integer.MIN_VALUE || x > Integer.MAX_VALUE
                                || z < Integer.MIN_VALUE || z > Integer.MAX_VALUE) {
                            terminal = State.LIMIT_EXCEEDED;
                            break;
                        }
                        var neighbor = new Column((int) x, (int) z);
                        if (scheduled.add(neighbor)) queue.addLast(neighbor);
                    }
                }
            }
            if (terminal == null && queue.isEmpty()) terminal = State.ENCLOSED;
        }
        return used;
    }
    public boolean complete() { return terminal != null; }
    public Evidence evidence() {
        if (!complete()) throw new IllegalStateException("Pen scan unfinished");
        return terminal == State.ENCLOSED ? new Evidence(terminal, interior, boundary)
                : new Evidence(terminal, Set.of(), Set.of());
    }
}
