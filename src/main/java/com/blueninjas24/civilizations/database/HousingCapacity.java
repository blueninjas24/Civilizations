package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.BuildingLayout;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.sql.*;
import java.util.*;

/** Shared population/food housing authority. Legacy observations remain separate from assigned homes. */
final class HousingCapacity {
    private HousingCapacity() {}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE household_capacity_sources (
                  world_id TEXT NOT NULL REFERENCES observed_worlds(id), x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL,
                  household_id TEXT NOT NULL REFERENCES households(id), capacity INTEGER NOT NULL CHECK(capacity BETWEEN 1 AND 8),
                  PRIMARY KEY(world_id,x,y,z)
                )
                """);
            s.execute("CREATE INDEX capacity_sources_household ON household_capacity_sources(household_id)");
            s.execute("""
                CREATE TABLE dwelling_bed_claims (
                  world_id TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL,
                  unit_id TEXT NOT NULL REFERENCES building_units(id), PRIMARY KEY(world_id,x,y,z)
                )
                """);
        }
        // Historical backend assignments may exist even though the live adapter was not enabled in109.
        var units=new ArrayList<UUID>();
        try(var s=c.createStatement();var r=s.executeQuery("SELECT DISTINCT unit_id FROM dwelling_assignments ORDER BY unit_id")) {
            while(r.next())units.add(UUID.fromString(r.getString(1)));
        }
        for(var unit:units)claim(c,unit,null);
    }
    static boolean managed(Connection c,UUID household)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM dwelling_assignments WHERE household_id=? LIMIT 1")) {
            s.setString(1,household.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
    static Set<UUID> legacyFamilies(Connection c,BuildingLayout layout)throws SQLException {
        var families=new HashSet<UUID>();
        for(var unit:layout.units())for(var bed:unit.beds()) {
            if(claimed(c,layout.world(),bed.head().x(),bed.head().y(),bed.head().z()))continue;
            try(var s=c.prepareStatement("SELECT household_id FROM village_beds WHERE world_id=? AND x=? AND y=? AND z=? AND household_id IS NOT NULL")) {
                bind(s,layout.world(),bed.head());try(var r=s.executeQuery()){if(r.next()) {
                    UUID family=UUID.fromString(r.getString(1));
                    if(!managed(c,family))try(var count=c.prepareStatement("SELECT 1 FROM residents WHERE household_id=? AND status='ALIVE' LIMIT 1")) {
                        count.setString(1,family.toString());try(var people=count.executeQuery()){if(people.next())families.add(family);}
                    }
                }}
            }
        }
        return Set.copyOf(families);
    }
    static boolean claimed(Connection c,UUID world,int x,int y,int z)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM dwelling_bed_claims WHERE world_id=? AND x=? AND y=? AND z=?")) {
            bind(s,world,new Position(x,y,z));try(var r=s.executeQuery()){return r.next();}
        }
    }
    /** Historical claims survive reclamation: legacy scanning cannot silently re-adopt a donated bed. */
    static void claim(Connection c,UUID unit,UUID household)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Bed claims must share assignment/migration transaction");
        UUID building;
        try(var s=c.prepareStatement("SELECT building_id FROM building_units WHERE id=?")) {
            s.setString(1,unit.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Missing dwelling unit");building=UUID.fromString(r.getString(1));}
        }
        var layout=new BuildingRegistry(c).find(building).orElseThrow().layout();
        var declared=layout.units().stream().filter(u->u.id().equals(unit)).findFirst().orElseThrow();
        for(var bed:declared.beds()) {
            if(household!=null) {
                try(var s=c.prepareStatement("""
                    SELECT b.household_id FROM village_beds b
                    WHERE b.world_id=? AND b.x=? AND b.y=? AND b.z=? AND b.household_id IS NOT NULL
                    AND b.household_id<>? AND EXISTS(SELECT 1 FROM residents r WHERE r.household_id=b.household_id AND r.status='ALIVE')
                    AND NOT EXISTS(SELECT 1 FROM dwelling_assignments a WHERE a.household_id=b.household_id)
                    """)) {
                    bind(s,layout.world(),bed.head());s.setString(5,household.toString());
                    try(var r=s.executeQuery()){if(r.next())throw new BuildingRegistry.Declined("Home still houses another legacy household; preserve that family first");}
                }
            }
            try(var s=c.prepareStatement("INSERT INTO dwelling_bed_claims VALUES(?,?,?,?,?) ON CONFLICT(world_id,x,y,z) DO NOTHING")) {
                bind(s,layout.world(),bed.head());s.setString(5,unit.toString());s.executeUpdate();
            }
            try(var s=c.prepareStatement("SELECT unit_id FROM dwelling_bed_claims WHERE world_id=? AND x=? AND y=? AND z=?")) {
                bind(s,layout.world(),bed.head());try(var r=s.executeQuery()){if(!r.next()||!unit.toString().equals(r.getString(1)))throw new BuildingRegistry.Declined("Bed is already assigned to another saved unit");}
            }
        }
    }
    static Map<UUID,Integer> read(Connection c)throws SQLException{return read(c,null);}
    static Map<UUID,Integer> read(Connection c,UUID town)throws SQLException {
        var result=new HashMap<UUID,Integer>();String filter=town==null?"":" WHERE h.settlement_id=?";
        try(var s=c.prepareStatement("SELECT h.id,c.capacity FROM household_capacity c JOIN households h ON h.id=c.household_id"+filter)) {
            if(town!=null)s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next())result.put(UUID.fromString(r.getString(1)),r.getInt(2));}
        }
        var touched=new HashSet<UUID>();
        try(var s=c.createStatement();var r=s.executeQuery("""
            SELECT DISTINCT b.household_id FROM village_beds b JOIN dwelling_bed_claims d
            ON d.world_id=b.world_id AND d.x=b.x AND d.y=b.y AND d.z=b.z WHERE b.household_id IS NOT NULL
            """)){while(r.next())touched.add(UUID.fromString(r.getString(1)));}
        for(var household:touched) {
            if(!result.containsKey(household))continue;
            try(var s=c.prepareStatement("""
                SELECT coalesce(sum(s.capacity),0),coalesce(sum(CASE WHEN d.unit_id IS NOT NULL THEN s.capacity ELSE 0 END),0)
                FROM household_capacity_sources s LEFT JOIN dwelling_bed_claims d
                ON d.world_id=s.world_id AND d.x=s.x AND d.y=s.y AND d.z=s.z WHERE s.household_id=?
                """)) {
                s.setString(1,household.toString());try(var r=s.executeQuery()) {
                    if(r.getLong(1)==result.get(household))result.put(household,Math.subtractExact(result.get(household),r.getInt(2)));
                    else result.remove(household); // Old unproven bed accounting is unknown, never invented space.
                }
            }
        }
        // An explicitly formed household has a known housing need even after enrollment refreshes raw
        // bed-group capacity. Only these recorded formations get zero; arbitrary old unknown homes do not.
        try(var s=c.prepareStatement("SELECT h.id FROM households h JOIN household_formations f ON f.household_id=h.id"+filter)) {
            if(town!=null)s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next())result.putIfAbsent(UUID.fromString(r.getString(1)),0);}
        }
        try(var s=c.prepareStatement("SELECT DISTINCT h.id FROM households h JOIN dwelling_assignments a ON a.household_id=h.id"+filter)) {
            if(town!=null)s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next())result.put(UUID.fromString(r.getString(1)),0);}
        }
        var occupancy=new DwellingOccupancy(c);var registry=new BuildingRegistry(c);
        try(var s=c.prepareStatement("SELECT o.household_id,o.unit_id FROM dwelling_active_occupancy o JOIN households h ON h.id=o.household_id"+filter)) {
            if(town!=null)s.setString(1,town.toString());try(var r=s.executeQuery()) {while(r.next()) {
                UUID household=UUID.fromString(r.getString(1)),unit=UUID.fromString(r.getString(2));
                var status=occupancy.unit(unit);
                if(status.isEmpty())continue;
                var known=status.get();var entry=registry.find(known.building()).orElseThrow();
                if(registry.hasDonationPermission(known.building())&&known.review().consent().equals(entry.latest().operation())&&known.issues().isEmpty())
                    result.put(household,known.beds());
            }}
        }
        return result;
    }
    private static void bind(PreparedStatement s,UUID world,Position p)throws SQLException {
        s.setString(1,world.toString());s.setInt(2,p.x());s.setInt(3,p.y());s.setInt(4,p.z());
    }
}
