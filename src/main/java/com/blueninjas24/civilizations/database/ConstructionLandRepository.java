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
        return protectedColumns(world,bounds,null);
    }
    /** A project's owner review may ignore only its own reservation; all player plots remain excluded. */
    Set<Column> protectedColumns(UUID world,Bounds bounds,UUID ownDraft)throws SQLException {
        Objects.requireNonNull(world);Objects.requireNonNull(bounds);
        long width=(long)bounds.maximum().x()-bounds.minimum().x()+1,depth=(long)bounds.maximum().z()-bounds.minimum().z()+1;
        if(width>96||depth>96)throw new IllegalArgumentException("Construction protection review exceeds local footprint budget");
        var result=new HashSet<Column>();
        try(var s=connection.prepareStatement("""
            SELECT DISTINCT c.x,c.z FROM protected_pen_columns c JOIN active_pen_claims a ON a.pen_id=c.pen_id
            WHERE c.world_id=? AND c.x BETWEEN ? AND ? AND c.z BETWEEN ? AND ? LIMIT 9217
            """)) {
            s.setString(1,world.toString());s.setInt(2,bounds.minimum().x());s.setInt(3,bounds.maximum().x());s.setInt(4,bounds.minimum().z());s.setInt(5,bounds.maximum().z());
            try(var r=s.executeQuery()){while(r.next()) {
                if(result.size()==9216)throw new SQLException("Construction protection coverage exceeded expected column budget");result.add(new Column(r.getInt(1),r.getInt(2)));
            }}
        }
        try(var s=connection.prepareStatement("""
            SELECT draft_id,min_x,max_x,min_z,max_z FROM construction_site_reservations
            WHERE world_id=? AND released_ms IS NULL AND min_x<=? AND max_x>=? AND min_z<=? AND max_z>=?
            ORDER BY draft_id LIMIT 129
            """)) {
            s.setString(1,world.toString());s.setInt(2,bounds.maximum().x());s.setInt(3,bounds.minimum().x());s.setInt(4,bounds.maximum().z());s.setInt(5,bounds.minimum().z());
            int reviewed=0;try(var r=s.executeQuery()){while(r.next()) {
                if(++reviewed>128)throw new SQLException("Construction reservation coverage exceeds local review budget");
                if(ownDraft!=null&&ownDraft.toString().equals(r.getString(1)))continue;
                int x0=Math.max(bounds.minimum().x(),r.getInt(2)),x1=Math.min(bounds.maximum().x(),r.getInt(3));
                int z0=Math.max(bounds.minimum().z(),r.getInt(4)),z1=Math.min(bounds.maximum().z(),r.getInt(5));
                // Long loop counters also safely handle a footprint at Integer.MAX_VALUE.
                for(long x=x0;x<=x1;x++)for(long z=z0;z<=z1;z++)result.add(new Column((int)x,(int)z));
            }}
        }
        return Set.copyOf(result);
    }
}
