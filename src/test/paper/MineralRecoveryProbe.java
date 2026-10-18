import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.Business;
import com.blueninjas24.civilizations.industry.BusinessFounding;
import com.blueninjas24.civilizations.industry.BusinessSite;
import com.blueninjas24.civilizations.population.JobRequirements;
import com.blueninjas24.civilizations.industry.MineralObservation;
import com.blueninjas24.civilizations.paper.PaperMineralRecovery;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.Settlement;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Disposable-only full native review/confirm probe against a private database. Never ship. */
public final class MineralRecoveryProbe extends JavaPlugin {
    private final ExecutorService fixtureWorker = Executors.newSingleThreadExecutor();
    private final List<String> messages = new ArrayList<>();
    private final Map<VillageObservation.Position, BlockData> originals = new LinkedHashMap<>();
    private PopulationSession session;
    private PaperMineralRecovery recovery;
    private World world;
    private String worldName;
    private Path folder;
    private UUID worldId, town, resident, site, business, deposit;
    private List<VillageObservation.Position> positions;
    private int ticks, stage;
    private boolean authorized, blocksPrepared;
    private int ticketX,ticketZ;
    private boolean fixtureTicket;
    private CompletableFuture<Void> seeded, started;
    private String confirmationToken;

    private void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    @Override public void onEnable() {
        try {
            var root = getServer().getWorldContainer().toPath().toRealPath();
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
            var spawn = world.getSpawnLocation();
            int chunkX = Math.floorDiv(spawn.getBlockX(), 16);
            int chunkZ = Math.floorDiv(spawn.getBlockZ(), 16);
            // Test setup only: production recovery never creates tickets or loads missing chunks.
            ticketX=chunkX;ticketZ=chunkZ;
            fixtureTicket=world.addPluginChunkTicket(chunkX,chunkZ,this);
            check(fixtureTicket&&world.isChunkLoaded(chunkX, chunkZ), "Disposable fixture must own its one test chunk ticket");
            int y = world.getMaxHeight() - 16;
            int firstX = chunkX * 16 + 3;
            int z = chunkZ * 16 + 8;
            positions = List.of(pos(firstX, y, z), pos(firstX + 1, y, z), pos(firstX + 2, y, z), pos(firstX + 3, y, z));
            folder = getDataFolder().toPath().resolve("mineral-recovery-private-" + UUID.randomUUID());
            town = id("recovery-town");
            resident = id("recovery-resident");
            site = id("recovery-site");
            business = id("recovery-mine");
            deposit = UUID.nameUUIDFromBytes(("civilizations:observed-mineral-v1:" + worldId + ":coal:" + firstX + ":" + y + ":" + z)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));

            var matches = Class.forName("com.blueninjas24.civilizations.paper.PaperMineralRecovery")
                    .getDeclaredMethod("matches", String.class, Material.class);
            matches.setAccessible(true);
            check((boolean) matches.invoke(null, "coal", Material.COAL_ORE), "Coal ore must match saved coal");
            check((boolean) matches.invoke(null, "coal", Material.DEEPSLATE_COAL_ORE), "Deepslate coal must match saved coal");
            check(!(boolean) matches.invoke(null, "coal", Material.IRON_ORE), "Iron ore must not match saved coal");

            for (var position : positions) {
                check(world.isChunkLoaded(Math.floorDiv(position.x(), 16), Math.floorDiv(position.z(), 16)),
                        "Every fixture coordinate must remain in an already-loaded chunk");
                var block = world.getBlockAt(position.x(), position.y(), position.z());
                check(block.getType() == Material.AIR, "Every fixture coordinate must begin in verified AIR");
                originals.put(position, block.getBlockData().clone());
            }

            seeded = CompletableFuture.runAsync(() -> {
                try { seedPrivateDatabase(); }
                catch (Exception failure) { throw new CompletionException(failure); }
            }, fixtureWorker);
            new BukkitRunnable() {
                @Override public void run() {
                    try { advanceProbe(); }
                    catch (Exception failure) { failProbe("NATIVE MINERAL RECOVERY FAIL", failure); }
                }
            }.runTaskTimer(this, 1, 1);
        } catch (Exception failure) {
            failProbe("Mineral recovery fixture refused", failure);
        }
    }

    private VillageObservation.Position pos(int x, int y, int z) {
        return new VillageObservation.Position(x, y, z);
    }

    private UUID id(String name) {
        return UUID.nameUUIDFromBytes(("mineral-recovery-probe:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void advanceProbe() throws Exception {
        check(++ticks <= 400, "Mineral recovery probe timed out at stage " + stage);
        switch (stage) {
            case 0 -> {
                if (!seeded.isDone()) return;
                seeded.join();
                for (int i = 0; i < positions.size() - 1; i++)
                    world.getBlockAt(positions.get(i).x(), positions.get(i).y(), positions.get(i).z()).setType(Material.COAL_ORE, false);
                blocksPrepared = true;
                session = new PopulationSession(folder.toFile(), getLogger(), PopulationSession.Settings.prototype(), System::currentTimeMillis);
                session.configureOreTracking();
                started = session.start(false, Set.of(worldId)).thenApply(ignored -> null);
                stage = 1;
            }
            case 1 -> {
                if (!started.isDone()) return;
                started.join();
                check(session.oreCaptureReady(), "Restarted private session must have healthy current capture");
                check(!session.oreDiscoveryReady(), "Historical global discovery must remain fenced");
                recovery = new PaperMineralRecovery(this, session);
                invoke("list");
                stage = 2;
            }
            case 2 -> {
                // The list result is asynchronous and read-only. Wait for it before reviewing the saved ID.
                if (messages.stream().noneMatch(line -> line.startsWith("Saved mineral deposit IDs"))) return;
                invoke("review " + deposit);
                stage = 3;
            }
            case 3 -> {
                if (confirmationToken == null) {
                    for (var line : messages) if (line.startsWith("Approve within120 seconds: /civ debug recover-ore confirm "))
                        confirmationToken = line.substring(line.lastIndexOf(' ') + 1);
                    if (confirmationToken == null) return;
                    check(messages.stream().anyMatch(line -> line.contains("4 saved coordinates") && line.contains("confirmed loss 25")),
                            "Review must cover all four saved coordinates and report exactly 25 lost batches");
                    invoke("confirm " + confirmationToken);
                    stage = 4;
                }
            }
            case 4 -> {
                if (messages.stream().noneMatch(line -> line.contains("reserve 100 -> 75; lost 25"))) return;
                check(session.oreCaptureReady(), "Current live capture must remain ready after approval");
                check(!session.oreDiscoveryReady(), "Approval must not reopen global discovery");
                verifyPrivateDatabase();
                session.close();
                session = null;
                getLogger().info("NATIVE MINERAL RECOVERY PASS: private saved deposit reviewed across four loaded coordinates; explicit confirmation retired only the AIR source share (100 -> 75); global discovery stayed fenced; world blocks will be restored");
                cancelTasks();
                getServer().getPluginManager().disablePlugin(MineralRecoveryProbe.this);
                getServer().shutdown();
            }
            default -> throw new IllegalStateException("Unexpected mineral recovery probe stage " + stage);
        }
    }

    private void invoke(String operation) {
        var sender = (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> true;
                    case "sendMessage" -> {
                        check(Bukkit.isPrimaryThread(), "Recovery messages must be delivered on Paper's main thread");
                        messages.add((String) args[0]);
                        yield null;
                    }
                    default -> throw new AssertionError("Unexpected sender access: " + method.getName());
                });
        recovery.onCommand(sender, getServer().getPluginCommand("civ"), "civ",
                ("debug recover-ore " + operation).split(" "));
    }

    private void seedPrivateDatabase() throws Exception {
        var db = new DatabaseManager(folder.toFile(), getLogger());
        try {
            db.initialize();
            db.saveSettlement(new Settlement(town, "Private mineral recovery fixture", worldName, 0, 64, 0, 1, 0, 0));
            db.getVillageIdentityRepository().observeWorld(worldId, worldName);
            db.getVillageIdentityRepository().bindSettlement(town, worldId);
            db.getPopulationRepository().insertResident(new Resident(resident, "Fixture miner", town, null, 300, Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(id("treasury"), town, 10_000, "Private probe fixture funds");
            var position = positions.getFirst();
            var businessSite = new BusinessSite(site, town, worldId, position.x(), position.y(), position.z(), 1,
                    "Private saved mineral recovery fixture");
            var mine = new Business(business, town, "Private fixture mine", resident, resident, "mining", site, 1, 0);
            var requirements = new JobRequirements(216, Map.of());
            db.getBusinessRepository().found(new BusinessFounding(mine, businessSite, requirements, requirements, 0,
                    "Private probe resource anchor"));
            db.getBusinessRepository().hire(id("mine-worker"), business, resident, 0);
            db.getIndustryRepository().openStore(business, 10_000, Map.of(Commodity.COAL, 1L, Commodity.STONE, 1L), 1_000);

            new OreTrackingRepository(db.getConnection()).begin(System.currentTimeMillis());
            var observation = new MineralObservation(worldId, System.currentTimeMillis(), true, Commodity.COAL, positions);
            var found = new MineralDiscoveryRepository(db.getConnection()).discover(site, observation, 25,
                    Map.of(Commodity.COAL, 1L, Commodity.STONE, 2L));
            check(found.created() && found.deposit().remainingBatches() == 100, "Private four-coordinate deposit must start at 100 batches");
            deposit=found.deposit().id();
            var tracking = new OreTrackingRepository(db.getConnection());
            tracking.markGap(System.currentTimeMillis());
            tracking.finish(System.currentTimeMillis(), true);
            check(!tracking.load().orElseThrow().trusted(), "Seed must retain globally untrusted historical capture");
        } finally {
            db.close();
        }
    }

    private void verifyPrivateDatabase() throws SQLException {
        try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+folder.resolve("civilizations.db").toUri()+"?mode=ro")) {
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");s.execute("PRAGMA busy_timeout=5000");}
            check(!new OreTrackingRepository(c).load().orElseThrow().trusted(),
                    "Global ore trust must remain false after recovery");
            check(new MineralRecovery(c).permitted(deposit), "Reviewed deposit must be permitted in current epoch");
            check(new IndustryRepository(c).deposit(deposit).orElseThrow().remainingBatches() == 75,
                    "Only the AIR coordinate's 25-batch share may be retired");
            check(count(c, "mineral_recoveries") == 1, "Exactly one recovery audit row required");
            check(count(c, "mineral_source_retirements") == 1, "Exactly one source retirement required");
            try (var statement = c.createStatement(); var result = statement.executeQuery("PRAGMA foreign_keys")) {
                check(result.next() && result.getInt(1) == 1, "Private database foreign-key enforcement must remain enabled");
            }
        }
    }

    private long count(java.sql.Connection c, String table) throws SQLException {
        try (var statement = c.createStatement(); var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            check(result.next(), "Count query must return a row for " + table);
            return result.getLong(1);
        }
    }

    private void restoreBlocks() {
        if (!blocksPrepared || world == null) return;
        for (var entry : originals.entrySet()) {
            var position = entry.getKey();
            check(world.isChunkLoaded(Math.floorDiv(position.x(), 16), Math.floorDiv(position.z(), 16)),
                    "Fixture chunk must remain loaded for exact block restoration");
            var block = world.getBlockAt(position.x(), position.y(), position.z());
            block.setBlockData(entry.getValue(), false);
            check(block.getBlockData().getAsString().equals(entry.getValue().getAsString()),
                    "Every original fixture block state must be restored exactly");
        }
        originals.clear();
        blocksPrepared = false;
        getLogger().info("MINERAL RECOVERY BLOCK RESTORATION PASS: all four original AIR states restored exactly");
    }

    private void cancelTasks() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    private void failProbe(String marker, Throwable failure) {
        getLogger().log(java.util.logging.Level.SEVERE, marker, failure);
        getServer().getPluginManager().disablePlugin(this);
        if (authorized) getServer().shutdown();
    }

    @Override public void onDisable() {
        try { restoreBlocks(); }
        catch (RuntimeException failure) { getLogger().log(java.util.logging.Level.SEVERE, "MINERAL RECOVERY BLOCK RESTORATION FAILED", failure); }
        finally {
            if(fixtureTicket&&world!=null){world.removePluginChunkTicket(ticketX,ticketZ,this);fixtureTicket=false;}
            if (session != null) session.close();
            fixtureWorker.shutdown();
        }
    }
}
