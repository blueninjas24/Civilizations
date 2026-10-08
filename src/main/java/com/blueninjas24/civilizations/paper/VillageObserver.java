package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.*;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Main-thread bounded snapshots of already loaded villages. Never tickets/loads a chunk. */
public final class VillageObserver extends BukkitRunnable {
    public record Settings(int radiusChunks, int readsPerTick, long nanosPerTick, int refreshTicks) {
        public Settings {
            if (radiusChunks < 1 || radiusChunks > 8 || readsPerTick < 64 || readsPerTick > 65_536
                    || nanosPerTick < 100_000 || nanosPerTick > 10_000_000 || refreshTicks < 20)
                throw new IllegalArgumentException("Invalid observer budgets");
        }
        public static Settings prototype() { return new Settings(4, 4096, 2_000_000, 100); }
    }
    private record Anchor(UUID world, int x, int z) {}
    private final org.bukkit.plugin.Plugin plugin;
    private final PopulationSession session;
    private final Settings settings;
    private final Deque<Chunk> seeds = new ArrayDeque<>();
    private final Map<Anchor, Long> visited = new LinkedHashMap<>();
    private Frame frame;
    private CompletableFuture<PopulationSession.ObservationResult> submitted;
    private boolean submittedComplete;
    private long ticks;
    private String lastDiagnostic;

    public VillageObserver(org.bukkit.plugin.Plugin plugin, PopulationSession session, Settings settings) {
        this.plugin = plugin; this.session = session; this.settings = settings;
    }
    public String diagnostic() {
        if (frame != null) return "Scanning loaded village: " + frame.chunks.size()
                + " chunk snapshots remaining; " + frame.beds.size() + " bed heads found";
        return lastDiagnostic == null ? "No village observation completed yet" : lastDiagnostic;
    }

    @Override public void run() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Village observer requires Paper's main thread");
        ticks++;
        if (!session.status().ready()) return;
        if (submitted != null) {
            if (!submitted.isDone()) return;
            try {
                var result = submitted.join();
                lastDiagnostic = (submittedComplete ? "Complete frame" : "Partial frame (housing unknown)")
                        + ": observed villages " + result.villages().size() + "; enrollment failures " + result.failures().size();
                if (!result.failures().isEmpty()) lastDiagnostic += "; " + result.failures().getFirst();
                for (var village : result.villages()) {
                    if (village.createdSettlement() || village.importedPeople() > 0)
                        plugin.getLogger().info("Village " + village.settlementId() + ": imported " + village.importedPeople() + " persistent residents");
                }
            } catch (java.util.concurrent.CompletionException failed) {
                lastDiagnostic = "Village observation deferred: " + failed.getCause().getMessage();
                plugin.getLogger().warning(lastDiagnostic);
            }
            submitted = null;
        }
        long began = System.nanoTime();
        try {
            if (frame != null) {
                frame.advance(began);
                if (frame.done()) {
                    var observation = frame.observation(); frame = null;
                    if (!observation.beds().isEmpty() && !observation.villagers().isEmpty()) {
                        lastDiagnostic = "Submitting " + (observation.complete() ? "complete" : "partial")
                                + " frame: " + observation.beds().size() + " beds, " + observation.villagers().size() + " villagers";
                        submittedComplete = observation.complete();
                        submitted = session.observe(observation);
                    }
                }
                return;
            }
            if (seeds.isEmpty() && ticks % settings.refreshTicks() == 1) {
                // Enumerate, but never retrieve/load chunks outside the already-loaded set.
                for (World world : Bukkit.getWorlds()) {
                    for (Chunk chunk : world.getLoadedChunks()) {
                        if (seeds.size() == 4096) break;
                        seeds.add(chunk);
                    }
                    if (seeds.size() == 4096) break;
                }
            }
            for (int inspected = 0; inspected < 2 && !seeds.isEmpty()
                    && System.nanoTime() - began < settings.nanosPerTick(); inspected++) {
                Chunk chunk = seeds.removeFirst();
                if (!chunk.isLoaded() || !chunk.isEntitiesLoaded()) continue;
                var anchor = new Anchor(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
                if (ticks - visited.getOrDefault(anchor, -1200L) < 1200) continue;
                if (Arrays.stream(chunk.getEntities()).noneMatch(entity -> entity instanceof Villager)) continue;
                visited.put(anchor, ticks);
                while (visited.size() > 4096) visited.remove(visited.keySet().iterator().next());
                frame = new Frame(chunk.getWorld(), chunk.getX(), chunk.getZ());
                break;
            }
        } catch (RuntimeException failed) {
            frame = null;
            lastDiagnostic = "Village scan rejected: " + failed.getMessage();
            plugin.getLogger().warning(lastDiagnostic);
        }
    }

    private final class Frame {
        private final World world;
        private final int left, right, top, bottom;
        private final Deque<Chunk> chunks = new ArrayDeque<>();
        private final List<Chunk> inspected = new ArrayList<>();
        private final Deque<Chunk> entityChunks = new ArrayDeque<>();
        private final List<VillageObservation.Position> beds = new ArrayList<>();
        private final Map<UUID, VillageObservation.Person> villagers = new HashMap<>();
        private boolean complete = true;
        private BedScan scan;
        private boolean collectingEntities;

        Frame(World world, int centerX, int centerZ) {
            this.world = world;
            int radius = settings.radiusChunks();
            left = (centerX - radius) * 16; right = (centerX + radius + 1) * 16 - 1;
            top = (centerZ - radius) * 16; bottom = (centerZ + radius + 1) * 16 - 1;
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    if (world.isChunkLoaded(x, z)) chunks.add(world.getChunkAt(x, z));
                    else complete = false;
                }
            }
        }
        void advance(long began) {
            if (collectingEntities) {
                for (int count = 0; count < 2 && !entityChunks.isEmpty()
                        && System.nanoTime() - began < settings.nanosPerTick(); count++) readEntities(entityChunks.removeFirst());
                return;
            }
            if (scan == null && !chunks.isEmpty()) {
                Chunk chunk = chunks.removeFirst();
                if (!chunk.isLoaded()) { complete = false; return; }
                inspected.add(chunk);
                var snapshot = chunk.getChunkSnapshot(false, false, false, false);
                scan = new BedScan(new PaperBedSource(snapshot, world.getMinHeight()), chunk.getX(), chunk.getZ(),
                        world.getMinHeight(), world.getMaxHeight(), session.settings().recognition().maximumBeds());
            }
            int remaining = settings.readsPerTick();
            while (scan != null && remaining > 0 && System.nanoTime() - began < settings.nanosPerTick()) {
                int used = scan.advance(Math.min(64, remaining)); remaining -= used;
                if (scan.complete()) {
                    beds.addAll(scan.beds()); scan = null;
                    if (beds.size() > session.settings().recognition().maximumBeds())
                        throw new IllegalArgumentException("Frame has too many beds; reduce observer radius");
                    // Capture at most one snapshot per tick, even when the chunk is empty.
                    break;
                }
            }
            if (chunks.isEmpty() && scan == null) { collectingEntities = true; entityChunks.addAll(inspected); }
        }
        boolean done() { return collectingEntities && entityChunks.isEmpty(); }
        private void readEntities(Chunk chunk) {
                if (!chunk.isLoaded() || !chunk.isEntitiesLoaded()) { complete = false; return; }
                for (var entity : chunk.getEntities()) {
                    if (entity instanceof Villager person) {
                        var location = person.getLocation();
                        String name = person.customName() == null ? null
                                : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(person.customName());
                        villagers.put(person.getUniqueId(), new VillageObservation.Person(person.getUniqueId(),
                                new VillageObservation.Position(location.getBlockX(), location.getBlockY(), location.getBlockZ()), person.isAdult(), name));
                        if (villagers.size() > session.settings().recognition().maximumVillagers())
                            throw new IllegalArgumentException("Frame has too many villagers; reduce observer radius");
                    }
                }
        }
        VillageObservation observation() {
            if (inspected.stream().anyMatch(chunk -> !chunk.isLoaded())) complete = false;
            int margin = Math.max(session.settings().recognition().connectionRadius(), session.settings().recognition().residentRadius());
            // No bed may connect across the scan boundary. Missing chunks make housing unknown.
            if (!new VillageObservation.Bounds(left, right, top, bottom).encloses(beds, margin)) complete = false;
            return new VillageObservation(world.getUID(), world.getName(), System.currentTimeMillis(), complete,
                    beds, List.copyOf(villagers.values()));
        }
    }
}
