package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.WorkshopInvestment;
import com.blueninjas24.civilizations.database.WorkplaceOperations;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Caller-thread polling: one SQL proposal or acceptance in flight, one point check per pass. */
public final class WorkshopStartupCycle {
    private final Function<UUID,CompletableFuture<WorkshopInvestment.Preparation>> prepare;
    private final Function<WorkshopInvestment.Draft,Optional<WorkplaceOperations.Confirmation>> observe;
    private final BiFunction<WorkshopInvestment.Draft,WorkplaceOperations.Confirmation,CompletableFuture<?>> accept;
    private CompletableFuture<WorkshopInvestment.Preparation> preparation;
    private CompletableFuture<?> acceptance;
    private UUID cursor;
    private String diagnostic="Automatic workshops awaiting review";
    public WorkshopStartupCycle(Function<UUID,CompletableFuture<WorkshopInvestment.Preparation>> prepare,
            Function<WorkshopInvestment.Draft,Optional<WorkplaceOperations.Confirmation>> observe,
            BiFunction<WorkshopInvestment.Draft,WorkplaceOperations.Confirmation,CompletableFuture<?>> accept) {
        this.prepare=Objects.requireNonNull(prepare);this.observe=Objects.requireNonNull(observe);this.accept=Objects.requireNonNull(accept);
    }
    public String diagnostic(){return diagnostic;}
    public void tick(Collection<UUID> towns) {
        try {
            if(acceptance!=null) {
                if(!acceptance.isDone())return;
                acceptance.join();acceptance=null;diagnostic="Automatic workshop started with real paid supplies";return;
            }
            if(preparation!=null) {
                if(!preparation.isDone())return;
                var result=preparation.join();preparation=null;
                diagnostic="Automatic workshop review: "+result.explanation();
                if(result.draft().isEmpty())return;
                var draft=result.draft().orElseThrow();
                var evidence=observe.apply(draft);
                if(evidence.isEmpty()){diagnostic="Automatic workshop waits for a loaded workplace; no chunks loaded";return;}
                if(!evidence.orElseThrow().present()){diagnostic="Automatic workshop waits for its observed smithing table; nothing spent";return;}
                acceptance=Objects.requireNonNull(accept.apply(draft,evidence.orElseThrow()));return;
            }
            var ordered=new TreeSet<UUID>(Comparator.comparing(UUID::toString));
            for(var town:towns){ordered.add(Objects.requireNonNull(town));if(ordered.size()>1024)throw new IllegalStateException("Workshop town review budget exceeded");}
            if(ordered.isEmpty()){diagnostic="Automatic workshops awaiting saved living residents";return;}
            var next=cursor==null?ordered.first():ordered.higher(cursor);if(next==null)next=ordered.first();
            cursor=next;preparation=Objects.requireNonNull(prepare.apply(next));
        }catch(RuntimeException failure) {
            preparation=null;acceptance=null;
            var cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
            diagnostic="Automatic workshop deferred: "+cause.getMessage();
            if(!(cause instanceof com.blueninjas24.civilizations.database.BusinessStartupGrants.Declined))throw failure;
        }
    }
}
