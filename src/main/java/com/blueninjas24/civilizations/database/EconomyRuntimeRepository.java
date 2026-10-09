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
                       HouseholdNeeds.Assessment needs, TreasuryRepository.Account treasury, VillagePriorities.Decision priority, FoodAid foodAid, FoodAppeals.Snapshot appeal) {}
    public record FoodAid(long pendingDeliveries, long nextPeriodUnits) {}
    private record Clock(long cadence, CivilizationTime.Anchor anchor, String settings, long completed) {}
    private final Connection connection;
    private final CommodityRepository commodities;
    private final EconomySettings settings;
    private final FarmObservation.Policy farms;
    private final long adulthood;
    private final VillagePriorities.Policy priorities;
    private final long seed;
    private final String signature;
    private final OpeningFunds openingFunds;
    private int cursor;
    public EconomyRuntimeRepository(Connection connection, EconomySettings settings, FarmObservation.Policy farms, long adulthood) {
        this(connection,settings,farms,adulthood,VillagePriorities.Policy.prototype(),0);
    }
    public EconomyRuntimeRepository(Connection connection, EconomySettings settings, FarmObservation.Policy farms, long adulthood,
                                    VillagePriorities.Policy priorities, long seed) {
        this(connection, settings, farms, adulthood, priorities, seed, OpeningFunds.disabled());
    }
    public EconomyRuntimeRepository(Connection connection, EconomySettings settings, FarmObservation.Policy farms, long adulthood,
                                    VillagePriorities.Policy priorities, long seed, OpeningFunds openingFunds) {
        this.connection = connection; this.commodities = new CommodityRepository(connection);
        this.openingFunds = Objects.requireNonNull(openingFunds);
        this.priorities = Objects.requireNonNull(priorities); this.seed = seed;
        this.settings = settings; this.farms = farms; this.adulthood = adulthood;
        signature = "food-runtime-1|" + settings + "|" + farms + "|adult=" + adulthood;
        if (adulthood < 1) throw new IllegalArgumentException("Invalid adult threshold");
    }
    public void start(long now) throws SQLException {
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
    /** Older/equal evidence is idempotent. Partial observations pause; never seed a zero-food village. */
    public void observe(FarmObservation evidence, long now) throws SQLException {
        transaction(() -> {
            var old = farm(evidence.settlement());
            if (old != null && old.observedMillis() >= evidence.observedMillis()) return null;
            try (var s = connection.prepareStatement("""
                    INSERT INTO farm_evidence VALUES (?,?,?,?,?)
                    ON CONFLICT(settlement_id) DO UPDATE SET world_id=excluded.world_id, observed_millis=excluded.observed_millis,
                    complete=excluded.complete, planted_crops=excluded.planted_crops
                    """)) {
                s.setString(1, evidence.settlement().toString()); s.setString(2, evidence.world().toString()); s.setLong(3, evidence.observedMillis());
                s.setInt(4, evidence.complete() ? 1 : 0); s.setLong(5, evidence.plantedCrops()); s.executeUpdate();
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
        Objects.requireNonNull(terms); Objects.requireNonNull(policy); Objects.requireNonNull(foods);
        return transaction(() -> {
            var purchases = new FoodPurchaseRepository(connection);
            if (purchases.find(terms.operation()).isPresent()) return purchases.matching(terms).status().name();
            if (!policy.enabled() || !foods.enabled()) throw new PurchaseDeclined("New food purchases disabled");
            if (terms.hundredthsPerEmerald() != policy.hundredthsPerEmerald()
                    || foods.units(terms.item(), terms.count()) != terms.units()) throw new PurchaseDeclined("Purchase conversion settings changed");
            if (!hasRuntime(terms.settlement())) throw new PurchaseDeclined("Food economy not initialized");
            accrue(terms.settlement(), now, false);
            var clock = clock(terms.settlement());
            if (clock.anchor().targetSteps() != clock.completed()) throw new PurchaseDeclined("Finish economy catch-up before a new purchase");
            var evidence = farm(terms.settlement());
            if (evidence == null || !evidence.complete()) throw new PurchaseDeclined("Reliable food evidence required for new purchase");
            var needs = new HouseholdNeedsRepository(connection).load(terms.settlement(), clock.completed()).orElseThrow(() -> new PurchaseDeclined("Food needs unknown"));
            var priority = new VillagePriorityRepository(connection).latest(terms.settlement()).orElseThrow();
            if (priority.selected() != VillagePriorities.Action.SEEK_FOOD || needs.demand() <= needs.fulfilled())
                throw new PurchaseDeclined("Village is not currently seeking food");
            var account = commodities.read(terms.settlement()).orElseThrow();
            long supply = account.state().stocks().get(Commodity.FOOD).units();
            supply = Math.addExact(supply, purchases.reservedUnits(terms.settlement()));
            supply = Math.addExact(supply, new FoodDeliveryRepository(connection).pendingUnits(terms.settlement()));
            long target = Math.multiplyExact(needs.demand(), policy.reservePeriods());
            if (supply >= target || terms.units() > target - supply) throw new PurchaseDeclined("Food offer exceeds remaining village purchase need");
            long fairValue = Math.multiplyExact(settings.price(Commodity.FOOD, needs.sharedFood()).movePointRight(2).longValueExact(), terms.units());
            var budget = new TreasuryRepository(connection).load(terms.settlement()).orElseThrow(() -> new PurchaseDeclined("Village treasury unknown"));
            if (terms.emeralds() > FoodPurchases.maximumPayment(fairValue, budget.hundredths(), policy))
                throw new PurchaseDeclined("Offer exceeds perceived value or available budget");
            if (new FoodDeliveryRepository(connection).find(terms.operation()).isPresent()) throw new SQLException("Operation already used for food delivery");
            return purchases.reserve(terms, clock.completed()).status().name();
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
        if(evidence==null||!evidence.complete()) saveClock(town,new CivilizationTime.Anchor(Math.max(now,clock.anchor().lastObservedMillis()),0,clock.completed()));
        else saveClock(town,CivilizationTime.accrue(clock.anchor(),now,clock.cadence(),offline
                ? Math.multiplyExact(clock.cadence(),settings.maximumOfflinePeriods()) : Long.MAX_VALUE).anchor());
    }
    public int pulse(long now, boolean offline) throws SQLException {
        var towns = towns();
        for (var town : towns) transaction(() -> {
            accrue(town,now,offline);
            return null;
        });
        int processed = 0;
        if (towns.isEmpty()) return processed;
        for (int visited = 0; visited < towns.size() && processed < settings.periodsPerBatch(); visited++) {
            var town = towns.get(Math.floorMod(cursor++, towns.size()));
            if (transaction(() -> step(town))) processed++;
        }
        return processed;
    }
    private boolean step(UUID town) throws SQLException {
        var clock = clock(town); requireSettings(clock); var farm = farm(town);
        if (farm == null || !farm.complete() || clock.completed() == clock.anchor().targetSteps()) return false;
        var account = commodities.read(town).orElseThrow();
        long adults = living(town, false), children = living(town, true);
        long output = settings.foodProduction(adults, OptionalLong.of(farm.plantedCrops() / farms.cropsPerWorker()));
        long nextPeriod=Math.incrementExact(clock.completed());var deliveries=new FoodDeliveryRepository(connection);
        var incoming=deliveries.eligible(town,nextPeriod);
        var flows = new TreeMap<Commodity, Flow>(); account.state().stocks().keySet().forEach(commodity -> flows.put(commodity, Flow.idle()));
        flows.put(Commodity.FOOD, new Flow(output, FoodDeliveryRepository.units(incoming), settings.foodDemand(adults, children), 0, 0, 0, 0));
        var period = commodities.advanceRows(town, nextPeriod, flows, settings.shortages());
        var needs = new HouseholdNeedsRepository(connection);
        var assessment = HouseholdNeeds.assess(town, needs.population(town), period, adulthood, settings, needs.load(town, nextPeriod - 1).orElse(null));
        needs.save(assessment);
        var decisions=new VillagePriorityRepository(connection);decisions.consider(assessment,seed,priorities);
        var appeals=new FoodAppealRepository(connection);
        var food=period.reports().stream().filter(report->report.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow();
        appeals.save(FoodAppeals.observe(assessment,decisions.latest(town).orElseThrow(),food,appeals.load(town,nextPeriod-1).orElse(null)));
        deliveries.apply(incoming,nextPeriod);
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
                    new FoodAppealRepository(connection).load(town, account.state().completedPeriods()).orElse(null)));
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
                        r.getInt("complete") == 1, r.getLong("planted_crops")) : null;
            }
        }
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
