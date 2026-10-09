package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.economy.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Main-thread whole-held-stack exchange; independently saved player proof gates all SQL effects. */
public final class FoodPurchaseTransfers extends BukkitRunnable implements AutoCloseable {
    public static final NamespacedKey KEY = Objects.requireNonNull(NamespacedKey.fromString(PendingFoodPurchase.KEY));
    private enum Stage { PROOF, RESERVE, COMPLETE, CANCEL }
    private record Work(PendingFoodPurchase receipt, Stage stage, CompletableFuture<?> future) {}
    private final Plugin plugin;
    private final PopulationSession session;
    private final FoodAidPreview.Policy foods;
    private final FoodPurchases.Policy policy;
    private final FoodBargaining.Policy bargaining;
    private final Path playerDirectory;
    private final java.util.function.IntFunction<ItemStack> paymentFactory;
    private final Map<UUID, Work> work = new HashMap<>();
    private final Set<UUID> warned = new HashSet<>();
    private final ExecutorService proofs = new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(16),
            r -> Thread.ofPlatform().daemon(true).name("Civilizations-purchase-proof").unstarted(r),new ThreadPoolExecutor.AbortPolicy());
    private int cursor;
    public FoodPurchaseTransfers(Plugin plugin,PopulationSession session,FoodAidPreview.Policy foods,FoodPurchases.Policy policy,Path directory) {
        this(plugin,session,foods,policy,directory,count->new ItemStack(Material.EMERALD,count));
    }
    public FoodPurchaseTransfers(Plugin plugin,PopulationSession session,FoodAidPreview.Policy foods,FoodPurchases.Policy policy,Path directory,FoodBargaining.Policy bargaining) {
        this(plugin,session,foods,policy,directory,count->new ItemStack(Material.EMERALD,count),bargaining);
    }
    FoodPurchaseTransfers(Plugin plugin,PopulationSession session,FoodAidPreview.Policy foods,FoodPurchases.Policy policy,Path directory,
                          java.util.function.IntFunction<ItemStack> paymentFactory) {
        this(plugin,session,foods,policy,directory,paymentFactory,FoodBargaining.Policy.disabled());
    }
    FoodPurchaseTransfers(Plugin plugin,PopulationSession session,FoodAidPreview.Policy foods,FoodPurchases.Policy policy,Path directory,
                          java.util.function.IntFunction<ItemStack> paymentFactory,FoodBargaining.Policy bargaining) {
        this.plugin=plugin;this.session=session;this.foods=foods;this.policy=policy;this.playerDirectory=directory;
        this.bargaining=Objects.requireNonNull(bargaining);
        this.paymentFactory=Objects.requireNonNull(paymentFactory);
    }
    /** Indicative cached quote. Owner revalidates funds/needs/price before reserving it. */
    public int quote(UUID town,ItemStack offered) {
        if(!policy.enabled()||!foods.enabled())return 0;
        try {
            long units=FoodAidPreviewCommand.foodUnits(offered,foods);
            var view=session.economyView().get(town);if(view==null||view.needs()==null||view.treasury()==null||view.priority()==null
                    ||view.queuedPeriods()!=0||!view.farm().complete()||view.priority().selected()!=VillagePriorities.Action.SEEK_FOOD)return 0;
            long target=Math.multiplyExact(view.needs().demand(),policy.reservePeriods());
            if(view.needs().demand()<=view.needs().fulfilled() || units>target-view.account().state().stocks().get(Commodity.FOOD).units())return 0;
            long value=Math.multiplyExact(session.economySettings().orElseThrow().price(Commodity.FOOD,view.needs().sharedFood()).movePointRight(2).longValueExact(),units);
            return FoodPurchases.maximumPayment(value,view.treasury().hundredths(),policy);
        }catch(IllegalArgumentException|ArithmeticException invalid){return 0;}
    }
    public boolean sell(Player player,UUID town,ItemStack offered,int emeralds) {
        if(!sameFood(player.getInventory().getItemInMainHand(),offered)||emeralds<1||emeralds>quote(town,offered)) {
            player.sendMessage("Your food or the village offer changed. Reopen village supplies.");return false;
        }
        return sellTerms(player,new FoodPurchases.Terms(UUID.randomUUID(),player.getUniqueId(),town,FoodAidPreviewCommand.foodUnits(offered,foods),
                offered.getType().name(),offered.getAmount(),emeralds,policy.hundredthsPerEmerald()));
    }
    /** Explicit acceptance reuses its saved discussion operation; caller never invents or increases the agreed amount. */
    public boolean sellTerms(Player player,FoodPurchases.Terms terms) {
        var town=terms.settlement();
        if(!session.status().ready()||!policy.enabled()||!foods.enabled()||!player.hasPermission("civilizations.trade")
                ||!terms.player().equals(player.getUniqueId())
                ||work.size()>=16||player.getPersistentDataContainer().has(KEY)||player.getPersistentDataContainer().has(FoodAidTransfers.KEY)||player.getPersistentDataContainer().has(FoodBarterTransfers.KEY)) {
            player.sendMessage("Trading is unavailable or a previous transfer is still recovering. No items taken.");return false;
        }
        if(!town.equals(FoodAidPreviewCommand.recipient(session,player.getLocation(),foods,town).town())) {
            player.sendMessage("The village is no longer ready or nearby. No items taken.");return false;
        }
        var held=player.getInventory().getItemInMainHand();
        if(held==null||held.hasItemMeta()||!held.getType().name().equals(terms.item())||held.getAmount()!=terms.count()
                ||terms.emeralds()>quote(town,held)) {
            player.sendMessage("Your food or the village offer changed. Reopen village supplies.");return false;
        }
        var receipt=new PendingFoodPurchase(terms,PendingFoodPurchase.Stage.PREPARED);
        set(player,receipt);saveAndProve(player,receipt);
        player.sendMessage("Checking village funds for your food offer. Keep that stack in your main hand until confirmed.");return true;
    }
    private static boolean sameFood(ItemStack a,ItemStack b) {
        return a!=null&&b!=null&&!a.hasItemMeta()&&!b.hasItemMeta()&&a.getType()==b.getType()&&a.getAmount()==b.getAmount();
    }
    private static void set(Player player,PendingFoodPurchase receipt){player.getPersistentDataContainer().set(KEY,PersistentDataType.STRING,receipt.encode());}
    private void saveAndProve(Player player,PendingFoodPurchase receipt) {
        try {
            player.saveData();
            work.put(player.getUniqueId(),new Work(receipt,Stage.PROOF,CompletableFuture.supplyAsync(()->{
                try{return SavedAidProof.contains(playerDirectory,receipt.terms().player(),PendingFoodPurchase.KEY,receipt.encode());}
                catch(java.io.IOException failure){throw new CompletionException(failure);}
            },proofs)));
        }catch(RuntimeException failure){warn(player,"Trade remains pending until Minecraft saves its recovery record.");}
    }
    @Override public void run() {
        var players=new ArrayList<>(plugin.getServer().getOnlinePlayers());if(players.isEmpty()){cursor=0;return;}
        for(int i=0;i<Math.min(4,players.size());i++)poll(players.get(Math.floorMod(cursor++,players.size())));
        var online=new HashSet<UUID>();players.forEach(p->online.add(p.getUniqueId()));work.keySet().removeIf(id->!online.contains(id));warned.retainAll(online);
    }
    public void poll(Player player) {
        if(!session.status().ready())return;
        PendingFoodPurchase receipt;
        try {
            var encoded=player.getPersistentDataContainer().get(KEY,PersistentDataType.STRING);
            if(encoded==null){if(player.getPersistentDataContainer().has(KEY))throw new IllegalArgumentException();work.remove(player.getUniqueId());warned.remove(player.getUniqueId());return;}
            receipt=PendingFoodPurchase.decode(encoded);if(!receipt.terms().player().equals(player.getUniqueId()))throw new IllegalArgumentException();
        }catch(RuntimeException invalid){warn(player,"Trade recovery record needs administrator review; no further inventory changes made.");return;}
        var pending=work.get(player.getUniqueId());
        if(pending==null){if(work.size()<16)saveAndProve(player,receipt);return;}
        if(!pending.receipt().equals(receipt)){warn(player,"Trade recovery record changed; administrator review required.");return;}
        if(!pending.future().isDone())return;
        try {
            var result=pending.future().join();var terms=receipt.terms();
            switch(pending.stage()) {
                case PROOF -> {
                    if(!Boolean.TRUE.equals(result)){work.remove(player.getUniqueId());warn(player,"Trade is waiting for a verified Minecraft save.");return;}
                    switch(receipt.stage()) {
                        case PREPARED -> work.put(player.getUniqueId(),new Work(receipt,Stage.RESERVE,session.reserveFoodPurchase(terms,policy,foods,bargaining)));
                        case EXCHANGED -> work.put(player.getUniqueId(),new Work(receipt,Stage.COMPLETE,session.completeFoodPurchase(terms)));
                        case CANCELLED -> work.put(player.getUniqueId(),new Work(receipt,Stage.CANCEL,session.cancelFoodPurchase(terms)));
                        case REVIEW -> warn(player,"Trade inventory recovery needs administrator review; funds remain held.");
                    }
                }
                case RESERVE -> {
                    if("CANCELLED".equals(result)){clear(player);return;}
                    if(!"RESERVED".equals(result)){warn(player,"Trade save and ledger disagree; administrator review required. No duplicate payout made.");return;}
                    var held=player.getInventory().getItemInMainHand();
                    if(held==null||held.hasItemMeta()||!held.getType().name().equals(terms.item())||held.getAmount()!=terms.count()) {
                        var cancelled=receipt.withStage(PendingFoodPurchase.Stage.CANCELLED);set(player,cancelled);saveAndProve(player,cancelled);return;
                    }
                    var exchanged=receipt.withStage(PendingFoodPurchase.Stage.EXCHANGED);var before=held.clone();
                    // Consume the whole confirmed stack and place its payment in that same slot; never drop overflow items.
                    set(player,exchanged);
                    try {player.getInventory().setItemInMainHand(paymentFactory.apply(terms.emeralds()));}
                    catch(RuntimeException failure){
                        try {
                            player.getInventory().setItemInMainHand(before);
                            if(!sameFood(player.getInventory().getItemInMainHand(),before))throw new IllegalStateException("Inventory restore not confirmed");
                            var cancelled=receipt.withStage(PendingFoodPurchase.Stage.CANCELLED);set(player,cancelled);saveAndProve(player,cancelled);
                        }catch(RuntimeException restoreFailure){
                            var review=receipt.withStage(PendingFoodPurchase.Stage.REVIEW);set(player,review);saveAndProve(player,review);
                            warn(player,"Trade inventory recovery needs administrator review; funds remain held.");
                        }
                        return;
                    }
                    saveAndProve(player,exchanged);
                }
                case COMPLETE -> {clear(player);player.sendMessage("Trade confirmed: "+terms.count()+" "+terms.item().toLowerCase(Locale.ROOT)+" for "+terms.emeralds()+" emeralds. Village supplies enter a future food period.");}
                case CANCEL -> {clear(player);player.sendMessage("Trade cancelled before exchange. Your items were not taken; reserved funds released.");}
            }
        }catch(RuntimeException failure) {
            work.remove(player.getUniqueId());
            if(pending.stage()==Stage.RESERVE && pending.future().isCompletedExceptionally()) {
                var cancelled=receipt.withStage(PendingFoodPurchase.Stage.CANCELLED);set(player,cancelled);saveAndProve(player,cancelled);
            }
            if(failure.getCause() instanceof com.blueninjas24.civilizations.database.EconomyRuntimeRepository.PurchaseDeclined declined)
                player.sendMessage("Offer declined: "+declined.getMessage()+". Your items were not exchanged.");
            else warn(player,"Trade remains in recovery. No second item exchange is needed.");
        }
    }
    private void clear(Player player){player.getPersistentDataContainer().remove(KEY);work.remove(player.getUniqueId());warned.remove(player.getUniqueId());player.saveData();}
    private void warn(Player player,String message){if(warned.add(player.getUniqueId())){player.sendMessage(message);plugin.getLogger().warning(message);}}
    @Override public void close(){proofs.shutdown();work.clear();warned.clear();}
}
