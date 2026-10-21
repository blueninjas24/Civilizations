package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.database.BuildingRegistry;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperHousingCandidatesTest {
    private final UUID town=new UUID(0,1),worldId=new UUID(0,2),actor=new UUID(0,3);
    private UUID currentWorld=worldId;private boolean permitted=true,online=true;private long now=1000;private int reads;
    private final List<String> messages=new ArrayList<>();private CompletableFuture<List<String>> result;
    private final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{
        if(m.getName().equals("getUID"))return currentWorld;throw new AssertionError("Unexpected world access: "+m.getName());});
    private Player player(UUID id){return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->id;case "getWorld"->world;case "hasPermission"->permitted;case "sendMessage"->{messages.add((String)a[0]);yield null;}default->throw new AssertionError(m.getName());});}
    private PaperHousingCandidates controller(){return new PaperHousingCandidates((v,w)->{reads++;assertEquals(town,v);assertEquals(worldId,w);return result;},id->online?player(id):null,()->now,Logger.getLogger("HousingCandidatesTest"));}
    private void command(PaperHousingCandidates c,UUID player){c.onCommand(player(player),null,"civ",new String[]{"build","housing",town.toString()});}
    @Test void readsOnceAndDeliversOnlyOnPollingWithoutNativeTerrainAccess(){
        result=new CompletableFuture<>();var c=controller();command(c,actor);command(c,actor);assertEquals(1,reads);c.run();
        assertFalse(messages.contains("Fixture family: REUSE"));result.complete(List.of("Fixture family: REUSE"));assertFalse(messages.contains("Fixture family: REUSE"));
        c.run();assertTrue(messages.contains("Fixture family: REUSE"));assertEquals(1,reads);
        assertTrue(messages.stream().anyMatch(s->s.contains("no project will be started")));
    }
    @Test void queueAuthorityWorldDisconnectExpiryAndCloseStayBounded(){
        result=new CompletableFuture<>();var c=controller();for(int i=0;i<5;i++)command(c,new UUID(0,10+i));assertEquals(4,reads);assertTrue(messages.getLast().contains("busy"));
        permitted=false;c.run();permitted=true;command(c,actor);currentWorld=new UUID(0,99);c.run();currentWorld=worldId;
        command(c,actor);online=false;c.run();online=true;command(c,actor);now+=30_001;c.run();assertTrue(messages.getLast().contains("expired"));
        command(c,actor);c.close();result.complete(List.of("Must not deliver"));c.run();assertFalse(messages.contains("Must not deliver"));
    }
    @Test void invalidInputPermissionAndOwnerFailureDoNotAnnounceSuccessfulPlanning(){
        result=CompletableFuture.failedFuture(new BuildingRegistry.Declined("Unknown village in this world"));var c=controller();
        permitted=false;command(c,actor);assertEquals(0,reads);permitted=true;
        c.onCommand(player(actor),null,"civ",new String[]{"build","housing","bad"});assertEquals(0,reads);
        command(c,actor);c.run();assertTrue(messages.getLast().contains("Unknown village in this world"));
        result=CompletableFuture.completedFuture(java.util.stream.IntStream.range(0,24).mapToObj(i->"Line "+i).toList());command(c,actor);c.run();
        assertFalse(messages.contains("Line 20"));assertTrue(messages.getLast().contains("first20"));
    }
}
