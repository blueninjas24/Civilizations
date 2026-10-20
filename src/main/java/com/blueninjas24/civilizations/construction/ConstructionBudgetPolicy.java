package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.Purpose;
import java.util.*;

/** Spending eligibility only. A quote never moves money, reserves materials or authorizes building. */
public record ConstructionBudgetPolicy(Account business,Account village) {
    public enum Source { BUSINESS,VILLAGE }
    public record Account(boolean enabled,long minimumCash,int foodReservePeriods,long cashPerFoodUnit,
                          long essentialUpkeepReserve,long maximumPurchase,long perPeriodLimit,Set<Purpose> purposes) {
        public Account {
            purposes=Set.copyOf(purposes);
            if(minimumCash<0||minimumCash>100_000_000_000L||foodReservePeriods<0||foodReservePeriods>100
                    ||cashPerFoodUnit<1||cashPerFoodUnit>100_000_000L||essentialUpkeepReserve<0||essentialUpkeepReserve>100_000_000_000L
                    ||maximumPurchase<1||maximumPurchase>100_000_000_000L||perPeriodLimit<1||perPeriodLimit>100_000_000_000L||purposes.isEmpty())
                throw new IllegalArgumentException("Invalid construction budget limits");
        }
    }
    public record Evidence(long cash,long foodDemand,long essentialObligations,long alreadyCommitted,long periodSpent) {
        public Evidence{if(cash<0||foodDemand<0||essentialObligations<0||alreadyCommitted<0||periodSpent<0)throw new IllegalArgumentException("Negative construction funding evidence");}
    }
    public record Quote(boolean allowed,long protectedCash,long availableCash,String reason){}
    public ConstructionBudgetPolicy{Objects.requireNonNull(business);Objects.requireNonNull(village);}
    /** Requested cash means an actual proposed purchase, never an invented fee for already-owned materials. */
    public Quote review(Source source,Purpose purpose,long requestedCash,Evidence evidence) {
        Objects.requireNonNull(source);Objects.requireNonNull(purpose);Objects.requireNonNull(evidence);
        if(requestedCash<0)throw new IllegalArgumentException("Negative construction purchase");
        var terms=source==Source.BUSINESS?business:village;
        if(!terms.enabled()||!terms.purposes().contains(purpose))return new Quote(false,0,0,"Construction purchases disabled for this source/purpose");
        long keep;
        try {
            long food=Math.multiplyExact(Math.multiplyExact(evidence.foodDemand(),terms.foodReservePeriods()),terms.cashPerFoodUnit());
            keep=Math.addExact(Math.max(terms.minimumCash(),food),Math.max(terms.essentialUpkeepReserve(),evidence.essentialObligations()));
            keep=Math.addExact(keep,evidence.alreadyCommitted());
        }catch(ArithmeticException overflow){return new Quote(false,Long.MAX_VALUE,0,"Food/upkeep/committed reserves exceed the supported range");}
        long available=evidence.cash()<=keep?0:evidence.cash()-keep;
        if(requestedCash>terms.maximumPurchase())return new Quote(false,keep,available,"Single construction purchase limit exceeded");
        if(evidence.periodSpent()>terms.perPeriodLimit()||requestedCash>terms.perPeriodLimit()-evidence.periodSpent())
            return new Quote(false,keep,available,"Construction period spending limit exhausted");
        if(requestedCash>available)return new Quote(false,keep,available,"Construction would spend protected food, upkeep or committed funds");
        return new Quote(true,keep,available,"Actual purchase affordable after protected reserves; live payment and site checks still required");
    }
    public static ConstructionBudgetPolicy prototype(){return new ConstructionBudgetPolicy(
            new Account(true,500,0,200,500,10_000,20_000,Set.of(Purpose.STORAGE,Purpose.WORKSHOP,Purpose.DOCK)),
            new Account(true,2500,2,200,1000,5000,5000,Set.of(Purpose.HOME,Purpose.PATH)));}
}
