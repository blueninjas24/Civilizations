package com.blueninjas24.civilizations.paper;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.persistence.PersistentDataType;

/** Persistent protective evidence, not ownership. No player identities, SQL, cancellation or resource changes. */
public final class LivestockBreedingProvenance implements Listener {
    private static final NamespacedKey PLAYER_BRED=new NamespacedKey("civilizations","livestock_player_bred");
    public static boolean recorded(Entity entity) {
        // Any saved value is protective; a malformed/old tag must not make an animal adoptable.
        return entity.getPersistentDataContainer().has(PLAYER_BRED);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void bred(EntityBreedEvent event) {
        if(event.isCancelled())return;
        var child=event.getEntity();var type=child.getType();
        if(type!=EntityType.COW&&type!=EntityType.CHICKEN)return;
        if(event.getBreeder() instanceof Player||recorded(event.getMother())||recorded(event.getFather())) {
            if(!recorded(child))child.getPersistentDataContainer().set(PLAYER_BRED,PersistentDataType.BYTE,(byte)1);
        }
    }
}
