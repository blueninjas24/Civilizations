package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.database.LivestockBirthProjections.Job;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.persistence.PersistentDataContainer;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperLivestockNewbornsTest {
    java.util.function.Predicate<Material> support=m->m==Material.STONE||m==Material.MAGMA_BLOCK||m==Material.CACTUS;
    java.util.function.Predicate<Material> air=m->m==Material.AIR;
    boolean loaded=true,entitiesLoaded=true,border=true,occupied;Material floor=Material.STONE,head=Material.AIR;int blockReads,chunkReads;
    @SuppressWarnings("unchecked") <T>T proxy(Class<T> type,BiFunction<String,Object[],Object> call) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->switch(m.getName()) {
            case "hashCode"->System.identityHashCode(p);case "equals"->p==a[0];case "toString"->"Fixture "+type.getSimpleName();default->call.apply(m.getName(),a);
        });
    }
    World world=proxy(World.class,(name,args)->switch(name) {
        case "getMinHeight"->-64;case "getMaxHeight"->320;case "isChunkLoaded"->loaded;
        case "getChunkAt"->{chunkReads++;yield proxy(Chunk.class,(m,a)->{assertEquals("isEntitiesLoaded",m);return entitiesLoaded;});}
        case "getWorldBorder"->proxy(WorldBorder.class,(m,a)->{assertEquals("isInside",m);return border;});
        case "getBlockAt"->{blockReads++;int y=(Integer)args[1];yield proxy(Block.class,(m,a)->{assertEquals("getType",m);return y==63?floor:head;});}
        case "getNearbyEntities"->occupied?List.of(proxy(Animals.class,(m,a)->null)):List.of();
        default->throw new AssertionError("Unexpected world operation "+name);
    });
    @Test void safeLoadedFullGroundFindsABoundedPlacementWithoutChangingTerrain() {
        var found=PaperLivestockNewborns.safeLocation(world,new Position(0,64,0),support,air);assertNotNull(found);assertEquals(64,found.getBlockY());assertTrue(blockReads<=3);assertEquals(1,chunkReads);
    }
    @Test void missingOrUnloadedEntityChunksNeverReadBlocksOrRequestAChunkLoad() {
        loaded=false;assertNull(PaperLivestockNewborns.safeLocation(world,new Position(0,64,0),support,air));assertEquals(0,blockReads);assertEquals(0,chunkReads);
        loaded=true;entitiesLoaded=false;assertFalse(PaperLivestockNewborns.clear(new Location(world,0.5,64,0.5),support,air));assertEquals(0,blockReads);assertEquals(1,chunkReads);
    }
    @Test void dangerousFloorsObstructionsWorldEdgesAndHeightBoundsRefusePlacement() {
        for(var unsafe:List.of(Material.MAGMA_BLOCK,Material.CACTUS,Material.CAMPFIRE,Material.SOUL_CAMPFIRE,Material.LAVA,Material.WATER,Material.AIR)) {
            floor=unsafe;assertFalse(PaperLivestockNewborns.clear(new Location(world,0.5,64,0.5),support,air));
        }
        floor=Material.STONE;head=Material.STONE;assertFalse(PaperLivestockNewborns.clear(new Location(world,0.5,64,0.5),support,air));head=Material.AIR;border=false;assertFalse(PaperLivestockNewborns.clear(new Location(world,0.5,64,0.5),support,air));border=true;
        assertFalse(PaperLivestockNewborns.clear(new Location(world,0.5,-64,0.5),support,air));assertFalse(PaperLivestockNewborns.clear(new Location(world,0.5,319,0.5),support,air));assertFalse(PaperLivestockNewborns.clear(new Location(null,0.5,64,0.5),support,air));
        assertNull(PaperLivestockNewborns.safeLocation(world,new Position(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE),support,air));
    }
    @Test void occupiedLivingSpaceIsNotUsedAndSearchStopsAtItsFiniteBudget() {
        occupied=true;assertNull(PaperLivestockNewborns.safeLocation(world,new Position(0,64,0),support,air));assertTrue(chunkReads<=27);assertTrue(blockReads<=81);
    }
    @Test void savedMaturityControlsAgeWithoutChangingInvulnerabilityAiOrPlayerAnimals() {
        var birth=LivestockBirth.batch(new UUID(0,1),1,1,12).getFirst();var values=new HashMap<Object,Object>();var adult=new boolean[]{true};var ageLocks=new int[]{0};
        var pdc=proxy(PersistentDataContainer.class,(m,a)->{if(m.equals("set")){values.put(a[0],a[2]);return null;}throw new AssertionError(m);});
        var animal=proxy(Animals.class,(m,a)->switch(m) {
            case "setAgeLock"->{assertEquals(true,a[0]);ageLocks[0]++;yield null;}
            case "getPersistentDataContainer"->pdc;case "isAdult"->adult[0];case "setBaby"->{adult[0]=false;yield null;}
            case "setAdult"->{adult[0]=true;yield null;}default->throw new AssertionError("Unexpected body mutation "+m);
        });
        var young=new Job(birth,LivestockHerd.Species.COW,new UUID(0,2),new Position(0,64,0),12,Optional.empty());PaperLivestockNewborns.applyAge(animal,young);assertFalse(adult[0]);assertEquals(birth.id().toString(),values.get(PaperLivestockNewborns.BIRTH));
        PaperLivestockNewborns.applyAge(animal,new Job(birth,young.species(),young.world(),young.home(),13,Optional.empty()));assertTrue(adult[0]);assertEquals(2,ageLocks[0]);
    }
}
