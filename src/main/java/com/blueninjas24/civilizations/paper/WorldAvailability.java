package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Main-thread world availability only. Chunk unloads deliberately do not pause civilization. */
public final class WorldAvailability implements Listener {
    private final Supplier<? extends Iterable<World>> worlds;
    private final java.util.function.Consumer<Set<UUID>> publish;
    public WorldAvailability(PopulationSession session, Supplier<? extends Iterable<World>> worlds, Logger logger) {
        this(worlds, ids -> session.updateLoadedWorlds(ids).whenComplete((ignored, failed) -> {
            if (failed != null) logger.log(Level.SEVERE, "World availability update failed; simulation requires review", failed);
        }));
    }
    WorldAvailability(Supplier<? extends Iterable<World>> worlds, java.util.function.Consumer<Set<UUID>> publish) {
        this.worlds = worlds; this.publish = publish;
    }
    public Set<UUID> snapshot() {
        var ids = new HashSet<UUID>();
        worlds.get().forEach(world -> ids.add(world.getUID()));
        return Set.copyOf(ids);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void loaded(WorldLoadEvent event) {
        var ids = new HashSet<>(snapshot()); ids.add(event.getWorld().getUID()); publish(ids);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void unloaded(WorldUnloadEvent event) {
        if (event.isCancelled()) return;
        var ids = new HashSet<>(snapshot()); ids.remove(event.getWorld().getUID()); publish(ids);
    }
    private void publish(Set<UUID> ids) {
        publish.accept(Set.copyOf(ids));
    }
}
