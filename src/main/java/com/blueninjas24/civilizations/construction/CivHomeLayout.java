package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** First single-household Civ home declaration from the frozen authored plan. No capacity or ownership grant. */
public final class CivHomeLayout {
    private CivHomeLayout(){}
    public static BuildingLayout from(ConstructionDraft draft){
        var template=draft.template();HomePlanCapacity.beds(template);
        UUID building=id("building",draft.world(),draft.id()),unit=id("unit",draft.world(),draft.id());
        var footprint=ConstructionPreview.footprint(template,draft.transform(),draft.origin());
        var bounds=new BuildingLayout.Bounds(footprint.minimum(),footprint.maximum());
        var entry=template.anchors().stream().filter(a->a.role()==Role.ENTRANCE).findFirst().orElseThrow();
        var entrance=draft.origin().plus(draft.transform().apply(entry.at(),template.size()));
        var beds=template.anchors().stream().filter(a->a.role()==Role.BED)
                .map(a->new BuildingLayout.Bed(draft.origin().plus(draft.transform().apply(a.at(),template.size())),a.facing().transform(draft.transform()))).toList();
        return new BuildingLayout(building,draft.settlement(),draft.world(),BuildingLayout.Kind.HOUSING,bounds,bounds,entrance,List.of(),
                List.of(new BuildingLayout.Unit(unit,entrance,List.of(bounds),beds)));
    }
    private static UUID id(String kind,UUID world,UUID draft){return UUID.nameUUIDFromBytes(("civ-built-home-v1|"+kind+"|"+world+"|"+draft).getBytes(StandardCharsets.UTF_8));}
}
