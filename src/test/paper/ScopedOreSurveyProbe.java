import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.DatabaseManager;
import com.blueninjas24.civilizations.database.MineralOpportunities;
import com.blueninjas24.civilizations.database.OreTrackingRepository;
import com.blueninjas24.civilizations.industry.OreObserverPolicy;
import com.blueninjas24.civilizations.paper.GeneratedTerrainObserver;
import com.blueninjas24.civilizations.paper.PaperOreObserver;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Marked disposable proof of current survey eligibility scoped to real generated terrain. */
public final class ScopedOreSurveyProbe extends JavaPlugin {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Map<Block, BlockData> originals = new LinkedHashMap<>();
    private PopulationSession session;
    private GeneratedTerrainObserver generatedObserver;
    private PaperOreObserver oreObserver;
    private World world;
    private Chunk oldChunk, freshChunk;
    private Path folder;
    private UUID worldId, town;
    private String worldName;
    private int oldX, oldZ, stage, ticks;
    private boolean authorized, oldTicket, freshTicket;
    private VillageObservation.Position excluded, admitted;
    private long completedSnapshot;
    private CompletableFuture<Void> seeded, ready, exclusionRecorded;
    private CompletableFuture<Boolean> oldEligibility, freshEligibility;
    private CompletableFuture<MineralOpportunities.Review> review;
    private CompletableFuture<List<MineralOpportunities.Evidence>> evidence;
    private CompletableFuture<Void> finalClose;

    private void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    @Override public void onEnable() {
        try {
            Path root = getServer().getWorldContainer().toPath().toRealPath();
            check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim()
                            .equals("Disposable Civilizations Phase 1 test environment")
                            && getServer().getIp().equals("127.0.0.1")
                            && getServer().getPort() == 25578
                            && getServer().getOnlinePlayers().isEmpty(),
                    "Marked disposable localhost fixture with zero players required");
            check(getServer().getPluginManager().getPlugin("WorldEdit") == null
                            && getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") == null,
                    "Scoped survey probe requires no WorldEdit or FAWE plugin");
            check(!getServer().getWorlds().isEmpty(), "A loaded fixture world is required");
            authorized = true;
            world = getServer().getWorlds().getFirst();
            worldId = world.getUID();
            worldName = world.getName();
            oldChunk = List.of(world.getLoadedChunks()).stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("An existing loaded fixture chunk is required"));
            oldX = oldChunk.getX();
            oldZ = oldChunk.getZ();
            oldTicket = oldChunk.addPluginChunkTicket(this);
            check(oldTicket, "Could not hold the already-loaded legacy fixture chunk");
            check(!world.isChunkGenerated(10_000, 10_000), "Scoped survey fixture chunk (10000,10000) must be fresh");

            folder = getDataFolder().toPath().resolve("scoped-ore-private-" + UUID.randomUUID());
            seeded = CompletableFuture.runAsync(this::seedUntrustedOreHistory, worker);
            new BukkitRunnable() {
                @Override public void run() {
                    try { advance(); }
                    catch (Throwable failure) { failProbe(failure); }
                }
            }.runTaskTimer(this, 1, 1);
        } catch (Throwable failure) {
            failProbe(failure);
        }
    }

    private void seedUntrustedOreHistory() {
        DatabaseManager database = new DatabaseManager(folder.toFile(), getLogger());
        try {
            database.initialize();
            var tracking = new OreTrackingRepository(database.getConnection());
            long now = System.currentTimeMillis();
            tracking.begin(now);
            tracking.markGap(now + 1);
            var state = tracking.finish(now + 2, true);
            check(state.cleanStop() && !state.trusted(), "A clean stop must not repair a previously recorded ore gap");
        } catch (Exception failure) {
            throw new CompletionException(failure);
        } finally {
            database.close();
        }
    }

    private void advance() throws Exception {
        check(++ticks <= 600, "Scoped ore survey probe timed out at stage " + stage + diagnostic());
        switch (stage) {
            case 0 -> {
                if (!seeded.isDone()) return;
                seeded.join();
                session = new PopulationSession(folder.toFile(), getLogger(), PopulationSession.Settings.prototype(), System::currentTimeMillis);
                session.configureOreTracking();
                session.configureForestTracking();
                int x = 10_000 * 16 + 8, z = 10_000 * 16 + 8, y = world.getMaxHeight() - 8;
                var home = new VillageObservation.Position(x, y, z);
                var home2 = new VillageObservation.Position(x + 2, y, z);
                var observation = new VillageObservation(worldId, worldName, System.currentTimeMillis(), true,
                        List.of(home, home2), List.of(
                        new VillageObservation.Person(UUID.randomUUID(), home, true, "Scoped Fixture A"),
                        new VillageObservation.Person(UUID.randomUUID(), home2, true, "Scoped Fixture B")));
                ready = session.start(false, Set.of(worldId))
                        .thenCompose(ignored -> session.observe(observation)).thenApply(ignored -> null);
                stage = 1;
            }
            case 1 -> {
                if (!ready.isDone()) return;
                ready.join();
                check(session.oreCaptureReady() && session.forestCaptureReady() && !session.oreDiscoveryReady(),
                        "Current capture must be healthy while global ore history remains untrusted");
                town = session.settlementNames().keySet().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("Synthetic fixture town was not saved"));
                generatedObserver = new GeneratedTerrainObserver(session, getLogger());
                getServer().getPluginManager().registerEvents(generatedObserver, this);
                freshChunk = world.getChunkAt(10_000, 10_000); // Explicit fixture setup; production observers never generate chunks.
                freshTicket = freshChunk.addPluginChunkTicket(this);
                check(freshTicket, "Could not hold the fresh fixture chunk");
                oldEligibility = session.canSurveyResourceChunk("ore", worldId, oldX, oldZ, System.currentTimeMillis());
                freshEligibility = session.canSurveyResourceChunk("ore", worldId, 10_000, 10_000, System.currentTimeMillis());

                int x = 10_000 * 16 + 5, z = 10_000 * 16 + 5, y = world.getMaxHeight() - 8;
                for (int offset = -1; offset <= 2; offset++) {
                    Block block = world.getBlockAt(x + offset, y, z);
                    check(block.getType().isAir(), "Fixture must preserve only original air at ore coordinate " + block.getLocation());
                    originals.put(block, block.getBlockData().clone());
                }
                world.getBlockAt(x, y, z).setType(Material.COAL_ORE, false);
                world.getBlockAt(x + 1, y, z).setType(Material.COAL_ORE, false);
                excluded = new VillageObservation.Position(x, y, z);
                admitted = new VillageObservation.Position(x + 1, y, z);
                stage = 2;
            }
            case 2 -> {
                if (!oldEligibility.isDone() || !freshEligibility.isDone()) return;
                check(!oldEligibility.join(), "An old loaded chunk must remain ineligible after an ore history gap");
                check(freshEligibility.join(), "The actual newly generated chunk must receive current ore coverage");
                check(!session.oreDiscoveryReady() && session.oreCaptureReady(),
                        "Scoped eligibility must not repair global ore trust or disable current capture");
                exclusionRecorded = session.recordOrePlacements(worldId, worldName, List.of(excluded), System.currentTimeMillis()).thenApply(ignored -> null);
                stage = 3;
            }
            case 3 -> {
                if (!exclusionRecorded.isDone()) return;
                exclusionRecorded.join();
                oreObserver = new PaperOreObserver(session, new OreObserverPolicy(true, 1, 256, 20, 0), System::currentTimeMillis);
                oreObserver.runTaskTimer(this, 1, 1);
                stage = 4;
            }
            case 4 -> {
                String diagnostic = oreObserver.diagnostic();
                if (diagnostic.startsWith("Ore survey skipped:") || diagnostic.startsWith("Ore survey deferred:"))
                    throw new IllegalStateException("Fresh generated chunk was unexpectedly rejected: " + diagnostic);
                if (!diagnostic.startsWith("Ore survey: ")) return;
                check(diagnostic.contains("1 observed patches; 0 linked"), "Expected exactly one unexcluded coal opportunity: " + diagnostic);
                Matcher matcher = Pattern.compile("snapshot captured at (\\d+);").matcher(diagnostic);
                check(matcher.find(), "Survey diagnostic must preserve its capture timestamp: " + diagnostic);
                completedSnapshot = Long.parseLong(matcher.group(1));
                review = session.currentMineralOpportunities(town, 60_000);
                evidence = session.mineralOpportunities(town, 512);
                stage = 5;
            }
            case 5 -> {
                if (!review.isDone() || !evidence.isDone()) return;
                var current = review.join();
                var rows = evidence.join();
                check(current.status() == MineralOpportunities.Status.CURRENT && current.candidates().size() == 1,
                        "Scoped current review must expose exactly one candidate: " + current);
                check(rows.size() == 1 && rows.getFirst().world().equals(worldId)
                                && rows.getFirst().position().equals(admitted)
                                && rows.getFirst().mineral().id().equals("coal")
                                && !rows.getFirst().excluded() && rows.getFirst().savedDeposit() == null
                                && rows.getFirst().lastSeenMillis() == completedSnapshot
                                && completedSnapshot >= 0 && completedSnapshot <= System.currentTimeMillis(),
                        "Durable SQL-owner evidence must contain only the unexcluded current coal: " + rows);
                check(current.candidates().getFirst().position().equals(admitted)
                                && !session.oreDiscoveryReady() && session.oreCaptureReady(),
                        "Current scoped candidate must not make old unverified terrain globally trusted");
                PopulationSession closing = session;
                finalClose = CompletableFuture.runAsync(() -> {
                    closing.close();
                    verifyNoBusinessOrFreeCash();
                }, worker);
                stage = 6;
            }
            case 6 -> {
                if (!finalClose.isDone()) return;
                finalClose.join();
                session = null;
                getLogger().info("NATIVE SCOPED ORE SURVEY PASS: real generated-chunk certificate admitted current survey while legacy loaded terrain stayed blocked; one unexcluded coal coordinate was saved with the actual snapshot timestamp; no deposit founding");
                Bukkit.getScheduler().cancelTasks(this);
                getServer().getPluginManager().disablePlugin(this);
                getServer().shutdown();
            }
            default -> throw new IllegalStateException("Unexpected scoped ore survey stage " + stage);
        }
    }

    private void verifyNoBusinessOrFreeCash() {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + folder.resolve("civilizations.db").toAbsolutePath());
             var statement = connection.createStatement()) {
            for (String table : List.of("businesses", "business_sites", "industry_stores", "industry_stock", "treasury_entries")) {
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    check(rows.next() && rows.getLong(1) == 0, "Survey fixture must not create businesses, stores, or cash: " + table);
                }
            }
        } catch (Exception failure) {
            throw new CompletionException(failure);
        }
    }

    private String diagnostic() {
        return oreObserver == null ? "" : ": " + oreObserver.diagnostic();
    }

    private void failProbe(Throwable failure) {
        getLogger().log(Level.SEVERE, "NATIVE SCOPED ORE SURVEY FAIL", failure);
        if (authorized) Bukkit.getScheduler().cancelTasks(this);
        getServer().getPluginManager().disablePlugin(this);
        if (authorized) getServer().shutdown();
    }

    @Override public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        originals.forEach((block, data) -> block.setBlockData(data, false));
        originals.forEach((block, data) -> check(block.getBlockData().equals(data), "Fixture block restoration failed"));
        if(!originals.isEmpty())getLogger().info("NATIVE SCOPED ORE SURVEY RESTORED: all four original AIR BlockData values");
        originals.clear();
        if (oldTicket && oldChunk != null) oldChunk.removePluginChunkTicket(this);
        if (freshTicket && freshChunk != null) freshChunk.removePluginChunkTicket(this);
        PopulationSession closing = session;
        if (closing == null) worker.shutdown();
        else CompletableFuture.runAsync(closing::close, worker).whenComplete((ignored, failure) -> {
            if (failure != null) getLogger().log(Level.SEVERE, "Private population session close failed", failure);
            worker.shutdown();
        });
        session = null;
    }
}
