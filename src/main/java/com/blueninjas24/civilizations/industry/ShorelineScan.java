package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Bounded read-only search for dry standing space adjacent to a selected water footprint. */
public final class ShorelineScan {
    public enum Cell { AIR, SAFE_FOOTING, OTHER, UNKNOWN }
    public record Position(int x,int y,int z) {}
    public interface Source { Cell cell(Position position); }
    private final Source source;
    private final List<Position> candidates;
    private final List<Position> suitable=new ArrayList<>();
    private int cursor,stage;
    private boolean safe=true,unknown;
    public ShorelineScan(Source source,WaterPatch.Patch patch,int maximumRise) {
        this.source=Objects.requireNonNull(source);Objects.requireNonNull(patch);
        if(!patch.evidence().complete()||patch.evidence().columns().size()>256||maximumRise<0||maximumRise>4)
            throw new IllegalArgumentException("Invalid shoreline search bounds");
        var water=new HashSet<>(patch.evidence().columns());
        var positions=new TreeSet<Position>(Comparator.comparingInt(Position::x).thenComparingInt(Position::y).thenComparingInt(Position::z));
        for(var column:water)for(var offset:List.of(new int[]{-1,0},new int[]{1,0},new int[]{0,-1},new int[]{0,1})) {
            long x=(long)column.x()+offset[0],z=(long)column.z()+offset[1];
            if(x<Integer.MIN_VALUE||x>Integer.MAX_VALUE||z<Integer.MIN_VALUE||z>Integer.MAX_VALUE||water.contains(new FisheryObservation.Column((int)x,(int)z)))continue;
            for(int rise=0;rise<=maximumRise;rise++) {
                long standing=(long)patch.surfaceY()+rise+1;
                if(standing<=Integer.MIN_VALUE||standing>=Integer.MAX_VALUE)continue;
                positions.add(new Position((int)x,(int)standing,(int)z));
            }
        }
        if(positions.size()>4096)throw new IllegalArgumentException("Shoreline candidate budget exceeded");
        candidates=List.copyOf(positions);
    }
    public int advance(int budget) {
        if(budget<1||budget>4096)throw new IllegalArgumentException("Invalid shoreline block budget");
        int used=0;
        while(!complete()&&used<budget) {
            var position=candidates.get(cursor);int y=position.y()+stage-1;
            var cell=Objects.requireNonNull(source.cell(new Position(position.x(),y,position.z())));used++;
            if(cell==Cell.UNKNOWN)unknown=true;
            if(cell!=(stage==0?Cell.SAFE_FOOTING:Cell.AIR))safe=false;
            if(++stage==3){if(safe)suitable.add(position);cursor++;stage=0;safe=true;}
        }
        return used;
    }
    public boolean complete(){return cursor==candidates.size();}
    public boolean encounteredUnknown(){return unknown;}
    public List<Position> suitable(){if(!complete())throw new IllegalStateException("Shoreline scan unfinished");return List.copyOf(suitable);}
}
