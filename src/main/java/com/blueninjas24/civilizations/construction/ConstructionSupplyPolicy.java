package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Editable reference quotes for actual local construction purchases, never a free supplier. */
public record ConstructionSupplyPolicy(boolean enabled,Map<Commodity,Long> prices) {
    public ConstructionSupplyPolicy {
        prices=Map.copyOf(prices);
        if(prices.isEmpty()||prices.size()>64||prices.values().stream().anyMatch(p->p<1||p>100_000_000))throw new IllegalArgumentException("Construction needs1..64 positive bounded prices");
    }
    public static ConstructionSupplyPolicy prototype(){return new ConstructionSupplyPolicy(true,Map.of(Commodity.STONE,25L,Commodity.TIMBER,40L,new Commodity("wool"),100L,new Commodity("storage_fittings"),200L));}
}
