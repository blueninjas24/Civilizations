package com.blueninjas24.civilizations.paper;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.*;
import java.util.function.*;

/** Approved village-only control. Positive saved identities; no SQL/world scan per interaction. */
public final class ManagedLivestockBreedingGuard implements Listener {
    private final Supplier<Set<UUID>> villageAnimals;
    private final BooleanSupplier enabled;
    private final Predicate<Material> cowFood,chickenFood;
    public ManagedLivestockBreedingGuard(Supplier<Set<UUID>> animals,BooleanSupplier enabled){
        this(animals,enabled,m->Tag.ITEMS_COW_FOOD.isTagged(m),m->Tag.ITEMS_CHICKEN_FOOD.isTagged(m));
    }
    ManagedLivestockBreedingGuard(Supplier<Set<UUID>> animals,BooleanSupplier enabled,Predicate<Material> cowFood,Predicate<Material> chickenFood){
        villageAnimals=Objects.requireNonNull(animals);this.enabled=Objects.requireNonNull(enabled);this.cowFood=Objects.requireNonNull(cowFood);this.chickenFood=Objects.requireNonNull(chickenFood);
    }
    private boolean managed(Entity animal){return (animal.getType()==EntityType.COW||animal.getType()==EntityType.CHICKEN)&&villageAnimals.get().contains(animal.getUniqueId());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void breed(EntityBreedEvent event){
        if(enabled.getAsBoolean()&&(managed(event.getMother())||managed(event.getFather())))event.setCancelled(true);
    }
    /** Stop breeding-food interaction before vanilla consumes items; buckets/name tags/leashes stay available. */
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void interact(PlayerInteractEntityEvent event){
        if(!enabled.getAsBoolean()||!managed(event.getRightClicked()))return;
        var item=event.getPlayer().getInventory().getItem(event.getHand());if(item==null)return;
        boolean breedingFood=event.getRightClicked().getType()==EntityType.COW?cowFood.test(item.getType()):chickenFood.test(item.getType());
        if(breedingFood){event.setCancelled(true);event.getPlayer().sendMessage("Village livestock breeding is managed by Civilizations.");}
    }
}
