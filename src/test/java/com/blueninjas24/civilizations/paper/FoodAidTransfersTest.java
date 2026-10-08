package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.database.VillageEnrollmentRepository;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.persistence.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class FoodAidTransfersTest {
    @TempDir Path directory;
    private final UUID world=UUID.randomUUID(),playerId=UUID.randomUUID();
    private final AtomicLong now=new AtomicLong(1000);
    private final Map<NamespacedKey,Object> data=new HashMap<>();
    private final List<String> messages=new ArrayList<>();
    private ItemStack held=new Plain(Material.BREAD,4);
    private boolean saves=true,allowed=true;
    private int debits;
    private static final class Plain extends ItemStack {
        private final Material type;private final int count;
        Plain(Material type,int count){super();this.type=type;this.count=count;}
        @Override public Material getType(){return type;}@Override public int getAmount(){return count;}
        @Override public boolean hasItemMeta(){return false;}
        @Override public boolean isSimilar(ItemStack other){return other!=null&&other.getType()==type;}
        @Override public ItemStack clone(){return new Plain(type,count);}
    }
    private Player player(){
        var pdc=(PersistentDataContainer)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PersistentDataContainer.class},(p,m,a)->switch(m.getName()){
            case "has" -> data.containsKey(a[0]);case "get" -> data.get(a[0]);case "set" -> {data.put((NamespacedKey)a[0],a[2]);yield null;}case "remove"->{data.remove(a[0]);yield null;}
            default -> throw new AssertionError("Unexpected PDC call "+m.getName());
        });
        var inventory=(PlayerInventory)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PlayerInventory.class},(p,m,a)->switch(m.getName()){
            case "getItemInMainHand" -> held;case "setItemInMainHand"->{held=a[0]==null?new Plain(Material.AIR,0):(ItemStack)a[0];debits++;yield null;}
            default -> throw new AssertionError("Unexpected inventory access "+m.getName());
        });
        var level=(World)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{if(m.getName().equals("getUID"))return world;throw new AssertionError();});
        return (Player)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
            case "getUniqueId" -> playerId;case "hasPermission" -> allowed;case "getPersistentDataContainer" -> pdc;
            case "getInventory" -> inventory;case "getLocation" -> new Location(level,0,64,0);
            case "sendMessage"->{messages.add((String)a[0]);yield null;}
            case "saveData"->{if(saves){var value=(String)data.get(FoodAidTransfers.KEY);if(value!=null){var receipt=PendingFoodAid.decode(value);SavedAidProofTest.write(directory.resolve("players"),receipt,value);}}yield null;}
            default -> throw new AssertionError("Unexpected player access "+m.getName());
        });
    }
    private PopulationSession session() throws Exception {
        var config=new YamlConfiguration();try(var in=new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/economy.yml")),StandardCharsets.UTF_8)){config.load(in);}
        var settings=new PopulationSession.Settings(42,30000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),MigrationPolicy.disabled(),VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
        var session=new PopulationSession(directory.resolve("db").toFile(),Logger.getLogger("AidTransfers"),settings,now::get,EconomyConfiguration.read(config),FarmObservation.Policy.prototype());
        session.start(false).get(5,TimeUnit.SECONDS);return session;
    }
    private UUID enroll(PopulationSession session)throws Exception {
        var bed=new VillageObservation.Position(0,64,0);var second=new VillageObservation.Position(2,64,0);
        var result=session.observe(new VillageObservation(world,"test",now.get(),true,List.of(bed,second),List.of(new VillageObservation.Person(new UUID(0,11),bed,true,"A"),new VillageObservation.Person(new UUID(0,12),second,true,"B")))).get(5,TimeUnit.SECONDS);
        var town=result.villages().getFirst().settlementId();session.observeFarms(new FarmObservation(town,world,now.get(),true,0)).get(5,TimeUnit.SECONDS);return town;
    }
    private FoodAidTransfers transfers(PopulationSession session){
        var plugin=(Plugin)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Plugin.class},(p,m,a)->{if(m.getName().equals("getLogger"))return Logger.getLogger("AidTransfers");throw new AssertionError("Unexpected plugin access "+m.getName());});
        return new FoodAidTransfers(plugin,session,FoodAidPreview.Policy.prototype(),true,directory.resolve("players"));
    }
    private void finish(FoodAidTransfers transfers,Player player)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(data.containsKey(FoodAidTransfers.KEY)&&System.nanoTime()<deadline){transfers.poll(player);Thread.sleep(5);}
        assertFalse(data.containsKey(FoodAidTransfers.KEY),"Recovery failed to finish");
    }
    @Test void confirmedDonationDebitsOnceAndEntersFutureFoodPeriod()throws Exception {
        try(var session=session();var transfer=transfers(session)){
            var town=enroll(session);var player=player();assertTrue(transfer.donate(player,town,held.clone()));finish(transfer,player);
            assertEquals(1,debits);assertEquals(0,held.getAmount());assertEquals(1,session.economyView().get(town).foodAid().pendingDeliveries());
            now.set(61000);session.pulse().get(5,TimeUnit.SECONDS);assertEquals(12,session.economyView().get(town).latest().reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().flow().imports());
        }
    }
    @Test void unsuccessfulVoidSaveCannotCreditAndLaterRecoveryNeverDebitsAgain()throws Exception {
        try(var session=session();var transfer=transfers(session)){
            var town=enroll(session);var player=player();saves=false;assertTrue(transfer.donate(player,town,held.clone()));
            for(int i=0;i<20;i++){transfer.poll(player);Thread.sleep(5);}assertEquals(0,session.economyView().get(town).foodAid().pendingDeliveries());assertTrue(data.containsKey(FoodAidTransfers.KEY));
            saves=true;finish(transfer,player);assertEquals(1,debits);assertEquals(1,session.economyView().get(town).foodAid().pendingDeliveries());
        }
    }
    @Test void shutdownBeforeCreditRecoversOriginalMarkerWithoutSecondRemoval()throws Exception {
        try(var session=session()){
            var town=enroll(session);var player=player();saves=false;
            try(var transfer=transfers(session)){assertTrue(transfer.donate(player,town,held.clone()));}
            saves=true;try(var restarted=transfers(session)){finish(restarted,player);}
            assertEquals(1,debits);assertEquals(1,session.economyView().get(town).foodAid().pendingDeliveries());
        }
    }
    @Test void staleSavedMarkerAfterCreditIsIdempotentAcrossRestart()throws Exception {
        try(var session=session()){
            var town=enroll(session);var player=player();String marker;
            try(var transfer=transfers(session)){assertTrue(transfer.donate(player,town,held.clone()));marker=(String)data.get(FoodAidTransfers.KEY);finish(transfer,player);}
            data.put(FoodAidTransfers.KEY,marker);saves=false;
            try(var restarted=transfers(session)){finish(restarted,player);}
            assertEquals(1,debits);assertEquals(1,session.economyView().get(town).foodAid().pendingDeliveries());
        }
    }
    @Test void changedStackUnknownTownPermissionsAndRepeatedClicksCannotTakeExtraItems()throws Exception {
        try(var session=session();var transfer=transfers(session)){
            var town=enroll(session);var player=player();assertFalse(transfer.donate(player,town,new Plain(Material.BREAD,5)));
            assertFalse(transfer.donate(player,UUID.randomUUID(),held.clone()));allowed=false;assertFalse(transfer.donate(player,town,held.clone()));allowed=true;
            assertEquals(0,debits);saves=false;var offered=held.clone();assertTrue(transfer.donate(player,town,offered));assertFalse(transfer.donate(player,town,offered));assertEquals(1,debits);
        }
    }
    @Test void wrongPlayerRecoveryMarkerDoesNotCreditOrRemoveAnything()throws Exception {
        try(var session=session();var transfer=transfers(session)){
            var town=enroll(session);data.put(FoodAidTransfers.KEY,new PendingFoodAid(UUID.randomUUID(),UUID.randomUUID(),town,12,"BREAD",4).encode());transfer.poll(player());
            assertEquals(0,debits);assertEquals(0,session.economyView().get(town).foodAid().pendingDeliveries());assertTrue(data.containsKey(FoodAidTransfers.KEY));
        }
    }
}
