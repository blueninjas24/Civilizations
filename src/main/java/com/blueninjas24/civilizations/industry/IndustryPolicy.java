package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.Map;

/** Editable prototype inputs; persistent deposits/stores retain their original opening terms. */
public record IndustryPolicy(long depositBatchesPerObservedOre, long batchesPerWorker,
                             long siteBatchCapacity, long storageCapacity, long localDeliveryPeriods, long localHaulUnitsPerPeriod,
                             Map<Commodity,Long> miningBundle, Map<Commodity,Long> storageSpacePerUnit) {
    public IndustryPolicy {
        miningBundle=Map.copyOf(miningBundle); storageSpacePerUnit=Map.copyOf(storageSpacePerUnit);
        for(long value:new long[]{depositBatchesPerObservedOre,batchesPerWorker,siteBatchCapacity,storageCapacity,localDeliveryPeriods,localHaulUnitsPerPeriod})
            if(value<1 || value>1_000_000_000L)throw new IllegalArgumentException("Industry settings must be 1..1000000000");
        if(miningBundle.isEmpty() || miningBundle.size()>32 || storageSpacePerUnit.size()>64)
            throw new IllegalArgumentException("Invalid industry commodity catalog size");
        for(var value:storageSpacePerUnit.values())if(value<1 || value>1_000_000L)throw new IllegalArgumentException("Invalid storage unit size");
        for(var value:miningBundle.values())if(value<1 || value>1_000_000L)throw new IllegalArgumentException("Invalid mining yield");
        MineProduction.calculate(0,0,0,batchesPerWorker,siteBatchCapacity,storageCapacity,miningBundle,storageSpacePerUnit);
    }
    public long initialDepositBatches(long observedOreBlocks) {
        if(observedOreBlocks<1)throw new IllegalArgumentException("Discovery requires positive trusted ore evidence");
        return Math.multiplyExact(observedOreBlocks,depositBatchesPerObservedOre);
    }
    public static IndustryPolicy prototype() {
        return new IndustryPolicy(150,8,32,1024,1,128,
                Map.of(Commodity.COAL,1L,Commodity.STONE,3L),
                Map.of(Commodity.COAL,1L,Commodity.STONE,1L,Commodity.IRON,1L,Commodity.TIMBER,2L,Commodity.FOOD,1L));
    }
}
