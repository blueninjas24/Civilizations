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
    private final AtomicReference<java.util.Set<UUID>> villageAnimals=new AtomicReference<>(java.util.Set.of());
    private final java.util.Set<UUID> adoptingAnimals=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public java.util.Set<UUID> villageAnimals(){
        if(adoptingAnimals.isEmpty())return villageAnimals.get();
        var result=new java.util.HashSet<>(villageAnimals.get());result.addAll(adoptingAnimals);return java.util.Set.copyOf(result);
    }
    private final AtomicBoolean livestockHistoryHealthy=new AtomicBoolean(),livestockCaptureGap=new AtomicBoolean();
    private final java.util.Set<UUID> pendingLivestockLosses=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean livestockTrackingStarted;
    public boolean livestockCaptureReady(){return status().ready()&&livestockHistoryHealthy.get()&&!livestockCaptureGap.get()&&pendingLivestockLosses.isEmpty();}
    public String livestockCaptureDiagnostic(){
        if(!status().ready())return "Livestock capture: starting or unavailable";
        if(!livestockHistoryHealthy.get()||livestockCaptureGap.get())return "Livestock capture: incomplete history; review needed before live herd production";
        if(!pendingLivestockLosses.isEmpty())return "Livestock capture: waiting for "+pendingLivestockLosses.size()+" physical loss saves";
        return "Livestock capture: trusted; saved herd work "+(livestockRuntimePolicy.enabled()&&livestockPolicy.enabled()?"configured":"disabled")+"; automatic adoption "+(adoptionPolicy.enabled()?"configured":"disabled");
    }
    public void markLivestockCaptureGap(){
        if(!livestockCaptureGap.compareAndSet(false,true))return;
        if(!started.get()||closing.get())return;
        submit(()->{requireReady();new com.blueninjas24.civilizations.database.LivestockCapture(database.getConnection()).markGap(now.getAsLong());return null;})
                .whenComplete((ignored,failure)->{if(failure!=null)logger.log(Level.SEVERE,"Livestock capture gap save deferred; live readiness remains false",failure);});
    }
    private final AtomicReference<PopulationState> snapshot = new AtomicReference<>();
    private final AtomicReference<ProjectionView> projectionView = new AtomicReference<>();
    private final AtomicBoolean started = new AtomicBoolean(), closing = new AtomicBoolean();
    private final AtomicInteger queued = new AtomicInteger();
    private boolean trackForests, forestTrackingStarted;
    private final AtomicBoolean forestHistoryHealthy=new AtomicBoolean(),forestCaptureGap=new AtomicBoolean();
    private final AtomicInteger pendingForestMutations=new AtomicInteger();
    private final AtomicInteger pendingOreMutations=new AtomicInteger();
    private final java.util.concurrent.atomic.AtomicLong oreMutationRevision=new java.util.concurrent.atomic.AtomicLong();
    private final AtomicReference<String> forestHistoryIssue=new AtomicReference<>("starting");
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
    private com.blueninjas24.civilizations.industry.FishingPolicy fishingPolicy=com.blueninjas24.civilizations.industry.FishingPolicy.disabled();
    private com.blueninjas24.civilizations.database.IndustryRuntime industries;
    private com.blueninjas24.civilizations.industry.BusinessPurchasePolicy purchasePolicy=com.blueninjas24.civilizations.industry.BusinessPurchasePolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy workshopFoundingPolicy=com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy craftsmanFoundingPolicy=com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy craftsmanOpportunityPolicy=com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy.disabled();
    private com.blueninjas24.civilizations.industry.BakeryFoundingPolicy bakeryFoundingPolicy=com.blueninjas24.civilizations.industry.BakeryFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.industry.BakeryMarketPolicy bakeryMarketPolicy=com.blueninjas24.civilizations.industry.BakeryMarketPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy bakeryOpportunityPolicy=com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WarehouseOpportunityPolicy warehouseOpportunityPolicy=com.blueninjas24.civilizations.industry.WarehouseOpportunityPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy warehouseFoundingPolicy=com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkplaceOperationPolicy workplaceOperationPolicy=com.blueninjas24.civilizations.industry.WorkplaceOperationPolicy.prototype();
    private com.blueninjas24.civilizations.industry.LivestockMarketPolicy livestockMarketPolicy=com.blueninjas24.civilizations.industry.LivestockMarketPolicy.disabled();
    private com.blueninjas24.civilizations.industry.LivestockWorkingArea livestockWorkingArea=com.blueninjas24.civilizations.industry.LivestockWorkingArea.prototype();
    private com.blueninjas24.civilizations.industry.LivestockPolicy livestockPolicy=com.blueninjas24.civilizations.industry.LivestockPolicy.disabled();
    private com.blueninjas24.civilizations.industry.LivestockRuntimePolicy livestockRuntimePolicy=com.blueninjas24.civilizations.industry.LivestockRuntimePolicy.disabled();
    private com.blueninjas24.civilizations.industry.LivestockSlaughterPolicy slaughterPolicy=com.blueninjas24.civilizations.industry.LivestockSlaughterPolicy.disabled();
    private volatile int pendingSlaughters;
    public String livestockSlaughterDiagnostic(){return "Saved slaughter permits awaiting exact native outcome: "+pendingSlaughters;}
    private void refreshSlaughterCount()throws java.sql.SQLException {pendingSlaughters=new com.blueninjas24.civilizations.database.LivestockSlaughter(database.getConnection()).pendingCount();}
    public void configureLivestockSlaughter(com.blueninjas24.civilizations.industry.LivestockSlaughterPolicy policy){
        if(started.get())throw new IllegalStateException("Configure slaughter before starting population");slaughterPolicy=java.util.Objects.requireNonNull(policy);
    }
    public CompletableFuture<java.util.Optional<com.blueninjas24.civilizations.database.LivestockSlaughter.Permit>> prepareLivestockSlaughter(
            com.blueninjas24.civilizations.database.LivestockPresence.Sight sight) {
        return submit(()->{requireReady();
            if(!industryPolicy.enabled()||!livestockRuntimePolicy.enabled()||!livestockCaptureReady())return java.util.Optional.empty();
            var result=new com.blueninjas24.civilizations.database.LivestockSlaughter(database.getConnection()).prepare(sight,now.getAsLong(),
                    loadedWorlds==null?java.util.Set.of():loadedWorlds,livestockPolicy,slaughterPolicy,operationPolicy,livestockWorkingArea,this::livestockCaptureReady);
            refreshSlaughterCount();return result;
        });
    }
    public CompletableFuture<Void> cancelLivestockSlaughter(UUID permit,String reason){return submit(()->{requireReady();new com.blueninjas24.civilizations.database.LivestockSlaughter(database.getConnection()).cancel(permit,reason);refreshSlaughterCount();return null;});}
    public void configureLivestockWork(com.blueninjas24.civilizations.industry.LivestockPolicy policy,com.blueninjas24.civilizations.industry.LivestockRuntimePolicy work){
        if(started.get())throw new IllegalStateException("Configure herd work before session starts");
        livestockPolicy=java.util.Objects.requireNonNull(policy);livestockRuntimePolicy=java.util.Objects.requireNonNull(work);
    }
    private com.blueninjas24.civilizations.industry.LivestockAdoptionPolicy adoptionPolicy=com.blueninjas24.civilizations.industry.LivestockAdoptionPolicy.disabled();
    public void configureLivestockAdoption(com.blueninjas24.civilizations.industry.LivestockAdoptionPolicy policy){
        if(started.get())throw new IllegalStateException("Configure adoption before session starts");adoptionPolicy=java.util.Objects.requireNonNull(policy);
    }
    private com.blueninjas24.civilizations.database.LivestockInvestment livestockInvestment(){
        return new com.blueninjas24.civilizations.database.LivestockInvestment(database.getConnection(),adoptionPolicy,livestockPolicy,startupGrantPolicy,settings.population().adulthoodSteps());
    }
    public CompletableFuture<java.util.List<com.blueninjas24.civilizations.database.LivestockInvestment.Village>> livestockVillages(){
        return submit(()->{requireReady();return livestockInvestment().villages();});
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.LivestockInvestment.Result> adoptLivestock(
            com.blueninjas24.civilizations.industry.LivestockOpportunity evidence,java.util.function.BooleanSupplier valid){
        var ids=evidence.animals().stream().map(com.blueninjas24.civilizations.industry.LivestockObservation.Animal::id).toList();
        adoptingAnimals.addAll(ids); // Death/product guards see candidates before the SQL handoff.
        return submit(()->{requireReady();
            if(economy==null||economyFailure!=null||status.get().remainingMonths()>0||!industryPolicy.enabled()||!livestockRuntimePolicy.enabled())
                return new com.blueninjas24.civilizations.database.LivestockInvestment.Result(false,null,"Current enabled population/economy and herd runtime required");
            var result=livestockInvestment().accept(evidence,status.get().completedMonths(),now.getAsLong(),loadedWorlds,()->valid.getAsBoolean()&&livestockCaptureReady());
            villageAnimals.set(new com.blueninjas24.civilizations.database.AnimalOwnershipRepository(database.getConnection()).villageAnimals());return result;
        }).whenComplete((result,failure)->adoptingAnimals.removeAll(ids));
    }
    private com.blueninjas24.civilizations.industry.BusinessOperationPolicy operationPolicy=com.blueninjas24.civilizations.industry.BusinessOperationPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkLearningPolicy learningPolicy=com.blueninjas24.civilizations.industry.WorkLearningPolicy.disabled();
    private com.blueninjas24.civilizations.industry.MineFoundingPolicy mineFoundingPolicy=com.blueninjas24.civilizations.industry.MineFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.industry.StartupGrantPolicy startupGrantPolicy=com.blueninjas24.civilizations.industry.StartupGrantPolicy.disabled();
    private com.blueninjas24.civilizations.database.AutomaticMineFounding automaticMines;
    private volatile String automaticMineDiagnostic="Automatic mine founding disabled";
    private com.blueninjas24.civilizations.industry.ForestryFoundingPolicy forestryFoundingPolicy=com.blueninjas24.civilizations.industry.ForestryFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.database.AutomaticForestryFounding automaticForestry;
    private volatile String automaticForestryDiagnostic="Automatic logging startup disabled";
    public String automaticForestryDiagnostic(){return automaticForestryDiagnostic;}
    public void configureForestryFounding(com.blueninjas24.civilizations.industry.ForestryFoundingPolicy policy){
        if(started.get())throw new IllegalStateException("Configure forestry founding before session starts");forestryFoundingPolicy=java.util.Objects.requireNonNull(policy);
    }
    private com.blueninjas24.civilizations.industry.QuarryFoundingPolicy quarryFoundingPolicy=com.blueninjas24.civilizations.industry.QuarryFoundingPolicy.disabled();
    private com.blueninjas24.civilizations.database.AutomaticQuarryFounding automaticQuarries;
    private volatile String automaticQuarryDiagnostic="Automatic quarry/sandpit founding disabled";
    public void configureQuarryFounding(com.blueninjas24.civilizations.industry.QuarryFoundingPolicy policy){
        if(started.get())throw new IllegalStateException("Configure quarry founding before session starts");quarryFoundingPolicy=java.util.Objects.requireNonNull(policy);
    }
    private com.blueninjas24.civilizations.industry.WorkplaceObserverPolicy workplacePolicy=com.blueninjas24.civilizations.industry.WorkplaceObserverPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy workshopOpportunityPolicy=com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy.disabled();
    private com.blueninjas24.civilizations.industry.WorkshopStarterPolicy workshopStarterPolicy=com.blueninjas24.civilizations.industry.WorkshopStarterPolicy.disabled();
    private com.blueninjas24.civilizations.industry.CareerStudyPolicy careerStudyPolicy=com.blueninjas24.civilizations.industry.CareerStudyPolicy.disabled();
    private com.blueninjas24.civilizations.industry.StudyPolicy studyPolicy=com.blueninjas24.civilizations.industry.StudyPolicy.disabled();
    private com.blueninjas24.civilizations.industry.VillageSupplyPolicy villageSupplyPolicy=com.blueninjas24.civilizations.industry.VillageSupplyPolicy.disabled();
    public void configureVillageSupplies(com.blueninjas24.civilizations.industry.VillageSupplyPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure village supplies before session starts");
        villageSupplyPolicy=java.util.Objects.requireNonNull(policy);
    }
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
    private com.blueninjas24.civilizations.economy.GrainHarvestPolicy grainHarvestPolicy=com.blueninjas24.civilizations.economy.GrainHarvestPolicy.disabled();
    public void configureGrainHarvest(com.blueninjas24.civilizations.economy.GrainHarvestPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure grain harvest before session starts");grainHarvestPolicy=java.util.Objects.requireNonNull(policy);
    }
    private com.blueninjas24.civilizations.economy.FarmMarketPolicy farmMarketPolicy=com.blueninjas24.civilizations.economy.FarmMarketPolicy.disabled();
    public void configureFarmMarket(com.blueninjas24.civilizations.economy.FarmMarketPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure farm market before session starts");farmMarketPolicy=java.util.Objects.requireNonNull(policy);
    }
    public com.blueninjas24.civilizations.database.BusinessRuntime.Snapshot businessView() { return businessView.get(); }
    public void configureForestTracking() {
        if(started.get())throw new IllegalStateException("Configure forest tracking before session starts");
        trackForests=true;
    }
    public boolean forestDiscoveryReady(){return trackForests&&forestHistoryHealthy.get()&&!forestCaptureGap.get()&&pendingForestMutations.get()==0&&status().ready();}
    public String forestTrackingDiagnostic(){
        if(!trackForests)return "Forest tracking: not configured";
        if(!forestHistoryHealthy.get()||forestCaptureGap.get())return "Forestry paused: "+forestHistoryIssue.get();
        if(pendingForestMutations.get()>0)return "Forestry paused: waiting for "+pendingForestMutations.get()+" native tree mutation saves";
        return status().ready()?"Forest history: trusted; pending saves 0":"Forest tracking: starting or stopped";
    }
    public void invalidateForestTracking(String reason){
        forestCaptureGap.set(true);forestHistoryHealthy.set(false);forestHistoryIssue.set(java.util.Objects.requireNonNull(reason));
    }
    public void configureOreTracking() {
        if(started.get())throw new IllegalStateException("Configure ore tracking before session starts");
        trackOres=true;
    }
    public void configureIndustryRuntime(com.blueninjas24.civilizations.industry.IndustryRuntimePolicy policy) {
        if(started.get())throw new IllegalStateException("Configure industry runtime before session starts");
        industryPolicy=java.util.Objects.requireNonNull(policy);
    }
    private com.blueninjas24.civilizations.industry.QuarryPolicy quarryPolicy=com.blueninjas24.civilizations.industry.QuarryPolicy.disabled();
    private com.blueninjas24.civilizations.industry.ForestryPolicy forestryPolicy=com.blueninjas24.civilizations.industry.ForestryPolicy.disabled();
    public void configureForestry(com.blueninjas24.civilizations.industry.ForestryPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure forestry before session starts");forestryPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureQuarries(com.blueninjas24.civilizations.industry.QuarryPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure quarries before session starts");quarryPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureFishing(com.blueninjas24.civilizations.industry.FishingPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure fishing before session starts");
        fishingPolicy=java.util.Objects.requireNonNull(policy);
    }
    private com.blueninjas24.civilizations.industry.WaterObserverPolicy waterPolicy=com.blueninjas24.civilizations.industry.WaterObserverPolicy.disabled();
    public void configureWater(com.blueninjas24.civilizations.industry.WaterObserverPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure water recognition before session starts");
        waterPolicy=java.util.Objects.requireNonNull(policy);
    }
    public CompletableFuture<Void> observeWater(UUID town,com.blueninjas24.civilizations.industry.WaterScan.Observation observation) {
        return submit(()->{requireReady();
            if(!waterPolicy.enabled())throw new IllegalStateException("Water recognition disabled");
            if(observation.minimumDepth()!=waterPolicy.minimumDepth())throw new IllegalArgumentException("Water depth policy changed since capture");
            new com.blueninjas24.civilizations.database.WaterOpportunities(database.getConnection()).observe(town,observation);return null;});
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.WaterOpportunities.Review> currentWaterOpportunities(UUID town,long maximumAgeMillis,int limit) {
        return submit(()->{requireReady();
            if(!waterPolicy.enabled())return new com.blueninjas24.civilizations.database.WaterOpportunities.Review(java.util.List.of(),"Water recognition disabled");
            return new com.blueninjas24.civilizations.database.WaterOpportunities(database.getConnection()).review(town,now.getAsLong(),maximumAgeMillis,loadedWorlds,waterPolicy.minimumDepth(),limit);});
    }
    private com.blueninjas24.civilizations.industry.FisheryOpportunityPolicy fisheryOpportunityPolicy=com.blueninjas24.civilizations.industry.FisheryOpportunityPolicy.disabled();
    public void configureFisheryOpportunities(com.blueninjas24.civilizations.industry.FisheryOpportunityPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure fishery opportunities before session starts");
        fisheryOpportunityPolicy=java.util.Objects.requireNonNull(policy);
    }
    private com.blueninjas24.civilizations.industry.FisheryFoundingPolicy fisheryFoundingPolicy=com.blueninjas24.civilizations.industry.FisheryFoundingPolicy.disabled();
    public void configureFisheryFounding(com.blueninjas24.civilizations.industry.FisheryFoundingPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure fishery founding before session starts");
        fisheryFoundingPolicy=java.util.Objects.requireNonNull(policy);
    }
    private com.blueninjas24.civilizations.industry.FisheryShorelinePolicy fisheryShorelinePolicy=com.blueninjas24.civilizations.industry.FisheryShorelinePolicy.disabled();
    public void configureFisheryShoreline(com.blueninjas24.civilizations.industry.FisheryShorelinePolicy policy) {
        if(started.get())throw new IllegalStateException("Configure fishery shoreline before session starts");fisheryShorelinePolicy=java.util.Objects.requireNonNull(policy);
    }
    public record FisheryWorksite(com.blueninjas24.civilizations.database.FisheryWorkplaces.Binding binding,com.blueninjas24.civilizations.industry.WaterPatch.Patch original) {}
    public CompletableFuture<java.util.List<FisheryWorksite>> fisheryWorksites(UUID after,int limit) {
        return submit(()->{requireReady();var result=new java.util.ArrayList<FisheryWorksite>();
            for(var binding:new com.blueninjas24.civilizations.database.FisheryWorkplaces(database.getConnection()).page(after,limit)) {
                var original=new com.blueninjas24.civilizations.industry.WaterPatch.Patch(new com.blueninjas24.civilizations.industry.FisheryObservation(binding.world(),0,true,new com.blueninjas24.civilizations.database.FisheryRepository(database.getConnection()).columns(binding.site())),binding.surfaceY());
                result.add(new FisheryWorksite(binding,original));
            }return java.util.List.copyOf(result);});
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.FisheryWorkplaces.Receipt> reconcileFisheryWorksite(UUID site,com.blueninjas24.civilizations.industry.ShorelineEvidence bank,com.blueninjas24.civilizations.industry.FisheryHabitatObservation habitat) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.FisheryWorksiteReview(database.getConnection()).reconcile(site,bank,habitat,now.getAsLong(),fisheryOpportunityPolicy.evidenceLifetimeMillis(),loadedWorlds);});
    }
    private com.blueninjas24.civilizations.database.FisheryInvestment fisheryInvestment() {
        var opportunities=new com.blueninjas24.civilizations.database.FisheryOpportunities(database.getConnection(),fisheryOpportunityPolicy,waterPolicy,fishingPolicy,industryPolicy,startupGrantPolicy,villageSupplyPolicy,studyPolicy,operationPolicy);
        return new com.blueninjas24.civilizations.database.FisheryInvestment(database.getConnection(),opportunities,fisheryFoundingPolicy,fisheryOpportunityPolicy,industryPolicy,waterPolicy,fisheryShorelinePolicy,startupGrantPolicy,settings.population().adulthoodSteps(),settings.seed());
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.BusinessStartupGrants.Fishery> acceptFisheryInvestment(com.blueninjas24.civilizations.database.FisheryInvestment.Proposal proposal,com.blueninjas24.civilizations.industry.ShorelineEvidence evidence) {
        return submit(()->{requireReady();return fisheryInvestment().accept(proposal,evidence,now.getAsLong(),loadedWorlds,fishingPolicy);});
    }
    /** Live water/bank caller may sponsor or refresh one supply-backed future first-owner course. */
    public CompletableFuture<String> reviewFisheryFounderStudy(com.blueninjas24.civilizations.database.FisheryOpportunities.Opportunity opportunity,com.blueninjas24.civilizations.industry.ShorelineEvidence bank) {
        return submit(()->{requireReady();if(economy==null||economyFailure!=null||status.get().remainingMonths()>0)return "First fishing-owner study waits for current enabled food/population economy";
            return economy.reviewFisheryFounderStudy(opportunity,bank,now.getAsLong());});
    }
    /** No invented shoreline point or startup debit; actual acceptance remains separate. */
    public CompletableFuture<com.blueninjas24.civilizations.database.FisheryInvestment.Preparation> prepareFisheryInvestment(UUID town) {
        return submit(()->{requireReady();return fisheryInvestment().prepare(town,status.get().completedMonths(),now.getAsLong(),loadedWorlds);});
    }
    /** Read-only SQL-owner prospects; neither schedules founders nor spends grants. */
    public CompletableFuture<com.blueninjas24.civilizations.database.FisheryOpportunities.Review> currentFisheryOpportunities(UUID town) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.FisheryOpportunities(database.getConnection(),fisheryOpportunityPolicy,
                waterPolicy,fishingPolicy,industryPolicy,startupGrantPolicy,villageSupplyPolicy,studyPolicy,operationPolicy).review(town,now.getAsLong(),loadedWorlds);});
    }
    public CompletableFuture<java.util.List<com.blueninjas24.civilizations.industry.WaterPatch.Patch>> currentFishingPatches(UUID town,long maximumAgeMillis,int maximumColumns,int maximumSites) {
        return submit(()->{requireReady();
            if(!waterPolicy.enabled()||!fishingPolicy.enabled())return java.util.List.of();
            return new com.blueninjas24.civilizations.database.WaterOpportunities(database.getConnection()).patches(town,now.getAsLong(),maximumAgeMillis,loadedWorlds,
                    waterPolicy.minimumDepth(),fishingPolicy.minimumWaterColumns(),maximumColumns,maximumSites);});
    }
    public String industryRuntimeDiagnostic() {
        return !industryPolicy.enabled()?"Timed industry disabled in economy.yml":industryFailure!=null
                ?"Timed industry deferred: "+industryFailure:"Timed industry active for saved firms; "+automaticMineDiagnostic+"; "+automaticQuarryDiagnostic+"; "+automaticForestryDiagnostic;
    }
    public void configureBusinessPurchases(com.blueninjas24.civilizations.industry.BusinessPurchasePolicy policy) {
        if(started.get())throw new IllegalStateException("Configure business purchases before session starts");
        purchasePolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureLivestockMarket(com.blueninjas24.civilizations.industry.LivestockMarketPolicy policy) {
        if(started.get())throw new IllegalStateException("Configure livestock market before session starts");
        livestockMarketPolicy=java.util.Objects.requireNonNull(policy);
    }
    public void configureLivestockWorkingArea(com.blueninjas24.civilizations.industry.LivestockWorkingArea policy) {
        if(started.get())throw new IllegalStateException("Configure livestock working area before session starts");
        livestockWorkingArea=java.util.Objects.requireNonNull(policy);
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
    public void configureCraftsman(com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy founding,
            com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy opportunities) {
        if(started.get())throw new IllegalStateException("Configure craftsman before startup");
        craftsmanFoundingPolicy=java.util.Objects.requireNonNull(founding);craftsmanOpportunityPolicy=java.util.Objects.requireNonNull(opportunities);
    }
    public void configureBakery(com.blueninjas24.civilizations.industry.BakeryFoundingPolicy founding,
            com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy opportunities) {
        configureBakery(founding,opportunities,com.blueninjas24.civilizations.industry.BakeryMarketPolicy.disabled());
    }
    public void configureBakery(com.blueninjas24.civilizations.industry.BakeryFoundingPolicy founding,
            com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy opportunities,com.blueninjas24.civilizations.industry.BakeryMarketPolicy market) {
        if(started.get())throw new IllegalStateException("Configure bakery before startup");
        bakeryMarketPolicy=java.util.Objects.requireNonNull(market);
        bakeryFoundingPolicy=java.util.Objects.requireNonNull(founding);bakeryOpportunityPolicy=java.util.Objects.requireNonNull(opportunities);
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
    public boolean oreDiscoveryReady() { return trackOres && oreHistoryHealthy.get() && oreCaptureReady(); }
    /** Continuous current capture is separate from missing historical provenance. */
    public boolean oreCaptureReady() { return trackOres&&!oreCaptureGap.get()&&pendingOreMutations.get()==0&&status().ready(); }
    public long oreMutationRevision(){return oreMutationRevision.get();}
    public String oreTrackingDiagnostic() {
        return !trackOres?"Ore placement tracking: not configured":oreDiscoveryReady()
                ?"Ore mutation history: trusted; mine startup also checks demand, skills, funding and resource anchors"
                :"Ore discovery and coordinate-backed mining paused: "+oreHistoryIssue.get();
    }
    /** Main-thread adapters may mark a gap immediately; no JDBC access or world work occurs here. */
    public void invalidateOreTracking(String reason) {
        oreMutationRevision.incrementAndGet();
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
                refreshSlaughterCount();
                var capture=new com.blueninjas24.civilizations.database.LivestockCapture(database.getConnection());
                livestockHistoryHealthy.set(capture.begin(now.getAsLong()).trusted());livestockTrackingStarted=true;
                if(livestockCaptureGap.get())capture.markGap(now.getAsLong());
                villageAnimals.set(new com.blueninjas24.civilizations.database.AnimalOwnershipRepository(database.getConnection()).villageAnimals());
                new com.blueninjas24.civilizations.database.WorkplaceOperations(database.getConnection()).configure(workplaceOperationPolicy);
                if(trackOres) {
                    var fence=new com.blueninjas24.civilizations.database.OreTrackingRepository(database.getConnection()).begin(now.getAsLong());
                    oreHistoryHealthy.set(fence.trusted() && !oreCaptureGap.get());
                    if(oreCaptureGap.get())new com.blueninjas24.civilizations.database.OreTrackingRepository(database.getConnection()).markGap(now.getAsLong());
                    if(!fence.trusted())oreHistoryIssue.set("unclean stop, capture gap or upgraded source coverage; reviewed recovery required");
                }
                if(trackForests) {
                    var fence=new com.blueninjas24.civilizations.database.ForestryTrackingRepository(database.getConnection()).begin(now.getAsLong());
                    forestTrackingStarted=true;
                    forestHistoryHealthy.set(fence.trusted()&&!forestCaptureGap.get());
                    if(forestCaptureGap.get())new com.blueninjas24.civilizations.database.ForestryTrackingRepository(database.getConnection()).markGap(now.getAsLong());
                    if(!fence.trusted())forestHistoryIssue.set("unclean stop, capture gap or legacy forest coverage; reviewed recovery required");
                }
                businesses = new com.blueninjas24.civilizations.database.BusinessRuntime(database.getConnection(),8,1024);
                businesses.configureResale(resalePolicy,purchasePolicy,settings.seed());
                businesses.configureStaffing(staffingPolicy);
                automaticMines=new com.blueninjas24.civilizations.database.AutomaticMineFounding(database.getConnection(),mineFoundingPolicy,industryPolicy,operationPolicy,startupGrantPolicy,settings.population().adulthoodSteps(),settings.seed(),()->!trackOres||(oreHistoryHealthy.get()&&!oreCaptureGap.get()&&pendingOreMutations.get()==0));
                automaticQuarries=new com.blueninjas24.civilizations.database.AutomaticQuarryFounding(database.getConnection(),quarryFoundingPolicy,quarryPolicy,industryPolicy,operationPolicy,startupGrantPolicy,settings.population().adulthoodSteps(),settings.seed(),()->!trackOres||(oreHistoryHealthy.get()&&!oreCaptureGap.get()&&pendingOreMutations.get()==0)).withPublicSupplies(villageSupplyPolicy,studyPolicy);
                automaticForestry=new com.blueninjas24.civilizations.database.AutomaticForestryFounding(database.getConnection(),forestryFoundingPolicy,forestryPolicy,industryPolicy,
                        operationPolicy,startupGrantPolicy,settings.population().adulthoodSteps(),settings.seed(),()->!trackForests||(forestHistoryHealthy.get()&&!forestCaptureGap.get()&&pendingForestMutations.get()==0))
                        .withPublicSupplies(villageSupplyPolicy,studyPolicy);
                industries = new com.blueninjas24.civilizations.database.IndustryRuntime(database.getConnection(),industryPolicy,operationPolicy,learningPolicy,()->!trackOres||(!oreCaptureGap.get()&&pendingOreMutations.get()==0));
                industries.configureFarmToolDemand(farmToolsPolicy,farmToolsBudget);
                industries.configureFishing(fishingPolicy);
                industries.configureQuarries(quarryPolicy);
                industries.configureForestry(forestryPolicy,()->!trackForests||(forestHistoryHealthy.get()&&!forestCaptureGap.get()&&pendingForestMutations.get()==0));
                industries.configureLivestock(livestockPolicy,livestockRuntimePolicy,livestockWorkingArea,this::livestockCaptureReady);
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
                    economy.configureGrainHarvest(grainHarvestPolicy);
                    economy.configureFarmMarket(farmMarketPolicy);
                    economy.configureFarmTools(farmToolsPolicy,farmToolsBudget,industryPolicy.mining());
                    economy.configureUpkeepContributions(upkeepContributions);
                    economy.configureStudy(studyPolicy);
                    economy.configureVillageSupplies(villageSupplyPolicy);
                    economy.configureWorkshopFounderStudy(new com.blueninjas24.civilizations.database.WorkshopOpportunities(database.getConnection(),workshopOpportunityPolicy,industryPolicy,
                        operationPolicy,startupGrantPolicy,workshopStarterPolicy,farmToolsPolicy,farmToolsBudget),workshopFoundingPolicy,careerStudyPolicy);
                    economy.configureCraftsmanFounderStudy(new com.blueninjas24.civilizations.database.CraftsmanOpportunities(database.getConnection(),craftsmanOpportunityPolicy,industryPolicy,
                            startupGrantPolicy,workshopStarterPolicy,operationPolicy),craftsmanFoundingPolicy,careerStudyPolicy);
                    economy.configureBakeryFounderStudy(new com.blueninjas24.civilizations.database.BakeryOpportunities(database.getConnection(),bakeryOpportunityPolicy,industryPolicy,
                            startupGrantPolicy,workshopStarterPolicy,villageSupplyPolicy,studyPolicy,operationPolicy,bakeryMarketPolicy),bakeryFoundingPolicy,careerStudyPolicy);
                    economy.configureWarehouseFounderStudy(new com.blueninjas24.civilizations.database.WarehouseOpportunities(database.getConnection(),warehouseOpportunityPolicy,industryPolicy,
                        operationPolicy,startupGrantPolicy,mineFoundingPolicy,()->!trackOres||oreHistoryHealthy.get()),warehouseFoundingPolicy,careerStudyPolicy);
                    economy.configureMineFounderStudy(automaticMines,careerStudyPolicy,mineFoundingPolicy);
                    economy.configureQuarryFounderStudy(automaticQuarries,careerStudyPolicy,quarryFoundingPolicy);
                    economy.configureForestryFounderStudy(automaticForestry,careerStudyPolicy,forestryFoundingPolicy);
                    economy.configureFisheryFounderStudy(new com.blueninjas24.civilizations.database.FisheryOpportunities(database.getConnection(),fisheryOpportunityPolicy,waterPolicy,fishingPolicy,industryPolicy,startupGrantPolicy,villageSupplyPolicy,studyPolicy,operationPolicy),fisheryFoundingPolicy,fisheryShorelinePolicy,fisheryOpportunityPolicy,careerStudyPolicy);
                    businesses.configureCareerStudy(economy,careerStudyPolicy);
                    economy.configureWorkLearning(learningPolicy);
                    if (loadedWorlds == null) economy.start(now.getAsLong());
                    else economy.startOnline(now.getAsLong(), loadedWorlds);
                    economyView.set(economy.views());
                }
                readyForWork = true;
                if (scheduleAutomatically&&!closing.get())try {
                    timer = owner.scheduleWithFixedDelay(() -> {
                        if (!closing.get()) {
                            try { pulseOnOwner(); } catch (Exception failure) { reportFailure(failure); }
                        }
                    }, 1, 1, TimeUnit.SECONDS);
                }catch(RejectedExecutionException stopped){if(!closing.get())throw stopped;}
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
        pendingOreMutations.incrementAndGet();oreMutationRevision.incrementAndGet();
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
            pendingOreMutations.decrementAndGet();
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
        pendingOreMutations.incrementAndGet();oreMutationRevision.incrementAndGet();
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
        }).whenComplete((result,failure)->{
            if(trackOres&&failure!=null)invalidateOreTracking("mutation save failed; reviewed recovery required");
            pendingOreMutations.decrementAndGet();
        });
    }

    /** Accepted native mutations immediately fence work, then drain on the single SQL owner. */
    public CompletableFuture<com.blueninjas24.civilizations.database.ForestryProvenance.Result> recordForestMutations(UUID world,String name,
            java.util.Collection<VillageObservation.Position> placements,java.util.Collection<VillageObservation.Position> removals,long millis,String reason) {
        var placed=List.copyOf(placements);var removed=List.copyOf(removals);
        java.util.Objects.requireNonNull(world);java.util.Objects.requireNonNull(name);
        pendingForestMutations.incrementAndGet();
        return submit(()->{
            requireReady();
            try {
                database.getVillageIdentityRepository().observeWorld(world,name);
                return new com.blueninjas24.civilizations.database.ForestryProvenance(database.getConnection()).record(world,placed,removed,millis,reason);
            }catch(SQLException|RuntimeException failure){
                if(forestTrackingStarted)try{new com.blueninjas24.civilizations.database.ForestryTrackingRepository(database.getConnection()).markGap(now.getAsLong());}
                catch(SQLException|RuntimeException fenceFailure){failure.addSuppressed(fenceFailure);}
                throw failure;
            }
        }).whenComplete((result,failure)->{
            if(failure!=null)invalidateForestTracking("native tree mutation save failed; reviewed recovery required");
            pendingForestMutations.decrementAndGet();
        });
    }
    public CompletableFuture<List<String>> workforceReview(UUID town){
        java.util.Objects.requireNonNull(town);
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.WorkforceCompetitionReport(database.getConnection()).read(town,operationPolicy).describe();});
    }
    public CompletableFuture<List<UUID>> mineralRecoveryCandidates(){
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.MineralRecovery(database.getConnection()).deposits();});
    }
    public record MineralReview(com.blueninjas24.civilizations.database.MineralRecovery.Plan plan,long revision) {}
    public CompletableFuture<MineralReview> prepareMineralRecovery(UUID deposit){
        return submit(()->{requireReady();if(!oreCaptureReady())throw new SQLException("Current ore capture is not ready");
            long revision=oreMutationRevision();
            var plan=new com.blueninjas24.civilizations.database.MineralRecovery(database.getConnection()).prepare(deposit);
            if(!oreCaptureReady()||revision!=oreMutationRevision())throw new SQLException("Capture changed during resource review");
            return new MineralReview(plan,revision);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.MineralRecovery.Receipt> approveMineralRecovery(
            UUID operation,String actor,MineralReview review,java.util.Map<VillageObservation.Position,Boolean> evidence,long captured){
        var frozen=java.util.Map.copyOf(evidence);
        return submit(()->{requireReady();
            if(loadedWorlds!=null&&!loadedWorlds.contains(review.plan().world()))throw new SQLException("Resource world is absent");
            return new com.blueninjas24.civilizations.database.MineralRecovery(database.getConnection()).approve(operation,actor,review.plan(),frozen,captured,now.getAsLong(),
                    ()->oreCaptureReady()&&oreMutationRevision()==review.revision());
        });
    }
    /** Read-only saved records on the SQL owner; no block truth or recovery is inferred. */
    public CompletableFuture<List<String>> resourceReview(){
        return submit(()->{requireReady();
            var lines=new ArrayList<String>();lines.add("Live capture: "+oreTrackingDiagnostic());lines.add("Live capture: "+forestTrackingDiagnostic());
            lines.addAll(new com.blueninjas24.civilizations.database.ResourceReviewReport(database.getConnection()).read(loadedWorlds).describe().lines().toList());
            return List.copyOf(lines);
        });
    }
    /** Immutable current samples authorize no land claim; founding independently rechecks startup. */
    public CompletableFuture<Boolean> observeForestSnapshot(UUID town,int chunkX,int chunkZ,com.blueninjas24.civilizations.industry.ForestObservation evidence){
        return submit(()->{requireReady();
            if(!forestDiscoveryReady()||automaticForestry==null||loadedWorlds!=null&&!loadedWorlds.contains(evidence.world()))return false;
            return automaticForestry.observe(town,chunkX,chunkZ,evidence);
        });
    }
    public CompletableFuture<java.util.Set<VillageObservation.Position>> forestExclusions(UUID world,int chunkX,int chunkZ){
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.ForestryProvenance(database.getConnection())
                .aroundChunk(world,chunkX,chunkZ,8,16384);});
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

    /** Separate complete stone/sand frames; never replaces coal/iron current survey membership. */
    public CompletableFuture<List<com.blueninjas24.civilizations.database.MineralSurveyReader.Patch>> surveyQuarries(UUID town,
            List<com.blueninjas24.civilizations.industry.MineralObservation> observations) {
        var frozen=List.copyOf(observations);
        return submit(()->{requireReady();
            if(!trackOres||!oreHistoryHealthy.get())throw new SQLException("Finite resource tracking is not ready; quarry survey paused");
            if(loadedWorlds==null||frozen.stream().anyMatch(o->!loadedWorlds.contains(o.world())))throw new SQLException("Quarry survey world absent or availability unknown");
            return com.blueninjas24.civilizations.database.MineralOpportunities.quarries(database.getConnection()).observe(town,frozen);
        });
    }
    public CompletableFuture<List<com.blueninjas24.civilizations.database.MineralSurveyReader.Patch>> surveyQuarries(UUID town,
            List<com.blueninjas24.civilizations.industry.MineralObservation> observations,int chunkX,int chunkZ) {
        var frozen=List.copyOf(observations);
        return submit(()->{requireReady();
            if(!trackOres||!oreHistoryHealthy.get())throw new SQLException("Finite resource tracking is not ready; quarry survey paused");
            if(loadedWorlds==null||frozen.stream().anyMatch(o->!loadedWorlds.contains(o.world())))throw new SQLException("Quarry survey world absent or availability unknown");
            return com.blueninjas24.civilizations.database.MineralOpportunities.quarries(database.getConnection()).observeChunk(town,frozen,chunkX,chunkZ);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.MineralOpportunities.Review> currentQuarryOpportunities(UUID town,long maximumAgeMillis){
        return submit(()->{requireReady();return com.blueninjas24.civilizations.database.MineralOpportunities.quarries(database.getConnection()).review(town,now.getAsLong(),maximumAgeMillis,loadedWorlds);});
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
    private com.blueninjas24.civilizations.database.CraftsmanInvestment craftsmanInvestment() {
        var evidence=new com.blueninjas24.civilizations.database.CraftsmanOpportunities(database.getConnection(),craftsmanOpportunityPolicy,industryPolicy,
                startupGrantPolicy,workshopStarterPolicy,operationPolicy);
        return new com.blueninjas24.civilizations.database.CraftsmanInvestment(database.getConnection(),evidence,craftsmanFoundingPolicy,craftsmanOpportunityPolicy,
                industryPolicy,startupGrantPolicy,workshopStarterPolicy,settings.population().adulthoodSteps(),settings.seed());
    }
    /** Read-only proposal on the SQL owner. Does not schedule, train or spend. */
    public CompletableFuture<com.blueninjas24.civilizations.database.CraftsmanInvestment.Preparation> prepareCraftsmanInvestment(UUID town) {
        return submit(()-> {
            requireReady();
            if(economy==null||economyFailure!=null||status.get().remainingMonths()>0||!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.CRAFTING_TABLE))
                return new com.blueninjas24.civilizations.database.CraftsmanInvestment.Preparation(java.util.Optional.empty(),"Craftsman waits for enabled current food/population/crafting-table evidence");
            return craftsmanInvestment().prepare(town,status.get().completedMonths(),now.getAsLong(),loadedWorlds);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.BusinessStartupGrants.Workshop> acceptCraftsmanInvestment(
            com.blueninjas24.civilizations.database.CraftsmanInvestment.Draft draft,com.blueninjas24.civilizations.database.WorkplaceOperations.Confirmation confirmation) {
        return submit(()-> {
            requireReady();
            if(economy==null||economyFailure!=null||status.get().remainingMonths()>0||!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.CRAFTING_TABLE))
                throw new com.blueninjas24.civilizations.database.BusinessStartupGrants.Declined("Craftsman acceptance waits for enabled current food/population/crafting-table evidence");
            return craftsmanInvestment().accept(draft,confirmation,now.getAsLong(),loadedWorlds);
        });
    }
    private com.blueninjas24.civilizations.database.BakeryInvestment bakeryInvestment() {
        var evidence=new com.blueninjas24.civilizations.database.BakeryOpportunities(database.getConnection(),bakeryOpportunityPolicy,industryPolicy,
                startupGrantPolicy,workshopStarterPolicy,villageSupplyPolicy,studyPolicy,operationPolicy,bakeryMarketPolicy);
        return new com.blueninjas24.civilizations.database.BakeryInvestment(database.getConnection(),evidence,bakeryFoundingPolicy,bakeryOpportunityPolicy,
                industryPolicy,startupGrantPolicy,workshopStarterPolicy,settings.population().adulthoodSteps(),settings.seed());
    }
    /** Read-only proposal on the SQL owner. Does not schedule, train or spend. */
    public CompletableFuture<com.blueninjas24.civilizations.database.BakeryInvestment.Preparation> prepareBakeryInvestment(UUID town) {
        return submit(()-> {
            requireReady();
            if(economy==null||economyFailure!=null||status.get().remainingMonths()>0||!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.SMOKER))
                return new com.blueninjas24.civilizations.database.BakeryInvestment.Preparation(java.util.Optional.empty(),"Bakery waits for enabled current food/population/smoker evidence");
            return bakeryInvestment().prepare(town,status.get().completedMonths(),now.getAsLong(),loadedWorlds);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.BusinessStartupGrants.Workshop> acceptBakeryInvestment(
            com.blueninjas24.civilizations.database.BakeryInvestment.Draft draft,com.blueninjas24.civilizations.database.WorkplaceOperations.Confirmation confirmation) {
        return submit(()-> {
            requireReady();
            if(economy==null||economyFailure!=null||status.get().remainingMonths()>0||!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.SMOKER))
                throw new com.blueninjas24.civilizations.database.BusinessStartupGrants.Declined("Bakery acceptance waits for enabled current food/population/smoker evidence");
            return bakeryInvestment().accept(draft,confirmation,now.getAsLong(),loadedWorlds);
        });
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
    /** May sponsor one supply-backed future-only merchant course; read-only prepare remains separate. */
    public CompletableFuture<com.blueninjas24.civilizations.database.WarehouseInvestment.Preparation> reviewAutomaticWarehouseInvestment(UUID town) {
        return submit(()-> {
            requireReady();
            String waiting=economy==null?"Food economy disabled":economyFailure!=null?"Food economy deferred: "+economyFailure
                    :status.get().remainingMonths()>0?"Population backlog must finish":!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.BARREL)?"Barrel recognition disabled":null;
            if(waiting!=null)return new com.blueninjas24.civilizations.database.WarehouseInvestment.Preparation(java.util.Optional.empty(),"Automatic warehouse waits: "+waiting);
            var result=warehouseInvestment().prepare(town,status.get().completedMonths(),now.getAsLong(),loadedWorlds);
            if(result.draft().isPresent())return result;
            String training=economy.reviewWarehouseFounderStudy(town,now.getAsLong());
            return new com.blueninjas24.civilizations.database.WarehouseInvestment.Preparation(java.util.Optional.empty(),result.explanation()+"; "+training);
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
    public CompletableFuture<com.blueninjas24.civilizations.database.CraftsmanInvestment.Preparation> reviewAutomaticCraftsmanInvestment(UUID town) {
        return submit(()-> {
            requireReady();
            String waiting=economy==null?"Food economy disabled":economyFailure!=null?"Food economy deferred: "+economyFailure
                    :status.get().remainingMonths()>0?"Population backlog must finish":!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.CRAFTING_TABLE)?"Craftsman workplace recognition disabled":null;
            if(waiting!=null)return new com.blueninjas24.civilizations.database.CraftsmanInvestment.Preparation(java.util.Optional.empty(),"Automatic craftsman waits: "+waiting);
            var result=craftsmanInvestment().prepare(town,status.get().completedMonths(),now.getAsLong(),loadedWorlds);
            if(result.draft().isPresent())return result;
            String training=economy.reviewCraftsmanFounderStudy(town,now.getAsLong());
            return new com.blueninjas24.civilizations.database.CraftsmanInvestment.Preparation(java.util.Optional.empty(),result.explanation()+"; "+training);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.BakeryInvestment.Preparation> reviewAutomaticBakeryInvestment(UUID town) {
        return submit(()-> {
            requireReady();
            String waiting=economy==null?"Food economy disabled":economyFailure!=null?"Food economy deferred: "+economyFailure
                    :status.get().remainingMonths()>0?"Population backlog must finish":!workplacePolicy.enabled()
                    ||!workplacePolicy.kinds().contains(com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.SMOKER)?"Bakery workplace recognition disabled":null;
            if(waiting!=null)return new com.blueninjas24.civilizations.database.BakeryInvestment.Preparation(java.util.Optional.empty(),"Automatic bakery waits: "+waiting);
            var result=bakeryInvestment().prepare(town,status.get().completedMonths(),now.getAsLong(),loadedWorlds);
            if(result.draft().isPresent())return result;
            String training=economy.reviewBakeryFounderStudy(town,now.getAsLong());
            return new com.blueninjas24.civilizations.database.BakeryInvestment.Preparation(java.util.Optional.empty(),result.explanation()+"; "+training);
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
    private com.blueninjas24.civilizations.database.LivestockMarket livestockMarket() {
        return new com.blueninjas24.civilizations.database.LivestockMarket(database.getConnection(),livestockMarketPolicy,operationPolicy);
    }
    public CompletableFuture<List<com.blueninjas24.civilizations.database.LivestockMarket.Offer>> livestockOffers(
            UUID player,com.blueninjas24.civilizations.industry.LivestockMarketPolicy.Position position) {
        return submit(()->{requireReady();return livestockMarket().offers(player,position,now.getAsLong(),loadedWorlds==null?java.util.Set.of():loadedWorlds);});
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.LivestockMarket.Reservation> reserveLivestockSale(
            com.blueninjas24.civilizations.industry.LivestockProductSale.Terms terms,com.blueninjas24.civilizations.industry.LivestockMarketPolicy.Position position) {
        return submit(()->{requireReady();return livestockMarket().reserve(terms,position,loadedWorlds==null?java.util.Set.of():loadedWorlds);});
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.LivestockProductSales.Receipt> completeLivestockSale(
            com.blueninjas24.civilizations.industry.LivestockProductSale.Terms terms) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.LivestockProductSales(database.getConnection(),operationPolicy).complete(terms);});
    }
    public CompletableFuture<Optional<com.blueninjas24.civilizations.database.LivestockProductSales.Receipt>> cancelLivestockSale(
            com.blueninjas24.civilizations.industry.LivestockProductSale.Terms terms) {
        return submit(()->{requireReady();return livestockMarket().cancel(terms);});
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
    /** Trusted adapter supplies fresh enclosure/animal/authority evidence; no Bukkit objects cross threads. */
    public CompletableFuture<com.blueninjas24.civilizations.industry.PenProtection> protectPen(
            com.blueninjas24.civilizations.industry.PenProtection pen,boolean authorized) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.PenProtectionRepository(database.getConnection()).save(pen,authorized);});
    }
    public CompletableFuture<Optional<com.blueninjas24.civilizations.industry.PenProtection>> protectedPen(UUID pen) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.PenProtectionRepository(database.getConnection()).load(pen);});
    }
    public CompletableFuture<java.util.List<UUID>> protectedPensAt(UUID world,int y,int x,int z) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.PenProtectionRepository(database.getConnection()).atAll(world,y,x,z);});
    }
    /** Actual loaded-world name captured by the trusted Paper adapter; unknown worlds need no town. */
    public CompletableFuture<com.blueninjas24.civilizations.industry.PenProtection> protectPen(
            com.blueninjas24.civilizations.industry.PenProtection pen,boolean authorized,String worldName) {
        java.util.Objects.requireNonNull(pen);java.util.Objects.requireNonNull(worldName);
        return submit(()->{requireReady();
            if(!authorized)throw new com.blueninjas24.civilizations.database.PenProtectionLifecycle.Declined("Pen protection requires certified authority");
            database.getVillageIdentityRepository().observeWorld(pen.world(),worldName);
            return new com.blueninjas24.civilizations.database.PenProtectionRepository(database.getConnection()).save(pen,true);
        });
    }
    public CompletableFuture<com.blueninjas24.civilizations.database.PenProtectionLifecycle.Release> releasePen(
            com.blueninjas24.civilizations.database.PenProtectionLifecycle.Release request,boolean admin) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.PenProtectionLifecycle(database.getConnection()).release(request,admin);});
    }
    /** Explicit trusted herd work handoff; no automatic scheduling or physical reconciliation implied. */
    public CompletableFuture<com.blueninjas24.civilizations.industry.LivestockHerd.Plan> advanceLivestock(
            UUID operation,UUID business,long expectedPeriod,long workPerWorker,long siteWorkLimit,
            com.blueninjas24.civilizations.industry.LivestockPolicy policy) {
        return submit(()-> {
            requireReady();var founding=new com.blueninjas24.civilizations.database.BusinessRepository(database.getConnection()).findFounding(business).orElseThrow();
            boolean active=loadedWorlds!=null&&loadedWorlds.contains(founding.site().worldId());
            if(active&&policy.enabled()&&!livestockCaptureReady())throw new IllegalStateException("Trusted livestock loss capture required before production");
            return new com.blueninjas24.civilizations.database.LivestockRepository(database.getConnection()).advance(operation,business,expectedPeriod,workPerWorker,siteWorkLimit,policy,operationPolicy,active,livestockWorkingArea);
        });
    }
    public CompletableFuture<java.util.List<com.blueninjas24.civilizations.database.LivestockPresence.Member>> livestockMembers(UUID after,int limit) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.LivestockPresence(database.getConnection()).page(after,limit);});
    }
    public CompletableFuture<Integer> recordLivestockPresence(java.util.List<com.blueninjas24.civilizations.database.LivestockPresence.Sight> observations) {
        var sights=java.util.List.copyOf(observations);
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.LivestockPresence(database.getConnection()).record(sights);});
    }
    public CompletableFuture<Optional<com.blueninjas24.civilizations.database.LivestockPresence.Sight>> livestockSight(UUID animal) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.LivestockPresence(database.getConnection()).find(animal);});
    }
    /** Trusted main-thread prepared-body claim. Publication of village identity precedes future completion/spawn. */
    public CompletableFuture<com.blueninjas24.civilizations.database.LivestockBirthProjections.Projection> claimLivestockBirth(
            com.blueninjas24.civilizations.database.LivestockBirthProjections.Claim claim) {
        return submit(()-> {
            requireReady();var repository=new com.blueninjas24.civilizations.database.LivestockBirthProjections(database.getConnection());
            if(!livestockCaptureReady()&&repository.forEntity(claim.entity()).isEmpty())throw new IllegalStateException("Livestock history must be trusted before a new physical birth claim");
            var result=repository.claim(claim,loadedWorlds==null?java.util.Set.of():loadedWorlds);
            villageAnimals.set(new com.blueninjas24.civilizations.database.AnimalOwnershipRepository(database.getConnection()).villageAnimals());return result;
        });
    }
    public CompletableFuture<java.util.List<com.blueninjas24.civilizations.database.LivestockBirthProjections.Job>> livestockBirthJobs(UUID after,int limit) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.LivestockBirthProjections(database.getConnection()).page(after,limit);});
    }
    public CompletableFuture<Optional<com.blueninjas24.civilizations.database.LivestockBirthProjections.Projection>> livestockBirthProjection(UUID birth) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.LivestockBirthProjections(database.getConnection()).find(birth);});
    }
    public CompletableFuture<Boolean> activateLivestockBirth(UUID birth,UUID entity) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.LivestockBirthProjections(database.getConnection()).activate(birth,entity);});
    }
    public CompletableFuture<Boolean> failUnspawnedLivestockBirth(UUID birth,UUID entity) {
        return submit(()->{requireReady();return new com.blueninjas24.civilizations.database.LivestockBirthProjections(database.getConnection()).failedUnspawned(birth,entity);});
    }
    public CompletableFuture<java.util.Optional<com.blueninjas24.civilizations.database.LivestockLosses.Receipt>> recordLivestockLoss(
            com.blueninjas24.civilizations.database.LivestockLosses.Observation observation) {
        stageLivestockLoss(observation);
        return submit(()->{requireReady();var result=new com.blueninjas24.civilizations.database.LivestockLosses(database.getConnection()).record(observation);pendingLivestockLosses.remove(observation.animal());refreshSlaughterCount();return result;});
    }
    /** Memory-only main-thread capture fence, before any bounded Paper queue delay. */
    public void stageLivestockLoss(com.blueninjas24.civilizations.database.LivestockLosses.Observation observation){
        pendingLivestockLosses.add(java.util.Objects.requireNonNull(observation).animal());
    }
    public CompletableFuture<Void> flushLivestockLosses(java.util.List<com.blueninjas24.civilizations.database.LivestockLosses.Observation> losses) {
        var captured=java.util.List.copyOf(losses);
        captured.forEach(loss->pendingLivestockLosses.add(loss.animal()));
        return submit(()->{requireReady();var repository=new com.blueninjas24.civilizations.database.LivestockLosses(database.getConnection());for(var loss:captured){repository.record(loss);pendingLivestockLosses.remove(loss.animal());}refreshSlaughterCount();return null;});
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
        if(forestTrackingStarted&&forestCaptureGap.get())new com.blueninjas24.civilizations.database.ForestryTrackingRepository(database.getConnection()).markGap(now.getAsLong());
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
        if(forestryFoundingPolicy.enabled()&&economy!=null&&status.get().remainingMonths()==0&&economyFailure==null) {
            try{automaticForestryDiagnostic=automaticForestry.pulse(status.get().completedMonths(),now.getAsLong(),loadedWorlds);}
            catch(SQLException|RuntimeException failure){
                if(!java.util.Objects.equals(automaticForestryDiagnostic,"Logging startup deferred: "+failure.getMessage()))logger.log(Level.SEVERE,"Automatic logging startup deferred; assets remain retryable",failure);
                automaticForestryDiagnostic="Logging startup deferred: "+failure.getMessage();
            }
        }
        if(quarryFoundingPolicy.enabled()&&economy!=null&&status.get().remainingMonths()==0&&economyFailure==null) {
            try {automaticQuarryDiagnostic=automaticQuarries.pulse(status.get().completedMonths(),now.getAsLong(),loadedWorlds);}
            catch(SQLException|RuntimeException failure) {
                if(!java.util.Objects.equals(automaticQuarryDiagnostic,"Quarry founding deferred: "+failure.getMessage()))logger.log(Level.SEVERE,"Automatic quarry founding deferred; startup assets remain retryable",failure);
                automaticQuarryDiagnostic="Quarry founding deferred: "+failure.getMessage();
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
                            && !(failure instanceof com.blueninjas24.civilizations.database.BusinessSales.Declined)
                            && !(failure instanceof com.blueninjas24.civilizations.database.PenProtectionLifecycle.Declined)) reportFailure(failure);
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
        if (closing.compareAndSet(false, true)) {
            var previous = status.get();
            status.set(new Status(false, previous.completedMonths(), previous.targetMonths(), previous.livingPeople(), previous.lastBatchNanos(), previous.failure()));
            var scheduled = timer; if (scheduled != null) scheduled.cancel(false);
            owner.execute(()-> {
                try {
                    if(livestockTrackingStarted)try{new com.blueninjas24.civilizations.database.LivestockCapture(database.getConnection())
                            .finish(now.getAsLong(),livestockHistoryHealthy.get()&&!livestockCaptureGap.get()&&pendingLivestockLosses.isEmpty());}
                        catch(SQLException|RuntimeException failure){logger.log(Level.SEVERE,"Livestock capture shutdown fence failed; live production requires review",failure);}
                    if(forestTrackingStarted)try{new com.blueninjas24.civilizations.database.ForestryTrackingRepository(database.getConnection())
                            .finish(now.getAsLong(),forestHistoryHealthy.get()&&!forestCaptureGap.get()&&pendingForestMutations.get()==0);}
                        catch(SQLException|RuntimeException failure){logger.log(Level.SEVERE,"Forest capture shutdown fence failed; forestry requires review",failure);}
                    if(trackOres && readyForWork)new com.blueninjas24.civilizations.database.OreTrackingRepository(database.getConnection())
                            .finish(now.getAsLong(),!oreCaptureGap.get()&&pendingOreMutations.get()==0);
                } catch(SQLException|RuntimeException failure) {
                    logger.log(Level.SEVERE,"Ore tracking shutdown fence failed; discovery will require reviewed recovery",failure);
                } finally {
                    database.close();readyForWork=false;
                    status.updateAndGet(current->new Status(false,current.completedMonths(),current.targetMonths(),current.livingPeople(),current.lastBatchNanos(),current.failure()));
                }
            });
            owner.shutdown();
        }
        // A repeated close from outside the owner still waits for the original drain.
        if (Thread.currentThread() == ownerThread.get()) return;
        try {
            if (!owner.awaitTermination(5, TimeUnit.SECONDS)) logger.warning("Population shutdown still finishing on its owner thread");
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
}
