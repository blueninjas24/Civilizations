package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.ConstructionPreview.Bounds;
import java.sql.*;
import java.util.*;

/** Read-only construction exclusion view. A saved fenced plot excludes its columns, even when empty. */
public final class ConstructionLandRepository {
    public record Column(int x,int z){}
    private final Connection connection;
    public ConstructionLandRepository(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public Set<Column> protectedColumns(UUID world,Bounds bounds)throws SQLException {
        Objects.requireNonNull(world);Objects.requireNonNull(bounds);
        long width=(long)bounds.maximum().x()-bounds.minimum().x()+1,depth=(long)bounds.maximum().z()-bounds.minimum().z()+1;
        if(width>66||depth>66)throw new IllegalArgumentException("Construction protection review exceeds local footprint budget");
        try(var s=connection.prepareStatement("""
            SELECT DISTINCT c.x,c.z FROM protected_pen_columns c JOIN active_pen_claims a ON a.pen_id=c.pen_id
            WHERE c.world_id=? AND c.x BETWEEN ? AND ? AND c.z BETWEEN ? AND ? LIMIT 4357
            """)) {
            s.setString(1,world.toString());s.setInt(2,bounds.minimum().x());s.setInt(3,bounds.maximum().x());s.setInt(4,bounds.minimum().z());s.setInt(5,bounds.maximum().z());
            var result=new HashSet<Column>();try(var r=s.executeQuery()){while(r.next()) {
                if(result.size()==4356)throw new SQLException("Construction protection coverage exceeded expected column budget");result.add(new Column(r.getInt(1),r.getInt(2)));
            }}return Set.copyOf(result);
        }
    }
}
