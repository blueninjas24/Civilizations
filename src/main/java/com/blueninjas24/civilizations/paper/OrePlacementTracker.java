package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.ExplosionResult;
import org.bukkit.block.Block;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/** Native finite-source mutations; immutable coordinates cross the owner queue, never Paper objects. */
public final class OrePlacementTracker implements Listener {
    @FunctionalInterface interface MutationRecorder {
        CompletableFuture<?> save(UUID world,String name,Collection<Position> placements,Collection<Position> removals,long millis,String reason);
    }
    private final MutationRecorder recorder;
    private final Consumer<String> gap;
    private final Logger logger;
    private final AtomicInteger pending=new AtomicInteger();
    private final AtomicBoolean reported=new AtomicBoolean();
    public OrePlacementTracker(PopulationSession session,Logger logger) {
        this(session::recordOreMutations,session::invalidateOreTracking,logger);
    }
    OrePlacementTracker(MutationRecorder recorder,Consumer<String> gap,Logger logger) {
        this.recorder=Objects.requireNonNull(recorder);this.gap=Objects.requireNonNull(gap);this.logger=Objects.requireNonNull(logger);
    }
    static boolean ore(Material type) { return type.name().endsWith("_ORE") || type==Material.ANCIENT_DEBRIS; }
    static boolean finiteResource(Material type) {
        return ore(type)||type==Material.STONE||type==Material.DEEPSLATE||type==Material.SAND||type==Material.RED_SAND;
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        if(event.isCancelled())return;
        try{changed(event.getBlock(),event.getTo(),"Entity changed supporting finite resource");}
        catch(RuntimeException failure){fail(failure);}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onForm(BlockFormEvent event) {
        if(event.isCancelled())return;
        try{changed(event.getBlock(),event.getNewState().getType(),"Newly formed finite resource is not a natural reserve");}
        catch(RuntimeException failure){fail(failure);}
    }
    private void changed(Block block,Material after,String reason) {
        try {
            var before=block.getType();if(before==after)return;
            boolean from=finiteResource(before),to=finiteResource(after);if(!from&&!to)return;
            var world=block.getWorld();var p=new Position(block.getX(),block.getY(),block.getZ());
            submit(world.getUID(),world.getName(),to?List.of(p):List.of(),from?List.of(p):List.of(),reason);
        }catch(RuntimeException failure){fail(failure);}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        if(event.isCancelled() || !event.canBuild())return;
        try {
            if(event instanceof BlockMultiPlaceEvent multiple)capture(multiple.getReplacedBlockStates().stream().map(s->s.getBlock()).toList());
            else capture(List.of(event.getBlockPlaced()));
        } catch(RuntimeException failure) {fail(failure);}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onExtend(BlockPistonExtendEvent event) {
        if(event.isCancelled())return;
        try { moved(event.getBlocks(),event.getDirection()); }
        catch(RuntimeException failure) {fail(failure);}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onRetract(BlockPistonRetractEvent event) {
        if(event.isCancelled())return;
        try { moved(event.getBlocks(),event.getDirection()); }
        catch(RuntimeException failure) {fail(failure);}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        if(!event.isCancelled())removed(List.of(event.getBlock()),"Player broke supporting ore");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if(!event.isCancelled()&&destroys(event.getExplosionResult()))removed(event.blockList(),"Block explosion removed supporting ore");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if(!event.isCancelled()&&destroys(event.getExplosionResult()))removed(event.blockList(),"Entity explosion removed supporting ore");
    }
    private static boolean destroys(ExplosionResult result) {
        return result==ExplosionResult.DESTROY||result==ExplosionResult.DESTROY_WITH_DECAY;
    }
    private void removed(List<Block> blocks,String reason) {capture(blocks,0,0,0,false,true,reason);}
    private void moved(List<Block> blocks,org.bukkit.block.BlockFace direction) {
        int dx=direction.getModX(),dy=direction.getModY(),dz=direction.getModZ();
        if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)!=1)
            throw new IllegalArgumentException("Piston direction is not an axial face");
        capture(blocks,dx,dy,dz,true,true,"Piston moved supporting ore");
    }
    /** Main-thread capture, also used by controlled integration adapters for known placements. */
    void capture(List<Block> blocks) {
        capture(blocks,0,0,0,false,false,"Player placed ore");
    }
    private void capture(List<Block> blocks,int dx,int dy,int dz,boolean movement,boolean removal,String reason) {
        try {
            if(blocks.size()>512)throw new IllegalArgumentException("Native mutation exceeds block budget");
            var ores=blocks.stream().filter(block->finiteResource(block.getType())).toList();
            if(ores.isEmpty())return;
            var world=ores.getFirst().getWorld();var id=world.getUID();var positions=new LinkedHashSet<Position>();var origins=new LinkedHashSet<Position>();
            for(var block:ores) {
                if(!block.getWorld().getUID().equals(id))throw new IllegalArgumentException("Placement spans worlds");
                var origin=new Position(block.getX(),block.getY(),block.getZ());
                if(removal)origins.add(origin);
                if(!removal||movement)positions.add(origin);
                if(movement) {
                    // The API's operation direction is not a natural-origin certificate.
                    // Exclude both possible destinations without querying/loading either block.
                    positions.add(new Position(Math.addExact(block.getX(),dx),Math.addExact(block.getY(),dy),Math.addExact(block.getZ(),dz)));
                    positions.add(new Position(Math.subtractExact(block.getX(),dx),Math.subtractExact(block.getY(),dy),Math.subtractExact(block.getZ(),dz)));
                }
            }
            if(positions.size()>512||origins.size()>512) { fail(new IllegalStateException("Placement evidence exceeds batch budget"));return; }
            submit(id,world.getName(),List.copyOf(positions),List.copyOf(origins),reason);
        } catch(RuntimeException failure) {fail(failure);}
    }
    private void submit(UUID id,String name,List<Position> positions,List<Position> origins,String reason) {
        if(pending.incrementAndGet()>256) { pending.decrementAndGet();fail(new IllegalStateException("Placement capture queue full"));return; }
        CompletableFuture<?> saved;
        try { saved=Objects.requireNonNull(recorder.save(id,name,positions,origins,System.currentTimeMillis(),reason)); }
        catch(RuntimeException failure) {pending.decrementAndGet();throw failure;}
        saved.whenComplete((ignored,failure)-> {
            pending.decrementAndGet();if(failure!=null)fail(failure);
        });
    }
    private void fail(Throwable failure) {
        if(reported.compareAndSet(false,true)) {
            gap.accept("ore mutation capture/save incomplete; reviewed recovery required");
            logger.log(java.util.logging.Level.SEVERE,
                    "Ore mutation capture incomplete; automatic discovery and coordinate-backed mining paused. Saved settlements and reserves are retained.",failure);
        }
    }
    public String diagnostic() {return "Ore mutation saves pending: "+pending.get()+(reported.get()?"; capture gap":"");}
}
