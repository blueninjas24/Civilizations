package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.Objects;
import java.util.UUID;

/** Positive position evidence only; no missing-chunk or ownership inference. */
public record LivestockWorkingArea(int radiusBlocks) {
    public LivestockWorkingArea {
        if(radiusBlocks<1||radiusBlocks>256)throw new IllegalArgumentException("Livestock working radius must be 1..256 blocks");
    }
    public static LivestockWorkingArea prototype(){return new LivestockWorkingArea(32);}
    public boolean contains(UUID homeWorld,Position home,UUID observedWorld,Position observed) {
        Objects.requireNonNull(homeWorld);Objects.requireNonNull(home);Objects.requireNonNull(observedWorld);Objects.requireNonNull(observed);
        if(!homeWorld.equals(observedWorld))return false;
        double dx=(double)home.x()-observed.x(),dy=(double)home.y()-observed.y(),dz=(double)home.z()-observed.z();
        return dx*dx+dy*dy+dz*dz<=(double)radiusBlocks*radiusBlocks;
    }
}
