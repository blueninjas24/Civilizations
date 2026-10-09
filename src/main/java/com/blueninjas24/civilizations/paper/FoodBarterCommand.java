package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;

/** Main-thread explicit preview/confirmation. Quotes hold no goods and expire without penalty. */
public final class FoodBarterCommand extends BukkitRunnable implements CommandExecutor,AutoCloseable {
    private record Work(CompletableFuture<FoodBarters.Terms> future,long deadline) {}
    private record Offer(FoodBarters.Terms terms,long deadline) {}
    private final Plugin plugin;private final PopulationSession session;private final FoodBarterTransfers transfers;
    private final FoodAidPreview.Policy foods;private final FoodBarters.Policy policy;
    private final Map<UUID,Work> work=new LinkedHashMap<>();private final Map<UUID,Offer> offers=new LinkedHashMap<>();
    private final java.util.function.LongSupplier now;private final long lifetime;
    public FoodBarterCommand(Plugin plugin,PopulationSession session,FoodBarterTransfers transfers,FoodAidPreview.Policy foods,
                            FoodBarters.Policy policy,int offerSeconds) {
        this(plugin,session,transfers,foods,policy,offerSeconds,System::currentTimeMillis);
    }
    FoodBarterCommand(Plugin plugin,PopulationSession session,FoodBarterTransfers transfers,FoodAidPreview.Policy foods,
                     FoodBarters.Policy policy,int offerSeconds,java.util.function.LongSupplier now) {
        if(offerSeconds<1||offerSeconds>3600)throw new IllegalArgumentException("Invalid barter quote lifetime");
        this.plugin=plugin;this.session=session;this.transfers=transfers;this.foods=foods;this.policy=policy;this.now=now;this.lifetime=offerSeconds*1000L;
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player player)){sender.sendMessage("Barter requires a player near a village.");return true;}
        if(!player.hasPermission("civilizations.trade")){player.sendMessage("You do not have permission to trade.");return true;}
        if(!policy.enabled()||!foods.enabled()){player.sendMessage("Food barter is disabled in economy.yml.");return true;}
        if(!session.status().ready()){player.sendMessage("Civilizations is still starting.");return true;}
        if(args.length<2||args.length>3){player.sendMessage("Usage: /civ barter <timber|stone|accept> [village UUID]");return true;}
        if(work.containsKey(player.getUniqueId())){player.sendMessage("A barter quote is still running.");return true;}
        if(player.getPersistentDataContainer().has(FoodBarterTransfers.KEY)||player.getPersistentDataContainer().has(FoodPurchaseTransfers.KEY)
                ||player.getPersistentDataContainer().has(FoodAidTransfers.KEY)){player.sendMessage("Finish your pending transfer before barter.");return true;}
        try {
            UUID requested=args.length==3?UUID.fromString(args[2]):null;
            var town=FoodAidPreviewCommand.recipient(session,player.getLocation(),foods,requested).town();
            if(town==null){player.sendMessage("No unique ready village nearby; select its UUID if villages overlap.");return true;}
            if(args[1].equalsIgnoreCase("accept")) {
                var offer=offers.remove(player.getUniqueId());
                if(offer==null||now.getAsLong()>=offer.deadline()||!town.equals(offer.terms().settlement())) {
                    player.sendMessage("No current barter offer for this village. Request a fresh offer.");return true;
                }
                transfers.confirm(player,offer.terms());return true;
            }
            if(work.size()>=16||offers.size()>=64&&!offers.containsKey(player.getUniqueId())){player.sendMessage("Village barter is busy; try shortly.");return true;}
            var commodity=switch(args[1].toLowerCase(Locale.ROOT)){case "timber"->Commodity.TIMBER;case "stone"->Commodity.STONE;default->throw new IllegalArgumentException();};
            var held=player.getInventory().getItemInMainHand();long units=FoodAidPreviewCommand.foodUnits(held,foods);
            offers.remove(player.getUniqueId());
            work.put(player.getUniqueId(),new Work(session.quoteFoodBarter(UUID.randomUUID(),player.getUniqueId(),town,units,
                    held.getType().name(),held.getAmount(),commodity,policy,foods),Math.addExact(now.getAsLong(),lifetime)));
            player.sendMessage("Checking actual village supplies. No food or goods moved.");
        }catch(IllegalArgumentException invalid){player.sendMessage("Hold plain supported food; choose timber, stone or accept, with an optional village UUID.");}
        return true;
    }
    @Override public void run() {
        int handled=0;var iterator=work.entrySet().iterator();
        while(iterator.hasNext()&&handled<4) {
            var entry=iterator.next();var pending=entry.getValue();if(!pending.future().isDone())continue;
            iterator.remove();handled++;var player=plugin.getServer().getPlayer(entry.getKey());if(player==null||!player.isOnline())continue;
            try {
                var terms=pending.future().join();
                if(now.getAsLong()>=pending.deadline()){player.sendMessage("Barter quote expired; request it again.");continue;}
                if(offers.size()>=64&&!offers.containsKey(entry.getKey())){player.sendMessage("Barter is busy; request a fresh quote shortly.");continue;}
                offers.put(entry.getKey(),new Offer(terms,pending.deadline()));
                player.sendMessage("Village offers "+terms.paymentCount()+" "+terms.paymentItem().toLowerCase(Locale.ROOT)+" for your whole "+terms.foodCount()+" "+terms.foodItem().toLowerCase(Locale.ROOT)+" stack. Use /civ barter accept before the offer expires. No items moved.");
            }catch(RuntimeException failure) {
                if(failure.getCause() instanceof com.blueninjas24.civilizations.database.EconomyRuntimeRepository.PurchaseDeclined declined)
                    player.sendMessage("Barter unavailable: "+declined.getMessage()+". No items exchanged.");
                else {player.sendMessage("Barter quote failed; no items exchanged.");plugin.getLogger().log(java.util.logging.Level.WARNING,"Barter quote failed",failure);}
            }
        }
        offers.entrySet().removeIf(entry->now.getAsLong()>=entry.getValue().deadline());
    }
    @Override public void close(){work.clear();offers.clear();}
}
