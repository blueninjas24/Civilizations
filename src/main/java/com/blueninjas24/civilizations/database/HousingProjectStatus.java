package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** Bounded read-only inspection of retained household projects and unused-cancellation eligibility. */
public final class HousingProjectStatus {
    public record Item(HousingProjects.Binding family,String plan,boolean siteHeld,long paidOrders,long intents,
                       boolean unused,Optional<String> pause){}
    public record Report(List<Item> items,boolean more){
        public Report{items=List.copyOf(items);}
        public List<String> describe(){
            var lines=new ArrayList<String>();lines.add("Saved home projects: "+items.size()+(more?" shown; more retained":"")+". Inspection only; no funds, site or work changed.");
            for(var item:items){
                lines.add("Project "+item.family().proposal().draft()+"; family "+item.family().proposal().household()+"; plan "+item.plan()+"; "+(item.family().completed()?"completed home "+item.family().completedBuilding():item.family().active()?"active":"cancelled")+".");
                lines.add("Site "+(item.siteHeld()?"reserved":"not held")+"; paid orders "+item.paidOrders()+"; physical intents "+item.intents()+"; unused cancellation "+(item.unused()?"eligible after confirmation":"unavailable")+". "+item.pause().orElse("Normal funding, labor and current-world checks still required."));
            }
            if(more)lines.add("Showing the first 8 retained projects; no history was discarded.");return List.copyOf(lines);
        }
    }
    private final Connection c;
    public HousingProjectStatus(Connection c){this.c=Objects.requireNonNull(c);}
    public Report read(UUID village,DwellingOccupancy.Context context)throws SQLException{
        Objects.requireNonNull(village);Objects.requireNonNull(context);
        if(!c.getAutoCommit())throw new SQLException("Housing status requires an idle SQL owner");c.setAutoCommit(false);
        try{
            try(var s=c.prepareStatement("SELECT 1 FROM settlement_worlds WHERE settlement_id=? AND world_id=?")){
                s.setString(1,village.toString());s.setString(2,context.world().toString());try(var r=s.executeQuery()){if(!r.next())throw new BuildingRegistry.Declined("Housing village does not belong to this world");}
            }
            var ids=new ArrayList<UUID>();
            try(var s=c.prepareStatement("SELECT draft_id FROM housing_project_bindings WHERE settlement_id=? AND world_id=? ORDER BY "+(CivBuildingCompletion.schemaAvailable(c)?"(cancelled_ms IS NOT NULL OR completed_ms IS NOT NULL)":"cancelled_ms IS NOT NULL")+",created_ms DESC,draft_id LIMIT 9")){
                s.setString(1,village.toString());s.setString(2,context.world().toString());try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
            }
            var items=new ArrayList<Item>();
            for(var id:ids.stream().limit(8).toList()){
                items.add(itemRows(id,context));
            }
            c.commit();c.setAutoCommit(true);return new Report(items,ids.size()>8);
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    public Item find(UUID draft,DwellingOccupancy.Context context)throws SQLException{
        Objects.requireNonNull(draft);Objects.requireNonNull(context);
        if(!c.getAutoCommit())throw new SQLException("Project inspection requires an idle SQL owner");c.setAutoCommit(false);
        try{var item=itemRows(draft,context);c.commit();c.setAutoCommit(true);return item;}
        catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private Item itemRows(UUID id,DwellingOccupancy.Context context)throws SQLException{
        var projects=new HousingProjects(c);var family=projects.find(id).orElseThrow(()->new BuildingRegistry.Declined("Unknown household project"));
        if(!family.world().equals(context.world()))throw new BuildingRegistry.Declined("Project belongs to a different world");
        var draft=new ConstructionDraftRepository(c).find(id).orElseThrow();var site=new ConstructionSiteReservations(c).find(id);
        long orders=count("construction_supply_orders",id),intents=count("construction_block_journal",id);
        boolean unused=family.active()&&(orders==0||new ConstructionSupplyRecalls(c).recalled(id)||new ConstructionSupplyReturns(c).dispatched(id))&&intents==0&&context.present()&&returned(id);
        var pause=family.active()?(context.present()?projects.pauseReasonRows(draft):Optional.of("World absent; no new work authorized")):Optional.of(family.completed()?"Completed home history retained; occupancy still requires current inspection":"Saved cancellation retained; cannot reactivate");
        return new Item(family,draft.template().id(),site.isPresent()&&site.orElseThrow().active(),orders,intents,unused,pause);
    }
    private long count(String table,UUID draft)throws SQLException{
        try(var s=c.prepareStatement("SELECT count(*) FROM "+table+" WHERE draft_id=?")){s.setString(1,draft.toString());try(var r=s.executeQuery()){return r.getLong(1);}}
    }
    private boolean returned(UUID draft)throws SQLException{
        try(var s=c.prepareStatement("SELECT operation_id FROM construction_material_reservations WHERE draft_id=? LIMIT 65")){
            s.setString(1,draft.toString());int count=0;try(var r=s.executeQuery()){while(r.next()){
                if(++count>64)throw new BuildingRegistry.Declined("Material receipt review budget exceeded");
                if(!new ConstructionMaterialReturns(c).returned(UUID.fromString(r.getString(1))))return false;
            }}return true;
        }
    }
}
