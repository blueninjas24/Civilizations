package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import org.bukkit.*;
import org.bukkit.util.BoundingBox;
import java.util.*;

/** Two loaded-only passes under one caller budget; no block changes, chunk loading or land permission. */
public final class PaperHomeSiteSurvey {
    public record Frame(Bounds search,ConstructionSitePlanner.Survey survey,Map<Position,ConstructionLandPolicy.Kind> terrain,Map<Position,String> states){
        public Frame{Objects.requireNonNull(search);Objects.requireNonNull(survey);terrain=Map.copyOf(terrain);states=Map.copyOf(states);}
        public Frame(Bounds search,ConstructionSitePlanner.Survey survey,Map<Position,ConstructionLandPolicy.Kind> terrain){this(search,survey,terrain,Map.of());}
    }
    private static final Set<Material> NATURAL=Set.of(Material.GRASS_BLOCK,Material.DIRT,Material.COARSE_DIRT,Material.ROOTED_DIRT,Material.PODZOL,Material.MYCELIUM);
    private final UUID worldId;private final Bounds search;private final long started;private long captured;
    private final List<Position> positions;private int cursor;private boolean verifying,incomplete;private Frame finished;
    private final Map<Position,Cell> cells=new HashMap<>();private final Map<Position,ConstructionLandPolicy.Kind> terrain=new HashMap<>();
    private final Map<Position,String> states=new HashMap<>();
    public PaperHomeSiteSurvey(UUID world,Position need,int radius,long now){
        this(world,need,radius,4,now);
    }
    public PaperHomeSiteSurvey(UUID world,Position need,int radius,int templateHeight,long now){
        requireMain();worldId=Objects.requireNonNull(world);Objects.requireNonNull(need);
        if(now<0)throw new IllegalArgumentException("Invalid bounded home survey time");
        search=bounds(need,radius,templateHeight);started=now;captured=now;
        var list=new ArrayList<Position>();
        for(int y=-3;y<templateHeight;y++)for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++)list.add(need.plus(new Position(x,y,z)));
        positions=List.copyOf(list);
    }
    static Bounds bounds(Position need,int radius,int height){
        Objects.requireNonNull(need);if(radius<8||radius>12||height<1||height>64||(long)(radius*2+1)*(radius*2+1)*(height+3)>ConstructionPreview.MAX_OBSERVATION_CELLS)
            throw new IllegalArgumentException("Selected home exceeds bounded site survey coverage");
        // Settlements are usually centered at bed height, one above the road. Include the entire roof.
        return new Bounds(need.plus(new Position(-radius,-3,-radius)),need.plus(new Position(radius,height-1,radius)));
    }
    public Optional<Frame> advance(int budget,long now){
        requireMain();if(budget<1||budget>256||now<0)throw new IllegalArgumentException("Invalid home survey budget/time");
        if(finished!=null)return Optional.of(finished);World world=Bukkit.getWorld(worldId);
        if(world==null||now<started||now-started>30_000)return Optional.of(finish(null,now,false));
        if(!world.getWorldBorder().isInside(new Location(world,search.minimum().x(),search.minimum().y(),search.minimum().z()))
                ||!world.getWorldBorder().isInside(new Location(world,search.maximum().x(),search.maximum().y(),search.maximum().z())))return Optional.of(finish(null,now,false));
        while(budget-->0&&cursor<positions.size()){
            var at=positions.get(cursor++);
            if(at.y()<world.getMinHeight()||at.y()>=world.getMaxHeight()||!world.isChunkLoaded(at.x()>>4,at.z()>>4)){incomplete=true;continue;}
            var block=world.getBlockAt(at.x(),at.y(),at.z());var material=block.getType();String state=block.getBlockData().getAsString();
            var cell=new Cell(material.isAir(),PaperConstructionPreview.stableSupport(material),material==Material.DIRT_PATH,false);
            var kind=material.isAir()?ConstructionLandPolicy.Kind.AIR:NATURAL.contains(material)?ConstructionLandPolicy.Kind.NATURAL_LOOKING_GROUND
                    :material==Material.DIRT_PATH?ConstructionLandPolicy.Kind.ROAD:ConstructionLandPolicy.Kind.OTHER;
            if(verifying){if(!cell.equals(cells.get(at))||kind!=terrain.get(at)||!state.equals(states.get(at)))incomplete=true;}
            else{cells.put(at,cell);terrain.put(at,kind);states.put(at,state);}
        }
        if(cursor<positions.size())return Optional.empty();
        if(!verifying&&!incomplete){verifying=true;cursor=0;captured=now;return Optional.empty();}
        return Optional.of(finish(world,now,!incomplete));
    }
    private Frame finish(World world,long now,boolean complete){
        var entities=new ArrayList<Bounds>();
        if(complete&&world!=null){
            for(int x=search.minimum().x()>>4;x<=search.maximum().x()>>4;x++)for(int z=search.minimum().z()>>4;z<=search.maximum().z()>>4;z++)if(!world.isChunkLoaded(x,z))complete=false;
            if(complete)for(var entity:world.getNearbyEntities(new BoundingBox(search.minimum().x(),search.minimum().y(),search.minimum().z(),search.maximum().x()+1.0,search.maximum().y()+1.0,search.maximum().z()+1.0))){
                if(entities.size()==ConstructionSitePlanner.MAX_EXCLUSIONS){complete=false;break;}
                var b=entity.getBoundingBox();entities.add(new Bounds(new Position((int)Math.floor(b.getMinX()),(int)Math.floor(b.getMinY()),(int)Math.floor(b.getMinZ())),
                        new Position((int)Math.floor(b.getMaxX()),(int)Math.floor(b.getMaxY()),(int)Math.floor(b.getMaxZ()))));
            }
        }
        finished=new Frame(search,new ConstructionSitePlanner.Survey(worldId,captured,complete,complete,cells,List.of(),entities),terrain,states);return finished;
    }
    private static void requireMain(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Home site survey requires the server thread");}
}
