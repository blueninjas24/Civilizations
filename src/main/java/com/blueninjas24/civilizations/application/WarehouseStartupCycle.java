package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.WarehouseInvestment;
import com.blueninjas24.civilizations.database.WorkplaceOperations;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Caller-thread polling: one SQL proposal or acceptance in flight, one point check per pass. */
public final class WarehouseStartupCycle {
    private final Function<UUID,CompletableFuture<WarehouseInvestment.Preparation>> prepare;
    private final Function<WarehouseInvestment.Draft,Optional<WorkplaceOperations.Confirmation>> observe;
    private final BiFunction<WarehouseInvestment.Draft,WorkplaceOperations.Confirmation,CompletableFuture<?>> accept;
    private CompletableFuture<WarehouseInvestment.Preparation> preparation;
    private CompletableFuture<?> acceptance;
    private UUID cursor;
    private String diagnostic="Automatic warehouses awaiting review";
    public WarehouseStartupCycle(Function<UUID,CompletableFuture<WarehouseInvestment.Preparation>> prepare,
            Function<WarehouseInvestment.Draft,Optional<WorkplaceOperations.Confirmation>> observe,
            BiFunction<WarehouseInvestment.Draft,WorkplaceOperations.Confirmation,CompletableFuture<?>> accept) {
        this.prepare=Objects.requireNonNull(prepare);this.observe=Objects.requireNonNull(observe);this.accept=Objects.requireNonNull(accept);
    }
    public String diagnostic(){return diagnostic;}
    public void tick(Collection<UUID> towns) {
        try {
            if(acceptance!=null) {
                if(!acceptance.isDone())return;
                acceptance.join();acceptance=null;diagnostic="Automatic warehouse started with real funding and empty storage";return;
            }
            if(preparation!=null) {
                if(!preparation.isDone())return;
                var result=preparation.join();preparation=null;
                diagnostic="Automatic warehouse review: "+result.explanation();
                if(result.draft().isEmpty())return;
                var draft=result.draft().orElseThrow();
                var evidence=observe.apply(draft);
                if(evidence.isEmpty()){diagnostic="Automatic warehouse waits for a loaded workplace; no chunks loaded";return;}
                if(!evidence.orElseThrow().present()){diagnostic="Automatic warehouse waits for its observed barrel; nothing spent";return;}
                acceptance=Objects.requireNonNull(accept.apply(draft,evidence.orElseThrow()));return;
            }
            var ordered=new TreeSet<UUID>(Comparator.comparing(UUID::toString));
            for(var town:towns){ordered.add(Objects.requireNonNull(town));if(ordered.size()>1024)throw new IllegalStateException("Warehouse town review budget exceeded");}
            if(ordered.isEmpty()){diagnostic="Automatic warehouses awaiting saved living residents";return;}
            var next=cursor==null?ordered.first():ordered.higher(cursor);if(next==null)next=ordered.first();
            cursor=next;preparation=Objects.requireNonNull(prepare.apply(next));
        }catch(RuntimeException failure) {
            preparation=null;acceptance=null;
            var cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
            diagnostic="Automatic warehouse deferred: "+cause.getMessage();
            if(!(cause instanceof com.blueninjas24.civilizations.database.BusinessStartupGrants.Declined))throw failure;
        }
    }
}
