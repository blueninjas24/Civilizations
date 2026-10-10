package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Bounded center-first nearby geometry, independent of live chunk availability or extraction rights. */
public final class OreSurveyArea {
    public record Chunk(int x,int z) {}
    private OreSurveyArea() {}
    public static List<Chunk> around(int blockX,int blockZ,int radius) {
        if(radius<0 || radius>2)throw new IllegalArgumentException("Ore survey radius must be 0..2 chunks");
        int centerX=Math.floorDiv(blockX,16),centerZ=Math.floorDiv(blockZ,16);
        var result=new ArrayList<Chunk>();
        for(int ring=0;ring<=radius;ring++)for(int dx=-ring;dx<=ring;dx++)for(int dz=-ring;dz<=ring;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;
            long x=(long)centerX+dx,z=(long)centerZ+dz;
            // Clip geometry at the integer block-coordinate boundary, never wrap into another site.
            if(x*16<Integer.MIN_VALUE || x*16+15>Integer.MAX_VALUE || z*16<Integer.MIN_VALUE || z*16+15>Integer.MAX_VALUE)continue;
            result.add(new Chunk((int)x,(int)z));
        }
        return List.copyOf(result);
    }
}
