package com.blueninjas24.civilizations.economy;

import java.util.Objects;

/** Deterministic price discussions; no inventory, spending, reputation or invented production. */
public final class FoodBargaining {
    private FoodBargaining() {}
    public record Policy(boolean enabled, int maximumAttempts, int cooldownSeconds, int offerSeconds) {
        public Policy {
            if (maximumAttempts < 1 || maximumAttempts > 100 || cooldownSeconds < 1 || cooldownSeconds > 86400
                    || offerSeconds < 1 || offerSeconds > 3600) throw new IllegalArgumentException("Invalid bargaining policy");
        }
        public static Policy prototype() { return new Policy(true,3,60,45); }
        public static Policy disabled() { return new Policy(false,3,60,45); }
    }
    public enum Status { AGREED, COUNTER, CLOSED, RESERVED }
    public record Result(Status status, int offer, int failedAttempts, long observedMillis, long expiresMillis,
                         long cooldownUntil, Policy policy) {
        public Result {
            Objects.requireNonNull(status); Objects.requireNonNull(policy);
            if (offer < 0 || offer > 64 || failedAttempts < 0 || failedAttempts > 100 || observedMillis < 0
                    || expiresMillis < observedMillis || cooldownUntil < 0) throw new IllegalArgumentException("Invalid bargain result");
        }
    }
    public record Deal(FoodPurchases.Terms requested, Result result) {
        public Deal { Objects.requireNonNull(requested); Objects.requireNonNull(result); }
        public FoodPurchases.Terms paymentTerms() {
            var t=requested;
            return new FoodPurchases.Terms(t.operation(),t.player(),t.settlement(),t.units(),t.item(),t.count(),result.offer(),t.hundredthsPerEmerald());
        }
    }
    public static Result discuss(int requested, int affordableValue, long now, Result previous, Policy policy) {
        if (!policy.enabled() || requested < 1 || requested > 64 || affordableValue < 1 || affordableValue > 64 || now < 0)
            throw new IllegalArgumentException("Enabled bargaining and a possible whole-emerald offer required");
        long observed=Math.max(now,previous==null ? 0 : previous.observedMillis());
        if (previous!=null && previous.status()==Status.CLOSED && observed<previous.cooldownUntil()) return previous;
        boolean continuing=previous!=null && previous.status()==Status.COUNTER && observed<previous.expiresMillis();
        int failed=continuing ? previous.failedAttempts() : 0;
        long expires=continuing ? previous.expiresMillis() : Math.addExact(observed,Math.multiplyExact(policy.offerSeconds(),1000L));
        if (requested<=affordableValue) return new Result(Status.AGREED,requested,0,observed,expires,0,policy);
        failed=Math.min(100,failed+1);
        if (failed>=policy.maximumAttempts()) return new Result(Status.CLOSED,0,failed,observed,expires,
                Math.addExact(observed,Math.multiplyExact(policy.cooldownSeconds(),1000L)),policy);
        return new Result(Status.COUNTER,affordableValue,failed,observed,expires,0,policy);
    }
}
