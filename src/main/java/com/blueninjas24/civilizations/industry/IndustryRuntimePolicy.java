package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Editable timed work for existing funded firms; no automatic founding or invented buyers. */
public record IndustryRuntimePolicy(boolean enabled, long millisPerPeriod, int businessesPerPulse,
                                    IndustryPolicy mining, Map<String,ProductionRecipe> recipes, WorkshopSupplyPolicy supply, WarehouseSupplyPolicy warehouseSupply, ToolUpkeepPolicy tools,StorageUpgradePolicy storageUpgrades) {
    public IndustryRuntimePolicy {
        Objects.requireNonNull(mining); recipes = Map.copyOf(recipes);
        Objects.requireNonNull(supply);
        Objects.requireNonNull(warehouseSupply);
        Objects.requireNonNull(tools);
        Objects.requireNonNull(storageUpgrades);
        if (millisPerPeriod < 1000 || millisPerPeriod > 86_400_000 || businessesPerPulse < 1 || businessesPerPulse > 64)
            throw new IllegalArgumentException("Industry cadence must be 1000..86400000 ms; pulse budget 1..64");
        var industries = new HashSet<String>();
        if (recipes.size() > 32) throw new IllegalArgumentException("At most 32 runtime recipes");
        for (var e : recipes.entrySet()) {
            var industry=e.getValue().industry();
            if (!e.getKey().equals(e.getValue().id()) || Set.of("mining","warehouse").contains(industry)
                    || (!industry.equals("bakery")&&!industries.add(industry)))
                throw new IllegalArgumentException("One named runtime recipe per non-bakery industry required");
            if(tools.applies(e.getValue().industry())&&e.getValue().inputs().containsKey(ToolUpkeepPolicy.TOOLS))
                throw new IllegalArgumentException("Tool upkeep cannot also consume tools as a recipe ingredient in the same industry");
        }
    }
    public WorkshopSupplyPolicy purchasingSupply() {
        if(!tools.enabled())return supply;
        var prices=new TreeMap<>(supply.unitHundredths());prices.put(ToolUpkeepPolicy.TOOLS,tools.toolValueHundredths());
        return new WorkshopSupplyPolicy(supply.enabled(),supply.inputPeriods(),supply.warehousesPerReview(),prices);
    }
    public IndustryRuntimePolicy(boolean enabled,long millisPerPeriod,int businessesPerPulse,IndustryPolicy mining,Map<String,ProductionRecipe> recipes,WorkshopSupplyPolicy supply,WarehouseSupplyPolicy warehouseSupply,ToolUpkeepPolicy tools) {
        this(enabled,millisPerPeriod,businessesPerPulse,mining,recipes,supply,warehouseSupply,tools,StorageUpgradePolicy.disabled());
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
