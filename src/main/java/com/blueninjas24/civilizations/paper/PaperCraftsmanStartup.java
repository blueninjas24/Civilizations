package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.database.WorkplaceOperations;
import com.blueninjas24.civilizations.population.Resident;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.logging.*;

/** Main-thread point confirmation; all funding, eligibility and final evidence checks remain on SQL owner. */
public final class PaperCraftsmanStartup extends BukkitRunnable {
    private final PopulationSession session;
    private final CraftsmanStartupCycle cycle;
    private final Logger logger;
    public PaperCraftsmanStartup(PopulationSession session,Logger logger) {
        this.session=Objects.requireNonNull(session);
        this.logger=Objects.requireNonNull(logger);
        cycle=new CraftsmanStartupCycle(session::reviewAutomaticCraftsmanInvestment,draft-> {
            var anchor=draft.anchor();var p=anchor.position();var world=Bukkit.getWorld(anchor.world());
            if(world==null||!world.isChunkLoaded(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16))
                    ||p.y()<world.getMinHeight()||p.y()>=world.getMaxHeight())return Optional.empty();
            return Optional.of(new WorkplaceOperations.Confirmation(anchor,world.getBlockAt(p.x(),p.y(),p.z()).getType()==Material.CRAFTING_TABLE,System.currentTimeMillis()));
        },session::acceptCraftsmanInvestment);
    }
    public String diagnostic(){return cycle.diagnostic();}
    @Override public void run() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Craftsman startup checks require main thread");
        if(!session.status().ready())return;
        try {
            session.projectionView().ifPresent(view->cycle.tick(view.population().residents().stream()
                    .filter(r->r.status()==Resident.Status.ALIVE).map(Resident::settlementId).distinct().toList()));
        }catch(RuntimeException failure){logger.log(Level.WARNING,"Automatic craftsman startup failed; saved state retained for retry",failure);}
    }
}
