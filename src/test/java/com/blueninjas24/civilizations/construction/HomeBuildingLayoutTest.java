package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.BuildingLayout.Bounds;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HomeBuildingLayoutTest {
    @Test void allEightOrientationsDeclareOneStableFourBedHomeRatherThanFourApartments()throws Exception {
        var template=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        var id=new UUID(0,1);var town=new UUID(0,2);var world=new UUID(0,3);var origin=new Position(-17,64,31);
        UUID unit=null;
        for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)) {
            var transform=new Transform(rotation,mirror);var size=transform.size(template.size());
            var plot=new Bounds(origin,origin.plus(new Position(size.x()-1,size.y()-1,size.z()-1)));
            var draft=new ConstructionDraft(id,town,world,origin,transform,template,1000);
            var layout=HomeBuildingLayout.fromDraft(draft,plot);
            assertEquals(id,layout.id());assertEquals(town,layout.settlement());assertEquals(world,layout.world());
            assertEquals(1,layout.units().size());assertEquals(4,layout.declaredBeds());
            if(unit==null)unit=layout.units().getFirst().id();else assertEquals(unit,layout.units().getFirst().id());
            for(var bed:layout.units().getFirst().beds()) {
                assertTrue(layout.structure().contains(bed.foot()));assertTrue(layout.structure().contains(bed.head()));
                assertEquals(Direction.SOUTH.transform(transform),bed.facing());
            }
            assertEquals(layout,HomeBuildingLayout.fromDraft(draft,plot));
        }
    }
    @Test void nonHomesAndPlotsThatDoNotContainTheStructureCannotAcquireHousingDeclarations()throws Exception {
        var plan=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_path.yml"));
        var origin=new Position(0,64,0);var plot=new Bounds(origin,new Position(10,70,10));
        var draft=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),origin,Transform.identity(),plan,1000);
        assertThrows(IllegalArgumentException.class,()->HomeBuildingLayout.fromDraft(draft,plot));
        var home=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        var homeDraft=new ConstructionDraft(draft.id(),draft.settlement(),draft.world(),origin,Transform.identity(),home,1000);
        assertThrows(IllegalArgumentException.class,()->HomeBuildingLayout.fromDraft(homeDraft,new Bounds(origin,origin)));
    }
}
