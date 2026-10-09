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

class OrePlacementTrackerTest {
    private static final UUID WORLD=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->switch(m.getName()) {
        case "getUID"->WORLD;case "getName"->"Fixture";default->throw new AssertionError(m.getName());
    });
    private Block block(Material material,int x) {
        return (Block)Proxy.newProxyInstance(Block.class.getClassLoader(),new Class<?>[]{Block.class},(p,m,a)->switch(m.getName()) {
            case "getWorld"->world;case "getType"->material;case "getX"->x;case "getY"->-60;case "getZ"->-1;
            default->throw new AssertionError(m.getName());
        });
    }
    private static BlockPlaceEvent placed(Block block,boolean canBuild) {
        return new BlockPlaceEvent(block,null,block,null,null,canBuild,EquipmentSlot.HAND);
    }
    @Test void successfulPlacementCapturesOnlyImmutableCoordinatesAndExpandedOreCatalog() {
        var captured=new AtomicReference<Collection<Position>>();
        var tracker=new OrePlacementTracker((world,name,positions,millis)-> {
            assertEquals(WORLD,world);assertEquals("Fixture",name);assertTrue(millis>0);captured.set(positions);
            return CompletableFuture.completedFuture(positions.size());
        },reason->fail(reason),Logger.getLogger("PlacementTrackerTest"));
        tracker.onPlace(placed(block(Material.DEEPSLATE_COAL_ORE,-1),true));
        assertEquals(List.of(new Position(-1,-60,-1)),captured.get());
        assertThrows(UnsupportedOperationException.class,()->captured.get().clear());
        assertTrue(tracker.diagnostic().contains("pending: 0"));
        for(var material:List.of(Material.COPPER_ORE,Material.DIAMOND_ORE,Material.REDSTONE_ORE,Material.ANCIENT_DEBRIS))
            assertTrue(OrePlacementTracker.ore(material));
        assertFalse(OrePlacementTracker.ore(Material.COAL_BLOCK));
    }
    @Test void cancelledForbiddenAndNonOrePlacementsDoNotQueueWrites() {
        var tracker=new OrePlacementTracker((w,n,p,t)-> {throw new AssertionError("Unexpected write");},r->fail(r),Logger.getLogger("PlacementTrackerTest"));
        var cancelled=placed(block(Material.COAL_ORE,0),true);cancelled.setCancelled(true);tracker.onPlace(cancelled);
        tracker.onPlace(placed(block(Material.COAL_ORE,0),false));tracker.onPlace(placed(block(Material.STONE,0),true));
    }
    @Test void multiPlacementDeduplicatesCurrentOreBlocksRatherThanOldAirStates() {
        var ore=block(Material.IRON_ORE,0);var state=(BlockState)Proxy.newProxyInstance(BlockState.class.getClassLoader(),
                new Class<?>[]{BlockState.class},(p,m,a)-> {if(m.getName().equals("getBlock"))return ore;throw new AssertionError(m.getName());});
        var saved=new AtomicReference<Collection<Position>>();
        var tracker=new OrePlacementTracker((w,n,p,t)-> {saved.set(p);return CompletableFuture.completedFuture(p.size());},r->fail(r),Logger.getLogger("PlacementTrackerTest"));
        tracker.onPlace(new BlockMultiPlaceEvent(List.of(state,state),ore,null,null,true,EquipmentSlot.HAND));
        assertEquals(List.of(new Position(0,-60,-1)),saved.get());
    }
    @Test void asynchronousSaveFailureAndSynchronousFailureMarkGapWithoutCancellingPlacement() {
        var issue=new AtomicReference<String>();var pending=new CompletableFuture<Integer>();
        var tracker=new OrePlacementTracker((w,n,p,t)->pending,issue::set,Logger.getLogger("PlacementTrackerTest"));
        var event=placed(block(Material.COAL_ORE,0),true);tracker.onPlace(event);
        assertTrue(tracker.diagnostic().contains("pending: 1"));pending.completeExceptionally(new IllegalStateException("injected"));
        assertNotNull(issue.get());assertFalse(event.isCancelled());assertTrue(tracker.diagnostic().contains("pending: 0"));
        issue.set(null);var rejected=new OrePlacementTracker((w,n,p,t)-> {throw new IllegalStateException("injected");},issue::set,Logger.getLogger("PlacementTrackerTest"));
        rejected.onPlace(event);assertNotNull(issue.get());
    }
    @Test void overloadedCaptureFailsClosedWithoutKeepingUnlimitedFutures() {
        var pending=new CompletableFuture<Integer>();var issue=new AtomicReference<String>();
        var tracker=new OrePlacementTracker((w,n,p,t)->pending,issue::set,Logger.getLogger("PlacementTrackerTest"));
        var event=placed(block(Material.COAL_ORE,0),true);
        for(int i=0;i<257;i++)tracker.onPlace(event);
        assertNotNull(issue.get());assertTrue(tracker.diagnostic().contains("pending: 256"));
        pending.complete(1);assertTrue(tracker.diagnostic().contains("pending: 0"));
    }
}
