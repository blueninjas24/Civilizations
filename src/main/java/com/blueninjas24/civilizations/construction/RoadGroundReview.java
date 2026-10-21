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
    private RoadGroundReview(){}
    public static Result inspect(UUID world,LocalRoadDesign.Design design,Observation observation,long now){
        Objects.requireNonNull(world);Objects.requireNonNull(design);Objects.requireNonNull(observation);if(now<0)throw new IllegalArgumentException("Invalid road review time");
        var issues=EnumSet.noneOf(Issue.class);var before=new TreeMap<Position,String>();
        if(!world.equals(observation.world()))issues.add(Issue.WRONG_WORLD);
        if(now<observation.capturedMillis()||now-observation.capturedMillis()>5000)issues.add(Issue.STALE);
        if(!observation.complete())issues.add(Issue.INCOMPLETE);
        for(var stage:design.template().stages())for(var offset:stage.blocks().keySet()){
            var at=design.origin().plus(offset);String state=observation.states().get(at);
            if(state==null)issues.add(Issue.INCOMPLETE);else if(!GROUND.contains(state))issues.add(Issue.GROUND_CHANGED);else before.put(at,state);
            for(int y=0;y<3;y++){
                var cell=at.plus(new Position(0,y,0));if(observation.protectedCells().contains(cell))issues.add(Issue.PROTECTED);
                if(!observation.states().containsKey(cell))issues.add(Issue.INCOMPLETE);
                else if(y>0&&!AIR.contains(observation.states().get(cell)))issues.add(Issue.HEADROOM_BLOCKED);
            }
        }
        if(!observation.states().containsKey(design.connection()))issues.add(Issue.INCOMPLETE);
        else if(!"minecraft:dirt_path".equals(observation.states().get(design.connection())))issues.add(Issue.CONNECTION_CHANGED);
        if(observation.protectedCells().contains(design.connection()))issues.add(Issue.PROTECTED);
        return new Result(issues,issues.isEmpty()?before:Map.of());
    }
}
