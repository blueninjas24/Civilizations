package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** Expand approved marker constraints without foreign-key disabling or history loss.
 * Caller owns the schema transaction. Children are removed before parents; copies restore
 * parents first. Explicit indices/triggers survive; triggers return after copying to avoid
 * generating artificial history during migration. No new observations or anchors are invented. */
final class WorkplaceMarkerMigration {
    private WorkplaceMarkerMigration() {}
    private static final List<String> CHILD_FIRST=List.of("workplace_markers","workplace_sightings","workplace_operation_history","business_workplace_anchors");
    private static final List<String> PARENT_FIRST=List.of("workplace_sightings","workplace_markers","business_workplace_anchors","workplace_operation_history");
    static void apply(Connection connection)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Marker migration requires schema transaction");
        // DROP TABLE may execute foreign-key cascade/set-null actions even though SQL triggers
        // do not fire. Unknown inbound references need an explicit preservation plan first.
        var schemaTables=new ArrayList<String>();
        try(var s=connection.createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")){while(r.next())schemaTables.add(r.getString(1));}
        for(String table:schemaTables)if(!CHILD_FIRST.contains(table)) {
            String quoted="\""+table.replace("\"","\"\"")+"\"";
            try(var s=connection.createStatement();var r=s.executeQuery("PRAGMA foreign_key_list("+quoted+")")) {
                while(r.next())if(CHILD_FIRST.contains(r.getString("table")))throw new SQLException("Marker upgrade refuses external foreign key from "+table+"; explicit preservation plan required");
            }
        }
        var definitions=new HashMap<String,String>();var accessories=new ArrayList<String>();
        for(String table:CHILD_FIRST) {
            try(var s=connection.prepareStatement("SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                s.setString(1,table);try(var r=s.executeQuery()) {
                    if(!r.next())throw new SQLException("Missing marker migration predecessor: "+table);
                    String sql=r.getString(1);
                    if(table.equals("workplace_sightings")||table.equals("business_workplace_anchors")) {
                        String expanded="CHECK(kind IN ('BARREL','SMITHING_TABLE','SMOKER','CRAFTING_TABLE'))";
                        String replaced=sql.replace("CHECK(kind IN('BARREL','SMITHING_TABLE'))",expanded)
                                .replace("CHECK(kind IN ('BARREL','SMITHING_TABLE'))",expanded);
                        if(replaced.equals(sql)&&!sql.contains(expanded))throw new SQLException("Unsupported marker predecessor constraint: "+table);
                        sql=replaced;
                    }
                    definitions.put(table,sql);
                }
            }
            try(var s=connection.prepareStatement("SELECT sql FROM sqlite_master WHERE tbl_name=? AND type IN('index','trigger') AND sql IS NOT NULL ORDER BY type,name")) {
                s.setString(1,table);try(var r=s.executeQuery()){while(r.next())accessories.add(r.getString(1));}
            }
            execute(connection,"CREATE TEMP TABLE civ_schema77_"+table+" AS SELECT * FROM "+table);
        }
        for(String table:CHILD_FIRST)execute(connection,"DROP TABLE "+table);
        for(String table:PARENT_FIRST) {
            execute(connection,definitions.get(table));
            execute(connection,"INSERT INTO "+table+" SELECT * FROM civ_schema77_"+table);
        }
        for(String sql:accessories)execute(connection,sql);
        try(var s=connection.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")) {
            if(r.next())throw new SQLException("Marker migration violates foreign keys in "+r.getString(1));
        }
        for(String table:CHILD_FIRST)execute(connection,"DROP TABLE civ_schema77_"+table);
    }
    private static void execute(Connection connection,String sql)throws SQLException{try(var s=connection.createStatement()){s.execute(sql);}}
}
