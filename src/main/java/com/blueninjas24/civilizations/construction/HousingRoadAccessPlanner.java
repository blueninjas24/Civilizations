package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.util.*;

/** Conditional road-first home expansion. Read-only: neither the home nor its road gains authority. */
public final class HousingRoadAccessPlanner {
    public static final int MAX_ENTRANCES=32,MAX_CANDIDATES=256,MAX_CELL_REVIEWS=262_144,MAX_ROUTE_NODES=4096;
    public enum Status { EXISTING_ACCESS,READY,NO_SAFE_PLAN,INVALID_EVIDENCE,BUDGET_LIMIT }
    public enum Rejection { HOME_UNSUITABLE,ROUTE_UNAVAILABLE,NATIVE_CLEARANCE_LIMIT }
    public record Expansion(Position homeOrigin,Transform homeTransform,String homeHash,LocalRoadDesign.Design road){}
    public record Result(Status status,Optional<Expansion> expansion,ConstructionSitePlanner.Result infill,
                         Map<Rejection,Integer> rejected,int candidates,int cellReviews,int routeNodes,boolean budgetLimited){
        public Result{expansion=Objects.requireNonNull(expansion);rejected=Map.copyOf(rejected);}
    }
    private HousingRoadAccessPlanner(){}
    public static Result select(ConstructionTemplate home,ConstructionTemplate roadRecipe,UUID world,Position need,
            ConstructionSitePlanner.Survey survey,ApprovedArea envelope,ConstructionLandPolicy land,
            Map<Position,ConstructionLandPolicy.Kind> terrain,LocalRoadAccessPlanner.Policy roads,long now,long maximumAge,long seed){
        Objects.requireNonNull(home);Objects.requireNonNull(roadRecipe);Objects.requireNonNull(land);Objects.requireNonNull(roads);var kinds=Map.copyOf(terrain);
        if(home.purpose()!=Purpose.HOME)throw new IllegalArgumentException("Housing expansion requires a home plan");
        var infill=ConstructionSitePlanner.select(home,world,need,survey,envelope,now,maximumAge,seed,land,kinds);
        if(infill.selected().isPresent())return result(Status.EXISTING_ACCESS,null,infill,Map.of(),0,0,0,infill.budgetLimited());
        if(infill.status()!=ConstructionSitePlanner.Status.NO_SAFE_SITE)return result(Status.INVALID_EVIDENCE,null,infill,Map.of(),0,0,0,infill.budgetLimited());
        if(survey.protections().size()==ConstructionSitePlanner.MAX_EXCLUSIONS)return result(Status.BUDGET_LIMIT,null,infill,Map.of(),0,0,0,true);
        var starts=survey.cells().entrySet().stream().filter(e->envelope.bounds().contains(e.getKey())&&kinds.get(e.getKey())==ConstructionLandPolicy.Kind.NATURAL_LOOKING_GROUND
                &&e.getValue().stableSupport()&&!e.getValue().protectedWork()).map(Map.Entry::getKey)
                .sorted(Comparator.comparingLong((Position p)->distance(p,need)).thenComparingLong(p->mix(seed,p)).thenComparing(Comparator.naturalOrder())).toList();
        var rejected=new EnumMap<Rejection,Integer>(Rejection.class);var choices=new ArrayList<Expansion>();
        int candidates=0,cellReviews=0,routeNodes=0;boolean limited=starts.size()>MAX_ENTRANCES;
        outer:for(var entrance:sampleEntrances(starts,seed))for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)){
            var transform=new Transform(rotation,mirror);var anchor=home.anchors().stream().filter(a->a.role()==Role.ROAD).findFirst().orElseThrow(()->new IllegalArgumentException("Home needs an access anchor"));
            var offset=transform.apply(anchor.at(),home.size());Position origin;
            Set<Position> required;
            try{origin=new Position(Math.subtractExact(entrance.x(),offset.x()),Math.subtractExact(entrance.y(),offset.y()),Math.subtractExact(entrance.z(),offset.z()));required=land.requiredCells(home,transform,origin);}
            catch(ArithmeticException|IllegalArgumentException outside){reject(rejected,Rejection.HOME_UNSUITABLE);continue;}
            if(candidates==MAX_CANDIDATES||required.size()>MAX_CELL_REVIEWS-cellReviews){limited=true;break outer;}
            candidates++;cellReviews+=required.size();
            var cells=new HashMap<Position,Cell>();var landCells=new HashMap<Position,ConstructionLandPolicy.Cell>();boolean complete=true;
            for(var at:required){
                var cell=survey.cells().get(at);var kind=kinds.get(at);
                if(cell==null||kind==null||(kind==ConstructionLandPolicy.Kind.AIR)!=cell.air()||(kind==ConstructionLandPolicy.Kind.ROAD)!=cell.road()){complete=false;continue;}
                boolean protectedWork=cell.protectedWork()||survey.protections().stream().anyMatch(b->b.contains(at));
                cells.put(at,new Cell(cell.air(),cell.stableSupport(),at.equals(entrance)||cell.road(),protectedWork));
                landCells.put(at,new ConstructionLandPolicy.Cell(at.equals(entrance)?ConstructionLandPolicy.Kind.ROAD:kind,protectedWork));
            }
            var footprint=ConstructionPreview.footprint(home,transform,origin);
            boolean entity=survey.entities().stream().anyMatch(b->overlaps(b,footprint));
            var assessment=ConstructionPreview.assess(home,transform,world,origin,new Observation(world,origin,survey.capturedMillis(),complete,entity,cells),envelope,now,maximumAge);
            if(!assessment.suitable()||!land.review(home,transform,origin,landCells,complete).suitable()){reject(rejected,Rejection.HOME_UNSUITABLE);continue;}
            if(routeNodes==MAX_ROUTE_NODES){limited=true;break outer;}
            var exclusions=new ArrayList<>(survey.protections());exclusions.add(new Bounds(new Position(footprint.minimum().x(),Math.subtractExact(origin.y(),1),footprint.minimum().z()),footprint.maximum()));
            var routeSurvey=new ConstructionSitePlanner.Survey(world,survey.capturedMillis(),survey.complete(),survey.protectionsComplete(),survey.cells(),exclusions,survey.entities());
            var ground=new Bounds(new Position(envelope.bounds().minimum().x(),entrance.y(),envelope.bounds().minimum().z()),new Position(envelope.bounds().maximum().x(),entrance.y(),envelope.bounds().maximum().z()));
            if(((long)ground.maximum().x()-ground.minimum().x()+1)*((long)ground.maximum().z()-ground.minimum().z()+1)>4096){limited=true;break outer;}
            var route=LocalRoadAccessPlanner.select(world,entrance,ground,routeSurvey,kinds,new LocalRoadAccessPlanner.Policy(roads.maximumNewSteps(),Math.min(roads.maximumReviewed(),MAX_ROUTE_NODES-routeNodes)),now,maximumAge,seed);
            routeNodes+=route.reviewed();
            if(route.status()!=LocalRoadAccessPlanner.Status.READY){reject(rejected,Rejection.ROUTE_UNAVAILABLE);continue;}
            var frozen=LocalRoadDesign.freeze(route,roadRecipe);
            if(frozen.isEmpty()){reject(rejected,Rejection.ROUTE_UNAVAILABLE);continue;}
            try{RoadGroundReview.requiredCells(frozen.get().geometry(),land.buildingBuffer());}
            catch(IllegalArgumentException tooLarge){reject(rejected,Rejection.NATIVE_CLEARANCE_LIMIT);continue;}
            choices.add(new Expansion(origin,transform,home.fingerprint(),frozen.get()));
        }
        var selected=choices.stream().min(Comparator.comparingInt((Expansion p)->p.road().template().blockCount()).thenComparingLong(p->distance(p.homeOrigin(),need))
                .thenComparingLong(p->mix(seed,p.homeOrigin())).thenComparing(Expansion::homeOrigin).thenComparingInt(p->p.homeTransform().rotation().ordinal()).thenComparing(p->p.homeTransform().mirror())).orElse(null);
        return result(selected!=null?Status.READY:limited?Status.BUDGET_LIMIT:Status.NO_SAFE_PLAN,selected,infill,rejected,candidates,cellReviews,routeNodes,limited);
    }
    private static Result result(Status status,Expansion expansion,ConstructionSitePlanner.Result infill,Map<Rejection,Integer> rejected,int candidates,int cells,int nodes,boolean limited){return new Result(status,Optional.ofNullable(expansion),infill,rejected,candidates,cells,nodes,limited);}
    private static void reject(Map<Rejection,Integer> rejected,Rejection reason){rejected.merge(reason,1,Integer::sum);}
    /** Keep nearby choices, but do not spend every bounded review on the same crowded town center. */
    private static List<Position> sampleEntrances(List<Position> sorted,long seed){
        if(sorted.size()<=MAX_ENTRANCES)return sorted;
        var selected=new LinkedHashSet<Position>(sorted.subList(0,8));int remaining=sorted.size()-8,slots=MAX_ENTRANCES-8;
        int offset=(int)Math.floorMod(seed,remaining);
        for(int i=0;i<slots;i++)selected.add(sorted.get(8+(int)((offset+(long)i*remaining/slots)%remaining)));
        return List.copyOf(selected);
    }
    private static long distance(Position a,Position b){return Math.abs((long)a.x()-b.x())+Math.abs((long)a.y()-b.y())+Math.abs((long)a.z()-b.z());}
    private static long mix(long seed,Position p){long n=seed^((long)p.x()*0x9e3779b97f4a7c15L)^((long)p.z()*0xc2b2ae3d27d4eb4fL)^p.y();return (n^(n>>>30))*0xbf58476d1ce4e5b9L;}
    private static boolean overlaps(Bounds a,Bounds b){return a.minimum().x()<=b.maximum().x()&&a.maximum().x()>=b.minimum().x()&&a.minimum().y()<=b.maximum().y()&&a.maximum().y()>=b.minimum().y()&&a.minimum().z()<=b.maximum().z()&&a.maximum().z()>=b.minimum().z();}
}
