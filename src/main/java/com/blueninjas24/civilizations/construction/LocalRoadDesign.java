package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Freeze a local corridor using an editable flat path recipe. No terrain replacement or financial authority. */
public final class LocalRoadDesign {
    private static final Set<String> SURFACES=Set.of("minecraft:cobblestone","minecraft:stone","minecraft:stone_bricks",
            "minecraft:andesite","minecraft:diorite","minecraft:granite","minecraft:cobbled_deepslate");
    public record Design(Position origin,Position connection,ConstructionTemplate template,String recipeHash) {
        public Design {
            Objects.requireNonNull(origin);Objects.requireNonNull(connection);Objects.requireNonNull(template);Objects.requireNonNull(recipeHash);
            if(!recipeHash.matches("[0-9a-f]{64}")||!template.id().equals("local_road_access")||template.purpose()!=Purpose.PATH
                    ||template.size().y()!=1||template.stages().size()!=1||template.blockCount()>64||template.anchors().size()!=1
                    ||template.anchors().getFirst().role()!=Role.ROAD||!origin.plus(template.anchors().getFirst().at()).equals(connection))
                throw new IllegalArgumentException("Invalid frozen local road design");
            var blocks=template.stages().getFirst().blocks();var end=template.anchors().getFirst().at();
            if(blocks.containsKey(end)||!SURFACES.containsAll(blocks.values()))throw new IllegalArgumentException("Invalid road surface or retained endpoint");
            var remaining=new HashSet<>(blocks.keySet());var queue=new ArrayDeque<Position>();queue.add(end);
            while(!queue.isEmpty()){
                var at=queue.removeFirst();for(var candidate:new ArrayList<>(remaining))if(at.y()==candidate.y()
                        &&Math.abs((long)at.x()-candidate.x())+Math.abs((long)at.z()-candidate.z())==1){remaining.remove(candidate);queue.addLast(candidate);}
            }
            if(!remaining.isEmpty())throw new IllegalArgumentException("Road design is disconnected from retained endpoint");
        }
    }
    private LocalRoadDesign(){}
    public static Optional<Design> freeze(LocalRoadAccessPlanner.Result route,ConstructionTemplate recipe){
        Objects.requireNonNull(route);Objects.requireNonNull(recipe);
        if(route.status()!=LocalRoadAccessPlanner.Status.READY)throw new IllegalArgumentException("Road design requires a complete safe route");
        if(recipe.purpose()!=Purpose.PATH||recipe.size().y()!=1||recipe.stages().size()!=1)
            throw new IllegalArgumentException("Local road recipe must be one flat path stage");
        var source=recipe.stages().getFirst();var states=new HashSet<>(source.blocks().values());
        if(states.size()!=1||!SURFACES.contains(states.iterator().next()))throw new IllegalArgumentException("Local road recipe requires one supported solid paving material");
        var points=route.corridor();int fresh=route.newSurfaceCells();
        if(points.isEmpty()||fresh<0||fresh>64||points.size()!=fresh+1||new HashSet<>(points).size()!=points.size())
            throw new IllegalArgumentException("Invalid bounded complete road geometry");
        for(int i=1;i<points.size();i++){
            var a=points.get(i-1);var b=points.get(i);
            if(a.y()!=b.y()||Math.abs((long)a.x()-b.x())+Math.abs((long)a.z()-b.z())!=1)
                throw new IllegalArgumentException("Road corridor must be flat cardinal contiguous cells");
        }
        if(fresh==0)return Optional.empty(); // Existing road is not a new paid project.
        var newCells=points.subList(0,fresh);int x=newCells.stream().mapToInt(Position::x).min().orElseThrow(),z=newCells.stream().mapToInt(Position::z).min().orElseThrow();
        var origin=new Position(x,points.getFirst().y(),z);var blocks=new TreeMap<Position,String>();
        for(var p:newCells)blocks.put(relative(p,origin),states.iterator().next());
        var size=new Size(Math.addExact(Math.subtractExact(newCells.stream().mapToInt(Position::x).max().orElseThrow(),x),1),1,
                Math.addExact(Math.subtractExact(newCells.stream().mapToInt(Position::z).max().orElseThrow(),z),1));
        var materials=new TreeMap<Commodity,Long>();source.materials().forEach((commodity,units)->materials.put(commodity,scaled(units,fresh,source.blocks().size())));
        var connection=points.getLast();var previous=points.get(fresh-1);
        var facing=connection.x()>previous.x()?Direction.EAST:connection.x()<previous.x()?Direction.WEST:connection.z()>previous.z()?Direction.SOUTH:Direction.NORTH;
        var template=new ConstructionTemplate("local_road_access",recipe.revision(),"Local village access",recipe.attribution(),Purpose.PATH,size,
                List.of(new Stage("surface",scaled(source.work(),fresh,source.blocks().size()),materials,blocks)),
                List.of(new Anchor(Role.ROAD,relative(connection,origin),facing)));
        return Optional.of(new Design(origin,connection,template,recipe.fingerprint()));
    }
    private static Position relative(Position p,Position origin){return new Position(Math.subtractExact(p.x(),origin.x()),Math.subtractExact(p.y(),origin.y()),Math.subtractExact(p.z(),origin.z()));}
    private static long scaled(long units,int cells,int batch){return Math.addExact(Math.multiplyExact(units,cells),batch-1)/batch;}
}
