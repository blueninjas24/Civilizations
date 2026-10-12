package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** One paid startup bundle, never an ongoing public supplier or free input grant. */
public record WorkshopStarterPolicy(boolean enabled,int maximumBatches,long maximumHundredths,
                                    long reserveUnits,Map<Commodity,Long> prices) {
    public WorkshopStarterPolicy {
        prices=Collections.unmodifiableMap(new TreeMap<>(prices));
        if(maximumBatches<1||maximumBatches>16||maximumHundredths<1||maximumHundredths>100_000_000_000L
                ||reserveUnits<0||reserveUnits>1_000_000_000L||prices.size()>64
                ||prices.containsKey(Commodity.FOOD)||prices.values().stream().anyMatch(p->p<1||p>100_000_000L))
            throw new IllegalArgumentException("Invalid workshop starter limits/prices; food cannot be sold");
    }
    public static WorkshopStarterPolicy disabled(){return new WorkshopStarterPolicy(false,1,4000,2,Map.of());}
}
