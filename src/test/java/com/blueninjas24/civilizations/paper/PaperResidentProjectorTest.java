package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.population.Resident;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Villager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class PaperResidentProjectorTest {
    @Test void savedAgeAndNameApplyWithoutTouchingTradesOrGrantingDamageImmunity() {
        var adult = new AtomicBoolean(true); var locked = new AtomicBoolean(); var name = new AtomicReference<Component>();
        var villager = (Villager) Proxy.newProxyInstance(Villager.class.getClassLoader(), new Class<?>[]{Villager.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "customName" -> { name.set((Component) args[0]); yield null; }
                    case "setAgeLock" -> { locked.set((boolean) args[0]); yield null; }
                    case "isAdult" -> adult.get();
                    case "setAdult" -> { adult.set(true); yield null; }
                    case "setBaby" -> { adult.set(false); yield null; }
                    default -> throw new AssertionError("Unexpected mutation: " + method.getName());
                });
        var person = new Resident(UUID.randomUUID(), "Alice", UUID.randomUUID(), null, 10, Resident.Status.ALIVE);
        PaperResidentProjector.apply(villager, person, 216);
        assertEquals(Component.text("Alice"), name.get()); assertTrue(locked.get()); assertFalse(adult.get());
        PaperResidentProjector.apply(villager, new Resident(person.id(), person.name(), person.settlementId(), null, 216, person.status()), 216);
        assertTrue(adult.get());
    }
    @Test void unloadedWorldAndEntityDataNeverTriggerLoadsOrEntityEnumeration() {
        var chunkQueries = new AtomicInteger();
        var chunk = (Chunk) Proxy.newProxyInstance(Chunk.class.getClassLoader(), new Class<?>[]{Chunk.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isEntitiesLoaded")) return false;
                    throw new AssertionError("Unexpected chunk access: " + method.getName());
                });
        var loaded = new AtomicBoolean(false);
        var world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isChunkLoaded" -> { assertEquals(-1, args[0]); assertEquals(-1, args[1]); yield loaded.get(); }
                    case "getChunkAt" -> { chunkQueries.incrementAndGet(); yield chunk; }
                    default -> throw new AssertionError("Unexpected world access: " + method.getName());
                });
        assertFalse(PaperResidentProjector.loaded(world, -1, -1)); assertEquals(0, chunkQueries.get());
        loaded.set(true); assertFalse(PaperResidentProjector.loaded(world, -1, -1)); assertEquals(1, chunkQueries.get());
    }
    @Test void projectionSettingsDefaultForOlderConfigsAndRejectPartialOrUnsafeValues() throws Exception {
        var config = new YamlConfiguration();
        assertEquals(PaperResidentProjector.Settings.prototype(), PopulationConfiguration.readProjection(config));
        config.loadFromString("projection:\n  nearby-radius: 64\n");
        assertThrows(IllegalArgumentException.class, () -> PopulationConfiguration.readProjection(config));
        config.loadFromString("projection:\n  nearby-radius: 64\n  maximum-per-world: 32\n  maximum-at-night: 8\n  chunks-per-tick: 2\n  residents-per-tick: 16\n");
        assertEquals(PaperResidentProjector.Settings.prototype(), PopulationConfiguration.readProjection(config));
        config.set("projection.chunks-per-tick", 200);
        assertThrows(IllegalArgumentException.class, () -> PopulationConfiguration.readProjection(config));
        config.set("projection.chunks-per-tick", 2.5);
        assertThrows(IllegalArgumentException.class, () -> PopulationConfiguration.readProjection(config));
    }
    @Test void nightDefersAdditionalMaterializationWithConfigurableBudgetAndDayWrap() {
        var settings = PaperResidentProjector.Settings.prototype();
        assertEquals(32, PaperResidentProjector.spawnLimit(settings, 6000));
        assertEquals(8, PaperResidentProjector.spawnLimit(settings, 14000));
        assertEquals(32, PaperResidentProjector.spawnLimit(settings, 23000));
        assertEquals(8, PaperResidentProjector.spawnLimit(settings, 38000));
        assertThrows(IllegalArgumentException.class, () -> new PaperResidentProjector.Settings(64, 8, 9, 2, 16));
    }
}
