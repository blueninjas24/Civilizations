package com.blueninjas24.civilizations.industry;

/** Bounded purchases by existing warehouses, not a grant or business founding policy. */
public record WarehouseSupplyPolicy(boolean enabled, int customersPerReview, int producersPerReview) {
    public WarehouseSupplyPolicy {
        if(customersPerReview<1 || customersPerReview>64 || producersPerReview<1 || producersPerReview>64)
            throw new IllegalArgumentException("Warehouse customer/producer review budgets must be 1..64");
    }
    public static WarehouseSupplyPolicy disabled(){return new WarehouseSupplyPolicy(false,16,16);}
}
