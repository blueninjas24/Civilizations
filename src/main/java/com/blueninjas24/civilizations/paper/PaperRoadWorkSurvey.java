package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.*;
import org.bukkit.*;
import org.bukkit.util.BoundingBox;
import java.util.*;

/** Two bounded loaded-only exact road/clearance passes. No chunk loading, permission or world modification. */
public final class PaperRoadWorkSurvey {
    public record Frame(LocalRoadDesign.Geometry geometry,int buffer,RoadGroundReview.Observation observation,boolean ready,String explanation){}
    private final ConstructionWorkQueue.Work work;private final LocalRoadDesign.Geometry geometry;private final int buffer;
    private final List<Position> cells;private final Map<Position,String> first=new TreeMap<>(),prefix=new HashMap<>();
    private final Map<Position,String> registered;
    private final long started;private final BoundingBox entities;private int cursor;private boolean verifying,unsafe;private String reason;private Frame finished;
    public PaperRoadWorkSurvey(ConstructionWorkQueue.Work work,int buffer,long now){
        main();this.work=Objects.requireNonNull(work);this.buffer=buffer;geometry=LocalRoadDesign.geometry(work.draft());started=now;
        if(now<0)throw new IllegalArgumentException("Invalid road survey time");cells=List.copyOf(RoadGroundReview.requiredCells(geometry,buffer));
        registered=RoadGroundReview.registeredWithin(work.roadSurfaces(),Set.copyOf(cells));
        var placements=work.draft().template().stages().getFirst().blocks().entrySet().stream().toList();
        if(work.nextOrdinal()<0||work.nextOrdinal()>placements.size())throw new IllegalArgumentException("Road work ordinal differs from frozen design");
        PaperConstructionLibrary.prepare(work.draft().template(),work.draft().transform());
        for(int i=0;i<work.nextOrdinal();i++)prefix.put(geometry.origin().plus(placements.get(i).getKey()),placements.get(i).getValue());
        if(work.pending().isPresent()){
            var intent=work.pending().get();if(work.nextOrdinal()==placements.size()||!intent.draft().equals(work.draft().id())||intent.ordinal()!=work.nextOrdinal()
                    ||!intent.at().equals(geometry.origin().plus(placements.get(work.nextOrdinal()).getKey()))||!intent.after().equals(placements.get(work.nextOrdinal()).getValue()))throw new IllegalArgumentException("Pending road intent differs from frozen work");
        }
        if(work.state()!=ConstructionWorkQueue.State.READY&&work.state()!=ConstructionWorkQueue.State.RESUME&&work.state()!=ConstructionWorkQueue.State.COMPLETE){unsafe=true;reason="Road work is not paid/earned and ready";}
        int minX=geometry.connection().x(),maxX=minX,minZ=geometry.connection().z(),maxZ=minZ;
        for(var p:placements){var at=geometry.origin().plus(p.getKey());minX=Math.min(minX,at.x());maxX=Math.max(maxX,at.x());minZ=Math.min(minZ,at.z());maxZ=Math.max(maxZ,at.z());}
        entities=new BoundingBox(minX,geometry.origin().y(),minZ,maxX+1.0,geometry.origin().y()+3.0,maxZ+1.0);
    }
    public Optional<Frame> advance(int budget,long now){
        main();if(budget<1||budget>256||now<0)throw new IllegalArgumentException("Invalid road survey budget/time");if(finished!=null)return Optional.of(finished);
        if(unsafe&&!verifying&&cursor==0)return Optional.of(finish(now,false,reason));
        var world=Bukkit.getWorld(work.draft().world());if(world==null||now<started||now-started>30_000)return Optional.of(finish(now,false,"Road world absent, clock changed or survey expired"));
        while(budget-->0&&cursor<cells.size()){
            var at=cells.get(cursor++);if(!available(world,at)){unsafe=true;reason="Road cells outside loaded chunks, border or height";continue;}
            String state=world.getBlockAt(at.x(),at.y(),at.z()).getBlockData().getAsString();
            if(verifying){if(!state.equals(first.get(at))){unsafe=true;reason="Road scene changed between passes";}}
            else first.put(at,state);
        }
        if(cursor<cells.size())return Optional.empty();
        if(!verifying&&!unsafe){verifying=true;cursor=0;return Optional.empty();}
        for(var at:cells)if(!available(world,at)){unsafe=true;reason="Road chunk or border became unavailable";}
        if(!unsafe&&!entityChunksLoaded(world,geometry)){unsafe=true;reason="Road entity census chunks are not fully available";}
        if(!unsafe&&!world.getNearbyEntities(entities).isEmpty()){unsafe=true;reason="Entity occupies road or retained access";}
        var known=new HashMap<>(prefix);
        if(work.pending().isPresent()){var p=work.pending().get();String current=first.get(p.at());if(p.after().equals(current))known.put(p.at(),p.after());else if(!p.before().equals(current)){unsafe=true;reason="Retained road before-state changed";}}
        var observation=new RoadGroundReview.Observation(work.draft().world(),now,!unsafe,first,Set.of(),registered);
        var result=RoadGroundReview.inspect(work.draft().world(),geometry,observation,now,known,buffer);
        if(!result.suitable()){unsafe=true;if(reason==null)reason="Road evidence refused: "+result.issues();}
        return Optional.of(finish(now,!unsafe,unsafe?reason:"Two exact road/clearance passes complete; paid owner authorization still required"));
    }
    private Frame finish(long now,boolean ready,String explanation){finished=new Frame(geometry,buffer,new RoadGroundReview.Observation(work.draft().world(),now,ready,first,Set.of(),registered),ready,explanation);return finished;}
    static BoundingBox entityBounds(LocalRoadDesign.Geometry geometry){
        int minX=geometry.connection().x(),maxX=minX,minZ=geometry.connection().z(),maxZ=minZ;
        for(var local:geometry.template().stages().getFirst().blocks().keySet()){var at=geometry.origin().plus(local);minX=Math.min(minX,at.x());maxX=Math.max(maxX,at.x());minZ=Math.min(minZ,at.z());maxZ=Math.max(maxZ,at.z());}
        return new BoundingBox(minX,geometry.origin().y(),minZ,maxX+1.0,geometry.origin().y()+3.0,maxZ+1.0);
    }
    static boolean entityChunksLoaded(World world,LocalRoadDesign.Geometry geometry){
        var bounds=entityBounds(geometry);int lastX=(int)Math.floor(bounds.getMaxX()-1),lastZ=(int)Math.floor(bounds.getMaxZ()-1);
        for(int x=((int)Math.floor(bounds.getMinX()))>>4;x<=lastX>>4;x++)for(int z=((int)Math.floor(bounds.getMinZ()))>>4;z<=lastZ>>4;z++)
            if(!world.isChunkLoaded(x,z)||!world.getChunkAt(x,z).isEntitiesLoaded())return false;
        return true;
    }
    static boolean available(World world,Position at){return at.y()>=world.getMinHeight()&&at.y()<world.getMaxHeight()&&world.isChunkLoaded(at.x()>>4,at.z()>>4)&&world.getWorldBorder().isInside(new Location(world,at.x(),at.y(),at.z()));}
    private static void main(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Road survey requires server thread");}
}
