package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.LivestockPresence.*;
import com.blueninjas24.civilizations.industry.LivestockHerd;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Animals;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** Bounded positive sightings and saved ages of known herd UUIDs. No scans, chunk loads, movement or adoption. */
public final class PaperLivestockPresence extends BukkitRunnable {
    interface Owner {boolean ready();CompletableFuture<List<Member>> page(UUID after,int limit);CompletableFuture<Integer> record(List<Sight> sights);}
    private final Owner owner;private final Function<Member,Optional<Sight>> capture;private final BooleanSupplier mainThread;
    private final Logger logger;private final LongSupplier clock;
    private UUID cursor;private CompletableFuture<List<Member>> page;private CompletableFuture<Integer> save;
    private List<Sight> retained=List.of();private long retryAt,observed;
    private BiConsumer<Member,Sight> reconcile=(member,sight)->{};private String ageFailure;
    public PaperLivestockPresence(PopulationSession session,Logger logger) {
        this(new Owner(){public boolean ready(){return session.status().ready();}
            public CompletableFuture<List<Member>> page(UUID after,int limit){return session.livestockMembers(after,limit);}
            public CompletableFuture<Integer> record(List<Sight> sights){return session.recordLivestockPresence(sights);}},
            PaperLivestockPresence::capture,Bukkit::isPrimaryThread,logger,System::currentTimeMillis);
        reconcile=(member,sight)-> {
            var entity=Bukkit.getEntity(member.animal());
            if(entity instanceof Animals animal&&animal.isValid()&&!animal.isDead()) {
                var expected=PaperLivestockSpecies.type(member.species());
                if(animal.getType()!=expected)throw new IllegalStateException("Saved livestock age species differs from body");
                LivestockAgeReconciler.apply(animal,member.business(),member.herdPeriod(),member.maturePeriod());
            }
        };
    }
    PaperLivestockPresence(Owner owner,Function<Member,Optional<Sight>> capture,BooleanSupplier mainThread,Logger logger,LongSupplier clock) {
        this.owner=Objects.requireNonNull(owner);this.capture=Objects.requireNonNull(capture);this.mainThread=Objects.requireNonNull(mainThread);this.logger=Objects.requireNonNull(logger);this.clock=Objects.requireNonNull(clock);
    }
    PaperLivestockPresence(Owner owner,Function<Member,Optional<Sight>> capture,BooleanSupplier mainThread,Logger logger,LongSupplier clock,BiConsumer<Member,Sight> reconcile) {
        this(owner,capture,mainThread,logger,clock);this.reconcile=Objects.requireNonNull(reconcile);
    }
    private static Optional<Sight> capture(Member member) {
        var entity=Bukkit.getEntity(member.animal());if(entity==null||!entity.isValid()||entity.isDead())return Optional.empty();
        var type=entity.getType();if(PaperLivestockSpecies.from(type).isEmpty())return Optional.empty();
        var location=entity.getLocation();var world=location.getWorld();
        if(world==null||!world.getUID().equals(entity.getWorld().getUID())||!Double.isFinite(location.getX())||!Double.isFinite(location.getY())||!Double.isFinite(location.getZ())
                ||location.getX()<Integer.MIN_VALUE||location.getX()>=Integer.MAX_VALUE||location.getZ()<Integer.MIN_VALUE||location.getZ()>=Integer.MAX_VALUE
                ||location.getY()<world.getMinHeight()||location.getY()>=world.getMaxHeight()
                ||!world.isChunkLoaded(Math.floorDiv(location.getBlockX(),16),Math.floorDiv(location.getBlockZ(),16)))return Optional.empty();
        return Optional.of(new Sight(member.animal(),PaperLivestockSpecies.from(type).orElseThrow(),world.getUID(),new Position(location.getBlockX(),location.getBlockY(),location.getBlockZ()),System.currentTimeMillis()));
    }
    @Override public void run() {
        if(!mainThread.getAsBoolean())throw new IllegalStateException("Livestock sightings require Paper main thread");
        long now=clock.getAsLong();if(!owner.ready()||now<retryAt)return;
        try {
            if(save!=null) {if(!save.isDone())return;observed+=save.join();save=null;retained=List.of();}
            if(!retained.isEmpty()){save=owner.record(retained);return;}
            if(page!=null) {
                if(!page.isDone())return;var members=page.join();page=null;
                if(members.size()>16)throw new IllegalStateException("Livestock member page exceeds native budget");
                var sights=new ArrayList<Sight>();for(var member:members) {
                    var sight=capture.apply(member);
                    if(sight.isPresent()){
                        if(!sight.get().animal().equals(member.animal()))throw new IllegalStateException("Captured another animal identity");
                        try{reconcile.accept(member,sight.get());}catch(RuntimeException failure){
                            var message=failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage();
                            if(!Objects.equals(ageFailure,message))logger.log(Level.WARNING,"Saved livestock age reconciliation deferred for "+member.animal()+"; position evidence retained",failure);ageFailure=message;
                        }
                        sights.add(sight.get());
                    }
                }
                cursor=members.isEmpty()?null:members.getLast().animal();retained=List.copyOf(sights);
                if(!retained.isEmpty()){save=owner.record(retained);return;}
            }
            page=owner.page(cursor,16);
        }catch(CompletionException failure) {
            if(save!=null)save=null;else page=null;
            retryAt=now+1000;logger.log(Level.WARNING,"Livestock presence save/read deferred; no departure or death inferred",failure.getCause());
        }
    }
    public void flush(){if(!retained.isEmpty())owner.record(retained).whenComplete((result,failure)->{if(failure!=null)logger.log(Level.SEVERE,"Livestock position shutdown save failed",failure);});}
    public String diagnostic(){return "Livestock positive sightings saved: "+observed+"; pending: "+retained.size()+"; missing bodies stay unknown; age last issue: "+(ageFailure==null?"none":ageFailure);}
}
