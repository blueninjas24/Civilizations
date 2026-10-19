package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/** Positive new-generation events only. Missing evidence stays unknown, never backfilled. */
public final class GeneratedTerrainObserver implements Listener {
    private static final int MAX_PENDING=8;
    private final PopulationSession session;
    private final Logger logger;
    private final AtomicInteger pending=new AtomicInteger();
    private final AtomicLong saved=new AtomicLong(),unknown=new AtomicLong();

    public GeneratedTerrainObserver(PopulationSession session,Logger logger) {
        this.session=Objects.requireNonNull(session);this.logger=Objects.requireNonNull(logger);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onLoad(ChunkLoadEvent event) {
        if(!event.isNewChunk())return;
        if(!Bukkit.isPrimaryThread()||(!session.oreCaptureReady()&&!session.forestCaptureReady())) {
            unknown.incrementAndGet();return;
        }
        if(pending.incrementAndGet()>MAX_PENDING){pending.decrementAndGet();unknown.incrementAndGet();return;}
        try {
            var chunk=event.getChunk();var world=event.getWorld();
            // Copy all Paper evidence now; only immutable IDs, text and numbers cross the queue.
            session.recordGeneratedTerrain(world.getUID(),world.getName(),chunk.getX(),chunk.getZ(),System.currentTimeMillis())
                    .whenComplete((count,failure)->{
                        pending.decrementAndGet();
                        if(failure!=null){if(unknown.getAndIncrement()==0)logger.warning("New terrain evidence was not saved; affected chunks remain unknown");}
                        else if(count==0)unknown.incrementAndGet();else saved.incrementAndGet();
                    });
        } catch(RuntimeException failure) {
            pending.decrementAndGet();unknown.incrementAndGet();
            logger.warning("New terrain evidence unavailable; chunk remains unknown: "+failure.getClass().getSimpleName());
        }
    }

    public String diagnostic() {
        return "New terrain evidence: "+saved.get()+" chunks saved this run, "+pending.get()+" pending, "+unknown.get()+" left unknown";
    }
}
