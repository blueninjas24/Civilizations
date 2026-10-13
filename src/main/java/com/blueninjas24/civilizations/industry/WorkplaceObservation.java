package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Immutable block-type evidence, never block ownership or inventory contents. */
public record WorkplaceObservation(UUID world,int chunkX,int chunkZ,long capturedMillis,boolean complete,List<Marker> markers) {
    public enum Kind {
        BARREL("warehouse"), SMITHING_TABLE("blacksmith"), SMOKER("bakery"), CRAFTING_TABLE("craftsman");
        private final String industry;
        Kind(String industry){this.industry=industry;}
        public String industry(){return industry;}
    }
    public record Marker(Position position,Kind kind) {
        public Marker {Objects.requireNonNull(position);Objects.requireNonNull(kind);}
        public String industry(){return kind.industry();}
    }
    public WorkplaceObservation {
        Objects.requireNonNull(world);markers=markers.stream().sorted(Comparator.comparingInt((Marker m)->m.position().x())
                .thenComparingInt(m->m.position().y()).thenComparingInt(m->m.position().z()).thenComparing(Marker::kind)).toList();
        if(capturedMillis<0||markers.size()>512||new HashSet<>(markers.stream().map(Marker::position).toList()).size()!=markers.size())throw new IllegalArgumentException("Invalid workplace observation");
        for(var m:markers)if(Math.floorDiv(m.position().x(),16)!=chunkX||Math.floorDiv(m.position().z(),16)!=chunkZ)throw new IllegalArgumentException("Marker lies outside captured chunk");
    }
}
