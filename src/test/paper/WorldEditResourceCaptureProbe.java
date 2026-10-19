import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.WorldEditResourceCapture;
import com.blueninjas24.civilizations.population.JobRequirements;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.Settlement;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Disposable native WorldEdit hook proof. This helper is excluded from the plugin jar. */
public final class WorldEditResourceCaptureProbe extends JavaPlugin {
    private final ExecutorService fixtureWorker = Executors.newSingleThreadExecutor();
    private volatile PopulationSession session;
    private WorldEditResourceCapture capture;
    private World world;
    private String worldName;
    private Path folder;
    private List<Position> positions;
    private Block[] blocks;
    private BlockData[] originals;
    private UUID worldId, town, resident, site, business, deposit;
    private int chunkX, chunkZ, stage, ticks;
    private boolean authorized, fixturePrepared, ticketed, restored;
    private CompletableFuture<Void> seeded, started, offThreadFence, stopped;
    private CompletableFuture<Set<Position>> oreRows, forestRows;
    private CompletableFuture<Boolean> generationProof;

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
            authorized = true;
            world = getServer().getWorlds().getFirst();
            worldId = world.getUID();
            worldName = world.getName();
            var loaded = List.of(world.getLoadedChunks()).stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Console-loaded generated fixture chunk required"));
            chunkX = loaded.getX(); chunkZ = loaded.getZ();
            ticketed = loaded.addPluginChunkTicket(this);
            check(ticketed && world.isChunkLoaded(chunkX, chunkZ), "Probe must hold one ticket on its already-loaded fixture chunk");
            int y = world.getMaxHeight() - 16;
            int x = chunkX * 16 + 4;
            int z = chunkZ * 16 + 8;
            positions = List.of(new Position(x, y, z), new Position(x + 1, y, z), new Position(x + 2, y, z),
                    new Position(x + 3, y, z), new Position(x + 4, y, z));
            blocks = positions.stream().map(p -> world.getBlockAt(p.x(), p.y(), p.z())).toArray(Block[]::new);
            originals = new BlockData[blocks.length];
            for (int i = 0; i < blocks.length; i++) {
                check(blocks[i].getType() == Material.AIR, "Each disposable marker coordinate must begin in AIR");
                originals[i] = blocks[i].getBlockData().clone();
            }
            fixturePrepared = true;
            blocks[0].setType(Material.COAL_ORE, false);
            blocks[1].setType(Material.COAL_ORE, false);

            folder = getDataFolder().toPath().resolve("worldedit-resource-private-" + UUID.randomUUID());
            town = id("town"); resident = id("resident"); site = id("site"); business = id("mine");
            seeded = CompletableFuture.runAsync(() -> {
                try { seedPrivateDatabase(); }
                catch (Exception failure) { throw new CompletionException(failure); }
            }, fixtureWorker);
            new BukkitRunnable() {
                @Override public void run() {
                    try { advance(); }
                    catch (Throwable failure) { failProbe("NATIVE WORLDEDIT RESOURCE CAPTURE FAIL", failure); }
                }
            }.runTaskTimer(this, 1, 1);
        } catch (Throwable failure) {
            failProbe("WorldEdit resource fixture refused", failure);
        }
    }

    private UUID id(String name) {
        return UUID.nameUUIDFromBytes(("worldedit-resource-capture-probe:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void seedPrivateDatabase() throws Exception {
        DatabaseManager db = new DatabaseManager(folder.toFile(), getLogger());
        try {
            db.initialize();
            var first = positions.getFirst();
            db.saveSettlement(new Settlement(town, "Private WorldEdit capture fixture", worldName,
                    first.x(), first.y(), first.z(), 1, 0, 0));
            db.getVillageIdentityRepository().observeWorld(worldId, worldName);
            db.getVillageIdentityRepository().bindSettlement(town, worldId);
            db.getPopulationRepository().insertResident(new Resident(resident, "Fixture miner", town, null, 300, Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(id("treasury"), town, 2_000,
                    "Private native WorldEdit fixture funds");
            long now = System.currentTimeMillis();
            var siteRecord = new BusinessSite(site, town, worldId, first.x(), first.y(), first.z(), now,
                    "Explicit private native resource anchor");
            var requirements = new JobRequirements(216, Map.of());
            var mine = new Business(business, town, "Private fixture mine", resident, resident, "mining", site, 1, 0);
            var founding = new BusinessFounding(mine, siteRecord, requirements, requirements, 500,
                    "Explicit private native resource anchor");
            var evidence = new MineralObservation(worldId, now, true, Commodity.COAL, positions.subList(0, 2));
            var request = new MiningStartupRepository.Request(founding, evidence, 10,
                    Map.of(Commodity.COAL, 1L), 10_000, Map.of(Commodity.COAL, 1L), 1_000, resident);
            var started = new MiningStartupRepository(db.getConnection()).start(request);
            deposit = started.discovery().deposit().id();
            check(started.discovery().created() && started.discovery().deposit().remainingBatches() == 20,
                    "Private startup must save only the two fixture coal blocks at ten batches each");

            new OreTrackingRepository(db.getConnection()).begin(now);
            new OreTrackingRepository(db.getConnection()).finish(now + 1, true);
            new ForestryTrackingRepository(db.getConnection()).begin(now);
            new ForestryTrackingRepository(db.getConnection()).finish(now + 1, true);
            new OreTrackingRepository(db.getConnection()).markGap(now + 2);
            new ForestryTrackingRepository(db.getConnection()).markGap(now + 2);
        } finally { db.close(); }
    }

    private void advance() throws Exception {
        check(++ticks <= 600, "WorldEdit capture probe timed out at stage " + stage);
        switch (stage) {
            case 0 -> {
                if (!seeded.isDone()) return;
                seeded.join();
                var plugin=getServer().getPluginManager().getPlugin("Civilizations");
                var ownerField=plugin.getClass().getDeclaredField("population");ownerField.setAccessible(true);
                var ordinary=(PopulationSession)ownerField.get(plugin);
                if(!ordinary.status().ready())return;
                check(ordinary.oreCaptureReady()&&ordinary.forestCaptureReady()
                        &&!ordinary.oreDiscoveryReady()&&!ordinary.forestDiscoveryReady(),
                        "Normal startup must install current WorldEdit capture while quarantining bootstrap history");
                int freshX=2048,freshZ=-2048;
                check(!world.isChunkGenerated(freshX,freshZ),"Fresh editor fixture chunk must not exist");
                world.getChunkAt(freshX,freshZ); // Explicit disposable-fixture generation, never production simulation.
                long observed=System.currentTimeMillis();
                generationProof=ordinary.canSurveyResourceChunk("ore",worldId,freshX,freshZ,observed)
                        .thenCombine(ordinary.canSurveyResourceChunk("forest",worldId,freshX,freshZ,observed),(ore,forest)->ore&&forest)
                        .thenCombine(ordinary.canSurveyResourceChunk("ore",worldId,chunkX,chunkZ,observed),(fresh,old)->fresh&&!old);
                session = new PopulationSession(folder.toFile(), getLogger(), PopulationSession.Settings.prototype(), System::currentTimeMillis);
                session.configureOreTracking();
                session.configureForestTracking();
                started = session.start(false, Set.of(worldId)).thenApply(ignored -> null);
                stage = 1;
            }
            case 1 -> {
                if (!started.isDone()) return;
                started.join();
                if(!generationProof.isDone())return;
                check(generationProof.join(),"Ordinary WorldEdit boot must authorize only actual newly generated terrain");
                check(session.oreCaptureReady() && session.forestCaptureReady()
                                && !session.oreDiscoveryReady() && !session.forestDiscoveryReady(),
                        "Legacy history must stay untrusted while current capture remains available");
                capture = new WorldEditResourceCapture(session, getLogger());
                capture.register(true); // The synthetic fixture deliberately keeps legacy history untrusted.
                runWorldEditChanges();
                oreRows = session.orePlacementsInChunk(worldId, chunkX, chunkZ, 512);
                forestRows = session.forestExclusions(worldId, chunkX, chunkZ);
                stage = 2;
            }
            case 2 -> {
                if (!oreRows.isDone() || !forestRows.isDone()) return;
                Set<Position> ore = oreRows.join(), forest = forestRows.join();
                check(ore.contains(positions.get(2)) && ore.contains(positions.get(4)),
                        "WorldEdit raw iron and the first stone placement must be excluded");
                check(forest.contains(positions.get(3)), "WorldEdit tree placement and undo must retain forest exclusion");
                check(!forest.contains(positions.get(4)), "Ordinary stone must not create forest provenance");
                check(session.oreCaptureReady() && session.forestCaptureReady()
                                && !session.oreDiscoveryReady() && !session.forestDiscoveryReady(),
                        "Supported synchronous edits must preserve current capture without trusting legacy history");
                capture.onCommand(new com.sk89q.worldedit.event.platform.CommandEvent(
                        BukkitAdapter.adapt(getServer().getConsoleSender()),"/regen"));
                check(!session.oreCaptureReady()&&!session.forestCaptureReady(),
                        "Unreviewed non-extent command must fence before any operation executes");
                check(blocks[0].getType() == Material.AIR && blocks[1].getType() == Material.COAL_ORE
                                && blocks[2].getType() == Material.IRON_ORE && blocks[3].getType() == Material.AIR
                                && blocks[4].getType() == Material.STONE,
                        "WorldEdit edits and undo must leave the expected fixture state before restoration");

                var event = new EditSessionEvent(BukkitAdapter.adapt(world), null, -1, EditSession.Stage.BEFORE_CHANGE);
                offThreadFence = CompletableFuture.runAsync(() -> capture.onEditSession(event), fixtureWorker);
                stage = 3;
            }
            case 3 -> {
                if (!offThreadFence.isDone()) return;
                offThreadFence.join();
                check(!session.oreCaptureReady() && !session.forestCaptureReady(),
                        "Detached off-thread hook invocation must fence both resource captures");
                capture.close();
                PopulationSession closing = session;
                stopped = CompletableFuture.runAsync(() -> {
                    closing.close();
                    session = null;
                    try { verifySavedDatabase(); }
                    catch (Exception failure) { throw new CompletionException(failure); }
                }, fixtureWorker);
                stage = 4;
            }
            case 4 -> {
                if (!stopped.isDone()) return;
                stopped.join();
                getLogger().info("NATIVE WORLDEDIT RESOURCE CAPTURE PASS: ordinary bootstrap quarantined older terrain and certified actual fresh generation; supported edit session, raw iron placement and one stone placement saved ore exclusions; actual removal retired only its ten-batch source share; tree placement and undo remained excluded; unchanged repeats added no duplicate provenance; unsupported command and off-thread fences retained; private edits created no generated terrain certificates or bonus goods");
                Bukkit.getScheduler().cancelTasks(this);
                getServer().shutdown();
            }
            default -> throw new IllegalStateException("Unexpected WorldEdit capture probe stage " + stage);
        }
    }

    private void runWorldEditChanges() throws Exception {
        var adapted = BukkitAdapter.adapt(world);
        try (EditSession edit = WorldEdit.getInstance().newEditSession(adapted)) {
            edit.setBlock(vector(positions.get(1)), BlockTypes.COAL_ORE.getDefaultState());
            edit.setBlock(vector(positions.get(0)), BlockTypes.AIR.getDefaultState());
            edit.rawSetBlock(vector(positions.get(2)), BlockTypes.IRON_ORE.getDefaultState());
        }
        EditSession edit = WorldEdit.getInstance().newEditSession(adapted);
        try { edit.setBlock(vector(positions.get(3)), BlockTypes.OAK_LOG.getDefaultState()); }
        finally { edit.close(); }
        try (EditSession undo = WorldEdit.getInstance().newEditSession(adapted)) {
            // Flush the original session before applying its inverse in a fresh hooked session.
            edit.undo(undo);
        }
        try (EditSession ordinary = WorldEdit.getInstance().newEditSession(adapted)) {
            ordinary.setBlock(vector(positions.get(4)), BlockTypes.STONE.getDefaultState());
            ordinary.setBlock(vector(positions.get(4)), BlockTypes.STONE.getDefaultState());
        }
    }

    private static BlockVector3 vector(Position position) {
        return BlockVector3.at(position.x(), position.y(), position.z());
    }

    private void verifySavedDatabase() throws Exception {
        String url = "jdbc:sqlite:" + folder.resolve("civilizations.db").toAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url)) {
            try (var statement = connection.createStatement()) { statement.execute("PRAGMA foreign_keys=ON"); }
            check(scalar(connection, "SELECT remaining_batches FROM mineral_deposits WHERE id='" + deposit + "'") == 10,
                    "Only the removed coal source share should be retired");
            Position second = positions.get(1);
            check(scalar(connection, "SELECT remaining_batches FROM mineral_sources WHERE deposit_id='" + deposit
                            + "' AND world_id='" + worldId + "' AND x=" + second.x() + " AND y=" + second.y() + " AND z=" + second.z()) == 10,
                    "Untouched second coal source must retain all ten batches");
            check(scalar(connection, "SELECT COUNT(*) FROM mineral_source_retirements") == 1,
                    "Exactly one saved source retirement is expected");
            Position iron = positions.get(2), tree = positions.get(3), stone = positions.get(4);
            check(scalar(connection, "SELECT COUNT(*) FROM player_ore_placements WHERE world_id='" + worldId + "' AND x="
                    + iron.x() + " AND y=" + iron.y() + " AND z=" + iron.z()) == 1,
                    "Raw-set iron ore must have a saved ore exclusion");
            check(scalar(connection, "SELECT COUNT(*) FROM forestry_exclusions WHERE world_id='" + worldId + "' AND x="
                    + tree.x() + " AND y=" + tree.y() + " AND z=" + tree.z()) == 1,
                    "Tree placement and undo must retain its exclusion");
            check(scalar(connection, "SELECT COUNT(*) FROM ore_source_removals WHERE world_id='" + worldId + "' AND x="
                    + positions.get(0).x() + " AND y=" + positions.get(0).y() + " AND z=" + positions.get(0).z()) == 1,
                    "Actual source removal must be recorded once");
            check(scalar(connection, "SELECT COUNT(*) FROM player_ore_placements WHERE world_id='" + worldId + "' AND x="
                    + stone.x() + " AND y=" + stone.y() + " AND z=" + stone.z()) == 1
                            && scalar(connection, "SELECT COUNT(*) FROM forestry_exclusions WHERE world_id='" + worldId + "' AND x="
                            + stone.x() + " AND y=" + stone.y() + " AND z=" + stone.z()) == 0,
                    "The first finite stone placement must be excluded exactly once");
            check(scalar(connection, "SELECT COUNT(*) FROM generated_resource_chunks") == 0,
                    "WorldEdit activity must not certify new terrain generation");
            check(scalar(connection, "SELECT COALESCE(SUM(units),0) FROM industry_stock WHERE commodity='coal'") == 0,
                    "Provenance updates must not grant or produce coal goods");
            check(scalar(connection, "SELECT hundredths FROM treasuries WHERE settlement_id='" + town + "'") == 500
                            && scalar(connection, "SELECT hundredths FROM industry_stores WHERE business_id='" + business + "'") == 1_000,
                    "Explicit 500 startup expense and 1,000 working capital must remain balanced");
            check(scalar(connection, "SELECT trusted FROM ore_tracking WHERE id=1") == 0
                            && scalar(connection, "SELECT trusted FROM forestry_tracking WHERE id=1") == 0,
                    "Off-thread hook failure and close must preserve both global trust fences");
            check("ok".equals(textScalar(connection, "PRAGMA integrity_check")), "Private database integrity must pass");
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("PRAGMA foreign_key_check")) {
                check(!rows.next(), "Private database foreign keys must remain valid");
            }
        }
    }

    private static long scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            if (!rows.next()) throw new SQLException("Missing scalar result for " + sql);
            return rows.getLong(1);
        }
    }

    private static String textScalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            if (!rows.next()) throw new SQLException("Missing scalar result for " + sql);
            return rows.getString(1);
        }
    }

    private void failProbe(String marker, Throwable failure) {
        getLogger().log(Level.SEVERE, marker, failure);
        if (authorized) {
            try { restoreFixture(); }
            catch (Throwable restoreFailure) { failure.addSuppressed(restoreFailure); }
            getServer().getPluginManager().disablePlugin(this);
            getServer().shutdown();
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void restoreFixture() {
        if (restored || !fixturePrepared || world == null) return;
        for (int i = 0; i < blocks.length; i++) blocks[i].setBlockData(originals[i], false);
        for (int i = 0; i < blocks.length; i++)
            check(blocks[i].getBlockData().equals(originals[i]), "Original fixture BlockData must be restored exactly");
        restored = true;
        getLogger().info("NATIVE WORLDEDIT RESOURCE CAPTURE RESTORED: all five original AIR BlockData values");
    }

    @Override public void onDisable() {
        if (capture != null) try { capture.close(); }
        catch (RuntimeException failure) { getLogger().log(Level.WARNING, "WorldEdit capture close failed", failure); }
        try { restoreFixture(); }
        catch (RuntimeException failure) { getLogger().log(Level.SEVERE, "WorldEdit fixture restoration failed", failure); }
        if (ticketed && world != null) {
            try { world.getChunkAt(chunkX, chunkZ).removePluginChunkTicket(this); }
            catch (RuntimeException failure) { getLogger().log(Level.WARNING, "Fixture chunk ticket release failed", failure); }
            ticketed = false;
        }
        PopulationSession closing = session;
        if (closing != null) {
            CompletableFuture.runAsync(closing::close, fixtureWorker).whenComplete((ignored, failure) -> {
                if (failure != null) getLogger().log(Level.SEVERE, "Private session close failed", failure);
                fixtureWorker.shutdown();
            });
            session = null;
        } else fixtureWorker.shutdown();
    }
}
