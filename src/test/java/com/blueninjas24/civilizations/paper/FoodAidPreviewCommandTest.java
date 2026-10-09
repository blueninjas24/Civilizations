package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.VillageEnrollmentRepository;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class FoodAidPreviewCommandTest {
    @TempDir Path directory;
    private final UUID worldId = new UUID(0,10);
    private final AtomicLong now = new AtomicLong(1000);
    private final List<String> messages = new ArrayList<>();
    private boolean inventoryRead;
    // Bukkit Location holds only a weak world reference; fixture worlds are not registered in a server.
    private final List<World> fixtureWorlds = new ArrayList<>();
    private static final class PlainStack extends ItemStack {
        private final boolean custom; private final Material material; private final int count;
        PlainStack(Material material, int count, boolean custom) { super(); this.material=material;this.count=count;this.custom=custom; }
        @Override public Material getType() { return material; }
        @Override public int getAmount() { return count; }
        @Override public boolean hasItemMeta() { return custom; }
    }
    private Player player(Location location, ItemStack stack, boolean allowed) {
        var inventory=(PlayerInventory)Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),new Class<?>[]{PlayerInventory.class},(p,m,a)->{
            if(m.getName().equals("getItemInMainHand")) {inventoryRead=true;return stack;}
            throw new AssertionError("Unexpected inventory mutation/access: "+m.getName());
        });
        return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()) {
            case "hasPermission" -> allowed && a[0].equals("civilizations.aid.preview");
            case "sendMessage" -> {messages.add((String)a[0]);yield null;}
            case "getLocation" -> location;
            case "getInventory" -> inventory;
            default -> throw new AssertionError("Unexpected player access: "+m.getName());
        });
    }
    private Location location(UUID id,double x) {
        var world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{
            if(m.getName().equals("getUID"))return id;throw new AssertionError("Unexpected world access: "+m.getName());
        });
        fixtureWorlds.add(world);
        return new Location(world,x,64,0);
    }
    private PopulationSession session() throws Exception {
        var config=new YamlConfiguration();try(var reader=new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/economy.yml")),StandardCharsets.UTF_8)){config.load(reader);}
        var settings=new PopulationSession.Settings(42,30000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),MigrationPolicy.disabled(),VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
        return new PopulationSession(directory.toFile(),Logger.getLogger("AidPreviewTest"),settings,now::get,EconomyConfiguration.read(config),FarmObservation.Policy.prototype());
    }
    private UUID ready(PopulationSession session) throws Exception {
        session.start(false).get(5,TimeUnit.SECONDS);
        var bed=new VillageObservation.Position(0,64,0);var second=new VillageObservation.Position(2,64,0);
        var result=session.observe(new VillageObservation(worldId,"test",now.get(),true,List.of(bed,second),List.of(
            new VillageObservation.Person(new UUID(0,11),bed,true,"First"),new VillageObservation.Person(new UUID(0,12),second,true,"Second")))).get(5,TimeUnit.SECONDS);
        var town=result.villages().getFirst().settlementId();
        session.observeFarms(new FarmObservation(town,worldId,now.get(),true,0)).get(5,TimeUnit.SECONDS);return town;
    }
    @Test void previewShowsConversionAndKnownFoodWithoutTakingItemsOrCreatingReceipt() throws Exception {
        try(var session=session()) {
            var town=ready(session);now.set(61000);session.pulse().get(5,TimeUnit.SECONDS);var before=session.economyView();
            var command=new FoodAidPreviewCommand(session,FoodAidPreview.Policy.prototype());
            command.onCommand(player(location(worldId,0),new PlainStack(Material.BREAD,4,false),true),null,"civ",new String[]{"aid"});
            assertTrue(messages.stream().anyMatch(m->m.contains("= 12 food units")));assertTrue(messages.stream().anyMatch(m->m.contains("demand supplied 4/4")));
            assertTrue(messages.stream().anyMatch(m->m.startsWith("Preview only")));assertTrue(inventoryRead);
            assertEquals(before,session.economyView());assertEquals(0,session.economyView().get(town).foodAid().pendingDeliveries());
        }
    }
    @Test void wrongWorldDistantOrPartialEvidenceRejectBeforeReadingInventory() throws Exception {
        try(var session=session()) {
            var town=ready(session);var command=new FoodAidPreviewCommand(session,FoodAidPreview.Policy.prototype());
            for(var position:List.of(location(UUID.randomUUID(),0),location(worldId,100)))command.onCommand(player(position,new PlainStack(Material.BREAD,4,false),true),null,"civ",new String[]{"aid"});
            session.observeFarms(new FarmObservation(town,worldId,1001,false,0)).get(5,TimeUnit.SECONDS);
            command.onCommand(player(location(worldId,0),new PlainStack(Material.BREAD,4,false),true),null,"civ",new String[]{"aid"});
            assertFalse(inventoryRead);assertEquals(3,messages.size());assertTrue(messages.stream().allMatch(m->m.startsWith("No ready village")));
        }
    }
    @Test void customFoodAndUnsupportedItemsAreRejected() throws Exception {
        try(var session=session()) {
            ready(session);var command=new FoodAidPreviewCommand(session,FoodAidPreview.Policy.prototype());
            for(var stack:List.of(new PlainStack(Material.BREAD,4,true),new PlainStack(Material.DIAMOND,4,false)))command.onCommand(player(location(worldId,0),stack,true),null,"civ",new String[]{"aid"});
            assertEquals(2,messages.size());assertTrue(messages.stream().noneMatch(m->m.startsWith("Food aid preview for")));
        }
    }
    @Test void routerAllowsPlayerPreviewWithoutOpeningAdminDiagnostics() throws Exception {
        try(var session=session()) {
            var router=new CivilizationsCommand(new PopulationDebugCommand(session),new FoodAidPreviewCommand(session,FoodAidPreview.Policy.prototype()));
            var player=player(location(worldId,0),new PlainStack(Material.BREAD,4,false),true);
            assertEquals(List.of("aid"),router.onTabComplete(player,null,"civ",new String[]{""}));
            router.onCommand(player,null,"civ",new String[]{"debug","status"});assertEquals(List.of("You do not have permission to inspect Civilizations."),messages);messages.clear();
            router.onCommand(player,null,"civ",new String[]{"aid"});assertTrue(messages.getFirst().contains("still starting"));assertFalse(inventoryRead);
        }
    }
    @Test void disabledPreviewMalformedTargetAndPermissionDenialHaveNoInventoryEffects() throws Exception {
        try(var session=session()) {
            var player=player(location(worldId,0),new PlainStack(Material.BREAD,4,false),true);
            new FoodAidPreviewCommand(session,new FoodAidPreview.Policy(false,32,Map.of("BREAD",3L))).onCommand(player,null,"civ",new String[]{"aid"});
            var command=new FoodAidPreviewCommand(session,FoodAidPreview.Policy.prototype());command.onCommand(player,null,"civ",new String[]{"aid","bad"});
            command.onCommand(player(location(worldId,0),new PlainStack(Material.BREAD,4,false),false),null,"civ",new String[]{"aid"});
            assertEquals(3,messages.size());assertFalse(inventoryRead);
        }
    }
}
