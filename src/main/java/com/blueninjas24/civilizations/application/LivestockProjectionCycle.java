package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

/** One claim-before-add handshake at a time. World operations stay with the main-thread adapter. */
public final class LivestockProjectionCycle implements AutoCloseable {
    public interface Owner {
        boolean ready();CompletableFuture<List<Job>> page(UUID after,int limit);
        CompletableFuture<Projection> claim(Claim claim);CompletableFuture<Boolean> activate(UUID birth,UUID entity);
        CompletableFuture<Boolean> failedUnspawned(UUID birth,UUID entity);
    }
    public enum Added { ADDED, NEVER_ATTEMPTED, UNCERTAIN }
    public interface Bodies {
        boolean existing(Job job);
        void apply(Job job);
        void release(Job job,UUID entity);
        Optional<Claim> prepare(Job job);
        Added add(Job job,Claim claim);
        /** Forget a prepared handle; never delete a world entity here. */
        void forget(Claim claim);
    }
    private enum Work { PAGE, CLAIM, ACTIVATE, FAIL }
    private final Owner owner;private final Bodies bodies;private final LongSupplier clock;
    private final Deque<Job> jobs=new ArrayDeque<>();private UUID cursor;private Job job;private Claim claim;
    private Work work;private CompletableFuture<?> pending;private long retryAt;private boolean attempted,closed;private String lastFailure;
    private CompletableFuture<Boolean> shutdown=CompletableFuture.completedFuture(false);
    public LivestockProjectionCycle(Owner owner,Bodies bodies,LongSupplier clock){this.owner=Objects.requireNonNull(owner);this.bodies=Objects.requireNonNull(bodies);this.clock=Objects.requireNonNull(clock);}
    public void tick() {
        if(closed||!owner.ready()||clock.getAsLong()<retryAt)return;
        try {
            if(work!=null) {
                if(pending==null)submit();if(!pending.isDone())return;
                var completed=work;var result=pending.join();pending=null;work=null;
                switch(completed) {
                    case PAGE -> {
                        @SuppressWarnings("unchecked")var page=(List<Job>)result;
                        if(page.size()>8)throw new IllegalStateException("Newborn projection page exceeds budget");
                        jobs.addAll(page);if(page.isEmpty()){cursor=null;retryAt=clock.getAsLong()+1000;return;}
                    }
                    case CLAIM -> {
                        var saved=(Projection)result;if(!saved.claim().equals(claim))throw new IllegalStateException("Newborn claim response changed identity");
                        if(saved.state()==State.CLAIMED) {
                            attempted=true;var added=bodies.add(job,claim);
                            if(added==Added.ADDED){begin(Work.ACTIVATE);return;}
                            if(added==Added.NEVER_ATTEMPTED){attempted=false;begin(Work.FAIL);return;}
                            lastFailure="Uncertain spawn attempt; saved claim retained without replacement";clear();retryAt=clock.getAsLong()+5000;return;
                        }
                        if(saved.state()==State.ACTIVE&&bodies.existing(withProjection(saved))){bodies.apply(withProjection(saved));bodies.release(withProjection(saved),claim.entity());}
                        clear();retryAt=clock.getAsLong()+5000;return;
                    }
                    case ACTIVATE -> {
                        if(Boolean.TRUE.equals(result)){bodies.release(job,claim==null?job.projection().orElseThrow().claim().entity():claim.entity());lastFailure=null;}
                        else lastFailure="Newborn activation superseded; no replacement requested";
                        clear();return;
                    }
                    case FAIL -> {clear();retryAt=clock.getAsLong()+5000;return;}
                }
            }
            if(jobs.isEmpty()){begin(Work.PAGE);return;}
            job=jobs.removeFirst();cursor=job.birth().id();
            var saved=job.projection();
            if(saved.isPresent()) {
                if(saved.get().state()==State.DECEASED){clear();return;}
                if(bodies.existing(job)) {
                    if(saved.get().state()==State.FAILED){lastFailure="Failed newborn candidate unexpectedly exists; review required";clear();return;}
                    bodies.apply(job);
                    if(saved.get().state()==State.CLAIMED){begin(Work.ACTIVATE);return;}
                    bodies.release(job,saved.get().claim().entity());clear();return;
                }
                if(saved.get().state()!=State.FAILED){lastFailure="Saved newborn body not currently visible; no replacement";clear();return;}
            }
            var candidate=bodies.prepare(job);if(candidate.isEmpty()){lastFailure="No safe loaded nearby newborn placement";clear();return;}
            var prepared=candidate.orElseThrow();if(!prepared.birth().equals(job.birth().id())){bodies.forget(prepared);throw new IllegalStateException("Prepared another logical birth");}
            claim=prepared;
            attempted=false;begin(Work.CLAIM);
        }catch(CompletionException failure) {
            // Preserve the exact operation/UUID for retry; no second add is inferred from an SQL error.
            if(work==null)clear();pending=null;lastFailure=message(failure);retryAt=clock.getAsLong()+5000;
        }catch(RuntimeException failure) {
            lastFailure=message(failure);
            if(claim!=null&&!attempted){work=Work.FAIL;pending=null;}
            else {clear();work=null;pending=null;}
            retryAt=clock.getAsLong()+5000;
        }
    }
    private Job withProjection(Projection saved){return new Job(job.birth(),job.species(),job.world(),job.home(),job.herdPeriod(),Optional.of(saved));}
    private static String message(Throwable failure){return failure.getCause()==null?failure.getMessage():failure.getCause().getMessage();}
    private void begin(Work next){work=next;submit();}
    private void submit(){pending=switch(work){case PAGE->owner.page(cursor,8);case CLAIM->owner.claim(claim);case ACTIVATE->owner.activate(job.birth().id(),claim==null?job.projection().orElseThrow().claim().entity():claim.entity());case FAIL->owner.failedUnspawned(claim.birth(),claim.entity());};}
    private void clear(){if(claim!=null)bodies.forget(claim);claim=null;job=null;attempted=false;}
    public String diagnostic(){return "Newborn projection: "+(work==null?"idle":work)+"; last failure: "+(lastFailure==null?"none":lastFailure);}
    /** Main-thread positive proof that this local candidate has had no add attempt. Drop its
     * native handle immediately; the queued immutable cleanup follows any accepted claim save. */
    public boolean abandonUnattempted() {
        if(closed||claim==null||attempted)return false;
        bodies.forget(claim);work=Work.FAIL;pending=null;
        try{submit();}catch(RuntimeException failure){lastFailure=message(failure);retryAt=clock.getAsLong()+5000;}
        return true;
    }
    public CompletableFuture<Boolean> shutdown(){return shutdown;}
    @Override public void close() {
        if(closed)return;closed=true;
        // Positive main-thread proof: this locally prepared candidate never had an add attempt.
        // Queue after its claim but before owner shutdown drains; no callback touches Paper.
        if(claim!=null&&!attempted)shutdown=owner.failedUnspawned(claim.birth(),claim.entity());
        clear();jobs.clear();pending=null;work=null;
    }
}
