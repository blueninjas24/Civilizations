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

class FoodBarterTransfersTest {
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
                var marker=(String)data.get(FoodBarterTransfers.KEY);
                boolean withheld=failExchangedSaves&&marker!=null&&PendingFoodBarter.decode(marker).stage()==PendingFoodBarter.Stage.EXCHANGED;
                if(saves&&!withheld)writeProof();yield null;
            }
            default->throw new AssertionError(m.getName());
        });
    }
    private void writeProof()throws IOException{
        var folder=directory.resolve("players");Files.createDirectories(folder);
        try(var out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(folder.resolve(playerId+".dat"))))){
            out.writeByte(10);out.writeUTF("");out.writeByte(10);out.writeUTF("BukkitValues");
            var value=(String)data.get(FoodBarterTransfers.KEY);
            if(value!=null){out.writeByte(8);out.writeUTF(PendingFoodBarter.KEY);out.writeUTF(value);}
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
    private Plugin plugin(Player player) {
        var server=(Server)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Server.class},(p,m,a)->{
            if(m.getName().equals("getPlayer"))return playerId.equals(a[0])?player:null;throw new AssertionError(m.getName());
        });
        return (Plugin)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Plugin.class},(p,m,a)->switch(m.getName()) {
            case "getLogger"->Logger.getLogger("BarterTransfers");case "getServer"->server;default->throw new AssertionError(m.getName());
        });
    }
    private FoodBarterTransfers transfer(PopulationSession session,Player player,FoodBarters.Policy policy) {
        return new FoodBarterTransfers(plugin(player),session,FoodAidPreview.Policy.prototype(),policy,directory.resolve("players"),
                t->new Plain(Material.valueOf(t.paymentItem()),t.paymentCount()));
    }
    private FoodBarters.Terms quote(PopulationSession session,UUID town)throws Exception {
        return session.quoteFoodBarter(UUID.randomUUID(),playerId,town,8,"WHEAT",8,Commodity.TIMBER,FoodBarters.Policy.prototype(),FoodAidPreview.Policy.prototype()).get(5,TimeUnit.SECONDS);
    }
    private long stock(PopulationSession session,UUID town){return session.economyView().get(town).account().state().stocks().get(Commodity.TIMBER).units();}
    private void await(java.util.function.BooleanSupplier done,Runnable poll)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(!done.getAsBoolean()&&System.nanoTime()<deadline){poll.run();Thread.sleep(5);}
        assertTrue(done.getAsBoolean(),messages.toString());
    }
    private void finish(FoodBarterTransfers transfer,Player player)throws Exception{await(()->!data.containsKey(FoodBarterTransfers.KEY),()->transfer.poll(player));}
    @Test void confirmedBarterDebitsActualMaterialsAndSavedExchangePaysOnlyOnce()throws Exception {
        try(var session=session()) {
            var town=enroll(session);var player=player();var terms=quote(session,town);assertEquals(6,terms.paymentCount());
            try(var transfer=transfer(session,player,FoodBarters.Policy.prototype())) {
                assertTrue(transfer.confirm(player,terms));assertFalse(transfer.confirm(player,terms));finish(transfer,player);
                assertEquals(1,exchanges);assertEquals(Material.OAK_LOG,held.getType());assertEquals(6,held.getAmount());assertEquals(2,stock(session,town));
                assertEquals(4800,session.economyView().get(town).treasury().hundredths());assertEquals(1,session.economyView().get(town).foodAid().pendingDeliveries());
                data.put(FoodBarterTransfers.KEY,new PendingFoodBarter(terms,PendingFoodBarter.Stage.EXCHANGED).encode());finish(transfer,player);assertEquals(1,exchanges);
            }
            now.set(121000);session.pulse().get(5,TimeUnit.SECONDS);
            assertEquals(8,session.economyView().get(town).latest().reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().flow().imports());
        }
    }
    @Test void unverifiedPreparationCannotReserveOrPayAndChangedStackRefundsGoods()throws Exception {
        try(var session=session()) {
            var town=enroll(session);var player=player();var terms=quote(session,town);
            try(var transfer=transfer(session,player,FoodBarters.Policy.prototype())) {
                saves=false;assertTrue(transfer.confirm(player,terms));for(int i=0;i<20;i++){transfer.poll(player);Thread.sleep(5);}
                assertEquals(0,exchanges);assertEquals(8,stock(session,town));
                saves=true;held=new Plain(Material.STONE,8);finish(transfer,player);
                assertEquals(Material.STONE,held.getType());assertEquals(0,exchanges);assertEquals(8,stock(session,town));
                assertEquals(0,session.economyView().get(town).foodAid().pendingDeliveries());
            }
        }
    }
    @Test void failedExchangedSaveLocksPayoutUntilRecoveryEvenWithBarterDisabled()throws Exception {
        try(var session=session()) {
            var town=enroll(session);var player=player();var terms=quote(session,town);failExchangedSaves=true;
            try(var transfer=transfer(session,player,FoodBarters.Policy.prototype())) {
                transfer.confirm(player,terms);await(()->held.getType()==Material.OAK_LOG,()->transfer.poll(player));
                for(int i=0;i<20;i++){transfer.poll(player);Thread.sleep(5);}
                assertTrue(PendingTradeInventoryGuard.locked(player));assertEquals(0,session.economyView().get(town).foodAid().pendingDeliveries());assertEquals(2,stock(session,town));
            }
            failExchangedSaves=false;try(var restarted=transfer(session,player,FoodBarters.Policy.disabled())){finish(restarted,player);}
            assertEquals(1,exchanges);assertFalse(PendingTradeInventoryGuard.locked(player));assertEquals(1,session.economyView().get(town).foodAid().pendingDeliveries());
        }
    }
    @Test void uncertainInventoryProviderFailureHoldsMaterialsForReview()throws Exception {
        try(var session=session()) {
            var town=enroll(session);var player=player();brokenInventory=true;
            try(var transfer=transfer(session,player,FoodBarters.Policy.prototype())) {
                transfer.confirm(player,quote(session,town));await(()->PendingFoodBarter.decode((String)data.get(FoodBarterTransfers.KEY)).stage()==PendingFoodBarter.Stage.REVIEW,()->transfer.poll(player));
                assertTrue(PendingTradeInventoryGuard.locked(player));assertEquals(2,stock(session,town));assertEquals(0,session.economyView().get(town).foodAid().pendingDeliveries());
                for(int i=0;i<20;i++){transfer.poll(player);Thread.sleep(5);}assertEquals(2,exchanges);
            }
        }
    }
    @Test void reservedPreparationRecoversAfterAdapterRestartWithoutMintingStock()throws Exception {
        try(var session=session()) {
            var town=enroll(session);var player=player();var terms=quote(session,town);
            try(var transfer=transfer(session,player,FoodBarters.Policy.prototype())) {
                transfer.confirm(player,terms);assertEquals("RESERVED",session.reserveFoodBarter(terms,FoodBarters.Policy.prototype(),FoodAidPreview.Policy.prototype()).get(5,TimeUnit.SECONDS));
                assertEquals(0,exchanges);assertEquals(2,stock(session,town));
            }
            try(var restarted=transfer(session,player,FoodBarters.Policy.disabled())){finish(restarted,player);}
            assertEquals(1,exchanges);assertEquals(2,stock(session,town));
        }
    }
    @Test void changedNeedDeclinesWithoutExchangeAndOtherTransferOrPermissionBlocksPreparation()throws Exception {
        try(var session=session()) {
            var town=enroll(session);var player=player();var terms=quote(session,town);
            try(var transfer=transfer(session,player,FoodBarters.Policy.prototype())) {
                allowed=false;assertFalse(transfer.confirm(player,terms));allowed=true;
                for(var key:List.of(FoodAidTransfers.KEY,FoodPurchaseTransfers.KEY)){data.put(key,"pending");assertFalse(transfer.confirm(player,terms));data.remove(key);}
                transfer.confirm(player,terms);session.recordDeliveredFood(UUID.randomUUID(),town,12,"Fixture relief").get(5,TimeUnit.SECONDS);finish(transfer,player);
                assertEquals(0,exchanges);assertEquals(8,stock(session,town));
            }
        }
    }
    @Test void commandQuoteMovesNothingAndExplicitAcceptanceExchangesSavedTerms()throws Exception {
        try(var session=session()) {
            var town=enroll(session);var player=player();
            try(var transfer=transfer(session,player,FoodBarters.Policy.prototype());var command=new FoodBarterCommand(plugin(player),session,transfer,FoodAidPreview.Policy.prototype(),FoodBarters.Policy.prototype(),45,now::get)) {
                command.onCommand(player,null,"civ",new String[]{"barter","timber"});await(()->messages.stream().anyMatch(m->m.contains("offers 6 oak_log")),command::run);
                assertEquals(0,exchanges);assertEquals(8,stock(session,town));
                command.onCommand(player,null,"civ",new String[]{"barter","accept"});finish(transfer,player);
                assertEquals(1,exchanges);assertEquals(2,stock(session,town));
                command.onCommand(player,null,"civ",new String[]{"barter","accept"});assertEquals(1,exchanges);
            }
        }
    }
    @Test void expiredOrChangedCommandOfferAndInvalidArgumentsCannotExchange()throws Exception {
        try(var session=session()) {
            var town=enroll(session);var player=player();
            try(var transfer=transfer(session,player,FoodBarters.Policy.prototype());var command=new FoodBarterCommand(plugin(player),session,transfer,FoodAidPreview.Policy.prototype(),FoodBarters.Policy.prototype(),1,now::get)) {
                command.onCommand(player,null,"civ",new String[]{"barter","diamond"});assertTrue(messages.getLast().contains("timber"));
                command.onCommand(player,null,"civ",new String[]{"barter","timber"});await(()->messages.stream().anyMatch(m->m.contains("offers 6")),command::run);
                now.addAndGet(1000);command.onCommand(player,null,"civ",new String[]{"barter","accept"});assertTrue(messages.getLast().contains("fresh"));
                messages.clear();command.onCommand(player,null,"civ",new String[]{"barter","timber"});await(()->messages.stream().anyMatch(m->m.contains("offers 6")),command::run);
                held=new Plain(Material.STONE,8);command.onCommand(player,null,"civ",new String[]{"barter","accept"});assertTrue(messages.getLast().contains("food changed"));
                assertFalse(data.containsKey(FoodBarterTransfers.KEY));assertEquals(0,exchanges);assertEquals(8,stock(session,town));
            }
        }
    }
    @Test void boundedReceiptRoundTripsAndUncertainOrMalformedMarkersLockInventory() {
        var terms=new FoodBarters.Terms(UUID.randomUUID(),playerId,UUID.randomUUID(),8,"WHEAT",8,Commodity.TIMBER,"OAK_LOG",6,1);var player=player();
        for(var stage:PendingFoodBarter.Stage.values()) {
            var receipt=new PendingFoodBarter(terms,stage);assertEquals(receipt,PendingFoodBarter.decode(receipt.encode()));data.put(FoodBarterTransfers.KEY,receipt.encode());
            assertEquals(stage==PendingFoodBarter.Stage.EXCHANGED||stage==PendingFoodBarter.Stage.REVIEW,PendingTradeInventoryGuard.locked(player));
        }
        assertThrows(IllegalArgumentException.class,()->PendingFoodBarter.decode("x".repeat(341)));
        assertThrows(IllegalArgumentException.class,()->PendingFoodBarter.decode(new PendingFoodBarter(terms,PendingFoodBarter.Stage.PREPARED).encode().replace("|8|WHEAT","|08|WHEAT")));
        data.put(FoodBarterTransfers.KEY,"malformed");assertTrue(PendingTradeInventoryGuard.locked(player));
    }
}
