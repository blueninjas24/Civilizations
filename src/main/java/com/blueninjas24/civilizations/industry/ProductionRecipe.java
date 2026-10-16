package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Abstract material transformation, not an item trade or permission to change blocks. */
public record ProductionRecipe(String id, String industry, Map<Commodity,Long> inputs,
                               Map<Commodity,Long> outputs, long batchesPerWorker, long siteCapacity) {
    public ProductionRecipe {
        new Commodity(id); new Commodity(industry);
        inputs=Map.copyOf(inputs); outputs=Map.copyOf(outputs);
        for(var catalog:List.of(inputs,outputs))
            if(catalog.isEmpty() || catalog.size()>32 || catalog.values().stream().anyMatch(v->v<1 || v>1_000_000))
                throw new IllegalArgumentException("Recipe needs bounded positive input and output quantities");
        if(batchesPerWorker<1 || batchesPerWorker>1_000_000 || siteCapacity<1 || siteCapacity>1_000_000)
            throw new IllegalArgumentException("Invalid recipe work capacity");
    }
    public static ProductionRecipe smithTools() {
        return new ProductionRecipe("smith_tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),
                Map.of(new Commodity("tools"),1L),2,8);
    }
    /** Editable abstract grain-to-meal prototype; does not represent items taken from a chest. */
    public static ProductionRecipe bakeryBread() {
        return new ProductionRecipe("bakery_bread","bakery",Map.of(new Commodity("grain"),3L),
                Map.of(Commodity.FOOD,3L),1,4);
    }
}
