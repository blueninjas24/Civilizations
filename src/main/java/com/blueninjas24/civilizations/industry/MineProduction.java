package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.Map;
import java.util.TreeMap;

/** One extraction budget produces the entire geological bundle, including all rock byproducts. */
public final class MineProduction {
    private MineProduction() {}
    public record Plan(long batches, Map<Commodity,Long> outputs, long storageSpace, String explanation) {
        public Plan { outputs=Map.copyOf(outputs); }
    }
    public static Plan calculate(long remainingBatches, long requestedBatches, int workers, long batchesPerWorker,
                                 long siteBatchCapacity, long freeStorage, Map<Commodity,Long> outputsPerBatch,
                                 Map<Commodity,Long> spacePerUnit) {
        if(remainingBatches<0 || requestedBatches<0 || workers<0 || batchesPerWorker<1 || siteBatchCapacity<1 || freeStorage<0)
            throw new IllegalArgumentException("Invalid mining limits");
        if(outputsPerBatch.isEmpty())throw new IllegalArgumentException("Extraction requires a geological output bundle");
        long bundleSpace=0;
        for(var entry:outputsPerBatch.entrySet()) {
            var space=spacePerUnit.get(entry.getKey());
            if(entry.getValue()<1 || space==null || space<1)throw new IllegalArgumentException("Every output needs a positive yield and storage cost");
            bundleSpace=Math.addExact(bundleSpace,Math.multiplyExact(entry.getValue(),space));
        }
        long labor=Math.multiplyExact((long)workers,batchesPerWorker);
        long batches=Math.min(Math.min(remainingBatches,requestedBatches),Math.min(labor,Math.min(siteBatchCapacity,freeStorage/bundleSpace)));
        Map<Commodity,Long> outputs=new TreeMap<>();
        for(var entry:outputsPerBatch.entrySet())outputs.put(entry.getKey(),Math.multiplyExact(batches,entry.getValue()));
        return new Plan(batches,outputs,Math.multiplyExact(batches,bundleSpace),"Mining: "+batches+" bundles; requested "+requestedBatches
                +", finite deposit "+remainingBatches+", labor "+labor+", site "+siteBatchCapacity+", storage "+(freeStorage/bundleSpace)+" bundles");
    }
}
