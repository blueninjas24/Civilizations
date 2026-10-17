package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Bounded local clearance only, never a proof that a distant enclosure does not exist. */
public final class FreeRangeScan {
    public enum Cell { CLEAR,BARRIER,UNKNOWN }
    public interface Source {Cell cell(Position position);}
    private final Source source;private final int minX,maxX,minY,maxY,minZ,maxZ;private int x,y,z;private Cell state;
    public FreeRangeScan(Source source,List<Position> animals,int radius){
        this.source=Objects.requireNonNull(source);
        if(animals.isEmpty()||animals.size()>32||radius<2||radius>16)throw new IllegalArgumentException("Invalid local clearance bounds");
        minX=Math.subtractExact(animals.stream().mapToInt(Position::x).min().orElseThrow(),radius);maxX=Math.addExact(animals.stream().mapToInt(Position::x).max().orElseThrow(),radius);
        minZ=Math.subtractExact(animals.stream().mapToInt(Position::z).min().orElseThrow(),radius);maxZ=Math.addExact(animals.stream().mapToInt(Position::z).max().orElseThrow(),radius);
        minY=animals.stream().mapToInt(Position::y).min().orElseThrow();maxY=animals.stream().mapToInt(Position::y).max().orElseThrow();
        if((long)maxX-minX>radius*4L||(long)maxZ-minZ>radius*4L||(long)maxY-minY>2)throw new IllegalArgumentException("Animal group too spread out for local clearance");
        x=minX;y=minY;z=minZ;
    }
    public int advance(int budget){
        if(budget<1||budget>4096)throw new IllegalArgumentException("Invalid clearance tick budget");int used=0;
        while(state==null&&used<budget){var cell=Objects.requireNonNull(source.cell(new Position(x,y,z)));used++;
            if(cell!=Cell.CLEAR){state=cell;break;}
            if(y<maxY)y++;else{y=minY;if(z<maxZ)z++;else{z=minZ;if(x<maxX)x++;else state=Cell.CLEAR;}}
        }
        return used;
    }
    public boolean complete(){return state!=null;}
    public Cell result(){if(state==null)throw new IllegalStateException("Clearance unfinished");return state;}
}
