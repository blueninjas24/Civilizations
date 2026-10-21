package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.ConstructionDraft;
import java.sql.*;
import java.util.*;

/** Bounded saved-work handoff. Readiness is never current native permission or completed housing capacity. */
public final class ConstructionWorkQueue {
    public enum State { WAIT_MATERIALS,WAIT_LABOR,READY,RESUME,CONFLICT,PAUSED,COMPLETE }
    public record Work(ConstructionDraft draft,State state,int nextOrdinal,Optional<ConstructionBlockJournal.Intent> pending,String explanation){}
    public record Page(List<Work> projects,UUID next){public Page{projects=List.copyOf(projects);}}
    private final Connection c;
    public ConstructionWorkQueue(Connection c){this.c=Objects.requireNonNull(c);}

    public Page page(UUID after,int limit,Set<UUID> worlds)throws SQLException{
        return page(after,limit,worlds,false);
    }
    /** Automatic physical work is scoped to formal household-bound homes, never arbitrary paid/manual drafts. */
    public Page homePage(UUID after,int limit,Set<UUID> worlds)throws SQLException{
        return page(after,limit,worlds,true);
    }
    private Page page(UUID after,int limit,Set<UUID> worlds,boolean homesOnly)throws SQLException{
        var present=Set.copyOf(worlds);if(limit<1||limit>8||present.size()>256)throw new IllegalArgumentException("Construction work page exceeds bounds");
        if(present.isEmpty())return new Page(List.of(),null);
        if(!c.getAutoCommit())throw new SQLException("Construction work snapshot requires idle SQL owner");c.setAutoCommit(false);
        try{
            var ids=new ArrayList<UUID>();String places=String.join(",",Collections.nCopies(present.size(),"?"));
            try(var s=c.prepareStatement("""
                SELECT d.id FROM construction_drafts d
                JOIN construction_site_reservations p ON p.draft_id=d.id AND p.released_ms IS NULL
                WHERE d.id>? AND d.world_id IN (
                """+places+") AND "+(homesOnly?"EXISTS(SELECT 1 FROM housing_project_bindings h WHERE h.draft_id=d.id AND h.cancelled_ms IS NULL AND h.completed_ms IS NULL)"
                    :"(EXISTS(SELECT 1 FROM housing_project_bindings h WHERE h.draft_id=d.id AND h.cancelled_ms IS NULL) OR EXISTS(SELECT 1 FROM construction_supply_orders o WHERE o.draft_id=d.id AND o.source='VILLAGE'))")
                    +" AND NOT EXISTS(SELECT 1 FROM construction_supply_orders o JOIN construction_supply_recalls r ON r.order_id=o.operation_id WHERE o.draft_id=d.id)"
                    +" AND NOT EXISTS(SELECT 1 FROM construction_supply_orders o JOIN construction_supply_returns r ON r.order_id=o.operation_id WHERE o.draft_id=d.id) ORDER BY d.id LIMIT ?")){
                int n=1;s.setString(n++,after==null?"":after.toString());for(var world:new TreeSet<>(present))s.setString(n++,world.toString());s.setInt(n,limit+1);
                try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
            }
            var work=new ArrayList<Work>();for(var id:ids.stream().limit(limit).toList())work.add(reviewRows(id));
            c.commit();return new Page(work,ids.size()>limit?ids.get(limit-1):null);
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
    Work reviewRows(UUID id)throws SQLException{
        var draft=new ConstructionDraftRepository(c).find(id).orElseThrow();var site=new ConstructionSiteReservations(c).find(id).orElseThrow();
        var pause=new HousingProjects(c).pauseReasonRows(draft);
        if(pause.isPresent())return held(draft,State.PAUSED,0,pause.get());
        if(!new ConstructionLandRepository(c).protectedColumns(draft.world(),site.footprint(),id).isEmpty())return held(draft,State.PAUSED,0,"Saved land protection changed; no new native authority");
        int applied=0,total=0;UUID pending=null;ConstructionBlockJournal.State pendingState=null;
        try(var s=c.prepareStatement("SELECT operation_id,ordinal,state FROM construction_block_journal WHERE draft_id=? ORDER BY ordinal LIMIT 16385")){
            s.setString(1,id.toString());try(var r=s.executeQuery()){while(r.next()){
                if(total>=draft.template().blockCount()||r.getInt("ordinal")!=total||pending!=null)throw new SQLException("Construction journal has a gap or work beyond its unresolved intent");
                total++;var state=ConstructionBlockJournal.State.valueOf(r.getString("state"));
                if(state==ConstructionBlockJournal.State.APPLIED)applied++;else{pending=UUID.fromString(r.getString("operation_id"));pendingState=state;}
            }}
        }
        var labor=new ConstructionPublicWork(c);
        if(applied==draft.template().blockCount()){
            int offset=0;for(var stage:draft.template().stages()){labor.requireReadyRows(draft,offset);offset+=stage.blocks().size();}
            return held(draft,State.COMPLETE,applied,"All native intents acknowledged with paid materials/earned labor; current final inspection and activation still required");
        }
        Optional<ConstructionBlockJournal.Intent> intent=pending==null?Optional.empty():new ConstructionBlockJournal(c).find(pending);
        if(pendingState==ConstructionBlockJournal.State.CONFLICT)return new Work(draft,State.CONFLICT,applied,intent,"Saved native conflict requires explicit review; no overwrite authorized");
        int stage=0,offset=0;while(applied>=offset+draft.template().stages().get(stage).blocks().size())offset+=draft.template().stages().get(stage++).blocks().size();
        var terms=draft.template().stages().get(stage);long earned=labor.work(id,stage);var stock=ConstructionSupplyOrders.deliveryOnly(c).stock(id);
        if(earned>terms.work())throw new SQLException("Saved construction work exceeds frozen stage");
        if(earned<terms.work()){
            if(pending!=null)throw new SQLException("Prepared native intent lacks its required paid stage work");
            boolean materials=earned>0||terms.materials().entrySet().stream().allMatch(e->stock.getOrDefault(e.getKey(),0L)>=e.getValue());
            return held(draft,materials?State.WAIT_LABOR:State.WAIT_MATERIALS,applied,materials?"Delivered stage inputs held; waiting for actual eligible construction effort":"Waiting for required actual arrived stage inputs; no free materials");
        }
        labor.requireReadyRows(draft,applied);
        return new Work(draft,pending==null?State.READY:State.RESUME,applied,intent,pending==null?"Paid stage work ready; fresh loaded-world/protection review and durable native intent still required":"Resume this exact durable native intent; first compare current native state, never invent replacement work");
    }
    private static Work held(ConstructionDraft draft,State state,int ordinal,String reason){return new Work(draft,state,ordinal,Optional.empty(),reason);}
}
