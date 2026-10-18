package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Per-resource capture continuity, independent of the sticky global trust fence. */
public final class ResourceRecoveryEpochs {
    public record State(UUID generation, boolean healthy) {
        public State {
            Objects.requireNonNull(generation, "generation");
        }
    }

    private final Connection connection;

    public ResourceRecoveryEpochs(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    public static void createSchema(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE resource_recovery_epochs(
                        kind TEXT PRIMARY KEY CHECK(kind IN ('ore','forest')),
                        generation TEXT NOT NULL,
                        healthy INTEGER NOT NULL CHECK(healthy IN (0,1))
                    )
                    """);
        }
    }

    public Optional<State> load(String kind) throws SQLException {
        return load(connection, kind);
    }

    private static Optional<State> load(Connection connection, String kind) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        requireKind(kind);
        try (var statement = connection.prepareStatement(
                "SELECT generation,healthy FROM resource_recovery_epochs WHERE kind=?")) {
            statement.setString(1, kind);
            try (var result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(new State(UUID.fromString(result.getString(1)), result.getInt(2) == 1))
                        : Optional.empty();
            }
        }
    }

    /** Apply a continuity transition inside the caller's existing transaction. */
    static void transitionRows(Connection connection, String kind, boolean begin,
                               boolean priorClean, boolean gap) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        requireKind(kind);
        if (connection.getAutoCommit())
            throw new SQLException("Recovery epoch transition requires an active transaction");

        Optional<State> previous = load(connection, kind);
        if (begin) {
            State next = previous.filter(state -> priorClean && state.healthy())
                    .orElseGet(() -> new State(UUID.randomUUID(), true));
            // A resumed capture is operationally healthy even when global trust is sticky-false.
            next = new State(next.generation(), true);
            write(connection, kind, next);
        } else if (gap) {
            State next;
            if (previous.isEmpty()) {
                next = new State(UUID.randomUUID(), false);
            } else {
                State old = previous.orElseThrow();
                next = new State(old.healthy() ? UUID.randomUUID() : old.generation(), false);
            }
            write(connection, kind, next);
        }
    }

    private static void write(Connection connection, String kind, State state) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO resource_recovery_epochs(kind,generation,healthy) VALUES(?,?,?)
                ON CONFLICT(kind) DO UPDATE SET generation=excluded.generation,healthy=excluded.healthy
                """)) {
            statement.setString(1, kind);
            statement.setString(2, state.generation().toString());
            statement.setInt(3, state.healthy() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    private static void requireKind(String kind) {
        if (!"ore".equals(kind) && !"forest".equals(kind))
            throw new IllegalArgumentException("Unknown resource recovery kind: " + kind);
    }
}
