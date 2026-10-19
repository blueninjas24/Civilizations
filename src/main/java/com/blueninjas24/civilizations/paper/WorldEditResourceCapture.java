package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.application.ResourceMutationCapture;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/** Optional, explicit-registration adapter. Installing it never certifies earlier editor history. */
public final class WorldEditResourceCapture implements AutoCloseable {
    private final PopulationSession session;
    private final Logger logger;
    private final AtomicBoolean registered=new AtomicBoolean(),failed=new AtomicBoolean();

    public WorldEditResourceCapture(PopulationSession session,Logger logger) {
        this.session=Objects.requireNonNull(session);this.logger=Objects.requireNonNull(logger);
    }

    public void register() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Register WorldEdit capture on the server thread");
        var plugin=Bukkit.getPluginManager().getPlugin("WorldEdit");
        if(plugin==null||!plugin.isEnabled()||Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit")!=null
                ||!plugin.getPluginMeta().getVersion().matches("7\\.4\\.4(?:[+\\-].*)?"))
            throw new IllegalStateException("Resource capture currently targets the verified WorldEdit 7.4.4 Bukkit adapter only");
        if(!registered.compareAndSet(false,true))throw new IllegalStateException("WorldEdit capture already registered");
        try {WorldEdit.getInstance().getEventBus().register(this);}
        catch(RuntimeException|LinkageError failure){registered.set(false);failClosed("WorldEdit hook registration failed");throw failure;}
    }

    @Subscribe public void onEditSession(EditSessionEvent event) {
        if(event.getStage()!=EditSession.Stage.BEFORE_CHANGE||event.getWorld()==null)return;
        if(!registered.get()||!Bukkit.isPrimaryThread()){failClosed("Unsupported off-thread or detached WorldEdit session");return;}
        World world=Bukkit.getWorld(event.getWorld().getName());
        if(world==null){failClosed("WorldEdit world identity unavailable");return;}
        var worldId=world.getUID();var name=world.getName();
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 p,T requested)throws WorldEditException {
                if(!registered.get()||!Bukkit.isPrimaryThread()) {
                    failClosed("WorldEdit write outside supported synchronous capture");return super.setBlock(p,requested);
                }
                if(!world.isChunkLoaded(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16))) {
                    failClosed("WorldEdit write in an unobserved unloaded chunk");return super.setBlock(p,requested);
                }
                // Read the live world, not a possibly buffered/cached editor extent.
                var before=BukkitAdapter.adapt(world.getBlockAt(p.x(),p.y(),p.z()).getBlockData());
                var wanted=requested.toImmutableState();
                if(before.equals(wanted))return super.setBlock(p,requested);
                Material old=Material.matchMaterial(before.getBlockType().id());
                Material target=Material.matchMaterial(wanted.getBlockType().id());
                if(old==null||target==null){failClosed("WorldEdit block type cannot be classified");return super.setBlock(p,requested);}
                boolean oreBefore=OrePlacementTracker.finiteResource(old),oreAfter=OrePlacementTracker.finiteResource(target);
                boolean forestBefore=ForestMutationTracker.treeMaterial(old),forestAfter=ForestMutationTracker.treeMaterial(target);
                if(!oreBefore&&!oreAfter&&!forestBefore&&!forestAfter)return super.setBlock(p,requested);
                ResourceMutationCapture capture;
                try {capture=session.beginExternalResourceMutation();}
                catch(RuntimeException unavailable){failClosed("WorldEdit resource capture unavailable");return super.setBlock(p,requested);}
                try(capture) {
                    boolean result=super.setBlock(p,requested);
                    if(!world.isChunkLoaded(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16))) {
                        failClosed("WorldEdit post-write evidence unavailable");return result;
                    }
                    var after=BukkitAdapter.adapt(world.getBlockAt(p.x(),p.y(),p.z()).getBlockData());
                    if(after.equals(before)){capture.unchanged();return result;}
                    if(!after.equals(wanted)){failClosed("WorldEdit resource write outcome ambiguous");return result;}
                    var point=List.of(new Position(p.x(),p.y(),p.z()));
                    capture.complete(new ResourceMutationCapture.Changes(worldId,name,
                            oreAfter?point:List.of(),oreBefore?point:List.of(),forestAfter?point:List.of(),forestBefore?point:List.of(),
                            System.currentTimeMillis(),"Confirmed WorldEdit 7.4.4 resource change"))
                            .whenComplete((ignored,failure)->{if(failure!=null)failClosed("WorldEdit resource save incomplete");});
                    return result;
                }
            }
        });
    }

    private void failClosed(String reason) {
        session.invalidateOreTracking(reason);session.invalidateForestTracking(reason);
        if(failed.compareAndSet(false,true))logger.warning(reason+"; resource provenance requires reviewed recovery; saved assets remain.");
    }

    @Override public void close() {
        if(registered.compareAndSet(true,false)) {
            try {WorldEdit.getInstance().getEventBus().unregister(this);}
            finally {failClosed("WorldEdit resource hook detached");}
        }
    }
}
