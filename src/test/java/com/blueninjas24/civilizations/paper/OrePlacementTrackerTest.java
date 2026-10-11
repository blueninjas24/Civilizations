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
import org.bukkit.event.entity.EntityExplodeEvent;
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
        var tracker=new OrePlacementTracker((world,name,positions,removals,millis,reason)-> {
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
        var tracker=new OrePlacementTracker((w,n,p,removed,t,reason)-> {throw new AssertionError("Unexpected write");},r->fail(r),Logger.getLogger("PlacementTrackerTest"));
        var cancelled=placed(block(Material.COAL_ORE,0),true);cancelled.setCancelled(true);tracker.onPlace(cancelled);
        tracker.onPlace(placed(block(Material.COAL_ORE,0),false));tracker.onPlace(placed(block(Material.STONE,0),true));
    }
    @Test void multiPlacementDeduplicatesCurrentOreBlocksRatherThanOldAirStates() {
        var ore=block(Material.IRON_ORE,0);var state=(BlockState)Proxy.newProxyInstance(BlockState.class.getClassLoader(),
                new Class<?>[]{BlockState.class},(p,m,a)-> {if(m.getName().equals("getBlock"))return ore;throw new AssertionError(m.getName());});
        var saved=new AtomicReference<Collection<Position>>();
        var tracker=new OrePlacementTracker((w,n,p,removed,t,reason)-> {saved.set(p);return CompletableFuture.completedFuture(p.size());},r->fail(r),Logger.getLogger("PlacementTrackerTest"));
        tracker.onPlace(new BlockMultiPlaceEvent(List.of(state,state),ore,null,null,true,EquipmentSlot.HAND));
        assertEquals(List.of(new Position(0,-60,-1)),saved.get());
    }
    @Test void asynchronousSaveFailureAndSynchronousFailureMarkGapWithoutCancellingPlacement() {
        var issue=new AtomicReference<String>();var pending=new CompletableFuture<Integer>();
        var tracker=new OrePlacementTracker((w,n,p,removed,t,reason)->pending,issue::set,Logger.getLogger("PlacementTrackerTest"));
        var event=placed(block(Material.COAL_ORE,0),true);tracker.onPlace(event);
        assertTrue(tracker.diagnostic().contains("pending: 1"));pending.completeExceptionally(new IllegalStateException("injected"));
        assertNotNull(issue.get());assertFalse(event.isCancelled());assertTrue(tracker.diagnostic().contains("pending: 0"));
        issue.set(null);var rejected=new OrePlacementTracker((w,n,p,removed,t,reason)-> {throw new IllegalStateException("injected");},issue::set,Logger.getLogger("PlacementTrackerTest"));
        rejected.onPlace(event);assertNotNull(issue.get());
    }
    @Test void overloadedCaptureFailsClosedWithoutKeepingUnlimitedFutures() {
        var pending=new CompletableFuture<Integer>();var issue=new AtomicReference<String>();
        var tracker=new OrePlacementTracker((w,n,p,removed,t,reason)->pending,issue::set,Logger.getLogger("PlacementTrackerTest"));
        var event=placed(block(Material.COAL_ORE,0),true);
        for(int i=0;i<257;i++)tracker.onPlace(event);
        assertNotNull(issue.get());assertTrue(tracker.diagnostic().contains("pending: 256"));
        pending.complete(1);assertTrue(tracker.diagnostic().contains("pending: 0"));
    }
    @Test void extensionAndRetractionExcludeSourceAndBothAxisDestinationsWithoutReadingNeighbors() {
        var saved=new AtomicReference<Collection<Position>>();
        var tracker=new OrePlacementTracker((w,n,p,removed,t,reason)-> {saved.set(p);return CompletableFuture.completedFuture(p.size());},r->fail(r),Logger.getLogger("PistonTrackerTest"));
        var piston=block(Material.STICKY_PISTON,0);var ore=block(Material.IRON_ORE,-16);
        for(var face:List.of(BlockFace.EAST,BlockFace.WEST,BlockFace.UP,BlockFace.DOWN,BlockFace.NORTH,BlockFace.SOUTH)) {
            var expected=Set.of(new Position(-16,-60,-1),
                    new Position(-16+face.getModX(),-60+face.getModY(),-1+face.getModZ()),
                    new Position(-16-face.getModX(),-60-face.getModY(),-1-face.getModZ()));
            tracker.onExtend(new BlockPistonExtendEvent(piston,List.of(ore),face));
            assertEquals(expected,new HashSet<>(saved.get()));
            tracker.onRetract(new BlockPistonRetractEvent(piston,List.of(ore),face));
            assertEquals(expected,new HashSet<>(saved.get()));
        }
        assertThrows(UnsupportedOperationException.class,()->saved.get().clear());
    }
    @Test void cancelledPistonsAndMovementsWithoutOreDoNotSave() {
        var tracker=new OrePlacementTracker((w,n,p,removed,t,reason)-> {throw new AssertionError("Unexpected write");},r->fail(r),Logger.getLogger("PistonTrackerTest"));
        var piston=block(Material.PISTON,0);var ore=block(Material.COAL_ORE,1);
        var extend=new BlockPistonExtendEvent(piston,List.of(ore),BlockFace.EAST);extend.setCancelled(true);tracker.onExtend(extend);
        var retract=new BlockPistonRetractEvent(piston,List.of(ore),BlockFace.EAST);retract.setCancelled(true);tracker.onRetract(retract);
        tracker.onExtend(new BlockPistonExtendEvent(piston,List.of(block(Material.STONE,1)),BlockFace.UP));
        tracker.onRetract(new BlockPistonRetractEvent(piston,List.of(),BlockFace.WEST));
    }
    @Test void overlappingMovedOresDeduplicateAndKeepBounds() {
        var saved=new AtomicReference<Collection<Position>>();
        var tracker=new OrePlacementTracker((w,n,p,removed,t,reason)-> {saved.set(p);return CompletableFuture.completedFuture(p.size());},r->fail(r),Logger.getLogger("PistonTrackerTest"));
        tracker.onExtend(new BlockPistonExtendEvent(block(Material.PISTON,0),List.of(block(Material.COAL_ORE,15),block(Material.IRON_ORE,16),block(Material.COAL_ORE,15)),BlockFace.EAST));
        assertEquals(Set.of(new Position(14,-60,-1),new Position(15,-60,-1),new Position(16,-60,-1),new Position(17,-60,-1)),new HashSet<>(saved.get()));
    }
    @Test void invalidDirectionAndOversizedMutationPauseDiscoveryWithoutCancellingPiston() {
        var issue=new AtomicReference<String>();
        var tracker=new OrePlacementTracker((w,n,p,removed,t,reason)-> {throw new AssertionError("Unexpected write");},issue::set,Logger.getLogger("PistonTrackerTest"));
        var invalid=new BlockPistonExtendEvent(block(Material.PISTON,0),List.of(block(Material.IRON_ORE,1)),BlockFace.NORTH_EAST);
        tracker.onExtend(invalid);assertNotNull(issue.get());assertFalse(invalid.isCancelled());
        issue.set(null);var bounded=new OrePlacementTracker((w,n,p,removed,t,reason)-> {throw new AssertionError("Unexpected write");},issue::set,Logger.getLogger("PistonTrackerTest"));
        var oversized=new BlockPistonRetractEvent(block(Material.PISTON,0),Collections.nCopies(513,block(Material.COAL_ORE,1)),BlockFace.WEST);
        bounded.onRetract(oversized);assertNotNull(issue.get());assertFalse(oversized.isCancelled());
    }
    @Test void pistonSaveFailureUsesExistingGapAndPendingAccounting() {
        var issue=new AtomicReference<String>();var future=new CompletableFuture<Integer>();
        var tracker=new OrePlacementTracker((w,n,p,removed,t,reason)->future,issue::set,Logger.getLogger("PistonTrackerTest"));
        var event=new BlockPistonRetractEvent(block(Material.PISTON,0),List.of(block(Material.ANCIENT_DEBRIS,1)),BlockFace.EAST);
        tracker.onRetract(event);assertTrue(tracker.diagnostic().contains("pending: 1"));
        future.completeExceptionally(new IllegalStateException("injected"));
        assertNotNull(issue.get());assertFalse(event.isCancelled());assertTrue(tracker.diagnostic().contains("pending: 0"));
    }
    @Test void breakQueuesOnlyActualOreOriginAndCancelledOrStoneBreaksDoNothing() {
        var removed=new AtomicReference<Collection<Position>>();
        var tracker=new OrePlacementTracker((w,n,p,r,t,reason)->{assertTrue(p.isEmpty());removed.set(r);return CompletableFuture.completedFuture(0);},r->fail(r),Logger.getLogger("BreakTrackerTest"));
        tracker.onBreak(new BlockBreakEvent(block(Material.DIAMOND_ORE,7),null));
        assertEquals(List.of(new Position(7,-60,-1)),removed.get());assertThrows(UnsupportedOperationException.class,()->removed.get().clear());
        removed.set(null);var cancelled=new BlockBreakEvent(block(Material.COAL_ORE,8),null);cancelled.setCancelled(true);tracker.onBreak(cancelled);
        tracker.onBreak(new BlockBreakEvent(block(Material.STONE,9),null));assertNull(removed.get());
    }
    @Test void destructiveExplosionsUseFinalDeduplicatedListAndNonDestructiveOrCancelledEventsDoNothing() {
        var removed=new AtomicReference<Collection<Position>>();
        var tracker=new OrePlacementTracker((w,n,p,r,t,reason)->{assertTrue(p.isEmpty());removed.set(r);return CompletableFuture.completedFuture(0);},r->fail(r),Logger.getLogger("ExplosionTrackerTest"));
        var ore=block(Material.COAL_ORE,2);var list=new ArrayList<>(List.of(ore,ore,block(Material.STONE,3)));
        tracker.onBlockExplosion(new BlockExplodeEvent(block(Material.TNT,0),null,list,1,ExplosionResult.DESTROY));
        assertEquals(List.of(new Position(2,-60,-1)),removed.get());
        removed.set(null);var keep=new BlockExplodeEvent(ore,null,list,1,ExplosionResult.KEEP);tracker.onBlockExplosion(keep);
        tracker.onBlockExplosion(new BlockExplodeEvent(ore,null,list,1,ExplosionResult.TRIGGER_BLOCK));assertNull(removed.get());
        var cancelled=new BlockExplodeEvent(ore,null,list,1,ExplosionResult.DESTROY);cancelled.setCancelled(true);tracker.onBlockExplosion(cancelled);assertNull(removed.get());
        tracker.onEntityExplosion(new EntityExplodeEvent(null,null,list,1,ExplosionResult.DESTROY_WITH_DECAY));assertEquals(List.of(new Position(2,-60,-1)),removed.get());
        removed.set(null);var entityCancelled=new EntityExplodeEvent(null,null,list,1,ExplosionResult.DESTROY);entityCancelled.setCancelled(true);tracker.onEntityExplosion(entityCancelled);assertNull(removed.get());
        list.clear();tracker.onEntityExplosion(new EntityExplodeEvent(null,null,list,1,ExplosionResult.DESTROY));assertNull(removed.get());
    }
    @Test void pistonRetiresActualOriginsWhileDestinationsRemainOnlyConservativeExclusions() {
        var retired=new AtomicReference<Collection<Position>>();var excluded=new AtomicReference<Collection<Position>>();
        var tracker=new OrePlacementTracker((w,n,p,r,t,reason)->{excluded.set(p);retired.set(r);return CompletableFuture.completedFuture(0);},r->fail(r),Logger.getLogger("PistonRemovalTest"));
        var ore=block(Material.IRON_ORE,10);tracker.onRetract(new BlockPistonRetractEvent(block(Material.STICKY_PISTON,0),List.of(ore),BlockFace.EAST));
        assertEquals(List.of(new Position(10,-60,-1)),retired.get());
        assertEquals(Set.of(new Position(9,-60,-1),new Position(10,-60,-1),new Position(11,-60,-1)),new HashSet<>(excluded.get()));
    }
    @Test void failedOrOversizedBreakCaptureMarksGapAndNeverCancelsWorldEvent() {
        var issue=new AtomicReference<String>();var pending=new CompletableFuture<Integer>();
        var tracker=new OrePlacementTracker((w,n,p,r,t,reason)->pending,issue::set,Logger.getLogger("BreakFailureTest"));
        var event=new BlockBreakEvent(block(Material.COAL_ORE,1),null);tracker.onBreak(event);pending.completeExceptionally(new IllegalStateException("Injected removal failure"));
        assertNotNull(issue.get());assertFalse(event.isCancelled());assertTrue(tracker.diagnostic().contains("pending: 0"));
        issue.set(null);var bounded=new OrePlacementTracker((w,n,p,r,t,reason)->{throw new AssertionError("Oversized queued");},issue::set,Logger.getLogger("ExplosionFailureTest"));
        var explosion=new BlockExplodeEvent(block(Material.TNT,0),null,Collections.nCopies(513,block(Material.COAL_ORE,1)),1,ExplosionResult.DESTROY);
        bounded.onBlockExplosion(explosion);assertNotNull(issue.get());assertFalse(explosion.isCancelled());
    }
}
