package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind;
import org.bukkit.*;
import java.util.*;

/** Loaded-only two-pass structural evidence, bracketed by exact fixed-marker reads within the same budget. */
public final class PaperWorkplaceSurvey {
    private final BuildingLayout layout;private final Position marker;private final PaperDwellingSurvey structure;private final long started;
    private boolean first;private String initialState;private Kind initialKind;private DwellingInspection.Observation observed;private WorkplaceInspection.Evidence finished;
    public PaperWorkplaceSurvey(BuildingLayout layout,Position marker,long now){
        this.layout=Objects.requireNonNull(layout);this.marker=Objects.requireNonNull(marker);started=now;
        if(!layout.structure().contains(marker))throw new IllegalArgumentException("Fixed marker must be inside workplace");
        structure=PaperDwellingSurvey.forWorkplace(layout,now);
    }
    public Optional<WorkplaceInspection.Evidence> advance(int budget,long now){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Workplace reads require server thread");
        if(budget<1||budget>256||now<0)throw new IllegalArgumentException("Invalid workplace survey budget/time");
        if(finished!=null)return Optional.of(finished);
        if(!first){
            var block=loadedMarker(now);first=true;
            if(block!=null){initialState=block.getBlockData().getAsString();initialKind=kind(block.getType());}
            if(--budget==0)return Optional.empty();
        }
        if(observed==null){var frame=structure.advance(budget,now);if(frame.isEmpty())return Optional.empty();observed=frame.orElseThrow();return Optional.empty();}
        var block=loadedMarker(now);
        boolean verified=block!=null&&initialState!=null&&initialKind!=null&&initialState.equals(block.getBlockData().getAsString());
        finished=new WorkplaceInspection.Evidence(observed,marker,Optional.ofNullable(initialKind),verified);return Optional.of(finished);
    }
    private org.bukkit.block.Block loadedMarker(long now){
        if(now<started||now-started>30_000)return null;var world=Bukkit.getWorld(layout.world());
        if(world==null||marker.y()<world.getMinHeight()||marker.y()>=world.getMaxHeight()||!world.isChunkLoaded(marker.x()>>4,marker.z()>>4))return null;
        return world.getBlockAt(marker.x(),marker.y(),marker.z());
    }
    private static Kind kind(Material material){return switch(material){case BARREL->Kind.BARREL;case SMITHING_TABLE->Kind.SMITHING_TABLE;case SMOKER->Kind.SMOKER;case CRAFTING_TABLE->Kind.CRAFTING_TABLE;default->null;};}
}
