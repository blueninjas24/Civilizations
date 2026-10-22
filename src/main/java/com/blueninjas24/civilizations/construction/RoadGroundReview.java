package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.util.*;

/** Read-only exact-ground evidence for future road intents. Never grants replacement authority. */
public final class RoadGroundReview {
    private static final Set<String> GROUND=Set.of("minecraft:grass_block[snowy=false]","minecraft:grass_block[snowy=true]",
            "minecraft:dirt","minecraft:coarse_dirt","minecraft:rooted_dirt","minecraft:podzol[snowy=false]","minecraft:podzol[snowy=true]","minecraft:mycelium[snowy=false]","minecraft:mycelium[snowy=true]");
    private static final Set<String> AIR=Set.of("minecraft:air","minecraft:cave_air","minecraft:void_air");
    public enum Issue { WRONG_WORLD, STALE, INCOMPLETE, PROTECTED, GROUND_CHANGED, HEADROOM_BLOCKED, CONNECTION_CHANGED }
    public record Observation(UUID world,long capturedMillis,boolean complete,Map<Position,String> states,Set<Position> protectedCells){
        public Observation{Objects.requireNonNull(world);states=Map.copyOf(states);protectedCells=Set.copyOf(protectedCells);if(capturedMillis<0||states.size()>256||protectedCells.size()>256)throw new IllegalArgumentException("Invalid road ground frame");}
    }
    public record Result(Set<Issue> issues,Map<Position,String> beforeStates){
        public Result{issues=Set.copyOf(issues);beforeStates=Map.copyOf(beforeStates);}
        public boolean suitable(){return issues.isEmpty();}
    }
    public static Set<String> naturalGroundStates(){return GROUND;}
    private RoadGroundReview(){}
    public static Result inspect(UUID world,LocalRoadDesign.Design design,Observation observation,long now){
        return inspect(world,design.geometry(),observation,now,Map.of(),0);
    }
    public static Result inspect(UUID world,LocalRoadDesign.Design design,Observation observation,long now,Map<Position,String> acknowledged){return inspect(world,design.geometry(),observation,now,acknowledged,0);}
    public static Set<Position> requiredCells(LocalRoadDesign.Geometry design,int buffer){
        if(buffer<0||buffer>16)throw new IllegalArgumentException("Invalid road buffer");var cells=new TreeSet<Position>();var centers=new ArrayList<Position>();
        design.template().stages().getFirst().blocks().keySet().forEach(p->centers.add(design.origin().plus(p)));centers.add(design.connection());
        for(var center:centers){
            for(int z=-buffer;z<=buffer;z++)for(int x=-buffer;x<=buffer;x++)for(int y=0;y<2;y++){
                cells.add(center.plus(new Position(x,y,z)));if(cells.size()>255)throw new IllegalArgumentException("Road evidence exceeds256-read native writer cap");
            }
            cells.add(center.plus(new Position(0,2,0)));if(cells.size()>255)throw new IllegalArgumentException("Road evidence exceeds256-read native writer cap");
        }
        return Collections.unmodifiableSet(cells);
    }
    public static Result inspect(UUID world,LocalRoadDesign.Geometry design,Observation observation,long now,Map<Position,String> acknowledged,int buffer){
        var required=requiredCells(design,buffer);
        Objects.requireNonNull(world);Objects.requireNonNull(design);Objects.requireNonNull(observation);acknowledged=Map.copyOf(acknowledged);if(now<0)throw new IllegalArgumentException("Invalid road review time");
        var issues=EnumSet.noneOf(Issue.class);var before=new TreeMap<Position,String>();
        if(!world.equals(observation.world()))issues.add(Issue.WRONG_WORLD);
        if(now<observation.capturedMillis()||now-observation.capturedMillis()>5000)issues.add(Issue.STALE);
        if(!observation.complete())issues.add(Issue.INCOMPLETE);
        for(var stage:design.template().stages())for(var offset:stage.blocks().keySet()){
            var at=design.origin().plus(offset);String state=observation.states().get(at);
            String prior=acknowledged.get(at);
            if(state==null)issues.add(Issue.INCOMPLETE);
            else if(prior!=null){if(!prior.equals(stage.blocks().get(offset))||!state.equals(prior))issues.add(Issue.GROUND_CHANGED);}
            else if(!GROUND.contains(state))issues.add(Issue.GROUND_CHANGED);else before.put(at,state);
            for(int y=0;y<3;y++){
                var cell=at.plus(new Position(0,y,0));if(observation.protectedCells().contains(cell))issues.add(Issue.PROTECTED);
                if(!observation.states().containsKey(cell))issues.add(Issue.INCOMPLETE);
                else if(y>0&&!AIR.contains(observation.states().get(cell)))issues.add(Issue.HEADROOM_BLOCKED);
            }
        }
        if(!observation.states().containsKey(design.connection()))issues.add(Issue.INCOMPLETE);
        else if(!"minecraft:dirt_path".equals(observation.states().get(design.connection())))issues.add(Issue.CONNECTION_CHANGED);
        for(int y=0;y<3;y++){
            var cell=design.connection().plus(new Position(0,y,0));
            if(observation.protectedCells().contains(cell))issues.add(Issue.PROTECTED);
            if(!observation.states().containsKey(cell))issues.add(Issue.INCOMPLETE);
            else if(y>0&&!AIR.contains(observation.states().get(cell)))issues.add(Issue.HEADROOM_BLOCKED);
        }
        for(var at:required){
            String state=observation.states().get(at);if(state==null){issues.add(Issue.INCOMPLETE);continue;}
            if(observation.protectedCells().contains(at))issues.add(Issue.PROTECTED);
            if(at.y()>design.origin().y()){if(!AIR.contains(state))issues.add(Issue.HEADROOM_BLOCKED);}
            else if(acknowledged.containsKey(at)){if(!state.equals(acknowledged.get(at)))issues.add(Issue.GROUND_CHANGED);}
            else if(!at.equals(design.connection())&&!GROUND.contains(state)&&!"minecraft:dirt_path".equals(state))issues.add(Issue.GROUND_CHANGED);
        }
        return new Result(issues,issues.isEmpty()?before:Map.of());
    }
}
