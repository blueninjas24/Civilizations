package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Editable timed work for existing funded firms; no automatic founding or invented buyers. */
public record IndustryRuntimePolicy(boolean enabled, long millisPerPeriod, int businessesPerPulse,
                                    IndustryPolicy mining, Map<String,ProductionRecipe> recipes, WorkshopSupplyPolicy supply, WarehouseSupplyPolicy warehouseSupply, ToolUpkeepPolicy tools) {
    public IndustryRuntimePolicy {
        Objects.requireNonNull(mining); recipes = Map.copyOf(recipes);
        Objects.requireNonNull(supply);
        Objects.requireNonNull(warehouseSupply);
        Objects.requireNonNull(tools);
        if (millisPerPeriod < 1000 || millisPerPeriod > 86_400_000 || businessesPerPulse < 1 || businessesPerPulse > 64)
            throw new IllegalArgumentException("Industry cadence must be 1000..86400000 ms; pulse budget 1..64");
        var industries = new HashSet<String>();
        if (recipes.size() > 32) throw new IllegalArgumentException("At most 32 runtime recipes");
        for (var e : recipes.entrySet()) {
            if (!e.getKey().equals(e.getValue().id()) || Set.of("mining","warehouse").contains(e.getValue().industry()) || !industries.add(e.getValue().industry()))
                throw new IllegalArgumentException("One named runtime recipe per non-mining industry required");
            if(tools.applies(e.getValue().industry())&&e.getValue().inputs().containsKey(ToolUpkeepPolicy.TOOLS))
                throw new IllegalArgumentException("Tool upkeep cannot also consume tools as a recipe ingredient in the same industry");
        }
    }
    public WorkshopSupplyPolicy purchasingSupply() {
        if(!tools.enabled())return supply;
        var prices=new TreeMap<>(supply.unitHundredths());prices.put(ToolUpkeepPolicy.TOOLS,tools.toolValueHundredths());
        return new WorkshopSupplyPolicy(supply.enabled(),supply.inputPeriods(),supply.warehousesPerReview(),prices);
    }
    public IndustryRuntimePolicy(boolean enabled,long millisPerPeriod,int businessesPerPulse,IndustryPolicy mining,Map<String,ProductionRecipe> recipes,WorkshopSupplyPolicy supply,WarehouseSupplyPolicy warehouseSupply) {
        this(enabled,millisPerPeriod,businessesPerPulse,mining,recipes,supply,warehouseSupply,ToolUpkeepPolicy.disabled());
    }
    public IndustryRuntimePolicy(boolean enabled,long millisPerPeriod,int businessesPerPulse,IndustryPolicy mining,Map<String,ProductionRecipe> recipes,WorkshopSupplyPolicy supply) {
        this(enabled,millisPerPeriod,businessesPerPulse,mining,recipes,supply,WarehouseSupplyPolicy.disabled());
    }
    public IndustryRuntimePolicy(boolean enabled,long millisPerPeriod,int businessesPerPulse,IndustryPolicy mining,Map<String,ProductionRecipe> recipes) {
        this(enabled,millisPerPeriod,businessesPerPulse,mining,recipes,WorkshopSupplyPolicy.disabled());
    }
    public static IndustryRuntimePolicy disabled() {
        return new IndustryRuntimePolicy(false,30_000,8,IndustryPolicy.prototype(),Map.of());
    }
}
