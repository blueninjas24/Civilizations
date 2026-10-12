package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Real public customers: separate food/material caps, never a new source of money. */
public record VillageSupplyPolicy(boolean enabled, long foodPeriodBudget, long materialPeriodBudget,
                                 long minimumTreasury, int foodReservePeriods, int deliveryPeriods,
                                 long haulUnitsPerPeriod, Map<Commodity,Long> prices) {
    public VillageSupplyPolicy {
        prices=Collections.unmodifiableMap(new TreeMap<>(prices));
        if(foodPeriodBudget<1||foodPeriodBudget>100_000_000_000L||materialPeriodBudget<1||materialPeriodBudget>100_000_000_000L
                ||minimumTreasury<0||minimumTreasury>100_000_000_000L||foodReservePeriods<1||foodReservePeriods>100
                ||deliveryPeriods<1||deliveryPeriods>100||haulUnitsPerPeriod<1||haulUnitsPerPeriod>1_000_000
                ||prices.isEmpty()||prices.size()>16||!prices.containsKey(Commodity.FOOD))
            throw new IllegalArgumentException("Invalid village supply budgets");
        prices.forEach((good,price)->{if(price<1||price>100_000_000L)throw new IllegalArgumentException("Invalid village supply price");});
    }
    public long affordable(Commodity good,long cash,long demand,long spent,long wanted,long available,long haulRemaining) {
        if(cash<0||demand<0||spent<0||wanted<0||available<0||haulRemaining<0)throw new IllegalArgumentException("Invalid village supply evidence");
        if(!enabled||!prices.containsKey(good))return 0;
        boolean food=good.equals(Commodity.FOOD);
        long protectedCash=minimumTreasury;
        if(!food) {
            try {protectedCash=Math.max(protectedCash,Math.multiplyExact(Math.multiplyExact(demand,foodReservePeriods),prices.get(Commodity.FOOD)));}
            catch(ArithmeticException overflow){return 0;}
        }
        long cap=food?foodPeriodBudget:materialPeriodBudget;
        return Math.min(Math.min(wanted,available),Math.min(haulRemaining,Math.min(Math.max(0,cap-spent),Math.max(0,cash-protectedCash))/prices.get(good)));
    }
    public static VillageSupplyPolicy prototype(){return new VillageSupplyPolicy(true,2000,1000,2500,2,1,32,
            Map.of(Commodity.FOOD,200L,Commodity.STONE,100L,Commodity.TIMBER,150L));}
    public static VillageSupplyPolicy disabled(){var p=prototype();return new VillageSupplyPolicy(false,p.foodPeriodBudget,p.materialPeriodBudget,p.minimumTreasury,p.foodReservePeriods,p.deliveryPeriods,p.haulUnitsPerPeriod,p.prices);}
}
