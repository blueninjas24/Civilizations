package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind;
import java.util.*;

/** Current safe entrance-to-marker access only; no ownership, inventory, staffing or capacity authority. */
public final class WorkplaceInspection {
    private WorkplaceInspection(){}
    public enum Issue { WRONG_WORLD, LAYOUT_CHANGED, STALE_OR_FUTURE, INCOMPLETE, MARKER_OUTSIDE,
        MARKER_UNVERIFIED, ENTRANCE_UNSAFE, HAZARD, WORK_UNREACHABLE, WORK_UNCOVERED }
    public record Evidence(Observation structure,Position marker,Optional<Kind> kind,boolean markerVerified){
        public Evidence{Objects.requireNonNull(structure);Objects.requireNonNull(marker);Objects.requireNonNull(kind);}
    }
    /** Conservative inspection preference, not an enabled production rule. */
    public record Policy(boolean requireCoveredWork){public static Policy prototype(){return new Policy(true);}}
    public static final class Assessment {
        private final Evidence evidence;private final Policy policy;private final Set<Issue> issues;private final Optional<Position> workPosition;
        private Assessment(Evidence evidence,Policy policy,Set<Issue> issues,Position workPosition){this.evidence=evidence;this.policy=policy;this.issues=Set.copyOf(issues);this.workPosition=Optional.ofNullable(workPosition);}
        public Evidence evidence(){return evidence;}public Policy policy(){return policy;}public Set<Issue> issues(){return issues;}
        public Optional<Position> workPosition(){return workPosition;}public boolean suitable(){return issues.isEmpty()&&workPosition.isPresent();}
    }
    public static Assessment inspect(BuildingLayout layout,Evidence evidence,long now,long maximumAge,Policy policy){
        Objects.requireNonNull(layout);Objects.requireNonNull(evidence);Objects.requireNonNull(policy);
        if(layout.kind()!=BuildingLayout.Kind.WORKPLACE)throw new IllegalArgumentException("Workplace inspection requires a workplace declaration");
        if(now<0||maximumAge<1||maximumAge>60_000)throw new IllegalArgumentException("Invalid workplace inspection freshness");
        var observation=evidence.structure();var issues=EnumSet.noneOf(Issue.class);
        if(!layout.world().equals(observation.world()))issues.add(Issue.WRONG_WORLD);
        if(!layout.fingerprint().equals(observation.layoutHash()))issues.add(Issue.LAYOUT_CHANGED);
        if(observation.capturedMillis()>now||now-observation.capturedMillis()>maximumAge)issues.add(Issue.STALE_OR_FUTURE);
        if(!observation.complete()||!observation.cells().keySet().equals(DwellingInspection.requiredCells(layout))
           ||observation.cells().values().stream().anyMatch(c->c.type()==Type.UNKNOWN))issues.add(Issue.INCOMPLETE);
        if(!layout.structure().contains(evidence.marker()))issues.add(Issue.MARKER_OUTSIDE);
        if(!evidence.markerVerified()||evidence.kind().isEmpty())issues.add(Issue.MARKER_UNVERIFIED);
        // The four full-block markers cannot also be observed as air, bedding or roof-only material.
        var markerCell=observation.cells().get(evidence.marker());
        if(markerCell!=null&&markerCell.type()!=Type.SUPPORT&&markerCell.type()!=Type.BLOCKED)issues.add(Issue.MARKER_UNVERIFIED);
        if(!issues.isEmpty())return new Assessment(evidence,policy,issues,null);
        var cells=observation.cells();
        if(cells.values().stream().anyMatch(c->c.type()==Type.HAZARD))issues.add(Issue.HAZARD);
        if(!standable(layout.entrance(),cells))issues.add(Issue.ENTRANCE_UNSAFE);
        if(!issues.isEmpty())return new Assessment(evidence,policy,issues,null);
        record Column(int x,int z){}
        var covers=new HashMap<Column,Integer>();
        if(policy.requireCoveredWork())cells.forEach((p,c)->{if(c.type()==Type.SUPPORT||c.type()==Type.COVER)covers.merge(new Column(p.x(),p.z()),p.y(),Math::max);});
        // Each coordinate can enter once, bounded by the already limited whole-structure observation.
        var pending=new ArrayDeque<Position>();var seen=new HashSet<Position>();pending.add(layout.entrance());seen.add(layout.entrance());
        boolean uncovered=false;
        while(!pending.isEmpty()){
            var at=pending.removeFirst();
            if(adjacent(at,evidence.marker())){
                Integer roof=covers.get(new Column(at.x(),at.z()));
                if(!policy.requireCoveredWork()||roof!=null&&(long)roof>=(long)at.y()+2)return new Assessment(evidence,policy,issues,at);
                uncovered=true;
            }
            for(int[] offset:HORIZONTAL)for(int dy=-1;dy<=1;dy++){
                var next=shift(at,offset[0],dy,offset[1]);
                if(next==null||seen.contains(next)||!standable(next,cells))continue;
                if(dy!=0&&!passable(shift(dy>0?at:next,0,2,0),cells))continue;
                seen.add(next);pending.addLast(next);
            }
        }
        issues.add(uncovered?Issue.WORK_UNCOVERED:Issue.WORK_UNREACHABLE);return new Assessment(evidence,policy,issues,null);
    }
    private static final int[][] HORIZONTAL={{1,0},{-1,0},{0,1},{0,-1}};
    private static boolean adjacent(Position a,Position b){return a.y()==b.y()&&Math.abs((long)a.x()-b.x())+Math.abs((long)a.z()-b.z())==1;}
    private static boolean standable(Position p,Map<Position,Cell> cells){return passable(p,cells)&&passable(shift(p,0,1,0),cells)&&type(shift(p,0,-1,0),cells)==Type.SUPPORT;}
    private static boolean passable(Position p,Map<Position,Cell> cells){return type(p,cells)==Type.PASSABLE;}
    private static Type type(Position p,Map<Position,Cell> cells){var cell=p==null?null:cells.get(p);return cell==null?Type.UNKNOWN:cell.type();}
    private static Position shift(Position p,int x,int y,int z){if(p==null)return null;try{return p.plus(new Position(x,y,z));}catch(ArithmeticException overflow){return null;}}
}
