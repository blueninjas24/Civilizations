package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.Home;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;

/** Main-thread loaded snapshots only; SQL callbacks cannot touch Paper or inventories. */
public final class PaperWorkplaceObserver extends BukkitRunnable {
    private record Target(UUID town,Home home,OreSurveyArea.Chunk chunk) {}
    private final PopulationSession session;
    private final WorkplaceObserverPolicy policy;
    private final LongSupplier clock;
    private final Deque<Target> targets=new ArrayDeque<>();
    private CompletableFuture<?> pending;
    private WorkplaceScan scan;
    private Target target;
    private long ticks,capturedMillis;
    private String diagnostic="No workplace scan completed";
    public PaperWorkplaceObserver(PopulationSession session,WorkplaceObserverPolicy policy){this(session,policy,System::currentTimeMillis);}
    public PaperWorkplaceObserver(PopulationSession session,WorkplaceObserverPolicy policy,LongSupplier clock){this.session=Objects.requireNonNull(session);this.policy=Objects.requireNonNull(policy);this.clock=Objects.requireNonNull(clock);}
    public String diagnostic(){return diagnostic;}
    @Override public void run() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Workplace observer requires main thread");
        if(!policy.enabled()){diagnostic="Workplace recognition disabled in economy.yml";return;}
        if(!session.status().ready())return;ticks++;
        try {
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(scan!=null) {
                var world=Bukkit.getWorld(target.home().worldId());
                if(world==null||!world.isChunkLoaded(target.chunk().x(),target.chunk().z())) {
                    pending=session.observeWorkplaces(target.town(),new WorkplaceObservation(target.home().worldId(),target.chunk().x(),target.chunk().z(),capturedMillis,false,List.of()));
                    scan=null;diagnostic="Workplace capture invalidated: world/chunk unloaded";return;
                }
                scan.advance(policy.cellsPerTick());if(!scan.complete())return;
                var observation=scan.observation(world.getUID(),capturedMillis);scan=null;
                pending=session.observeWorkplaces(target.town(),observation);
                diagnostic=(observation.complete()?"Complete":"Partial")+" workplace snapshot: "+observation.markers().size()+" markers; no inventory or ownership changes";return;
            }
            if(targets.isEmpty()&&ticks%policy.intervalTicks()==1) {
                var view=session.projectionView().orElseThrow();var homes=new TreeMap<UUID,Home>();
                for(var household:view.population().households()) {
                    var home=view.identities().homes().get(household.id());if(home!=null)homes.merge(household.settlementId(),home,PaperWorkplaceObserver::earlier);
                    if(homes.size()>1024)throw new IllegalStateException("Workplace town observation budget exceeded");
                }
                homes.forEach((town,home)->OreSurveyArea.around(home.position().x(),home.position().z(),policy.horizontalRadiusChunks()).forEach(chunk->targets.addLast(new Target(town,home,chunk))));
            }
            if(targets.isEmpty())return;target=targets.removeFirst();var world=Bukkit.getWorld(target.home().worldId());
            if(world==null||!world.isChunkLoaded(target.chunk().x(),target.chunk().z()))return;
            int minimumY=(int)Math.max(world.getMinHeight(),(long)target.home().position().y()-policy.verticalRadius());
            int maximumY=(int)Math.min(world.getMaxHeight(),(long)target.home().position().y()+policy.verticalRadius()+1);
            if(minimumY>=maximumY){diagnostic="Workplace home outside world height";return;}
            capturedMillis=clock.getAsLong();
            var snapshot=world.getChunkAt(target.chunk().x(),target.chunk().z()).getChunkSnapshot(false,false,false,false);
            scan=new WorkplaceScan(new PaperWorkplaceSource(snapshot,world.getMinHeight(),world.getMaxHeight()),target.chunk().x(),target.chunk().z(),minimumY,maximumY,policy.kinds());
            diagnostic="Workplace scan in nearby loaded chunk "+target.chunk().x()+","+target.chunk().z();
        }catch(RuntimeException failure){scan=null;pending=null;diagnostic="Workplace scan deferred: "+failure.getMessage();}
    }
    private static Home earlier(Home a,Home b) {
        var order=Comparator.comparing(Home::worldId).thenComparingInt(h->h.position().x()).thenComparingInt(h->h.position().y()).thenComparingInt(h->h.position().z());return order.compare(a,b)<=0?a:b;
    }
}
