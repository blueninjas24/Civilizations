package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperDwellingReviewTest {
    private final UUID worldId=new UUID(0,1),donor=new UUID(0,2),building=new UUID(0,3),unit=new UUID(0,4);
    private UUID currentWorld=worldId;private boolean permission=true,admin=false,online=true;private long time=1000;
    private final List<String> messages=new ArrayList<>();private int reads,inspections,lookups;private boolean complete=true,broken=false;
    private final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{
        if(m.getName().equals("getUID"))return currentWorld;throw new AssertionError(m.getName());});
    private Player player(UUID actor){return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->actor;case "getWorld"->world;
        case "hasPermission"->a[0].equals(PaperBuildingProtection.ADMIN)?admin:permission;
        case "sendMessage"->{messages.add((String)a[0]);yield null;}default->throw new AssertionError(m.getName());});}
    private final Bounds bounds=new Bounds(new Position(0,0,0),new Position(4,3,4));
    private final BuildingLayout layout=new BuildingLayout(building,new UUID(0,5),worldId,Kind.HOUSING,bounds,bounds,
            new Position(1,1,0),List.of(),List.of(new Unit(unit,new Position(1,1,0),List.of(bounds),
            List.of(new BuildingLayout.Bed(new Position(2,1,2),Direction.EAST)))));
    private CompletableFuture<BuildingRegistry.Entry> lookup;
    private Observation observation() {
        var cells=new HashMap<Position,Cell>();
        for(var p:DwellingInspection.requiredCells(layout))cells.put(p,Cell.of(p.y()==0||p.y()==3?Type.SUPPORT:Type.PASSABLE));
        cells.put(new Position(2,1,2),Cell.bed(false,"minecraft:white_bed",Direction.EAST));
        if(!broken)cells.put(new Position(3,1,2),Cell.bed(true,"minecraft:white_bed",Direction.EAST));
        return new Observation(worldId,layout.fingerprint(),time,complete,cells);
    }
    private PaperDwellingReview review(boolean donorOwns) {
        var owner=new PaperDwellingReview.Owner(){
            public CompletableFuture<BuildingRegistry.Entry> find(UUID id,UUID actor,boolean administrator,UUID world){lookups++;return lookup!=null?lookup:CompletableFuture.completedFuture(new BuildingRegistry.Entry(layout,donorOwns?donor:new UUID(0,99),0,null));}
            public CompletableFuture<Assessment> inspect(BuildingLayout l,Observation o,long now){inspections++;return CompletableFuture.completedFuture(DwellingInspection.inspect(l,o,now,30_000));}
        };
        return new PaperDwellingReview(owner,id->online?player(id):null,(l,now)->(budget,t)->{reads+=budget;return Optional.of(observation());},()->time,Logger.getLogger("DwellingReviewTest"));
    }
    private void start(PaperDwellingReview r,UUID actor){r.onCommand(player(actor),null,"civ",new String[]{"building","inspect",building.toString()});}
    @Test void reportsRealBedFailuresAndIncompleteEvidenceWithoutClaimingHousingActivation() {
        var r=review(true);start(r,donor);assertEquals(0,reads);r.run();
        assertEquals(256,reads);assertEquals(1,inspections);assertTrue(messages.stream().anyMatch(s->s.contains("1 accessible beds")));
        assertTrue(messages.stream().anyMatch(s->s.contains("usable capacity are separate")));
        broken=true;messages.clear();start(r,donor);r.run();assertTrue(messages.stream().anyMatch(s->s.contains("bed pair changed")));
        complete=false;messages.clear();start(r,donor);r.run();assertTrue(messages.getLast().contains("Previous housing state is unchanged"));
    }
    @Test void allPlayersShareOneCellBudgetAndOnlyFourRequestsCanQueue() {
        admin=true;var r=review(true);
        for(int i=0;i<5;i++)start(r,new UUID(0,10+i));
        assertEquals(4,lookups);assertTrue(messages.getLast().contains("busy"));
        r.run();assertEquals(256,reads);assertEquals(1,inspections);
        r.run();assertEquals(512,reads);assertEquals(2,inspections);
    }
    @Test void currentAuthorityWorldDisconnectExpiryAndCloseCancelUnsubmittedScans() {
        admin=true;var r=review(false);start(r,donor);admin=false;r.run();assertEquals(0,reads);assertTrue(messages.getLast().contains("authority changed"));
        r=review(true);start(r,donor);currentWorld=UUID.randomUUID();r.run();assertEquals(0,reads);currentWorld=worldId;
        start(r,donor);permission=false;r.run();assertEquals(0,reads);permission=true;
        start(r,donor);online=false;r.run();assertEquals(0,reads);online=true;
        start(r,donor);time+=30_001;r.run();assertEquals(0,reads);assertTrue(messages.getLast().contains("expired"));
        start(r,donor);r.close();r.run();assertEquals(0,reads);
    }
    @Test void pendingLookupAndFailuresNeverStartWorldReadsOrAnnounceSuccessfulAssessment() {
        lookup=new CompletableFuture<>();var r=review(true);start(r,donor);start(r,donor);assertEquals(1,lookups);r.run();assertEquals(0,reads);
        lookup.completeExceptionally(new BuildingRegistry.Declined("Unknown saved building"));r.run();assertTrue(messages.getLast().contains("Unknown saved building"));assertEquals(0,reads);
        lookup=CompletableFuture.failedFuture(new CancellationException());start(r,donor);r.run();assertEquals(0,reads);
        r.onCommand(player(donor),null,"civ",new String[]{"building","inspect","bad"});assertTrue(messages.getLast().contains("must be a UUID"));
    }
}
