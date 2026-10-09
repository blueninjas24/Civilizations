package com.blueninjas24.civilizations.paper;

import org.bukkit.World;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class WorldAvailabilityTest {
    private World world(UUID id) {
        return (World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(proxy,method,args)->{
            if(method.getName().equals("getUID"))return id;
            throw new AssertionError("Unexpected world/chunk operation: "+method.getName());
        });
    }
    @Test void loadAndUnloadPublishImmutableIdsIncludingWorldListTransitionOrder() {
        var first=UUID.randomUUID();var second=UUID.randomUUID();var a=world(first);var b=world(second);
        var published=new AtomicReference<Set<UUID>>();
        var availability=new WorldAvailability(()->List.of(a),published::set);
        assertEquals(Set.of(first),availability.snapshot());
        availability.loaded(new WorldLoadEvent(b));assertEquals(Set.of(first,second),published.get());
        var immutable=published.get();assertThrows(UnsupportedOperationException.class,()->immutable.clear());
        availability.unloaded(new WorldUnloadEvent(a));assertEquals(Set.of(),published.get());
        assertEquals(Set.of(first,second),immutable);
    }
    @Test void cancelledUnloadDoesNotPauseTheWorld() {
        var a=world(UUID.randomUUID());var published=new AtomicReference<Set<UUID>>();
        var availability=new WorldAvailability(()->List.of(a),published::set);
        var event=new WorldUnloadEvent(a);event.setCancelled(true);availability.unloaded(event);
        assertNull(published.get());
    }
}
