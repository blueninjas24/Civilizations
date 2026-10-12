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
    private boolean trackOres;
    private final AtomicBoolean oreHistoryHealthy=new AtomicBoolean();
    private final AtomicBoolean oreCaptureGap=new AtomicBoolean();
    private final AtomicReference<String> oreHistoryIssue=new AtomicReference<>("starting");
    private final AtomicReference<Thread> ownerThread = new AtomicReference<>();
    private volatile ScheduledFuture<?> timer;
    private boolean readyForWork; // Owner-thread readiness survives graceful draining, not new submissions.
    private final EconomySettings economySettings;
    private final FarmObservation.Policy farmPolicy;
    private final VillagePriorities.Policy priorityPolicy;
    private final OpeningFunds openingFunds;
    private final HouseholdWellbeing.Policy wellbeing;
    private final FoodResponses.Settings responses;
    private EconomyRuntimeRepository economy;
    private final AtomicReference<java.util.Map<UUID, EconomyRuntimeRepository.View>> economyView = new AtomicReference<>(java.util.Map.of());
    private volatile String economyFailure;
    private java.util.Set<UUID> loadedWorlds; // Owner-thread snapshot; null only for legacy headless harnesses.
    private volatile boolean onlineOnly;
    private com.blueninjas24.civilizations.industry.FarmBusinessPolicy farmBusinessPolicy=com.blueninjas24.civilizations.industry.FarmBusinessPolicy.disabled();
    private com.blueninjas24.civilizations.database.BusinessRuntime businesses;
    private com.blueninjas24.civilizations.industry.IndustryRuntimePolicy industryPolicy=com.blueninjas24.civilizations.industry.IndustryRuntimePolicy.disabled();
    private com.blueninjas24.civilizations.database.IndustryRuntime industries;
    private com.blueninjas24.civilizations.industry.BusinessPurchasePolicy purchasePolicy=com.blueninjas24.civilizations.industry.BusinessPurchasePolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy workshopFoundingPolicy=com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WarehouseOpportunityPolicy warehouseOpportunityPolicy=com.blueninjas24.civilizations.industry.WarehouseOpportunityPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy warehouseFoundingPolicy=com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkplaceOperationPolicy workplaceOperationPolicy=com.blueninjas24.civilizations.industry.WorkplaceOperationPolicy.prototype();
    private com.blueninjas24.civilizations.industry.BusinessOperationPolicy operationPolicy=com.blueninjas24.civilizations.industry.BusinessOperationPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkLearningPolicy learningPolicy=com.blueninjas24.civilizations.industry.WorkLearningPolicy.disabled();
    private com.blueninjas24.civilizations.industry.MineFoundingPolicy mineFoundingPolicy=com.blueninjas24.civilizations.industry.MineFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.industry.StartupGrantPolicy startupGrantPolicy=com.blueninjas24.civilizations.industry.StartupGrantPolicy.disabled();
    private com.blueninjas24.civilizations.database.AutomaticMineFounding automaticMines;
    private volatile String automaticMineDiagnostic="Automatic mine founding disabled";
    private com.blueninjas24.civilizations.industry.WorkplaceObserverPolicy workplacePolicy=com.blueninjas24.civilizations.industry.WorkplaceObserverPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy workshopOpportunityPolicy=com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkshopStarterPolicy workshopStarterPolicy=com.blueninjas24.civilizations.industry.WorkshopStarterPolicy.disabled();
    private com.blueninjas24.civilizations.industry.CareerStudyPolicy careerStudyPolicy=com.blueninjas24.civilizations.industry.CareerStudyPolicy.disabled();
    private com.blueninjas24.civilizations.industry.StudyPolicy studyPolicy=com.blueninjas24.civilizations.industry.StudyPolicy.disabled();
    private com.blueninjas24.civilizations.industry.BusinessStaffingPolicy staffingPolicy=com.blueninjas24.civilizations.industry.BusinessStaffingPolicy.disabled();
    private com.blueninjas24.civilizations.industry.BusinessResalePolicy resalePolicy=com.blueninjas24.civilizations.industry.BusinessResalePolicy.disabled();
    private com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy farmToolsPolicy=com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy.disabled();
    private com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy farmToolsBudget=com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy.disabled();
    public void configureFarmTools(com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy policy,com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy budget) {
        if(started.get())throw new IllegalStateException("Configure farm tools before start");
        farmToolsPolicy=java.util.Objects.requireNonNull(policy);farmToolsBudget=java.util.Objects.requireNonNull(budget);
    }
    private com.blueninjas24.civilizations.industry.UpkeepContributionPolicy upkeepContributions=com.blueninjas24.civilizations.industry.UpkeepContributionPolicy.disabled();
    public void configureUpkeepContributions(com.blueninjas24.civilizations.industry.UpkeepContributionPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure contributions before start");
        upkeepContributions=java.util.Objects.requireNonNull(policy);
    }
    private boolean listingClockSuspended;
    private volatile String listingClockFailure;
    private volatile String industryFailure;
    private boolean industrySuspended; // Failed boundary cannot safely reconstruct a live interval; restart skips it.
    private final AtomicReference<com.blueninjas24.civilizations.database.BusinessRuntime.Snapshot> businessView =
            new AtomicReference<>(com.blueninjas24.civilizations.database.BusinessRuntime.Snapshot.empty());

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
        this(dataFolder, logger, settings, now, economySettings, farmPolicy, priorityPolicy, openingFunds, HouseholdWellbeing.Policy.disabled());
    }
    public PopulationSession(File dataFolder, Logger logger, Settings settings, LongSupplier now,
                             EconomySettings economySettings, FarmObservation.Policy farmPolicy, VillagePriorities.Policy priorityPolicy,
                             OpeningFunds openingFunds, HouseholdWellbeing.Policy wellbeing) {
        this(dataFolder,logger,settings,now,economySettings,farmPolicy,priorityPolicy,openingFunds,wellbeing,FoodResponses.Settings.disabled());
    }
    public PopulationSession(File dataFolder,Logger logger,Settings settings,LongSupplier now,EconomySettings economySettings,FarmObservation.Policy farmPolicy,
                             VillagePriorities.Policy priorityPolicy,OpeningFunds openingFunds,HouseholdWellbeing.Policy wellbeing,FoodResponses.Settings responses) {
        this.responses=java.util.Objects.requireNonNull(responses);
        this.logger = logger; this.settings = settings; this.now = now;
        this.wellbeing = java.util.Objects.requireNonNull(wellbeing);
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
    public boolean onlineOnly() { return onlineOnly; }
    public void configureFarmBusinesses(com.blueninjas24.civilizations.industry.FarmBusinessPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure farm businesses before session starts");
        farmBusinessPolicy=java.util.Objects.requireNonNull(policy);
    }
    public com.blueninjas24.civilizations.database.BusinessRuntime.Snapshot businessView() { return businessView.get(); }
    public void configureOreTracking() {
        if(started.get())throw new IllegalStateException("Configure ore tracking before session starts");
        trackOres=true;
    }
    public void configureIndustryRuntime(com.blueninjas24.civilizations.industry.IndustryRuntimePolicy policy) {
        if(started.get())throw new IllegalStateException("Configure industry runtime before session starts");
        industryPolicy=java.util.Objects.requireNonNull(policy);
    }
    public String industryRuntimeDiagnostic() {
        return !industryPolicy.enabled()?"Timed industry disabled in economy.yml":industryFailure!=null
                ?"Timed industry deferred: "+industryFailure:"Timed industry active for saved firms; "+automaticMineDiagnostic;
    }
    public void configureBusinessPurchases(com.blueninjas24.civilizations.industry.BusinessPurchasePolicy policy) {
        if(started.get())throw new IllegalStateException("Configure business purchases before session starts");
        purchasePolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureBusinessOperations(com.blueninjas24.civilizations.industry.BusinessOperationPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure business operation before session starts");
        operationPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureWorkLearning(com.blueninjas24.civilizations.industry.WorkLearningPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure work learning before session starts");
        learningPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureWorkshopFounding(com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure workshop founding before startup");
        workshopFoundingPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureWarehouseFounding(com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure warehouse founding before startup");
        warehouseFoundingPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureWarehouseOpportunities(com.blueninjas24.civilizations.industry.WarehouseOpportunityPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure warehouse opportunities before startup");
        warehouseOpportunityPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureWorkplaceOperations(com.blueninjas24.civilizations.industry.WorkplaceOperationPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure marker operation policy before startup");
        workplaceOperationPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureWorkplaces(com.blueninjas24.civilizations.industry.WorkplaceObserverPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure workplaces before session starts");workplacePolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureWorkshopOpportunities(com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy policy,com.blueninjas24.civilizations.industry.WorkshopStarterPolicy starter) {
        if(started.get())throw new IllegalStateException("Configure workshop review before session starts");
        workshopOpportunityPolicy=java.util.Objects.requireNonNull(policy);workshopStarterPolicy=java.util.Objects.requireNonNull(starter);
    }
    public void configureMineFounding(com.blueninjas24.civilizations.industry.MineFoundingPolicy policy,com.blueninjas24.civilizations.industry.StartupGrantPolicy grants) {
        if(started.get())throw new IllegalStateException("Configure mine founding before session starts");
        mineFoundingPolicy=java.util.Objects.requireNonNull(policy);startupGrantPolicy=java.util.Objects.requireNonNull(grants);
        automaticMineDiagnostic=policy.enabled()?"Automatic mine founding awaiting actual eligible opportunities":"Automatic mine founding disabled";
    }
    public void configureCareerStudy(com.blueninjas24.civilizations.industry.CareerStudyPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure career study before session starts");
        careerStudyPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureStudy(com.blueninjas24.civilizations.industry.StudyPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure study before session starts");
        studyPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureAutomaticResale(com.blueninjas24.civilizations.industry.BusinessResalePolicy policy) {
        if(started.get())throw new IllegalStateException("Configure automatic resale before session starts");
        resalePolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureBusinessStaffing(com.blueninjas24.civilizations.industry.BusinessStaffingPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure staffing before session starts");
        staffingPolicy=java.util.Objects.requireNonNull(policy);
    }
    /** Internal acquisition boundary; listing/valuation/buyer selection are separate lifecycle work. */
    public CompletableFuture<com.blueninjas24.civilizations.database.BusinessSales.Request> sellBusiness(com.blueninjas24.civilizations.database.BusinessSales.Request request) {
        return submit(()->{
            requireReady();
            var sales=new com.blueninjas24.civilizations.database.BusinessSales(database.getConnection(),purchasePolicy);
            if(sales.find(request.operation()).isEmpty()) {
                if(listingClockSuspended)throw new SQLException("Listing clock suspended until reviewed restart: "+listingClockFailure);
                new com.blueninjas24.civilizations.database.BusinessSaleListings(database.getConnection()).observe(now.getAsLong(),loadedWorlds);
                var founding=database.getBusinessRepository().findFounding(request.business()).orElseThrow(()->new SQLException("Unknown business"));
                if(loadedWorlds!=null&&!loadedWorlds.contains(founding.site().worldId()))throw new com.blueninjas24.civilizations.database.BusinessSales.Declined("Business world is absent");
                if(request.step()!=status.get().completedMonths())throw new com.blueninjas24.civilizations.database.BusinessSales.Declined("Sale requires current completed calendar step");
            }
            return sales.sell(request);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.BusinessSaleListings.Listing> openBusinessSale(UUID business,long price,long deadlineMillis) {
        return submit(()->{
            requireReady();if(listingClockSuspended)throw new SQLException("Listing clock suspended until reviewed restart: "+listingClockFailure);
            var founding=database.getBusinessRepository().findFounding(business).orElseThrow(()->new SQLException("Unknown business"));
            if(loadedWorlds!=null&&!loadedWorlds.contains(founding.site().worldId()))throw new com.blueninjas24.civilizations.database.BusinessSales.Declined("Business world is absent");
            return new com.blueninjas24.civilizations.database.BusinessSaleListings(database.getConnection()).open(business,price,deadlineMillis,now.getAsLong(),status.get().completedMonths());
        });
    }
    public String businessSaleDiagnostic(){return listingClockFailure==null?"Saved sale clocks ready; automatic resale "+(resalePolicy.enabled()?"enabled; qualification and real funding required":"disabled in owner settings"):"Sale clock deferred: "+listingClockFailure;}
    public boolean oreDiscoveryReady() { return trackOres && oreHistoryHealthy.get() && status().ready(); }
    public String oreTrackingDiagnostic() {
        return !trackOres?"Ore placement tracking: not configured":oreDiscoveryReady()
                ?"Ore mutation history: trusted; mine startup also checks demand, skills, funding and resource anchors"
                :"Ore discovery and coordinate-backed mining paused: "+oreHistoryIssue.get();
    }
    /** Main-thread adapters may mark a gap immediately; no JDBC access or world work occurs here. */
    public void invalidateOreTracking(String reason) {
        oreCaptureGap.set(true);oreHistoryHealthy.set(false);oreHistoryIssue.set(java.util.Objects.requireNonNull(reason));
    }

    public CompletableFuture<Status> start() {
        return start(true);
    }

    /** Disable the real timer for accelerated tests; explicit pulses retain identical behavior. */
    public CompletableFuture<Status> start(boolean scheduleAutomatically) {
        return start(scheduleAutomatically, null);
    }

    /** Live Paper adapter supplies immutable world IDs; no Bukkit object crosses the owner queue. */
    public CompletableFuture<Status> start(boolean scheduleAutomatically, java.util.Set<UUID> initialWorlds) {
        var initial = initialWorlds == null ? null : java.util.Set.copyOf(initialWorlds);
        if (!started.compareAndSet(false, true)) return CompletableFuture.failedFuture(new IllegalStateException("Session already started"));
        onlineOnly = initial != null;
        return submit(() -> {
            try {
                loadedWorlds = initial;
                database.initialize();
                new com.blueninjas24.civilizations.database.WorkplaceOperations(database.getConnection()).configure(workplaceOperationPolicy);
                if(trackOres) {
                    var fence=new com.blueninjas24.civilizations.database.OreTrackingRepository(database.getConnection()).begin(now.getAsLong());
                    oreHistoryHealthy.set(fence.trusted() && !oreCaptureGap.get());
                    if(oreCaptureGap.get())new com.blueninjas24.civilizations.database.OreTrackingRepository(database.getConnection()).markGap(now.getAsLong());
                    if(!fence.trusted())oreHistoryIssue.set("unclean stop, capture gap or upgraded source coverage; reviewed recovery required");
                }
                businesses = new com.blueninjas24.civilizations.database.BusinessRuntime(database.getConnection(),8,1024);
                businesses.configureResale(resalePolicy,purchasePolicy,settings.seed());
                businesses.configureStaffing(staffingPolicy);
                automaticMines=new com.blueninjas24.civilizations.database.AutomaticMineFounding(database.getConnection(),mineFoundingPolicy,industryPolicy,operationPolicy,startupGrantPolicy,settings.population().adulthoodSteps(),settings.seed(),()->!trackOres||oreHistoryHealthy.get());
                industries = new com.blueninjas24.civilizations.database.IndustryRuntime(database.getConnection(),industryPolicy,operationPolicy,learningPolicy,()->!trackOres||oreHistoryHealthy.get());
                industries.configureFarmToolDemand(farmToolsPolicy,farmToolsBudget);
                industries.start(now.getAsLong());
                new com.blueninjas24.civilizations.database.BusinessSaleListings(database.getConnection()).resume(now.getAsLong());
                var demographic = database.getDemographicRepository();
                var saved = demographic.load(settings.population(), settings.migration());
                if (saved.isEmpty())
                    demographic.initializeClock(settings.seed(), settings.population(), settings.migration());
                else if (saved.get().seed() != settings.seed())
                    throw new SQLException("Saved population seed differs; explicit transition required");
                var clock = database.getCivilizationClockRepository();
                if (clock.load(settings.millisPerMonth()).isEmpty()) clock.initialize(now.getAsLong(), settings.millisPerMonth());
                var accrued = loadedWorlds == null ? clock.accrue(now.getAsLong(), settings.millisPerMonth(),
                        Math.multiplyExact(settings.millisPerMonth(), settings.maximumOfflineMonths()))
                        : new CivilizationTime.Accrual(clock.resumeOnline(now.getAsLong(), settings.millisPerMonth()), 0, 0);
                if (accrued.discardedMillis() > 0) logger.info("Prototype offline calendar cap discarded " + accrued.discardedMillis() + " real milliseconds");
                advance(accrued.anchor().targetSteps());
                if (economySettings != null) {
                    economy = new EconomyRuntimeRepository(database.getConnection(), economySettings, farmPolicy, settings.population().adulthoodSteps(),priorityPolicy,settings.seed(),openingFunds,wellbeing,responses);
                    economy.configureFarmBusinesses(farmBusinessPolicy,operationPolicy);
                    economy.configureFarmTools(farmToolsPolicy,farmToolsBudget,industryPolicy.mining());
                    economy.configureUpkeepContributions(upkeepContributions);
                    economy.configureStudy(studyPolicy);
                    economy.configureWorkshopFounderStudy(new com.blueninjas24.civilizations.database.WorkshopOpportunities(database.getConnection(),workshopOpportunityPolicy,industryPolicy,
                        operationPolicy,startupGrantPolicy,workshopStarterPolicy,farmToolsPolicy,farmToolsBudget),workshopFoundingPolicy,careerStudyPolicy);
                    economy.configureMineFounderStudy(automaticMines,careerStudyPolicy,mineFoundingPolicy);
                    businesses.configureCareerStudy(economy,careerStudyPolicy);
                    economy.configureWorkLearning(learningPolicy);
                    if (loadedWorlds == null) economy.start(now.getAsLong());
                    else economy.startOnline(now.getAsLong(), loadedWorlds);
                    economyView.set(economy.views());
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

    /** Accrue the old world's interval before switching eligibility; saved masks retain queued work. */
    public CompletableFuture<Void> updateLoadedWorlds(java.util.Set<UUID> worlds) {
        var next = java.util.Set.copyOf(worlds);
        long observed = now.getAsLong();
        return submit(() -> {
            requireReady();
            if (loadedWorlds == null) throw new IllegalStateException("World availability requires online mode");
            var clock = database.getCivilizationClockRepository();
            var accrued = clock.accrueOnline(observed, settings.millisPerMonth(), loadedWorlds);
            if (economy != null) economy.updateLoadedWorlds(observed, next);
            if(!listingClockSuspended)try {new com.blueninjas24.civilizations.database.BusinessSaleListings(database.getConnection()).observe(observed,loadedWorlds);}
            catch(SQLException|RuntimeException failure) {
                listingClockSuspended=true;listingClockFailure=failure.getMessage();
                logger.log(Level.SEVERE,"Listing clock boundary failed; new sales suspended until reviewed restart",failure);
            }
            if(!industrySuspended)try {industries.observeAvailability(observed,loadedWorlds);}
            catch(SQLException|RuntimeException failure) {
                industrySuspended=true;industryFailure="failed world boundary; fix cause and restart before industry resumes: "+failure.getMessage();
                logger.log(Level.SEVERE,"Industry clock boundary failed; industry suspended without blocking population world availability",failure);
            }
            loadedWorlds = next;
            advance(accrued.anchor().targetSteps());
            if (economy != null) economyView.set(economy.views());
            return null;
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
    /** Immutable placement evidence is saved on the same owner as discovery, including worlds without towns. */
    public CompletableFuture<Integer> recordOrePlacements(UUID world, String worldName,
            java.util.Collection<VillageObservation.Position> positions, long observedMillis) {
        var frozen = List.copyOf(positions);
        java.util.Objects.requireNonNull(world); java.util.Objects.requireNonNull(worldName);
        return submit(() -> {
            requireReady();
            try {
                database.getVillageIdentityRepository().observeWorld(world,worldName);
                return new com.blueninjas24.civilizations.database.OrePlacementRepository(database.getConnection())
                        .record(world,frozen,observedMillis);
            } catch(SQLException|RuntimeException failure) {
                if(trackOres)try {new com.blueninjas24.civilizations.database.OreTrackingRepository(database.getConnection()).markGap(now.getAsLong());}
                catch(SQLException|RuntimeException fenceFailure){failure.addSuppressed(fenceFailure);}
                throw failure;
            }
        }).whenComplete((result,failure)-> {
            if(trackOres && failure!=null)invalidateOreTracking("placement save failed; reviewed recovery required");
        });
    }

    public CompletableFuture<java.util.Set<VillageObservation.Position>> orePlacementsInChunk(UUID world,
            int chunkX, int chunkZ, int maximumPositions) {
        return submit(() -> {
            requireReady();
            return new com.blueninjas24.civilizations.database.OrePlacementRepository(database.getConnection())
                    .inChunk(world,chunkX,chunkZ,maximumPositions);
        });
    }

    /** One native mutation: immutable placements and actual removed origins share one transaction. */
    public CompletableFuture<com.blueninjas24.civilizations.database.OreMutationRepository.Result> recordOreMutations(UUID world,String name,
            java.util.Collection<VillageObservation.Position> placements,java.util.Collection<VillageObservation.Position> removals,long millis,String reason) {
        var placed=List.copyOf(placements);var removed=List.copyOf(removals);
        java.util.Objects.requireNonNull(world);java.util.Objects.requireNonNull(name);
        return submit(()-> {
            requireReady();
            try {
                database.getVillageIdentityRepository().observeWorld(world,name);
                return new com.blueninjas24.civilizations.database.OreMutationRepository(database.getConnection()).record(world,placed,removed,millis,reason);
            }catch(SQLException|RuntimeException failure) {
                if(trackOres)try {new com.blueninjas24.civilizations.database.OreTrackingRepository(database.getConnection()).markGap(now.getAsLong());}
                catch(SQLException|RuntimeException fenceFailure){failure.addSuppressed(fenceFailure);}
                throw failure;
            }
        }).whenComplete((result,failure)->{if(trackOres&&failure!=null)invalidateOreTracking("mutation save failed; reviewed recovery required");});
    }

    /** Provisional immutable observations only. New founding must independently revalidate saved coordinate claims. */
    public CompletableFuture<List<com.blueninjas24.civilizations.database.MineralSurveyReader.Patch>> surveyMinerals(UUID town,
            List<com.blueninjas24.civilizations.industry.MineralObservation> observations) {
        var frozen=List.copyOf(observations);
        return submit(()-> {
            // Accepted owner work drains during close; public readiness becomes false immediately.
            requireReady();if(!trackOres || !oreHistoryHealthy.get())throw new SQLException("Live ore tracking is not ready; survey paused");
            if(loadedWorlds!=null && frozen.stream().anyMatch(o->!loadedWorlds.contains(o.world())))
                throw new SQLException("Survey world is absent; no live resource observation");
            return new com.blueninjas24.civilizations.database.MineralOpportunities(database.getConnection()).observe(town,frozen);
        });
    }

    /** Diagnostic evidence only; a saved observation never authorizes extraction or founding. */
    public CompletableFuture<List<com.blueninjas24.civilizations.database.MineralOpportunities.Evidence>> mineralOpportunities(UUID town, int limit) {
        return submit(() -> {
            requireReady();
            return new com.blueninjas24.civilizations.database.MineralOpportunities(database.getConnection()).read(town,limit);
        });
    }

    /** Read-only current evidence review on the SQL owner, with immediate mutation and world availability guards. */
    public CompletableFuture<com.blueninjas24.civilizations.database.MineralOpportunities.Review> currentMineralOpportunities(UUID town,long maximumAgeMillis) {
        return submit(()-> {
            requireReady();
            if(!trackOres || !oreHistoryHealthy.get())throw new SQLException("Live ore tracking is not ready; candidate review paused");
            return new com.blueninjas24.civilizations.database.MineralOpportunities(database.getConnection())
                .review(town,now.getAsLong(),maximumAgeMillis,loadedWorlds);
        });
    }

    /** Existing funded input pressure and current observed minerals; no startup or asset mutation. */
    public CompletableFuture<com.blueninjas24.civilizations.database.LocalIndustryDemand.MiningReview> miningInputDemand(UUID town,long maximumAgeMillis,int businessLimit) {
        return submit(()-> {
            requireReady();
            if(!trackOres||!oreHistoryHealthy.get())throw new SQLException("Live ore tracking is not ready; mining demand review paused");
            return new com.blueninjas24.civilizations.database.LocalIndustryDemand(database.getConnection(),industryPolicy,operationPolicy)
                .reviewMining(town,now.getAsLong(),maximumAgeMillis,loadedWorlds,businessLimit);
        });
    }

    public CompletableFuture<com.blueninjas24.civilizations.database.BusinessOperations.Availability> currentBusinessAvailability(UUID business) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.BusinessOperations(database.getConnection(),operationPolicy).evaluate(business);});
    }
    public CompletableFuture<java.util.List<com.blueninjas24.civilizations.database.WorkplaceOperations.Anchor>> workplaceOperationAnchors(UUID after) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.WorkplaceOperations(database.getConnection()).anchors(after,64);});
    }
    public CompletableFuture<Void> confirmWorkplaceOperations(java.util.List<com.blueninjas24.civilizations.database.WorkplaceOperations.Confirmation> evidence) {
        var page=java.util.List.copyOf(evidence);
        return submit(()->{requireReady();new com.blueninjas24.civilizations.database.WorkplaceOperations(database.getConnection()).confirm(page,now.getAsLong(),loadedWorlds);return null;});
    }
    public CompletableFuture<Void> observeWorkplaces(UUID town,com.blueninjas24.civilizations.industry.WorkplaceObservation observation) {
        return submit(()->{requireReady();if(!workplacePolicy.enabled())throw new IllegalStateException("Workplace recognition disabled");new com.blueninjas24.civilizations.database.WorkplaceOpportunities(database.getConnection()).observe(town,observation);return null;});
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.WorkplaceOpportunities.Review> currentWorkplaceOpportunities(UUID town,long maximumAgeMillis,int limit) {
        return submit(()->{requireReady();if(!workplacePolicy.enabled())return new com.blueninjas24.civilizations.database.WorkplaceOpportunities.Review(java.util.List.of(),"Workplace recognition disabled");
            var r=new com.blueninjas24.civilizations.database.WorkplaceOpportunities(database.getConnection()).review(town,now.getAsLong(),maximumAgeMillis,loadedWorlds,limit);
            return new com.blueninjas24.civilizations.database.WorkplaceOpportunities.Review(r.candidates().stream().filter(c->workplacePolicy.kinds().contains(c.kind())).toList(),r.explanation()+"; current enabled marker types enforced");});
    }
    public CompletableFuture<Void> observeFarms(FarmObservation observation) {
        return submit(() -> {
            requireReady(); if (economy == null) throw new IllegalStateException("Economy not enabled in this session");
            economy.observe(observation, now.getAsLong()); economyView.set(economy.views()); return null;
        });
    }
    /** Trusted read-only investment preparation; no business, course or reserved money. */
    public CompletableFuture<com.blueninjas24.civilizations.database.WorkshopOpportunities.Review> currentWorkshopOpportunities(UUID town) {
        return submit(()-> {
            requireReady();
            if(!workplacePolicy.enabled()||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.SMITHING_TABLE))
                return new com.blueninjas24.civilizations.database.WorkshopOpportunities.Review(java.util.List.of(),"Smithing workplace recognition disabled");
            return new com.blueninjas24.civilizations.database.WorkshopOpportunities(database.getConnection(),workshopOpportunityPolicy,industryPolicy,
                operationPolicy,startupGrantPolicy,workshopStarterPolicy,farmToolsPolicy,farmToolsBudget).review(town,now.getAsLong(),loadedWorlds);
        });
    }
    private com.blueninjas24.civilizations.database.WorkshopInvestment workshopInvestment() {
        var evidence=new com.blueninjas24.civilizations.database.WorkshopOpportunities(database.getConnection(),workshopOpportunityPolicy,industryPolicy,
                operationPolicy,startupGrantPolicy,workshopStarterPolicy,farmToolsPolicy,farmToolsBudget);
        return new com.blueninjas24.civilizations.database.WorkshopInvestment(database.getConnection(),evidence,workshopFoundingPolicy,workshopOpportunityPolicy,
                industryPolicy,startupGrantPolicy,workshopStarterPolicy,settings.population().adulthoodSteps(),settings.seed());
    }
    /** Read-only mineral-chain warehouse prospects; never claims a marker or pays a grant. */
    public CompletableFuture<com.blueninjas24.civilizations.database.WarehouseOpportunities.Review> currentWarehouseOpportunities(UUID town) {
        return submit(()-> {
            requireReady();
            if(!workplacePolicy.enabled()||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.BARREL))
                return new com.blueninjas24.civilizations.database.WarehouseOpportunities.Review(java.util.List.of(),"Barrel recognition disabled");
            return new com.blueninjas24.civilizations.database.WarehouseOpportunities(database.getConnection(),warehouseOpportunityPolicy,industryPolicy,
                operationPolicy,startupGrantPolicy,mineFoundingPolicy,()->!trackOres||oreHistoryHealthy.get()).review(town,now.getAsLong(),loadedWorlds);
        });
    }
    private com.blueninjas24.civilizations.database.WarehouseInvestment warehouseInvestment() {
        var evidence=new com.blueninjas24.civilizations.database.WarehouseOpportunities(database.getConnection(),warehouseOpportunityPolicy,industryPolicy,
                operationPolicy,startupGrantPolicy,mineFoundingPolicy,()->!trackOres||oreHistoryHealthy.get());
        return new com.blueninjas24.civilizations.database.WarehouseInvestment(database.getConnection(),evidence,warehouseFoundingPolicy,warehouseOpportunityPolicy,
                industryPolicy,startupGrantPolicy,settings.population().adulthoodSteps(),settings.seed());
    }
    /** Read-only qualified merchant preparation; does not train, schedule, reserve or spend. */
    public CompletableFuture<com.blueninjas24.civilizations.database.WarehouseInvestment.Preparation> prepareWarehouseInvestment(UUID town) {
        return submit(()-> {
            requireReady();
            if(!workplacePolicy.enabled()||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.BARREL))
                return new com.blueninjas24.civilizations.database.WarehouseInvestment.Preparation(java.util.Optional.empty(),"Barrel recognition disabled");
            return warehouseInvestment().prepare(town,status.get().completedMonths(),now.getAsLong(),loadedWorlds);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.BusinessStartupGrants.Warehouse> acceptWarehouseInvestment(
            com.blueninjas24.civilizations.database.WarehouseInvestment.Draft draft,com.blueninjas24.civilizations.database.WorkplaceOperations.Confirmation confirmation) {
        return submit(()-> {
            requireReady();
            if(economy==null||economyFailure!=null||status.get().remainingMonths()>0||!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.BARREL))
                throw new com.blueninjas24.civilizations.database.BusinessStartupGrants.Declined("Warehouse acceptance waits for enabled current food/population/barrel evidence");
            return warehouseInvestment().accept(draft,confirmation,now.getAsLong(),loadedWorlds);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.WorkshopInvestment.Preparation> prepareWorkshopInvestment(UUID town) {
        return submit(()-> {
            requireReady();
            if(!workplacePolicy.enabled()||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.SMITHING_TABLE))
                return new com.blueninjas24.civilizations.database.WorkshopInvestment.Preparation(java.util.Optional.empty(),"Smithing workplace recognition disabled");
            return workshopInvestment().prepare(town,status.get().completedMonths(),now.getAsLong(),loadedWorlds);
        });
    }
    /** Automatic review may sponsor one real future-only course; read-only preparation remains separate. */
    public CompletableFuture<com.blueninjas24.civilizations.database.WorkshopInvestment.Preparation> reviewAutomaticWorkshopInvestment(UUID town) {
        return submit(()-> {
            requireReady();
            String waiting=economy==null?"Food economy disabled":economyFailure!=null?"Food economy deferred: "+economyFailure
                    :status.get().remainingMonths()>0?"Population backlog must finish":!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.SMITHING_TABLE)?"Smithing workplace recognition disabled":null;
            if(waiting!=null)return new com.blueninjas24.civilizations.database.WorkshopInvestment.Preparation(java.util.Optional.empty(),"Automatic workshop waits: "+waiting);
            var result=workshopInvestment().prepare(town,status.get().completedMonths(),now.getAsLong(),loadedWorlds);
            if(result.draft().isPresent())return result;
            String training=economy.reviewWorkshopFounderStudy(town,now.getAsLong());
            return new com.blueninjas24.civilizations.database.WorkshopInvestment.Preparation(java.util.Optional.empty(),result.explanation()+"; "+training);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.BusinessStartupGrants.Workshop> acceptWorkshopInvestment(
            com.blueninjas24.civilizations.database.WorkshopInvestment.Draft draft,com.blueninjas24.civilizations.database.WorkplaceOperations.Confirmation confirmation) {
        return submit(()-> {
            requireReady();
            if(economy==null||economyFailure!=null||status.get().remainingMonths()>0||!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.SMITHING_TABLE))
                throw new com.blueninjas24.civilizations.database.BusinessStartupGrants.Declined("Workshop acceptance waits for enabled current food/population/marker evidence");
            return workshopInvestment().accept(draft,confirmation,now.getAsLong(),loadedWorlds);
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
        return reserveFoodPurchase(terms,policy,foods,FoodBargaining.Policy.disabled());
    }
    public CompletableFuture<String> reserveFoodPurchase(FoodPurchases.Terms terms, FoodPurchases.Policy policy, FoodAidPreview.Policy foods,FoodBargaining.Policy bargaining) {
        return submit(() -> {
            requireReady(); if(economy==null)throw new IllegalStateException("Economy not enabled");
            var result=economy.reserveFoodPurchase(terms,policy,foods,bargaining,now.getAsLong());economyView.set(economy.views());return result;
        });
    }
    public CompletableFuture<FoodBargaining.Deal> bargain(FoodPurchases.Terms requested,FoodPurchases.Policy purchases,FoodAidPreview.Policy foods,FoodBargaining.Policy bargaining) {
        return submit(()->{requireReady();if(economy==null)throw new IllegalStateException("Economy not enabled");
            return economy.bargain(requested,purchases,foods,bargaining,now.getAsLong());});
    }
    public CompletableFuture<FoodPurchases.Terms> bargainAcceptance(UUID player,UUID town,FoodBargaining.Policy bargaining) {
        return submit(()->{requireReady();if(economy==null)throw new IllegalStateException("Economy not enabled");
            return economy.bargainAcceptance(player,town,bargaining,now.getAsLong());});
    }
    public CompletableFuture<Boolean> completeFoodPurchase(FoodPurchases.Terms terms) {
        return submit(() -> {
            requireReady(); if(economy==null)throw new IllegalStateException("Economy not enabled");
            var result=economy.completeFoodPurchase(terms,now.getAsLong());economyView.set(economy.views());return result;
        });
    }
    public CompletableFuture<FoodBarters.Terms> quoteFoodBarter(UUID operation,UUID player,UUID town,long units,String item,int count,
                                                               Commodity commodity,FoodBarters.Policy policy,FoodAidPreview.Policy foods) {
        return submit(()->{requireReady();if(economy==null)throw new IllegalStateException("Economy not enabled");
            return economy.quoteFoodBarter(operation,player,town,units,item,count,commodity,policy,foods,now.getAsLong());});
    }
    public CompletableFuture<String> reserveFoodBarter(FoodBarters.Terms terms,FoodBarters.Policy policy,FoodAidPreview.Policy foods) {
        return submit(()->{requireReady();if(economy==null)throw new IllegalStateException("Economy not enabled");
            var result=economy.reserveFoodBarter(terms,policy,foods,now.getAsLong());economyView.set(economy.views());return result;});
    }
    public CompletableFuture<Boolean> completeFoodBarter(FoodBarters.Terms terms) {
        return submit(()->{requireReady();if(economy==null)throw new IllegalStateException("Economy not enabled");
            var result=economy.completeFoodBarter(terms,now.getAsLong());economyView.set(economy.views());return result;});
    }
    public CompletableFuture<Boolean> cancelFoodBarter(FoodBarters.Terms terms) {
        return submit(()->{requireReady();if(economy==null)throw new IllegalStateException("Economy not enabled");
            var result=economy.cancelFoodBarter(terms);economyView.set(economy.views());return result;});
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
        if(trackOres&&oreCaptureGap.get())new com.blueninjas24.civilizations.database.OreTrackingRepository(database.getConnection()).markGap(now.getAsLong());
        var clock = database.getCivilizationClockRepository();
        var queuedTime = loadedWorlds == null ? clock.accrue(now.getAsLong(), settings.millisPerMonth(), Long.MAX_VALUE)
                : clock.accrueOnline(now.getAsLong(), settings.millisPerMonth(), loadedWorlds);
        var previous = status.get();
        status.set(new Status(previous.ready(), previous.completedMonths(), queuedTime.anchor().targetSteps(),
                previous.livingPeople(), previous.lastBatchNanos(), previous.failure()));
        advance(queuedTime.anchor().targetSteps());
        if(!listingClockSuspended)try {new com.blueninjas24.civilizations.database.BusinessSaleListings(database.getConnection()).observe(now.getAsLong(),loadedWorlds);listingClockFailure=null;}
        catch(SQLException|RuntimeException failure) {
            if(!java.util.Objects.equals(listingClockFailure,failure.getMessage()))logger.log(Level.SEVERE,"Listing clocks deferred; no sale terms or funds changed",failure);
            listingClockFailure=failure.getMessage();
        }
        // Close expired unsold offers before new industry/economy work; refresh is bounded.
        businessView.set(businesses.refresh(status.get().completedMonths(),loadedWorlds,now.getAsLong(),!listingClockSuspended&&listingClockFailure==null));
        if(!industrySuspended)try {industries.pulse(now.getAsLong(),loadedWorlds);industryFailure=null;}
        catch(SQLException|RuntimeException failure) {
            if(!java.util.Objects.equals(industryFailure,failure.getMessage()))logger.log(Level.SEVERE,"Industry update deferred; saved work remains retryable",failure);
            industryFailure=failure.getMessage();
        }
        if (economy != null && status.get().remainingMonths() == 0) {
            try { economy.pulse(now.getAsLong(), false); economyView.set(economy.views()); economyFailure = null; }
            catch (SQLException | RuntimeException failure) {
                if (!java.util.Objects.equals(economyFailure, failure.getMessage())) logger.log(Level.SEVERE, "Economy update deferred; saved periods remain retryable", failure);
                economyFailure = failure.getMessage();
            }
        }
        if(mineFoundingPolicy.enabled()&&economy!=null&&status.get().remainingMonths()==0&&economyFailure==null) {
            try {automaticMineDiagnostic=automaticMines.pulse(status.get().completedMonths(),now.getAsLong(),loadedWorlds);}
            catch(SQLException|RuntimeException failure) {
                if(!java.util.Objects.equals(automaticMineDiagnostic,"Mine founding deferred: "+failure.getMessage()))logger.log(Level.SEVERE,"Automatic mine founding deferred; startup assets remain retryable",failure);
                automaticMineDiagnostic="Mine founding deferred: "+failure.getMessage();
            }
        }
    }
    private void advance(long target) throws SQLException {
        long began = System.nanoTime();
        var result = database.getDemographicRepository().advance(target, settings.monthsPerBatch(), settings.population(), settings.migration(),
                loadedWorlds == null ? null : database.getCivilizationClockRepository().activeWorlds());
        var state = result.checkpoint().state(); publish(state);
        businessView.set(businesses.refresh(result.checkpoint().completedSteps(),loadedWorlds));
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
                    if (failure instanceof SQLException && !(failure instanceof EconomyRuntimeRepository.PurchaseDeclined)
                            && !(failure instanceof com.blueninjas24.civilizations.database.BusinessSales.Declined)) reportFailure(failure);
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
        owner.execute(()-> {
            try {
                if(trackOres && readyForWork)new com.blueninjas24.civilizations.database.OreTrackingRepository(database.getConnection())
                        .finish(now.getAsLong(),oreHistoryHealthy.get());
            } catch(SQLException|RuntimeException failure) {
                logger.log(Level.SEVERE,"Ore tracking shutdown fence failed; discovery will require reviewed recovery",failure);
            } finally {database.close();}
        });
        owner.shutdown();
        if (Thread.currentThread() == ownerThread.get()) return;
        try {
            if (!owner.awaitTermination(5, TimeUnit.SECONDS)) logger.warning("Population shutdown still finishing on its owner thread");
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
}
