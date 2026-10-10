package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Existing business funds buy real recipe inputs; prices are hundredths of editable Trade Value. */
public record WorkshopSupplyPolicy(boolean enabled, int inputPeriods, int warehousesPerReview,
                                   Map<Commodity,Long> unitHundredths) {
    public WorkshopSupplyPolicy {
        unitHundredths=Map.copyOf(unitHundredths);
        if(inputPeriods<1 || inputPeriods>8 || warehousesPerReview<1 || warehousesPerReview>64
                || unitHundredths.size()>64 || unitHundredths.values().stream().anyMatch(p->p<1 || p>100_000_000))
            throw new IllegalArgumentException("Supply coverage 1..8, review budget 1..64 and positive bounded prices required");
    }
    public static WorkshopSupplyPolicy disabled(){return new WorkshopSupplyPolicy(false,1,16,Map.of());}
}
