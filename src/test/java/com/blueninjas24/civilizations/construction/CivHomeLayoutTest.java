package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CivHomeLayoutTest {
    @Test void frozenFourBedHomeHasStableSingleUnitIdentityAndCorrectBedsInAllEightOrientations()throws Exception{
        var plan=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)){
            var transform=new Transform(rotation,mirror);var draft=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new Position(-32,70,100),transform,plan,1000);
            var layout=CivHomeLayout.from(draft);assertEquals(layout,CivHomeLayout.from(draft));assertEquals(1,layout.units().size());assertEquals(4,layout.declaredBeds());assertEquals(BuildingLayout.Kind.HOUSING,layout.kind());
            assertNotEquals(layout.id(),layout.units().getFirst().id());assertEquals(layout.plot(),layout.structure());
            for(var anchor:plan.anchors())if(anchor.role()==Role.BED){
                var foot=draft.origin().plus(transform.apply(anchor.at(),plan.size()));
                // Authored starter beds face south; geometric transformed head independently checks the direction transform.
                var head=draft.origin().plus(transform.apply(anchor.at().plus(new Position(0,0,1)),plan.size()));
                var bed=layout.units().getFirst().beds().stream().filter(b->b.foot().equals(foot)).findFirst().orElseThrow();assertEquals(head,bed.head());
            }
        }
    }
}
