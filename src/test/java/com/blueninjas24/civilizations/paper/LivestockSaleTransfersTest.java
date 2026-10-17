package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.database.LivestockMarket.*;
import com.blueninjas24.civilizations.database.LivestockProductSales.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

/** Handoff/crash tests use a minimal inventory provider. Native Paper probe covers real stacks/PDC. */
class LivestockSaleTransfersTest {
    @TempDir Path directory;
    final UUID playerId=new UUID(1,1),worldId=new UUID(2,2);
    final LivestockMarketPolicy policy=LivestockMarketPolicy.prototype();
    final Terms terms=policy.terms(new UUID(3,3),playerId,new UUID(4,4),worldId,new UUID(5,5),Product.MILK);
    final Map<NamespacedKey,Object> data=new HashMap<>();
    final List<String> messages=new ArrayList<>();
    boolean allowed=true,online=true,dead,saves=true,failExchangeSave,throwExchangeSave;
    double x;long time=1000;int saveCalls;
    final Owner owner=new Owner();final Items items=new Items();
    final World world=(World)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{
        if(m.getName().equals("getUID"))return worldId;throw new AssertionError(m.getName());
    });
    final Player player=player();
    private Player player() {
        var pdc=(PersistentDataContainer)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PersistentDataContainer.class},(p,m,a)->switch(m.getName()){
            case "get"->data.get(a[0]);case "has"->data.containsKey(a[0]);case "set"->{data.put((NamespacedKey)a[0],a[2]);yield null;}
            case "remove"->{data.remove(a[0]);yield null;}default->throw new AssertionError(m.getName());
        });
        return (Player)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
            case "getUniqueId"->playerId;case "isOnline"->online;case "isDead"->dead;case "hasPermission"->allowed;
            case "getLocation"->new Location(world,x,64,0);case "getItemOnCursor"->null;case "getPersistentDataContainer"->pdc;
            case "sendMessage"->{messages.add((String)a[0]);yield null;}
            case "saveData"->{save();yield null;}
            default->throw new AssertionError(m.getName());
        });
    }
    private void save()throws IOException {
        saveCalls++;var raw=(String)data.get(LivestockSaleTransfers.KEY);
        boolean exchanged=raw!=null&&PendingLivestockSale.decode(raw).stage()==PendingLivestockSale.Stage.EXCHANGED;
        if(exchanged&&throwExchangeSave)throw new IllegalStateException("Injected saveData exception");
        if(!saves||exchanged&&failExchangeSave)return;
        try(var out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(directory.resolve(playerId+".dat"))))) {
            out.writeByte(10);out.writeUTF("");out.writeByte(10);out.writeUTF("BukkitValues");
            if(raw!=null){out.writeByte(8);out.writeUTF(PendingLivestockSale.KEY);out.writeUTF(raw);}
            out.writeByte(0);out.writeByte(0);
        }
    }
    private final class Owner implements LivestockSaleTransfers.Owner {
        boolean ready=true,decline,failCompleteOnce;int stock=2,reservations,completions,cancellations;long cash;
        Receipt saved;CompletableFuture<Reservation> held;
        public boolean ready(){return ready;}
        Reservation result(){return new Reservation(saved,worldId,0,64,0);}
        public CompletableFuture<Reservation> reserve(Terms t,LivestockMarketPolicy.Position position) {
            reservations++;
            if(decline)return CompletableFuture.failedFuture(new IllegalStateException("Unavailable stock"));
            if(saved==null){stock-=t.count();saved=new Receipt(t,Status.RESERVED,1);}else assertEquals(saved.terms(),t);
            return held==null?CompletableFuture.completedFuture(result()):held;
        }
        public CompletableFuture<Receipt> complete(Terms t) {
            completions++;assertNotNull(saved);assertEquals(saved.terms(),t);
            if(saved.status()==Status.RESERVED){cash+=t.cost();saved=new Receipt(t,Status.COMPLETED,1);}
            if(failCompleteOnce){failCompleteOnce=false;return CompletableFuture.failedFuture(new IllegalStateException("Committed response was lost"));}
            return CompletableFuture.completedFuture(saved);
        }
        public CompletableFuture<Optional<Receipt>> cancel(Terms t) {
            cancellations++;
            if(saved==null)return CompletableFuture.completedFuture(Optional.empty());
            if(saved.status()==Status.RESERVED){stock+=t.count();saved=new Receipt(t,Status.CANCELLED,1);}
            return CompletableFuture.completedFuture(Optional.of(saved));
        }
    }
    private final class Items implements LivestockSaleTransfers.Items {
        int emeralds=5,buckets=1,goods,exchanges,releases;boolean failApply,failRestore;
        public LivestockSaleTransfers.Exchange plan(Player p,Terms t) {
            if(emeralds<t.emeralds()||buckets<t.emptyBuckets())throw new IllegalArgumentException("Missing payment or bucket");
            int beforeMoney=emeralds,beforeBuckets=buckets,beforeGoods=goods;
            return new LivestockSaleTransfers.Exchange() {
                public void apply(){emeralds-=t.emeralds();buckets-=t.emptyBuckets();goods+=t.count();exchanges++;if(failApply)throw new IllegalStateException("Injected partial provider failure");}
                public void restore(){if(failRestore)throw new IllegalStateException("Injected failed restore");emeralds=beforeMoney;buckets=beforeBuckets;goods=beforeGoods;}
            };
        }
        public void verify(Player p,Terms t){if(goods!=t.count())throw new IllegalStateException("Missing or unexpected pending quantity");}
        public void release(Player p,Terms t){assertEquals(t.count(),goods);releases++;}
    }
    private LivestockSaleTransfers transfer() {return transfer(policy);}
    private LivestockSaleTransfers transfer(LivestockMarketPolicy settings) {
        var server=(Server)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Server.class},(p,m,a)->{
            if(m.getName().equals("getOnlinePlayers"))return online?List.of(player):List.of();throw new AssertionError(m.getName());
        });
        var plugin=(Plugin)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Plugin.class},(p,m,a)->switch(m.getName()){
            case "getLogger"->Logger.getLogger("Livestock transfer fixture");case "getServer"->server;default->throw new AssertionError(m.getName());
        });
        return new LivestockSaleTransfers(plugin,owner,settings,items,r->SavedAidProof.contains(directory,r.terms().player(),PendingLivestockSale.KEY,r.encode()),()->time);
    }
    Offer offer(){return new Offer(terms,"Fixture dairy",0,64,0,2,1000,31000);}
    PendingLivestockSale receipt(){return PendingLivestockSale.decode((String)data.get(LivestockSaleTransfers.KEY));}
    void await(LivestockSaleTransfers transfer,java.util.function.BooleanSupplier condition)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
        while(!condition.getAsBoolean()&&System.nanoTime()<deadline){transfer.run();Thread.sleep(2);}
        assertTrue(condition.getAsBoolean(),messages.toString());
    }
    void finish(LivestockSaleTransfers transfer)throws Exception{await(transfer,()->!data.containsKey(LivestockSaleTransfers.KEY));}
    @Test void exactPaymentContainerAndBusinessRevenueOccurOnceUnderFrequentPolling()throws Exception {
        try(var t=transfer()){assertTrue(t.buy(player,offer()));assertFalse(t.buy(player,offer()));finish(t);for(int i=0;i<20;i++)t.run();}
        assertEquals(3,items.emeralds);assertEquals(0,items.buckets);assertEquals(1,items.goods);assertEquals(1,items.exchanges);assertEquals(1,items.releases);
        assertEquals(1,owner.stock);assertEquals(1600,owner.cash);assertEquals(Status.COMPLETED,owner.saved.status());
    }
    @Test void eggOrdersChargeTheirOwnTermsWithoutTakingOrCreatingBuckets()throws Exception {
        owner.stock=5;var eggs=policy.terms(UUID.randomUUID(),playerId,terms.settlement(),worldId,terms.seller(),Product.EGGS);
        try(var t=transfer()){assertTrue(t.buy(player,new Offer(eggs,"Fixture chickens",0,64,0,5,1000,31000)));finish(t);}
        assertEquals(4,items.emeralds);assertEquals(1,items.buckets);assertEquals(4,items.goods);assertEquals(1,owner.stock);assertEquals(800,owner.cash);assertEquals(1,items.exchanges);
    }
    @Test void unsavedIntentCannotReserveAnythingUntilIndependentFileProofExists()throws Exception {
        saves=false;try(var t=transfer()){assertTrue(t.buy(player,offer()));for(int i=0;i<20;i++){t.run();Thread.sleep(2);}assertEquals(0,owner.reservations);assertEquals(0,items.exchanges);saves=true;finish(t);}
        assertEquals(1600,owner.cash);assertEquals(1,items.exchanges);
    }
    @Test void exchangedGoodsCannotCreditSellerUntilTheirSaveProofThenRestartNeverDeliversAgain()throws Exception {
        failExchangeSave=true;
        try(var t=transfer()){t.buy(player,offer());await(t,()->items.exchanges==1);for(int i=0;i<20;i++){t.run();Thread.sleep(2);}assertEquals(0,owner.cash);assertEquals(PendingLivestockSale.Stage.EXCHANGED,receipt().stage());assertTrue(PendingTradeInventoryGuard.locked(player));}
        failExchangeSave=false;try(var restarted=transfer()){finish(restarted);}
        assertEquals(1600,owner.cash);assertEquals(1,items.exchanges);assertFalse(PendingTradeInventoryGuard.locked(player));
    }
    @Test void thrownExchangeSaveDiscardsOldWorkAndRetriesCurrentReceiptInsteadOfStrandingIt()throws Exception {
        throwExchangeSave=true;try(var t=transfer()){t.buy(player,offer());await(t,()->items.exchanges==1);assertEquals(0,owner.cash);throwExchangeSave=false;finish(t);}
        assertEquals(1,items.exchanges);assertEquals(1600,owner.cash);
    }
    @Test void externalLossOfPendingGoodsRequiresReviewBeforeSellerCreditAndNeverInventsReplacement()throws Exception {
        failExchangeSave=true;
        try(var t=transfer()){t.buy(player,offer());await(t,()->items.exchanges==1);items.goods=0;failExchangeSave=false;await(t,()->receipt().stage()==PendingLivestockSale.Stage.REVIEW);}
        assertEquals(0,owner.completions);assertEquals(0,owner.cash);assertEquals(1,owner.stock);assertEquals(0,owner.cancellations);assertEquals(1,items.exchanges);
    }
    @Test void lostCompletionResponseRetriesLedgerWithoutSecondDeliveryOrCredit()throws Exception {
        owner.failCompleteOnce=true;try(var t=transfer()){t.buy(player,offer());finish(t);}
        assertEquals(2,owner.completions);assertEquals(1600,owner.cash);assertEquals(1,items.exchanges);assertEquals(1,items.releases);
    }
    @Test void deathPausesInventoryFinalizationUntilRespawnAndOtherDeathRulesAreUntouched()throws Exception {
        failExchangeSave=true;try(var t=transfer()){t.buy(player,offer());await(t,()->items.exchanges==1);dead=true;failExchangeSave=false;
            for(int i=0;i<20;i++)t.run();assertEquals(0,owner.cash);assertEquals(0,items.releases);assertTrue(data.containsKey(LivestockSaleTransfers.KEY));dead=false;finish(t);}
        assertEquals(1,items.exchanges);assertEquals(1600,owner.cash);
    }
    @Test void movedPlayerIsRecheckedAfterDelayedSqlAcceptanceAndReservationIsReturned()throws Exception {
        owner.held=new CompletableFuture<>();try(var t=transfer()){t.buy(player,offer());await(t,()->owner.saved!=null);x=65;owner.held.complete(owner.result());finish(t);}
        assertEquals(Status.CANCELLED,owner.saved.status());assertEquals(2,owner.stock);assertEquals(0,owner.cash);assertEquals(0,items.exchanges);assertEquals(5,items.emeralds);assertEquals(1,items.buckets);
    }
    @Test void changedPermissionOrMissingBucketAfterSqlAcceptanceCancelsWithoutTakingAnything()throws Exception {
        owner.held=new CompletableFuture<>();try(var t=transfer()){t.buy(player,offer());await(t,()->owner.saved!=null);allowed=false;owner.held.complete(owner.result());finish(t);}
        assertEquals(0,items.exchanges);assertEquals(2,owner.stock);
    }
    @Test void missingInventoryAtFinalHandoffAlsoReturnsRealHeldStock()throws Exception {
        owner.held=new CompletableFuture<>();try(var t=transfer()){t.buy(player,offer());await(t,()->owner.saved!=null);items.buckets=0;owner.held.complete(owner.result());finish(t);}
        assertEquals(2,owner.stock);assertEquals(0,items.exchanges);assertEquals(5,items.emeralds);
    }
    @Test void partialInventoryFailureRestoresSnapshotThenCancelsRatherThanCreatingRevenue()throws Exception {
        items.failApply=true;try(var t=transfer()){t.buy(player,offer());finish(t);}
        assertEquals(5,items.emeralds);assertEquals(1,items.buckets);assertEquals(0,items.goods);assertEquals(2,owner.stock);assertEquals(0,owner.cash);
    }
    @Test void uncertainRestoreRequiresReviewAndNeverReturnsAlreadyPossiblyDeliveredStock()throws Exception {
        items.failApply=true;items.failRestore=true;try(var t=transfer()){t.buy(player,offer());await(t,()->receipt().stage()==PendingLivestockSale.Stage.REVIEW);for(int i=0;i<20;i++)t.run();}
        assertEquals(1,owner.stock);assertEquals(0,owner.cash);assertEquals(0,owner.cancellations);assertTrue(PendingTradeInventoryGuard.locked(player));
    }
    @Test void unsubmittedOrDeclinedReservationCanClearWithoutInventingARefund()throws Exception {
        owner.decline=true;try(var t=transfer()){t.buy(player,offer());finish(t);}
        assertNull(owner.saved);assertEquals(2,owner.stock);assertEquals(0,items.exchanges);assertEquals(0,owner.cash);
    }
    @Test void expiredWrongPlayerDisabledAndMissingBucketOffersCannotStart() {
        try(var t=transfer()){
            time=31000;assertFalse(t.buy(player,offer()));time=1000;items.buckets=0;assertFalse(t.buy(player,offer()));items.buckets=1;
            allowed=false;assertFalse(t.buy(player,offer()));allowed=true;x=65;assertFalse(t.buy(player,offer()));x=0;
            var wrong=policy.terms(UUID.randomUUID(),UUID.randomUUID(),terms.settlement(),worldId,terms.seller(),Product.MILK);
            assertFalse(t.buy(player,new Offer(wrong,"Other player",0,64,0,2,1000,31000)));
        }
        try(var t=transfer(LivestockMarketPolicy.disabled())){assertFalse(t.buy(player,offer()));}
        assertTrue(data.isEmpty());assertEquals(0,owner.reservations);assertEquals(0,items.exchanges);
    }
    @Test void completedLedgerWithPreparedReceiptRequiresReviewInsteadOfDuplicateDelivery()throws Exception {
        owner.saved=new Receipt(terms,Status.COMPLETED,1);
        try(var t=transfer()){t.buy(player,offer());await(t,()->receipt().stage()==PendingLivestockSale.Stage.REVIEW);}
        assertEquals(0,items.exchanges);assertEquals(0,owner.cancellations);
    }
    @Test void exchangedRecoveryFinishesEvenAfterMarketIsDisabledButDoesNotStartNewOrders()throws Exception {
        failExchangeSave=true;try(var t=transfer()){t.buy(player,offer());await(t,()->items.exchanges==1);}
        failExchangeSave=false;try(var t=transfer(LivestockMarketPolicy.disabled())){finish(t);assertFalse(t.buy(player,offer()));}
        assertEquals(1600,owner.cash);assertEquals(1,items.exchanges);
    }
    @Test void otherTransferKeysAndMalformedLivestockProofsBlockFreshOrdersWithoutInventoryChanges() {
        try(var t=transfer()) {
            for(var key:List.of(FoodAidTransfers.KEY,FoodPurchaseTransfers.KEY,FoodBarterTransfers.KEY)){
                data.put(key,"other pending transfer");assertFalse(t.buy(player,offer()));data.remove(key);
            }
            data.put(LivestockSaleTransfers.KEY,"malformed");assertFalse(t.buy(player,offer()));t.run();assertTrue(PendingTradeInventoryGuard.locked(player));
        }
        assertEquals(0,owner.reservations);assertEquals(0,items.exchanges);
    }
}
