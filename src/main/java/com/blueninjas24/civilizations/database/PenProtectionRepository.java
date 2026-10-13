package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.sql.*;
import java.util.*;

/** SQL-owner confirmed protective claims, never inferred from sign presence. No live player API yet. */
public final class PenProtectionRepository {
    private final Connection connection;
    public PenProtectionRepository(Connection connection) { this.connection = Objects.requireNonNull(connection); }
    public Optional<PenProtection> load(UUID id) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM protected_pens WHERE id=?")) {
            s.setString(1,id.toString());
            try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                var interior = new HashSet<PenScan.Column>(); var boundary = new HashSet<PenScan.Column>();
                try (var cells = connection.prepareStatement("SELECT x,z,kind FROM protected_pen_columns WHERE pen_id=?")) {
                    cells.setString(1,id.toString()); try (var c = cells.executeQuery()) { while (c.next())
                        (c.getString(3).equals("INTERIOR") ? interior : boundary).add(new PenScan.Column(c.getInt(1),c.getInt(2))); }
                }
                var animals = new HashSet<UUID>();
                try (var rows = connection.prepareStatement("SELECT animal_id FROM protected_pen_animals WHERE pen_id=?")) {
                    rows.setString(1,id.toString()); try (var a = rows.executeQuery()) { while(a.next()) animals.add(UUID.fromString(a.getString(1))); }
                }
                return Optional.of(new PenProtection(id,UUID.fromString(r.getString("world_id")),UUID.fromString(r.getString("owner_id")),
                        r.getInt("standing_y"),r.getInt("sign_x"),r.getInt("sign_y"),r.getInt("sign_z"),
                        new PenScan.Column(r.getInt("seed_x"),r.getInt("seed_z")),new PenScan.Evidence(PenScan.State.ENCLOSED,interior,boundary),animals,r.getLong("confirmed_millis")));
            }
        }
    }
    /** True authority is supplied by the trusted adapter after fresh geometry/animal/permission checks.
     * Unknown old pens require admin resolution. A nearby player or sign placer is not authority. */
    public PenProtection save(PenProtection pen, boolean authorized) throws SQLException {
        Objects.requireNonNull(pen);
        if (!authorized) throw new SQLException("Pen protection requires certified authority");
        if (!connection.getAutoCommit()) throw new SQLException("Pen protection requires no active transaction");
        connection.setAutoCommit(false);
        try {
            var old = load(pen.id());
            if (old.isPresent()) {
                if (!old.orElseThrow().equals(pen)) throw new SQLException("Pen ID reused with changed protection");
            } else saveRows(pen);
            connection.commit(); connection.setAutoCommit(true); return old.orElse(pen);
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch(SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
    private void saveRows(PenProtection pen) throws SQLException {
        try (var s = connection.prepareStatement("INSERT INTO protected_pens VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,pen.id().toString());s.setString(2,pen.world().toString());s.setString(3,pen.owner().toString());s.setInt(4,pen.standingY());
            s.setInt(5,pen.signX());s.setInt(6,pen.signY());s.setInt(7,pen.signZ());s.setInt(8,pen.seed().x());s.setInt(9,pen.seed().z());s.setLong(10,pen.confirmedMillis());s.executeUpdate();
        }
        for (var kind : List.of("INTERIOR","BOUNDARY")) for (var p : kind.equals("INTERIOR") ? pen.evidence().interior() : pen.evidence().boundary())
            try (var s = connection.prepareStatement("INSERT INTO protected_pen_columns VALUES(?,?,?,?,?,?)")) {
                s.setString(1,pen.id().toString());s.setString(2,pen.world().toString());s.setInt(3,pen.standingY());s.setInt(4,p.x());s.setInt(5,p.z());s.setString(6,kind);s.executeUpdate();
            }
        var ownership = new AnimalOwnershipRepository(connection);
        for (var animal : pen.animals().stream().sorted().toList()) {
            var old = ownership.load(animal);
            if (old.isPresent()) {
                var claim = old.orElseThrow();
                if (claim.kind()!=LivestockAdoption.Ownership.PLAYER || !pen.owner().equals(claim.player()))
                    throw new SQLException("Pen cannot take an animal belonging to another player or village");
            } else ownership.recordRows(new AnimalOwnershipRepository.Claim(animal,pen.world(),LivestockAdoption.Ownership.PLAYER,
                    pen.owner(),null,pen.confirmedMillis(),"Confirmed pen "+pen.id()));
            try (var s = connection.prepareStatement("INSERT INTO protected_pen_animals VALUES(?,?)")) {
                s.setString(1,pen.id().toString());s.setString(2,animal.toString());s.executeUpdate();
            }
        }
    }
    /** Saved protection is independent of today's fence/sign and never spans an entire world. */
    public Optional<UUID> at(UUID world,int standingY,int x,int z) throws SQLException {
        try (var s = connection.prepareStatement("SELECT pen_id FROM protected_pen_columns WHERE world_id=? AND standing_y=? AND x=? AND z=?")) {
            s.setString(1,world.toString());s.setInt(2,standingY);s.setInt(3,x);s.setInt(4,z);
            try (var r = s.executeQuery()) { return r.next() ? Optional.of(UUID.fromString(r.getString(1))) : Optional.empty(); }
        }
    }
}
