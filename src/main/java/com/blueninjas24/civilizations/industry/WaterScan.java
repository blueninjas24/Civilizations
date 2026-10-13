package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Bounded read-only full-height snapshot scan. Evidence alone grants no fishing or building rights. */
public final class WaterScan {
    public enum Cell { AIR, WATER, OTHER, UNKNOWN }
    public interface Source { Cell cell(int x, int y, int z); }
    public record Column(FisheryObservation.Column position, FisheryHabitatObservation.State state, Integer surfaceY) {
        public Column {
            Objects.requireNonNull(position);Objects.requireNonNull(state);
            if((state==FisheryHabitatObservation.State.SUITABLE)!=(surfaceY!=null))
                throw new IllegalArgumentException("Water surface requires suitable evidence");
        }
    }
    public record Observation(UUID world, int chunkX, int chunkZ, long capturedMillis,
                              int minimumY, int maximumY, int minimumDepth, List<Column> columns) {
        public Observation {
            Objects.requireNonNull(world); columns=List.copyOf(columns);
            long height=(long)maximumY-minimumY;
            if(capturedMillis<0||height<1||height>4096||minimumDepth<1||minimumDepth>64||minimumDepth>height
                    ||columns.size()!=256||columns.stream().map(Column::position).distinct().count()!=256
                    ||columns.stream().anyMatch(c -> Math.floorDiv(c.position().x(),16)!=chunkX
                        ||Math.floorDiv(c.position().z(),16)!=chunkZ
                        ||(c.surfaceY()!=null&&((long)c.surfaceY()<((long)minimumY+minimumDepth-1)||c.surfaceY()>=maximumY))))
                throw new IllegalArgumentException("Invalid full-height water observation");
            columns=columns.stream().sorted(Comparator.comparingInt((Column c)->c.position().x()).thenComparingInt(c->c.position().z())).toList();
        }
        public boolean complete() { return columns.stream().noneMatch(c -> c.state()==FisheryHabitatObservation.State.UNKNOWN); }
        public List<FisheryObservation.Column> suitableColumns() {
            return columns.stream().filter(c -> c.state()==FisheryHabitatObservation.State.SUITABLE).map(Column::position).toList();
        }
    }

    private final Source source;
    private final int chunkX,chunkZ,originX,originZ,minimumY,maximumY,minimumDepth;
    private final List<Column> results=new ArrayList<>();
    private int column,y,depth;
    private Integer surfaceY;
    private boolean exposed=true,reliable=true;

    /** Bounds must be the world's complete height range, not a clipped village-height window. */
    public WaterScan(Source source,int chunkX,int chunkZ,int minimumY,int maximumY,int minimumDepth) {
        this.source=Objects.requireNonNull(source);
        long height=(long)maximumY-minimumY;
        if(height<1||height>4096||minimumDepth<1||minimumDepth>64||minimumDepth>height)
            throw new IllegalArgumentException("Invalid water scan bounds/depth");
        this.chunkX=chunkX;this.chunkZ=chunkZ;this.minimumY=minimumY;this.maximumY=maximumY;this.minimumDepth=minimumDepth;
        originX=Math.multiplyExact(chunkX,16);originZ=Math.multiplyExact(chunkZ,16);
        Math.addExact(originX,15);Math.addExact(originZ,15);y=maximumY-1;
    }

    public int advance(int budget) {
        if(budget<1)throw new IllegalArgumentException("Invalid water cell budget");
        int used=0;
        while(!complete()&&used<budget) {
            var cell=Objects.requireNonNull(source.cell(column&15,y,column>>4));
            if(cell==Cell.UNKNOWN)reliable=false;
            // Only the first non-air surface is a candidate. Roofs, ice and waterlogged blocks are not open water.
            if(exposed&&cell==Cell.WATER) { surfaceY=y;depth=1;exposed=false; }
            else if(surfaceY!=null&&y==surfaceY-depth&&cell==Cell.WATER)depth++;
            if(cell!=Cell.AIR)exposed=false;
            used++;
            if(y==minimumY) {
                var state=!reliable?FisheryHabitatObservation.State.UNKNOWN:
                        depth>=minimumDepth?FisheryHabitatObservation.State.SUITABLE:FisheryHabitatObservation.State.ABSENT;
                results.add(new Column(new FisheryObservation.Column(originX+(column&15),originZ+(column>>4)),state,
                        state==FisheryHabitatObservation.State.SUITABLE?surfaceY:null));
                column++;y=maximumY-1;depth=0;surfaceY=null;exposed=true;reliable=true;
            } else y--;
        }
        return used;
    }
    public boolean complete(){return column==256;}
    public Observation observation(UUID world,long capturedMillis) {
        if(!complete())throw new IllegalStateException("Water scan unfinished");
        return new Observation(world,chunkX,chunkZ,capturedMillis,minimumY,maximumY,minimumDepth,results);
    }
}
