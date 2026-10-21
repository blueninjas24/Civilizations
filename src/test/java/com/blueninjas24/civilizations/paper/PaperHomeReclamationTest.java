package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.BuildingConsentRequests;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperHomeReclamationTest {
    private final UUID actor=UUID.randomUUID(),worldId=UUID.randomUUID(),building=UUID.randomUUID(),token=UUID.randomUUID();
    private long time=1000;private boolean permitted=true,online=true;private UUID currentWorld=worldId;
    private int confirms,cancels;private final List<String> messages=new ArrayList<>();
    private final CompletableFuture<BuildingRegistry.Consent> saving=new CompletableFuture<>();
    private BuildingConsentRequests.Preview preview() {
        var bounds=new BuildingLayout.Bounds(new Position(0,0,0),new Position(4,3,4));var entry=new Position(1,1,0);
        var layout=new BuildingLayout(building,UUID.randomUUID(),worldId,BuildingLayout.Kind.HOUSING,bounds,bounds,entry,List.of(),
                List.of(new BuildingLayout.Unit(UUID.randomUUID(),entry,List.of(bounds),List.of(new BuildingLayout.Bed(new Position(2,1,2),Direction.EAST)))));
        return new BuildingConsentRequests.Preview(token,actor,worldId,building,BuildingRegistry.Action.RECLAIM,layout,actor,UUID.randomUUID(),1000,61000,"Impact",Set.of(UUID.randomUUID()),Set.of());
    }
    private final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{if(m.getName().equals("getUID"))return currentWorld;throw new AssertionError(m.getName());});
    private final Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()) {
        case "getUniqueId"->actor;case "getWorld"->world;case "hasPermission"->!a[0].equals(PaperBuildingProtection.ADMIN)&&permitted;
        case "sendMessage"->{if(a[0] instanceof String s)messages.add(s);yield null;}default->throw new AssertionError(m.getName());
    });
    private PaperHomeReclamation controls() {
        return new PaperHomeReclamation(new PaperHomeReclamation.Owner(){
            public CompletableFuture<BuildingConsentRequests.Preview> preview(UUID p,UUID w,UUID b,boolean allowed,boolean admin){return CompletableFuture.completedFuture(PaperHomeReclamationTest.this.preview());}
            public CompletableFuture<BuildingRegistry.Consent> confirm(UUID p,UUID w,UUID t,boolean allowed,boolean admin){confirms++;return saving;}
            public void cancel(UUID p){cancels++;}
        },id->online?player:null,()->time,Logger.getAnonymousLogger());
    }
    private void command(PaperHomeReclamation c,String...args){c.onCommand(player,null,"civ",args);}
    @Test void previewShowsHouseholdImpactAndOnlyMatchingConfirmationCommitsThenDrainsAfterDisconnect() {
        var c=controls();command(c,"building","reclaim",building.toString());c.run();assertEquals(0,confirms);
        assertTrue(messages.stream().anyMatch(s->s.contains("1 assigned households")));
        command(c,"building","confirmreclaim",UUID.randomUUID().toString());assertEquals(0,confirms);
        command(c,"building","confirmreclaim",token.toString());assertEquals(1,confirms);
        command(c,"building","cancelreclaim");assertEquals(0,cancels);online=false;c.run();
        saving.complete(new BuildingRegistry.Consent(token,building,actor,BuildingRegistry.Action.RECLAIM,null,false,1000));c.run();
        assertFalse(saving.isCancelled());c.close();
    }
    @Test void changedWorldPermissionExpiryAndUnsubmittedCancellationRefuseConfirmation() {
        var c=controls();command(c,"building","reclaim",building.toString());c.run();currentWorld=UUID.randomUUID();
        command(c,"building","confirmreclaim",token.toString());assertEquals(0,confirms);c.run();assertEquals(1,cancels);
        currentWorld=worldId;command(c,"building","reclaim",building.toString());c.run();permitted=false;
        command(c,"building","confirmreclaim",token.toString());assertEquals(0,confirms);c.run();
        permitted=true;command(c,"building","reclaim",building.toString());c.run();time=61000;
        command(c,"building","confirmreclaim",token.toString());assertEquals(0,confirms);c.run();
        time=1000;command(c,"building","reclaim",building.toString());c.run();command(c,"building","cancelreclaim");assertTrue(cancels>=4);c.close();
    }
    @Test void closingKeepsAlreadyAcceptedSaveAlive() {
        var c=controls();command(c,"building","reclaim",building.toString());c.run();command(c,"building","confirmreclaim",token.toString());
        c.close();assertEquals(0,cancels);assertFalse(saving.isCancelled());
    }
}
