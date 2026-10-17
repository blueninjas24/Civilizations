package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PendingFoodPurchase;
import com.blueninjas24.civilizations.application.PendingFoodBarter;
import com.blueninjas24.civilizations.application.PendingLivestockSale;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;

/** Prevent spending/moving an unverified payout into a separately saved chest, dropped entity or vanilla trade. */
public final class PendingTradeInventoryGuard implements Listener {
    static boolean locked(Player player) {
        if(player.getPersistentDataContainer().has(LivestockSaleTransfers.KEY)) {
            try {
                var receipt=PendingLivestockSale.decode(player.getPersistentDataContainer().get(LivestockSaleTransfers.KEY,PersistentDataType.STRING));
                if(receipt.stage()==PendingLivestockSale.Stage.EXCHANGED||receipt.stage()==PendingLivestockSale.Stage.REVIEW)return true;
            }catch(RuntimeException malformed){return true;}
        }
        if(player.getPersistentDataContainer().has(FoodBarterTransfers.KEY)) {
            try {
                var receipt=PendingFoodBarter.decode(player.getPersistentDataContainer().get(FoodBarterTransfers.KEY,PersistentDataType.STRING));
                if(receipt.stage()==PendingFoodBarter.Stage.EXCHANGED||receipt.stage()==PendingFoodBarter.Stage.REVIEW)return true;
            }catch(RuntimeException malformed){return true;}
        }
        if (!player.getPersistentDataContainer().has(FoodPurchaseTransfers.KEY)) return false;
        try {
            var receipt=PendingFoodPurchase.decode(player.getPersistentDataContainer().get(FoodPurchaseTransfers.KEY,PersistentDataType.STRING));
            return receipt.stage()==PendingFoodPurchase.Stage.EXCHANGED || receipt.stage()==PendingFoodPurchase.Stage.REVIEW;
        } catch(RuntimeException malformed){return true;}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void click(InventoryClickEvent event) {
        if(event.getWhoClicked() instanceof Player player && locked(player))event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void drag(InventoryDragEvent event) {
        if(event.getWhoClicked() instanceof Player player && locked(player))event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void drop(PlayerDropItemEvent event) {
        if(locked(event.getPlayer()))event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent event) {
        if(locked(event.getPlayer()))event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void interact(PlayerInteractEvent event) {
        if(locked(event.getPlayer()))event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entity(PlayerInteractEntityEvent event) {
        if(locked(event.getPlayer()))event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void recipe(com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent event) {
        if(locked(event.getPlayer()))event.setCancelled(true);
    }
}
