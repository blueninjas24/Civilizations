package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import com.blueninjas24.civilizations.database.VillageEnrollmentRepository;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.persistence.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class FoodPurchaseTransfersTest {
    @TempDir Path directory;
    private final UUID worldId=UUID.randomUUID(),playerId=UUID.randomUUID();
    private final Map<NamespacedKey,Object> data=new HashMap<>();
    private final List<String> messages=new ArrayList<>();
    private final AtomicLong now=new AtomicLong(1000);
    private ItemStack held=new Plain(Material.WHEAT,8);
    private boolean saves=true,allowed=true;
    private boolean failExchangedSaves, brokenInventory;
    private int exchanges;
    private final World world=(World)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{
        if(m.getName().equals("getUID"))return worldId;throw new AssertionError(m.getName());
    });
    private static final class Plain extends ItemStack {
        private final Material type;private final int count;
        Plain(Material type,int count){super();this.type=type;this.count=count;}
        @Override public Material getType(){return type;}@Override public int getAmount(){return count;}
        @Override public boolean hasItemMeta(){return false;}@Override public ItemStack clone(){return new Plain(type,count);}
    }
    private Player player(){
        var pdc=(PersistentDataContainer)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PersistentDataContainer.class},(p,m,a)->switch(m.getName()){
            case "has"->data.containsKey(a[0]);case "get"->data.get(a[0]);case "set"->{data.put((NamespacedKey)a[0],a[2]);yield null;}
            case "remove"->{data.remove(a[0]);yield null;}default->throw new AssertionError(m.getName());
        });
        var inv=(PlayerInventory)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PlayerInventory.class},(p,m,a)->switch(m.getName()){
            case "getItemInMainHand"->held;case "setItemInMainHand"->{held=(ItemStack)a[0];exchanges++;if(brokenInventory)throw new IllegalStateException("Injected inventory provider failure");yield null;}
            default->throw new AssertionError(m.getName());
        });
        return (Player)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
            case "getUniqueId"->playerId;case "isOnline"->true;case "hasPermission"->allowed;case "getPersistentDataContainer"->pdc;
            case "getInventory"->inv;case "getLocation"->new Location(world,0,64,0);
            case "sendMessage"->{messages.add((String)a[0]);yield null;}case "saveData"->{
                var marker=(String)data.get(FoodPurchaseTransfers.KEY);
                boolean withheld=failExchangedSaves&&marker!=null&&PendingFoodPurchase.decode(marker).stage()==PendingFoodPurchase.Stage.EXCHANGED;
                if(saves&&!withheld)writeProof();yield null;
            }
            default->throw new AssertionError(m.getName());
        });
    }
    private void writeProof()throws IOException{
        var folder=directory.resolve("players");Files.createDirectories(folder);
        try(var out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(folder.resolve(playerId+".dat"))))){
            out.writeByte(10);out.writeUTF("");out.writeByte(10);out.writeUTF("BukkitValues");
            var value=(String)data.get(FoodPurchaseTransfers.KEY);
            if(value!=null){out.writeByte(8);out.writeUTF(PendingFoodPurchase.KEY);out.writeUTF(value);}
            out.writeByte(0);out.writeByte(0);
        }
    }
    private PopulationSession session()throws Exception{
        var config=new YamlConfiguration();try(var reader=new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/economy.yml")),StandardCharsets.UTF_8)){config.load(reader);}
        config.set("starting-units-per-resident.food",0);config.set("food.output-percent",0);
        var base=PopulationSession.Settings.prototype();
        var settings=new PopulationSession.Settings(base.seed(),30000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),MigrationPolicy.disabled(),
                VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
        var session=new PopulationSession(directory.resolve("db").toFile(),Logger.getLogger("PurchaseTransfers"),settings,now::get,
                EconomyConfiguration.read(config),FarmObservation.Policy.prototype(),VillagePriorities.Policy.prototype(),OpeningFunds.prototype());
        session.start(false).get(5,TimeUnit.SECONDS);return session;
    }
    private UUID enroll(PopulationSession session)throws Exception{
        var a=new VillageObservation.Position(0,64,0);var b=new VillageObservation.Position(2,64,0);
        var result=session.observe(new VillageObservation(worldId,"world",now.get(),true,List.of(a,b),List.of(
                new VillageObservation.Person(UUID.randomUUID(),a,true,"A"),new VillageObservation.Person(UUID.randomUUID(),b,true,"B")))).get(5,TimeUnit.SECONDS);
        var town=result.villages().getFirst().settlementId();session.observeFarms(new FarmObservation(town,worldId,1000,true,0)).get(5,TimeUnit.SECONDS);
        now.set(61000);session.pulse().get(5,TimeUnit.SECONDS);return town;
    }
    private FoodPurchaseTransfers transfer(PopulationSession session,FoodPurchases.Policy policy){
        var plugin=(Plugin)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Plugin.class},(p,m,a)->{
            if(m.getName().equals("getLogger"))return Logger.getLogger("PurchaseTransfers");throw new AssertionError(m.getName());
        });
        return new FoodPurchaseTransfers(plugin,session,FoodAidPreview.Policy.prototype(),policy,directory.resolve("players"),count->new Plain(Material.EMERALD,count));
    }
    private Plugin bargainPlugin(Player player){
        var server=(Server)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Server.class},(p,m,a)->{
            if(m.getName().equals("getPlayer"))return playerId.equals(a[0])?player:null;
            throw new AssertionError(m.getName());
        });
        return (Plugin)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Plugin.class},(p,m,a)->switch(m.getName()){
            case "getLogger"->Logger.getLogger("Bargaining");case "getServer"->server;default->throw new AssertionError(m.getName());
        });
    }
    private void awaitMessage(FoodBargainingCommand command,String contains)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(messages.stream().noneMatch(m->m.contains(contains))&&System.nanoTime()<deadline){command.run();Thread.sleep(5);}
        assertTrue(messages.stream().anyMatch(m->m.contains(contains)),messages.toString());
    }
    @Test void commandCounterAndExplicitAcceptancePayExactlyOnce()throws Exception{
        try(var session=session()){
            var town=enroll(session);var player=player();var plugin=bargainPlugin(player);
            try(var transfer=new FoodPurchaseTransfers(plugin,session,FoodAidPreview.Policy.prototype(),FoodPurchases.Policy.prototype(),directory.resolve("players"),count->new Plain(Material.EMERALD,count),FoodBargaining.Policy.prototype());
                var command=new FoodBargainingCommand(plugin,session,transfer,FoodAidPreview.Policy.prototype(),FoodPurchases.Policy.prototype(),FoodBargaining.Policy.prototype())){
                command.onCommand(player,null,"civ",new String[]{"bargain","5"});awaitMessage(command,"counters with 3");
                assertEquals(0,exchanges);assertEquals(4800,session.economyView().get(town).treasury().hundredths());
                command.onCommand(player,null,"civ",new String[]{"bargain","accept"});awaitMessage(command,"Checking your saved bargain");
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                while(!data.containsKey(FoodPurchaseTransfers.KEY)&&System.nanoTime()<deadline){command.run();Thread.sleep(5);}
                assertTrue(data.containsKey(FoodPurchaseTransfers.KEY),messages.toString());finish(transfer,player);
                assertEquals(1,exchanges);assertEquals(Material.EMERALD,held.getType());assertEquals(3,held.getAmount());
                assertEquals(2400,session.economyView().get(town).treasury().hundredths());
                messages.clear();command.onCommand(player,null,"civ",new String[]{"bargain","accept"});awaitMessage(command,"offer unavailable");
                assertEquals(1,exchanges);
            }
        }
    }
    @Test void changedFoodAndInvalidOrUnauthorizedCommandCannotMoveFunds()throws Exception{
        try(var session=session()){
            var town=enroll(session);var player=player();var plugin=bargainPlugin(player);
            try(var transfer=transfer(session,FoodPurchases.Policy.prototype());
                var command=new FoodBargainingCommand(plugin,session,transfer,FoodAidPreview.Policy.prototype(),FoodPurchases.Policy.prototype(),FoodBargaining.Policy.prototype())){
                allowed=false;command.onCommand(player,null,"civ",new String[]{"bargain","5"});assertTrue(messages.getLast().contains("permission"));allowed=true;
                command.onCommand(player,null,"civ",new String[]{"bargain","65"});assertTrue(messages.getLast().contains("1 to 64"));
                command.onCommand(player,null,"civ",new String[]{"bargain","3"});awaitMessage(command,"agrees to 3");held=new Plain(Material.STONE,8);
                messages.clear();command.onCommand(player,null,"civ",new String[]{"bargain","accept"});awaitMessage(command,"food");
                session.bargainAcceptance(playerId,town,FoodBargaining.Policy.prototype()).get(5,TimeUnit.SECONDS);command.run();
                assertFalse(data.containsKey(FoodPurchaseTransfers.KEY));assertEquals(0,exchanges);
                assertEquals(4800,session.economyView().get(town).treasury().hundredths());
            }
        }
    }
    private void finish(FoodPurchaseTransfers transfer,Player player)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(data.containsKey(FoodPurchaseTransfers.KEY)&&System.nanoTime()<deadline){transfer.poll(player);Thread.sleep(5);}
        assertFalse(data.containsKey(FoodPurchaseTransfers.KEY),"Purchase did not finish: "+messages);
    }
    @Test void confirmedFoodExchangePaysOnceAndConsumesFundsThenCreditsFutureFood()throws Exception{
        try(var session=session();var transfer=transfer(session,FoodPurchases.Policy.prototype())){
            var town=enroll(session);var player=player();assertEquals(3,transfer.quote(town,held));
            assertTrue(transfer.sell(player,town,held.clone(),3));assertFalse(transfer.sell(player,town,held.clone(),3));finish(transfer,player);
            assertEquals(Material.EMERALD,held.getType());assertEquals(3,held.getAmount());assertEquals(1,exchanges);
            assertEquals(2400,session.economyView().get(town).treasury().hundredths());assertEquals(1,session.economyView().get(town).foodAid().pendingDeliveries());
            now.set(121000);session.pulse().get(5,TimeUnit.SECONDS);
            assertEquals(8,session.economyView().get(town).latest().reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().flow().imports());
        }
    }
    @Test void failedPreparedSaveCannotReserveDebitOrPayUntilVerified()throws Exception{
        try(var session=session();var transfer=transfer(session,FoodPurchases.Policy.prototype())){
            var town=enroll(session);var player=player();saves=false;assertTrue(transfer.sell(player,town,held.clone(),3));
            for(int i=0;i<20;i++){transfer.poll(player);Thread.sleep(5);}
            assertEquals(0,exchanges);assertEquals(4800,session.economyView().get(town).treasury().hundredths());
            saves=true;finish(transfer,player);assertEquals(1,exchanges);
        }
    }
    @Test void changedHeldStackCancelsReservationWithoutTakingReplacementItems()throws Exception{
        try(var session=session();var transfer=transfer(session,FoodPurchases.Policy.prototype())){
            var town=enroll(session);var player=player();assertTrue(transfer.sell(player,town,held.clone(),3));held=new Plain(Material.STONE,8);
            finish(transfer,player);assertEquals(Material.STONE,held.getType());assertEquals(0,exchanges);
            assertEquals(4800,session.economyView().get(town).treasury().hundredths());assertEquals(0,session.economyView().get(town).foodAid().pendingDeliveries());
        }
    }
    @Test void staleExchangedReceiptAfterCompletionDoesNotExchangeAgain()throws Exception{
        try(var session=session()){
            var town=enroll(session);var player=player();PendingFoodPurchase receipt;
            try(var transfer=transfer(session,FoodPurchases.Policy.prototype())){
                assertTrue(transfer.sell(player,town,held.clone(),3));receipt=PendingFoodPurchase.decode((String)data.get(FoodPurchaseTransfers.KEY));finish(transfer,player);
            }
            data.put(FoodPurchaseTransfers.KEY,receipt.withStage(PendingFoodPurchase.Stage.EXCHANGED).encode());
            try(var restarted=transfer(session,FoodPurchases.Policy.disabled())){finish(restarted,player);}
            assertEquals(1,exchanges);assertEquals(2400,session.economyView().get(town).treasury().hundredths());
            assertEquals(1,session.economyView().get(town).foodAid().pendingDeliveries());
        }
    }
    @Test void failedExchangedSaveKeepsFoodUncreditedAndRecoveryDoesNotPayAgain()throws Exception{
        try(var session=session()){
            var town=enroll(session);var player=player();failExchangedSaves=true;
            try(var transfer=transfer(session,FoodPurchases.Policy.prototype())){
                assertTrue(transfer.sell(player,town,held.clone(),3));
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                while(held.getType()!=Material.EMERALD&&System.nanoTime()<deadline){transfer.poll(player);Thread.sleep(5);}
                assertEquals(Material.EMERALD,held.getType());
                for(int i=0;i<20;i++){transfer.poll(player);Thread.sleep(5);}
                assertEquals(0,session.economyView().get(town).foodAid().pendingDeliveries());
                assertEquals(2400,session.economyView().get(town).treasury().hundredths());
            }
            failExchangedSaves=false;try(var restarted=transfer(session,FoodPurchases.Policy.disabled())){finish(restarted,player);}
            assertEquals(1,exchanges);assertEquals(1,session.economyView().get(town).foodAid().pendingDeliveries());
        }
    }
    @Test void ambiguousInventoryProviderFailureRequiresReviewWithoutAutomaticRefundOrCredit()throws Exception{
        try(var session=session();var transfer=transfer(session,FoodPurchases.Policy.prototype())){
            var town=enroll(session);var player=player();brokenInventory=true;assertTrue(transfer.sell(player,town,held.clone(),3));
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(PendingFoodPurchase.decode((String)data.get(FoodPurchaseTransfers.KEY)).stage()!=PendingFoodPurchase.Stage.REVIEW&&System.nanoTime()<deadline){transfer.poll(player);Thread.sleep(5);}
            assertEquals(PendingFoodPurchase.Stage.REVIEW,PendingFoodPurchase.decode((String)data.get(FoodPurchaseTransfers.KEY)).stage());
            for(int i=0;i<20;i++){transfer.poll(player);Thread.sleep(5);}
            assertEquals(2400,session.economyView().get(town).treasury().hundredths());
            assertEquals(0,session.economyView().get(town).foodAid().pendingDeliveries());assertEquals(2,exchanges);
        }
    }
    @Test void reservedPreparedTradeResumesAfterShutdownEvenWhenNewTradingIsDisabled()throws Exception{
        try(var session=session()){
            var town=enroll(session);var player=player();
            try(var transfer=transfer(session,FoodPurchases.Policy.prototype())){
                assertTrue(transfer.sell(player,town,held.clone(),3));
                var receipt=PendingFoodPurchase.decode((String)data.get(FoodPurchaseTransfers.KEY));
                assertTrue(SavedAidProof.contains(directory.resolve("players"),playerId,PendingFoodPurchase.KEY,receipt.encode()));
                assertEquals("RESERVED",session.reserveFoodPurchase(receipt.terms(),FoodPurchases.Policy.prototype(),FoodAidPreview.Policy.prototype()).get(5,TimeUnit.SECONDS));
                assertEquals(2400,session.economyView().get(town).treasury().hundredths());assertEquals(0,exchanges);
            }
            try(var restarted=transfer(session,FoodPurchases.Policy.disabled())){finish(restarted,player);}
            assertEquals(1,exchanges);assertEquals(3,held.getAmount());
        }
    }
    @Test void rejectedReservationClearsPreparationWithoutChangingGoodsOrMintingRefund()throws Exception{
        try(var session=session();var transfer=transfer(session,FoodPurchases.Policy.prototype())){
            var town=enroll(session);var player=player();assertTrue(transfer.sell(player,town,held.clone(),3));
            session.recordDeliveredFood(UUID.randomUUID(),town,12,"Fixture donation").get(5,TimeUnit.SECONDS);
            finish(transfer,player);assertEquals(0,exchanges);assertEquals(Material.WHEAT,held.getType());
            assertEquals(4800,session.economyView().get(town).treasury().hundredths());
        }
    }
    @Test void permissionOrAnotherPendingTransferPreventsNewPreparation()throws Exception{
        try(var session=session();var transfer=transfer(session,FoodPurchases.Policy.prototype())){
            var town=enroll(session);var player=player();allowed=false;assertFalse(transfer.sell(player,town,held.clone(),3));
            allowed=true;data.put(FoodAidTransfers.KEY,"pending");assertFalse(transfer.sell(player,town,held.clone(),3));assertEquals(0,exchanges);
        }
    }
    @Test void receiptFormatIsBoundedCanonicalAndPreservesAcceptedPayment(){
        var terms=new FoodPurchases.Terms(UUID.randomUUID(),playerId,UUID.randomUUID(),8,"WHEAT",8,3,800);
        for(var stage:PendingFoodPurchase.Stage.values()){
            var receipt=new PendingFoodPurchase(terms,stage);assertEquals(receipt,PendingFoodPurchase.decode(receipt.encode()));
        }
        assertThrows(IllegalArgumentException.class,()->PendingFoodPurchase.decode("x".repeat(261)));
        assertThrows(IllegalArgumentException.class,()->PendingFoodPurchase.decode(new PendingFoodPurchase(terms,PendingFoodPurchase.Stage.PREPARED).encode().replace("|8|WHEAT","|08|WHEAT")));
    }
    @Test void unverifiedOrAmbiguousPayoutIsLockedButPreparationAndCompletedTradesAreNot(){
        var terms=new FoodPurchases.Terms(UUID.randomUUID(),playerId,UUID.randomUUID(),8,"WHEAT",8,3,800);var player=player();
        assertFalse(PendingTradeInventoryGuard.locked(player));
        for(var stage:PendingFoodPurchase.Stage.values()){
            data.put(FoodPurchaseTransfers.KEY,new PendingFoodPurchase(terms,stage).encode());
            assertEquals(stage==PendingFoodPurchase.Stage.EXCHANGED||stage==PendingFoodPurchase.Stage.REVIEW,PendingTradeInventoryGuard.locked(player));
        }
        data.put(FoodPurchaseTransfers.KEY,"malformed");assertTrue(PendingTradeInventoryGuard.locked(player));
        data.remove(FoodPurchaseTransfers.KEY);assertFalse(PendingTradeInventoryGuard.locked(player));
    }
}
