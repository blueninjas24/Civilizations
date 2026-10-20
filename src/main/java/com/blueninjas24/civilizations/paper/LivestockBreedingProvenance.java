package com.blueninjas24.civilizations.paper;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

/** Persistent protective evidence, not ownership. No player identities, SQL, cancellation or resource changes. */
public final class LivestockBreedingProvenance implements Listener {
    private static final NamespacedKey PLAYER_BRED=new NamespacedKey("civilizations","livestock_player_bred");
    private static final NamespacedKey HUMAN_ORIGIN=new NamespacedKey("civilizations","livestock_human_origin");
    public static boolean recorded(Entity entity) {
        // Any saved value is protective; a malformed/old tag must not make an animal adoptable.
        var data=entity.getPersistentDataContainer();
        return data.has(PLAYER_BRED)||data.has(HUMAN_ORIGIN)||humanOrigin(entity.getEntitySpawnReason());
    }
    private static boolean humanOrigin(CreatureSpawnEvent.SpawnReason reason) {
        return reason==CreatureSpawnEvent.SpawnReason.EGG||reason==CreatureSpawnEvent.SpawnReason.BREEDING
                ||reason==CreatureSpawnEvent.SpawnReason.SPAWNER_EGG||reason==CreatureSpawnEvent.SpawnReason.DISPENSE_EGG
                ||reason==CreatureSpawnEvent.SpawnReason.COMMAND;
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void spawned(CreatureSpawnEvent event) {
        if(event.isCancelled()||!humanOrigin(event.getSpawnReason()))return;
        var entity=event.getEntity();var type=entity.getType();
        if(PaperLivestockSpecies.from(type).isEmpty())return;
        if(!entity.getPersistentDataContainer().has(HUMAN_ORIGIN))
            entity.getPersistentDataContainer().set(HUMAN_ORIGIN,PersistentDataType.BYTE,(byte)1);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void bred(EntityBreedEvent event) {
        if(event.isCancelled())return;
        var child=event.getEntity();var type=child.getType();
        if(PaperLivestockSpecies.from(type).isEmpty())return;
        if(event.getBreeder() instanceof Player||recorded(event.getMother())||recorded(event.getFather())) {
            var data=child.getPersistentDataContainer();
            if(!data.has(PLAYER_BRED))data.set(PLAYER_BRED,PersistentDataType.BYTE,(byte)1);
        }
    }
}
