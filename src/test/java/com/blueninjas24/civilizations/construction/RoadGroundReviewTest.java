package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoadGroundReviewTest {
    private final UUID world=UUID.randomUUID();
    private LocalRoadDesign.Design design()throws Exception{return LocalRoadDesign.freeze(new LocalRoadAccessPlanner.Result(LocalRoadAccessPlanner.Status.READY,List.of(new Position(1,63,0),new Position(0,63,0)),1,2),ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_path.yml"))).orElseThrow();}
    private Map<Position,String> states(){return new HashMap<>(Map.of(new Position(1,63,0),"minecraft:grass_block[snowy=false]",new Position(1,64,0),"minecraft:air",new Position(1,65,0),"minecraft:air",new Position(0,63,0),"minecraft:dirt_path",new Position(0,64,0),"minecraft:air",new Position(0,65,0),"minecraft:air"));}
    @Test void preservesExactBeforeStateAndNeverIncludesExistingRoad()throws Exception{
        var result=RoadGroundReview.inspect(world,design(),new RoadGroundReview.Observation(world,1000,true,states(),Set.of()),1100);
        assertTrue(result.suitable());assertEquals(Map.of(new Position(1,63,0),"minecraft:grass_block[snowy=false]"),result.beforeStates());assertThrows(UnsupportedOperationException.class,()->result.beforeStates().clear());
    }
    @Test void existingConnectionMustStillHaveClearUnprotectedHeadroom()throws Exception{
        var design=design();var cells=states();cells.put(new Position(0,64,0),"minecraft:oak_planks");
        var blocked=RoadGroundReview.inspect(world,design,new RoadGroundReview.Observation(world,1000,true,cells,Set.of()),1100);
        assertTrue(blocked.issues().contains(RoadGroundReview.Issue.HEADROOM_BLOCKED));assertTrue(blocked.beforeStates().isEmpty());
        var protectedAccess=RoadGroundReview.inspect(world,design,new RoadGroundReview.Observation(world,1000,true,states(),Set.of(new Position(0,65,0))),1100);
        assertTrue(protectedAccess.issues().contains(RoadGroundReview.Issue.PROTECTED));assertTrue(protectedAccess.beforeStates().isEmpty());
    }
    @Test void editedProtectedUnknownOrStaleGroundCannotYieldPartialReplacementEvidence()throws Exception{
        for(String unsafe:List.of("minecraft:stone","minecraft:chest","minecraft:diamond_ore","minecraft:water","minecraft:cobblestone","minecraft:air")){
            var cells=states();cells.put(new Position(1,63,0),unsafe);var result=RoadGroundReview.inspect(world,design(),new RoadGroundReview.Observation(world,1000,true,cells,Set.of()),1100);assertFalse(result.suitable());assertTrue(result.beforeStates().isEmpty());
        }
        var design=design();assertTrue(RoadGroundReview.inspect(world,design,new RoadGroundReview.Observation(world,1000,true,states(),Set.of(new Position(1,63,0))),1100).issues().contains(RoadGroundReview.Issue.PROTECTED));
        assertTrue(RoadGroundReview.inspect(world,design,new RoadGroundReview.Observation(world,1000,true,states(),Set.of()),7000).issues().contains(RoadGroundReview.Issue.STALE));
        var cells=states();cells.remove(new Position(1,65,0));assertTrue(RoadGroundReview.inspect(world,design,new RoadGroundReview.Observation(world,1000,true,cells,Set.of()),1100).issues().contains(RoadGroundReview.Issue.INCOMPLETE));
    }
}
