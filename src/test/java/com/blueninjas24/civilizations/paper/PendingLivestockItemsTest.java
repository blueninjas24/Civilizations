package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PendingLivestockSale;
import com.blueninjas24.civilizations.industry.LivestockMarketPolicy;
import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PendingLivestockItemsTest {
    final Terms terms=LivestockMarketPolicy.prototype().terms(new UUID(1,1),new UUID(2,2),new UUID(3,3),new UUID(4,4),new UUID(5,5),Product.EGGS);
    final Map<NamespacedKey,Object> data=new HashMap<>();
    final PendingLivestockItems listener=new PendingLivestockItems();
    static PersistentDataContainer pdc(Map<NamespacedKey,Object> data) {
        return (PersistentDataContainer)Proxy.newProxyInstance(PersistentDataContainer.class.getClassLoader(),new Class<?>[]{PersistentDataContainer.class},(p,m,a)->switch(m.getName()){
            case "has"->data.containsKey(a[0]);case "get"->data.get(a[0]);case "set"->{data.put((NamespacedKey)a[0],a[2]);yield null;}
            case "remove"->{data.remove(a[0]);yield null;}default->throw new AssertionError(m.getName());
        });
    }
    static final class Meta implements InvocationHandler {
        final Map<NamespacedKey,Object> data;
        Meta(Map<NamespacedKey,Object> data){this.data=new HashMap<>(data);}
        ItemMeta proxy(){return (ItemMeta)Proxy.newProxyInstance(ItemMeta.class.getClassLoader(),new Class<?>[]{ItemMeta.class},this);}
        public Object invoke(Object proxy,Method method,Object[] args){
            if(method.getName().equals("getPersistentDataContainer"))return pdc(data);throw new AssertionError(method.getName());
        }
    }
    static final class Stack extends ItemStack {
        final Material material;int count;Map<NamespacedKey,Object> data=new HashMap<>();boolean rejectMeta;
        Stack(Material material,int count){super();this.material=material;this.count=count;}
        @Override public Material getType(){return material;}@Override public int getAmount(){return count;}@Override public void setAmount(int count){this.count=count;}
        @Override public boolean hasItemMeta(){return !data.isEmpty();}@Override public ItemMeta getItemMeta(){return new Meta(data).proxy();}
        @Override public boolean setItemMeta(ItemMeta meta){if(rejectMeta)return false;data=new HashMap<>(((Meta)Proxy.getInvocationHandler(meta)).data);return true;}
        @Override public ItemStack clone(){var clone=new Stack(material,count);clone.data=new HashMap<>(data);clone.rejectMeta=rejectMeta;return clone;}
        @Override public boolean equals(Object other){return other instanceof Stack s&&material==s.material&&count==s.count&&data.equals(s.data);}
        @Override public int hashCode(){return Objects.hash(material,count,data);}
    }
    Player player(){return (Player)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->terms.player();case "getPersistentDataContainer"->pdc(data);default->throw new AssertionError(m.getName());
    });}
    PlayerDeathEvent event(ItemStack... drops) {
        var damage=(DamageSource)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{DamageSource.class},(p,m,a)->{throw new AssertionError(m.getName());});
        return new PlayerDeathEvent(player(),damage,new ArrayList<>(Arrays.asList(drops)),7,Component.text("Ordinary death"),true);
    }
    void receipt(PendingLivestockSale.Stage stage){data.put(PendingLivestockItems.RECEIPT_KEY,new PendingLivestockSale(terms,stage).encode());}
    Stack marked(int count){var stack=new Stack(Material.EGG,count);PendingLivestockItems.mark(stack,terms);return stack;}
    @Test void onlyExactOperationPlayerAndProductGoodsAreRetainedAndAllOtherDropsStayOrdinary() {
        receipt(PendingLivestockSale.Stage.EXCHANGED);var bought=marked(4);var oldEggs=new Stack(Material.EGG,12);var sword=new Stack(Material.DIAMOND_SWORD,1);
        var different=marked(4);different.data.put(PendingLivestockItems.ITEM_KEY,UUID.randomUUID()+"|"+terms.player());
        var e=event(oldEggs,bought,sword,different);listener.death(e);
        assertEquals(List.of(oldEggs,sword,different),e.getDrops());assertEquals(List.of(bought),e.getItemsToKeep());assertFalse(e.getKeepInventory());assertEquals(7,e.getDroppedExp());assertEquals(Component.text("Ordinary death"),e.deathMessage());
    }
    @Test void existingKeepEntriesAreNotDuplicatedAndSplitOrdersAreRetainedExactlyOnce() {
        receipt(PendingLivestockSale.Stage.EXCHANGED);var a=marked(2);var b=marked(2);var e=event(a,b);e.getItemsToKeep().add(a.clone());listener.death(e);
        assertTrue(e.getDrops().isEmpty());assertEquals(4,e.getItemsToKeep().stream().mapToInt(ItemStack::getAmount).sum());
        e=event(marked(4));e.getItemsToKeep().add(marked(4));listener.death(e);assertEquals(1,e.getItemsToKeep().size());assertTrue(e.getDrops().isEmpty());
    }
    @Test void keepInventoryRuleIsNeverChangedAndMatchingDropsAreRemovedWithoutExtraKeepCopies() {
        receipt(PendingLivestockSale.Stage.EXCHANGED);var sword=new Stack(Material.DIAMOND_SWORD,1);var e=event(marked(4),sword);e.setKeepInventory(true);listener.death(e);
        assertTrue(e.getKeepInventory());assertTrue(e.getItemsToKeep().isEmpty());assertEquals(List.of(sword),e.getDrops());
    }
    @Test void noReceiptPreparedCancelledMalformedOrWrongPlayerNeverGrantExtraRetention() {
        var marked=marked(4);
        for(var stage:List.of(PendingLivestockSale.Stage.PREPARED,PendingLivestockSale.Stage.CANCELLED)) {
            receipt(stage);var e=event(marked);listener.death(e);assertTrue(e.getItemsToKeep().isEmpty());assertEquals(List.of(marked),e.getDrops());
        }
        for(Object raw:List.of("invalid",42,new PendingLivestockSale(new Terms(terms.operation(),UUID.randomUUID(),terms.settlement(),terms.world(),terms.seller(),Product.EGGS,4,1,800),PendingLivestockSale.Stage.EXCHANGED).encode())) {
            data.put(PendingLivestockItems.RECEIPT_KEY,raw);var e=event(marked);listener.death(e);assertTrue(e.getItemsToKeep().isEmpty());
        }
        data.clear();var e=event(marked);listener.death(e);assertTrue(e.getItemsToKeep().isEmpty());
    }
    @Test void excessUnexpectedMarkedGoodsArePreservedAsOrdinaryDropsRatherThanDeletedOrKept() {
        receipt(PendingLivestockSale.Stage.REVIEW);var e=event(marked(9));listener.death(e);
        assertEquals(4,e.getItemsToKeep().getFirst().getAmount());assertEquals(5,e.getDrops().getFirst().getAmount());assertFalse(PendingLivestockItems.matches(e.getDrops().getFirst(),terms));
    }
    @Test void metadataFailureDoesNotRemoveAnyDropsBeforeAllRetentionCopiesArePrepared() {
        receipt(PendingLivestockSale.Stage.EXCHANGED);var extra=marked(9);extra.rejectMeta=true;var e=event(extra);
        assertThrows(IllegalStateException.class,()->listener.death(e));assertEquals(List.of(extra),e.getDrops());assertTrue(e.getItemsToKeep().isEmpty());
    }
    @Test void pendingMilkCannotBeConsumedButAnOrdinaryMilkBucketCan() {
        var milk=LivestockMarketPolicy.prototype().terms(UUID.randomUUID(),terms.player(),terms.settlement(),terms.world(),terms.seller(),Product.MILK);
        var item=new Stack(Material.MILK_BUCKET,1);PendingLivestockItems.mark(item,milk);var e=new PlayerItemConsumeEvent(player(),item,EquipmentSlot.HAND);listener.consume(e);assertTrue(e.isCancelled());
        e=new PlayerItemConsumeEvent(player(),new Stack(Material.MILK_BUCKET,1),EquipmentSlot.HAND);listener.consume(e);assertFalse(e.isCancelled());
    }
    @Test void recipeBookCannotMoveAnUnverifiedPurchaseIntoTheCraftingGrid() {
        var guard=new PendingTradeInventoryGuard();var key=new NamespacedKey("minecraft","cake");
        receipt(PendingLivestockSale.Stage.EXCHANGED);
        var event=new com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent(player(),key,true);guard.recipe(event);assertTrue(event.isCancelled());
        data.clear();event=new com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent(player(),key,true);guard.recipe(event);assertFalse(event.isCancelled());
    }
    @Test void releaseRemovesOnlyOwnMarkerAfterExactCountAndRestoresAfterPartialWriteFailure() {
        var customKey=new NamespacedKey("fixture","name");var item=marked(4);item.data.put(customKey,"Preserved metadata");ItemStack[][] slots={new ItemStack[]{item,null}};boolean[] fail={true};
        var inventory=(PlayerInventory)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PlayerInventory.class},(p,m,a)->switch(m.getName()){
            case "getStorageContents"->slots[0].clone();case "setStorageContents"->{slots[0]=((ItemStack[])a[0]).clone();if(fail[0]){fail[0]=false;throw new IllegalStateException("Injected release write");}yield null;}
            default->throw new AssertionError(m.getName());
        });
        assertThrows(IllegalStateException.class,()->PendingLivestockItems.release(inventory,terms));assertEquals(4,PendingLivestockItems.count(inventory,terms));
        PendingLivestockItems.release(inventory,terms);assertEquals(0,PendingLivestockItems.count(inventory,terms));assertEquals("Preserved metadata",((Stack)slots[0][0]).data.get(customKey));
        assertThrows(IllegalStateException.class,()->PendingLivestockItems.release(inventory,terms));assertEquals(4,slots[0][0].getAmount());
    }
}
