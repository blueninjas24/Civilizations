package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/** Holds current-capture readiness across an external world write and its accepted SQL saves. */
public final class ResourceMutationCapture implements AutoCloseable {
    public record Changes(UUID world, String worldName, List<Position> orePlacements, List<Position> oreRemovals,
            List<Position> forestPlacements, List<Position> forestRemovals, long observedMillis, String reason) {
        public Changes {
            Objects.requireNonNull(world);Objects.requireNonNull(worldName);Objects.requireNonNull(reason);
            orePlacements=List.copyOf(orePlacements);oreRemovals=List.copyOf(oreRemovals);
            forestPlacements=List.copyOf(forestPlacements);forestRemovals=List.copyOf(forestRemovals);
            if(worldName.isBlank()||worldName.length()>256||reason.isBlank()||reason.length()>2048||observedMillis<0
                    ||List.of(orePlacements,oreRemovals,forestPlacements,forestRemovals).stream().anyMatch(p->p.size()>512))
                throw new IllegalArgumentException("External resource capture exceeds immutable evidence bounds");
        }
    }
    private final Runnable release;
    private final Consumer<String> invalidate;
    private final Function<Changes,CompletableFuture<?>> save;
    private final AtomicBoolean handedOff=new AtomicBoolean();
    private final AtomicBoolean released=new AtomicBoolean();

    ResourceMutationCapture(Runnable release,Consumer<String> invalidate,Function<Changes,CompletableFuture<?>> save) {
        this.release=Objects.requireNonNull(release);this.invalidate=Objects.requireNonNull(invalidate);this.save=Objects.requireNonNull(save);
    }

    /** Explicitly confirmed unchanged/cancelled write; no resource mutation or new provenance is saved. */
    public void unchanged() {
        if(!handedOff.compareAndSet(false,true))throw new IllegalStateException("Resource write outcome already supplied");
        releaseOnce();
    }

    /** Caller must positively establish the actual changed blocks, rather than trusting an editor's estimated return value. */
    public CompletableFuture<Void> complete(Changes changes) {
        Objects.requireNonNull(changes);
        if(!handedOff.compareAndSet(false,true))throw new IllegalStateException("Resource write outcome already supplied");
        try {
            return Objects.requireNonNull(save.apply(changes)).handle((ignored,failure)-> {
                try {
                    if(failure!=null){invalidate.accept("External resource mutation save incomplete; reviewed recovery required");
                        throw new java.util.concurrent.CompletionException(failure);}
                    return (Void)null;
                } finally { releaseOnce(); }
            });
        } catch(RuntimeException failure) {
            try {invalidate.accept("External resource mutation handoff failed; reviewed recovery required");}
            finally {releaseOnce();}
            throw failure;
        }
    }

    /** An exception or an unconfirmed outcome may have changed the world; closing must never silently certify it. */
    @Override public void close() {
        if(handedOff.compareAndSet(false,true)) {
            try {invalidate.accept("External resource mutation outcome unconfirmed; reviewed recovery required");}
            finally {releaseOnce();}
        }
    }
    private void releaseOnce(){if(released.compareAndSet(false,true))release.run();}
}
