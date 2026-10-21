package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.HousingProjectPlanner.*;
import com.blueninjas24.civilizations.population.Household;
import java.sql.*;
import java.util.*;

/** Read-only SQL-owner bridge from saved families/permitted vacancies to candidate housing decisions. */
public final class HousingPlanning {
    public record Report(Plan plan,int unknownHouseholds,int unassignedResidents,boolean vacanciesVerified,boolean pendingHome) {
        public List<String> describe(){
            var lines=new ArrayList<String>();
            lines.add("Housing candidates only: no projects, assignments, spending, land rights or blocks created.");
            lines.add("Unknown family capacity: "+unknownHouseholds+"; residents without a saved household: "+unassignedResidents
                    +". These are held for review, never treated as zero housing.");
            lines.add("Permitted vacancy evidence "+(vacanciesVerified?"current":"incomplete or stale")
                    +"; active home project "+(pendingHome?"present (beneficiary mapping still pending)":"absent")+".");
            for(var r:plan.recommendations().stream().limit(16).toList())lines.add("Family "+r.household()+": "+r.action()
                    +(r.vacantHome()==null?"":" unit "+r.vacantHome())+(r.templateId()==null?"":" plan "+r.templateId())+". "+r.explanation());
            if(plan.recommendations().size()>16)lines.add("Showing16 of "+plan.recommendations().size()+" family recommendations.");
            return List.copyOf(lines);
        }
    }
    private final Connection c;
    public HousingPlanning(Connection c){this.c=Objects.requireNonNull(c);}
    public Report review(UUID village,DwellingOccupancy.Context context,List<ConstructionTemplate> templates,
                         HousingProjectPlanner planner,long seed,long period)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM settlement_worlds WHERE settlement_id=? AND world_id=?")){
            s.setString(1,village.toString());s.setString(2,context.world().toString());
            try(var r=s.executeQuery()){if(!r.next())throw new BuildingRegistry.Declined("Housing village does not belong to this world");}
        }
        var capacities=HousingCapacity.read(c,village);var needs=new ArrayList<Need>();int unknown=0,households=0;
        try(var s=c.prepareStatement("""
                SELECT h.id,h.name,count(r.id) FROM households h LEFT JOIN residents r
                ON r.household_id=h.id AND r.status='ALIVE' WHERE h.settlement_id=?
                GROUP BY h.id,h.name ORDER BY h.id LIMIT 1025
                """)){
            s.setString(1,village.toString());try(var r=s.executeQuery()){while(r.next()){
                if(++households>1024)throw new BuildingRegistry.Declined("Housing planning household budget exceeded");
                UUID id=UUID.fromString(r.getString(1));int living=r.getInt(3);
                if(living>0&&!capacities.containsKey(id)){unknown++;continue;}
                needs.add(new Need(new Household(id,village,r.getString(2)),living,capacities.getOrDefault(id,0)>=living));
            }}
        }
        int unassigned;
        try(var s=c.prepareStatement("SELECT count(*) FROM residents WHERE settlement_id=? AND status='ALIVE' AND household_id IS NULL")){
            s.setString(1,village.toString());try(var r=s.executeQuery()){unassigned=r.getInt(1);}
        }
        var vacancies=new ArrayList<Vacancy>();boolean complete=context.present();
        if(context.present()){
            var buildings=new ArrayList<UUID>();
            try(var s=c.prepareStatement("SELECT id FROM building_records WHERE settlement_id=? AND world_id=? ORDER BY id LIMIT 129")){
                s.setString(1,village.toString());s.setString(2,context.world().toString());try(var r=s.executeQuery()){while(r.next())buildings.add(UUID.fromString(r.getString(1)));}
            }
            if(buildings.size()>128)throw new BuildingRegistry.Declined("Housing planning building budget exceeded");
            var registry=new BuildingRegistry(c);var occupancy=new DwellingOccupancy(c);
            for(var id:buildings){
                var entry=registry.find(id).orElseThrow();
                if(entry.layout().kind()!=BuildingLayout.Kind.HOUSING||!registry.hasDonationPermission(id))continue;
                var available=occupancy.vacancies(id,context);complete&=available.verified();
                for(var unit:available.units()){
                    if(vacancies.size()>=1024)throw new BuildingRegistry.Declined("Housing planning vacancy budget exceeded");
                    vacancies.add(new Vacancy(unit.unit(),village,context.world(),unit.beds(),true,true,false));
                }
            }
        }
        // Existing draft schema has no household-beneficiary binding. Never claim that a second home is needed
        // while any retained active home project might already serve that family. Reuse/starting stay separate.
        boolean pending=false;var drafts=new ArrayList<UUID>();
        try(var s=c.prepareStatement("""
                SELECT d.id FROM construction_drafts d JOIN construction_site_reservations r ON r.draft_id=d.id
                WHERE d.settlement_id=? AND d.world_id=? AND r.released_ms IS NULL ORDER BY d.id LIMIT 65
                """)){
            s.setString(1,village.toString());s.setString(2,context.world().toString());try(var r=s.executeQuery()){while(r.next())drafts.add(UUID.fromString(r.getString(1)));}
        }
        if(drafts.size()>64)throw new BuildingRegistry.Declined("Housing planning project budget exceeded");
        var repository=new ConstructionDraftRepository(c);
        for(var id:drafts)if(repository.find(id).orElseThrow().template().purpose()==ConstructionTemplate.Purpose.HOME)pending=true;
        Set<UUID> waiting=pending?needs.stream().map(n->n.household().id()).collect(java.util.stream.Collectors.toSet()):Set.of();
        return new Report(planner.review(village,context.world(),needs,vacancies,templates,waiting,complete,context.present(),seed,period),unknown,unassigned,complete,pending);
    }
}
