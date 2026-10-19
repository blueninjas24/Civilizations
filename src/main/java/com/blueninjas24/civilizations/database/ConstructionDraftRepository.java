package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/** SQL-owner immutable proposal storage. This repository cannot approve, fund or execute construction. */
public final class ConstructionDraftRepository {
    private final Connection connection;
    public ConstructionDraftRepository(Connection connection){this.connection=Objects.requireNonNull(connection);}
    static void createSchema(Connection c)throws SQLException {
        try(var statement=c.createStatement()){statement.execute("""
            CREATE TABLE construction_drafts (
                id TEXT PRIMARY KEY NOT NULL,
                settlement_id TEXT NOT NULL,
                world_id TEXT NOT NULL,
                origin_x INTEGER NOT NULL CHECK(typeof(origin_x)='integer' AND origin_x BETWEEN -2147483648 AND 2147483647),
                origin_y INTEGER NOT NULL CHECK(typeof(origin_y)='integer' AND origin_y BETWEEN -2147483648 AND 2147483647),
                origin_z INTEGER NOT NULL CHECK(typeof(origin_z)='integer' AND origin_z BETWEEN -2147483648 AND 2147483647),
                rotation TEXT NOT NULL CHECK(rotation IN ('NONE','CLOCKWISE_90','CLOCKWISE_180','COUNTERCLOCKWISE_90')),
                mirrored INTEGER NOT NULL CHECK(typeof(mirrored)='integer' AND mirrored IN (0,1)),
                template_hash TEXT NOT NULL CHECK(length(template_hash)=64),
                template_snapshot BLOB NOT NULL CHECK(typeof(template_snapshot)='blob' AND length(template_snapshot) BETWEEN 1 AND 8388608),
                created_ms INTEGER NOT NULL CHECK(typeof(created_ms)='integer' AND created_ms>=0),
                FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id)
            )
            """);statement.execute("CREATE INDEX construction_drafts_by_village ON construction_drafts(settlement_id,created_ms,id)");}
    }
    /** Identity replay is exact; changed terms never silently replace a previously reviewed plan. */
    public ConstructionDraft save(ConstructionDraft draft)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Construction draft save requires no active owner transaction");
        connection.setAutoCommit(false);
        try {
            var old=find(draft.id());
            if(old.isPresent()) {
                if(!old.orElseThrow().equals(draft))throw new SQLException("Construction draft identity reused with changed terms");
            }else try(var s=connection.prepareStatement("INSERT INTO construction_drafts VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,draft.id().toString());s.setString(2,draft.settlement().toString());s.setString(3,draft.world().toString());
                s.setInt(4,draft.origin().x());s.setInt(5,draft.origin().y());s.setInt(6,draft.origin().z());s.setString(7,draft.transform().rotation().name());s.setInt(8,draft.transform().mirror()?1:0);
                s.setString(9,draft.template().fingerprint());s.setBytes(10,ConstructionTemplateSnapshot.encode(draft.template()));s.setLong(11,draft.createdMillis());s.executeUpdate();
            }
            connection.commit();connection.setAutoCommit(true);return draft;
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    public Optional<ConstructionDraft> find(UUID id)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM construction_drafts WHERE id=?")) {
            s.setString(1,id.toString());try(var row=s.executeQuery()) {
                if(!row.next())return Optional.empty();
                try {
                    var template=ConstructionTemplateSnapshot.decode(row.getBytes("template_snapshot"));
                    if(!template.fingerprint().equals(row.getString("template_hash")))throw new SQLException("Construction snapshot hash mismatch");
                    return Optional.of(new ConstructionDraft(UUID.fromString(row.getString("id")),UUID.fromString(row.getString("settlement_id")),UUID.fromString(row.getString("world_id")),
                            new Position(row.getInt("origin_x"),row.getInt("origin_y"),row.getInt("origin_z")),new Transform(Rotation.valueOf(row.getString("rotation")),row.getBoolean("mirrored")),template,row.getLong("created_ms")));
                }catch(IOException|IllegalArgumentException|ArithmeticException invalid){throw new SQLException("Invalid saved construction draft",invalid);}
            }
        }
    }
}
