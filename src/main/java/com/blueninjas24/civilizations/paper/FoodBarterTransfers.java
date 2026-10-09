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
public final class FoodBarterTransfers extends BukkitRunnable implements AutoCloseable {
    public static final NamespacedKey KEY = Objects.requireNonNull(NamespacedKey.fromString(PendingFoodBarter.KEY));
    private enum Stage { PROOF, RESERVE, COMPLETE, CANCEL }
    private record Work(PendingFoodBarter receipt, Stage stage, CompletableFuture<?> future) {}
    private final Plugin plugin;
    private final PopulationSession session;
    private final FoodAidPreview.Policy foods;
    private final FoodBarters.Policy policy;
    private final Path playerDirectory;
    private final java.util.function.Function<FoodBarters.Terms,ItemStack> paymentFactory;
    private final Map<UUID, Work> work = new HashMap<>();
    private final Set<UUID> warned = new HashSet<>();
    private final ExecutorService proofs = new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(16),
            r -> Thread.ofPlatform().daemon(true).name("Civilizations-barter-proof").unstarted(r),new ThreadPoolExecutor.AbortPolicy());
    private int cursor;
    public FoodBarterTransfers(Plugin plugin,PopulationSession session,FoodAidPreview.Policy foods,FoodBarters.Policy policy,Path directory) {
        this(plugin,session,foods,policy,directory,t->new ItemStack(Material.valueOf(t.paymentItem()),t.paymentCount()));
    }
    FoodBarterTransfers(Plugin plugin,PopulationSession session,FoodAidPreview.Policy foods,FoodBarters.Policy policy,Path directory,
                       java.util.function.Function<FoodBarters.Terms,ItemStack> paymentFactory) {
        this.plugin=plugin;this.session=session;this.foods=foods;this.policy=policy;this.playerDirectory=directory;this.paymentFactory=Objects.requireNonNull(paymentFactory);
    }
    /** Main-thread explicit acceptance. Durable preparation precedes owner revalidation/reservation. */
    public boolean confirm(Player player,FoodBarters.Terms terms) {
        if(!session.status().ready()||!policy.enabled()||!foods.enabled()||!player.hasPermission("civilizations.trade")
                ||!terms.player().equals(player.getUniqueId())||work.size()>=16
                ||player.getPersistentDataContainer().has(KEY)||player.getPersistentDataContainer().has(FoodPurchaseTransfers.KEY)
                ||player.getPersistentDataContainer().has(FoodAidTransfers.KEY)) {
            player.sendMessage("Barter is unavailable or another transfer is recovering. No items taken.");return false;
        }
        if(!terms.settlement().equals(FoodAidPreviewCommand.recipient(session,player.getLocation(),foods,terms.settlement()).town())) {
            player.sendMessage("The village is no longer ready or nearby. No items taken.");return false;
        }
        var held=player.getInventory().getItemInMainHand();
        if(held==null||held.hasItemMeta()||!held.getType().name().equals(terms.foodItem())||held.getAmount()!=terms.foodCount()) {
            player.sendMessage("Your food changed. Request a fresh barter offer.");return false;
        }
        var receipt=new PendingFoodBarter(terms,PendingFoodBarter.Stage.PREPARED);set(player,receipt);saveAndProve(player,receipt);
        player.sendMessage("Checking village supplies. Keep that food stack in your main hand until confirmed.");return true;
    }
    private static boolean sameFood(ItemStack a,ItemStack b) {
        return a!=null&&b!=null&&!a.hasItemMeta()&&!b.hasItemMeta()&&a.getType()==b.getType()&&a.getAmount()==b.getAmount();
    }
    private static void set(Player player,PendingFoodBarter receipt){player.getPersistentDataContainer().set(KEY,PersistentDataType.STRING,receipt.encode());}
    private void saveAndProve(Player player,PendingFoodBarter receipt) {
        try {
            player.saveData();
            work.put(player.getUniqueId(),new Work(receipt,Stage.PROOF,CompletableFuture.supplyAsync(()->{
                try{return SavedAidProof.contains(playerDirectory,receipt.terms().player(),PendingFoodBarter.KEY,receipt.encode());}
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
        PendingFoodBarter receipt;
        try {
            var encoded=player.getPersistentDataContainer().get(KEY,PersistentDataType.STRING);
            if(encoded==null){if(player.getPersistentDataContainer().has(KEY))throw new IllegalArgumentException();work.remove(player.getUniqueId());warned.remove(player.getUniqueId());return;}
            receipt=PendingFoodBarter.decode(encoded);if(!receipt.terms().player().equals(player.getUniqueId()))throw new IllegalArgumentException();
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
                        case PREPARED -> work.put(player.getUniqueId(),new Work(receipt,Stage.RESERVE,session.reserveFoodBarter(terms,policy,foods)));
                        case EXCHANGED -> work.put(player.getUniqueId(),new Work(receipt,Stage.COMPLETE,session.completeFoodBarter(terms)));
                        case CANCELLED -> work.put(player.getUniqueId(),new Work(receipt,Stage.CANCEL,session.cancelFoodBarter(terms)));
                        case REVIEW -> warn(player,"Trade inventory recovery needs administrator review; supplies remain held.");
                    }
                }
                case RESERVE -> {
                    if("CANCELLED".equals(result)){clear(player);return;}
                    if(!"RESERVED".equals(result)){warn(player,"Trade save and ledger disagree; administrator review required. No duplicate payout made.");return;}
                    var held=player.getInventory().getItemInMainHand();
                    if(held==null||held.hasItemMeta()||!held.getType().name().equals(terms.foodItem())||held.getAmount()!=terms.foodCount()) {
                        var cancelled=receipt.withStage(PendingFoodBarter.Stage.CANCELLED);set(player,cancelled);saveAndProve(player,cancelled);return;
                    }
                    var exchanged=receipt.withStage(PendingFoodBarter.Stage.EXCHANGED);var before=held.clone();
                    // Consume the whole confirmed stack and place its payment in that same slot; never drop overflow items.
                    set(player,exchanged);
                    try {player.getInventory().setItemInMainHand(paymentFactory.apply(terms));}
                    catch(RuntimeException failure){
                        try {
                            player.getInventory().setItemInMainHand(before);
                            if(!sameFood(player.getInventory().getItemInMainHand(),before))throw new IllegalStateException("Inventory restore not confirmed");
                            var cancelled=receipt.withStage(PendingFoodBarter.Stage.CANCELLED);set(player,cancelled);saveAndProve(player,cancelled);
                        }catch(RuntimeException restoreFailure){
                            var review=receipt.withStage(PendingFoodBarter.Stage.REVIEW);set(player,review);saveAndProve(player,review);
                            warn(player,"Trade inventory recovery needs administrator review; supplies remain held.");
                        }
                        return;
                    }
                    saveAndProve(player,exchanged);
                }
                case COMPLETE -> {clear(player);player.sendMessage("Trade confirmed: "+terms.foodCount()+" "+terms.foodItem().toLowerCase(Locale.ROOT)+" for "+terms.paymentCount()+" "+terms.paymentItem().toLowerCase(Locale.ROOT)+". Village supplies enter a future food period.");}
                case CANCEL -> {clear(player);player.sendMessage("Trade cancelled before exchange. Your items were not taken; reserved supplies released.");}
            }
        }catch(RuntimeException failure) {
            work.remove(player.getUniqueId());
            if(pending.stage()==Stage.RESERVE && pending.future().isCompletedExceptionally()) {
                var cancelled=receipt.withStage(PendingFoodBarter.Stage.CANCELLED);set(player,cancelled);saveAndProve(player,cancelled);
            }
            if(failure.getCause() instanceof com.blueninjas24.civilizations.database.EconomyRuntimeRepository.PurchaseDeclined declined)
                player.sendMessage("Offer declined: "+declined.getMessage()+". Your items were not exchanged.");
            else {
                if(!warned.contains(player.getUniqueId()))plugin.getLogger().log(java.util.logging.Level.WARNING,"Barter save/ledger verification failed; recovery record retained",failure);
                warn(player,"Trade remains in recovery. No second item exchange is needed.");
            }
        }
    }
    private void clear(Player player){player.getPersistentDataContainer().remove(KEY);work.remove(player.getUniqueId());warned.remove(player.getUniqueId());player.saveData();}
    private void warn(Player player,String message){if(warned.add(player.getUniqueId())){player.sendMessage(message);plugin.getLogger().warning(message);}}
    @Override public void close(){proofs.shutdown();work.clear();warned.clear();}
}
