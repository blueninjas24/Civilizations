package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperHomeDonationTest {
    private final UUID actor=new UUID(0,1),worldId=new UUID(0,2),village=new UUID(0,3);
    private UUID currentWorld=worldId;private boolean allowed=true,online=true,delayed=false;private long time=1000;
    private int reads,previews,confirms,cancels;private final List<String> messages=new ArrayList<>();private final List<Component> buttons=new ArrayList<>();
    private Bounds selected=new Bounds(new Position(0,0,0),new Position(4,3,4));
    private PopulationSession.HomeDonationPreview preview;private CompletableFuture<BuildingRegistry.Consent> saved;
    private final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{
        if(m.getName().equals("getUID"))return currentWorld;throw new AssertionError(m.getName());});
    private Player player(UUID id){return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->id;case "getWorld"->world;case "getLocation"->new Location(world,0,1,0);case "hasPermission"->allowed;
        case "sendMessage"->{if(a[0] instanceof Component c)buttons.add(c);else messages.add((String)a[0]);yield null;}
        default->throw new AssertionError(m.getName());});}
    private Observation frame(Bounds bounds) {
        var cells=new HashMap<Position,Cell>();
        for(int x=0;x<=4;x++)for(int y=0;y<=3;y++)for(int z=0;z<=4;z++)cells.put(new Position(x,y,z),Cell.of(y==0||y==3?Type.SUPPORT:Type.PASSABLE));
        cells.put(new Position(1,1,2),Cell.bed(false,"minecraft:white_bed",Direction.EAST));cells.put(new Position(2,1,2),Cell.bed(true,"minecraft:white_bed",Direction.EAST));
        return new Observation(worldId,HomeDeclaration.regionHash(worldId,bounds),time,true,cells);
    }
    private PaperHomeDonation controls() {
        var owner=new PaperHomeDonation.Owner(){
            public CompletableFuture<PopulationSession.HomeDonationPreview> preview(UUID who,UUID w,UUID building,UUID town,Bounds bounds,Position entry,Observation observation) {
                previews++;var review=HomeDeclaration.singleHousehold(building,town,w,bounds,bounds,entry,observation,time,Policy.prototype());assertTrue(review.passes());
                var consent=new BuildingConsentRequests.Preview(UUID.randomUUID(),who,w,building,BuildingRegistry.Action.DONATE,review.layout(),who,null,time,time+60_000,"Fixture preview");
                preview=new PopulationSession.HomeDonationPreview(review,consent);return CompletableFuture.completedFuture(preview);
            }
            public CompletableFuture<BuildingRegistry.Consent> confirm(UUID who,UUID w,UUID token) {
                confirms++;assertEquals(preview.consent().token(),token);return saved!=null?saved:CompletableFuture.completedFuture(new BuildingRegistry.Consent(token,preview.review().layout().id(),who,BuildingRegistry.Action.DONATE,null,false,time));
            }
            public void cancel(UUID who){cancels++;}
        };
        return new PaperHomeDonation(owner,p->selected,id->online?player(id):null,(w,b,t)->(budget,now)->{reads+=budget;return delayed?Optional.empty():Optional.of(frame(b));},()->time,Logger.getLogger("HomeDonationTest"));
    }
    private void command(PaperHomeDonation c,String...args){c.onCommand(player(actor),null,"civ",args);}
    private void begin(PaperHomeDonation c){command(c,"building","home",village.toString());}
    private void confirm(PaperHomeDonation c,UUID token){command(c,"building","confirmhome",token.toString(),"owned");}
    @Test void explicitReviewedHomeNeedsMatchingOwnershipConfirmationAndSavesNoCapacity() {
        var c=controls();begin(c);assertEquals(0,reads);assertEquals(0,previews);c.run();assertEquals(128,reads);assertEquals(1,previews);assertEquals(0,confirms);
        assertTrue(messages.stream().anyMatch(s->s.contains("ONE family home")));
        String click=((net.kyori.adventure.text.event.ClickEvent.Payload.Text)buttons.getFirst().clickEvent().payload()).value();
        assertEquals("/civ building confirmhome "+preview.consent().token()+" owned",click);
        confirm(c,UUID.randomUUID());assertEquals(0,confirms);
        command(c,"building","confirmhome",preview.consent().token().toString());assertEquals(0,confirms);
        confirm(c,preview.consent().token());assertEquals(1,confirms);c.run();assertTrue(messages.getLast().contains("no villagers teleport"));
        confirm(c,preview.consent().token());assertEquals(1,confirms);
    }
    @Test void changedSelectionWorldPermissionExpiryAndCancellationCannotConfirmOldHome() {
        var c=controls();begin(c);c.run();UUID old=preview.consent().token();selected=new Bounds(new Position(0,0,0),new Position(3,3,4));
        confirm(c,old);assertEquals(0,confirms);c.run();assertEquals(1,cancels);
        selected=new Bounds(new Position(0,0,0),new Position(4,3,4));begin(c);c.run();old=preview.consent().token();currentWorld=UUID.randomUUID();c.run();confirm(c,old);assertEquals(0,confirms);currentWorld=worldId;
        begin(c);c.run();allowed=false;c.run();assertEquals(0,confirms);allowed=true;
        begin(c);c.run();time+=60_000;c.run();assertTrue(messages.getLast().contains("expired"));assertEquals(0,confirms);
        begin(c);command(c,"building","cancelhome");c.run();assertEquals(0,confirms);
    }
    @Test void globalQueueAndReadBudgetStayBoundedAndReadOnlyInspectionSharesTheOtherHalf() {
        delayed=true;var c=controls();for(int i=0;i<5;i++)c.onCommand(player(new UUID(0,10+i)),null,"civ",new String[]{"building","home",village.toString()});
        assertTrue(messages.getLast().contains("busy"));c.run();assertEquals(128,reads);c.run();assertEquals(256,reads);assertEquals(0,previews);
        assertEquals(256,PaperHomeDonation.CELLS_PER_TICK+PaperDwellingReview.CELLS_PER_TICK);c.close();assertEquals(4,cancels);
    }
    @Test void submittedSaveDrainsAcrossDisconnectAndFailureNeedsExplicitSameTokenRetry() {
        var c=controls();begin(c);c.run();UUID token=preview.consent().token();saved=new CompletableFuture<>();confirm(c,token);
        command(c,"building","cancelhome");assertTrue(messages.getLast().contains("already finishing"));assertEquals(0,cancels);
        saved.completeExceptionally(new java.sql.SQLException("Fixture failure"));c.run();assertFalse(messages.stream().anyMatch(s->s.startsWith("Home donation permission saved")));
        saved=null;confirm(c,token);assertEquals(2,confirms);online=false;c.run();assertEquals(0,cancels);
        online=true;begin(c);c.run();saved=new CompletableFuture<>();confirm(c,preview.consent().token());c.close();assertEquals(0,cancels);
    }
}
