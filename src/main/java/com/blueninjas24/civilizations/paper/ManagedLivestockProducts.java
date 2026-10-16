package com.blueninjas24.civilizations.paper;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.*;
import java.util.function.*;

/** Village milk/eggs belong to saved business stock. No inventory, SQL or world scanning per event. */
public final class ManagedLivestockProducts implements Listener {
    private final Supplier<Set<UUID>> villageAnimals;
    private final BooleanSupplier enabled;
    public ManagedLivestockProducts(Supplier<Set<UUID>> animals,BooleanSupplier enabled) {
        this.villageAnimals=Objects.requireNonNull(animals);this.enabled=Objects.requireNonNull(enabled);
    }
    private boolean managed(Entity entity,EntityType type) {
        return enabled.getAsBoolean()&&entity.getType()==type&&villageAnimals.get().contains(entity.getUniqueId());
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void milk(PlayerInteractEntityEvent event) {
        if(!managed(event.getRightClicked(),EntityType.COW))return;
        var item=event.getPlayer().getInventory().getItem(event.getHand());
        if(item!=null&&item.getType()==Material.BUCKET) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Village milk belongs to its dairy business. Obtain it through village trade.");
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void egg(EntityDropItemEvent event) {
        if(!managed(event.getEntity(),EntityType.CHICKEN))return;
        var item=event.getItemDrop().getItemStack().getType();
        if(item==Material.EGG||item==Material.BROWN_EGG||item==Material.BLUE_EGG)event.setCancelled(true);
    }
}
