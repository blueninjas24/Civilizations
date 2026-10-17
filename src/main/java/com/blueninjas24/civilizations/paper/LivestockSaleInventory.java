package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.LivestockInventory;
import com.blueninjas24.civilizations.industry.LivestockProductSale.Terms;
import org.bukkit.Material;
import org.bukkit.inventory.*;
import java.util.*;
import java.util.function.BiFunction;

/** Main-thread storage snapshot/plan. Preserves full metadata of unchanged items, never drops overflow. */
public final class LivestockSaleInventory {
    private LivestockSaleInventory() {}
    public static final class Exchange {
        private final ItemStack[] before, after;
        private boolean attempted;
        private Exchange(ItemStack[] before, ItemStack[] after) { this.before=copy(before);this.after=copy(after); }
        public void apply(PlayerInventory inventory) {
            if(attempted)throw new IllegalStateException("Inventory exchange already attempted");
            if(!same(inventory.getStorageContents(),before))throw new IllegalStateException("Inventory changed before exchange");
            attempted=true;
            inventory.setStorageContents(copy(after));
            if(!same(inventory.getStorageContents(),after))throw new IllegalStateException("Inventory exchange not confirmed");
        }
        /** Only after an attempted apply in the same main-thread operation. Caller retains REVIEW if this cannot be proved. */
        public void restore(PlayerInventory inventory) {
            if(!attempted)return;
            inventory.setStorageContents(copy(before));
            if(!same(inventory.getStorageContents(),before))throw new IllegalStateException("Inventory restore not confirmed");
        }
    }
    public static Exchange plan(PlayerInventory inventory,Terms terms) { return plan(inventory,terms,ItemStack::new); }
    static Exchange plan(PlayerInventory inventory,Terms terms,BiFunction<Material,Integer,ItemStack> factory) {
        var before=copy(inventory.getStorageContents());
        if(before.length!=36)throw new IllegalArgumentException("Expected 36 main storage slots");
        var slots=new ArrayList<LivestockInventory.Slot>();
        for(var item:before) {
            if(empty(item)) {slots.add(LivestockInventory.Slot.EMPTY);continue;}
            int max=item.getMaxStackSize();
            slots.add(new LivestockInventory.Slot(item.getType().name(),item.getAmount(),max,!item.hasItemMeta()&&item.getAmount()<=max));
        }
        var output=Objects.requireNonNull(factory.apply(Material.valueOf(terms.product().item()),1));
        var plan=LivestockInventory.plan(slots,terms,output.getMaxStackSize());
        var after=copy(before);
        for(int i=0;i<after.length;i++)if(!plan.before().get(i).equals(plan.after().get(i))) {
            var target=plan.after().get(i);
            if(target.count()==0)after[i]=null;
            else {
                var item=Objects.requireNonNull(factory.apply(Material.valueOf(target.item()),target.count()));
                if(!item.getType().name().equals(target.item())||item.getAmount()!=target.count()||item.hasItemMeta()||item.getAmount()>item.getMaxStackSize())
                    throw new IllegalArgumentException("Inventory provider returned an invalid ordinary stack");
                after[i]=item;
            }
        }
        return new Exchange(before,after);
    }
    private static boolean empty(ItemStack item) {
        if(item==null||item.getAmount()==0)return true;
        // Storage emptiness does not require consulting the live block registry for every slot.
        var type=item.getType();return type==Material.AIR||type==Material.CAVE_AIR||type==Material.VOID_AIR;
    }
    private static ItemStack[] copy(ItemStack[] slots) {
        var result=new ItemStack[slots.length];for(int i=0;i<slots.length;i++)result[i]=empty(slots[i])?null:slots[i].clone();return result;
    }
    private static boolean same(ItemStack[] actual,ItemStack[] expected) {return Arrays.equals(copy(actual),expected);}
}
