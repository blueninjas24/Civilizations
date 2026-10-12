package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;

/** SQL-owner habitat identity, conserved renewable stock and immutable work audit. */
public final class FisheryRepository {
    public record Resource(UUID site, UUID world, FishStock stock, long observedMillis, String openingTerms) {}
    private final Connection connection;
    public FisheryRepository(Connection connection) { this.connection = Objects.requireNonNull(connection); }

    /** Trusted evidence only. Repeat visits/retunes never refill or enlarge an existing habitat. */
    public Resource open(UUID site, FisheryObservation observation, FishingPolicy policy) throws SQLException {
        Objects.requireNonNull(site); Objects.requireNonNull(observation); Objects.requireNonNull(policy);
        if (!connection.getAutoCommit()) throw new SQLException("Fishery opening requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var result = openRows(site, observation, policy);
            connection.commit(); connection.setAutoCommit(true); return result;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }

    Resource openRows(UUID site, FisheryObservation observation, FishingPolicy policy) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Fishery opening rows require owner transaction");
        if (!observation.complete()) throw new SQLException("Fishery needs complete suitable water evidence");
        try (var s = connection.prepareStatement("SELECT world_id FROM business_sites WHERE id=?")) {
            s.setString(1, site.toString()); try (var r = s.executeQuery()) {
                if (!r.next() || !observation.world().toString().equals(r.getString(1)))
                    throw new SQLException("Fishery evidence must match the actual saved site world");
            }
        }
        var old = find(site);
        if (old.isPresent()) {
            if (!columns(site).equals(observation.columns()))
                throw new SQLException("Fishery footprint changed; explicit resource transition required");
            return old.orElseThrow();
        }
        var opening = policy.opening(observation.columns().size(), true);
        try (var s = connection.prepareStatement("INSERT INTO fishery_resources(site_id,world_id,capacity,available,remainder,observed_ms,opening_terms) VALUES(?,?,?,?,?,?,?)")) {
            s.setString(1, site.toString()); s.setString(2, observation.world().toString());
            s.setLong(3, opening.capacity()); s.setLong(4, opening.available()); s.setInt(5, 0);
            s.setLong(6, observation.observedMillis()); s.setString(7, policy.toString()); s.executeUpdate();
        }
        for (var c : observation.columns()) try (var s = connection.prepareStatement("INSERT INTO fishery_columns(world_id,x,z,site_id) VALUES(?,?,?,?)")) {
            s.setString(1, observation.world().toString()); s.setInt(2, c.x()); s.setInt(3, c.z()); s.setString(4, site.toString()); s.executeUpdate();
        }
        return find(site).orElseThrow();
    }

    public Optional<Resource> find(UUID site) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM fishery_resources WHERE site_id=?")) {
            s.setString(1, site.toString()); try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                return Optional.of(new Resource(site, UUID.fromString(r.getString("world_id")),
                        new FishStock(r.getLong("capacity"), r.getLong("available"), r.getInt("remainder")),
                        r.getLong("observed_ms"), r.getString("opening_terms")));
            }
        }
    }

    private List<FisheryObservation.Column> columns(UUID site) throws SQLException {
        var result = new ArrayList<FisheryObservation.Column>();
        try (var s = connection.prepareStatement("SELECT x,z FROM fishery_columns WHERE site_id=? ORDER BY x,z")) {
            s.setString(1, site.toString()); try (var r = s.executeQuery()) {
                while (r.next()) result.add(new FisheryObservation.Column(r.getInt(1), r.getInt(2)));
            }
        }
        return List.copyOf(result);
    }

    void saveWorkRows(UUID operation, UUID business, Resource before, long period, FishStock.Period plan, FishingPolicy policy) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Fishery work requires owner transaction");
        try (var s = connection.prepareStatement("UPDATE fishery_resources SET available=?,remainder=? WHERE site_id=? AND available=? AND remainder=?")) {
            s.setLong(1, plan.stock().available()); s.setInt(2, plan.stock().recoveryRemainder()); s.setString(3, before.site().toString());
            s.setLong(4, before.stock().available()); s.setInt(5, before.stock().recoveryRemainder());
            if (s.executeUpdate() != 1) throw new SQLException("Fishery stock changed during owner work");
        }
        try (var s = connection.prepareStatement("INSERT INTO fishery_work VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1, operation.toString()); s.setString(2, before.site().toString()); s.setString(3, business.toString()); s.setLong(4, period);
            s.setLong(5, before.stock().available()); s.setLong(6, plan.recovered()); s.setLong(7, plan.caught()); s.setLong(8, plan.stock().available());
            s.setInt(9, before.stock().recoveryRemainder()); s.setInt(10, plan.stock().recoveryRemainder()); s.setString(11, policy.toString()); s.executeUpdate();
        }
    }
}
