package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.BuildingLayout.Bounds;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.BuildingRegistry.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperBuildingProtectionTest {
    private UUID actor=new UUID(0,1),worldId=new UUID(0,2);private boolean allowed=true,admin=false,online=true;
    private long time=1000;private int particles;private final List<String> messages=new ArrayList<>();private final List<Component> buttons=new ArrayList<>();
    private final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->switch(m.getName()){
        case "getUID"->worldId;case "getName"->"Building fixture";default->throw new AssertionError(m.getName());});
    private Location location=new Location(world,0,64,0);
    private final Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->actor;case "getLocation"->location;case "getWorld"->world;
        case "hasPermission"->a[0].equals(PaperBuildingProtection.ADMIN)?admin:allowed;
        case "sendMessage"->{if(a[0] instanceof Component c){buttons.add(c);messages.add(PlainTextComponentSerializer.plainText().serialize(c));}else messages.add((String)a[0]);yield null;}
        case "spawnParticle"->{particles++;yield null;}
        default->throw new AssertionError("Unexpected player/world action: "+m.getName());});
    private class Owner implements PaperBuildingProtection.Owner {
        int protects,releases;Exclusion saved;CompletableFuture<Exclusion> outcome;CompletableFuture<Void> releaseOutcome;boolean ready=true;
        UUID lastRelease;long lastReleaseMillis;final List<Boolean> releaseAdminFlags=new ArrayList<>();
        public boolean ready(){return ready;}
        public CompletableFuture<Exclusion> protect(Exclusion e,String name){protects++;saved=e;assertEquals("Building fixture",name);return outcome==null?CompletableFuture.completedFuture(e):outcome;}
        public CompletableFuture<Optional<Exclusion>> find(UUID id){return CompletableFuture.completedFuture(Optional.ofNullable(saved));}
        public CompletableFuture<List<UUID>> at(UUID world,int x,int z){return CompletableFuture.completedFuture(saved==null?List.of():List.of(saved.id()));}
        public CompletableFuture<Void> release(UUID op,Exclusion e,UUID who,UUID current,boolean administrator,long now){releases++;lastRelease=op;lastReleaseMillis=now;releaseAdminFlags.add(administrator);assertTrue(administrator||e.actor().equals(who));assertEquals(e.world(),current);return releaseOutcome==null?CompletableFuture.completedFuture(null):releaseOutcome;}
    }
    private final Owner owner=new Owner();
    private PaperBuildingProtection controls(){return new PaperBuildingProtection(owner,BuildingProtectionSettings.prototype(),id->online&&id.equals(actor)?player:null,()->time,Logger.getLogger("BuildingTest"));}
    private void command(PaperBuildingProtection c,String...args){new CivilizationsCommand(null,null,null,null,null,null,null,c).onCommand(player,null,"civ",args);}
    private void select(PaperBuildingProtection c){location=new Location(world,0,64,0);command(c,"building","corner1");location=new Location(world,4,67,4);command(c,"building","corner2");}
    private void preview(PaperBuildingProtection c){select(c);command(c,"building","protect");}
    private UUID token(){return UUID.fromString(((net.kyori.adventure.text.event.ClickEvent.Payload.Text)buttons.getLast().clickEvent().payload()).value().split(" ")[3]);}
    private void confirm(PaperBuildingProtection c,UUID id){command(c,"building","confirm",id.toString(),"owned");c.run();}

    @Test void playerSelectionAndClickableOwnershipConfirmationSaveOnlyAfterExplicitMatchingToken() {
        var c=controls();preview(c);var id=token();assertEquals(0,owner.protects);assertTrue(particles>0&&particles<=68);
        assertTrue(messages.stream().anyMatch(s->s.contains("all heights")));
        confirm(c,UUID.randomUUID());assertEquals(0,owner.protects);
        command(c,"building","confirm",id.toString());assertEquals(0,owner.protects);
        confirm(c,id);assertEquals(1,owner.protects);assertEquals(actor,owner.saved.actor());
        assertEquals(new Bounds(new Position(0,64,0),new Position(4,67,4)),owner.saved.bounds());
        assertTrue(messages.getLast().startsWith("Building plot protected:"));confirm(c,id);assertEquals(1,owner.protects);
        command(c,"building","here");c.run();assertTrue(messages.getLast().contains(owner.saved.id().toString()));
    }
    @Test void changedSelectionExpiredPreviewWrongWorldOrLostPermissionCannotSave() {
        var c=controls();preview(c);var old=token();command(c,"building","corner1");confirm(c,old);assertEquals(0,owner.protects);
        preview(c);old=token();time+=60_000;confirm(c,old);assertEquals(0,owner.protects);
        preview(c);old=token();worldId=new UUID(0,99);confirm(c,old);assertEquals(0,owner.protects);
        worldId=new UUID(0,2);preview(c);old=token();allowed=false;confirm(c,old);assertEquals(0,owner.protects);
    }
    @Test void cancellationCloseAndPendingSaveDoNotLieAboutCommittedProtection() {
        var c=controls();preview(c);var id=token();command(c,"building","cancel");confirm(c,id);assertEquals(0,owner.protects);
        owner.outcome=new CompletableFuture<>();preview(c);id=token();command(c,"building","confirm",id.toString(),"owned");
        command(c,"building","cancel");assertTrue(messages.getLast().contains("already finishing"));
        command(c,"building","corner1");assertEquals(1,owner.protects);owner.outcome.complete(owner.saved);c.run();
        assertTrue(messages.getLast().startsWith("Building plot protected:"));c.close();preview(c);assertEquals(1,owner.protects);
    }
    @Test void failedSaveIsNotAnnouncedAndExplicitRetryKeepsTheSameIdentity() {
        owner.outcome=CompletableFuture.failedFuture(new Declined("Fixture conflict"));var c=controls();preview(c);var id=token();confirm(c,id);
        assertEquals(1,owner.protects);assertFalse(messages.stream().anyMatch(s->s.startsWith("Building plot protected:")));
        assertTrue(messages.getLast().contains("Fixture conflict"));owner.outcome=null;confirm(c,id);
        assertEquals(2,owner.protects);assertEquals(id,owner.saved.id());assertTrue(messages.getLast().startsWith("Building plot protected:"));
    }
    @Test void releaseRequiresCurrentRecordedOwnerOrAdminAndSeparateExactConfirmation() {
        var c=controls();preview(c);confirm(c,token());
        command(c,"building","release",owner.saved.id().toString());c.run();var releaseToken=UUID.fromString(((net.kyori.adventure.text.event.ClickEvent.Payload.Text)buttons.getLast().clickEvent().payload()).value().split(" ")[3]);
        assertEquals(0,owner.releases);command(c,"building","confirmrelease",releaseToken.toString());c.run();assertEquals(1,owner.releases);
        actor=new UUID(0,99);command(c,"building","release",owner.saved.id().toString());c.run();assertTrue(messages.getLast().contains("Only its recorded owner"));
        admin=true;command(c,"building","release",owner.saved.id().toString());c.run();releaseToken=UUID.fromString(((net.kyori.adventure.text.event.ClickEvent.Payload.Text)buttons.getLast().clickEvent().payload()).value().split(" ")[3]);
        admin=false;command(c,"building","confirmrelease",releaseToken.toString());assertEquals(1,owner.releases);
    }
    @Test void geometryAndEditableSettingsAreBoundedWithoutWorldBlockReads()throws Exception {
        var s=BuildingProtectionSettings.prototype();
        assertEquals(new Bounds(new Position(-1,0,-1),new Position(2,4,2)),PaperBuildingProtection.selectedBounds(new Position(2,4,2),new Position(-1,0,-1),s));
        assertThrows(IllegalArgumentException.class,()->PaperBuildingProtection.selectedBounds(new Position(0,0,0),new Position(128,0,1),s));
        assertThrows(IllegalArgumentException.class,()->PaperBuildingProtection.selectedBounds(new Position(0,0,0),new Position(64,0,64),s));
        var config=new YamlConfiguration();assertEquals(s,BuildingProtectionSettings.read(config));
        config.loadFromString("building-protection:\n  maximum-area: 1024\n  enabled: false\n");
        assertFalse(BuildingProtectionSettings.read(config).enabled());assertEquals(1024,BuildingProtectionSettings.read(config).maximumArea());
        config.set("building-protection.maximum-area",1.5);assertThrows(IllegalArgumentException.class,()->BuildingProtectionSettings.read(config));
        config.set("building-protection.maximum-area",1024);config.set("building-protection.typo",1);assertThrows(IllegalArgumentException.class,()->BuildingProtectionSettings.read(config));
    }
    @Test void failedReleaseRetryFreezesOriginalTimeAndAuthorityWhileStillCheckingCurrentPermission() {
        var c=controls();preview(c);confirm(c,token());command(c,"building","release",owner.saved.id().toString());c.run();
        UUID releaseToken=UUID.fromString(((net.kyori.adventure.text.event.ClickEvent.Payload.Text)buttons.getLast().clickEvent().payload()).value().split(" ")[3]);
        owner.releaseOutcome=CompletableFuture.failedFuture(new java.sql.SQLException("Fixture failure"));
        command(c,"building","confirmrelease",releaseToken.toString());c.run();long original=owner.lastReleaseMillis;
        assertEquals(1,owner.releases);time+=1000;admin=true;owner.releaseOutcome=null;
        command(c,"building","confirmrelease",releaseToken.toString());c.run();
        assertEquals(2,owner.releases);assertEquals(original,owner.lastReleaseMillis);assertEquals(releaseToken,owner.lastRelease);
        assertEquals(List.of(false,false),owner.releaseAdminFlags);assertTrue(messages.getLast().startsWith("Building exclusion released."));
    }
}
