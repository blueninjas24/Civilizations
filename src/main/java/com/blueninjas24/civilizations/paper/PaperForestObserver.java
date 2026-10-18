package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.Home;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.function.LongSupplier;

/** Bounded read-only environmental surveys. These samples never certify provenance or authorize adoption. */
public final class PaperForestObserver extends BukkitRunnable {
    public record EnvironmentalSample(UUID town,int chunkX,int chunkZ,ForestObservation observation) {}
    private record Target(UUID town,Home home,OreSurveyArea.Chunk chunk) {}
    private final PopulationSession session;
    private final ForestryObserverPolicy policy;
    private final LongSupplier now;
    private final Deque<Target> targets=new ArrayDeque<>();
    private java.util.concurrent.CompletableFuture<Set<com.blueninjas24.civilizations.settlement.VillageObservation.Position>> exclusions;
    private java.util.concurrent.CompletableFuture<Boolean> submitted;
    private ForestScan scan;
    private Target target;
    private EnvironmentalSample latest;
    private long ticks,captured;
    private String diagnostic="No forest survey completed",lastCompleted;
    public PaperForestObserver(PopulationSession session,ForestryObserverPolicy policy){this(session,policy,System::currentTimeMillis);}
    public PaperForestObserver(PopulationSession session,ForestryObserverPolicy policy,LongSupplier now){this.session=Objects.requireNonNull(session);this.policy=Objects.requireNonNull(policy);this.now=Objects.requireNonNull(now);}
    public String diagnostic(){return diagnostic+(lastCompleted==null||lastCompleted.equals(diagnostic)?"":"; last completed: "+lastCompleted);}
    /** Main-thread immutable diagnostic evidence; a future adopter still needs separate current provenance and owner validation. */
    public Optional<EnvironmentalSample> latestSample(){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Forest samples require main thread access");
        return Optional.ofNullable(latest);
    }
    @Override public void run() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Forest observation requires main thread");
        if(!policy.enabled()){diagnostic="Forest observation disabled in economy.yml";return;}
        if(!session.status().ready())return;
        ticks++;
        try {
            if(submitted!=null&&submitted.isDone()){submitted.join();submitted=null;}
            if(scan!=null) {
                var world=Bukkit.getWorld(target.home().worldId());
                if(world==null||!world.isChunkLoaded(target.chunk().x(),target.chunk().z())) {
                    scan=null;diagnostic="Forest survey discarded: world/chunk unloaded; no habitat loss inferred";return;
                }
                scan.advance(policy.cellsPerTick());if(!scan.complete())return;
                var evidence=scan.observation(world.getUID(),captured);scan=null;
                if(!evidence.complete()){diagnostic="Forest survey incomplete: unknown geometry or evidence limit; no adoption or habitat loss";return;}
                latest=new EnvironmentalSample(target.town(),target.chunk().x(),target.chunk().z(),evidence);
                submitted=session.observeForestSnapshot(target.town(),target.chunk().x(),target.chunk().z(),evidence);
                lastCompleted=diagnostic="Forest survey: "+evidence.trunks().size()+" canopy-backed log coordinates; chunk "+target.chunk().x()+","+target.chunk().z()
                        +"; snapshot captured at "+captured+"; environmental evidence only; logging startup independently checks provenance, skills, customers and funds";
                return;
            }
            if(exclusions!=null) {
                if(!exclusions.isDone())return;
                var protectedCoordinates=exclusions.join();exclusions=null;
                capture(protectedCoordinates);return;
            }
            if(targets.isEmpty()&&ticks%policy.intervalTicks()==1) {
                var homes=new TreeMap<UUID,Home>();var view=session.projectionView().orElseThrow();
                for(var household:view.population().households()) {
                    var home=view.identities().homes().get(household.id());
                    if(home!=null)homes.merge(household.settlementId(),home,PaperForestObserver::earlier);
                    if(homes.size()>1024)throw new IllegalStateException("Forest town observation budget exceeded");
                }
                homes.forEach((town,home)->OreSurveyArea.around(home.position().x(),home.position().z(),policy.horizontalRadiusChunks())
                        .forEach(chunk->targets.addLast(new Target(town,home,chunk))));
            }
            if(targets.isEmpty())return;
            target=targets.removeFirst();
            exclusions=session.forestExclusions(target.home().worldId(),target.chunk().x(),target.chunk().z());
            diagnostic="Forest survey awaiting saved placement exclusions";
        }catch(RuntimeException failure){scan=null;exclusions=null;submitted=null;diagnostic="Forest survey deferred: "+failure.getMessage();}
    }
    private void capture(Set<com.blueninjas24.civilizations.settlement.VillageObservation.Position> protectedCoordinates) {
            var world=Bukkit.getWorld(target.home().worldId());
            int x=target.chunk().x(),z=target.chunk().z();
            if(world==null||!world.isChunkLoaded(x,z))return;
            int minimumY=(int)Math.max(world.getMinHeight(),(long)target.home().position().y()-policy.verticalRadius());
            int maximumY=(int)Math.min(world.getMaxHeight(),(long)target.home().position().y()+policy.verticalRadius()+1);
            if(minimumY>=maximumY){diagnostic="Forest survey skipped: home outside world height";return;}
            captured=now.getAsLong();var snapshots=new HashMap<OreSurveyArea.Chunk,ChunkSnapshot>();
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                int cx=Math.addExact(x,dx),cz=Math.addExact(z,dz);
                if(world.isChunkLoaded(cx,cz))snapshots.put(new OreSurveyArea.Chunk(cx,cz),world.getChunkAt(cx,cz).getChunkSnapshot(false,false,false,false));
            }
            int margin=policy.geometry().horizontalCanopyRadius();
            int originX=Math.multiplyExact(x,16),originZ=Math.multiplyExact(z,16);
            var bounds=new ForestScan.Bounds(Math.subtractExact(originX,margin),minimumY,Math.subtractExact(originZ,margin),
                    Math.addExact(originX,16+margin),maximumY,Math.addExact(originZ,16+margin));
            scan=new ForestScan(new PaperForestSource(snapshots,world.getMinHeight(),world.getMaxHeight()),bounds,protectedCoordinates,policy.geometry());
            diagnostic="Forest survey scanning loaded chunk "+x+","+z;
    }
    private static Home earlier(Home a,Home b) {
        var order=Comparator.comparing(Home::worldId).thenComparingInt(h->h.position().x()).thenComparingInt(h->h.position().y()).thenComparingInt(h->h.position().z());
        return order.compare(a,b)<=0?a:b;
    }
}
