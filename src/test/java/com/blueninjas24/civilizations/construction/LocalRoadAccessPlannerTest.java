package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionLandPolicy.Kind;
import com.blueninjas24.civilizations.construction.LocalRoadAccessPlanner.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalRoadAccessPlannerTest {
    private final UUID world=new UUID(0,1);
    private final Bounds bounds=new Bounds(p(0,63,0),p(8,63,4));
    private static Position p(int x,int y,int z){return new Position(x,y,z);}
    private Map<Position,Cell> cells(){
        var result=new HashMap<Position,Cell>();
        for(int x=0;x<=8;x++)for(int z=0;z<=4;z++)for(int y=63;y<=65;y++)result.put(p(x,y,z),new Cell(y>63,y==63,y==63&&x==0&&z==2,false));
        return result;
    }
    private Map<Position,Kind> terrain(Map<Position,Cell> cells){
        var kinds=new HashMap<Position,Kind>();cells.forEach((p,c)->kinds.put(p,c.air()?Kind.AIR:c.road()?Kind.ROAD:Kind.NATURAL_LOOKING_GROUND));return kinds;
    }
    private Result route(Map<Position,Cell> cells,List<Bounds> protectedLand,List<Bounds> entities,Policy policy,long seed){
        return select(new ConstructionSitePlanner.Survey(world,1000,true,true,cells,protectedLand,entities),terrain(cells),policy,1100,seed);
    }
    private Result select(ConstructionSitePlanner.Survey survey,Map<Position,Kind> kinds,Policy policy,long now,long seed){
        return LocalRoadAccessPlanner.select(world,p(6,63,2),bounds,survey,kinds,policy,now,1000,seed);
    }
    @Test void shortestConnectionIsDeterministicImmutableAndCountsOnlyNewSurface(){
        var cells=cells();var result=route(cells,List.of(),List.of(),Policy.prototype(),42);
        assertEquals(Status.READY,result.status());assertEquals(6,result.newSurfaceCells());assertEquals(7,result.corridor().size());
        assertEquals(p(6,63,2),result.corridor().getFirst());assertEquals(p(0,63,2),result.corridor().getLast());
        assertEquals(result,route(new LinkedHashMap<>(cells),List.of(),List.of(),Policy.prototype(),42));
        assertThrows(UnsupportedOperationException.class,()->result.corridor().clear());
        var already=LocalRoadAccessPlanner.select(world,p(0,63,2),bounds,new ConstructionSitePlanner.Survey(world,1000,true,true,cells,List.of(),List.of()),terrain(cells),Policy.prototype(),1100,1000,42);
        assertEquals(0,already.newSurfaceCells());assertEquals(1,already.corridor().size());
    }
    @Test void goesAroundProtectionAndOccupiedHeadroomWithoutClearingOrCrossing(){
        var cells=cells();cells.put(p(4,64,2),new Cell(false,false,false,false));
        var protection=new Bounds(p(3,63,1),p(3,65,2));var entity=new Bounds(p(2,64,2),p(2,65,2));
        var result=route(cells,List.of(protection),List.of(entity),Policy.prototype(),42);
        assertEquals(Status.READY,result.status());assertTrue(result.newSurfaceCells()>6);
        assertTrue(result.corridor().stream().noneMatch(p->protection.contains(p)||p.x()==4&&p.z()==2||p.x()==2&&p.z()==2));
        for(int z=0;z<=4;z++)cells.put(p(1,63,z),new Cell(false,true,false,true));
        var blocked=route(cells,List.of(),List.of(),Policy.prototype(),42);assertEquals(Status.NO_SAFE_ROUTE,blocked.status());assertTrue(blocked.corridor().isEmpty());
    }
    @Test void boundedWorkNeverReturnsHalfRouteAsReady(){
        var cells=cells();var steps=route(cells,List.of(),List.of(),new Policy(3,1024),42);
        assertEquals(Status.BUDGET_LIMIT,steps.status());assertEquals(List.of(),steps.corridor());
        var work=route(cells,List.of(),List.of(),new Policy(16,2),42);
        assertEquals(Status.BUDGET_LIMIT,work.status());assertEquals(2,work.reviewed());assertEquals(0,work.newSurfaceCells());
    }
    @Test void staleIncompleteWrongWorldOrContradictoryEvidenceCannotBecomeRoute(){
        var cells=cells();var kinds=terrain(cells);var policy=Policy.prototype();
        assertEquals(Status.WRONG_WORLD,select(new ConstructionSitePlanner.Survey(UUID.randomUUID(),1000,true,true,cells,List.of(),List.of()),kinds,policy,1100,42).status());
        assertEquals(Status.STALE,select(new ConstructionSitePlanner.Survey(world,1000,true,true,cells,List.of(),List.of()),kinds,policy,3000,42).status());
        assertEquals(Status.PROTECTION_UNKNOWN,select(new ConstructionSitePlanner.Survey(world,1000,true,false,cells,List.of(),List.of()),kinds,policy,1100,42).status());
        cells.remove(p(8,65,4));assertEquals(Status.INCOMPLETE,route(cells,List.of(),List.of(),policy,42).status());
        cells=cells();kinds=terrain(cells);kinds.put(p(6,63,2),Kind.AIR);
        assertEquals(Status.INCOMPLETE,select(new ConstructionSitePlanner.Survey(world,1000,true,true,cells,List.of(),List.of()),kinds,policy,1100,42).status());
    }
}
