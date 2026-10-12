package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.WorkplaceOperations;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.*;

/** At most 64 fixed-anchor loaded block reads per pass. SQL futures never inspect Paper. */
public final class PaperWorkplaceOperations extends BukkitRunnable {
    private final PopulationSession session;
    private final Logger logger;
    private CompletableFuture<List<WorkplaceOperations.Anchor>> page;
    private CompletableFuture<Void> save;
    private UUID cursor;
    private String diagnostic="No marker-founded businesses checked";
    public PaperWorkplaceOperations(PopulationSession session,Logger logger){this.session=Objects.requireNonNull(session);this.logger=Objects.requireNonNull(logger);}
    public String diagnostic(){return diagnostic;}
    @Override public void run() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Marker checks require main thread");
        if(!session.status().ready())return;
        try {
            if(save!=null){if(!save.isDone())return;save.join();save=null;}
            if(page==null){page=session.workplaceOperationAnchors(cursor);return;}
            if(!page.isDone())return;
            var anchors=page.join();page=null;
            cursor=anchors.size()==64?anchors.getLast().business():null;
            var evidence=new ArrayList<WorkplaceOperations.Confirmation>();
            long captured=System.currentTimeMillis();
            for(var anchor:anchors) {
                var world=Bukkit.getWorld(anchor.world());var p=anchor.position();
                if(world==null||!world.isChunkLoaded(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16)))continue;
                if(p.y()<world.getMinHeight()||p.y()>=world.getMaxHeight())continue;
                Material required=switch(anchor.kind()){case BARREL->Material.BARREL;case SMITHING_TABLE->Material.SMITHING_TABLE;};
                evidence.add(new WorkplaceOperations.Confirmation(anchor,world.getBlockAt(p.x(),p.y(),p.z()).getType()==required,captured));
            }
            if(!evidence.isEmpty())save=session.confirmWorkplaceOperations(evidence);
            diagnostic="Workplace operation checks: "+evidence.size()+" loaded anchors; unknown chunks unchanged";
        }catch(RuntimeException failure) {
            page=null;save=null;diagnostic="Workplace operation checks deferred: "+failure.getMessage();
            logger.log(Level.WARNING,"Workplace operation confirmation failed; retrying without resetting businesses",failure);
        }
    }
}
