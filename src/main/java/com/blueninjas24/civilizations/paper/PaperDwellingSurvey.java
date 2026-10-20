package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.BuildingLayout;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.DwellingInspection;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import java.util.*;

/** Incremental read-only snapshot, never chunk loading or housing authority.
 * A future owner adapter must budget surveys globally and revalidate before activating housing. */
public final class PaperDwellingSurvey {
    public static final int MAX_CELLS_PER_ADVANCE=256;
    private final UUID worldId;
    private final BuildingLayout.Bounds bounds;
    private final String hash;
    private final long started;
    private int x,y,z,remaining;
    private final Map<Position,Cell> cells=new HashMap<>();
    private boolean incomplete;
    private Observation finished;

    public PaperDwellingSurvey(BuildingLayout layout,long now) {
        this(layout.world(),layout.structure(),layout.fingerprint(),now);
        if(layout.kind()!=BuildingLayout.Kind.HOUSING)throw new IllegalArgumentException("Housing survey required");
    }
    public static PaperDwellingSurvey forHomeDeclaration(UUID world,BuildingLayout.Bounds structure,long now) {
        return new PaperDwellingSurvey(world,structure,com.blueninjas24.civilizations.construction.HomeDeclaration.regionHash(world,structure),now);
    }
    private PaperDwellingSurvey(UUID world,BuildingLayout.Bounds bounds,String hash,long now) {
        requireMainThread();worldId=Objects.requireNonNull(world);this.bounds=Objects.requireNonNull(bounds);this.hash=Objects.requireNonNull(hash);
        if(now<0||bounds.volume()>BuildingLayout.MAX_STRUCTURE_VOLUME)throw new IllegalArgumentException("Invalid housing survey time/volume");
        started=now;x=bounds.min().x();y=bounds.min().y();z=bounds.min().z();remaining=Math.toIntExact(bounds.volume());
    }
    /** Caller schedules subsequent advances; this class registers no task and writes no blocks. */
    public Optional<Observation> advance(int budget,long now) {
        requireMainThread();
        if(budget<1 || budget>MAX_CELLS_PER_ADVANCE || now<0)throw new IllegalArgumentException("Invalid dwelling survey budget/time");
        if(finished!=null)return Optional.of(finished);
        World world=Bukkit.getWorld(worldId);
        if(world==null || now<started || now-started>30_000){incomplete=true;remaining=0;}
        if(world!=null) {
            var b=bounds;var border=world.getWorldBorder();
            if(!border.isInside(new Location(world,b.min().x(),b.min().y(),b.min().z()))
                    || !border.isInside(new Location(world,b.max().x(),b.max().y(),b.max().z()))) {incomplete=true;remaining=0;}
        }
        while(world!=null && budget-->0 && remaining>0) {
            var p=next();
            if(p.y()<world.getMinHeight() || p.y()>=world.getMaxHeight() || !world.isChunkLoaded(p.x()>>4,p.z()>>4)) {
                incomplete=true;continue;
            }
            cells.put(p,classify(world.getBlockAt(p.x(),p.y(),p.z())));
        }
        if(remaining>0)return Optional.empty();
        if(world!=null) {
            for(int x=bounds.min().x()>>4;x<=bounds.max().x()>>4;x++)for(int z=bounds.min().z()>>4;z<=bounds.max().z()>>4;z++)
                if(!world.isChunkLoaded(x,z))incomplete=true;
        }
        finished=new Observation(worldId,hash,started,!incomplete,cells);return Optional.of(finished);
    }
    private Position next() {
        var p=new Position(x,y,z);
        if(--remaining>0) {
            if(x<bounds.max().x())x++;
            else {x=bounds.min().x();if(z<bounds.max().z())z++;else{z=bounds.min().z();y++;}}
        }
        return p;
    }
    private static final Set<Material> HAZARDS=Set.of(Material.WATER,Material.LAVA,Material.FIRE,Material.SOUL_FIRE,
            Material.CACTUS,Material.MAGMA_BLOCK,Material.CAMPFIRE,Material.SOUL_CAMPFIRE,Material.POWDER_SNOW,
            Material.SWEET_BERRY_BUSH,Material.WITHER_ROSE,Material.TNT);
    private static Cell classify(Block block) {
        Material material=block.getType();var data=block.getBlockData();
        if(HAZARDS.contains(material) || data instanceof Waterlogged w && w.isWaterlogged())return Cell.of(Type.HAZARD);
        if(data instanceof Bed bed)return Cell.bed(bed.getPart()==Bed.Part.HEAD,material.getKey().toString(),Direction.valueOf(bed.getFacing().name()));
        if(material.isAir() || data instanceof Door && Tag.WOODEN_DOORS.isTagged(material))return Cell.of(Type.PASSABLE);
        // Unsupported partial shapes, fluids, vegetation and iron/copper doors stay blocked.
        if(material.isSolid() && material.isOccluding() && !material.hasGravity())return Cell.of(Type.SUPPORT);
        // Roof-only evidence: does not pretend partial shapes are supported walking routes.
        if(Tag.STAIRS.isTagged(material)||Tag.SLABS.isTagged(material)||material==Material.GLASS
                ||material==Material.TINTED_GLASS||material.name().endsWith("_STAINED_GLASS"))return Cell.of(Type.COVER);
        return Cell.of(Type.BLOCKED);
    }
    private static void requireMainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Dwelling block reads require server thread");}
}
