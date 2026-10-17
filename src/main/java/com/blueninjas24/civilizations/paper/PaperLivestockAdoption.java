package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.LivestockInvestment;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

/** Repeated positive local evidence only. No chunk loads, animal spawning, block edits or implicit pen transfer. */
public final class PaperLivestockAdoption extends BukkitRunnable implements Listener,AutoCloseable {
    private record Seen(long first,long last) {}
    private final PopulationSession session;private final LivestockAdoptionPolicy policy;private final Logger logger;
    private final PaperLivestockSource source=new PaperLivestockSource();
    private CompletableFuture<List<LivestockInvestment.Village>> villagesFuture;
    private CompletableFuture<LivestockInvestment.Result> save;
    private List<LivestockInvestment.Village> villages=List.of();private int villageCursor,speciesCursor,anchorCursor,ticks;
    private Job job;private AtomicBoolean proof=new AtomicBoolean(true);private boolean closed;
    private final Map<String,Seen> seen=new LinkedHashMap<>();
    private String explanation="waiting for trusted village data";
    private long opened;
    private static final class Job {
        final LivestockInvestment.Village village;final World world;final int minX,maxX,minZ,maxZ;final long started;
        final List<LivestockObservation.Animal> animals=new ArrayList<>();int x,z;
        FreeRangeScan clearance;List<LivestockObservation.Animal> group=List.of();
        Job(LivestockInvestment.Village village,World world,int radius,long now){this.village=village;this.world=world;started=now;
            minX=Math.floorDiv(village.center().x()-radius,16);maxX=Math.floorDiv(village.center().x()+radius,16);
            minZ=Math.floorDiv(village.center().z()-radius,16);maxZ=Math.floorDiv(village.center().z()+radius,16);x=minX;z=minZ;}
    }
    public PaperLivestockAdoption(PopulationSession session,LivestockAdoptionPolicy policy,Logger logger){this.session=Objects.requireNonNull(session);this.policy=Objects.requireNonNull(policy);this.logger=Objects.requireNonNull(logger);}
    @Override public void run(){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Livestock discovery requires main thread");
        if(closed||!policy.enabled())return;
        try {
            if(save!=null){if(!save.isDone())return;var result=save.join();save=null;explanation=result.explanation();if(result.opened())opened++;job=null;}
            if(!session.livestockCaptureReady()){job=null;seen.clear();proof.set(false);explanation="waiting for trusted animal loss capture";return;}
            if(villagesFuture!=null){if(!villagesFuture.isDone())return;villages=villagesFuture.join();villagesFuture=null;}
            if(job!=null){advance();return;}
            if(++ticks<policy.intervalTicks())return;ticks=0;
            if(villages.isEmpty()||villageCursor>=villages.size()){villageCursor=0;villagesFuture=session.livestockVillages();return;}
            var village=villages.get(villageCursor++);var world=Bukkit.getWorld(village.world());
            if(world==null){explanation="saved world absent";return;}
            proof=new AtomicBoolean(true);job=new Job(village,world,policy.villageRadius(),System.currentTimeMillis());explanation="checking loaded nearby animals";
        }catch(CompletionException failed){job=null;save=null;villagesFuture=null;proof.set(false);explanation="save/query failed; retrying bounded observations";logger.log(Level.WARNING,"Automatic livestock review failed",failed.getCause());}
        catch(RuntimeException failed){job=null;proof.set(false);explanation="observation failed; no adoption";logger.log(Level.WARNING,"Automatic livestock observation failed",failed);}
    }
    private void advance(){
        var j=job;long now=System.currentTimeMillis();
        if(!proof.get()||now<j.started||now-j.started>30000||Bukkit.getWorld(j.village.world())!=j.world){uncertain("observation changed or expired");return;}
        if(j.clearance==null){
            int chunks=0;
            while(j.x<=j.maxX&&chunks++<policy.chunksPerTick()){
                if(j.world.isChunkLoaded(j.x,j.z)){
                    var frame=source.capture(j.world.getChunkAt(j.x,j.z),j.village.world(),now,4096,256);
                    if(!frame.complete()){uncertain("incomplete loaded animal frame");return;}
                    for(var animal:frame.animals())if(LivestockOpportunity.natural(animal)&&!session.villageAnimals().contains(animal.id())
                            &&near(animal,j.village.center(),policy.villageRadius()))j.animals.add(animal);
                    if(j.animals.size()>256){uncertain("local animal review limit exceeded");return;}
                }
                if(j.z<j.maxZ)j.z++;else{j.z=j.minZ;j.x++;}
            }
            if(j.x<=j.maxX)return;
            var species=LivestockHerd.Species.values()[speciesCursor++%LivestockHerd.Species.values().length];
            var possible=j.animals.stream().filter(a->a.species()==species).sorted(Comparator.comparing(a->a.id().toString())).toList();
            if(possible.isEmpty()){discard("no eligible natural "+species.name().toLowerCase(Locale.ROOT)+" in loaded village area");return;}
            var anchor=possible.get(Math.floorMod(anchorCursor++,possible.size())).position();
            j.group=possible.stream().filter(a->Math.abs((long)a.position().x()-anchor.x())<=policy.clearRadius()
                    &&Math.abs((long)a.position().z()-anchor.z())<=policy.clearRadius()&&Math.abs((long)a.position().y()-anchor.y())<=1).limit(policy.maximumAnimals()).toList();
            if(j.group.stream().filter(LivestockObservation.Animal::adult).count()<policy.minimumAdults()){discard("waiting for enough eligible adults of one species");return;}
            j.clearance=new FreeRangeScan(new PaperFreeRangeSource(j.world),j.group.stream().map(LivestockObservation.Animal::position).toList(),policy.clearRadius());
        }
        j.clearance.advance(policy.cellsPerTick());if(!j.clearance.complete())return;
        if(j.clearance.result()!=FreeRangeScan.Cell.CLEAR){discard("nearby barrier or unavailable terrain; ownership remains untouched");return;}
        var fresh=refresh(j);if(fresh.isEmpty()){discard("animals moved or protection/origin changed; fresh proof required");return;}
        String key=j.village.town()+":"+j.group.stream().map(a->a.id().toString()).sorted().toList();
        seen.entrySet().removeIf(e->now<e.getValue().last()||now-e.getValue().last()>120000);
        var old=seen.get(key);if(old==null){if(seen.size()>=256)seen.remove(seen.keySet().iterator().next());old=new Seen(now,now);}
        seen.put(key,new Seen(old.first(),now));
        if(now-old.first()<policy.observationMillis()){explanation="free-range group observed; waiting for repeated clear proof";job=null;return;}
        var evidence=new LivestockOpportunity(j.village.town(),j.village.world(),fresh.getFirst().position(),fresh,now,now-old.first());
        var acceptedProof=proof;save=session.adoptLivestock(evidence,acceptedProof::get);explanation="checking ownership, skills and protected startup funds";
    }
    private List<LivestockObservation.Animal> refresh(Job j){
        var frames=new HashMap<Long,LivestockObservation>();var result=new ArrayList<LivestockObservation.Animal>();
        for(var animal:j.group){int x=Math.floorDiv(animal.position().x(),16),z=Math.floorDiv(animal.position().z(),16);long key=((long)x<<32)^(z&0xffffffffL);
            if(!j.world.isChunkLoaded(x,z))return List.of();
            var frame=frames.computeIfAbsent(key,k->source.capture(j.world.getChunkAt(x,z),j.village.world(),System.currentTimeMillis(),4096,256));
            if(!frame.complete())return List.of();
            var current=frame.animals().stream().filter(a->a.id().equals(animal.id())&&a.species()==animal.species()&&a.position().equals(animal.position())&&LivestockOpportunity.natural(a)).findFirst();
            if(current.isEmpty())return List.of();result.add(current.orElseThrow());
        }
        return List.copyOf(result);
    }
    private static boolean near(LivestockObservation.Animal a,com.blueninjas24.civilizations.settlement.VillageObservation.Position center,int radius){
        long x=(long)a.position().x()-center.x(),z=(long)a.position().z()-center.z();return Math.abs((long)a.position().y()-center.y())<=radius&&Math.abs(x)<=radius&&Math.abs(z)<=radius&&x*x+z*z<=(long)radius*radius;
    }
    private void discard(String reason){if(job!=null&&!job.group.isEmpty())seen.remove(job.village.town()+":"+job.group.stream().map(a->a.id().toString()).sorted().toList());proof.set(false);job=null;explanation=reason;}
    private void uncertain(String reason){if(job!=null){String prefix=job.village.town()+":";seen.keySet().removeIf(k->k.startsWith(prefix));}discard(reason);}
    private void changed(){if(job!=null||save!=null){proof.set(false);seen.clear();}}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void placed(BlockPlaceEvent event){changed();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void broken(BlockBreakEvent event){changed();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void interacted(PlayerInteractEntityEvent event){changed();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void exploded(BlockExplodeEvent event){changed();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void exploded(EntityExplodeEvent event){changed();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void extended(BlockPistonExtendEvent event){changed();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void retracted(BlockPistonRetractEvent event){changed();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void bred(EntityBreedEvent event){changed();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void unloaded(ChunkUnloadEvent event){if(job!=null&&job.village.world().equals(event.getWorld().getUID()))changed();}
    public String diagnostic(){return "Livestock founding: "+(policy.enabled()?explanation:"disabled")+"; opened "+opened;}
    @Override public void close(){closed=true;proof.set(false);job=null;seen.clear();}
}
