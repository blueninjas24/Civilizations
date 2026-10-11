package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Incremental plain-Java traversal over an immutable chunk snapshot. */
public final class WorkplaceScan {
    public enum Cell { OTHER, BARREL, SMITHING_TABLE, UNKNOWN }
    public interface Source {Cell cell(int x,int y,int z);}
    private final Source source;
    private final int chunkX,chunkZ,originX,originZ,maximumY;
    private final Set<WorkplaceObservation.Kind> kinds;
    private final List<WorkplaceObservation.Marker> markers=new ArrayList<>();
    private int y,column;
    private boolean reliable=true;
    public WorkplaceScan(Source source,int chunkX,int chunkZ,int minimumY,int maximumY,Set<WorkplaceObservation.Kind> kinds) {
        this.source=Objects.requireNonNull(source);this.kinds=Set.copyOf(kinds);this.chunkX=chunkX;this.chunkZ=chunkZ;
        if(minimumY>=maximumY||maximumY==Integer.MAX_VALUE||minimumY==Integer.MIN_VALUE||this.kinds.isEmpty())throw new IllegalArgumentException("Invalid workplace scan bounds");
        originX=Math.multiplyExact(chunkX,16);originZ=Math.multiplyExact(chunkZ,16);Math.addExact(originX,15);Math.addExact(originZ,15);y=minimumY;this.maximumY=maximumY;
    }
    public int advance(int budget) {
        if(budget<1)throw new IllegalArgumentException("Invalid cell budget");int used=0;
        while(!complete()&&used<budget) {
            int x=column&15,z=column>>4;var cell=Objects.requireNonNull(source.cell(x,y,z));
            if(cell==Cell.UNKNOWN)reliable=false;
            WorkplaceObservation.Kind kind=switch(cell){case BARREL->WorkplaceObservation.Kind.BARREL;case SMITHING_TABLE->WorkplaceObservation.Kind.SMITHING_TABLE;default->null;};
            if(kind!=null&&kinds.contains(kind)) {
                if(markers.size()==512)reliable=false;else markers.add(new WorkplaceObservation.Marker(new Position(originX+x,y,originZ+z),kind));
            }
            used++;if(++column==256){column=0;y++;}
        }return used;
    }
    public boolean complete(){return y==maximumY;}
    public WorkplaceObservation observation(UUID world,long capturedMillis) {
        if(!complete())throw new IllegalStateException("Workplace scan unfinished");return new WorkplaceObservation(world,chunkX,chunkZ,capturedMillis,reliable,markers);
    }
}
