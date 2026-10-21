package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import java.util.*;

/** First-wave infill beside known local roads. No block reads, inferred permissions, grading or spending. */
public final class ConstructionSitePlanner {
    public static final int MAX_ROADS=64,MAX_CANDIDATES=256,MAX_EXCLUSIONS=128,MAX_CELL_REVIEWS=65_536;
    public enum Status { READY,NO_PERMISSION,WRONG_WORLD,STALE,INCOMPLETE,PROTECTION_UNKNOWN,NO_ROAD,NO_SAFE_SITE }
    public record Survey(UUID world,long capturedMillis,boolean complete,boolean protectionsComplete,
                         Map<Position,Cell> cells,List<Bounds> protections,List<Bounds> entities) {
        public Survey {
            Objects.requireNonNull(world);cells=Map.copyOf(cells);protections=List.copyOf(protections);entities=List.copyOf(entities);
            if(capturedMillis<0||cells.size()>ConstructionPreview.MAX_OBSERVATION_CELLS||protections.size()>MAX_EXCLUSIONS||entities.size()>MAX_EXCLUSIONS)
                throw new IllegalArgumentException("Site survey exceeds bounded limits");
        }
    }
    public record Candidate(Position origin,Transform transform,long distance,long tieBreak,Assessment assessment,Set<ConstructionLandPolicy.Finding> landFindings) {
        public Candidate{Objects.requireNonNull(origin);Objects.requireNonNull(transform);Objects.requireNonNull(assessment);landFindings=Set.copyOf(landFindings);}
        public Candidate(Position origin,Transform transform,long distance,long tieBreak,Assessment assessment){this(origin,transform,distance,tieBreak,assessment,Set.of());}
    }
    public record Result(Status status,Optional<Candidate> selected,List<Candidate> reviewed,int roadsConsidered,boolean budgetLimited) {
        public Result{Objects.requireNonNull(status);selected=Objects.requireNonNull(selected);reviewed=List.copyOf(reviewed);}
    }
    private record Key(Position origin,Transform transform){}
    private ConstructionSitePlanner(){}
    public static Result select(ConstructionTemplate template,UUID world,Position need,Survey survey,ApprovedArea approval,
                                long now,long maximumAgeMillis,long seed) {
        return select(template,world,need,survey,approval,now,maximumAgeMillis,seed,null,Map.of());
    }
    /** Buffered selection screens every orientation before ranking; it still cannot grant land/write authority. */
    public static Result select(ConstructionTemplate template,UUID world,Position need,Survey survey,ApprovedArea approval,
                                long now,long maximumAgeMillis,long seed,ConstructionLandPolicy land,
                                Map<Position,ConstructionLandPolicy.Kind> terrain) {
        Objects.requireNonNull(template);Objects.requireNonNull(world);Objects.requireNonNull(need);Objects.requireNonNull(survey);
        terrain=Map.copyOf(terrain);
        if(terrain.size()>ConstructionPreview.MAX_OBSERVATION_CELLS||land==null&&!terrain.isEmpty())throw new IllegalArgumentException("Invalid buffered terrain evidence");
        if(now<0||maximumAgeMillis<1||maximumAgeMillis>3_600_000)throw new IllegalArgumentException("Invalid site survey time limits");
        if(approval==null||!approval.world().equals(world))return blocked(Status.NO_PERMISSION);
        if(!survey.world().equals(world))return blocked(Status.WRONG_WORLD);
        if(survey.capturedMillis()>now||now-survey.capturedMillis()>maximumAgeMillis)return blocked(Status.STALE);
        if(!survey.complete())return blocked(Status.INCOMPLETE);
        if(!survey.protectionsComplete())return blocked(Status.PROTECTION_UNKNOWN);
        var roads=survey.cells().entrySet().stream().filter(e->e.getValue().road()&&approval.bounds().contains(e.getKey())&&!protectedAt(e.getKey(),survey))
                .map(Map.Entry::getKey).sorted(Comparator.comparingLong((Position p)->distance(p,need)).thenComparing(Comparator.naturalOrder())).toList();
        if(roads.isEmpty())return blocked(Status.NO_ROAD);
        var reviewed=new ArrayList<Candidate>();var keys=new HashSet<Key>();int roadCount=0,cellReviews=0;boolean limited=roads.size()>MAX_ROADS;
        var protectedCells=new HashSet<Position>();for(var p:survey.cells().keySet())if(protectedAt(p,survey))protectedCells.add(p);
        outer:for(var road:roads.subList(0,Math.min(roads.size(),MAX_ROADS))) {
            roadCount++;
            for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)) {
                var transform=new Transform(rotation,mirror);
                for(var anchor:template.anchors())if(anchor.role()==Role.ROAD) {
                    var offset=transform.apply(anchor.at(),template.size());Position origin;
                    try{origin=new Position(Math.subtractExact(road.x(),offset.x()),Math.subtractExact(road.y(),offset.y()),Math.subtractExact(road.z(),offset.z()));}
                    catch(ArithmeticException outside){continue;}
                    if(!keys.add(new Key(origin,transform)))continue;
                    if(reviewed.size()==MAX_CANDIDATES){limited=true;break outer;}
                    Set<Position> required;Bounds footprint;
                    try{required=land==null?ConstructionPreview.requiredCells(template,transform,origin):land.requiredCells(template,transform,origin);footprint=ConstructionPreview.footprint(template,transform,origin);}
                    catch(ArithmeticException outside){continue;}
                    if(required.size()>MAX_CELL_REVIEWS-cellReviews){limited=true;break outer;}cellReviews+=required.size();
                    var cells=new TreeMap<Position,Cell>();var landCells=new HashMap<Position,ConstructionLandPolicy.Cell>();boolean complete=true;
                    for(var p:required) {
                        var cell=survey.cells().get(p);if(cell==null){complete=false;continue;}
                        cells.put(p,new Cell(cell.air(),cell.stableSupport(),cell.road(),cell.protectedWork()||protectedCells.contains(p)));
                        if(land!=null){
                            var kind=terrain.get(p);
                            if(kind==null||(kind==ConstructionLandPolicy.Kind.AIR)!=cell.air()||(kind==ConstructionLandPolicy.Kind.ROAD)!=cell.road()
                                    ||kind==ConstructionLandPolicy.Kind.NATURAL_LOOKING_GROUND&&!cell.stableSupport())complete=false;
                            if(kind!=null)landCells.put(p,new ConstructionLandPolicy.Cell(kind,cell.protectedWork()||protectedCells.contains(p)));
                        }
                    }
                    boolean entity=survey.entities().stream().anyMatch(bounds->overlaps(bounds,footprint));
                    var observation=new Observation(world,origin,survey.capturedMillis(),complete,entity,cells);
                    var assessment=ConstructionPreview.assess(template,transform,world,origin,observation,approval,now,maximumAgeMillis);
                    Set<ConstructionLandPolicy.Finding> landFindings=Set.of();
                    if(land!=null){
                        landFindings=land.review(template,transform,origin,landCells,complete).findings();
                        if(!landFindings.isEmpty()){var issues=EnumSet.noneOf(Issue.class);issues.addAll(assessment.issues());issues.add(Issue.LAND_UNSUITABLE);assessment=new Assessment(assessment.footprint(),assessment.plannedBlocks(),issues);}
                    }
                    reviewed.add(new Candidate(origin,transform,distance(origin,need),mix(seed,origin,transform),assessment,landFindings));
                }
            }
        }
        var selected=reviewed.stream().filter(c->c.assessment().suitable()).min(Comparator.comparingLong(Candidate::distance)
                .thenComparing((a,b)->Long.compareUnsigned(a.tieBreak(),b.tieBreak())).thenComparing(Candidate::origin)
                .thenComparingInt(c->c.transform().rotation().ordinal()).thenComparing(c->c.transform().mirror()));
        return new Result(selected.isPresent()?Status.READY:Status.NO_SAFE_SITE,selected,reviewed,roadCount,limited);
    }
    private static boolean protectedAt(Position p,Survey survey){return survey.protections().stream().anyMatch(b->b.contains(p));}
    private static boolean overlaps(Bounds a,Bounds b){return a.minimum().x()<=b.maximum().x()&&a.maximum().x()>=b.minimum().x()
            &&a.minimum().y()<=b.maximum().y()&&a.maximum().y()>=b.minimum().y()&&a.minimum().z()<=b.maximum().z()&&a.maximum().z()>=b.minimum().z();}
    private static long distance(Position a,Position b){return Math.abs((long)a.x()-b.x())+Math.abs((long)a.y()-b.y())+Math.abs((long)a.z()-b.z());}
    private static long mix(long seed,Position p,Transform t){long n=seed^((long)p.x()*0x9e3779b97f4a7c15L)^((long)p.z()*0xc2b2ae3d27d4eb4fL)^p.y()^t.rotation().ordinal()^(t.mirror()?0x165667b19e3779f9L:0);n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    private static Result blocked(Status status){return new Result(status,Optional.empty(),List.of(),0,false);}
}
