package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Explicit area release preserves the original enclosure and every animal's ownership. */
public final class PenProtectionLifecycle {
    public record Release(UUID operation, UUID pen, UUID actor, long releasedMillis, String reason) {
        public Release {
            Objects.requireNonNull(operation); Objects.requireNonNull(pen); Objects.requireNonNull(actor);
            Objects.requireNonNull(reason);
            if (releasedMillis < 0 || reason.isBlank() || reason.length() > 256)
                throw new IllegalArgumentException("Invalid pen release");
        }
    }

    /** Expected permission/conflict rejection, distinct from a database failure. */
    public static final class Declined extends SQLException {
        public Declined(String message) { super(message); }
    }

    private final Connection connection;
    public PenProtectionLifecycle(Connection connection) { this.connection = Objects.requireNonNull(connection); }

    static void createSchema(Connection connection) throws SQLException {
        try (var s = connection.createStatement()) {
            // No table references these columns. Retain all historical coordinates while
            // moving the location exclusion rule to active claims, not immutable history.
            s.execute("""
                CREATE TABLE pen_columns_v88 (
                    pen_id TEXT NOT NULL, world_id TEXT NOT NULL, standing_y INTEGER NOT NULL,
                    x INTEGER NOT NULL,z INTEGER NOT NULL,kind TEXT NOT NULL CHECK(kind IN('INTERIOR','BOUNDARY')),
                    PRIMARY KEY(pen_id,x,z),
                    FOREIGN KEY(pen_id,world_id,standing_y) REFERENCES protected_pens(id,world_id,standing_y)
                )
                """);
            s.execute("INSERT INTO pen_columns_v88 SELECT * FROM protected_pen_columns");
            s.execute("DROP TABLE protected_pen_columns");
            s.execute("ALTER TABLE pen_columns_v88 RENAME TO protected_pen_columns");
            s.execute("CREATE INDEX protected_pen_location ON protected_pen_columns(world_id,standing_y,x,z,pen_id)");
            s.execute("""
                CREATE TABLE active_pen_claims (
                    pen_id TEXT PRIMARY KEY NOT NULL REFERENCES protected_pens(id)
                )
                """);
            s.execute("INSERT INTO active_pen_claims SELECT id FROM protected_pens");
            s.execute("""
                CREATE TABLE pen_releases (
                    operation_id TEXT PRIMARY KEY NOT NULL,
                    pen_id TEXT NOT NULL UNIQUE REFERENCES protected_pens(id),
                    actor_id TEXT NOT NULL,
                    released_millis INTEGER NOT NULL CHECK(typeof(released_millis)='integer' AND released_millis>=0),
                    reason TEXT NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 256)
                )
                """);
            // Adjacent enclosures may share a fence. Neither interior nor a fence
            // occupying another enclosure's interior may overlap an active claim.
            s.execute("""
                CREATE TRIGGER active_pen_overlap BEFORE INSERT ON active_pen_claims
                WHEN EXISTS (
                    SELECT 1 FROM protected_pen_columns proposed
                    JOIN protected_pen_columns existing ON existing.world_id=proposed.world_id
                        AND existing.standing_y=proposed.standing_y AND existing.x=proposed.x AND existing.z=proposed.z
                    JOIN active_pen_claims a ON a.pen_id=existing.pen_id
                    WHERE proposed.pen_id=NEW.pen_id AND (proposed.kind='INTERIOR' OR existing.kind='INTERIOR')
                ) OR EXISTS (SELECT 1 FROM pen_releases WHERE pen_id=NEW.pen_id)
                BEGIN SELECT RAISE(ABORT,'Pen overlaps active protection or was released'); END
                """);
        }
    }

    public Optional<Release> released(UUID pen) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM pen_releases WHERE pen_id=?")) {
            s.setString(1, Objects.requireNonNull(pen).toString());
            try (var r = s.executeQuery()) {
                return r.next() ? Optional.of(new Release(UUID.fromString(r.getString("operation_id")), pen,
                        UUID.fromString(r.getString("actor_id")), r.getLong("released_millis"), r.getString("reason"))) : Optional.empty();
            }
        }
    }

    /** Only the recorded owner or a freshly certified admin may release a saved area.
     * The admin flag is trusted adapter authority, never sign placement or proximity. */
    public Release release(Release request, boolean admin) throws SQLException {
        Objects.requireNonNull(request);
        if (!connection.getAutoCommit()) throw new SQLException("Pen release requires no active transaction");
        connection.setAutoCommit(false);
        try {
            var pen = new PenProtectionRepository(connection).load(request.pen())
                    .orElseThrow(() -> new Declined("Unknown protected pen"));
            if (!admin && !pen.owner().equals(request.actor())) throw new Declined("Only the recorded owner or admin may release this area");
            if (request.releasedMillis() < pen.confirmedMillis()) throw new Declined("Pen release predates its protection");
            var previous = released(request.pen());
            if (previous.isPresent()) {
                if (!previous.orElseThrow().equals(request)) throw new Declined("Pen already released with different terms");
            } else {
                try (var s = connection.prepareStatement("SELECT 1 FROM pen_releases WHERE operation_id=?")) {
                    s.setString(1, request.operation().toString());
                    try (var r = s.executeQuery()) { if (r.next()) throw new Declined("Release operation already used for another pen"); }
                }
                try (var s = connection.prepareStatement("INSERT INTO pen_releases VALUES(?,?,?,?,?)")) {
                    s.setString(1, request.operation().toString()); s.setString(2, request.pen().toString());
                    s.setString(3, request.actor().toString()); s.setLong(4, request.releasedMillis()); s.setString(5, request.reason());
                    s.executeUpdate();
                }
                try (var s = connection.prepareStatement("DELETE FROM active_pen_claims WHERE pen_id=?")) {
                    s.setString(1, request.pen().toString());
                    if (s.executeUpdate() != 1) throw new SQLException("Saved pen is missing its active area claim");
                }
            }
            connection.commit(); connection.setAutoCommit(true); return request;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
}
