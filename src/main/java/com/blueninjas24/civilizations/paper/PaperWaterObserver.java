package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.Home;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;
import java.util.logging.*;

/** Main-thread, already-loaded full-height snapshots; persistence runs on the SQL owner. */
public final class PaperWaterObserver extends BukkitRunnable {
    private record Target(UUID town,Home home,OreSurveyArea.Chunk chunk) {}
    private final PopulationSession session;
    private final WaterObserverPolicy policy;
    private final LongSupplier clock;
    private final Logger logger;
    private final Deque<Target> targets=new ArrayDeque<>();
    private CompletableFuture<?> pending;
    private WaterScan scan;
    private Target target;
    private long ticks,capturedMillis;
    private int minimumY,maximumY;
    private String diagnostic="No water scan completed";
    public PaperWaterObserver(PopulationSession session,WaterObserverPolicy policy,Logger logger){this(session,policy,logger,System::currentTimeMillis);}
    public PaperWaterObserver(PopulationSession session,WaterObserverPolicy policy,Logger logger,LongSupplier clock){this.session=Objects.requireNonNull(session);this.policy=Objects.requireNonNull(policy);this.logger=Objects.requireNonNull(logger);this.clock=Objects.requireNonNull(clock);}
    public String diagnostic(){return diagnostic;}
    @Override public void run() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Water observer requires main thread");
        if(!policy.enabled()){diagnostic="Water observation disabled in economy.yml";return;}
        if(!session.status().ready())return;ticks++;
        try {
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(scan!=null) {
                var world=Bukkit.getWorld(target.home().worldId());
                if(world==null||!world.isChunkLoaded(target.chunk().x(),target.chunk().z())) {
                    var unknown=new ArrayList<WaterScan.Column>();
                    for(int x=0;x<16;x++)for(int z=0;z<16;z++)unknown.add(new WaterScan.Column(new FisheryObservation.Column(target.chunk().x()*16+x,target.chunk().z()*16+z),FisheryHabitatObservation.State.UNKNOWN,null));
                    pending=session.observeWater(target.town(),new WaterScan.Observation(target.home().worldId(),target.chunk().x(),target.chunk().z(),capturedMillis,minimumY,maximumY,policy.minimumDepth(),unknown));
                    scan=null;diagnostic="Water capture invalidated: world/chunk unloaded; no habitat loss inferred";return;
                }
                scan.advance(policy.cellsPerTick());if(!scan.complete())return;
                var observation=scan.observation(world.getUID(),capturedMillis);scan=null;
                pending=session.observeWater(target.town(),observation);
                diagnostic=(observation.complete()?"Complete":"Partial")+" water snapshot: "+observation.suitableColumns().size()+" suitable columns; qualified shoreline startup checks run separately";return;
            }
            if(targets.isEmpty()&&ticks%policy.intervalTicks()==1) {
                var view=session.projectionView().orElseThrow();var homes=new TreeMap<UUID,Home>();
                for(var household:view.population().households()) {
                    var home=view.identities().homes().get(household.id());if(home!=null)homes.merge(household.settlementId(),home,PaperWaterObserver::earlier);
                    if(homes.size()>1024)throw new IllegalStateException("Water town observation budget exceeded");
                }
                homes.forEach((town,home)->OreSurveyArea.around(home.position().x(),home.position().z(),policy.horizontalRadiusChunks()).forEach(chunk->targets.addLast(new Target(town,home,chunk))));
            }
            if(targets.isEmpty())return;target=targets.removeFirst();var world=Bukkit.getWorld(target.home().worldId());
            if(world==null||!world.isChunkLoaded(target.chunk().x(),target.chunk().z()))return;
            minimumY=world.getMinHeight();maximumY=world.getMaxHeight();capturedMillis=clock.getAsLong();
            var snapshot=world.getChunkAt(target.chunk().x(),target.chunk().z()).getChunkSnapshot(false,false,false,false);
            scan=new WaterScan(new PaperWaterSource(snapshot,minimumY,maximumY),target.chunk().x(),target.chunk().z(),minimumY,maximumY,policy.minimumDepth());
            diagnostic="Water scan in nearby loaded chunk "+target.chunk().x()+","+target.chunk().z();
        }catch(RuntimeException failure){scan=null;pending=null;diagnostic="Water scan deferred: "+failure.getMessage();logger.log(Level.WARNING,"Water observation failed; no resource or business created",failure);}
    }
    private static Home earlier(Home a,Home b) {
        var order=Comparator.comparing(Home::worldId).thenComparingInt(h->h.position().x()).thenComparingInt(h->h.position().y()).thenComparingInt(h->h.position().z());return order.compare(a,b)<=0?a:b;
    }
}
