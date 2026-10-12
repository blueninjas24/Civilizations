package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Editable public startup spending, separate from private ownership or household prosperity. */
public record StartupGrantPolicy(boolean enabled, long maximumGrant, long perPeriodLimit,
                                 long lifetimeLimit, long minimumTreasury, int foodReservePeriods,
                                 long hundredthsPerFoodUnit, Set<String> industries) {
    public StartupGrantPolicy {
        industries=Collections.unmodifiableSet(new TreeSet<>(industries));
        if(maximumGrant<1 || maximumGrant>100_000_000_000L || perPeriodLimit<1 || perPeriodLimit>100_000_000_000L
                || lifetimeLimit<1 || lifetimeLimit>100_000_000_000L || minimumTreasury<0 || minimumTreasury>100_000_000_000L
                || foodReservePeriods<1 || foodReservePeriods>100 || hundredthsPerFoodUnit<1 || hundredthsPerFoodUnit>100_000_000L
                || industries.isEmpty() || industries.size()>32 || industries.stream().anyMatch(s->!s.matches("[a-z][a-z0-9_-]{0,63}")))
            throw new IllegalArgumentException("Invalid startup grant limits");
    }
    public record Review(boolean eligible,long protectedHundredths,String reason) {}
    public Review review(String industry,long cash,long demand,long amount,long periodSpent,long lifetimeSpent) {
        if(cash<0 || demand<0 || amount<1 || periodSpent<0 || lifetimeSpent<0)throw new IllegalArgumentException("Invalid grant evidence");
        if(!enabled || !industries.contains(industry))return new Review(false,0,"Startup grants disabled for this industry");
        long keep;
        try { keep=Math.max(minimumTreasury,Math.multiplyExact(Math.multiplyExact(demand,foodReservePeriods),hundredthsPerFoodUnit)); }
        catch(ArithmeticException overflow){return new Review(false,Long.MAX_VALUE,"Food protection exceeds supported money range");}
        if(amount>maximumGrant)return new Review(false,keep,"Requested startup exceeds single-grant limit");
        if(periodSpent>perPeriodLimit || amount>perPeriodLimit-periodSpent)return new Review(false,keep,"Village economic-period grant budget exhausted");
        if(lifetimeSpent>lifetimeLimit || amount>lifetimeLimit-lifetimeSpent)return new Review(false,keep,"Village lifetime grant budget exhausted");
        if(cash<keep || amount>cash-keep)return new Review(false,keep,"Startup would spend protected food money");
        return new Review(true,keep,"Real village startup funds available after food protection");
    }
    public static StartupGrantPolicy prototype(){return new StartupGrantPolicy(true,5000,5000,20000,2500,2,200,Set.of("mining","blacksmith","warehouse","logging","fishing","quarry","sandpit"));}
    public static StartupGrantPolicy disabled(){var p=prototype();return new StartupGrantPolicy(false,p.maximumGrant,p.perPeriodLimit,p.lifetimeLimit,p.minimumTreasury,p.foodReservePeriods,p.hundredthsPerFoodUnit,p.industries);}
}
