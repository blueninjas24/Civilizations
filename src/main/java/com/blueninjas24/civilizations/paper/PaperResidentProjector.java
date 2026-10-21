package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.*;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.*;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.plugin.Plugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/** Incremental main-thread reconciliation with one durable spawn handshake at a time. */
public final class PaperResidentProjector extends BukkitRunnable {
    private static final NamespacedKey PENDING = new NamespacedKey("civilizations", "projection_pending");
    public record Settings(int nearbyRadius, int maximumPerWorld, int maximumAtNight, int chunksPerTick, int residentsPerTick) {
        public Settings {
            if (nearbyRadius < 8 || nearbyRadius > 128 || maximumPerWorld < 1 || maximumPerWorld > 256
                    || chunksPerTick < 1 || chunksPerTick > 8 || residentsPerTick < 1 || residentsPerTick > 128)
                throw new IllegalArgumentException("Invalid projection budgets");
            if (maximumAtNight < 1 || maximumAtNight > maximumPerWorld) throw new IllegalArgumentException("Invalid nighttime projection budget");
        }
        public static Settings prototype() { return new Settings(64, 32, 8, 2, 16); }
    }
    private enum Work { CLAIM, ACTIVATE, RETIRE }
    private final Plugin plugin;
    private final PopulationSession session;
    private final Settings settings;
    private final Predicate<Location> nearby;
    private final Deque<Chunk> chunks = new ArrayDeque<>();
    private final Map<UUID, Set<UUID>> seen = new HashMap<>();
    private List<Resident> people = List.of();
    private int cursor;
    private long ticks, retryAt;
    private Work work;
    private Resident workingPerson;
    private Villager prepared;
    private UUID workingEntity;
    private CompletableFuture<Boolean> pending;
    private String lastFailure;

    public PaperResidentProjector(Plugin plugin, PopulationSession session, Settings settings) {
        this(plugin, session, settings, location -> location.getWorld().getPlayers().stream()
                .anyMatch(player -> player.getLocation().distanceSquared(location) <= (double) settings.nearbyRadius() * settings.nearbyRadius()));
    }
    /** Disposable integration harness may supply observer positions without creating fake players. */
    public PaperResidentProjector(Plugin plugin, PopulationSession session, Settings settings, Predicate<Location> nearby) {
        this.plugin = plugin; this.session = session; this.settings = settings; this.nearby = nearby;
    }
    public String diagnostic() { return "Projection: " + (pending == null ? "idle" : work) + "; last failure: " + (lastFailure == null ? "none" : lastFailure); }

    @Override public void run() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Projection requires Paper main thread");
        ticks++;
        if (!session.status().ready()) return;
        if (pending != null) finishPending();
        var view = session.projectionView().orElseThrow();
        var residents = new HashMap<UUID, Resident>(); view.population().residents().forEach(p -> residents.put(p.id(), p));
        var aliasedPeople = new HashSet<>(view.identities().aliases().values());
        if (chunks.isEmpty() && ticks % 100 == 1) {
            seen.clear();
            for (var world : Bukkit.getWorlds()) for (var chunk : world.getLoadedChunks()) {
                if (chunks.size() < 4096) chunks.addLast(chunk);
            }
        }
        for (int count = 0; count < settings.chunksPerTick() && !chunks.isEmpty(); count++) {
            var chunk = chunks.removeFirst();
            if (!chunk.isLoaded() || !chunk.isEntitiesLoaded()) continue;
            for (var entity : chunk.getEntities()) {
                if (!(entity instanceof Villager villager)) continue;
                UUID id = view.identities().aliases().get(new EntityKey(villager.getWorld().getUID(), villager.getUniqueId()));
                if (id == null) continue;
                seen.computeIfAbsent(villager.getWorld().getUID(), ignored -> new HashSet<>()).add(villager.getUniqueId());
                var projection = view.identities().projections().get(id); var person = residents.get(id);
                if (projection == null || person == null) continue;
                if (!projection.entityId().equals(villager.getUniqueId()) || projection.state() == State.RETIRED || person.status() == Resident.Status.DECEASED) villager.remove();
                else if (projection.state() == State.ACTIVE) {
                    apply(villager, person, session.settings().population().adulthoodSteps());
                    // Activation may have committed immediately before shutdown, before its main-thread acknowledgement.
                    if (villager.getPersistentDataContainer().has(PENDING, PersistentDataType.BYTE)) {
                        release(villager);
                    }
                    assignHome(villager,person,view.identities());
                }
            }
        }
        if (pending != null || System.currentTimeMillis() < retryAt) return;
        // Complete each stable cursor before adopting a newer snapshot, avoiding starvation under pulses.
        if (cursor >= people.size()) { people = view.population().residents(); cursor = 0; }
        for (int count = 0; count < settings.residentsPerTick() && cursor < people.size(); count++) {
            var original = people.get(cursor++); var person = residents.get(original.id());
            if (person == null) continue;
            var projection = view.identities().projections().get(person.id());
            if (projection != null && projection.state() != State.RETIRED) {
                if (person.status() == Resident.Status.DECEASED || !Objects.equals(person.householdId(), projection.householdId())) {
                    submit(Work.RETIRE, person, projection.entityId(), session.retireProjection(person.id(), projection.entityId())); return;
                }
                if (projection.state() == State.CLAIMED) {
                    var world = Bukkit.getWorld(projection.worldId());
                    if (world == null) continue;
                    var entity = world.getEntity(projection.entityId());
                    if (entity instanceof Villager villager) {
                        freeze(villager); prepared = villager;
                        submit(Work.ACTIVATE, person, projection.entityId(), session.activateProjection(person.id(), projection.entityId())); return;
                    } else if (projection.position() != null && loaded(world, projection.position().x(), projection.position().z())) {
                        submit(Work.RETIRE, person, projection.entityId(), session.retireProjection(person.id(), projection.entityId())); return;
                    }
                }
                continue; // Missing active entities never respawn or count as deaths.
            }
            if (person.status() != Resident.Status.ALIVE || person.householdId() == null) continue;
            if (!chunks.isEmpty()) continue; // Do not spend density budget against a partial loaded census.
            if (projection == null && aliasedPeople.contains(person.id())) continue;
            var home = view.identities().homes().get(person.householdId()); if (home == null) continue;
            var world = Bukkit.getWorld(home.worldId()); if (world == null) continue;
            var location = safeLocation(world, home.bedFor(person.id()));
            if (location == null || !nearby.test(location) || seen.getOrDefault(home.worldId(), Set.of()).size() >= spawnLimit(settings, world.getTime())) continue;
            prepared = world.createEntity(location, Villager.class); freeze(prepared); apply(prepared, person, session.settings().population().adulthoodSteps());
            var position = new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            submit(Work.CLAIM, person, prepared.getUniqueId(), session.claimProjection(person.id(), home.worldId(), prepared.getUniqueId(), person.householdId(), position)); return;
        }
    }
    private void submit(Work operation, Resident person, UUID entity, CompletableFuture<Boolean> future) {
        work = operation; workingPerson = person; workingEntity = entity; pending = future;
    }
    private void finishPending() {
        if (!pending.isDone()) return;
        try {
            boolean accepted = pending.join(); var completed = work; pending = null;
            var view = session.projectionView().orElseThrow();
            var projection = view.identities().projections().get(workingPerson.id());
            var person = view.population().residents().stream().filter(p -> p.id().equals(workingPerson.id())).findFirst().orElseThrow();
            boolean current = projection != null && projection.entityId().equals(workingEntity) && person.status() == Resident.Status.ALIVE
                    && Objects.equals(person.householdId(), projection.householdId());
            if (completed == Work.RETIRE && accepted && projection != null) {
                var world = Bukkit.getWorld(projection.worldId());
                if (world != null && world.getEntity(workingEntity) instanceof Villager old) {
                    old.remove(); seen.getOrDefault(world.getUID(), new HashSet<>()).remove(workingEntity);
                }
            }
            if (completed == Work.CLAIM && accepted && current && prepared != null) {
                var location = prepared.getLocation();
                var home = view.identities().homes().get(person.householdId());
                if (home != null && safeLocation(location.getWorld(), home.bedFor(person.id())) != null
                        && loaded(location.getWorld(), location.getBlockX(), location.getBlockZ()) && nearby.test(location) && clear(location)) {
                    location.getWorld().addEntity(prepared);
                    if (prepared.isValid()) {
                        seen.computeIfAbsent(location.getWorld().getUID(), ignored -> new HashSet<>()).add(workingEntity);
                        submit(Work.ACTIVATE, person, workingEntity, session.activateProjection(person.id(), workingEntity)); return;
                    }
                }
                if (prepared.isValid()) prepared.remove(); prepared = null;
                retryAt = System.currentTimeMillis() + 5000;
                submit(Work.RETIRE, person, workingEntity, session.retireProjection(person.id(), workingEntity)); return;
            }
            if (completed == Work.ACTIVATE && current && projection.state() == State.ACTIVE && prepared != null && prepared.isValid()) {
                apply(prepared, person, session.settings().population().adulthoodSteps());
                release(prepared);
                assignHome(prepared,person,view.identities());
            }
            prepared = null; lastFailure = null;
        } catch (RuntimeException failure) {
            pending = null; prepared = null; retryAt = System.currentTimeMillis() + 5000;
            String message = failure instanceof java.util.concurrent.CompletionException ? failure.getCause().getMessage() : failure.getMessage();
            if (!Objects.equals(lastFailure, message)) plugin.getLogger().warning("Resident projection deferred: " + message);
            lastFailure = message;
        }
    }
    static void apply(Villager villager, Resident person, long adulthood) {
        villager.customName(Component.text(person.name())); villager.setAgeLock(true);
        if (person.ageSteps() >= adulthood) { if (!villager.isAdult()) villager.setAdult(); }
        else if (villager.isAdult()) villager.setBaby();
    }
    static int spawnLimit(Settings settings, long time) {
        long dayTime = Math.floorMod(time, 24000);
        return dayTime >= 13000 && dayTime < 23000 ? settings.maximumAtNight() : settings.maximumPerWorld();
    }
    private static void freeze(Villager villager) {
        villager.getPersistentDataContainer().set(PENDING, PersistentDataType.BYTE, (byte)1);
        villager.setAI(false); villager.setGravity(false); villager.setInvulnerable(true); villager.setPersistent(true);
    }
    private static void release(Villager villager) {
        villager.setAI(true); villager.setGravity(true); villager.setInvulnerable(false); villager.setPersistent(true);
        villager.getPersistentDataContainer().remove(PENDING);
    }
    private static void assignHome(Villager villager,Resident person,Snapshot identities) {
        if(person.householdId()==null)return; // No recorded household is not an explicit loss of an assigned home.
        var home=identities.homes().get(person.householdId());
        if(home==null){if(identities.managedHousing().contains(person.householdId()))villager.setMemory(MemoryKey.HOME,null);return;}
        var bed=home.bedFor(person.id());if(bed==null)return;
        if(!home.worldId().equals(villager.getWorld().getUID()))return;
        if(!loaded(villager.getWorld(),bed.x(),bed.z()))return; // Unloaded does not mean destroyed.
        var actual=villager.getWorld().getBlockAt(bed.x(),bed.y(),bed.z());
        if(!(actual.getBlockData() instanceof Bed data)||data.getPart()!=Bed.Part.HEAD)return;
        var target=new Location(villager.getWorld(),bed.x(),bed.y(),bed.z());
        if(!target.equals(villager.getMemory(MemoryKey.HOME)))villager.setMemory(MemoryKey.HOME,target);
    }
    public void close() { prepared = null; pending = null; }
    static boolean loaded(World world, int x, int z) {
        return world.isChunkLoaded(Math.floorDiv(x, 16), Math.floorDiv(z, 16))
                && world.getChunkAt(Math.floorDiv(x, 16), Math.floorDiv(z, 16)).isEntitiesLoaded();
    }
    static Location safeLocation(World world, Position home) {
        if (home==null||!loaded(world, home.x(), home.z())) return null;
        var bed = world.getBlockAt(home.x(), home.y(), home.z());
        if (!(bed.getBlockData() instanceof Bed data) || data.getPart() != Bed.Part.HEAD) return null;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int x = home.x() + dx, z = home.z() + dz;
            if (!loaded(world, x, z)) continue;
            var location = new Location(world, x + 0.5, home.y(), z + 0.5);
            if (clear(location)) return location;
        }
        return null;
    }
    private static boolean clear(Location location) {
        var world = location.getWorld(); int y = location.getBlockY(), x = location.getBlockX(), z = location.getBlockZ();
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight() || !world.getWorldBorder().isInside(location)) return false;
        var floor = world.getBlockAt(x, y - 1, z).getType();
        return floor.isSolid() && floor != Material.MAGMA_BLOCK && floor != Material.CACTUS
                && floor != Material.CAMPFIRE && floor != Material.SOUL_CAMPFIRE
                && world.getBlockAt(x, y, z).getType().isAir() && world.getBlockAt(x, y + 1, z).getType().isAir();
    }
}
