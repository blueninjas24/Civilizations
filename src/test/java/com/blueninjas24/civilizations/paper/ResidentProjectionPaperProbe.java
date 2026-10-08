package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.State;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Real-Paper integration probe. Test classes only; never packaged into Civilizations. */
public final class ResidentProjectionPaperProbe extends JavaPlugin implements Listener {
    private final AtomicLong now = new AtomicLong(1000);
    private World world;
    private PopulationSession session;
    private PopulationSession.Settings settings;
    private File scenario;
    private PaperResidentProjector projector;
    private ManagedVillagerLifecycle lifecycle;
    private ManagedVillagerConversionGuard conversion;
    private CompletableFuture<?> future;
    private List<Villager> originals;
    private UUID firstChild, secondChild, pendingId, originalPerson, sourceTown, destinationTown;
    private Map<UUID, UUID> savedEntities;
    private List<FamilyTie> savedFamilies;
    private int stage, infections, ticks;
    private int blockedUntil;
    private long deadline;
    private final Set<Long> tickets = new HashSet<>();

    @Override public void onEnable() {
        try {
            var root = getDataFolder().toPath().toAbsolutePath().normalize().getParent().getParent();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim()
                    .equals("Disposable Civilizations Phase 1 test environment"), "Not a marked disposable server");
            require(Bukkit.getOnlinePlayers().isEmpty(), "Players present; probe must not run");
            var plugin = Bukkit.getPluginManager().getPlugin("Civilizations");
            // Isolate fixture enrollment from the normal observer. Production code itself is exercised below.
            for (String field : List.of("observer", "projector")) {
                var member = plugin.getClass().getDeclaredField(field); member.setAccessible(true);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    try { if (member.get(plugin) instanceof BukkitRunnable task) task.cancel(); }
                    catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
                }, 30);
            }
            Bukkit.getPluginManager().registerEvents(this, this);
            world = Bukkit.getWorld("civilizations-phase1-test"); require(world != null, "Wrong test world");
            settings = settings(1, 0); scenario = new File(getDataFolder(), "births-" + UUID.randomUUID()); open();
            deadline = System.currentTimeMillis() + 90_000;
            new BukkitRunnable() {
                @Override public void run() {
                    if (++ticks < 40) return;
                    try { require(System.currentTimeMillis() < deadline, "Timed out at stage " + stage + " " + (projector == null ? "" : projector.diagnostic())); step(); }
                    catch (Throwable failure) { getLogger().log(java.util.logging.Level.SEVERE, "POPULATION_PROBE_FAILED stage " + stage, failure); cleanup(); cancel(); }
                    if (stage == 99) { cleanup(); cancel(); getLogger().info("POPULATION_PROBE_PASSED: births, age, names, restart, pending claim, native breeding/infection/death, saved migration, simulated deaths and enrollment/death race"); }
                }
            }.runTaskTimer(this, 1, 1);
        } catch (Exception failure) { throw new IllegalStateException("Probe safety checks failed", failure); }
    }
    private PopulationSession.Settings settings(double birth, double migration) {
        var prototype = PopulationSession.Settings.prototype();
        return new PopulationSession.Settings(42, 30, 0, 2, new PopulationPolicy(12, 216, 240, 480, 720, birth, 0, 0),
                new MigrationPolicy(migration, 0), prototype.recognition(), prototype.enrollment());
    }
    private void open() { session = new PopulationSession(scenario, getLogger(), settings, now::get); future = session.start(false); }
    private boolean ready() { if (!future.isDone()) return false; future.join(); return true; }
    private void adapters() {
        conversion = new ManagedVillagerConversionGuard(() -> session.projectionView().orElseThrow().identities());
        Bukkit.getPluginManager().registerEvents(conversion, this);
        lifecycle = new ManagedVillagerLifecycle(session, getLogger()); Bukkit.getPluginManager().registerEvents(lifecycle, this); lifecycle.runTaskTimer(this, 1, 1);
        projector = new PaperResidentProjector(this, session, PaperResidentProjector.Settings.prototype(), location -> true);
        projector.runTaskTimer(this, 1, 1);
    }
    private void pause() {
        if (projector != null) { try { projector.cancel(); } catch (IllegalStateException ignored) {} projector = null; }
        if (lifecycle != null) { lifecycle.flush(); lifecycle.cancel(); HandlerList.unregisterAll(lifecycle); lifecycle = null; }
        if (conversion != null) { HandlerList.unregisterAll(conversion); conversion = null; }
        if (session != null) { session.close(); session = null; }
    }
    private void cleanup() { pause(); if (world != null) for (long key : tickets) world.removePluginChunkTicket((int)(key >> 32), (int)key, this); tickets.clear(); }
    @Override public void onDisable() { cleanup(); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void infection(EntityTransformEvent event) {
        if (event.getTransformReason() == EntityTransformEvent.TransformReason.INFECTION
                && originals != null && originals.contains(event.getEntity())) { require(event.isCancelled(), "Managed infection not cancelled"); infections++; }
    }
    private List<Villager> fixture(int x, int beds, int people) {
        int z = 512;
        for (int xx = x - 3; xx <= x + (beds > 2 ? 32 : beds * 2) + 3; xx++) for (int zz = z - 3; zz <= z + 3; zz++) {
            int cx = Math.floorDiv(xx, 16), cz = Math.floorDiv(zz, 16);
            long key = ((long)cx << 32) | Integer.toUnsignedLong(cz);
            if (tickets.add(key)) world.addPluginChunkTicket(cx, cz, this); // Explicit test fixture loading, never production adapter loading.
            world.getBlockAt(xx, 63, zz).setType(Material.STONE, false);
            world.getBlockAt(xx, 64, zz).setType(Material.AIR, false); world.getBlockAt(xx, 65, zz).setType(Material.AIR, false);
        }
        for (int i = 0; i < beds; i++) {
            for (var part : Bed.Part.values()) {
                var data = (Bed) Bukkit.createBlockData(Material.RED_BED); data.setFacing(BlockFace.SOUTH); data.setPart(part);
                world.getBlockAt(bedX(x, beds, i), 64, z + (part == Bed.Part.HEAD ? 1 : 0)).setBlockData(data, false);
            }
        }
        var villagers = new ArrayList<Villager>();
        for (int i = 0; i < people; i++) villagers.add(world.spawn(new Location(world, x + 0.5, 64, z - 1.5), Villager.class,
                villager -> { villager.setAI(false); villager.setInvulnerable(true); villager.setPersistent(true); villager.setAdult(); }));
        return villagers;
    }
    private VillageObservation observe(int x, int beds, List<Villager> villagers) {
        var positions = new ArrayList<VillageObservation.Position>();
        for (int i = 0; i < beds; i++) positions.add(new VillageObservation.Position(bedX(x, beds, i), 64, 513));
        return new VillageObservation(world.getUID(), world.getName(), now.get(), true, positions, villagers.stream().map(villager ->
                new VillageObservation.Person(villager.getUniqueId(), new VillageObservation.Position(villager.getLocation().getBlockX(), 64, villager.getLocation().getBlockZ()), true, null)).toList());
    }
    private static int bedX(int x, int beds, int index) {
        // Destination has an occupied home and a separate vacant home, within one settlement.
        return x + (beds > 2 && index >= 2 ? 20 + (index - 2) * 2 : index * 2);
    }
    private Villager visible(UUID person) {
        var projection = session.projectionView().orElseThrow().identities().projections().get(person);
        if (projection == null || projection.state() != State.ACTIVE) return null;
        return world.getEntity(projection.entityId()) instanceof Villager villager ? villager : null;
    }
    private List<Resident> people() { return session.snapshot().orElseThrow().residents(); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private void step() {
        switch (stage) {
            case 0 -> {
                if (!ready()) return;
                originals = fixture(512, 2, 2); future = session.observe(observe(512, 2, originals)); stage++;
            }
            case 1 -> {
                if (!ready()) return; adapters();
                originalPerson = session.projectionView().orElseThrow().identities().aliases()
                        .get(new com.blueninjas24.civilizations.database.ResidentProjectionRepository.EntityKey(world.getUID(), originals.getFirst().getUniqueId()));
                for (int x = 510; x <= 514; x++) for (int z = 511; z <= 515; z++) world.getBlockAt(x, 63, z).setType(Material.CAMPFIRE, false);
                now.addAndGet(30); future = session.pulse(); stage++;
            }
            case 2 -> {
                if (!ready()) return;
                require(people().size() == 3, "Birth did not save third person");
                firstChild = people().stream().filter(person -> person.ageSteps() == 0).findFirst().orElseThrow().id(); blockedUntil = ticks + 20; stage++;
            }
            case 3 -> {
                if (ticks < blockedUntil) { require(visible(firstChild) == null, "Baby appeared on hazardous campfire floor"); return; }
                if (blockedUntil > 0) {
                    for (int x = 510; x <= 514; x++) for (int z = 511; z <= 515; z++) world.getBlockAt(x, 63, z).setType(Material.STONE, false);
                    blockedUntil = 0;
                }
                var child = visible(firstChild); if (child == null || child.isInvulnerable()) return;
                require(!child.isAdult() && child.getAgeLock(), "Saved baby age not projected");
                var resident = people().stream().filter(person -> person.id().equals(firstChild)).findFirst().orElseThrow();
                require(Component.text(resident.name()).equals(child.customName()), "Saved baby name not projected");
                require(child.hasAI() && child.hasGravity(), "Committed baby remained frozen"); child.setAI(false);
                savedEntities = new HashMap<>(); session.projectionView().orElseThrow().identities().projections().forEach((id, projection) -> savedEntities.put(id, projection.entityId()));
                savedFamilies = session.snapshot().orElseThrow().familyTies();
                // Simulate shutdown after the ACTIVE DB commit, before main-thread unfreezing.
                child.getPersistentDataContainer().set(new NamespacedKey("civilizations", "projection_pending"), PersistentDataType.BYTE, (byte)1);
                child.setAI(false); child.setGravity(false); child.setInvulnerable(true);
                pause(); open(); stage++;
            }
            case 4 -> {
                if (!ready()) return;
                require(people().size() == 3 && savedFamilies.equals(session.snapshot().orElseThrow().familyTies()), "Restart lost identities/families");
                savedEntities.forEach((id, entity) -> require(entity.equals(session.projectionView().orElseThrow().identities().projections().get(id).entityId()), "Restart duplicated incarnation"));
                if (projector == null) adapters();
                var child = visible(firstChild); if (child == null || child.isInvulnerable() || !child.hasGravity() || !child.hasAI()) return;
                child.setAI(false); projector.cancel(); projector.close(); projector = null;
                now.addAndGet(30); future = session.pulse(); stage++;
            }
            case 5 -> {
                if (!ready()) return;
                secondChild = people().stream().filter(person -> person.ageSteps() == 0).findFirst().orElseThrow().id();
                var unspawned = world.createEntity(new Location(world, 511.5, 64, 512.5), Villager.class); pendingId = unspawned.getUniqueId();
                var person = people().stream().filter(p -> p.id().equals(secondChild)).findFirst().orElseThrow();
                future = session.claimProjection(person.id(), world.getUID(), pendingId, person.householdId(), new VillageObservation.Position(511, 64, 512)); stage++;
            }
            case 6 -> {
                if (!ready()) return; require(Boolean.TRUE.equals(future.join()), "Pending fixture claim rejected"); pause(); open(); stage++;
            }
            case 7 -> {
                if (!ready()) return; adapters(); stage++;
            }
            case 8 -> {
                var child = visible(secondChild); if (child == null || child.isInvulnerable()) return;
                child.setAI(false); require(!pendingId.equals(child.getUniqueId()) && world.getEntity(pendingId) == null, "Pending recovery reused or duplicated entity");
                require(people().size() == 4, "Pending recovery changed saved headcount");
                var baby = world.createEntity(new Location(world, 512.5, 64, 512.5), Villager.class);
                var breed = new EntityBreedEvent(baby, originals.getFirst(), originals.get(1), null, null, 0);
                Bukkit.getPluginManager().callEvent(breed); require(breed.isCancelled(), "Managed vanilla breeding bypassed saved birth rules");
                var unmanaged = world.createEntity(new Location(world, 512.5, 64, 512.5), Villager.class);
                var normalBreed = new EntityBreedEvent(baby, unmanaged, unmanaged, null, null, 0);
                Bukkit.getPluginManager().callEvent(normalBreed); require(!normalBreed.isCancelled(), "Unmanaged breeding was cancelled");
                world.setDifficulty(Difficulty.HARD);
                var zombie = world.spawn(new Location(world, 512.5, 64, 512.5), Zombie.class, entity -> { entity.setAI(false); entity.setInvulnerable(true); });
                var original = originals.getFirst(); original.setInvulnerable(false); original.setHealth(1); original.damage(1000, zombie); zombie.remove(); stage++;
            }
            case 9 -> {
                if (infections == 0) return;
                require(world.getNearbyEntities(new Location(world, 512.5, 64, 512.5), 12, 8, 12).stream().noneMatch(entity -> entity instanceof ZombieVillager), "Zombie villager was spawned");
                var person = people().stream().filter(p -> p.id().equals(originalPerson)).findFirst().orElseThrow();
                if (person.status() != Resident.Status.DECEASED) return;
                require(session.projectionView().orElseThrow().recentDeaths().size() == 1, "Actual native death missing or duplicated");
                now.addAndGet(216 * 30); future = session.pulse(); stage++;
            }
            case 10 -> {
                if (!ready()) return;
                if (session.status().remainingMonths() > 0) { future = session.pulse(); return; }
                var child = visible(firstChild); if (child == null || !child.isAdult()) return;
                require(child.getAgeLock(), "Saved adulthood did not retain age ownership");
                pause(); settings = settings(0, 1); scenario = new File(getDataFolder(), "migration-" + UUID.randomUUID()); now.set(1000); open(); stage++;
            }
            case 11 -> {
                if (!ready()) return; originals = fixture(640, 2, 5); future = session.observe(observe(640, 2, originals)); stage++;
            }
            case 12 -> {
                if (!ready()) return;
                sourceTown = people().getFirst().settlementId(); savedEntities = new HashMap<>();
                session.projectionView().orElseThrow().identities().projections().forEach((id, projection) -> savedEntities.put(id, projection.entityId()));
                var destination = fixture(704, 6, 2); future = session.observe(observe(704, 6, destination)); stage++;
            }
            case 13 -> {
                if (!ready()) return;
                destinationTown = people().stream().filter(p -> !p.settlementId().equals(sourceTown)).findFirst().orElseThrow().settlementId();
                adapters(); now.addAndGet(30); future = session.pulse(); stage++;
            }
            case 14 -> {
                if (!ready()) return;
                require(people().size() == 7 && people().stream().allMatch(p -> p.settlementId().equals(destinationTown)), "Saved household migration failed");
                for (var id : savedEntities.keySet()) {
                    var villager = visible(id); if (villager == null || villager.isInvulnerable() || savedEntities.get(id).equals(villager.getUniqueId())) return;
                    require(villager.getLocation().getX() > 690, "Moved person did not appear near destination home"); villager.setAI(false);
                    if (world.getEntity(savedEntities.get(id)) != null) return;
                }
                require(session.projectionView().orElseThrow().recentEvents().stream().filter(e -> e.event().type() == PopulationEvent.Type.MIGRATION).count() == 5, "Migration event count wrong");
                pause();
                var base = settings(0, 0);
                settings = new PopulationSession.Settings(base.seed(), base.millisPerMonth(), base.maximumOfflineMonths(), base.monthsPerBatch(),
                        new PopulationPolicy(12, 216, 240, 480, 720, 0, 1, 0), base.migration(), base.recognition(), base.enrollment());
                scenario = new File(getDataFolder(), "mortality-" + UUID.randomUUID()); now.set(1000); open(); stage++;
            }
            case 15 -> {
                if (!ready()) return; originals = fixture(800, 2, 2); future = session.observe(observe(800, 2, originals)); stage++;
            }
            case 16 -> {
                if (!ready()) return; adapters(); now.addAndGet(30); future = session.pulse(); stage++;
            }
            case 17 -> {
                if (!ready()) return;
                require(session.status().livingPeople() == 0 && people().size() == 2, "Simulated mortality lost history or retained living count");
                if (originals.stream().anyMatch(villager -> world.getEntity(villager.getUniqueId()) != null)) return;
                require(session.projectionView().orElseThrow().recentEvents().stream().filter(e -> e.event().type() == PopulationEvent.Type.DEATH).count() == 2, "Simulated death events wrong");
                require(session.projectionView().orElseThrow().recentDeaths().isEmpty(), "Projection removal was mistaken for a second physical death");
                pause(); settings = settings(0, 0); scenario = new File(getDataFolder(), "enrollment-race-" + UUID.randomUUID()); now.set(1000); open(); stage++;
            }
            case 18 -> {
                if (!ready()) return; adapters(); originals = fixture(864, 2, 2);
                var observation = observe(864, 2, originals); originals.getFirst().setInvulnerable(false); originals.getFirst().damage(1000);
                future = session.observe(observation); stage++;
            }
            case 19 -> {
                if (!ready()) return;
                if (session.status().livingPeople() != 1) return;
                require(people().size() == 2 && session.projectionView().orElseThrow().recentDeaths().size() == 1,
                        "Death during queued observation lost or duplicated identity"); stage = 99;
            }
            default -> throw new AssertionError("Unknown stage");
        }
    }
}
