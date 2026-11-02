package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.WorkplaceDonationRequests;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.JobRequirements;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperWorkplaceDonationTest {
    final UUID worldId=UUID.randomUUID(),town=UUID.randomUUID(),actor=UUID.randomUUID(),business=UUID.randomUUID(),person=UUID.randomUUID();
    UUID currentWorld=worldId;boolean permission=true,online=true;long time=1000;int reads,previews,saves,cancels,lookups;
    BuildingLayout.Bounds bounds=new BuildingLayout.Bounds(new Position(0,0,0),new Position(4,3,4));
    WorkplaceDonationRequests.Preview preview;CompletableFuture<WorkplaceDonation.Receipt> saving=new CompletableFuture<>();
    final List<String> messages=new ArrayList<>();
    final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{if(m.getName().equals("getUID"))return currentWorld;throw new AssertionError(m.getName());});
    Player player(UUID id){return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->id;case "getWorld"->world;case "getLocation"->new Location(world,0,1,2);case "hasPermission"->permission;
        case "sendMessage"->{messages.add(a[0].toString());yield null;}default->throw new AssertionError(m.getName());});}
    BusinessFounding founding(){var site=new BusinessSite(UUID.randomUUID(),town,worldId,3,1,2,0,"Fixture workplace");var firm=new Business(business,town,"Existing firm",person,person,"blacksmith",site.id(),2,0);var requirement=new JobRequirements(0,Map.of());return new BusinessFounding(firm,site,requirement,requirement,0,"Fixture");}
    PaperWorkplaceDonation controller(){return new PaperWorkplaceDonation(new PaperWorkplaceDonation.Owner(){
        public CompletableFuture<BusinessFounding> target(UUID id,UUID world){lookups++;return CompletableFuture.completedFuture(founding());}
        public CompletableFuture<WorkplaceDonationRequests.Preview> preview(UUID player,UUID world,UUID firm,BuildingLayout layout,WorkplaceInspection.Evidence evidence){previews++;preview=new WorkplaceDonationRequests.Preview(UUID.randomUUID(),player,firm,layout,null,time,time+60_000,WorkplaceInspection.inspect(layout,evidence,time,5000,WorkplaceInspection.Policy.prototype()));return CompletableFuture.completedFuture(preview);}
        public CompletableFuture<WorkplaceDonation.Receipt> confirm(UUID player,UUID world,UUID token,WorkplaceInspection.Evidence evidence){assertEquals(preview.token(),token);assertEquals(time,evidence.structure().capturedMillis());saves++;return saving;}
        public void cancel(UUID player){cancels++;}
    },p->bounds,id->online?player(id):null,(layout,marker,started)->(budget,now)->{
        reads+=budget;var cells=new HashMap<Position,Cell>();for(var p:DwellingInspection.requiredCells(layout))cells.put(p,Cell.of(p.y()==0||p.y()==3?Type.SUPPORT:Type.PASSABLE));cells.put(marker,Cell.of(Type.SUPPORT));
        return Optional.of(new WorkplaceInspection.Evidence(new Observation(worldId,layout.fingerprint(),started,true,cells),marker,Optional.of(WorkplaceObservation.Kind.SMITHING_TABLE),true));
    },()->time,Logger.getAnonymousLogger());}
    void start(PaperWorkplaceDonation c,UUID id){c.onCommand(player(id),null,"civ",new String[]{"building","workplace",business.toString()});}
    void confirm(PaperWorkplaceDonation c){c.onCommand(player(actor),null,"civ",new String[]{"building","confirmworkplace",preview.token().toString(),"owned"});}
    @Test void previewNeverSavesAndOwnedConfirmationRequiresASecondFreshSurvey(){
        var c=controller();start(c,actor);c.run();assertEquals(1,previews);assertEquals(0,saves);assertEquals(128,reads);
        time=2000;confirm(c);assertEquals(0,saves);c.run();assertEquals(1,saves);assertEquals(256,reads);
        for(int i=0;i<10;i++)c.run();assertEquals(256,reads);assertEquals(1,saves);assertTrue(messages.stream().anyMatch(s->s.contains("Nothing donated yet")));c.close();assertFalse(saving.isCancelled());
    }
    @Test void anAcceptedSaveDrainsAfterDisconnectWithoutCancellingTheOwnerOperation(){
        var c=controller();start(c,actor);c.run();confirm(c);c.run();online=false;c.run();assertEquals(0,cancels);assertFalse(saving.isCancelled());
        var receipt=new WorkplaceDonation.Receipt(new BuildingRegistry.Consent(preview.token(),preview.layout().id(),actor,BuildingRegistry.Action.DONATE,null,false,time),
                new BusinessPremises.Hold(new BusinessPremises.Request(UUID.randomUUID(),business,preview.layout().id(),preview.token(),preview.layout().fingerprint(),time),null,null));
        saving.complete(receipt);c.run();assertEquals(1,cancels);assertEquals(1,saves);c.close();
    }
    @Test void changedPermissionSelectionExpiryAndCancelPreventSubmission(){
        var c=controller();start(c,actor);c.run();permission=false;confirm(c);c.run();assertEquals(0,saves);permission=true;
        start(c,actor);c.run();bounds=new BuildingLayout.Bounds(new Position(0,0,0),new Position(5,3,4));confirm(c);c.run();assertEquals(0,saves);
        start(c,actor);c.run();time=preview.expiresMillis();confirm(c);c.run();assertEquals(0,saves);
        start(c,actor);c.onCommand(player(actor),null,"civ",new String[]{"building","cancelworkplace"});c.run();assertEquals(0,saves);c.close();
    }
    @Test void allPlayersShareOneNativeBudgetAndOnlyFourPreviewsCanQueue(){
        var c=controller();for(int i=0;i<5;i++)start(c,new UUID(0,i));assertEquals(4,lookups);assertTrue(messages.getLast().contains("busy"));
        c.run();assertEquals(128,reads);assertEquals(1,previews);c.run();assertEquals(256,reads);assertEquals(2,previews);assertEquals(0,saves);c.close();
    }
    @Test void explicitReclaimedBuildingIdIsFrozenForOwnerValidatedRedonation(){
        UUID previousBuilding=UUID.randomUUID();var c=controller();
        c.onCommand(player(actor),null,"civ",new String[]{"building","workplace",business.toString(),previousBuilding.toString()});c.run();
        assertEquals(previousBuilding,preview.layout().id());assertEquals(0,saves);c.close();
    }
}
