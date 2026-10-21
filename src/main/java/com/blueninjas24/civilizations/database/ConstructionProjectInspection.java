package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;

/** Read-only saved-state report; never guesses current native blocks or grants repair permission. */
public final class ConstructionProjectInspection {
    private final Connection c;
    public ConstructionProjectInspection(Connection c){this.c=Objects.requireNonNull(c);}
    public List<String> inspect(UUID id,UUID world,boolean present)throws SQLException{
        if(!c.getAutoCommit())throw new SQLException("Project inspection requires idle SQL owner");c.setAutoCommit(false);
        try{
            var draft=new ConstructionDraftRepository(c).find(id).orElseThrow(()->new BuildingRegistry.Declined("Unknown construction project"));
            if(!draft.world().equals(world))throw new BuildingRegistry.Declined("Project belongs to another world");
            var lines=new ArrayList<String>();var at=draft.origin();
            lines.add("Project "+id+"; village "+draft.settlement()+"; plan "+draft.template().id()+" ("+draft.template().purpose()+").");
            lines.add("Origin "+at.x()+" "+at.y()+" "+at.z()+"; "+draft.transform().rotation()+", mirrored "+draft.transform().mirror()+"; world "+world+".");
            var site=new ConstructionSiteReservations(c).find(id);var complete=new CivBuildingCompletion(c).findDraft(id);
            if(complete.isPresent())lines.add("Completed village home "+complete.get().building()+"; housing still uses actual inspections.");
            else if(!present)lines.add("World absent; saved history only, no current native authority.");
            else if(site.isEmpty()||!site.get().active())lines.add("No active site reservation; this report grants no work authority.");
            else{
                var work=new ConstructionWorkQueue(c).reviewRows(id);
                lines.add("Saved work: "+work.state()+"; acknowledged "+work.nextOrdinal()+"/"+draft.template().blockCount()+" blocks. "+work.explanation());
            }
            try(var s=c.prepareStatement("SELECT operation_id FROM construction_block_journal WHERE draft_id=? AND state<>'APPLIED' ORDER BY ordinal LIMIT 2")){
                s.setString(1,id.toString());try(var r=s.executeQuery()){
                    if(r.next()){
                        var intent=new ConstructionBlockJournal(c).find(UUID.fromString(r.getString(1))).orElseThrow();var p=intent.at();
                        lines.add("Unresolved intent "+intent.operation()+"; block "+(intent.ordinal()+1)+"; "+intent.state()+" at "+p.x()+" "+p.y()+" "+p.z()+".");
                        lines.add("Saved before: "+intent.before());lines.add("Saved after: "+intent.after());
                        if(r.next())throw new SQLException("Multiple unresolved native intents require saved-history review");
                    }else lines.add("No unresolved native intent. Any new block still needs current world and payment checks.");
                }
            }
            lines.add("Saved evidence only: no chunk read/load, repair, replacement, refund or block change. Use /civ debug construction for the latest native hold.");
            c.commit();return List.copyOf(lines);
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
}
