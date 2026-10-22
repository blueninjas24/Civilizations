package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.LocalRoadAccessPlanner.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalRoadDesignTest {
    private static Position p(int x,int z){return new Position(x,63,z);}
    private ConstructionTemplate recipe()throws Exception{return ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_path.yml"));}
    private Result route(List<Position> points){return new Result(Status.READY,points,points.size()-1,30);}
    @Test void eightNewCellsUseFourRealStoneUnitsAndEightWorkWithoutPavingExistingConnection()throws Exception{
        var points=new ArrayList<Position>();for(int x=8;x>=0;x--)points.add(p(x,0));
        var source=recipe();var design=LocalRoadDesign.freeze(route(points),source).orElseThrow();var plan=design.template();
        assertEquals(p(1,0),design.origin());assertEquals(p(0,0),design.connection());assertEquals(8,plan.blockCount());
        assertEquals(8,plan.work());assertEquals(Map.of(Commodity.STONE,4L),plan.materials());assertEquals(source.fingerprint(),design.recipeHash());
        assertEquals(new Position(-1,0,0),plan.anchors().getFirst().at());assertEquals(Direction.WEST,plan.anchors().getFirst().facing());
        assertFalse(plan.stages().getFirst().blocks().containsKey(new Position(-1,0,0)));
        assertEquals(plan,ConstructionTemplateSnapshot.decode(ConstructionTemplateSnapshot.encode(plan)));
    }
    @Test void bendsKeepOnlyActualRouteCellsAndAdminRecipeControlsRealCostsAndSurface()throws Exception{
        var source=recipe();var recipe=new ConstructionTemplate(source.id(),2,source.name(),source.attribution(),Purpose.PATH,source.size(),
                List.of(new Stage("surface",10,Map.of(Commodity.STONE,5L),source.stages().getFirst().blocks().keySet().stream().collect(java.util.stream.Collectors.toMap(k->k,k->"minecraft:stone_bricks")))),source.anchors());
        var result=LocalRoadDesign.freeze(route(List.of(p(3,2),p(2,2),p(2,1),p(1,1))),recipe).orElseThrow();
        assertEquals(new Size(2,1,2),result.template().size());assertEquals(3,result.template().blockCount());assertEquals(6,result.template().work());assertEquals(Map.of(Commodity.STONE,3L),result.template().materials());
        assertEquals(Set.of("minecraft:stone_bricks"),new HashSet<>(result.template().stages().getFirst().blocks().values()));
        assertFalse(result.template().stages().getFirst().blocks().containsKey(new Position(1,0,0))); // Corner's empty cell stays empty.
        assertThrows(UnsupportedOperationException.class,()->result.template().stages().getFirst().blocks().clear());
    }
    @Test void directDesignConstructionCannotSmuggleOtherPurposesOrAnUnconnectedSurface()throws Exception{
        var design=LocalRoadDesign.freeze(route(List.of(p(3,0),p(2,0),p(1,0))),recipe()).orElseThrow();var t=design.template();
        var home=new ConstructionTemplate(t.id(),1,t.name(),t.attribution(),Purpose.HOME,t.size(),t.stages(),List.of(t.anchors().getFirst(),new Anchor(Role.ENTRANCE,new Position(0,0,0),Direction.WEST)));
        assertThrows(IllegalArgumentException.class,()->new LocalRoadDesign.Design(design.origin(),design.connection(),home,design.recipeHash()));
        var disconnected=new ConstructionTemplate(t.id(),1,t.name(),t.attribution(),Purpose.PATH,t.size(),List.of(new Stage("surface",1,t.materials(),Map.of(new Position(1,0,0),"minecraft:cobblestone"))),t.anchors());
        assertThrows(IllegalArgumentException.class,()->new LocalRoadDesign.Design(design.origin(),design.connection(),disconnected,design.recipeHash()));
        assertThrows(IllegalArgumentException.class,()->new LocalRoadDesign.Design(design.origin(),p(99,99),t,design.recipeHash()));
    }
    @Test void incompleteDisconnectedDuplicateOrSlopingRoutesCannotBecomePlans()throws Exception{
        var source=recipe();assertTrue(LocalRoadDesign.freeze(route(List.of(p(0,0))),source).isEmpty());
        assertThrows(IllegalArgumentException.class,()->LocalRoadDesign.freeze(new Result(Status.BUDGET_LIMIT,List.of(),0,20),source));
        for(var points:List.of(List.of(p(0,0),p(2,0)),List.of(p(0,0),p(1,0),p(0,0)),List.of(p(0,0),new Position(1,64,0))))
            assertThrows(IllegalArgumentException.class,()->LocalRoadDesign.freeze(route(points),source));
        assertThrows(IllegalArgumentException.class,()->LocalRoadDesign.freeze(new Result(Status.READY,List.of(p(0,0),p(1,0)),0,2),source));
    }
}
