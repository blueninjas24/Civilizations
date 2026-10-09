package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.*;
import org.bukkit.event.block.*;

/** Native ore mutations; immutable coordinates cross the owner queue, never Paper objects. */
public final class OrePlacementTracker implements Listener {
    @FunctionalInterface interface Recorder {
        CompletableFuture<Integer> save(UUID world,String name,Collection<Position> positions,long millis);
    }
    private final Recorder recorder;
    private final Consumer<String> gap;
    private final Logger logger;
    private final AtomicInteger pending=new AtomicInteger();
    private final AtomicBoolean reported=new AtomicBoolean();
    public OrePlacementTracker(PopulationSession session,Logger logger) {
        this(session::recordOrePlacements,session::invalidateOreTracking,logger);
    }
    OrePlacementTracker(Recorder recorder,Consumer<String> gap,Logger logger) {
        this.recorder=Objects.requireNonNull(recorder);this.gap=Objects.requireNonNull(gap);this.logger=Objects.requireNonNull(logger);
    }
    static boolean ore(Material type) { return type.name().endsWith("_ORE") || type==Material.ANCIENT_DEBRIS; }
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
    private void moved(List<Block> blocks,org.bukkit.block.BlockFace direction) {
        int dx=direction.getModX(),dy=direction.getModY(),dz=direction.getModZ();
        if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)!=1)
            throw new IllegalArgumentException("Piston direction is not an axial face");
        capture(blocks,dx,dy,dz,true);
    }
    /** Main-thread capture, also used by controlled integration adapters for known placements. */
    void capture(List<Block> blocks) {
        capture(blocks,0,0,0,false);
    }
    private void capture(List<Block> blocks,int dx,int dy,int dz,boolean movement) {
        try {
            if(blocks.size()>512)throw new IllegalArgumentException("Native mutation exceeds block budget");
            var ores=blocks.stream().filter(block->ore(block.getType())).toList();
            if(ores.isEmpty())return;
            var world=ores.getFirst().getWorld();var id=world.getUID();var positions=new LinkedHashSet<Position>();
            for(var block:ores) {
                if(!block.getWorld().getUID().equals(id))throw new IllegalArgumentException("Placement spans worlds");
                positions.add(new Position(block.getX(),block.getY(),block.getZ()));
                if(movement) {
                    // The API's operation direction is not a natural-origin certificate.
                    // Exclude both possible destinations without querying/loading either block.
                    positions.add(new Position(Math.addExact(block.getX(),dx),Math.addExact(block.getY(),dy),Math.addExact(block.getZ(),dz)));
                    positions.add(new Position(Math.subtractExact(block.getX(),dx),Math.subtractExact(block.getY(),dy),Math.subtractExact(block.getZ(),dz)));
                }
            }
            if(positions.size()>512) { fail(new IllegalStateException("Placement evidence exceeds batch budget"));return; }
            if(pending.incrementAndGet()>256) { pending.decrementAndGet();fail(new IllegalStateException("Placement capture queue full"));return; }
            CompletableFuture<Integer> saved;
            try { saved=Objects.requireNonNull(recorder.save(id,world.getName(),List.copyOf(positions),System.currentTimeMillis())); }
            catch(RuntimeException failure) {pending.decrementAndGet();throw failure;}
            saved.whenComplete((ignored,failure)-> {
                pending.decrementAndGet();if(failure!=null)fail(failure);
            });
        } catch(RuntimeException failure) {fail(failure);}
    }
    private void fail(Throwable failure) {
        if(reported.compareAndSet(false,true)) {
            gap.accept("ore mutation capture/save incomplete; reviewed recovery required");
            logger.log(java.util.logging.Level.SEVERE,
                    "Ore mutation capture incomplete; automatic resource discovery paused. Saved settlements and reserves are retained.",failure);
        }
    }
    public String diagnostic() {return "Ore placement saves pending: "+pending.get()+(reported.get()?"; capture gap":"");}
}
