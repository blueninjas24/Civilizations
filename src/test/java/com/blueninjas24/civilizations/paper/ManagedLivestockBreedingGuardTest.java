package com.blueninjas24.civilizations.paper;

import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.Material;
import org.bukkit.inventory.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManagedLivestockBreedingGuardTest {
    final UUID a=new UUID(0,1),b=new UUID(0,2);
    LivingEntity animal(UUID id,EntityType type){return (LivingEntity)Proxy.newProxyInstance(LivingEntity.class.getClassLoader(),new Class<?>[]{LivingEntity.class},(p,m,args)->switch(m.getName()){case "getUniqueId"->id;case "getType"->type;default->throw new AssertionError("Unexpected world/entity call: "+m.getName());});}
    EntityBreedEvent event(EntityType type){return new EntityBreedEvent(animal(new UUID(0,3),type),animal(a,type),animal(b,type),null,null,0);}
    ManagedLivestockBreedingGuard guard(Set<UUID> ids,boolean enabled){return new ManagedLivestockBreedingGuard(()->ids,()->enabled,m->false,m->false);}
    @Test void eitherVillageParentBlocksUnaccountedCowOrChickenBirth() {
        for(var species:List.of(EntityType.COW,EntityType.CHICKEN))for(var owner:Set.of(a,b)){var e=event(species);guard(Set.of(owner),true).breed(e);assertTrue(e.isCancelled());}
    }
    @Test void playerUnclaimedAndUnsupportedSpeciesKeepNormalBreeding() {
        var player=event(EntityType.COW);guard(Set.of(),true).breed(player);assertFalse(player.isCancelled());
        var pig=event(EntityType.PIG);guard(Set.of(a),true).breed(pig);assertFalse(pig.isCancelled());
    }
    @Test void disabledGuardAndPreviouslyCancelledEventsRemainUnchanged() {
        var e=event(EntityType.COW);guard(Set.of(a),false).breed(e);assertFalse(e.isCancelled());
        e.setCancelled(true);guard(Set.of(),true).breed(e);assertTrue(e.isCancelled());
    }
    @Test void newlyPublishedPositiveIdentityIsUsedWithoutSqlOrWorldAccess() {
        var ids=new AtomicReference<Set<UUID>>(Set.of());var guard=new ManagedLivestockBreedingGuard(ids::get,()->true,m->false,m->false);
        var e=event(EntityType.COW);guard.breed(e);assertFalse(e.isCancelled());ids.set(Set.of(a));guard.breed(e);assertTrue(e.isCancelled());
    }
    PlayerInteractEntityEvent interaction(Material material,EquipmentSlot hand,boolean village) {
        var item=new ItemStack(){@Override public Material getType(){return material;}@Override public int getAmount(){return 1;}};
        var inventory=(PlayerInventory)Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),new Class<?>[]{PlayerInventory.class},(p,m,args)->{if(m.getName().equals("getItem")){assertEquals(hand,args[0]);return item;}throw new AssertionError("Unexpected inventory write/read: "+m.getName());});
        var player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,args)->switch(m.getName()){case "getInventory"->inventory;case "sendMessage"->null;default->throw new AssertionError(m.getName());});
        var e=new PlayerInteractEntityEvent(player,animal(village?a:b,EntityType.COW),hand);
        new ManagedLivestockBreedingGuard(()->Set.of(a),()->true,m->m==Material.WHEAT,m->m==Material.WHEAT_SEEDS).interact(e);return e;
    }
    @Test void breedingFoodInEitherHandIsCancelledWithoutInventoryMutation() {
        for(var hand:List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND))assertTrue(interaction(Material.WHEAT,hand,true).isCancelled());
        assertFalse(interaction(Material.WHEAT,EquipmentSlot.HAND,false).isCancelled());
    }
    @Test void milkingNamingAndLeashingRemainAvailableForVillageAnimals() {
        for(var item:List.of(Material.BUCKET,Material.NAME_TAG,Material.LEAD))assertFalse(interaction(item,EquipmentSlot.HAND,true).isCancelled());
    }
}
