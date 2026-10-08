package com.blueninjas24.civilizations.economy;

import java.math.BigInteger;
import java.util.*;

/** Exact, deterministic period accounting. No inferred output, consumption rates or crisis effects. */
public final class CommodityAccounting {
    private CommodityAccounting() {}
    public enum Condition { SEVERE_SHORTAGE, SHORTAGE, BALANCED, SURPLUS, ABUNDANT }
    /** Ratio thresholds in basis points (10,000 = one full period's demand). No runtime defaults. */
    public record Policy(int severeCoverageBps, int surplusCoverageBps, int abundantCoverageBps, long severeAfterPeriods) {
        public Policy {
            if (severeCoverageBps < 0 || severeCoverageBps >= 10_000 || surplusCoverageBps <= 10_000
                    || abundantCoverageBps <= surplusCoverageBps || severeAfterPeriods < 1)
                throw new IllegalArgumentException("Invalid shortage policy");
        }
    }
    public record Stock(long units, long shortagePeriods) {
        public Stock { nonnegative(units, shortagePeriods); }
    }
    /** Inputs describe requested uses, not guaranteed exports or production capacity. */
    public record Flow(long production, long imports, long households, long industry,
                       long construction, long exports, long events) {
        public Flow { nonnegative(production, imports, households, industry, construction, exports, events); }
        public long incoming() { return Math.addExact(production, imports); }
        public long demand() {
            return Math.addExact(Math.addExact(Math.addExact(households, industry), Math.addExact(construction, exports)), events);
        }
        public static Flow idle() { return new Flow(0, 0, 0, 0, 0, 0, 0); }
    }
    public record State(long completedPeriods, Map<Commodity, Stock> stocks) {
        public State {
            nonnegative(completedPeriods);
            stocks = Collections.unmodifiableMap(new TreeMap<>(stocks));
            if (stocks.isEmpty() || stocks.values().stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("An explicit nonempty commodity inventory is required");
        }
    }
    public record Report(Commodity commodity, Stock before, Flow flow, long available, long demand,
                         long fulfilled, long unmet, Stock after, Condition condition) {
        public String explanation() {
            return commodity.id() + ": " + condition + "; reserve " + before.units() + ", produced " + flow.production()
                    + ", imported " + flow.imports() + ", demand " + demand + ", fulfilled " + fulfilled
                    + ", unmet " + unmet + ", remaining " + after.units() + ", consecutive shortage periods " + after.shortagePeriods();
        }
    }
    public record Result(State state, List<Report> reports) { public Result { reports = List.copyOf(reports); } }

    /** Every commodity requires explicit inputs; omitted/unknown entries are not interpreted as zero. */
    public static Result step(State before, Map<Commodity, Flow> flows, Policy policy) {
        Objects.requireNonNull(policy);
        if (!before.stocks().keySet().equals(flows.keySet())) throw new IllegalArgumentException("Flows must cover exactly the initialized commodities");
        var stocks = new TreeMap<Commodity, Stock>(); var reports = new ArrayList<Report>();
        for (var entry : before.stocks().entrySet()) {
            var commodity = entry.getKey(); var stock = entry.getValue(); var flow = Objects.requireNonNull(flows.get(commodity));
            long available = Math.addExact(stock.units(), flow.incoming()), demand = flow.demand();
            long fulfilled = Math.min(available, demand), unmet = demand - fulfilled;
            long streak = unmet > 0 ? Math.incrementExact(stock.shortagePeriods()) : 0;
            var after = new Stock(available - fulfilled, streak);
            Condition condition;
            if (unmet > 0) condition = streak >= policy.severeAfterPeriods() && atMost(available, demand, policy.severeCoverageBps())
                    ? Condition.SEVERE_SHORTAGE : Condition.SHORTAGE;
            else if (demand == 0) condition = Condition.BALANCED; // Idle reserves do not imply a known market opportunity.
            else if (atLeast(available, demand, policy.abundantCoverageBps())) condition = Condition.ABUNDANT;
            else if (atLeast(available, demand, policy.surplusCoverageBps())) condition = Condition.SURPLUS;
            else condition = Condition.BALANCED;
            stocks.put(commodity, after);
            reports.add(new Report(commodity, stock, flow, available, demand, fulfilled, unmet, after, condition));
        }
        return new Result(new State(Math.incrementExact(before.completedPeriods()), stocks), reports);
    }
    private static int compare(long available, long demand, int bps) {
        return BigInteger.valueOf(available).multiply(BigInteger.valueOf(10_000))
                .compareTo(BigInteger.valueOf(demand).multiply(BigInteger.valueOf(bps)));
    }
    private static boolean atMost(long available, long demand, int bps) { return compare(available, demand, bps) <= 0; }
    private static boolean atLeast(long available, long demand, int bps) { return compare(available, demand, bps) >= 0; }
    private static void nonnegative(long... values) {
        for (long value : values) if (value < 0) throw new IllegalArgumentException("Economy quantities cannot be negative");
    }
}
