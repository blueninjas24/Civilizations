package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.ExplosionResult;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BlockFace;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;

/** Native tree-material mutations; immutable coordinates cross the owner queue, never Paper objects. */
public final class ForestMutationTracker implements Listener {
    @FunctionalInterface interface MutationRecorder {
        CompletableFuture<?> save(UUID world,String name,Collection<Position> placements,Collection<Position> removals,long millis,String reason);
    }
    private final MutationRecorder recorder;
    private final Consumer<String> gap;
    private final Logger logger;
    private final Predicate<Material> treeMaterials;
    private final AtomicInteger pending=new AtomicInteger();
    private final AtomicBoolean reported=new AtomicBoolean();

    public ForestMutationTracker(PopulationSession session,Logger logger) {
        this(session::recordForestMutations,session::invalidateForestTracking,logger,ForestMutationTracker::treeMaterial);
    }
    ForestMutationTracker(MutationRecorder recorder,Consumer<String> gap,Logger logger) {
        this(recorder,gap,logger,ForestMutationTracker::treeMaterial);
    }
    ForestMutationTracker(MutationRecorder recorder,Consumer<String> gap,Logger logger,Predicate<Material> treeMaterials) {
        this.recorder=Objects.requireNonNull(recorder);this.gap=Objects.requireNonNull(gap);this.logger=Objects.requireNonNull(logger);
        this.treeMaterials=Objects.requireNonNull(treeMaterials);
    }
    static boolean treeMaterial(Material material) {
        return Tag.LOGS.isTagged(material)||Tag.LEAVES.isTagged(material)||Tag.SAPLINGS.isTagged(material);
    }
    private boolean isTreeMaterial(Material material){return treeMaterials.test(material);}

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        if(event.isCancelled()||!event.canBuild())return;
        try {
            var replaced=event instanceof BlockMultiPlaceEvent multiple
                    ?multiple.getReplacedBlockStates():List.of(event.getBlockReplacedState());
            var placements=new LinkedHashSet<Position>();var removals=new LinkedHashSet<Position>();
            var world=event.getBlockPlaced().getWorld();var worldId=world.getUID();
            for(var state:replaced) {
                var block=state.getBlock();
                if(!block.getWorld().getUID().equals(worldId))throw new IllegalArgumentException("Placement spans worlds");
                var position=position(block);
                if(isTreeMaterial(state.getType()))add(placements,removals,position,false,true);
                // A multi-place can contain different resulting materials. The event's primary
                // block type describes only one of them; each replaced coordinate has its own result.
                if(isTreeMaterial(block.getType()))add(placements,removals,position,true,false);
            }
            if(isTreeMaterial(event.getBlockPlaced().getType()))add(placements,removals,position(event.getBlockPlaced()),true,false);
            submit(worldId,world.getName(),placements,removals,"Placed or replaced a tree-supporting block");
        }catch(RuntimeException failure){fail(failure);}
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        if(!event.isCancelled())remove(List.of(event.getBlock()),"Broke a tree-supporting block");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBurn(BlockBurnEvent event) {
        if(!event.isCancelled())remove(List.of(event.getBlock()),"Burned a tree-supporting block");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onDecay(LeavesDecayEvent event) {
        if(!event.isCancelled())remove(List.of(event.getBlock()),"Leaves decayed from a tree-supporting coordinate");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if(!event.isCancelled()&&destroys(event.getExplosionResult()))remove(event.blockList(),"Block explosion removed tree-supporting material");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if(!event.isCancelled()&&destroys(event.getExplosionResult()))remove(event.blockList(),"Entity explosion removed tree-supporting material");
    }
    private static boolean destroys(ExplosionResult result) {
        return result==ExplosionResult.DESTROY||result==ExplosionResult.DESTROY_WITH_DECAY;
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onExtend(BlockPistonExtendEvent event) {
        if(!event.isCancelled())moved(event.getBlocks(),event.getDirection());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onRetract(BlockPistonRetractEvent event) {
        if(!event.isCancelled())moved(event.getBlocks(),event.getDirection());
    }
    private void moved(List<Block> blocks,BlockFace direction) {
        int dx=direction.getModX(),dy=direction.getModY(),dz=direction.getModZ();
        if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)!=1) {fail(new IllegalArgumentException("Piston direction is not an axial face"));return;}
        try {
            var placements=new LinkedHashSet<Position>();var removals=new LinkedHashSet<Position>();
            UUID worldId=null;String worldName=null;
            for(var block:blocks) {
                if(!isTreeMaterial(block.getType()))continue;
                var world=block.getWorld();
                if(worldId==null){worldId=world.getUID();worldName=world.getName();}
                else if(!worldId.equals(world.getUID()))throw new IllegalArgumentException("Piston mutation spans worlds");
                var p=position(block);add(placements,removals,p,true,true);
                add(placements,removals,new Position(Math.addExact(p.x(),dx),Math.addExact(p.y(),dy),Math.addExact(p.z(),dz)),true,false);
                add(placements,removals,new Position(Math.subtractExact(p.x(),dx),Math.subtractExact(p.y(),dy),Math.subtractExact(p.z(),dz)),true,false);
            }
            if(worldId!=null)submit(worldId,worldName,placements,removals,"Piston moved tree-supporting material");
        }catch(RuntimeException failure){fail(failure);}
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        if(!event.isCancelled())changed(event.getBlock(),event.getTo(),"Entity changed tree-supporting material");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onForm(BlockFormEvent event) {
        if(!event.isCancelled())changed(event.getBlock(),event.getNewState().getType(),"Block formation changed tree-supporting material");
    }
    private void changed(Block block,Material after,String reason) {
        try {
            var before=block.getType();if(before==after)return;
            boolean from=isTreeMaterial(before),to=isTreeMaterial(after);if(!from&&!to)return;
            var p=position(block);submit(block.getWorld().getUID(),block.getWorld().getName(),to?List.of(p):List.of(),from?List.of(p):List.of(),reason);
        }catch(RuntimeException failure){fail(failure);}
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onStructureGrow(StructureGrowEvent event) {
        if(!event.isCancelled())growth(event.getBlocks(),"Structure growth added tree-supporting material");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onFertilize(BlockFertilizeEvent event) {
        if(!event.isCancelled())growth(event.getBlocks(),"Fertilization added tree-supporting material");
    }
    void growth(List<BlockState> states,String reason) {
        try {
            var placements=new LinkedHashSet<Position>();var removals=new LinkedHashSet<Position>();UUID worldId=null;String worldName=null;
            for(var state:states) {
                var block=state.getBlock();var world=block.getWorld();
                if(worldId==null){worldId=world.getUID();worldName=world.getName();}
                else if(!worldId.equals(world.getUID()))throw new IllegalArgumentException("Growth spans worlds");
                Material before=block.getType(),after=state.getType();var p=position(block);
                if(before!=after&&isTreeMaterial(before))add(placements,removals,p,false,true);
                // Growth BlockStates describe the new material; inspect them directly, not the old live block.
                if(isTreeMaterial(after))add(placements,removals,p,true,false);
            }
            if(worldId!=null)submit(worldId,worldName,placements,removals,reason);
        }catch(RuntimeException failure){fail(failure);}
    }

    private void remove(List<Block> blocks,String reason) {
        try {
            var removals=new LinkedHashSet<Position>();UUID worldId=null;String worldName=null;
            for(var block:blocks) {
                if(!isTreeMaterial(block.getType()))continue;
                var world=block.getWorld();
                if(worldId==null){worldId=world.getUID();worldName=world.getName();}
                else if(!worldId.equals(world.getUID()))throw new IllegalArgumentException("Removal spans worlds");
                removals.add(position(block));
                if(removals.size()>512)throw new IllegalArgumentException("Tree mutation coordinate batch exceeds budget");
            }
            if(worldId!=null)submit(worldId,worldName,List.of(),removals,reason);
        }catch(RuntimeException failure){fail(failure);}
    }
    private static Position position(Block block){return new Position(block.getX(),block.getY(),block.getZ());}

    private static void add(Set<Position> placements,Set<Position> removals,Position position,boolean placement,boolean removal) {
        if(placement)placements.add(position);if(removal)removals.add(position);
        int overlap=0;for(var p:placements)if(removals.contains(p))overlap++;
        if(placements.size()>512||removals.size()>512||placements.size()+removals.size()-overlap>512)
            throw new IllegalArgumentException("Tree mutation coordinate batch exceeds budget");
    }

    private void submit(UUID world,String name,Collection<Position> placements,Collection<Position> removals,String reason) {
        var placed=List.copyOf(placements);var removed=List.copyOf(removals);
        var all=new HashSet<>(placed);all.addAll(removed);
        if(placed.size()>512||removed.size()>512||all.size()>512) {fail(new IllegalStateException("Tree mutation coordinate batch exceeds budget"));return;}
        if(placed.isEmpty()&&removed.isEmpty())return;
        if(pending.incrementAndGet()>256) {pending.decrementAndGet();fail(new IllegalStateException("Tree mutation capture queue full"));return;}
        CompletableFuture<?> saved;
        try {saved=Objects.requireNonNull(recorder.save(world,name,placed,removed,System.currentTimeMillis(),reason));}
        catch(RuntimeException failure){pending.decrementAndGet();fail(failure);return;}
        saved.whenComplete((ignored,failure)->{pending.decrementAndGet();if(failure!=null)fail(failure);});
    }
    private void fail(Throwable failure) {
        if(reported.compareAndSet(false,true)) {
            try{gap.accept("forest mutation capture/save incomplete; reviewed recovery required");}
            catch(RuntimeException gapFailure){failure.addSuppressed(gapFailure);}
            logger.log(java.util.logging.Level.SEVERE,
                    "Forest mutation capture incomplete; observed habitat is not trusted until reviewed recovery.",failure);
        }
    }
    public String diagnostic(){return "Forest mutation saves pending: "+pending.get()+(reported.get()?"; capture gap":"");}
}
