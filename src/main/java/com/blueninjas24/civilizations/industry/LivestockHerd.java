package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Deterministic active-period cohort calculation, ready for a future transactional herd ledger.
 * No Paper mutations, stock transfer or wall-clock accrual. Caller must supply real available budgets. */
public final class LivestockHerd {
    private LivestockHerd() {}
    public enum Species { COW, CHICKEN }
    public record Young(long maturePeriod,long heads) {
        public Young {if(maturePeriod<1||heads<1||heads>1_000_000)throw new IllegalArgumentException("Invalid young cohort");}
    }
    public record State(long period,long adults,long capacity,long nextBirthPeriod,List<Young> young) {
        public State {
            young=List.copyOf(young);
            if(period<0||adults<0||adults>1_000_000||capacity<0||capacity>1_000_000||nextBirthPeriod<0||young.size()>1000)
                throw new IllegalArgumentException("Invalid herd state");
            var cohorts=new TreeMap<Long,Long>();
            for(var c:young){if(c.maturePeriod()<=period)throw new IllegalArgumentException("Overdue cohort must have matured in its completed period");cohorts.merge(c.maturePeriod(),c.heads(),Math::addExact);}
            young=cohorts.entrySet().stream().map(e->new Young(e.getKey(),e.getValue())).toList();
            if(Math.addExact(adults,young.stream().mapToLong(Young::heads).sum())>1_000_000)throw new IllegalArgumentException("Herd head limit exceeded");
        }
        public long heads(){return adults+young.stream().mapToLong(Young::heads).sum();}
    }
    public record Plan(State after,long matured,long births,long slaughtered,long feedUsed,long workUsed,
                       long renewableUnits,long meatUnits) {}
    public static Plan plan(State before,long feed,long work,long storage,long slaughterRequest,
                            LivestockPolicy policy,Species species,boolean worldActive) {
        return plan(before,feed,work,storage,slaughterRequest,policy,species,worldActive,0);
    }
    /** Unavailable adults remain alive, owned and aging; only their work/breeding is paused. */
    public static Plan plan(State before,long feed,long work,long storage,long slaughterRequest,
                            LivestockPolicy policy,Species species,boolean worldActive,long unavailableAdults) {
        Objects.requireNonNull(before);Objects.requireNonNull(policy);Objects.requireNonNull(species);
        if(feed<0||work<0||storage<0||slaughterRequest<0||unavailableAdults<0)throw new IllegalArgumentException("Negative herd budget");
        if(!worldActive||!policy.enabled())return new Plan(before,0,0,0,0,0,0,0);
        long period=Math.incrementExact(before.period());long matured=0;
        var young=new ArrayList<Young>();
        for(var cohort:before.young())if(cohort.maturePeriod()<=period)matured=Math.addExact(matured,cohort.heads());else young.add(cohort);
        long totalAdults=Math.addExact(before.adults(),matured);
        if(unavailableAdults>totalAdults)throw new IllegalArgumentException("Unavailable adults exceed herd");
        var production=LivestockWork.plan(totalAdults-unavailableAdults,feed,work,storage,slaughterRequest,
                species==Species.COW?policy.cow():policy.chicken());
        long retainedAdults=totalAdults-production.slaughtered();
        long heads=retainedAdults+young.stream().mapToLong(Young::heads).sum();
        long fedSurvivors=Math.min(production.fedAdults(),production.retainedAdults());
        long births=period<before.nextBirthPeriod()?0:Math.min(Math.min(policy.maximumBirthsPerPeriod(),fedSurvivors/2),
                Math.min(Math.max(0,before.capacity()-heads),Math.min((feed-production.feedUsed())/policy.feedPerBirth(),(work-production.workUsed())/policy.workPerBirth())));
        long nextBirth=before.nextBirthPeriod();
        if(births>0){young.add(new Young(Math.addExact(period,policy.maturityPeriods()),births));nextBirth=Math.addExact(period,policy.breedingDelayPeriods());}
        return new Plan(new State(period,retainedAdults,before.capacity(),nextBirth,young),matured,births,production.slaughtered(),
                Math.addExact(production.feedUsed(),Math.multiplyExact(births,policy.feedPerBirth())),
                Math.addExact(production.workUsed(),Math.multiplyExact(births,policy.workPerBirth())),production.renewableUnits(),production.meatUnits());
    }
}
