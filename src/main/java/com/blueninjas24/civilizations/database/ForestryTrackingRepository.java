package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** Clean-stop fence for loaded-world tree provenance; gaps never silently certify placement history. */
public final class ForestryTrackingRepository {
    public record State(boolean cleanStop, boolean trusted, long observedMillis) {}

    private final Connection connection;

    public ForestryTrackingRepository(Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    public Optional<State> load() throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT clean_stop,trusted,observed_millis FROM forestry_tracking WHERE id=1")) {
            return result.next()
                    ? Optional.of(new State(result.getInt(1) == 1, result.getInt(2) == 1, result.getLong(3)))
                    : Optional.empty();
        }
    }

    /** Persists a dirty fence before capture; first-run trust is safe only when no saved forests exist. */
    public State begin(long now) throws SQLException {
        return change(now, Transition.BEGIN, true);
    }

    /** A healthy stop cannot repair a prior unclean stop or capture gap. */
    public State finish(long now, boolean healthy) throws SQLException {
        return change(now, Transition.FINISH, healthy);
    }

    public State markGap(long now) throws SQLException {
        return change(now, Transition.GAP, false);
    }

    private enum Transition { BEGIN, FINISH, GAP }

    private State change(long now, Transition transition, boolean healthy) throws SQLException {
        if (now < 0) throw new IllegalArgumentException("Invalid forestry tracking timestamp");
        if (!connection.getAutoCommit()) throw new SQLException("Forestry tracking transition requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var previous = load();
            if (transition != Transition.BEGIN && previous.isEmpty())
                throw new SQLException("Forestry tracking has not begun");

            ResourceRecoveryEpochs.transitionRows(connection, "forest",
                    transition == Transition.BEGIN,
                    previous.map(State::cleanStop).orElse(false),
                    transition == Transition.GAP || (transition == Transition.FINISH && !healthy));

            boolean trusted;
            if (previous.isEmpty()) {
                trusted = transition == Transition.BEGIN && healthy && resourceCount() == 0;
            } else {
                var state = previous.orElseThrow();
                trusted = state.trusted() && (transition != Transition.BEGIN || state.cleanStop())
                        && transition != Transition.GAP && healthy;
            }
            boolean cleanStop = transition == Transition.FINISH && healthy;
            long observedMillis = Math.max(now, previous.map(State::observedMillis).orElse(0L));
            var state = new State(cleanStop, trusted, observedMillis);
            try (var statement = connection.prepareStatement("""
                    INSERT INTO forestry_tracking(id,clean_stop,trusted,observed_millis) VALUES(1,?,?,?)
                    ON CONFLICT(id) DO UPDATE SET clean_stop=excluded.clean_stop,trusted=excluded.trusted,
                        observed_millis=excluded.observed_millis
                    """)) {
                statement.setInt(1, state.cleanStop() ? 1 : 0);
                statement.setInt(2, state.trusted() ? 1 : 0);
                statement.setLong(3, state.observedMillis());
                statement.executeUpdate();
            }
            connection.commit();
            connection.setAutoCommit(true);
            return state;
        } catch (SQLException | RuntimeException failure) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollback) {
                failure.addSuppressed(rollback);
            }
            throw failure;
        }
    }

    private long resourceCount() throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM forestry_resources")) {
            if (!result.next()) throw new SQLException("Cannot count saved forestry resources");
            return result.getLong(1);
        }
    }
}
