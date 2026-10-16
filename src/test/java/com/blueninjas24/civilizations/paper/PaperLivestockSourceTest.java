package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperLivestockSourceTest {
    UUID id=UUID.randomUUID();boolean loaded=true,entitiesLoaded=true,playerBreeding;int entityReads;
    World world=proxy(World.class,m->switch(m){case "getUID"->id;case "isChunkLoaded"->loaded;case "getMinHeight"->-64;case "getMaxHeight"->320;default->null;});
    @SuppressWarnings("unchecked") <T>T proxy(Class<T> type,Function<String,Object> answer){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{if(m.getName().equals("toString"))return "Fixture "+type.getSimpleName();if(m.getName().equals("hashCode"))return System.identityHashCode(p);if(m.getName().equals("equals"))return p==a[0];return answer.apply(m.getName());});}
    Chunk chunk(Entity... entities){return proxy(Chunk.class,m->switch(m){case "getWorld"->world;case "getX","getZ"->0;case "isLoaded"->loaded;case "isEntitiesLoaded"->entitiesLoaded;case "getEntities"->{entityReads++;yield entities;}default->null;});}
    Entity animal(UUID identity,EntityType kind,int age,boolean named,boolean leashed,int x) {
        Class<? extends Entity> type=kind==EntityType.CHICKEN?Chicken.class:Cow.class;
        var pdc=proxy(org.bukkit.persistence.PersistentDataContainer.class,m->{assertEquals("has",m);return playerBreeding;});
        return proxy(type,m->switch(m){case "getUniqueId"->identity;case "getWorld"->world;case "getType"->kind;case "getLocation"->new Location(world,x,64,1);case "getAge"->age;case "getCustomName"->named?"Protected fixture":null;case "isLeashed"->leashed;case "isValid"->true;case "isDead"->false;case "getPersistentDataContainer"->pdc;default->null;});
    }
    PaperLivestockSource source(){return new PaperLivestockSource(()->true);}
    @Test void capturesStableImmutableRawEvidenceWithoutChangingAnimals() {
        var a=new UUID(0,1);var b=new UUID(0,2);var frame=source().capture(chunk(animal(b,EntityType.COW,-400,true,true,1),animal(a,EntityType.CHICKEN,0,false,false,2)),id,1000,10,5);
        assertTrue(frame.complete());assertEquals(List.of(a,b),frame.animals().stream().map(LivestockObservation.Animal::id).toList());assertTrue(frame.animals().getFirst().adult());assertFalse(frame.animals().getLast().adult());assertTrue(frame.animals().getLast().named());assertTrue(frame.animals().getLast().leashed());assertEquals(-400,frame.animals().getLast().ageTicks());assertThrows(UnsupportedOperationException.class,()->frame.animals().clear());assertEquals(1,entityReads);
    }
    @Test void unloadedEntityChunksAreUnknownAndNeverReadOrLoaded() {
        loaded=false;assertFalse(source().capture(chunk(),id,1000,10,5).complete());assertEquals(0,entityReads);
        loaded=true;entitiesLoaded=false;assertFalse(source().capture(chunk(),id,1000,10,5).complete());assertEquals(0,entityReads);
    }
    @Test void immutablePlayerBreedingEvidenceProtectsAnOtherwiseEligibleFreeRangeAnimal() {
        playerBreeding=true;var observed=source().capture(chunk(animal(UUID.randomUUID(),EntityType.COW,0,false,false,1)),id,1000,10,5).animals().getFirst();playerBreeding=false;
        assertTrue(observed.recordedPlayerBreeding());
        assertEquals(LivestockAdoption.Decision.PROTECTED,LivestockAdoption.assess(new LivestockAdoption.Evidence(LivestockAdoption.Ownership.UNCLAIMED,LivestockAdoption.Habitat.FREE_RANGE,observed.named(),observed.leashed(),observed.recordedPlayerBreeding(),true,10000),1000));
    }
    @Test void capsAndCrossedPositionsReturnUnknownRatherThanPartialOrZeroEvidence() {
        var a=animal(UUID.randomUUID(),EntityType.COW,0,false,false,1);var b=animal(UUID.randomUUID(),EntityType.CHICKEN,0,false,false,2);
        assertFalse(source().capture(chunk(a,b),id,1000,1,1).complete());var excess=source().capture(chunk(a,b),id,1000,10,1);assertFalse(excess.complete());assertTrue(excess.animals().isEmpty());
        assertFalse(source().capture(chunk(animal(UUID.randomUUID(),EntityType.COW,0,false,false,17)),id,1000,10,5).complete());
    }
    @Test void threadAndWorldIdentityGuardsPrecedeEntityAccess() {
        assertThrows(IllegalStateException.class,()->new PaperLivestockSource(()->false).capture(chunk(),id,1000,10,5));assertEquals(0,entityReads);
        assertThrows(IllegalArgumentException.class,()->source().capture(chunk(),UUID.randomUUID(),1000,10,5));assertEquals(0,entityReads);
    }
    @Test void unsupportedSpeciesAndDuplicateIdentitiesNeverEstablishAnEligibleRoster() {
        assertTrue(source().capture(chunk(animal(UUID.randomUUID(),EntityType.MOOSHROOM,0,false,false,1)),id,1000,10,5).animals().isEmpty());
        var unsupported=proxy(Entity.class,m->m.equals("getType")?EntityType.COW:null);assertFalse(source().capture(chunk(unsupported),id,1000,10,5).complete());
        var a=animal(UUID.randomUUID(),EntityType.COW,0,false,false,1);var frame=source().capture(chunk(a,a),id,1000,10,5);assertFalse(frame.complete());assertTrue(frame.animals().isEmpty());
    }
}
