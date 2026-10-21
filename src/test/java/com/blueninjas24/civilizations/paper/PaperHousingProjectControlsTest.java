package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.database.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperHousingProjectControlsTest {
    private final UUID actor=new UUID(0,1),worldId=new UUID(0,2),town=new UUID(0,3),draft=new UUID(0,4),family=new UUID(0,5);
    private UUID currentWorld=worldId;private boolean manage=true,preview=false,online=true,unused=true;private long now=1000;private int lookups,saves;private long submitted;
    private CompletableFuture<HousingProjects.Binding> saving;private final List<UUID> operations=new ArrayList<>();
    private final List<String> messages=new ArrayList<>();private final List<Component> buttons=new ArrayList<>();
    private final HousingProjects.Binding binding=new HousingProjects.Binding(new HousingProjects.Proposal(draft,family,500),town,worldId,"0".repeat(64),2,0,null,null);
    private final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{
        if(m.getName().equals("getUID"))return currentWorld;throw new AssertionError("Unexpected world access: "+m.getName());});
    private Player player(UUID id){return(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->id;case "getWorld"->world;
        case "hasPermission"->a[0].equals(PaperHousingProjectControls.MANAGE)?manage:a[0].equals(PaperConstructionPreview.PERMISSION)&&preview;
        case "sendMessage"->{if(a[0] instanceof String text)messages.add(text);else buttons.add((Component)a[0]);yield null;}default->throw new AssertionError(m.getName());});}
    private HousingProjectStatus.Item item(){return new HousingProjectStatus.Item(binding,"starter_home",true,unused?0:1,0,unused,Optional.empty());}
    private PaperHousingProjectControls controls(){return new PaperHousingProjectControls(new PaperHousingProjectControls.Owner(){
        public CompletableFuture<HousingProjectStatus.Report> status(UUID v,UUID w){lookups++;return CompletableFuture.completedFuture(new HousingProjectStatus.Report(List.of(item()),false));}
        public CompletableFuture<HousingProjectStatus.Item> inspect(UUID d,UUID w){lookups++;assertEquals(draft,d);assertEquals(worldId,w);return CompletableFuture.completedFuture(item());}
        public CompletableFuture<HousingProjects.Binding> retire(UUID operation,UUID d,UUID w,long at){saves++;operations.add(operation);assertEquals(draft,d);assertEquals(worldId,w);if(submitted==0)submitted=at;else assertEquals(submitted,at);return saving;}
        public CompletableFuture<ConstructionSupplyOrders.VillageFunding> funding(UUID d,UUID w){lookups++;assertEquals(draft,d);assertEquals(worldId,w);return CompletableFuture.completedFuture(
                new ConstructionSupplyOrders.VillageFunding(draft,2,List.of(new ConstructionSupplyOrders.MaterialNeed(new com.blueninjas24.civilizations.economy.Commodity("stone"),12,2,1,1,0,1)),2940L,5000,3500,1500,50,128,false,"Reference readiness only"));}
    },id->online?player(id):null,()->now,Logger.getLogger("ProjectControlsTest"));}
    private void command(PaperHousingProjectControls c,UUID player,String...parts){var args=new ArrayList<>(List.of("build"));args.addAll(List.of(parts));c.onCommand(player(player),null,"civ",args.toArray(String[]::new));}
    private String token(){return ((ClickEvent.Payload.Text)buttons.getLast().clickEvent().payload()).value().split(" ")[3];}
    @Test void routedManagePermissionDoesNotRequirePreviewAndConfirmationIsPlayerBound(){
        var c=controls();var route=new PaperConstructionPreview(new PaperConstructionLibrary(Map.of()));route.configureProjects(c);
        assertTrue(route.onTabComplete(player(actor),null,"civ",new String[]{"build","cancel"}).contains("cancelproject"));
        route.onCommand(player(actor),null,"civ",new String[]{"build","cancelproject",draft.toString()});c.run();assertEquals(0,saves);assertFalse(buttons.isEmpty());
        String token=token();command(c,new UUID(0,99),"confirmcancel",token);assertEquals(0,saves);
        saving=new CompletableFuture<>();command(c,actor,"confirmcancel",token);assertEquals(1,saves);
        command(c,actor,"confirmcancel",token);assertEquals(1,saves);c.run();assertFalse(messages.stream().anyMatch(s->s.startsWith("Unused home project cancelled")));
        saving.complete(new HousingProjects.Binding(binding.proposal(),town,worldId,binding.planHash(),2,0,operations.getFirst(),submitted));c.run();
        assertTrue(messages.getLast().contains("history retained"));command(c,actor,"confirmcancel",token);assertEquals(1,saves);
    }
    @Test void changedAuthorityWorldExpiryPaidWorkAndQueueLimitsBlockUnsubmittedCancellation(){
        var c=controls();unused=false;command(c,actor,"cancelproject",draft.toString());c.run();assertTrue(buttons.isEmpty());assertEquals(0,saves);unused=true;
        command(c,actor,"cancelproject",draft.toString());c.run();String old=token();manage=false;command(c,actor,"confirmcancel",old);assertEquals(0,saves);c.run();manage=true;
        command(c,actor,"cancelproject",draft.toString());c.run();old=token();currentWorld=new UUID(0,88);command(c,actor,"confirmcancel",old);assertEquals(0,saves);c.run();currentWorld=worldId;
        command(c,actor,"cancelproject",draft.toString());c.run();old=token();now+=60_001;command(c,actor,"confirmcancel",old);assertEquals(0,saves);
        for(int i=10;i<15;i++)command(c,new UUID(0,i),"cancelproject",draft.toString());assertTrue(messages.getLast().contains("busy"));c.run();
        online=false;c.run();online=true;command(c,actor,"cancelproject",draft.toString());c.run();assertTrue(messages.getLast().contains("Unused cancellation preview"));
    }
    @Test void failedSaveRetriesExactTokenAndTimeAndAcceptedSaveSurvivesDisconnectOrClose(){
        var c=controls();command(c,actor,"cancelproject",draft.toString());c.run();String token=token();
        saving=CompletableFuture.failedFuture(new SQLException("Injected save failure"));command(c,actor,"confirmcancel",token);c.run();assertEquals(1,saves);
        now+=100;saving=new CompletableFuture<>();command(c,actor,"confirmcancel",token);assertEquals(2,saves);assertEquals(operations.getFirst(),operations.getLast());
        online=false;c.run();command(c,actor,"cancelrequest");assertTrue(messages.getLast().contains("already submitted"));
        c.close();saving.complete(binding);c.run();assertEquals(2,saves); // Owner work was accepted, never cancelled or duplicated by UI shutdown.
    }
    @Test void statusNeedsOnlyReadPermissionAndDiscardingAReadNeverSaves(){
        manage=false;preview=true;var c=controls();command(c,actor,"projects",town.toString());c.run();assertTrue(messages.stream().anyMatch(s->s.contains("Inspection only")));
        command(c,actor,"projects",town.toString());command(c,actor,"cancelrequest");c.run();assertEquals(0,saves);
        command(c,actor,"cancelproject",draft.toString());assertEquals(2,lookups);
    }
    @Test void fundingReportUsesReadPermissionFormatsValueAndNeverSavesOrGrantsConfirmation(){
        manage=false;preview=true;var c=controls();var route=new PaperConstructionPreview(new PaperConstructionLibrary(Map.of()));route.configureProjects(c);
        route.onCommand(player(actor),null,"civ",new String[]{"build","funding",draft.toString()});c.run();
        assertTrue(messages.stream().anyMatch(s->s.contains("village 50; protected 35; available 15")));
        assertTrue(messages.stream().anyMatch(s->s.contains("Remaining reference cost: 29.4")));
        assertTrue(messages.stream().anyMatch(s->s.contains("still needed 10")&&s.contains("held for stages 1")));
        assertTrue(messages.getLast().contains("no supplier selected"));assertEquals(0,saves);assertTrue(buttons.isEmpty());
        command(c,actor,"funding",draft.toString());preview=false;int count=messages.size();c.run();assertEquals(count,messages.size());assertEquals(0,saves);
    }
}
