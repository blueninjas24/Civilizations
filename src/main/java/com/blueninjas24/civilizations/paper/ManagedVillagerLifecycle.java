package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.EntityKey;
import org.bukkit.entity.Villager;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/** Native breeding cannot bypass saved families. Actual deaths retry on the SQL owner. */
public final class ManagedVillagerLifecycle extends BukkitRunnable implements Listener {
    private record Death(EntityKey entity, long millis, String reason) {}
    private final PopulationSession session;
    private final Logger logger;
    private final Map<EntityKey, Death> deaths = new LinkedHashMap<>();
    private final Map<EntityKey, Death> recentUnbound = new LinkedHashMap<>();
    private Death submitted;
    private CompletableFuture<Boolean> pending;
    private long retryAt;
    public ManagedVillagerLifecycle(PopulationSession session, Logger logger) { this.session = session; this.logger = logger; }
    private boolean managed(org.bukkit.entity.Entity entity) {
        return entity instanceof Villager && session.projectionView().map(view -> view.identities().aliases()
                .containsKey(new EntityKey(entity.getWorld().getUID(), entity.getUniqueId()))).orElse(false);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (managed(event.getMother()) || managed(event.getFather())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager)) return;
        var entity = event.getEntity();
        var key = new EntityKey(entity.getWorld().getUID(), entity.getUniqueId());
        // Native damage type only: no attacker/player names are captured in public diagnostics.
        var death = new Death(key, System.currentTimeMillis(), event.getDamageSource().getDamageType().getKey().toString());
        if (managed(entity)) deaths.putIfAbsent(key, death);
        else {
            // Positive observation may already be queued, but its identity is not yet published.
            if (recentUnbound.size() >= 256) recentUnbound.remove(recentUnbound.keySet().iterator().next());
            recentUnbound.putIfAbsent(key, death);
        }
        run();
    }
    @Override public void run() {
        var identities = session.projectionView().map(PopulationSession.ProjectionView::identities).orElse(null);
        recentUnbound.entrySet().removeIf(entry -> {
            if (identities != null && identities.aliases().containsKey(entry.getKey())) { deaths.putIfAbsent(entry.getKey(), entry.getValue()); return true; }
            return System.currentTimeMillis() - entry.getValue().millis() > 30_000;
        });
        if (pending != null) {
            if (!pending.isDone()) return;
            try { pending.join(); deaths.remove(submitted.entity()); }
            catch (java.util.concurrent.CompletionException failure) {
                logger.warning("Resident death save deferred: " + failure.getCause().getMessage()); retryAt = System.currentTimeMillis() + 1000;
            }
            pending = null; submitted = null;
        }
        if (!session.status().ready() || deaths.isEmpty() || System.currentTimeMillis() < retryAt) return;
        submitted = deaths.values().iterator().next();
        pending = session.recordPhysicalDeath(submitted.entity().worldId(), submitted.entity().entityId(), submitted.millis(), submitted.reason());
    }
    public String diagnostic() { return "Native death saves pending: " + deaths.size(); }
    public void flush() {
        if (deaths.isEmpty() && recentUnbound.isEmpty()) return;
        var observations = new ArrayList<>(deaths.values()); observations.addAll(recentUnbound.values());
        session.flushPhysicalDeaths(observations.stream().map(death -> new PopulationSession.DeathObservation(
                death.entity().worldId(), death.entity().entityId(), death.millis(), death.reason())).toList())
                .whenComplete((ignored, failure) -> { if (failure != null) logger.severe("Resident death shutdown save failed: " + failure.getMessage()); });
    }
}
