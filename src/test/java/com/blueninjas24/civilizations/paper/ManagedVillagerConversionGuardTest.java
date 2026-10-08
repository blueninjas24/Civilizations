package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.database.ResidentProjectionRepository.*;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.entity.EntityTransformEvent;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ManagedVillagerConversionGuardTest {
    private final UUID worldId = UUID.randomUUID(), entityId = UUID.randomUUID(), personId = UUID.randomUUID();
    private final World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getUID")) return worldId;
                throw new AssertionError("Unexpected world operation: " + method.getName());
            });
    private <T extends Entity> T entity(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getUniqueId" -> entityId;
            default -> throw new AssertionError("Unexpected entity operation: " + method.getName());
        }));
    }
    private EntityTransformEvent event(Entity original, EntityTransformEvent.TransformReason reason) {
        return new EntityTransformEvent(original, List.of(entity(ZombieVillager.class)), reason);
    }
    private Snapshot managed() { return new Snapshot(Map.of(), Map.of(new EntityKey(worldId, entityId), personId), Map.of()); }

    @Test void cancelsInfectionOfKnownManagedVillagerUsingOnlyImmutableIdentityEvidence() {
        var event = event(entity(Villager.class), EntityTransformEvent.TransformReason.INFECTION);
        new ManagedVillagerConversionGuard(this::managed).onTransform(event);
        assertTrue(event.isCancelled());
    }
    @Test void leavesUnmanagedVillagersAndDifferentWorldsUnchanged() {
        var event = event(entity(Villager.class), EntityTransformEvent.TransformReason.INFECTION);
        new ManagedVillagerConversionGuard(Snapshot::empty).onTransform(event); assertFalse(event.isCancelled());
        var elsewhere = new Snapshot(Map.of(), Map.of(new EntityKey(UUID.randomUUID(), entityId), personId), Map.of());
        new ManagedVillagerConversionGuard(() -> elsewhere).onTransform(event); assertFalse(event.isCancelled());
    }
    @Test void leavesCuringLightningOtherReasonsAndOtherEntityTypesUnchanged() {
        var guard = new ManagedVillagerConversionGuard(this::managed);
        for (var reason : EntityTransformEvent.TransformReason.values()) {
            if (reason == EntityTransformEvent.TransformReason.INFECTION) continue;
            var event = event(entity(Villager.class), reason); guard.onTransform(event); assertFalse(event.isCancelled(), reason.name());
        }
        var other = event(entity(ZombieVillager.class), EntityTransformEvent.TransformReason.INFECTION);
        guard.onTransform(other); assertFalse(other.isCancelled());
    }
    @Test void preservesEarlierCancellationAndSeesNewlyPublishedImportsWithoutSql() {
        var snapshot = new AtomicReference<>(Snapshot.empty());
        var guard = new ManagedVillagerConversionGuard(snapshot::get);
        var event = event(entity(Villager.class), EntityTransformEvent.TransformReason.INFECTION);
        guard.onTransform(event); assertFalse(event.isCancelled());
        snapshot.set(managed()); guard.onTransform(event); assertTrue(event.isCancelled());
        var unrelated = event(entity(Villager.class), EntityTransformEvent.TransformReason.LIGHTNING);
        unrelated.setCancelled(true); guard.onTransform(unrelated); assertTrue(unrelated.isCancelled());
    }
}
