package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Trusted loaded-world local proof, not a saved claim. Observed duration resets after uncertain scans. */
public record LivestockOpportunity(UUID settlement,UUID world,Position site,List<LivestockObservation.Animal> animals,
                                  long capturedMillis,long observedMillis) {
    public LivestockOpportunity {
        Objects.requireNonNull(settlement);Objects.requireNonNull(world);Objects.requireNonNull(site);animals=List.copyOf(animals);
        if(animals.isEmpty()||animals.size()>32||capturedMillis<0||observedMillis<0
                ||animals.stream().map(LivestockObservation.Animal::id).distinct().count()!=animals.size()
                ||animals.stream().map(LivestockObservation.Animal::species).distinct().count()!=1)
            throw new IllegalArgumentException("Invalid single-species livestock opportunity");
    }
    public LivestockHerd.Species species(){return animals.getFirst().species();}
    public static boolean natural(LivestockObservation.Animal animal){return !animal.named()&&!animal.leashed()&&!animal.recordedPlayerBreeding()
            &&Set.of("NATURAL","CHUNK_GEN").contains(animal.spawnReason());}
}
