package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import java.util.*;

/** Main-thread, loaded-only immutable snapshots. A partial entity frame never becomes a roster. */
final class PaperPenCapture implements PaperPenControls.Capture {
    private final World world;
    private final PaperPenControls.Site site;
    private final PenProtectionSettings settings;
    private final Deque<PaperPenSource.Chunk> remaining=new ArrayDeque<>();
    private final Map<PaperPenSource.Chunk,PaperPenSource.Captured> snapshots=new HashMap<>();
    private final Map<UUID,LivestockObservation.Animal> animals=new HashMap<>();
    private final PaperLivestockSource livestock=new PaperLivestockSource();
    private PenScan scan;
    private PaperPenControls.Frame result;
    PaperPenCapture(World world,PaperPenControls.Site site,PenProtectionSettings settings){
        this.world=Objects.requireNonNull(world);this.site=site;this.settings=settings;
        if(!world.getUID().equals(site.world()))throw new IllegalArgumentException("Pen world changed");
        long minX=(long)site.seed().x()-settings.radius(),maxX=(long)site.seed().x()+settings.radius();
        long minZ=(long)site.seed().z()-settings.radius(),maxZ=(long)site.seed().z()+settings.radius();
        if(minX<Integer.MIN_VALUE||maxX>Integer.MAX_VALUE||minZ<Integer.MIN_VALUE||maxZ>Integer.MAX_VALUE)throw new IllegalArgumentException("Pen coordinate limit");
        for(int x=Math.floorDiv((int)minX,16);x<=Math.floorDiv((int)maxX,16);x++)
            for(int z=Math.floorDiv((int)minZ,16);z<=Math.floorDiv((int)maxZ,16);z++)remaining.add(new PaperPenSource.Chunk(x,z));
    }
    @Override public void advance(){
        if(result!=null)return;
        if(Bukkit.getWorld(site.world())!=world)throw new IllegalStateException("Pen world unloaded; try again");
        if(!world.isChunkLoaded(Math.floorDiv(site.signX(),16),Math.floorDiv(site.signZ(),16))
                ||!PaperPenControls.protectionSign(world.getBlockAt(site.signX(),site.signY(),site.signZ()).getState()))
            throw new IllegalStateException("Protection sign changed; request a new preview");
        if(captureAnimals){animalStep();return;}
        for(int n=0;n<settings.chunksPerTick()&&!remaining.isEmpty();n++){
            var key=remaining.removeFirst();if(!world.isChunkLoaded(key.x(),key.z()))continue;
            var chunk=world.getChunkAt(key.x(),key.z());
            snapshots.put(key,new PaperPenSource.Captured(site.world(),chunk.getChunkSnapshot()));
        }
        if(!remaining.isEmpty())return;
        if(scan==null)scan=new PenScan(new PaperPenSource(site.world(),site.y(),world.getMinHeight(),world.getMaxHeight(),snapshots),site.seed(),new PenScan.Limits(settings.radius(),settings.maximumArea()));
        scan.advance(settings.cellsPerTick());if(!scan.complete())return;
        var evidence=scan.evidence();
        if(evidence.state()!=PenScan.State.ENCLOSED){result=new PaperPenControls.Frame(evidence,Set.of());return;}
        // Inspect only chunks touched by the actual complete pen, not all terrain in the search window.
        if(animals.isEmpty()&&remaining.isEmpty()){
            var needed=new TreeSet<PaperPenSource.Chunk>(Comparator.comparingInt(PaperPenSource.Chunk::x).thenComparingInt(PaperPenSource.Chunk::z));
            for(var p:evidence.interior())needed.add(new PaperPenSource.Chunk(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16)));
            remaining.addAll(needed);captureAnimals=true;
        }
    }
    private boolean captureAnimals;
    @Override public boolean complete(){return result!=null;}
    @Override public PaperPenControls.Frame frame(){if(result==null)throw new IllegalStateException("Pen capture incomplete");return result;}
    /** Geometry and entity collection use separate bounded phases. */
    void animalStep(){
        for(int n=0;n<settings.chunksPerTick()&&!remaining.isEmpty();n++){
            var key=remaining.removeFirst();
            if(!world.isChunkLoaded(key.x(),key.z()))throw new IllegalStateException("Pen chunk unloaded; try again");
            var chunk=world.getChunkAt(key.x(),key.z());
            var frame=livestock.capture(chunk,site.world(),System.currentTimeMillis(),4096,256);
            if(!frame.complete())throw new IllegalStateException("Pen animal evidence incomplete; try again");
            for(var animal:frame.animals())if(animal.position().y()==site.y()&&scan.evidence().interior().contains(new PenScan.Column(animal.position().x(),animal.position().z()))) {
                if(animals.putIfAbsent(animal.id(),animal)!=null)throw new IllegalStateException("Animal crossed capture chunks; try again");
                if(animals.size()>settings.maximumAnimals())throw new IllegalStateException("Pen animal limit exceeded");
            }
        }
        if(remaining.isEmpty())result=new PaperPenControls.Frame(scan.evidence(),animals.keySet());
    }
}
