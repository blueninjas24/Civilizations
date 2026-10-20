package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.LivestockLosses;
import org.bukkit.entity.EntityType;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

/** Main-thread confirmed deaths, immutable SQL-owner handoff and graceful draining.
 * Ordinary loot stays native; exact intentional deaths delegate to their controlled retirement adapter. */
public final class ManagedLivestockDeaths extends BukkitRunnable implements Listener {
    interface Owner {
        Set<UUID> villageAnimals();boolean ready();
        CompletableFuture<Optional<LivestockLosses.Receipt>> record(LivestockLosses.Observation observation);
        CompletableFuture<Void> flush(List<LivestockLosses.Observation> observations);
        void gap();
        void captured(LivestockLosses.Observation observation);
    }
    private final Owner owner;private final Logger logger;private final LongSupplier clock;
    private final Map<UUID,LivestockLosses.Observation> deaths=new LinkedHashMap<>(),recent=new LinkedHashMap<>();
    private LivestockLosses.Observation submitted;private CompletableFuture<?> pending;private long retryAt;
    private java.util.function.Predicate<EntityDeathEvent> controlled=event->false;
    public ManagedLivestockDeaths(PopulationSession session,Logger logger,java.util.function.Predicate<EntityDeathEvent> controlled) {
        this(session,logger);this.controlled=Objects.requireNonNull(controlled);
    }
    public ManagedLivestockDeaths(PopulationSession session,Logger logger) {
        this(new Owner(){public Set<UUID> villageAnimals(){return session.villageAnimals();}public boolean ready(){return session.status().ready();}
            public CompletableFuture<Optional<LivestockLosses.Receipt>> record(LivestockLosses.Observation o){return session.recordLivestockLoss(o);}
            public CompletableFuture<Void> flush(List<LivestockLosses.Observation> o){return session.flushLivestockLosses(o);}public void gap(){session.markLivestockCaptureGap();}public void captured(LivestockLosses.Observation o){session.stageLivestockLoss(o);}},logger,System::currentTimeMillis);
    }
    ManagedLivestockDeaths(Owner owner,Logger logger,LongSupplier clock){this.owner=Objects.requireNonNull(owner);this.logger=Objects.requireNonNull(logger);this.clock=Objects.requireNonNull(clock);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void died(EntityDeathEvent event) {
        if(event.isCancelled()||controlled.test(event))return;var entity=event.getEntity();var type=entity.getType();
        if(PaperLivestockSpecies.from(type).isEmpty())return;
        var animal=entity.getUniqueId();var id=UUID.nameUUIDFromBytes(("civilizations:physical-livestock-loss-v1:"+animal).getBytes(StandardCharsets.UTF_8));
        String reason=event.getDamageSource().getDamageType().getKey().toString();
        var observed=new LivestockLosses.Observation(id,animal,entity.getWorld().getUID(),clock.getAsLong(),reason.length()>256?reason.substring(0,256):reason);
        if(owner.villageAnimals().contains(animal)){if(deaths.putIfAbsent(animal,observed)==null)owner.captured(observed);}
        else {if(recent.size()>=256&&!recent.containsKey(animal)){if(!owner.ready())owner.gap();recent.remove(recent.keySet().iterator().next());}recent.putIfAbsent(animal,observed);}
        run();
    }
    /** Exact intentional-death evidence enters the same retry/shutdown queue as ordinary native deaths. */
    public void captureControlled(LivestockLosses.Observation observation) {
        if(deaths.putIfAbsent(observation.animal(),observation)==null)owner.captured(observation);run();
    }
    @Override public void run() {
        var ids=owner.villageAnimals();long now=clock.getAsLong();
        recent.entrySet().removeIf(e->{if(ids.contains(e.getKey())){if(deaths.putIfAbsent(e.getKey(),e.getValue())==null)owner.captured(e.getValue());return true;}boolean expired=now-e.getValue().observedMillis()>30000;if(expired&&!owner.ready())owner.gap();return expired;});
        if(pending!=null) {
            if(!pending.isDone())return;
            try{pending.join();deaths.remove(submitted.animal());}
            catch(java.util.concurrent.CompletionException failed){logger.log(java.util.logging.Level.WARNING,"Physical livestock loss save deferred; exact observation retained for retry",failed.getCause());retryAt=now+1000;}
            pending=null;submitted=null;
        }
        if(!owner.ready()||deaths.isEmpty()||now<retryAt)return;
        submitted=deaths.values().iterator().next();pending=owner.record(submitted);
    }
    public String diagnostic(){return "Physical livestock losses pending: "+deaths.size()+"; recent unbound: "+recent.size();}
    public void flush() {
        if(deaths.isEmpty()&&recent.isEmpty())return;
        var observations=new ArrayList<>(deaths.values());observations.addAll(recent.values());
        owner.flush(List.copyOf(observations)).whenComplete((ignored,failure)->{if(failure!=null)logger.log(java.util.logging.Level.SEVERE,"Physical livestock loss shutdown save failed",failure);});
    }
}
