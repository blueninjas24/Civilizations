package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.database.ResidentProjectionRepository.Home;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Positive saved housing authority only. No native reads, assignment changes or capacity creation. */
final class ResidentProjectionHomes {
    record View(Map<UUID,Home> homes,Set<UUID> managed){}
    static View read(Connection c,UUID only)throws SQLException {
        var homes=new HashMap<UUID,Home>();var managed=new HashSet<UUID>();
        boolean modern=table(c,"dwelling_assignments"),claims=table(c,"dwelling_bed_claims");
        if(modern)try(var s=c.prepareStatement("SELECT DISTINCT household_id FROM dwelling_assignments WHERE (? IS NULL OR household_id=?)")){
            filter(s,only);try(var r=s.executeQuery()){while(r.next())managed.add(UUID.fromString(r.getString(1)));}
        }
        String exclusions=claims?" AND NOT EXISTS (SELECT 1 FROM dwelling_bed_claims d WHERE d.world_id=b.world_id AND d.x=b.x AND d.y=b.y AND d.z=b.z)":"";
        if(table(c,"construction_block_journal"))exclusions+=" AND NOT EXISTS (SELECT 1 FROM construction_block_journal j JOIN construction_drafts d ON d.id=j.draft_id WHERE d.world_id=b.world_id AND j.world_x=b.x AND j.world_y=b.y AND j.world_z=b.z AND instr(j.after_state,'_bed')>0)";
        try(var s=c.prepareStatement("SELECT b.* FROM village_beds b JOIN households h ON h.id=b.household_id JOIN settlement_worlds w ON w.settlement_id=h.settlement_id AND w.world_id=b.world_id WHERE (? IS NULL OR h.id=?)"+exclusions+" ORDER BY b.world_id,b.x,b.y,b.z")){
            filter(s,only);try(var r=s.executeQuery()){while(r.next()){
                UUID family=UUID.fromString(r.getString("household_id"));if(managed.contains(family))continue;
                homes.putIfAbsent(family,new Home(UUID.fromString(r.getString("world_id")),new Position(r.getInt("x"),r.getInt("y"),r.getInt("z"))));
            }}
        }
        if(modern)try(var s=c.prepareStatement("SELECT o.household_id,o.unit_id,h.settlement_id,w.world_id FROM dwelling_active_occupancy o JOIN households h ON h.id=o.household_id JOIN settlement_worlds w ON w.settlement_id=h.settlement_id WHERE (? IS NULL OR h.id=?) ORDER BY h.id")){
            filter(s,only);try(var r=s.executeQuery()){while(r.next()){
                UUID family=UUID.fromString(r.getString(1)),unit=UUID.fromString(r.getString(2));
                var status=new DwellingOccupancy(c).unit(unit);if(status.isEmpty())continue;
                var known=status.get();var registry=new BuildingRegistry(c);var entry=registry.find(known.building()).orElseThrow();var layout=entry.layout();
                if(!layout.world().toString().equals(r.getString(4))||!layout.settlement().toString().equals(r.getString(3))
                        ||!registry.hasDonationPermission(known.building())||!known.review().consent().equals(entry.latest().operation())||!known.issues().isEmpty())continue;
                var declared=layout.units().stream().filter(u->u.id().equals(unit)).findFirst().orElseThrow();
                if(known.beds()!=declared.beds().size()||known.beds()==0)continue;
                var people=new ArrayList<UUID>();
                try(var q=c.prepareStatement("SELECT id FROM residents WHERE household_id=? AND settlement_id=? AND status='ALIVE' ORDER BY id")){
                    q.setString(1,family.toString());q.setString(2,r.getString(3));try(var rows=q.executeQuery()){while(rows.next())people.add(UUID.fromString(rows.getString(1)));}
                }
                if(people.isEmpty()||people.size()>known.beds())continue;
                var beds=declared.beds().stream().map(b->b.head()).sorted(Comparator.comparingInt(com.blueninjas24.civilizations.construction.ConstructionTemplate.Position::x).thenComparingInt(com.blueninjas24.civilizations.construction.ConstructionTemplate.Position::y).thenComparingInt(com.blueninjas24.civilizations.construction.ConstructionTemplate.Position::z)).toList();
                var mapped=new LinkedHashMap<UUID,Position>();for(int i=0;i<people.size();i++){var p=beds.get(i);mapped.put(people.get(i),new Position(p.x(),p.y(),p.z()));}
                homes.put(family,new Home(layout.world(),mapped.get(people.getFirst()),mapped));
            }}
        }
        return new View(Map.copyOf(homes),Set.copyOf(managed));
    }
    private static boolean table(Connection c,String name)throws SQLException{try(var s=c.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")){s.setString(1,name);try(var r=s.executeQuery()){return r.next();}}}
    private static void filter(PreparedStatement s,UUID only)throws SQLException{String value=only==null?null:only.toString();s.setString(1,value);s.setString(2,value);}
}
