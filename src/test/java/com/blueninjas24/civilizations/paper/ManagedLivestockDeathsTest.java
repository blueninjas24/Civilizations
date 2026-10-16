package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.database.LivestockLosses;
import org.bukkit.*;
import org.bukkit.damage.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManagedLivestockDeathsTest {
    UUID animal=new UUID(0,1),world=new UUID(0,2);AtomicLong now=new AtomicLong(1000);
    static class Owner implements ManagedLivestockDeaths.Owner {
        Set<UUID> ids=Set.of();boolean ready=true;List<LivestockLosses.Observation> submitted=new ArrayList<>(),flushed;
        List<CompletableFuture<Optional<LivestockLosses.Receipt>>> futures=new ArrayList<>();
        public Set<UUID> villageAnimals(){return ids;}public boolean ready(){return ready;}
        public CompletableFuture<Optional<LivestockLosses.Receipt>> record(LivestockLosses.Observation o){submitted.add(o);var result=new CompletableFuture<Optional<LivestockLosses.Receipt>>();futures.add(result);return result;}
        public CompletableFuture<Void> flush(List<LivestockLosses.Observation> o){flushed=o;return CompletableFuture.completedFuture(null);}
    }
    <T>T proxy(Class<T> type,java.util.function.Function<String,Object> answer){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->answer.apply(m.getName())));}
    EntityDeathEvent event(EntityType type) {
        var observedWorld=proxy(World.class,m->{assertEquals("getUID",m);return world;});
        var entity=proxy(LivingEntity.class,m->switch(m){case "getType"->type;case "getUniqueId"->animal;case "getWorld"->observedWorld;default->throw new AssertionError("Unexpected physical mutation: "+m);});
        var damage=proxy(DamageType.class,m->{assertEquals("getKey",m);return new NamespacedKey("minecraft","player_attack");});
        var source=proxy(DamageSource.class,m->{assertEquals("getDamageType",m);return damage;});
        return new EntityDeathEvent(entity,source,new ArrayList<>(),3);
    }
    ManagedLivestockDeaths listener(Owner owner){return new ManagedLivestockDeaths(owner,Logger.getLogger("Livestock death fixture"),now::get);}
    @Test void confirmedKnownDeathUsesImmutableOwnerQueueWithoutChangingDeathLootOrExperience() {
        var owner=new Owner();owner.ids=Set.of(animal);var listener=listener(owner);var e=event(EntityType.COW);var drop=new ItemStack(){@Override public int getAmount(){return 2;}};e.getDrops().add(drop);
        listener.died(e);assertEquals(1,owner.submitted.size());var first=owner.submitted.getFirst();assertEquals(animal,first.animal());assertEquals(world,first.observedWorld());assertEquals("minecraft:player_attack",first.reason());assertFalse(e.isCancelled());assertEquals(List.of(drop),e.getDrops());assertEquals(3,e.getDroppedExp());
        now.set(1001);listener.died(e);assertEquals(1,owner.submitted.size());owner.futures.getFirst().complete(Optional.empty());listener.run();assertTrue(listener.diagnostic().contains("pending: 0"));
    }
    @Test void failedOwnerWriteRetriesSameEvidenceAfterDelayWithoutDroppingLoss() {
        var owner=new Owner();owner.ids=Set.of(animal);var listener=listener(owner);listener.died(event(EntityType.CHICKEN));var first=owner.submitted.getFirst();owner.futures.getFirst().completeExceptionally(new java.sql.SQLException("Fixture failure"));
        listener.run();assertEquals(1,owner.submitted.size());now.set(1999);listener.run();assertEquals(1,owner.submitted.size());now.set(2000);listener.run();assertEquals(2,owner.submitted.size());assertSame(first,owner.submitted.getLast());
        owner.futures.getLast().complete(Optional.empty());listener.run();assertTrue(listener.diagnostic().contains("pending: 0"));
    }
    @Test void recentUnpublishedIdentityCanCatchUpButReadinessAndUnknownDeathsDoNotCreateOwnership() {
        var owner=new Owner();owner.ready=false;var listener=listener(owner);listener.died(event(EntityType.COW));assertTrue(owner.submitted.isEmpty());owner.ids=Set.of(animal);listener.run();assertTrue(owner.submitted.isEmpty());owner.ready=true;listener.run();assertEquals(1,owner.submitted.size());
        var unknown=new Owner();var other=listener(unknown);other.died(event(EntityType.COW));now.set(32000);other.run();assertTrue(unknown.submitted.isEmpty());assertTrue(other.diagnostic().contains("recent unbound: 0"));
    }
    @Test void cancelledAndUnsupportedDeathsAreIgnoredAndGracefulFlushRetainsPendingObservations() {
        var owner=new Owner();owner.ids=Set.of(animal);owner.ready=false;var listener=listener(owner);var cancelled=event(EntityType.COW);cancelled.setCancelled(true);listener.died(cancelled);listener.died(event(EntityType.PIG));listener.flush();assertNull(owner.flushed);
        listener.died(event(EntityType.COW));listener.flush();assertEquals(1,owner.flushed.size());assertEquals(animal,owner.flushed.getFirst().animal());assertThrows(UnsupportedOperationException.class,()->owner.flushed.clear());assertTrue(cancelled.isCancelled());assertEquals(3,cancelled.getDroppedExp());
    }
}
