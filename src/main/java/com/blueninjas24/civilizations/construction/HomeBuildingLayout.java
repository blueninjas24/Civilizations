package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Existing single-household home plans become explicit declarations, never completed/occupied buildings. */
public final class HomeBuildingLayout {
    private HomeBuildingLayout() {}
    public static BuildingLayout fromDraft(ConstructionDraft draft, Bounds plot) {
        HomePlanCapacity.beds(draft.template()); // Validate complete authored pairs before applying orientation.
        var entries=draft.template().anchors().stream().filter(a->a.role()==Role.ENTRANCE).toList();
        if(entries.size()!=1)throw new IllegalArgumentException("Single-home layout requires one declared primary entrance");
        var transform=draft.transform();var size=transform.size(draft.template().size());
        var structure=new Bounds(draft.origin(),draft.origin().plus(new Position(size.x()-1,size.y()-1,size.z()-1)));
        var entrance=draft.origin().plus(transform.apply(entries.getFirst().at(),draft.template().size()));
        var beds=draft.template().anchors().stream().filter(a->a.role()==Role.BED).map(a->new Bed(
                draft.origin().plus(transform.apply(a.at(),draft.template().size())),a.facing().transform(transform))).toList();
        var unitId=UUID.nameUUIDFromBytes(("civilizations:single-home-unit:"+draft.id()).getBytes(StandardCharsets.UTF_8));
        var unit=new Unit(unitId,entrance,List.of(structure),beds);
        return new BuildingLayout(draft.id(),draft.settlement(),draft.world(),Kind.HOUSING,plot,structure,entrance,List.of(),List.of(unit));
    }
}
