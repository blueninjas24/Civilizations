package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.*;
import org.bukkit.ChunkSnapshot;
import java.util.*;

/** Immutable centre/cardinal snapshots. Missing faces remain unknown; never reads or loads a World. */
public final class PaperOreNeighborhood implements OreScan.Source {
    private final int chunkX,chunkZ;
    private final Map<OreSurveyArea.Chunk,PaperOreSource> sources;
    public static PaperOreNeighborhood ores(Map<OreSurveyArea.Chunk,ChunkSnapshot> snapshots,int x,int z,int minimumY,int maximumY){return new PaperOreNeighborhood(snapshots,x,z,minimumY,maximumY,false);}
    public static PaperOreNeighborhood quarries(Map<OreSurveyArea.Chunk,ChunkSnapshot> snapshots,int x,int z,int minimumY,int maximumY){return new PaperOreNeighborhood(snapshots,x,z,minimumY,maximumY,true);}
    private PaperOreNeighborhood(Map<OreSurveyArea.Chunk,ChunkSnapshot> snapshots,int x,int z,int minimumY,int maximumY,boolean quarry) {
        if(minimumY>=maximumY||snapshots.size()<1||snapshots.size()>5)throw new IllegalArgumentException("Bounded centre/cardinal snapshots required");
        Math.addExact(Math.multiplyExact(x,16),15);Math.addExact(Math.multiplyExact(z,16),15);chunkX=x;chunkZ=z;
        var centre=Objects.requireNonNull(snapshots.get(new OreSurveyArea.Chunk(x,z)),"Centre snapshot required");var name=Objects.requireNonNull(centre.getWorldName(),"Snapshot world name required");
        var result=new HashMap<OreSurveyArea.Chunk,PaperOreSource>();
        for(var entry:snapshots.entrySet()) {
            var key=Objects.requireNonNull(entry.getKey());Math.addExact(Math.multiplyExact(key.x(),16),15);Math.addExact(Math.multiplyExact(key.z(),16),15);var snapshot=Objects.requireNonNull(entry.getValue());
            if(Math.abs((long)key.x()-x)+Math.abs((long)key.z()-z)>1||snapshot.getX()!=key.x()||snapshot.getZ()!=key.z()||!name.equals(snapshot.getWorldName()))throw new IllegalArgumentException("Snapshot location/world does not match bounded neighborhood");
            result.put(key,quarry?PaperOreSource.quarries(snapshot,minimumY,maximumY):new PaperOreSource(snapshot,minimumY,maximumY));
        }
        sources=Map.copyOf(result);
    }
    @Override public OreScan.Kind kind(int x,int y,int z) {
        if(x< -1||x>16||z< -1||z>16||(x<0||x>15)&&(z<0||z>15))return OreScan.Kind.UNKNOWN;
        var source=sources.get(new OreSurveyArea.Chunk(Math.addExact(chunkX,Math.floorDiv(x,16)),Math.addExact(chunkZ,Math.floorDiv(z,16))));
        return source==null?OreScan.Kind.UNKNOWN:source.kind(Math.floorMod(x,16),y,Math.floorMod(z,16));
    }
}
