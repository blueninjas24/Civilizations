package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.ConstructionPreview.Bounds;
import java.util.*;

/** Read-only flat local access corridor. A route grants no land rights, paving, payment or write authority. */
public final class LocalRoadAccessPlanner {
    public enum Status { READY, WRONG_WORLD, STALE, INCOMPLETE, PROTECTION_UNKNOWN, NO_ROAD, NO_SAFE_ROUTE, BUDGET_LIMIT }
    public record Policy(int maximumNewSteps,int maximumReviewed) {
        public Policy { if(maximumNewSteps<1||maximumNewSteps>64||maximumReviewed<1||maximumReviewed>4096)throw new IllegalArgumentException("Invalid local road search budget"); }
        public static Policy prototype(){return new Policy(16,1024);}
    }
    public record Result(Status status,List<Position> corridor,int newSurfaceCells,int reviewed) {
        public Result { corridor=List.copyOf(corridor); }
    }
    private LocalRoadAccessPlanner(){}
    /** start is a ground-surface coordinate; two clear cells above it are required. No slopes or clearing. */
    public static Result select(UUID world,Position start,Bounds ground,ConstructionSitePlanner.Survey survey,
                                Map<Position,ConstructionLandPolicy.Kind> terrain,Policy policy,long now,long maximumAge,long seed) {
        Objects.requireNonNull(world);Objects.requireNonNull(start);Objects.requireNonNull(ground);Objects.requireNonNull(survey);Objects.requireNonNull(policy);
        terrain=Map.copyOf(terrain);
        long width=(long)ground.maximum().x()-ground.minimum().x()+1,depth=(long)ground.maximum().z()-ground.minimum().z()+1;
        if(ground.minimum().y()!=ground.maximum().y()||!ground.contains(start)||width*depth>4096||now<0||maximumAge<1||maximumAge>3_600_000)
            throw new IllegalArgumentException("Invalid bounded local road frame");
        if(!survey.world().equals(world))return held(Status.WRONG_WORLD,0);
        if(survey.capturedMillis()>now||now-survey.capturedMillis()>maximumAge)return held(Status.STALE,0);
        if(!survey.complete())return held(Status.INCOMPLETE,0);
        if(!survey.protectionsComplete())return held(Status.PROTECTION_UNKNOWN,0);
        boolean road=false;
        for(int z=ground.minimum().z();;z++){
            for(int x=ground.minimum().x();;x++){
                var p=new Position(x,start.y(),z);
                for(int y=0;y<3;y++){
                    var at=p.plus(new Position(0,y,0));var cell=survey.cells().get(at);var kind=terrain.get(at);
                    if(cell==null||kind==null||(kind==ConstructionLandPolicy.Kind.AIR)!=cell.air()||(kind==ConstructionLandPolicy.Kind.ROAD)!=cell.road())return held(Status.INCOMPLETE,0);
                }
                if(survey.cells().get(p).road()&&usable(p,survey,terrain))road=true;
                if(x==ground.maximum().x())break;
            }
            if(z==ground.maximum().z())break;
        }
        if(!road)return held(Status.NO_ROAD,0);
        if(!usable(start,survey,terrain))return held(Status.NO_SAFE_ROUTE,0);
        var waiting=new ArrayDeque<Position>();var parent=new HashMap<Position,Position>();var distances=new HashMap<Position,Integer>();
        waiting.add(start);distances.put(start,0);int reviewed=0;boolean capped=false;
        int[][] directions={{1,0},{0,1},{-1,0},{0,-1}};int offset=(int)Math.floorMod(seed,4);
        while(!waiting.isEmpty()){
            if(reviewed==policy.maximumReviewed())return held(Status.BUDGET_LIMIT,reviewed);
            var p=waiting.removeFirst();reviewed++;
            if(survey.cells().get(p).road()){
                var route=new ArrayList<Position>();for(var q=p;q!=null;q=parent.get(q))route.add(q);Collections.reverse(route);
                int fresh=(int)route.stream().filter(q->!survey.cells().get(q).road()).count();
                if(fresh<=policy.maximumNewSteps())return new Result(Status.READY,route,fresh,reviewed);
                capped=true;continue;
            }
            if(distances.get(p)>=policy.maximumNewSteps()){capped=true;continue;}
            for(int i=0;i<4;i++){
                var direction=directions[(i+offset)%4];long x=(long)p.x()+direction[0],z=(long)p.z()+direction[1];
                if(x<ground.minimum().x()||x>ground.maximum().x()||z<ground.minimum().z()||z>ground.maximum().z())continue;
                var next=new Position((int)x,p.y(),(int)z);
                if(distances.containsKey(next)||!usable(next,survey,terrain))continue;
                parent.put(next,p);distances.put(next,distances.get(p)+1);waiting.addLast(next);
            }
        }
        return held(capped?Status.BUDGET_LIMIT:Status.NO_SAFE_ROUTE,reviewed);
    }
    private static boolean usable(Position p,ConstructionSitePlanner.Survey survey,Map<Position,ConstructionLandPolicy.Kind> terrain){
        var ground=survey.cells().get(p);var kind=terrain.get(p);
        if(!ground.stableSupport()||kind!=ConstructionLandPolicy.Kind.ROAD&&kind!=ConstructionLandPolicy.Kind.NATURAL_LOOKING_GROUND)return false;
        for(int y=0;y<3;y++){
            var at=p.plus(new Position(0,y,0));var cell=survey.cells().get(at);
            if(cell.protectedWork()||survey.protections().stream().anyMatch(b->b.contains(at))||survey.entities().stream().anyMatch(b->b.contains(at))||y>0&&!cell.air())return false;
        }
        return true;
    }
    private static Result held(Status status,int reviewed){return new Result(status,List.of(),0,reviewed);}
}
