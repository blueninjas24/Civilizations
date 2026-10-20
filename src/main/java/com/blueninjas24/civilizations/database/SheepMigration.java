package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** Version103 expands only four species constraints, retaining rows, keys, indexes and triggers. */
final class SheepMigration {
    static final List<String> TABLES=List.of("livestock_herds","livestock_birth_entities","livestock_presence","livestock_slaughter");
    private SheepMigration(){}
    static void apply(Connection c)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Sheep migration requires the migration transaction");
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_keys")) {
            if(r.getInt(1)!=0)throw new SQLException("Species parent rebuild requires FK enforcement disabled before transaction");
        }
        for(var table:TABLES) {
            String original;
            try(var s=c.prepareStatement("SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                s.setString(1,table);try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Missing livestock table: "+table);original=r.getString(1);}
            }
            String old="CHECK(species IN('COW','CHICKEN'))",expanded="CHECK(species IN('COW','CHICKEN','SHEEP'))";
            if(original.contains(expanded))continue; // Historical test fixtures may already carry this additive constraint.
            if(!original.contains(old))throw new SQLException("Unrecognized species constraint in "+table);
            var extras=new ArrayList<String>();
            try(var s=c.prepareStatement("SELECT sql FROM sqlite_master WHERE tbl_name=? AND type IN('index','trigger') AND sql IS NOT NULL ORDER BY type,name")) {
                s.setString(1,table);try(var r=s.executeQuery()){while(r.next())extras.add(r.getString(1));}
            }
            String temporary=table+"_sheep_upgrade";
            String updated=original.replaceFirst("CREATE TABLE \"?"+table+"\"?","CREATE TABLE "+temporary).replace(old,expanded);
            if(updated.equals(original)||!updated.contains("CREATE TABLE "+temporary))throw new SQLException("Cannot rebuild livestock table "+table);
            try(var s=c.createStatement()) {
                s.execute(updated);s.execute("INSERT INTO "+temporary+" SELECT * FROM "+table);
                s.execute("DROP TABLE "+table);s.execute("ALTER TABLE "+temporary+" RENAME TO "+table);
                for(var sql:extras)s.execute(sql);
            }
        }
    }
}
