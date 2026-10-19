package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.util.*;

/** Pure bounded site review. A successful review is not permission to debit goods or write blocks. */
public final class ConstructionPreview {
    private ConstructionPreview() {}
    public record Bounds(Position minimum,Position maximum) {
        public Bounds {Objects.requireNonNull(minimum);Objects.requireNonNull(maximum);if(minimum.x()>maximum.x()||minimum.y()>maximum.y()||minimum.z()>maximum.z())throw new IllegalArgumentException("Inverted bounds");}
        public boolean contains(Position p){return p.x()>=minimum.x()&&p.x()<=maximum.x()&&p.y()>=minimum.y()&&p.y()<=maximum.y()&&p.z()>=minimum.z()&&p.z()<=maximum.z();}
        public boolean contains(Bounds other){return contains(other.minimum())&&contains(other.maximum());}
    }
    /** Caller must establish explicit development permission; this class never infers ownership from blocks. */
    public record ApprovedArea(UUID id,UUID world,Bounds bounds) {public ApprovedArea{Objects.requireNonNull(id);Objects.requireNonNull(world);Objects.requireNonNull(bounds);}}
    public record Cell(boolean air,boolean stableSupport,boolean road,boolean protectedWork) {}
    public record Observation(UUID world,Position origin,long observedMillis,boolean complete,boolean entityObstruction,Map<Position,Cell> cells) {
        public Observation {Objects.requireNonNull(world);Objects.requireNonNull(origin);cells=Map.copyOf(cells);if(observedMillis<0||cells.size()>MAX_OBSERVATION_CELLS)throw new IllegalArgumentException("Invalid site observation");}
    }
    public enum Issue { NO_APPROVED_AREA, WRONG_WORLD, STALE_OR_FUTURE, INCOMPLETE, ENTITY_PRESENT, PROTECTED, OBSTRUCTED, UNSUPPORTED, ROAD_REQUIRED }
    public record Assessment(Bounds footprint,int plannedBlocks,Set<Issue> issues) {
        public Assessment{issues=Set.copyOf(issues);}
        public boolean suitable(){return issues.isEmpty();}
    }
    public static final int MAX_OBSERVATION_CELLS=ConstructionTemplate.MAX_BLOCKS+8192;
    public static Bounds footprint(ConstructionTemplate template,Transform transform,Position origin) {
        var size=transform.size(template.size());return new Bounds(origin,origin.plus(new Position(size.x()-1,size.y()-1,size.z()-1)));
    }
    public static Set<Position> requiredCells(ConstructionTemplate template,Transform transform,Position origin) {
        var size=transform.size(template.size());var result=new TreeSet<Position>();
        for(int y=-1;y<size.y();y++)for(int z=0;z<size.z();z++)for(int x=0;x<size.x();x++)result.add(origin.plus(new Position(x,y,z)));
        for(var anchor:template.anchors())if(anchor.role()==Role.ROAD)result.add(origin.plus(transform.apply(anchor.at(),template.size())));
        if(result.size()>MAX_OBSERVATION_CELLS)throw new IllegalArgumentException("Site observation exceeds bounded budget");
        return Collections.unmodifiableSet(result);
    }
    public static Assessment assess(ConstructionTemplate template,Transform transform,UUID world,Position origin,
                                    Observation observation,ApprovedArea approval,long now,long maximumAgeMillis) {
        Objects.requireNonNull(observation);Objects.requireNonNull(world);
        if(now<0||maximumAgeMillis<1||maximumAgeMillis>3_600_000)throw new IllegalArgumentException("Invalid preview time limits");
        var footprint=footprint(template,transform,origin);var issues=EnumSet.noneOf(Issue.class);
        if(approval==null||!approval.world().equals(world)||!approval.bounds().contains(footprint))issues.add(Issue.NO_APPROVED_AREA);
        if(!observation.world().equals(world))issues.add(Issue.WRONG_WORLD);
        if(observation.observedMillis()>now||now-observation.observedMillis()>maximumAgeMillis)issues.add(Issue.STALE_OR_FUTURE);
        var required=requiredCells(template,transform,origin);
        if(!observation.complete()||!observation.origin().equals(origin)||!observation.cells().keySet().containsAll(required))issues.add(Issue.INCOMPLETE);
        if(observation.entityObstruction())issues.add(Issue.ENTITY_PRESENT);
        // Partial/wrong-world frames must not turn missing evidence into apparent safe air or solid ground.
        if(!issues.contains(Issue.WRONG_WORLD)&&!issues.contains(Issue.INCOMPLETE)) {
            var size=transform.size(template.size());
            for(var position:required) {
                var cell=observation.cells().get(position);
                if(cell.protectedWork())issues.add(Issue.PROTECTED);
                if(footprint.contains(position)&&!cell.air())issues.add(Issue.OBSTRUCTED);
                if(position.y()==Math.subtractExact(origin.y(),1)&&position.x()>=origin.x()&&position.x()<=footprint.maximum().x()
                        &&position.z()>=origin.z()&&position.z()<=footprint.maximum().z()&&!cell.stableSupport())issues.add(Issue.UNSUPPORTED);
            }
            if(template.purpose()!=Purpose.PATH)for(var anchor:template.anchors())if(anchor.role()==Role.ROAD
                    &&!observation.cells().get(origin.plus(transform.apply(anchor.at(),template.size()))).road())issues.add(Issue.ROAD_REQUIRED);
        }
        return new Assessment(footprint,template.blockCount(),issues);
    }
}
