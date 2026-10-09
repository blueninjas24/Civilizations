package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Conservative face-connected observed patches, not a claim about hidden geological continuity. */
public final class OreClusters {
    private OreClusters() {}
    private static final int[][] FACES={{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};

    /** Split complete bounded evidence without synthesizing coordinates, merging reserves or changing origin trust. */
    public static List<MineralObservation> split(MineralObservation observation) {
        Objects.requireNonNull(observation);
        if(!observation.complete())throw new IllegalArgumentException("Incomplete evidence cannot establish resource patches");
        var remaining=new HashSet<>(observation.ore());
        var clusters=new ArrayList<MineralObservation>();
        // MineralObservation supplies canonical coordinate order: anchors and output are deterministic.
        for(var anchor:observation.ore()) {
            if(!remaining.remove(anchor))continue;
            var queue=new ArrayDeque<Position>();queue.add(anchor);
            var patch=new ArrayList<Position>();
            while(!queue.isEmpty()) {
                var current=queue.removeFirst();patch.add(current);
                for(var face:FACES) {
                    long x=(long)current.x()+face[0],y=(long)current.y()+face[1],z=(long)current.z()+face[2];
                    if(x<Integer.MIN_VALUE || x>Integer.MAX_VALUE || y<Integer.MIN_VALUE || y>Integer.MAX_VALUE
                            || z<Integer.MIN_VALUE || z>Integer.MAX_VALUE)continue;
                    var neighbor=new Position((int)x,(int)y,(int)z);
                    if(remaining.remove(neighbor))queue.addLast(neighbor);
                }
            }
            clusters.add(new MineralObservation(observation.world(),observation.observedMillis(),true,observation.mineral(),patch));
        }
        return List.copyOf(clusters);
    }
}
