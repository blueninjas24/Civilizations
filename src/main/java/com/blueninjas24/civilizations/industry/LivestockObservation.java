package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Immutable visible evidence only. An empty frame never establishes death, ownership or adoption. */
public record LivestockObservation(UUID world,int chunkX,int chunkZ,long capturedMillis,boolean complete,
        List<Animal> animals,String explanation) {
    public record Animal(UUID id,LivestockHerd.Species species,Position position,int ageTicks,boolean named,boolean leashed) {
        public Animal {Objects.requireNonNull(id);Objects.requireNonNull(species);Objects.requireNonNull(position);}
        public boolean adult(){return ageTicks>=0;}
    }
    public LivestockObservation {
        Objects.requireNonNull(world);Objects.requireNonNull(explanation);animals=List.copyOf(animals);
        if(capturedMillis<0||animals.size()>256||explanation.isBlank()||explanation.length()>1024||!complete&&!animals.isEmpty())throw new IllegalArgumentException("Invalid livestock observation");
        var ids=new HashSet<UUID>();for(var a:animals)if(!ids.add(a.id())||Math.floorDiv(a.position().x(),16)!=chunkX||Math.floorDiv(a.position().z(),16)!=chunkZ)throw new IllegalArgumentException("Duplicate or out-of-chunk livestock identity");
    }
}
