package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.Home;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;

/** One bounded centre/cardinal snapshot group at a time; read-only, no forced chunk loading. */
public final class PaperOreObserver extends BukkitRunnable {
    private record Target(UUID town,Home home,OreSurveyArea.Chunk chunk) {}
    private final PopulationSession session;
    private final OreObserverPolicy policy;
    private final LongSupplier clock;
    private final boolean quarry;
    private final Deque<Target> targets=new ArrayDeque<>();
    private CompletableFuture<?> pending;
    private CompletableFuture<Boolean> eligibility;
    private boolean approvedTarget;
    private OreScan scan;
    private Target target;
    private int chunkX,chunkZ;
    private long ticks;
    private long snapshotMillis;
    private String diagnostic="No live ore survey completed";
    private String lastCompleted;
    public PaperOreObserver(PopulationSession session,OreObserverPolicy policy){this(session,policy,System::currentTimeMillis);}
    public PaperOreObserver(PopulationSession session,OreObserverPolicy policy,LongSupplier clock){this(session,policy,clock,false);}
    private PaperOreObserver(PopulationSession session,OreObserverPolicy policy,LongSupplier clock,boolean quarry){this.session=Objects.requireNonNull(session);this.policy=Objects.requireNonNull(policy);this.clock=Objects.requireNonNull(clock);this.quarry=quarry;}
    public static PaperOreObserver quarries(PopulationSession session,OreObserverPolicy policy){return quarries(session,policy,System::currentTimeMillis);}
    public static PaperOreObserver quarries(PopulationSession session,OreObserverPolicy policy,LongSupplier clock){return new PaperOreObserver(session,policy,clock,true);}
    public String diagnostic(){return (quarry?"Stone/sand: ":"")+diagnostic+(lastCompleted==null || diagnostic.equals(lastCompleted)?"":"; last completed: "+lastCompleted);}
    @Override public void run() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Ore observer requires main thread");
        if(!policy.enabled()){diagnostic="Live ore surveys disabled in economy.yml";return;}
        if(!session.oreCaptureReady()){scan=null;targets.clear();eligibility=null;approvedTarget=false;diagnostic="Live ore surveys paused: tracking not ready";return;}
        ticks++;
        try {
            if(pending!=null) {
                if(!pending.isDone())return;
                pending.join();pending=null;
            }
            if(eligibility!=null) {
                if(!eligibility.isDone())return;
                boolean allowed=eligibility.join();eligibility=null;
                if(!allowed){diagnostic="Ore survey skipped: chunk unavailable or lacks current provenance";return;}
                approvedTarget=true;
            }
            if(scan!=null) {
                var world=Bukkit.getWorld(target.home().worldId());
                if(world==null || !world.isChunkLoaded(chunkX,chunkZ)){scan=null;diagnostic="Ore survey discarded: world/chunk unloaded";return;}
                scan.advance(policy.cellsPerTick());
                if(!scan.complete())return;
                var evidence=scan.observations(world.getUID(),snapshotMillis);scan=null;
                if(evidence.stream().anyMatch(o->!o.complete())){diagnostic="Ore survey discarded: incomplete snapshot evidence";return;}
                var capturedTarget=target;
                pending=(quarry?session.surveyQuarries(target.town(),evidence,chunkX,chunkZ):session.surveyMinerals(target.town(),evidence)).thenAccept(patches-> {
                    // Callback touches only this volatile text; never world/entity/chunk APIs.
                    completed="Ore survey: "+patches.size()+" observed patches; "+patches.stream().filter(p->p.savedDeposit()!=null).count()
                            +" linked to saved deposits; snapshot captured at "+evidence.getFirst().observedMillis()+"; town "+capturedTarget.town()
                            +"; chunk "+capturedTarget.chunk().x()+","+capturedTarget.chunk().z()+"; positive evidence saved; automatic founding reviewed separately";
                });
                diagnostic="Ore survey awaiting owner validation";return;
            }
            // Publish one completed result before the next target can overwrite its explanation.
            if(completed!=null){lastCompleted=completed;diagnostic=completed;completed=null;return;}
            if(!approvedTarget&&targets.isEmpty() && ticks%policy.intervalTicks()==1) {
                var view=session.projectionView().orElseThrow();var homes=new TreeMap<UUID,Home>();
                for(var household:view.population().households()) {
                    var home=view.identities().homes().get(household.id());
                    if(home!=null)homes.merge(household.settlementId(),home,PaperOreObserver::earlier);
                    if(homes.size()>1024)throw new IllegalStateException("Ore town observation budget exceeded");
                }
                homes.forEach((town,home)->OreSurveyArea.around(home.position().x(),home.position().z(),policy.horizontalRadiusChunks())
                        .forEach(chunk->targets.addLast(new Target(town,home,chunk))));
            }
            if(!approvedTarget) {
                if(targets.isEmpty())return;
                target=targets.removeFirst();
                if(!session.oreDiscoveryReady()) {
                    eligibility=session.canSurveyResourceChunk("ore",target.home().worldId(),target.chunk().x(),target.chunk().z(),clock.getAsLong());
                    diagnostic="Ore survey awaiting scoped chunk provenance";return;
                }
            }
            approvedTarget=false;var world=Bukkit.getWorld(target.home().worldId());
            chunkX=target.chunk().x();chunkZ=target.chunk().z();
            if(world==null || !world.isChunkLoaded(chunkX,chunkZ))return;
            int minimumY=(int)Math.max(world.getMinHeight(),(long)target.home().position().y()-policy.verticalRadius());
            int maximumY=(int)Math.min(world.getMaxHeight(),(long)target.home().position().y()+policy.verticalRadius()+1);
            if(minimumY>=maximumY){diagnostic="Ore survey discarded: home outside world height";return;}
            snapshotMillis=clock.getAsLong();
            var snapshots=new HashMap<OreSurveyArea.Chunk,ChunkSnapshot>();
            for(var offset:new int[][]{{0,0},{-1,0},{1,0},{0,-1},{0,1}}) {
                int x=Math.addExact(chunkX,offset[0]),z=Math.addExact(chunkZ,offset[1]);
                if(world.isChunkLoaded(x,z))snapshots.put(new OreSurveyArea.Chunk(x,z),world.getChunkAt(x,z).getChunkSnapshot(false,false,false,false));
            }
            scan=quarry?OreScan.quarries(PaperOreNeighborhood.quarries(snapshots,chunkX,chunkZ,world.getMinHeight(),world.getMaxHeight()),chunkX,chunkZ,minimumY,maximumY,Set.of())
                    :new OreScan(PaperOreNeighborhood.ores(snapshots,chunkX,chunkZ,world.getMinHeight(),world.getMaxHeight()),chunkX,chunkZ,minimumY,maximumY,Set.of());
            diagnostic="Ore survey scanning nearby loaded chunk "+chunkX+","+chunkZ;
        } catch(RuntimeException failure){scan=null;pending=null;eligibility=null;approvedTarget=false;diagnostic="Ore survey deferred: "+failure.getMessage();}
    }
    private volatile String completed;
    private static Home earlier(Home a,Home b) {
        var order=Comparator.comparing(Home::worldId).thenComparingInt(h->h.position().x()).thenComparingInt(h->h.position().y()).thenComparingInt(h->h.position().z());
        return order.compare(a,b)<=0?a:b;
    }
}
