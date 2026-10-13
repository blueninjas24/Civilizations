package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.PenScan;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import java.util.*;

/** Immutable loaded-chunk evidence for simple flat pens. Never calls or loads a World.
 * Capture snapshots on the main thread. Unsupported ramps/walls/doors stay UNKNOWN. */
public final class PaperPenSource implements PenScan.Source {
    public record Chunk(int x,int z) {}
    public record Captured(UUID world,ChunkSnapshot snapshot) {
        public Captured {Objects.requireNonNull(world);Objects.requireNonNull(snapshot);}
    }
    private static final Set<Material> CUBES=Set.of(Material.STONE,Material.COBBLESTONE,Material.STONE_BRICKS,
            Material.DIRT,Material.GRASS_BLOCK,Material.DEEPSLATE,Material.COBBLED_DEEPSLATE,
            Material.SAND,Material.RED_SAND,Material.CLAY,Material.GRAVEL,
            Material.OAK_PLANKS,Material.SPRUCE_PLANKS,Material.BIRCH_PLANKS,Material.DARK_OAK_PLANKS,
            Material.ACACIA_PLANKS,Material.JUNGLE_PLANKS,Material.MANGROVE_PLANKS,Material.CHERRY_PLANKS);
    private final Map<Chunk,ChunkSnapshot> snapshots;
    private final int standingY,minimumY,maximumY;
    public PaperPenSource(UUID world,int standingY,int minimumY,int maximumY,Map<Chunk,Captured> captured) {
        Objects.requireNonNull(world);
        if(minimumY>=maximumY || (long)standingY-1<minimumY || (long)standingY+1>=maximumY || captured.size()>81)
            throw new IllegalArgumentException("Invalid bounded pen snapshot heights/count");
        var copy=new HashMap<Chunk,ChunkSnapshot>();String name=null;
        for(var entry:captured.entrySet()) {
            var key=Objects.requireNonNull(entry.getKey());var chunk=Objects.requireNonNull(entry.getValue());var snapshot=chunk.snapshot();
            if(!world.equals(chunk.world())||key.x()!=snapshot.getX()||key.z()!=snapshot.getZ())throw new IllegalArgumentException("Pen snapshot identity mismatch");
            if(name==null)name=Objects.requireNonNull(snapshot.getWorldName());
            if(!name.equals(snapshot.getWorldName()))throw new IllegalArgumentException("Mixed pen snapshot worlds");
            copy.put(key,snapshot);
        }
        snapshots=Map.copyOf(copy);this.standingY=standingY;this.minimumY=minimumY;this.maximumY=maximumY;
    }
    private BlockData data(long x,int y,long z) {
        if(x<Integer.MIN_VALUE||x>Integer.MAX_VALUE||z<Integer.MIN_VALUE||z>Integer.MAX_VALUE||y<minimumY||y>=maximumY)return null;
        var snapshot=snapshots.get(new Chunk(Math.floorDiv((int)x,16),Math.floorDiv((int)z,16)));
        return snapshot==null?null:snapshot.getBlockData(Math.floorMod((int)x,16),y,Math.floorMod((int)z,16));
    }
    private static boolean air(BlockData data){return data!=null&&(data.getMaterial()==Material.AIR||data.getMaterial()==Material.CAVE_AIR||data.getMaterial()==Material.VOID_AIR);}
    private static boolean cube(BlockData data){return data!=null&&CUBES.contains(data.getMaterial());}
    private static boolean wet(BlockData data){return data instanceof Waterlogged w&&w.isWaterlogged();}
    private boolean connected(long x,long z,BlockFace direction) {
        var neighbor=data(x+direction.getModX(),standingY,z+direction.getModZ());
        if(neighbor==null||wet(neighbor))return false;
        if(neighbor instanceof Fence f)return f.getFaces().contains(direction.getOppositeFace());
        if(neighbor instanceof Gate g)return (g.getFacing()==BlockFace.NORTH||g.getFacing()==BlockFace.SOUTH)
                ? direction==BlockFace.EAST||direction==BlockFace.WEST : direction==BlockFace.NORTH||direction==BlockFace.SOUTH;
        return cube(neighbor)&&cube(data(x+direction.getModX(),standingY+1,z+direction.getModZ()));
    }
    @Override public PenScan.Cell cell(PenScan.Column position) {
        long x=position.x(),z=position.z();var floor=data(x,standingY-1,z);var feet=data(x,standingY,z);var head=data(x,standingY+1,z);
        if(floor==null||feet==null||head==null)return PenScan.Cell.UNKNOWN;
        if(cube(feet)&&cube(head))return PenScan.Cell.BARRIER;
        if(!cube(floor))return air(floor)?PenScan.Cell.OPEN:PenScan.Cell.UNKNOWN;
        if(air(feet)&&air(head))return PenScan.Cell.INTERIOR;
        if(!air(head)||wet(feet))return PenScan.Cell.UNKNOWN;
        if(feet instanceof Fence fence) {
            var faces=fence.getFaces();
            if(faces.size()<2||faces.stream().anyMatch(f->f!=BlockFace.NORTH&&f!=BlockFace.SOUTH&&f!=BlockFace.EAST&&f!=BlockFace.WEST))return PenScan.Cell.UNKNOWN;
            return faces.stream().allMatch(f->connected(x,z,f))?PenScan.Cell.BARRIER:PenScan.Cell.UNKNOWN;
        }
        if(feet instanceof Gate gate) {
            if(gate.getFacing()!=BlockFace.NORTH&&gate.getFacing()!=BlockFace.SOUTH&&gate.getFacing()!=BlockFace.EAST&&gate.getFacing()!=BlockFace.WEST)return PenScan.Cell.UNKNOWN;
            boolean horizontal=gate.getFacing()==BlockFace.NORTH||gate.getFacing()==BlockFace.SOUTH;
            return connected(x,z,horizontal?BlockFace.EAST:BlockFace.NORTH)&&connected(x,z,horizontal?BlockFace.WEST:BlockFace.SOUTH)
                    ?PenScan.Cell.BARRIER:PenScan.Cell.UNKNOWN;
        }
        return PenScan.Cell.UNKNOWN;
    }
}
