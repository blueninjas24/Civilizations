package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PendingLivestockSale;
import com.blueninjas24.civilizations.industry.LivestockProductSale.Terms;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Only separately marked purchased goods get temporary retention. Existing goods and death rules stay ordinary. */
public final class PendingLivestockItems implements Listener {
    public static final NamespacedKey ITEM_KEY=Objects.requireNonNull(NamespacedKey.fromString("civilizations:pending_livestock_goods"));
    public static final NamespacedKey RECEIPT_KEY=Objects.requireNonNull(NamespacedKey.fromString(PendingLivestockSale.KEY));
    private static String identity(Terms terms){return terms.operation()+"|"+terms.player();}
    public static void mark(ItemStack item,Terms terms) {
        if(!item.getType().name().equals(terms.product().item()))throw new IllegalArgumentException("Wrong pending product");
        var meta=Objects.requireNonNull(item.getItemMeta());meta.getPersistentDataContainer().set(ITEM_KEY,PersistentDataType.STRING,identity(terms));
        if(!item.setItemMeta(meta))throw new IllegalStateException("Pending product marker not accepted");
    }
    public static boolean matches(ItemStack item,Terms terms) {
        if(item==null||item.getAmount()<1||!item.getType().name().equals(terms.product().item())||!item.hasItemMeta())return false;
        try{return identity(terms).equals(item.getItemMeta().getPersistentDataContainer().get(ITEM_KEY,PersistentDataType.STRING));}
        catch(IllegalArgumentException invalid){return false;}
    }
    private static boolean tagged(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(ITEM_KEY);}
    public static long count(PlayerInventory inventory,Terms terms) {
        long count=0;for(var item:inventory.getStorageContents())if(matches(item,terms))count=Math.addExact(count,item.getAmount());return count;
    }
    /** Removes only our marker, never creates another payout. Caller has confirmed the ledger and saved exchange first. */
    public static void release(PlayerInventory inventory,Terms terms) {
        if(count(inventory,terms)!=terms.count())throw new IllegalStateException("Pending product count needs review");
        var before=copy(inventory.getStorageContents());var after=copy(before);
        for(var item:after)if(matches(item,terms)) {
            var meta=item.getItemMeta();meta.getPersistentDataContainer().remove(ITEM_KEY);
            if(!item.setItemMeta(meta))throw new IllegalStateException("Pending product release not accepted");
        }
        try {
            inventory.setStorageContents(copy(after));
            if(!Arrays.equals(copy(inventory.getStorageContents()),after))throw new IllegalStateException("Product release not confirmed");
        }catch(RuntimeException failure) {
            try{inventory.setStorageContents(copy(before));if(!Arrays.equals(copy(inventory.getStorageContents()),before))throw new IllegalStateException("Release restore not confirmed");}
            catch(RuntimeException restore){failure.addSuppressed(restore);}throw failure;
        }
    }
    private static ItemStack[] copy(ItemStack[] contents){var result=new ItemStack[contents.length];for(int i=0;i<contents.length;i++)result[i]=contents[i]==null?null:contents[i].clone();return result;}
    private static PendingLivestockSale pending(Player player) {
        try {
            var raw=player.getPersistentDataContainer().get(RECEIPT_KEY,PersistentDataType.STRING);if(raw==null)return null;
            var receipt=PendingLivestockSale.decode(raw);
            return receipt.terms().player().equals(player.getUniqueId())&&(receipt.stage()==PendingLivestockSale.Stage.EXCHANGED||receipt.stage()==PendingLivestockSale.Stage.REVIEW)?receipt:null;
        }catch(RuntimeException invalid){return null;}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void consume(PlayerItemConsumeEvent event){if(tagged(event.getItem()))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)
    public void death(PlayerDeathEvent event) {
        var receipt=pending(event.getEntity());if(receipt==null)return;var terms=receipt.terms();
        long kept=event.getItemsToKeep().stream().filter(item->matches(item,terms)).mapToLong(ItemStack::getAmount).sum();
        long covered=kept;
        // Prepare all copies first: a metadata/provider failure must not silently discard death drops.
        var remaining=new ArrayList<ItemStack>();var retainItems=new ArrayList<ItemStack>();
        for(var drop:event.getDrops()) {
            if(!matches(drop,terms)){remaining.add(drop);continue;}
            if(event.getKeepInventory())continue;
            long already=Math.min(covered,drop.getAmount());covered-=already;
            int needed=(int)Math.min(drop.getAmount()-already,Math.max(0,terms.count()-kept));
            if(needed>0){var retain=drop.clone();retain.setAmount(needed);retainItems.add(retain);kept+=needed;}
            int remainder=(int)(drop.getAmount()-already-needed);
            if(remainder>0) {
                // Unexpected extra marked quantities are not part of this purchase; preserve them under normal death rules.
                var ordinary=drop.clone();ordinary.setAmount(remainder);var meta=ordinary.getItemMeta();meta.getPersistentDataContainer().remove(ITEM_KEY);
                if(!ordinary.setItemMeta(meta))throw new IllegalStateException("Extra product marker removal not accepted");remaining.add(ordinary);
            }
        }
        event.getDrops().clear();event.getDrops().addAll(remaining);event.getItemsToKeep().addAll(retainItems);
    }
}
