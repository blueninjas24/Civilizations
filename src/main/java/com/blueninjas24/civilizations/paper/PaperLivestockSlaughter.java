package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** Bounded exact-body dispatch. Saved permits precede native effects; only confirmed deaths earn meat. */
public final class PaperLivestockSlaughter extends BukkitRunnable implements AutoCloseable {
    private final PopulationSession session;private final LivestockSlaughterPolicy policy;private final LivestockWorkingArea area;
    private final Logger logger;private final LongSupplier clock;
    private Consumer<LivestockLosses.Observation> deathSink;
    private UUID cursor;private long nextReview,ticks,confirmed;private String failure="none";private boolean closed;
    private CompletableFuture<List<LivestockPresence.Member>> page;
    private final ArrayDeque<LivestockPresence.Member> candidates=new ArrayDeque<>();
    private CompletableFuture<Optional<LivestockSlaughter.Permit>> pending;
    private LivestockPresence.Member selected;
    private LivestockSlaughter.Permit executing;private EntityDeathEvent captured;
    public PaperLivestockSlaughter(PopulationSession session,LivestockSlaughterPolicy policy,LivestockWorkingArea area,Logger logger) {
        this(session,policy,area,logger,System::currentTimeMillis);
    }
    public PaperLivestockSlaughter(PopulationSession session,LivestockSlaughterPolicy policy,LivestockWorkingArea area,Logger logger,LongSupplier clock) {
        this.session=Objects.requireNonNull(session);this.policy=Objects.requireNonNull(policy);this.area=Objects.requireNonNull(area);this.logger=Objects.requireNonNull(logger);this.clock=Objects.requireNonNull(clock);
    }
    public void setDeathSink(Consumer<LivestockLosses.Observation> sink){deathSink=Objects.requireNonNull(sink);}
    /** Called by the registered death listener while our exact synchronous retirement is executing. */
    public boolean observeDeath(EntityDeathEvent event) {
        if(executing==null||!executing.animal().equals(event.getEntity().getUniqueId()))return false;
        if(event.isCancelled())return false;
        captured=event;event.getDrops().clear();event.setDroppedExp(0);return true;
    }
    private Optional<LivestockPresence.Sight> sight(LivestockPresence.Member member) {
        var entity=Bukkit.getEntity(member.animal());
        if(!(entity instanceof Animals body)||!body.isValid()||body.isDead()||!body.isAdult()||body.isLeashed()||body.customName()!=null)return Optional.empty();
        if((member.species()==LivestockHerd.Species.COW&&body.getType()!=EntityType.COW)
                ||(member.species()==LivestockHerd.Species.CHICKEN&&body.getType()!=EntityType.CHICKEN))return Optional.empty();
        var loc=body.getLocation();var world=body.getWorld();var p=new Position(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ());
        if(!world.isChunkLoaded(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16))||!area.contains(member.world(),member.home(),world.getUID(),p))return Optional.empty();
        return Optional.of(new LivestockPresence.Sight(member.animal(),member.species(),world.getUID(),p,clock.getAsLong()));
    }
    @Override public void run() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Physical livestock retirement requires Paper main thread");
        if(closed||!policy.enabled()||deathSink==null)return;ticks++;
        try {
            if(pending!=null) {
                if(!pending.isDone())return;
                var permit=pending.join();pending=null;
                if(permit.isPresent())execute(permit.orElseThrow(),selected);
                selected=null;return;
            }
            if(!session.livestockCaptureReady())return;
            if(page!=null) {
                if(!page.isDone())return;var members=page.join();page=null;
                if(members.isEmpty())cursor=null;else{cursor=members.getLast().animal();candidates.addAll(members);}
            }
            while(!candidates.isEmpty()) {
                var member=candidates.removeFirst();if(member.maturePeriod()>member.herdPeriod())continue;
                var sight=sight(member);if(sight.isEmpty())continue;
                selected=member;pending=session.prepareLivestockSlaughter(sight.orElseThrow());return;
            }
            if(ticks>=nextReview){nextReview=ticks+policy.intervalTicks();page=session.livestockMembers(cursor,policy.candidatesPerReview());}
        }catch(CompletionException failed){failure=failed.getCause().toString();logger.log(Level.WARNING,"Slaughter preparation deferred; no physical effect dispatched",failed.getCause());pending=null;page=null;selected=null;}
    }
    private void execute(LivestockSlaughter.Permit permit,LivestockPresence.Member member) {
        long now=clock.getAsLong();
        if(!session.livestockCaptureReady()||now<permit.preparedMillis()||now-permit.preparedMillis()>policy.evidenceMaxAgeMillis()||sight(member).isEmpty()) {
            cancel(permit,"No fresh eligible loaded body at dispatch; no native slaughter performed");return;
        }
        var entity=(Animals)Bukkit.getEntity(permit.animal());executing=permit;captured=null;
        try{entity.setHealth(0);}catch(RuntimeException error){failure=error.toString();logger.log(Level.WARNING,"Native slaughter interrupted; checking exact death evidence",error);}
        finally {
            var event=captured;executing=null;captured=null;
            if(event!=null&&!event.isCancelled()&&entity.isDead()) {
                var operation=UUID.nameUUIDFromBytes(("civilizations:physical-livestock-loss-v1:"+permit.animal()).getBytes(StandardCharsets.UTF_8));
                deathSink.accept(new LivestockLosses.Observation(operation,permit.animal(),permit.world(),clock.getAsLong(),LivestockSlaughter.REASON_PREFIX+permit.id()));confirmed++;
            } else if(entity.isValid()&&!entity.isDead())cancel(permit,"Native death cancelled; living body retained, no products credited");
            else {session.markLivestockCaptureGap();failure="Native body disappeared without a confirmed death; saved permit retained for reviewed recovery";logger.severe(failure);}
        }
    }
    private void cancel(LivestockSlaughter.Permit permit,String reason){session.cancelLivestockSlaughter(permit.id(),reason).whenComplete((ignored,error)->{if(error!=null)logger.log(Level.SEVERE,"Slaughter cancellation save failed; permit retained",error);});}
    public String diagnostic(){return "Controlled livestock slaughter: "+(policy.enabled()?"configured":"disabled")+"; confirmed bodies="+confirmed+"; last failure="+failure;}
    @Override public void close(){closed=true;candidates.clear();}
}
