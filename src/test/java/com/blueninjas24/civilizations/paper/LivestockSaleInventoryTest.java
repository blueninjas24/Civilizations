package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import org.bukkit.Material;
import org.bukkit.inventory.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockSaleInventoryTest {
    private static final class Stack extends ItemStack {
        final Material type;final int count;final String custom;
        Stack(Material type,int count){this(type,count,"");}
        Stack(Material type,int count,String custom){super();this.type=type;this.count=count;this.custom=custom;}
        @Override public Material getType(){return type;}@Override public int getAmount(){return count;}
        @Override public int getMaxStackSize(){return switch(type){case EGG,BUCKET->16;case MILK_BUCKET,DIAMOND_SWORD->1;default->64;};}
        @Override public boolean hasItemMeta(){return !custom.isEmpty();}@Override public ItemStack clone(){return new Stack(type,count,custom);}
        @Override public boolean equals(Object o){return o instanceof Stack s&&type==s.type&&count==s.count&&custom.equals(s.custom);}
        @Override public int hashCode(){return Objects.hash(type,count,custom);}
    }
    ItemStack[] items=new ItemStack[36];int writes;boolean failOnce,ignoreWrites;
    PlayerInventory inventory(){return (PlayerInventory)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PlayerInventory.class},(p,m,a)->switch(m.getName()){
        case "getStorageContents"->items.clone();
        case "setStorageContents"->{writes++;var incoming=(ItemStack[])a[0];if(failOnce){failOnce=false;items[0]=incoming[0];throw new IllegalStateException("Fixture partial inventory write");}if(!ignoreWrites)items=incoming.clone();yield null;}
        default->throw new AssertionError(m.getName());
    });}
    Terms terms(Product product,int count){return new Terms(new UUID(1,1),new UUID(2,2),new UUID(3,3),new UUID(4,4),new UUID(5,5),product,count,2,800);}
    LivestockSaleInventory.Exchange plan(Product product,int count){return LivestockSaleInventory.plan(inventory(),terms(product,count),Stack::new);}
    @Test void atomicMilkPlanUsesStorageOnlyAndPreservesUnchangedCustomMetadata() {
        items[0]=new Stack(Material.EMERALD,5);items[1]=new Stack(Material.BUCKET,16);items[2]=new Stack(Material.DIAMOND_SWORD,1,"Valuable named sword");
        var e=plan(Product.MILK,1);assertEquals(0,writes);e.apply(inventory());
        assertEquals(new Stack(Material.EMERALD,3),items[0]);assertEquals(new Stack(Material.BUCKET,15),items[1]);assertEquals(new Stack(Material.DIAMOND_SWORD,1,"Valuable named sword"),items[2]);assertEquals(new Stack(Material.MILK_BUCKET,1),items[3]);assertEquals(1,writes);
    }
    @Test void actualStackMaximumSplitsLargeEggOrderWithoutIllegalStacks() {
        items[0]=new Stack(Material.EMERALD,2);var e=plan(Product.EGGS,64);e.apply(inventory());int eggs=0,stacks=0;
        for(var item:items)if(item!=null&&item.getType()==Material.EGG){eggs+=item.getAmount();stacks++;assertTrue(item.getAmount()<=item.getMaxStackSize());}
        assertEquals(64,eggs);assertEquals(4,stacks);
    }
    @Test void partialWriteCanRestoreTheExactSnapshotWithoutASecondPayment() {
        items[0]=new Stack(Material.EMERALD,5);items[1]=new Stack(Material.BUCKET,1);var before=items.clone();var e=plan(Product.MILK,1);failOnce=true;
        assertThrows(IllegalStateException.class,()->e.apply(inventory()));e.restore(inventory());assertArrayEquals(before,items);assertEquals(2,writes);
    }
    @Test void changedInventoryIsNotOverwrittenByFailedApplyOrItsRestore() {
        items[0]=new Stack(Material.EMERALD,5);items[1]=new Stack(Material.BUCKET,1);var e=plan(Product.MILK,1);items[5]=new Stack(Material.DIAMOND_SWORD,1,"New item");var changed=items.clone();
        assertThrows(IllegalStateException.class,()->e.apply(inventory()));e.restore(inventory());assertEquals(0,writes);assertArrayEquals(changed,items);
    }
    @Test void silentInventoryProviderFailureIsDetectedAndInvalidFactoriesNeverWrite() {
        items[0]=new Stack(Material.EMERALD,5);items[1]=new Stack(Material.BUCKET,1);var e=plan(Product.MILK,1);ignoreWrites=true;
        assertThrows(IllegalStateException.class,()->e.apply(inventory()));e.restore(inventory());
        int before=writes;assertThrows(IllegalArgumentException.class,()->LivestockSaleInventory.plan(inventory(),terms(Product.MILK,1),(material,count)->new Stack(material,count,"Unexpected custom output")));assertEquals(before,writes);
    }
}
