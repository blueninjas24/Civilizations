package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Read-only recovery plan around the same original habitat; no repairs, closure or resource changes. */
public final class ShorelineRecovery {
    public enum Action { KEEP, RELOCATE, WAIT_UNKNOWN, NEEDS_REPAIR }
    public record Decision(Action action,ShorelineScan.Position position,boolean confirmedSafe,boolean currentConfirmedUnsafe,String explanation) {}
    private final ShorelineScan scan;
    private final ShorelineScan.Position current;
    private final int maximumHorizontalDistance;
    public ShorelineRecovery(ShorelineScan.Source source,WaterPatch.Patch originalHabitat,ShorelineScan.Position current,
            int maximumRise,int maximumHorizontalDistance) {
        this.current=Objects.requireNonNull(current);
        if(maximumHorizontalDistance<0||maximumHorizontalDistance>256)throw new IllegalArgumentException("Invalid shoreline relocation distance");
        this.maximumHorizontalDistance=maximumHorizontalDistance;
        scan=new ShorelineScan(source,originalHabitat,maximumRise);
        if(!scan.candidates().contains(current))throw new IllegalArgumentException("Saved standing point must border its original habitat within opening height terms");
    }
    public int advance(int budget){return scan.advance(budget);}
    public boolean complete(){return scan.complete();}
    public Decision decision() {
        var assessments=scan.assessments();
        var original=assessments.stream().filter(a->a.position().equals(current)).findFirst().orElseThrow();
        if(original.state()==ShorelineScan.State.SAFE)return new Decision(Action.KEEP,current,true,false,"Original shoreline remains safe; no relocation or resource change");
        if(original.state()==ShorelineScan.State.UNKNOWN)return new Decision(Action.WAIT_UNKNOWN,current,false,false,"Original standing point unknown; wait for loaded evidence, no inferred damage or relocation");
        var nearby=assessments.stream().filter(a->horizontalDistance(a.position())<=maximumHorizontalDistance).toList();
        var order=Comparator.comparingLong((ShorelineScan.Position p)->horizontalDistance(p)+Math.abs((long)p.y()-current.y()))
                .thenComparingInt(ShorelineScan.Position::x).thenComparingInt(ShorelineScan.Position::y).thenComparingInt(ShorelineScan.Position::z);
        var replacement=nearby.stream().filter(a->a.state()==ShorelineScan.State.SAFE).map(ShorelineScan.Assessment::position).min(order);
        if(replacement.isPresent())return new Decision(Action.RELOCATE,replacement.orElseThrow(),true,true,"Confirmed unsafe standing point; use nearest safe nearby bank beside the same original habitat, preserving business and resources");
        if(nearby.stream().anyMatch(a->a.state()==ShorelineScan.State.UNKNOWN))return new Decision(Action.WAIT_UNKNOWN,current,false,true,"Standing point unsafe but nearby bank evidence incomplete; wait without closing or inventing repairs");
        return new Decision(Action.NEEDS_REPAIR,current,false,true,"No safe nearby replacement; retain business and habitat, request a feasible protected repair/dock project when construction is available");
    }
    private long horizontalDistance(ShorelineScan.Position point){return Math.abs((long)point.x()-current.x())+Math.abs((long)point.z()-current.z());}
}
