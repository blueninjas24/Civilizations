package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.EntityKey;
import com.blueninjas24.civilizations.economy.FoodAidPreview;
import com.blueninjas24.civilizations.population.Resident;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Villager;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

/** Sneak-right-click a saved living villager; ordinary vanilla trading is left available. */
public final class FoodAidMenu implements Listener {
    private final org.bukkit.plugin.Plugin plugin;
    private final PopulationSession session;
    private final FoodAidTransfers transfers;
    private final FoodAidPreview.Policy policy;
    private static final class Menu implements InventoryHolder {
        private Inventory inventory;
        private boolean used, canDonate;
        private final UUID town, player;
        private final ItemStack offered;
        Menu(UUID town,UUID player,ItemStack offered){this.town=town;this.player=player;this.offered=offered.clone();}
        @Override public Inventory getInventory(){return inventory;}
    }
    public FoodAidMenu(org.bukkit.plugin.Plugin plugin,PopulationSession session,FoodAidTransfers transfers,FoodAidPreview.Policy policy){this.plugin=plugin;this.session=session;this.transfers=transfers;this.policy=policy;}
    @EventHandler(ignoreCancelled=true) public void interact(PlayerInteractEntityEvent event) {
        if(event.getHand()!=EquipmentSlot.HAND||!event.getPlayer().isSneaking()||!(event.getRightClicked() instanceof Villager villager))return;
        var view=session.projectionView().orElse(null);if(view==null||!policy.enabled())return;
        var id=view.identities().aliases().get(new EntityKey(villager.getWorld().getUID(),villager.getUniqueId()));
        if(id==null)return;
        var person=view.population().residents().stream().filter(p->p.id().equals(id)&&p.status()==Resident.Status.ALIVE).findFirst().orElse(null);
        if(person==null)return;
        event.setCancelled(true);var player=event.getPlayer();
        if(!player.hasPermission("civilizations.aid.preview")){player.sendMessage("You do not have permission to open village supplies.");return;}
        if(!person.settlementId().equals(FoodAidPreviewCommand.recipient(session,player.getLocation(),policy,person.settlementId()).town())){player.sendMessage("Village food information is not ready yet.");return;}
        var offered=player.getInventory().getItemInMainHand();long units=0;boolean supported=true;
        try{units=FoodAidPreviewCommand.foodUnits(offered,policy);}catch(IllegalArgumentException invalid){supported=false;}
        var menu=new Menu(person.settlementId(),player.getUniqueId(),offered);
        menu.inventory=Bukkit.createInventory(menu,27,Component.text("Village supplies"));
        boolean available=supported&&transfers.enabled()&&player.hasPermission("civilizations.aid.donate");menu.canDonate=available;
        var button=new ItemStack(available?Material.BREAD:Material.BARRIER);var meta=button.getItemMeta();meta.displayName(Component.text(available?"Donate held food":"Donations unavailable"));
        var economy=session.economyView().get(person.settlementId());
        var situation=economy.needs()==null?"Food report pending":economy.needs().sharedFood().name().replace('_',' ').toLowerCase(Locale.ROOT);
        meta.lore(List.of(Component.text(session.settlementNames().getOrDefault(person.settlementId(),"Nearby village")),Component.text("Food situation: "+situation),
            Component.text(supported?"Give "+offered.getAmount()+" "+offered.getType().name().toLowerCase(Locale.ROOT).replace('_',' ')+" = "+units+" food units":"Hold plain supported food to offer a donation."),
            Component.text(available?"Click to donate the whole held stack.":!supported?"Reopen holding food to donate.":"Disabled by server settings or permissions."),Component.text("No payment or reputation reward yet.")));
        button.setItemMeta(meta);menu.inventory.setItem(13,button);
        var notice=new ItemStack(Material.PAPER);var noticeMeta=notice.getItemMeta();
        var appeal=economy.appeal();boolean asking=appeal!=null&&appeal.status()==com.blueninjas24.civilizations.economy.FoodAppeals.Status.OPEN;
        noticeMeta.displayName(Component.text(asking?"Village asks for food":"Village food report"));
        var lines=new ArrayList<Component>();
        if(appeal==null)lines.add(Component.text("Food appeal report pending."));
        else {
            lines.add(Component.text(asking?"Latest shortage: "+appeal.unmet()+" food units.":"No active food appeal."));
            lines.add(Component.text("Based on saved food period "+appeal.period()+"."));
            if(asking)lines.add(Component.text("Voluntary help; no payment promised."));
        }
        lines.add(Component.text("Pending deliveries: "+economy.foodAid().pendingDeliveries()+"."));
        if(economy.queuedPeriods()>0)lines.add(Component.text("Village is catching up; conditions may change."));
        noticeMeta.lore(lines);notice.setItemMeta(noticeMeta);menu.inventory.setItem(11,notice);player.openInventory(menu.inventory);
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if(!(event.getView().getTopInventory().getHolder() instanceof Menu menu))return;
        event.setCancelled(true);
        if(!menu.canDonate||menu.used||event.getRawSlot()!=13||!event.isLeftClick()||!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)||!menu.player.equals(player.getUniqueId()))return;
        menu.used=true;
        plugin.getServer().getScheduler().runTask(plugin,()->{
            if(!player.isOnline())return;
            if(player.getOpenInventory().getTopInventory()==menu.inventory)player.closeInventory();
            transfers.donate(player,menu.town,menu.offered);
        });
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof Menu)event.setCancelled(true);}
}
