package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.DatabaseManager;
import com.blueninjas24.civilizations.database.VillageEnrollmentRepository;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository;
import com.blueninjas24.civilizations.database.DemographicRepository;
import com.blueninjas24.civilizations.database.EconomyRuntimeRepository;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import com.blueninjas24.civilizations.simulation.CivilizationTime;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** One DB owner, queued immutable observations and bounded saved-time catch-up. No Bukkit calls. */
public final class PopulationSession implements AutoCloseable {
    public record Settings(long seed, long millisPerMonth, int maximumOfflineMonths, int monthsPerBatch,
                           PopulationPolicy population, MigrationPolicy migration,
                           VillageRecognition.Policy recognition, VillageEnrollmentRepository.Policy enrollment) {
        public Settings {
            if (millisPerMonth < 1 || maximumOfflineMonths < 0 || monthsPerBatch < 1 || monthsPerBatch > 120)
                throw new IllegalArgumentException("Invalid population runtime settings");
            Math.multiplyExact(millisPerMonth, maximumOfflineMonths);
            java.util.Objects.requireNonNull(population); java.util.Objects.requireNonNull(migration);
            java.util.Objects.requireNonNull(recognition); java.util.Objects.requireNonNull(enrollment);
        }
        public static Settings prototype() {
            return new Settings(872613, 30_000, 12, 2, PopulationPolicy.prototype(), MigrationPolicy.prototype(),
                    VillageRecognition.Policy.prototype(), VillageEnrollmentRepository.Policy.prototype());
        }
    }
    public record Status(boolean ready, long completedMonths, long targetMonths, long livingPeople,
                         long lastBatchNanos, String failure) {
        public long remainingMonths() { return targetMonths - completedMonths; }
    }
    public record ObservationResult(List<VillageEnrollmentRepository.Result> villages, List<String> failures) {
        public ObservationResult { villages = List.copyOf(villages); failures = List.copyOf(failures); }
    }
    public record ProjectionView(PopulationState population, ResidentProjectionRepository.Snapshot identities,
                                 List<DemographicRepository.StoredEvent> recentEvents, List<ResidentProjectionRepository.PhysicalDeath> recentDeaths) {
        public ProjectionView { recentEvents = List.copyOf(recentEvents); recentDeaths = List.copyOf(recentDeaths); }
        public ProjectionView(PopulationState population, ResidentProjectionRepository.Snapshot identities) {
            this(population, identities, List.of(), List.of());
        }
    }
    private final DatabaseManager database;
    private final Settings settings;
    private final Logger logger;
    private final LongSupplier now;
    private final ScheduledThreadPoolExecutor owner;
    private final AtomicReference<Status> status = new AtomicReference<>(new Status(false, 0, 0, 0, 0, null));
    private final AtomicReference<java.util.Map<UUID,String>> settlementNames = new AtomicReference<>(java.util.Map.of());
    private final AtomicReference<PopulationState> snapshot = new AtomicReference<>();
    private final AtomicReference<ProjectionView> projectionView = new AtomicReference<>();
    private final AtomicBoolean started = new AtomicBoolean(), closing = new AtomicBoolean();
    private final AtomicInteger queued = new AtomicInteger();
    private final AtomicReference<Thread> ownerThread = new AtomicReference<>();
    private volatile ScheduledFuture<?> timer;
    private boolean readyForWork; // Owner-thread readiness survives graceful draining, not new submissions.
    private final EconomySettings economySettings;
    private final FarmObservation.Policy farmPolicy;
    private final VillagePriorities.Policy priorityPolicy;
    private final OpeningFunds openingFunds;
    private EconomyRuntimeRepository economy;
    private final AtomicReference<java.util.Map<UUID, EconomyRuntimeRepository.View>> economyView = new AtomicReference<>(java.util.Map.of());
    private volatile String economyFailure;

    public PopulationSession(File dataFolder, Logger logger, Settings settings, LongSupplier now) {
        this(dataFolder, logger, settings, now, null, FarmObservation.Policy.prototype());
    }
    public PopulationSession(File dataFolder, Logger logger, Settings settings, LongSupplier now,
                             EconomySettings economySettings, FarmObservation.Policy farmPolicy) {
        this(dataFolder,logger,settings,now,economySettings,farmPolicy,VillagePriorities.Policy.prototype());
    }
    public PopulationSession(File dataFolder, Logger logger, Settings settings, LongSupplier now,
                             EconomySettings economySettings, FarmObservation.Policy farmPolicy, VillagePriorities.Policy priorityPolicy) {
        this(dataFolder, logger, settings, now, economySettings, farmPolicy, priorityPolicy, OpeningFunds.disabled());
    }
    public PopulationSession(File dataFolder, Logger logger, Settings settings, LongSupplier now,
                             EconomySettings economySettings, FarmObservation.Policy farmPolicy, VillagePriorities.Policy priorityPolicy,
                             OpeningFunds openingFunds) {
        this.logger = logger; this.settings = settings; this.now = now;
        this.openingFunds = java.util.Objects.requireNonNull(openingFunds);
        this.priorityPolicy = java.util.Objects.requireNonNull(priorityPolicy);
        this.economySettings = economySettings; this.farmPolicy = java.util.Objects.requireNonNull(farmPolicy);
        database = new DatabaseManager(dataFolder, logger);
        owner = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = Thread.ofPlatform().daemon(true).name("Civilizations-population").unstarted(runnable);
            ownerThread.set(thread); return thread;
        });
        owner.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        owner.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        owner.setRemoveOnCancelPolicy(true);
    }

    public Status status() { return status.get(); }
    public java.util.Map<UUID,String> settlementNames() { return settlementNames.get(); }
    public Optional<PopulationState> snapshot() { return Optional.ofNullable(snapshot.get()); }
    public Optional<ProjectionView> projectionView() { return Optional.ofNullable(projectionView.get()); }
    public Settings settings() { return settings; }
    public java.util.Map<UUID, EconomyRuntimeRepository.View> economyView() { return economyView.get(); }
    public Optional<EconomySettings> economySettings() { return Optional.ofNullable(economySettings); }
    public String economyFailure() { return economyFailure; }

    public CompletableFuture<Status> start() {
        return start(true);
    }

    /** Disable the real timer for accelerated tests; explicit pulses retain identical behavior. */
    public CompletableFuture<Status> start(boolean scheduleAutomatically) {
        if (!started.compareAndSet(false, true)) return CompletableFuture.failedFuture(new IllegalStateException("Session already started"));
        return submit(() -> {
            try {
                database.initialize();
                var demographic = database.getDemographicRepository();
                var saved = demographic.load(settings.population(), settings.migration());
                if (saved.isEmpty())
                    demographic.initializeClock(settings.seed(), settings.population(), settings.migration());
                else if (saved.get().seed() != settings.seed())
                    throw new SQLException("Saved population seed differs; explicit transition required");
                var clock = database.getCivilizationClockRepository();
                if (clock.load(settings.millisPerMonth()).isEmpty()) clock.initialize(now.getAsLong(), settings.millisPerMonth());
                var accrued = clock.accrue(now.getAsLong(), settings.millisPerMonth(),
                        Math.multiplyExact(settings.millisPerMonth(), settings.maximumOfflineMonths()));
                if (accrued.discardedMillis() > 0) logger.info("Prototype offline calendar cap discarded " + accrued.discardedMillis() + " real milliseconds");
                advance(accrued.anchor().targetSteps());
                if (economySettings != null) {
                    economy = new EconomyRuntimeRepository(database.getConnection(), economySettings, farmPolicy, settings.population().adulthoodSteps(),priorityPolicy,settings.seed(),openingFunds);
                    economy.start(now.getAsLong()); economyView.set(economy.views());
                }
                readyForWork = true;
                if (scheduleAutomatically) timer = owner.scheduleWithFixedDelay(() -> {
                    if (!closing.get()) {
                        try { pulseOnOwner(); } catch (Exception failure) { reportFailure(failure); }
                    }
                }, 1, 1, TimeUnit.SECONDS);
                return status.get();
            } catch (Exception failure) {
                reportFailure(failure); database.close(); throw failure;
            }
        });
    }

    /** Explicit accelerated-test/diagnostic pulse; scheduler uses the same owner-thread operation. */
    public CompletableFuture<Status> pulse() {
        return submit(() -> { requireReady(); pulseOnOwner(); return status.get(); });
    }

    public CompletableFuture<ObservationResult> observe(VillageObservation observation) {
        return submit(() -> {
            requireReady();
            pulseOnOwner();
            if (status.get().remainingMonths() > 0) throw new RejectedExecutionException("Population catch-up pending; retry village observation later");
            var recognized = VillageRecognition.recognize(observation, settings.recognition());
            var imported = new ArrayList<VillageEnrollmentRepository.Result>();
            var failures = new ArrayList<String>();
            for (var candidate : recognized.candidates()) {
                try { imported.add(database.getVillageEnrollmentRepository().enroll(observation, candidate, settings.enrollment())); }
                catch (SQLException failure) {
                    failures.add(failure.getMessage()); logger.warning("Village enrollment rejected: " + failure.getMessage());
                }
            }
            var saved = database.getDemographicRepository().load(settings.population(), settings.migration()).orElseThrow();
            publish(saved.state());
            var previous = status.get();
            status.set(new Status(!closing.get(), saved.completedSteps(), previous.targetMonths(), saved.state().livingCount(), previous.lastBatchNanos(), null));
            return new ObservationResult(imported, failures);
        });
    }

    public CompletableFuture<Boolean> claimProjection(UUID person, UUID world, UUID entity, UUID household, VillageObservation.Position position) {
        return projectionWork(() -> database.getResidentProjectionRepository().claim(person, world, entity, household, position));
    }
    public CompletableFuture<Void> observeFarms(FarmObservation observation) {
        return submit(() -> {
            requireReady(); if (economy == null) throw new IllegalStateException("Economy not enabled in this session");
            economy.observe(observation, now.getAsLong()); economyView.set(economy.views()); return null;
        });
    }
    /** Internal delivery bridge; source adapter must first establish the actual goods/consent. */
    public CompletableFuture<Boolean> recordDeliveredFood(UUID operation, UUID town, long units, String source) {
        return submit(() -> {
            requireReady();if(economy==null)throw new IllegalStateException("Economy not enabled in this session");
            boolean accepted=economy.recordDeliveredFood(operation,town,units,source,now.getAsLong());economyView.set(economy.views());return accepted;
        });
    }
    public CompletableFuture<Boolean> activateProjection(UUID person, UUID entity) {
        return projectionWork(() -> database.getResidentProjectionRepository().activate(person, entity));
    }
    public CompletableFuture<String> reserveFoodPurchase(FoodPurchases.Terms terms, FoodPurchases.Policy policy, FoodAidPreview.Policy foods) {
        return submit(() -> {
            requireReady(); if(economy==null)throw new IllegalStateException("Economy not enabled");
            var result=economy.reserveFoodPurchase(terms,policy,foods,now.getAsLong());economyView.set(economy.views());return result;
        });
    }
    public CompletableFuture<Boolean> completeFoodPurchase(FoodPurchases.Terms terms) {
        return submit(() -> {
            requireReady(); if(economy==null)throw new IllegalStateException("Economy not enabled");
            var result=economy.completeFoodPurchase(terms,now.getAsLong());economyView.set(economy.views());return result;
        });
    }
    public CompletableFuture<Boolean> cancelFoodPurchase(FoodPurchases.Terms terms) {
        return submit(() -> {
            requireReady(); if(economy==null)throw new IllegalStateException("Economy not enabled");
            var result=economy.cancelFoodPurchase(terms);economyView.set(economy.views());return result;
        });
    }
    public CompletableFuture<Boolean> retireProjection(UUID person, UUID entity) {
        return projectionWork(() -> database.getResidentProjectionRepository().retire(person, entity));
    }
    public CompletableFuture<Boolean> recordPhysicalDeath(UUID world, UUID entity, long observedMillis, String reason) {
        return projectionWork(() -> database.getResidentProjectionRepository().recordPhysicalDeath(world, entity, observedMillis, reason));
    }
    public record DeathObservation(UUID world, UUID entity, long observedMillis, String reason) {}
    public CompletableFuture<Boolean> flushPhysicalDeaths(List<DeathObservation> deaths) {
        var observations = List.copyOf(deaths);
        return projectionWork(() -> {
            boolean changed = false;
            for (var death : observations) changed |= database.getResidentProjectionRepository()
                    .recordPhysicalDeath(death.world(), death.entity(), death.observedMillis(), death.reason());
            return changed;
        });
    }
    private CompletableFuture<Boolean> projectionWork(Operation<Boolean> operation) {
        return submit(() -> {
            requireReady();
            boolean result = operation.run();
            var saved = database.getDemographicRepository().load(settings.population(), settings.migration()).orElseThrow();
            publish(saved.state());
            var previous = status.get();
            status.set(new Status(!closing.get(), saved.completedSteps(), previous.targetMonths(), saved.state().livingCount(), previous.lastBatchNanos(), null));
            return result;
        });
    }
    private void publish(PopulationState state) throws SQLException {
        var identities = database.getResidentProjectionRepository().snapshot();
        settlementNames.set(database.getVillageIdentityRepository().settlementNames());
        projectionView.set(new ProjectionView(state, identities, database.getDemographicRepository().recentEvents(100),
                database.getResidentProjectionRepository().recentDeaths(100)));
        snapshot.set(state);
    }

    private void pulseOnOwner() throws SQLException {
        var queuedTime = database.getCivilizationClockRepository().accrue(now.getAsLong(), settings.millisPerMonth(), Long.MAX_VALUE);
        var previous = status.get();
        status.set(new Status(previous.ready(), previous.completedMonths(), queuedTime.anchor().targetSteps(),
                previous.livingPeople(), previous.lastBatchNanos(), previous.failure()));
        advance(queuedTime.anchor().targetSteps());
        if (economy != null && status.get().remainingMonths() == 0) {
            try { economy.pulse(now.getAsLong(), false); economyView.set(economy.views()); economyFailure = null; }
            catch (SQLException | RuntimeException failure) {
                if (!java.util.Objects.equals(economyFailure, failure.getMessage())) logger.log(Level.SEVERE, "Economy update deferred; saved periods remain retryable", failure);
                economyFailure = failure.getMessage();
            }
        }
    }
    private void advance(long target) throws SQLException {
        long began = System.nanoTime();
        var result = database.getDemographicRepository().advance(target, settings.monthsPerBatch(), settings.population(), settings.migration());
        var state = result.checkpoint().state(); publish(state);
        status.set(new Status(!closing.get(), result.checkpoint().completedSteps(), target, state.livingCount(), System.nanoTime() - began, null));
    }
    private void requireReady() {
        if (!readyForWork) throw new IllegalStateException("Population session is not ready");
    }
    @FunctionalInterface private interface Operation<T> { T run() throws Exception; }
    private <T> CompletableFuture<T> submit(Operation<T> operation) {
        if (closing.get()) return CompletableFuture.failedFuture(new RejectedExecutionException("Population session is closing"));
        if (queued.incrementAndGet() > 64) {
            queued.decrementAndGet(); return CompletableFuture.failedFuture(new RejectedExecutionException("Population queue full; retry later"));
        }
        var result = new CompletableFuture<T>();
        try {
            owner.execute(() -> {
                try {
                    // Accepted work drains before the queued close; new submissions are rejected above.
                    result.complete(operation.run());
                } catch (Exception failure) {
                    if (failure instanceof SQLException && !(failure instanceof EconomyRuntimeRepository.PurchaseDeclined)) reportFailure(failure);
                    result.completeExceptionally(failure);
                }
                finally { queued.decrementAndGet(); }
            });
        } catch (RejectedExecutionException failure) { queued.decrementAndGet(); result.completeExceptionally(failure); }
        return result;
    }
    private void reportFailure(Exception failure) {
        var previous = status.get();
        if (!java.util.Objects.equals(previous.failure(), failure.getMessage()))
            logger.log(Level.SEVERE, "Civilizations population work failed; saved work remains retryable", failure);
        status.set(new Status(previous.ready(), previous.completedMonths(), previous.targetMonths(), previous.livingPeople(), previous.lastBatchNanos(), failure.getMessage()));
    }

    @Override public void close() {
        if (!closing.compareAndSet(false, true)) return;
        var previous = status.get();
        status.set(new Status(false, previous.completedMonths(), previous.targetMonths(), previous.livingPeople(), previous.lastBatchNanos(), previous.failure()));
        var scheduled = timer; if (scheduled != null) scheduled.cancel(false);
        owner.execute(database::close);
        owner.shutdown();
        if (Thread.currentThread() == ownerThread.get()) return;
        try {
            if (!owner.awaitTermination(5, TimeUnit.SECONDS)) logger.warning("Population shutdown still finishing on its owner thread");
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
}
