package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Explicit single-household declaration from a bounded region, never automatic apartment inference or consent. */
public final class HomeDeclaration {
    private HomeDeclaration(){}
    public record Review(BuildingLayout layout,Assessment assessment) {
        public Review {
            Objects.requireNonNull(layout);Objects.requireNonNull(assessment);
            if(!layout.world().equals(assessment.world())||!layout.fingerprint().equals(assessment.layoutHash()))
                throw new IllegalArgumentException("Home review does not match declared layout");
        }
        public boolean passes(){return assessment.issues().isEmpty()&&assessment.units().size()==1
                &&assessment.units().values().iterator().next().issues().isEmpty();}
    }
    /** Identifies raw region evidence before a building/unit layout exists. Not permission or an inspection result. */
    public static String regionHash(UUID world,Bounds structure) {
        Objects.requireNonNull(world);Objects.requireNonNull(structure);
        if(structure.volume()>BuildingLayout.MAX_STRUCTURE_VOLUME)throw new IllegalArgumentException("Home selection exceeds scan volume limit");
        try {
            var bytes=("civilizations:home-region:1:"+world+":"+structure.min()+":"+structure.max()).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    public static Review singleHousehold(UUID building,UUID village,UUID world,Bounds plot,Bounds structure,
            Position entrance,Observation region,long now,Policy policy) {
        Objects.requireNonNull(region);Objects.requireNonNull(policy);
        if(!region.world().equals(world)||!region.layoutHash().equals(regionHash(world,structure)))
            throw new IllegalArgumentException("Home observation belongs to another world or selection");
        if(!region.complete()||region.capturedMillis()>now||now-region.capturedMillis()>30_000
                ||region.cells().size()!=structure.volume()||region.cells().entrySet().stream().anyMatch(e->!structure.contains(e.getKey())||e.getValue().type()==Type.UNKNOWN))
            throw new IllegalArgumentException("Home selection evidence is incomplete or expired; scan again");
        var beds=region.cells().entrySet().stream().filter(e->e.getValue().type()==Type.BED_FOOT)
                .map(e->new BuildingLayout.Bed(e.getKey(),e.getValue().facing())).sorted().toList();
        if(beds.isEmpty())throw new IllegalArgumentException("No real bed feet in the selected home");
        var id=UUID.nameUUIDFromBytes(("civilizations:single-home-unit:"+building).getBytes(StandardCharsets.UTF_8));
        var unit=new Unit(id,entrance,List.of(structure),beds);
        var layout=new BuildingLayout(building,village,world,Kind.HOUSING,plot,structure,entrance,List.of(),List.of(unit));
        var observation=new Observation(world,layout.fingerprint(),region.capturedMillis(),true,region.cells());
        return new Review(layout,DwellingInspection.inspect(layout,observation,now,30_000,policy));
    }
}
