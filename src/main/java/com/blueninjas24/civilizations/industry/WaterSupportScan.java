package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Bounded exact original-column check: all overhead air and saved depth of open water. */
public final class WaterSupportScan {
    public enum Cell { AIR, WATER, OTHER, UNKNOWN }
    public record Position(int x,int y,int z) {}
    public interface Source {Cell cell(Position position);}
    private final Source source;
    private final WaterPatch.Patch original;
    private final int maximumY,depth;
    private final boolean surfaceWithinWorld;
    private final List<FisheryHabitatObservation.Support> supports=new ArrayList<>();
    private int cursor;private long y;
    private boolean suitable=true,unknown;
    public WaterSupportScan(Source source,WaterPatch.Patch original,int minimumY,int maximumY,int depth) {
        this.source=Objects.requireNonNull(source);this.original=Objects.requireNonNull(original);
        if((long)maximumY-minimumY<1||(long)maximumY-minimumY>4096||depth<1||depth>64||original.evidence().columns().size()>256||!original.evidence().complete())throw new IllegalArgumentException("Invalid live support bounds");
        this.maximumY=maximumY;this.depth=depth;surfaceWithinWorld=original.surfaceY()>=minimumY&&original.surfaceY()<maximumY;suitable=surfaceWithinWorld;y=(long)maximumY-1;
    }
    public int advance(int budget) {
        if(budget<1||budget>4096)throw new IllegalArgumentException("Invalid support read budget");int used=0;
        while(!complete()&&used<budget) {
            var column=original.evidence().columns().get(cursor);
            if(y<(long)original.surfaceY()-depth+1){finish(column);continue;}
            if(y<Integer.MIN_VALUE||y>Integer.MAX_VALUE){suitable=false;y=(long)original.surfaceY()-depth;continue;}
            var cell=Objects.requireNonNull(source.cell(new Position(column.x(),(int)y,column.z())));used++;
            if(cell==Cell.UNKNOWN)unknown=true;
            if(cell!=(y>original.surfaceY()?Cell.AIR:Cell.WATER))suitable=false;
            y--;
        }
        return used;
    }
    private void finish(FisheryObservation.Column column) {
        supports.add(new FisheryHabitatObservation.Support(column,unknown?FisheryHabitatObservation.State.UNKNOWN:suitable?FisheryHabitatObservation.State.SUITABLE:FisheryHabitatObservation.State.ABSENT));
        cursor++;y=(long)maximumY-1;suitable=surfaceWithinWorld;unknown=false;
    }
    public boolean complete(){return cursor==original.evidence().columns().size();}
    public FisheryHabitatObservation observation(long capturedMillis) {
        if(!complete())throw new IllegalStateException("Support scan incomplete");return new FisheryHabitatObservation(original.evidence().world(),capturedMillis,true,supports);
    }
    public boolean allSuitable(){return complete()&&supports.stream().allMatch(s->s.state()==FisheryHabitatObservation.State.SUITABLE);}
}
