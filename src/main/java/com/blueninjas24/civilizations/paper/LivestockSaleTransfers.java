package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.database.LivestockMarket.*;
import com.blueninjas24.civilizations.database.LivestockProductSales.*;
import com.blueninjas24.civilizations.industry.LivestockMarketPolicy;
import com.blueninjas24.civilizations.industry.LivestockMarketPolicy.Position;
import com.blueninjas24.civilizations.industry.LivestockProductSale.Terms;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

/** Main-thread inventory handoff. Independent durable player proof gates every SQL transition.
 * A pending purchase delivers once; recovery completes its ledger and removes markers, never pays again. */
public final class LivestockSaleTransfers extends BukkitRunnable implements AutoCloseable {
    public static final NamespacedKey KEY = PendingLivestockItems.RECEIPT_KEY;
    interface Owner {
        boolean ready();
        CompletableFuture<Reservation> reserve(Terms terms,Position position);
        CompletableFuture<Receipt> complete(Terms terms);
        CompletableFuture<Optional<Receipt>> cancel(Terms terms);
    }
    interface Exchange { void apply(); void restore(); }
    interface Items { Exchange plan(Player player,Terms terms); void verify(Player player,Terms terms); void release(Player player,Terms terms); }
    @FunctionalInterface interface Proof { boolean contains(PendingLivestockSale receipt) throws java.io.IOException; }
    private enum Stage { PROOF, RESERVE, COMPLETE, CANCEL }
    private record Work(PendingLivestockSale receipt, Stage stage, CompletableFuture<?> future) {}
    private final Plugin plugin;
    private final Owner owner;
    private final LivestockMarketPolicy policy;
    private final Items items;
    private final Proof proof;
    private final LongSupplier now;
    private final Map<UUID,Work> work=new HashMap<>();
    private final Set<UUID> warned=new HashSet<>();
    private final ExecutorService proofs=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(16),
            r->Thread.ofPlatform().daemon(true).name("Civilizations-livestock-proof").unstarted(r),new ThreadPoolExecutor.AbortPolicy());
    private int cursor;
    public LivestockSaleTransfers(Plugin plugin,PopulationSession session,LivestockMarketPolicy policy,Path directory) {
        this(plugin,new Owner() {
            public boolean ready(){return session.status().ready();}
            public CompletableFuture<Reservation> reserve(Terms t,Position p){return session.reserveLivestockSale(t,p);}
            public CompletableFuture<Receipt> complete(Terms t){return session.completeLivestockSale(t);}
            public CompletableFuture<Optional<Receipt>> cancel(Terms t){return session.cancelLivestockSale(t);}
        },policy,new Items() {
            public Exchange plan(Player player,Terms terms) {
                var plan=LivestockSaleInventory.pending(player.getInventory(),terms);
                return new Exchange(){public void apply(){plan.apply(player.getInventory());}public void restore(){plan.restore(player.getInventory());}};
            }
            public void verify(Player player,Terms terms) {
                if(PendingLivestockItems.count(player.getInventory(),terms)!=terms.count())throw new IllegalStateException("Pending purchase quantity needs review");
            }
            public void release(Player player,Terms terms){PendingLivestockItems.release(player.getInventory(),terms);}
        },r->SavedAidProof.contains(directory,r.terms().player(),PendingLivestockSale.KEY,r.encode()),System::currentTimeMillis);
    }
    LivestockSaleTransfers(Plugin plugin,Owner owner,LivestockMarketPolicy policy,Items items,Proof proof,LongSupplier now) {
        this.plugin=Objects.requireNonNull(plugin);this.owner=Objects.requireNonNull(owner);this.policy=Objects.requireNonNull(policy);
        this.items=Objects.requireNonNull(items);this.proof=Objects.requireNonNull(proof);this.now=Objects.requireNonNull(now);
    }
    static Position position(Location location){return new Position(Objects.requireNonNull(location.getWorld()).getUID(),location.getX(),location.getY(),location.getZ());}
    public boolean buy(Player player,Offer offer) {
        var terms=offer.terms();long time=now.getAsLong();
        if(!owner.ready()||!player.isOnline()||player.isDead()||!player.hasPermission("civilizations.trade")||!policy.accepts(terms)
                ||!terms.player().equals(player.getUniqueId())||time<offer.quotedMillis()||time>=offer.expiresMillis()
                ||!policy.nearby(position(player.getLocation()),terms.world(),offer.x(),offer.y(),offer.z())) {
            player.sendMessage("This offer is no longer available nearby. Reopen /civ market. No items taken.");return false;
        }
        if(work.size()>=16||pending(player)) {
            player.sendMessage("A transfer is still recovering, or trading is busy. No items taken.");return false;
        }
        if(!emptyCursor(player)){player.sendMessage("Put the item on your cursor away before buying.");return false;}
        try {items.plan(player,terms);}catch(IllegalArgumentException unavailable){player.sendMessage(unavailable.getMessage());return false;}
        var receipt=new PendingLivestockSale(terms,PendingLivestockSale.Stage.PREPARED);
        persist(player,receipt);player.sendMessage("Checking saved business stock. Keep your payment and bucket until confirmed.");return true;
    }
    static boolean pending(Player player) {
        var data=player.getPersistentDataContainer();
        return data.has(KEY)||data.has(FoodAidTransfers.KEY)||data.has(FoodPurchaseTransfers.KEY)||data.has(FoodBarterTransfers.KEY);
    }
    private static boolean emptyCursor(Player player){var item=player.getItemOnCursor();return item==null||item.getAmount()==0||item.getType()==org.bukkit.Material.AIR;}
    private static void set(Player player,PendingLivestockSale receipt){player.getPersistentDataContainer().set(KEY,PersistentDataType.STRING,receipt.encode());}
    private void persist(Player player,PendingLivestockSale receipt) {
        set(player,receipt);work.remove(player.getUniqueId());
        try {
            player.saveData();
            work.put(player.getUniqueId(),new Work(receipt,Stage.PROOF,CompletableFuture.supplyAsync(()->{
                try{return proof.contains(receipt);}catch(java.io.IOException failure){throw new CompletionException(failure);}
            },proofs)));
        }catch(RuntimeException failure){warn(player,"Purchase waits for a verified Minecraft save; its recovery record is retained.");}
    }
    @Override public void run() {
        var players=new ArrayList<>(plugin.getServer().getOnlinePlayers());if(players.isEmpty()){work.clear();warned.clear();cursor=0;return;}
        for(int i=0;i<Math.min(4,players.size());i++)poll(players.get(Math.floorMod(cursor++,players.size())));
        var online=new HashSet<UUID>();players.forEach(p->online.add(p.getUniqueId()));work.keySet().removeIf(id->!online.contains(id));warned.retainAll(online);
    }
    public void poll(Player player) {
        if(!owner.ready()||!player.isOnline()||player.isDead())return;
        PendingLivestockSale receipt;
        try {
            var raw=player.getPersistentDataContainer().get(KEY,PersistentDataType.STRING);
            if(raw==null){if(player.getPersistentDataContainer().has(KEY))throw new IllegalArgumentException();work.remove(player.getUniqueId());warned.remove(player.getUniqueId());return;}
            receipt=PendingLivestockSale.decode(raw);if(!receipt.terms().player().equals(player.getUniqueId()))throw new IllegalArgumentException();
        }catch(RuntimeException invalid){warn(player,"Purchase recovery record needs administrator review; no inventory changes made.");return;}
        if(receipt.stage()==PendingLivestockSale.Stage.REVIEW){warn(player,"Purchase inventory needs administrator review; no second delivery will be made.");return;}
        var pending=work.get(player.getUniqueId());
        if(pending==null){if(work.size()<16)persist(player,receipt);return;}
        if(!pending.receipt().equals(receipt)){warn(player,"Purchase record changed; administrator review required.");return;}
        if(!pending.future().isDone())return;
        try {
            var result=pending.future().join();var terms=receipt.terms();
            switch(pending.stage()) {
                case PROOF -> {
                    work.remove(player.getUniqueId());
                    if(!Boolean.TRUE.equals(result)){warn(player,"Purchase is waiting for a verified Minecraft save.");return;}
                    switch(receipt.stage()) {
                        case PREPARED -> {
                            try{work.put(player.getUniqueId(),new Work(receipt,Stage.RESERVE,owner.reserve(terms,position(player.getLocation()))));}
                            catch(RuntimeException unavailable){cancel(player,receipt);}
                        }
                        case EXCHANGED -> {
                            try{items.verify(player,terms);}catch(RuntimeException uncertain){review(player,receipt);return;}
                            work.put(player.getUniqueId(),new Work(receipt,Stage.COMPLETE,owner.complete(terms)));
                        }
                        case CANCELLED -> work.put(player.getUniqueId(),new Work(receipt,Stage.CANCEL,owner.cancel(terms)));
                        case REVIEW -> throw new IllegalStateException("Review cannot exchange");
                    }
                }
                case RESERVE -> exchange(player,receipt,(Reservation)result);
                case COMPLETE -> {
                    var saved=(Receipt)result;
                    if(!saved.terms().equals(terms)||saved.status()!=Status.COMPLETED){review(player,receipt);return;}
                    try{items.release(player,terms);}catch(RuntimeException uncertain){review(player,receipt);return;}
                    clear(player);
                    player.sendMessage("Purchase confirmed: "+terms.count()+" "+terms.product().name().toLowerCase(Locale.ROOT)+" for "+terms.emeralds()+" emeralds.");
                }
                case CANCEL -> {
                    @SuppressWarnings("unchecked") var cancelled=(Optional<Receipt>)result;
                    if(cancelled.isPresent()&&(!cancelled.get().terms().equals(terms)||cancelled.get().status()!=Status.CANCELLED)){review(player,receipt);return;}
                    clear(player);player.sendMessage("Purchase cancelled before delivery. Your payment and bucket were not taken.");
                }
            }
        }catch(RuntimeException failure) {
            work.remove(player.getUniqueId());
            if(pending.stage()==Stage.RESERVE&&pending.future().isCompletedExceptionally())cancel(player,receipt);
            else warn(player,"Purchase remains in recovery; no second delivery is needed.");
        }
    }
    private void exchange(Player player,PendingLivestockSale receipt,Reservation saved) {
        var terms=receipt.terms();
        if(!saved.receipt().terms().equals(terms)){review(player,receipt);return;}
        if(saved.status()==Status.CANCELLED){clear(player);return;}
        if(saved.status()!=Status.RESERVED){review(player,receipt);return;}
        if(!policy.accepts(terms)||!player.hasPermission("civilizations.trade")||!emptyCursor(player)
                ||!policy.nearby(position(player.getLocation()),saved.world(),saved.x(),saved.y(),saved.z())) {cancel(player,receipt);return;}
        final Exchange plan;
        try{plan=items.plan(player,terms);}catch(IllegalArgumentException unavailable){player.sendMessage(unavailable.getMessage());cancel(player,receipt);return;}
        var exchanged=receipt.withStage(PendingLivestockSale.Stage.EXCHANGED);set(player,exchanged);
        try{plan.apply();}
        catch(RuntimeException failure) {
            try{plan.restore();cancel(player,receipt);}catch(RuntimeException uncertain){review(player,receipt);}return;
        }
        persist(player,exchanged);
    }
    private void cancel(Player player,PendingLivestockSale receipt){persist(player,receipt.withStage(PendingLivestockSale.Stage.CANCELLED));}
    private void review(Player player,PendingLivestockSale receipt){persist(player,receipt.withStage(PendingLivestockSale.Stage.REVIEW));warn(player,"Purchase inventory needs administrator review; reserved goods and receipt are retained.");}
    private void clear(Player player) {
        player.getPersistentDataContainer().remove(KEY);work.remove(player.getUniqueId());warned.remove(player.getUniqueId());
        try{player.saveData();}catch(RuntimeException failure){warn(player,"Purchase confirmed; Minecraft still needs to save the cleared receipt.");}
    }
    private void warn(Player player,String message){if(warned.add(player.getUniqueId())){player.sendMessage(message);plugin.getLogger().warning(message);}}
    @Override public void close(){proofs.shutdown();work.clear();warned.clear();}
}
