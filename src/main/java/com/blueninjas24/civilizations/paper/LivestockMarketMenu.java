package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.LivestockMarket.Offer;
import com.blueninjas24.civilizations.industry.LivestockMarketPolicy;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Explicit player retail orders, not automatic harvesting. Quotes are finite saved stock, never a promise of output. */
public final class LivestockMarketMenu extends BukkitRunnable implements CommandExecutor,Listener,AutoCloseable {
    private final Plugin plugin;
    private final PopulationSession session;
    private final LivestockSaleTransfers transfers;
    private final LivestockMarketPolicy policy;
    private final Map<UUID,CompletableFuture<List<Offer>>> pending=new LinkedHashMap<>();
    private static final class Menu implements InventoryHolder {
        final UUID player;final List<Offer> offers;Inventory inventory;boolean used;
        Menu(UUID player,List<Offer> offers){this.player=player;this.offers=List.copyOf(offers);}
        public Inventory getInventory(){return inventory;}
    }
    public LivestockMarketMenu(Plugin plugin,PopulationSession session,LivestockSaleTransfers transfers,LivestockMarketPolicy policy) {
        this.plugin=plugin;this.session=session;this.transfers=transfers;this.policy=policy;
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player player)){sender.sendMessage("Open /civ market in game near a livestock business.");return true;}
        if(!player.hasPermission("civilizations.trade")){player.sendMessage("You do not have permission to trade.");return true;}
        if(args.length!=1){player.sendMessage("Use /civ market near a livestock business.");return true;}
        if(!policy.enabled()||!session.status().ready()){player.sendMessage("Livestock market is disabled or not ready. Admins can enable livestock-market in economy.yml.");return true;}
        if(player.isDead()||LivestockSaleTransfers.pending(player)){player.sendMessage("Finish the previous transfer before opening the market.");return true;}
        if(pending.size()>=16||pending.containsKey(player.getUniqueId())){player.sendMessage("Market stock is being checked. Please wait.");return true;}
        pending.put(player.getUniqueId(),session.livestockOffers(player.getUniqueId(),LivestockSaleTransfers.position(player.getLocation())));
        player.sendMessage("Checking nearby livestock businesses for recorded milk and eggs.");return true;
    }
    @Override public void run() {
        int budget=4;
        for(var iterator=pending.entrySet().iterator();iterator.hasNext()&&budget-->0;) {
            var entry=iterator.next();var player=plugin.getServer().getPlayer(entry.getKey());
            if(player==null||!player.isOnline()){iterator.remove();continue;}
            if(!entry.getValue().isDone())continue;iterator.remove();
            if(player.isDead()||!player.hasPermission("civilizations.trade")||LivestockSaleTransfers.pending(player))continue;
            try {
                var offers=entry.getValue().join();
                if(offers.isEmpty()){player.sendMessage("No nearby staffed livestock business has a full order in stock. Owning animals alone does not create sale stock.");continue;}
                if(player.getItemOnCursor()!=null&&player.getItemOnCursor().getAmount()>0){player.sendMessage("Put the item on your cursor away, then reopen /civ market.");continue;}
                open(player,offers);
            }catch(RuntimeException failure){player.sendMessage("Market stock is temporarily unavailable. No items or payment moved.");plugin.getLogger().warning("Livestock market quote failed: "+failure.getClass().getSimpleName());}
        }
    }
    private void open(Player player,List<Offer> offers) {
        var menu=new Menu(player.getUniqueId(),offers);
        menu.inventory=Bukkit.createInventory(menu,54,Component.text("Village livestock market"));
        for(int slot=0;slot<Math.min(32,offers.size());slot++) {
            var offer=offers.get(slot);var terms=offer.terms();var item=new ItemStack(Material.valueOf(terms.product().item()));var meta=item.getItemMeta();
            meta.displayName(Component.text("Buy "+terms.count()+" "+terms.product().name().toLowerCase(Locale.ROOT)));
            meta.lore(List.of(Component.text(offer.businessName()),Component.text("Price: "+terms.emeralds()+" emeralds"),
                    Component.text(terms.emptyBuckets()>0?"Bring one plain empty bucket.":"Bring plain emeralds and free inventory space."),
                    Component.text("Recorded stock: "+offer.availableUnits()+" units."),Component.text("Click once to buy. Offers expire; stock is rechecked.")));
            item.setItemMeta(meta);menu.inventory.setItem(slot,item);
        }
        var notice=new ItemStack(Material.PAPER);var meta=notice.getItemMeta();meta.displayName(Component.text("Goods come from real business stock"));
        meta.lore(List.of(Component.text("Prices and order sizes are editable in economy.yml."),Component.text("Only the new order is held during save confirmation."),
                Component.text("Other items follow the server's usual death rules.")));
        notice.setItemMeta(meta);menu.inventory.setItem(49,notice);player.openInventory(menu.inventory);
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if(!(event.getView().getTopInventory().getHolder() instanceof Menu menu))return;event.setCancelled(true);
        int slot=event.getRawSlot();
        if(menu.used||!event.isLeftClick()||slot<0||slot>=Math.min(32,menu.offers.size())||!(event.getWhoClicked() instanceof Player player)||!menu.player.equals(player.getUniqueId()))return;
        menu.used=true;
        plugin.getServer().getScheduler().runTask(plugin,()->{
            if(!player.isOnline()||player.getOpenInventory().getTopInventory()!=menu.inventory)return;
            if(player.getItemOnCursor()!=null&&player.getItemOnCursor().getAmount()>0){player.sendMessage("Put the item on your cursor away before buying.");return;}
            player.closeInventory();transfers.buy(player,menu.offers.get(slot));
        });
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof Menu)event.setCancelled(true);}
    @Override public void close(){pending.clear();}
}
