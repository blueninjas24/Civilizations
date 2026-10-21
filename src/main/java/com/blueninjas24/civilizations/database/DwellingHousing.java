package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import java.sql.*;
import java.util.*;

/** Bounded reconciliation of reviewed homes, preserving whole families and incumbent legacy occupants. */
public final class DwellingHousing {
    public record Report(DwellingOccupancy.Outcome review,int assigned,int released,String explanation) {}
    private final Connection connection;
    public DwellingHousing(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public Report reconcile(BuildingLayout layout,DwellingInspection.Assessment assessment,DwellingOccupancy.Context context)throws SQLException {
        var repository=new DwellingOccupancy(connection);
        var outcome=repository.review(UUID.randomUUID(),layout.id(),assessment,context);
        if(outcome==DwellingOccupancy.Outcome.UNVERIFIED)return new Report(outcome,0,0,"Incomplete housing evidence; previous state retained");
        int released=0,assigned=0;
        var living=new TreeMap<UUID,Integer>();
        try(var s=connection.prepareStatement("SELECT household_id,count(*) FROM residents WHERE settlement_id=? AND status='ALIVE' AND household_id IS NOT NULL GROUP BY household_id ORDER BY household_id")) {
            s.setString(1,layout.settlement().toString());try(var r=s.executeQuery()){while(r.next())living.put(UUID.fromString(r.getString(1)),r.getInt(2));}
        }
        for(var unit:layout.units()) {
            var active=repository.forUnit(unit.id());
            if(active.isPresent()&&!living.containsKey(active.get().household())) {
                repository.releaseEmpty(UUID.randomUUID(),active.get().operation(),context);released++;
            }
        }
        var vacancies=repository.vacancies(layout.id(),context);
        if(!vacancies.verified())return new Report(outcome,0,released,vacancies.explanation());
        var capacities=HousingCapacity.read(connection,layout.settlement());
        String explanation="No suitable whole household currently needs these vacancies";
        for(var status:vacancies.units()) {
            if(assigned>=4)break;
            var declared=layout.units().stream().filter(u->u.id().equals(status.unit())).findFirst().orElseThrow();
            var incumbents=new TreeSet<UUID>();
            for(var bed:declared.beds()) {
                try(var s=connection.prepareStatement("SELECT household_id FROM village_beds WHERE world_id=? AND x=? AND y=? AND z=? AND household_id IS NOT NULL")) {
                    s.setString(1,layout.world().toString());s.setInt(2,bed.head().x());s.setInt(3,bed.head().y());s.setInt(4,bed.head().z());
                    try(var r=s.executeQuery()){if(r.next()){UUID id=UUID.fromString(r.getString(1));if(living.containsKey(id)&&!HousingCapacity.managed(connection,id))incumbents.add(id);}}
                }
            }
            UUID selected=null;
            if(incumbents.size()>1) {explanation="Legacy families share this declared unit; no family split or displacement";continue;}
            if(incumbents.size()==1) {
                UUID incumbent=incumbents.first();
                if(living.get(incumbent)<=status.beds())selected=incumbent;
                else {explanation="Existing whole family does not fit; legacy housing retained";continue;}
            } else {
                selected=living.keySet().stream().filter(id->capacities.containsKey(id)&&capacities.get(id)<living.get(id)&&living.get(id)<=status.beds())
                        .min(Comparator.<UUID>comparingInt(id->capacities.get(id)).thenComparing(Comparator.naturalOrder())).orElse(null);
            }
            if(selected==null)continue;
            var previous=repository.forHousehold(selected).map(DwellingOccupancy.Assignment::operation).orElse(null);
            repository.assign(new DwellingOccupancy.Assignment(UUID.randomUUID(),selected,status.unit(),status.review().operation(),previous,context.now()),context);
            capacities.put(selected,status.beds());assigned++;explanation="Whole families assigned to inspected permitted homes; identity preserved";
        }
        return new Report(outcome,assigned,released,explanation);
    }
}
