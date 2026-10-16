package com.blueninjas24.civilizations.paper;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class ManagedLivestockProductsTest {
    UUID village=new UUID(0,1),other=new UUID(0,2);int messages;
    <T>T proxy(Class<T> type,java.util.function.BiFunction<String,Object[],Object> answer) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->answer.apply(m.getName(),a)));
    }
    Entity entity(UUID id,EntityType type) {return proxy(Entity.class,(m,a)->switch(m){case "getType"->type;case "getUniqueId"->id;default->throw new AssertionError("Unexpected entity/world access: "+m);});}
    ItemStack stack(Material material){return new ItemStack(){@Override public Material getType(){return material;}@Override public int getAmount(){return 1;}};}
    PlayerInteractEntityEvent interaction(Entity entity,Material material,EquipmentSlot hand) {
        var item=material==null?null:stack(material);
        var inv=proxy(PlayerInventory.class,(m,a)->{assertEquals("getItem",m);assertEquals(hand,a[0]);return item;});
        var player=proxy(Player.class,(m,a)->switch(m){case "getInventory"->inv;case "sendMessage"->{messages++;yield null;}default->throw new AssertionError("Unexpected player write/read: "+m);});
        return new PlayerInteractEntityEvent(player,entity,hand);
    }
    EntityDropItemEvent drop(UUID id,EntityType type,Material material) {
        var item=proxy(Item.class,(m,a)->{assertEquals("getItemStack",m);return stack(material);});
        return new EntityDropItemEvent(entity(id,type),item);
    }
    ManagedLivestockProducts guard(boolean enabled){return new ManagedLivestockProducts(()->Set.of(village),()->enabled);}
    @Test void villageMilkingStopsInEitherHandWithoutChangingBucketsOrAwardingGoods() {
        for(var hand:List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND)) {
            var e=interaction(entity(village,EntityType.COW),Material.BUCKET,hand);guard(true).milk(e);
            assertTrue(e.isCancelled());assertEquals(1,e.getPlayer().getInventory().getItem(hand).getAmount());
        }
        assertEquals(2,messages);
    }
    @ParameterizedTest @EnumSource(value=Material.class,names={"EGG","BROWN_EGG","BLUE_EGG"})
    void allVillageChickenEggVariantsStopBeforeWorldDrop(Material material) {
        var e=drop(village,EntityType.CHICKEN,material);guard(true).egg(e);assertTrue(e.isCancelled());
        assertEquals(1,e.getItemDrop().getItemStack().getAmount());
    }
    @Test void playerUnclaimedAndUnsupportedAnimalsAndOtherItemsStayNormal() {
        for(var kind:List.of(EntityType.COW,EntityType.CHICKEN,EntityType.MOOSHROOM)) {
            var e=interaction(entity(other,kind),Material.BUCKET,EquipmentSlot.HAND);guard(true).milk(e);assertFalse(e.isCancelled());
        }
        for(var material:List.of(Material.NAME_TAG,Material.LEAD,Material.WHEAT,Material.MILK_BUCKET)) {
            var e=interaction(entity(village,EntityType.COW),material,EquipmentSlot.HAND);guard(true).milk(e);assertFalse(e.isCancelled());
        }
        for(var kind:List.of(EntityType.CHICKEN,EntityType.MOOSHROOM)) {
            var e=interaction(entity(village,kind),Material.BUCKET,EquipmentSlot.HAND);guard(true).milk(e);assertFalse(e.isCancelled());
        }
        var empty=interaction(entity(village,EntityType.COW),null,EquipmentSlot.HAND);guard(true).milk(empty);assertFalse(empty.isCancelled());assertEquals(0,messages);
        for(var egg:List.of(Material.EGG,Material.BROWN_EGG,Material.BLUE_EGG)) {
            var unclaimed=drop(other,EntityType.CHICKEN,egg);guard(true).egg(unclaimed);assertFalse(unclaimed.isCancelled());
            var player=drop(village,EntityType.PLAYER,egg);guard(true).egg(player);assertFalse(player.isCancelled());
        }
        var feather=drop(village,EntityType.CHICKEN,Material.FEATHER);guard(true).egg(feather);assertFalse(feather.isCancelled());
    }
    @Test void disabledModeAndOtherPluginsCancellationArePreserved() {
        var milk=interaction(entity(village,EntityType.COW),Material.BUCKET,EquipmentSlot.HAND);guard(false).milk(milk);assertFalse(milk.isCancelled());
        var egg=drop(village,EntityType.CHICKEN,Material.EGG);guard(false).egg(egg);assertFalse(egg.isCancelled());
        milk.setCancelled(true);guard(false).milk(milk);assertTrue(milk.isCancelled());assertEquals(0,messages);
        egg.setCancelled(true);guard(false).egg(egg);assertTrue(egg.isCancelled());
    }
    @Test void committedIdentityPublicationTakesEffectWithoutSqlOrChunkAccess() {
        var ids=new AtomicReference<Set<UUID>>(Set.of());var g=new ManagedLivestockProducts(ids::get,()->true);
        var before=drop(village,EntityType.CHICKEN,Material.EGG);g.egg(before);assertFalse(before.isCancelled());
        ids.set(Set.of(village));var after=drop(village,EntityType.CHICKEN,Material.EGG);g.egg(after);assertTrue(after.isCancelled());
        ids.set(Set.of());var released=drop(village,EntityType.CHICKEN,Material.EGG);g.egg(released);assertFalse(released.isCancelled());
    }
}
