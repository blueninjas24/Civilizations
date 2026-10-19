import com.blueninjas24.civilizations.application.BusinessDiagnostics;
import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.EconomyConfiguration;
import com.blueninjas24.civilizations.population.JobRequirements;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.Settlement;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Guarded one-village mixed Phase 3 acceptance. Uses packaged defaults and an explicit private SQL fixture;
 * no production database, blocks, entities, chunks or automatic resource claims are touched.
 */
public final class PhaseThreeAcceptanceProbe extends JavaPlugin {
    private static final String MARKER = "Disposable Civilizations Phase 1 test environment";
    private static final int PERIODS = 40;
    private final AtomicLong now = new AtomicLong(1_000);
    private final List<Long> pulseNanos = new ArrayList<>();
    private volatile CompletableFuture<Void> work;
    private Path folder;
    private UUID world, town, mine, quarry, logger, fishery, supplier, customer, craftsman, bakery;
    private boolean authorized;
    private int ticks;

    private void require(boolean condition, String reason) {
        if (!condition) throw new IllegalStateException(reason);
    }

    @Override public void onEnable() {
        try {
            var root = getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals(MARKER)
                            && getServer().getIp().equals("127.0.0.1")
                            && getServer().getPort() == 25578
                            && getServer().getOnlinePlayers().isEmpty(),
                    "Marked localhost disposable server with zero players required");
            authorized = true;
            world = getServer().getWorlds().getFirst().getUID();
            for(int i=0;i<8;i++)require(!getServer().getWorlds().getFirst().isChunkLoaded(128+i,128),
                    "All abstract workplace chunks must start unloaded");
            folder = getDataFolder().toPath().resolve("phase3-acceptance-" + UUID.randomUUID());
            require(!Files.exists(folder), "Fixture database path must be private and new");
            work = CompletableFuture.runAsync(this::verify);
            new BukkitRunnable() {
                @Override public void run() {
                    try {
                        require(getServer().getOnlinePlayers().isEmpty(), "No-player guard changed during probe");
                        require(++ticks <= 1800, "Mixed Phase 3 fixture timed out");
                        if (!work.isDone()) return;
                        work.join();
                        for(int i=0;i<8;i++)require(!getServer().getWorlds().getFirst().isChunkLoaded(128+i,128),
                                "Simulation must not load abstract workplace chunks");
                        long total = pulseNanos.stream().mapToLong(Long::longValue).sum();
                        long maximum = pulseNanos.stream().mapToLong(Long::longValue).max().orElse(0);
                        long averageMicros = pulseNanos.isEmpty() ? 0 : total / pulseNanos.size() / 1_000;
                        getLogger().info("PHASE3_ACCEPTANCE_PROBE_PASSED one private settlement; packaged finite mine/quarry/forestry/fishery, paid warehouse supply, actual craftsman storage customer/installation and bakery grain processing; "
                                + pulseNanos.size() + " measured owner pulses, average=" + averageMicros + "us, max=" + maximum / 1_000 + "us; all eight workplace chunks remained unloaded; absent-world pause, restart/no offline catch-up, diagnostics, cash conservation and foreign keys checked; no scale claim, production access or world mutation");
                        cancel();
                        getServer().getPluginManager().disablePlugin(PhaseThreeAcceptanceProbe.this);
                        getServer().shutdown();
                    } catch (Throwable failure) {
                        cancel();
                        getLogger().log(java.util.logging.Level.SEVERE, "PHASE3_ACCEPTANCE_PROBE_FAILED", failure);
                        getServer().getPluginManager().disablePlugin(PhaseThreeAcceptanceProbe.this);
                        if (authorized) getServer().shutdown();
                    }
                }
            }.runTaskTimer(this, 1, 1);
        } catch (Throwable failure) {
            getLogger().log(java.util.logging.Level.SEVERE, "Phase 3 acceptance fixture refused", failure);
            getServer().getPluginManager().disablePlugin(this);
            if (authorized) getServer().shutdown();
        }
    }

    private YamlConfiguration packagedDefaults() throws Exception {
        var stream = Objects.requireNonNull(ProductionRecipe.class.getResourceAsStream("/economy.yml"),
                "Packaged economy.yml required");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private IndustryRuntimePolicy runtimePolicy(YamlConfiguration config) {
        config.set("industry-runtime.real-millis-per-period", 1_000);
        return EconomyConfiguration.readIndustryRuntime(config);
    }

    private PopulationSession open(IndustryRuntimePolicy industry, YamlConfiguration config) throws Exception {
        var settings = PopulationSession.Settings.prototype();
        var session = new PopulationSession(folder.toFile(), getLogger(), settings, now::get);
        session.configureIndustryRuntime(industry);
        session.configureBusinessOperations(EconomyConfiguration.readBusinessOperations(config));
        session.configureQuarries(EconomyConfiguration.readQuarryResources(config));
        session.configureForestry(EconomyConfiguration.readForestryResources(config));
        session.configureFishing(EconomyConfiguration.readFishingResources(config));
        session.start(false, Set.of(world)).get(10, TimeUnit.SECONDS);
        return session;
    }

    private void verify() {
        try {
            var config = packagedDefaults();
            var industry = runtimePolicy(config);
            var quarries = EconomyConfiguration.readQuarryResources(config);
            var forestry = EconomyConfiguration.readForestryResources(config);
            var fishing = EconomyConfiguration.readFishingResources(config);
            require(industry.enabled() && industry.recipes().containsKey("craft_storage_fittings")
                            && industry.recipes().containsKey("craft_storage_fittings_stone")
                            && industry.recipes().containsKey("bakery_bread")
                            && industry.storageUpgrades().enabled()
                            && quarries.enabled() && forestry.enabled() && fishing.enabled(),
                    "Packaged mixed-industry defaults must remain enabled and readable");
            seed(industry, quarries, forestry, fishing);
            var before = snapshot(industry);
            require(before.cash() == initialCash && before.workRows() == 0 && before.shipments() == 0,
                    "Private fixture must start with only declared capital/materials and no earned work");

            try (var session = open(industry, config)) {
                for (int period = 0; period < PERIODS; period++) {
                    now.addAndGet(1_000);
                    long started = System.nanoTime();
                    session.pulse().get(10, TimeUnit.SECONDS);
                    pulseNanos.add(System.nanoTime() - started);
                }
            }

            var active = snapshot(industry);
            require(active.workRows() > 0 && active.mineRemaining() < before.mineRemaining()
                            && active.quarryRemaining() < before.quarryRemaining()
                            && active.forestAvailable() < before.forestAvailable()
                            && active.fishAvailable() < before.fishAvailable(),
                    "Live owner pulses must perform finite mine/quarry, renewable timber and fish work");
            require(active.craftsmanInputs() > 0 && active.fittingsProduced() > 0
                            && active.installedUpgrades() == 1 && active.customerCapacity() > 100,
                    "Craftsman must consume real inputs and sell one fitting to the actual crowded business customer");
            require(active.bakeryGrainConsumed() > 0 && active.bakeryFoodProduced() > 0,
                    "Bakery must consume actual owned grain and produce abstract food within saved capacity");
            require(active.paidShipments() > 0 && active.cash() == initialCash,
                    "Local producer/warehouse/workshop payments must conserve all seeded real money");
            require(active.capacityViolations() == 0, "No business stock may exceed saved storage capacity");
            require(active.recipeRuns() > 0 && active.productions() > 0,
                    "Production receipts must be saved in the ordinary owner runtime");
            verifyDiagnostics(industry);

            var absent = snapshot(industry);
            try (var session = open(industry, config)) {
                session.updateLoadedWorlds(Set.of()).get(10, TimeUnit.SECONDS);
                now.addAndGet(100_000);
                session.pulse().get(10, TimeUnit.SECONDS);
            }
            require(absent.equals(snapshot(industry)),
                    "Absent-world time must not extract, produce, purchase, deliver or install");

            try (var session = open(industry, config)) {
                session.pulse().get(10, TimeUnit.SECONDS);
            }
            require(absent.equals(snapshot(industry)), "Restart must not award offline catch-up at the same saved time");
            now.addAndGet(1_000);
            try (var session = open(industry, config)) {
                session.pulse().get(10, TimeUnit.SECONDS);
            }
            var resumed = snapshot(industry);
            require(resumed.workRows() - absent.workRows() <= 8,
                    "A resumed one-period pulse must not replay the absent-world backlog");
            require(resumed.cash() == initialCash && resumed.capacityViolations() == 0,
                    "Restarted owner work must conserve cash and respect all storage caps");
        } catch (Throwable failure) {
            throw new CompletionException(failure);
        }
    }

    private long initialCash;
    private void seed(IndustryRuntimePolicy industry, QuarryPolicy quarries, ForestryPolicy forestry,
                      FishingPolicy fishing) throws Exception {
        town = UUID.randomUUID();
        mine = UUID.randomUUID(); quarry = UUID.randomUUID(); logger = UUID.randomUUID(); fishery = UUID.randomUUID();
        supplier = UUID.randomUUID(); customer = UUID.randomUUID(); craftsman = UUID.randomUUID(); bakery = UUID.randomUUID();
        var ids = List.of(mine, quarry, logger, fishery, supplier, customer, craftsman, bakery);
        var kinds = List.of("mining", "quarry", "logging", "fishing", "warehouse", "warehouse", "craftsman", "bakery");
        var residents = new ArrayList<UUID>();
        for (int i = 0; i < ids.size(); i++) residents.add(UUID.randomUUID());
        var requirements = new JobRequirements(216, Map.of());
        var catalog = industry.mining().storageSpacePerUnit();
        var initialBusinessCash = List.of(1_000L, 1_000L, 1_000L, 1_000L, 12_000L, 6_000L, 4_000L, 4_000L);
        initialCash = 100_000L;

        var db = new DatabaseManager(folder.toFile(), getLogger());
        try {
            db.initialize();
            db.saveSettlement(new Settlement(town, "Phase 3 mixed fixture", "Disposable fixture", 0, 64, 0, 1, 0, 0));
            db.getVillageIdentityRepository().observeWorld(world, "Disposable fixture");
            db.getVillageIdentityRepository().bindSettlement(town, world);
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(), town, 100_000, "Declared private fixture treasury");
            for (int i = 0; i < ids.size(); i++) {
                var person = residents.get(i);
                db.getPopulationRepository().insertResident(new Resident(person, "Fixture adult " + i, town, null, 300, Resident.Status.ALIVE));
                var site = new BusinessSite(UUID.randomUUID(), town, world, 2048+i * 16, 64, 2048, 64,
                        "Explicit private acceptance fixture site; no physical claim");
                var business = new Business(ids.get(i), town, "Fixture " + kinds.get(i), person, person, kinds.get(i), site.id(), 1, 0);
                db.getBusinessRepository().found(new BusinessFounding(business, site, requirements, requirements, 0,
                        "Explicit private fixture business; not automatic founding"));
                db.getBusinessRepository().hire(UUID.randomUUID(), business.id(), person, 0);
                long capacity = i == 5 ? 100 : 1_024;
                db.getIndustryRepository().openStore(business.id(), capacity, catalog, initialBusinessCash.get(i));
            }
            new TreasuryRepository(db.getConnection()).load(town).orElseThrow();
            new MineralDiscoveryRepository(db.getConnection()).discover(
                    db.getBusinessRepository().findBusiness(mine).orElseThrow().siteId(),
                    new MineralObservation(world, now.get(), true, Commodity.COAL,
                            List.of(pos(10, 40, 10), pos(11, 40, 10))),
                    industry.mining().depositBatchesPerObservedOre(), industry.mining().miningBundle());
            var stoneTerms = quarries.terms(Commodity.STONE);
            new MineralDiscoveryRepository(db.getConnection()).discover(
                    db.getBusinessRepository().findBusiness(quarry).orElseThrow().siteId(),
                    new MineralObservation(world, now.get(), true, Commodity.STONE,
                            List.of(pos(30, 40, 10), pos(31, 40, 10))),
                    stoneTerms.reserveBatchesPerBlock(), quarries.bundle(Commodity.STONE));
            var trunks = new ArrayList<VillageObservation.Position>();
            for (int i = 0; i < forestry.minimumTrunkCoordinates(); i++) trunks.add(pos(50 + i, 64, 10));
            new ForestryRepository(db.getConnection()).open(
                    db.getBusinessRepository().findBusiness(logger).orElseThrow().siteId(),
                    new ForestObservation(world, now.get(), true, trunks), forestry);
            var columns = new ArrayList<FisheryObservation.Column>();
            for (int i = 0; i < fishing.minimumWaterColumns(); i++) columns.add(new FisheryObservation.Column(2048+80 + i, 2048+10));
            new FisheryRepository(db.getConnection()).open(
                    db.getBusinessRepository().findBusiness(fishery).orElseThrow().siteId(),
                    new FisheryObservation(world, now.get(), true, columns), fishing);
            try (var statement = db.getConnection().createStatement()) {
                statement.executeUpdate("UPDATE industry_stock SET units=120 WHERE business_id='" + supplier + "' AND commodity='grain'");
                statement.executeUpdate("UPDATE industry_stock SET units=80 WHERE business_id='" + customer + "' AND commodity='coal'");
            }
            require(db.getIndustryRepository().store(customer).orElseThrow().usedSpace() == 80,
                    "The business customer must start at the configured 80% storage demand threshold");
            require(new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()
                    ==initialCash-initialBusinessCash.stream().mapToLong(Long::longValue).sum(),
                    "Business capital must come from the declared fixture treasury");
        } finally {
            db.close();
        }
    }

    private VillageObservation.Position pos(int x, int y, int z) {
        return new VillageObservation.Position(2048+x, y, 2048+z);
    }

    private record Snapshot(long cash, long workRows, long productions, long recipeRuns, long shipments,
                            long paidShipments, long mineRemaining, long quarryRemaining, long forestAvailable,
                            long fishAvailable, long craftsmanInputs, long fittingsProduced, long installedUpgrades,
                            long customerCapacity, long bakeryGrainConsumed, long bakeryFoodProduced,
                            long capacityViolations) {}

    private Snapshot snapshot(IndustryRuntimePolicy industry) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:file:" + folder.resolve("civilizations.db")
                .toAbsolutePath().toString().replace('\\', '/') + "?mode=ro")) {
            long cash = scalar(connection, "SELECT COALESCE((SELECT SUM(hundredths) FROM treasuries),0)+COALESCE((SELECT SUM(hundredths) FROM industry_stores),0)");
            long workRows = scalar(connection, "SELECT (SELECT COUNT(*) FROM industry_production)+(SELECT COUNT(*) FROM forestry_work)+(SELECT COUNT(*) FROM fishery_work)");
            long productions = scalar(connection, "SELECT COUNT(*) FROM industry_production");
            long recipes = scalar(connection, "SELECT COUNT(*) FROM recipe_runs");
            long shipments = scalar(connection, "SELECT COUNT(*) FROM local_shipments");
            long paid = scalar(connection, "SELECT COUNT(*) FROM local_shipments WHERE price>0");
            long mineBatches = scalar(connection, "SELECT COALESCE((SELECT remaining_batches FROM mineral_deposits d JOIN business_sites s ON s.id=d.site_id JOIN businesses b ON b.site_id=s.id WHERE b.id='" + mine + "'),0)");
            long quarryBatches = scalar(connection, "SELECT COALESCE((SELECT remaining_batches FROM mineral_deposits d JOIN business_sites s ON s.id=d.site_id JOIN businesses b ON b.site_id=s.id WHERE b.id='" + quarry + "'),0)");
            long forest = scalar(connection, "SELECT COALESCE((SELECT available FROM forestry_resources WHERE site_id=(SELECT site_id FROM businesses WHERE id='" + logger + "')),0)");
            long fish = scalar(connection, "SELECT COALESCE((SELECT available FROM fishery_resources WHERE site_id=(SELECT site_id FROM businesses WHERE id='" + fishery + "')),0)");
            long craftInputs = scalar(connection, "SELECT COALESCE(SUM(units),0) FROM recipe_flows f JOIN recipe_runs r ON r.operation_id=f.operation_id WHERE r.business_id='" + craftsman + "' AND f.direction='INPUT'");
            long fittings = scalar(connection, "SELECT COALESCE(SUM(units),0) FROM recipe_flows f JOIN recipe_runs r ON r.operation_id=f.operation_id WHERE r.business_id='" + craftsman + "' AND f.direction='OUTPUT' AND f.commodity='storage_fittings'");
            long upgrades = scalar(connection, "SELECT COUNT(*) FROM business_storage_upgrades WHERE buyer_id='" + customer + "' AND installed_period IS NOT NULL");
            long capacity = scalar(connection, "SELECT capacity FROM industry_stores WHERE business_id='" + customer + "'");
            long grainUsed = scalar(connection, "SELECT COALESCE(SUM(units),0) FROM recipe_flows f JOIN recipe_runs r ON r.operation_id=f.operation_id WHERE r.business_id='" + bakery + "' AND f.direction='INPUT' AND f.commodity='grain'");
            long foodMade = scalar(connection, "SELECT COALESCE(SUM(units),0) FROM recipe_flows f JOIN recipe_runs r ON r.operation_id=f.operation_id WHERE r.business_id='" + bakery + "' AND f.direction='OUTPUT' AND f.commodity='food'");
            long badCapacity = scalar(connection, "SELECT COUNT(*) FROM industry_stores s WHERE s.capacity < COALESCE((SELECT SUM(k.units*k.space_per_unit) FROM industry_stock k WHERE k.business_id=s.business_id),0)");
            try (var statement = connection.createStatement(); var result = statement.executeQuery("PRAGMA foreign_key_check")) {
                require(!result.next(), "Mixed acceptance foreign keys must remain clean");
            }
            return new Snapshot(cash, workRows, productions, recipes, shipments, paid, mineBatches, quarryBatches,
                    forest, fish, craftInputs, fittings, upgrades, capacity, grainUsed, foodMade, badCapacity);
        }
    }

    private long scalar(java.sql.Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            require(result.next(), "Missing scalar for " + sql);
            return result.getLong(1);
        }
    }

    private void verifyDiagnostics(IndustryRuntimePolicy industry) throws Exception {
        var db = new DatabaseManager(folder.toFile(), getLogger());
        try {
            db.initialize();
            var report = new BusinessRuntime(db.getConnection(), 8, 64).refresh(now.get(), Set.of(world));
            var lines = BusinessDiagnostics.describe(report, town);
            require(lines.stream().anyMatch(line -> line.contains("Latest industrial production")),
                    "Ordinary business diagnostics must expose saved production reasoning");
        } finally {
            db.close();
        }
    }

    @Override public void onDisable() {
        if (work != null && !work.isDone()) work.cancel(true);
    }
}
