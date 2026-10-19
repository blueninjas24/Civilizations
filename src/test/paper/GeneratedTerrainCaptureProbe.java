import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.GeneratedTerrainRepository;
import com.blueninjas24.civilizations.database.ResourceRecoveryEpochs;
import com.blueninjas24.civilizations.paper.GeneratedTerrainObserver;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Disposable native proof that only real new-chunk events create generated-terrain certificates. */
public final class GeneratedTerrainCaptureProbe extends JavaPlugin {
    private record Certificate(String kind, UUID world, int x, int z, UUID generation, long observedMillis) {}
    private record Snapshot(Set<Certificate> certificates) {}

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile PopulationSession session;
    private GeneratedTerrainObserver observer;
    private World world;
    private Chunk existingChunk;
    private Path folder;
    private UUID worldId;
    private String worldName;
    private int oldX, oldZ, stage, ticks;
    private boolean authorized;
    private CompletableFuture<PopulationSession.Status> started, restarted;
    private CompletableFuture<?> barrier;
    private CompletableFuture<Snapshot> beforeRestart;
    private CompletableFuture<Void> closed, finalClose;
    private Snapshot saved;

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
                    "Generated terrain probe requires no WorldEdit or FAWE plugin");
            authorized = true;
            world = getServer().getWorlds().getFirst();
            worldId = world.getUID();
            worldName = world.getName();
            existingChunk = List.of(world.getLoadedChunks()).stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("An existing loaded fixture chunk is required"));
            oldX = existingChunk.getX(); oldZ = existingChunk.getZ();
            folder = getDataFolder().toPath().resolve("generated-terrain-private-" + UUID.randomUUID());

            session = newSession();
            started = session.start(false);
            new BukkitRunnable() {
                @Override public void run() {
                    try { advance(); }
                    catch (Throwable failure) { failProbe(failure); }
                }
            }.runTaskTimer(this, 1, 1);
        } catch (Throwable failure) { failProbe(failure); }
    }

    private PopulationSession newSession() {
        PopulationSession next = new PopulationSession(folder.toFile(), getLogger(),
                PopulationSession.Settings.prototype(), System::currentTimeMillis);
        next.configureOreTracking();
        next.configureForestTracking();
        return next;
    }

    private void advance() throws Exception {
        check(++ticks <= 600, "Generated terrain probe timed out at stage " + stage);
        switch (stage) {
            case 0 -> {
                if (!started.isDone()) return;
                started.join();
                check(session.oreCaptureReady() && session.forestCaptureReady(), "Private current capture must be healthy");
                observer = new GeneratedTerrainObserver(session, getLogger());
                getServer().getPluginManager().registerEvents(observer, this);
                observer.onLoad(new ChunkLoadEvent(existingChunk, false));

                // This is an explicit destructive-to-world setup action authorized only by the marked test fixture.
                // Production observation never requests, force-loads, or generates a chunk.
                int targetX = 10_000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(900_000);
                int targetZ = 10_000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(900_000);
                world.getChunkAt(targetX, targetZ);

                barrier = session.forestExclusions(worldId, oldX, oldZ);
                stage = 1;
            }
            case 1 -> {
                if (!barrier.isDone()) return;
                barrier.join();
                beforeRestart = CompletableFuture.supplyAsync(() -> {
                    try { return readAndVerify(true, null); }
                    catch (Exception failure) { throw new CompletionException(failure); }
                }, worker);
                stage = 2;
            }
            case 2 -> {
                if (!beforeRestart.isDone()) return;
                saved = beforeRestart.join();
                HandlerList.unregisterAll(this);
                PopulationSession closing = session;
                closed = CompletableFuture.runAsync(() -> { closing.close(); session = null; }, worker);
                stage = 3;
            }
            case 3 -> {
                if (!closed.isDone()) return;
                closed.join();
                session = newSession();
                restarted = session.start(false);
                stage = 4;
            }
            case 4 -> {
                if (!restarted.isDone()) return;
                restarted.join();
                check(session.oreCaptureReady() && session.forestCaptureReady(), "Clean restart must preserve healthy current capture");
                CompletableFuture<Snapshot> afterRestart = CompletableFuture.supplyAsync(() -> {
                    try { return readAndVerify(true, saved.certificates()); }
                    catch (Exception failure) { throw new CompletionException(failure); }
                }, worker);
                // The verification runs off the Paper thread; continue polling it in the next stage.
                beforeRestart = afterRestart;
                stage = 5;
            }
            case 5 -> {
                if (!beforeRestart.isDone()) return;
                beforeRestart.join();
                session.invalidateOreTracking("Generated terrain probe revoked capture");
                session.invalidateForestTracking("Generated terrain probe revoked capture");
                PopulationSession closing = session;
                finalClose = CompletableFuture.runAsync(() -> {
                    closing.close();
                    session = null;
                    try { readAndVerify(false, saved.certificates()); }
                    catch (Exception failure) { throw new CompletionException(failure); }
                }, worker);
                stage = 6;
            }
            case 6 -> {
                if (!finalClose.isDone()) return;
                finalClose.join();
                getLogger().info("NATIVE GENERATED TERRAIN CAPTURE PASS: real new-chunk events saved current ore and forest certificates; existing-chunk load created none; clean restart preserved coverage; a capture gap revoked coverage without deleting certificates; private database integrity and foreign keys passed");
                Bukkit.getScheduler().cancelTasks(this);
                getServer().shutdown();
            }
            default -> throw new IllegalStateException("Unexpected generated terrain probe stage " + stage);
        }
    }

    private Snapshot readAndVerify(boolean expectedCoverage, Set<Certificate> expectedRows) throws Exception {
        String url = "jdbc:sqlite:" + folder.resolve("civilizations.db").toAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url)) {
            try (var statement = connection.createStatement()) { statement.execute("PRAGMA foreign_keys=ON"); }
            var epochs = new ResourceRecoveryEpochs(connection);
            var ore = epochs.load("ore").orElseThrow();
            var forest = epochs.load("forest").orElseThrow();
            List<Certificate> rows = new ArrayList<>();
            try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                    SELECT kind,world_id,chunk_x,chunk_z,generation,observed_ms
                    FROM generated_resource_chunks ORDER BY kind,world_id,chunk_x,chunk_z
                    """)) {
                while (result.next()) {
                    Certificate row = new Certificate(result.getString(1), UUID.fromString(result.getString(2)),
                            result.getInt(3), result.getInt(4), UUID.fromString(result.getString(5)), result.getLong(6));
                    check(row.world().equals(worldId), "Saved certificate must retain the observed permanent world UUID");
                    if (expectedCoverage) {
                        check(row.kind().equals("ore") && row.generation().equals(ore.generation()) && ore.healthy()
                                        || row.kind().equals("forest") && row.generation().equals(forest.generation()) && forest.healthy(),
                                "Every certificate must match its healthy current resource epoch");
                    }
                    check(!(row.x() == oldX && row.z() == oldZ), "Ordinary load must not certify the existing chunk");
                    rows.add(row);
                }
            }
            Set<Certificate> certificates = Set.copyOf(rows);
            check(certificates.stream().anyMatch(row -> row.kind().equals("ore"))
                            && certificates.stream().anyMatch(row -> row.kind().equals("forest")),
                    "At least one newly generated chunk must be certified for both resource kinds");
            if (expectedRows != null) check(certificates.equals(expectedRows), "Certificate rows must survive clean restart and gap unchanged");
            check(scalar(connection, "SELECT COUNT(*) FROM observed_worlds WHERE id='" + worldId + "' AND name='" + worldName + "'") == 1,
                    "The saved observation must preserve the real world's stable identity and name");
            check(scalar(connection, "SELECT COUNT(*) FROM businesses") == 0
                            && scalar(connection, "SELECT COUNT(*) FROM residents") == 0
                            && scalar(connection, "SELECT COUNT(*) FROM industry_stores") == 0
                            && scalar(connection, "SELECT COUNT(*) FROM industry_stock") == 0,
                    "Generated terrain probe must not seed people, businesses, goods, or stores");
            if (expectedCoverage) {
                Certificate witness = rows.stream().filter(row -> row.kind().equals("ore")).findFirst().orElseThrow();
                var position = new Position(Math.multiplyExact(witness.x(), 16) + 1, 64,
                        Math.multiplyExact(witness.z(), 16) + 1);
                check(new GeneratedTerrainRepository(connection).covers("ore", worldId, List.of(position)),
                        "A saved generated chunk must satisfy coverage for its current epoch");
            } else {
                Certificate witness = saved.certificates().stream().filter(row -> row.kind().equals("ore")).findFirst().orElseThrow();
                var position = new Position(Math.multiplyExact(witness.x(), 16) + 1, 64,
                        Math.multiplyExact(witness.z(), 16) + 1);
                check(!ore.healthy() && !forest.healthy(), "Both resource epochs must be unhealthy after capture invalidation");
                check(!new GeneratedTerrainRepository(connection).covers("ore", worldId, List.of(position))
                                && !new GeneratedTerrainRepository(connection).covers("forest", worldId, List.of(position)),
                        "Revoked epochs must stop granting coverage while keeping old certificate rows");
            }
            check(scalar(connection, "SELECT COUNT(*) FROM generated_resource_chunks") == certificates.size(),
                    "Certificate row count must not change during restart or revocation");
            check("ok".equals(textScalar(connection, "PRAGMA integrity_check")), "Private database integrity check must pass");
            try (var statement = connection.createStatement(); var result = statement.executeQuery("PRAGMA foreign_key_check")) {
                check(!result.next(), "Private database must have no foreign-key violations");
            }
            return new Snapshot(certificates);
        }
    }

    private static long scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            if (!result.next()) throw new SQLException("Missing scalar result for " + sql);
            return result.getLong(1);
        }
    }

    private static String textScalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            if (!result.next()) throw new SQLException("Missing scalar result for " + sql);
            return result.getString(1);
        }
    }

    private void failProbe(Throwable failure) {
        getLogger().log(Level.SEVERE, "NATIVE GENERATED TERRAIN CAPTURE FAIL", failure);
        getServer().getPluginManager().disablePlugin(this);
        if (authorized) getServer().shutdown();
    }

    @Override public void onDisable() {
        HandlerList.unregisterAll(this);
        PopulationSession closing = session;
        if (closing != null) {
            CompletableFuture.runAsync(closing::close, worker).whenComplete((ignored, failure) -> {
                if (failure != null) getLogger().log(Level.SEVERE, "Private population session close failed", failure);
                worker.shutdown();
            });
            session = null;
        } else worker.shutdown();
    }
}
