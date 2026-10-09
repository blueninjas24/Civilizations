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

/** Successful native placements only; immutable coordinates cross the owner queue, never Paper objects. */
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
        if(event instanceof BlockMultiPlaceEvent multiple)capture(multiple.getReplacedBlockStates().stream().map(s->s.getBlock()).toList());
        else capture(List.of(event.getBlockPlaced()));
    }
    /** Main-thread capture, also used by controlled integration adapters for known placements. */
    void capture(List<Block> blocks) {
        try {
            var ores=blocks.stream().filter(block->ore(block.getType())).toList();
            if(ores.isEmpty())return;
            var world=ores.getFirst().getWorld();var id=world.getUID();var positions=new LinkedHashSet<Position>();
            for(var block:ores) {
                if(!block.getWorld().getUID().equals(id))throw new IllegalArgumentException("Placement spans worlds");
                positions.add(new Position(block.getX(),block.getY(),block.getZ()));
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
            gap.accept("placement capture/save incomplete; reviewed recovery required");
            logger.log(java.util.logging.Level.SEVERE,
                    "Ore placement capture incomplete; automatic resource discovery paused. Saved settlements and reserves are retained.",failure);
        }
    }
    public String diagnostic() {return "Ore placement saves pending: "+pending.get()+(reported.get()?"; capture gap":"");}
}
