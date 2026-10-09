package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository.Home;
import com.blueninjas24.civilizations.economy.*;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Read-only planted fields around saved homes, main-thread snapshots, no forced loading/ownership claims. */
public final class PaperFarmObserver extends BukkitRunnable {
    private final PopulationSession session;
    private final FarmObservation.Policy policy;
    private final Deque<UUID> towns = new ArrayDeque<>();
    private Frame frame;
    private CompletableFuture<?> pending;
    private long ticks;
    private String diagnostic = "No farm scan completed";
    public PaperFarmObserver(PopulationSession session, FarmObservation.Policy policy) { this.session = session; this.policy = policy; }
    public String diagnostic() { return frame == null ? diagnostic : "Farm scan: " + frame.chunks.size() + " chunks remain"; }
    @Override public void run() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Farm observer requires main thread");
        if (!session.status().ready()) return;
        ticks++;
        if (pending != null) {
            if (!pending.isDone()) return;
            try { pending.join(); } catch (java.util.concurrent.CompletionException failure) { diagnostic = "Farm save deferred: " + failure.getCause().getMessage(); }
            pending = null;
        }
        try {
            if (frame != null) {
                if (!frame.advance()) return;
                var evidence = frame.evidence(); frame = null;
                diagnostic = (evidence.complete() ? "Complete" : "Partial/conflicting") + " farm scan: " + evidence.plantedCrops() + " planted crops";
                pending = session.observeFarms(evidence); return;
            }
            var view = session.projectionView().orElseThrow(); var homes = new TreeMap<UUID, List<Home>>();
            for (var household : view.population().households()) {
                var home = view.identities().homes().get(household.id());
                if (home != null) homes.computeIfAbsent(household.settlementId(), ignored -> new ArrayList<>()).add(home);
            }
            if (towns.isEmpty() && ticks % 200 == 1) towns.addAll(homes.keySet());
            if (!towns.isEmpty()) {
                var town = towns.removeFirst(); var anchors = homes.get(town);
                // Unloaded villages keep last reliable evidence; do not mark unloading itself as crop loss.
                if (anchors == null || anchors.isEmpty()) return;
                var world = Bukkit.getWorld(anchors.getFirst().worldId());
                if (world == null || anchors.stream().noneMatch(h -> world.isChunkLoaded(Math.floorDiv(h.position().x(), 16), Math.floorDiv(h.position().z(), 16)))) return;
                if (anchors.size() > 64) {
                    pending = session.observeFarms(new FarmObservation(town, world.getUID(), System.currentTimeMillis(), false, 0));
                    diagnostic = "Farm scan deferred: home count exceeds budget"; return;
                }
                try { frame = new Frame(town, world, anchors, homes); }
                catch (RuntimeException failure) {
                    pending = session.observeFarms(new FarmObservation(town, world.getUID(), System.currentTimeMillis(), false, 0));
                    throw failure;
                }
            }
        } catch (RuntimeException failure) {
            if (frame != null) {
                pending = session.observeFarms(new FarmObservation(frame.town, frame.world.getUID(),
                        System.currentTimeMillis(), false, Math.min(frame.crops.size(), 100000)));
            }
            frame = null; diagnostic = "Farm scan deferred: " + failure.getMessage();
        }
    }
    private final class Frame {
        private final UUID town;
        private final World world;
        private final List<Home> anchors;
        private final Map<UUID, List<Home>> allHomes;
        private final Deque<Chunk> chunks = new ArrayDeque<>();
        private final List<Chunk> inspected = new ArrayList<>();
        private final Set<com.blueninjas24.civilizations.settlement.VillageObservation.Position> crops = new HashSet<>();
        private boolean complete = true;
        private CropScan scan;
        private final int minimumY, maximumY;
        Frame(UUID town, World world, List<Home> anchors, Map<UUID, List<Home>> allHomes) {
            this.town = town; this.world = world; this.anchors = List.copyOf(anchors); this.allHomes = allHomes;
            minimumY = Math.max(world.getMinHeight(), anchors.stream().mapToInt(h -> h.position().y()).min().orElseThrow() - policy.verticalRadius());
            maximumY = Math.min(world.getMaxHeight(), anchors.stream().mapToInt(h -> h.position().y()).max().orElseThrow() + policy.verticalRadius() + 1);
            var keys = new TreeSet<Long>();
            for (var home : anchors) {
                if (!home.worldId().equals(world.getUID())) throw new IllegalArgumentException("Village homes cross worlds");
                for (int x = Math.floorDiv(home.position().x() - policy.radius(), 16); x <= Math.floorDiv(home.position().x() + policy.radius(), 16); x++)
                    for (int z = Math.floorDiv(home.position().z() - policy.radius(), 16); z <= Math.floorDiv(home.position().z() + policy.radius(), 16); z++)
                        keys.add(((long)x << 32) | (z & 0xffffffffL));
            }
            if (keys.size() > 256) throw new IllegalArgumentException("Farm footprint exceeds scan budget");
            for (long key : keys) {
                int x = (int)(key >> 32), z = (int)key;
                if (world.isChunkLoaded(x, z)) chunks.add(world.getChunkAt(x, z)); else complete = false;
            }
        }
        boolean advance() {
            long began = System.nanoTime(); int remaining = 4096;
            if (scan == null && !chunks.isEmpty()) {
                var chunk = chunks.removeFirst();
                if (!chunk.isLoaded()) { complete = false; return false; }
                inspected.add(chunk);
                scan = new CropScan(new PaperCropSource(chunk.getChunkSnapshot(false, false, false, false), world.getMinHeight()),
                        chunk.getX(), chunk.getZ(), minimumY, maximumY);
            }
            while (scan != null && remaining > 0 && System.nanoTime() - began < 1_000_000) {
                remaining -= scan.advance(Math.min(64, remaining));
                if (scan.complete()) {
                    for (var crop : scan.crops()) {
                        if (anchors.stream().noneMatch(h -> CropScan.near(crop, h.position(), policy))) continue;
                        if (allHomes.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(town) && entry.getValue().stream()
                                .anyMatch(h -> h.worldId().equals(world.getUID()) && CropScan.near(crop, h.position(), policy)))) complete = false;
                        crops.add(crop);
                        if (crops.size() > 100000) throw new IllegalArgumentException("Farm crop count exceeds budget");
                    }
                    scan = null; break;
                }
            }
            return scan == null && chunks.isEmpty();
        }
        FarmObservation evidence() {
            if (inspected.stream().anyMatch(chunk -> !chunk.isLoaded())) complete = false;
            var representative=crops.stream().min(Comparator.comparingInt(com.blueninjas24.civilizations.settlement.VillageObservation.Position::x)
                    .thenComparingInt(com.blueninjas24.civilizations.settlement.VillageObservation.Position::y)
                    .thenComparingInt(com.blueninjas24.civilizations.settlement.VillageObservation.Position::z)).orElse(null);
            return new FarmObservation(town, world.getUID(), System.currentTimeMillis(), complete, crops.size(),representative);
        }
    }
}
