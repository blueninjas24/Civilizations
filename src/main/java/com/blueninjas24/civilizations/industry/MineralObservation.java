package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;

/** Bounded coordinates from a trusted survey. This does not certify natural origin or grant block rights. */
public record MineralObservation(UUID world,long observedMillis,boolean complete,Commodity mineral,List<Position> ore) {
    public MineralObservation {
        Objects.requireNonNull(world);Objects.requireNonNull(mineral);ore=List.copyOf(ore);
        if(observedMillis<0 || ore.size()>512 || !Set.of(Commodity.COAL,Commodity.IRON,Commodity.STONE,Commodity.SAND).contains(mineral)
                || new HashSet<>(ore).size()!=ore.size())throw new IllegalArgumentException("Invalid bounded finite-resource survey");
        ore=ore.stream().sorted(Comparator.comparingInt(Position::x).thenComparingInt(Position::y).thenComparingInt(Position::z)).toList();
    }
}
