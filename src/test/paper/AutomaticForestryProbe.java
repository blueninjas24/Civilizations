import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Guarded native Paper-to-owner-to-funded-forestry startup proof. Never ship. */
public final class AutomaticForestryProbe extends JavaPlugin {
    private final AtomicLong clock = new AtomicLong(1000);
    private final ExecutorService fixtureWorker = Executors.newSingleThreadExecutor();
    private final List<OreSurveyArea.Chunk> tickets = new ArrayList<>();
    private final Map<Location, BlockData> originals = new LinkedHashMap<>();
    private final List<VillageObservation.Position> expectedTrunks = new ArrayList<>();
    private PopulationSession session;
    private PaperForestObserver observer;
    private World world;
    private Path folder;
    private UUID worldId, town;
    private UUID founder, craftOwner, craftWorker;
    private int chunkX, chunkZ, rootY, ticks, stage;
    private boolean authorized;
    private CompletableFuture<Void> initial, seeded;
    private CompletableFuture<LocalIndustryDemand.Review> customerReview;
    private CompletableFuture<PopulationSession.Status> foundingPulse;
    private YamlConfiguration config;
    private PopulationSession.Settings populationSettings;
    private EconomySettings economySettings;
    private IndustryRuntimePolicy industry;
    private ForestryPolicy forestry;
    private ForestryFoundingPolicy founding;
    private StartupGrantPolicy grants;
    private ForestryObserverPolicy observerPolicy;
    private CompletableFuture<long[]> chainMetrics;
    private int chainSteps;

    private void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private VillageObservation.Position pos(int x, int y, int z) {
        return new VillageObservation.Position(x, y, z);
    }

    private YamlConfiguration defaults() throws Exception {
        var civilizations = Objects.requireNonNull(getServer().getPluginManager().getPlugin("Civilizations"),
                "Civilizations plugin required");
        var stream = Objects.requireNonNull(civilizations.getResource("economy.yml"), "Packaged economy.yml required");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @Override public void onEnable() {
        try {
            var root = getServer().getWorldContainer().toPath().toRealPath();
            check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim()
                            .equals("Disposable Civilizations Phase 1 test environment")
                            && getServer().getIp().equals("127.0.0.1")
                            && getServer().getPort() == 25578
                            && getServer().getOnlinePlayers().isEmpty(),
                    "Marked disposable localhost fixture with zero players only");
            authorized = true;
            world = getServer().getWorlds().getFirst();
            worldId = world.getUID();
            var spawn = world.getSpawnLocation();
            chunkX = Math.floorDiv(spawn.getBlockX(), 16);
            chunkZ = Math.floorDiv(spawn.getBlockZ(), 16);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                var chunk = new OreSurveyArea.Chunk(chunkX + dx, chunkZ + dz);
                check(world.addPluginChunkTicket(chunk.x(), chunk.z(), this), "Fixture must own its explicit 3x3 chunk tickets");
                tickets.add(chunk);
                check(world.isChunkLoaded(chunk.x(), chunk.z()), "Ticketed fixture chunk must remain loaded");
            }

            config = defaults();
            config.set("pace.seconds-per-period", 1);
            config.set("starting-units-per-resident.timber", 0);
            config.set("industry-runtime.real-millis-per-period", 1000);
            config.set("business-tools.enabled", false);
            config.set("storage-upgrades.enabled", false);
            economySettings = EconomyConfiguration.read(config);
            industry = EconomyConfiguration.readIndustryRuntime(config);
            forestry = EconomyConfiguration.readForestryResources(config);
            founding = EconomyConfiguration.readForestryFounding(config);
            grants = EconomyConfiguration.readStartupGrants(config);
            observerPolicy = new ForestryObserverPolicy(true, 0, 24, 4096, 20, ForestScan.Geometry.prototype());
            check(founding.enabled() && forestry.enabled() && industry.enabled(), "Packaged forestry startup defaults must be enabled");
            check(industry.recipes().values().stream().anyMatch(recipe -> recipe.industry().equals("craftsman")
                            && recipe.inputs().getOrDefault(Commodity.TIMBER, 0L) > 0),
                    "A real configured craftsman timber recipe is required");

            var populationDefaults = PopulationSession.Settings.prototype();
            populationSettings = new PopulationSession.Settings(populationDefaults.seed(), 1000, 0,
                    populationDefaults.monthsPerBatch(), populationDefaults.population(), populationDefaults.migration(),
                    populationDefaults.recognition(), populationDefaults.enrollment());
            folder = getDataFolder().toPath().resolve("run-" + UUID.randomUUID());
            rootY = world.getMaxHeight() - 16;
            int firstX = chunkX * 16 + 3, secondX = chunkX * 16 + 10, z = chunkZ * 16 + 8;
            var homeA = pos(chunkX * 16 + 1, rootY, z);
            var homeB = pos(chunkX * 16 + 2, rootY + 1, z + 1);
            var homeC = pos(chunkX * 16 + 2, rootY, z - 1);
            var people = List.of(
                    new VillageObservation.Person(id("forestry-founder"), homeA, true, "Fixture forestry founder"),
                    new VillageObservation.Person(id("craft-owner"), homeB, true, "Fixture craftsman owner"),
                    new VillageObservation.Person(id("craft-worker"), homeC, true, "Fixture craftsman worker"));
            var village = new VillageObservation(worldId, "Automatic forestry fixture", 1000, true,
                    List.of(homeA, homeB, homeC), people);

            session = new PopulationSession(folder.toFile(), getLogger(), populationSettings, clock::get);
            session.configureForestTracking();
            initial = session.start(false, Set.of(worldId)).thenCompose(ignored -> session.observe(village)).thenApply(ignored -> null);
            new BukkitRunnable() {
                @Override public void run() {
                    try { advanceProbe(); }
                    catch (Exception failure) { failProbe("NATIVE AUTOMATIC FORESTRY FAIL", failure); }
                }
            }.runTaskTimer(this, 1, 1);
        } catch (Exception failure) {
            failProbe("Automatic forestry fixture refused", failure);
        }
    }

    private void advanceProbe() throws Exception {
        check(++ticks <= 2400, "Automatic forestry probe timed out at stage " + stage);
        switch (stage) {
            case 0 -> {
                if (!initial.isDone()) return;
                initial.join();
                check(session.settlementNames().size() == 1, "Initial observation must create one private settlement");
                town = session.settlementNames().keySet().iterator().next();
                session.close(); session = null;
                seeded = CompletableFuture.runAsync(() -> {
                    try { seedPrivateEconomy(); }
                    catch (Exception failure) { throw new CompletionException(failure); }
                }, fixtureWorker);
                stage = 1;
            }
            case 1 -> {
                if (!seeded.isDone()) return;
                seeded.join();
                prepareNineAirBlocksAndTrees();
                session = owner();
                getServer().getPluginManager().registerEvents(new ForestMutationTracker(session, getLogger()), this);
                initial = session.start(false, Set.of(worldId)).thenApply(ignored->null);
                stage = 2;
            }
            case 2 -> {
                if (!initial.isDone()) return;
                initial.join();
                observer = new PaperForestObserver(session, observerPolicy, clock::get);
                observer.runTaskTimer(this, 1, 1);
                stage = 3;
            }
            case 3 -> {
                if (observer == null || !observer.diagnostic().contains("logging startup independently checks")) return;
                var sample = observer.latestSample().orElseThrow(() -> new IllegalStateException("Ordinary Paper observer must publish its sample"));
                check(sample.town().equals(town) && sample.chunkX() == chunkX && sample.chunkZ() == chunkZ,
                        "Paper sample must retain saved town and center chunk identity");
                check(sample.observation().complete() && sample.observation().observedMillis() == clock.get(),
                        "Paper sample must be complete and preserve original capture time");
                check(sample.observation().trunks().equals(expectedTrunks),
                        "Ordinary observer must report both complete nonpersistent canopy-backed columns and reject the horizontal log");
                observer.cancel(); observer = null;
                // The preceding observer submission is queued first on the SQL owner. This query checks actual demand
                // before the following session pulse invokes automatic founding on that same owner.
                customerReview = CompletableFuture.supplyAsync(() -> {
                    try (var connection = readOnlyConnection()) {
                        var demand = new LocalIndustryDemand(connection, industry, BusinessOperationPolicy.prototype()).review(town, Set.of(worldId), 16);
                        check(demand.complete() && demand.uncovered().getOrDefault(Commodity.TIMBER, 0L) > 0,
                                "A staffed funded craftsman must expose real uncovered timber need");
                        return demand;
                    } catch (Exception failure) { throw new CompletionException(failure); }
                }, fixtureWorker);
                stage = 4;
            }
            case 4 -> {
                if (!customerReview.isDone()) return;
                customerReview.join();
                clock.set(3000);
                foundingPulse = session.pulse();
                stage = 5;
            }
            case 5 -> {
                if (!foundingPulse.isDone()) return;
                foundingPulse.join();
                chainMetrics = readMetrics();
                stage = 6;
            }
            case 6 -> {
                if (!chainMetrics.isDone()) return;
                var result = chainMetrics.join();
                check(result[0] == 1 && result[1] == 1 && result[2] == 1 && result[3] == 6,
                        "One atomic funded logging business must own all six observed trunk supports");
                check(result[4] == 7500 && result[5] == 2000 && result[6] == 1,
                        "Qualified founder and protected 500+2000 grant debit must be persisted");
                check(scalarReadOnly("SELECT COUNT(*) FROM business_employment e JOIN businesses b ON b.id=e.business_id WHERE b.industry='logging' AND e.ended_step IS NULL") == 1,
                        "Exactly one real first worker episode must be hired");
                check(world.getBlockAt(expectedTrunks.getFirst().x(), expectedTrunks.getFirst().y(), expectedTrunks.getFirst().z()).getType() == Material.OAK_LOG,
                        "Founding must leave all physical trees unchanged");
                clock.set(clock.get() + 1000);
                foundingPulse = session.pulse();
                chainSteps = 1;
                stage = 7;
            }
            case 7 -> {
                if (!foundingPulse.isDone()) return;
                foundingPulse.join();
                chainMetrics = readMetrics();
                stage = 8;
            }
            case 8 -> {
                if (!chainMetrics.isDone()) return;
                var result = chainMetrics.join();
                if (result[7] > 0 && result[8] > 0 && result[9] > 0 && result[10] > 2000) {
                    check(result[11] == 32500,
                            "Treasury plus all business stores must conserve 33,000 less the 500 startup expense");
                    getLogger().info("NATIVE AUTOMATIC FORESTRY PASS: current Paper evidence founded qualified logging; paid logging-to-warehouse-to-craftsman shipments produced storage fittings within "
                            + chainSteps + " fixed-clock periods, with all six supports and unchanged trees; cash ledger retained 32,500");
                    getServer().getPluginManager().disablePlugin(this); getServer().shutdown();
                    stage = 9;
                    return;
                }
                if (chainSteps >= 20) throw new IllegalStateException("Paid timber chain did not complete in 20 fixed-clock periods: " + Arrays.toString(result));
                chainSteps++;
                clock.set(clock.get() + 1000);
                foundingPulse = session.pulse();
                stage = 7;
            }
            default -> { }
        }
    }

    private CompletableFuture<long[]> readMetrics() {
        return CompletableFuture.supplyAsync(() -> {
            try (var connection = readOnlyConnection();
                 var statement = connection.createStatement()) {
                long firms = scalar(statement, "SELECT COUNT(*) FROM businesses WHERE settlement_id='" + town + "' AND industry='logging'");
                long grantsCount = scalar(statement, "SELECT COUNT(*) FROM startup_grants WHERE settlement_id='" + town + "'");
                long forests = scalar(statement, "SELECT COUNT(*) FROM forestry_resources");
                long supports = scalar(statement, "SELECT COUNT(*) FROM forestry_supports");
                long cash = scalar(statement, "SELECT hundredths FROM treasuries WHERE settlement_id='" + town + "'");
                long loggingCash = scalar(statement, "SELECT k.hundredths FROM industry_stores k JOIN businesses b ON b.id=k.business_id WHERE b.industry='logging'");
                String ownerId = scalarText(statement, "SELECT owner_id FROM businesses WHERE settlement_id='" + town + "' AND industry='logging'");
                long fittings = scalar(statement, "SELECT COALESCE(SUM(s.units),0) FROM industry_stock s JOIN businesses b ON b.id=s.business_id WHERE b.settlement_id='" + town + "' AND b.industry='craftsman' AND s.commodity='storage_fittings'");
                long loggingToWarehouse = scalar(statement, "SELECT COUNT(*) FROM local_shipments s JOIN businesses a ON a.id=s.seller_id JOIN businesses b ON b.id=s.buyer_id WHERE a.industry='logging' AND b.industry='warehouse' AND s.commodity='timber' AND s.price>0 AND s.delivered_period IS NOT NULL");
                long warehouseToCraftsman = scalar(statement, "SELECT COUNT(*) FROM local_shipments s JOIN businesses a ON a.id=s.seller_id JOIN businesses b ON b.id=s.buyer_id WHERE a.industry='warehouse' AND b.industry='craftsman' AND s.commodity='timber' AND s.price>0 AND s.delivered_period IS NOT NULL");
                long loggerBalance = scalar(statement, "SELECT k.hundredths FROM industry_stores k JOIN businesses b ON b.id=k.business_id WHERE b.industry='logging'");
                long totalCash = scalar(statement, "SELECT (SELECT COALESCE(SUM(hundredths),0) FROM treasuries WHERE settlement_id='" + town + "') + (SELECT COALESCE(SUM(k.hundredths),0) FROM industry_stores k JOIN businesses b ON b.id=k.business_id WHERE b.settlement_id='" + town + "')");
                return new long[]{firms, grantsCount, forests, supports, cash, loggingCash,
                        ownerId.equals(founder.toString()) ? 1 : 0, fittings, loggingToWarehouse,
                        warehouseToCraftsman, loggerBalance, totalCash};
            } catch (Exception failure) { throw new CompletionException(failure); }
        }, fixtureWorker);
    }

    private void seedPrivateEconomy() throws Exception {
        var db = new DatabaseManager(folder.toFile(), getLogger());
        try {
            db.initialize();
            founder=db.getVillageIdentityRepository().findResident(worldId,id("forestry-founder")).orElseThrow();
            craftOwner=db.getVillageIdentityRepository().findResident(worldId,id("craft-owner")).orElseThrow();
            craftWorker=db.getVillageIdentityRepository().findResident(worldId,id("craft-worker")).orElseThrow();
            var residents = db.getPopulationRepository().findResidents(town);
            check(residents.size() == 3, "Private village must contain exactly three fixture adults");
            try (var statement = db.getConnection().createStatement()) {
                statement.execute("UPDATE residents SET age_steps=300,status='ALIVE' WHERE settlement_id='" + town + "'");
            }
            db.getPopulationRepository().recordSkillExperience(founder, "forestry", 10);
            db.getPopulationRepository().recordSkillExperience(craftWorker, "crafting", 10);
            db.getPopulationRepository().recordSkillExperience(craftOwner, "commerce", 1);
            var recipe = industry.recipes().values().stream().filter(value -> value.industry().equals("craftsman")
                    && value.inputs().getOrDefault(Commodity.TIMBER, 0L) > 0).findFirst().orElseThrow();
            var requirements = new JobRequirements(216, Map.of());
            var workerRequirements = new JobRequirements(216, Map.of("crafting", 1L));
            var site = new BusinessSite(id("craftsman-site"), town, worldId, chunkX * 16 + 14, rootY, chunkZ * 16 + 14,
                    1000, "Explicit private funded craftsman customer fixture");
            var business = new Business(id("craftsman-business"), town, "Fixture craftsman", craftOwner, craftOwner,
                    "craftsman", site.id(), 2, 0);
            db.getBusinessRepository().found(new BusinessFounding(business, site, requirements, workerRequirements, 0,
                    "Explicit private native demand customer"));
            new TreasuryRepository(db.getConnection()).initialize(id("fixture-village-treasury"), town, 33_000,
                    "Explicit marked disposable forestry probe buyer and startup funds");
            var spaces = new TreeMap<Commodity, Long>(industry.mining().storageSpacePerUnit());
            for (var good : recipe.inputs().keySet()) spaces.putIfAbsent(good, 1L);
            for (var good : recipe.outputs().keySet()) spaces.putIfAbsent(good, 1L);
            db.getIndustryRepository().openStore(business.id(), 1000, spaces, 20_000);
            db.getBusinessRepository().hire(id("craftsman-first-worker"), business.id(), craftWorker, 0);
            try (var statement = db.getConnection().prepareStatement("UPDATE industry_stock SET units=1 WHERE business_id=? AND commodity='stone'")) {
                statement.setString(1, business.id().toString());
                check(statement.executeUpdate() == 1, "Craftsman must hold its actual stone recipe input");
            }
            var warehouseSite = new BusinessSite(id("warehouse-site"), town, worldId, chunkX * 16 + 15, rootY, chunkZ * 16 + 15,
                    1000, "Explicit private funded warehouse for paid timber-chain fixture");
            var warehouse = new Business(id("warehouse-business"), town, "Fixture warehouse", craftOwner, craftOwner,
                    "warehouse", warehouseSite.id(), 2, 1000);
            var warehouseQualification = new JobRequirements(216, Map.of("commerce", 1L));
            db.getBusinessRepository().found(new BusinessFounding(warehouse, warehouseSite, warehouseQualification,
                    warehouseQualification, 0, "Explicit private staffed local warehouse fixture"));
            db.getIndustryRepository().openStore(warehouse.id(), 1024, spaces, 3000);
            db.getBusinessRepository().hire(id("warehouse-commerce-worker"), warehouse.id(), craftOwner, 1000);
            var economy = new EconomyRuntimeRepository(db.getConnection(), economySettings, FarmObservation.Policy.prototype(),
                    populationSettings.population().adulthoodSteps(), VillagePriorities.Policy.prototype(), populationSettings.seed());
            economy.observe(new FarmObservation(town, worldId, 1000, true, 0), 1000);
            economy.startOnline(1000, Set.of(worldId));
            economy.pulse(2000, false);
        } finally { db.close(); }
    }

    private PopulationSession owner() {
        var created = new PopulationSession(folder.toFile(), getLogger(), populationSettings, clock::get,
                economySettings, FarmObservation.Policy.prototype());
        created.configureForestTracking();
        created.configureForestry(forestry);
        created.configureForestryFounding(founding);
        created.configureIndustryRuntime(industry);
        created.configureBusinessOperations(BusinessOperationPolicy.prototype());
        created.configureMineFounding(MineFoundingPolicy.disabled(), grants);
        created.configureQuarries(QuarryPolicy.disabled());
        created.configureQuarryFounding(QuarryFoundingPolicy.disabled());
        created.configureCareerStudy(CareerStudyPolicy.disabled());
        created.configureStudy(StudyPolicy.disabled());
        created.configureVillageSupplies(VillageSupplyPolicy.disabled());
        created.configureWorkLearning(WorkLearningPolicy.prototype());
        return created;
    }

    private void prepareNineAirBlocksAndTrees() {
        int firstX = chunkX * 16 + 3, secondX = chunkX * 16 + 10, z = chunkZ * 16 + 8;
        var fixture = List.of(pos(firstX, rootY, z), pos(firstX, rootY + 1, z), pos(firstX, rootY + 2, z),
                pos(firstX + 1, rootY + 2, z), pos(secondX, rootY, z), pos(secondX, rootY + 1, z),
                pos(secondX, rootY + 2, z), pos(secondX + 1, rootY + 2, z), pos(chunkX * 16 + 7, rootY + 4, z));
        for (var p : fixture) {
            var block = world.getBlockAt(p.x(), p.y(), p.z());
            check(block.getType() == Material.AIR, "Every fixture cell must begin in verified AIR");
            originals.put(block.getLocation(), block.getBlockData().clone());
        }
        for (var p : fixture.subList(0, 3)) { verticalLog(p.x(), p.y(), p.z()); expectedTrunks.add(p); }
        leaf(firstX + 1, rootY + 2, z);
        for (var p : fixture.subList(4, 7)) { verticalLog(p.x(), p.y(), p.z()); expectedTrunks.add(p); }
        leaf(secondX + 1, rootY + 2, z);
        horizontalLog(chunkX * 16 + 7, rootY + 4, z);
        expectedTrunks.sort(VillageObservation.Position.ORDER);
    }

    private void verticalLog(int x, int y, int z) {
        var data = (Orientable) Material.OAK_LOG.createBlockData(); data.setAxis(Axis.Y);
        world.getBlockAt(x, y, z).setBlockData(data, false);
    }
    private void horizontalLog(int x, int y, int z) {
        var data = (Orientable) Material.OAK_LOG.createBlockData(); data.setAxis(Axis.X);
        world.getBlockAt(x, y, z).setBlockData(data, false);
    }
    private void leaf(int x, int y, int z) {
        var data = (Leaves) Material.OAK_LEAVES.createBlockData(); data.setPersistent(false); data.setDistance(1);
        world.getBlockAt(x, y, z).setBlockData(data, false);
    }

    private java.sql.Connection readOnlyConnection()throws SQLException {
        var connection=DriverManager.getConnection(readOnlyUrl());
        try(var statement=connection.createStatement()){statement.execute("PRAGMA foreign_keys=ON");statement.execute("PRAGMA busy_timeout=5000");}
        catch(SQLException failure){connection.close();throw failure;}
        return connection;
    }
    private String readOnlyUrl() {
        return "jdbc:sqlite:file:" + folder.resolve("civilizations.db").toAbsolutePath().toString().replace((char) 92, '/') + "?mode=ro";
    }
    private long scalarReadOnly(String sql) throws Exception {
        try (var connection = readOnlyConnection(); var statement = connection.createStatement()) {
            return scalar(statement, sql);
        }
    }
    private static long scalar(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) { if (!result.next()) throw new SQLException("Missing scalar: " + sql); return result.getLong(1); }
    }
    private static String scalarText(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) { if (!result.next()) throw new SQLException("Missing scalar: " + sql); return result.getString(1); }
    }
    private static UUID id(String name) { return UUID.nameUUIDFromBytes(("automatic-forestry-probe:" + name).getBytes(StandardCharsets.UTF_8)); }

    private void failProbe(String label, Throwable failure) {
        getLogger().log(java.util.logging.Level.SEVERE, label, failure);
        if (observer != null) observer.cancel();
        getServer().getPluginManager().disablePlugin(this);
        if (authorized) getServer().shutdown();
    }

    private void restoreFixture() {
        if (world == null) return;
        for (var entry : originals.entrySet()) {
            var location = entry.getKey();
            check(world.isChunkLoaded(Math.floorDiv(location.getBlockX(), 16), Math.floorDiv(location.getBlockZ(), 16)),
                    "Held fixture chunk must remain loaded during exact restoration");
            world.getBlockAt(location).setBlockData(entry.getValue(), false);
            check(world.getBlockAt(location).getBlockData().getAsString().equals(entry.getValue().getAsString()),
                    "Every original fixture block state must be restored exactly");
        }
        originals.clear();
        getLogger().info("AUTOMATIC FORESTRY FIXTURE RESTORED: nine original AIR block states");
    }

    @Override public void onDisable() {
        if (observer != null) observer.cancel();
        try { restoreFixture(); }
        catch (RuntimeException failure) { getLogger().log(java.util.logging.Level.SEVERE, "Automatic forestry restoration failed", failure); }
        finally {
            if (session != null) session.close();
            for (var chunk : tickets) world.removePluginChunkTicket(chunk.x(), chunk.z(), this);
            tickets.clear();
            fixtureWorker.shutdown();
        }
    }
}
