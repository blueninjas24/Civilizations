package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.economy.FoodAidPreview;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Main-thread debit/recovery coordinator. Worker reads saved proof; SQL owner credits only proven debits. */
public final class FoodAidTransfers extends BukkitRunnable implements AutoCloseable {
    public static final NamespacedKey KEY = Objects.requireNonNull(NamespacedKey.fromString(SavedAidProof.KEY));
    private enum Stage { PROOF, DELIVERY }
    private record Work(PendingFoodAid receipt, Stage stage, CompletableFuture<?> future) {}
    private final Plugin plugin;
    private final PopulationSession session;
    private final FoodAidPreview.Policy policy;
    private final boolean enabled;
    private final Path playerDirectory;
    private final Map<UUID,Work> work = new HashMap<>();
    private final Set<UUID> warned = new HashSet<>();
    private final ExecutorService proofs = new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(16),
            runnable -> Thread.ofPlatform().daemon(true).name("Civilizations-aid-proof").unstarted(runnable),new ThreadPoolExecutor.AbortPolicy());
    private int cursor;
    public FoodAidTransfers(Plugin plugin, PopulationSession session, FoodAidPreview.Policy policy, boolean enabled, Path playerDirectory) {
        this.plugin=plugin;this.session=session;this.policy=policy;this.enabled=enabled;this.playerDirectory=playerDirectory;
    }
    public boolean enabled(){return enabled&&policy.enabled();}
    /** Consent comes from clicking the donation button. Any changed held stack/recipient invalidates that offer. */
    public boolean donate(Player player,UUID town,ItemStack offered) {
        if (!enabled || !policy.enabled()) {player.sendMessage("Food donations are disabled by this server.");return false;}
        if (!player.hasPermission("civilizations.aid.donate")) {player.sendMessage("You do not have permission to donate food.");return false;}
        if (!session.status().ready() || work.size()>=16) {player.sendMessage("Food delivery is busy; your items were not taken.");return false;}
        if (player.getPersistentDataContainer().has(KEY)) {player.sendMessage("Your previous food delivery is still pending recovery. No more items taken.");return false;}
        if (!town.equals(FoodAidPreviewCommand.recipient(session,player.getLocation(),policy,town).town())) {player.sendMessage("The village is no longer ready or nearby. Your items were not taken.");return false;}
        var held=player.getInventory().getItemInMainHand();
        if (!held.isSimilar(offered)||held.getAmount()!=offered.getAmount()) {player.sendMessage("Your held food changed. Reopen the village menu.");return false;}
        long units;
        try {units=FoodAidPreviewCommand.foodUnits(held,policy);}
        catch(IllegalArgumentException invalid) {player.sendMessage("Only plain supported food can be donated.");return false;}
        var receipt=new PendingFoodAid(UUID.randomUUID(),player.getUniqueId(),town,units,held.getType().name(),held.getAmount());
        // Both fields serialize in the same player .dat snapshot, before any village credit.
        player.getPersistentDataContainer().set(KEY,PersistentDataType.STRING,receipt.encode());
        try {player.getInventory().setItemInMainHand(null);}
        catch(RuntimeException failure) {player.getPersistentDataContainer().remove(KEY);throw failure;}
        saveAndProve(player,receipt);
        player.sendMessage("Food set aside for delivery: "+receipt.count()+" items, "+receipt.units()+" food units. Delivery confirmation will follow.");
        return true;
    }
    private void saveAndProve(Player player,PendingFoodAid receipt) {
        try {
            player.saveData(); // void/possibly unsuccessful: verify the actual saved receipt independently.
            work.put(player.getUniqueId(),new Work(receipt,Stage.PROOF,CompletableFuture.supplyAsync(()->{
                try{return SavedAidProof.contains(playerDirectory,receipt);}catch(java.io.IOException failure){throw new CompletionException(failure);}
            },proofs)));
        } catch(RuntimeException failure) {warn(player,"Food delivery remains pending; the saved debit could not be verified.");}
    }
    @Override public void run() {
        var players=new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if(players.isEmpty()){cursor=0;return;}
        for(int visited=0;visited<Math.min(4,players.size());visited++)poll(players.get(Math.floorMod(cursor++,players.size())));
        var online=new HashSet<UUID>();players.forEach(p->online.add(p.getUniqueId()));work.keySet().removeIf(id->!online.contains(id));warned.retainAll(online);
    }
    /** Main-thread polling also exercises recovery with the isolated synthetic Paper player. */
    public void poll(Player player) {
        if(!session.status().ready())return;
        String encoded;
        try{encoded=player.getPersistentDataContainer().get(KEY,PersistentDataType.STRING);}
        catch(RuntimeException invalid){warn(player,"Pending food receipt has an unsupported data type; administrator review required.");return;}
        if(encoded==null&&player.getPersistentDataContainer().has(KEY)){warn(player,"Pending food receipt has an unsupported data type; administrator review required.");return;}
        if(encoded==null){work.remove(player.getUniqueId());warned.remove(player.getUniqueId());return;}
        PendingFoodAid receipt;
        try{receipt=PendingFoodAid.decode(encoded);if(!receipt.player().equals(player.getUniqueId()))throw new IllegalArgumentException();}
        catch(IllegalArgumentException malformed){warn(player,"Pending food receipt needs administrator review; no village credit made.");return;}
        var pending=work.get(player.getUniqueId());
        if(pending==null){if(work.size()<16)saveAndProve(player,receipt);return;}
        if(!pending.receipt().equals(receipt)){warn(player,"Pending food receipt changed; administrator review required.");return;}
        if(!pending.future().isDone())return;
        try {
            Object result=pending.future().join();
            if(pending.stage()==Stage.PROOF) {
                if(!Boolean.TRUE.equals(result)){work.remove(player.getUniqueId());warn(player,"Food delivery is waiting for Minecraft to save your inventory. It will retry safely.");return;}
                work.put(player.getUniqueId(),new Work(receipt,Stage.DELIVERY,session.recordDeliveredFood(receipt.operation(),receipt.settlement(),receipt.units(),receipt.source())));
            } else {
                player.getPersistentDataContainer().remove(KEY);work.remove(player.getUniqueId());warned.remove(player.getUniqueId());
                player.saveData(); // A stale saved marker after failure is harmless: the SQL operation ID is idempotent.
                player.sendMessage("Food delivery confirmed: "+receipt.units()+" units for "+session.settlementNames().getOrDefault(receipt.settlement(),"the village")+". Supplies enter a future food period. Thank you!");
            }
        } catch(RuntimeException failure){work.remove(player.getUniqueId());warn(player,"Food delivery remains pending and will retry; no second item debit is needed.");}
    }
    private void warn(Player player,String message){if(warned.add(player.getUniqueId())){player.sendMessage(message);plugin.getLogger().warning(message);}}
    @Override public void close(){proofs.shutdown();work.clear();warned.clear();}
}
