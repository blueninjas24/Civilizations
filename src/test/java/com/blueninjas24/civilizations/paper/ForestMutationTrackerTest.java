package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.block.*;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForestMutationTrackerTest {
    private static final UUID WORLD=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Set<Material> TREE_MATERIALS=EnumSet.of(Material.OAK_LOG,Material.OAK_LEAVES,Material.OAK_SAPLING);
    private final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->switch(m.getName()) {
        case "getUID"->WORLD;case "getName"->"Forest fixture";default->throw new AssertionError(m.getName());
    });

    private Block block(Material material,int x) {
        return (Block)Proxy.newProxyInstance(Block.class.getClassLoader(),new Class<?>[]{Block.class},(p,m,a)->switch(m.getName()) {
            case "getWorld"->world;case "getType"->material;case "getX"->x;case "getY"->64;case "getZ"->-2;
            default->throw new AssertionError(m.getName());
        });
    }

    private static BlockState state(Block block,Material resultingOrOldType) {
        return (BlockState)Proxy.newProxyInstance(BlockState.class.getClassLoader(),new Class<?>[]{BlockState.class},(p,m,a)->switch(m.getName()) {
            case "getBlock"->block;case "getType"->resultingOrOldType;default->throw new AssertionError(m.getName());
        });
    }

    @Test void multiPlaceUsesEachCoordinateActualResultAndExplosionFiltersBeforeBudgeting() {
        var placements=new AtomicReference<Collection<Position>>();var removals=new AtomicReference<Collection<Position>>();
        var tracker=new ForestMutationTracker((w,n,p,r,t,reason)->{
            assertEquals(WORLD,w);assertEquals("Forest fixture",n);placements.set(p);removals.set(r);
            return CompletableFuture.completedFuture(0);
        },reason->fail(reason),Logger.getLogger("ForestMutationTrackerTest"),TREE_MATERIALS::contains);

        var overwrittenLog=block(Material.DIRT,3);var addedLog=block(Material.OAK_LOG,4);
        var event=new BlockMultiPlaceEvent(List.of(state(overwrittenLog,Material.OAK_LOG),state(addedLog,Material.AIR)),
                block(Material.DIRT,3),null,null,true,EquipmentSlot.HAND);
        tracker.onPlace(event);
        assertEquals(List.of(new Position(4,64,-2)),placements.get());
        assertEquals(List.of(new Position(3,64,-2)),removals.get());

        placements.set(null);removals.set(null);
        var affected=new ArrayList<Block>();
        for(int x=0;x<513;x++)affected.add(block(Material.DIRT,x));
        affected.add(block(Material.OAK_LEAVES,900));
        tracker.onBlockExplosion(new BlockExplodeEvent(block(Material.TNT,0),null,affected,1,ExplosionResult.DESTROY));
        assertTrue(placements.get().isEmpty());
        assertEquals(List.of(new Position(900,64,-2)),removals.get());
        assertFalse(tracker.diagnostic().contains("capture gap"));
    }

    @Test void growthUsesNewBlockStateMaterialAndFailedSaveMarksGapWithoutCancellingMutation() {
        var issue=new AtomicReference<String>();var save=new CompletableFuture<Integer>();var placements=new AtomicReference<Collection<Position>>();
        var tracker=new ForestMutationTracker((w,n,p,r,t,reason)->{placements.set(p);return save;},issue::set,
                Logger.getLogger("ForestGrowthTest"),TREE_MATERIALS::contains);
        var newSapling=state(block(Material.AIR,12),Material.OAK_SAPLING);
        tracker.growth(List.of(newSapling),"test growth");
        assertEquals(List.of(new Position(12,64,-2)),placements.get());
        assertTrue(tracker.diagnostic().contains("pending: 1"));
        save.completeExceptionally(new IllegalStateException("injected save failure"));
        assertNotNull(issue.get());assertTrue(tracker.diagnostic().contains("pending: 0"));
    }
}
