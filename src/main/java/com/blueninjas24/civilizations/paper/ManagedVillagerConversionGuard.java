package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.database.ResidentProjectionRepository.EntityKey;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.Snapshot;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import java.util.function.Supplier;

/** Approved prototype rule: managed villagers cannot be converted into zombie villagers. */
public final class ManagedVillagerConversionGuard implements Listener {
    private final Supplier<Snapshot> identities;
    public ManagedVillagerConversionGuard(Supplier<Snapshot> identities) {
        this.identities = java.util.Objects.requireNonNull(identities);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.INFECTION
                || !(event.getEntity() instanceof Villager villager)) return;
        var key = new EntityKey(villager.getWorld().getUID(), villager.getUniqueId());
        if (identities.get().aliases().containsKey(key)) event.setCancelled(true);
    }
}
