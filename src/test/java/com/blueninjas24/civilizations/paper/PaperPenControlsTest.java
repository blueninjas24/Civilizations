package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.database.PenProtectionLifecycle;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperPenControlsTest {
    UUID actor=new UUID(0,1),worldId=new UUID(0,2);boolean admin=true,releasePermission=true,online=true;
    long time=1000;int captured,advanced;final List<String> messages=new ArrayList<>();
    final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->switch(m.getName()){
        case "getUID"->worldId;case "getName"->"Pen test world";default->throw new AssertionError(m.getName());});
    Location location=new Location(world,0,64,0);
    final Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->actor;case "getLocation"->location;case "getWorld"->world;
        case "hasPermission"->a[0].equals(PaperPenControls.ADMIN)?admin:releasePermission;
        case "sendMessage"->{messages.add((String)a[0]);yield null;}
        case "spawnParticle"->null;
        default->throw new AssertionError("Unexpected player action: "+m.getName());});
    PenScan.Evidence geometry(){var s=new PenScan(p->Math.abs(p.x())==2||Math.abs(p.z())==2?PenScan.Cell.BARRIER:PenScan.Cell.INTERIOR,new PenScan.Column(0,0),new PenScan.Limits(4,16));while(!s.complete())s.advance(16);return s.evidence();}
    PaperPenControls.Frame frame=new PaperPenControls.Frame(geometry(),Set.of(new UUID(0,3)));
    class Owner implements PaperPenControls.Owner {
        PenProtection saved;int protects,releases;CompletableFuture<PenProtection> result;
        public boolean ready(){return true;}
        public CompletableFuture<PenProtection> protect(PenProtection pen,String name){protects++;saved=pen;assertEquals("Pen test world",name);return result==null?CompletableFuture.completedFuture(pen):result;}
        public CompletableFuture<Optional<PenProtection>> find(UUID id){return CompletableFuture.completedFuture(Optional.ofNullable(saved));}
        public CompletableFuture<List<UUID>> at(UUID world,int y,int x,int z){return CompletableFuture.completedFuture(saved==null?List.of():List.of(saved.id()));}
        public CompletableFuture<PenProtectionLifecycle.Release> release(PenProtectionLifecycle.Release request,boolean allowed){releases++;assertTrue(allowed||saved.owner().equals(actor));return CompletableFuture.completedFuture(request);}
    }
    final Owner owner=new Owner();
    PaperPenControls controls(){return controls(PenProtectionSettings.prototype());}
    PaperPenControls controls(PenProtectionSettings settings){return new PaperPenControls(owner,settings,id->online&&id.equals(actor)?player:null,(p,s)->{
        captured++;var copy=frame;return new PaperPenControls.Capture(){boolean complete;public void advance(){advanced++;complete=true;}public boolean complete(){return complete;}public PaperPenControls.Frame frame(){return copy;}};
    },()->time,Logger.getLogger("PaperPenControlsTest"));}
    void command(PaperPenControls controls,String... args){new CivilizationsCommand(null,null,null,null,null,controls).onCommand(player,null,"civ",args);}
    void preview(PaperPenControls controls){controls.preview(player,3,64,0);controls.run();}
    void confirm(PaperPenControls controls){command(controls,"pen","confirm");controls.run();controls.run();}

    @Test void previewAndFreshConfirmationProduceOneSaveOnlyAfterExplicitConsent(){
        var c=controls();preview(c);assertEquals(0,owner.protects);assertTrue(messages.stream().anyMatch(s->s.contains("9 floor columns; 1 cows/chickens")));
        confirm(c);assertEquals(2,captured);assertEquals(1,owner.protects);assertEquals(actor,owner.saved.owner());assertEquals(frame.animals(),owner.saved.animals());
        assertTrue(messages.stream().anyMatch(s->s.startsWith("Pen protected: ")));confirm(c);assertEquals(1,owner.protects);
        command(c,"pen","here");c.run();assertTrue(messages.getLast().contains(owner.saved.id().toString()));
    }
    @Test void unknownAuthorityCannotScanAndRevokedAuthorityCannotFinish(){
        var c=controls();admin=false;c.preview(player,3,64,0);assertEquals(0,captured);
        admin=true;preview(c);command(c,"pen","confirm");admin=false;c.run();assertEquals(0,owner.protects);
    }
    @Test void changedRosterOrGeometryRequiresAnotherPreview(){
        var c=controls();preview(c);frame=new PaperPenControls.Frame(geometry(),Set.of());confirm(c);assertEquals(0,owner.protects);
        frame=new PaperPenControls.Frame(geometry(),Set.of());preview(c);
        frame=new PaperPenControls.Frame(new PenScan.Evidence(PenScan.State.UNKNOWN,Set.of(),Set.of()),Set.of());confirm(c);assertEquals(0,owner.protects);
    }
    @Test void expiryMovingOrDisconnectingCannotSubmitAClaim(){
        var c=controls();preview(c);time+=60_000;confirm(c);assertEquals(0,owner.protects);
        preview(c);location=new Location(world,0,65,0);confirm(c);assertEquals(0,owner.protects);
        location=new Location(world,0,64,0);preview(c);command(c,"pen","confirm");online=false;c.run();assertEquals(0,owner.protects);
    }
    @Test void failedSaveDoesNotSilentlyRetryOrAnnounceProtection(){
        owner.result=CompletableFuture.failedFuture(new PenProtectionLifecycle.Declined("Another owner"));var c=controls();preview(c);confirm(c);c.run();
        assertEquals(1,owner.protects);assertTrue(messages.stream().anyMatch(s->s.contains("not confirmed: Another owner")));assertFalse(messages.stream().anyMatch(s->s.startsWith("Pen protected:")));
    }
    @Test void releaseRequiresPreviewConfirmationAndCurrentOwnerOrAdmin(){
        var c=controls();preview(c);confirm(c);admin=false;
        command(c,"pen","release",owner.saved.id().toString());c.run();assertEquals(0,owner.releases);command(c,"pen","confirmrelease");c.run();assertEquals(1,owner.releases);
        actor=new UUID(0,99);command(c,"pen","release",owner.saved.id().toString());c.run();command(c,"pen","confirmrelease");c.run();assertEquals(1,owner.releases);
        admin=true;command(c,"pen","release",owner.saved.id().toString());c.run();releasePermission=false;admin=false;command(c,"pen","confirmrelease");assertEquals(1,owner.releases);
    }
    @Test void cancelAndCloseRemoveUnsubmittedWorkButCannotUndoSubmittedSave(){
        var c=controls();c.preview(player,3,64,0);command(c,"pen","cancel");c.run();assertEquals(0,advanced);
        owner.result=new CompletableFuture<>();preview(c);command(c,"pen","confirm");c.run();assertEquals(1,owner.protects);
        command(c,"pen","cancel");assertTrue(messages.getLast().contains("already finishing"));owner.result.complete(owner.saved);c.run();
        assertTrue(messages.getLast().startsWith("Pen protected:"));c.preview(player,3,64,0);c.close();c.run();assertEquals(1,owner.protects);
    }
    @Test void settingsAreEditableAndRejectTruncatedOrUnboundedValues()throws Exception{
        var yaml=new YamlConfiguration();assertEquals(PenProtectionSettings.prototype(),PenProtectionSettings.read(yaml));
        yaml.loadFromString("pen-protection:\n  radius: 16\n  enabled: false\n");assertFalse(PenProtectionSettings.read(yaml).enabled());assertEquals(16,PenProtectionSettings.read(yaml).radius());
        for(Object bad:List.of(1.5,"32",0,65,Long.MAX_VALUE)){yaml.set("pen-protection.radius",bad);assertThrows(IllegalArgumentException.class,()->PenProtectionSettings.read(yaml));}
        yaml.set("pen-protection.radius",32);yaml.set("pen-protection.enabled","yes");assertThrows(IllegalArgumentException.class,()->PenProtectionSettings.read(yaml));
    }
}
