package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.PenScan;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperPenSourceTest {
    record Position(int x,int y,int z) {}
    final UUID world=new UUID(0,1);final Map<Position,BlockData> blocks=new HashMap<>();
    BlockData data(Material material){return (BlockData)Proxy.newProxyInstance(BlockData.class.getClassLoader(),new Class<?>[]{BlockData.class},(p,m,a)->{if(m.getName().equals("getMaterial"))return material;throw new AssertionError(m.getName());});}
    BlockData fence(Set<BlockFace> faces){return (Fence)Proxy.newProxyInstance(Fence.class.getClassLoader(),new Class<?>[]{Fence.class},(p,m,a)->switch(m.getName()){case "getMaterial"->Material.OAK_FENCE;case "getFaces"->faces;case "isWaterlogged"->false;default->throw new AssertionError(m.getName());});}
    BlockData gate(){return (Gate)Proxy.newProxyInstance(Gate.class.getClassLoader(),new Class<?>[]{Gate.class},(p,m,a)->switch(m.getName()){case "getMaterial"->Material.OAK_FENCE_GATE;case "getFacing"->BlockFace.NORTH;default->throw new AssertionError(m.getName());});}
    ChunkSnapshot snapshot(int cx,int cz){return (ChunkSnapshot)Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),new Class<?>[]{ChunkSnapshot.class},(p,m,a)->switch(m.getName()){
        case "getX"->cx;case "getZ"->cz;case "getWorldName"->"Pen fixture";
        case "getBlockData"->{int x=(int)a[0],y=(int)a[1],z=(int)a[2];assertTrue(x>=0&&x<16&&z>=0&&z<16);yield blocks.getOrDefault(new Position(cx*16+x,y,cz*16+z),data(y==63?Material.STONE:Material.AIR));}
        default->throw new AssertionError(m.getName());});}
    PaperPenSource source(){return new PaperPenSource(world,64,-64,320,Map.of(new PaperPenSource.Chunk(0,0),new PaperPenSource.Captured(world,snapshot(0,0))));}
    @Test void flatFootingAndTwoAirBlocksCertifyInteriorButVerticalEscapeDoesNot() {
        var point=new PenScan.Column(8,8);assertEquals(PenScan.Cell.INTERIOR,source().cell(point));
        blocks.put(new Position(8,63,8),data(Material.AIR));assertEquals(PenScan.Cell.OPEN,source().cell(point));
        blocks.put(new Position(8,63,8),data(Material.STONE));blocks.put(new Position(8,65,8),data(Material.STONE));assertEquals(PenScan.Cell.UNKNOWN,source().cell(point));
    }
    @Test void solidTwoBlockBuildingEdgeIsBarrierButShortWallAndSlabsAreUnknown() {
        var point=new PenScan.Column(8,8);blocks.put(new Position(8,64,8),data(Material.OAK_PLANKS));assertEquals(PenScan.Cell.UNKNOWN,source().cell(point));
        blocks.put(new Position(8,65,8),data(Material.OAK_PLANKS));assertEquals(PenScan.Cell.BARRIER,source().cell(point));
        blocks.put(new Position(8,64,8),data(Material.OAK_SLAB));assertEquals(PenScan.Cell.UNKNOWN,source().cell(point));
    }
    @Test void gateRequiresRealConnectionsAndDoesNotDependOnOpenState() {
        var point=new PenScan.Column(8,8);blocks.put(new Position(8,64,8),gate());
        blocks.put(new Position(7,64,8),fence(Set.of(BlockFace.EAST,BlockFace.WEST)));blocks.put(new Position(9,64,8),fence(Set.of(BlockFace.EAST,BlockFace.WEST)));
        // Gate proxy rejects isOpen: openness must not affect structural protection.
        assertEquals(PenScan.Cell.BARRIER,source().cell(point));blocks.remove(new Position(9,64,8));assertEquals(PenScan.Cell.UNKNOWN,source().cell(point));
    }
    @Test void isolatedOrDisconnectedFenceCannotBecomeAnImaginaryBoundary() {
        var point=new PenScan.Column(8,8);blocks.put(new Position(8,64,8),fence(Set.of(BlockFace.EAST)));assertEquals(PenScan.Cell.UNKNOWN,source().cell(point));
        blocks.put(new Position(8,64,8),fence(Set.of(BlockFace.EAST,BlockFace.WEST)));assertEquals(PenScan.Cell.UNKNOWN,source().cell(point));
        blocks.put(new Position(7,64,8),fence(Set.of(BlockFace.EAST,BlockFace.WEST)));blocks.put(new Position(9,64,8),fence(Set.of(BlockFace.EAST,BlockFace.WEST)));assertEquals(PenScan.Cell.BARRIER,source().cell(point));
    }
    @Test void missingChunkIsUnknownAndNegativeCoordinatesUseTheirActualSnapshot() {
        assertEquals(PenScan.Cell.UNKNOWN,source().cell(new PenScan.Column(-1,-1)));
        var negative=new PaperPenSource(world,64,-64,320,Map.of(new PaperPenSource.Chunk(-1,-1),new PaperPenSource.Captured(world,snapshot(-1,-1))));
        assertEquals(PenScan.Cell.INTERIOR,negative.cell(new PenScan.Column(-1,-1)));assertEquals(PenScan.Cell.UNKNOWN,negative.cell(new PenScan.Column(0,0)));
    }
    @Test void mixedWorldWrongChunkOrUnsafeHeightCannotProducePenEvidence() {
        assertThrows(IllegalArgumentException.class,()->new PaperPenSource(new UUID(0,2),64,-64,320,Map.of(new PaperPenSource.Chunk(0,0),new PaperPenSource.Captured(world,snapshot(0,0)))));
        assertThrows(IllegalArgumentException.class,()->new PaperPenSource(world,64,-64,320,Map.of(new PaperPenSource.Chunk(1,0),new PaperPenSource.Captured(world,snapshot(0,0)))));
        assertThrows(IllegalArgumentException.class,()->new PaperPenSource(world,-64,-64,320,Map.of()));
    }
}
