package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.Map;

/** New finite stone/sand opening terms. Retuning never rewrites saved deposits. */
public record QuarryPolicy(boolean enabled, Terms stone, Terms sand) {
    public record Terms(long reserveBatchesPerBlock,long unitsPerBatch,long batchesPerWorker,long siteCapacity) {
        public Terms {
            if(reserveBatchesPerBlock<1||reserveBatchesPerBlock>1_000_000_000L||unitsPerBatch<1||unitsPerBatch>1_000_000
                ||batchesPerWorker<1||batchesPerWorker>1_000_000||siteCapacity<1||siteCapacity>1_000_000)
                throw new IllegalArgumentException("Invalid finite quarry terms");
        }
    }
    public QuarryPolicy {java.util.Objects.requireNonNull(stone);java.util.Objects.requireNonNull(sand);}
    public Terms terms(Commodity resource) {
        if(resource.equals(Commodity.STONE))return stone;
        if(resource.equals(Commodity.SAND))return sand;
        throw new IllegalArgumentException("Quarry terms require stone or sand");
    }
    public Map<Commodity,Long> bundle(Commodity resource){return Map.of(resource,terms(resource).unitsPerBatch());}
    public static QuarryPolicy prototype(){return new QuarryPolicy(true,new Terms(64,3,6,32),new Terms(64,4,12,32));}
    public static QuarryPolicy disabled(){var p=prototype();return new QuarryPolicy(false,p.stone(),p.sand());}
}
