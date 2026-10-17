package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockProjectionCycleTest {
    UUID business=new UUID(0,1),world=new UUID(0,2),entity=new UUID(0,3);
    LivestockBirth birth=LivestockBirth.batch(business,1,1,12).getFirst();
    Job job=new Job(birth,LivestockHerd.Species.COW,world,new Position(0,64,0),1,Optional.empty());
    Claim claim=new Claim(birth.id(),entity,world,LivestockHerd.Species.COW,0,64,0,1000);long now=1000;
    class Owner implements LivestockProjectionCycle.Owner {
        boolean ready=true;int pages,claims,activations,failures;List<Job> jobs=List.of(job);List<Claim> requests=new ArrayList<>();
        CompletableFuture<Projection> claimed=new CompletableFuture<>();CompletableFuture<Boolean> activated=new CompletableFuture<>(),failed=new CompletableFuture<>();
        public boolean ready(){return ready;}
        public CompletableFuture<List<Job>> page(UUID after,int limit){assertEquals(8,limit);pages++;return CompletableFuture.completedFuture(jobs);}
        public CompletableFuture<Projection> claim(Claim c){claims++;requests.add(c);return claimed;}
        public CompletableFuture<Boolean> activate(UUID b,UUID e){assertEquals(birth.id(),b);assertEquals(entity,e);activations++;return activated;}
        public CompletableFuture<Boolean> failedUnspawned(UUID b,UUID e){assertEquals(birth.id(),b);assertEquals(entity,e);failures++;return failed;}
    }
    class Bodies implements LivestockProjectionCycle.Bodies {
        boolean exists,place=true;int prepares,adds,applies,releases,forgets;RuntimeException addFailure;Claim candidate=claim;LivestockProjectionCycle.Added added=LivestockProjectionCycle.Added.ADDED;
        public boolean existing(Job j){return exists;}
        public void apply(Job j){applies++;}
        public void release(Job j,UUID e){assertEquals(entity,e);releases++;}
        public Optional<Claim> prepare(Job j){prepares++;return place?Optional.of(candidate):Optional.empty();}
        public LivestockProjectionCycle.Added add(Job j,Claim c){adds++;if(addFailure!=null)throw addFailure;if(added==LivestockProjectionCycle.Added.ADDED)exists=true;return added;}
        public void forget(Claim c){forgets++;}
    }
    Owner owner=new Owner();Bodies bodies=new Bodies();LivestockProjectionCycle cycle=new LivestockProjectionCycle(owner,bodies,()->now);
    void request(){cycle.tick();cycle.tick();assertEquals(1,owner.claims);}
    void claimed(){owner.claimed.complete(new Projection(claim,State.CLAIMED));cycle.tick();}
    Job state(State s){return new Job(birth,job.species(),world,job.home(),1,Optional.of(new Projection(claim,s)));}
    @Test void commitPrecedesOneAddAndActivationCommitPrecedesRelease() {
        request();cycle.tick();assertEquals(0,bodies.adds);claimed();assertEquals(1,bodies.adds);assertEquals(1,owner.activations);assertEquals(0,bodies.releases);
        cycle.tick();assertEquals(0,bodies.releases);owner.activated.complete(true);cycle.tick();assertEquals(1,bodies.releases);assertEquals(1,bodies.forgets);assertEquals(0,owner.failures);
    }
    @Test void uncertainClaimWriteRetriesTheExactPreparedUuidBeforeAnyAdd() {
        request();owner.claimed.completeExceptionally(new IllegalStateException("fixture claim save"));cycle.tick();assertEquals(0,bodies.adds);cycle.tick();assertEquals(1,owner.claims);
        now=6000;owner.claimed=CompletableFuture.completedFuture(new Projection(claim,State.CLAIMED));cycle.tick();assertEquals(List.of(claim,claim),owner.requests);assertEquals(1,bodies.adds);assertEquals(1,bodies.prepares);
    }
    @Test void failedActivationRetriesOnlyActivationAndNeverAddsAnotherBody() {
        request();claimed();owner.activated.completeExceptionally(new IllegalStateException("fixture activation save"));cycle.tick();assertEquals(1,bodies.adds);assertEquals(0,bodies.releases);
        now=6000;owner.activated=CompletableFuture.completedFuture(true);cycle.tick();assertEquals(2,owner.activations);assertEquals(1,bodies.adds);assertEquals(1,bodies.releases);assertEquals(0,owner.failures);
    }
    @Test void lostSafePlacementUsesPositiveNeverAttemptedFailureBeforeDiscard() {
        bodies.added=LivestockProjectionCycle.Added.NEVER_ATTEMPTED;request();claimed();assertEquals(1,owner.failures);assertEquals(0,bodies.forgets);assertEquals(0,owner.activations);
        owner.failed.completeExceptionally(new IllegalStateException("fixture fail save"));cycle.tick();assertEquals(0,bodies.forgets);now=6000;owner.failed=CompletableFuture.completedFuture(true);cycle.tick();assertEquals(2,owner.failures);assertEquals(1,bodies.forgets);assertEquals(1,bodies.prepares);
    }
    @Test void uncertainNativeAddAndLaterMissingClaimNeverAuthorizeReplacement() {
        bodies.added=LivestockProjectionCycle.Added.UNCERTAIN;request();claimed();assertEquals(0,owner.failures);assertEquals(0,owner.activations);assertEquals(1,bodies.forgets);
        owner.jobs=List.of(state(State.CLAIMED));now=6000;cycle.tick();cycle.tick();assertEquals(1,bodies.prepares);assertEquals(1,bodies.adds);assertEquals(1,owner.claims);assertTrue(cycle.diagnostic().contains("no replacement"));
    }
    @Test void thrownNativeAddIsUncertainEvenIfItsCallerCannotTellWhetherItEnteredWorld() {
        bodies.addFailure=new IllegalStateException("fixture add after possible side effect");request();claimed();assertEquals(0,owner.failures);assertEquals(1,bodies.forgets);
        owner.jobs=List.of(state(State.CLAIMED));now=6000;cycle.tick();cycle.tick();assertEquals(1,bodies.prepares);assertEquals(1,bodies.adds);
    }
    @Test void missingClaimedAndActiveBodiesStayUnknownWithoutPreparation() {
        for(var state:List.of(State.CLAIMED,State.ACTIVE)) {
            var o=new Owner();o.jobs=List.of(state(state));var b=new Bodies();var c=new LivestockProjectionCycle(o,b,()->now);c.tick();c.tick();
            assertEquals(0,b.prepares);assertEquals(0,o.failures);assertEquals(0,o.claims);assertTrue(c.diagnostic().contains("no replacement"));
        }
    }
    @Test void positivelyFoundClaimedBodyRecoversWithoutAnotherClaimOrAdd() {
        owner.jobs=List.of(state(State.CLAIMED));bodies.exists=true;cycle.tick();cycle.tick();assertEquals(1,bodies.applies);assertEquals(1,owner.activations);assertEquals(0,bodies.adds);assertEquals(0,bodies.prepares);
        owner.activated.complete(true);cycle.tick();assertEquals(1,bodies.releases);assertEquals(0,owner.claims);
    }
    @Test void activeBodiesReceiveSavedAgeAndPendingReleaseWithoutClaimingAnythingElse() {
        owner.jobs=List.of(state(State.ACTIVE));bodies.exists=true;cycle.tick();cycle.tick();assertEquals(1,bodies.applies);assertEquals(1,bodies.releases);assertEquals(0,owner.activations);assertEquals(0,bodies.prepares);
    }
    @Test void failedCandidateUnexpectedlyPresentRequiresReviewRatherThanSpawningOrDeleting() {
        owner.jobs=List.of(state(State.FAILED));bodies.exists=true;cycle.tick();cycle.tick();assertEquals(0,bodies.prepares);assertEquals(0,bodies.releases);assertEquals(0,bodies.forgets);assertTrue(cycle.diagnostic().contains("review required"));
    }
    @Test void closeBeforeClaimAcknowledgementQueuesPositiveNeverAddedProofAndNoLateWorldCallback() {
        request();cycle.close();assertEquals(1,owner.failures);assertEquals(1,bodies.forgets);owner.claimed.complete(new Projection(claim,State.CLAIMED));owner.failed.complete(true);cycle.tick();
        assertEquals(0,bodies.adds);assertTrue(cycle.shutdown().join());cycle.close();assertEquals(1,owner.failures);
    }
    @Test void closeAfterAddKeepsItsSavedClaimAndLateActivationDoesNotTouchWorld() {
        request();claimed();cycle.close();owner.activated.complete(true);cycle.tick();assertEquals(0,owner.failures);assertEquals(0,bodies.releases);assertEquals(1,bodies.forgets);assertEquals(1,bodies.adds);
    }
    @Test void unreadyOwnerUnsafeLocationAndOversizedPagesCauseNoSpawnWork() {
        owner.ready=false;cycle.tick();assertEquals(0,owner.pages);owner.ready=true;bodies.place=false;cycle.tick();cycle.tick();assertEquals(0,owner.claims);assertEquals(0,bodies.adds);
        var o=new Owner();o.jobs=Collections.nCopies(9,job);var b=new Bodies();var c=new LivestockProjectionCycle(o,b,()->now);c.tick();c.tick();assertEquals(0,b.prepares);assertTrue(c.diagnostic().contains("exceeds budget"));
    }
    @Test void preparedOrReturnedIdentityMismatchCannotAuthorizeAnotherLogicalBirth() {
        bodies.candidate=new Claim(UUID.randomUUID(),entity,world,job.species(),0,64,0,1000);cycle.tick();cycle.tick();assertEquals(0,owner.claims);assertEquals(0,owner.failures);assertEquals(1,bodies.forgets);
        var o=new Owner();var b=new Bodies();var c=new LivestockProjectionCycle(o,b,()->now);c.tick();c.tick();o.claimed.complete(new Projection(new Claim(UUID.randomUUID(),entity,world,job.species(),0,64,0,1000),State.CLAIMED));c.tick();assertEquals(0,b.adds);assertEquals(0,o.activations);
    }
    @Test void unloadedWorldDropsUnaddedNativeHandleWhileClaimAndCleanupFinishInOwnerOrder() {
        request();assertTrue(cycle.abandonUnattempted());assertEquals(1,bodies.forgets);assertEquals(1,owner.failures);assertEquals(0,bodies.adds);
        owner.claimed.complete(new Projection(claim,State.CLAIMED));cycle.tick();assertEquals(0,bodies.adds);assertEquals(0,owner.activations);
        owner.failed.completeExceptionally(new IllegalStateException("fixture unload cleanup retry"));cycle.tick();now=6000;owner.failed=CompletableFuture.completedFuture(true);cycle.tick();assertEquals(2,owner.failures);assertEquals(0,bodies.adds);
    }
    @Test void anAttemptedBodyCannotBeDeclaredNeverAddedWhenItsWorldDisappears() {
        request();claimed();assertFalse(cycle.abandonUnattempted());assertEquals(0,owner.failures);owner.activated.complete(true);cycle.tick();assertEquals(1,bodies.adds);assertEquals(1,bodies.releases);
    }
}
