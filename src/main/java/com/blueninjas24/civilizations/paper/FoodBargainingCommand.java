package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;

/** Main-thread price/accept interface. SQL discussion futures never operate on Bukkit objects. */
public final class FoodBargainingCommand extends BukkitRunnable implements CommandExecutor,AutoCloseable {
    private record Work(UUID player,CompletableFuture<?> future,boolean accepting) {}
    private final PopulationSession session;
    private final FoodPurchaseTransfers transfers;
    private final FoodAidPreview.Policy foods;
    private final FoodPurchases.Policy purchases;
    private final FoodBargaining.Policy policy;
    private final org.bukkit.plugin.Plugin plugin;
    private final Map<UUID,Work> work=new LinkedHashMap<>();
    public FoodBargainingCommand(org.bukkit.plugin.Plugin plugin,PopulationSession session,FoodPurchaseTransfers transfers,
                                FoodAidPreview.Policy foods,FoodPurchases.Policy purchases,FoodBargaining.Policy policy) {
        this.plugin=plugin;this.session=session;this.transfers=transfers;this.foods=foods;this.purchases=purchases;this.policy=policy;
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player player)){sender.sendMessage("Bargaining requires a player near a village.");return true;}
        if(!player.hasPermission("civilizations.trade")){player.sendMessage("You do not have permission to trade.");return true;}
        if(!policy.enabled()||!purchases.enabled()||!foods.enabled()){player.sendMessage("Food bargaining is disabled in economy.yml.");return true;}
        if(!session.status().ready()){player.sendMessage("Civilizations is still starting.");return true;}
        if(args.length<2||args.length>3){player.sendMessage("Usage: /civ bargain <emeralds|accept> [village UUID]");return true;}
        if(work.size()>=16||work.containsKey(player.getUniqueId())){player.sendMessage("A price check is still running; try shortly.");return true;}
        if(player.getPersistentDataContainer().has(FoodPurchaseTransfers.KEY)||player.getPersistentDataContainer().has(FoodAidTransfers.KEY)) {
            player.sendMessage("Finish your pending transfer before bargaining.");return true;
        }
        try {
            UUID requested=args.length==3 ? UUID.fromString(args[2]) : null;
            var recipient=FoodAidPreviewCommand.recipient(session,player.getLocation(),foods,requested);
            if(recipient.town()==null){player.sendMessage("No unique ready village nearby. For overlapping villages, choose its UUID.");return true;}
            boolean accepting=args[1].equalsIgnoreCase("accept");CompletableFuture<?> future;
            if(accepting)future=session.bargainAcceptance(player.getUniqueId(),recipient.town(),policy);
            else {
                int asking=Integer.parseInt(args[1]);var stack=player.getInventory().getItemInMainHand();
                var terms=new FoodPurchases.Terms(UUID.randomUUID(),player.getUniqueId(),recipient.town(),FoodAidPreviewCommand.foodUnits(stack,foods),
                        stack.getType().name(),stack.getAmount(),asking,purchases.hundredthsPerEmerald());
                future=session.bargain(terms,purchases,foods,policy);
            }
            work.put(player.getUniqueId(),new Work(player.getUniqueId(),future,accepting));
            player.sendMessage(accepting ? "Checking your saved bargain; keep the food stack in hand." : "Checking the village's current price. No items or funds moved.");
        }catch(IllegalArgumentException invalid){player.sendMessage("Use a price from 1 to 64 and a plain supported food stack; optional village ID must be a UUID.");}
        return true;
    }
    @Override public void run() {
        int handled=0;
        var iterator=work.values().iterator();
        while(iterator.hasNext()&&handled<4) {
            var pending=iterator.next();if(!pending.future().isDone())continue;iterator.remove();handled++;
            var player=plugin.getServer().getPlayer(pending.player());if(player==null||!player.isOnline())continue;
            try {
                var result=pending.future().join();
                if(pending.accepting())transfers.sellTerms(player,(FoodPurchases.Terms)result);
                else {
                    var deal=(FoodBargaining.Deal)result;var response=deal.result();
                    switch(response.status()) {
                        case AGREED -> player.sendMessage("Village agrees to "+response.offer()+" emeralds for that whole stack. Use /civ bargain accept to confirm before it expires.");
                        case COUNTER -> player.sendMessage("Village counters with "+response.offer()+" emeralds. Failed price offers: "+response.failedAttempts()+"/"+response.policy().maximumAttempts()+". Accept with /civ bargain accept, or propose another price.");
                        case CLOSED -> player.sendMessage("Deal closed after failed price offers. Wait for the village cooldown; no items taken or reputation penalty.");
                        case RESERVED -> player.sendMessage("That offer already entered transfer recovery; no second payout requested.");
                    }
                }
            }catch(RuntimeException failure) {
                if(failure.getCause() instanceof com.blueninjas24.civilizations.database.EconomyRuntimeRepository.PurchaseDeclined declined)
                    player.sendMessage("Village offer unavailable: "+declined.getMessage()+". No new items exchanged.");
                else {player.sendMessage("Price check failed; no new items exchanged.");plugin.getLogger().log(java.util.logging.Level.WARNING,"Food bargain could not be checked; saved history remains retryable.",failure);}
            }
        }
    }
    @Override public void close(){work.clear();}
}
