package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.simulation.CivilizationTime;
import java.sql.*;
import java.util.*;

/** Single SQL-owner food runtime. Saved clocks, evidence and period effects; no Bukkit access. */
public final class EconomyRuntimeRepository {
    /** Expected new-offer refusal, distinct from a database fault or corrupt saved trade. */
    public static final class PurchaseDeclined extends SQLException {
        public PurchaseDeclined(String reason) { super(reason); }
    }
    public record View(CommodityRepository.Account account, FarmObservation farm, Result latest, long queuedPeriods,
                       HouseholdNeeds.Assessment needs, TreasuryRepository.Account treasury, VillagePriorities.Decision priority, FoodAid foodAid,
                       FoodAppeals.Snapshot appeal, List<HouseholdWellbeing.Snapshot> wellbeing, FoodResponses.Plan response, String farmBusiness,String study) {
        public View { wellbeing = List.copyOf(wellbeing); }
        /** Derived from immutable retained period inputs; never from mutable current reserves. */
        public Optional<FoodImportImpact.Outcome> foodImportImpact() {
            return latest == null ? Optional.empty() : latest.reports().stream()
                    .filter(report -> report.commodity().equals(Commodity.FOOD)).findFirst().map(FoodImportImpact::assess);
        }
    }
    public record FoodAid(long pendingDeliveries, long nextPeriodUnits) {}
    private record Clock(long cadence, CivilizationTime.Anchor anchor, String settings, long completed) {}
    private final Connection connection;
    private final CommodityRepository commodities;
    private final EconomySettings settings;
    private final FarmObservation.Policy farms;
    private final long adulthood;
    private final VillagePriorities.Policy priorities;
    private final long seed;
    private String signature;
    private com.blueninjas24.civilizations.industry.FarmBusinessPolicy farmBusinesses=com.blueninjas24.civilizations.industry.FarmBusinessPolicy.disabled();
    private FarmEnterprises enterprises;
    private com.blueninjas24.civilizations.industry.WorkLearningPolicy learningPolicy=com.blueninjas24.civilizations.industry.WorkLearningPolicy.disabled();
    private com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy farmToolPolicy=com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy.disabled();
    private com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy farmToolBudget=com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy.disabled();
    private com.blueninjas24.civilizations.industry.IndustryPolicy farmToolTransport=com.blueninjas24.civilizations.industry.IndustryPolicy.prototype();
    private com.blueninjas24.civilizations.industry.BusinessOperationPolicy farmOperations=com.blueninjas24.civilizations.industry.BusinessOperationPolicy.disabled();
    private com.blueninjas24.civilizations.industry.UpkeepContributionPolicy upkeepContributions=com.blueninjas24.civilizations.industry.UpkeepContributionPolicy.disabled();
    public void configureUpkeepContributions(com.blueninjas24.civilizations.industry.UpkeepContributionPolicy policy) {
        if(inUse||farmTools!=null)throw new IllegalStateException("Configure contributions before economy use");
        upkeepContributions=Objects.requireNonNull(policy);
        if(policy.enabled())signature+="|upkeep-contributions-v1:"+policy;
    }
    private FarmTools farmTools;
    private boolean farmToolConfiguration;
    private FarmTools farmTools(){if(farmTools==null)farmTools=new FarmTools(connection,farmToolPolicy,farmToolBudget,farmOperations,farmToolTransport,upkeepContributions);return farmTools;}
    public void configureFarmTools(com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy policy,
                                   com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy budget,
                                   com.blueninjas24.civilizations.industry.IndustryPolicy transport) {
        if(inUse||farmTools!=null||farmToolConfiguration)throw new IllegalStateException("Configure farm tools once before economy use");
        farmToolPolicy=Objects.requireNonNull(policy);farmToolBudget=Objects.requireNonNull(budget);farmToolTransport=Objects.requireNonNull(transport);
        farmToolConfiguration=true;
        if(policy.enabled())signature+="|farm-tools-v1:"+policy+"|budget:"+budget+"|delivery:"+transport.localDeliveryPeriods()+"|haul:"+transport.localHaulUnitsPerPeriod();
    }
    private boolean inUse;
    private final Map<UUID,String> enterpriseReasons=new HashMap<>();
    private final OpeningFunds openingFunds;
    private final HouseholdWellbeing.Policy wellbeing;
    private final FoodResponses.Settings responses;
    private com.blueninjas24.civilizations.industry.StudyPolicy studyPolicy=com.blueninjas24.civilizations.industry.StudyPolicy.disabled();
    private com.blueninjas24.civilizations.industry.VillageSupplyPolicy villageSupplies=com.blueninjas24.civilizations.industry.VillageSupplyPolicy.disabled();
    public void configureVillageSupplies(com.blueninjas24.civilizations.industry.VillageSupplyPolicy policy) {
        if(inUse)throw new IllegalStateException("Configure village supplies before economy use");
        villageSupplies=Objects.requireNonNull(policy);
    }
    private VillageSupplyPurchases villagePurchases(){return new VillageSupplyPurchases(connection,villageSupplies,studyPolicy,farmOperations);}
    private int cursor;
    private Set<UUID> loadedWorlds;
    public EconomyRuntimeRepository(Connection connection, EconomySettings settings, FarmObservation.Policy farms, long adulthood) {
        this(connection,settings,farms,adulthood,VillagePriorities.Policy.prototype(),0);
    }
    public EconomyRuntimeRepository(Connection connection, EconomySettings settings, FarmObservation.Policy farms, long adulthood,
                                    VillagePriorities.Policy priorities, long seed) {
        this(connection, settings, farms, adulthood, priorities, seed, OpeningFunds.disabled());
    }
    public EconomyRuntimeRepository(Connection connection, EconomySettings settings, FarmObservation.Policy farms, long adulthood,
                                    VillagePriorities.Policy priorities, long seed, OpeningFunds openingFunds) {
        this(connection, settings, farms, adulthood, priorities, seed, openingFunds, HouseholdWellbeing.Policy.disabled());
    }
    public EconomyRuntimeRepository(Connection connection, EconomySettings settings, FarmObservation.Policy farms, long adulthood,
                                    VillagePriorities.Policy priorities, long seed, OpeningFunds openingFunds, HouseholdWellbeing.Policy wellbeing) {
        this(connection,settings,farms,adulthood,priorities,seed,openingFunds,wellbeing,FoodResponses.Settings.disabled());
    }
    public EconomyRuntimeRepository(Connection connection, EconomySettings settings, FarmObservation.Policy farms, long adulthood,
                                    VillagePriorities.Policy priorities,long seed,OpeningFunds openingFunds,HouseholdWellbeing.Policy wellbeing,FoodResponses.Settings responses) {
        this.responses=Objects.requireNonNull(responses);
        this.connection = connection; this.commodities = new CommodityRepository(connection);
        this.wellbeing = Objects.requireNonNull(wellbeing);
        this.openingFunds = Objects.requireNonNull(openingFunds);
        this.priorities = Objects.requireNonNull(priorities); this.seed = seed;
        this.settings = settings; this.farms = farms; this.adulthood = adulthood;
        signature = "food-runtime-1|" + settings + "|" + farms + "|adult=" + adulthood;
        if (adulthood < 1) throw new IllegalArgumentException("Invalid adult threshold");
    }
    public void start(long now) throws SQLException {
        inUse=true;
        for (var town : towns()) transaction(() -> {
            var clock = clock(town);
            if (!commodities.read(town).orElseThrow().state().stocks().keySet().equals(settings.startingPerResident().keySet()))
                throw new SQLException("Commodity catalog changed; explicit inventory transition required");
            if (!clock.settings().equals(signature)) {
                if (clock.anchor().targetSteps() != clock.completed()) throw new SQLException("Finish queued economic catch-up under old settings before retuning");
                long discarded = Math.addExact(Math.max(0, now - clock.anchor().lastObservedMillis()), clock.anchor().remainderMillis());
                try (var s = connection.prepareStatement("INSERT INTO economy_tuning(settlement_id,after_period,observed_millis,discarded_millis,old_settings,new_settings) VALUES (?,?,?,?,?,?)")) {
                    s.setString(1, town.toString()); s.setLong(2, clock.completed()); s.setLong(3, now); s.setLong(4, discarded);
                    s.setString(5, clock.settings()); s.setString(6, signature); s.executeUpdate();
                }
                try (var s = connection.prepareStatement("UPDATE economy_accounts SET severe_bps=?,surplus_bps=?,abundant_bps=?,severe_after=? WHERE settlement_id=?")) {
                    var policy = settings.shortages(); s.setInt(1, policy.severeCoverageBps()); s.setInt(2, policy.surplusCoverageBps());
                    s.setInt(3, policy.abundantCoverageBps()); s.setLong(4, policy.severeAfterPeriods()); s.setString(5, town.toString()); s.executeUpdate();
                }
                saveClock(town, new CivilizationTime.Anchor(Math.max(now, clock.anchor().lastObservedMillis()), 0, clock.completed()));
                try (var s = connection.prepareStatement("UPDATE economy_runtime SET millis_per_period=?, settings=? WHERE settlement_id=?")) {
                    s.setLong(1, Math.multiplyExact(settings.secondsPerPeriod(), 1000)); s.setString(2, signature); s.setString(3, town.toString()); s.executeUpdate();
                }
            }
            initializeOpeningFunds(town);
            return null;
        });
        pulse(now, true);
    }
    /** Preserve earned queue/fractions, but never credit the interval while the server was stopped. */
    public void startOnline(long now, Set<UUID> worlds) throws SQLException {
        loadedWorlds = Set.copyOf(worlds);
        for (var town : towns()) transaction(() -> {
            var saved = clock(town).anchor();
            saveClock(town, new CivilizationTime.Anchor(Math.max(now, saved.lastObservedMillis()), saved.remainderMillis(), saved.targetSteps()));
            return null;
        });
        start(now);
    }
    public void updateLoadedWorlds(long now, Set<UUID> worlds) throws SQLException {
        for (var town : towns()) transaction(() -> { accrue(town, now, false); return null; });
        loadedWorlds = Set.copyOf(worlds);
    }
    private GrainHarvestPolicy grainHarvestPolicy=GrainHarvestPolicy.disabled();
    private FarmMarketPolicy farmMarketPolicy=FarmMarketPolicy.disabled();
    private boolean marketConfigured;
    public void configureFarmMarket(FarmMarketPolicy policy) {
        if(inUse||marketConfigured)throw new IllegalStateException("Configure farm market once before using runtime");
        marketConfigured=true;farmMarketPolicy=Objects.requireNonNull(policy);
        if(policy.enabled())signature+="|farm-market-v1:"+policy;
    }
    private boolean grainConfigured;
    public void configureGrainHarvest(GrainHarvestPolicy policy) {
        if(inUse||grainConfigured)throw new IllegalStateException("Configure grain harvest once before using runtime");
        grainConfigured=true;grainHarvestPolicy=Objects.requireNonNull(policy);
        if(policy.enabled())signature+="|grain-harvest-v1:"+policy;
    }
    /** Configure once before use; startup applies existing-save forward settings transitions. */
    public void configureFarmBusinesses(com.blueninjas24.civilizations.industry.FarmBusinessPolicy policy) throws SQLException {
        configureFarmBusinesses(policy,com.blueninjas24.civilizations.industry.BusinessOperationPolicy.disabled());
    }
    public void configureFarmBusinesses(com.blueninjas24.civilizations.industry.FarmBusinessPolicy policy,com.blueninjas24.civilizations.industry.BusinessOperationPolicy operations) throws SQLException {
        if(inUse || enterprises!=null)throw new IllegalStateException("Configure farm businesses once before using runtime");
        farmOperations=Objects.requireNonNull(operations);farmBusinesses=Objects.requireNonNull(policy);enterprises=new FarmEnterprises(connection,policy,adulthood,operations);
        if(policy.enabled())signature+="|farm-business-v1:"+policy;
        if(operations.enabled())signature+="|business-operations-v1:"+operations;
    }
    public void configureWorkLearning(com.blueninjas24.civilizations.industry.WorkLearningPolicy policy) {
        if(inUse)throw new IllegalStateException("Configure farm learning before using economy");
        learningPolicy=Objects.requireNonNull(policy);
    }
    public void configureStudy(com.blueninjas24.civilizations.industry.StudyPolicy policy) {
        if(inUse)throw new IllegalStateException("Configure study before using economy");
        studyPolicy=Objects.requireNonNull(policy);
    }
    private FisheryFounderStudy fisheryFounderStudy;
    public void configureFisheryFounderStudy(FisheryOpportunities opportunities,com.blueninjas24.civilizations.industry.FisheryFoundingPolicy founding,
            com.blueninjas24.civilizations.industry.FisheryShorelinePolicy shoreline,com.blueninjas24.civilizations.industry.FisheryOpportunityPolicy terms,
            com.blueninjas24.civilizations.industry.CareerStudyPolicy careers) {
        if(inUse)throw new IllegalStateException("Configure fishing founder study before economy use");
        fisheryFounderStudy=new FisheryFounderStudy(connection,opportunities,founding,shoreline,terms,careers,studyPolicy,adulthood);
    }
    public String reviewFisheryFounderStudy(FisheryOpportunities.Opportunity opportunity,com.blueninjas24.civilizations.industry.ShorelineEvidence bank,long now)throws SQLException {
        return transaction(()-> {
            if(fisheryFounderStudy==null)return "First fishing-owner study not configured";
            var town=opportunity.town();if(!hasRuntime(town)||!active(town))return "Fishing-owner study waits for present-world food economy";
            var field=farm(town);if(field==null||!field.complete())return "Fishing-owner study waits for complete food evidence";
            long period=commodities.read(town).orElseThrow().state().completedPeriods();if(period==0)return "Fishing-owner study waits for first food assessment";
            accrue(town,now,false);return fisheryFounderStudy.reviewRows(opportunity,bank,now,loadedWorlds==null?Set.of():loadedWorlds,period,clock(town).anchor().targetSteps());
        });
    }
    private MineFounderStudy mineFounderStudy;
    private WorkshopFounderStudy workshopFounderStudy;
    private WarehouseFounderStudy warehouseFounderStudy;
    public void configureWarehouseFounderStudy(WarehouseOpportunities opportunities,com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy founding,
            com.blueninjas24.civilizations.industry.CareerStudyPolicy careers) {
        if(inUse)throw new IllegalStateException("Configure warehouse founder study before economy use");
        warehouseFounderStudy=new WarehouseFounderStudy(connection,opportunities,founding,careers,studyPolicy,adulthood);
    }
    public String reviewWarehouseFounderStudy(UUID town,long now)throws SQLException {
        return transaction(()-> {
            if(warehouseFounderStudy==null)return "Warehouse founder study not configured";
            if(!hasRuntime(town)||!active(town))return "Warehouse founder study waits for present-world food economy";
            long period=commodities.read(town).orElseThrow().state().completedPeriods();
            if(period==0)return "Warehouse founder study waits for first food assessment";
            accrue(town,now,false);
            return warehouseFounderStudy.reviewRows(town,now,loadedWorlds,period,clock(town).anchor().targetSteps());
        });
    }
    public void configureWorkshopFounderStudy(WorkshopOpportunities opportunities,com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy founding,
            com.blueninjas24.civilizations.industry.CareerStudyPolicy careers) {
        if(inUse)throw new IllegalStateException("Configure workshop founder study before economy use");
        workshopFounderStudy=new WorkshopFounderStudy(connection,opportunities,founding,careers,studyPolicy,adulthood);
    }
    public String reviewWorkshopFounderStudy(UUID town,long now)throws SQLException {
        return transaction(()-> {
            if(workshopFounderStudy==null)return "Workshop founder study not configured";
            if(!hasRuntime(town)||!active(town))return "Workshop founder study waits for present-world food economy";
            long period=commodities.read(town).orElseThrow().state().completedPeriods();
            if(period==0)return "Workshop founder study waits for first food assessment";
            accrue(town,now,false);
            return workshopFounderStudy.reviewRows(town,now,loadedWorlds,period,clock(town).anchor().targetSteps());
        });
    }
    public void configureMineFounderStudy(AutomaticMineFounding mines,com.blueninjas24.civilizations.industry.CareerStudyPolicy careers,
                                         com.blueninjas24.civilizations.industry.MineFoundingPolicy founding) {
        if(inUse)throw new IllegalStateException("Configure mine founder study before economy use");
        mineFounderStudy=new MineFounderStudy(connection,mines,careers,studyPolicy,founding,adulthood);
        mines.configureTraining((opportunity,now)-> {
            if(!hasRuntime(opportunity.town())||!active(opportunity.town()))return "Founder study waits for present-world food economy";
            var evidence=farm(opportunity.town());if(evidence==null||!evidence.complete())return "Founder study waits for complete food evidence";
            long period=commodities.read(opportunity.town()).orElseThrow().state().completedPeriods();
            if(period==0)return "Founder study waits for first food assessment";
            accrue(opportunity.town(),now,false);
            return mineFounderStudy.reviewRows(opportunity,period,clock(opportunity.town()).anchor().targetSteps());
        });
    }
    public ResidentStudy.Course enrollStudy(UUID operation,UUID resident,UUID town,String skill,long now)throws SQLException {
        return transaction(()->{
            var study=new ResidentStudy(connection,studyPolicy,adulthood);
            var existing=study.find(operation);
            if(existing.isPresent())return study.enrollRows(operation,resident,town,skill,existing.orElseThrow().eligibleAfter());
            if(!hasRuntime(town)||!active(town))throw new SQLException("Study requires initialized present-world economy");
            accrue(town,now,false);
            return study.enrollRows(operation,resident,town,skill,clock(town).anchor().targetSteps());
        });
    }
    /** Actual existing job opportunities; no fake employer or enrollment into earned backlog. */
    public String reviewCareerStudy(UUID business,com.blueninjas24.civilizations.industry.CareerStudyPolicy policy,long step,long now)throws SQLException {
        if(step<0||now<0)throw new IllegalArgumentException("Invalid career observation time");
        if(!policy.enabled()||!studyPolicy.enabled())return "Automatic career study disabled";
        return transaction(()-> {
            var founding=new BusinessRepository(connection).findFounding(business).orElseThrow();var town=founding.business().settlementId();
            if(!hasRuntime(town)||!active(town))return "Career study waits for initialized present-world economy";
            var evidence=farm(town);
            if(evidence==null||!evidence.complete())return "Career study waits for complete food evidence";
            long period=commodities.read(town).orElseThrow().state().completedPeriods();
            if(period==0)return "Career study waits for first completed food assessment";
            accrue(town,now,false);
            return new AutomaticCareerStudy(connection,policy,studyPolicy,adulthood).reviewRows(business,step,period,clock(town).anchor().targetSteps());
        });
    }
    private boolean active(UUID town) throws SQLException {
        if (loadedWorlds == null) return true;
        try (var s = connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")) {
            s.setString(1, town.toString());
            try (var r = s.executeQuery()) { return r.next() && loadedWorlds.contains(UUID.fromString(r.getString(1))); }
        }
    }
    /** Older/equal evidence is idempotent. Partial observations pause; never seed a zero-food village. */
    public void observe(FarmObservation evidence, long now) throws SQLException {
        inUse=true;
        transaction(() -> {
            var old = farm(evidence.settlement());
            if (old != null && old.observedMillis() >= evidence.observedMillis()) return null;
            try (var s = connection.prepareStatement("""
                    INSERT INTO farm_evidence(settlement_id,world_id,observed_millis,complete,planted_crops,site_x,site_y,site_z) VALUES (?,?,?,?,?,?,?,?)
                    ON CONFLICT(settlement_id) DO UPDATE SET world_id=excluded.world_id, observed_millis=excluded.observed_millis,
                    complete=excluded.complete, planted_crops=excluded.planted_crops,site_x=excluded.site_x,site_y=excluded.site_y,site_z=excluded.site_z
                    """)) {
                s.setString(1, evidence.settlement().toString()); s.setString(2, evidence.world().toString()); s.setLong(3, evidence.observedMillis());
                s.setInt(4, evidence.complete() ? 1 : 0); s.setLong(5, evidence.plantedCrops());
                if(evidence.representativeCrop()==null)for(int i=6;i<=8;i++)s.setNull(i,Types.INTEGER);
                else {var crop=evidence.representativeCrop();s.setInt(6,crop.x());s.setInt(7,crop.y());s.setInt(8,crop.z());}
                s.executeUpdate();
            }
            try(var s=connection.prepareStatement("DELETE FROM farm_grain_evidence WHERE settlement_id=?")) {
                s.setString(1,evidence.settlement().toString());s.executeUpdate();
            }
            if(evidence.grainCrops()!=null) {
                try(var s=connection.prepareStatement("INSERT INTO farm_grain_evidence VALUES (?,?,?)")) {
                    s.setString(1,evidence.settlement().toString());s.setLong(2,evidence.observedMillis());s.setLong(3,evidence.grainCrops());s.executeUpdate();
                }
            }
            if (evidence.complete() && living(evidence.settlement(), false) + living(evidence.settlement(), true) > 0 && !hasRuntime(evidence.settlement())) {
                long people = living(evidence.settlement(), false) + living(evidence.settlement(), true);
                var account = commodities.read(evidence.settlement());
                if (account.isPresent() && !account.orElseThrow().state().stocks().keySet().equals(settings.startingPerResident().keySet()))
                    throw new SQLException("Commodity catalog changed; explicit inventory transition required");
                if (account.isEmpty()) commodities.initializeRows(evidence.settlement(), settings.startingReserves(people), settings.shortages());
                else if (!account.orElseThrow().policy().equals(settings.shortages())) throw new SQLException("Explicit legacy-account policy transition required");
                long completed = account.map(a -> a.state().completedPeriods()).orElse(0L);
                try (var s = connection.prepareStatement("INSERT INTO economy_runtime VALUES (?,?,?,?,?,?)")) {
                    s.setString(1, evidence.settlement().toString()); s.setLong(2, Math.multiplyExact(settings.secondsPerPeriod(), 1000));
                    s.setLong(3, now); s.setLong(4, 0); s.setLong(5, completed); s.setString(6, signature); s.executeUpdate();
                }
            }
            if (evidence.complete() && hasRuntime(evidence.settlement())) initializeOpeningFunds(evidence.settlement());
            if (!evidence.complete() && hasRuntime(evidence.settlement())) {
                var clock = clock(evidence.settlement());
                saveClock(evidence.settlement(), new CivilizationTime.Anchor(Math.max(now, clock.anchor().lastObservedMillis()), 0, clock.completed()));
            }
            return null;
        });
    }
    /** Trusted caller records already-delivered goods; never a substitute for verifying/removing player items. */
    public boolean recordDeliveredFood(UUID operation, UUID town, long units, String source, long now) throws SQLException {
        FoodDeliveryRepository.validate(operation,town,units,source);
        return transaction(() -> {
            var deliveries=new FoodDeliveryRepository(connection);var old=deliveries.find(operation);
            if(old.isPresent()) {
                var receipt=old.orElseThrow();
                if(!receipt.settlement().equals(town)||receipt.units()!=units||!receipt.source().equals(source)) throw new SQLException("Food delivery ID reused with different inputs");
                return false;
            }
            if(!hasRuntime(town)) throw new SQLException("Food economy not initialized; no inventory inferred");
            accrue(town,now,false);var target=clock(town).anchor().targetSteps();
            if(target==Long.MAX_VALUE) throw new SQLException("No representable future food period");
            deliveries.record(operation,town,units,source,target);return true;
        });
    }
    /** Trusted adapter must preserve consent and prepared player state before requesting a reservation. */
    public String reserveFoodPurchase(FoodPurchases.Terms terms, FoodPurchases.Policy policy, FoodAidPreview.Policy foods, long now) throws SQLException {
        return reserveFoodPurchase(terms,policy,foods,FoodBargaining.Policy.disabled(),now);
    }
    public String reserveFoodPurchase(FoodPurchases.Terms terms, FoodPurchases.Policy policy, FoodAidPreview.Policy foods,
                                      FoodBargaining.Policy bargaining, long now) throws SQLException {
        Objects.requireNonNull(terms); Objects.requireNonNull(policy); Objects.requireNonNull(foods);
        return transaction(() -> {
            var purchases = new FoodPurchaseRepository(connection);
            if (purchases.find(terms.operation()).isPresent()) return purchases.matching(terms).status().name();
            if (!policy.enabled() || !foods.enabled()) throw new PurchaseDeclined("New food purchases disabled");
            if (new FoodBarterRepository(connection).find(terms.operation()).isPresent()) throw new SQLException("Purchase operation already used for barter");
            if (new FoodBarterRepository(connection).hasReservedPlayer(terms.player())) throw new PurchaseDeclined("Finish reserved barter before another purchase");
            if (terms.hundredthsPerEmerald() != policy.hundredthsPerEmerald()
                    || foods.units(terms.item(), terms.count()) != terms.units()) throw new PurchaseDeclined("Purchase conversion settings changed");
            int maximum=maximumFoodPayment(terms.settlement(),terms.units(),policy,now);
            if (terms.emeralds()>maximum) throw new PurchaseDeclined("Offer exceeds perceived value or available budget");
            if (bargaining.enabled()) new FoodBargainRepository(connection).reserve(terms,bargaining,now);
            if (new FoodDeliveryRepository(connection).find(terms.operation()).isPresent()) throw new SQLException("Operation already used for food delivery");
            return purchases.reserve(terms, clock(terms.settlement()).completed()).status().name();
        });
    }
    /** New discussions record no payment promise or goods transfer. Current owner facts set each counter. */
    public FoodBargaining.Deal bargain(FoodPurchases.Terms requested, FoodPurchases.Policy purchases, FoodAidPreview.Policy foods,
                                       FoodBargaining.Policy bargaining, long now) throws SQLException {
        return transaction(() -> {
            var repository=new FoodBargainRepository(connection);
            var duplicate=repository.find(requested.operation()).orElse(null);
            if(duplicate!=null) {
                if(!duplicate.requested().equals(requested))throw new SQLException("Bargain operation reused with different terms");
                return duplicate;
            }
            if(!bargaining.enabled() || !purchases.enabled() || !foods.enabled())throw new PurchaseDeclined("New bargaining disabled");
            var previous=repository.latest(requested.player(),requested.settlement()).orElse(null);
            if(previous!=null && previous.result().status()==FoodBargaining.Status.CLOSED
                    && Math.max(now,previous.result().observedMillis())<previous.result().cooldownUntil())return previous;
            if(requested.hundredthsPerEmerald()!=purchases.hundredthsPerEmerald()
                    || foods.units(requested.item(),requested.count())!=requested.units())throw new PurchaseDeclined("Offer conversion changed");
            int maximum=maximumFoodPayment(requested.settlement(),requested.units(),purchases,now);
            if(maximum<1)throw new PurchaseDeclined("Village cannot offer a whole emerald for this stack");
            var result=FoodBargaining.discuss(requested.emeralds(),maximum,now,previous==null ? null : previous.result(),bargaining);
            var deal=new FoodBargaining.Deal(requested,result);repository.save(deal);return deal;
        });
    }
    public FoodPurchases.Terms bargainAcceptance(UUID player,UUID town,FoodBargaining.Policy policy,long now) throws SQLException {
        if(!policy.enabled())throw new PurchaseDeclined("New bargaining disabled");
        var deal=new FoodBargainRepository(connection).latest(player,town).orElseThrow(()->new PurchaseDeclined("Make a food price offer first"));
        var result=deal.result();
        if((result.status()!=FoodBargaining.Status.AGREED && result.status()!=FoodBargaining.Status.COUNTER)
                || Math.max(now,result.observedMillis())>=result.expiresMillis())throw new PurchaseDeclined("No current bargain to accept");
        return deal.paymentTerms();
    }
    /** Reads inside the caller's owner transaction; pending supplies and reservations reduce remaining need. */
    private int maximumFoodPayment(UUID town,long units,FoodPurchases.Policy policy,long now) throws SQLException {
        long fairValue=validateFoodNeed(town,units,policy.reservePeriods(),now);
        var budget = new TreasuryRepository(connection).load(town).orElseThrow(() -> new PurchaseDeclined("Village treasury unknown"));
        return FoodPurchases.maximumPayment(fairValue, budget.hundredths(), policy);
    }
    /** Food value independent of money: a cash-poor village may still have goods to barter. */
    private long validateFoodNeed(UUID town,long units,int reservePeriods,long now) throws SQLException {
            if (!hasRuntime(town)) throw new PurchaseDeclined("Food economy not initialized");
            accrue(town, now, false);
            var clock = clock(town);
            if (clock.anchor().targetSteps() != clock.completed()) throw new PurchaseDeclined("Finish economy catch-up before a new purchase");
            var evidence = farm(town);
            if (evidence == null || !evidence.complete()) throw new PurchaseDeclined("Reliable food evidence required for new purchase");
            var needs = new HouseholdNeedsRepository(connection).load(town, clock.completed()).orElseThrow(() -> new PurchaseDeclined("Food needs unknown"));
            var priority = new VillagePriorityRepository(connection).latest(town).orElseThrow();
            if (priority.selected() != VillagePriorities.Action.SEEK_FOOD || needs.demand() <= needs.fulfilled())
                throw new PurchaseDeclined("Village is not currently seeking food");
            var account = commodities.read(town).orElseThrow();
            long supply = account.state().stocks().get(Commodity.FOOD).units();
            supply = Math.addExact(supply, new FoodPurchaseRepository(connection).reservedUnits(town));
            supply = Math.addExact(supply, new FoodBarterRepository(connection).reservedFood(town));
            supply = Math.addExact(supply, new FoodDeliveryRepository(connection).pendingUnits(town));
            long target = Math.multiplyExact(needs.demand(), reservePeriods);
            if (supply >= target || units > target - supply) throw new PurchaseDeclined("Food offer exceeds remaining village purchase need");
            return Math.multiplyExact(settings.price(Commodity.FOOD, needs.sharedFood()).movePointRight(2).longValueExact(), units);
    }
    /** Indicative owner quote; no items, reserves or funds move. Caller still needs durable player consent/proof. */
    public FoodBarters.Terms quoteFoodBarter(UUID operation,UUID player,UUID town,long units,String item,int count,
                                            Commodity commodity,FoodBarters.Policy policy,FoodAidPreview.Policy foods,long now) throws SQLException {
        return transaction(()->barterOffer(operation,player,town,units,item,count,commodity,policy,foods,now));
    }
    private FoodBarters.Terms barterOffer(UUID operation,UUID player,UUID town,long units,String item,int count,
                                         Commodity commodity,FoodBarters.Policy policy,FoodAidPreview.Policy foods,long now) throws SQLException {
        if(!policy.enabled()||!foods.enabled())throw new PurchaseDeclined("New barter disabled");
        var goods=policy.goods().get(commodity);if(goods==null)throw new PurchaseDeclined("Village does not offer this barter commodity");
        if(foods.units(item,count)!=units)throw new PurchaseDeclined("Food conversion changed");
        new FoodBargainRepository(connection).requireNoCooldown(player,town,now);
        long foodValue=validateFoodNeed(town,units,policy.foodReservePeriods(),now);
        var stock=commodities.read(town).orElseThrow().state().stocks().get(commodity);
        if(stock==null||stock.shortagePeriods()>0)throw new PurchaseDeclined("Available barter stock unknown or in shortage");
        // Phase 2 only exports idle opening materials. Future industry/construction inputs need their own reserve rules.
        try(var s=connection.prepareStatement("SELECT households,industry,construction,exports,events FROM economy_periods WHERE settlement_id=? AND commodity=? AND period=?")) {
            s.setString(1,town.toString());s.setString(2,commodity.id());s.setLong(3,clock(town).completed());
            try(var r=s.executeQuery()) {
                if(!r.next())throw new PurchaseDeclined("Barter commodity history unknown");
                for(int i=1;i<=5;i++)if(r.getLong(i)!=0)throw new PurchaseDeclined("Village needs these materials; prototype barter unavailable");
            }
        }
        long outgoingValue=settings.price(commodity,Condition.BALANCED).movePointRight(2).longValueExact();
        int payment=FoodBarters.maximumPayment(foodValue,outgoingValue,stock.units(),goods,policy.maximumItems());
        if(payment<1)throw new PurchaseDeclined("No whole barter item affordable from disposable supplies");
        return new FoodBarters.Terms(operation,player,town,units,item,count,commodity,goods.item(),payment,goods.unitsPerItem());
    }
    /** Trusted adapter only: PREPARED player save must be independently verified before this call. */
    public String reserveFoodBarter(FoodBarters.Terms terms,FoodBarters.Policy policy,FoodAidPreview.Policy foods,long now) throws SQLException {
        return transaction(()->{
            var barters=new FoodBarterRepository(connection);
            if(barters.find(terms.operation()).isPresent())return barters.matching(terms).status().name();
            try(var s=connection.prepareStatement("SELECT 1 FROM food_purchases WHERE player_id=? AND status='RESERVED'")) {
                s.setString(1,terms.player().toString());try(var r=s.executeQuery()){if(r.next())throw new PurchaseDeclined("Finish reserved purchase before barter");}
            }
            if(barters.hasReservedPlayer(terms.player()))throw new PurchaseDeclined("Finish your reserved barter first");
            if(new FoodPurchaseRepository(connection).find(terms.operation()).isPresent()
                    ||new FoodDeliveryRepository(connection).find(terms.operation()).isPresent()
                    ||new FoodBargainRepository(connection).find(terms.operation()).isPresent())throw new SQLException("Barter operation already used");
            var quote=barterOffer(terms.operation(),terms.player(),terms.settlement(),terms.foodUnits(),terms.foodItem(),terms.foodCount(),terms.commodity(),policy,foods,now);
            if(!quote.paymentItem().equals(terms.paymentItem())||quote.unitsPerItem()!=terms.unitsPerItem()||terms.paymentCount()>quote.paymentCount())
                throw new PurchaseDeclined("Barter goods conversion/value/available stock changed");
            return barters.reserve(terms,clock(terms.settlement()).completed());
        });
    }
    /** Only after independent saved-player EXCHANGED proof. Existing reservations recover despite retuning/disable. */
    public boolean completeFoodBarter(FoodBarters.Terms terms,long now) throws SQLException {
        return transaction(()->{
            var barters=new FoodBarterRepository(connection);var saved=barters.matching(terms);
            if(saved.status()==FoodBarterRepository.Status.COMPLETED)return false;
            if(saved.status()!=FoodBarterRepository.Status.RESERVED)throw new SQLException("Cancelled barter cannot complete");
            accrue(terms.settlement(),now,false);long target=clock(terms.settlement()).anchor().targetSteps();
            if(target==Long.MAX_VALUE)throw new SQLException("No future food period representable");
            new FoodDeliveryRepository(connection).record(terms.operation(),terms.settlement(),terms.foodUnits(),terms.source(),target);
            barters.finish(terms,FoodBarterRepository.Status.COMPLETED,clock(terms.settlement()).completed());return true;
        });
    }
    /** Only after durable proof of no player exchange. An ambiguous inventory failure must hold supplies for review. */
    public boolean cancelFoodBarter(FoodBarters.Terms terms) throws SQLException {
        return transaction(()->{
            var barters=new FoodBarterRepository(connection);if(barters.find(terms.operation()).isEmpty())return false;
            var saved=barters.matching(terms);if(saved.status()==FoodBarterRepository.Status.CANCELLED)return false;
            if(saved.status()!=FoodBarterRepository.Status.RESERVED)throw new SQLException("Completed barter cannot be refunded");
            barters.finish(terms,FoodBarterRepository.Status.CANCELLED,clock(terms.settlement()).completed());return true;
        });
    }
    /** Only after independently verifying a saved player goods/payment exchange. Recovery ignores new-offer settings. */
    public boolean completeFoodPurchase(FoodPurchases.Terms terms, long now) throws SQLException {
        return transaction(() -> {
            var purchases = new FoodPurchaseRepository(connection); var saved = purchases.matching(terms);
            if (saved.status() == FoodPurchaseRepository.Status.COMPLETED) return false;
            if (saved.status() != FoodPurchaseRepository.Status.RESERVED) throw new SQLException("Cancelled purchase cannot complete");
            accrue(terms.settlement(), now, false); long target = clock(terms.settlement()).anchor().targetSteps();
            if (target == Long.MAX_VALUE) throw new SQLException("No future food period representable");
            new FoodDeliveryRepository(connection).record(terms.operation(), terms.settlement(), terms.units(), terms.source(), target);
            purchases.finish(terms, FoodPurchaseRepository.Status.COMPLETED); return true;
        });
    }
    /** Only if the adapter proves exchange never happened. Never use an uncertain refund after a failed save. */
    public boolean cancelFoodPurchase(FoodPurchases.Terms terms) throws SQLException {
        return transaction(() -> {
            var purchases = new FoodPurchaseRepository(connection);
            if (purchases.find(terms.operation()).isEmpty()) return false;
            var saved = purchases.matching(terms);
            if (saved.status() == FoodPurchaseRepository.Status.CANCELLED) return false;
            if (saved.status() != FoodPurchaseRepository.Status.RESERVED) throw new SQLException("Completed purchase cannot be refunded");
            purchases.finish(terms, FoodPurchaseRepository.Status.CANCELLED); return true;
        });
    }
    private void accrue(UUID town,long now,boolean offline) throws SQLException {
        var clock=clock(town);requireSettings(clock);var evidence=farm(town);
        if (!active(town)) saveClock(town,new CivilizationTime.Anchor(Math.max(now,clock.anchor().lastObservedMillis()),clock.anchor().remainderMillis(),clock.anchor().targetSteps()));
        else if(evidence==null||!evidence.complete()) saveClock(town,new CivilizationTime.Anchor(Math.max(now,clock.anchor().lastObservedMillis()),0,clock.completed()));
        else saveClock(town,CivilizationTime.accrue(clock.anchor(),now,clock.cadence(),offline
                && loadedWorlds == null ? Math.multiplyExact(clock.cadence(),settings.maximumOfflinePeriods()) : Long.MAX_VALUE).anchor());
    }
    public int pulse(long now, boolean offline) throws SQLException {
        inUse=true;
        var towns = towns();
        for (var town : towns) transaction(() -> {
            accrue(town,now,offline);
            return null;
        });
        int processed = 0;
        if (towns.isEmpty()) return processed;
        for (int visited = 0; visited < towns.size() && processed < settings.periodsPerBatch(); visited++) {
            var town = towns.get(Math.floorMod(cursor++, towns.size()));
            if(farmBusinesses.enabled() && active(town) && clock(town).completed()<clock(town).anchor().targetSteps())
                enterpriseReasons.put(town,enterprises.prepare(farm(town),farms.cropsPerWorker()));
            if (transaction(() -> step(town))) {
                processed++;
                if(farmBusinesses.enabled()&&farmToolPolicy.enabled()) {
                    String order=farmTools().reviewPurchase(town,clock(town).completed(),loadedWorlds);
                    enterpriseReasons.put(town,enterpriseReasons.getOrDefault(town,"Farm work reviewed")+"; "+order);
                }
            }
        }
        return processed;
    }
    private boolean step(UUID town) throws SQLException {
        if (!active(town)) return false;
        var clock = clock(town); requireSettings(clock); var farm = farm(town);
        if (farm == null || !farm.complete() || clock.completed() == clock.anchor().targetSteps()) return false;
        var account = commodities.read(town).orElseThrow();
        long adults = living(town, false), children = living(town, true);
        long labor=farmBusinesses.enabled()?enterprises.workers(town):adults;
        long output = settings.foodProduction(labor, OptionalLong.of(farm.plantedCrops() / farms.cropsPerWorker()));
        long nextPeriod=Math.incrementExact(clock.completed());
        farmTools().deliverRows(town,nextPeriod);
        var toolWork=farmBusinesses.enabled()?farmTools().prepareRows(town,output):null;
        if(toolWork!=null)output=toolWork.requested();
        var deliveries=new FoodDeliveryRepository(connection);
        var incoming=deliveries.eligible(town,nextPeriod);
        var flows = new TreeMap<Commodity, Flow>(); account.state().stocks().keySet().forEach(commodity -> flows.put(commodity, Flow.idle()));
        flows.put(Commodity.FOOD, new Flow(output, FoodDeliveryRepository.units(incoming), settings.foodDemand(adults, children), 0, 0, 0, 0));
        var supplyOrders=villagePurchases();
        for(var entry:supplyOrders.incomingRows(town,nextPeriod).entrySet()) {
            var flow=flows.get(entry.getKey());
            if(flow==null)throw new SQLException("Paid village cargo missing from saved supply catalog");
            flows.put(entry.getKey(),new Flow(flow.production(),Math.addExact(flow.imports(),entry.getValue()),flow.households(),flow.industry(),flow.construction(),flow.exports(),flow.events()));
        }
        GrainHarvest.Plan grainPlan=null;
        FarmMarketHarvest.Plan marketPlan=null;
        if(farmMarketPolicy.enabled()&&farmBusinesses.enabled()) {
            if(labor>0&&output>0) {
                var store=FarmMarketStore.ensureRows(connection,FarmEnterprises.businessId(town),farmMarketPolicy,clock.anchor().lastObservedMillis());
                var foodFlow=flows.get(Commodity.FOOD);
                marketPlan=FarmMarketHarvest.plan(output,account.state().stocks().get(Commodity.FOOD).units(),foodFlow.imports(),foodFlow.households(),
                        store.units().get(FarmMarketStore.GRAIN),FarmMarketStore.freeGoodsSpace(connection,store),farm,grainHarvestPolicy,farmMarketPolicy);
                flows.put(Commodity.FOOD,new Flow(marketPlan.publicFood(),foodFlow.imports(),foodFlow.households(),foodFlow.industry(),foodFlow.construction(),foodFlow.exports(),foodFlow.events()));
                output=marketPlan.actualWork();
            }
        } else if(grainHarvestPolicy.enabled()) {
            var foodFlow=flows.get(Commodity.FOOD);
            grainPlan=GrainHarvest.plan(output,account.state().stocks().get(Commodity.FOOD).units(),foodFlow.imports(),foodFlow.households(),
                    new GrainHarvestRepository(connection).stock(town),farm,farmBusinesses.enabled()&&labor>0,grainHarvestPolicy);
            flows.put(Commodity.FOOD,new Flow(grainPlan.foodOutput(),foodFlow.imports(),foodFlow.households(),foodFlow.industry(),foodFlow.construction(),foodFlow.exports(),foodFlow.events()));
        }
        var study=new ResidentStudy(connection,studyPolicy,adulthood);
        if(mineFounderStudy!=null)study.withFounderOpportunity(course->mineFounderStudy.availableRows(course,clock.anchor().lastObservedMillis(),loadedWorlds));
        if(workshopFounderStudy!=null||warehouseFounderStudy!=null||fisheryFounderStudy!=null)study.withFounderPractice(course->{
            var workshop=workshopFounderStudy==null?ResidentStudy.FounderState.READY:workshopFounderStudy.stateRows(course,clock.anchor().lastObservedMillis(),loadedWorlds);
            if(workshop!=ResidentStudy.FounderState.READY)return workshop;
            var warehouse=warehouseFounderStudy==null?ResidentStudy.FounderState.READY:warehouseFounderStudy.stateRows(course,clock.anchor().lastObservedMillis(),loadedWorlds);
            return warehouse!=ResidentStudy.FounderState.READY?warehouse:fisheryFounderStudy==null?ResidentStudy.FounderState.READY:
                    fisheryFounderStudy.stateRows(course,clock.anchor().lastObservedMillis(),loadedWorlds==null?Set.of():loadedWorlds);
        });
        var studyPlan=study.planRows(town,nextPeriod,account.state(),flows,farmBusinesses.enabled()||output==0);
        var period = commodities.advanceRows(town, nextPeriod, studyPlan.flows(), settings.shortages());
        if(grainPlan!=null)new GrainHarvestRepository(connection).finishRows(town,nextPeriod,grainPlan);
        if(marketPlan!=null)new FarmMarketRepository(connection).finishRows(town,nextPeriod,marketPlan);
        // Grain uses the same actual farm work and tool service; it cannot make that work free.
        farmTools().finishRows(town,nextPeriod,toolWork,output);
        study.applyRows(town,nextPeriod,studyPlan,period);
        if(farmBusinesses.enabled())new FarmLearning(connection,learningPolicy).reviewRows(town,nextPeriod,farm.plantedCrops()/farms.cropsPerWorker());
        var needs = new HouseholdNeedsRepository(connection);
        var assessment = HouseholdNeeds.assess(town, needs.population(town), period, adulthood, settings, needs.load(town, nextPeriod - 1).orElse(null));
        needs.save(assessment);
        new HouseholdWellbeingRepository(connection).assess(assessment,wellbeing,settings.shortages().severeAfterPeriods());
        var decisions=new VillagePriorityRepository(connection);decisions.consider(assessment,seed,priorities);
        var appeals=new FoodAppealRepository(connection);
        var food=period.reports().stream().filter(report->report.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow();
        appeals.save(FoodAppeals.observe(assessment,decisions.latest(town).orElseThrow(),food,appeals.load(town,nextPeriod-1).orElse(null)));
        deliveries.apply(incoming,nextPeriod);
        supplyOrders.applyRows(town,nextPeriod);
        if(nextPeriod==clock.anchor().targetSteps())supplyOrders.reviewRows(town,nextPeriod,loadedWorlds);
        if(responses.policy().enabled()) {
            long pending=Math.addExact(supplyOrders.pending(town,Commodity.FOOD),Math.addExact(deliveries.pendingUnits(town),Math.addExact(new FoodPurchaseRepository(connection).reservedUnits(town),new FoodBarterRepository(connection).reservedFood(town))));
            var budget=new TreasuryRepository(connection).load(town).orElse(null);
            new FoodResponseRepository(connection).save(FoodResponses.assess(town,nextPeriod,seed,decisions.latest(town).orElseThrow(),assessment,
                    nextPeriod==clock.anchor().targetSteps(),pending,budget==null?null:budget.hundredths(),period,settings,responses));
        }
        return true;
    }
    public Map<UUID, View> views() throws SQLException {
        var result = new TreeMap<UUID, View>();
        for (var town : towns()) {
            var account = commodities.load(town).orElseThrow();
            var latest = account.state().completedPeriods() == 0 ? null : commodities.history(town, account.state().completedPeriods()).orElseThrow();
            var pending=new FoodDeliveryRepository(connection).pending(town,account.state().completedPeriods()==Long.MAX_VALUE ? Long.MAX_VALUE : account.state().completedPeriods()+1);
            result.put(town, new View(account, farm(town), latest, clock(town).anchor().targetSteps() - account.state().completedPeriods(),
                    new HouseholdNeedsRepository(connection).load(town, account.state().completedPeriods()).orElse(null),
                    new TreasuryRepository(connection).load(town).orElse(null),new VillagePriorityRepository(connection).latest(town).orElse(null),
                    new FoodAid(pending.deliveries(),pending.eligibleNextPeriodUnits()),
                      new FoodAppealRepository(connection).load(town, account.state().completedPeriods()).orElse(null),
                      new HouseholdWellbeingRepository(connection).load(town,account.state().completedPeriods()),
                      new FoodResponseRepository(connection).load(town,account.state().completedPeriods()).orElse(null),
                      (farmBusinesses.enabled()?enterpriseReasons.getOrDefault(town,"Farm business enabled; awaiting eligible economic period")
                              +new FarmLearning(connection,learningPolicy).explanation(town,account.state().completedPeriods()).map(reason->"; "+reason).orElse("")
                              +farmTools().explanation(town,account.state().completedPeriods()).map(reason->"; "+reason).orElse(""):
                              "Automatic farm businesses disabled; legacy field accounting")+new GrainHarvestRepository(connection).summary(town,account.state().completedPeriods())
                              +new FarmMarketRepository(connection).summary(town,account.state().completedPeriods()),
                      new ResidentStudy(connection,studyPolicy,adulthood).summary(town)+"; "+villagePurchases().summary(town)));
        }
        return Collections.unmodifiableMap(result);
    }
    private long living(UUID town, boolean children) throws SQLException {
        try (var s = connection.prepareStatement("SELECT COUNT(*) FROM residents WHERE settlement_id=? AND status='ALIVE' AND age_steps " + (children ? "<" : ">=") + " ?")) {
            s.setString(1, town.toString()); s.setLong(2, adulthood); try (var r = s.executeQuery()) { return r.getLong(1); }
        }
    }
    /** Called inside the runtime's transaction. Existing accounts, including zero, are never topped up. */
    private void initializeOpeningFunds(UUID town) throws SQLException {
        if (!openingFunds.enabled()) return;
        var treasury = new TreasuryRepository(connection);
        if (treasury.load(town).isPresent()) return;
        long people = Math.addExact(living(town, false), living(town, true));
        if (people == 0) return;
        var operation = UUID.nameUUIDFromBytes(("civilizations-opening-funds-1|" + town)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        treasury.initializeRows(operation, town, openingFunds.amount(people),
                "Prototype opening capital: living-residents=" + people + "; hundredths-per-resident=" + openingFunds.hundredthsPerResident());
    }
    private List<UUID> towns() throws SQLException {
        var result = new ArrayList<UUID>();
        try (var s = connection.createStatement(); var r = s.executeQuery("SELECT settlement_id FROM economy_runtime ORDER BY settlement_id")) {
            while (r.next()) result.add(UUID.fromString(r.getString(1)));
        }
        return result;
    }
    private boolean hasRuntime(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT 1 FROM economy_runtime WHERE settlement_id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) { return r.next(); }
        }
    }
    private FarmObservation farm(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM farm_evidence WHERE settlement_id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) {
                return r.next() ? new FarmObservation(town, UUID.fromString(r.getString("world_id")), r.getLong("observed_millis"),
                        r.getInt("complete") == 1, r.getLong("planted_crops"),cropSite(r),grainCrops(town,r.getLong("observed_millis"))) : null;
            }
        }
    }
    private Long grainCrops(UUID town,long observedMillis) throws SQLException {
        try(var s=connection.prepareStatement("SELECT observed_millis,grain_crops FROM farm_grain_evidence WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()) {
                if(!r.next())return null;
                if(r.getLong(1)!=observedMillis)throw new SQLException("Grain evidence does not match saved farm frame");
                return r.getLong(2);
            }
        }
    }
    private static com.blueninjas24.civilizations.settlement.VillageObservation.Position cropSite(ResultSet r) throws SQLException {
        Object x=r.getObject("site_x"),y=r.getObject("site_y"),z=r.getObject("site_z");
        if(x==null && y==null && z==null)return null;
        if(x==null || y==null || z==null)throw new SQLException("Incomplete saved farm site coordinates");
        return new com.blueninjas24.civilizations.settlement.VillageObservation.Position(r.getInt("site_x"),r.getInt("site_y"),r.getInt("site_z"));
    }
    private Clock clock(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT r.*,a.completed_periods FROM economy_runtime r JOIN economy_accounts a USING(settlement_id) WHERE settlement_id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("Economy runtime not initialized");
                return new Clock(r.getLong("millis_per_period"), new CivilizationTime.Anchor(r.getLong("last_observed_millis"), r.getLong("remainder_millis"),
                        r.getLong("target_periods")), r.getString("settings"), r.getLong("completed_periods"));
            }
        }
    }
    private void saveClock(UUID town, CivilizationTime.Anchor anchor) throws SQLException {
        try (var s = connection.prepareStatement("UPDATE economy_runtime SET last_observed_millis=?,remainder_millis=?,target_periods=? WHERE settlement_id=?")) {
            s.setLong(1, anchor.lastObservedMillis()); s.setLong(2, anchor.remainderMillis()); s.setLong(3, anchor.targetSteps()); s.setString(4, town.toString());
            if (s.executeUpdate() != 1) throw new SQLException("Economy clock disappeared");
        }
    }
    private void requireSettings(Clock clock) throws SQLException {
        if (!clock.settings().equals(signature)) throw new SQLException("Economy settings require startup transition");
    }
    @FunctionalInterface private interface Work<T> { T run() throws SQLException; }
    private <T> T transaction(Work<T> work) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Food runtime requires no active transaction");
        connection.setAutoCommit(false);
        try { var result = work.run(); connection.commit(); connection.setAutoCommit(true); return result; }
        catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
}
